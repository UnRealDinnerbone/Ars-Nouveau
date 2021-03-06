package com.hollingsworth.arsnouveau.common.event;

import com.google.common.collect.ImmutableSet;
import com.hollingsworth.arsnouveau.ArsNouveau;
import com.hollingsworth.arsnouveau.api.event.EventQueue;
import com.hollingsworth.arsnouveau.common.entity.ModEntities;
import com.hollingsworth.arsnouveau.setup.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.Config;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityClassification;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.blockplacer.SimpleBlockPlacer;
import net.minecraft.world.gen.blockstateprovider.SimpleBlockStateProvider;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.foliageplacer.BlobFoliagePlacer;
import net.minecraft.world.gen.trunkplacer.StraightTrunkPlacer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = ArsNouveau.MODID)
public class WorldEvent {
    public static ConfiguredFeature<BaseTreeFeatureConfig, ?> MAGIC_TREE_CONFIG = Feature.TREE.withConfiguration((
            new BaseTreeFeatureConfig.Builder(new SimpleBlockStateProvider(Blocks.OAK_LOG.getDefaultState()),
                    new SimpleBlockStateProvider(Blocks.OAK_LEAVES.getDefaultState()),
                    new BlobFoliagePlacer(FeatureSpread.func_242252_a(2), FeatureSpread.func_242252_a(0), 3),
                    new StraightTrunkPlacer(4, 2, 0),
                    new TwoLayerFeature(1, 0, 1))).setIgnoreVines().build());

    public static void registerFeatures() {
        BlockClusterFeatureConfig BERRY_BUSH_PATCH_CONFIG = (new BlockClusterFeatureConfig.Builder(new SimpleBlockStateProvider(BlockRegistry.MANA_BERRY_BUSH.getDefaultState()), SimpleBlockPlacer.PLACER)).tries(64).whitelist(ImmutableSet.of(Blocks.GRASS_BLOCK)).func_227317_b_().build();

        Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, BlockRegistry.ARCANE_ORE.getRegistryName(),
                Feature.ORE.withConfiguration(new OreFeatureConfig(OreFeatureConfig.FillerBlockType.BASE_STONE_OVERWORLD,
                        BlockRegistry.ARCANE_ORE.getDefaultState(), 5)).range(60).square().func_242731_b(5));

        Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, BlockRegistry.MANA_BERRY_BUSH.getRegistryName(), Feature.RANDOM_PATCH.withConfiguration(BERRY_BUSH_PATCH_CONFIG).withPlacement(Features.Placements.PATCH_PLACEMENT).chance(12));
        Registry.register(WorldGenRegistries.CONFIGURED_FEATURE, "magic_tree", WorldEvent.MAGIC_TREE_CONFIG);

    }

    @SubscribeEvent
    public static void biomeLoad(BiomeLoadingEvent e) {
        if (e.getCategory() == Biome.Category.NETHER || e.getCategory() == Biome.Category.THEEND)
            return;
        if (Config.SPAWN_ORE.get()) {
            e.getGeneration().withFeature(GenerationStage.Decoration.UNDERGROUND_ORES,
                    WorldGenRegistries.CONFIGURED_FEATURE.getOrDefault(BlockRegistry.ARCANE_ORE.getRegistryName())).build();
        }
        List<Biome.Category> categories = Arrays.asList(Biome.Category.FOREST, Biome.Category.EXTREME_HILLS, Biome.Category.JUNGLE,
                Biome.Category.PLAINS, Biome.Category.SWAMP, Biome.Category.SAVANNA);
        if (categories.contains(e.getCategory())) {
            e.getSpawns().withSpawner(EntityClassification.CREATURE, new MobSpawnInfo.Spawners(ModEntities.ENTITY_CARBUNCLE_TYPE, Config.CARBUNCLE_WEIGHT.get(), 1, 1));
            e.getSpawns().withSpawner(EntityClassification.CREATURE, new MobSpawnInfo.Spawners(ModEntities.ENTITY_SYLPH_TYPE, Config.SYLPH_WEIGHT.get(), 1, 1));
        }

        if (e.getCategory().equals(Biome.Category.TAIGA) && Config.SPAWN_BERRIES.get()) {
            e.getGeneration().withFeature(GenerationStage.Decoration.VEGETAL_DECORATION, Objects.requireNonNull(WorldGenRegistries.CONFIGURED_FEATURE.getOrDefault(BlockRegistry.MANA_BERRY_BUSH.getRegistryName()))).build();
        }

    }

    @SubscribeEvent
    public static void worldTick(TickEvent.WorldTickEvent e) {
        World world = e.world;
        if (world.isRemote || e.phase != TickEvent.Phase.END)
            return;
        EventQueue.getInstance().tick();
    }
}
