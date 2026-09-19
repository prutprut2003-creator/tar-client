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
    @Unique private boolean tar$wasMaximized;
    @Inject(method="updateWindowRegion",at=@At("HEAD"),cancellable=true)
    private void tar$window(CallbackInfo ci){
        if(TarClient.CONFIG.on("borderless")&&fullscreen){
            if(!tar$borderless&&GLFW.glfwGetWindowMonitor(handle)==0){windowedX=x;windowedY=y;windowedWidth=width;windowedHeight=height;tar$wasMaximized=GLFW.glfwGetWindowAttrib(handle,GLFW.GLFW_MAXIMIZED)==GLFW.GLFW_TRUE;}
            long monitor=GLFW.glfwGetPrimaryMonitor();var monitors=GLFW.glfwGetMonitors();long best=-1;
            try(var stack=MemoryStack.stackPush()){
                var mx=stack.mallocInt(1);var my=stack.mallocInt(1);
                if(monitors!=null)for(int i=0;i<monitors.limit();i++){long m=monitors.get(i);var mode=GLFW.glfwGetVideoMode(m);if(mode==null)continue;GLFW.glfwGetMonitorPos(m,mx,my);long overlap=(long)Math.max(0,Math.min(x+width,mx.get(0)+mode.width())-Math.max(x,mx.get(0)))*Math.max(0,Math.min(y+height,my.get(0)+mode.height())-Math.max(y,my.get(0)));if(overlap>best){best=overlap;monitor=m;}}
                var mode=GLFW.glfwGetVideoMode(monitor);if(mode==null)return;GLFW.glfwGetMonitorPos(monitor,mx,my);
                // Win32 can synchronously invoke position/size callbacks while changing
                // decorations. Those callbacks overwrite our shadow fields. Keep the
                // requested monitor rectangle immutable until all native calls finish.
                final int targetX=mx.get(0),targetY=my.get(0),targetWidth=mode.width(),targetHeight=mode.height();
                GLFW.glfwRestoreWindow(handle);
                GLFW.glfwSetWindowAttrib(handle,GLFW.GLFW_DECORATED,GLFW.GLFW_FALSE);
                GLFW.glfwSetWindowMonitor(handle,0,targetX,targetY,targetWidth,targetHeight,GLFW.GLFW_DONT_CARE);
                GLFW.glfwSetWindowPos(handle,targetX,targetY);
                GLFW.glfwSetWindowSize(handle,targetWidth,targetHeight);
                x=targetX;y=targetY;width=targetWidth;height=targetHeight;
                tar$borderless=true;ci.cancel();
            }
        }else if(tar$borderless){
            final int restoreX=windowedX,restoreY=windowedY,restoreWidth=windowedWidth,restoreHeight=windowedHeight;
            GLFW.glfwSetWindowAttrib(handle,GLFW.GLFW_DECORATED,GLFW.GLFW_TRUE);
            tar$borderless=false;
            // Also preserve the restore rectangle when switching to exclusive fullscreen.
            x=restoreX;y=restoreY;width=restoreWidth;height=restoreHeight;
            if(!fullscreen){
                GLFW.glfwSetWindowMonitor(handle,0,restoreX,restoreY,restoreWidth,restoreHeight,GLFW.GLFW_DONT_CARE);
                if(tar$wasMaximized)GLFW.glfwMaximizeWindow(handle);
                ci.cancel();
            }
        }
    }
}
