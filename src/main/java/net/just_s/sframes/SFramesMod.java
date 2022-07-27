package net.just_s.sframes;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
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

	public static void sendPackets(List<ServerPlayerEntity> players, Packet<?> packet) {
		for (ServerPlayerEntity player : players) {
			sendPackets(player, packet);
		}
	}

	public static void sendPackets(ServerPlayerEntity player, Packet<?> packet) {
		player.networkHandler.sendPacket(packet);
	}

	public static boolean shouldGlow(ItemFrameEntity frame) {
		return frame.getScoreboardTags().contains("invisibleframe") && frame.getHeldItemStack().isEmpty() && CONFIG.radiusOfGlowing > -1;
	}

	public static EntityTrackerUpdateS2CPacket generateGlowPacket(ItemFrameEntity frame, boolean shouldGlow) {
		DataTracker tracker = frame.getDataTracker();
		List<DataTracker.Entry<?>> trackedValues = tracker.getAllEntries();

		for (DataTracker.Entry<?> entry : trackedValues) {
			if (entry.get().getClass() == Byte.class) {
				DataTracker.Entry<Byte> byteEntry = (DataTracker.Entry<Byte>) entry;
				if (shouldGlow)
					byteEntry.set((byte) ((byte)entry.get() | 1 << 6));
				else
					byteEntry.set((byte) ((byte)entry.get() & ~(1 << 6)));
				break;
			}
		}

		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		buf.writeVarInt(frame.getId());
		DataTracker.entriesToPacket(trackedValues, buf);

		return new EntityTrackerUpdateS2CPacket(buf);
	}
}
