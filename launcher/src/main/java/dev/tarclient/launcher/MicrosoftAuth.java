package dev.tarclient.launcher;

import com.google.gson.*;
import java.util.*;
import java.util.function.*;
import java.io.*;

/** Browser-based Microsoft device authorization. Tokens live only in this process. */
public final class MicrosoftAuth {
    /** Public desktop application identifier; never a password or client secret. */
    public static final String DEFAULT_CLIENT_ID="c8d8f6e2-12dc-4499-911c-1c7294e91f44";
    static String clientId(String override) {
        return override==null||override.isBlank()?DEFAULT_CLIENT_ID:override.trim();
    }
    public record Session(String name,String uuid,String accessToken,long expiresAt,boolean demo) {
        public static Session demoSession() { return new Session("DemoPlayer","00000000000000000000000000000000","0",Long.MAX_VALUE,true); }
        @Override public String toString() { return name+(demo?" (demo)":""); }
    }
    public record DeviceCode(String code,String url,int expiresIn) {}
    public Session login(String clientId,Consumer<DeviceCode> showCode) throws Exception {
        clientId=clientId(clientId);
        String root="https://login.microsoftonline.com/consumers/oauth2/v2.0/";
        var device=Net.form(root+"devicecode",Map.of("client_id",clientId.trim(),"scope","XboxLive.signin"));
        check(device);
        int seconds=device.get("expires_in").getAsInt(), interval=device.get("interval").getAsInt();
        showCode.accept(new DeviceCode(device.get("user_code").getAsString(),device.get("verification_uri").getAsString(),seconds));
        long deadline=System.currentTimeMillis()+seconds*1000L; String ms=null;
        while(System.currentTimeMillis()<deadline) {
            Thread.sleep(interval*1000L);
            var token=Net.form(root+"token",Map.of("grant_type","urn:ietf:params:oauth:grant-type:device_code","client_id",clientId.trim(),"device_code",device.get("device_code").getAsString()));
            if(!token.has("error")) { ms=token.get("access_token").getAsString(); break; }
            String err=token.get("error").getAsString();
            if(err.equals("authorization_pending")) continue;
            if(err.equals("slow_down")) { interval+=5; continue; }
            check(token);
        }
        if(ms==null) throw new IOException("Sign-in expired. Please try again.");
        var xboxBody=new JsonObject(); var p=new JsonObject();
        p.addProperty("AuthMethod","RPS"); p.addProperty("SiteName","user.auth.xboxlive.com"); p.addProperty("RpsTicket","d="+ms);
        xboxBody.add("Properties",p); xboxBody.addProperty("RelyingParty","http://auth.xboxlive.com"); xboxBody.addProperty("TokenType","JWT");
        var xbox=Net.post("https://user.auth.xboxlive.com/user/authenticate",xboxBody);
        var xstsBody=new JsonObject(); p=new JsonObject(); p.addProperty("SandboxId","RETAIL");
        var tokens=new JsonArray(); tokens.add(xbox.get("Token").getAsString()); p.add("UserTokens",tokens);
        xstsBody.add("Properties",p); xstsBody.addProperty("RelyingParty","rp://api.minecraftservices.com/"); xstsBody.addProperty("TokenType","JWT");
        var xsts=Net.post("https://xsts.auth.xboxlive.com/xsts/authorize",xstsBody);
        String uhs=xsts.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
        var mcBody=new JsonObject(); mcBody.addProperty("identityToken","XBL3.0 x="+uhs+";"+xsts.get("Token").getAsString());
        var mc=Net.post("https://api.minecraftservices.com/authentication/login_with_xbox",mcBody);
        String access=mc.get("access_token").getAsString();
        var entitlements=Net.send(Net.request("https://api.minecraftservices.com/entitlements/mcstore").header("Authorization","Bearer "+access).GET().build()).getAsJsonObject();
        if(!entitlements.has("items")||entitlements.getAsJsonArray("items").isEmpty())throw new IOException("This Microsoft account has no active Minecraft Java entitlement. Use the demo option or sign in with an eligible account.");
        // A successful profile response is required: no offline account impersonation or ownership bypass.
        var profile=Net.send(Net.request("https://api.minecraftservices.com/minecraft/profile").header("Authorization","Bearer "+access).GET().build()).getAsJsonObject();
        return new Session(profile.get("name").getAsString(),profile.get("id").getAsString(),access,System.currentTimeMillis()+mc.get("expires_in").getAsLong()*1000L,false);
    }
    private void check(JsonObject result) throws IOException {
        if(result.has("error")) throw new IOException("Microsoft sign-in: "+result.get("error").getAsString()+". Check the application ID, public-client setting, and Xbox/Minecraft API access.");
    }
}
