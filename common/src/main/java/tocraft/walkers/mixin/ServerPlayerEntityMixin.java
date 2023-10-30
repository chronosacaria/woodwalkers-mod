package tocraft.walkers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import tocraft.walkers.Walkers;
import tocraft.walkers.api.FlightHelper;
import tocraft.walkers.api.PlayerShapeChanger;
import tocraft.walkers.mixin.accessor.PlayerEntityAccessor;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin extends Player {

	@Override
	@Shadow
	public abstract boolean isCreative();

	@Override
	@Shadow
	public abstract boolean isSpectator();

	@Override
	@Shadow
	public abstract void displayClientMessage(Component message, boolean actionBar);

	public ServerPlayerEntityMixin(Level world, BlockPos pos, float yaw, GameProfile gameProfile) {
		super(world, pos, yaw, gameProfile);
	}

	@Inject(method = "die", at = @At("HEAD"))
	private void revoke2ndShapeOnDeath(DamageSource source, CallbackInfo ci) {
		if (Walkers.CONFIG.revoke2ndShapeOnDeath && !this.isCreative() && !this.isSpectator()) {
			PlayerShapeChanger.change2ndShape((ServerPlayer) (Object) this, null);
		}
	}

	@Inject(method = "initMenu()V", at = @At("HEAD"))
	private void onSpawn(CallbackInfo ci) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		if (Walkers.hasFlyingPermissions(player)) {
			if (!FlightHelper.hasFlight(player)) {
				FlightHelper.grantFlightTo(player);
				((PlayerEntityAccessor) player).getAbilities().setFlyingSpeed(Walkers.CONFIG.flySpeed);
				onUpdateAbilities();
			}

			FlightHelper.grantFlightTo(player);
		}
	}
}
