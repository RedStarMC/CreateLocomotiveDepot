package top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.graph.*;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import top.redstarmc.mod.createlocomotivedepot.CreateLocomotiveDepot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FourSignalBoundary extends TrackEdgePoint {

    public Couple<Map<BlockPos, Boolean>> blockEntities;
    public Couple<UUID> groups;
    public Couple<Boolean> sidesToUpdate;
    public Couple<FourAspectState> cachedStates;

    private Couple<Map<UUID, Boolean>> nextSignals;

    public FourSignalBoundary() {
        blockEntities = Couple.create(HashMap :: new);
        groups = Couple.create(null, null);
        sidesToUpdate = Couple.create(true, true);
        cachedStates = Couple.create(() -> FourAspectState.INVALID);
        nextSignals = Couple.create(null, null);
    }

    public void setGroup(boolean primary, UUID groupId) {
        UUID previous = groups.get(primary);

        groups.set(primary, groupId);

        UUID opposite = groups.get(! primary);
        Map<UUID, SignalEdgeGroup> signalEdgeGroups = Create.RAILWAYS.signalEdgeGroups;

        if ( opposite != null && signalEdgeGroups.containsKey(opposite) ) {
            SignalEdgeGroup oppositeGroup = signalEdgeGroups.get(opposite);
            if ( previous != null )
                oppositeGroup.removeAdjacent(previous);
            if ( groupId != null )
                oppositeGroup.putAdjacent(groupId);
        }

        if ( groupId != null && signalEdgeGroups.containsKey(groupId) ) {
            SignalEdgeGroup group = signalEdgeGroups.get(groupId);
            if ( opposite != null )
                group.putAdjacent(opposite);
        }
    }

    public void setGroupAndUpdate(TrackNode side, UUID groupId) {
        boolean primary = isPrimary(side);
        setGroup(primary, groupId);
        sidesToUpdate.set(primary, false);
        nextSignals.set(primary, null);
    }

    @Override
    public boolean canMerge() {
        return true;
        //
    }

    @Override
    public boolean canCoexistWith(EdgePointType<?> otherType, boolean front) {
        return otherType == getType();
        //
    }

    @Override
    public void invalidate(LevelAccessor level) {
        blockEntities.forEach(s -> s.keySet()
                .forEach(p -> invalidateAt(level, p)));
        groups.forEach(uuid -> {
            if ( Create.RAILWAYS.signalEdgeGroups.remove(uuid) != null )
                Create.RAILWAYS.sync.edgeGroupRemoved(uuid);
        });
    }

    @Override
    public void blockEntityAdded(BlockEntity blockEntity, boolean front) {
        Map<BlockPos, Boolean> sideMap = blockEntities.get(front);
        sideMap.put(
                blockEntity.getBlockPos(),
                blockEntity instanceof FourSignalBlockEntity ste && ste.getReportedPower()
        );
    }

    @Override
    public void blockEntityRemoved(BlockPos blockEntityPos, boolean front) {
        blockEntities.forEach(s -> s.remove(blockEntityPos));
        if ( blockEntities.both(Map :: isEmpty) )
            removeFromAllGraphs();
    }

    @Override
    public boolean canNavigateVia(TrackNode side) {
        return ! blockEntities.get(isPrimary(side)).isEmpty();
        //
    }

    @Override
    public void onRemoved(TrackGraph graph) {
        super.onRemoved(graph);
        FourSignalPropagator.onSignalRemoved(graph, this);
    }

    public void updateBlockEntityPower(FourSignalBlockEntity blockEntity) {
        for ( boolean front : Iterate.trueAndFalse )
            blockEntities.get(front)
                    .computeIfPresent(blockEntity.getBlockPos(), (p, c) -> blockEntity.getReportedPower());
    }

    public void queueUpdate(TrackNode side) {
        sidesToUpdate.set(isPrimary(side), true);
        //
    }

    public UUID getGroup(TrackNode side) {
        return groups.get(isPrimary(side));
        //
    }

    public FourAspectState getStateFor(BlockPos blockEntityPos) {
        for ( boolean side : new boolean[] {true, false} ) {
            if ( blockEntities.get(side).containsKey(blockEntityPos) ) {
                return cachedStates.get(side);
            }
        }
        return FourAspectState.INVALID;
    }

    public SignalBlockEntity.OverlayState getOverlayFor(BlockPos blockEntity) {
        for ( boolean first : Iterate.trueAndFalse ) {
            Map<BlockPos, Boolean> set = blockEntities.get(first);
            for ( BlockPos blockPos : set.keySet() ) {
                if ( blockPos.equals(blockEntity) )
                    return blockEntities.get(! first).isEmpty() ? SignalBlockEntity.OverlayState.RENDER : SignalBlockEntity.OverlayState.DUAL;
            }
        }
        return SignalBlockEntity.OverlayState.SKIP;
    }


    // ---------- 序列化 ----------
    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean migration, DimensionPalette dimensions) {
        super.read(nbt, registries, migration, dimensions);
        if ( migration ) return;

        blockEntities = Couple.create(HashMap :: new);
        groups = Couple.create(null, null);
        for ( int i = 1; i <= 2; i++ ) {
            if ( nbt.contains("Tiles" + i) ) {
                int finalI = i;
                NBTHelper.iterateCompoundList(nbt.getList("Tiles" + i, Tag.TAG_COMPOUND), c -> {
                    BlockPos pos = NBTHelper.readBlockPos(c, "Pos");
                    boolean powered = c.getBoolean("Power");
                    blockEntities.get(finalI == 1).put(pos, powered);
                });
            }
            if ( nbt.contains("Group" + i) )
                groups.set(i == 1, nbt.getUUID("Group" + i));
            sidesToUpdate.set(i == 1, nbt.contains("Update" + i));
            cachedStates.set(i == 1, NBTHelper.readEnum(nbt, "State" + i, FourAspectState.class));
        }
    }

    @Override
    public void read(FriendlyByteBuf buffer, DimensionPalette dimensions) {
        super.read(buffer, dimensions);
        for ( int i = 1; i <= 2; i++ ) {
            if ( buffer.readBoolean() )
                groups.set(i == 1, buffer.readUUID());
        }
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, DimensionPalette dimensions) {
        super.write(nbt, registries, dimensions);
        for ( int i = 1; i <= 2; i++ ) {
            if ( ! blockEntities.get(i == 1).isEmpty() ) {
                nbt.put("Tiles" + i, NBTHelper.writeCompoundList(blockEntities.get(i == 1).entrySet(), e -> {
                    CompoundTag c = new CompoundTag();
                    c.put("Pos", NbtUtils.writeBlockPos(e.getKey()));
                    c.putBoolean("Power", e.getValue());
                    return c;
                }));
            }
            if ( groups.get(i == 1) != null )
                nbt.putUUID("Group" + i, groups.get(i == 1));
            if ( sidesToUpdate.get(i == 1) )
                nbt.putBoolean("Update" + i, true);
            NBTHelper.writeEnum(nbt, "State" + i, cachedStates.get(i == 1));
        }
    }

    @Override
    public void write(FriendlyByteBuf buffer, DimensionPalette dimensions) {
        super.write(buffer, dimensions);
        for ( int i = 1; i <= 2; i++ ) {
            boolean hasGroup = groups.get(i == 1) != null;
            buffer.writeBoolean(hasGroup);
            if ( hasGroup )
                buffer.writeUUID(groups.get(i == 1));
        }
    }

    @Override
    public void tick(TrackGraph graph, boolean preTrains) {
        super.tick(graph, preTrains);
        if ( ! preTrains ) {
            tickState(graph);
            return;
        }
        // 预火车阶段：更新信号组传播
        for ( boolean side : new boolean[] {true, false} ) {
            if ( sidesToUpdate.get(side) ) {
                sidesToUpdate.set(side, false);
                FourSignalPropagator.propagateSignalGroup(graph, this, side);
                nextSignals.set(side, null);
            }
        }
    }

    private void tickState(TrackGraph graph) {
        for ( boolean side : new boolean[] {true, false} ) {
            if ( blockEntities.get(side).isEmpty() ) continue;

            boolean forcedRed = isForcedRed(side);
            UUID groupId = groups.get(side);
            if ( groupId == null ) {
                cachedStates.set(side, FourAspectState.INVALID);
                continue;
            }
            SignalEdgeGroup group = Create.RAILWAYS.signalEdgeGroups.get(groupId);
            if ( group == null ) {
                cachedStates.set(side, FourAspectState.INVALID);
                continue;
            }

            // 占用检查
            boolean occupied = forcedRed || isGroupOccupied(groupId, this);
            if ( occupied ) {
                cachedStates.set(side, FourAspectState.RED);
                continue;
            }

            // 四显示逻辑：根据下游信号状态决定
            cachedStates.set(side, resolveAspectFromChain(graph, side));
        }
    }

    private boolean isForcedRed(boolean side) {
        return blockEntities.get(side).values().stream().anyMatch(b -> b);
    }

    private FourAspectState resolveAspectFromChain(TrackGraph graph, boolean side) {
        if ( nextSignals.get(side) == null ) {
            nextSignals.set(side, FourSignalPropagator.collectNextSignals(graph, this, side));
        }
        Map<UUID, Boolean> downstream = nextSignals.get(side);
        if ( downstream.isEmpty() ) {
            // 没有下游信号，视为完全空闲 → 绿色
            return FourAspectState.GREEN;
        }

        // 收集所有下游信号的状态（取最严格的结果）
        FourAspectState worst = FourAspectState.GREEN;
        for ( Map.Entry<UUID, Boolean> entry : downstream.entrySet() ) {
            UUID otherId = entry.getKey();
            boolean otherSide = entry.getValue(); // true 表示对面是 primary
            FourSignalBoundary other = graph.getPoint(CreateLocomotiveDepot.FOUR_SIGNAL, otherId);
            if ( other == null ) {
                // 信号丢失，返回无效
                return FourAspectState.INVALID;
            }
            FourAspectState otherState = other.cachedStates.get(otherSide);
            // 根据四显示规则：红 > 黄 > 绿黄 > 绿
            worst = worstAspect(worst, otherState);
        }
        // 根据下游最差状态，决定本信号应该显示什么（黄 = 下游红，绿黄 = 下游黄，绿 = 下游绿黄或绿）
        return FourAspectState.fromNextState(worst);
    }

    private FourAspectState worstAspect(FourAspectState a, FourAspectState b) {
        if ( a == FourAspectState.INVALID ) return b;
        if ( b == FourAspectState.INVALID ) return a;
        // 优先级：RED > YELLOW > GREEN_YELLOW > GREEN
        if ( a == FourAspectState.RED || b == FourAspectState.RED ) return FourAspectState.RED;
        if ( a == FourAspectState.YELLOW || b == FourAspectState.YELLOW ) return FourAspectState.YELLOW;
        if ( a == FourAspectState.GREEN_YELLOW || b == FourAspectState.GREEN_YELLOW )
            return FourAspectState.GREEN_YELLOW;
        return FourAspectState.GREEN;
    }

    // 判断某个方块位置属于哪一侧（primary = true 表示 edgeLocation 的第一端）
    public boolean isPrimaryForPos(BlockPos pos) {
        return blockEntities.getFirst().containsKey(pos);
    }

    // 标记某一侧需要更新（不需要传入 TrackNode，因为边界内部可以从 edgeLocation 获取节点）
    public void queueUpdateForSide(boolean primary) {
        sidesToUpdate.set(primary, true);
        nextSignals.set(primary, null);
    }

    // 获取某一侧对应的轨道节点（需要传入当前所属的 TrackGraph）
    @Nullable
    public TrackNode getNodeForSide(boolean primary, TrackGraph graph) {
        TrackNodeLocation loc = primary ? edgeLocation.getFirst() : edgeLocation.getSecond();
        return graph.locateNode(loc);
    }

    private boolean isGroupOccupied(UUID groupId, FourSignalBoundary self) {
        SignalEdgeGroup group = Create.RAILWAYS.signalEdgeGroups.get(groupId);
        if ( group == null ) return false;
        return ! group.trains.isEmpty() || (group.reserved != null && (Object) group.reserved != (Object) self);
    }

}
