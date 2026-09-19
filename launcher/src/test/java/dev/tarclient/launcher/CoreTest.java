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
    private Path jar(String name,String content,String entry)throws Exception{Path p=temp.resolve(name);try(var zip=new ZipOutputStream(Files.newOutputStream(p))){zip.putNextEntry(new ZipEntry(entry));zip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();}return p;}
}
