package top.redstarmc.mod.createlocomotivedepot.mixin;

import com.simibubi.create.content.trains.graph.EdgePointStorage;
import com.simibubi.create.content.trains.graph.TrackGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露 {@code TrackGraph.edgePoints}（包级私有）供 {@link TrackGraphMixin} 直接读取内部存储。
 *
 * <p>必须读原始存储而不是调用 {@code getPoint}/{@code getPoints}，否则会与
 * {@link TrackGraphMixin} 的注入互相递归。</p>
 */
@Mixin(value = TrackGraph.class, remap = false)
public interface TrackGraphAccessor {

    @Accessor("edgePoints")
    EdgePointStorage cld$getEdgePointStorage();

}
