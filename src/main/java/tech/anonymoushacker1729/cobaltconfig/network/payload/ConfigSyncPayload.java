package tech.anonymoushacker1729.cobaltconfig.network.payload;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tech.anonymoushacker1729.cobaltconfig.CobaltConfig;

import java.lang.reflect.Type;
import java.util.Map;

public record ConfigSyncPayload(String configClassName,
                                Map<String, Object> configValues) implements CustomPacketPayload {

	public static final ResourceLocation ID = new ResourceLocation(CobaltConfig.MOD_ID, "config_sync");
	private static final Gson GSON = new Gson();
	private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
	}.getType();

	public static ConfigSyncPayload fromBuffer(final FriendlyByteBuf buffer) {
		String configClassName = buffer.readUtf();
		Map<String, Object> configValues = GSON.fromJson(buffer.readUtf(), MAP_TYPE);
		return new ConfigSyncPayload(configClassName, configValues);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUtf(configClassName);
		buffer.writeUtf(GSON.toJson(configValues, MAP_TYPE));
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}