package tech.anonymoushacker1279.cobaltconfig;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.fml.common.Mod.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import tech.anonymoushacker1729.cobaltconfig.CobaltConfig;
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
			new ConfigBuilder(CobaltConfig.MOD_ID, "test", TestConfigEntries.class).build();
			new ConfigBuilder(CobaltConfig.MOD_ID, "test-client", TestConfigClientEntries.class).setClientOnly(true).build();
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
	public static class TestConfigEntries {
		@ConfigEntry(type = Integer.class, comment = "This is an int config option")
		public static int testIntOption = 50;

		@ConfigEntry(type = Double.class, comment = "This is a double config option")
		public static double testDoubleOption = 10.0;

		@ConfigEntry(type = Boolean.class, comment = "This is a boolean config option")
		public static boolean testBoolean = true;

		@ConfigEntry(type = List.class, comment = "This is a list config option")
		public static List<Integer> testList = Arrays.asList(1, 2, 3);

		@ConfigEntry(type = Map.class, comment = "This is a map config option")
		public static Map<String, ?> testMap = new HashMap<>() {{
			put("key1", "value1");
			put("key2", 3);
		}};
	}

	/**
	 * Test data which is only available on the client.
	 */
	public static class TestConfigClientEntries {
		@ConfigEntry(type = Integer.class, comment = "This is an int config option")
		public static int testIntOption = 10;
	}
}