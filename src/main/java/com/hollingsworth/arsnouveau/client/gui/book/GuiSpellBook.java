package com.hollingsworth.arsnouveau.client.gui.book;

import com.hollingsworth.arsnouveau.api.ArsNouveauAPI;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractCastMethod;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.util.SpellRecipeUtil;
import com.hollingsworth.arsnouveau.client.gui.buttons.CraftingButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.GlyphButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.GuiImageButton;
import com.hollingsworth.arsnouveau.client.gui.buttons.GuiSpellSlot;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import com.hollingsworth.arsnouveau.common.network.Networking;
import com.hollingsworth.arsnouveau.common.network.PacketUpdateSpellbook;
import com.hollingsworth.arsnouveau.setup.Config;
import com.hollingsworth.arsnouveau.setup.ItemsRegistry;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ChangePageButton;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.StringTextComponent;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GuiSpellBook extends BaseBook {

    public int numLinks = 10;
    public SpellBook spellBook;
    public ArsNouveauAPI api;

    private int selected_cast_slot;
    public TextFieldWidget spell_name;
    public CompoundNBT spell_book_tag;
    public GuiSpellSlot selected_slot;
    public int max_spell_tier; // Used to load spells that are appropriate tier
    List<CraftingButton> craftingCells;
    public List<AbstractSpellPart>unlockedSpells;
    public List<AbstractSpellPart> effects;
    public List<Widget> effectButtons;
    public int page = 0;
    ChangePageButton nextButton;
    ChangePageButton previousButton;
    public GuiSpellBook(ArsNouveauAPI api, CompoundNBT tag, int tier, String unlockedSpells) {
        super();
        this.api = api;
        this.selected_cast_slot = 1;
        craftingCells = new ArrayList<>();
        this.max_spell_tier = tier;
        this.spell_book_tag = tag;
        this.unlockedSpells = SpellRecipeUtil.getSpellsFromString(unlockedSpells);
        this.effects = this.unlockedSpells.stream().filter(a -> a instanceof AbstractEffect).collect(Collectors.toList());
        effectButtons = new ArrayList<>();
    }

    @Override
    public void init() {
        super.init();
        int selected_slot_ind = SpellBook.getMode(spell_book_tag);
        if(selected_slot_ind == 0) selected_slot_ind = 1;

        //Crafting slots
        for (int i = 0; i < numLinks; i++) {
            String icon = null;
            String spell_id = "";
            int offset = i >= 5 ? 5 : 0;
            CraftingButton cell = new CraftingButton(this,bookLeft +14 + 24 * i + offset, bookTop + FULL_HEIGHT - 52, i, this::onCraftingSlotClick);
            //GlyphButton glyphButton = new GlyphButton(this,bookLeft + 10 + 28 * i, bookTop + FULL_HEIGHT - 24, )
            addButton(cell);
            craftingCells.add(cell);
        }
        updateCraftingSlots(selected_slot_ind);

        addSpellParts(0);
        addButton(new GuiImageButton(bookRight - 70, bookBottom - 28, 0,0,46, 18, 46, 18, "textures/gui/create_button.png", this::onCreateClick));

        spell_name = new TextFieldWidget(minecraft.fontRenderer, bookLeft + 16, bookTop + FULL_HEIGHT - 25,
                115, 12, null, new StringTextComponent("Spell Name"));
        int mode = SpellBook.getMode(spell_book_tag);
        mode = mode == 0 ? 1 : mode;
        spell_name.setText(SpellBook.getSpellName(spell_book_tag, mode));
        if(spell_name.getText().isEmpty())
            spell_name.setSuggestion("My Spell");
//
        addButton(spell_name);
        // Add spell slots
        for(int i = 1; i <= 10; i++){
            GuiSpellSlot slot = new GuiSpellSlot(this,bookLeft + 261, bookTop - 3 + 15 * i, i);
            if(i == selected_slot_ind) {
                selected_slot = slot;
                selected_cast_slot = i;
                slot.isSelected = true;
            }
            addButton(slot);
        }

        addButton(new GuiImageButton(bookLeft - 15, bookTop, 0, 0, 16, 16, 16,16, "textures/items/worn_notebook.png",this::onDocumentationClick));
        addButton(new GuiImageButton(bookLeft - 15, bookTop + 15, 0, 0, 16, 16, 16,16, "textures/gui/color_wheel.png",this::onColorClick));
        this.nextButton = addButton(new ChangePageButton(bookRight -20, bookBottom -10, true, this::onPageIncrease, true));
        this.previousButton = addButton(new ChangePageButton(bookLeft - 5 , bookBottom -10, false, this::onPageDec, true));
        if(effects.size() < 36){
            nextButton.visible = false;
            nextButton.active = false;
        }else{
            nextButton.visible = true;
            nextButton.active = true;
        }
        previousButton.active = false;
        previousButton.visible = false;
    }

    public void addSpellParts(int page){
        for(Widget w : effectButtons) {
            buttons.remove(w);
            children.remove(w);
        }
        effectButtons.clear();
        Collections.sort(unlockedSpells);

        List<AbstractSpellPart> displayedEffects = effects.subList(36 * page, Math.min(effects.size(), 36 * (page + 1)));
        //Adding spell parts
        int numCast = 0;
        int numEffect = 0;
        int numAugment = 0;
        for(AbstractSpellPart key  : unlockedSpells){
            AbstractSpellPart spell = this.api.getSpell_map().get(key.tag);
            GlyphButton cell = null;
            if(spell.getTier().ordinal() > max_spell_tier)
                continue; //Skip spells too high of a tier

            if(spell instanceof AbstractCastMethod) {
                int xOffset = 18 * (numCast % 6 );
                int yOffset = (numCast / 6) * 20 ;
                cell = new GlyphButton(this, bookLeft + 15 + xOffset, bookTop + 20 + yOffset, false, spell.getIcon(), spell.tag);
                numCast++;
            }else if(spell instanceof AbstractAugment){
                int xOffset = 20 * (numAugment % 6 );
                int yOffset = (numAugment / 6) * 20 ;
                cell = new GlyphButton(this, bookLeft + 15 + xOffset, bookTop + 70 +  yOffset, false, spell.getIcon(), spell.tag);
                numAugment++;
            }else{
                continue;
            }
            addButton(cell);
        }
        for(AbstractSpellPart s : displayedEffects){
            AbstractEffect spell = (AbstractEffect)s;
            if(!Config.isSpellEnabled(s.tag))
                continue;
            GlyphButton cell;
            int xOffset = 20 * (numEffect % 6 );
            int yOffset = (numEffect / 6) * 18 ;
            cell = new GlyphButton(this, bookLeft + 140 + xOffset, bookTop + 20 +  yOffset, false, spell.getIcon(), spell.tag);
            numEffect ++;
            effectButtons.add(addButton(cell));
        }
    }

    public void onPageIncrease(Button button){
        page++;
        if(effects.size() < 36 * (page + 1)){
            nextButton.visible = false;
            nextButton.active = false;
        }
        previousButton.active = true;
        previousButton.visible = true;
        addSpellParts(page);
    }

    public void onPageDec(Button button){
        page--;
        if(page == 0){
            previousButton.active = false;
            previousButton.visible = false;
        }

        if(effects.size() > 36 * (page + 1)){
            nextButton.visible = true;
            nextButton.active = true;
        }
        addSpellParts(page);
    }

    public void onDocumentationClick(Button button){
        PatchouliAPI.instance.openBookGUI(Registry.ITEM.getKey(ItemsRegistry.wornNotebook));
    }

    public void onColorClick(Button button){
        ParticleColor.IntWrapper color = SpellBook.getSpellColor(spell_book_tag, selected_cast_slot);
        Minecraft.getInstance().displayGuiScreen(new GuiColorScreen(color.r, color.g, color.b, selected_cast_slot));
    }

    public void onCraftingSlotClick(Button button){
        ((CraftingButton) button).spellTag = "";
        ((CraftingButton) button).resourceIcon = "";
    }

    public void onGlyphClick(Button button){
        GlyphButton button1 = (GlyphButton) button;
        for(CraftingButton b : craftingCells){
            if(b.resourceIcon.equals("")){
                b.resourceIcon = button1.resourceIcon;
                b.spellTag = button1.spell_id;
                return;
            }
        }
    }

    public void onSlotChange(Button button){
        this.selected_slot.isSelected = false;
        this.selected_slot = (GuiSpellSlot) button;
        this.selected_slot.isSelected = true;
        this.selected_cast_slot = this.selected_slot.slotNum;
        updateCraftingSlots(this.selected_cast_slot);
        spell_name.setText(SpellBook.getSpellName(spell_book_tag, this.selected_cast_slot));
    }

    public void updateCraftingSlots(int bookSlot){
        //Crafting slots
        List<AbstractSpellPart> spell_recipe = this.spell_book_tag != null ? SpellBook.getRecipeFromTag(spell_book_tag, bookSlot) : null;
        for (int i = 0; i < craftingCells.size(); i++) {
            CraftingButton slot = craftingCells.get(i);
            slot.spellTag = "";
            slot.resourceIcon = "";
            if (spell_recipe != null && i < spell_recipe.size()){
                slot.spellTag = spell_recipe.get(i).getTag();
                slot.resourceIcon = spell_recipe.get(i).getIcon();
            }
        }
    }

    public void onCreateClick(Button button){
        List<String> ids = new ArrayList<>();
        for(CraftingButton slot : craftingCells){
            ids.add(slot.spellTag);
        }
        Networking.INSTANCE.sendToServer(new PacketUpdateSpellbook(ids.toString(), this.selected_cast_slot, this.spell_name.getText()));
    }

    public static void open(ArsNouveauAPI api, CompoundNBT spell_book_tag, int tier, String unlockedSpells){
        Minecraft.getInstance().displayGuiScreen(new GuiSpellBook(api, spell_book_tag, tier, unlockedSpells));
    }

    public void drawBackgroundElements(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        super.drawBackgroundElements(stack, mouseX, mouseY, partialTicks);
        minecraft.fontRenderer.drawString(stack, "Form", 15, 10,  0);
        minecraft.fontRenderer.drawString(stack,"Effect", 140, 10,  0);
        minecraft.fontRenderer.drawString(stack,"Augment", 15, 60,  0);
        minecraft.fontRenderer.drawString(stack,"Create", 208, 157,  0);
    }

    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        super.render(ms, mouseX, mouseY, partialTicks);
        spell_name.setSuggestion(spell_name.getText().isEmpty() ? "My Spell Name" : "");
    }

}