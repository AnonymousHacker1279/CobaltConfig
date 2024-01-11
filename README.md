![Cobalt Config Logo](logo.png)

# Cobalt Config

## A simple annotation-driven configuration library for Minecraft mods.

Cobalt Config is designed to be simple and lightweight. That makes it ideal
for [Jar-in-Jar](https://docs.neoforged.net/neogradle/docs/dependencies/jarinjar) purposes.

Using it is dead simple. Somewhere (ideally in your mod constructor), create a `ConfigManager` instance:

```java
public YourMod(IEventBus modEventBus) {
	// Creates a config named "modid.json"
	new ConfigBuilder("modid", ConfigEntries.class).build();

	// Creates a config named "modid-client.json", and marks it as client-only
	new ConfigBuilder("modid", "client", ConfigClientEntries.class).setClientOnly(true).build();
}
```

And then define classes that contain your configuration entries:

```java
/**
 * Data which is available on both the client and server.
 * It will be synced from the server to clients.
 */
public static class ConfigEntries {
	@ConfigEntry(type = Integer.class, comment = "This is an int config option", max = 100)
	public static int testIntOption = 50;   // Creates a JSON key named "testIntOption" with a default value of 50

	@ConfigEntry(type = Double.class, comment = "This is a double config option", min = 5.0, max = 15.0)
	public static double testDoubleOption = 10.0;   // Creates a JSON key named "testDoubleOption" with a default value of 10.0

	@ConfigEntry(type = Integer.class, comment = "This is an int config option with no bounds")
	public static int testIntOptionNoBounds = 50;   // Creates a JSON key named "testIntOptionNoBounds" with a default value of 50

	@ConfigEntry(type = Double.class, comment = "This is a double config option with no bounds")
	public static double testDoubleOptionNoBounds = 10.0;  // Creates a JSON key named "testDoubleOptionNoBounds" with a default value of 10.0

	@ConfigEntry(type = Boolean.class, comment = "This is a boolean config option")
	public static boolean testBoolean = true;   // Creates a JSON key named "testBoolean" with a default value of true

	@ConfigEntry(type = List.class, comment = "This is a list config option", group = "Test Group")
	public static List<Integer> testList = Arrays.asList(1, 2, 3);   // Creates a JSON key named "testList" with a default value of [1, 2, 3]

	@ConfigEntry(type = Map.class, comment = "This is a map config option", group = "Test Group")
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

### Configuration Menu

Cobalt Config is equipped with a simple configuration menu which is accessible via the mod menu "Config" button for any
mods that implement this library. Simply register the configuration screen during mod construction and no other work is
required:

```java

@SubscribeEvent
public static void constructMod(FMLConstructModEvent event) {
	ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
			() -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> CobaltConfigScreen.getScreen(screen, CobaltConfig.MOD_ID)));
}
```

Comments added via annotations will be displayed next to each entry. They may either be plaintext or translation keys.
The same applies to groups.<sup id="a1">[1](#f1)</sup>

If min/max bounds are specified on a numeric-type entry, a slider will be displayed instead of a text box.

When using the configuration menu, your language file should also contain entries for each entry. They might look
something like this:

```json
{
	"modid.cobaltconfig.yourField": "This is a translation key for the config option yourField"
}
```

<b id="f1">1</b>: Groups are a way to organize config entries in the menu. They do not affect the JSON output. [↩](#a1)