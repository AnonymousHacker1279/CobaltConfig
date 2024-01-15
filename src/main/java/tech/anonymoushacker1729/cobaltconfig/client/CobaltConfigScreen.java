package tech.anonymoushacker1729.cobaltconfig.client;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;
import org.jetbrains.annotations.Nullable;
import tech.anonymoushacker1729.cobaltconfig.CobaltConfig;
import tech.anonymoushacker1729.cobaltconfig.client.CobaltConfigScreen.ConfigList.ConfigFileEntry;
import tech.anonymoushacker1729.cobaltconfig.client.CobaltConfigScreen.ConfigValueList.ConfigValueEntry;
import tech.anonymoushacker1729.cobaltconfig.config.ConfigManager;

import java.lang.reflect.Type;
import java.util.*;

public class CobaltConfigScreen extends Screen {

	private final Screen parent;
	private final String modId;
	@Nullable
	private ConfigValueList configValueList;
	@Nullable
	private MultiLineTextWidget errorWidget;
	@Nullable
	private MultiLineTextWidget splashTextWidget;
	private double previousGuiScale = 0d;

	private static final Gson gson = new Gson();

	protected CobaltConfigScreen(Screen parent, String modId) {
		super(Component.translatable(modId + ".cobaltconfig.title"));

		this.parent = parent;
		this.modId = modId;

		minecraft = parent.getMinecraft();

		previousGuiScale = getMinecraft().getWindow().getGuiScale();
		getMinecraft().getWindow().setGuiScale(2.0d);
	}

	public static Screen getScreen(Screen parent, String modId) {
		return new CobaltConfigScreen(parent, modId);
	}

	@Override
	public void onClose() {
		getMinecraft().getWindow().setGuiScale(previousGuiScale);
		getMinecraft().setScreen(parent);
	}

	@Override
	protected void init() {
		super.init();

		if (minecraft == null) {
			return;
		}

		List<ConfigManager> configManagers = ConfigManager.getManagers(modId, false);

		// Add a list of config classes
		ConfigList configList = this.addRenderableWidget(new ConfigList(minecraft, 175, minecraft.getWindow().getGuiScaledHeight(), 7, 20, configManagers));

		// Add a save button
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {

			boolean failed = false;

			if (configList.getSelected() != null && configValueList != null) {
				ConfigManager configManager = configList.getSelected().getConfigManager();

				// Create an empty map to store the config values
				Map<String, Object> configValues = new HashMap<>(30);

				// Iterate over the ConfigValueEntry objects in the ConfigValueList
				for (ConfigValueEntry entry : configValueList.children()) {
					// Get the value from the valueField or valueButton, depending on the type of the value
					Object value;
					if (entry.valueField != null) {
						String valueString = entry.valueField.getValue();
						if (entry.getValueType() == Integer.class) {
							value = Integer.parseInt(valueString);
						} else if (entry.getValueType() == Double.class) {
							value = Double.parseDouble(valueString);
						} else if (entry.getValueType() == Float.class) {
							value = Float.parseFloat(valueString);
						} else if (entry.getValueType() == Boolean.class) {
							value = Boolean.parseBoolean(valueString);
						} else if (List.class.isAssignableFrom(entry.getValueType())) {
							Type type = new TypeToken<List<?>>() {}.getType();
							try {
								value = gson.fromJson(valueString, type);
							} catch (JsonSyntaxException e) {
								CobaltConfig.LOGGER.error("Failed to save config due to invalid JSON syntax in list: " + valueString);
								failed = true;
								break;
							}
						} else if (Map.class.isAssignableFrom(entry.getValueType())) {
							Type type = new TypeToken<Map<String, ?>>() {}.getType();
							try {
								value = gson.fromJson(valueString, type);
							} catch (JsonSyntaxException e) {
								CobaltConfig.LOGGER.error("Failed to save config due to invalid JSON syntax in map: " + valueString);
								failed = true;
								break;
							}
						} else {
							value = valueString;
						}
					} else if (entry.valueButton != null) {
						value = entry.valueButton.getMessage().equals(CommonComponents.OPTION_ON);
					} else if (entry.valueSlider != null) {
						if (entry.getValueType() == Integer.class) {
							value = entry.valueSlider.getValueInt();
						} else {
							value = entry.valueSlider.getValue();
						}
					} else {
						continue;
					}

					// Add the key-value pair to the map
					configValues.put(entry.key, value);
				}

				if (!failed) {
					ConfigManager.loadFromMap(configManager.getConfigClass().getName(), configValues);
					configManager.writeChanges();

					CobaltConfig.LOGGER.info("Saved config for " + configManager.getConfigName().getString() + " [" + configManager.getModId() + "]");

					onClose();
				} else {
					if (errorWidget != null) {
						errorWidget.setMessage(Component.nullToEmpty(null));
					}

					errorWidget = new MultiLineTextWidget((minecraft.getWindow().getGuiScaledWidth() / 2 + 4) + 155,
							minecraft.getWindow().getGuiScaledHeight() - 48,
							Component.translatable("cobaltconfig.screen.json_error")
									.withStyle(ChatFormatting.ITALIC, ChatFormatting.RED),
							minecraft.font);
					errorWidget.setMaxWidth(256);

					addRenderableWidget(errorWidget);
				}
			}

			if (!failed) {
				onClose();
			}
		}).bounds(this.minecraft.getWindow().getGuiScaledWidth() / 2 + 4, minecraft.getWindow().getGuiScaledHeight() - 28, 150, 20).build());

		// Add a cancel button
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
			onClose();
		}).bounds(minecraft.getWindow().getGuiScaledWidth() / 2 - 154, minecraft.getWindow().getGuiScaledHeight() - 28, 150, 20).build());

		// Add a splash text to the center of the screen
		splashTextWidget = new MultiLineTextWidget((minecraft.getWindow().getGuiScaledWidth() / 2),
				(minecraft.getWindow().getGuiScaledHeight() / 2),
				Component.translatable("cobaltconfig.screen.splash_text")
						.withStyle(ChatFormatting.ITALIC, ChatFormatting.BLUE),
				minecraft.font);
		splashTextWidget.setMaxWidth(128);
		splashTextWidget.setCentered(true);

		addRenderableWidget(splashTextWidget);
	}

	public class ConfigList extends ContainerObjectSelectionList<ConfigFileEntry> {

		public ConfigList(Minecraft minecraft, int width, int height, int y, int itemHeight, List<ConfigManager> configManagers) {
			super(minecraft, width, height, y, itemHeight);

			setX(7);

			// Add entries to the list
			for (int i = 0; i < configManagers.size(); i++) {
				this.addEntry(new ConfigFileEntry(configManagers.get(i), i));
			}
		}

		public class ConfigFileEntry extends Entry<ConfigFileEntry> {

			private final Button button;
			private final ConfigManager configManager;

			public ConfigFileEntry(ConfigManager configManager, int index) {
				this.configManager = configManager;

				this.button = Button.builder(configManager.getConfigName(), button -> {
							// Reload the config from file in case it was changed externally
							configManager.reload(configManager.getConfigClass());
							Map<String, Object> configValues = ConfigManager.classToMap(configManager.getConfigClass());

							if (configValues != null) {
								if (configValueList != null) {
									configValueList.clearEntries();
								}

								configValueList = addRenderableWidget(new ConfigValueList(minecraft, minecraft.getWindow().getGuiScaledHeight() - 60, 7, 25, configValues, configManager));
								configValueList.setFocused(true);

								// Hide the splash text
								if (splashTextWidget != null) {
									splashTextWidget.setMessage(Component.nullToEmpty(null));
								}
							}
						})
						.bounds(getX() + 7, 10 + (index + 20), 160, 20)
						.tooltip(Tooltip.create(configManager.isClientOnly() ? Component.translatable("cobaltconfig.screen.config_button.client_config") : Component.translatable("cobaltconfig.screen.config_button.common_config")))
						.build();
			}

			@Override
			public void render(GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick) {
				button.setY(pTop);
				button.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return Lists.newArrayList(button);
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return Lists.newArrayList(button);
			}

			public ConfigManager getConfigManager() {
				return configManager;
			}
		}
	}

	public class ConfigValueList extends ContainerObjectSelectionList<ConfigValueEntry> {

		Set<String> encounteredGroups = new HashSet<>(15);

		public ConfigValueList(Minecraft minecraft, int height, int y, int itemHeight, Map<String, Object> configValues, ConfigManager configManager) {
			super(minecraft, minecraft.getWindow().getGuiScaledWidth() - 214, height, y, itemHeight);
			Class<?> configClass = configManager.getConfigClass();

			setX(200);

			Map<String, List<Map.Entry<String, Object>>> sortedEntries = new LinkedHashMap<>(30);
			for (Map.Entry<String, Object> entry : configValues.entrySet()) {
				String group = ConfigManager.getGroup(configClass, entry.getKey());
				sortedEntries.computeIfAbsent(group, k -> new ArrayList<>(5)).add(entry);
			}

			int i = 0;
			int groups = 1;
			for (Map.Entry<String, List<Map.Entry<String, Object>>> groupEntry : sortedEntries.entrySet()) {
				int additionalHeight = 0;
				if (!groupEntry.getKey().isEmpty()) {
					additionalHeight = (20 * groups);
					groups++;
				}

				// Add all the entries of the group
				for (Map.Entry<String, Object> entry : groupEntry.getValue()) {
					this.addEntry(new ConfigValueEntry(entry.getKey(), entry.getValue(), i++, configClass, configManager, additionalHeight));
				}
			}
		}

		@Override
		protected int getScrollbarPosition() {
			// Should be at the far right of the screen minus 14 pixels
			return minecraft.getWindow().getGuiScaledWidth() - 14;
		}

		@Override
		public int getMaxScroll() {
			int totalHeight = 0;
			for (ConfigValueEntry entry : this.children()) {
				totalHeight += 20; // Assuming each entry has a height of 20
				if (entry.groupTextWidget != null) {
					totalHeight += 15; // Assuming the group text widget has a height of 15
				}
				if (entry.commentWidget != null) {
					totalHeight += 10; // Assuming the comment widget has a height of 10
				}
			}

			return (totalHeight - minecraft.getWindow().getGuiScaledHeight()) / 2;
		}

		@Override
		protected void clearEntries() {
			clearWidgets();
			init();
			super.clearEntries();
		}

		@Override
		public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
			// Only allow scrolling if there are enough elements to have a scrollbar
			if (getMaxScroll() > 0) {
				return super.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY);
			} else {
				return false;
			}
		}

		public class ConfigValueEntry extends Entry<ConfigValueEntry> {

			private final MultiLineTextWidget textWidget;
			@Nullable
			private EditBox valueField;
			@Nullable
			private Button valueButton;
			@Nullable
			private CustomSliderWidget valueSlider;
			private final SpriteIconButton resetButton;
			@Nullable
			private SpriteIconButton restartRequiredButton;
			@Nullable
			private MultiLineTextWidget commentWidget;
			@Nullable
			private MultiLineTextWidget groupTextWidget;
			private final Class<?> valueType;
			private final String key;
			private final int additionalHeight;

			public ConfigValueEntry(String key, Object value, int index, Class<?> configClass, ConfigManager configManager, int additionalHeight) {
				this.valueType = value.getClass();
				this.key = key;
				this.additionalHeight = additionalHeight;

				Component keyComponent = Component.translatable(modId + ".cobaltconfig." + key);
				int elementY = (10 + index * 20) + additionalHeight;
				textWidget = new MultiLineTextWidget(215, elementY, keyComponent, minecraft.font);
				textWidget.setMaxWidth(512);

				int widgetX = minecraft.getWindow().getGuiScaledWidth() - 196; // box width of 175 + padding of 21 = 196
				if (value instanceof Boolean) {
					// Create a button for boolean values
					valueButton = Button.builder((Boolean) value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF, (button) -> {
						boolean currentValue = button.getMessage().equals(CommonComponents.OPTION_ON);
						button.setMessage(currentValue ? CommonComponents.OPTION_OFF : CommonComponents.OPTION_ON);
					}).bounds(widgetX, elementY, 175, 20).build();
					addRenderableWidget(valueButton);
				} else if (value instanceof Integer || value instanceof Double || value instanceof Float) {
					double min = ConfigManager.getMin(configClass, key);
					double max = ConfigManager.getMax(configClass, key);

					if (min != Double.MIN_VALUE || max != Double.MAX_VALUE) {
						if (value instanceof Integer i) {
							valueSlider = new CustomSliderWidget(widgetX, elementY, 175, 20, Component.nullToEmpty(""), min, max, i, valueType);
						} else if (value instanceof Float f) {
							valueSlider = new CustomSliderWidget(widgetX, elementY, 175, 20, Component.nullToEmpty(""), min, max, f, valueType);
						} else {
							valueSlider = new CustomSliderWidget(widgetX, elementY, 175, 20, Component.nullToEmpty(""), min, max, (double) value, valueType);
						}

						addRenderableWidget(valueSlider);
					} else {
						valueField = new EditBox(minecraft.font, widgetX, elementY, 175, 20, Component.nullToEmpty(""));
						valueField.setMaxLength(9999);
						valueField.setValue(value.toString());

						// Set the filter based on the type of the value
						if (value instanceof Integer) {
							valueField.setFilter((str) -> str.matches("\\d*"));
						} else if (value instanceof Double) {
							valueField.setFilter((str) -> str.matches("\\d*(\\.\\d*)?"));
						}

						addRenderableWidget(valueField);
					}
				} else {
					// Create an EditBox for other types of values
					valueField = new EditBox(minecraft.font, widgetX, elementY, 175, 20, Component.nullToEmpty(""));
					valueField.setMaxLength(9999);

					if (Map.class.isAssignableFrom(valueType)) {
						valueField.setValue(gson.toJson(value));
					} else {
						valueField.setValue(value.toString());
					}

					addRenderableWidget(valueField);
				}

				// Add a reset button to the left of the value widget
				resetButton = SpriteIconButton.builder(Component.translatable("cobaltconfig.screen.reset_button"), (button) -> {
							Map<String, Object> defaultValues = configManager.getDefaultValues();
							Object defaultValue = defaultValues.get(key);

							if (defaultValue != null) {
								if (valueField != null) {
									if (Map.class.isAssignableFrom(valueType)) {
										Type type = new TypeToken<Map<String, ?>>() {}.getType();
										valueField.setValue(gson.toJson(defaultValue, type));
									} else {
										valueField.setValue(defaultValue.toString());
									}
								} else if (valueButton != null) {
									valueButton.setMessage((Boolean) defaultValue ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
								} else if (valueSlider != null) {
									if (defaultValue instanceof Integer i) {
										valueSlider.setValue(i);
									} else if (defaultValue instanceof Float f) {
										valueSlider.setValue(f);
									} else {
										valueSlider.setValue((double) defaultValue);
									}
								}
							}

						}, true)
						.sprite(new ResourceLocation(CobaltConfig.MOD_ID, "icon/reset"), 12, 12)
						.size(20, 20)
						.build();
				resetButton.setPosition(widgetX - 21, elementY);
				resetButton.setTooltip(Tooltip.create(Component.translatable("cobaltconfig.screen.reset_button")));
				addRenderableWidget(resetButton);

				// Add a restart required button if this entry needs it
				if (ConfigManager.getRestartRequired(configClass, key)) {
					restartRequiredButton = SpriteIconButton.builder(Component.translatable("cobaltconfig.screen.restart_required_button"), (button) -> {}, true)
							.sprite(new ResourceLocation(CobaltConfig.MOD_ID, "icon/restart_required"), 12, 12)
							.size(20, 20)
							.build();
					restartRequiredButton.setPosition(widgetX - 42, elementY);
					restartRequiredButton.setTooltip(Tooltip.create(Component.translatable("cobaltconfig.screen.restart_required_button")));
					addRenderableWidget(restartRequiredButton);
				}

				// Get the comment or translatable comment from the ConfigEntry annotation
				String comment = ConfigManager.getComment(configClass, key);
				Component commentComponent = null;

				if (comment != null && !comment.isEmpty()) {
					commentComponent = Component.translatable(comment);
				}

				if (commentComponent != null) {
					commentComponent = commentComponent.copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
					commentWidget = new MultiLineTextWidget(225, textWidget.getY(), commentComponent, minecraft.font) {
						@Override
						public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
							float textCenterX = this.getX() + this.width / 2.0F;
							float textCenterY = this.getY() + this.height / 2.0F;

							pGuiGraphics.pose().translate(textCenterX, textCenterY, 0);
							pGuiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
							pGuiGraphics.pose().translate(-textCenterX, -textCenterY, 0);
							super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

							pGuiGraphics.pose().setIdentity();
						}
					};
					commentWidget.setMaxWidth(512);
					addRenderableWidget(commentWidget);
				}

				// Create the group text widget if this entry is the first one in its group
				String group = ConfigManager.getGroup(configClass, key);
				if (isFirstInGroup(group)) {
					groupTextWidget = new MultiLineTextWidget(150 + (ConfigValueList.this.width / 2), elementY, Component.translatable(group), minecraft.font);
					groupTextWidget.setMaxWidth(512);
					addRenderableWidget(groupTextWidget);
				}
			}

			public boolean isFirstInGroup(String group) {
				if (encounteredGroups.contains(group)) {
					return false;
				} else {
					encounteredGroups.add(group);
					return true;
				}
			}

			@Override
			public void render(GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick) {
				int newY = pTop + additionalHeight - (int) ConfigValueList.this.getScrollAmount();

				// If widgets are outside the viewing area, hide them
				if (newY < 7 || newY > ConfigValueList.this.height - pHeight || newY > ConfigValueList.this.height) {
					for (AbstractWidget widget : getActiveWidgets()) {
						hideWidgets(widget);
					}

					return;
				} else {
					for (AbstractWidget widget : getActiveWidgets()) {
						widget.visible = true;
					}
				}

				// Adjust the y position based on whether the group text widget exists
				if (groupTextWidget != null && !groupTextWidget.getMessage().getString().isEmpty()) {
					groupTextWidget.setY(newY - 15);
					groupTextWidget.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
				}

				textWidget.setY(newY);
				textWidget.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

				if (commentWidget != null) {
					// Scale comments to 50% size
					int commentY = textWidget.getY() + textWidget.getHeight() + 1;
					commentWidget.setY(commentY);
					commentWidget.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
				}

				if (valueField != null) {
					valueField.setY(newY);
					valueField.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
				}

				if (valueButton != null) {
					valueButton.setY(newY);
					valueButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
				}

				if (valueSlider != null) {
					valueSlider.setY(newY);
					valueSlider.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
				}

				resetButton.setY(newY);
				resetButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

				if (restartRequiredButton != null) {
					restartRequiredButton.setY(newY);
					restartRequiredButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
				}
			}

			@Override
			public List<? extends GuiEventListener> children() {
				return getActiveWidgets();
			}

			@Override
			public List<? extends NarratableEntry> narratables() {
				return getActiveWidgets();
			}

			private List<? extends AbstractWidget> getActiveWidgets() {
				List<AbstractWidget> widgets = new ArrayList<>(10);
				widgets.add(textWidget);
				widgets.add(resetButton);
				if (commentWidget != null) {
					widgets.add(commentWidget);
				}
				if (groupTextWidget != null) {
					widgets.add(groupTextWidget);
				}
				if (valueField != null) {
					widgets.add(valueField);
				}
				if (valueButton != null) {
					widgets.add(valueButton);
				}
				if (valueSlider != null) {
					widgets.add(valueSlider);
				}
				if (restartRequiredButton != null) {
					widgets.add(restartRequiredButton);
				}
				return widgets;
			}

			public Class<?> getValueType() {
				return valueType;
			}
		}
	}

	public static class CustomSliderWidget extends ExtendedSlider {

		final Class<?> type;

		public CustomSliderWidget(int x, int y, int width, int height, Component text, double minValue, double maxValue, int value, Class<?> type) {
			super(x, y, width, height, text, Component.empty(), minValue, maxValue, value, true);
			this.type = type;
		}

		public CustomSliderWidget(int x, int y, int width, int height, Component text, double minValue, double maxValue, double value, Class<?> type) {
			super(x, y, width, height, text, Component.empty(), minValue, maxValue, value, 0.05D, 0, true);
			this.type = type;
		}
	}
}