package com.hollingsworth.arsnouveau.common.spell.effect;

import com.hollingsworth.arsnouveau.ModConfig;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractEffect;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import com.hollingsworth.arsnouveau.common.block.tile.IntangibleAirTile;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.setup.BlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class EffectIntangible extends AbstractEffect {
    public EffectIntangible() {
        super(ModConfig.EffectIntangibleID, "Intangible");
    }

    @Override
    public void onResolve(RayTraceResult rayTraceResult, World world, @Nullable LivingEntity shooter, List<AbstractAugment> augments, SpellContext spellContext) {
        if(rayTraceResult instanceof BlockRayTraceResult){
            BlockPos pos = ((BlockRayTraceResult) rayTraceResult).getPos();
            int aoeBuff = getBuffCount(augments, AugmentAOE.class);
            int duration = 60 + 20 * getDurationModifier(augments);
            List<BlockPos> posList = SpellUtil.calcAOEBlocks(shooter, pos, (BlockRayTraceResult)rayTraceResult,1 + aoeBuff, 1 + aoeBuff, 1, -1);
            for(BlockPos pos1 : posList) {
                if(world.getTileEntity(pos1) != null || world.getBlockState(pos1).getMaterial() == Material.AIR
                        || world.getBlockState(pos1).getBlock() == Blocks.BEDROCK || world.getBlockState(pos1).getBlockHardness(world, pos1) > getHardness(augments) )
                    continue;

                BlockState state = world.getBlockState(pos1);
                int id = Block.getStateId(state);
                world.setBlockState(pos1, BlockRegistry.INTANGIBLE_AIR.getDefaultState());
                IntangibleAirTile tile = ((IntangibleAirTile) world.getTileEntity(pos1));
                tile.stateID = id;
                tile.maxLength = duration;
            }
        }
    }

    @Override
    public Item getCraftingReagent() {
        return Items.PHANTOM_MEMBRANE;
    }

    @Override
    public Tier getTier() {
        return Tier.THREE;
    }

    @Override
    public int getManaCost() {
        return 30;
    }

    @Override
    protected String getBookDescription() {
        return "Causes blocks to temporarily turn into air. Can be modified with Amplify for blocks of higher hardness, AOE, Duration Down, and Extend Time.";
    }
}
