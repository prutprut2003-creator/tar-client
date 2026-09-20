package dev.tarclient.config;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Module snapshots only: no account details, worlds or mod binaries. */
public final class ProfileStore {
    private final Path folder;
    public ProfileStore(Path configFolder){folder=configFolder.resolve("tar-profiles");}
    private Path path(String name)throws IOException {
        if(name==null||!name.matches("[A-Za-z0-9][A-Za-z0-9 _-]{0,47}"))throw new IOException("Use 1-48 letters, numbers, spaces, underscores or hyphens; start with a letter or number.");
        return folder.resolve(name+".json");
    }
    public List<String> list()throws IOException {
        if(!Files.isDirectory(folder))return List.of();
        try(var files=Files.list(folder)){return files.filter(p->p.getFileName().toString().endsWith(".json")).map(p->p.getFileName().toString().replaceFirst("\\.json$","")).sorted(String.CASE_INSENSITIVE_ORDER).toList();}
    }
    public void save(String name,ClientConfig config)throws IOException{config.save(path(name));}
    public ClientConfig load(String name)throws IOException{Path p=path(name);if(!Files.isRegularFile(p))throw new IOException("Profile not found: "+name);return ClientConfig.read(p);}
    public void presets()throws IOException {
        for(String name:List.of("Bedwars","SMP")) {
            if(Files.exists(path(name)))continue;
            ClientConfig config=new ClientConfig();
            for(var m:ClientConfig.MODULES)config.set(m.id(),"enabled",false);
            for(String id:List.of("profiles","borderless","disconnect","unfocused"))config.set(id,"enabled",true);
            for(String id:name.equals("Bedwars")?List.of("fps","ping","keys","crosshair","armor"):List.of("fullbright","shield","fire","coordinates","shulker"))config.set(id,"enabled",true);
            save(name,config);
        }
    }
}
