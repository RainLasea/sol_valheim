package vice.sol_valheim;

import com.mojang.authlib.GameProfile;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import vice.sol_valheim.accessors.PlayerEntityMixinDataAccessor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupplementariesRuntimeChecks {
    @Test void basketRecordsOnlyTheConsumedFoodAndHonorsItsCooldown() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        SOLValheim.Config = new ModConfig();
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
        var data = ((PlayerEntityMixinDataAccessor) player).sol_valheim$getFoodData();
        var id = ResourceLocation.parse("supplementaries:lunch_basket");
        assertTrue(BuiltInRegistries.ITEM.containsKey(id), "Supplementaries must load for this check");
        var basket = BuiltInRegistries.ITEM.get(id).getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, basket);

        setContents(basket, new ItemStack(Items.BREAD, 2));
        assertNull(ModConfig.getFoodConfig(basket.getItem()));
        assertNull(ModConfig.getFoodConfig(basket, player));
        assertFalse(data.eatConfiguredItem(basket, ModConfig.getFoodConfig(Items.BREAD)));
        assertFalse(SOLValheim.Config.common.foodConfigs.containsKey(id.toString()));
        assertEquals(InteractionResult.CONSUME, basket.use(level, player, InteractionHand.MAIN_HAND).getResult());
        basket.finishUsingItem(level, player);
        SOLValheim.consume(player, basket); // The outer finish event must be ignored.
        assertEquals(1, data.ItemEntries.size());
        assertSame(Items.BREAD, data.ItemEntries.getFirst().item);
        assertEquals(ModConfig.getFoodConfig(Items.BREAD).nutrition, data.getTotalFoodNutrition());
        assertFalse(data.canEat(Items.BREAD));
        assertEquals(InteractionResult.FAIL, basket.use(level, player, InteractionHand.MAIN_HAND).getResult());

        setContents(basket, Items.APPLE.getDefaultInstance());
        assertEquals(InteractionResult.CONSUME, basket.use(level, player, InteractionHand.MAIN_HAND).getResult());
        basket.finishUsingItem(level, player); // Consume the last item: the basket becomes empty.
        SOLValheim.consume(player, basket);
        assertEquals(2, data.ItemEntries.size());
        assertSame(Items.APPLE, data.ItemEntries.get(1).item);
        assertTrue(data.eatItem(Items.CARROT));
        assertEquals(3, data.ItemEntries.size());
        assertTrue(data.ItemEntries.stream().noneMatch(entry -> SupplementariesCompat.isLunchBasket(entry.item)));
        assertFalse(data.canEat(Items.BAKED_POTATO));

        var saved = data.save(new CompoundTag(), registries);
        // Replace one entry with a basket recorded by the buggy version, with saved food values.
        saved.put("stack0", GameVersion.saveStack(basket, registries));
        saved.putString("id0", id.toString());
        var oldValues = new CompoundTag();
        oldValues.putInt("nutrition", 5);
        oldValues.putFloat("saturation", 0.6f);
        oldValues.putFloat("regen", 1f);
        saved.put("values0", oldValues);
        var restored = ValheimFoodData.read(saved, registries);
        assertEquals(2, restored.ItemEntries.size());
        assertTrue(restored.ItemEntries.stream().noneMatch(entry -> SupplementariesCompat.isLunchBasket(entry.item)));
        assertTrue(restored.canEat(Items.BREAD));
        saved.remove("stack0"); // Legacy saves recorded only the item ID.
        assertEquals(2, ValheimFoodData.read(saved, registries).ItemEntries.size());

        setContents(basket, Items.ROTTEN_FLESH.getDefaultInstance());
        assertEquals(InteractionResult.CONSUME, basket.use(level, player, InteractionHand.MAIN_HAND).getResult());
        basket.finishUsingItem(level, player);
        SOLValheim.consume(player, basket);
        assertFalse(data.hasFood());

        setContents(basket, Items.HONEY_BOTTLE.getDefaultInstance());
        assertEquals(InteractionResult.CONSUME, basket.use(level, player, InteractionHand.MAIN_HAND).getResult());
        basket.finishUsingItem(level, player);
        SOLValheim.consume(player, basket);
        assertTrue(data.ItemEntries.isEmpty());
        assertNotNull(data.DrinkSlot);
        assertSame(Items.HONEY_BOTTLE, data.DrinkSlot.item);
        assertFalse(data.canEat(Items.HONEY_BOTTLE));
    }

    @SuppressWarnings("unchecked")
    private static void setContents(ItemStack basket, ItemStack food) throws Exception {
        var component = (DataComponentType<Object>) basket.getItem().getClass().getMethod("getComponentType").invoke(basket.getItem());
        var content = basket.get(component);
        var empty = content.getClass().getMethod("empty", int.class).invoke(null, 9);
        var mutable = empty.getClass().getMethod("toMutable").invoke(empty);
        mutable.getClass().getMethod("tryAdding", ItemStack.class).invoke(mutable, food);
        mutable.getClass().getMethod("switchMode").invoke(mutable);
        basket.set(component, mutable.getClass().getMethod("toImmutable").invoke(mutable));
    }

    private static class TestPlayer extends Player {
        TestPlayer(Level level) { super(level, BlockPos.ZERO, 0f, new GameProfile(UUID.randomUUID(), "BasketTest")); }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
    }
}
