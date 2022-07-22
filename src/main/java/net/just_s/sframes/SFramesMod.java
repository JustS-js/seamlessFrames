package net.just_s.sframes;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SFramesMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("sframes");
	public static final Config CONFIG = new Config();

	@Override
	public void onInitialize() {
		LOGGER.info("Seamless Frames loaded successfully!");
		CONFIG.load();
		CommandRegistrationCallback.EVENT.register(FrameCommand::register);
	}

	public static void sendPackets(List<PlayerEntity> players, Packet<?> packet) {
		for (PlayerEntity player : players) {
			sendPackets((ServerPlayerEntity) player, packet);
		}
	}

	public static void sendPackets(ServerPlayerEntity player, Packet<?> packet) {
		player.networkHandler.sendPacket(packet);
	}

	public static boolean shouldGlow(ItemFrameEntity frame) {
		return frame.getScoreboardTags().contains("invisibleframe") && frame.getHeldItemStack().isEmpty() && CONFIG.radiusOfGlowing > -1;
	}
}
