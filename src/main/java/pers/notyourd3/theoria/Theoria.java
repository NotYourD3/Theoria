package pers.notyourd3.theoria;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import pers.notyourd3.theoria.client.gui.TheoriaScreen;
import pers.notyourd3.theoria.command.TheoryCommand;
import pers.notyourd3.theoria.item.ModCreativeModeTab;
import pers.notyourd3.theoria.item.ModDataComponents;
import pers.notyourd3.theoria.item.ModItems;
import pers.notyourd3.theoria.network.SyncPlayerTheoriesPayload;
import pers.notyourd3.theoria.network.SyncTheoriesPayload;
import pers.notyourd3.theoria.theory.TheoryLoader;
import pers.notyourd3.theoria.theory.TheoryManager;
import pers.notyourd3.theoria.theory.provider.TheoryProvider;

import java.util.List;
import java.util.Set;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Theoria.MODID)
public class Theoria {
    public static final String MODID = "theoria";
    private static final Logger LOGGER = LogUtils.getLogger();
    KeyMapping KEY = new KeyMapping("key.opentheoryscreen", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, KeyMapping.Category.GAMEPLAY);
    public Theoria(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::gatherData);
        modEventBus.addListener(this::setupPackets);
        modEventBus.addListener(this::onRegisterKeyMappings);
        //NeoForge.EVENT_BUS.addListener(this::player);
        ModDataComponents.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeModeTab.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
    }
    public void setupPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1");
        registrar.playToClient(SyncTheoriesPayload.TYPE, SyncTheoriesPayload.STREAM_CODEC, (packet, context) -> packet.handle(packet, context));
        registrar.playToClient(SyncPlayerTheoriesPayload.TYPE, SyncPlayerTheoriesPayload.STREAM_CODEC, (packet, context) -> packet.handle(packet, context));
    }
    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        TheoryManager.getInstance().loadTheoryData(server);
    }
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TheoryCommand.register(event.getDispatcher());
    }
    @SubscribeEvent
    public void addReloadListenerEvent(AddServerReloadListenersEvent event) {
        event.addListener(ResourceLocation.fromNamespaceAndPath(MODID,"theories"),new TheoryLoader());
    }
    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event){
        if(Minecraft.getInstance().level == null){return;}
        if(KEY.consumeClick()){
            if (Minecraft.getInstance().screen == null){
                Minecraft.getInstance().setScreen(new TheoriaScreen());
            }
        }
    }
    @SubscribeEvent
    public void onPlayerJoinWorld(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity().level().isClientSide())return;
        TheoryManager.getInstance().syncTheories();
        TheoryManager.getInstance().syncPlayerTheories(event.getEntity());
    }
    //@SubscribeEvent
    public void player1(PlayerWakeUpEvent event){//DEBUG METHOD
        if(!event.getEntity().level().isClientSide())return;
        Minecraft.getInstance().setScreen(new TheoriaScreen());
    }
    public void gatherData(GatherDataEvent.Client event) {
        event.createProvider((output,provider) -> new TheoryProvider(output));
    }
    public void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY);
    }
}
