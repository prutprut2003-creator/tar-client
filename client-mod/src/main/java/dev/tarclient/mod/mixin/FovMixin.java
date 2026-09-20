package dev.tarclient.mod.mixin;
import net.minecraft.client.render.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.tarclient.mod.ClientFeatures;
@Mixin(GameRenderer.class)
public class FovMixin {
    @Inject(method="getFov",at=@At("RETURN"),cancellable=true)
    private void tar$zoom(Camera camera,float delta,boolean changing,CallbackInfoReturnable<Float> cir){if(changing)cir.setReturnValue(ClientFeatures.fov(cir.getReturnValue()));}
}
