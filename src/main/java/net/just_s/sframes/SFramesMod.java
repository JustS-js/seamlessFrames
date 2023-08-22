package net.just_s.sframes;

import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.just_s.sframes.mixin.AccessDataTrackerEntries;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
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
		return frame.getCommandTags().contains("invisibleframe") && frame.getHeldItemStack().isEmpty() && CONFIG.radiusOfGlowing > -1;
	}

	public static EntityTrackerUpdateS2CPacket generateGlowPacket(ItemFrameEntity frame, boolean shouldGlow) {
		DataTracker tracker = frame.getDataTracker();
		List<DataTracker.Entry<?>> trackedValues = getAllEntries(tracker);

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
		for (DataTracker.Entry<?> entry : trackedValues) {
			writeEntryToPacket(buf, entry);
		}
		buf.writeByte(255);

		return new EntityTrackerUpdateS2CPacket(buf);
	}

	@Nullable
	private static List<DataTracker.Entry<?>> getAllEntries(DataTracker tracker) {
		List<DataTracker.Entry<?>> list = null;
		((AccessDataTrackerEntries)tracker).getLock().readLock().lock();

		DataTracker.Entry entry;
		for(ObjectIterator var2 = ((AccessDataTrackerEntries)tracker).getEntries().values().iterator(); var2.hasNext(); list.add(new DataTracker.Entry(entry.getData(), entry.getData().getType().copy(entry.get())))) {
			entry = (DataTracker.Entry)var2.next();
			if (list == null) {
				list = Lists.newArrayList();
			}
		}

		((AccessDataTrackerEntries)tracker).getLock().readLock().unlock();
		return list;
	}

	private static <T> void writeEntryToPacket(PacketByteBuf buf, DataTracker.Entry<T> entry) {
		TrackedData<T> trackedData = entry.getData();
		int i = TrackedDataHandlerRegistry.getId(trackedData.getType());
		if (i < 0) {
			LOGGER.error("Unknown serializer type " + trackedData.getType());
		} else {
			buf.writeByte(trackedData.getId());
			buf.writeVarInt(i);
			trackedData.getType().write(buf, entry.get());
		}
	}
}
