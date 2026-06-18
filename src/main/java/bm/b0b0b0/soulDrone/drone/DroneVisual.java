package bm.b0b0b0.soulDrone.drone;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.lang.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class DroneVisual {

    private final List<BlockDisplay> displays = new ArrayList<>();
    private final List<UUID> entityIds = new ArrayList<>();
    private final Matrix4f[] segmentMatrices;
    private final SegmentSpec[] segments;
    private final Vector restForward;
    private final float blockScale;
    private TextDisplay label;
    private Interaction hitbox;
    private final boolean labelEnabled;
    private final float labelOffsetY;
    private final int labelBackgroundArgb;
    private final float hitboxWidth;
    private final float hitboxHeight;

    public DroneVisual(
            PluginConfig config,
            MessageService messages,
            World world,
            Location pivot,
            String receiverName,
            String senderName
    ) {
        this.segments = config.segmentSpecs().toArray(new SegmentSpec[0]);
        this.segmentMatrices = new Matrix4f[segments.length];
        this.blockScale = config.blockScale();
        this.restForward = restForwardFromSegments(segments, blockScale);
        this.labelEnabled = config.labelEnabled();
        this.labelOffsetY = config.labelOffsetY();
        this.labelBackgroundArgb = config.labelBackgroundArgb();
        this.hitboxWidth = config.hitboxWidth();
        this.hitboxHeight = config.hitboxHeight();

        for (int index = 0; index < segments.length; index++) {
            SegmentSpec segment = segments[index];
            segmentMatrices[index] = localMatrix(segment);
            BlockDisplay display = world.spawn(pivot, BlockDisplay.class, entity -> {
                entity.setBlock(segment.material().createBlockData());
                entity.setBillboard(Display.Billboard.FIXED);
                entity.setPersistent(false);
                entity.setGravity(false);
                entity.setBrightness(new Display.Brightness(15, 15));
                entity.setInterpolationDuration(0);
                entity.setInterpolationDelay(0);
                entity.setTeleportDuration(0);
            });
            displays.add(display);
            entityIds.add(display.getUniqueId());
        }

        if (labelEnabled) {
            Component text = messages.component("drone-label", receiverName, senderName);
            label = world.spawn(pivot, TextDisplay.class, entity -> {
                entity.text(text);
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setPersistent(false);
                entity.setSeeThrough(true);
                entity.setShadowed(true);
                entity.setDefaultBackground(false);
                if (labelBackgroundArgb != 0) {
                    entity.setBackgroundColor(Color.fromARGB(labelBackgroundArgb));
                }
                entity.setBrightness(new Display.Brightness(15, 15));
                entity.setInterpolationDuration(0);
                entity.setInterpolationDelay(0);
                entity.setTeleportDuration(0);
            });
            entityIds.add(label.getUniqueId());
        }

        hitbox = world.spawn(pivot, Interaction.class, entity -> {
            entity.setInteractionWidth(hitboxWidth);
            entity.setInteractionHeight(hitboxHeight);
            entity.setResponsive(true);
            entity.setPersistent(false);
        });
        entityIds.add(hitbox.getUniqueId());
        updateHitbox(pivot);
    }

    public List<UUID> entityIds() {
        return entityIds;
    }

    public Matrix4f[] createAssemblyStartMatrices() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Matrix4f[] matrices = new Matrix4f[segments.length];
        for (int index = 0; index < segments.length; index++) {
            float scatterX = random.nextFloat(-5f, 5f);
            float scatterY = random.nextFloat(-2.5f, 3.5f);
            float scatterZ = random.nextFloat(-5f, 5f);
            matrices[index] = new Matrix4f()
                    .rotateXYZ(
                            (float) Math.toRadians(random.nextFloat(-180f, 180f)),
                            (float) Math.toRadians(random.nextFloat(-180f, 180f)),
                            (float) Math.toRadians(random.nextFloat(-180f, 180f))
                    )
                    .translate(scatterX, scatterY, scatterZ)
                    .translate(-0.5f, -0.5f, -0.5f);
        }
        return matrices;
    }

    public void updateAssembly(
            Location pivot,
            float yaw,
            float pitch,
            float progress,
            int spinTick,
            Matrix4f[] startMatrices
    ) {
        Matrix4f rotation = matrixFromYawPitch(yaw, pitch, restForward);
        float spinStrength = (1f - progress) * (float) Math.toRadians(22f);

        for (int index = 0; index < displays.size(); index++) {
            Matrix4f end = new Matrix4f(rotation).mul(segmentMatrices[index]);
            Matrix4f current = new Matrix4f(startMatrices[index]).lerp(end, progress);
            if (spinStrength > 0.001f) {
                current.rotateY(spinStrength * spinTick);
            }
            applyMatrix(displays.get(index), pivot, current);
        }
        updateLabel(pivot);
        updateHitbox(pivot);
    }

    public void update(Location pivot, float yaw, float pitch) {
        update(pivot, yaw, pitch, 0f);
    }

    public void update(Location pivot, float yaw, float pitch, float extraSpinRadians) {
        Matrix4f rotation = matrixFromYawPitch(yaw, pitch, restForward);
        if (Math.abs(extraSpinRadians) > 0.001f) {
            rotation.rotateY(extraSpinRadians);
        }

        for (int index = 0; index < displays.size(); index++) {
            Matrix4f matrix = new Matrix4f(rotation).mul(segmentMatrices[index]);
            applyMatrix(displays.get(index), pivot, matrix);
        }
        updateLabel(pivot);
        updateHitbox(pivot);
    }

    public void remove() {
        for (BlockDisplay display : displays) {
            display.remove();
        }
        displays.clear();
        if (label != null) {
            label.remove();
            label = null;
        }
        if (hitbox != null) {
            hitbox.remove();
            hitbox = null;
        }
        entityIds.clear();
    }

    public static Vector noseDirection(float yaw, float pitch) {
        Location location = new Location(null, 0, 0, 0, yaw, pitch);
        return location.getDirection().normalize();
    }

    public static Vector lateralFromYawPitch(float yaw, float pitch) {
        Vector forward = noseDirection(yaw, pitch);
        Vector lateral = forward.clone().crossProduct(new Vector(0, 1, 0));
        if (lateral.lengthSquared() < 0.0001) {
            return new Vector(1, 0, 0);
        }
        return lateral.normalize();
    }

    public static Matrix4f matrixFromYawPitch(float yaw, float pitch, Vector restForward) {
        Vector target = noseDirection(yaw, pitch);
        Quaternionf rotation = new Quaternionf().rotationTo(
                (float) restForward.getX(),
                (float) restForward.getY(),
                (float) restForward.getZ(),
                (float) target.getX(),
                (float) target.getY(),
                (float) target.getZ()
        );
        return new Matrix4f().rotate(rotation);
    }

    private Matrix4f localMatrix(SegmentSpec segment) {
        return new Matrix4f()
                .translate(
                        segment.lateral() * blockScale,
                        segment.vertical() * blockScale,
                        segment.forward() * blockScale
                )
                .translate(-0.5f, -0.5f, -0.5f)
                .translate(0.5f, 0.5f, 0.5f)
                .scale(blockScale)
                .translate(-0.5f, -0.5f, -0.5f);
    }

    private void applyMatrix(BlockDisplay display, Location pivot, Matrix4f matrix) {
        display.setInterpolationDuration(0);
        display.setInterpolationDelay(0);
        display.setTeleportDuration(0);
        display.teleport(pivot);
        display.setTransformationMatrix(matrix);
    }

    private void updateLabel(Location pivot) {
        if (label == null) {
            return;
        }
        Location labelLocation = pivot.clone().add(0, labelOffsetY, 0);
        label.setInterpolationDuration(0);
        label.setInterpolationDelay(0);
        label.setTeleportDuration(0);
        label.teleport(labelLocation);
    }

    private void updateHitbox(Location pivot) {
        if (hitbox == null) {
            return;
        }
        Location hitboxLocation = pivot.clone().subtract(0, hitboxHeight * 0.5f, 0);
        hitbox.teleport(hitboxLocation);
    }

    private static Vector restForwardFromSegments(SegmentSpec[] specs, float scale) {
        if (specs.length == 0) {
            return new Vector(0, 0, 1);
        }
        SegmentSpec tip = specs[specs.length - 1];
        SegmentSpec tail = specs[Math.max(0, specs.length - 2)];
        Vector3f nose = segmentPointLocal(tip, 1f, 0.5f, 0.5f, scale);
        Vector3f tailPoint = segmentPointLocal(tail, 0.5f, 0.5f, 0.5f, scale);
        return new Vector(nose.x - tailPoint.x, nose.y - tailPoint.y, nose.z - tailPoint.z).normalize();
    }

    private static Vector3f segmentPointLocal(SegmentSpec segment, float x, float y, float z, float scale) {
        Matrix4f matrix = new Matrix4f()
                .translate(segment.lateral() * scale, segment.vertical() * scale, segment.forward() * scale)
                .translate(-0.5f, -0.5f, -0.5f)
                .translate(0.5f, 0.5f, 0.5f)
                .scale(scale)
                .translate(-0.5f, -0.5f, -0.5f);
        Vector3f point = new Vector3f(x, y, z);
        matrix.transformPosition(point);
        return point;
    }

    public record SegmentSpec(int forward, int lateral, int vertical, Material material) {
    }

}
