package dev.tarclient.mod.mixin;
import net.minecraft.client.render.debug.EntityHitboxDebugRenderer;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.dragon.*;
import net.minecraft.util.math.*;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static dev.tarclient.mod.TarClient.CONFIG;
@Mixin(EntityHitboxDebugRenderer.class)
public class HitboxMixin {
    @Inject(method="drawHitbox",at=@At("HEAD"),cancellable=true)
    private void tar$hitbox(Entity entity,float tick,boolean server,CallbackInfo ci){
        if(!CONFIG.on("hitboxes"))return;
        Vec3d pos=entity.getLerpedPos(tick),offset=pos.subtract(entity.getEntityPos());Box box=entity.getBoundingBox().offset(offset);int color=CONFIG.color("hitboxes","color");
        GizmoDrawing.box(box,DrawStyle.stroked(color));
        if(CONFIG.bool("hitboxes","eyeLine")&&entity instanceof LivingEntity)GizmoDrawing.box(new Box(box.minX,box.minY+entity.getStandingEyeHeight()-0.01,box.minZ,box.maxX,box.minY+entity.getStandingEyeHeight()+0.01,box.maxZ),DrawStyle.stroked(0xFFFF7070));
        if(CONFIG.bool("hitboxes","direction")){var eye=pos.add(0,entity.getStandingEyeHeight(),0);GizmoDrawing.arrow(eye,eye.add(entity.getRotationVec(tick).multiply(2)),0xFF73B9FF);}
        if(entity instanceof EnderDragonEntity dragon)for(EnderDragonPart part:dragon.getBodyParts())GizmoDrawing.box(part.getBoundingBox().offset(part.getLerpedPos(tick).subtract(part.getEntityPos())),DrawStyle.stroked(color));
        ci.cancel();
    }
}
