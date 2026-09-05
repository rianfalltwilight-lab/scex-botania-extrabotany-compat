package dev.scex.compat.botania.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.api.block.WandBindable;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.common.item.WandOfTheForestItem;

import java.lang.reflect.Method;
import java.util.Optional;
import org.slf4j.Logger;

/**
 * ExtraBotany's pool-selection mixin runs before Botania can complete a normal
 * flower binding and consumes the click. Complete that standard binding first.
 */
@Mixin(value = WandOfTheForestItem.class, remap = false)
public abstract class WandOfTheForestItemMixin {
    @Unique
    private static final Logger SCEX_LOGGER = LogUtils.getLogger();

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true, remap = true)
    private void scex$completeFlowerToPoolBinding(UseOnContext context,
                                                  CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive()) {
            return;
        }

        ItemStack wand = context.getItemInHand();
        if (!WandOfTheForestItem.getBindMode(wand)) {
            return;
        }

        Level level = context.getLevel();
        BlockPos target = context.getClickedPos();
        BlockEntity targetBlockEntity = level.getBlockEntity(target);
        if (targetBlockEntity != null && targetBlockEntity.getClass().getName().endsWith("ManalinkBlockEntity")) {
            SCEX_LOGGER.debug("SCEX_MANALINK_BIND_TRACE side={} type={} sneaking={}",
                    level.isClientSide ? "client" : "server",
                    BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(targetBlockEntity.getType()),
                    player.isSecondaryUseActive());
        }

        // ExtraBotany 2.0-scex.5-dev records the pool correctly but its injected
        // Manalink completion only mutates the logical client in this stack.
        // Complete it at method entry so the same write occurs on both sides.
        if (scex$isManalink(targetBlockEntity) && scex$bindManalink(targetBlockEntity, wand)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        if (!(targetBlockEntity instanceof ManaPool)) {
            return;
        }

        Optional<GlobalPos> selected = WandOfTheForestItem.getBindingAttempt(wand);
        if (selected.isEmpty()) {
            return;
        }

        GlobalPos source = selected.get();
        if (!source.dimension().equals(level.dimension()) || source.pos().equals(target)) {
            return;
        }

        Direction targetSide = context.getClickedFace();
        Direction sourceSide = WandOfTheForestItem.getBindingSide(wand).orElse(null);
        WandBindable bindable = WandBindable.LOOKUP.find(level, source.pos(), sourceSide);

        // Match Botania's normal semantics: a valid second click consumes the
        // selection even if the source refuses this particular target.
        WandOfTheForestItem.setBindingAttempt(wand, null, null, null);
        if (bindable != null) {
            if (bindable.bindTo(player, wand, target, targetSide)) {
                WandOfTheForestItem.doParticleBeamWithOffset(level, source.pos(), target);
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Unique
    private static boolean scex$isManalink(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        ResourceLocation typeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        return typeId != null && typeId.getNamespace().equals("extrabotany")
                && typeId.getPath().equals("manalink");
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static boolean scex$bindManalink(BlockEntity manalink, ItemStack wand) {
        try {
            Class<?> extension = Class.forName(
                    "io.github.lounode.extrabotany.common.item.WandOfTheForestItemExtension");
            Method getBindingAttempt = extension.getMethod("getBindingAttempt", ItemStack.class);
            Optional<GlobalPos> selected = (Optional<GlobalPos>) getBindingAttempt.invoke(null, wand);
            if (selected.isEmpty()) {
                return false;
            }
            Method setLinkPos = manalink.getClass().getMethod("setLinkPos", GlobalPos.class);
            setLinkPos.invoke(manalink, selected.get());
            // ExtraBotany's setter only assigns the field. Mark the block entity
            // dirty so a later chunk save persists a newly chosen pool.
            manalink.setChanged();
            SCEX_LOGGER.debug("SCEX_MANALINK_BIND_APPLIED side={} target={}",
                    manalink.getLevel() != null && manalink.getLevel().isClientSide ? "client" : "server",
                    selected.get());
            return true;
        } catch (ReflectiveOperationException | ClassCastException exception) {
            SCEX_LOGGER.warn("Unable to complete ExtraBotany Manalink binding", exception);
            return false;
        }
    }
}
