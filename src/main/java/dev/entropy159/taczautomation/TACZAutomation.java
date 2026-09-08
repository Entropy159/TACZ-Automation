package dev.entropy159.taczautomation;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModRecipe;
import com.tacz.guns.resource.CommonAssetsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mod(TACZAutomation.MODID)
public class TACZAutomation {
    public static final String MODID = "taczautomation";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final List<JsonObject> RECIPES = new ArrayList<>();
    public static final Set<ResourceLocation> ALLOWED = Set.of(ResourceLocation.parse("tacz:ammo"), ResourceLocation.parse("lrtactical:throwable"), ResourceLocation.parse("lrtactical:consumable"));
    public static final Set<String> ALLOWED_NBT = Set.of("AmmoId", "ThrowableId", "ConsumableId");
    private static boolean updated = false;

    public TACZAutomation(IEventBus bus, ModContainer container) {
        NeoForge.EVENT_BUS.register(this);
        container.registerConfig(ModConfig.Type.COMMON, TACZAutomationConfig.SPEC);
    }

    public static void readCache() {
        File cache = getCacheFile();
        if (cache.exists()) {
            try (FileReader reader = new FileReader(cache)) {
                JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    RECIPES.add(i, array.get(i).getAsJsonObject());
                }
            } catch (IOException e) {
                LOGGER.error("Error reading cache file! ", e);
            }
        }
    }

    public static void writeCache() {
        File cache = getCacheFile();
        if (!cache.exists()) {
            try {
                if (!cache.createNewFile()) {
                    LOGGER.error("Unknown error creating cache file!");
                }
            } catch (IOException e) {
                LOGGER.error("Error creating cache file! ", e);
            }
        }
        try (FileWriter writer = new FileWriter(cache)) {
            writer.write(GSON.toJson(RECIPES));
        } catch (IOException e) {
            LOGGER.error("Error writing cache file! ", e);
        }
    }

    public static File getCacheFile() {
        return FMLPaths.CONFIGDIR.get().resolve(MODID + "_cache.json").toFile();
    }

    public static JsonElement ingredientJson(Ingredient ingredient) {
        return Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient).result().orElseThrow(() -> new IllegalStateException("Failed to encode Ingredient to JSON"));
    }

    @SubscribeEvent
    public void recipes(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            TACZAutomation.loadRecipes(event.getRegistryAccess());
        }
    }

    @SubscribeEvent
    public void onServerLoad(PlayerEvent.PlayerLoggedInEvent event) {
        if (updated) {
            if (event.getEntity().hasPermissions(3)) {
                event.getEntity().sendSystemMessage(Component.literal("TACZ Automation has new recipes, please /reload").withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    public static void loadRecipes(RegistryAccess registryAccess) {
        if (CommonAssetsManager.getInstance() != null && CommonAssetsManager.getInstance().recipeManager != null) {
            String old = GSON.toJson(RECIPES);
            List<GunSmithTableRecipe> recipes = CommonAssetsManager.getInstance().recipeManager.getAllRecipesFor(ModRecipe.GUN_SMITH_TABLE_CRAFTING.get()).stream().map(RecipeHolder::value).toList();
            RECIPES.clear();
            for (GunSmithTableRecipe recipe : recipes) {
                recipe.init(registryAccess);
                ResourceLocation output = BuiltInRegistries.ITEM.getKey(recipe.getOutput().getItem());
                if (ALLOWED.contains(output) && TACZAutomationConfig.TOGGLES.containsKey(output.toString()) && TACZAutomationConfig.TOGGLES.get(output.toString()).get()) {
                    RECIPES.add(recipe(recipe, output));
                }
            }
            LOGGER.info("Generated {} TACZ automation recipes", RECIPES.size());
            writeCache();
            updated = !old.equals(GSON.toJson(RECIPES));
        } else {
            LOGGER.error("Recipe manager is null!");
        }
    }

    private static JsonObject recipe(GunSmithTableRecipe recipe, ResourceLocation type) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:compacting");
        JsonArray ingredients = new JsonArray();
        for (GunSmithTableIngredient i : recipe.getInputs()) {
            for (int index = 0; index < i.getCount(); index++) {
                ingredients.add(ingredientJson(i.getIngredient()));
            }
        }
        json.add("ingredients", ingredients);
        JsonArray results = new JsonArray();
        JsonObject result = new JsonObject();
        result.addProperty("id", type.toString());
        result.addProperty("amount", recipe.getOutput().getCount());
        if (recipe.getOutput().has(DataComponents.CUSTOM_DATA)) {
            JsonObject nbt = new JsonObject();
            nbt.addProperty("minecraft:custom_data", recipe.getOutput().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().toString());
            result.add("components", nbt);
        }
        results.add(result);
        json.add("results", results);
        return json;
    }
}
