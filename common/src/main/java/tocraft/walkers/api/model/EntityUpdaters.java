package tocraft.walkers.api.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import tocraft.walkers.Walkers;
import tocraft.walkers.api.PlayerAbilities;
import tocraft.walkers.api.model.impl.ShulkerEntityUpdater;
import tocraft.walkers.api.model.impl.SquidEntityUpdater;
import tocraft.walkers.impl.NearbySongAccessor;
import tocraft.walkers.mixin.accessor.CreeperEntityAccessor;
import tocraft.walkers.mixin.accessor.ParrotEntityAccessor;
import tocraft.walkers.mixin.accessor.ShulkerAccessor;
import tocraft.walkers.mixin.accessor.SquidEntityAccessor;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry class for {@link EntityUpdater} instances.
 *
 * <p>
 * {@link EntityUpdater}s are used to apply changes to shape entity instances on
 * the client using information from the player. As an example, an
 * {@link EntityUpdater} can be used to tell a shape bat to "stop roosting,"
 * which triggers the flight animation. {@link EntityUpdater}s are called once
 * every render tick
 * {@link net.minecraft.client.renderer.entity.EntityRenderer#render(Entity, float, float, PoseStack, MultiBufferSource, int)}.
 */
@Environment(EnvType.CLIENT)
public class EntityUpdaters {

    private static final Map<EntityType<? extends LivingEntity>, EntityUpdater<? extends LivingEntity>> map = new HashMap<>();

    /**
     * Returns a {@link EntityUpdater} if one has been registered for the given
     * {@link EntityType}, or null.
     *
     * @param entityType entity type key to retrieve a value registered in
     *                   {@link EntityUpdaters#register(EntityType, EntityUpdater)}
     * @param <T>        passed in {@link EntityType} generic
     * @return registered {@link EntityUpdater} instance for the given
     * {@link EntityType}, or null if one does not exist
     */
    @SuppressWarnings("unchecked")
    public static <T extends LivingEntity> EntityUpdater<T> getUpdater(EntityType<T> entityType) {
        return (EntityUpdater<T>) map.getOrDefault(entityType, null);
    }

    /**
     * Registers an {@link EntityUpdater} for the given {@link EntityType}.
     *
     * <p>
     * Note that a given {@link EntityType} can only have 1 {@link EntityUpdater}
     * associated with it.
     *
     * @param type          entity type key associated with the given
     *                      {@link EntityUpdater}
     * @param entityUpdater {@link EntityUpdater} associated with the given
     *                      {@link EntityType}
     * @param <T>           passed in {@link EntityType} generic
     */
    public static <T extends LivingEntity> void register(EntityType<T> type, EntityUpdater<T> entityUpdater) {
        map.put(type, entityUpdater);
    }

    public static void init() {
        // register specific entity animation handling
        EntityUpdaters.register(EntityType.BAT, (player, bat) -> bat.setResting(player.onGround()));

        EntityUpdaters.register(EntityType.PARROT, (player, parrot) -> {
            if (player.onGround() && ((NearbySongAccessor) player).shape_isNearbySongPlaying()) {
                parrot.setRecordPlayingNearby(player.blockPosition(), true);
                parrot.setOrderedToSit(true);
                parrot.setOnGround(true);
            } else if (player.onGround()) {
                parrot.setRecordPlayingNearby(player.blockPosition(), false);
                parrot.setOrderedToSit(true);
                parrot.setOnGround(true);
                parrot.oFlap = 0;
                parrot.flap = 0;
                parrot.flapSpeed = 0;
                parrot.oFlapSpeed = 0;
            } else {
                parrot.setRecordPlayingNearby(player.blockPosition(), false);
                parrot.setOrderedToSit(false);
                parrot.setOnGround(false);
                parrot.setInSittingPose(false);
                ((ParrotEntityAccessor) parrot).callCalculateFlapping();
            }
        });

        EntityUpdaters.register(EntityType.ENDER_DRAGON, (player, dragon) -> {
            dragon.flapTime += 0.01F;
            dragon.oFlapTime = dragon.flapTime;

            // setting yaw without +180 making tail faces front, for some reason
            if (dragon.posPointer < 0) {
                for (int l = 0; l < dragon.positions.length; ++l) {
                    dragon.positions[l][0] = (double) player.getYRot() + 180;
                    dragon.positions[l][1] = player.getY();
                }
            }

            if (++(dragon).posPointer == (dragon).positions.length) {
                (dragon).posPointer = 0;
            }

            dragon.positions[dragon.posPointer][0] = (double) player.getYRot() + 180;
            dragon.positions[dragon.posPointer][1] = player.getY();
        });

        EntityUpdaters.register(EntityType.ENDERMAN, (player, enderman) -> {
            ItemStack heldStack = player.getMainHandItem();

            if (heldStack.getItem() instanceof BlockItem) {
                enderman.setCarriedBlock(((BlockItem) heldStack.getItem()).getBlock().defaultBlockState());
            }
        });

        // To prevent Creeper shapes from flickering white, we reset currentFuseTime to
        // 0.
        // Creepers normally tick their fuse timer in tick(), but:
        // 1. shapes do not tick
        // 2. The Creeper ability is instant, so we do not need to re-implement ticking
        EntityUpdaters.register(EntityType.CREEPER, (player, creeper) -> ((CreeperEntityAccessor) creeper).setSwell(0));

        EntityUpdaters.register(EntityType.SQUID, new SquidEntityUpdater());

        EntityUpdaters.register(EntityType.SHULKER, new ShulkerEntityUpdater());
    }
}
