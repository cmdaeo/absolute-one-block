package com.zwcess.absoluteoneblock.client;

import com.zwcess.absoluteoneblock.AbsoluteOneBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class OneBlockHudOverlay {

    @SuppressWarnings("null")
    public static final IGuiOverlay HUD_ONEBLOCK = (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ResourceLocation oneBlockDimensionLocation = new ResourceLocation(AbsoluteOneBlock.MOD_ID, "oneblock_dimension");
        if (!mc.player.level().dimension().location().equals(oneBlockDimensionLocation)) return;

        int blocksBroken = ClientOneBlockData.getBlocksBroken();
        int blocksNeeded = ClientOneBlockData.getBlocksNeeded();
        String phaseName = ClientOneBlockData.getCurrentPhaseName(); 
        String nextPhaseName = ClientOneBlockData.getNextPhaseName(); 

        String text;
        float progress = 0.0F;

        if (phaseName.equals("Infinity")) {
            text = "Infinity Phase - Total Broken: " + blocksBroken;
        } else if (blocksNeeded <= 0) {
            text = "Next Phase: " + nextPhaseName;
        } else {
            text = phaseName + " Progress: " + blocksBroken + " / " + blocksNeeded;
            progress = Mth.clamp((float) blocksBroken / blocksNeeded, 0.0F, 1.0F);
        }
        
        int barWidth = 182;
        int barHeight = 5;
        int x = (screenWidth - barWidth) / 2;
        int y = 15;

        guiGraphics.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF000000); 
        guiGraphics.fill(x, y, x + barWidth, y + barHeight, 0xFF404040); 
        if (progress > 0) {
            guiGraphics.fill(x, y, x + (int)(progress * barWidth), y + barHeight, 0xFF00FF00); 
        }
        
        int textWidth = mc.font.width(text);
        guiGraphics.drawString(mc.font, text, (screenWidth - textWidth) / 2, y - 12, 0xFFFFFFFF, true);
    };
}
