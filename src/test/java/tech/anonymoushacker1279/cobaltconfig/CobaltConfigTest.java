package tech.anonymoushacker1279.cobaltconfig;

import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.fml.common.Mod.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import tech.anonymoushacker1729.cobaltconfig.CobaltConfig;
import tech.anonymoushacker1729.cobaltconfig.client.CobaltConfigScreen;
import tech.anonymoushacker1729.cobaltconfig.config.ConfigEntry;
import tech.anonymoushacker1729.cobaltconfig.config.ConfigManager.ConfigBuilder;

import java.util.*;

import static tech.anonymoushacker1729.cobaltconfig.CobaltConfig.LOGGER;

public class CobaltConfigTest {

	@EventBusSubscriber(modid = CobaltConfig.MOD_ID, bus = Bus.MOD)
	public static class ModEventSubscriber {

		@SubscribeEvent
		public static void setup(FMLCommonSetupEvent event) {
			LOGGER.debug("testIntOption: " + TestConfigEntries.testIntOption);
			LOGGER.debug("testDoubleOption: " + TestConfigEntries.testDoubleOption);
			LOGGER.debug("testBoolean: " + TestConfigEntries.testBoolean);
			LOGGER.debug("testList: " + TestConfigEntries.testList);
			LOGGER.debug("testMap: " + TestConfigEntries.testMap);
		}

		@SubscribeEvent
		public static void constructMod(FMLConstructModEvent event) {
			LOGGER.info("Cobalt Config Test is starting");

			// Test config
			new ConfigBuilder(CobaltConfig.MOD_ID, "test", TestConfigEntries.class)
					.setConfigName("Test Common Config", false)
					.build();
			new ConfigBuilder(CobaltConfig.MOD_ID, "test-client", TestConfigClientEntries.class)
					.setConfigName("Test Client Config", false)
					.setClientOnly(true)
					.build();
		}
	}

	@EventBusSubscriber(modid = CobaltConfig.MOD_ID, bus = Bus.MOD, value = Dist.CLIENT)
	public static class ClientModEventSubscriber {

		@SubscribeEvent
		public static void constructMod(FMLConstructModEvent event) {
			ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
					() -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> CobaltConfigScreen.getScreen(screen, CobaltConfig.MOD_ID)));
		}
	}

	@EventBusSubscriber(modid = CobaltConfig.MOD_ID, bus = Bus.FORGE)
	public static class ForgeEventSubscriber {

		@SubscribeEvent
		public static void entityJoin(EntityJoinLevelEvent event) {
			if (event.getEntity() instanceof Player) {
				LOGGER.debug("testIntOption: " + TestConfigEntries.testIntOption);
			}
		}
	}

	/**
	 * Test data which is available on both the client and server.
	 */
	@SuppressWarnings("unused")
	public static class TestConfigEntries {
		@ConfigEntry(comment = "This is an int config option", max = 100)
		public static int testIntOption = 50;

		@ConfigEntry(comment = "This is a double config option", min = 5.0, max = 15.0)
		public static double testDoubleOption = 10.0;

		@ConfigEntry(comment = "This is an int config option with no bounds")
		public static int testIntOptionNoBounds = 50;

		@ConfigEntry(comment = "This is a double config option with no bounds")
		public static double testDoubleOptionNoBounds = 10.0;

		@ConfigEntry(comment = "This is a boolean config option")
		public static boolean testBoolean = true;

		@ConfigEntry(comment = "This is a list config option", group = "Test Group")
		public static List<Integer> testList = Arrays.asList(1, 2, 3);

		@ConfigEntry(comment = "This is a map config option", group = "Test Group")
		public static Map<String, ?> testMap = new HashMap<>() {{
			put("key1", "value1");
			put("key2", 3);
		}};
	}

	/**
	 * Test data which is only available on the client.
	 */
	public static class TestConfigClientEntries {
		@ConfigEntry(comment = "This is an int config option")
		public static int testIntOption = 10;
	}
}