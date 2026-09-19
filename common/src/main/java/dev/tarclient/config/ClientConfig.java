package dev.tarclient.config;

import com.google.gson.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Shared schema: the launcher and the in-game editor use exactly the same settings. */
public final class ClientConfig {
    public record Setting(String key, String label, Object initial, double min, double max, double step) {}
    public record Module(String id, String name, String category, String description, List<Setting> settings) {}
    public static final List<Module> MODULES = new ArrayList<>();
    static Setting b(String k,String l,boolean v) { return new Setting(k,l,v,0,1,1); }
    static Setting n(String k,String l,double v,double min,double max,double step) { return new Setting(k,l,v,min,max,step); }
    static Setting s(String k,String l,String v) { return new Setting(k,l,v,0,0,0); }
    static void add(String id,String name,String cat,String desc,boolean on,Setting... settings) {
        var list = new ArrayList<Setting>(); list.add(b("enabled","Enabled",on)); list.addAll(List.of(settings));
        MODULES.add(new Module(id,name,cat,desc,List.copyOf(list)));
    }
    static Setting[] hud(int x,int y,Setting... extra) {
        var list=new ArrayList<Setting>(List.of(n("x","Horizontal position (%)",x,0,100,1),n("y","Vertical position (%)",y,0,100,1),n("scale","HUD scale",1,0.5,3,0.1),s("color","Text color (hex)","E8EDF5"),s("background","Background (ARGB hex)","B8181D29")));
        list.addAll(List.of(extra)); return list.toArray(Setting[]::new);
    }
    static {
        add("fps","FPS","HUD","Live frames per second.",true,hud(2,2));
        add("ping","Ping","HUD","Your server latency; singleplayer is shown separately.",true,hud(2,7));
        add("armor","Armor status","HUD","Durability for every armor slot, with a repeating low-durability alert.",true,hud(2,72,n("threshold","Alert below (%)",15,1,50,1),b("sound","Play warning sound",true),n("volume","Warning volume",0.7,0,1,0.1),n("cooldown","Warning interval (seconds)",10,2,120,1),b("percent","Show percent",true)));
        add("potions","Potion status","HUD","Active effect names, amplifiers and remaining time.",true,hud(80,20,b("beneficial","Show beneficial effects",true),b("harmful","Show harmful effects",true)));
        add("keys","Keystrokes + mouse","HUD","Movement keys, jump, mouse buttons and left/right CPS.",true,hud(2,18,b("mouse","Show mouse buttons",true),b("cps","Show clicks per second",true),s("pressed","Pressed color (hex)","A5F078")));
        add("crosshair","Custom crosshair","Visual","Choose size, gap, thickness, color, dot and outline.",true,n("size","Arm length",5,1,30,1),n("gap","Center gap",3,0,20,1),n("thickness","Thickness",1,1,8,1),s("color","Color (hex)","A5F078"),b("dot","Center dot",false),b("outline","Black outline",true),b("thirdPerson","Show in third person",false));
        add("fullbright","Fullbright","Visual","Adjust the lightmap brightness without applying potion effects.",false,n("strength","Brightness",1,0,1,0.05));
        add("nofog","No fog","Visual","Remove terrain fog; fluid fog can be changed separately.",false,b("fluids","Also remove water/lava fog",false));
        add("items","Item size","Visual","Scale all rendered items by context. Per-item overrides use namespace:item=scale.",false,n("hand","First-person scale",0.8,0.1,2.5,0.05),n("gui","Inventory scale",1,0.1,2,0.05),n("ground","Dropped item scale",1,0.1,3,0.05),n("thirdPerson","Third-person scale",1,0.1,3,0.05),n("fixed","Item frame / display scale",1,0.1,3,0.05),s("overrides","Per-item overrides (semicolon separated)",""));
        add("shield","Low / side shield","Visual","Adjust the held shield position while preserving blocking behavior.",true,n("down","Lower shield",0.3,0,1.5,0.05),n("side","Move shield outward",0.15,0,1.5,0.05));
        add("fire","Low fire","Visual","Lower the on-screen fire overlay.",true,n("offset","Lower fire",0.5,0,1.5,0.05));
        add("glint","Enchantment glint","Visual","Disable glint or tune the vanilla glint strength and speed.",true,b("hidden","Hide glint",false),n("strength","Glint strength",0.4,0,1,0.05),n("speed","Glint speed",0.5,0,1,0.05));
        add("hitboxes","Hitbox outlines","Visual","Customize F3+B debug outlines. Collision and reach stay vanilla.",false,b("always","Show without F3+B",false),s("color","Outline color (hex)","A5F078"),b("eyeLine","Show eye-height box",true),b("direction","Show view direction",true));
        add("borderless","Borderless fullscreen","Window","F11 uses a borderless monitor-sized window.",true);
    }
    private final JsonObject values;
    public ClientConfig() { this(new JsonObject()); }
    private ClientConfig(JsonObject values) { this.values=values; validate(); }
    public static ClientConfig read(Path path) throws IOException {
        if (!Files.exists(path)) return new ClientConfig();
        try (var reader=Files.newBufferedReader(path)) { return new ClientConfig(JsonParser.parseReader(reader).getAsJsonObject()); }
        catch (RuntimeException e) { throw new IOException("Invalid settings in "+path+". Rename it to reset settings.",e); }
    }
    public synchronized void save(Path path) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path tmp=Files.createTempFile(path.toAbsolutePath().getParent(),"tar-settings-",".tmp");
        try { Files.writeString(tmp,new GsonBuilder().setPrettyPrinting().create().toJson(values));
            try { Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e) { Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(tmp); }
    }
    public synchronized JsonElement get(String module,String key) { return values.getAsJsonObject(module).get(key); }
    public boolean on(String module) { return bool(module,"enabled"); }
    public boolean bool(String m,String k) { return get(m,k).getAsBoolean(); }
    public double number(String m,String k) { return get(m,k).getAsDouble(); }
    public float f(String m,String k) { return (float)number(m,k); }
    public int i(String m,String k) { return (int)Math.round(number(m,k)); }
    public String text(String m,String k) { return get(m,k).getAsString(); }
    public int color(String m,String k) {
        try { String v=text(m,k).replace("#",""); long c=Long.parseLong(v,16); return (int)(v.length()<=6?c|0xFF000000L:c); }
        catch(Exception e) { return 0xFFE8EDF5; }
    }
    public synchronized void set(String m,String k,Object value) {
        values.getAsJsonObject(m).add(k,new Gson().toJsonTree(value)); validate();
    }
    public double itemScale(String id,String context) {
        for(String part:text("items","overrides").split(";")) {
            String[] pair=part.trim().split("=");
            if(pair.length==2 && pair[0].trim().equals(id)) try { double x=Double.parseDouble(pair[1].trim()); if(Double.isFinite(x)) return Math.clamp(x,0.1,3); } catch(NumberFormatException ignored) {}
        }
        return number("items",context);
    }
    private synchronized void validate() {
        for(Module module:MODULES) {
            if(!values.has(module.id())||!values.get(module.id()).isJsonObject()) values.add(module.id(),new JsonObject());
            JsonObject obj=values.getAsJsonObject(module.id());
            for(Setting setting:module.settings()) {
                try {
                    JsonElement v=obj.get(setting.key()); if(v==null||!v.isJsonPrimitive()) throw new IllegalArgumentException();
                    if(setting.initial() instanceof Boolean && !v.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException();
                    if(setting.initial() instanceof Number) { double d=v.getAsDouble(); if(!Double.isFinite(d)) throw new IllegalArgumentException(); obj.addProperty(setting.key(),Math.clamp(d,setting.min(),setting.max())); }
                    if(setting.initial() instanceof String && !v.getAsJsonPrimitive().isString()) throw new IllegalArgumentException();
                } catch(Exception e) { obj.add(setting.key(),new Gson().toJsonTree(setting.initial())); }
            }
        }
    }
}
