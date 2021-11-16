package xyz.wagyourtail.minimap.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.movement.IMovement;
import baritone.api.utils.BetterBlockPos;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.wagyourtail.minimap.api.client.MinimapClientApi;
import xyz.wagyourtail.minimap.api.client.config.MinimapClientConfig;
import xyz.wagyourtail.minimap.client.gui.hud.map.AbstractMinimapRenderer;
import xyz.wagyourtail.minimap.client.gui.hud.overlay.AbstractMinimapOverlay;

public class BaritoneMinimapOverlay extends AbstractMinimapOverlay {

    public BaritoneMinimapOverlay(AbstractMinimapRenderer parent) {
        super(parent);
    }

    @Override
    public void renderOverlay(PoseStack stack, @NotNull Vec3 center, float maxLength, @NotNull Vec3 player_pos, float player_rot) {
        IPath path = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().orElse(null);
        if (path != null) {
            int chunkRadius = MinimapClientApi.getInstance().getConfig().get(MinimapClientConfig.class).chunkRadius;
            int chunkDiam = chunkRadius * 2 - 1;
            float chunkScale = maxLength / ((float) chunkDiam - 1);

            for (IMovement movement : path.movements()) {
                stack.pushPose();
                BetterBlockPos start = movement.getSrc();
                BetterBlockPos end = movement.getDest();

                Vec3 startVec = new Vec3(start.x, start.y, start.z).subtract(center);
                Vec3 endVec = new Vec3(end.x, end.y, end.z).subtract(center);

                if (parent.rotate) {
                    startVec = startVec.yRot((float) Math.toRadians(
                        player_rot - 180));
                    endVec = endVec.yRot((float) Math.toRadians(
                        player_rot - 180));
                }
                if (parent.scaleBy != 1) {
                    startVec = startVec.multiply(parent.scaleBy, 1, parent.scaleBy);
                    endVec = endVec.multiply(parent.scaleBy, 1, parent.scaleBy);
                }

                float startScale = parent.getScaleForVecToBorder(startVec, chunkRadius, maxLength);
                float endScale = parent.getScaleForVecToBorder(endVec, chunkRadius, maxLength);

                if (startScale < 1 && endScale < 1) {
                    continue;
                }
                if (startScale < 1) {
                    startVec.multiply(startScale, 1, startScale);
                }
                if (endScale < 1) {
                    endVec.multiply(endScale, 1, endScale);
                }

                line(
                    stack,
                    (float) (maxLength / 2 + startVec.x * chunkScale / 16f),
                    (float) (maxLength / 2 + startVec.z * chunkScale / 16f),
                    (float) (maxLength / 2 + endVec.x * chunkScale / 16f),
                    (float) (maxLength / 2 + endVec.z * chunkScale / 16f),
                0xFF0000FF
                );


                stack.popPose();
            }

        }
    }

    public void line(PoseStack stack, float x0, float y0, float x1, float y1, int abgr) {
        Matrix4f matrix = stack.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        float a = (abgr >> 0x18 & 0xFF) / 255f;
        float b = (abgr >> 0x10 & 0xFF) / 255f;
        float g = (abgr >> 0x08 & 0xFF) / 255f;
        float r = (abgr & 0xFF) / 255f;
        builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(matrix, x0, y0, 0).color(r, g, b, a).endVertex();
        builder.vertex(matrix, x1, y1, 0).color(r, g, b, a).endVertex();
        builder.end();
        BufferUploader.end(builder);

    }

}
