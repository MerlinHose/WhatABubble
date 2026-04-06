package de.mmerlin.mmxp.whatabubble.network;
import de.mmerlin.mmxp.whatabubble.network.packet.ModeSyncPacket;
import de.mmerlin.mmxp.whatabubble.network.packet.SpeechPacket;
import de.mmerlin.mmxp.whatabubble.util.ModLogger;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
public class NetworkHandler {
    private static final double BROADCAST_RANGE_SQ = 32.0 * 32.0;

    public void registerPayloadTypes() {
        ModLogger.info("Registering payload types...");
        PayloadTypeRegistry.playC2S().register(SpeechPacket.PACKET_ID, SpeechPacket.CODEC);
        ModLogger.info("Registered SpeechPacket C2S");
        PayloadTypeRegistry.playS2C().register(SpeechPacket.PACKET_ID, SpeechPacket.CODEC);
        ModLogger.info("Registered SpeechPacket S2C");
        PayloadTypeRegistry.playC2S().register(ModeSyncPacket.PACKET_ID, ModeSyncPacket.CODEC);
        ModLogger.info("Registered ModeSyncPacket C2S");
        PayloadTypeRegistry.playS2C().register(ModeSyncPacket.PACKET_ID, ModeSyncPacket.CODEC);
        ModLogger.info("Registered ModeSyncPacket S2C");
        ModLogger.info("Payload types registered.");
    }

    public void registerReceivers() {
        ModLogger.info("Registering server-side packet receivers...");
        ServerPlayNetworking.registerGlobalReceiver(SpeechPacket.PACKET_ID, (payload, context) -> {
            ModLogger.info("[SpeechPacket] Received from {} (uuid={}) | lang={} | text=\"{}\"",
                    context.player().getName().getString(),
                    context.player().getUuid(),
                    payload.languageCode(),
                    payload.text());
            context.server().execute(() ->
                    broadcastToNearby(context.server(), context.player(), payload));
        });
        ServerPlayNetworking.registerGlobalReceiver(ModeSyncPacket.PACKET_ID, (payload, context) -> {
            ModLogger.info("[ModeSyncPacket] Received from {} (uuid={}) | mode={}",
                    context.player().getName().getString(),
                    context.player().getUuid(),
                    payload.mode());
            context.server().execute(() ->
                    broadcastToNearby(context.server(), context.player(), payload));
        });
        ModLogger.info("Server packet receivers registered.");
    }

    private <T extends net.minecraft.network.packet.CustomPayload> void broadcastToNearby(
            MinecraftServer server, ServerPlayerEntity sender, T payload) {
        String payloadType = payload.getClass().getSimpleName();
        int totalPlayers = server.getPlayerManager().getPlayerList().size();
        ModLogger.info("[{}] Broadcasting from {} | {} player(s) online",
                payloadType, sender.getName().getString(), totalPlayers);

        int[] sentCount = {0};
        server.getPlayerManager().getPlayerList().forEach(p -> {
            boolean isSelf = p.equals(sender);
            double distSq = isSelf ? 0.0 : sender.squaredDistanceTo(p);
            boolean inRange = isSelf || distSq <= BROADCAST_RANGE_SQ;
            if (inRange) {
                ModLogger.info("[{}]  -> Sending to {} (self={}, distSq={})",
                        payloadType, p.getName().getString(), isSelf, String.format("%.1f", distSq));
                ServerPlayNetworking.send(p, payload);
                sentCount[0]++;
            } else {
                ModLogger.info("[{}]  -- Skipping {} (distSq={} > {})",
                        payloadType, p.getName().getString(),
                        String.format("%.1f", distSq), String.format("%.1f", BROADCAST_RANGE_SQ));
            }
        });

        ModLogger.info("[{}] Broadcast from {} complete — sent to {}/{} player(s).",
                payloadType, sender.getName().getString(), sentCount[0], totalPlayers);
    }
}