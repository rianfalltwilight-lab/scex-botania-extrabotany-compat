package dev.scex.compat.botania.testharness;

import com.mojang.logging.LogUtils;
import dev.scex.compat.botania.ScexBotaniaExtraBotanyCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import vazkii.botania.api.block.Bound;
import vazkii.botania.api.block.WandBindable;
import vazkii.botania.common.item.WandOfTheForestItem;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/** Development-only physical-client probe; excluded from the release JAR. */
@EventBusSubscriber(modid = ScexBotaniaExtraBotanyCompat.MOD_ID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.GAME)
public final class ClientInteractionHarness {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("scex.compat.clientTest");
    private static final String WORLD = System.getProperty("scex.compat.clientTestWorld", "compat-client-world-1");
    private static final ResourceLocation FLOWER_ID = ResourceLocation.fromNamespaceAndPath("extrabotany", "reikarlily");
    private static final ResourceLocation BOTANIA_FUNCTIONAL_FLOWER_ID =
            ResourceLocation.fromNamespaceAndPath("botania", "agricarnation");
    private static final ResourceLocation SPREADER_ID = ResourceLocation.fromNamespaceAndPath("botania", "mana_spreader");
    private static final ResourceLocation POOL_ID = ResourceLocation.fromNamespaceAndPath("botania", "mana_pool");
    private static final ResourceLocation MANALINK_ID = ResourceLocation.fromNamespaceAndPath("extrabotany", "manalink");
    private static final ResourceLocation WAND_ID = ResourceLocation.fromNamespaceAndPath("botania", "wand_of_the_forest");

    private static int stage;
    private static int ticks;
    private static boolean requestPending;
    private static volatile Throwable asyncFailure;
    private static volatile boolean asyncDone;
    private static BlockPos flowerPos;
    private static BlockPos botaniaFunctionalFlowerPos;
    private static BlockPos spreaderPos;
    private static BlockPos poolPos;
    private static BlockPos manalinkPos;

    private ClientInteractionHarness() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (stage == 0) {
                if (minecraft.screen != null && minecraft.level == null && ++ticks >= 60) {
                    stage = 1;
                    ticks = 0;
                    LOGGER.info("SCEX_WAND_CLIENT_TEST opening_world={}", WORLD);
                    minecraft.createWorldOpenFlows().openWorld(WORLD,
                            () -> fail(minecraft, new IllegalStateException("Unable to open test world " + WORLD)));
                }
                return;
            }
            if (stage == 22) {
                awaitWorldCloseAndReopen(minecraft);
                return;
            }
            if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null
                    || minecraft.getSingleplayerServer() == null) {
                return;
            }
            switch (stage) {
                case 1 -> requestSetup(minecraft);
                case 2 -> awaitAsync(minecraft, 3);
                case 3 -> afterDelay(30, () -> interact(minecraft, flowerPos, "select_extra_generating_flower", 4));
                case 4 -> afterDelay(30, () -> requestAssertion(minecraft,
                        player -> assertNormalSelection(player, flowerPos), 5));
                case 5 -> awaitAsync(minecraft, 6);
                case 6 -> afterDelay(10, () -> interact(minecraft, spreaderPos, "bind_flower_to_spreader", 7));
                case 7 -> afterDelay(30, () -> requestAssertion(minecraft,
                        player -> assertFlowerBoundToSpreader(player, flowerPos, spreaderPos), 8));
                case 8 -> awaitAsync(minecraft, 9);
                case 9 -> afterDelay(10, () -> interact(minecraft, botaniaFunctionalFlowerPos,
                        "select_botania_functional_flower", 10));
                case 10 -> afterDelay(30, () -> requestAssertion(minecraft,
                        player -> assertBotaniaFunctionalSelection(player, botaniaFunctionalFlowerPos), 11));
                case 11 -> awaitAsync(minecraft, 12);
                case 12 -> afterDelay(10, () -> interact(minecraft, poolPos,
                        "bind_botania_functional_flower_to_pool", 13));
                case 13 -> afterDelay(30, () -> requestAssertion(minecraft,
                        player -> assertBotaniaFunctionalFlowerBoundToPool(
                                player, botaniaFunctionalFlowerPos, poolPos), 14));
                case 14 -> awaitAsync(minecraft, 15);
                case 15 -> afterDelay(10, () -> interact(minecraft, poolPos,
                        "select_pool_for_manalink", 16));
                case 16 -> afterDelay(30, () -> requestAssertion(minecraft,
                        player -> assertExtendedPoolSelection(player, poolPos), 17));
                case 17 -> awaitAsync(minecraft, 18);
                case 18 -> afterDelay(10, () -> interact(minecraft, manalinkPos,
                        "bind_manalink_to_pool", 19));
                case 19 -> afterDelay(30, () -> requestAssertion(minecraft,
                        player -> assertManalinkBound(player, manalinkPos, poolPos), 20));
                case 20 -> awaitAsync(minecraft, 21);
                case 21 -> beginSaveReload(minecraft);
                case 23 -> afterDelay(40, () -> requestAssertion(minecraft,
                        player -> assertReloadedBindings(player), 24));
                case 24 -> awaitAsync(minecraft, 25);
                case 25 -> finish(minecraft);
                default -> { }
            }
        } catch (Throwable throwable) {
            fail(minecraft, throwable);
        }
    }

    private static void requestSetup(Minecraft minecraft) {
        if (requestPending) return;
        requestPending = true;
        UUID playerId = minecraft.player.getUUID();
        minecraft.getSingleplayerServer().execute(() -> {
            try {
                ServerPlayer player = minecraft.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                if (player == null) throw new IllegalStateException("Server player unavailable");
                BlockPos origin = player.blockPosition().offset(0, 2, 0);
                flowerPos = origin.offset(0, 0, 1);
                spreaderPos = origin.offset(1, 0, 1);
                poolPos = origin.offset(2, 0, 1);
                manalinkPos = origin.offset(3, 0, 1);
                botaniaFunctionalFlowerPos = origin.offset(4, 0, 1);
                Block flower = requireBlock(FLOWER_ID);
                Block botaniaFunctionalFlower = requireBlock(BOTANIA_FUNCTIONAL_FLOWER_ID);
                Block spreader = requireBlock(SPREADER_ID);
                Block pool = requireBlock(POOL_ID);
                Block manalink = requireBlock(MANALINK_ID);
                for (int x = -1; x <= 5; x++) {
                    player.serverLevel().setBlockAndUpdate(origin.offset(x, -1, 0), Blocks.GRASS_BLOCK.defaultBlockState());
                    player.serverLevel().setBlockAndUpdate(origin.offset(x, -1, 1), Blocks.GRASS_BLOCK.defaultBlockState());
                }
                player.serverLevel().setBlock(flowerPos, flower.defaultBlockState(), Block.UPDATE_CLIENTS);
                player.serverLevel().setBlock(spreaderPos, spreader.defaultBlockState(), Block.UPDATE_CLIENTS);
                player.serverLevel().setBlock(poolPos, pool.defaultBlockState(), Block.UPDATE_CLIENTS);
                player.serverLevel().setBlock(manalinkPos, manalink.defaultBlockState(), Block.UPDATE_CLIENTS);
                player.serverLevel().setBlock(botaniaFunctionalFlowerPos,
                        botaniaFunctionalFlower.defaultBlockState(), Block.UPDATE_CLIENTS);
                player.setGameMode(GameType.CREATIVE);
                player.getInventory().selected = 0;
                ItemStack wand = new ItemStack(BuiltInRegistries.ITEM.get(WAND_ID));
                WandOfTheForestItem.setBindMode(wand, true);
                player.getInventory().setItem(0, wand);
                player.setShiftKeyDown(true);
                asyncDone = true;
            } catch (Throwable throwable) {
                asyncFailure = throwable;
                asyncDone = true;
            }
        });
        stage = 2;
    }

    private static void interact(Minecraft minecraft, BlockPos pos, String action, int nextStage) {
        minecraft.options.keyShift.setDown(true);
        minecraft.player.input.shiftKeyDown = true;
        minecraft.player.setShiftKeyDown(true);
        minecraft.player.connection.send(new ServerboundPlayerCommandPacket(
                minecraft.player, ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
        WandBindable bindable = WandBindable.LOOKUP.find(minecraft.level, pos, Direction.UP);
        LOGGER.info("SCEX_WAND_CLIENT_PRE action={} stack={} bindMode={} sneaking={} block={} blockEntity={} wandBindable={}",
                action, minecraft.player.getMainHandItem(),
                WandOfTheForestItem.getBindMode(minecraft.player.getMainHandItem()),
                minecraft.player.isSecondaryUseActive(), minecraft.level.getBlockState(pos),
                blockEntity == null ? null : blockEntity.getClass().getName(),
                bindable == null ? null : bindable.getClass().getName());
        InteractionResult result = minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, hit);
        LOGGER.info("SCEX_WAND_CLIENT_INTERACTION action={} pos={} result={}", action, pos, result);
        stage = nextStage;
        ticks = 0;
    }

    private static void requestAssertion(Minecraft minecraft, ServerAssertion assertion, int nextStage) {
        if (requestPending) return;
        requestPending = true;
        asyncDone = false;
        asyncFailure = null;
        UUID playerId = minecraft.player.getUUID();
        minecraft.getSingleplayerServer().execute(() -> {
            try {
                ServerPlayer player = minecraft.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                if (player == null) throw new IllegalStateException("Server player unavailable");
                assertion.run(player);
            } catch (Throwable throwable) {
                asyncFailure = throwable;
            } finally {
                asyncDone = true;
            }
        });
        stage = nextStage;
    }

    private static void awaitAsync(Minecraft minecraft, int nextStage) {
        if (!asyncDone) return;
        requestPending = false;
        asyncDone = false;
        if (asyncFailure != null) {
            Throwable failure = asyncFailure;
            asyncFailure = null;
            fail(minecraft, failure);
            return;
        }
        if (nextStage == 3) {
            primeClientWand(minecraft);
        }
        stage = nextStage;
        ticks = 0;
    }

    private static void primeClientWand(Minecraft minecraft) {
        minecraft.player.getInventory().selected = 0;
        ItemStack wand = new ItemStack(BuiltInRegistries.ITEM.get(WAND_ID));
        WandOfTheForestItem.setBindMode(wand, true);
        minecraft.player.getInventory().setItem(0, wand);
        minecraft.player.connection.send(new ServerboundSetCreativeModeSlotPacket(36, wand.copy()));
        LOGGER.info("SCEX_WAND_CLIENT_TEST primed_wand={}", wand);
    }

    private static void assertNormalSelection(ServerPlayer player, BlockPos expected) {
        ItemStack wand = player.getMainHandItem();
        Optional<GlobalPos> selected = WandOfTheForestItem.getBindingAttempt(wand);
        BlockEntity blockEntity = player.serverLevel().getBlockEntity(expected);
        WandBindable capabilityBeforeAssertion = WandBindable.LOOKUP.find(player.level(), expected, Direction.UP);
        LOGGER.info("SCEX_WAND_SERVER_STATE stack={} bindMode={} sneaking={} block={} blockEntity={} wandBindable={} selected={}",
                wand, WandOfTheForestItem.getBindMode(wand), player.isSecondaryUseActive(),
                player.level().getBlockState(expected),
                blockEntity == null ? null : blockEntity.getClass().getName(),
                capabilityBeforeAssertion == null ? null : capabilityBeforeAssertion.getClass().getName(), selected);
        if (selected.isEmpty() || !selected.get().dimension().equals(player.level().dimension())
                || !selected.get().pos().equals(expected)) {
            throw new AssertionError("ExtraBotany generating flower was not selected: " + selected);
        }
        WandBindable capability = WandBindable.LOOKUP.find(player.level(), expected, Direction.UP);
        if (capability == null) {
            throw new AssertionError("ExtraBotany generating flower has no WandBindable capability");
        }
        LOGGER.info("SCEX_WAND_ASSERT_PASS check=extra_flower_selected capability={}", capability.getClass().getName());
    }

    private static void assertFlowerBoundToSpreader(ServerPlayer player, BlockPos flower, BlockPos spreader) {
        BlockEntity blockEntity = player.serverLevel().getBlockEntity(flower);
        if (!(blockEntity instanceof Bound bound) || !spreader.equals(bound.getBinding())) {
            throw new AssertionError("Generating flower binding mismatch: "
                    + (blockEntity instanceof Bound bound ? bound.getBinding() : blockEntity));
        }
        if (WandOfTheForestItem.getBindingAttempt(player.getMainHandItem()).isPresent()) {
            throw new AssertionError("Normal binding selection was not cleared");
        }
        LOGGER.info("SCEX_WAND_ASSERT_PASS check=extra_flower_bound_to_spreader flower={} spreader={}",
                flower, spreader);
    }

    private static void assertBotaniaFunctionalSelection(ServerPlayer player, BlockPos expected) {
        ItemStack wand = player.getMainHandItem();
        Optional<GlobalPos> selected = WandOfTheForestItem.getBindingAttempt(wand);
        WandBindable capability = WandBindable.LOOKUP.find(player.level(), expected, Direction.UP);
        if (selected.isEmpty() || !selected.get().dimension().equals(player.level().dimension())
                || !selected.get().pos().equals(expected)) {
            throw new AssertionError("Botania functional flower was not selected: " + selected);
        }
        if (capability == null) {
            throw new AssertionError("Botania functional flower has no WandBindable capability");
        }
        LOGGER.info("SCEX_WAND_ASSERT_PASS check=botania_functional_flower_selected capability={}",
                capability.getClass().getName());
    }

    private static void assertBotaniaFunctionalFlowerBoundToPool(ServerPlayer player,
                                                                  BlockPos flower,
                                                                  BlockPos pool) {
        BlockEntity blockEntity = player.serverLevel().getBlockEntity(flower);
        if (!(blockEntity instanceof Bound bound) || !pool.equals(bound.getBinding())) {
            throw new AssertionError("Botania functional flower binding mismatch: "
                    + (blockEntity instanceof Bound bound ? bound.getBinding() : blockEntity));
        }
        if (WandOfTheForestItem.getBindingAttempt(player.getMainHandItem()).isPresent()) {
            throw new AssertionError("Normal Botania binding selection was not cleared");
        }
        LOGGER.info("SCEX_WAND_ASSERT_PASS check=botania_functional_flower_bound_to_pool flower={} pool={}",
                flower, pool);
    }

    @SuppressWarnings("unchecked")
    private static void assertExtendedPoolSelection(ServerPlayer player, BlockPos pool) throws Exception {
        Class<?> extension = Class.forName("io.github.lounode.extrabotany.common.item.WandOfTheForestItemExtension");
        Method getter = extension.getMethod("getBindingAttempt", ItemStack.class);
        Optional<GlobalPos> selected = (Optional<GlobalPos>) getter.invoke(null, player.getMainHandItem());
        if (selected.isEmpty() || !selected.get().dimension().equals(player.level().dimension())
                || !selected.get().pos().equals(pool)) {
            throw new AssertionError("Mana Pool extension selection mismatch: " + selected);
        }
        LOGGER.info("SCEX_WAND_ASSERT_PASS check=mana_pool_selected_for_manalink pool={}", pool);
    }

    @SuppressWarnings("unchecked")
    private static void assertManalinkBound(ServerPlayer player, BlockPos manalink, BlockPos pool) throws Exception {
        BlockEntity blockEntity = player.serverLevel().getBlockEntity(manalink);
        if (blockEntity == null || !blockEntity.getClass().getName().endsWith("ManalinkBlockEntity")) {
            throw new AssertionError("Manalink block entity unavailable: " + blockEntity);
        }
        Optional<GlobalPos> link = (Optional<GlobalPos>) blockEntity.getClass().getMethod("getLinkPos").invoke(blockEntity);
        if (link.isEmpty() || !link.get().dimension().equals(player.level().dimension())
                || !link.get().pos().equals(pool)) {
            throw new AssertionError("Manalink target mismatch: " + link);
        }
        LOGGER.info("SCEX_WAND_ASSERT_PASS check=manalink_bound_to_pool manalink={} pool={}", manalink, pool);
    }

    private static void assertReloadedBindings(ServerPlayer player) throws Exception {
        BlockEntity extraFlower = player.serverLevel().getBlockEntity(flowerPos);
        if (!(extraFlower instanceof Bound extraBound) || !spreaderPos.equals(extraBound.getBinding())) {
            throw new AssertionError("Reloaded ExtraBotany flower binding mismatch: "
                    + (extraFlower instanceof Bound bound ? bound.getBinding() : extraFlower));
        }
        if (WandBindable.LOOKUP.find(player.level(), flowerPos, Direction.UP) == null) {
            throw new AssertionError("Reloaded ExtraBotany flower lost WandBindable capability");
        }

        BlockEntity botaniaFlower = player.serverLevel().getBlockEntity(botaniaFunctionalFlowerPos);
        if (!(botaniaFlower instanceof Bound botaniaBound) || !poolPos.equals(botaniaBound.getBinding())) {
            throw new AssertionError("Reloaded Botania functional flower binding mismatch: "
                    + (botaniaFlower instanceof Bound bound ? bound.getBinding() : botaniaFlower));
        }

        BlockEntity manalink = player.serverLevel().getBlockEntity(manalinkPos);
        if (manalink == null || !manalink.getClass().getName().endsWith("ManalinkBlockEntity")) {
            throw new AssertionError("Reloaded Manalink block entity unavailable: " + manalink);
        }
        @SuppressWarnings("unchecked")
        Optional<GlobalPos> link = (Optional<GlobalPos>) manalink.getClass()
                .getMethod("getLinkPos").invoke(manalink);
        if (link.isEmpty() || !link.get().dimension().equals(player.level().dimension())
                || !link.get().pos().equals(poolPos)) {
            throw new AssertionError("Reloaded Manalink target mismatch: " + link);
        }
        LOGGER.info("SCEX_WAND_ASSERT_PASS check=save_reload_persisted extra_flower={} "
                        + "botania_flower={} manalink={}",
                extraBound.getBinding(), botaniaBound.getBinding(), link.get());
    }

    private static Block requireBlock(ResourceLocation id) {
        Block block = BuiltInRegistries.BLOCK.get(id);
        if (block == Blocks.AIR) throw new IllegalStateException("Missing block " + id);
        return block;
    }

    private static void afterDelay(int delay, Runnable action) {
        if (++ticks >= delay) action.run();
    }

    private static void beginSaveReload(Minecraft minecraft) {
        LOGGER.info("SCEX_WAND_SAVE_RELOAD_BEGIN world={}", WORLD);
        minecraft.options.keyShift.setDown(false);
        minecraft.player.input.shiftKeyDown = false;
        minecraft.player.setShiftKeyDown(false);
        stage = 22;
        ticks = 0;
        minecraft.getSingleplayerServer().halt(true);
        minecraft.disconnect();
    }

    private static void awaitWorldCloseAndReopen(Minecraft minecraft) {
        if (minecraft.level != null || minecraft.getSingleplayerServer() != null || minecraft.screen == null) {
            return;
        }
        if (++ticks < 60) {
            return;
        }
        LOGGER.info("SCEX_WAND_SAVE_RELOAD_REOPEN world={}", WORLD);
        stage = 23;
        ticks = 0;
        minecraft.createWorldOpenFlows().openWorld(WORLD,
                () -> fail(minecraft, new IllegalStateException("Unable to reopen test world " + WORLD)));
    }

    private static void finish(Minecraft minecraft) {
        LOGGER.info("SCEX_WAND_CLIENT_TEST_PASS extra_flower_to_spreader=true "
                + "botania_flower_to_pool=true pool_to_manalink=true save_reload=true");
        minecraft.options.keyShift.setDown(false);
        minecraft.player.input.shiftKeyDown = false;
        minecraft.getSingleplayerServer().halt(true);
        minecraft.disconnect();
        minecraft.stop();
        stage = 99;
    }

    private static void fail(Minecraft minecraft, Throwable throwable) {
        LOGGER.error("SCEX_WAND_CLIENT_TEST_FAIL", throwable);
        minecraft.options.keyShift.setDown(false);
        if (minecraft.player != null && minecraft.player.input != null) {
            minecraft.player.input.shiftKeyDown = false;
        }
        minecraft.stop();
        stage = 99;
    }

    @FunctionalInterface
    private interface ServerAssertion {
        void run(ServerPlayer player) throws Exception;
    }
}
