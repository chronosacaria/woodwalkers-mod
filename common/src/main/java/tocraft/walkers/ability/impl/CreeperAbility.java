package tocraft.walkers.ability.impl;

import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import tocraft.walkers.ability.WalkersAbility;

public class CreeperAbility extends WalkersAbility<Creeper> {

    @Override
    public void onUse(Player player, Creeper shape, Level world) {
        world.explode(player, player.getX(), player.getY(), player.getZ(), 3.0f, Explosion.BlockInteraction.NONE);
    }

    @Override
    public Item getIcon() {
        return Items.TNT;
    }
}
