package neko.shulker.geyseryggdrasil.auth;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class YggdrasilAuth {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private final String apiRoot;

    public YggdrasilAuth(String apiRoot) {
        this.apiRoot = trimTrailingSlash(apiRoot);
    }

    public String sessionServerRoot() {
        return this.apiRoot + "/sessionserver";
    }

    public AuthResult authenticate(String username, String password) throws IOException, InterruptedException {
        JsonObject request = new JsonObject();
        JsonObject agent = new JsonObject();
        agent.addProperty("name", "Minecraft");
        agent.addProperty("version", 1);
        request.add("agent", agent);
        request.addProperty("username", username);
        request.addProperty("password", password);
        request.addProperty("requestUser", false);
        return parseAuthResult(post(this.apiRoot + "/authserver/authenticate", request));
    }

    public AuthResult refresh(String accessToken, String clientToken, Profile selectedProfile) throws IOException, InterruptedException {
        JsonObject request = new JsonObject();
        request.addProperty("accessToken", accessToken);
        request.addProperty("clientToken", clientToken);
        JsonObject profile = new JsonObject();
        profile.addProperty("id", undashed(selectedProfile.id()));
        profile.addProperty("name", selectedProfile.name());
        request.add("selectedProfile", profile);
        request.addProperty("requestUser", false);
        return parseAuthResult(post(this.apiRoot + "/authserver/refresh", request));
    }

    private static JsonObject post(String url, JsonObject body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(errorMessage(response.body(), response.statusCode()));
        }
        return GSON.fromJson(response.body(), JsonObject.class);
    }

    private static AuthResult parseAuthResult(JsonObject json) {
        String accessToken = string(json, "accessToken");
        String clientToken = string(json, "clientToken");
        Profile selectedProfile = profile(json.getAsJsonObject("selectedProfile"));
        List<Profile> profiles = new ArrayList<>();
        JsonArray availableProfiles = json.getAsJsonArray("availableProfiles");
        if (availableProfiles != null) {
            for (JsonElement element : availableProfiles) {
                Profile profile = profile(element.getAsJsonObject());
                if (profile != null) {
                    profiles.add(profile);
                }
            }
        }
        return new AuthResult(accessToken, clientToken, List.copyOf(profiles), selectedProfile);
    }

    private static Profile profile(JsonObject json) {
        if (json == null) {
            return null;
        }
        return new Profile(parseUuid(string(json, "id")), string(json, "name"));
    }

    private static String string(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static UUID parseUuid(String value) {
        if (value.length() == 32) {
            value = value.substring(0, 8) + "-" + value.substring(8, 12) + "-" + value.substring(12, 16) + "-" + value.substring(16, 20) + "-" + value.substring(20);
        }
        return UUID.fromString(value);
    }

    private static String undashed(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    private static String errorMessage(String body, int statusCode) {
        try {
            JsonObject json = GSON.fromJson(body, JsonObject.class);
            String message = string(json, "errorMessage");
            return message == null ? "HTTP " + statusCode : message;
        } catch (Throwable ignored) {
            return "HTTP " + statusCode;
        }
    }

    private static String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public record Profile(UUID id, String name) {
    }

    public record AuthResult(String accessToken, String clientToken, List<Profile> availableProfiles, Profile selectedProfile) {
    }
}
