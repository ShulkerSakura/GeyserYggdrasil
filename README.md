# GeyserYggdrasil

A lightweight Java agent that lets [Geyser](https://github.com/GeyserMC/Geyser) authenticate Bedrock
players with third-party Yggdrasil authentication server instead of a Microsoft account.

It is attached to Geyser at startup with `-javaagent` and requires **no source modifications** to Geyser,
so it keeps working across most Geyser updates without re-patching.

## Usage

### 1. Get the agent jar

## Building

The project does not ship its own Gradle wrapper. Use any Gradle 9.x, or reuse the wrapper from a Geyser
checkout placed alongside this repository.

```bash
./gradlew build
```

The shaded agent jar is produced at:

```
build/libs/GeyserYggdrasil-1.0.0-all.jar
```

### 2. Configure the Yggdrasil API root

Create a config file next to your Geyser jar, e.g. `geyser-yggdrasil.properties`:

```properties
yggdrasil-api-root=https://example.com/api/yggdrasil
```

The agent derives `authserver` and `sessionserver` automatically by appending `/authserver` and
`/sessionserver` to this root.

The API root can be provided in three ways (in order of precedence):

1. A path to a `.properties` file passed as the agent argument.
2. The API root URL passed directly as the agent argument.
3. The `geyser.yggdrasil.apiRoot` system property.

If no API root is configured, the agent stays disabled and Geyser behaves as normal.

### 3. Attach the agent to Geyser

```bash
java -javaagent:./GeyserYggdrasil-1.0.0-all.jar=./geyser-yggdrasil.properties -jar Geyser-Standalone.jar
```

Or pass the URL inline instead of a config file:

```bash
java -javaagent:./GeyserYggdrasil-1.0.0-all.jar=https://example.com/api/yggdrasil -jar Geyser-Standalone.jar
```

Place `-javaagent` before `-jar`.

### 4. Geyser configuration

Keep Geyser's own auth type set to online in its `config.yml`:

```yaml
java:
  auth-type: online
```
