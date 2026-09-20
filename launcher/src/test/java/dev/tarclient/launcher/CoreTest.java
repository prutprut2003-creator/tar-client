package dev.tarclient.launcher;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import dev.tarclient.config.ClientConfig;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class CoreTest {
    @TempDir Path temp;
    @Test void profilesKeepModuleSettingsAndRejectEscapingNames() throws Exception {
        var store=new dev.tarclient.config.ProfileStore(temp);store.presets();
        var bedwars=store.load("Bedwars");assertTrue(bedwars.on("keys"));assertFalse(bedwars.on("fullbright"));
        var smp=store.load("SMP");assertTrue(smp.on("fullbright"));assertFalse(smp.on("keys"));
        bedwars.set("armor","showBackground",true);bedwars.set("zoom","factor",7);store.save("Custom",bedwars);
        var restored=store.load("Custom");assertTrue(restored.bool("armor","showBackground"));assertEquals(7,restored.number("zoom","factor"));
        restored.set("keys","enabled",false);assertTrue(store.load("Custom").on("keys"));
        store.save("Bedwars",restored);store.presets();assertFalse(store.load("Bedwars").on("keys"));
        for(String bad:List.of("../escape","C:/escape","","a/b","..\\escape"))assertThrows(java.io.IOException.class,()->store.save(bad,restored));
    }
    @Test void oldSettingsAcquireEveryHudBackgroundSwitch() throws Exception {
        Path file=temp.resolve("old.json");Files.writeString(file,"{\"fps\":{\"enabled\":true,\"background\":\"80000000\"}}");
        var c=ClientConfig.read(file);assertTrue(c.on("fps"));assertEquals("80000000",c.text("fps","background"));
        for(var module:ClientConfig.MODULES)if(module.category().equals("HUD")){assertFalse(c.bool(module.id(),"showBackground"));c.set(module.id(),"showBackground",true);assertTrue(c.bool(module.id(),"showBackground"));}
        assertTrue(c.bool("armor","horizontal"));assertEquals(4,c.number("zoom","factor"));
    }
    @Test void latestUpgradeBacksUpPreviousPatch() throws Exception {
        Path mods=temp.resolve("mods");Files.createDirectories(mods);
        byte[] old=Files.readAllBytes(jar("previous-patch.jar","{\"id\":\"tarclient\",\"version\":\"0.2.1\"}","fabric.mod.json"));Files.write(mods.resolve("tar-client-0.2.1.jar"),old);
        byte[] update=Files.readAllBytes(jar("new-core.jar","{\"id\":\"tarclient\",\"version\":\"0.3.0\"}","fabric.mod.json"));
        CoreInstaller.install(temp,new java.io.ByteArrayInputStream(update),"0.3.0");assertFalse(Files.exists(mods.resolve("tar-client-0.2.1.jar")));assertArrayEquals(update,Files.readAllBytes(mods.resolve("tar-client-0.3.0.jar")));
        try(var backups=Files.list(temp.resolve("removed-mods"))){assertArrayEquals(old,Files.readAllBytes(backups.findFirst().orElseThrow()));}
    }
    @Test void rejectsTraversalAndAbsoluteFilenames() {
        for(String bad:List.of("../escape.jar","C:/escape.jar","a/../../escape","..\\escape.jar","stream:ads"))assertThrows(Exception.class,()->Net.child(temp,bad));
        assertDoesNotThrow(()->Net.child(temp,"valid.jar"));
    }
    @Test void honorsOrderedRulesAndFeatures() {
        JsonObject rules=JsonParser.parseString("{\"rules\":[{\"action\":\"allow\"},{\"action\":\"disallow\",\"os\":{\"name\":\"windows\"}}]}").getAsJsonObject();
        assertFalse(GameInstaller.allowed(rules));
        JsonObject demo=JsonParser.parseString("{\"rules\":[{\"action\":\"allow\",\"features\":{\"is_demo_user\":true}}]}").getAsJsonObject();
        assertFalse(GameInstaller.allowed(demo));assertTrue(GameInstaller.allowed(demo,Map.of("is_demo_user",true)));
    }
    @Test void settingsRoundTripClampAndPerItem() throws Exception {
        var c=new ClientConfig();c.set("armor","threshold",999);assertEquals(50,c.number("armor","threshold"));
        c.set("items","overrides","minecraft:diamond_sword=0.35;minecraft:apple=NaN");assertEquals(0.35,c.itemScale("minecraft:diamond_sword","hand"));assertEquals(0.8,c.itemScale("minecraft:apple","hand"));
        Path file=temp.resolve("settings.json");c.save(file);assertEquals(50,ClientConfig.read(file).number("armor","threshold"));
        Files.writeString(file,"not json");assertThrows(Exception.class,()->ClientConfig.read(file));
    }
    @Test void fabricVersionRangesAndAlternatives() throws Exception {
        assertTrue(ModManager.matches(new JsonPrimitive(">=1.21.10 <1.22"),"1.21.11"));assertFalse(ModManager.matches(new JsonPrimitive("1.21.1"),"1.21.11"));assertTrue(ModManager.matches(new JsonPrimitive("~1.21.1"),"1.21.11"));
        assertTrue(ModManager.matches(JsonParser.parseString("[\"1.21.10\",\"1.21.11\"]"),"1.21.11"));
    }
    @Test void localImportsRejectForgeWrongVersionAndDuplicates() throws Exception {
        var m=new ModManager(temp.resolve("game"),s->{});
        Path forge=jar("forge.jar","{}","META-INF/mods.toml");assertThrows(Exception.class,()->m.importJar(forge));
        Path wrong=jar("wrong.jar","{\"id\":\"test\",\"version\":\"1.0\",\"depends\":{\"minecraft\":\"1.20.1\"}}","fabric.mod.json");assertThrows(Exception.class,()->m.importJar(wrong));
        Path valid=jar("valid.jar","{\"id\":\"test\",\"version\":\"1.0\",\"depends\":{\"minecraft\":\"1.21.11\"}}","fabric.mod.json");m.importJar(valid);m.preflight();assertThrows(Exception.class,()->m.importJar(valid));
        m.toggle(m.list().getFirst());assertTrue(m.list().getFirst().toString().endsWith(".disabled"));m.toggle(m.list().getFirst());m.preflight();
    }
    @Test void missingDependenciesBlockLaunch() throws Exception {
        var m=new ModManager(temp.resolve("game"),s->{});m.importJar(jar("dependent.jar","{\"id\":\"test\",\"version\":\"1.0\",\"depends\":{\"missing\":\"*\"}}","fabric.mod.json"));assertThrows(Exception.class,m::preflight);
    }
    @Test void demoSessionIsExplicitAndTokenNotPrinted() {var demo=MicrosoftAuth.Session.demoSession();assertTrue(demo.demo());var live=new MicrosoftAuth.Session("Player","id","TOP-SECRET",0,false);assertFalse(live.toString().contains("TOP-SECRET"));}
    @Test void microsoftIdWorksForFreshAndLegacyPreferences() {
        assertEquals("c8d8f6e2-12dc-4499-911c-1c7294e91f44",MicrosoftAuth.clientId(null));
        for(String empty:List.of("", "  ", "\t\n"))assertEquals(MicrosoftAuth.DEFAULT_CLIENT_ID,MicrosoftAuth.clientId(empty));
        assertEquals("custom-application-id",MicrosoftAuth.clientId(" custom-application-id "));
    }
    @Test void patchUpgradeRemovesBothPreviousCoresWithoutTouchingOtherMods() throws Exception {
        Path mods=temp.resolve("mods");Files.createDirectories(mods);
        for(String previous:List.of("0.1.0","0.2.0")) {
            Files.copy(jar("old-"+previous+".jar","{\"id\":\"tarclient\",\"version\":\""+previous+"\"}","fabric.mod.json"),mods.resolve("tar-client-"+previous+".jar"));
        }
        Path other=mods.resolve("other.jar");Files.writeString(other,"keep this mod");
        byte[] update=Files.readAllBytes(jar("patch.jar","{\"id\":\"tarclient\",\"version\":\"0.2.1\"}","fabric.mod.json"));
        for(int i=0;i<2;i++)CoreInstaller.install(temp,new java.io.ByteArrayInputStream(update),"0.2.1");
        assertArrayEquals(update,Files.readAllBytes(mods.resolve("tar-client-0.2.1.jar")));
        assertFalse(Files.exists(mods.resolve("tar-client-0.1.0.jar")));
        assertFalse(Files.exists(mods.resolve("tar-client-0.2.0.jar")));
        assertEquals("keep this mod",Files.readString(other));
        try(var backups=Files.list(temp.resolve("removed-mods"))){assertEquals(2,backups.count());}
    }
    @Test void coreUpgradePreservesWorldsAndBacksUpPreviousCore() throws Exception {
        Path game=temp.resolve("game"),mods=game.resolve("mods");Files.createDirectories(mods);
        Path old=jar("previous.jar","{\"id\":\"tarclient\",\"version\":\"0.1.0\"}","fabric.mod.json");
        Files.copy(old,mods.resolve("tar-client-0.1.0.jar"));
        Path world=game.resolve("saves/My world/level.dat");Files.createDirectories(world.getParent());Files.writeString(world,"world data");
        Path other=mods.resolve("my-mod.jar");Files.writeString(other,"unrelated mod");
        byte[] update=Files.readAllBytes(jar("update.jar","{\"id\":\"tarclient\",\"version\":\"0.2.0\"}","fabric.mod.json"));
        for(int i=0;i<2;i++)CoreInstaller.install(game,new java.io.ByteArrayInputStream(update),"0.2.0");
        assertArrayEquals(update,Files.readAllBytes(mods.resolve("tar-client-0.2.0.jar")));
        assertFalse(Files.exists(mods.resolve("tar-client-0.1.0.jar")));
        assertEquals("world data",Files.readString(world));assertEquals("unrelated mod",Files.readString(other));
        try(var backups=Files.list(game.resolve("removed-mods"))){var saved=backups.toList();assertEquals(1,saved.size());assertArrayEquals(Files.readAllBytes(old),Files.readAllBytes(saved.getFirst()));}
    }
    @Test void missingBundledCoreLeavesPreviousInstallUntouched() throws Exception {
        Path old=temp.resolve("mods/tar-client-0.1.0.jar");Files.createDirectories(old.getParent());Files.writeString(old,"original");
        assertThrows(java.io.IOException.class,()->CoreInstaller.install(temp,null,"0.2.0"));assertEquals("original",Files.readString(old));
    }
    @Test void decodesWebpModrinthIcons() throws Exception {
        // Original 2x2 solid-color fixture, encoded losslessly as WebP.
        byte[] bytes=Base64.getDecoder().decode("UklGRh4AAABXRUJQVlA4TBEAAAAvAUAAAAdQvFJUpv+BiOh/AAA=");
        try(var input=new javax.imageio.stream.MemoryCacheImageInputStream(new java.io.ByteArrayInputStream(bytes))){
            var readers=javax.imageio.ImageIO.getImageReaders(input);assertTrue(readers.hasNext());var reader=readers.next();
            try{reader.setInput(input);var image=reader.read(0);assertEquals(2,image.getWidth());assertEquals(2,image.getHeight());assertEquals(0xFF147832,image.getRGB(0,0));}finally{reader.dispose();}
        }
    }
    private Path jar(String name,String content,String entry)throws Exception{Path p=temp.resolve(name);try(var zip=new ZipOutputStream(Files.newOutputStream(p))){zip.putNextEntry(new ZipEntry(entry));zip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();}return p;}
}
