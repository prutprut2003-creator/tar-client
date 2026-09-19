package dev.tarclient.mod.mixin;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.tarclient.mod.*;
@Mixin(InGameHud.class)
public class HudMixin {
    @Inject(method="renderCrosshair",at=@At("HEAD"),cancellable=true)
    private void tar$crosshair(DrawContext context,RenderTickCounter ticks,CallbackInfo ci){
        if(TarClient.CONFIG.on("crosshair")&&!MinecraftClient.getInstance().getDebugHud().shouldShowDebugHud()){TarHud.crosshair(context);ci.cancel();}
    }
}
