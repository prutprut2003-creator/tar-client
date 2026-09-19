package dev.tarclient.mod.mixin;

import dev.tarclient.mod.TarClient;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method="onKey",at=@At("HEAD"),cancellable=true)
    private void tar$menu(long window,int action,KeyInput input,CallbackInfo ci){
        if(window!=client.getWindow().getHandle()||TarClient.MENU_KEY==null||!TarClient.MENU_KEY.matchesKey(input))return;
        // Leave modifier keys alone while typing or changing a key binding.
        if(client.currentScreen instanceof net.minecraft.client.gui.screen.option.ControlsOptionsScreen)return;
        if(client.currentScreen instanceof net.minecraft.client.gui.screen.option.KeybindsScreen)return;
        if(client.currentScreen!=null&&client.currentScreen.getFocused() instanceof TextFieldWidget
            &&!(client.currentScreen instanceof dev.tarclient.mod.TarSettingsScreen&&input.key()==GLFW.GLFW_KEY_RIGHT_SHIFT))return;
        if(action==GLFW.GLFW_PRESS)TarClient.toggleMenu(client);
        TarClient.MENU_KEY.setPressed(false);
        ci.cancel();
    }
}
