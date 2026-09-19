package dev.tarclient.launcher;

import com.google.gson.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.io.*;
import java.util.*;

public final class Net {
    public static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(25)).followRedirects(HttpClient.Redirect.NORMAL).build();
    public static HttpRequest.Builder request(String url) {
        URI uri=URI.create(url); if(!"https".equals(uri.getScheme())) throw new IllegalArgumentException("HTTPS required");
        return HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(3)).header("User-Agent","TarClient/0.2.0 (personal Minecraft launcher)");
    }
    public static JsonObject object(String url) throws Exception { return json(url).getAsJsonObject(); }
    public static JsonElement json(String url) throws Exception { return send(request(url).GET().build()); }
    public static JsonElement send(HttpRequest request) throws Exception {
        var r=HTTP.send(request,HttpResponse.BodyHandlers.ofString());
        if(r.statusCode()/100!=2) throw new IOException("Service returned HTTP "+r.statusCode()+" at "+request.uri().getHost());
        return JsonParser.parseString(r.body());
    }
    public static JsonObject post(String url,JsonObject body) throws Exception {
        return send(request(url).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(JSON.toJson(body))).build()).getAsJsonObject();
    }
    public static JsonObject form(String url,Map<String,String> values) throws Exception {
        StringJoiner form=new StringJoiner("&"); values.forEach((k,v)->form.add(enc(k)+"="+enc(v)));
        var r=HTTP.send(request(url).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form.toString())).build(),HttpResponse.BodyHandlers.ofString());
        JsonObject obj;
        try { obj=JsonParser.parseString(r.body()).getAsJsonObject(); } catch(Exception e) { throw new IOException("Sign-in service returned HTTP "+r.statusCode()); }
        if(r.statusCode()/100!=2 && !obj.has("error")) throw new IOException("Sign-in service returned HTTP "+r.statusCode());
        return obj;
    }
    public static String enc(String value) { return URLEncoder.encode(value,java.nio.charset.StandardCharsets.UTF_8); }
    public static String hash(Path file,String algorithm) throws Exception {
        var digest=MessageDigest.getInstance(algorithm);
        try(var in=Files.newInputStream(file)) { byte[] buffer=new byte[65536]; int n; while((n=in.read(buffer))!=-1) digest.update(buffer,0,n); }
        return HexFormat.of().formatHex(digest.digest());
    }
    public static void download(String url,Path target,String algorithm,String expected) throws Exception {
        if(Files.isRegularFile(target) && expected!=null && hash(target,algorithm).equalsIgnoreCase(expected)) return;
        Files.createDirectories(target.toAbsolutePath().getParent());
        Path temp=Files.createTempFile(target.toAbsolutePath().getParent(),"download-",".part");
        try {
            var r=HTTP.send(request(url).GET().build(),HttpResponse.BodyHandlers.ofFile(temp));
            if(r.statusCode()/100!=2) throw new IOException("Download HTTP "+r.statusCode()+": "+URI.create(url).getHost());
            if(expected!=null&&!hash(temp,algorithm).equalsIgnoreCase(expected)) throw new IOException("Checksum failed: "+target.getFileName());
            move(temp,target);
        } finally { Files.deleteIfExists(temp); }
    }
    public static void move(Path from,Path to) throws IOException {
        try { Files.move(from,to,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
        catch(AtomicMoveNotSupportedException e) { Files.move(from,to,StandardCopyOption.REPLACE_EXISTING); }
    }
    public static Path child(Path root,String relative) throws IOException {
        Path base=root.toAbsolutePath().normalize(), out=base.resolve(relative).normalize();
        if(Path.of(relative).isAbsolute()||!out.startsWith(base)||out.equals(base)||relative.contains(":")) throw new IOException("Unsafe file path: "+relative);
        return out;
    }
    public static void writeJson(Path path,Object value) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path tmp=Files.createTempFile(path.toAbsolutePath().getParent(),"json-",".tmp");
        try { Files.writeString(tmp,JSON.toJson(value)); move(tmp,path); } finally { Files.deleteIfExists(tmp); }
    }
}
