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
    private String category="All",selected,query="";
    private int page,left,top,panelWidth,panelHeight,mainX,mainWidth,contentY;
    private final List<Caption> captions=new ArrayList<>();
    private final List<Tile> tiles=new ArrayList<>();
    private record Caption(String text,int x,int y,int width) {}
    private record Tile(ClientConfig.Module module,int x,int y,int width) {}
    public TarSettingsScreen(Screen parent){super(Text.literal("Tar Client"));this.parent=parent;}
    @Override protected void init(){
        captions.clear();tiles.clear();
        panelWidth=Math.min(740,width-20);panelHeight=Math.min(440,height-20);
        left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        int side=panelWidth<480?88:110;
        mainX=left+side+14;mainWidth=panelWidth-side-28;contentY=top+78;
        addDrawableChild(new TarButton(left+panelWidth-36,top+12,24,22,"X",b->close()));
        int navY=top+49;
        for(String cat:List.of("All","HUD","Visual","Window")){
            addDrawableChild(new TarButton(left+10,navY,side-18,25,cat.equals("All")?"All modules":cat,b->{selected=null;category=cat;page=0;clearAndInit();},()->selected==null&&category.equals(cat)));
            navY+=26;
        }
        TarButton edit=new TarButton(left+10,top+panelHeight-65,side-18,25,"Edit HUD",b->client.setScreen(new HudEditorScreen(this)));
        edit.active=client.player!=null;addDrawableChild(edit);
        if(selected!=null){detail();return;}
        var search=new TextFieldWidget(textRenderer,mainX,top+44,mainWidth,22,Text.literal("Search modules"));
        search.setMaxLength(60);search.setText(query);search.setPlaceholder(Text.literal("Search modules..."));
        search.setChangedListener(value->{query=value;page=0;clearAndInit();});addDrawableChild(search);
        if(!query.isEmpty())setFocused(search);
        var modules=ClientConfig.MODULES.stream().filter(m->category.equals("All")||m.category().equals(category))
            .filter(m->(m.name()+" "+m.description()).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList();
        int cols=mainWidth>=270?2:1,rows=Math.max(1,(panelHeight-114)/67),perPage=rows*cols;
        page=Math.clamp(page,0,Math.max(0,(modules.size()-1)/perPage));
        int cw=(mainWidth-(cols-1)*8)/cols;
        for(int i=page*perPage;i<Math.min(modules.size(),(page+1)*perPage);i++){
            var m=modules.get(i);int local=i-page*perPage,px=mainX+(local%cols)*(cw+8),py=contentY+(local/cols)*67;
            tiles.add(new Tile(m,px,py,cw));
            addDrawableChild(new TarButton(px+8,py+34,cw-65,21,"Settings",b->{selected=m.id();page=0;clearAndInit();}));
            addDrawableChild(new TarButton(px+cw-51,py+34,43,21,CONFIG.on(m.id())?"ON":"OFF",b->{CONFIG.set(m.id(),"enabled",!CONFIG.on(m.id()));TarClient.save();b.setMessage(Text.literal(CONFIG.on(m.id())?"ON":"OFF"));},()->CONFIG.on(m.id())));
        }
        if(modules.isEmpty())captions.add(new Caption("No matching modules",mainX,contentY+20,mainWidth));
        pages((modules.size()+perPage-1)/perPage);
    }
    private void detail(){
        var module=ClientConfig.MODULES.stream().filter(m->m.id().equals(selected)).findFirst().orElseThrow();
        addDrawableChild(new TarButton(mainX,top+44,90,22,"< Modules",b->{selected=null;page=0;clearAndInit();}));
        int perPage=Math.max(1,(panelHeight-110)/42);
        page=Math.clamp(page,0,Math.max(0,(module.settings().size()-1)/perPage));
        for(int i=page*perPage;i<Math.min(module.settings().size(),(page+1)*perPage);i++){
            var s=module.settings().get(i);int y=contentY+(i-page*perPage)*42;
            captions.add(new Caption(s.label(),mainX,y,mainWidth));
            if(s.initial() instanceof Boolean){
                addDrawableChild(new TarButton(mainX,y+13,mainWidth,22,CONFIG.bool(selected,s.key())?"Enabled":"Disabled",b->{CONFIG.set(selected,s.key(),!CONFIG.bool(selected,s.key()));TarClient.save();b.setMessage(Text.literal(CONFIG.bool(selected,s.key())?"Enabled":"Disabled"));},()->CONFIG.bool(selected,s.key())));
            }else if(s.initial() instanceof Number){
                addDrawableChild(new SliderWidget(mainX,y+13,mainWidth,22,Text.empty(),(CONFIG.number(selected,s.key())-s.min())/(s.max()-s.min())){
                    {updateMessage();}
                    protected void updateMessage(){setMessage(Text.literal(String.format(Locale.ROOT,"%.2f",CONFIG.number(selected,s.key()))));}
                    protected void applyValue(){double n=Math.round((s.min()+value*(s.max()-s.min()))/s.step())*s.step();CONFIG.set(selected,s.key(),n);updateMessage();TarClient.save();}
                });
            }else{
                var field=new TextFieldWidget(textRenderer,mainX,y+13,mainWidth,22,Text.literal(s.label()));
                field.setMaxLength(2048);field.setText(CONFIG.text(selected,s.key()));field.setChangedListener(v->{CONFIG.set(selected,s.key(),v);TarClient.save();});addDrawableChild(field);
            }
        }
        pages((module.settings().size()+perPage-1)/perPage);
    }
    private void pages(int total){
        if(total>1){
            TarButton back=new TarButton(mainX,top+panelHeight-27,30,20,"<",b->{page--;clearAndInit();});back.active=page>0;addDrawableChild(back);
            TarButton next=new TarButton(mainX+mainWidth-30,top+panelHeight-27,30,20,">",b->{page++;clearAndInit();});next.active=page<total-1;addDrawableChild(next);
        }
    }
    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0xAD080C12);
        TarButton.rounded(c,left,top,panelWidth,panelHeight,0xFA101720);
        c.fill(mainX-8,top+12,mainX-7,top+panelHeight-12,0xFF293341);
        c.drawTextWithShadow(textRenderer,"TAR",left+13,top+17,0xFFB9F47A);
        c.drawTextWithShadow(textRenderer,"CLIENT",left+13,top+30,0xFF8D9BAC);
        String title=selected==null?"Your client. Your way.":ClientConfig.MODULES.stream().filter(m->m.id().equals(selected)).findFirst().orElseThrow().name();
        c.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(title,Math.max(1,mainWidth-36)),mainX,top+20,0xFFF0F5FA);
        for(var tile:tiles){
            TarButton.rounded(c,tile.x(),tile.y(),tile.width(),61,0xFF1A2430);
            c.fill(tile.x()+8,tile.y()+10,tile.x()+11,tile.y()+24,CONFIG.on(tile.module().id())?0xFFB9F47A:0xFF516074);
            c.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(tile.module().name(),Math.max(1,tile.width()-28)),tile.x()+18,tile.y()+8,0xFFEAF0F6);
            c.drawTextWithShadow(textRenderer,tile.module().category(),tile.x()+18,tile.y()+20,0xFF8190A2);
        }
        for(var caption:captions)c.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(caption.text(),caption.width()),caption.x(),caption.y(),0xFF9DADBF);
        c.drawCenteredTextWithShadow(textRenderer,"Saved automatically  /  "+(page+1),mainX+mainWidth/2,top+panelHeight-21,0xFF8190A2);
        c.drawTextWithShadow(textRenderer,"RSHIFT / ESC",left+11,top+panelHeight-21,0xFF8190A2);
        super.render(c,mx,my,delta);
    }
    @Override public void close(){TarClient.save();client.setScreen(parent);}
    @Override public boolean shouldPause(){return false;}
}
