package top.redstarmc.mod.createlocomotivedepot.content.trains.signal.four;

import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class FourSignalVisual extends AbstractBlockEntityVisual<FourSignalBlockEntity> implements SimpleTickableVisual {

    private final TransformedInstance signalOverlay;

    public FourSignalVisual(VisualizationContext ctx, FourSignalBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);
        signalOverlay = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.TRACK_SIGNAL_OVERLAY))
                .createInstance();
        setupVisual();
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {

    }

    @Override
    public void updateLight(float partialTick) {

    }

    @Override
    protected void _delete() {
        signalOverlay.delete();
    }

    @Override
    public void tick(Context context) {
        setupVisual();
        //
    }

    private void setupVisual() {

    }

}
