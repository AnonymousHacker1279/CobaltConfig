package tech.anonymoushacker1729.cobaltconfig.config;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import tech.anonymoushacker1729.cobaltconfig.CobaltConfig;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConfigManager {

	private final String modId;
	private final Class<?> configClass;
	private final Path path;
	private final boolean clientOnly;

	private static final List<ConfigManager> instances = new ArrayList<>(10);

	private static final Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	/**
	 * Represents a configuration manager.
	 *
	 * @param modId       the mod ID
	 * @param suffix      the suffix, if any
	 * @param configClass the config class
	 * @param clientOnly  whether the config is client-only
	 */
	private ConfigManager(String modId, @Nullable String suffix, Class<?> configClass, boolean clientOnly) {
		this.modId = modId;
		this.configClass = configClass;
		this.path = getConfigPath(modId, suffix);
		this.clientOnly = clientOnly;

		instances.add(this);

		if (clientOnly && FMLEnvironment.dist.isDedicatedServer()) {
			return;
		}

		initializeConfig(configClass);
		writeChanges();
	}

	@SuppressWarnings("unused")
	public static class ConfigBuilder {
		private final String modId;
		private final Class<?> configClass;
		@Nullable
		private final String suffix;
		private boolean clientOnly = false;

		/**
		 * Creates a new <code>ConfigBuilder</code> instance with no suffix.
		 *
		 * @param modId       the mod ID
		 * @param configClass the config class
		 */
		@AvailableSince("1.0.0")
		public ConfigBuilder(String modId, Class<?> configClass) {
			this(modId, null, configClass);
		}

		/**
		 * Creates a new <code>ConfigBuilder</code> instance with a suffix.
		 *
		 * @param modId       the mod ID
		 * @param suffix      the suffix
		 * @param configClass the config class
		 */
		@AvailableSince("1.0.0")
		public ConfigBuilder(String modId, @Nullable String suffix, Class<?> configClass) {
			this.modId = modId;
			this.configClass = configClass;
			this.suffix = suffix;
		}

		/**
		 * Mark a config as client-only. These will not be generated in dedicated server environments and therefore
		 * are not synced to clients.
		 *
		 * <p>Defaults to false.
		 *
		 * @param clientOnly whether the config is client-only
		 * @return ConfigBuilder
		 */
		@AvailableSince("1.0.0")
		public ConfigBuilder setClientOnly(boolean clientOnly) {
			this.clientOnly = clientOnly;
			return this;
		}

		/**
		 * Build a new <code>ConfigManager</code> instance.
		 */
		@AvailableSince("1.0.0")
		public void build() {
			new ConfigManager(modId, suffix, configClass, clientOnly);
		}
	}

	/**
	 * Initializes the config file by reading the values and setting the static fields.
	 *
	 * @param configClass the config class
	 */
	private void initializeConfig(Class<?> configClass) {
		Map<String, Object> configValues = openConfig();

		for (Field field : configClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(ConfigEntry.class)) {
				ConfigEntry entry = field.getAnnotation(ConfigEntry.class);
				// Read the value from the config file or use the default value
				Object value;
				try {
					value = readValueFromConfigFile(field.getName(), field.get(null), configValues);
				} catch (IllegalAccessException e) {
					CobaltConfig.LOGGER.error("Failed to read config file for mod " + modId + "! The following error was thrown:");
					throw new RuntimeException(e);
				}
				// Set the value of the static field
				try {
					field.set(null, value);
				} catch (IllegalAccessException e) {
					CobaltConfig.LOGGER.error("Failed to set config value for mod " + modId + "! The following error was thrown:");
					throw new RuntimeException(e);
				} catch (IllegalArgumentException e) {
					CobaltConfig.LOGGER.error("Failed to set config value for mod " + modId + "! The following error was thrown:");
					CobaltConfig.LOGGER.error("The value " + value + " is not of type " + entry.type().getSimpleName());
					throw new RuntimeException(e);
				}
			}
		}
	}

	/**
	 * Reloads the config file by reading the values and setting the static fields.
	 *
	 * @param configClass the config class
	 */
	public void reload(Class<?> configClass) {
		Map<String, Object> configValues = openConfig();

		for (Field field : configClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(ConfigEntry.class)) {
				// Read the value from the config file or use the default value
				Object value;
				try {
					value = readValueFromConfigFile(field.getName(), field.get(null), configValues);
				} catch (IllegalAccessException e) {
					CobaltConfig.LOGGER.error("Failed to read config file for mod " + modId + "! The following error was thrown:");
					throw new RuntimeException(e);
				}
				// Set the value of the static field
				try {
					field.set(null, value);
				} catch (IllegalAccessException e) {
					CobaltConfig.LOGGER.error("Failed to set config value for mod " + modId + "! The following error was thrown:");
					throw new RuntimeException(e);
				}
			}
		}
	}

	/**
	 * Loads the config values from a map.
	 *
	 * @param configClassName the config class name
	 * @param configValues    the config values
	 */
	@Internal
	public static void loadFromMap(String configClassName, Map<String, Object> configValues) {
		for (ConfigManager manager : instances) {
			if (manager.configClass.getName().equals(configClassName)) {
				for (Field field : manager.configClass.getDeclaredFields()) {
					if (field.isAnnotationPresent(ConfigEntry.class)) {
						// Read the value from the config file or use the default value
						Object value;
						try {
							value = manager.readValueFromConfigFile(field.getName(), field.get(null), configValues);
						} catch (IllegalAccessException e) {
							CobaltConfig.LOGGER.error("Failed to read config file for mod " + manager.modId + "! The following error was thrown:");
							throw new RuntimeException(e);
						}
						// Set the value of the static field
						try {
							field.set(null, value);
						} catch (IllegalAccessException e) {
							CobaltConfig.LOGGER.error("Failed to set config value for mod " + manager.modId + "! The following error was thrown:");
							throw new RuntimeException(e);
						}
					}
				}
			}
		}
	}

	/**
	 * Opens the config file and returns the values as a map.
	 *
	 * @return Map
	 */
	@Nullable
	private Map<String, Object> openConfig() {
		Map<String, Object> configValues = null;
		if (Files.exists(path)) {
			Type type = new TypeToken<Map<String, Object>>() {
			}.getType();
			try {
				configValues = gson.fromJson(new String(Files.readAllBytes(path)), type);
			} catch (IOException e) {
				CobaltConfig.LOGGER.error("Failed to read config file for mod " + modId + "! The following error was thrown:");
				throw new RuntimeException(e);
			}
		}

		return configValues;
	}

	/**
	 * Reads the value from the config file or uses the default value.
	 *
	 * @param name         the name of the config option
	 * @param defaultValue the default value
	 * @param configValues the config values
	 * @return Object
	 */
	private Object readValueFromConfigFile(String name, Object defaultValue, @Nullable Map<String, Object> configValues) {
		if (configValues != null && configValues.containsKey(name)) {
			Object configValue = configValues.get(name);

			if (defaultValue instanceof Integer && configValue instanceof Double doubleValue) {
				return doubleValue.intValue();
			} else if (defaultValue instanceof List && configValue instanceof List<?> listValue) {
				return new ArrayList<>(listValue);
			} else if (defaultValue instanceof Map && configValue instanceof Map<?, ?> mapValue) {
				return new HashMap<>(mapValue);
			}

			return defaultValue.getClass().cast(configValue);
		} else {
			return defaultValue;
		}
	}

	/**
	 * Writes the current values of the config class to the config file.
	 */
	@SuppressWarnings("unchecked")
	public void writeChanges() {
		try {
			if (!Files.exists(path)) {
				Files.createFile(path);
			}

			JsonObject jsonObject = gson.toJsonTree(buildConfigValuesMap(configClass)).getAsJsonObject();

			// Append Map type values because they are not serialized correctly by default
			for (Field field : configClass.getDeclaredFields()) {
				if (field.isAnnotationPresent(ConfigEntry.class) && field.get(null) instanceof Map) {
					Map<String, ?> map = (Map<String, ?>) field.get(null);
					if (map != null) {
						JsonElement mapJson = gson.toJsonTree(map, new TypeToken<Map<String, ?>>() {}.getType());
						jsonObject.add(field.getName(), mapJson);
					}
				}
			}


			Files.write(path, gson.toJson(jsonObject).getBytes());
		} catch (Exception e) {
			CobaltConfig.LOGGER.error("Failed to create config file for mod " + modId + "! The following error was thrown:");
			CobaltConfig.LOGGER.error(e.getMessage());
		}
	}

	/**
	 * Reloads all config files.
	 */
	public static void reloadAll() {
		for (ConfigManager instance : instances) {
			CobaltConfig.LOGGER.info("Reloading config for mod " + instance.modId + " (" + instance.path.getFileName() + ")");
			instance.reload(instance.configClass);
		}
	}

	/**
	 * Converts a config class to a map.
	 *
	 * @param configClass the config class
	 * @return Map
	 */
	@Nullable
	@Internal
	public static Map<String, Object> classToMap(Class<?> configClass) {
		for (ConfigManager instance : instances) {
			if (instance.configClass.equals(configClass)) {
				return buildConfigValuesMap(configClass);
			}
		}

		return null;
	}

	/**
	 * Builds a map of config values.
	 *
	 * @param configClass the config class
	 * @return Map
	 */
	private static Map<String, Object> buildConfigValuesMap(Class<?> configClass) {
		Map<String, Object> configValues = new HashMap<>(30);
		for (Field field : configClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(ConfigEntry.class)) {
				try {
					configValues.put(field.getName(), field.get(null));
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return configValues;
	}

	/**
	 * Get the list of configuration managers.
	 */
	public static List<ConfigManager> getManagers(boolean ignoreClientOnly) {
		if (ignoreClientOnly) {
			return instances.stream().filter(manager -> !manager.clientOnly).toList();
		}

		return instances;
	}

	/**
	 * Gets the config class.
	 *
	 * @return Class
	 */
	public Class<?> getConfigClass() {
		return configClass;
	}

	/**
	 * Gets the path to the config file.
	 *
	 * @param modId  the mod ID
	 * @param suffix the suffix, if any
	 * @return Path
	 */
	public static Path getConfigPath(String modId, @Nullable String suffix) {
		return FMLPaths.CONFIGDIR.get().resolve(modId + (suffix == null ? "" : "-" + suffix) + ".json");
	}
}