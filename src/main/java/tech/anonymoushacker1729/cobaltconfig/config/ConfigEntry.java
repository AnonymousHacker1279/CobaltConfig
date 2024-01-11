package tech.anonymoushacker1729.cobaltconfig.config;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus.AvailableSince;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@AvailableSince("1.0.0")
public @interface ConfigEntry {
	/**
	 * The class representing the config entry type.
	 */
	Class<?> type();

	/**
	 * Any comments associated with the entry.
	 */
	String comment() default "";

	/**
	 * Any comments associated with the entry that should be passed through {@link Component#translatable(String)}.
	 * <p>
	 * Translatable comments, if present, will override the value of {@link #comment()}.
	 */
	String translatableComment() default "";
}