package dev.tarclient.mod;
import dev.tarclient.config.ProfileStore;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import java.util.List;
public final class ProfilesScreen extends Screen {
    private final Screen parent;
    private final ProfileStore store=new ProfileStore(FabricLoader.getInstance().getConfigDir());
    private String status="Save your current modules or load a different setup.";
    private int page;
    public ProfilesScreen(Screen parent){super(Text.literal("Tar profiles"));this.parent=parent;}
    protected void init(){
        int x=width/2-150;
        var name=new TextFieldWidget(textRenderer,x,48,220,20,Text.literal("Profile name"));name.setMaxLength(48);name.setPlaceholder(Text.literal("Profile name"));addDrawableChild(name);
        addDrawableChild(new TarButton(x+226,48,74,20,"Save",b->{try{
            String value=name.getText().trim();
            if(store.list().contains(value)){client.setScreen(new ConfirmScreen(yes->{client.setScreen(this);if(yes)save(value);},Text.literal("Replace profile?"),Text.literal(value)));}else save(value);
        }catch(Exception e){status=e.getMessage();}}));
        try{store.presets();List<String> profiles=store.list();int count=Math.max(1,(height-150)/26);page=Math.clamp(page,0,Math.max(0,(profiles.size()-1)/count));
            for(int i=page*count;i<Math.min(profiles.size(),(page+1)*count);i++){String profile=profiles.get(i);addDrawableChild(new TarButton(x,82+(i-page*count)*26,300,22,"Load  "+profile,b->{try{TarClient.CONFIG=store.load(profile);TarClient.save();status="Loaded "+profile+". Integration changes need a game restart.";}catch(Exception e){status=e.getMessage();}}));}
            if(page>0)addDrawableChild(new TarButton(x,height-60,45,20,"<",b->{page--;clearAndInit();}));
            if((page+1)*count<profiles.size())addDrawableChild(new TarButton(x+255,height-60,45,20,">",b->{page++;clearAndInit();}));
        }catch(Exception e){status=e.getMessage();}
        addDrawableChild(new TarButton(width/2-45,height-32,90,20,"Done",b->close()));
    }
    private void save(String name){try{store.save(name,TarClient.CONFIG);status="Saved "+name;clearAndInit();}catch(Exception e){status=e.getMessage();}}
    public void render(DrawContext c,int x,int y,float delta){c.fill(0,0,width,height,0xEF101720);c.drawCenteredTextWithShadow(textRenderer,"PROFILES",width/2,22,0xFFA5F078);c.drawCenteredTextWithShadow(textRenderer,textRenderer.trimToWidth(status,width-20),width/2,height-82,0xFFE8EDF5);super.render(c,x,y,delta);}
    public void close(){client.setScreen(parent);}
    public boolean shouldPause(){return false;}
}
