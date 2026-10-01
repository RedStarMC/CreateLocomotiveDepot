package top.redstarmc.mod.createlocomotivedepot.mixin;

import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.track.CurvedTrackSelectionPacket;
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.redstarmc.mod.createlocomotivedepot.CreateLocomotiveDepot;
import top.redstarmc.mod.createlocomotivedepot.registry.CLDBlocks;

/**
 * 让本模组的四显示信号机能够在<b>贝塞尔弯道轨道</b>上放置。
 *
 * <p>Create 的 {@code CurvedTrackSelectionPacket.applySettings} 里是这样判定目标点类型的：</p>
 * <pre>
 * EdgePointType&lt;?&gt; type = AllBlocks.TRACK_SIGNAL.isIn(stack)
 *         ? EdgePointType.SIGNAL
 *         : EdgePointType.STATION;
 * </pre>
 * <p>手持本模组的信号机时会落到 {@code STATION} 分支，于是弯道放置会被当成车站处理
 * （提示"位置已被占用"或直接失败）。这里把局部变量 {@code type} 改写为
 * {@code FOUR_SIGNAL}，使弯道放置走正确的类型。</p>
 *
 * <p>注入的局部变量索引与原版 {@code TrackTargetingClient.clientTick} 的做法一致，
 * 依赖编译期保留的局部变量名 {@code type}。</p>
 */
@Mixin(value = CurvedTrackSelectionPacket.class, remap = false)
public class CurvedTrackSelectionPacketMixin {

    /** 让 IDE 与编译器知道 CLDBlocks 被引用（供后续扩展/避免未使用导入）。 */
    @SuppressWarnings("unused")
    private static final Class<?> cld$marker = CLDBlocks.class;

    @ModifyVariable(
            method = "applySettings",
            at = @At("STORE"),
            name = "type"
    )
    private EdgePointType<?> cld$useFourSignalType(EdgePointType<?> type, ServerPlayer player) {
        if ( type != EdgePointType.SIGNAL )
            return type;

        ItemStack stack = player.getMainHandItem();
        if ( stack.getItem() instanceof TrackTargetingBlockItem targeting
                && targeting.getType(stack) == CreateLocomotiveDepot.FOUR_SIGNAL )
            return CreateLocomotiveDepot.FOUR_SIGNAL;

        // 副手也查一次，覆盖副手放置的情况
        ItemStack offhand = player.getOffhandItem();
        if ( offhand.getItem() instanceof TrackTargetingBlockItem targeting
                && targeting.getType(offhand) == CreateLocomotiveDepot.FOUR_SIGNAL )
            return CreateLocomotiveDepot.FOUR_SIGNAL;

        return type;
    }

}
