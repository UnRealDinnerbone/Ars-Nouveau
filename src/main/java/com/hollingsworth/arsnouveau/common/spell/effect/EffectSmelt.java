package com.hollingsworth.arsnouveau.common.spell.effect;

import com.hollingsworth.arsnouveau.ModConfig;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.FurnaceRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class EffectSmelt extends AbstractEffect {

    public EffectSmelt() {
        super(ModConfig.EffectSmeltID, "Smelt");
    }

    @Override
    public void onResolve(RayTraceResult rayTraceResult, World world, LivingEntity shooter, List<AbstractAugment> augments, SpellContext spellContext) {
        if(!(rayTraceResult instanceof BlockRayTraceResult))
            return;


        float maxHardness = getHardness(augments);

        int aoeBuff = getBuffCount(augments, AugmentAOE.class);
        int maxItemSmelt = 3 + 4*aoeBuff;

        List<BlockPos> posList = SpellUtil.calcAOEBlocks(shooter, ((BlockRayTraceResult) rayTraceResult).getPos(), (BlockRayTraceResult)rayTraceResult,1 + aoeBuff, 1 + aoeBuff, 1, -1);
        List<ItemEntity> itemEntities = world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB(((BlockRayTraceResult) rayTraceResult).getPos()).grow(aoeBuff + 1.0));
        smeltItems(world, itemEntities, maxItemSmelt);

        for(BlockPos pos : posList) {
            smeltBlock(world, pos, maxHardness, shooter);
        }

    }

    public void smeltBlock(World world, BlockPos pos, float maxHardness, LivingEntity shooter){
        BlockState state = world.getBlockState(pos);
        Optional<FurnaceRecipe> optional = world.getRecipeManager().getRecipe(IRecipeType.SMELTING, new Inventory(new ItemStack(state.getBlock().asItem(), 1)),
                world);
        if (optional.isPresent()) {
            ItemStack itemstack = optional.get().getRecipeOutput();
            if (!itemstack.isEmpty()) {
                if(itemstack.getItem() instanceof BlockItem){
                    world.setBlockState(pos, ((BlockItem)itemstack.getItem()).getBlock().getDefaultState());
                }else{
                    if(!(state.getBlockHardness(world, pos) <= maxHardness && state.getBlockHardness(world, pos) >= 0)){
                        return;
                    }
                    BlockUtil.destroyBlockSafely(world, pos, false, shooter);
                    world.addEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(),itemstack.copy()));
                    BlockUtil.safelyUpdateState(world, pos);
                }
            }
        }
    }


    public void smeltItems(World world, List<ItemEntity> itemEntities, int maxItemSmelt){
        int numSmelted = 0;
        for(int i = 0; i < itemEntities.size(); i++){
            if( numSmelted > maxItemSmelt)
                break;
            ItemEntity entity = itemEntities.get(i);
            Optional<FurnaceRecipe> optional = world.getRecipeManager().getRecipe(IRecipeType.SMELTING, new Inventory(entity.getItem()),
                    world);
            if(optional.isPresent()){
                ItemStack result = optional.get().getRecipeOutput().copy();
                if(result.isEmpty())
                    continue;
                while(numSmelted < maxItemSmelt && !entity.getItem().isEmpty()){
                    entity.getItem().shrink(1);
                    world.addEntity(new ItemEntity(world, entity.getPosX(), entity.getPosY(), entity.getPosZ(), result.copy()));
                    numSmelted++;
                }
            }
        }
    }

    @Override
    public boolean wouldSucceed(RayTraceResult rayTraceResult, World world, LivingEntity shooter, List<AbstractAugment> augments) {
        return rayTraceResult instanceof BlockRayTraceResult;
    }

    @Override
    protected String getBookDescription() {
        return "Smelts blocks and items in the world. AOE will increase the number of items and radius of blocks that can be smelted at once, while Amplify will allow Smelt to work on blocks of higher hardness.";
    }

    @Override
    public Tier getTier() {
        return Tier.TWO;
    }

    @Override
    public int getManaCost() {
        return 100;
    }

    @Override
    public Item getCraftingReagent() {
        return Items.BLAST_FURNACE;
    }
}
