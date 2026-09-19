package dev.tarclient.mod;

import dev.tarclient.config.ClientConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import java.util.*;
import static dev.tarclient.mod.TarClient.CONFIG;

public final class TarSettingsScreen extends Screen {
    private final Screen parent;
    private String category="All",selected;
    private int page;
    private final List<String> captions=new ArrayList<>();
    public TarSettingsScreen(Screen parent){super(Text.literal("Tar Client"));this.parent=parent;}
    @Override protected void init(){
        captions.clear();int left=Math.max(14,(width-530)/2),top=65,contentWidth=Math.min(530,width-28);
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"),b->close()).dimensions(width-82,12,70,20).build());
        if(selected!=null){detail(left,top,contentWidth);return;}
        int x=left;for(String cat:List.of("All","HUD","Visual","Window")){addDrawableChild(ButtonWidget.builder(Text.literal(cat.equals(category)?"[ "+cat+" ]":cat),b->{category=cat;page=0;clearAndInit();}).dimensions(x,40,76,20).build());x+=80;}
        addDrawableChild(ButtonWidget.builder(Text.literal("Edit HUD positions"),b->{if(client.player!=null)client.setScreen(new HudEditorScreen(this));}).dimensions(left+contentWidth-150,40,150,20).build());
        List<ClientConfig.Module> modules=ClientConfig.MODULES.stream().filter(m->category.equals("All")||m.category().equals(category)).toList();
        int rows=Math.max(1,(height-top-42)/46),perPage=rows*2;page=Math.clamp(page,0,Math.max(0,(modules.size()-1)/perPage));
        for(int i=page*perPage;i<Math.min(modules.size(),(page+1)*perPage);i++){
            var m=modules.get(i);int local=i-page*perPage,col=local%2,row=local/2,px=left+col*(contentWidth/2+4),py=top+row*46,cw=contentWidth/2-6;
            addDrawableChild(ButtonWidget.builder(Text.literal(m.name()),b->{selected=m.id();page=0;clearAndInit();}).dimensions(px,py,cw-52,34).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(CONFIG.on(m.id())?"ON":"OFF"),b->{CONFIG.set(m.id(),"enabled",!CONFIG.on(m.id()));TarClient.save();b.setMessage(Text.literal(CONFIG.on(m.id())?"ON":"OFF"));}).dimensions(px+cw-49,py,49,34).build());
        }
        pages(left,contentWidth,(modules.size()+perPage-1)/perPage);
    }
    private void detail(int left,int top,int cw){
        var module=ClientConfig.MODULES.stream().filter(m->m.id().equals(selected)).findFirst().orElseThrow();
        addDrawableChild(ButtonWidget.builder(Text.literal("‹ Modules"),b->{selected=null;page=0;clearAndInit();}).dimensions(left,40,100,20).build());
        int perPage=Math.max(1,(height-top-42)/42);page=Math.clamp(page,0,Math.max(0,(module.settings().size()-1)/perPage));
        for(int i=page*perPage;i<Math.min(module.settings().size(),(page+1)*perPage);i++){
            var s=module.settings().get(i);int y=top+(i-page*perPage)*42;captions.add(s.label());
            if(s.initial() instanceof Boolean){addDrawableChild(ButtonWidget.builder(Text.literal(CONFIG.bool(selected,s.key())?"Enabled":"Disabled"),b->{CONFIG.set(selected,s.key(),!CONFIG.bool(selected,s.key()));TarClient.save();b.setMessage(Text.literal(CONFIG.bool(selected,s.key())?"Enabled":"Disabled"));}).dimensions(left+cw-160,y+14,160,20).build());}
            else if(s.initial() instanceof Number){addDrawableChild(new SliderWidget(left+cw-210,y+14,210,20,Text.empty(),(CONFIG.number(selected,s.key())-s.min())/(s.max()-s.min())){
                {updateMessage();}
                protected void updateMessage(){setMessage(Text.literal(String.format(Locale.ROOT,"%.2f",CONFIG.number(selected,s.key()))));}
                protected void applyValue(){double n=Math.round((s.min()+value*(s.max()-s.min()))/s.step())*s.step();CONFIG.set(selected,s.key(),n);updateMessage();TarClient.save();}
            });}
            else {var field=new TextFieldWidget(textRenderer,left+cw-260,y+14,260,20,Text.literal(s.label()));field.setMaxLength(2048);field.setText(CONFIG.text(selected,s.key()));field.setChangedListener(v->{CONFIG.set(selected,s.key(),v);TarClient.save();});addDrawableChild(field);}
        }
        pages(left,cw,(module.settings().size()+perPage-1)/perPage);
    }
    private void pages(int left,int cw,int total){if(total>1){addDrawableChild(ButtonWidget.builder(Text.literal("‹"),b->{page=Math.max(0,page-1);clearAndInit();}).dimensions(left,height-30,40,20).build());addDrawableChild(ButtonWidget.builder(Text.literal("›"),b->{page=Math.min(total-1,page+1);clearAndInit();}).dimensions(left+cw-40,height-30,40,20).build());}}
    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0xF00F1219);c.fill(0,0,4,height,0xFFA5F078);
        c.drawTextWithShadow(textRenderer,"TAR CLIENT",16,17,0xFFA5F078);
        if(selected!=null){var module=ClientConfig.MODULES.stream().filter(m->m.id().equals(selected)).findFirst().orElseThrow();c.drawCenteredTextWithShadow(textRenderer,module.name(),width/2,17,0xFFFFFFFF);int left=Math.max(14,(width-530)/2);for(int i=0;i<captions.size();i++)c.drawTextWithShadow(textRenderer,captions.get(i),left,65+i*42,0xFFB1BDCE);}
        else c.drawCenteredTextWithShadow(textRenderer,"HUD & visual settings",width/2,17,0xFFB1BDCE);
        c.drawCenteredTextWithShadow(textRenderer,"Changes save automatically • Page "+(page+1),width/2,height-23,0xFF8290A5);
        super.render(c,mx,my,delta);
    }
    @Override public void close(){TarClient.save();client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}
