package dev.tarclient.mod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.*;
import net.minecraft.client.util.InputUtil;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import org.lwjgl.glfw.GLFW;
import static dev.tarclient.mod.TarClient.CONFIG;

public final class ClientFeatures {
    public static KeyBinding zoomKey,freelookKey;
    public static boolean freelooking;
    public static float yaw,pitch;
    private static Perspective previous;
    private static double zoom=1;
    private static long lastFrame;
    public static double reach;
    public static long lastAttack;
    public static volatile float saturation;
    private static int ticks;
    public static void initialize(){
        var category=KeyBinding.Category.create(Identifier.of("tarclient","features"));
        zoomKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tarclient.zoom",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_C,category));
        freelookKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tarclient.freelook",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_LEFT_ALT,category));
        AttackEntityCallback.EVENT.register((player,world,hand,entity,hit)->{
            if(world.isClient()&&CONFIG.on("reach")&&(!CONFIG.bool("reach","playersOnly")||entity instanceof PlayerEntity)){
                var eye=player.getEyePos();var end=eye.add(player.getRotationVec(1).multiply(player.getEntityInteractionRange()));
                var point=entity.getBoundingBox().raycast(eye,end);
                if(point.isPresent()){reach=eye.distanceTo(point.get());lastAttack=System.currentTimeMillis();}
            }
            return ActionResult.PASS;
        });
    }
    public static void tick(MinecraftClient client){
        boolean active=CONFIG.on("freelook")&&freelookKey.isPressed()&&client.player!=null&&!client.player.isSleeping()&&client.currentScreen==null&&client.isWindowFocused();
        if(active&&!freelooking){previous=client.options.getPerspective();yaw=client.player.getYaw();pitch=client.player.getPitch();client.options.setPerspective(Perspective.THIRD_PERSON_BACK);}
        if(!active&&freelooking&&previous!=null)client.options.setPerspective(previous);
        freelooking=active;
        if(client.player==null){lastAttack=0;saturation=0;}
        else if(++ticks%10==0){
            if(client.isInSingleplayer()&&client.getServer()!=null){
                var server=client.getServer();var uuid=client.player.getUuid();
                server.execute(()->{var player=server.getPlayerManager().getPlayer(uuid);if(player!=null)saturation=player.getHungerManager().getSaturationLevel();});
            }else saturation=client.player.getHungerManager().getSaturationLevel();
        }
        SpotifyMedia.tick(CONFIG.on("spotify"));
    }
    public static void mouse(double dx,double dy){yaw+=(float)(dx*0.15*CONFIG.number("freelook","sensitivity"));pitch=(float)Math.clamp(pitch+dy*0.15*CONFIG.number("freelook","sensitivity"),-90,90);}
    public static float fov(float base){
        var mc=MinecraftClient.getInstance();boolean active=CONFIG.on("zoom")&&zoomKey!=null&&zoomKey.isPressed()&&mc.currentScreen==null&&mc.isWindowFocused();
        double target=active?1/CONFIG.number("zoom","factor"):1;
        long now=System.nanoTime();double dt=lastFrame==0?1:Math.clamp((now-lastFrame)/1e9,0,0.1);lastFrame=now;
        zoom=CONFIG.bool("zoom","smooth")?zoom+(target-zoom)*(1-Math.exp(-dt*18)):target;
        return (float)(base*zoom);
    }
    public static void applyTime(){
        var mc=MinecraftClient.getInstance();
        if(mc.player==null||mc.getNetworkHandler()==null||!CONFIG.on("timechanger"))return;
        if(mc.getNetworkHandler().getCommandDispatcher().getRoot().getChild("time")==null){mc.player.sendMessage(Text.literal("Time changer needs /time permission (OP or singleplayer cheats)."),false);return;}
        mc.getNetworkHandler().sendChatCommand("time set "+CONFIG.i("timechanger","time"));
    }
}
