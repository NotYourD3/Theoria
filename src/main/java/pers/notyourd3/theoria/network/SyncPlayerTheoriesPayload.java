package pers.notyourd3.theoria.network;

import com.fasterxml.jackson.databind.ser.std.UUIDSerializer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import pers.notyourd3.theoria.Theoria;
import pers.notyourd3.theoria.client.ClientTheoryManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record SyncPlayerTheoriesPayload(UUID playerId, Set<ResourceLocation> unlockedTheories) implements CustomPacketPayload, IPayloadHandler<SyncPlayerTheoriesPayload> {
    public static final Type<SyncPlayerTheoriesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Theoria.MODID, "sync_player_theories"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerTheoriesPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            SyncPlayerTheoriesPayload::playerId,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)),
            SyncPlayerTheoriesPayload::unlockedTheories,
            SyncPlayerTheoriesPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(SyncPlayerTheoriesPayload packet, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            ClientTheoryManager.getInstance().setUnlockedTheories(packet.unlockedTheories());
        }
    }
}
