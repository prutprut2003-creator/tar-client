package dev.tarclient.mod.mixin;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.*;
import net.minecraft.world.World;
import net.minecraft.util.HeldItemContext;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.tarclient.mod.ScaledItemState;
import dev.tarclient.mod.TarClient;
@Mixin(ItemModelManager.class)
public class ItemModelMixin {
    @Inject(method="clearAndUpdate",at=@At("TAIL"))
    private void tar$item(ItemRenderState state,ItemStack stack,ItemDisplayContext context,World world,HeldItemContext held,int seed,CallbackInfo ci){
        String id=Registries.ITEM.getId(stack.getItem()).toString();((ScaledItemState)state).tar$setItem(id);
        // GUI items are cached by model key. Include the scale so editing it invalidates the cached image.
        if(context==ItemDisplayContext.GUI)state.addModelKey(TarClient.CONFIG.on("items")?TarClient.CONFIG.itemScale(id,"gui"):1.0);
    }
}
