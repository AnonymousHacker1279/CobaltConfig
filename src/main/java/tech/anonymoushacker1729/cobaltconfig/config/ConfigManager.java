package tech.anonymoushacker1729.cobaltconfig.config;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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
	private final String configName;
	private final Map<String, Object> defaultValues = new HashMap<>(30);

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
	private ConfigManager(String modId, @Nullable String suffix, Class<?> configClass, boolean clientOnly, String configName) {
		this.modId = modId;
		this.configClass = configClass;
		this.path = getConfigPath(modId, suffix);
		this.clientOnly = clientOnly;
		this.configName = configName;

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
		private String configName;

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
			this.configName = configClass.getSimpleName();
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
		 * Set the config name. If not provided, the config class name will be used.
		 *
		 * <p>
		 * This is used for listing available config buttons in the config screen, if one is registered.
		 *
		 * @param configName the config name
		 * @return ConfigBuilder
		 */
		@AvailableSince("1.0.0")
		public ConfigBuilder setConfigName(String configName) {
			this.configName = configName;
			return this;
		}

		/**
		 * Build a new <code>ConfigManager</code> instance.
		 */
		@AvailableSince("1.0.0")
		public void build() {
			new ConfigManager(modId, suffix, configClass, clientOnly, configName);
		}
	}

	/**
	 * Initializes the config file by reading the values and setting the static fields.
	 *
	 * @param configClass the config class
	 */
	@Internal
	public void initializeConfig(Class<?> configClass) {
		Map<String, Object> configValues = openConfig();

		for (Field field : configClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(ConfigEntry.class)) {
				ConfigEntry entry = field.getAnnotation(ConfigEntry.class);
				// Read the value from the config file or use the default value
				Object value;
				try {
					String fieldName = field.getName();
					Object fieldValue = field.get(null);

					defaultValues.put(fieldName, fieldValue);
					value = readValueFromConfigFile(fieldName, fieldValue, configValues);
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
					CobaltConfig.LOGGER.error("The value " + value + " is not of type " + entry.getClass().getSimpleName());
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
			Type type = new TypeToken<Map<String, Object>>() {}.getType();
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
	@SuppressWarnings("unchecked")
	private Object readValueFromConfigFile(String name, Object defaultValue, @Nullable Map<String, Object> configValues) {
		defaultValue = defaultValues.getOrDefault(name, defaultValue);

		if (configValues != null && configValues.containsKey(name)) {
			Object configValue = configValues.get(name);

			if (defaultValue instanceof Integer && configValue instanceof Double doubleValue) {
				return (int) clampToBounds(doubleValue.intValue(), name);
			} else if (defaultValue instanceof Float && configValue instanceof Double doubleValue) {
				return (float) clampToBounds(doubleValue.floatValue(), name);
			} else if (defaultValue instanceof List && configValue instanceof List<?> listValue) {
				return new ArrayList<>(listValue);
			} else if (defaultValue instanceof Map && configValue instanceof Map<?, ?> mapValue) {
				return getCastMap((Map<String, Object>) defaultValue, mapValue);
			}

			try {
				Object castValue = defaultValue.getClass().cast(configValue);

				if (castValue instanceof Double d) {
					return clampToBounds(d, name);
				} else {
					return castValue;
				}
			} catch (ClassCastException e) {
				CobaltConfig.LOGGER.error("Failed to read config file for mod " + modId + "! The following error was thrown:");
				CobaltConfig.LOGGER.error("The value " + configValue + " is not of type " + defaultValue.getClass().getSimpleName());
				throw new RuntimeException(e);
			}
		} else {
			return defaultValue;
		}
	}

	/**
	 * Casts the map values to the correct type as defined by the default value.
	 *
	 * @param defaultValue the default value
	 * @param mapValue     the map value
	 * @return Map
	 */
	private static Map<String, Object> getCastMap(Map<String, Object> defaultValue, Map<?, ?> mapValue) {
		Map<String, Object> castMap = new HashMap<>(30);
		for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
			String key = (String) entry.getKey();
			Object value = entry.getValue();
			Object defaultMapValue = defaultValue.get(key);
			if (defaultMapValue instanceof Integer && value instanceof Double) {
				castMap.put(key, ((Double) value).intValue());
			} else if (defaultMapValue instanceof Float && value instanceof Double) {
				castMap.put(key, ((Double) value).floatValue());
			} else {
				castMap.put(key, value);
			}
		}
		return castMap;
	}

	/**
	 * Clamps a value to the min and max values specified in the config entry annotation.
	 *
	 * @param value the value to clamp
	 * @param key   the config option name
	 * @return double
	 */
	private double clampToBounds(double value, String key) {
		double min = getMin(configClass, key);
		double max = getMax(configClass, key);

		return Mth.clamp(value, min, max);
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
		Map<String, Object> configValues = new LinkedHashMap<>(30);
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
	 * Get the list of configuration managers for a specific mod ID.
	 *
	 * @param modId            the mod ID
	 * @param ignoreClientOnly whether to ignore client-only configs
	 * @return List
	 */
	public static List<ConfigManager> getManagers(String modId, boolean ignoreClientOnly) {
		if (ignoreClientOnly) {
			return instances.stream().filter(manager -> !manager.clientOnly && manager.modId.equals(modId)).toList();
		} else {
			return instances.stream().filter(manager -> manager.modId.equals(modId)).toList();
		}
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
	 * Get a {@link ConfigEntry#comment()} for a given key if present.
	 *
	 * @param configClass the config class
	 * @param key         the key
	 * @return the comment or null if not present
	 */
	@Nullable
	@AvailableSince("1.0.0")
	public static String getComment(Class<?> configClass, String key) {
		try {
			Field field = configClass.getField(key);
			ConfigEntry configEntry = field.getAnnotation(ConfigEntry.class);
			if (configEntry != null) {
				return configEntry.comment();
			}
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Config field not found: " + key, e);
		}
		return null;
	}

	/**
	 * Get a {@link ConfigEntry#group()} for a given key if present.
	 *
	 * @param configClass the config class
	 * @param key         the key
	 * @return the group if present, otherwise an empty string
	 */
	@AvailableSince("1.0.0")
	public static String getGroup(Class<?> configClass, String key) {
		try {
			// Get the ConfigEntry annotation from the configClass using reflection
			ConfigEntry configEntry = configClass.getField(key).getAnnotation(ConfigEntry.class);
			if (configEntry != null) {
				// Get the group from the annotation
				return configEntry.group();
			}
		} catch (NoSuchFieldException e) {
			CobaltConfig.LOGGER.error("Failed to get the group for config option " + key + "! The field does not exist.");
		}
		return "";
	}

	/**
	 * Get the {@link ConfigEntry#min()} for a given key if present.
	 *
	 * @param configClass the config class
	 * @param key         the key
	 * @return the min value. If not specified, it will be {@link Double#MIN_VALUE}
	 */
	public static Double getMin(Class<?> configClass, String key) {
		try {
			Field field = configClass.getField(key);
			ConfigEntry configEntry = field.getAnnotation(ConfigEntry.class);
			if (configEntry != null) {
				return configEntry.min();
			}
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Config field not found: " + key, e);
		}

		return Double.MIN_VALUE;
	}

	/**
	 * Get the {@link ConfigEntry#max()} for a given key if present.
	 *
	 * @param configClass the config class
	 * @param key         the key
	 * @return the max value. If not specified, it will be {@link Double#MAX_VALUE}
	 */
	public static Double getMax(Class<?> configClass, String key) {
		try {
			Field field = configClass.getField(key);
			ConfigEntry configEntry = field.getAnnotation(ConfigEntry.class);
			if (configEntry != null) {
				return configEntry.max();
			}
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Config field not found: " + key, e);
		}

		return Double.MIN_VALUE;
	}

	/**
	 * Get the {@link ConfigEntry#restartRequired()} for a given key if present.
	 *
	 * @param configClass the config class
	 * @param key         the key
	 * @return true if a restart is required
	 */
	@AvailableSince("1.0.0")
	public static boolean getRestartRequired(Class<?> configClass, String key) {
		try {
			Field field = configClass.getField(key);
			ConfigEntry configEntry = field.getAnnotation(ConfigEntry.class);
			if (configEntry != null) {
				return configEntry.restartRequired();
			}
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Config field not found: " + key, e);
		}

		return false;
	}

	/**
	 * Get the default values of this configuration.
	 *
	 * @return Map
	 */
	@AvailableSince("1.0.0")
	public Map<String, Object> getDefaultValues() {
		return defaultValues;
	}

	/**
	 * Gets the config name as a {@link Component}.
	 *
	 * @return Component
	 */
	@AvailableSince("1.0.0")
	public Component getConfigName() {
		return Component.translatable(configName);
	}

	/**
	 * Gets the mod ID.
	 *
	 * @return String
	 */
	@AvailableSince("1.0.0")
	public String getModId() {
		return modId;
	}

	/**
	 * Returns whether the config is client-only.
	 *
	 * @return boolean
	 */
	@AvailableSince("1.0.0")
	public boolean isClientOnly() {
		return clientOnly;
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