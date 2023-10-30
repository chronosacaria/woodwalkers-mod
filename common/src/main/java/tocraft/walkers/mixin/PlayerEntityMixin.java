package tocraft.walkers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import tocraft.walkers.Walkers;
import tocraft.walkers.api.PlayerShape;
import tocraft.walkers.mixin.accessor.EntityAccessor;
import tocraft.walkers.mixin.accessor.IronGolemEntityAccessor;
import tocraft.walkers.mixin.accessor.LivingEntityAccessor;
import tocraft.walkers.mixin.accessor.MobEntityAccessor;
import tocraft.walkers.mixin.accessor.RavagerEntityAccessor;
import tocraft.walkers.registry.WalkersEntityTags;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntityMixin {

	@Shadow
	public abstract boolean isSpectator();

	@Shadow
	public abstract EntityDimensions getDimensions(Pose pose);

	@Shadow
	public abstract boolean isSwimming();

	private PlayerEntityMixin(EntityType<? extends LivingEntity> type, Level world) {
		super(type, world);
	}

	@Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
	private void getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		LivingEntity entity = PlayerShape.getCurrentShape((Player) (Object) this);

		if (entity != null) {
			cir.setReturnValue(entity.getDimensions(pose));
		}
	}

	/**
	 * When a player turns into an Aquatic shape, they lose breath outside water.
	 *
	 * @param ci mixin callback info
	 */
	@Inject(method = "tick", at = @At("HEAD"))
	private void tickAquaticBreathingOutsideWater(CallbackInfo ci) {
		LivingEntity shape = PlayerShape.getCurrentShape((Player) (Object) this);

		if (shape != null) {
			if (Walkers.isAquatic(shape)) {
				int air = this.getAirSupply();

				// copy of WaterCreatureEntity#tickWaterBreathingAir
				if (this.isAlive() && !this.isInWaterOrBubble()) {
					int i = EnchantmentHelper.getRespiration((LivingEntity) (Object) this);

					// If the player has respiration, 50% chance to not consume air
					if (i > 0) {
						if (random.nextInt(i + 1) <= 0) {
							this.setAirSupply(air - 1);
						}
					}

					// No respiration, decrease air as normal
					else {
						this.setAirSupply(air - 1);
					}

					// Air has ran out, start drowning
					if (this.getAirSupply() == -20) {
						this.setAirSupply(0);
						this.hurt(DamageSource.FALL, 2.0F);
					}
				} else {
					this.setAirSupply(air + 1);
				}
			}
		}
	}

	@Inject(method = "getStandingEyeHeight", at = @At("HEAD"), cancellable = true)
	private void shape_getStandingEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
		// cursed
		try {
			LivingEntity shape = PlayerShape.getCurrentShape((Player) (Object) this);

			if (shape != null) {
				cir.setReturnValue(
						((LivingEntityAccessor) shape).callGetEyeHeight(getPose(), getDimensions(getPose())));
			}
		} catch (Exception ignored) {

		}
	}

	@Environment(EnvType.CLIENT)
	@Override
	public float getEyeHeight(Pose pose) {
		LivingEntity shape = PlayerShape.getCurrentShape((Player) (Object) this);

		if (shape != null) {
			return shape.getEyeHeight(pose);
		} else {
			return this.getEyeHeight(pose, this.getDimensions(pose));
		}
	}

	@Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
	private void getHurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
		LivingEntity shape = PlayerShape.getCurrentShape((Player) (Object) this);

		if (Walkers.CONFIG.useShapeSounds && shape != null) {
			cir.setReturnValue(((LivingEntityAccessor) shape).callGetHurtSound(source));
		}
	}

	// todo: separate mixin for ambient sounds
	private int shape_ambientSoundChance = 0;

	@Inject(method = "tick", at = @At("HEAD"))
	private void tickAmbientSounds(CallbackInfo ci) {
		LivingEntity shape = PlayerShape.getCurrentShape((Player) (Object) this);

		if (!level.isClientSide && Walkers.CONFIG.playAmbientSounds && shape instanceof Mob) {
			Mob mobShape = (Mob) shape;

			if (this.isAlive() && this.random.nextInt(1000) < this.shape_ambientSoundChance++) {
				// reset sound delay
				this.shape_ambientSoundChance = -mobShape.getAmbientSoundInterval();

				// play ambient sound
				SoundEvent sound = ((MobEntityAccessor) mobShape).callGetAmbientSound();
				if (sound != null) {
					float volume = ((LivingEntityAccessor) mobShape).callGetSoundVolume();
					float pitch = ((LivingEntityAccessor) mobShape).callGetVoicePitch();

					// By default, players can not hear their own ambient noises.
					// This is because ambient noises can be very annoying.
					if (Walkers.CONFIG.hearSelfAmbient) {
						this.level.playSound(null, this.getX(), this.getY(), this.getZ(), sound,
								this.getSoundSource(), volume, pitch);
					} else {
						this.level.playSound((Player) (Object) this, this.getX(), this.getY(), this.getZ(), sound,
								this.getSoundSource(), volume, pitch);
					}
				}
			}
		}
	}

	@Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
	private void getDeathSound(CallbackInfoReturnable<SoundEvent> cir) {
		LivingEntity shape = PlayerShape.getCurrentShape((Player) (Object) this);

		if (Walkers.CONFIG.useShapeSounds && shape != null) {
			cir.setReturnValue(((LivingEntityAccessor) shape).callGetDeathSound());
		}
	}

	@Inject(method = "getFallSounds", at = @At("HEAD"), cancellable = true)
	private void getFallSounds(CallbackInfoReturnable<LivingEntity.Fallsounds> cir) {
		LivingEntity shape = PlayerShape.getCurrentShape((Player) (Object) this);

		if (Walkers.CONFIG.useShapeSounds && shape != null) {
			cir.setReturnValue(shape.getFallSounds());
		}
	}

	@Inject(method = "attack", at = @At("HEAD"))
	protected void shape_tryAttack(Entity target, CallbackInfo ci) {
		LivingEntity shape = PlayerShape.getCurrentShape((Player) (Object) this);

		if (shape instanceof IronGolem golem) {
			((IronGolemEntityAccessor) golem).setAttackTicksLeft(10);
		}

		if (shape instanceof Ravager ravager) {
			((RavagerEntityAccessor) ravager).setAttackTick(10);
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void tickGolemAttackTicks(CallbackInfo ci) {
		LivingEntity shape = PlayerShape.getCurrentShape((Player) (Object) this);

		if (shape instanceof IronGolem golem) {
			IronGolemEntityAccessor accessor = (IronGolemEntityAccessor) golem;
			if (accessor.getAttackTicksLeft() > 0) {
				accessor.setAttackTicksLeft(accessor.getAttackTicksLeft() - 1);
			}
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void tickRavagerAttackTicks(CallbackInfo ci) {
		LivingEntity shape = PlayerShape.getCurrentShape((Player) (Object) this);

		if (shape instanceof Ravager ravager) {
			RavagerEntityAccessor accessor = (RavagerEntityAccessor) ravager;
			if (accessor.getAttackTick() > 0) {
				accessor.setAttackTick(accessor.getAttackTick() - 1);
			}
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void tickFire(CallbackInfo ci) {
		Player player = (Player) (Object) this;
		LivingEntity shape = PlayerShape.getCurrentShape(player);

		if (!player.level.isClientSide && !player.isCreative() && !player.isSpectator()) {
			// check if the player is shape
			if (shape != null) {
				EntityType<?> type = shape.getType();

				// check if the player's current shape burns in sunlight
				if (type.is(WalkersEntityTags.BURNS_IN_DAYLIGHT)) {
					boolean bl = this.isInDaylight();
					if (bl) {

						// Can't burn in the rain
						if (player.level.isRaining()) {
							return;
						}

						// check for helmets to negate burning
						ItemStack itemStack = player.getItemBySlot(EquipmentSlot.HEAD);
						if (!itemStack.isEmpty()) {
							if (itemStack.isDamageableItem()) {

								// damage stack instead of burning player
								itemStack.setDamageValue(itemStack.getDamageValue() + player.getRandom().nextInt(2));
								if (itemStack.getDamageValue() >= itemStack.getMaxDamage()) {
									player.broadcastBreakEvent(EquipmentSlot.HEAD);
									player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
								}
							}

							bl = false;
						}

						// set player on fire
						if (bl) {
							player.setSecondsOnFire(8);
						}
					}
				}
			}
		}
	}

	@Unique
	private boolean isInDaylight() {
		if (level.isDay() && !level.isClientSide) {
			float brightnessAtEyes = getBrightness();
			BlockPos daylightTestPosition = new BlockPos(getX(), (double) Math.round(getY()), getZ());

			// move test position up one block for boats
			if (getVehicle() instanceof Boat) {
				daylightTestPosition = daylightTestPosition.above();
			}

			return brightnessAtEyes > 0.5F && random.nextFloat() * 30.0F < (brightnessAtEyes - 0.4F) * 2.0F
					&& level.canSeeSky(daylightTestPosition);
		}

		return false;
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void tickTemperature(CallbackInfo ci) {
		Player player = (Player) (Object) this;
		LivingEntity shape = PlayerShape.getCurrentShape(player);

		if (!player.isCreative() && !player.isSpectator()) {
			// check if the player is shape
			if (shape != null) {
				EntityType<?> type = shape.getType();

				// damage player if they are an shape that gets hurt by high temps (eg. snow
				// golem in nether)
				if (type.is(WalkersEntityTags.HURT_BY_HIGH_TEMPERATURE)) {
					Biome biome = level.getBiome(blockPosition()).value();
					if (!biome.coldEnoughToSnow(blockPosition())) {
						player.hurt(DamageSource.ON_FIRE, 1.0f);
					}
				}
			}
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void tickWalkers(CallbackInfo ci) {
		if (!level.isClientSide) {
			Player player = (Player) (Object) this;
			LivingEntity shape = PlayerShape.getCurrentShape(player);

			// assign basic data to entity from player on server; most data transferring
			// occurs on client
			if (shape != null) {
				shape.setPosRaw(player.getX(), player.getY(), player.getZ());
				shape.setYHeadRot(player.getYHeadRot());
				shape.setJumping(((LivingEntityAccessor) player).isJumping());
				shape.setSprinting(player.isSprinting());
				shape.setArrowCount(player.getArrowCount());
				shape.setInvulnerable(true);
				shape.setNoGravity(true);
				shape.setShiftKeyDown(player.isShiftKeyDown());
				shape.setSwimming(player.isSwimming());
				shape.startUsingItem(player.getUsedItemHand());
				shape.setPose(player.getPose());

				if (shape instanceof TamableAnimal) {
					((TamableAnimal) shape).setInSittingPose(player.isShiftKeyDown());
					((TamableAnimal) shape).setOrderedToSit(player.isShiftKeyDown());
				}

				((EntityAccessor) shape).shape_callSetFlag(7, player.isFallFlying());

				((LivingEntityAccessor) shape).callUpdatingUsingItem();
				PlayerShape.sync((ServerPlayer) player); // safe cast - context is server world
			}
		}
	}
}
