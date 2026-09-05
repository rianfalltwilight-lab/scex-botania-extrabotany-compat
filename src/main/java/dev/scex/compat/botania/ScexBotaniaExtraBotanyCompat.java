package dev.scex.compat.botania;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import vazkii.botania.api.block.WandBindable;
import vazkii.botania.api.neoforge.BotaniaNeoForgeCapabilities;

@Mod(ScexBotaniaExtraBotanyCompat.MOD_ID)
public final class ScexBotaniaExtraBotanyCompat {
    public static final String MOD_ID = "scex_botania_extrabotany_compat";
    private static final String EXTRABOTANY_NAMESPACE = "extrabotany";

    public ScexBotaniaExtraBotanyCompat(IEventBus modBus) {
        modBus.addListener(this::registerCapabilities);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        BlockCapability<WandBindable, Direction> capability =
                BotaniaNeoForgeCapabilities.getBlockApiLookupById(WandBindable.LOOKUP);

        BuiltInRegistries.BLOCK_ENTITY_TYPE.entrySet().stream()
                .filter(entry -> EXTRABOTANY_NAMESPACE.equals(entry.getKey().location().getNamespace()))
                .map(java.util.Map.Entry::getValue)
                .forEach(type -> registerWandBindable(event, capability, type));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerWandBindable(RegisterCapabilitiesEvent event,
                                             BlockCapability<WandBindable, Direction> capability,
                                             BlockEntityType<?> type) {
        event.registerBlockEntity(capability, (BlockEntityType) type,
                (BlockEntity blockEntity, Direction side) ->
                        blockEntity instanceof WandBindable bindable ? bindable : null);
    }
}
