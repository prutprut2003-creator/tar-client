package dev.tarclient.mod.mixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.InactivityFpsLimiter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import static dev.tarclient.mod.TarClient.CONFIG;
@Mixin(InactivityFpsLimiter.class)
public class FpsLimitMixin {
    @Inject(method="update",at=@At("RETURN"),cancellable=true)
    private void tar$limit(CallbackInfoReturnable<Integer> cir){if(CONFIG.on("unfocused")&&!MinecraftClient.getInstance().isWindowFocused())cir.setReturnValue(Math.min(cir.getReturnValue(),CONFIG.i("unfocused","fps")));}
}
