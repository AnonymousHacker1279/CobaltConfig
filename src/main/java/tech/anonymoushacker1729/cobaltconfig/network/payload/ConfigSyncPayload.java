package tech.anonymoushacker1729.cobaltconfig.network.payload;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import tech.anonymoushacker1729.cobaltconfig.CobaltConfig;

import java.util.Map;

public record ConfigSyncPayload(String configClassName,
                                Map<String, Object> configValues) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<ConfigSyncPayload> TYPE = new CustomPacketPayload.Type<>(new ResourceLocation(CobaltConfig.MOD_ID, "config_sync"));
	private static final Gson GSON = new Gson();
	private static final java.lang.reflect.Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

	public static final StreamCodec<FriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC = StreamCodec.of(
			ConfigSyncPayload::toNetwork, ConfigSyncPayload::fromBuffer
	);

	public static ConfigSyncPayload fromBuffer(final FriendlyByteBuf buffer) {
		String configClassName = buffer.readUtf();
		Map<String, Object> configValues = GSON.fromJson(buffer.readUtf(), MAP_TYPE);
		return new ConfigSyncPayload(configClassName, configValues);
	}

	private static void toNetwork(final FriendlyByteBuf buffer, final ConfigSyncPayload payload) {
		buffer.writeUtf(payload.configClassName);
		buffer.writeUtf(GSON.toJson(payload.configValues, MAP_TYPE));
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}