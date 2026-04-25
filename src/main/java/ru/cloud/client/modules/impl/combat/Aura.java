package ru.cloud.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import lombok.Getter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.input.EventMouseRotation;
import ru.cloud.base.events.impl.player.EventMoveInput;
import ru.cloud.base.events.impl.player.EventRotate;
import ru.cloud.base.events.impl.render.EventRender3D;
import ru.cloud.base.events.impl.server.EventPacket;
import ru.cloud.base.player.AttackUtil;
import ru.cloud.base.rotation.RotationTarget;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.MultiBooleanSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.game.player.PointFinder;
import ru.cloud.utility.game.player.RaytracingUtil;
import ru.cloud.utility.game.player.SimulatedPlayer;
import ru.cloud.utility.game.player.TargetSelector;
import ru.cloud.utility.math.Timer;
import ru.cloud.utility.game.player.*;
import ru.cloud.utility.game.player.rotation.Rotation;
import ru.cloud.utility.game.player.rotation.RotationDelta;
import ru.cloud.utility.game.player.rotation.RotationUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import static ru.cloud.utility.game.player.MovingUtil.fixMovement;

@ModuleAnnotation(name = "Aura", category = Category.COMBAT, description = "Настройки модуля Aura")
public final class Aura extends Module {

    public static final Aura INSTANCE = new Aura();
    private Aura() {}

    
    private final ModeSetting rotationMode = new ModeSetting("Режим ротации");
    private final ModeSetting.Value hvh = new ModeSetting.Value(rotationMode, "ХВХ");
    private final ModeSetting.Value hollyworld = new ModeSetting.Value(rotationMode, "HollyWorld").select();
    private final ModeSetting.Value aimAssist = new ModeSetting.Value(rotationMode, "AimAssist");
    private final ModeSetting.Value sloth = new ModeSetting.Value(rotationMode, "Sloth");
    private final ModeSetting.Value funtime = new ModeSetting.Value(rotationMode, "FunTime");
    private final ModeSetting.Value spooky = new ModeSetting.Value(rotationMode, "SpookyTime");
    private final ModeSetting.Value reallyworld = new ModeSetting.Value(rotationMode, "ReallyWorld");
    private final ModeSetting.Value matrix = new ModeSetting.Value(rotationMode, "Matrix");
    private final ModeSetting.Value test = new ModeSetting.Value(rotationMode, "Test");

    // AimAssist ���������
    private final NumberSetting aimAssistSpeed = new NumberSetting("A im As si st Sp ee d", 7.5f, 1f, 20f, 0.5f, () -> aimAssist.isSelected());
    private final BooleanSetting aimAssistRandomize = new BooleanSetting("AA Рандомизация", false, () -> aimAssist.isSelected());

    
    private final ModeSetting aaHitboxMode = new ModeSetting("Прицел по хитбоксу", () -> aimAssist.isSelected());
    private final ModeSetting.Value aaHitboxOff    = new ModeSetting.Value(aaHitboxMode, "Выкл").select();
    private final ModeSetting.Value aaHitboxEdge   = new ModeSetting.Value(aaHitboxMode, "К краю");
    private final ModeSetting.Value aaHitboxCenter = new ModeSetting.Value(aaHitboxMode, "Центр");
    // ������ �� ���� �������� (������ ����� �� ����)
    private final NumberSetting aaHitboxPadding = new NumberSetting("Отступ хитбокса", 0.05f, 0f, 0.5f, 0.01f,
            () -> aimAssist.isSelected() && !aaHitboxOff.isSelected());

    
    private final ModeSetting sprintMode = new ModeSetting("Бег");
    private final ModeSetting.Value sprintHvh = new ModeSetting.Value(sprintMode, "ХВХ");
    private final ModeSetting.Value sprintNormal = new ModeSetting.Value(sprintMode, "Нормал").select();
    private final ModeSetting.Value sprintLegit = new ModeSetting.Value(sprintMode, "Легит");
    private final ModeSetting.Value sprintNone = new ModeSetting.Value(sprintMode, "Без спринта");

    // ��������� ��������
    private final ModeSetting correction = new ModeSetting("Коррекция движения");
    private final ModeSetting.Value correctionFocus = new ModeSetting.Value(correction, "Фокус");
    private final ModeSetting.Value correctionGood = new ModeSetting.Value(correction, "Свободная").select();
    private final ModeSetting.Value correctionNone = new ModeSetting.Value(correction, "Без коррекции");

    // ?????????
    private final NumberSetting distance = new NumberSetting("Дистанция", 3, 0.5f, 6, 0.1f, "Радиус атаки");
    private final NumberSetting distanceRotation = new NumberSetting("Дистанция ротации", 0.1f, 0, 6, 0.1f);

    // ������ ���������
    private final MultiBooleanSetting settings = new MultiBooleanSetting("Настройки");
    private final MultiBooleanSetting.Value shieldBreak = new MultiBooleanSetting.Value(settings, "Ломать щит", true);
    private final MultiBooleanSetting.Value shielRealese = new MultiBooleanSetting.Value(settings, "Отпускать щит", true);
    private final MultiBooleanSetting.Value eatUseAttack = new MultiBooleanSetting.Value(settings, "Атаковать во время еды", true);
    private final MultiBooleanSetting.Value attackIgnoreWals = new MultiBooleanSetting.Value(settings, "Игнорировать стены", true);

    // ���� �����
    private final MultiBooleanSetting targetTypeSetting = MultiBooleanSetting.create("Цели", List.of("Игроки", "Мобы", "Животные"));

    // �����
    private final BooleanSetting onlyCrit = new BooleanSetting("Только криты", true);
    private final BooleanSetting smartCrit = new BooleanSetting("Умные криты", "Умные криты", false, onlyCrit::isEnabled);

    // FakeLag
    private final BooleanSetting fakeLag = new BooleanSetting("FakeLag [Beta]", "Задержка пакетов", false);
    private final NumberSetting fakeLagDelay = new NumberSetting("Задержка FakeLag", 200f, 50f, 1000f, 10f, fakeLag::isEnabled);
    private final NumberSetting fakeLagRelease = new NumberSetting("FL Сброс (мс)", 100f, 10f, 500f, 10f, fakeLag::isEnabled);

    // private
    private final TargetSelector targetSelector = new TargetSelector();
    private final PointFinder pointFinder = new PointFinder();
    private LivingEntity target = null;
    private boolean legitBackStop = false;
    @Getter
    private boolean preAttack = false;
    @Getter
    private boolean isCanAttack = false;

    // FakeLag state
    private final Deque<net.minecraft.network.packet.Packet<?>> fakeLagQueue = new ArrayDeque<>();
    private final Timer fakeLagCycleTimer = new Timer();
    private boolean fakeLagActive = false;

    // Sloth state
    private final Random slothRandom = new Random();
    // ������� ����� �� ���� (������������ �� body)
    private double slothTargetH  = 0.5;
    private double slothTargetOX = 0.0;
    private double slothTargetOZ = 0.0;
    // ����������������� �����
    private double slothCurrentH  = 0.5;
    private double slothCurrentOX = 0.0;
    private double slothCurrentOZ = 0.0;
    // ������ ����� ������������
    private final Timer slothPointTimer = new Timer();
    private long slothPointInterval = 100;
    private LivingEntity slothLastTarget = null;
    // ������� �������
    private float slothJitterYaw         = 0f;
    private float slothJitterPitch       = 0f;
    private float slothJitterTargetYaw   = 0f;
    private float slothJitterTargetPitch = 0f;
    private final Timer slothJitterTimer = new Timer();
    
    private float slothFlickYaw        = 0f;
    private float slothFlickPitch      = 0f;
    private float slothFlickTargetYaw  = 0f;
    private float slothFlickTargetPitch= 0f;
    
    private final Timer slothAutoFlickTimer = new Timer();
    private long slothAutoFlickInterval = 300;
    
    private float slothHumanSpeed      = 1.0f;
    private float slothHumanSpeedTarget= 1.0f;

    // FunTime state
    private final Random ftRandom = new Random();
    private final Timer ftAttackTimer = new Timer();
    private int ftAttackCount = 0;

    // Legit state
    private float legitLastYaw = 0f;
    private float legitLastPitch = 0f;

    // AimAssist state
    private final Random aaRandom = new Random();
    private double aaTargetHeightFactor = 0.5;
    private double aaTargetOffsetX = 0.0;
    private double aaTargetOffsetZ = 0.0;
    private double aaCurrentHeightFactor = 0.5;
    private double aaCurrentOffsetX = 0.0;
    private double aaCurrentOffsetZ = 0.0;
    private float aaCurrentRandomSpeed = 1.0f;
    private float aaTargetRandomSpeed = 1.0f;
    private LivingEntity aaLastTarget = null;
    private float aaPendingMouseDeltaX = 0f;
    private float aaPendingMouseDeltaY = 0f;

    @EventTarget
    public void eventRotate(EventRotate e) {
        if (legitBackStop) {
            legitBackStop = false;
            mc.options.forwardKey.setPressed(
                    InputUtil.isKeyPressed(mc.getWindow().getHandle(), mc.options.forwardKey.getDefaultKey().getCode())
            );
        }

        target = updateTarget();
        preAttack = false;
        isCanAttack = false;
        if (target == null) return;

        Pair<Vec3d, Box> point = pointFinder.computeVector(
                target,
                distance.getCurrent(),
                rotationManager.getCurrentRotation(),
                new Vec3d(0, 0, 0),
                attackIgnoreWals.isEnabled()
        );

        Vec3d eyes = SimulatedPlayer.simulateLocalPlayer(1).pos.add(0, mc.player.getDimensions(mc.player.getPose()).eyeHeight(), 0);
        Rotation angle = RotationUtil.fromVec3d(point.getLeft().subtract(eyes));

        Box box = point.getRight();
        preAttack = updatePreAttack();
        isCanAttack = isAttack();

        if (RaytracingUtil.rayTrace(rotationManager.getCurrentRotation().toVector(), distance.getCurrent(), box)
                && isCanAttack
                && (!Zenith.getInstance().getServerHandler().isServerSprint() || mc.player.isGliding() || AttackUtil.hasMovementRestrictions() || sprintHvh.isSelected() || sprintNone.isSelected())) {

            if (sprintHvh.isSelected()) {
                mc.player.setSprinting(false);
                mc.player.sendSprintingPacket();
            }

            AttackUtil.attackEntity(target);
            // Флик после удара для Sloth
            if (sloth.isSelected()) {
                slothFlickTargetYaw   = (slothRandom.nextFloat() - 0.5f) * 36f; // ±18°
                slothFlickTargetPitch = (slothRandom.nextFloat() - 0.5f) * 20f; // ±10°
            }
            // ���������� ������ FunTime ��� �����
            if (funtime.isSelected()) {
                ftAttackTimer.reset();
                ftAttackCount++;
            }
            // ���������� ������ ReallyWorld ��� �����
            if (reallyworld.isSelected()) {
                rwAttackTimer.reset();
                rwAttackCount++;
            }
            if (test.isSelected()) testAttackTimer.reset();
            mc.options.sprintKey.setPressed(true);        }
        preAttack = updatePreAttack();
        isCanAttack = isAttack();

        if (hvh.isSelected()) {
            rotationManager.setRotation(
                    new RotationTarget(angle, () -> aimManager.rotate(aimManager.getInstantSetup(), angle), aimManager.getInstantSetup()),
                    3, this
            );
        }

        if (hollyworld.isSelected() && (preAttack || isCanAttack)) {
            rotationManager.setRotation(
                    new RotationTarget(angle, () -> aimManager.rotate(aimManager.getInstantSetup(), angle), aimManager.getAiSetup()),
                    3, this
            );
        }

        if (aimAssist.isSelected()) {
            // ���������� ������� ��� ����� ����
            if (target != aaLastTarget) {
                randomizeAimAssistOffset(target);
                aaCurrentHeightFactor = aaTargetHeightFactor;
                aaCurrentOffsetX = aaTargetOffsetX;
                aaCurrentOffsetZ = aaTargetOffsetZ;
                aaLastTarget = target;
            }
            if (preAttack || isCanAttack) randomizeAimAssistOffset(target);
            Rotation serverAngle = new Rotation(mc.player.getYaw(), mc.player.getPitch());
            rotationManager.setRotation(
                    new RotationTarget(serverAngle, () -> aimManager.rotate(aimManager.getInstantSetup(), new Rotation(mc.player.getYaw(), mc.player.getPitch())), aimManager.getInstantSetup()),
                    3, this
            );
        }

        if (sloth.isSelected()) {
            Rotation slothAngle = computeSlothRotation(rotationManager.getCurrentRotation(), angle);
            rotationManager.setRotation(
                    new RotationTarget(slothAngle, () -> aimManager.rotate(aimManager.getInstantSetup(), slothAngle), aimManager.getInstantSetup()),
                    3, this
            );
        }

        if (funtime.isSelected()) {
            Rotation ftAngle = computeFunTimeRotation(rotationManager.getCurrentRotation(), angle);
            rotationManager.setRotation(
                    new RotationTarget(ftAngle, () -> aimManager.rotate(aimManager.getInstantSetup(), ftAngle), aimManager.getInstantSetup()),
                    3, this
            );
        }

        if (spooky.isSelected()) {
            Rotation spAngle = computeSpookyTimeRotation(rotationManager.getCurrentRotation(), angle);
            rotationManager.setRotation(
                    new RotationTarget(spAngle, () -> aimManager.rotate(aimManager.getInstantSetup(), spAngle), aimManager.getInstantSetup()),
                    3, this
            );
        }

        if (reallyworld.isSelected()) {
            Rotation rwAngle = computeReallyWorldRotation(rotationManager.getCurrentRotation(), angle);
            rotationManager.setRotation(
                    new RotationTarget(rwAngle, () -> aimManager.rotate(aimManager.getInstantSetup(), rwAngle), aimManager.getInstantSetup()),
                    3, this
            );
        }

        if (matrix.isSelected()) {
            Rotation mxAngle = computeMatrixRotation(rotationManager.getCurrentRotation(), angle);
            rotationManager.setRotation(
                    new RotationTarget(mxAngle, () -> aimManager.rotate(aimManager.getInstantSetup(), mxAngle), aimManager.getInstantSetup()),
                    3, this
            );
        }

        if (test.isSelected()) {
            Rotation testAngle = computeTestRotation(rotationManager.getCurrentRotation(), angle);
            rotationManager.setRotation(
                    new RotationTarget(testAngle, () -> aimManager.rotate(aimManager.getInstantSetup(), testAngle), aimManager.getInstantSetup()),
                    3, this
            );
        }

        if (preAttack || isCanAttack) {
            updateSprint();
        }
    }

    private boolean updatePreAttack() {
        SimulatedPlayer simulatedPlayer = SimulatedPlayer.simulateLocalPlayer(1);

        if (mc.player.isUsingItem() && !eatUseAttack.isEnabled()) return false;
        if (mc.player.getAttackCooldownProgress(1) < 0.9) return false;

        if (onlyCrit.isEnabled() && !AttackUtil.hasPreMovementRestrictions(simulatedPlayer)) {
            return AttackUtil.isPrePlayerInCriticalState(simulatedPlayer) || (smartCrit.isEnabled() && !mc.options.jumpKey.isPressed());
        }
        return true;
    }

    private boolean isAttack() {
        if (mc.player.isUsingItem() && !eatUseAttack.isEnabled()) return false;
        if (mc.player.getAttackCooldownProgress(1) < 0.9) return false;

        if (onlyCrit.isEnabled() && !AttackUtil.hasMovementRestrictions()) {
            return AttackUtil.isPlayerInCriticalState() || (smartCrit.isEnabled() && !mc.options.jumpKey.isPressed());
        }
        return true;
    }

    public void updateSprint() {
        if (!hasStopSprint()) return;

        boolean sprint = mc.options.sprintKey.isPressed();
        boolean forward = mc.options.forwardKey.isPressed();

        if (sprintLegit.isSelected()) {
            sprint = false;
            if (mc.player.isSprinting()) {
                forward = false;
                legitBackStop = true;
            }
        }

        if (sprintNormal.isSelected()) {
            if (mc.player.isSprinting()) mc.player.setSprinting(false);
            sprint = false;
        }

        mc.options.sprintKey.setPressed(sprint);
        mc.options.forwardKey.setPressed(forward);
    }

    public boolean hasStopSprint() {
        return !sprintNone.isSelected() && !AttackUtil.hasMovementRestrictions();
    }

    @EventTarget
    public void onMouseRotation(EventMouseRotation e) {
        if (!aimAssist.isSelected() || mc.player == null) return;
        mc.player.changeLookDirection(e.getCursorDeltaX(), e.getCursorDeltaY());
        e.setCancelled(true);
    }

    @EventTarget
    public void onRender3D(EventRender3D e) {
        if (!aimAssist.isSelected() || mc.player == null || target == null) return;

        float pt = e.getPartialTicks();

        float pointAlpha = 1f - (float) Math.pow(1f - 0.015f, pt * 60f / 20f);
        aaCurrentHeightFactor = MathHelper.lerp(pointAlpha, (float) aaCurrentHeightFactor, (float) aaTargetHeightFactor);
        aaCurrentOffsetX      = MathHelper.lerp(pointAlpha, (float) aaCurrentOffsetX,      (float) aaTargetOffsetX);
        aaCurrentOffsetZ      = MathHelper.lerp(pointAlpha, (float) aaCurrentOffsetZ,      (float) aaTargetOffsetZ);

        Vec3d aaPoint;
        if (aaHitboxEdge.isSelected() || aaHitboxCenter.isSelected()) {
            Box bb = target.getBoundingBox();
            Vec3d eyes = mc.player.getEyePos();
            float pad = aaHitboxPadding.getCurrent();

            double innerMinX = bb.minX + pad;
            double innerMaxX = bb.maxX - pad;
            double innerMinZ = bb.minZ + pad;
            double innerMaxZ = bb.maxZ - pad;
            double targetY   = bb.minY + bb.getLengthY() * aaCurrentHeightFactor;

            if (aaHitboxEdge.isSelected()) {
                double clampedX = MathHelper.clamp(eyes.x, innerMinX, innerMaxX);
                double clampedZ = MathHelper.clamp(eyes.z, innerMinZ, innerMaxZ);
                aaPoint = new Vec3d(clampedX, targetY, clampedZ);
            } else {
                double cx = target.getX();
                double cz = target.getZ();
                double dx = eyes.x - cx;
                double dz = eyes.z - cz;
                if (Math.abs(dx) >= Math.abs(dz)) {
                    double faceX = dx > 0 ? innerMaxX : innerMinX;
                    aaPoint = new Vec3d(faceX, targetY, cx + aaCurrentOffsetZ * 0.3);
                } else {
                    double faceZ = dz > 0 ? innerMaxZ : innerMinZ;
                    aaPoint = new Vec3d(cx + aaCurrentOffsetX * 0.3, targetY, faceZ);
                }
            }
        } else {
            aaPoint = target.getPos().add(aaCurrentOffsetX, target.getHeight() * aaCurrentHeightFactor, aaCurrentOffsetZ);
        }

        Vec3d aaEyes = mc.player.getEyePos();
        float targetYaw = (float) Math.toDegrees(Math.atan2(aaPoint.z - aaEyes.z, aaPoint.x - aaEyes.x)) - 90f;
        targetYaw = MathHelper.wrapDegrees(targetYaw);
        double hDist = Math.sqrt(Math.pow(aaPoint.x - aaEyes.x, 2) + Math.pow(aaPoint.z - aaEyes.z, 2));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(aaPoint.y - aaEyes.y, hDist));
        targetPitch = MathHelper.clamp(targetPitch, -90f, 90f);

        float curYaw   = mc.player.getYaw();
        float curPitch = mc.player.getPitch();
        float yawDiff   = MathHelper.wrapDegrees(targetYaw - curYaw);
        float pitchDiff = MathHelper.wrapDegrees(targetPitch - curPitch);
        float totalDiff = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

        float baseSpeed = aimAssistSpeed.getCurrent() / 100f;
        float frameScale = pt;
        float speed = baseSpeed * frameScale;

        float t = MathHelper.clamp(totalDiff / 45f, 0f, 1f);
        float easeOut = 1f - (1f - t) * (1f - t) * (1f - t);
        speed *= MathHelper.clamp(easeOut + 0.05f, 0.05f, 1f);

        if (aimAssistRandomize.isEnabled()) {
            if (aaRandom.nextFloat() < 0.002f) aaTargetRandomSpeed = 0.8f + aaRandom.nextFloat() * 0.4f;
            aaCurrentRandomSpeed = MathHelper.lerp(0.001f * frameScale * 60f, aaCurrentRandomSpeed, aaTargetRandomSpeed);
            speed *= aaCurrentRandomSpeed;
        }

        speed = MathHelper.clamp(speed, 0.0001f, 1f);

        mc.player.setYaw(curYaw   + yawDiff   * speed);
        mc.player.setPitch(MathHelper.clamp(curPitch + pitchDiff * speed, -90f, 90f));
    }

    
    private Rotation computeSlothRotation(Rotation current, Rotation ignored) {
        if (target == null) return current;

        // ����� ��� ����� ����
        if (target != slothLastTarget) {
            slothLastTarget = target;
            slothPickNewPoint(true);
            slothJitterYaw = 0f;
            slothJitterPitch = 0f;
            slothJitterTargetYaw = 0f;
            slothJitterTargetPitch = 0f;
            slothFlickYaw = 0f;
            slothFlickPitch = 0f;
            slothFlickTargetYaw = 0f;
            slothFlickTargetPitch = 0f;
            slothHumanSpeed = 1.0f;
            slothHumanSpeedTarget = 1.0f;
            slothAutoFlickTimer.reset();
        }

        
        if (slothPointTimer.finished(slothPointInterval)) {
            slothPickNewPoint(false);
        }

        
        if (slothRandom.nextFloat() < 0.04f) {
            slothHumanSpeedTarget = 0.55f + slothRandom.nextFloat() * 0.45f;
        }
        slothHumanSpeed = MathHelper.lerp(0.07f, slothHumanSpeed, slothHumanSpeedTarget);

        // ������� ������������ � ������������ � humanize-���������
        float lerpSpeed = 0.18f * slothHumanSpeed;
        slothCurrentH  = MathHelper.lerp(lerpSpeed, (float) slothCurrentH,  (float) slothTargetH);
        slothCurrentOX = MathHelper.lerp(lerpSpeed, (float) slothCurrentOX, (float) slothTargetOX);
        slothCurrentOZ = MathHelper.lerp(lerpSpeed, (float) slothCurrentOZ, (float) slothTargetOZ);

        // ����� ������� �� ����
        Vec3d aimPoint = target.getPos().add(
                slothCurrentOX,
                target.getHeight() * slothCurrentH,
                slothCurrentOZ
        );

        Rotation targetAngle = RotationUtil.fromVec3d(aimPoint.subtract(mc.player.getEyePos()));

        
        if (slothJitterTimer.finished(80 + (long)(slothRandom.nextFloat() * 80))) {
            slothJitterTargetYaw   = (slothRandom.nextFloat() - 0.5f) * 8f;
            slothJitterTargetPitch = (slothRandom.nextFloat() - 0.5f) * 5f;
            slothJitterTimer.reset();
        }
        slothJitterYaw   = MathHelper.lerp(0.12f, slothJitterYaw,   slothJitterTargetYaw);
        slothJitterPitch = MathHelper.lerp(0.12f, slothJitterPitch, slothJitterTargetPitch);

        
        if (slothAutoFlickTimer.finished(slothAutoFlickInterval)) {
            // ����������� ������ �������� target (���������, �� ��������������)
            slothFlickTargetYaw   += (slothRandom.nextFloat() - 0.5f) * 22f; // ±11°
            slothFlickTargetPitch += (slothRandom.nextFloat() - 0.5f) * 14f; // ±7°
            slothAutoFlickInterval = 200 + (long)(slothRandom.nextFloat() * 300);
            slothAutoFlickTimer.reset();
        }

        
        slothFlickYaw   = MathHelper.lerp(0.25f, slothFlickYaw,   slothFlickTargetYaw);
        slothFlickPitch = MathHelper.lerp(0.25f, slothFlickPitch, slothFlickTargetPitch);
        
        slothFlickTargetYaw   *= 0.88f;
        slothFlickTargetPitch *= 0.88f;

        float yawDelta   = MathHelper.wrapDegrees(targetAngle.getYaw()   + slothJitterYaw   + slothFlickYaw   - current.getYaw());
        float pitchDelta = MathHelper.wrapDegrees(targetAngle.getPitch() + slothJitterPitch + slothFlickPitch - current.getPitch());

        
        float dist = (float) Math.hypot(yawDelta, pitchDelta);
        float accel = MathHelper.clamp(dist / 30f, 0f, 1f); // 0 при малом угле, 1 при >30°
        float finalSpeed = MathHelper.lerp(accel, slothHumanSpeed, 1.0f);

        float newYaw   = current.getYaw()   + yawDelta * finalSpeed;
        float newPitch = MathHelper.clamp(current.getPitch() + pitchDelta * finalSpeed, -90f, 90f);

        return new Rotation(newYaw, newPitch);
    }

    
    private void slothPickNewPoint(boolean instant) {
        if (target == null) return;

        slothTargetH  = 0.30 + slothRandom.nextDouble() * 0.40;
        slothTargetOX = (slothRandom.nextDouble() - 0.5) * 0.25;
        slothTargetOZ = (slothRandom.nextDouble() - 0.5) * 0.25;

        if (instant) {
            slothCurrentH  = slothTargetH;
            slothCurrentOX = slothTargetOX;
            slothCurrentOZ = slothTargetOZ;
        }

        
        slothPointInterval = 50 + (long)(slothRandom.nextFloat() * 70);
        slothPointTimer.reset();
    }

    
    private Rotation computeFunTimeRotation(Rotation current, Rotation targetRot) {
        RotationDelta delta = current.rotationDeltaTo(targetRot);
        float yawDelta   = delta.getDeltaYaw();
        float pitchDelta = delta.getDeltaPitch();
        float rotDiff    = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (rotDiff == 0f) return current;

        float lineYaw   = Math.abs(yawDelta   / rotDiff) * 180f;
        float linePitch = Math.abs(pitchDelta / rotDiff) * 180f;
        float moveYaw   = MathHelper.clamp(yawDelta,   -lineYaw,   lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        boolean canAttack = isCanAttack || preAttack;

        if (canAttack) {
            // === ����� "entity != null" �� ��������� ===
            // speed = 1.0, ������� lerp ����� � ����
            float speed = 1.0f;
            float lerpT = ftRandomLerp(speed, speed + 0.2f);
            float newYaw   = MathHelper.lerp(lerpT, current.getYaw(),   current.getYaw()   + moveYaw);
            float newPitch = MathHelper.lerp(lerpT, current.getPitch(), current.getPitch() + movePitch);
            return new Rotation(newYaw, MathHelper.clamp(newPitch, -90f, 90f));
        } else {
            // === ����� "entity == null" �� ��������� ===
            
            float speed = ftAttackTimer.finished(430)
                    ? (ftRandom.nextBoolean() ? 0.4f : 0.2f)
                    : -0.2f;

            float random = ftAttackTimer.getElapsedTime() / 40f + (ftAttackCount % 6);
            int suck = ftAttackCount % 3;

            float[] rv = switch (suck) {
                case 0  -> new float[]{ (float) Math.cos(random), (float) Math.sin(random) };
                case 1  -> new float[]{ (float) Math.sin(random), (float) Math.cos(random) };
                case 2  -> new float[]{ (float) Math.sin(random), (float) -Math.cos(random) };
                default -> new float[]{ (float) -Math.cos(random), (float) Math.sin(random) };
            };

            // ��������� �������� ������ ���� �� ������ 2000 ��
            float yawNoise   = !ftAttackTimer.finished(2000) ? ftRandomLerp(12f, 24f) * rv[0] : 0f;
            float pitchExtra = ftRandomLerp(0f, 2f) * (float) Math.cos((double) System.currentTimeMillis() / 5000.0);
            float pitchNoise = !ftAttackTimer.finished(2000) ? ftRandomLerp(2f, 6f) * rv[1] + pitchExtra : 0f;

            float lerpT  = MathHelper.clamp(ftRandomLerp(speed, speed + 0.2f), 0f, 1f);
            float newYaw   = MathHelper.lerp(lerpT, current.getYaw(),   current.getYaw()   + moveYaw) + yawNoise;
            float newPitch = MathHelper.lerp(lerpT, current.getPitch(), current.getPitch() + movePitch) + pitchNoise;
            return new Rotation(newYaw, MathHelper.clamp(newPitch, -90f, 90f));
        }
    }

    private float ftRandomLerp(float min, float max) {
        return MathHelper.lerp(ftRandom.nextFloat(), min, max);
    }

    // SpookyTime rotation
    private static final float SP_ROTATION_SPEED       = 25.5f;
    private static final float SP_LIMIT_ROTATION_SPEED = 44.5f;
    private static final float SP_ANGLE_LIMIT_PITCH    = 32.334f;
    private final Random spRandom = new Random();

    private Rotation computeSpookyTimeRotation(Rotation current, Rotation targetRot) {
        RotationDelta delta = current.rotationDeltaTo(targetRot);
        float yawDelta   = delta.getDeltaYaw();
        float pitchDelta = delta.getDeltaPitch();
        float length     = (float) Math.hypot(yawDelta, pitchDelta);
        if (length < 1e-3f) return current;

        float angleLimitYaw = (float) Math.min(Math.abs(yawDelta), 74 + spRandom.nextFloat() * 1.0329834f);

        // Pitch
        float newPitch = current.getPitch();
        {
            boolean limitReached = Math.abs(pitchDelta) >= SP_ANGLE_LIMIT_PITCH;
            float maxStep = limitReached ? SP_LIMIT_ROTATION_SPEED : SP_ROTATION_SPEED;
            float step    = Math.min(length, maxStep);
            float scale   = step / length;
            if (!limitReached) scale = spEase(scale);
            newPitch = MathHelper.clamp(current.getPitch() + pitchDelta * scale, -89f, 90f);
        }

        // Yaw
        float newYaw = current.getYaw();
        {
            boolean limitReached = Math.abs(yawDelta) >= angleLimitYaw;
            float maxStep = limitReached ? SP_LIMIT_ROTATION_SPEED : SP_ROTATION_SPEED;
            float step    = Math.min(length, maxStep);
            float scale   = step / length;
            if (!limitReached) scale = spEase(scale);
            newYaw = current.getYaw() + yawDelta * scale;
        }

        return new Rotation(newYaw, newPitch);
    }

    private float spEase(float v) { return v * (0.5f + 0.5f * v); }

    // ReallyWorld rotation
    private final Random rwRandom = new Random();
    private final Timer rwAttackTimer = new Timer();
    private int rwAttackCount = 0;

    private Rotation computeReallyWorldRotation(Rotation current, Rotation targetRot) {
        
        if (target != null) {
            double distance = mc.player.getPos().distanceTo(target.getPos());
            double normalizedDistance = MathHelper.clamp((distance - 1.0) / (3.5 - 1.0), 0.0, 1.0);
            double targetY = target.getY() + target.getHeight() * (0.2 + 0.6 * normalizedDistance);
            Vec3d aimPoint = new Vec3d(target.getX(), targetY, target.getZ());
            targetRot = RotationUtil.fromVec3d(aimPoint.subtract(mc.player.getEyePos()));
        }

        RotationDelta delta = current.rotationDeltaTo(targetRot);
        float yawDelta   = delta.getDeltaYaw();
        float pitchDelta = delta.getDeltaPitch();
        float rotDiff    = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        if (rotDiff == 0f) return current;

        boolean canAttack = isCanAttack || preAttack;

        float lineYaw   = Math.abs(yawDelta   / rotDiff) * 180f;
        float linePitch = Math.abs(pitchDelta / rotDiff) * 180f;
        float moveYaw   = MathHelper.clamp(yawDelta,   -lineYaw,   lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        float jitterYaw   = canAttack ? 0f : (float) (-6 * Math.cos(System.currentTimeMillis() / 90.0));
        float jitterPitch = canAttack ? 0f : (float) ( 6 * Math.sin(System.currentTimeMillis() / 90.0));

        float speed = 1f;
        float lerpT = MathHelper.lerp(rwRandom.nextFloat(), speed, speed + 0.2f);

        float newYaw   = MathHelper.lerp(lerpT, current.getYaw(),   current.getYaw()   + moveYaw)   + jitterYaw;
        float newPitch = MathHelper.lerp(lerpT, current.getPitch(), current.getPitch() + movePitch) + jitterPitch;

        
        if (rwAttackCount > 0 && rwAttackCount % 50 == 0 && !rwAttackTimer.finished(200)) {
            newPitch = MathHelper.lerp(0.55f, current.getPitch(), current.getPitch() - 90f) + jitterPitch;
        }

        return new Rotation(newYaw, MathHelper.clamp(newPitch, -90f, 90f));
    }

    // Matrix rotation
    private final Random mxRandom = new Random();

    private Rotation computeMatrixRotation(Rotation current, Rotation targetRot) {
        RotationDelta delta = current.rotationDeltaTo(targetRot);
        float yawDelta   = delta.getDeltaYaw();
        float pitchDelta = delta.getDeltaPitch();
        float rotDiff    = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        if (rotDiff == 0f) return current;

        boolean canAttack = isCanAttack || preAttack;

        float speed = canAttack ? 1f : MathHelper.lerp(mxRandom.nextFloat(), 0f, 0.5f);

        float lineYaw   = Math.abs(yawDelta   / rotDiff) * (canAttack ? 360f : 100f);
        float linePitch = Math.abs(pitchDelta / rotDiff) * 180f;
        float moveYaw   = MathHelper.clamp(yawDelta,   -lineYaw,   lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        float jitterYaw   = canAttack ? 0f : (float) (MathHelper.lerp(mxRandom.nextFloat(), 0f, 6f)
                * Math.sin(System.currentTimeMillis() / MathHelper.lerp(mxRandom.nextFloat(), 15f, 145f)));
        float jitterPitch = canAttack ? 0f : (float) (MathHelper.lerp(mxRandom.nextFloat(), 1f, 3f)
                * Math.sin(System.currentTimeMillis() / MathHelper.lerp(mxRandom.nextFloat(), 15f, 145f)));

        float lerpT = speed; // оригинал: randomLerp(speed, speed) == speed
        float newYaw   = MathHelper.lerp(lerpT, current.getYaw(),   current.getYaw()   + moveYaw)   + jitterYaw;
        float newPitch = MathHelper.lerp(lerpT, current.getPitch(), current.getPitch() + movePitch) + jitterPitch;

        return new Rotation(newYaw, MathHelper.clamp(newPitch, -90f, 90f));
    }

    // Test rotation (port of LGAngle / CakeWorld)
    private final Random testRandom = new Random();
    private final Timer testAttackTimer = new Timer();

    private Rotation computeTestRotation(Rotation current, Rotation targetRot) {
        RotationDelta delta = current.rotationDeltaTo(targetRot);
        float yawDelta   = delta.getDeltaYaw();
        float pitchDelta = delta.getDeltaPitch();
        float rotDiff    = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        if (rotDiff == 0f) return current;

        boolean canAttack = isCanAttack || preAttack;
        float distanceToTarget = target != null ? (float) mc.player.distanceTo(target) : 0f;

        float baseSpeed = canAttack ? 0.87f : 0.56f;
        float speed = baseSpeed;

        if (distanceToTarget > 0 && distanceToTarget < 0.66f) {
            float closeRangeSpeed = MathHelper.clamp(distanceToTarget / 1.5f * 0.35f, 0.1f, 0.6f);
            speed = canAttack ? 0.85f : Math.min(speed, closeRangeSpeed);
        }

        float lineYaw   = Math.abs(yawDelta   / rotDiff) * 180f;
        float linePitch = Math.abs(pitchDelta / rotDiff) * 180f;

        float jitterYaw   = 0f;
        float jitterPitch = 0f;

        if (!isEnabled() || target == null) {
            if (testAttackTimer.finished(1000)) {
                baseSpeed = 0.35f;
                jitterYaw = 0f;
                jitterPitch = 0f;
            }
        }

        float moveYaw   = MathHelper.clamp(yawDelta,   -lineYaw,   lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

        float newYaw   = MathHelper.lerp(baseSpeed, current.getYaw(),   current.getYaw()   + moveYaw)   + jitterYaw;
        float newPitch = MathHelper.lerp(baseSpeed, current.getPitch(), current.getPitch() + movePitch) + jitterPitch;

        return new Rotation(newYaw, MathHelper.clamp(newPitch, -90f, 90f));
    }

    private float testRandomLerp(float min, float max) {
        return MathHelper.lerp(testRandom.nextFloat(), min, max);
    }

    private void randomizeAimAssistOffset(LivingEntity t) {        if (t == null) return;
        boolean isJumping = !t.isOnGround();
        float rng = aaRandom.nextFloat();
        double minH, maxH;
        if (rng < 0.10f) {
            minH = 0.05; maxH = 0.3;
        } else {
            float rem = (rng - 0.10f) / 0.90f;
            float bodyThreshold = isJumping ? 0.35f : 0.65f;
            if (rem < bodyThreshold) { minH = 0.35; maxH = 0.75; }
            else                     { minH = 0.75; maxH = 0.95; }
        }
        aaTargetHeightFactor = minH + (maxH - minH) * aaRandom.nextDouble();
        aaTargetOffsetX = (aaRandom.nextDouble() - 0.5) * 0.4;
        aaTargetOffsetZ = (aaRandom.nextDouble() - 0.5) * 0.4;
    }

    private LivingEntity updateTarget() {
        TargetSelector.EntityFilter filter = new TargetSelector.EntityFilter(targetTypeSetting.getSelectedNames());
        targetSelector.searchTargets(mc.world.getEntities(), distance.getCurrent() + distanceRotation.getCurrent(), attackIgnoreWals.isEnabled());
        targetSelector.validateTarget(filter::isValid);
        return targetSelector.getCurrentTarget();
    }

    @EventTarget
    private void setCorrection(EventMoveInput eventMoveInput) {
        if (correctionNone.isSelected()) return;

        if (correctionFocus.isSelected()) {
            Rotation angle = RotationUtil.fromVec3d(target.getBoundingBox().getCenter().subtract(mc.player.getBoundingBox().getCenter()));
            fixMovement(eventMoveInput, rotationManager.getCurrentRotation().getYaw(), angle.getYaw());
        } else {
            fixMovement(eventMoveInput, rotationManager.getCurrentRotation().getYaw(), mc.player.getYaw());
        }
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (!fakeLag.isEnabled() || !e.isSent()) return;
        if (!(e.getPacket() instanceof PlayerMoveC2SPacket)) return;

        boolean hasTarget = target != null;

        if (hasTarget) {
            if (!fakeLagActive) {
                fakeLagActive = true;
                fakeLagCycleTimer.reset();
            }
            long elapsed = fakeLagCycleTimer.getElapsedTime();
            long delay = (long) fakeLagDelay.getCurrent();
            long release = (long) fakeLagRelease.getCurrent();
            long phase = elapsed % (delay + release);
            if (phase < delay) {
                fakeLagQueue.add(e.getPacket());
                e.setCancelled(true);
            } else {
                flushFakeLagQueue();
            }
        } else {
            fakeLagActive = false;
            flushFakeLagQueue();
        }
    }

    private void flushFakeLagQueue() {
        if (mc.getNetworkHandler() == null) {
            fakeLagQueue.clear();
            return;
        }
        while (!fakeLagQueue.isEmpty()) {
            mc.getNetworkHandler().sendPacket(fakeLagQueue.poll());
        }
    }

    @Override
    public void onDisable() {
        flushFakeLagQueue();
        fakeLagActive = false;
        slothFlickYaw = 0f;
        slothFlickPitch = 0f;
        slothFlickTargetYaw = 0f;
        slothFlickTargetPitch = 0f;
        slothHumanSpeed = 1.0f;
        slothHumanSpeedTarget = 1.0f;
        slothAutoFlickInterval = 300;
        super.onDisable();
    }

    public LivingEntity getTarget() {
        return this.isEnabled() ? target : null;
    }
}


