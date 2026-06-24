package neko.shulker.geyseryggdrasil;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.geysermc.mcprotocollib.auth.GameProfile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class YggdrasilSessionJoiner {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private YggdrasilSessionJoiner() {
    }

    public static void join(String sessionServerRoot, GameProfile profile, String accessToken, String serverId) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("accessToken", accessToken);
            request.addProperty("selectedProfile", profile.getId().toString().replace("-", ""));
            request.addProperty("serverId", serverId);
            HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder(URI.create(trimTrailingSlash(sessionServerRoot) + "/session/minecraft/join"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(request)))
                    .build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 204 && response.statusCode() != 200) {
                throw new IOException("Yggdrasil join failed with HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    private static String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
