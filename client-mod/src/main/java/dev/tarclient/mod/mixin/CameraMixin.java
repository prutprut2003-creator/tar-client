package dev.tarclient.mod.mixin;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import dev.tarclient.mod.ClientFeatures;
@Mixin(Camera.class)
public class CameraMixin {
    @ModifyArgs(method="update",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/Camera;setRotation(FF)V",ordinal=0))
    private void tar$rotation(Args args){if(ClientFeatures.freelooking){args.set(0,ClientFeatures.yaw);args.set(1,ClientFeatures.pitch);}}
    @ModifyArgs(method="update",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/Camera;setRotation(FF)V",ordinal=1))
    private void tar$normalRotation(Args args){if(ClientFeatures.freelooking){args.set(0,ClientFeatures.yaw);args.set(1,ClientFeatures.pitch);}}
}
