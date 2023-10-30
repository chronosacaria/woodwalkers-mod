package tocraft.walkers.network;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import tocraft.craftedcore.client.CraftedCoreClient;
import tocraft.craftedcore.network.NetworkManager;
import tocraft.craftedcore.network.client.ClientNetworking.ApplicablePacket;
import tocraft.walkers.impl.DimensionsRefresher;
import tocraft.walkers.impl.PlayerDataProvider;
import tocraft.walkers.network.impl.UnlockPackets;

public class ClientNetworking implements NetworkHandler {

	public static void registerPacketHandlers() {
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, NetworkHandler.SHAPE_SYNC,
				ClientNetworking::handleWalkersSyncPacket);
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, NetworkHandler.ABILITY_SYNC,
				ClientNetworking::handleAbilitySyncPacket);
		NetworkManager.registerReceiver(NetworkManager.Side.S2C, NetworkHandler.UNLOCK_SYNC,
				UnlockPackets::handleUnlockSyncPacket);
	}

	public static void runOrQueue(NetworkManager.PacketContext context, ApplicablePacket packet) {
		if (context.getPlayer() == null) {
			CraftedCoreClient.getSyncPacketQueue().add(packet);
		} else {
			context.queue(() -> packet.apply(context.getPlayer()));
		}
	}

	public static void sendAbilityRequest() {
		NetworkManager.sendToServer(USE_ABILITY, new FriendlyByteBuf(Unpooled.buffer()));
	}

	public static void handleWalkersSyncPacket(FriendlyByteBuf packet, NetworkManager.PacketContext context) {
		final UUID uuid = packet.readUUID();
		final String id = packet.readUtf();
		final CompoundTag entityNbt = packet.readNbt();

		runOrQueue(context, player -> {
			@Nullable
			Player syncTarget = player.getCommandSenderWorld().getPlayerByUUID(uuid);

			if (syncTarget != null) {
				PlayerDataProvider data = (PlayerDataProvider) syncTarget;

				// set shape to null (no shape) if the entity id is "minecraft:empty"
				if (id.equals("minecraft:empty")) {
					data.setCurrentShape(null);
					((DimensionsRefresher) syncTarget).shape_refreshDimensions();
					return;
				}

				// If entity type was valid, deserialize entity data from tag/
				if (entityNbt != null) {
					entityNbt.putString("id", id);
					Optional<EntityType<?>> type = EntityType.by(entityNbt);
					if (type.isPresent()) {
						LivingEntity shape = data.getCurrentShape();

						// ensure entity data exists
						if (shape == null || !type.get().equals(shape.getType())) {
							shape = (LivingEntity) type.get().create(syncTarget.level);
							data.setCurrentShape(shape);

							// refresh player dimensions/hitbox on client
							((DimensionsRefresher) syncTarget).shape_refreshDimensions();
						}

						if (shape != null) {
							shape.load(entityNbt);
						}
					}
				}
			}
		});
	}

	public static void handleAbilitySyncPacket(FriendlyByteBuf packet, NetworkManager.PacketContext context) {
		int cooldown = packet.readInt();
		runOrQueue(context, player -> ((PlayerDataProvider) player).setAbilityCooldown(cooldown));
	}
}
