package dev.tarclient.mod.mixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static dev.tarclient.mod.TarClient.CONFIG;
@Mixin(GameMenuScreen.class)
public abstract class DisconnectMixin {
    @Unique private boolean tar$confirmed;
    @Shadow private void method_19836(ButtonWidget button){throw new AssertionError();}
    @Inject(method="method_19836",at=@At("HEAD"),cancellable=true)
    private void tar$confirm(ButtonWidget button,CallbackInfo ci){
        if(!CONFIG.on("disconnect")||tar$confirmed)return;
        var mc=MinecraftClient.getInstance();var parent=(Screen)(Object)this;
        mc.setScreen(new ConfirmScreen(yes->{mc.setScreen(parent);if(yes){tar$confirmed=true;try{method_19836(button);}finally{tar$confirmed=false;}}},Text.literal("Are you sure you want to leave?"),Text.literal("Choose Cancel to stay in your game."),Text.literal("Leave"),Text.literal("Cancel")));
        ci.cancel();
    }
}
