package ru.cloud.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

import ru.cloud.base.events.impl.player.EventRotate;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.rotation.RotationTarget;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.utility.game.player.rotation.Rotation;

@ModuleAnnotation(name = "AutoCart", category = Category.COMBAT, description = "????????? ?????? AutoCart")
public final class AutoCart extends Module {

    public static final AutoCart INSTANCE = new AutoCart();
    private AutoCart() {}

    private final ModeSetting modeSetting = new ModeSetting("?????");
    private final ModeSetting.Value preRail   = new ModeSetting.Value(modeSetting, "??????????").select();
    private final ModeSetting.Value instaCart = new ModeSetting.Value(modeSetting, "?????-?????????");

    private final BooleanSetting silentRotation = new BooleanSetting("????? ???????", "????? ???????", false);

    private enum State {
        IDLE,
        PLACING_RAIL, DRAWING_BOW, SHOOTING, PLACING_MINECART,
        INSTA_DRAWING_BOW, INSTA_SHOOTING, INSTA_PLACING_RAIL, INSTA_PLACING_MINECART,
        DONE
    }

    private State currentState = State.IDLE;
    private int actionTimer = 0;
    private int originalSlot = 0;
    private int railSlot = -1;
    private int bowSlot = -1;
    private int tntMinecartSlot = -1;
    private BlockHitResult targetHit = null;
    private boolean bowStarted = false;
    private float originalPitch = 0;
    private float originalYaw = 0;

    @Override
    public void onEnable() {
        super.onEnable();
        // ????????? ??? ????????? ???? ??????? ?? ????
        execute();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        reset();
    }

    @EventTarget
    public void onRotate(EventRotate e) {
        if (mc.player == null || currentState == State.IDLE) return;
        if (!silentRotation.isEnabled()) return;

        boolean shouldRotate = (preRail.isSelected() && currentState == State.SHOOTING && actionTimer >= 4)
                || (instaCart.isSelected() && currentState == State.INSTA_SHOOTING && actionTimer >= 4);

        if (shouldRotate) {
            float targetPitch = originalPitch - 12.0f;
            Rotation angle = new Rotation(mc.player.getYaw(), targetPitch);
            rotationManager.setRotation(
                    new RotationTarget(angle, () -> aimManager.rotate(aimManager.getInstantSetup(), angle), aimManager.getInstantSetup()),
                    5, this
            );
        }
    }

    @EventTarget
    public void onTick(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;
        if (currentState != State.IDLE) {
            processTick();
        }
    }

    private void execute() {
        if (mc.player == null || mc.world == null) return;

        tntMinecartSlot = findTNTMinecart();
        railSlot        = findRail();
        bowSlot         = findFlameBow();

        if (tntMinecartSlot == -1 || railSlot == -1 || bowSlot == -1) return;

        HitResult hit = mc.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        targetHit     = (BlockHitResult) hit;
        originalSlot  = mc.player.getInventory().selectedSlot;
        originalPitch = mc.player.getPitch();
        originalYaw   = mc.player.getYaw();
        actionTimer   = 0;
        bowStarted    = false;

        currentState = preRail.isSelected() ? State.PLACING_RAIL : State.INSTA_DRAWING_BOW;
    }

    private void processTick() {
        if (mc.player == null || mc.interactionManager == null || mc.world == null) {
            reset();
            return;
        }
        actionTimer++;
        try {
            if (preRail.isSelected()) processPreRailMode();
            else processInstaCartMode();
        } catch (Exception ex) {
            reset();
        }
    }

    private void processPreRailMode() {
        switch (currentState) {
            case PLACING_RAIL -> {
                if (actionTimer == 1) {
                    mc.player.getInventory().selectedSlot = railSlot;
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, targetHit);
                    currentState = State.DRAWING_BOW;
                    actionTimer = 0;
                }
            }
            case DRAWING_BOW -> {
                if (actionTimer == 1) {
                    mc.player.getInventory().selectedSlot = bowSlot;
                    mc.options.useKey.setPressed(true);
                    bowStarted = true;
                    currentState = State.SHOOTING;
                    actionTimer = 0;
                }
            }
            case SHOOTING -> {
                if (actionTimer == 5 && !silentRotation.isEnabled()) {
                    mc.player.setPitch(mc.player.getPitch() - 12.0f);
                } else if (actionTimer == 6 && bowStarted) {
                    mc.options.useKey.setPressed(false);
                    bowStarted = false;
                } else if (actionTimer >= 7) {
                    if (!silentRotation.isEnabled()) {
                        mc.player.setPitch(originalPitch);
                        mc.player.setYaw(originalYaw);
                    }
                    currentState = State.PLACING_MINECART;
                    actionTimer = 0;
                }
            }
            case PLACING_MINECART -> {
                if (actionTimer == 1) {
                    mc.player.getInventory().selectedSlot = tntMinecartSlot;
                    mc.options.useKey.setPressed(true);
                } else if (actionTimer == 2) {
                    mc.options.useKey.setPressed(false);
                    currentState = State.DONE;
                    actionTimer = 0;
                }
            }
            case DONE -> {
                if (actionTimer >= 1) {
                    mc.player.getInventory().selectedSlot = originalSlot;
                    reset();
                }
            }
            default -> {}
        }
    }

    private void processInstaCartMode() {
        switch (currentState) {
            case INSTA_DRAWING_BOW -> {
                if (actionTimer == 1) {
                    mc.player.getInventory().selectedSlot = bowSlot;
                    mc.options.useKey.setPressed(true);
                    bowStarted = true;
                    currentState = State.INSTA_SHOOTING;
                    actionTimer = 0;
                }
            }
            case INSTA_SHOOTING -> {
                if (actionTimer == 5 && !silentRotation.isEnabled()) {
                    mc.player.setPitch(mc.player.getPitch() - 12.0f);
                } else if (actionTimer == 6 && bowStarted) {
                    mc.options.useKey.setPressed(false);
                    bowStarted = false;
                } else if (actionTimer >= 7) {
                    if (!silentRotation.isEnabled()) {
                        mc.player.setPitch(originalPitch);
                        mc.player.setYaw(originalYaw);
                    }
                    currentState = State.INSTA_PLACING_RAIL;
                    actionTimer = 0;
                }
            }
            case INSTA_PLACING_RAIL -> {
                if (actionTimer == 1) {
                    mc.player.getInventory().selectedSlot = railSlot;
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, targetHit);
                    currentState = State.INSTA_PLACING_MINECART;
                    actionTimer = 0;
                }
            }
            case INSTA_PLACING_MINECART -> {
                if (actionTimer == 1) {
                    mc.player.getInventory().selectedSlot = tntMinecartSlot;
                    mc.options.useKey.setPressed(true);
                } else if (actionTimer == 2) {
                    mc.options.useKey.setPressed(false);
                    currentState = State.DONE;
                    actionTimer = 0;
                }
            }
            case DONE -> {
                if (actionTimer >= 1) {
                    mc.player.getInventory().selectedSlot = originalSlot;
                    reset();
                }
            }
            default -> {}
        }
    }

    private void reset() {
        if (bowStarted && mc.options != null) mc.options.useKey.setPressed(false);
        currentState  = State.IDLE;
        actionTimer   = 0;
        bowStarted    = false;
        targetHit     = null;
        originalPitch = 0;
        originalYaw   = 0;
    }

    private int findTNTMinecart() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TNT_MINECART) return i;
        }
        return -1;
    }

    private int findRail() {
        for (int i = 0; i < 9; i++) {
            String id = Registries.ITEM.getId(mc.player.getInventory().getStack(i).getItem()).toString();
            if (id.contains("rail")) return i;
        }
        return -1;
    }

    private int findFlameBow() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() != Items.BOW) continue;
            ItemEnchantmentsComponent enchants = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
            if (enchants.isEmpty()) continue;
            var reg   = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            var flame = reg.get(Enchantments.FLAME);
            if (flame != null && enchants.getLevel(reg.getEntry(flame)) > 0) return i;
        }
        return -1;
    }
}






