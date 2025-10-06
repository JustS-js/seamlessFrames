package net.just_s.sframes;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.just_s.sframes.mixin.AccessDataTrackerEntries;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SFramesMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("sframes");
	public static final Config CONFIG = new Config();
	protected static final String TEAM_NAME = "SeamlessFramesInternalScoreboardTeam";
	protected static Team TEAM;

	public static final AttachmentType<Formatting> COLOR_ATTACHMENT;
	public static final AttachmentType<Boolean> HAS_COLOR_ATTACHMENT;

	static {
		AttachmentRegistry.Builder<Formatting> colorBuilder = AttachmentRegistry.builder();
		COLOR_ATTACHMENT = colorBuilder
				.copyOnDeath()
				.initializer(() -> Formatting.WHITE)
				.persistent(Formatting.CODEC)
				.buildAndRegister(Identifier.of("sframes", "color"));
		AttachmentRegistry.Builder<Boolean> hasColorBuilder = AttachmentRegistry.builder();
		HAS_COLOR_ATTACHMENT = hasColorBuilder
				.copyOnDeath()
				.initializer(() -> false)
				.persistent(Codec.BOOL)
				.buildAndRegister(Identifier.of("sframes", "has_color"));
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Seamless Frames loaded successfully!");
		CONFIG.load();
		CommandRegistrationCallback.EVENT.register(FrameCommand::register);
		ServerLifecycleEvents.SERVER_STARTED.register(
				server -> {
					Team team = server.getScoreboard().getTeam(TEAM_NAME);
					if (team == null) {
						team = server.getScoreboard().addTeam(TEAM_NAME);
					}
                    team.setColor(SFramesMod.CONFIG.getData().baseColor());
					TEAM = team;
				}
		);
	}

	public static Team getTeam() {
		if (TEAM == null) {
			LOGGER.error("Could not obtain SFrames Team instance. Something is wrong, report this.");
			throw new IllegalStateException("Could not obtain SFrames Team instance");
		}
		return TEAM;
	}

	public static void addFrameToTeam(ItemFrameEntity itemFrame) {
		MinecraftServer server = itemFrame.getEntityWorld().getServer();
		if (server == null) {
			return;
		}
		server.getScoreboard().addScoreHolderToTeam(itemFrame.getNameForScoreboard(), getTeam());
		itemFrame.addCommandTag(TEAM_NAME);
	}

	public static void removeFrameFromTeam(ItemFrameEntity itemFrame) {
		MinecraftServer server = itemFrame.getEntityWorld().getServer();
		if (server == null) {
			return;
		}
		server.getScoreboard().removeScoreHolderFromTeam(itemFrame.getNameForScoreboard(), getTeam());
		itemFrame.removeCommandTag(TEAM_NAME);
	}

	public static boolean isFrameInTeam(ItemFrameEntity itemFrame) {
		return getTeam().equals(itemFrame.getScoreboardTeam()) || itemFrame.getCommandTags().contains(TEAM_NAME);
	}

	public static void sendPacket(ServerPlayerEntity player, Packet<?> packet) {
		player.networkHandler.sendPacket(packet);
	}

	public static void sendPackets(List<ServerPlayerEntity> players, Packet<?> packet) {
		for (ServerPlayerEntity player : players) {
			player.networkHandler.sendPacket(packet);
		}
	}

	public static boolean shouldGlow(ItemFrameEntity frame) {
		return isFrameInTeam(frame) && frame.getHeldItemStack().isEmpty() && CONFIG.getData().radiusOfGlowing() > -1;
	}

	public static List<ServerPlayerEntity> getPlayersNearby(ItemFrameEntity itemFrame) {
		return (List<ServerPlayerEntity>)(Object)itemFrame.getEntityWorld().getOtherEntities(
				null,
				new Box(
						itemFrame.getEntityPos().add(
								SFramesMod.CONFIG.getData().radiusOfGlowing() + 1,
								SFramesMod.CONFIG.getData().radiusOfGlowing() + 1,
								SFramesMod.CONFIG.getData().radiusOfGlowing() + 1
						),
						itemFrame.getEntityPos().add(
								-1 * SFramesMod.CONFIG.getData().radiusOfGlowing(),
								-1 * SFramesMod.CONFIG.getData().radiusOfGlowing(),
								-1 * SFramesMod.CONFIG.getData().radiusOfGlowing()
						)
				),
				entity -> entity instanceof ServerPlayerEntity
		);
	}

	public static TeamS2CPacket generateColorPacket(ServerPlayerEntity player) {
		Team team = getTeam();
		Formatting baseColor = team.getColor();
		team.setColor(player.getAttachedOrElse(COLOR_ATTACHMENT, baseColor));
		TeamS2CPacket packet = TeamS2CPacket.updateTeam(team, false);
		team.setColor(baseColor);
		return packet;
	}

	public static EntityTrackerUpdateS2CPacket generateGlowPacket(ItemFrameEntity frame, boolean shouldGlow) {
		DataTracker tracker = frame.getDataTracker();
		List<DataTracker.Entry<?>> trackedValues = getAllEntries(tracker);
		List<DataTracker.SerializedEntry<?>> serializedEntryList = new ArrayList<>();
		boolean wasModified = false;
		for (DataTracker.Entry<?> entry : trackedValues) {
			if (entry.get().getClass() == Byte.class && !wasModified) {
				DataTracker.Entry<Byte> byteEntry = (DataTracker.Entry<Byte>) entry;
				if (shouldGlow)
					byteEntry.set((byte) ((byte)entry.get() | 1 << 6));
				else
					byteEntry.set((byte) ((byte)entry.get() & ~(1 << 6)));
				wasModified = true;
			}
			serializedEntryList.add(entry.toSerialized());
		}

		return new EntityTrackerUpdateS2CPacket(frame.getId(), serializedEntryList);
	}

	@Nullable
	private static List<DataTracker.Entry<?>> getAllEntries(DataTracker tracker) {
		List<DataTracker.Entry<?>> list = Lists.newArrayList();

		for (DataTracker.Entry entry : ((AccessDataTrackerEntries)tracker).getEntries()) {
			list.add(new DataTracker.Entry(entry.getData(), entry.getData().dataType().copy(entry.get())));
		}

		return list.isEmpty() ? null : list;
	}
}
