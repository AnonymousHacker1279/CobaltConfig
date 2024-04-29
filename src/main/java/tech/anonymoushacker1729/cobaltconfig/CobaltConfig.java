package tech.anonymoushacker1729.cobaltconfig;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.slf4j.Logger;
import tech.anonymoushacker1729.cobaltconfig.config.ConfigManager;
import tech.anonymoushacker1729.cobaltconfig.network.handler.ConfigSyncPayloadHandler;
import tech.anonymoushacker1729.cobaltconfig.network.payload.ConfigSyncPayload;
import tech.anonymoushacker1729.cobaltconfig.network.task.ConfigSyncConfigurationTask;

import java.util.Map;

@Mod(CobaltConfig.MOD_ID)
public class CobaltConfig {

	public static final String MOD_ID = "cobaltconfig";
	public static final Logger LOGGER = LogUtils.getLogger();

	public CobaltConfig(IEventBus modEventBus) {
		LOGGER.info("Cobalt Config is starting");

		modEventBus.addListener(this::registerGameConfigurationEvent);
		modEventBus.addListener(this::registerPayloadHandlerEvent);

		NeoForge.EVENT_BUS.addListener(this::reloadEvent);
		NeoForge.EVENT_BUS.addListener(this::clientPlayerNetworkLoggingOutEvent);
	}

	public void registerGameConfigurationEvent(final RegisterConfigurationTasksEvent event) {
		LOGGER.info("Registering game configuration tasks");

		event.register(new ConfigSyncConfigurationTask(event.getListener()));
	}

	public void registerPayloadHandlerEvent(RegisterPayloadHandlersEvent event) {
		LOGGER.info("Registering packet payload handlers");

		String version = ModList.get()
				.getModContainerById(MOD_ID)
				.map(ModContainer::getModInfo)
				.map(IModInfo::getVersion)
				.map(ArtifactVersion::toString)
				.orElse("[UNKNOWN]");

		PayloadRegistrar registrar = event.registrar(version);

		registrar.commonToClient(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC, ConfigSyncPayloadHandler.getInstance()::handleData);
	}

	public void reloadEvent(AddReloadListenerEvent event) {
		if (ServerLifecycleHooks.getCurrentServer() != null) {
			LOGGER.info("Reloading all configuration files");
			ConfigManager.reloadAll();

			// Send update packets to clients
			ConfigManager.getManagers(true).forEach(manager -> {
				String configClassName = manager.getConfigClass().getName();
				Map<String, Object> configValues = ConfigManager.classToMap(manager.getConfigClass());

				if (configValues != null) {
					final ConfigSyncPayload payload = new ConfigSyncPayload(configClassName, configValues);
					PacketDistributor.sendToAllPlayers(payload);
				}
			});
		}
	}

	public void clientPlayerNetworkLoggingOutEvent(ClientPlayerNetworkEvent.LoggingOut event) {
		// Reload all configuration files in case a server was using different values
		ConfigManager.reloadAll();
	}
}