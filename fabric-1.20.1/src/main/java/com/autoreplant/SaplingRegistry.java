package com.autoreplant;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.*;

public class SaplingRegistry {
    private static final Map<Block, Item> LOG_TO_SAPLING = new HashMap<>();
    private static final Set<Block> LOG_BLOCKS = new HashSet<>();
    private static final Set<Item> TWO_BY_TWO = new HashSet<>();
    private static final Map<Item, Block> REQUIRED_GROUND = new HashMap<>();
    private static final Map<Item, String> SAPLING_CONFIG_KEY = new HashMap<>();

    static {
        register(Items.OAK_SAPLING, "oak",
                Blocks.OAK_LOG, Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_OAK_WOOD);
        register(Items.SPRUCE_SAPLING, "spruce",
                Blocks.SPRUCE_LOG, Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_WOOD);
        register(Items.BIRCH_SAPLING, "birch",
                Blocks.BIRCH_LOG, Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_BIRCH_WOOD);
        register(Items.JUNGLE_SAPLING, "jungle",
                Blocks.JUNGLE_LOG, Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_WOOD);
        register(Items.ACACIA_SAPLING, "acacia",
                Blocks.ACACIA_LOG, Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_ACACIA_WOOD);
        register(Items.DARK_OAK_SAPLING, "dark_oak",
                Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_WOOD);
        register(Items.MANGROVE_PROPAGULE, "mangrove",
                Blocks.MANGROVE_LOG, Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_WOOD);
        register(Items.CHERRY_SAPLING, "cherry",
                Blocks.CHERRY_LOG, Blocks.CHERRY_WOOD, Blocks.STRIPPED_CHERRY_LOG, Blocks.STRIPPED_CHERRY_WOOD);

        register(Items.CRIMSON_FUNGUS, "crimson",
                Blocks.CRIMSON_STEM, Blocks.CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_HYPHAE);
        register(Items.WARPED_FUNGUS, "warped",
                Blocks.WARPED_STEM, Blocks.WARPED_HYPHAE, Blocks.STRIPPED_WARPED_STEM, Blocks.STRIPPED_WARPED_HYPHAE);

        TWO_BY_TWO.add(Items.DARK_OAK_SAPLING);
        TWO_BY_TWO.add(Items.JUNGLE_SAPLING);

        REQUIRED_GROUND.put(Items.CRIMSON_FUNGUS, Blocks.CRIMSON_NYLIUM);
        REQUIRED_GROUND.put(Items.WARPED_FUNGUS, Blocks.WARPED_NYLIUM);
    }

    private static void register(Item sapling, String configKey, Block... logs) {
        for (Block log : logs) {
            LOG_TO_SAPLING.put(log, sapling);
            LOG_BLOCKS.add(log);
        }
        SAPLING_CONFIG_KEY.put(sapling, configKey);
    }

    public static boolean isLog(Block block) {
        return LOG_BLOCKS.contains(block);
    }

    public static Item getSaplingFor(Block log) {
        return LOG_TO_SAPLING.get(log);
    }

    public static boolean is2x2Sapling(Item sapling) {
        return TWO_BY_TWO.contains(sapling);
    }

    public static Block getRequiredGround(Item sapling) {
        return REQUIRED_GROUND.get(sapling);
    }

    public static String getConfigKey(Item sapling) {
        return SAPLING_CONFIG_KEY.get(sapling);
    }
}
