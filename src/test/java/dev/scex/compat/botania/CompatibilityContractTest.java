package dev.scex.compat.botania;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityContractTest {
    @Test
    void mixinRunsBeforeExtraBotanyPoolInterceptor() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/dev/scex/compat/botania/mixin/WandOfTheForestItemMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("at = @At(\"HEAD\")"));
        assertTrue(source.contains("targetBlockEntity instanceof ManaPool"));
        assertTrue(source.contains("WandBindable.LOOKUP.find"));
        assertTrue(source.contains("setBindingAttempt(wand, null, null, null)"));
        assertTrue(source.contains("scex$bindManalink"));
        assertTrue(source.contains("setLinkPos"));
        assertTrue(source.contains("manalink.setChanged()"));
    }

    @Test
    void capabilityBridgeIsRestrictedToExtraBotanyAndBindableInstances() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/dev/scex/compat/botania/ScexBotaniaExtraBotanyCompat.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("EXTRABOTANY_NAMESPACE"));
        assertTrue(source.contains("blockEntity instanceof WandBindable"));
        assertTrue(source.contains("registerBlockEntity"));
    }
}
