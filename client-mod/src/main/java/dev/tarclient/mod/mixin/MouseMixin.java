package dev.tarclient.mod.mixin;
import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.tarclient.mod.TarClient;
@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method="onMouseButton",at=@At("HEAD"))
    private void tar$click(long window,MouseInput input,int action,CallbackInfo ci){if(action==1&&MinecraftClient.getInstance().currentScreen==null)TarClient.click(input.button());}
}
