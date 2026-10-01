package top.redstarmc.mod.createlocomotivedepot.registry;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import top.redstarmc.mod.createlocomotivedepot.CreateLocomotiveDepot;
import top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four.FourAspectState;

import java.util.EnumMap;
import java.util.Map;

/**
 * 四显示信号机的灯位 {@link PartialModel}。
 *
 * <p>灯位自上而下为：绿 / 绿黄 / 黄 / 红；每个灯位都有"点亮"与"熄灭"两套模型，
 * 渲染时按当前档位选择。红、黄两档复用 Create 自带纹理
 * （{@code create:block/signal_glow} 与 {@code signal_glow_2}），
 * 绿与绿黄使用本模组自绘的同规格纹理。</p>
 */
public final class CLDPartialModels {

    /** 灯位自下而上的 Y 区间，与 lamp_*.json 中的模型坐标一一对应。 */
    public static final PartialModel LAMP_GREEN_LIT = block("four_signal/lamp_green");
    public static final PartialModel LAMP_GREEN_OFF = block("four_signal/lamp_green_off");
    public static final PartialModel LAMP_GREEN_YELLOW_LIT = block("four_signal/lamp_green_yellow");
    public static final PartialModel LAMP_GREEN_YELLOW_OFF = block("four_signal/lamp_green_yellow_off");
    public static final PartialModel LAMP_YELLOW_LIT = block("four_signal/lamp_yellow");
    public static final PartialModel LAMP_YELLOW_OFF = block("four_signal/lamp_yellow_off");
    public static final PartialModel LAMP_RED_LIT = block("four_signal/lamp_red");
    public static final PartialModel LAMP_RED_OFF = block("four_signal/lamp_red_off");
    private static final Map<FourAspectState, LampModels> BY_ASPECT = new EnumMap<>(FourAspectState.class);

    static {
        // INVALID 与 RED 都亮红灯（INVALID 在渲染层做闪烁）
        BY_ASPECT.put(FourAspectState.INVALID,
                new LampModels(LAMP_GREEN_OFF, LAMP_GREEN_YELLOW_OFF, LAMP_YELLOW_OFF, LAMP_RED_LIT));
        BY_ASPECT.put(FourAspectState.RED,
                new LampModels(LAMP_GREEN_OFF, LAMP_GREEN_YELLOW_OFF, LAMP_YELLOW_OFF, LAMP_RED_LIT));
        BY_ASPECT.put(FourAspectState.YELLOW,
                new LampModels(LAMP_GREEN_OFF, LAMP_GREEN_YELLOW_OFF, LAMP_YELLOW_LIT, LAMP_RED_OFF));
        BY_ASPECT.put(FourAspectState.GREEN_YELLOW,
                new LampModels(LAMP_GREEN_OFF, LAMP_GREEN_YELLOW_LIT, LAMP_YELLOW_OFF, LAMP_RED_OFF));
        BY_ASPECT.put(FourAspectState.GREEN,
                new LampModels(LAMP_GREEN_LIT, LAMP_GREEN_YELLOW_OFF, LAMP_YELLOW_OFF, LAMP_RED_OFF));
    }

    private CLDPartialModels() {
    }

    private static PartialModel block(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(
                CreateLocomotiveDepot.MOD_ID, "block/" + path));
    }

    /** 取某档位对应的四灯位模型组合。 */
    public static LampModels forAspect(FourAspectState aspect) {
        LampModels models = BY_ASPECT.get(aspect);
        return models != null ? models : BY_ASPECT.get(FourAspectState.RED);
    }

    /** 该档位是否有灯处于点亮态（决定是否使用全亮光照）。 */
    public static boolean isLit(FourAspectState aspect) {
        return aspect == FourAspectState.RED || aspect == FourAspectState.YELLOW
                || aspect == FourAspectState.GREEN_YELLOW || aspect == FourAspectState.GREEN;
    }

    public static void init() {
        // 触发静态初始化
    }

    /** 一个档位下四个灯位的点亮/熄灭组合。 */
    public record LampModels(PartialModel green, PartialModel greenYellow, PartialModel yellow, PartialModel red) {

        /** 全部灯位换成熄灭模型（用于 INVALID 的闪烁半周期）。 */
        public LampModels toDimmed() {
            return new LampModels(LAMP_GREEN_OFF, LAMP_GREEN_YELLOW_OFF, LAMP_YELLOW_OFF, LAMP_RED_OFF);
        }

    }

}
