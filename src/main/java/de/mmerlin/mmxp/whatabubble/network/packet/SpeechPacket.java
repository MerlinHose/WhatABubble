package de.mmerlin.mmxp.whatabubble.network.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record SpeechPacket(UUID playerUuid, String languageCode, String text) implements CustomPayload {

    public static final CustomPayload.Id<SpeechPacket> PACKET_ID =
            new CustomPayload.Id<>(Identifier.of("whatabubble", "speech"));

    public static final PacketCodec<PacketByteBuf, SpeechPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeUuid(value.playerUuid());
                buf.writeString(value.languageCode(), 16);
                buf.writeString(value.text(), 512);
            },
            buf -> new SpeechPacket(buf.readUuid(), buf.readString(16), buf.readString(512))
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}


