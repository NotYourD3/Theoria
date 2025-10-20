package pers.notyourd3.theoria.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import pers.notyourd3.theoria.Theoria;
import pers.notyourd3.theoria.client.ClientTheoryManager;
import pers.notyourd3.theoria.theory.Theory;
import pers.notyourd3.theoria.theory.TheoryCategory;

import java.util.List;

public record SyncTheoriesPayload(List<TheoryCategory> categories, List<Theory> theories) implements CustomPacketPayload, IPayloadHandler<SyncTheoriesPayload> {
    public static final Type<SyncTheoriesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Theoria.MODID, "sync_theories"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTheoriesPayload> STREAM_CODEC = StreamCodec.composite(
            TheoryCategory.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SyncTheoriesPayload::categories,
            Theory.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SyncTheoriesPayload::theories,
            SyncTheoriesPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(SyncTheoriesPayload packet, IPayloadContext iPayloadContext) {
        if(iPayloadContext.flow().isClientbound()){
            ClientTheoryManager.getInstance().sync(packet.categories(), packet.theories());
        }
    }
}