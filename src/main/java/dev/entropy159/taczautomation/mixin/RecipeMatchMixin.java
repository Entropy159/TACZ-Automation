package dev.entropy159.taczautomation.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import dev.entropy159.taczautomation.TACZAutomation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FilteringBehaviour.class)
public abstract class RecipeMatchMixin extends BlockEntityBehaviour {
    public RecipeMatchMixin(SmartBlockEntity be) {
        super(be);
    }

    @Shadow
    public abstract boolean isActive();

    @Shadow
    protected FilterItemStack filter;

    @ModifyReturnValue(method = "test(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), remap = false)
    public boolean nbtForAmmo(boolean original, @Local(argsOnly = true) ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (TACZAutomation.ALLOWED_NBT.stream().anyMatch(nbt -> tag.contains(nbt, 8))) {
            return !isActive() || filter.test(blockEntity.getLevel(), stack, true);
        }
        return original;
    }
}