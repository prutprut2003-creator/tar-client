package dev.tarclient.mod;
import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
/** Reads only the local Spotify media session; no remote login, playback or microphone access. */
public final class SpotifyMedia {
    public record Track(boolean available,String title,String artist,boolean playing,byte[] artwork,String status) {}
    public static volatile Track current=new Track(false,"","",false,new byte[0],"Open Spotify and play a song");
    private static volatile boolean busy;
    private static long nextPoll;
    private static volatile Process process;
    public static void tick(boolean enabled){
        if(!enabled){if(process!=null)process.destroy();return;}
        if(busy||System.currentTimeMillis()<nextPoll)return;
        nextPoll=System.currentTimeMillis()+5000;busy=true;
        Thread.ofVirtual().name("Tar Spotify media").start(()->{try{
            if(!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows")){current=new Track(false,"","",false,new byte[0],"Spotify overlay requires Windows");return;}
            String script;try(var in=SpotifyMedia.class.getResourceAsStream("/tar-spotify.ps1")){if(in==null)throw new IllegalStateException("Media reader missing");script=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
            var p=new ProcessBuilder("powershell.exe","-NoLogo","-NoProfile","-NonInteractive","-WindowStyle","Hidden","-Command",script).redirectError(ProcessBuilder.Redirect.DISCARD).start();process=p;
            var output=new FutureTask<byte[]>(()->p.getInputStream().readNBytes(1_600_001));Thread.ofVirtual().start(output);
            if(!p.waitFor(12,TimeUnit.SECONDS)){p.destroyForcibly();throw new TimeoutException();}
            byte[] bytes=output.get(1,TimeUnit.SECONDS);if(bytes.length>1_600_000)throw new IllegalStateException("Oversized media response");
            var json=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8).strip()).getAsJsonObject();
            boolean available=json.get("available").getAsBoolean();
            current=available?new Track(true,text(json,"title"),text(json,"artist"),json.get("playing").getAsBoolean(),Base64.getDecoder().decode(text(json,"artwork")),""):new Track(false,"","",false,new byte[0],text(json,"status"));
        }catch(Exception e){current=new Track(false,"","",false,new byte[0],"Spotify media unavailable");}finally{if(process!=null)process.destroy();process=null;busy=false;}});
    }
    private static String text(JsonObject o,String key){return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():"";}
}
