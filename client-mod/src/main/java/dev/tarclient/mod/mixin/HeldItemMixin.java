package dev.tarclient.mod.mixin;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.tarclient.mod.TarClient;
@Mixin(HeldItemRenderer.class)
public class HeldItemMixin {
    @Inject(method="renderFirstPersonMap",at=@At("HEAD"))
    private void tar$mapScale(MatrixStack matrices,OrderedRenderCommandQueue queue,int light,ItemStack stack,CallbackInfo ci){
        matrices.push();if(TarClient.CONFIG.on("items")){float scale=(float)TarClient.CONFIG.itemScale("minecraft:filled_map","hand");matrices.scale(scale,scale,scale);}
    }
    @Inject(method="renderFirstPersonMap",at=@At("RETURN"))
    private void tar$mapRestore(MatrixStack matrices,OrderedRenderCommandQueue queue,int light,ItemStack stack,CallbackInfo ci){matrices.pop();}
    @Inject(method="renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",at=@At("HEAD"))
    private void tar$shield(LivingEntity entity,ItemStack stack,ItemDisplayContext context,MatrixStack matrices,OrderedRenderCommandQueue queue,int light,CallbackInfo ci){
        matrices.push();if(TarClient.CONFIG.on("shield")&&stack.isOf(Items.SHIELD)&&context.isFirstPerson())matrices.translate((context.isLeftHand()?-1:1)*TarClient.CONFIG.f("shield","side"),-TarClient.CONFIG.f("shield","down"),0);
    }
    @Inject(method="renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",at=@At("RETURN"))
    private void tar$restore(LivingEntity entity,ItemStack stack,ItemDisplayContext context,MatrixStack matrices,OrderedRenderCommandQueue queue,int light,CallbackInfo ci){matrices.pop();}
}
