package vice.sol_valheim;

import com.mojang.authlib.GameProfile;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import vice.sol_valheim.accessors.LunchContainerAccessor;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;
import java.util.UUID;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OnionRuntimeChecks {
    @Test void actualPublishedContainersHandleUseConsumptionAndOneIdentity() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        SOLValheim.Config = new ModConfig();
        SOLValheim.Config.common.eatAgainPercentage = 0.5f;
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var level = mock(Level.class);
        when(level.registryAccess()).thenReturn(registries);
        var side = Level.class.getDeclaredField("isClientSide");
        side.setAccessible(true);
        side.setBoolean(level, false);
        var random = Level.class.getDeclaredField("random");
        random.setAccessible(true);
        random.set(level, net.minecraft.util.RandomSource.create(1));
        var player = new TestPlayer(level);
        var onionEvent = Class.forName("team.creative.solonion.common.SOLOnion").getField("EVENT").get(null);
        // This synthetic player has no server connection for Onion's network sync.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(onionEvent);
        try {
            var data = ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData();
            for (String name : new String[]{"lunchbag", "lunchbox", "golden_lunchbox"}) {
                data.clear();
                var id = ResourceLocation.parse("solonion:" + name);
                assertTrue(BuiltInRegistries.ITEM.containsKey(id), "Onion must load for this integration check");
                var box = BuiltInRegistries.ITEM.get(id);
                assertInstanceOf(LunchContainerAccessor.class, box);
                var stack = box.getDefaultInstance();
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                assertEquals(InteractionResult.PASS, box.use(level, player, InteractionHand.MAIN_HAND).getResult());
                assertFalse(data.hasFood());
                setContents(stack, Items.BREAD.getDefaultInstance());
                assertEquals(InteractionResult.CONSUME, box.use(level, player, InteractionHand.MAIN_HAND).getResult());
                box.finishUsingItem(stack, level, player);
                SOLValheim.consume(player, stack); // Outer finish event must not count twice.
                assertEquals(1, data.ItemEntries.size());
                assertSame(box, data.ItemEntries.getFirst().item);
                assertEquals(ModConfig.getFoodConfig(Items.BREAD).nutrition, data.ItemEntries.getFirst().getConfig().nutrition);
                assertTrue(((LunchContainerAccessor) box).sol_valheim$getActualFood(player, stack).isEmpty());
                setContents(stack, Items.APPLE.getDefaultInstance());
                stack.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("New box contents"));
                assertEquals(InteractionResult.FAIL, box.use(level, player, InteractionHand.MAIN_HAND).getResult());
                data.ItemEntries.getFirst().ticksLeft = data.ItemEntries.getFirst().getConfig().getTime() / 2 - 1;
                assertEquals(InteractionResult.CONSUME, box.use(level, player, InteractionHand.MAIN_HAND).getResult());
                box.finishUsingItem(stack, level, player);
                assertEquals(1, data.ItemEntries.size());
                assertSame(box, data.ItemEntries.getFirst().item);
                assertEquals(ModConfig.getFoodConfig(Items.APPLE).nutrition, data.ItemEntries.getFirst().getConfig().nutrition);
                var restored = ValheimFoodData.read(data.save(new CompoundTag(), registries), registries);
                assertEquals(1, restored.ItemEntries.size());
                assertEquals(ModConfig.getFoodConfig(Items.APPLE).nutrition, restored.ItemEntries.getFirst().getConfig().nutrition);
                player.stopUsingItem();
            }
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(onionEvent);
        }
    }

    private static void setContents(ItemStack stack, ItemStack food) {
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(food)));
    }
    private static class TestPlayer extends Player {
        TestPlayer(Level level) { super(level, BlockPos.ZERO, 0f, new GameProfile(UUID.randomUUID(), "LunchTest")); }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
    }
}
