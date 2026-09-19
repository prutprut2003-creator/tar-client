package dev.tarclient.mod.mixin;
import net.minecraft.client.render.debug.*;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static dev.tarclient.mod.TarClient.CONFIG;
@Mixin(DebugRenderer.class)
public class DebugRendererMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void tar$always(Frustum frustum,double x,double y,double z,float tick,CallbackInfo ci){
        var c=MinecraftClient.getInstance();if(CONFIG.on("hitboxes")&&CONFIG.bool("hitboxes","always")&&c.world!=null&&c.getNetworkHandler()!=null&&!c.debugHudEntryList.isEntryVisible(DebugHudEntries.ENTITY_HITBOXES))new EntityHitboxDebugRenderer(c).render(x,y,z,c.getNetworkHandler().getDebugDataStore(),frustum,tick);
    }
}
