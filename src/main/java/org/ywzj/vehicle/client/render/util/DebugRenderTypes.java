package org.ywzj.vehicle.client.render.util;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.OptionalDouble;

/**
 * Render types for debug overlays.
 * Subclasses RenderType only to access protected shader and state constants.
 */
@OnlyIn(Dist.CLIENT)
public final class DebugRenderTypes extends RenderType {

    /**
     * Lines that ignore the depth buffer so boxes buried under terrain stay visible.
     * The stock line render type depth-tests, hiding what a collision overlay needs to show.
     */
    public static final RenderType LINES_NO_DEPTH = create(
            "ywzj_lines_no_depth",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.empty()))
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .createCompositeState(false));

    private DebugRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                             boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

}
