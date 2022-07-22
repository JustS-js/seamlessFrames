package net.just_s.sframes.mixin;

import net.just_s.sframes.SFramesMod;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;


@Mixin(MinecraftDedicatedServer.class)
public class ServerStartMixin {
    @Inject(at = @At("TAIL"), method = "setupServer")
    private void inject(CallbackInfoReturnable<Boolean> cir) {
        MinecraftDedicatedServer server = ((MinecraftDedicatedServer)(Object)this);
        World world = server.getWorlds().iterator().next();

        try {
            teamSettings(Objects.requireNonNull(world.getScoreboard().getTeam("SeamlessFrames")));
        } catch (NullPointerException e) {
            Team team = world.getScoreboard().addTeam("SeamlessFrames");
            teamSettings(team);
        }
    }

    private void teamSettings(@NotNull Team team) {
        team.setColor(Formatting.byName(SFramesMod.CONFIG.outlineColor));
    }
}
