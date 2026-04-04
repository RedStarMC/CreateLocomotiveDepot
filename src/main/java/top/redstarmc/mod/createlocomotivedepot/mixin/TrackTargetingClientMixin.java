package top.redstarmc.mod.createlocomotivedepot.mixin;

import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.content.trains.track.TrackTargetingClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.redstarmc.mod.createlocomotivedepot.CreateLocomotiveDepot;
import top.redstarmc.mod.createlocomotivedepot.mixin.accessor.TrackTargetingClientAccessor;

@OnlyIn(Dist.CLIENT)
@Mixin(value = TrackTargetingClient.class, remap = false)
public class TrackTargetingClientMixin {

    /**
     * 在 TrackTargetingClient.render 方法中，局部变量 type 被赋值后，
     * 检查当前的 lastType 是否为 FOUR_SIGNAL，若是则将其改为 SIGNAL 类型。
     */
    @ModifyVariable(
            method = "render",
            at = @At(value = "STORE", ordinal = 0),
            remap = false,
            name = "type")
    private static RenderedTrackOverlayType modifyOverlayType(RenderedTrackOverlayType type) {
        EdgePointType<?> lastType = TrackTargetingClientAccessor.getLastType();
        if ( lastType == CreateLocomotiveDepot.FOUR_SIGNAL ) {
            return RenderedTrackOverlayType.SIGNAL;
        }
        return type;
    }

}