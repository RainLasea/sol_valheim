package vice.sol_valheim;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Cache selector syntax only: tag membership must follow datapack reloads. */
public final class FoodExclusions {
    private static final Logger LOGGER = LoggerFactory.getLogger(SOLValheim.MOD_ID);
    public static final TagKey<Item> EXCLUDED_FOODS = TagKey.create(Registries.ITEM,
            ResourceLocation.tryParse("sol_valheim:excluded_foods"));

    private FoodExclusions() {}

    public static boolean isExcluded(ItemStack stack) {
        return !stack.isEmpty() && isExcluded(stack.getItem());
    }

    public static boolean isExcluded(Item item) {
        var rules = rules();
        var holder = item.builtInRegistryHolder();
        if (holder.is(EXCLUDED_FOODS) || rules.items.contains(BuiltInRegistries.ITEM.getKey(item))) return true;
        for (var tag : rules.tags) if (holder.is(tag)) return true;
        return false;
    }

    public static void validate(ModConfig.Common common) {
        rules(common);
    }

    /** Run only after item registration; missing optional mods are harmless. */
    public static void warnUnknownItems() {
        for (var id : rules().items) {
            if (!BuiltInRegistries.ITEM.containsKey(id))
                LOGGER.warn("foodBlacklist item '{}' is not registered; ignoring it until the item is available", id);
        }
    }

    private static Rules rules() {
        return rules(SOLValheim.Config.common);
    }

    private static Rules rules(ModConfig.Common common) {
        var selectors = common.foodBlacklist == null ? List.<String>of() : common.foodBlacklist;
        if (common.exclusionRules == null || !common.exclusionRules.selectors.equals(selectors))
            common.exclusionRules = new Rules(selectors);
        return common.exclusionRules;
    }

    static final class Rules {
        final List<String> selectors;
        final Set<ResourceLocation> items = new HashSet<>();
        final List<TagKey<Item>> tags = new ArrayList<>();

        Rules(List<String> selectors) {
            this.selectors = new ArrayList<>(selectors);
            for (var selector : selectors) {
                var text = selector == null ? "" : selector.trim();
                boolean tag = text.startsWith("#");
                var value = tag ? text.substring(1) : text;
                var id = value.isEmpty() ? null : ResourceLocation.tryParse(value);
                if (id == null) {
                    LOGGER.warn("Ignoring invalid foodBlacklist entry '{}'; expected an item ID or #tag ID", selector);
                } else if (tag) tags.add(TagKey.create(Registries.ITEM, id));
                else items.add(id);
            }
        }
    }
}
