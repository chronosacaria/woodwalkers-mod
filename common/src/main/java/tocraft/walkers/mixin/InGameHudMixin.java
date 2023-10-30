package tocraft.walkers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.gui.Gui;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import tocraft.walkers.Walkers;
import tocraft.walkers.api.PlayerShape;
import tocraft.walkers.registry.WalkersEntityTags;

@Mixin(Gui.class)
public abstract class InGameHudMixin {

	@Shadow
	protected abstract Player getCameraPlayer();

	@ModifyArg(method = "renderPlayerHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isEyeInFluid(Lnet/minecraft/tags/Tag;)Z"))
	private Tag<Fluid> shouldRenderBreath(Tag<Fluid> tag) {
		Player player = this.getCameraPlayer();
		LivingEntity shape = PlayerShape.getCurrentShape(player);

		if (shape != null) {
			if (Walkers.isAquatic(shape)
					|| shape.getType().is(WalkersEntityTags.UNDROWNABLE) && player.isEyeInFluid(FluidTags.WATER)) {
				return FluidTags.LAVA; // will cause isEyeInFluid to return false, preventing air render
			}
		}

		return tag;
	}
}
