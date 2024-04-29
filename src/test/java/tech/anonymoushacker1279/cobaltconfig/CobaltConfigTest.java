package tech.anonymoushacker1279.cobaltconfig;

import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
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
			LOGGER.debug("testFloatOption: " + TestConfigEntries.testFloatOption);
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
					.setConfigName("Test Common Config")
					.build();
			new ConfigBuilder(CobaltConfig.MOD_ID, "test-client", TestConfigClientEntries.class)
					.setConfigName("Test Client Config")
					.setClientOnly(true)
					.build();

			new ConfigBuilder("minecraft", "test", TestConfigEntries.class)
					.setConfigName("Test MC Common Config")
					.build();

			// TODO: remove
			new ConfigBuilder(CobaltConfig.MOD_ID, "big-screen-test", LargeEntriesTest.class)
					.setConfigName("Large Entries")
					.build();
		}
	}

	@EventBusSubscriber(modid = CobaltConfig.MOD_ID, bus = Bus.MOD, value = Dist.CLIENT)
	public static class ClientModEventSubscriber {

		@SubscribeEvent
		public static void constructMod(FMLConstructModEvent event) {
			ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
					() -> (minecraft, modListScreen) -> CobaltConfigScreen.getScreen(modListScreen, CobaltConfig.MOD_ID));
		}
	}

	@EventBusSubscriber(modid = CobaltConfig.MOD_ID, bus = Bus.GAME)
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

		@ConfigEntry(comment = "This is a float config option", min = 5.0, max = 15.0)
		public static float testFloatOption = 10.0f;

		@ConfigEntry(comment = "This is a double config option", min = 5.0, max = 15.0)
		public static double testDoubleOption = 10.0d;

		@ConfigEntry(comment = "This is an int config option with no bounds")
		public static int testIntOptionNoBounds = 50;

		@ConfigEntry(comment = "This is a double config option with no bounds")
		public static double testDoubleOptionNoBounds = 10.0d;

		@ConfigEntry(comment = "This is a boolean config option")
		public static boolean testBoolean = true;

		@ConfigEntry(comment = "This is a boolean config option that requires a restart", restartRequired = true)
		public static boolean testBooleanRestartRequired = false;

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

	// TODO: remove
	@SuppressWarnings("unused")
	public static class LargeEntriesTest {
		@ConfigEntry(comment = "Enable the Tiltros dimension portal", group = "General")
		public static boolean tiltrosEnabled = true;

		@ConfigEntry(comment = "Force the number of particles produced by the smoke grenade to be the same on all clients. A value of -1 will allow clients to define their own values.", group = "General", min = -1, max = 256)
		public static int forceSmokeGrenadeParticles = -1;

		@ConfigEntry(comment = "Set the maximum armor protection cap. The vanilla default is 20, but higher values allow better armor to work as intended. 25 is fully unlocked.", group = "Combat Rules", min = 20, max = 25)
		public static double maxArmorProtection = 25.0d;

		@ConfigEntry(comment = "Set the range for checking criteria of the entity discovery advancement", group = "Entity Settings", min = 0, max = 256)
		public static int discoveryAdvancementRange = 50;

		@ConfigEntry(comment = "Set the spawn checking radius for the Celestial Tower. Higher values increase the effectiveness of Celestial Lanterns.", group = "Entity Settings", min = 0, max = 512)
		public static int celestialTowerSpawnCheckingRadius = 256;

		@ConfigEntry(comment = "Multiplier to change the wave size of Celestial Tower summons", group = "Entity Settings", min = 0, max = 5.0d)
		public static double celestialTowerWaveSizeModifier = 1.0d;

		@ConfigEntry(comment = "Specify maximum caps on enchantments to prevent the Skygazer from upgrading them past a certain level. Max level enchants appear with a gold tooltip.", group = "Entity Settings")
		public static Map<String, Integer> skygazerEnchantCaps = getEnchantCapsMap();

		@ConfigEntry(comment = "Set the chance for a fired bullet to be critical", group = "General Weapon Settings", min = 0, max = 1.0d)
		public static double gunCritChance = 0.1d;

		@ConfigEntry(comment = "Allow bullets to break glass", group = "General Weapon Settings")
		public static boolean bulletsBreakGlass = true;

		@ConfigEntry(comment = "Allow infinity-type enchantments to work on all ammo tiers. By default, it is restricted to cobalt and lower tiers.", group = "General Weapon Settings")
		public static boolean infiniteAmmoOnAllTiers = false;

		@ConfigEntry(comment = "Set the base velocity of bullets", group = "Flintlock Pistol", min = 0, max = 10)
		public static float flintlockPistolFireVelocity = 2.5f;

		@ConfigEntry(comment = "Set the inaccuracy modifier", group = "Flintlock Pistol", min = 0, max = 10)
		public static float flintlockPistolFireInaccuracy = 1.75f;

		@ConfigEntry(comment = "Set the base velocity of bullets", group = "Blunderbuss", min = 0, max = 10)
		public static float blunderbussFireVelocity = 1.7f;

		@ConfigEntry(comment = "Set the inaccuracy modifier", group = "Blunderbuss", min = 0, max = 10)
		public static float blunderbussFireInaccuracy = 2.0f;

		@ConfigEntry(comment = "Set the base velocity of bullets", group = "Musket", min = 0, max = 10)
		public static float musketFireVelocity = 4.0f;

		@ConfigEntry(comment = "Set the inaccuracy modifier", group = "Musket", min = 0, max = 10)
		public static float musketFireInaccuracy = 0.15f;

		@ConfigEntry(comment = "Set the base velocity of bullets", group = "Hand Cannon", min = 0, max = 10)
		public static float handCannonFireVelocity = 2.55f;

		@ConfigEntry(comment = "Set the inaccuracy modifier", group = "Hand Cannon", min = 0, max = 10)
		public static float handCannonFireInaccuracy = 1.85f;

		@ConfigEntry(comment = "Set the maximum range the staff can be used", group = "Meteor Staff", min = 0, max = 256)
		public static int meteorStaffMaxUseRange = 100;

		@ConfigEntry(comment = "Set the radius of the explosion created", group = "Meteor Staff", min = 0, max = 10)
		public static float meteorStaffExplosionRadius = 3.0f;

		@ConfigEntry(comment = "Allow explosions to break blocks", group = "Meteor Staff")
		public static boolean meteorStaffExplosionBreakBlocks = false;

		@ConfigEntry(comment = "Set the maximum range the staff can be used", group = "Cursed Sight Staff", min = 0, max = 256)
		public static int cursedSightStaffMaxUseRange = 50;

		@ConfigEntry(comment = "Set the maximum range the staff can be used", group = "Sculk Staff", min = 0, max = 256)
		public static int sculkStaffMaxUseRange = 15;

		@ConfigEntry(comment = "Allow the sonic blast to travel through blocks", group = "Sculk Staff")
		public static boolean sculkStaffSonicBlastThroughBlocks = true;

		@ConfigEntry(comment = "Set the maximum range the staff can be used", group = "Recovery Staff", min = 0, max = 256)
		public static int recoveryStaffMaxUseRange = 15;

		private static Map<String, Integer> getEnchantCapsMap() {
			Map<String, Integer> enchantCaps = new HashMap<>(40);
			enchantCaps.put("minecraft:mending", 1);
			enchantCaps.put("minecraft:silk_touch", 1);
			enchantCaps.put("minecraft:knockback", 5);
			enchantCaps.put("minecraft:punch", 5);
			enchantCaps.put("minecraft:flame", 1);
			enchantCaps.put("minecraft:infinity", 1);
			enchantCaps.put("minecraft:channeling", 1);
			enchantCaps.put("minecraft:multishot", 1);
			enchantCaps.put("minecraft:protection", 5);
			enchantCaps.put("minecraft:blast_protection", 5);
			enchantCaps.put("minecraft:fire_protection", 5);
			enchantCaps.put("minecraft:projectile_protection", 5);
			enchantCaps.put("minecraft:feather_falling", 5);
			enchantCaps.put("minecraft:swift_sneak", 5);
			enchantCaps.put("minecraft:lure", 5);
			enchantCaps.put("minecraft:aqua_affinity", 1);
			enchantCaps.put("immersiveweapons:extended_reach", 1);
			enchantCaps.put("immersiveweapons:endless_musket_pouch", 1);
			enchantCaps.put("immersiveweapons:scorch_shot", 3);
			enchantCaps.put("immersiveweapons:velocity", 5);
			enchantCaps.put("immersiveweapons:impact", 5);
			enchantCaps.put("immersiveweapons:burning_heat", 1);
			enchantCaps.put("immersiveweapons:celestial_fury", 1);
			enchantCaps.put("immersiveweapons:heavy_comet", 4);
			enchantCaps.put("immersiveweapons:regenerative_assault", 5);
			enchantCaps.put("immersiveweapons:malevolent_gaze", 5);
			return enchantCaps;
		}
	}
}