package top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.*;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Pair;

import java.util.*;

public class FourSignalPropagator {

    private static void walkSignals(TrackGraph graph, FourSignalBoundary signal, boolean front,
                                    java.util.function.Predicate<Pair<TrackNode, FourSignalBoundary>> boundaryCallback,
                                    java.util.function.Predicate<EdgeData> nonBoundaryCallback,
                                    boolean forCollection) {
        Couple<TrackNodeLocation> edgeLocation = signal.edgeLocation;
        Couple<TrackNode> startNodes = edgeLocation.map(graph :: locateNode);
        Couple<TrackEdge> startEdges = startNodes.mapWithParams((l1, l2) -> graph.getConnectionsFrom(l1).get(l2), startNodes.swap());

        TrackNode node1 = startNodes.get(front);
        TrackNode node2 = startNodes.get(! front);
        TrackEdge startEdge = startEdges.get(front);
        TrackEdge oppositeEdge = startEdges.get(! front);

        if ( startEdge == null ) return;

        if ( ! forCollection ) {
            notifyTrains(graph, startEdge, oppositeEdge);
            startEdge.getEdgeData().refreshIntersectingSignalGroups(graph);
            Create.RAILWAYS.sync.edgeDataChanged(graph, node1, node2, startEdge, oppositeEdge);
        }

        // 检查同一条边上是否有其他信号（紧邻）
        FourSignalBoundary immediate = getImmediateSignalOnEdge(startEdge, signal.getLocationOn(startEdge));
        if ( immediate != null ) {
            if ( boundaryCallback.test(Pair.of(node1, immediate)) )
                startEdge.getEdgeData().refreshIntersectingSignalGroups(graph);
            return;
        }

        List<Couple<TrackNode>> frontier = new ArrayList<>();
        frontier.add(Couple.create(node2, node1));
        walkSignalsRecursive(graph, frontier, boundaryCallback, nonBoundaryCallback, forCollection);
    }

    private static FourSignalBoundary getImmediateSignalOnEdge(TrackEdge edge, double position) {
        EdgeData data = edge.getEdgeData();
        TrackEdgePoint point = data.next(position + 1e-5);
        if ( point instanceof FourSignalBoundary fsb ) return fsb;
        return null;
    }

    private static void walkSignalsRecursive(TrackGraph graph, List<Couple<TrackNode>> frontier,
                                             java.util.function.Predicate<Pair<TrackNode, FourSignalBoundary>> boundaryCallback,
                                             java.util.function.Predicate<EdgeData> nonBoundaryCallback,
                                             boolean forCollection) {
        Set<TrackEdge> visited = new HashSet<>();
        while ( ! frontier.isEmpty() ) {
            Couple<TrackNode> couple = frontier.remove(0);
            TrackNode currentNode = couple.getFirst();
            TrackNode prevNode = couple.getSecond();

            for ( Map.Entry<TrackNode, TrackEdge> entry : graph.getConnectionsFrom(currentNode).entrySet() ) {
                TrackNode nextNode = entry.getKey();
                TrackEdge edge = entry.getValue();
                if ( nextNode == prevNode ) continue;
                if ( ! visited.add(edge) ) continue;

                if ( forCollection && ! graph.getConnectionsFrom(prevNode).get(currentNode).canTravelTo(edge) )
                    continue;

                TrackEdge oppositeEdge = graph.getConnectionsFrom(nextNode).get(currentNode);
                visited.add(oppositeEdge);

                for ( boolean flip : new boolean[] {false, true} ) {
                    TrackEdge currentEdge = flip ? oppositeEdge : edge;
                    EdgeData signalData = currentEdge.getEdgeData();

                    if ( ! signalData.hasSignalBoundaries() ) {
                        if ( nonBoundaryCallback.test(signalData) ) {
                            notifyTrains(graph, currentEdge);
                            Create.RAILWAYS.sync.edgeDataChanged(graph, currentNode, nextNode, edge, oppositeEdge);
                        }
                        continue;
                    }

                    // 找到第一个信号（可能是原版或自定义，但我们要找 FourSignalBoundary）
                    FourSignalBoundary nextSignal = findFirstFourSignalOnEdge(currentEdge);
                    if ( nextSignal == null ) continue;
                    if ( boundaryCallback.test(Pair.of(currentNode, nextSignal)) ) {
                        notifyTrains(graph, edge, oppositeEdge);
                        currentEdge.getEdgeData().refreshIntersectingSignalGroups(graph);
                        Create.RAILWAYS.sync.edgeDataChanged(graph, currentNode, nextNode, edge, oppositeEdge);
                    }
                    // 找到第一个信号后就停止这条路径的继续探索（与信号链逻辑一致）
                    break;
                }
                // 只有当前边没有信号时才继续沿着边走到下一个节点
                if ( getImmediateSignalOnEdge(edge, 0) == null && getImmediateSignalOnEdge(oppositeEdge, 0) == null ) {
                    frontier.add(Couple.create(nextNode, currentNode));
                }
            }
        }
    }

    private static FourSignalBoundary findFirstFourSignalOnEdge(TrackEdge edge) {
        EdgeData data = edge.getEdgeData();
        for ( TrackEdgePoint point : data.getPoints() ) {
            if ( point instanceof FourSignalBoundary fsb ) return fsb;
        }
        return null;
    }

    public static void onSignalRemoved(TrackGraph graph, FourSignalBoundary signal) {
        signal.sidesToUpdate.map($ -> false);
        for ( boolean front : new boolean[] {true, false} ) {
            if ( signal.sidesToUpdate.get(front) )
                continue;
            UUID id = signal.groups.get(front);
            if ( Create.RAILWAYS.signalEdgeGroups.remove(id) != null )
                Create.RAILWAYS.sync.edgeGroupRemoved(id);

            walkSignals(graph, signal, front, pair -> {
                TrackNode node = pair.getFirst();
                FourSignalBoundary boundary = pair.getSecond();
                boundary.queueUpdate(node);
                return false;
            }, signalData -> {
                if ( ! signalData.hasSignalBoundaries() ) {
                    signalData.setSingleSignalGroup(graph, EdgeData.passiveGroup);
                    return true;
                }
                return false;
            }, false);
        }
    }

    public static void propagateSignalGroup(TrackGraph graph, FourSignalBoundary signal, boolean front) {
        Map<UUID, SignalEdgeGroup> globalGroups = Create.RAILWAYS.signalEdgeGroups;
        TrackGraphSync sync = Create.RAILWAYS.sync;

        SignalEdgeGroup group = new SignalEdgeGroup(UUID.randomUUID());
        UUID groupId = group.id;
        globalGroups.put(groupId, group);
        signal.setGroup(front, groupId);
        sync.pointAdded(graph, signal);

        walkSignals(graph, signal, front, pair -> {
            TrackNode node = pair.getFirst();
            FourSignalBoundary boundary = pair.getSecond();
            UUID currentGroup = boundary.getGroup(node);
            if ( currentGroup != null )
                if ( globalGroups.remove(currentGroup) != null )
                    sync.edgeGroupRemoved(currentGroup);
            boundary.setGroupAndUpdate(node, groupId);
            sync.pointAdded(graph, boundary);
            return true;
        }, signalData -> {
            UUID singleGroup = signalData.getSingleSignalGroup();
            if ( singleGroup != null )
                if ( globalGroups.remove(singleGroup) != null )
                    sync.edgeGroupRemoved(singleGroup);
            signalData.setSingleSignalGroup(graph, groupId);
            return true;
        }, false);

        group.resolveColor();
        sync.edgeGroupCreated(groupId, group.color);
    }

    public static Map<UUID, Boolean> collectNextSignals(TrackGraph graph, FourSignalBoundary signal, boolean front) {
        HashMap<UUID, Boolean> map = new HashMap<>();
        walkSignals(graph, signal, front, pair -> {
            FourSignalBoundary boundary = pair.getSecond();
            map.put(boundary.id, ! boundary.isPrimary(pair.getFirst()));
            return false;
        }, __ -> false, true);
        return map;
    }

    public static void notifyTrains(TrackGraph graph, TrackEdge... edges) {
        for ( TrackEdge edge : edges ) {
            for ( Train train : Create.RAILWAYS.trains.values() ) {
                if ( train.graph != graph ) continue;
                if ( train.updateSignalBlocks ) continue;
                train.forEachTravellingPoint(tp -> {
                    if ( tp.edge == edge ) train.updateSignalBlocks = true;
                });
            }
        }
    }

}