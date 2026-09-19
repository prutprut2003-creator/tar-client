package dev.tarclient.launcher;

import java.nio.file.*;
import java.util.*;

/** Explicit developer smoke runner. Never reuses or reads a player's account. */
public final class Smoke {
    public static void main(String[] args) throws Exception {
        if(args.length<2)throw new IllegalArgumentException("Usage: --smoke <isolated-instance-path> [--launch]");
        Path game=Path.of(args[1]).toAbsolutePath();Files.createDirectories(game);
        var mods=new ModManager(game,System.out::println);
        mods.installDefaults();
        try(var in=Smoke.class.getResourceAsStream("/bundled/tar-client.jar")){if(in!=null)Files.copy(in,game.resolve("mods/tar-client-0.1.0.jar"),StandardCopyOption.REPLACE_EXISTING);}
        mods.preflight();
        var installer=new GameInstaller(game,System.out::println);var install=installer.install();
        System.out.println("INSTALLATION VERIFIED: "+install.classpath().size()+" libraries");
        if(Arrays.asList(args).contains("--launch")) {
            var p=installer.launch(install,MicrosoftAuth.Session.demoSession(),3072,Path.of(System.getProperty("java.home"),"bin/java.exe").toString());
            try(var reader=p.inputReader();var log=Files.newBufferedWriter(game.resolve("smoke.log"))){String line;while((line=reader.readLine())!=null){log.write(line);log.newLine();log.flush();System.out.println(line);}}
            System.out.println("GAME EXIT: "+p.waitFor());
        }
    }
}
