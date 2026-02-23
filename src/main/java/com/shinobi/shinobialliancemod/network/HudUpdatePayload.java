package com.shinobi.shinobialliancemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record HudUpdatePayload(
    String village,
    String rank,
    int points,
    int claimed,
    int limit
) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<HudUpdatePayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.parse("shinobialliancemod:hud_update"));

    public static final StreamCodec<FriendlyByteBuf, HudUpdatePayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeUtf(payload.village);
            buf.writeUtf(payload.rank);
            buf.writeInt(payload.points);
            buf.writeInt(payload.claimed);
            buf.writeInt(payload.limit);
        },
        buf -> new HudUpdatePayload(
            buf.readUtf(),
            buf.readUtf(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
