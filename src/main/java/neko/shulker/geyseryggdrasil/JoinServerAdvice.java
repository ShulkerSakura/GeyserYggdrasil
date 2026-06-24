package neko.shulker.geyseryggdrasil;

import net.bytebuddy.asm.Advice;
import org.geysermc.mcprotocollib.auth.GameProfile;

public final class JoinServerAdvice {
    private JoinServerAdvice() {
    }

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean enter(@Advice.Argument(0) GameProfile profile,
                                @Advice.Argument(1) String accessToken,
                                @Advice.Argument(2) String serverId) {
        return AgentHooks.joinServer(profile, accessToken, serverId);
    }
}
