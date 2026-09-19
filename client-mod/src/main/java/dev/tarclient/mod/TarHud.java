package dev.tarclient.mod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import static dev.tarclient.mod.TarClient.CONFIG;

public final class TarHud {
    public static final Map<String,Box> BOXES=new LinkedHashMap<>();
    public record Box(int x,int y,int w,int h) {public boolean contains(double px,double py){return px>=x&&py>=y&&px<=x+w&&py<=y+h;}}
    public static void render(DrawContext c,boolean editing) {
        var mc=MinecraftClient.getInstance();if(mc.player==null||mc.options.hudHidden&&!editing)return;
        BOXES.clear();
        if(CONFIG.on("fps")) textPanel(c,"fps",List.of(mc.getCurrentFps()+" FPS"));
        if(CONFIG.on("ping")) {
            var entry=mc.getNetworkHandler()==null?null:mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            String text=mc.isInSingleplayer()?"Singleplayer":entry==null?"Ping unavailable":entry.getLatency()+" ms";
            textPanel(c,"ping",List.of(text));
        }
        if(CONFIG.on("armor")) armor(c);
        if(CONFIG.on("potions")) {
            List<String> effects=new ArrayList<>();
            for(var effect:mc.player.getStatusEffects()) {
                boolean good=effect.getEffectType().value().isBeneficial();if(good&&!CONFIG.bool("potions","beneficial")||!good&&!CONFIG.bool("potions","harmful"))continue;
                int secs=Math.max(0,effect.getDuration()/20);String time=effect.isInfinite()?"∞":String.format(Locale.ROOT,"%d:%02d",secs/60,secs%60);
                effects.add(effect.getEffectType().value().getName().getString()+" "+(effect.getAmplifier()+1)+"  "+time);
            }
            if(!effects.isEmpty()||editing)textPanel(c,"potions",effects.isEmpty()?List.of("No active effects"):effects);
        }
        if(CONFIG.on("keys")) keys(c);
        if(editing)for(var e:BOXES.entrySet()){var b=e.getValue();c.drawStrokedRectangle(b.x-1,b.y-1,b.w+2,b.h+2,0xFFA5F078);}
    }
    private static void begin(DrawContext c,String id,int w,int h) {
        float scale=CONFIG.f(id,"scale");int width=Math.round(w*scale),height=Math.round(h*scale);
        int x=Math.round((c.getScaledWindowWidth()-width)*CONFIG.f(id,"x")/100),y=Math.round((c.getScaledWindowHeight()-height)*CONFIG.f(id,"y")/100);
        x=Math.max(0,x);y=Math.max(0,y);BOXES.put(id,new Box(x,y,width,height));
        c.getMatrices().pushMatrix();c.getMatrices().translate(x,y);c.getMatrices().scale(scale,scale);c.fill(0,0,w,h,CONFIG.color(id,"background"));
    }
    private static void end(DrawContext c){c.getMatrices().popMatrix();}
    private static void textPanel(DrawContext c,String id,List<String> lines) {
        var font=MinecraftClient.getInstance().textRenderer;int w=lines.stream().mapToInt(font::getWidth).max().orElse(60)+12;
        begin(c,id,w,lines.size()*13+8);int y=5;for(String s:lines){c.drawTextWithShadow(font,s,6,y,CONFIG.color(id,"color"));y+=13;}end(c);
    }
    private static void armor(DrawContext c) {
        var mc=MinecraftClient.getInstance();begin(c,"armor",110,84);int y=2;
        for(var slot:TarClient.ARMOR) {
            var stack=mc.player.getEquippedStack(slot);
            if(!stack.isEmpty()){c.drawItem(stack,3,y);String s="—";int color=CONFIG.color("armor","color");if(stack.isDamageable()){int remaining=Math.max(0,stack.getMaxDamage()-stack.getDamage());double pct=remaining*100.0/stack.getMaxDamage();s=CONFIG.bool("armor","percent")?Math.round(pct)+"%":remaining+" / "+stack.getMaxDamage();if(pct<=CONFIG.number("armor","threshold"))color=0xFFFF7878;}c.drawTextWithShadow(mc.textRenderer,s,24,y+4,color);}
            else c.drawTextWithShadow(mc.textRenderer,slot.getName()+"  —",5,y+4,0xFF778397);
            y+=20;
        }end(c);
    }
    private static void keys(DrawContext c) {
        var mc=MinecraftClient.getInstance();boolean mouse=CONFIG.bool("keys","mouse");begin(c,"keys",86,mouse?106:76);
        key(c,mc.options.forwardKey,30,3,26);key(c,mc.options.leftKey,2,28,26);key(c,mc.options.backKey,30,28,26);key(c,mc.options.rightKey,58,28,26);key(c,mc.options.jumpKey,2,53,82);
        if(mouse){long h=mc.getWindow().getHandle();mouse(c,2,78,"LMB",GLFW.glfwGetMouseButton(h,0)==GLFW.GLFW_PRESS,TarClient.LEFT_CLICKS.size());mouse(c,44,78,"RMB",GLFW.glfwGetMouseButton(h,1)==GLFW.GLFW_PRESS,TarClient.RIGHT_CLICKS.size());}end(c);
    }
    private static void key(DrawContext c,KeyBinding key,int x,int y,int width) {String name=key.getBoundKeyLocalizedText().getString();if(name.length()>9)name=name.substring(0,9);cell(c,x,y,width,21,name,key.isPressed());}
    private static void mouse(DrawContext c,int x,int y,String label,boolean pressed,int cps){cell(c,x,y,40,25,label,pressed);if(CONFIG.bool("keys","cps"))c.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,cps+" CPS",x+4,y+14,CONFIG.color("keys","color"));}
    private static void cell(DrawContext c,int x,int y,int w,int h,String s,boolean pressed){c.fill(x,y,x+w,y+h,pressed?CONFIG.color("keys","pressed"):0xBB2B3342);var font=MinecraftClient.getInstance().textRenderer;c.drawTextWithShadow(font,s,x+(w-font.getWidth(s))/2,y+4,pressed?0xFF111820:CONFIG.color("keys","color"));}
    public static void crosshair(DrawContext c) {
        var mc=MinecraftClient.getInstance();if(mc.player==null||mc.player.isSpectator()||!mc.options.getPerspective().isFirstPerson()&&!CONFIG.bool("crosshair","thirdPerson"))return;
        int x=c.getScaledWindowWidth()/2,y=c.getScaledWindowHeight()/2,s=CONFIG.i("crosshair","size"),g=CONFIG.i("crosshair","gap"),t=CONFIG.i("crosshair","thickness"),o=t/2,color=CONFIG.color("crosshair","color");
        int[][] rects={{x-g-s,y-o,x-g,y-o+t},{x+g+1,y-o,x+g+s+1,y-o+t},{x-o,y-g-s,x-o+t,y-g},{x-o,y+g+1,x-o+t,y+g+s+1}};
        if(CONFIG.bool("crosshair","outline"))for(var r:rects)c.fill(r[0]-1,r[1]-1,r[2]+1,r[3]+1,0xFF000000);
        for(var r:rects)c.fill(r[0],r[1],r[2],r[3],color);
        if(CONFIG.bool("crosshair","dot")){if(CONFIG.bool("crosshair","outline"))c.fill(x-o-1,y-o-1,x-o+t+1,y-o+t+1,0xFF000000);c.fill(x-o,y-o,x-o+t,y-o+t,color);}
    }
}
