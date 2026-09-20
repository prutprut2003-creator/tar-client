package dev.tarclient.launcher;

import com.google.gson.*;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.zip.*;
import java.io.*;

public final class ModManager {
    private static final String API="https://api.modrinth.com/v2/";
    private final Path game,mods,lock;
    private final Consumer<String> status;
    public ModManager(Path game,Consumer<String> status) throws IOException { this.game=game;this.mods=game.resolve("mods");this.lock=game.resolve("tar-mods.json");this.status=status;Files.createDirectories(mods); }
    public JsonArray search(String query) throws Exception {
        String facets="[[\"project_type:mod\"],[\"categories:fabric\"],[\"versions:1.21.11\"],[\"client_side:required\",\"client_side:optional\"]]";
        return Net.object(API+"search?limit=30&query="+Net.enc(query)+"&facets="+Net.enc(facets)).getAsJsonArray("hits");
    }
    public record SearchPage(JsonArray hits,int total) {}
    public SearchPage searchPage(String query,String index,int offset,String category) throws Exception {
        if(!Set.of("relevance","downloads","updated","newest").contains(index))throw new IllegalArgumentException("Unknown sort order");
        if(offset<0)throw new IllegalArgumentException("Invalid page");
        var facets=JsonParser.parseString("[[\"project_type:mod\"],[\"categories:fabric\"],[\"versions:1.21.11\"],[\"client_side:required\",\"client_side:optional\"]]").getAsJsonArray();
        if(!category.isBlank()){
            if(!Set.of("optimization","utility","decoration").contains(category))throw new IllegalArgumentException("Unknown category");
            var filter=new JsonArray();filter.add("categories:"+category);facets.add(filter);
        }
        var result=Net.object(API+"search?limit=12&offset="+offset+"&index="+index+"&query="+Net.enc(query)+"&facets="+Net.enc(facets.toString()));
        return new SearchPage(result.getAsJsonArray("hits"),result.get("total_hits").getAsInt());
    }
    private JsonObject latest(String project) throws Exception {
        var versions=Net.json(API+"project/"+Net.enc(project)+"/version?loaders="+Net.enc("[\"fabric\"]")+"&game_versions="+Net.enc("[\"1.21.11\"]")).getAsJsonArray();
        if(versions.isEmpty())throw new IOException("No Fabric 1.21.11 release for "+project);
        for(var v:versions)if(v.getAsJsonObject().get("version_type").getAsString().equals("release"))return v.getAsJsonObject();
        return versions.get(0).getAsJsonObject();
    }
    public synchronized void install(String project) throws Exception {
        LinkedHashMap<String,JsonObject> plan=new LinkedHashMap<>(); resolve(latest(project),plan,new HashSet<>());
        Path staging=Files.createTempDirectory(game,".mod-install-");
        try {
            var managed=readLock(); List<Path> staged=new ArrayList<>();
            for(var version:plan.values()) {
                JsonObject file=primary(version); String filename=file.get("filename").getAsString();
                if(!filename.toLowerCase(Locale.ROOT).endsWith(".jar"))throw new IOException("Mod release is not a JAR: "+filename);
                Path dest=Net.child(staging,filename); if(!dest.getParent().equals(staging.toAbsolutePath()))throw new IOException("Nested mod filename rejected");
                status.accept("Downloading "+version.get("name").getAsString());
                Net.download(file.get("url").getAsString(),dest,"SHA-512",file.getAsJsonObject("hashes").get("sha512").getAsString());
                validateJar(dest); staged.add(dest);
                var record=new JsonObject();record.add("version",version.get("id"));record.addProperty("filename",filename);record.add("sha512",file.getAsJsonObject("hashes").get("sha512"));record.add("dependencies",version.get("dependencies"));
                managed.add(version.get("project_id").getAsString(),record);
            }
            // Verify all downloads before touching installed mods. Keep backups until commit succeeds.
            Map<Path,Path> backups=new LinkedHashMap<>(); List<Path> added=new ArrayList<>();
            try {
                Set<String> incoming=new HashSet<>();
                for(Path p:staged)if(!incoming.add(metadata(p).get("id").getAsString()))throw new IOException("Dependency plan contains duplicate Fabric mod IDs");
                for(Path old:list()) {
                    JsonObject meta=metadata(old);
                    if(incoming.contains(meta.get("id").getAsString())) {
                        Path backup=staging.resolve("backup-"+old.getFileName());Files.move(old,backup);backups.put(old,backup);
                    }
                }
                for(Path p:staged) {Path target=mods.resolve(p.getFileName());if(Files.exists(target))throw new IOException("Filename conflict: "+target.getFileName());Files.move(p,target);added.add(target);}
                preflight(); Net.writeJson(lock,managed);
            } catch(Exception e) {
                for(Path p:added)Files.deleteIfExists(p);
                for(var b:backups.entrySet())Files.move(b.getValue(),b.getKey(),StandardCopyOption.REPLACE_EXISTING);
                throw e;
            }
        } finally { cleanStaging(staging); }
    }
    private void resolve(JsonObject v,LinkedHashMap<String,JsonObject> plan,Set<String> visiting) throws Exception {
        if(!contains(v.getAsJsonArray("game_versions"),"1.21.11")||!contains(v.getAsJsonArray("loaders"),"fabric"))throw new IOException("Incompatible required dependency: "+v.get("name"));
        String project=v.get("project_id").getAsString(), id=v.get("id").getAsString();
        if(plan.containsKey(project)) {if(!plan.get(project).get("id").getAsString().equals(id))throw new IOException("Conflicting dependency versions for "+project);return;}
        if(!visiting.add(project))throw new IOException("Circular dependency at "+project);
        for(var dependency:v.getAsJsonArray("dependencies")) {
            var d=dependency.getAsJsonObject(); if(!d.get("dependency_type").getAsString().equals("required"))continue;
            if(!d.get("version_id").isJsonNull())resolve(Net.object(API+"version/"+d.get("version_id").getAsString()),plan,visiting);
            else if(!d.get("project_id").isJsonNull())resolve(latest(d.get("project_id").getAsString()),plan,visiting);
            else throw new IOException("Dependency needs manual installation: "+d);
        }
        visiting.remove(project);plan.put(project,v);
    }
    static boolean contains(JsonArray array,String value) { for(var a:array)if(a.getAsString().equals(value))return true;return false; }
    static JsonObject primary(JsonObject v) throws IOException { for(var f:v.getAsJsonArray("files"))if(f.getAsJsonObject().get("primary").getAsBoolean())return f.getAsJsonObject();if(v.getAsJsonArray("files").isEmpty())throw new IOException("Release has no files");return v.getAsJsonArray("files").get(0).getAsJsonObject(); }
    private JsonObject readLock() throws IOException {return Files.exists(lock)?JsonParser.parseString(Files.readString(lock)).getAsJsonObject():new JsonObject();}
    public List<Path> list() throws IOException {try(var paths=Files.list(mods)){return paths.filter(p->p.toString().endsWith(".jar")||p.toString().endsWith(".jar.disabled")).sorted().toList();}}
    public JsonObject metadata(Path path) throws IOException {
        try(var zip=new ZipFile(path.toFile())) {
            var entry=zip.getEntry("fabric.mod.json");if(entry==null)throw new IOException(path.getFileName()+" is not a Fabric mod.");
            try(var in=zip.getInputStream(entry)) {return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
        }catch(RuntimeException e){throw new IOException("Invalid Fabric metadata: "+path.getFileName(),e);}
    }
    private void validateJar(Path path) throws Exception {
        var meta=metadata(path);
        if(!meta.has("id")||!meta.has("version"))throw new IOException("Missing mod ID or version");
        if(meta.has("environment")&&meta.get("environment").getAsString().equals("server"))throw new IOException("This is a server-only mod.");
        if(meta.has("depends")) {var d=meta.getAsJsonObject("depends");for(var e:Map.of("minecraft","1.21.11","java","21","fabricloader",GameInstaller.LOADER).entrySet())if(d.has(e.getKey())&&!matches(d.get(e.getKey()),e.getValue()))throw new IOException(meta.get("id").getAsString()+" requires "+e.getKey()+" "+d.get(e.getKey()));}
    }
    static boolean matches(JsonElement constraint,String version) throws Exception {
        if(constraint.isJsonArray()){for(var p:constraint.getAsJsonArray())if(matches(p,version))return true;return false;}
        return VersionPredicate.parse(constraint.getAsString()).test(Version.parse(version));
    }
    public synchronized void importJar(Path source) throws Exception {
        validateJar(source);String id=metadata(source).get("id").getAsString();
        for(Path p:list())if(metadata(p).get("id").getAsString().equals(id))throw new IOException("Mod "+id+" is already installed. Remove it before importing a replacement.");
        Path target=Net.child(mods,source.getFileName().toString());if(Files.exists(target))throw new IOException("A file with this name already exists.");
        Files.copy(source,target);
        // Dependency validation happens at launch, allowing several local JARs to be imported in any order.
    }
    public synchronized void toggle(Path file) throws Exception {
        if(!file.toAbsolutePath().getParent().equals(mods.toAbsolutePath()))throw new IOException("Invalid mod path");
        if(metadata(file).get("id").getAsString().equals("tarclient"))throw new IOException("Tar Client core is managed by the launcher.");
        String name=file.getFileName().toString();Path dest=mods.resolve(name.endsWith(".disabled")?name.substring(0,name.length()-9):name+".disabled");
        Files.move(file,dest);
    }
    public synchronized void remove(Path file) throws Exception {
        if(!file.toAbsolutePath().getParent().equals(mods.toAbsolutePath()))throw new IOException("Invalid mod path");
        if(metadata(file).get("id").getAsString().equals("tarclient"))throw new IOException("Tar Client core is managed by the launcher.");
        // Reversible removal; preserve the exact jar outside the mods directory.
        Path trash=game.resolve("removed-mods");Files.createDirectories(trash);Files.move(file,trash.resolve(System.currentTimeMillis()+"-"+file.getFileName()));
    }
    public void preflight() throws Exception {
        Map<String,String> installed=new HashMap<>(Map.of("minecraft","1.21.11","java","21","fabricloader",GameInstaller.LOADER));
        List<JsonObject> metas=new ArrayList<>();Set<String> roots=new HashSet<>();
        for(Path p:list())if(p.toString().endsWith(".jar")) {validateJar(p);String id=metadata(p).get("id").getAsString();if(!roots.add(id))throw new IOException("Duplicate mod ID: "+id);}
        for(Path p:list())if(p.toString().endsWith(".jar")) {try(var z=new ZipFile(p.toFile())) {collect(z,metas,installed,0);}}
        for(var m:metas) {
            if(m.has("depends")) for(var d:m.getAsJsonObject("depends").entrySet()) {
                if(!installed.containsKey(d.getKey())||!matches(d.getValue(),installed.get(d.getKey())))throw new IOException(m.get("id").getAsString()+" needs "+d.getKey()+" "+d.getValue()+". Install or enable a compatible dependency.");
            }
            if(m.has("breaks"))for(var d:m.getAsJsonObject("breaks").entrySet())if(installed.containsKey(d.getKey())&&matches(d.getValue(),installed.get(d.getKey())))throw new IOException(m.get("id").getAsString()+" conflicts with "+d.getKey());
        }
    }
    private void collect(ZipFile zip,List<JsonObject> metas,Map<String,String> installed,int depth) throws Exception {
        if(depth>8)throw new IOException("Nested mod depth exceeded");
        var e=zip.getEntry("fabric.mod.json");if(e==null)return;
        JsonObject meta;try(var in=zip.getInputStream(e)){meta=JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
        String id=meta.get("id").getAsString(),version=meta.get("version").getAsString();
        if(installed.containsKey(id)) {
            if(Version.parse(version).compareTo(Version.parse(installed.get(id)))<=0)return;
            metas.removeIf(m->m.get("id").getAsString().equals(id));
        }
        installed.put(id,version);metas.add(meta);
        if(meta.has("provides"))for(var alias:meta.getAsJsonArray("provides"))installed.putIfAbsent(alias.getAsString(),version);
        if(meta.has("jars"))for(var nested:meta.getAsJsonArray("jars")) {
            var item=zip.getEntry(nested.getAsJsonObject().get("file").getAsString());if(item==null)throw new IOException("Missing nested dependency in "+id);
            Path tmp=Files.createTempFile(game,".nested-",".jar");
            try {try(var in=zip.getInputStream(item)){Files.copy(in,tmp,StandardCopyOption.REPLACE_EXISTING);}try(var child=new ZipFile(tmp.toFile())){collect(child,metas,installed,depth+1);}}
            finally {Files.deleteIfExists(tmp);}
        }
    }
    private void cleanStaging(Path dir) throws IOException {try(var paths=Files.walk(dir)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    public void installDefaults() throws Exception {
        var managed=readLock();
        for(String project:List.of("fabric-api","modmenu","betterf3")) {
            var info=Net.object(API+"project/"+project);String id=info.get("id").getAsString();
            if(!managed.has(id))install(project);
        }
    }
    public void syncIntegrations(dev.tarclient.config.ClientConfig config)throws Exception {
        for(var entry:dev.tarclient.config.Integrations.ALL){
            Path found=null;for(Path p:list())if(metadata(p).get("id").getAsString().equals(entry.modId())){found=p;break;}
            if(config.on(entry.module())){
                if(found==null)install(entry.project());
                else if(found.toString().endsWith(".disabled"))toggle(found);
            }else if(found!=null&&found.toString().endsWith(".jar"))toggle(found);
        }
    }
}
