package tocraft.walkers.ability.impl;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;

public class SheepAbility extends GrassEaterAbility<Sheep> {
    public int eatTick = 0;

    @Override
    public void onUse(Player player, Sheep shape, Level world) {
        if (!shape.isSheared() && player.getMainHandItem().getItem() instanceof ShearsItem) {
            shape.shear(player.getSoundSource());
        } else {
            eatTick = Mth.positiveCeilDiv(40, 2);
        }
    }

    @Override
    public Item getIcon() {
        return Items.WHITE_WOOL;
    }
}
