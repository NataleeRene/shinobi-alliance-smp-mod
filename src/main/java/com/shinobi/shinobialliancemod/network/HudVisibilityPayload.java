package com.shinobi.shinobialliancemod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HudVisibilityPayload(boolean visible) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<HudVisibilityPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.parse("shinobialliancemod:hud_visibility"));

    public static final StreamCodec<FriendlyByteBuf, HudVisibilityPayload> CODEC = StreamCodec.of(
        (buf, payload) -> buf.writeBoolean(payload.visible),
        buf -> new HudVisibilityPayload(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
