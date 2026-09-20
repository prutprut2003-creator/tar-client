package dev.tarclient.launcher;

import java.io.*;
import java.nio.file.*;
import java.util.HexFormat;
import java.security.MessageDigest;

/** Installs the bundled core and backs up the previous preview on upgrade. */
final class CoreInstaller {
    static void install(Path game, InputStream bundled, String version) throws Exception {
        if (bundled == null) throw new IOException("Bundled Tar Client mod is missing. Rebuild the distribution.");
        Path target = game.resolve("mods/tar-client-" + version + ".jar");
        Files.createDirectories(target.getParent());
        byte[] bytes = bundled.readAllBytes();
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (!Files.exists(target) || !Net.hash(target,"SHA-256").equals(hash)) {
            Path pending = Files.createTempFile(target.getParent(),".tar-core-",".tmp");
            try {
                Files.write(pending, bytes);
                Files.move(pending,target,StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(pending); }
        }
        for (String previous : new String[]{"0.1.0", "0.2.0", "0.2.1"}) {
            Path old = game.resolve("mods/tar-client-"+previous+".jar");
            if (!old.equals(target) && Files.isRegularFile(old)) {
                var metadata = new ModManager(game,s->{}).metadata(old);
                if (metadata.has("id") && "tarclient".equals(metadata.get("id").getAsString())) {
                    Path backup = game.resolve("removed-mods/tar-client-"+previous+"-"+System.currentTimeMillis()+".jar");
                    Files.createDirectories(backup.getParent());
                    Files.move(old,backup);
                }
            }
        }
    }
}
