package neko.shulker.geyseryggdrasil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AgentConfig {
    private final String yggdrasilApiRoot;

    private AgentConfig(String yggdrasilApiRoot) {
        this.yggdrasilApiRoot = trimTrailingSlash(yggdrasilApiRoot == null ? "" : yggdrasilApiRoot.trim());
    }

    public static AgentConfig load(String agentArgs) {
        if (agentArgs == null || agentArgs.isBlank()) {
            return new AgentConfig(System.getProperty("geyser.yggdrasil.apiRoot", ""));
        }

        Path path = Path.of(agentArgs);
        if (!Files.isRegularFile(path)) {
            return new AgentConfig(agentArgs);
        }

        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read agent config: " + path, ex);
        }
        return new AgentConfig(properties.getProperty("yggdrasil-api-root", properties.getProperty("apiRoot", "")));
    }

    public boolean enabled() {
        return !this.yggdrasilApiRoot.isBlank();
    }

    public String yggdrasilApiRoot() {
        return this.yggdrasilApiRoot;
    }

    public String sessionServerRoot() {
        return this.yggdrasilApiRoot + "/sessionserver";
    }

    private static String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
