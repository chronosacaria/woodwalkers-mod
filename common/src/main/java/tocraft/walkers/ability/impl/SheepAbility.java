package tocraft.walkers.ability.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import tocraft.walkers.ability.WalkersAbility;

public class SheepAbility<T extends Mob> extends WalkersAbility<T> {

    @Override
    public void onUse(Player player, Mob shape, Level world) {
        BlockPos playerPos = player.blockPosition();
        BlockPos blockPos = new BlockPos(playerPos.getX(), playerPos.getY()-1, playerPos.getZ());

        if ((world.getBlockState(playerPos).getBlock() == Registry.BLOCK.get(new ResourceLocation("minecraft:grass")) || world.getBlockState(playerPos).getBlock() == Registry.BLOCK.get(new ResourceLocation("minecraft:tall_grass")))) {
            BlockState defaultAirBlockState =  Registry.BLOCK.get(new ResourceLocation("minecraft:air")).defaultBlockState();
            world.setBlockAndUpdate(playerPos, defaultAirBlockState);
            player.getFoodData().eat(2, 0.1F);
        }
        else if (world.getBlockState(blockPos).getBlock() == Registry.BLOCK.get(new ResourceLocation("minecraft:grass_block"))) {
            BlockState defaultDirtBlockState =  Registry.BLOCK.get(new ResourceLocation("minecraft:dirt")).defaultBlockState();
            world.setBlockAndUpdate(blockPos, defaultDirtBlockState);
            player.getFoodData().eat(3, 0.1F);
        }

        world.playSound(null, player, SoundEvents.SHEEP_STEP, SoundSource.PLAYERS, 1.0F, (world.random.nextFloat() - world.random.nextFloat()) * 0.2F + 1.0F);
    }

    @Override
    public Item getIcon() {
        return Items.GRASS;
    }
}
