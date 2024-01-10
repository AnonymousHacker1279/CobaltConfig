package tech.anonymoushacker1729.cobaltconfig.network.task;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import tech.anonymoushacker1729.cobaltconfig.CobaltConfig;
import tech.anonymoushacker1729.cobaltconfig.config.ConfigManager;
import tech.anonymoushacker1729.cobaltconfig.network.payload.ConfigSyncPayload;

import java.util.Map;
import java.util.function.Consumer;

public record ConfigSyncConfigurationTask(
		ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
	public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(new ResourceLocation(CobaltConfig.MOD_ID, "config_sync"));

	@Override
	public void run(final Consumer<CustomPacketPayload> sender) {
		for (ConfigManager manager : ConfigManager.getManagers(true)) {
			String configClassName = manager.getConfigClass().getName();
			Map<String, Object> configValues = ConfigManager.classToMap(manager.getConfigClass());

			if (configValues != null) {
				final ConfigSyncPayload payload = new ConfigSyncPayload(configClassName, configValues);
				sender.accept(payload);
			}
		}

		listener.finishCurrentTask(type());
	}

	@Override
	public ConfigurationTask.Type type() {
		return TYPE;
	}
}