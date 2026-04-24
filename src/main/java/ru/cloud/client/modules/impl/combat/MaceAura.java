package ru.cloud.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import ru.cloud.base.events.impl.player.EventMove;
import ru.cloud.base.events.impl.player.EventMoveInput;
import ru.cloud.base.events.impl.player.EventRotate;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.player.AttackUtil;
import ru.cloud.base.rotation.RotationTarget;
import ru.cloud.utility.game.player.*;
import ru.cloud.utility.math.Timer;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.MultiBooleanSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.game.player.*;
import ru.cloud.utility.game.player.rotation.Rotation;
import ru.cloud.utility.game.player.rotation.RotationUtil;

import java.util.List;

import static ru.cloud.utility.game.player.MovingUtil.fixMovement;

@ModuleAnnotation(name = "MaceAura", category = Category.COMBAT, description = "Бьет булавой без ожидания кд")
public final class MaceAura extends Module {

    public static final MaceAura INSTANCE = new MaceAura();
    private MaceAura() {}

    private final NumberSetting distance        = new NumberSetting("Дистанция", 3, 0.5f, 6, 0.1f, "Дистанция атаки");
    private final NumberSetting distanceRotation = new NumberSetting("Пре-дистанция", 0.1f, 0, 6, 0.1f);
    private final NumberSetting elytraRange     = new NumberSetting("Дайв дистанция", 12, 1, 30, 0.5f, "Радиус поиска цели при полёте на элитрах");
    private final NumberSetting diveSpeed       = new NumberSetting("Скорость дайва", 0.8f, 0.1f, 3.0f, 0.05f, "Скорость полёта к цели");

    private final MultiBooleanSetting settings = new MultiBooleanSetting("Настройки");
    private final MultiBooleanSetting.Value attackThroughWalls = new MultiBooleanSetting.Value(settings, "Бить через стены", true);
    private final MultiBooleanSetting.Value fixMovementSetting = new MultiBooleanSetting.Value(settings, "Коррекция движения", true);
    private final MultiBooleanSetting.Value autoDive           = new MultiBooleanSetting.Value(settings, "Авто-дайв на элитрах", true);
    private final MultiBooleanSetting.Value autoSwapChest      = new MultiBooleanSetting.Value(settings, "Свап элитры на нагрудник", true);

    private final MultiBooleanSetting targetTypeSetting = MultiBooleanSetting.create("Атаковать", List.of("Игроков", "Враждебных", "Мирных"));

    private final BooleanSetting onlyMace = new BooleanSetting("Только булава", "Атаковать только если в руке булава", true);

    private final TargetSelector targetSelector = new TargetSelector();
    private final PointFinder pointFinder    = new PointFinder();

    private LivingEntity target        = null;
    private boolean      swappedElytra = false;
    private boolean      diving        = false;
    // Таймер между ударами булавой (чтобы не спамить)
    private final Timer  maceHitTimer  = new Timer();
    // Флаг: уже ударили в этом дайве, ждём следующего прыжка
    private boolean      hitThisDive   = false;

    private static final int CHEST_ARMOR_SLOT = 6;

    @Override
    public void onDisable() {
        super.onDisable();
        restoreElytra();
        target        = null;
        swappedElytra = false;
        diving        = false;
        hitThisDive   = false;
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        
        boolean elytraDive = autoDive.isEnabled() && isGlidingInAir();
        float searchRange = elytraDive
                ? elytraRange.getCurrent()
                : distance.getCurrent() + distanceRotation.getCurrent();

        TargetSelector.EntityFilter filter = new TargetSelector.EntityFilter(targetTypeSetting.getSelectedNames());
        targetSelector.searchTargets(mc.world.getEntities(), searchRange, attackThroughWalls.isEnabled());
        targetSelector.validateTarget(filter::isValid);
        target = targetSelector.getCurrentTarget();

        if (target == null) {
            
            if (swappedElytra) restoreElytra();
            diving = false;
            return;
        }

        if (elytraDive) {
            // �������� ����: ������� ������ �� ��������� ����� ��� ����������� ����
            if (autoSwapChest.isEnabled() && isWearingElytra() && !swappedElytra) {
                doSwapElytraToChestplate();
            }
            diving = true;
        } else {
            // Приземлились или дайв выключен
            if (swappedElytra && !isGlidingInAir()) {
                restoreElytra();
            }
            diving = false;
        }
    }

    @EventTarget
    public void onRotate(EventRotate e) {
        if (target == null) return;
        if (onlyMace.isEnabled() && !(mc.player.getMainHandStack().getItem() instanceof MaceItem)) return;

        Pair<Vec3d, Box> point = pointFinder.computeVector(
                target,
                distance.getCurrent(),
                rotationManager.getCurrentRotation(),
                new Vec3d(0, 0, 0),
                attackThroughWalls.isEnabled()
        );

        Vec3d eyes = SimulatedPlayer.simulateLocalPlayer(1).pos
                .add(0, mc.player.getDimensions(mc.player.getPose()).eyeHeight(), 0);
        Rotation angle = RotationUtil.fromVec3d(point.getLeft().subtract(eyes));

        // Всегда ротируемся на цель
        rotationManager.setRotation(
                new RotationTarget(angle, () -> aimManager.rotate(aimManager.getAiSetup(), angle), aimManager.getAiSetup()),
                3, this
        );

        boolean inAir     = !mc.player.isOnGround();
        
        
        boolean canAttack;
        if (diving) {
            // Дайв-режим: бьём один раз за дайв, не раньше чем через 500мс после предыдущего удара
            canAttack = !hitThisDive && maceHitTimer.finished(500);
        } else {
            // Обычный режим: ждём полный кд
            canAttack = mc.player.getAttackCooldownProgress(1) >= 0.9f;
        }

        if (canAttack && RaytracingUtil.rayTrace(rotationManager.getCurrentRotation().toVector(), distance.getCurrent(), point.getRight())) {
            AttackUtil.attackEntity(target);
            mc.options.sprintKey.setPressed(true);
            maceHitTimer.reset();
            if (diving) {
                hitThisDive = true;
                // После удара возвращаем элитру
                if (swappedElytra) restoreElytra();
                diving = false;
            }
        }

        // Сбрасываем hitThisDive когда игрок снова поднялся в воздух (новый дайв)
        if (mc.player.isOnGround()) {
            hitThisDive = false;
        }
    }

    @EventTarget
    public void onMove(EventMove e) {
        if (!diving || target == null) return;
        if (mc.player.isOnGround()) return;

        Vec3d from = mc.player.getPos();
        Vec3d to   = target.getPos();

        double dx  = to.x - from.x;
        double dy  = to.y - from.y;
        double dz  = to.z - from.z;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (len < 0.01) return;

        // Останавливаем дайв когда уже в радиусе атаки
        if (len <= distance.getCurrent()) return;

        double spd = diveSpeed.getCurrent();
        e.setMovePos(new Vec3d((dx / len) * spd, (dy / len) * spd, (dz / len) * spd));
    }

    @EventTarget
    private void onMoveInput(EventMoveInput e) {
        if (!fixMovementSetting.isEnabled() || target == null) return;
        fixMovement(e, rotationManager.getCurrentRotation().getYaw(), mc.player.getYaw());
    }

    // Игрок летит на элитрах в воздухе
    private boolean isGlidingInAir() {
        return !mc.player.isOnGround() && mc.player.isGliding();
    }

    private boolean isWearingElytra() {
        return mc.player.getInventory().getArmorStack(2).getItem() == Items.ELYTRA;
    }

    private void doSwapElytraToChestplate() {
        Slot chest = PlayerInventoryUtil.getSlot(s -> {
            Item item = s.getStack().getItem();
            if (!(item instanceof ArmorItem armor)) return false;
            String name = armor.toString().toLowerCase();
            return (name.contains("chestplate") || name.contains("tunic")) && s.id != CHEST_ARMOR_SLOT;
        });
        if (chest == null) return;

        // pickup нагрудник -> положить в слот брони (вытолкнет элитру) -> закрыть курсор
        PlayerInventoryUtil.clickSlot(chest.id,        0, SlotActionType.PICKUP, false);
        PlayerInventoryUtil.clickSlot(CHEST_ARMOR_SLOT, 0, SlotActionType.PICKUP, false);
        PlayerInventoryUtil.clickSlot(chest.id,        0, SlotActionType.PICKUP, false);
        PlayerInventoryUtil.closeScreen(true);
        swappedElytra = true;
    }

    private void restoreElytra() {
        if (!swappedElytra) return;
        Slot elytraSlot = PlayerInventoryUtil.getSlot(s ->
                s.getStack().getItem() == Items.ELYTRA && s.id != CHEST_ARMOR_SLOT);
        if (elytraSlot != null) {
            PlayerInventoryUtil.clickSlot(elytraSlot.id,   0, SlotActionType.PICKUP, false);
            PlayerInventoryUtil.clickSlot(CHEST_ARMOR_SLOT, 0, SlotActionType.PICKUP, false);
            PlayerInventoryUtil.clickSlot(elytraSlot.id,   0, SlotActionType.PICKUP, false);
            PlayerInventoryUtil.closeScreen(true);
        }
        swappedElytra = false;
    }

    public LivingEntity getTarget() {
        return this.isEnabled() ? target : null;
    }
}

