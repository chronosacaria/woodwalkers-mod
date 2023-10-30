package tocraft.walkers.ability.impl;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tocraft.walkers.ability.WalkersAbility;

public class WitherEntityAbility extends WalkersAbility<WitherBoss> {

    @Override
    public void onUse(Player player, WitherBoss shape, Level world) {
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.NEUTRAL, 0.5F, 0.4F / (world.random.nextFloat() * 0.4F + 0.8F));

        if (!world.isClientSide) {
            Vec3 lookDirection = player.getLookAngle();
            WitherSkull skull = new WitherSkull(world, player, lookDirection.x, lookDirection.y, lookDirection.z);
            skull.setPosRaw(player.getX(), player.getY() + 2, player.getZ());
            skull.shootFromRotation(player, player.xRotO, player.yRotO, 0.0F, 1.5F, 1.0F);
            world.addFreshEntity(skull);
        }
    }

    @Override
    public Item getIcon() {
        return Items.WITHER_SKELETON_SKULL;
    }
}
