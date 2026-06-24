plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-beta10"
}

group = "neko.shulker"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    implementation("net.bytebuddy:byte-buddy:1.17.5")
    implementation("net.bytebuddy:byte-buddy-agent:1.17.5")
    implementation("com.google.code.gson:gson:2.10.1")

    compileOnly("org.geysermc.mcprotocollib:protocol:26.2-20260616.191648-13")
    compileOnly("org.geysermc.cumulus:cumulus:1.1.2")
}

tasks.jar {
    manifest {
        attributes(
            "Premain-Class" to "neko.shulker.geyseryggdrasil.GeyserYggdrasilAgent",
            "Agent-Class" to "neko.shulker.geyseryggdrasil.GeyserYggdrasilAgent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true"
        )
    }
}

tasks.shadowJar {
    archiveClassifier.set("all")
    manifest.inheritFrom(tasks.jar.get().manifest)
    relocate("net.bytebuddy", "neko.shulker.geyseryggdrasil.shadow.bytebuddy")
    relocate("com.google.gson", "neko.shulker.geyseryggdrasil.shadow.gson")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
