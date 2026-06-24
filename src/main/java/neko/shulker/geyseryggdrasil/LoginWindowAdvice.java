package neko.shulker.geyseryggdrasil;

import net.bytebuddy.asm.Advice;

public final class LoginWindowAdvice {
    private LoginWindowAdvice() {
    }

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean enter(@Advice.Argument(0) Object session) {
        return AgentHooks.shouldHandleLoginWindow(session);
    }
}
