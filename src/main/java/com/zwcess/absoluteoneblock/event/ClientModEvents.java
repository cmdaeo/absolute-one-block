package com.zwcess.absoluteoneblock.event;

import com.zwcess.absoluteoneblock.AbsoluteOneBlock;
import com.zwcess.absoluteoneblock.client.OneBlockHudOverlay;
import com.zwcess.absoluteoneblock.core.Registration;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = AbsoluteOneBlock.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEvents {
    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("oneblock_progress", OneBlockHudOverlay.HUD_ONEBLOCK);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MenuScreens.register(Registration.PLATFORM_BUILDER_TOOL_MENU.get(),
                com.zwcess.absoluteoneblock.client.PlatformBuilderToolScreen::new);
    }
}
