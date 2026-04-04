package top.redstarmc.mod.createlocomotivedepot.mixin.accessor;

import com.simibubi.create.content.trains.graph.EdgePointType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = com.simibubi.create.content.trains.track.TrackTargetingClient.class, remap = false)
public interface TrackTargetingClientAccessor {

    @Accessor("lastType")
    static EdgePointType<?> getLastType() {
        throw new AssertionError("Mixin accessor not applied");
    }

}