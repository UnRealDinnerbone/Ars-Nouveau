package com.hollingsworth.arsnouveau.client.gui;

import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.api.mana.IMana;
import com.hollingsworth.arsnouveau.api.util.StackUtil;
import com.hollingsworth.arsnouveau.common.capability.ManaCapability;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class GuiManaHUD extends AbstractGui {
    private static final Minecraft minecraft = Minecraft.getInstance();

    public void drawHUD(MatrixStack ms, float pt) {
        ItemStack stack = StackUtil.getHeldSpellbook(minecraft.player);
        if(stack != null && stack.getItem() instanceof SpellBook && stack.getTag() != null){
            IMana mana = ManaCapability.getMana(minecraft.player).orElse(null);
            if(mana == null)
                return;

            int offsetLeft = 10;
            double x = 100 ; //Length
            x = (x) * ((mana.getCurrentMana()) / ((double) mana.getMaxMana() - 0.0))  + offsetLeft;

            int y = minecraft.getMainWindow().getScaledHeight() - 5;
            //int offsetLeft = minecraft.mainWindow.getScaledWidth()/2;
//
//            int offsetStop =  minecraft.getMainWindow().getScaledHeight() - 15;; // This determines the thickness from Y. Draws from Y to this value. Must be > y.
//            fill(ms,(int)offsetLeft, y, (int)100+ offsetLeft, offsetStop, 0xFF000000 | Integer.parseInt("C9CAB9", 16));
//            fillGradient(ms,(int)x, y, offsetLeft, offsetStop, 0xFF000000 | Integer.parseInt("337CFF", 16), new Color(0xFF000000 | Integer.parseInt("1145A1", 16)).darker().getRGB());
//            //fillGradient(50, 10, 8, 5,  0xFF000000 | Integer.parseInt("DDDDDD", 16), new Color(0xFF000000 | Integer.parseInt("DDDDDD", 16)).darker().getRGB());
//            for(int i = 100; i <= mana.getMaxMana(); i+=100){
//                double marker = (100) * ((i) / (new Double(mana.getMaxMana()) - 0.0))  + offsetLeft;
//                fill(ms,(int)marker, y, (int)marker+1, offsetStop, 0xFF000000 | Integer.parseInt("E4F10A", 16));
//            }

            int manaLength = 96;
            manaLength = (int) ((manaLength) * ((mana.getCurrentMana()) / ((double) mana.getMaxMana() - 0.0)));

            int height = minecraft.getMainWindow().getScaledHeight() - 5;

            Minecraft.getInstance().textureManager.bindTexture(new ResourceLocation(ArsNouveau.MODID, "textures/gui/manabar_gui_border.png"));
            blit(ms,offsetLeft, height - 18, 0, 0, 108, 18, 256, 256);
            int manaOffset = (int) (((minecraft.world.getGameTime() + pt) / 7 % (42))) * 6;

            // 96
            Minecraft.getInstance().textureManager.bindTexture(new ResourceLocation(ArsNouveau.MODID, "textures/gui/manabar_gui_mana.png"));
            blit(ms,offsetLeft + 9, height - 9, 0, manaOffset, manaLength,6, 256, 256);

            Minecraft.getInstance().textureManager.bindTexture(new ResourceLocation(ArsNouveau.MODID, "textures/gui/manabar_gui_border.png"));
            blit(ms,offsetLeft, height - 17, 0, 18, 108, 20, 256, 256);

        }

    }
}
