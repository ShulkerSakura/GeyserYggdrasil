package neko.shulker.geyseryggdrasil;

import neko.shulker.geyseryggdrasil.auth.YggdrasilAuth;
import org.geysermc.cumulus.component.DropdownComponent;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class AgentHooks {
    private static final Map<String, String> SESSION_SERVER_BY_TOKEN = new ConcurrentHashMap<>();

    private AgentHooks() {
    }

    public static boolean shouldHandleLoginWindow(Object session) {
        if (!GeyserYggdrasilAgent.config.enabled()) {
            return false;
        }
        if (isLoggedIn(session)) {
            return true;
        }
        if (!isOnlineAuth(session)) {
            return false;
        }
        resetTimeParameters(session);
        showYggdrasilLoginWindow(session);
        return true;
    }

    public static boolean joinServer(GameProfile profile, String accessToken, String serverId) {
        String sessionServerRoot = SESSION_SERVER_BY_TOKEN.get(accessToken);
        if (sessionServerRoot == null) {
            return false;
        }
        YggdrasilSessionJoiner.join(sessionServerRoot, profile, accessToken, serverId);
        return true;
    }

    static void showYggdrasilLoginWindow(Object session) {
        if (isLoggedIn(session)) {
            return;
        }
        resetTimeParameters(session);
        sendForm(session, CustomForm.builder()
                .title("Login")
                .label("Please log in with your third-party account.")
                .input("Username (email or character name)", "")
                .input("Password", "")
                .closedOrInvalidResultHandler(() -> showYggdrasilLoginWindow(session))
                .validResultHandler(response -> {
                    String username = response.next();
                    String password = response.next();
                    if (username == null || username.isBlank() || password == null || password.isEmpty()) {
                        showYggdrasilLoginWindow(session);
                        return;
                    }
                    authenticateWithYggdrasil(session, username.trim(), password);
                }));
    }

    static void showYggdrasilProfileWindow(Object session, YggdrasilAuth auth, YggdrasilAuth.AuthResult result) {
        List<YggdrasilAuth.Profile> profiles = result.availableProfiles();
        DropdownComponent.Builder dropdown = DropdownComponent.builder("Character");
        for (YggdrasilAuth.Profile profile : profiles) {
            dropdown.option(profile.name());
        }

        sendForm(session, CustomForm.builder()
                .title("Select Character")
                .label("This account has multiple characters. Please choose one.")
                .dropdown(dropdown)
                .closedOrInvalidResultHandler(() -> showYggdrasilProfileWindow(session, auth, result))
                .validResultHandler(response -> {
                    int index = (int) response.next();
                    if (index < 0 || index >= profiles.size()) {
                        showYggdrasilProfileWindow(session, auth, result);
                        return;
                    }
                    bindAndConnectYggdrasil(session, auth, result, profiles.get(index));
                }));
    }

    static void authenticateWithYggdrasil(Object session, String username, String password) {
        CompletableFuture.runAsync(() -> {
            try {
                YggdrasilAuth auth = new YggdrasilAuth(GeyserYggdrasilAgent.config.yggdrasilApiRoot());
                YggdrasilAuth.AuthResult result = auth.authenticate(username, password);
                YggdrasilAuth.Profile selected = result.selectedProfile();
                if (selected == null) {
                    List<YggdrasilAuth.Profile> profiles = result.availableProfiles();
                    if (profiles.isEmpty()) {
                        disconnect(session, "This account has no available Minecraft profile.");
                        return;
                    }
                    if (profiles.size() == 1) {
                        bindAndConnectYggdrasil(session, auth, result, profiles.getFirst());
                        return;
                    }
                    connect(session);
                    showYggdrasilProfileWindow(session, auth, result);
                    return;
                }
                finishYggdrasilLogin(session, auth, result.accessToken(), selected);
            } catch (Throwable ex) {
                disconnect(session, "Yggdrasil authentication failed: " + rootMessage(ex));
            }
        });
    }

    static void bindAndConnectYggdrasil(Object session, YggdrasilAuth auth, YggdrasilAuth.AuthResult result, YggdrasilAuth.Profile profile) {
        CompletableFuture.runAsync(() -> {
            try {
                YggdrasilAuth.AuthResult refreshed = auth.refresh(result.accessToken(), result.clientToken(), profile);
                YggdrasilAuth.Profile selected = refreshed.selectedProfile() == null ? profile : refreshed.selectedProfile();
                finishYggdrasilLogin(session, auth, refreshed.accessToken(), selected);
            } catch (Throwable ex) {
                disconnect(session, "Yggdrasil profile selection failed: " + rootMessage(ex));
            }
        });
    }

    private static void finishYggdrasilLogin(Object session, YggdrasilAuth auth, String accessToken, YggdrasilAuth.Profile profile) throws Exception {
        SESSION_SERVER_BY_TOKEN.put(accessToken, auth.sessionServerRoot());
        MinecraftProtocol protocol = new MinecraftProtocol(new GameProfile(profile.id(), profile.name()), accessToken);
        setField(session, "protocol", protocol);
        invoke(session, "connectDownstream");
    }

    private static boolean isLoggedIn(Object session) {
        return Boolean.TRUE.equals(invoke(session, "isLoggedIn"));
    }

    private static boolean isOnlineAuth(Object session) {
        try {
            Object geyser = invoke(session, "getGeyser");
            Object config = invoke(geyser, "config");
            Object javaConfig = invoke(config, "java");
            Object authType = invoke(javaConfig, "authType");
            return "ONLINE".equals(String.valueOf(authType));
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static void resetTimeParameters(Object session) {
        invoke(session, "resetTimeParameters");
    }

    private static void connect(Object session) {
        invoke(session, "connect");
    }

    private static void disconnect(Object session, String reason) {
        invoke(session, "disconnect", new Class<?>[]{String.class}, reason);
    }

    private static void sendForm(Object session, Object formBuilder) {
        for (Method method : session.getClass().getMethods()) {
            if (!method.getName().equals("sendForm") || method.getParameterCount() != 1) {
                continue;
            }
            if (method.getParameterTypes()[0].isInstance(formBuilder)) {
                try {
                    method.setAccessible(true);
                    method.invoke(session, formBuilder);
                    return;
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
        throw new IllegalStateException("No compatible sendForm method found for " + formBuilder.getClass());
    }

    private static Object invoke(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (NoSuchMethodException ex) {
            try {
                Method method = target.getClass().getDeclaredMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (ReflectiveOperationException inner) {
                throw new IllegalStateException(inner);
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Object invoke(Object target, String name, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(name, parameterTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (NoSuchMethodException ex) {
            try {
                Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method.invoke(target, args);
            } catch (ReflectiveOperationException inner) {
                throw new IllegalStateException(inner);
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
