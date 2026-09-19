package dev.tarclient.mod.mixin;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import dev.tarclient.mod.TarClient;
@Mixin(LightmapTextureManager.class)
public class LightmapMixin {
    // Fourth lightmap uniform is the visual night-vision factor. No player effect is added.
    @ModifyArg(method="update",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",ordinal=3),index=0)
    private float tar$brightness(float original){return TarClient.CONFIG.on("fullbright")?Math.max(original,TarClient.CONFIG.f("fullbright","strength")):original;}
}
