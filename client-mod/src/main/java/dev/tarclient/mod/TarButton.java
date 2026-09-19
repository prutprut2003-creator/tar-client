package dev.tarclient.mod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.function.BooleanSupplier;

/** A keyboard-accessible button using Tar's flat panel styling. */
public final class TarButton extends ButtonWidget {
    private final BooleanSupplier selected;
    public TarButton(int x,int y,int w,int h,String text,PressAction action){this(x,y,w,h,text,action,()->false);}
    public TarButton(int x,int y,int w,int h,String text,PressAction action,BooleanSupplier selected){
        super(x,y,w,h,net.minecraft.text.Text.literal(text),action,DEFAULT_NARRATION_SUPPLIER);this.selected=selected;
    }
    @Override protected void drawIcon(DrawContext c,int mx,int my,float delta){
        boolean on=selected.getAsBoolean(),hover=isHovered()||isFocused();
        int bg=!active?0xFF1C222A:on?0xFFB9F47A:hover?0xFF354252:0xFF252E3B;
        rounded(c,getX(),getY(),getWidth(),getHeight(),bg);
        if(isFocused())c.fill(getX()+5,getY()+getHeight()-2,getX()+getWidth()-5,getY()+getHeight()-1,0xFFB9F47A);
        var font=MinecraftClient.getInstance().textRenderer;
        String text=font.trimToWidth(getMessage().getString(),Math.max(1,getWidth()-12));
        c.drawCenteredTextWithShadow(font,text,getX()+getWidth()/2,getY()+(getHeight()-8)/2,!active?0xFF637082:on?0xFF142019:0xFFEAF0F6);
    }
    public static void rounded(DrawContext c,int x,int y,int w,int h,int color){
        c.fill(x+3,y,x+w-3,y+h,color);c.fill(x,y+3,x+w,y+h-3,color);
        c.fill(x+1,y+1,x+w-1,y+h-1,color);
    }
}
