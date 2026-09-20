package dev.tarclient.mod.mixin;
import java.util.Optional;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.tarclient.mod.ShulkerPreview;
import static dev.tarclient.mod.TarClient.CONFIG;
@Mixin(ItemStack.class)
public class ShulkerMixin {
    @Inject(method="getTooltipData",at=@At("RETURN"),cancellable=true)
    private void tar$preview(CallbackInfoReturnable<Optional<TooltipData>> cir){
        if(!CONFIG.on("shulker"))return;
        long window=MinecraftClient.getInstance().getWindow().getHandle();
        if(CONFIG.bool("shulker","shift")&&GLFW.glfwGetKey(window,GLFW.GLFW_KEY_LEFT_SHIFT)!=GLFW.GLFW_PRESS&&GLFW.glfwGetKey(window,GLFW.GLFW_KEY_RIGHT_SHIFT)!=GLFW.GLFW_PRESS)return;
        var stack=(ItemStack)(Object)this;
        if(stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof ShulkerBoxBlock){var contents=stack.get(DataComponentTypes.CONTAINER);if(contents!=null)cir.setReturnValue(Optional.of(new ShulkerPreview(contents.stream().limit(27).toList())));}
    }
}
