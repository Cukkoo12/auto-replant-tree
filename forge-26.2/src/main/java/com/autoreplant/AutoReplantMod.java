package com.autoreplant;

import com.autoreplant.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Mod(AutoReplantMod.MOD_ID)
public class AutoReplantMod {
    public static final String MOD_ID = "autoreplant";
    static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    static ModConfig config;

    public AutoReplantMod() {
        config = ModConfig.load(FMLPaths.CONFIGDIR.get().resolve("autoreplant.json"));
        LOGGER.info("Auto Replant Tree config loaded (sneakToDisable={})", config.sneakToDisable);
        LOGGER.info("Auto Replant Tree loaded!");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class GameEvents {
        private static final Map<ResourceKey<Level>, List<ReplantEntry>> pending = new HashMap<>();

        private record ReplantEntry(BlockPos pos, Item sapling, Player player) {}

        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            Player player = event.getPlayer();
            if (config.sneakToDisable && player.isCrouching()) return;

            Block brokenBlock = event.getState().getBlock();
            if (!SaplingRegistry.isLog(brokenBlock)) return;

            Item sapling = SaplingRegistry.getSaplingFor(brokenBlock);
            if (sapling == null) return;

            String configKey = SaplingRegistry.getConfigKey(sapling);
            if (configKey != null && !config.isEnabled(configKey)) return;

            LevelAccessor la = event.getLevel();
            if (!(la instanceof Level level)) return;
            if (level.isClientSide()) return;

            pending.computeIfAbsent(level.dimension(), k -> new ArrayList<>())
                    .add(new ReplantEntry(event.getPos().immutable(), sapling, player));
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
            if (pending.isEmpty()) return;

            List<Map.Entry<ResourceKey<Level>, List<ReplantEntry>>> snapshot;
            snapshot = new ArrayList<>(pending.entrySet());
            pending.clear();

            MinecraftServer server = event.server();
            for (var entry : snapshot) {
                ServerLevel level = server.getLevel(entry.getKey());
                if (level == null) continue;

                for (ReplantEntry r : entry.getValue()) {
                    if (SaplingRegistry.is2x2Sapling(r.sapling)) {
                        process2x2Replant(level, r);
                    } else {
                        processSingleReplant(level, r);
                    }
                }
            }
        }
    }

    // === Replant logic ===

    private static void processSingleReplant(Level level, GameEvents.ReplantEntry entry) {
        BlockPos pos = entry.pos;
        if (!isValidGround(level.getBlockState(pos.below()).getBlock(), entry.sapling)) return;
        if (!level.getBlockState(pos).isAir()) return;

        int slot = -1;
        if (config.requireSapling) {
            slot = findSaplingSlot(entry.player.getInventory(), entry.sapling);
            if (slot == -1) return;
        }

        BlockState saplingState = Block.byItem(entry.sapling).defaultBlockState();
        if (!saplingState.canSurvive(level, pos)) return;

        level.setBlockAndUpdate(pos, saplingState);
        if (config.requireSapling) {
            entry.player.getInventory().getItem(slot).shrink(1);
        }
    }

    private static void process2x2Replant(Level level, GameEvents.ReplantEntry entry) {
        BlockPos anchor = find2x2Anchor(level, entry.pos, entry.sapling);
        if (anchor == null) return;

        BlockPos[] positions = {
                anchor,
                anchor.offset(1, 0, 0),
                anchor.offset(0, 0, 1),
                anchor.offset(1, 0, 1)
        };

        if (config.requireSapling && countSaplings(entry.player.getInventory(), entry.sapling) < 4) return;

        BlockState saplingState = Block.byItem(entry.sapling).defaultBlockState();
        for (BlockPos p : positions) {
            BlockState atPos = level.getBlockState(p);
            if (!atPos.isAir() && !SaplingRegistry.isLog(atPos.getBlock())) return;
            if (!saplingState.canSurvive(level, p)) return;
        }

        for (BlockPos p : positions) {
            level.setBlockAndUpdate(p, saplingState);
        }
        if (config.requireSapling) {
            consumeSaplings(entry.player.getInventory(), entry.sapling, 4);
        }
    }

    private static BlockPos find2x2Anchor(Level level, BlockPos brokenPos, Item sapling) {
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
