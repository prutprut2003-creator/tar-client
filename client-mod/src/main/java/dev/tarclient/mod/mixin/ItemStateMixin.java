package dev.tarclient.mod.mixin;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.tarclient.mod.*;
@Mixin(ItemRenderState.class)
public class ItemStateMixin implements ScaledItemState {
    @Shadow ItemDisplayContext displayContext;
    @Unique private String tar$id="minecraft:air";
    public void tar$setItem(String id){tar$id=id;}
    @Inject(method="render",at=@At("HEAD"))
    private void tar$scale(MatrixStack matrices,OrderedRenderCommandQueue queue,int light,int overlay,int outline,CallbackInfo ci){
        matrices.push();if(!TarClient.CONFIG.on("items")||displayContext==null)return;
        String context=switch(displayContext){case GUI->"gui";case GROUND->"ground";case FIRST_PERSON_LEFT_HAND,FIRST_PERSON_RIGHT_HAND->"hand";case THIRD_PERSON_LEFT_HAND,THIRD_PERSON_RIGHT_HAND,HEAD->"thirdPerson";default->"fixed";};
        float scale=(float)TarClient.CONFIG.itemScale(tar$id,context);matrices.scale(scale,scale,scale);
    }
    @Inject(method="render",at=@At("RETURN"))
    private void tar$restore(MatrixStack matrices,OrderedRenderCommandQueue queue,int light,int overlay,int outline,CallbackInfo ci){matrices.pop();}
}
