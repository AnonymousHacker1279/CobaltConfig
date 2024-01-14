package tech.anonymoushacker1729.cobaltconfig.config;

import org.jetbrains.annotations.ApiStatus.AvailableSince;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@AvailableSince("1.0.0")
public @interface ConfigEntry {

	/**
	 * Any comments associated with the entry. They may be plaintext or translatable.
	 */
	String comment() default "";

	/**
	 * Entries may be grouped together in the config GUI. This is the name of the group this entry belongs to.
	 */
	String group() default "";

	/**
	 * Define the minimum value for numeric entries.
	 */
	double min() default Double.MIN_VALUE;

	/**
	 * Define the maximum value for numeric entries.
	 */
	double max() default Double.MAX_VALUE;

	/**
	 * Mark that an entry requires a restart for changes to take effect.
	 */
	boolean restartRequired() default false;
}