package top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four;

import com.simibubi.create.content.trains.signal.SignalBlockEntity;

/**
 * 四显示档位。
 *
 * <p>显示语义（中国铁路四显示自动闭塞）：</p>
 * <ul>
 *   <li>{@link #RED} 前方闭塞分区被占用 —— 停车</li>
 *   <li>{@link #YELLOW} 前方 1 个闭塞分区空闲，第一架信号机显示红灯</li>
 *   <li>{@link #GREEN_YELLOW} 前方 2 个闭塞分区空闲，第一架信号机显示黄灯</li>
 *   <li>{@link #GREEN} 前方 ≥3 个闭塞分区空闲，或前方已无信号机</li>
 * </ul>
 *
 * <p>{@link #INVALID} 表示信号机尚未绑定轨道 / 分组缺失，显示为闪烁红灯。</p>
 *
 * <p>注意：本枚举只驱动<b>显示</b>。列车的通行判定完全由 Create 的
 * {@code SignalEdgeGroup.isOccupiedUnless(...)} 决定，与本枚举无关。</p>
 */
public enum FourAspectState {

    INVALID,
    RED,
    YELLOW,
    GREEN_YELLOW,
    GREEN;

    /**
     * 取两个档位中更严格的一个（用于分岔处汇总下游分支）。
     */
    public static FourAspectState mostRestrictive(FourAspectState a, FourAspectState b) {
        if ( a == null ) return b == null ? GREEN : b;
        if ( b == null ) return a;
        if ( a == INVALID ) return b;
        if ( b == INVALID ) return a;
        return a.severity() >= b.severity() ? a : b;
    }

    /**
     * 级联兜底：本机档位 = 下游最严格档位降一级。
     *
     * <p>当闭塞分区计数无法完成时（链断裂、分岔、信号丢失）使用此规则。</p>
     */
    public static FourAspectState fromNextState(FourAspectState state) {
        if ( state == null || state == INVALID ) return GREEN;

        return switch ( state ) {
            case RED -> YELLOW;
            case YELLOW -> GREEN_YELLOW;
            case GREEN_YELLOW -> GREEN;
            default -> GREEN;
        };
    }

    /**
     * 按"前方空闲闭塞分区数量"换算档位（主算法）。
     *
     * @param freeBlocks 前方连续空闲闭塞分区数，已封顶到 3
     */
    public static FourAspectState fromFreeBlocks(int freeBlocks) {
        return switch ( Math.min(freeBlocks, 3) ) {
            case 0 -> RED;
            case 1 -> YELLOW;
            case 2 -> GREEN_YELLOW;
            default -> GREEN;
        };
    }

    /**
     * 把 Create 原版的三态信号状态折算为本模组的四显示档位。
     *
     * <p>用于下游是原版信号机的场合：原版只有红/黄/绿，没有绿黄。</p>
     */
    public static FourAspectState fromVanilla(SignalBlockEntity.SignalState state) {
        if ( state == null ) return GREEN;

        return switch ( state ) {
            case RED -> RED;
            case YELLOW -> YELLOW;
            case GREEN -> GREEN;
            case INVALID -> INVALID;
        };
    }

    /** 用于阻塞判断的严格程度排序，数值越大越严格。 */
    public int severity() {
        return switch ( this ) {
            case RED -> 4;
            case YELLOW -> 3;
            case GREEN_YELLOW -> 2;
            case GREEN -> 1;
            case INVALID -> 0;
        };
    }

    /** 红灯与无效态都要求停车。 */
    public boolean isStop() {
        return this == RED;
    }

    public boolean isRedLight(float renderTime) {
        return this == RED || this == INVALID && renderTime % 40 < 3;
    }

    public boolean isYellowLight() {
        return this == YELLOW;
    }

    /** 绿黄档位的第二盏灯（黄绿灯位）。 */
    public boolean isGreenYellowLight() {
        return this == GREEN_YELLOW;
    }

    public boolean isGreenLight() {
        return this == GREEN;
    }

}
