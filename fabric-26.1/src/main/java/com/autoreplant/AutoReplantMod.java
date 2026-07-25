package com.autoreplant;

import com.autoreplant.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoReplantMod implements ModInitializer {

    public static final String MOD_ID = "autoreplant";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ModConfig config;

    @Override
    public void onInitialize() {
        var configPath = FabricLoader.getInstance().getConfigDir().resolve("autoreplant.json");
        config = ModConfig.load(configPath);
        LOGGER.info("Auto Replant Tree config loaded (sneakToDisable={})", config.sneakToDisable);

        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level.isClientSide()) return;
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            if (config.sneakToDisable && serverPlayer.isCrouching()) return;

            Block brokenBlock = state.getBlock();
            if (!SaplingRegistry.isLog(brokenBlock)) return;

            Item requiredSapling = SaplingRegistry.getSaplingFor(brokenBlock);
            if (requiredSapling == null) return;

            String configKey = SaplingRegistry.getConfigKey(requiredSapling);
            if (configKey != null && !config.isEnabled(configKey)) return;

            if (SaplingRegistry.is2x2Sapling(requiredSapling)) {
                handle2x2Plant(level, pos, requiredSapling, serverPlayer.getInventory());
            } else {
                handleSinglePlant(level, pos, requiredSapling, serverPlayer.getInventory());
            }
        });

        LOGGER.info("Auto Replant Tree loaded!");
    }

    private void handleSinglePlant(Level level, BlockPos pos, Item sapling, Inventory inventory) {
        if (!isValidGround(level.getBlockState(pos.below()).getBlock(), sapling)) return;
        if (!level.getBlockState(pos).isAir()) return;

        int slot = -1;
        if (config.requireSapling) {
            slot = findSaplingSlot(inventory, sapling);
            if (slot == -1) return;
        }

        BlockState saplingState = Block.byItem(sapling).defaultBlockState();
        if (!saplingState.canSurvive(level, pos)) return;

        level.setBlockAndUpdate(pos, saplingState);
        if (config.requireSapling) {
            inventory.getItem(slot).shrink(1);
        }
    }

    private void handle2x2Plant(Level level, BlockPos brokenPos, Item sapling, Inventory inventory) {
        BlockPos anchor = find2x2Anchor(level, brokenPos, sapling);
        if (anchor == null) return;

        BlockPos p1 = anchor;
        BlockPos p2 = anchor.offset(1, 0, 0);
        BlockPos p3 = anchor.offset(0, 0, 1);
        BlockPos p4 = anchor.offset(1, 0, 1);
        BlockPos[] positions = {p1, p2, p3, p4};

        if (config.requireSapling) {
            int available = countSaplings(inventory, sapling);
            if (available < 4) return;
        }

        BlockState saplingState = Block.byItem(sapling).defaultBlockState();
        for (BlockPos p : positions) {
            BlockState atPos = level.getBlockState(p);
            if (!atPos.isAir() && !SaplingRegistry.isLog(atPos.getBlock())) return;
            if (!saplingState.canSurvive(level, p)) return;
        }

        for (BlockPos p : positions) {
            level.setBlockAndUpdate(p, saplingState);
        }

        if (config.requireSapling) {
            consumeSaplings(inventory, sapling, 4);
        }
    }

    private BlockPos find2x2Anchor(Level level, BlockPos brokenPos, Item sapling) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                BlockPos anchor = brokenPos.offset(-dx, 0, -dz);
                BlockPos[] positions = {
                        anchor,
                        anchor.offset(1, 0, 0),
                        anchor.offset(0, 0, 1),
                        anchor.offset(1, 0, 1)
                };

                boolean allValid = true;
                for (BlockPos p : positions) {
                    if (!isValidGround(level.getBlockState(p.below()).getBlock(), sapling)) {
                        allValid = false;
                        break;
                    }
                    BlockState atPos = level.getBlockState(p);
                    if (!atPos.isAir() && !SaplingRegistry.isLog(atPos.getBlock())) {
                        allValid = false;
                        break;
                    }
                }
                if (allValid) return anchor;
            }
        }
        return null;
    }

    private static boolean isValidGround(Block block, Item sapling) {
        Block required = SaplingRegistry.getRequiredGround(sapling);
        if (required != null) return block == required;

        return block == Blocks.GRASS_BLOCK
                || block == Blocks.DIRT
                || block == Blocks.COARSE_DIRT
                || block == Blocks.ROOTED_DIRT
                || block == Blocks.PODZOL
                || block == Blocks.MYCELIUM
                || block == Blocks.MUD
                || block == Blocks.MUDDY_MANGROVE_ROOTS
                || block == Blocks.MOSS_BLOCK;
    }

    private static int findSaplingSlot(Inventory inventory, Item sapling) {
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.is(sapling)) return i;
        }
        ItemStack offhand = inventory.getItem(Inventory.SLOT_OFFHAND);
        if (!offhand.isEmpty() && offhand.is(sapling)) return Inventory.SLOT_OFFHAND;
        return -1;
    }

    private static int countSaplings(Inventory inventory, Item sapling) {
        int count = 0;
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.is(sapling)) count += stack.getCount();
        }
        ItemStack offhand = inventory.getItem(Inventory.SLOT_OFFHAND);
        if (!offhand.isEmpty() && offhand.is(sapling)) count += offhand.getCount();
        return count;
    }

    private static void consumeSaplings(Inventory inventory, Item sapling, int amount) {
        int remaining = amount;
        for (int i = 0; i < Inventory.INVENTORY_SIZE && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.is(sapling)) {
                int take = Math.min(stack.getCount(), remaining);
                stack.shrink(take);
                remaining -= take;
            }
        }
        if (remaining > 0) {
            ItemStack offhand = inventory.getItem(Inventory.SLOT_OFFHAND);
            if (!offhand.isEmpty() && offhand.is(sapling)) {
                offhand.shrink(remaining);
            }
        }
    }
}
