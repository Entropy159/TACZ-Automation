package dev.entropy159.taczautomation.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.entropy159.taczautomation.TACZAutomation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"))
    public void recipes(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller filler, CallbackInfo ci) {
        int i = 0;
        TACZAutomation.readCache();
        for (JsonObject recipe : TACZAutomation.RECIPES) {
            i++;
            map.put(ResourceLocation.parse(TACZAutomation.MODID + i), recipe);
        }
    }
}