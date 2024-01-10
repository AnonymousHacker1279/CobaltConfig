package tech.anonymoushacker1729.cobaltconfig.network.handler;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import tech.anonymoushacker1729.cobaltconfig.config.ConfigManager;
import tech.anonymoushacker1729.cobaltconfig.network.payload.ConfigSyncPayload;

public class ConfigSyncPayloadHandler {

	private static final ConfigSyncPayloadHandler INSTANCE = new ConfigSyncPayloadHandler();

	public static ConfigSyncPayloadHandler getInstance() {
		return INSTANCE;
	}

	public void handleData(final ConfigSyncPayload data, final IPayloadContext context) {
		context.workHandler().submitAsync(() -> {
					ConfigManager.loadFromMap(data.configClassName(), data.configValues());
				})
				.exceptionally(e -> {
					context.packetHandler().disconnect(Component.translatable("cobaltconfig.networking.failure.generic", e.getMessage()));
					return null;
				});
	}
}