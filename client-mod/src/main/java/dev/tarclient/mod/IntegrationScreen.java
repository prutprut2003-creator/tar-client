package dev.tarclient.mod;
import dev.tarclient.config.Integrations;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
public final class IntegrationScreen extends Screen {
    private final Screen parent;private final Integrations.Entry entry;private String message;
    public IntegrationScreen(Screen parent,Integrations.Entry entry){super(Text.literal("Module integration"));this.parent=parent;this.entry=entry;message=FabricLoader.getInstance().isModLoaded(entry.modId())?"Installed. Use Mod settings to customize this module.":"Enable this module, then close and relaunch Minecraft from Tar.";}
    protected void init(){
        int x=width/2-130;
        addDrawableChild(new TarButton(x,80,260,24,TarClient.CONFIG.on(entry.module())?"Enabled for next launch":"Disabled for next launch",b->{TarClient.CONFIG.set(entry.module(),"enabled",!TarClient.CONFIG.on(entry.module()));TarClient.save();clearAndInit();}));
        var settings=new TarButton(x,112,260,24,"Mod settings",b->{try{
            var cls=Class.forName("com.terraformersmc.modmenu.ModMenu");
            Screen screen=(Screen)cls.getMethod("getConfigScreen",String.class,Screen.class).invoke(null,entry.modId(),this);
            if(screen==null)screen=(Screen)Class.forName("com.terraformersmc.modmenu.gui.ModsScreen").getConstructor(Screen.class).newInstance(this);
            client.setScreen(screen);
        }catch(Exception e){message="Use the Mods button to configure "+entry.modId()+". Resource Tree is configured in Resource Packs.";}});
        settings.active=FabricLoader.getInstance().isModLoaded(entry.modId());addDrawableChild(settings);
        if(entry.module().equals("packorganizer"))addDrawableChild(new TarButton(x,144,260,24,"Resource Packs",b->client.setScreen(new net.minecraft.client.gui.screen.pack.PackScreen(client.getResourcePackManager(),manager->{client.options.refreshResourcePacks(manager);client.setScreen(this);},client.getResourcePackDir(),Text.translatable("resourcePack.title")))));
        addDrawableChild(new TarButton(x,184,260,24,"Back",b->close()));
    }
    public void render(DrawContext c,int x,int y,float delta){c.fill(0,0,width,height,0xEF101720);String title=dev.tarclient.config.ClientConfig.MODULES.stream().filter(m->m.id().equals(entry.module())).findFirst().orElseThrow().name();c.drawCenteredTextWithShadow(textRenderer,title,width/2,26,0xFFA5F078);c.drawCenteredTextWithShadow(textRenderer,textRenderer.trimToWidth(message,width-20),width/2,51,0xFFE8EDF5);super.render(c,x,y,delta);}
    public void close(){client.setScreen(parent);}
    public boolean shouldPause(){return false;}
}
