package net.just_s.sframes;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SerializableTeam {
    private final Text displayName;
    private final Text prefix;
    private final Text suffix;
    private final String nameTagVisibilityRule;
    private final String collisionRule;
    private final Formatting color;
    private final int friendlyFlags;

    public SerializableTeam(Team team, Formatting color) {
        this.displayName = team.getDisplayName();
        this.friendlyFlags = team.getFriendlyFlagsBitwise();
        this.nameTagVisibilityRule = team.getNameTagVisibilityRule().name;
        this.collisionRule = team.getCollisionRule().name;
        this.color = color;
        this.prefix = team.getPrefix();
        this.suffix = team.getSuffix();
    }

    public SerializableTeam(PacketByteBuf buf) {
        this.displayName = buf.readText();
        this.friendlyFlags = buf.readByte();
        this.nameTagVisibilityRule = buf.readString(40);
        this.collisionRule = buf.readString(40);
        this.color = (Formatting)buf.readEnumConstant(Formatting.class);
        this.prefix = buf.readText();
        this.suffix = buf.readText();
    }

    public Text getDisplayName() {
        return this.displayName;
    }

    public int getFriendlyFlagsBitwise() {
        return this.friendlyFlags;
    }

    public Formatting getColor() {
        return this.color;
    }

    public String getNameTagVisibilityRule() {
        return this.nameTagVisibilityRule;
    }

    public String getCollisionRule() {
        return this.collisionRule;
    }

    public Text getPrefix() {
        return this.prefix;
    }

    public Text getSuffix() {
        return this.suffix;
    }

    public void write(PacketByteBuf buf) {
        buf.writeText(this.displayName);
        buf.writeByte(this.friendlyFlags);
        buf.writeString(this.nameTagVisibilityRule);
        buf.writeString(this.collisionRule);
        buf.writeEnumConstant(this.color);
        buf.writeText(this.prefix);
        buf.writeText(this.suffix);
    }
}
