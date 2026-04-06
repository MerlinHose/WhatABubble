package de.mmerlin.mmxp.whatabubble.network.packet;

import de.mmerlin.mmxp.whatabubble.mode.BubbleMode;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record ModeSyncPacket(UUID playerUuid, BubbleMode mode) implements CustomPayload {

    public static final CustomPayload.Id<ModeSyncPacket> PACKET_ID =
            new CustomPayload.Id<>(Identifier.of("whatabubble", "mode_sync"));

    public static final PacketCodec<PacketByteBuf, ModeSyncPacket> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeUuid(value.playerUuid());
                buf.writeString(value.mode().name(), 32);
            },
            buf -> new ModeSyncPacket(buf.readUuid(), BubbleMode.valueOf(buf.readString(32)))
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}


