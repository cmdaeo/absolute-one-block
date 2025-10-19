package com.zwcess.absoluteoneblock;

import com.zwcess.absoluteoneblock.command.AbsoluteCommands;
import com.zwcess.absoluteoneblock.core.ModCreativeTabs;
import com.zwcess.absoluteoneblock.core.Registration;
import com.zwcess.absoluteoneblock.game.PhaseManager;
import com.zwcess.absoluteoneblock.network.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.zwcess.absoluteoneblock.config.Config;
import net.minecraftforge.fml.config.ModConfig.Type;

@Mod(AbsoluteOneBlock.MOD_ID)
public class AbsoluteOneBlock {
    public static final String MOD_ID = "absoluteoneblock";
    private static final Logger LOGGER = LogManager.getLogger();

    public static final PhaseManager phaseManager = new PhaseManager();

    public AbsoluteOneBlock() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        Registration.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        
        MinecraftForge.EVENT_BUS.register(this);
        PacketHandler.register();

        ModLoadingContext.get().registerConfig(Type.SERVER, Config.SPEC, "absoluteoneblock-server.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Absolute One Block is initializing!");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        AbsoluteCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Corrected: The method is now named 'load'.
        phaseManager.load(event.getServer());
        LOGGER.info("Absolute One Block systems are loaded for the server.");
    }
}
