package dev.tarclient.mod.mixin;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.tarclient.mod.TarClient;
@Mixin(InGameOverlayRenderer.class)
public class FireMixin {
    @Inject(method="renderFireOverlay",at=@At("HEAD"))
    private static void tar$lower(MatrixStack matrices,VertexConsumerProvider consumers,Sprite sprite,CallbackInfo ci){matrices.push();if(TarClient.CONFIG.on("fire"))matrices.translate(0,-TarClient.CONFIG.f("fire","offset"),0);}
    @Inject(method="renderFireOverlay",at=@At("RETURN"))
    private static void tar$restore(MatrixStack matrices,VertexConsumerProvider consumers,Sprite sprite,CallbackInfo ci){matrices.pop();}
}
