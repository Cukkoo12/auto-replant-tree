package com.autoreplant;

import com.autoreplant.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient) return;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

            if (config.sneakToDisable && serverPlayer.isSneaking()) return;

            Block brokenBlock = state.getBlock();
            if (!SaplingRegistry.isLog(brokenBlock)) return;

            Item requiredSapling = SaplingRegistry.getSaplingFor(brokenBlock);
            if (requiredSapling == null) return;

            String configKey = SaplingRegistry.getConfigKey(requiredSapling);
            if (configKey != null && !config.isEnabled(configKey)) return;

            if (SaplingRegistry.is2x2Sapling(requiredSapling)) {
                handle2x2Plant(world, pos, requiredSapling, serverPlayer.getInventory());
            } else {
                handleSinglePlant(world, pos, requiredSapling, serverPlayer.getInventory());
            }
        });

        LOGGER.info("Auto Replant Tree loaded!");
    }

    private void handleSinglePlant(World world, BlockPos pos, Item sapling, net.minecraft.entity.player.PlayerInventory inventory) {
        if (!isValidGround(world.getBlockState(pos.down()).getBlock(), sapling)) return;
        if (!world.getBlockState(pos).isAir()) return;

        int slot = -1;
        if (config.requireSapling) {
            slot = findSaplingSlot(inventory, sapling);
            if (slot == -1) return;
        }

        BlockState saplingState = ((BlockItem) sapling).getBlock().getDefaultState();
        if (!saplingState.canPlaceAt(world, pos)) return;

        world.setBlockState(pos, saplingState);
        if (config.requireSapling) {
            inventory.getStack(slot).decrement(1);
        }
    }

    private void handle2x2Plant(World world, BlockPos brokenPos, Item sapling, net.minecraft.entity.player.PlayerInventory inventory) {
        BlockPos anchor = find2x2Anchor(world, brokenPos, sapling);
        if (anchor == null) return;

        BlockPos p1 = anchor;
        BlockPos p2 = anchor.east();
        BlockPos p3 = anchor.south();
        BlockPos p4 = anchor.east().south();
        BlockPos[] positions = {p1, p2, p3, p4};

        if (config.requireSapling) {
            int available = countSaplings(inventory, sapling);
            if (available < 4) return;
        }

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
            consumeSaplings(inventory, sapling, 4);
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

    private static int findSaplingSlot(net.minecraft.entity.player.PlayerInventory inventory, Item sapling) {
        for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack stack = inventory.main.get(i);
            if (!stack.isEmpty() && stack.isOf(sapling)) return i;
        }
        ItemStack offhand = inventory.offHand.get(0);
        if (!offhand.isEmpty() && offhand.isOf(sapling)) return inventory.main.size();
        return -1;
    }

    private static int countSaplings(net.minecraft.entity.player.PlayerInventory inventory, Item sapling) {
        int count = 0;
        for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack stack = inventory.main.get(i);
            if (!stack.isEmpty() && stack.isOf(sapling)) count += stack.getCount();
        }
        ItemStack offhand = inventory.offHand.get(0);
        if (!offhand.isEmpty() && offhand.isOf(sapling)) count += offhand.getCount();
        return count;
    }

    private static void consumeSaplings(net.minecraft.entity.player.PlayerInventory inventory, Item sapling, int amount) {
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
            ItemStack offhand = inventory.offHand.get(0);
            if (!offhand.isEmpty() && offhand.isOf(sapling)) {
                offhand.decrement(remaining);
            }
        }
    }
}
