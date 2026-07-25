package com.autoreplant;

import com.autoreplant.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoReplantMod implements ModInitializer {
    public static final String MOD_ID = "autoreplant";
    static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    static ModConfig config;

    @Override
    public void onInitialize() {
        config = ModConfig.load(FabricLoader.getInstance().getConfigDir().resolve("autoreplant.json"));
        LOGGER.info("Auto Replant Tree config loaded (sneakToDisable={})", config.sneakToDisable);
        LOGGER.info("Auto Replant Tree loaded!");

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient) return;
            if (config.sneakToDisable && player.isSneaking()) return;

            Block brokenBlock = state.getBlock();
            if (!SaplingRegistry.isLog(brokenBlock)) return;

            Item sapling = SaplingRegistry.getSaplingFor(brokenBlock);
            if (sapling == null) return;

            String configKey = SaplingRegistry.getConfigKey(sapling);
            if (configKey != null && !config.isEnabled(configKey)) return;

            world.getServer().execute(() -> {
                if (SaplingRegistry.is2x2Sapling(sapling)) {
                    process2x2Replant((ServerWorld) world, pos, sapling, player);
                } else {
                    processSingleReplant((ServerWorld) world, pos, sapling, player);
                }
            });
        });
    }

    private void processSingleReplant(ServerWorld world, BlockPos pos, Item sapling, PlayerEntity player) {
        if (!isValidGround(world.getBlockState(pos.down()).getBlock(), sapling)) return;
        if (!world.getBlockState(pos).isAir()) return;

        int slot = -1;
        if (config.requireSapling) {
            slot = findSaplingSlot(player.getInventory(), sapling);
            if (slot == -1) return;
        }

        BlockState saplingState = ((BlockItem) sapling).getBlock().getDefaultState();
        if (!saplingState.canPlaceAt(world, pos)) return;

        world.setBlockState(pos, saplingState);
        if (config.requireSapling) {
            player.getInventory().getStack(slot).decrement(1);
        }
    }

    private void process2x2Replant(ServerWorld world, BlockPos pos, Item sapling, PlayerEntity player) {
        BlockPos anchor = find2x2Anchor(world, pos, sapling);
        if (anchor == null) return;

        BlockPos[] positions = {
                anchor,
                anchor.east(),
                anchor.south(),
                anchor.east().south()
        };

        if (config.requireSapling && countSaplings(player.getInventory(), sapling) < 4) return;

        BlockState saplingState = ((BlockItem) sapling).getBlock().getDefaultState();
        for (BlockPos p : positions) {
            BlockState atPos = world.getBlockState(p);
            if (!atPos.isAir() && !SaplingRegistry.isLog(atPos.getBlock())) return;
            if (!saplingState.canPlaceAt(world, p)) return;
        }

        for (BlockPos p : positions) {
            world.setBlockState(p, saplingState);
        }
        if (config.requireSapling) {
            consumeSaplings(player.getInventory(), sapling, 4);
        }
    }

    private BlockPos find2x2Anchor(World world, BlockPos brokenPos, Item sapling) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                BlockPos anchor = brokenPos.add(-dx, 0, -dz);
                BlockPos[] positions = {
                        anchor,
                        anchor.east(),
                        anchor.south(),
                        anchor.east().south()
                };

                boolean allValid = true;
                for (BlockPos p : positions) {
                    if (!isValidGround(world.getBlockState(p.down()).getBlock(), sapling)) {
                        allValid = false;
                        break;
                    }
                    BlockState atPos = world.getBlockState(p);
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

    private boolean isValidGround(Block block, Item sapling) {
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

    private int findSaplingSlot(PlayerInventory inventory, Item sapling) {
        for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack stack = inventory.main.get(i);
            if (!stack.isEmpty() && stack.isOf(sapling)) return i;
        }
        for (int i = 0; i < inventory.offHand.size(); i++) {
            ItemStack stack = inventory.offHand.get(i);
            if (!stack.isEmpty() && stack.isOf(sapling)) return inventory.main.size() + inventory.armor.size() + i;
        }
        return -1;
    }

    private int countSaplings(PlayerInventory inventory, Item sapling) {
        int count = 0;
        for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack stack = inventory.main.get(i);
            if (!stack.isEmpty() && stack.isOf(sapling)) count += stack.getCount();
        }
        for (int i = 0; i < inventory.offHand.size(); i++) {
            ItemStack stack = inventory.offHand.get(i);
            if (!stack.isEmpty() && stack.isOf(sapling)) count += stack.getCount();
        }
        return count;
    }

    private void consumeSaplings(PlayerInventory inventory, Item sapling, int amount) {
        int remaining = amount;
        for (int i = 0; i < inventory.main.size() && remaining > 0; i++) {
            ItemStack stack = inventory.main.get(i);
            if (!stack.isEmpty() && stack.isOf(sapling)) {
                int take = Math.min(stack.getCount(), remaining);
                stack.decrement(take);
                remaining -= take;
            }
        }
        if (remaining > 0) {
            for (int i = 0; i < inventory.offHand.size(); i++) {
                ItemStack stack = inventory.offHand.get(i);
                if (!stack.isEmpty() && stack.isOf(sapling)) {
                    stack.decrement(remaining);
                    break;
                }
            }
        }
    }
}
