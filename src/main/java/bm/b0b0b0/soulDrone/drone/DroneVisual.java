package bm.b0b0b0.soulDrone.drone;

import bm.b0b0b0.soulDrone.config.PluginConfig;
import bm.b0b0b0.soulDrone.lang.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class DroneVisual {

    private static final int MOVE_INTERPOLATION_TICKS = 2;

    private final List<Display> partDisplays = new ArrayList<>();
    private final List<UUID> entityIds = new ArrayList<>();
    private final Matrix4f[] partMatrices;
    private final Vector restForward;
    private TextDisplay label;
    private Interaction hitbox;
    private final boolean labelEnabled;
    private final float labelOffsetY;
    private final int labelBackgroundArgb;
    private final float hitboxWidth;
    private final float hitboxHeight;
    private final boolean particlesEnabled;
    private final float blockScale;
    private final List<Vector3f> fanLocalPoints;
    private final Vector3f smokeLocalPoint;

    public DroneVisual(
            PluginConfig config,
            MessageService messages,
            World world,
            Location pivot,
            String receiverName,
            String senderName
    ) {
        this.labelEnabled = config.labelEnabled();
        this.labelOffsetY = config.labelOffsetY();
        this.labelBackgroundArgb = config.labelBackgroundArgb();
        this.hitboxWidth = config.hitboxWidth();
        this.hitboxHeight = config.hitboxHeight();
        this.particlesEnabled = config.droneParticlesEnabled();

        SegmentSpec[] segments = config.segmentSpecs().toArray(new SegmentSpec[0]);
        float blockScale = config.blockScale();
        this.blockScale = blockScale;
        this.fanLocalPoints = fanPointsFromSegments(segments, blockScale);
        this.smokeLocalPoint = smokePointFromSegments(segments, blockScale);
        partMatrices = new Matrix4f[segments.length];
        for (int index = 0; index < segments.length; index++) {
            SegmentSpec segment = segments[index];
            partMatrices[index] = localMatrix(segment, blockScale);
            BlockDisplay display = world.spawn(pivot, BlockDisplay.class, entity -> {
                entity.setBlock(segment.material().createBlockData());
                configureDisplay(entity, 15, 15);
            });
            partDisplays.add(display);
            entityIds.add(display.getUniqueId());
        }
        this.restForward = restForwardFromSegments(segments, blockScale);

        if (labelEnabled) {
            Component text = messages.component("drone-label", receiverName, senderName);
            label = world.spawn(pivot, TextDisplay.class, entity -> {
                entity.text(text);
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setPersistent(false);
                entity.setSeeThrough(true);
                entity.setShadowed(true);
                entity.setDefaultBackground(false);
                entity.setGravity(false);
                entity.setBrightness(new Display.Brightness(15, 15));
                entity.setInterpolationDelay(0);
                if (labelBackgroundArgb != 0) {
                    entity.setBackgroundColor(Color.fromARGB(labelBackgroundArgb));
                }
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
        Matrix4f[] matrices = new Matrix4f[partMatrices.length];
        for (int index = 0; index < partMatrices.length; index++) {
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

        for (int index = 0; index < partDisplays.size(); index++) {
            Matrix4f end = new Matrix4f(rotation).mul(partMatrices[index]);
            Matrix4f current = new Matrix4f(startMatrices[index]).lerp(end, progress);
            if (spinStrength > 0.001f) {
                current.rotateY(spinStrength * spinTick);
            }
            applyMatrix(partDisplays.get(index), pivot, current);
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

        for (int index = 0; index < partDisplays.size(); index++) {
            Matrix4f matrix = new Matrix4f(rotation).mul(partMatrices[index]);
            applyMatrix(partDisplays.get(index), pivot, matrix);
        }
        updateLabel(pivot);
        updateHitbox(pivot);
    }

    public void remove() {
        for (Display display : partDisplays) {
            display.remove();
        }
        partDisplays.clear();
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

    public void spawnAmbientParticles(World world, Location pivot, float yaw, float pitch, int tick) {
        if (!particlesEnabled || fanLocalPoints.isEmpty()) {
            return;
        }

        Matrix4f rotation = matrixFromYawPitch(yaw, pitch, restForward);
        float fallCycle = (tick % 12) * blockScale * 0.04f;

        for (Vector3f fanLocal : fanLocalPoints) {
            Location fanBase = transformLocalPoint(pivot, rotation, fanLocal);
            Location falling = fanBase.clone().subtract(0, fallCycle, 0);
            world.spawnParticle(Particle.SMOKE, falling, 1, 0.035, 0.015, 0.035, 0.002);
        }

        Location smoke = transformLocalPoint(pivot, rotation, smokeLocalPoint);
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, smoke, 1, 0.04, 0.05, 0.04, 0.006);
        world.spawnParticle(Particle.SMOKE, smoke, 1, 0.03, 0.06, 0.03, 0.004);
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
        float restYaw = (float) Math.toDegrees(Math.atan2(-restForward.getX(), restForward.getZ()));
        return new Matrix4f()
                .rotateY((float) Math.toRadians(yaw - restYaw))
                .rotateX((float) Math.toRadians(pitch));
    }

    private static void configureDisplay(Display entity, int blockLight, int skyLight) {
        entity.setBillboard(Display.Billboard.FIXED);
        entity.setPersistent(false);
        entity.setGravity(false);
        entity.setBrightness(new Display.Brightness(blockLight, skyLight));
        entity.setInterpolationDelay(0);
    }

    private static Matrix4f localMatrix(SegmentSpec segment, float blockScale) {
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

    private void applyMatrix(Display display, Location pivot, Matrix4f matrix) {
        display.setInterpolationDuration(MOVE_INTERPOLATION_TICKS);
        display.setTeleportDuration(MOVE_INTERPOLATION_TICKS);
        display.teleport(pivot);
        display.setTransformationMatrix(matrix);
    }

    private void updateLabel(Location pivot) {
        if (label == null) {
            return;
        }
        Location labelLocation = pivot.clone().add(0, labelOffsetY, 0);
        label.setInterpolationDuration(MOVE_INTERPOLATION_TICKS);
        label.setTeleportDuration(MOVE_INTERPOLATION_TICKS);
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
        Matrix4f matrix = localMatrix(segment, scale);
        Vector3f point = new Vector3f(x, y, z);
        matrix.transformPosition(point);
        return point;
    }

    private static List<Vector3f> fanPointsFromSegments(SegmentSpec[] segments, float scale) {
        List<Vector3f> points = new ArrayList<>();
        for (SegmentSpec segment : segments) {
            if (segment.forward() == 0 && segment.lateral() == 0 && segment.vertical() == 0) {
                continue;
            }
            if (segment.material() == Material.SHULKER_BOX) {
                continue;
            }
            points.add(segmentPointLocal(segment, 0.5f, 0.08f, 0.5f, scale));
        }
        return points;
    }

    private static Vector3f smokePointFromSegments(SegmentSpec[] segments, float scale) {
        SegmentSpec center = segments.length > 0 ? segments[0] : null;
        for (SegmentSpec segment : segments) {
            if (segment.forward() == 0 && segment.lateral() == 0 && segment.vertical() == 0) {
                center = segment;
                break;
            }
        }
        if (center == null) {
            return new Vector3f(0f, scale * 0.6f, 0f);
        }
        Vector3f top = segmentPointLocal(center, 0.5f, 1f, 0.5f, scale);
        top.y += scale * 0.12f;
        return top;
    }

    private static Location transformLocalPoint(Location pivot, Matrix4f rotation, Vector3f local) {
        Vector3f point = new Vector3f(local);
        rotation.transformPosition(point);
        return new Location(
                pivot.getWorld(),
                pivot.getX() + point.x,
                pivot.getY() + point.y,
                pivot.getZ() + point.z
        );
    }

    public record SegmentSpec(int forward, int lateral, int vertical, Material material) {
    }

}
