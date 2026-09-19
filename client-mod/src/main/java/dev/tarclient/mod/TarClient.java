package dev.tarclient.mod;

import dev.tarclient.config.ClientConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;

public final class TarClient implements ClientModInitializer {
    public static ClientConfig CONFIG=new ClientConfig();
    private static final Path PATH=FabricLoader.getInstance().getConfigDir().resolve("tarclient.json");
    public static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    public static final Deque<Long> LEFT_CLICKS=new ArrayDeque<>(),RIGHT_CLICKS=new ArrayDeque<>();
    private long lastAlert;
    private boolean glintApplied;
    private double vanillaGlintStrength,vanillaGlintSpeed;
    public static KeyBinding MENU_KEY;
    @Override public void onInitializeClient() {
        try{CONFIG=ClientConfig.read(PATH);}catch(Exception e){org.slf4j.LoggerFactory.getLogger("TarClient").error("Could not read settings; defaults are active",e);}
        MENU_KEY=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tarclient.menu",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_SHIFT,KeyBinding.Category.create(Identifier.of("tarclient","client"))));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.of("tarclient","hud"),(context,ticks)->{if(!(MinecraftClient.getInstance().currentScreen instanceof HudEditorScreen))TarHud.render(context,false);});
        ScreenEvents.AFTER_INIT.register((client,screen,w,h)->{if(screen instanceof TitleScreen||screen instanceof GameMenuScreen)Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Tar settings"),b->client.setScreen(new TarSettingsScreen(screen))).dimensions(w-114,8,106,20).build());});
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            long now=System.currentTimeMillis();prune(LEFT_CLICKS,now);prune(RIGHT_CLICKS,now);
            updateGlint(client);
            if(client.player==null){lastAlert=0;return;}
            if(CONFIG.on("armor")&&CONFIG.bool("armor","sound")&&now-lastAlert>=CONFIG.number("armor","cooldown")*1000) {
                for(var slot:ARMOR) {var stack=client.player.getEquippedStack(slot);if(stack.isDamageable()&&(stack.getMaxDamage()-stack.getDamage())*100.0/stack.getMaxDamage()<=CONFIG.number("armor","threshold")) {
                    client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),1.5f,CONFIG.f("armor","volume")));lastAlert=now;break;
                }}
            }
        });
        org.slf4j.LoggerFactory.getLogger("TarClient").info("Tar Client initialized: 14 modules for Minecraft 1.21.11");
    }
    public static void toggleMenu(MinecraftClient client){
        if(client.currentScreen instanceof TarSettingsScreen menu)menu.close();
        else if(client.currentScreen instanceof HudEditorScreen editor)editor.close();
        else client.setScreen(new TarSettingsScreen(client.currentScreen));
    }
    private void updateGlint(MinecraftClient client) {
        if(CONFIG.on("glint")) {
            if(!glintApplied){vanillaGlintStrength=client.options.getGlintStrength().getValue();vanillaGlintSpeed=client.options.getGlintSpeed().getValue();glintApplied=true;}
            client.options.getGlintStrength().setValue(CONFIG.bool("glint","hidden")?0.0:CONFIG.number("glint","strength"));
            client.options.getGlintSpeed().setValue(CONFIG.number("glint","speed"));
        }else if(glintApplied){client.options.getGlintStrength().setValue(vanillaGlintStrength);client.options.getGlintSpeed().setValue(vanillaGlintSpeed);glintApplied=false;}
    }
    public static void click(int button){if(button==0)LEFT_CLICKS.addLast(System.currentTimeMillis());if(button==1)RIGHT_CLICKS.addLast(System.currentTimeMillis());}
    private static void prune(Deque<Long> q,long now){while(!q.isEmpty()&&q.peekFirst()<now-1000)q.removeFirst();}
    public static void save(){try{CONFIG.save(PATH);}catch(Exception e){org.slf4j.LoggerFactory.getLogger("TarClient").error("Could not save settings",e);MinecraftClient c=MinecraftClient.getInstance();if(c.player!=null)c.player.sendMessage(Text.literal("Tar Client: could not save settings: "+e.getMessage()),false);}}
}
