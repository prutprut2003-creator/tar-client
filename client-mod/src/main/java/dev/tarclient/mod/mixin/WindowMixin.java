package dev.tarclient.mod.mixin;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.tarclient.mod.TarClient;
@Mixin(Window.class)
public class WindowMixin {
    @Shadow @Final private long handle;
    @Shadow private boolean fullscreen;
    @Shadow private int x,y,width,height,windowedX,windowedY,windowedWidth,windowedHeight;
    @Unique private boolean tar$borderless;
    @Inject(method="updateWindowRegion",at=@At("HEAD"),cancellable=true)
    private void tar$window(CallbackInfo ci){
        if(TarClient.CONFIG.on("borderless")&&fullscreen){
            if(!tar$borderless&&GLFW.glfwGetWindowMonitor(handle)==0){windowedX=x;windowedY=y;windowedWidth=width;windowedHeight=height;}
            long monitor=GLFW.glfwGetPrimaryMonitor();var monitors=GLFW.glfwGetMonitors();long best=-1;
            try(var stack=MemoryStack.stackPush()){
                var mx=stack.mallocInt(1);var my=stack.mallocInt(1);
                if(monitors!=null)for(int i=0;i<monitors.limit();i++){long m=monitors.get(i);var mode=GLFW.glfwGetVideoMode(m);if(mode==null)continue;GLFW.glfwGetMonitorPos(m,mx,my);long overlap=(long)Math.max(0,Math.min(x+width,mx.get(0)+mode.width())-Math.max(x,mx.get(0)))*Math.max(0,Math.min(y+height,my.get(0)+mode.height())-Math.max(y,my.get(0)));if(overlap>best){best=overlap;monitor=m;}}
                var mode=GLFW.glfwGetVideoMode(monitor);if(mode==null)return;GLFW.glfwGetMonitorPos(monitor,mx,my);x=mx.get(0);y=my.get(0);width=mode.width();height=mode.height();
                GLFW.glfwSetWindowAttrib(handle,GLFW.GLFW_DECORATED,GLFW.GLFW_FALSE);GLFW.glfwSetWindowMonitor(handle,0,x,y,width,height,GLFW.GLFW_DONT_CARE);tar$borderless=true;ci.cancel();
            }
        }else if(tar$borderless){GLFW.glfwSetWindowAttrib(handle,GLFW.GLFW_DECORATED,GLFW.GLFW_TRUE);tar$borderless=false;}
    }
}
