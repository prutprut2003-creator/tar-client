package dev.tarclient.launcher;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.*;

final class ModIcons {
    private static final ConcurrentMap<String,ImageIcon> CACHE=new ConcurrentHashMap<>();
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    static void load(JLabel target,String url){
        if(url==null||url.isBlank())return;
        ImageIcon cached=CACHE.get(url);if(cached!=null){target.setText("");target.setIcon(cached);return;}
        CompletableFuture.runAsync(()->{
            try{
                URI uri=URI.create(url);
                if(!"https".equals(uri.getScheme())||!"cdn.modrinth.com".equals(uri.getHost()))return;
                var response=HTTP.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8)).GET().build(),HttpResponse.BodyHandlers.ofInputStream());
                byte[] data;
                try(var stream=response.body()){if(response.statusCode()!=200)return;data=stream.readNBytes(262145);}
                if(data.length>262144)return;
                try(var stream=new javax.imageio.stream.MemoryCacheImageInputStream(new ByteArrayInputStream(data))){
                    var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())return;var reader=readers.next();
                    try{reader.setInput(stream);if(reader.getWidth(0)>2048||reader.getHeight(0)>2048)return;
                        var icon=new ImageIcon(reader.read(0).getScaledInstance(44,44,Image.SCALE_SMOOTH));
                        if(CACHE.size()<150)CACHE.put(url,icon);
                        SwingUtilities.invokeLater(()->{target.setText("");target.setIcon(icon);});
                    }finally{reader.dispose();}
                }
            }catch(Exception ignored){/* The initial-letter icon remains available offline. */}
        });
    }
}
