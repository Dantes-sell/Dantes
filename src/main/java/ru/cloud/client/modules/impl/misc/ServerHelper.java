package ru.cloud.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.StringUtils;
import ru.cloud.base.events.impl.input.EventSetScreen;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.events.impl.render.EventRender2D;
import ru.cloud.base.events.impl.render.EventRender3D;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.game.player.rotation.RotationUtil;
import ru.cloud.base.events.impl.server.EventChatReceive;
import ru.cloud.base.events.impl.server.EventPacket;
import ru.cloud.base.font.Fonts;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.KeySetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.utility.game.player.PlayerIntersectionUtil;
import ru.cloud.utility.game.player.PlayerInventoryUtil;
import ru.cloud.utility.math.MathUtil;
import ru.cloud.utility.math.ProjectionUtil;
import ru.cloud.utility.math.Timer;
import ru.cloud.utility.render.display.base.color.ColorUtil;
import ru.cloud.utility.render.level.Render3DUtil;

import java.util.*;


@ModuleAnnotation(name = "ServerHelper", category = Category.MISC, description = "???????? ??? ????????? ???????")
public final class ServerHelper extends Module {

    public static final ServerHelper INSTANCE = new ServerHelper();

    // --- Settings ---------------------------------------------------------------
    private final ModeSetting mode = new ModeSetting("??? ???????", "ReallyWorld", "HolyWorld", "FunTime");

    private final BooleanSetting autoLootSetting = new BooleanSetting("???????",
            "????????????? ????????? ??? ?? ????????? ???????????", true,
            () -> mode.is("HolyWorld"));

    private final BooleanSetting autoShulkerSetting = new BooleanSetting("????-??????",
            "????????????? ?????????? ??????? ?? ???????", true,
            () -> mode.is("HolyWorld"));

    private final BooleanSetting autoRepairSetting = new BooleanSetting("????-???????",
            "????? ????? ?????? ??? ?????? ?????????", true,
            () -> mode.is("HolyWorld"));

    private final BooleanSetting consumablesSetting = new BooleanSetting("??????????",
            "?????????? ??????? ??????????? ? ??????????????? ????????", true,
            () -> mode.is("FunTime"));

    private final BooleanSetting autoPointSetting = new BooleanSetting("????-?????",
            "?????????? ????? ??????? ? ?? ?????? ?? HUD", true,
            () -> mode.is("FunTime"));

    // --- Key bindings ------------------------------------------------------------
    private final KeySetting keyAntiFlight = new KeySetting("????-?????", -1, () -> mode.is("ReallyWorld"));
    private final KeySetting keyExpScroll = new KeySetting("?????? ?????", -1, () -> mode.is("ReallyWorld"));
    private final KeySetting keyDTrap = new KeySetting("?-??????", -1, () -> mode.is("HolyWorld"));
    private final KeySetting keyTrapHoly = new KeySetting("????-??????", -1, () -> mode.is("HolyWorld"));
    private final KeySetting keyStan = new KeySetting("????", -1, () -> mode.is("HolyWorld"));
    private final KeySetting keyDItem = new KeySetting("?-???????", -1, () -> mode.is("HolyWorld"));
    private final KeySetting keySnow = new KeySetting("????????? ???????", -1, () -> mode.is("HolyWorld") || mode.is("FunTime"));
    private final KeySetting keyBojAura = new KeySetting("???-????", -1, () -> mode.is("FunTime"));
    private final KeySetting keyTrap = new KeySetting("??????", -1, () -> mode.is("FunTime"));
    private final KeySetting keyPlast = new KeySetting("?????", -1, () -> mode.is("FunTime"));
    private final KeySetting keySugar = new KeySetting("???????? ????", -1, () -> mode.is("FunTime"));
    private final KeySetting keyFireSwirl = new KeySetting("???????? ?????", -1, () -> mode.is("FunTime"));
    private final KeySetting keyDisorientation = new KeySetting("?????????????", -1, () -> mode.is("FunTime"));
    private final KeySetting keyTikva = new KeySetting("????????? ????", -1, () -> mode.is("HolyWorld"));
    private final KeySetting keyExp = new KeySetting("??????? ?????", -1, () -> mode.is("HolyWorld"));
    private final KeySetting keyShulker1 = new KeySetting("?????? 1", -1, () -> mode.is("HolyWorld"));
    private final KeySetting keyShulker2 = new KeySetting("?????? 2", -1, () -> mode.is("HolyWorld"));
    private final KeySetting keyShulker3 = new KeySetting("?????? 3", -1, () -> mode.is("HolyWorld"));
    private final KeySetting keyShulker4 = new KeySetting("?????? 4", -1, () -> mode.is("HolyWorld"));
    private final KeySetting keyOtriga = new KeySetting("??????", -1, () -> mode.is("FunTime"));
    private final KeySetting keySerka = new KeySetting("?????", -1, () -> mode.is("FunTime"));
    private final KeySetting keyVspihka = new KeySetting("???????", -1, () -> mode.is("FunTime"));
    private final KeySetting keyMochaFlesha = new KeySetting("???? ?????", -1, () -> mode.is("FunTime"));
    private final KeySetting keyPobedilka = new KeySetting("?????????", -1, () -> mode.is("FunTime"));
    private final KeySetting keyAgent = new KeySetting("?????", -1, () -> mode.is("FunTime"));
    private final KeySetting keyMedik = new KeySetting("?????", -1, () -> mode.is("FunTime"));
    private final KeySetting keyKiller = new KeySetting("??????", -1, () -> mode.is("FunTime"));


    // --- Inner types -------------------------------------------------------------
    private static class ServerEvent {
        final String name;
        final String lvl;
        final String owner;
        final Vec3d center;
        final int duration;
        final int delay;
        final long createdAt;

        ServerEvent(String name, String lvl, String owner, Vec3d center, int duration, int delay) {
            this.name = name;
            this.lvl = lvl;
            this.owner = owner;
            this.center = center;
            this.duration = duration;
            this.delay = delay;
            this.createdAt = System.currentTimeMillis();
        }
    }

    private static class Structure {
        final String name;
        final Vec3d center;

        Structure(String name, Vec3d center) {
            this.name = name;
            this.center = center;
        }
    }

    // --- Item config -------------------------------------------------------------
    private static class ItemInfo {
        final String searchName;
        final Item item;
        final String displayName;
        final KeySetting key;
        final float distance;

        ItemInfo(String searchName, Item item, String displayName, KeySetting key, float distance) {
            this.searchName = searchName;
            this.item = item;
            this.displayName = displayName;
            this.key = key;
            this.distance = distance;
        }
    }

    private final List<ItemInfo> itemInfos = new ArrayList<>();

    // --- State -------------------------------------------------------------------
    private final Map<BlockPos, BlockState> blockStateMap = new HashMap<>();
    private final List<ServerEvent> serverEvents = new ArrayList<>();
    private final List<Structure> structures = new ArrayList<>();
    private final Map<Integer, Item> stacks = new HashMap<>();
    private final List<String> potionQueue = new ArrayList<>();
    private final Map<String, Boolean> lastKeyStates = new HashMap<>();

    private final Timer itemsWatch = new Timer();
    private final Timer shulkerWatch = new Timer();
    private final Timer repairWatch = new Timer();
    private final Timer potionTimer = new Timer();

    private UUID entityUUID;
    private boolean shulkerScriptPending = false;

    private ServerHelper() {
    }

    @Override
    public void onEnable() {
        super.onEnable();
        stacks.clear();
        potionQueue.clear();
        potionTimer.reset();
        lastKeyStates.replaceAll((k, v) -> false);
        shulkerScriptPending = false;
        initItemInfos();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        lastKeyStates.replaceAll((k, v) -> false);
        potionQueue.clear();
        potionTimer.reset();
    }

    private void initItemInfos() {
        itemInfos.clear();
        itemInfos.add(new ItemInfo("?????????????", Items.ENDER_EYE, "?????????????", keyDisorientation, 10));
        itemInfos.add(new ItemInfo("Sugar", Items.SUGAR, "Sugar", keySugar, 10));
        itemInfos.add(new ItemInfo("???-????", Items.PHANTOM_MEMBRANE, "???-????", keyBojAura, 0));
        itemInfos.add(new ItemInfo("????????? ???????", Items.SNOWBALL, "????????? ???????", keySnow, 0));
        itemInfos.add(new ItemInfo("?????", Items.DRIED_KELP, "?????", keyPlast, 0));
        itemInfos.add(new ItemInfo("??????", Items.NETHERITE_SCRAP, "??????", keyTrap, 0));
        itemInfos.add(new ItemInfo("???????? ?????", Items.FIRE_CHARGE, "???????? ?????", keyFireSwirl, 10));
        itemInfos.add(new ItemInfo("??????", Items.SPLASH_POTION, "??????", keyOtriga, 0));
        itemInfos.add(new ItemInfo("?????", Items.SPLASH_POTION, "?????", keySerka, 0));
        itemInfos.add(new ItemInfo("???????", Items.SPLASH_POTION, "???????", keyVspihka, 0));
        itemInfos.add(new ItemInfo("???? ?????", Items.SPLASH_POTION, "???? ?????", keyMochaFlesha, 0));
        itemInfos.add(new ItemInfo("?????????", Items.SPLASH_POTION, "?????????", keyPobedilka, 0));
        itemInfos.add(new ItemInfo("?????", Items.SPLASH_POTION, "?????", keyAgent, 0));
        itemInfos.add(new ItemInfo("?????", Items.SPLASH_POTION, "?????", keyMedik, 0));
        itemInfos.add(new ItemInfo("??????", Items.SPLASH_POTION, "??????", keyKiller, 0));
        itemInfos.add(new ItemInfo("????-?????", Items.FIREWORK_STAR, "????-?????", keyAntiFlight, 0));
        itemInfos.add(new ItemInfo("?????? ?????", Items.FLOWER_BANNER_PATTERN, "?????? ?????", keyExpScroll, 0));
        itemInfos.add(new ItemInfo("?-??????", Items.PRISMARINE_SHARD, "?-??????", keyDTrap, 5));
        itemInfos.add(new ItemInfo("????-??????", Items.POPPED_CHORUS_FRUIT, "????-??????", keyTrapHoly, 0));
        itemInfos.add(new ItemInfo("????", Items.NETHER_STAR, "????", keyStan, 30));
        itemInfos.add(new ItemInfo("?-???????", Items.FIRE_CHARGE, "?-???????", keyDItem, 5));
        itemInfos.add(new ItemInfo("????????? ????", Items.JACK_O_LANTERN, "????????? ????", keyTikva, 0));
        itemInfos.add(new ItemInfo("??????? ?????", Items.EXPERIENCE_BOTTLE, "??????? ?????", keyExp, 0));
        itemInfos.add(new ItemInfo("Shulker I", Items.PINK_SHULKER_BOX, "Shulker I", keyShulker1, 0));
        itemInfos.add(new ItemInfo("Shulker II", Items.BLUE_SHULKER_BOX, "Shulker II", keyShulker2, 0));
        itemInfos.add(new ItemInfo("Shulker III", Items.RED_SHULKER_BOX, "Shulker III", keyShulker3, 0));
        itemInfos.add(new ItemInfo("Shulker IV", Items.PINK_SHULKER_BOX, "Shulker IV", keyShulker4, 0));
        itemInfos.forEach(info -> lastKeyStates.put(info.displayName, false));
    }


    // --- Helpers -----------------------------------------------------------------
    private void addEvent(String name, String lvl, String owner, Vec3d center, int duration, int delay) {
        serverEvents.removeIf(e -> e.name.equals(name));
        serverEvents.add(new ServerEvent(name, lvl, owner, center, duration, delay));
    }

    // --- Packet handler ----------------------------------------------------------
    @EventTarget
    public void onPacket(EventPacket e) {
        if (PlayerIntersectionUtil.nullCheck()) return;
        if (!e.isReceive()) return;

        if (e.getPacket() instanceof ItemPickupAnimationS2CPacket item
                && autoShulkerSetting.isEnabled() && autoShulkerSetting.isVisible()
                && item.getCollectorEntityId() == mc.player.getId()
                && mc.world.getEntityById(item.getEntityId()) instanceof ItemEntity entity) {
            ItemStack stack = entity.getStack();
            if (stack.get(DataComponentTypes.CONTAINER) == null) {
                stacks.put(-((int) MathUtil.getRandom(1, 999999999)), stack.getItem());
                shulkerWatch.reset();
            }
        }

        if (e.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket slot && slot.getSyncId() == 0) {
            Item item = slot.getStack().getItem();
            stacks.entrySet().stream()
                    .filter(entry -> entry.getKey() < 0 && entry.getValue().equals(item))
                    .findFirst()
                    .ifPresent(entry -> {
                        stacks.put(slot.getSlot() + 18, item);
                        stacks.remove(entry.getKey());
                    });
        }

        if (e.getPacket() instanceof ChunkDeltaUpdateS2CPacket chunkDelta
                && consumablesSetting.isEnabled() && consumablesSetting.isVisible()) {
            chunkDelta.visitUpdates((pos, state) -> blockStateMap.put(pos.add(0, 0, 0), state));
        }

        if (e.getPacket() instanceof OpenScreenS2CPacket openScreen
                && openScreen.getName().getString().contains("������")
                && !stacks.isEmpty()) {
            shulkerScriptPending = true;
        }
    }

    // --- Chat handler ------------------------------------------------------------
    @EventTarget
    public void onChat(EventChatReceive e) {
        if (PlayerIntersectionUtil.nullCheck()) return;
        Text content = e.getMessage();
        String message = content.getString();
        String contentString = content.toString();

        if (autoPointSetting.isEnabled() && autoPointSetting.isVisible()) {
            String name = StringUtils.substringBetween(message, "||| [", "] ");
            if (name != null) {
                String position = StringUtils.substringBetween(contentString, "value='/gps ", "'");
                String lvl = extractField(message, "???????:");
                String owner = extractField(message, "????????:");
                if (position != null) {
                    String[] pose = position.split(" ");
                    Vec3d center = BlockPos.ofFloored(
                            Integer.parseInt(pose[0]), Integer.parseInt(pose[1]), Integer.parseInt(pose[2])
                    ).toCenterPos();
                    int duration = 300;
                    int delay = 0;
                    if ("??????".equals(name)) {
                        delay = 120;
                    }
                    addEvent(name, lvl, owner, center, duration, delay);
                } else if ("?????? ?????".equals(name)) {
                    addEvent(name, lvl, owner, BlockPos.ofFloored(48, 87, 73).toCenterPos(), 180, 120);
                }
            }
        }

        if (message.toLowerCase(Locale.ROOT).contains("?????")) {
            String sub = extractNumber(message);
            if (sub != null && !sub.isEmpty()) {
                try {
                    int duration = Integer.parseInt(sub.trim()) * 20;
                    ItemCooldownManager manager = mc.player.getItemCooldownManager();
                    manager.set(Items.EXPERIENCE_BOTTLE.getDefaultStack(), duration);
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    private String extractField(String message, String label) {
        int index = message.indexOf(label);
        if (index == -1) {
            return null;
        }
        String tail = message.substring(index + label.length()).trim();
        int newline = tail.indexOf("\n");
        return newline >= 0 ? tail.substring(0, newline).trim() : tail;
    }
    private String extractNumber(String message) {
        var matcher = java.util.regex.Pattern.compile("(\\d+)").matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }
    // --- SetScreen handler -------------------------------------------------------
    @EventTarget
    public void onSetScreen(EventSetScreen e) {
        if (e.getScreen() instanceof GenericContainerScreen screen
                && screen.getTitle().getString().contains("������")
                && shulkerScriptPending) {
            e.setScreen(null);
        }
    }

    // --- Update handler ----------------------------------------------------------
    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (PlayerIntersectionUtil.nullCheck()) return;

        // пїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ
        long now = System.currentTimeMillis();
        serverEvents.removeIf(ev -> now - ev.createdAt > (ev.duration + ev.delay) * 1000L);

        
        if (autoRepairSetting.isEnabled() && autoRepairSetting.isVisible()
                && repairWatch.finished(3000)) {
            boolean needRepair = false;
            for (int i = 36; i <= 39; i++) {
                ItemStack armor = mc.player.getInventory().getStack(i);
                if (!armor.isEmpty()) {
                    int maxDamage = armor.getMaxDamage();
                    if (maxDamage > 0) {
                        int damage = armor.getDamage();
                        float durability = 1f - (float) damage / maxDamage;
                        if (durability < 0.2f) {
                            needRepair = true;
                            break;
                        }
                    }
                }
            }
            if (needRepair) {
                repairWatch.reset();
                PlayerInventoryUtil.swapAndUse(Items.EXPERIENCE_BOTTLE);
            }
        }

        // пїЅпїЅпїЅпїЅ-пїЅпїЅпїЅпїЅпїЅпїЅ (HolyWorld): пїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅ пїЅпїЅпїЅпїЅпїЅпїЅ
        if (autoShulkerSetting.isEnabled() && autoShulkerSetting.isVisible()
                && shulkerScriptPending
                && shulkerWatch.finished(500)
                && mc.currentScreen == null) {
            shulkerScriptPending = false;
            Map<Integer, Item> toMove = new HashMap<>(stacks);
            stacks.clear();
            toMove.forEach((slotId, item) -> {
                if (slotId > 0) {
                    Slot slot = PlayerInventoryUtil.getSlot(s -> s.id == slotId && s.getStack().getItem().equals(item));
                    if (slot != null) {
                        PlayerInventoryUtil.moveItem(slot, mc.player.getInventory().selectedSlot, true);
                    }
                }
            });
        }

        // пїЅпїЅпїЅпїЅ-пїЅпїЅпїЅ пїЅ пїЅпїЅпїЅпїЅпїЅ (HolyWorld): пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅ пїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ
        if (autoLootSetting.isEnabled() && autoLootSetting.isVisible()
                && itemsWatch.finished(200)) {
            itemsWatch.reset();
            mc.world.getEntities().forEach(entity -> {
                if (entity instanceof MerchantEntity merchant
                        && merchant.isDead()
                        && merchant.distanceTo(mc.player) < 5f) {
                    mc.world.getEntities().forEach(e2 -> {
                        if (e2 instanceof ItemEntity itemEntity
                                && itemEntity.distanceTo(mc.player) < 5f) {
                            mc.interactionManager.attackEntity(mc.player, itemEntity);
                        }
                    });
                }
            });
        }

        // пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ
        for (ItemInfo info : itemInfos) {
            if (info.key.getKeyCode() == -1 || !info.key.isVisible()) continue;
            boolean pressed = PlayerIntersectionUtil.isKey(info.key);
            boolean wasPressed = lastKeyStates.getOrDefault(info.displayName, false);
            lastKeyStates.put(info.displayName, pressed);
            if (pressed && !wasPressed) {
                if (info.distance > 0) {
                    
                    mc.world.getPlayers().stream()
                            .filter(p -> p != mc.player && p.distanceTo(mc.player) <= info.distance)
                            .min(Comparator.comparingDouble(p -> p.distanceTo(mc.player)))
                            .ifPresentOrElse(
                                    target -> PlayerInventoryUtil.swapAndUse(info.item,
                                            RotationUtil.calculateAngle(target.getEyePos())),
                                    () -> PlayerInventoryUtil.swapAndUse(info.item)
                            );
                } else {
                    PlayerInventoryUtil.swapAndUse(info.item);
                }
            }
        }
    }

    // --- Render 3D handler -------------------------------------------------------
    @EventTarget
    public void onRender3D(EventRender3D e) {
        if (PlayerIntersectionUtil.nullCheck()) return;
        if (!autoPointSetting.isEnabled() || !autoPointSetting.isVisible()) return;

        long now = System.currentTimeMillis();
        for (ServerEvent ev : serverEvents) {
            long elapsed = (now - ev.createdAt) / 1000L;
            if (elapsed < ev.delay) continue; // пїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅ пїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅ
            long remaining = ev.duration + ev.delay - elapsed;
            if (remaining <= 0) continue;

            int color = remaining < 30 ? ColorUtil.makeColor(255, 80, 80, 180)
                    : remaining < 90 ? ColorUtil.makeColor(255, 200, 50, 180)
                    : ColorUtil.makeColor(80, 200, 255, 180);

            Box box = new Box(ev.center.subtract(1, 0, 1), ev.center.add(1, 2, 1));
            Render3DUtil.drawBox(box, color, 1.5f);
            Render3DUtil.drawLine(mc.player.getEyePos(), ev.center.add(0, 1, 0), color, 1f, false);
        }
    }

    // --- Render 2D handler -------------------------------------------------------
    @EventTarget
    public void onRender2D(EventRender2D e) {
        if (PlayerIntersectionUtil.nullCheck()) return;

        float screenW = mc.getWindow().getScaledWidth();
        float screenH = mc.getWindow().getScaledHeight();
        long now = System.currentTimeMillis();

        // пїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ (FunTime): пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ
        if (consumablesSetting.isEnabled() && consumablesSetting.isVisible()) {
            float y = screenH / 2f - 40;
            float x = screenW - 120;
            for (ItemInfo info : itemInfos) {
                if (!info.key.isVisible()) continue;
                float cd = mc.player.getItemCooldownManager().getCooldownProgress(info.item.getDefaultStack(), 0f);
                if (cd <= 0) continue;
                int seconds = Math.round(cd);
                String text = info.displayName + ": " + seconds + "с";
                float w = Fonts.MEDIUM.getWidth(text, 7f) + 8;
                e.getContext().drawRoundedRect(x - w, y, w, 13,
                        BorderRadius.all(4),
                        new ColorRGBA(0, 0, 0, 140));
                e.getContext().drawText(Fonts.MEDIUM.getFont(7f), text, x - w + 4, y + 3,
                        new ColorRGBA(255, 255, 255, 255));
                y += 15;
            }
        }

        // пїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅ (FunTime): пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅ
        if (autoPointSetting.isEnabled() && autoPointSetting.isVisible()) {
            float y = 10;
            for (ServerEvent ev : serverEvents) {
                long elapsed = (now - ev.createdAt) / 1000L;
                long remaining = ev.duration + ev.delay - elapsed;
                if (remaining <= 0) continue;

                boolean active = elapsed >= ev.delay;
                String status = active ? "???????" : "????? " + (ev.delay - elapsed) + "?";
                String line1 = ev.name + (ev.lvl != null ? " [" + ev.lvl + "]" : "");
                String line2 = status + " | " + remaining + "?";
                String line3 = ev.owner != null ? "????????: " + ev.owner : null;

                float maxW = Math.max(Fonts.SEMIBOLD.getWidth(line1, 7.5f), Fonts.MEDIUM.getWidth(line2, 7f));
                if (line3 != null) maxW = Math.max(maxW, Fonts.MEDIUM.getWidth(line3, 7f));
                maxW += 12;

                int bgColor = active ? ColorUtil.makeColor(20, 20, 20, 180) : ColorUtil.makeColor(40, 30, 10, 180);
                e.getContext().drawRoundedRect(8, y, maxW, line3 != null ? 38 : 28,
                        BorderRadius.all(5),
                        new ColorRGBA(bgColor));

                e.getContext().drawText(Fonts.SEMIBOLD.getFont(7.5f), line1, 14, y + 4,
                        new ColorRGBA(255, 255, 255, 255));
                e.getContext().drawText(Fonts.MEDIUM.getFont(7f), line2, 14, y + 15,
                        new ColorRGBA(active ? 100 : 255, active ? 220 : 180, active ? 100 : 50, 255));
                if (line3 != null) {
                    e.getContext().drawText(Fonts.MEDIUM.getFont(7f), line3, 14, y + 26,
                            new ColorRGBA(180, 180, 180, 255));
                }

                // пїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅпїЅпїЅпїЅпїЅ пїЅпїЅ пїЅпїЅпїЅпїЅпїЅ
                if (ProjectionUtil.canSee(ev.center)) {
                    Vec3d screen = ProjectionUtil.worldSpaceToScreenSpace(ev.center.add(0, 1, 0));
                    if (screen.z > 0 && screen.z < 1) {
                        float sx = (float) screen.x;
                        float sy = (float) screen.y;
                        String dist = (int) mc.player.getPos().distanceTo(ev.center) + "м";
                        float dw = Fonts.MEDIUM.getWidth(dist, 6.5f) + 6;
                        e.getContext().drawRoundedRect(sx - dw / 2, sy - 8, dw, 12,
                                BorderRadius.all(3),
                                new ColorRGBA(0, 0, 0, 160));
                        e.getContext().drawText(Fonts.MEDIUM.getFont(6.5f), dist, sx - dw / 2 + 3, sy - 6,
                                new ColorRGBA(255, 255, 255, 255));
                    }
                }

                y += (line3 != null ? 38 : 28) + 4;
            }
        }
    }
}



