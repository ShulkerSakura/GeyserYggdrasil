package neko.shulker.geyseryggdrasil;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

public final class GeyserYggdrasilAgent {
    static volatile AgentConfig config = new AgentConfigLoader().load(null);

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        install(agentArgs, instrumentation);
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        install(agentArgs, instrumentation);
    }

    private static void install(String agentArgs, Instrumentation instrumentation) {
        config = new AgentConfigLoader().load(agentArgs);
        if (!config.enabled()) {
            System.out.println("[GeyserYggdrasil] Disabled: no yggdrasil-api-root configured");
            return;
        }

        new AgentBuilder.Default()
                .ignore(ElementMatchers.nameStartsWith("dev.shulker.geyseryggdrasil."))
                .type(ElementMatchers.named("org.geysermc.geyser.util.LoginEncryptionUtils"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> builder
                        .visit(Advice.to(LoginWindowAdvice.class)
                                .on(ElementMatchers.named("buildAndShowLoginWindow")
                                        .and(ElementMatchers.takesArguments(1)))))
                .type(ElementMatchers.named("org.geysermc.mcprotocollib.auth.SessionService"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> builder
                        .visit(Advice.to(JoinServerAdvice.class)
                                .on(ElementMatchers.named("joinServer")
                                        .and(ElementMatchers.takesArguments(3)))))
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .installOn(instrumentation);

        System.out.println("[GeyserYggdrasil] Enabled with API root: " + config.yggdrasilApiRoot());
    }

    private static final class AgentConfigLoader {
        AgentConfig load(String agentArgs) {
            return AgentConfig.load(agentArgs);
        }
    }
}
