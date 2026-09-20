package dev.tarclient.mod.mixin;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.*;
import net.minecraft.client.render.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import static dev.tarclient.mod.TarClient.CONFIG;
@Mixin(LivingEntityRenderer.class)
public class HitColorMixin {
    @Inject(method="getMixColor",at=@At("RETURN"),cancellable=true)
    private void tar$color(LivingEntityRenderState state,CallbackInfoReturnable<Integer> cir){if(CONFIG.on("hitcolor")&&state instanceof PlayerEntityRenderState&&state.hurt)cir.setReturnValue(CONFIG.color("hitcolor","color")|0xFF000000);}
    @Inject(method="getOverlay",at=@At("RETURN"),cancellable=true)
    private static void tar$overlay(LivingEntityRenderState state,float white,CallbackInfoReturnable<Integer> cir){if(CONFIG.on("hitcolor")&&state instanceof PlayerEntityRenderState&&state.hurt)cir.setReturnValue(OverlayTexture.getUv(white,false));}
}
