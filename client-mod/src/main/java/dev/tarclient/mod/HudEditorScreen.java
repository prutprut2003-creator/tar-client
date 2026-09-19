package dev.tarclient.mod;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import static dev.tarclient.mod.TarClient.CONFIG;

public final class HudEditorScreen extends Screen {
    private final Screen parent;private String dragged;private double dx,dy;
    public HudEditorScreen(Screen parent){super(Text.literal("Edit Tar HUD"));this.parent=parent;}
    @Override protected void init(){addDrawableChild(ButtonWidget.builder(Text.literal("Done"),b->close()).dimensions(width/2-45,12,90,20).build());}
    @Override public void render(DrawContext c,int mx,int my,float delta){TarHud.render(c,true);c.drawCenteredTextWithShadow(textRenderer,"Drag HUD panels • Escape to save",width/2,38,0xFFA5F078);super.render(c,mx,my,delta);}
    @Override public boolean mouseClicked(Click click,boolean doubled){if(super.mouseClicked(click,doubled))return true;if(click.button()==0)for(var e:TarHud.BOXES.entrySet())if(e.getValue().contains(click.x(),click.y())){dragged=e.getKey();dx=click.x()-e.getValue().x();dy=click.y()-e.getValue().y();return true;}return false;}
    @Override public boolean mouseDragged(Click click,double deltaX,double deltaY){if(dragged!=null){var box=TarHud.BOXES.get(dragged);if(box!=null){CONFIG.set(dragged,"x",100*(click.x()-dx)/Math.max(1,width-box.w()));CONFIG.set(dragged,"y",100*(click.y()-dy)/Math.max(1,height-box.h()));}return true;}return super.mouseDragged(click,deltaX,deltaY);}
    @Override public boolean mouseReleased(Click click){if(dragged!=null){dragged=null;TarClient.save();return true;}return super.mouseReleased(click);}
    @Override public void close(){TarClient.save();client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}
