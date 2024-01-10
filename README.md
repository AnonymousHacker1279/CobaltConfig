![Cobalt Config Logo](logo.png)

# Cobalt Config

## A simple annotation-driven configuration library for Minecraft mods.

Cobalt Config is designed to be simple and lightweight. That makes it ideal
for [Jar-in-Jar](https://docs.neoforged.net/neogradle/docs/dependencies/jarinjar) purposes.

Using it is dead simple. Somewhere (ideally in your mod constructor), create a `ConfigManager` instance:

```java
public YourMod(IEventBus modEventBus) {
	// Creates a config named "mod_id.json"
	new ConfigBuilder("mod_id", ConfigEntries.class).build();

	// Creates a config named "mod_id-client.json", and marks it as client-only
	new ConfigBuilder("mod_id", "client", ConfigClientEntries.class).setClientOnly(true).build();
}
```

And then define classes that contain your configuration entries:

```java
/**
 * Data which is available on both the client and server.
 * It will be synced from the server to clients.
 */
public static class ConfigEntries {
	@ConfigEntry(type = Integer.class, comment = "This is an int config option")
	public static int testIntOption = 50;   // Creates a JSON key named "testIntOption" with a default value of 50

	@ConfigEntry(type = Double.class, comment = "This is a double config option")
	public static double testDoubleOption = 10.0;   // Creates a JSON key named "testDoubleOption" with a default value of 10.0

	@ConfigEntry(type = Boolean.class, comment = "This is a boolean config option")
	public static boolean testBoolean = true;   // Creates a JSON key named "testBoolean" with a default value of true

	@ConfigEntry(type = List.class, comment = "This is a list config option")
	public static List<Integer> testList = Arrays.asList(1, 2, 3);   // Creates a JSON key named "testList" with a default value of [1, 2, 3]

	@ConfigEntry(type = Map.class, comment = "This is a map config option")
	public static Map<String, ?> testMap = new HashMap<>() {{  // Creates a JSON key named "testMap" with a default value of {"key1": "value1", "key2": 3}
		put("key1", "value1");
		put("key2", 3);
	}};
}

/**
 * Data which is only available on the client.
 * It is not synced and will not generate on dedicated servers.
 */
public static class ConfigClientEntries {
	@ConfigEntry(type = Integer.class, comment = "This is an int config option")
	public static int testIntOption = 10;   // Creates a JSON key named "testIntOption" with a default value of 10
}
```

The library handles the rest. You can then access the config values directly via static references.

Reloading from within the game is also possible via the game's `/reload` command. This will reload all configs, and will
sync them to connected clients.