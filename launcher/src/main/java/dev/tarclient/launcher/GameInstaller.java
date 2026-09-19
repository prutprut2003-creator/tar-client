package dev.tarclient.launcher;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.zip.*;
import java.io.*;

public final class GameInstaller {
    public static final String VERSION="1.21.11", LOADER="0.19.5";
    private final Path root;
    private final Consumer<String> progress;
    public GameInstaller(Path root,Consumer<String> progress) { this.root=root.toAbsolutePath();this.progress=progress; }
    public record Installation(JsonObject vanilla,JsonObject fabric,List<Path> classpath,Path natives,String assetsId) {}
    public Installation install() throws Exception {
        Files.createDirectories(root);
        progress.accept("Checking Minecraft "+VERSION+" manifest…");
        var manifest=Net.object("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
        JsonObject entry=null;
        for(var e:manifest.getAsJsonArray("versions")) if(e.getAsJsonObject().get("id").getAsString().equals(VERSION)) entry=e.getAsJsonObject();
        if(entry==null) throw new IOException("Minecraft "+VERSION+" is absent from the official manifest.");
        Path metadata=root.resolve("versions/"+VERSION+"/"+VERSION+".json");
        Net.download(entry.get("url").getAsString(),metadata,"SHA-1",entry.get("sha1").getAsString());
        var vanilla=JsonParser.parseString(Files.readString(metadata)).getAsJsonObject();
        var fabric=Net.object("https://meta.fabricmc.net/v2/versions/loader/"+VERSION+"/"+LOADER+"/profile/json");
        Net.writeJson(root.resolve("versions/tar-fabric/profile.json"),fabric);
        Path gameJar=root.resolve("versions/"+VERSION+"/"+VERSION+".jar");
        progress.accept("Verifying Minecraft client…");
        download(vanilla.getAsJsonObject("downloads").getAsJsonObject("client"),gameJar);
        LinkedHashMap<String,JsonObject> libs=new LinkedHashMap<>();
        // Fabric overrides a matching group/artifact/classifier, irrespective of version.
        for(var source:List.of(vanilla,fabric)) for(var e:source.getAsJsonArray("libraries")) {
            var lib=e.getAsJsonObject(); String[] c=lib.get("name").getAsString().split(":");
            libs.put(c[0]+":"+c[1]+(c.length>3?":"+c[3]:""),lib);
        }
        List<Path> cp=new ArrayList<>(); Path natives=root.resolve("natives/"+VERSION); Files.createDirectories(natives);
        int count=0;
        for(var lib:libs.values()) {
            if(!allowed(lib)) continue;
            progress.accept("Verifying library "+(++count)+" / "+libs.size());
            var downloads=lib.has("downloads")?lib.getAsJsonObject("downloads"):null;
            if(downloads!=null&&downloads.has("artifact")) {
                var artifact=downloads.getAsJsonObject("artifact"); Path path=Net.child(root.resolve("libraries"),artifact.get("path").getAsString());
                download(artifact,path); cp.add(path);
            } else if(downloads==null) {
                String relative=mavenPath(lib.get("name").getAsString()); Path path=Net.child(root.resolve("libraries"),relative);
                String base=lib.has("url")?lib.get("url").getAsString():"https://libraries.minecraft.net/";
                String sha=lib.has("sha1")?lib.get("sha1").getAsString():null;
                if(sha==null) {
                    // Fabric profile lacks hashes; retrieve its published Maven checksum.
                    var uri=Net.request(base+relative+".sha1").GET().build();
                    var response=java.net.http.HttpClient.newHttpClient().send(uri,java.net.http.HttpResponse.BodyHandlers.ofString());
                    if(response.statusCode()!=200) throw new IOException("Missing checksum for "+relative);
                    sha=response.body().trim().split("\\s+")[0];
                    if(!sha.matches("[a-fA-F0-9]{40}")) throw new IOException("Invalid Maven checksum");
                }
                Net.download(base+relative,path,"SHA-1",sha); cp.add(path);
            }
            if(lib.has("natives")&&lib.getAsJsonObject("natives").has("windows")) {
                String classifier=lib.getAsJsonObject("natives").get("windows").getAsString().replace("${arch}","64");
                var artifact=downloads.getAsJsonObject("classifiers").getAsJsonObject(classifier);
                Path path=Net.child(root.resolve("libraries"),artifact.get("path").getAsString()); download(artifact,path);
                try(var zip=new ZipFile(path.toFile())) { for(var e:Collections.list(zip.entries())) if(!e.isDirectory()&&!e.getName().startsWith("META-INF/")) {
                    Path out=Net.child(natives,e.getName()); Files.createDirectories(out.getParent());try(var in=zip.getInputStream(e)){Files.copy(in,out,StandardCopyOption.REPLACE_EXISTING);}
                }}
            }
        }
        cp.add(gameJar);
        var indexInfo=vanilla.getAsJsonObject("assetIndex"); String id=indexInfo.get("id").getAsString();
        Path index=root.resolve("assets/indexes/"+id+".json"); download(indexInfo,index);
        var objects=JsonParser.parseString(Files.readString(index)).getAsJsonObject().getAsJsonObject("objects");
        progress.accept("Verifying "+objects.size()+" game assets…");
        try(var executor=Executors.newFixedThreadPool(12)) {
            List<Future<?>> futures=new ArrayList<>(); var done=new java.util.concurrent.atomic.AtomicInteger();
            Set<String> unique=new HashSet<>();
            for(var e:objects.entrySet()) {
                String hash=e.getValue().getAsJsonObject().get("hash").getAsString(); if(!unique.add(hash))continue;
                futures.add(executor.submit(()->{try {
                    String key=hash.substring(0,2)+"/"+hash;
                    Net.download("https://resources.download.minecraft.net/"+key,root.resolve("assets/objects/"+key),"SHA-1",hash);
                    int n=done.incrementAndGet(); if(n%100==0)progress.accept("Game assets: "+n+" verified");
                } catch(Exception ex) { throw new CompletionException(ex); }}));
            }
            for(var future:futures) future.get();
        }
        return new Installation(vanilla,fabric,cp,natives,id);
    }
    private void download(JsonObject info,Path path) throws Exception { Net.download(info.get("url").getAsString(),path,"SHA-1",info.get("sha1").getAsString()); }
    public static String mavenPath(String coordinate) {
        String[] c=coordinate.split(":");
        if(c.length<3||c.length>4)throw new IllegalArgumentException("Bad Maven coordinate");
        return c[0].replace('.','/')+"/"+c[1]+"/"+c[2]+"/"+c[1]+"-"+c[2]+(c.length==4?"-"+c[3]:"")+".jar";
    }
    public static boolean allowed(JsonObject entry) { return allowed(entry,Map.of()); }
    public static boolean allowed(JsonObject entry,Map<String,Boolean> features) {
        if(!entry.has("rules"))return true;
        boolean result=false;
        for(var rule:entry.getAsJsonArray("rules")) {
            var r=rule.getAsJsonObject(); boolean matches=true;
            if(r.has("os")) {
                var os=r.getAsJsonObject("os");
                if(os.has("name"))matches&=os.get("name").getAsString().equals("windows");
                if(os.has("arch"))matches&=System.getProperty("os.arch").matches(os.get("arch").getAsString());
                if(os.has("version"))matches&=System.getProperty("os.version").matches(os.get("version").getAsString());
            }
            if(r.has("features"))for(var f:r.getAsJsonObject("features").entrySet())matches&=features.getOrDefault(f.getKey(),false)==f.getValue().getAsBoolean();
            if(matches)result=r.get("action").getAsString().equals("allow");
        }
        return result;
    }
    public List<String> command(Installation installation,MicrosoftAuth.Session session,int ram,String java) throws Exception {
        if(session.expiresAt()<System.currentTimeMillis()+60000)throw new IOException("Your session expired. Sign in again.");
        if(ram<2048||ram>32768)throw new IOException("Memory must be between 2048 and 32768 MB.");
        Map<String,String> v=new HashMap<>();
        v.put("auth_player_name",session.name()); v.put("version_name","TarClient-"+VERSION); v.put("game_directory",root.toString());
        v.put("assets_root",root.resolve("assets").toString());v.put("assets_index_name",installation.assetsId());v.put("auth_uuid",session.uuid());v.put("auth_access_token",session.accessToken());
        v.put("user_type","msa");v.put("version_type","Tar Client");v.put("natives_directory",installation.natives().toString());v.put("launcher_name","TarClient");v.put("launcher_version","0.2.0");
        v.put("classpath",String.join(File.pathSeparator,installation.classpath().stream().map(Path::toString).toList()));
        v.put("library_directory",root.resolve("libraries").toString());v.put("classpath_separator",File.pathSeparator);v.put("clientid","");v.put("auth_xuid","");v.put("resolution_width","1280");v.put("resolution_height","800");
        List<String> args=new ArrayList<>(List.of(java,"-Xms512M","-Xmx"+ram+"M","-Dfile.encoding=UTF-8"));
        Map<String,Boolean> features=Map.of("is_demo_user",session.demo(),"has_custom_resolution",true);
        arguments(installation.vanilla(),"jvm",args,v,features); arguments(installation.fabric(),"jvm",args,v,features);
        args.add(installation.fabric().get("mainClass").getAsString());
        arguments(installation.vanilla(),"game",args,v,features); arguments(installation.fabric(),"game",args,v,features);
        return args;
    }
    private void arguments(JsonObject source,String type,List<String> out,Map<String,String> values,Map<String,Boolean> features) throws IOException {
        if(!source.has("arguments")||!source.getAsJsonObject("arguments").has(type))return;
        for(var a:source.getAsJsonObject("arguments").getAsJsonArray(type)) {
            if(a.isJsonPrimitive())out.add(expand(a.getAsString(),values));
            else if(allowed(a.getAsJsonObject(),features)) {
                var value=a.getAsJsonObject().get("value");
                if(value.isJsonArray())for(var x:value.getAsJsonArray())out.add(expand(x.getAsString(),values)); else out.add(expand(value.getAsString(),values));
            }
        }
    }
    private String expand(String arg,Map<String,String> values) throws IOException {
        for(var e:values.entrySet())arg=arg.replace("${"+e.getKey()+"}",e.getValue());
        if(arg.contains("${"))throw new IOException("Unsupported launch argument: "+arg);
        return arg;
    }
    public Process launch(Installation installation,MicrosoftAuth.Session session,int ram,String java) throws Exception {
        var args=command(installation,session,ram,java);
        // Windows command lines have a 32K limit. Java's argument file handles long library paths.
        Path argFile=Files.createTempFile(root,".launch-",".args");
        List<String> encoded=args.subList(1,args.size()).stream().map(GameInstaller::quoteArg).toList();
        Files.write(argFile,encoded);
        Process p;
        try { p=new ProcessBuilder(java,"@"+argFile).directory(root.toFile()).redirectErrorStream(true).start(); }
        catch(Exception e) { Files.deleteIfExists(argFile); throw e; }
        // Delete promptly once the JVM has consumed the file, and always after exit.
        Thread.ofVirtual().start(()->{try {Thread.sleep(8000);Files.deleteIfExists(argFile);}catch(Exception ignored){}});
        p.onExit().thenRun(()->{try{Files.deleteIfExists(argFile);}catch(IOException ignored){}});
        return p;
    }
    static String quoteArg(String value) { return "\""+value.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\""; }
}
