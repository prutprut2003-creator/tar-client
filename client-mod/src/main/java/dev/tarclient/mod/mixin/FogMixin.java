package dev.tarclient.mod.mixin;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.enums.CameraSubmersionType;
import com.mojang.blaze3d.buffers.Std140Builder;
import org.joml.Vector4f;
import java.nio.ByteBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.tarclient.mod.TarClient;
@Mixin(FogRenderer.class)
public class FogMixin {
    @Inject(method="applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V",at=@At("HEAD"),cancellable=true)
    private void tar$fog(ByteBuffer buffer,int position,Vector4f color,float a,float b,float c,float d,float e,float f,CallbackInfo ci){
        if(!TarClient.CONFIG.on("nofog"))return;
        var client=MinecraftClient.getInstance();if(client.gameRenderer==null)return;
        if(!TarClient.CONFIG.bool("nofog","fluids")&&client.gameRenderer.getCamera().getSubmersionType()!=CameraSubmersionType.NONE)return;
        buffer.position(position);Std140Builder.intoBuffer(buffer).putVec4(color).putFloat(1.0e8f).putFloat(1.1e8f).putFloat(1.0e8f).putFloat(1.1e8f).putFloat(1.1e8f).putFloat(1.1e8f);ci.cancel();
    }
}
