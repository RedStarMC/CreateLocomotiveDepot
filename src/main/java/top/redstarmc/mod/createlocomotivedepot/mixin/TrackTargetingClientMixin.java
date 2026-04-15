package top.redstarmc.mod.createlocomotivedepot.mixin;

import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.track.TrackTargetingClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.redstarmc.mod.createlocomotivedepot.CreateLocomotiveDepot;

@OnlyIn(Dist.CLIENT)
@Mixin(value = TrackTargetingClient.class, remap = false)
public class TrackTargetingClientMixin {

    @ModifyVariable(
            method = "clientTick",
            at = @At("STORE"),
            name = "type"
    )
    private static EdgePointType<?> modifyEdgePointType(EdgePointType<?> type) {
        return type == CreateLocomotiveDepot.FOUR_SIGNAL ? EdgePointType.SIGNAL : type;
    }

}