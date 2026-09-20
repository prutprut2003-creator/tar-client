package dev.tarclient.mod;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import static dev.tarclient.mod.TarClient.CONFIG;
public record ShulkerPreview(List<ItemStack> stacks) implements TooltipData {
    public static final class Component implements TooltipComponent {
        private final ShulkerPreview data;
        public Component(ShulkerPreview data){this.data=data;}
        public int getWidth(TextRenderer font){return 166;}
        public int getHeight(TextRenderer font){return 58;}
        public void drawItems(TextRenderer font,int x,int y,int width,int height,DrawContext c){
            if(CONFIG.bool("shulker","showBackground"))c.fill(x,y,x+166,y+56,CONFIG.color("shulker","background"));
            for(int i=0;i<Math.min(27,data.stacks.size());i++){var stack=data.stacks.get(i);int px=x+2+i%9*18,py=y+2+i/9*18;c.drawItem(stack,px,py);c.drawStackOverlay(font,stack,px,py);}
        }
    }
}
