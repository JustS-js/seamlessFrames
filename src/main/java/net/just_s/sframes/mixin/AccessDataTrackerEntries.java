package net.just_s.sframes.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.data.DataTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.locks.ReadWriteLock;

@Mixin(DataTracker.class)
public interface AccessDataTrackerEntries {
    @Accessor
    public Int2ObjectMap<DataTracker.Entry<?>> getEntries();

    @Accessor
    public ReadWriteLock getLock();
}
