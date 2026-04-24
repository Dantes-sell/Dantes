package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.input.EventMouse;
import ru.cloud.base.events.impl.input.EventSetScreen;
import ru.cloud.base.events.impl.other.EventModuleToggle;
import ru.cloud.base.events.impl.other.EventWindowResize;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.events.impl.render.EventHudRender;
import ru.cloud.base.font.Font;
import ru.cloud.base.font.Fonts;
import ru.cloud.base.theme.Theme;
import ru.cloud.client.hud.elements.component.CooldownComponent;
import ru.cloud.client.hud.elements.component.DynamicIslandComponent;
import ru.cloud.client.hud.elements.component.HootBarComponent;
import ru.cloud.client.hud.elements.component.InformationComponent;
import ru.cloud.client.hud.elements.component.InventoryComponent;
import ru.cloud.client.hud.elements.component.KeybindsComponent;
import ru.cloud.client.hud.elements.component.MusicInfoComponent;
import ru.cloud.client.hud.elements.component.NotifyComponent;
import ru.cloud.client.hud.elements.component.PlayerListComponent;
import ru.cloud.client.hud.elements.component.PotionsComponent;
import ru.cloud.client.hud.elements.component.ScoreBoardComponent;
import ru.cloud.client.hud.elements.component.StaffComponent;
import ru.cloud.client.hud.elements.component.TargetHudComponent;
import ru.cloud.client.hud.elements.component.WatermarkComponent;
import ru.cloud.client.hud.elements.draggable.DraggableHudElement;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.client.modules.api.setting.impl.ModeSetting;
import ru.cloud.client.modules.api.setting.impl.MultiBooleanSetting;
import ru.cloud.client.modules.api.setting.impl.NumberSetting;
import ru.cloud.utility.game.other.TextUtil;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.CustomDrawContext;
import ru.cloud.utility.render.display.base.GuiUtil;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.render.display.shader.DrawUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static ru.cloud.utility.render.display.Render2DUtil.glowCache;

@ModuleAnnotation(name = "Interface", category = Category.RENDER, description = "Mind style interface")
public final class Interface extends Module {

    public static final Interface INSTANCE = new Interface();

    private final List<DraggableHudElement> elements = new ArrayList<>();

    private final MultiBooleanSetting elementsSetting = MultiBooleanSetting.create("Elements", List.of(
            "Watermark",
            "Keybinds",
            "Totems",
            "Fireworks",
            "Armor",
            "Notifications",
            "Staff List",
            "Potion List",
            "Target HUD",
            "Information",
            "Timer Indicator",
            "Event Schedule",
            "Cooldowns",
            "Dynamic Island",
            "Inventory",
            "Music",
            "Hotbar",
            "Scoreboard",
            "Tab"
    ));

    private final NumberSetting scale = new NumberSetting("Scale", 2.0f, 1.0f, 3.0f, 0.1f, (oldValue, newValue) -> {
        float width = mc.getWindow().getWidth() / newValue;
        float height = mc.getWindow().getHeight() / newValue;
        for (DraggableHudElement element : elements) {
            element.windowResized(width, height);
        }
    });
    private final BooleanSetting corners = new BooleanSetting("Corners", true);
    private final BooleanSetting blur = new BooleanSetting("Blur", false);
    private final BooleanSetting glow = new BooleanSetting("Glow", false);
    private final ModeSetting watermarkStyle = new ModeSetting("Theme", "Old", "New");

    private final WatermarkComponent watermarkComponent =
            new WatermarkComponent("Watermark", 0.0f, 0.0f, 960.0f, 495.5f, 10.0f, 10.0f, DraggableHudElement.Align.TOP_LEFT);
    private final KeybindsComponent keybindsComponent =
            new KeybindsComponent("Keybinds", 0.0f, 0.0f, 960.0f, 495.5f, 120.0f, 95.0f, DraggableHudElement.Align.TOP_LEFT);
    private final ItemCounterElement totemCounter =
            new ItemCounterElement("Totems", Items.TOTEM_OF_UNDYING, "Totems", 150.0f, 150.0f, DraggableHudElement.Align.TOP_LEFT);
    private final ItemCounterElement fireworkCounter =
            new ItemCounterElement("Fireworks", Items.FIREWORK_ROCKET, "Fireworks", 150.0f, 169.0f, DraggableHudElement.Align.TOP_LEFT);
    private final ArmorPreviewElement armorPreview =
            new ArmorPreviewElement("Armor", 0.0f, 0.0f, 0.0f, -58.0f, DraggableHudElement.Align.BOTTOM_CENTER);
    private final NotifyComponent notifyComponent =
            new NotifyComponent("Notify", 181.80615f, 135.5f, 960.0f, 495.5f, 157.03516f, -72.5f, DraggableHudElement.Align.CENTER);
    private final StaffComponent staffComponent =
            new StaffComponent("Staff", 0.0f, 0.0f, 960.0f, 495.5f, 350.0f, 50.0f, DraggableHudElement.Align.TOP_LEFT);
    private final PotionsComponent potionsComponent =
            new PotionsComponent("Potions", 0.0f, 0.0f, 960.0f, 495.5f, 7.0f, 42.0f, DraggableHudElement.Align.TOP_LEFT);
    private final TargetHudComponent targetHudComponent =
            new TargetHudComponent("TargetHUD", 166.5f, 128.5f, 960.0f, 495.5f, 0.0f, 31.75f, DraggableHudElement.Align.CENTER);
    private final InformationComponent informationComponent =
            new InformationComponent("Information", 0.0f, 0.0f, 960.0f, 495.5f, 10.0f, -30.0f, DraggableHudElement.Align.BOTTOM_LEFT);
    private final TimerIndicatorElement timerIndicator =
            new TimerIndicatorElement("TimerIndicator", 0.0f, 0.0f, 160.0f, 180.0f, DraggableHudElement.Align.TOP_LEFT);
    private final EventScheduleElement eventSchedule =
            new EventScheduleElement("EventSchedule", 0.0f, 0.0f, 350.0f, 55.0f, DraggableHudElement.Align.TOP_LEFT);
    private final CooldownComponent cooldownComponent =
            new CooldownComponent("Cooldowns", 0.0f, 0.0f, 960.0f, 495.5f, 7.0f, 105.0f, DraggableHudElement.Align.TOP_LEFT);
    private final DynamicIslandComponent dynamicIsland =
            new DynamicIslandComponent("Dynamic Island", 0f, 0f, 960f, 495.5f, 0f, 4f, DraggableHudElement.Align.TOP_CENTER);
    private final InventoryComponent inventoryComponent =
            new InventoryComponent("Inventory", 269.0f, 229.0f, 960.0f, 495.5f, -11.5f, -74.0f, DraggableHudElement.Align.BOTTOM_RIGHT);
    private final MusicInfoComponent musicInfoComponent =
            new MusicInfoComponent("MusicInfo", 342.0f, 257.0f, 960.0f, 495.5f, -11.5f, -16.5f, DraggableHudElement.Align.BOTTOM_RIGHT);
    private final HootBarComponent hotbarComponent =
            new HootBarComponent("Hotbar", 116.5f, 265.0f, 960.0f, 495.5f, 0.0f, -16.5f, DraggableHudElement.Align.BOTTOM_CENTER);
    private final ScoreBoardComponent scoreBoardComponent =
            new ScoreBoardComponent("Scoreboard", 0.0f, 0.0f, 960.0f, 495.5f, -10.0f, 10.0f, DraggableHudElement.Align.CENTER_RIGHT);
    private final PlayerListComponent playerListComponent =
            new PlayerListComponent("Tab");

    private DraggableHudElement draggingElement;
    private float dragOffsetX;
    private float dragOffsetY;

    private Interface() {
        Collections.addAll(
                elements,
                watermarkComponent,
                keybindsComponent,
                totemCounter,
                fireworkCounter,
                armorPreview,
                notifyComponent,
                staffComponent,
                potionsComponent,
                targetHudComponent,
                informationComponent,
                timerIndicator,
                eventSchedule,
                cooldownComponent,
                dynamicIsland,
                inventoryComponent,
                musicInfoComponent,
                hotbarComponent,
                scoreBoardComponent,
                playerListComponent
        );

        // Mind-like defaults.
        elementsSetting.get(10).setEnabled(false);
        elementsSetting.get(14).setEnabled(false);
        elementsSetting.get(15).setEnabled(false);
        elementsSetting.get(16).setEnabled(false);
        elementsSetting.get(17).setEnabled(false);
        elementsSetting.get(18).setEnabled(false);

        Zenith.getInstance().getNotifyManager().setNotifyComponent(notifyComponent);
    }

    @Override
    public JsonObject save() {
        JsonObject object = super.save();
        JsonObject elementsObject = new JsonObject();

        for (DraggableHudElement element : elements) {
            elementsObject.add(element.getName(), element.save());
        }

        object.add("HudElements", elementsObject);
        return object;
    }

    @Override
    public void load(JsonObject object) {
        super.load(object);

        if (!object.has("HudElements") || !object.get("HudElements").isJsonObject()) {
            return;
        }

        JsonObject elementsObject = object.getAsJsonObject("HudElements");
        for (DraggableHudElement element : elements) {
            if (elementsObject.has(element.getName()) && elementsObject.get(element.getName()).isJsonObject()) {
                element.load(elementsObject.getAsJsonObject(element.getName()));
            }
        }
    }

    @EventTarget
    public void onRender(EventHudRender event) {
        if (!(mc.currentScreen instanceof ChatScreen) && draggingElement != null) {
            draggingElement.release();
            draggingElement = null;
        }

        if (mc.options.hudHidden) {
            return;
        }

        CustomDrawContext ctx = event.getContext();
        float width = mc.getWindow().getWidth() / getCustomScale();
        float height = mc.getWindow().getHeight() / getCustomScale();

        for (DraggableHudElement element : elements) {
            if (!shouldRender(element)) {
                continue;
            }

            try {
                element.render(ctx);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        if (mc.currentScreen instanceof ChatScreen && draggingElement != null) {
            Vector2f mousePos = GuiUtil.getMouse(getCustomScale());
            draggingElement.set(ctx, mousePos.getX() - dragOffsetX, mousePos.getY() - dragOffsetY, this, width, height);
        }
    }

    @EventTarget
    public void onMouse(EventMouse event) {
        if (!(mc.currentScreen instanceof ChatScreen)) {
            if (draggingElement != null) {
                draggingElement.release();
            }
            draggingElement = null;
            return;
        }

        Vector2f mousePos = GuiUtil.getMouse(getCustomScale());
        double mouseX = mousePos.getX();
        double mouseY = mousePos.getY();

        if (event.getAction() == 1 && event.getButton() == 0) {
            List<DraggableHudElement> reversed = new ArrayList<>(elements);
            Collections.reverse(reversed);

            for (DraggableHudElement element : reversed) {
                if (!shouldRender(element) || !element.isMouseOver(mouseX, mouseY)) {
                    continue;
                }

                draggingElement = element;
                dragOffsetX = (float) mouseX - element.getX();
                dragOffsetY = (float) mouseY - element.getY();
                break;
            }
        } else if (event.getAction() == 0) {
            if (draggingElement != null) {
                draggingElement.release();
            }
            draggingElement = null;
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (glowCache.size() > 400) {
            glowCache.values().removeIf(value -> {
                if (value.tick()) {
                    value.destroy();
                    return true;
                }
                return false;
            });
        }

        for (DraggableHudElement element : elements) {
            element.tick();
        }
    }

    @EventTarget
    public void onResize(EventWindowResize event) {
        float width = mc.getWindow().getWidth() / getCustomScale();
        float height = mc.getWindow().getHeight() / getCustomScale();

        for (DraggableHudElement element : elements) {
            element.windowResized(width, height);
        }
    }

    @EventTarget
    public void onScreen(EventSetScreen event) {
        if (!(event.getScreen() instanceof ChatScreen) && draggingElement != null) {
            draggingElement.release();
            draggingElement = null;
        }
    }

    @EventTarget
    public void onModuleToggle(EventModuleToggle event) {
        if (dynamicIsland != null && elementsSetting.isEnable(13) && !"Menu".equals(event.getModule().getName())) {
            dynamicIsland.showModuleNotification(event.getModule(), event.isEnabled());
        }
    }

    private boolean shouldRender(DraggableHudElement element) {
        int index = elements.indexOf(element);
        return index >= 0 && elementsSetting.isEnable(index);
    }

    public float getCustomScale() {
        return scale.getCurrent();
    }

    public org.joml.Vector2f getNearest(float x, float y) {
        float minDeltaX = Float.MAX_VALUE;
        float minDeltaY = Float.MAX_VALUE;
        float thoroughness = 2f;
        org.joml.Vector2f nearest = new org.joml.Vector2f(-1, -1);

        for (DraggableHudElement element : elements) {
            if (element == draggingElement) {
                continue;
            }

            float tempXA = element.getX();
            float tempYA = element.getY();
            float tempXB = element.getX() + element.getWidth();
            float tempYB = element.getY() + element.getHeight();
            float tempXC = element.getX() + element.getWidth() / 2f;
            float tempYC = element.getY() + element.getHeight() / 2f;

            float minX = getNearest(tempXA, tempXB, tempXC, x);
            float minY = getNearest(tempYA, tempYB, tempYC, y);
            float deltaX = Math.abs(minX - x);
            float deltaY = Math.abs(minY - y);

            if (deltaX < minDeltaX) {
                minDeltaX = deltaX;
                if (minDeltaX < thoroughness) {
                    nearest.x = minX;
                }
            }

            if (deltaY < minDeltaY) {
                minDeltaY = deltaY;
                if (minDeltaY < thoroughness) {
                    nearest.y = minY;
                }
            }
        }

        if (nearest.x == -1 || nearest.y == -1) {
            float centerX = mc.getWindow().getScaledWidth() / 2f;
            float centerY = mc.getWindow().getScaledHeight() / 2f;

            float minX = getNearest(centerX, centerX, centerX, x);
            float minY = getNearest(centerY, centerY, centerY, y);
            float deltaX = Math.abs(minX - x);
            float deltaY = Math.abs(minY - y);

            if (deltaX < minDeltaX && deltaX < thoroughness) {
                nearest.x = minX;
            }
            if (deltaY < minDeltaY && deltaY < thoroughness) {
                nearest.y = minY;
            }
        }

        return nearest;
    }

    private float getNearest(float a, float b, float c, float target) {
        float nearest = a;
        if (Math.abs(b - target) < Math.abs(nearest - target)) {
            nearest = b;
        }
        if (Math.abs(c - target) < Math.abs(nearest - target)) {
            nearest = c;
        }
        return nearest;
    }

    public boolean isBlur() {
        return blur.isEnabled();
    }

    public boolean isGlow() {
        return glow.isEnabled();
    }

    public boolean isCorners() {
        return corners.isEnabled();
    }

    public boolean isNursultanTargetHud() {
        return watermarkStyle.is("New");
    }

    public boolean isNursultanWatermark() {
        return watermarkStyle.is("New");
    }

    public boolean isEnableScoreBar() {
        return elementsSetting.isEnable(17);
    }

    public boolean isEnableHotBar() {
        return elementsSetting.isEnable(16);
    }

    public boolean isEnableTab() {
        return elementsSetting.isEnable(18);
    }

    public DynamicIslandComponent getDynamicIsland() {
        return dynamicIsland;
    }

    public boolean isDynamicIslandEnabled() {
        return elementsSetting.isEnable(13);
    }

    public int getGlowRadius() {
        return 10;
    }

    public static float getBossBarShift() {
        Interface iface = Interface.INSTANCE;
        if (iface == null || !iface.isEnabled() || !iface.isDynamicIslandEnabled()) {
            return 0f;
        }

        DynamicIslandComponent island = iface.getDynamicIsland();
        if (island == null) {
            return 0f;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return 0f;
        }

        float screenHeight = mc.getWindow().getScaledHeight();
        if (island.getY() + island.getHeight() > screenHeight / 4f) {
            return 0f;
        }

        return island.getY() + island.getHeight() + 4f;
    }

    private abstract static class MindCardElement extends DraggableHudElement {
        protected MindCardElement(String name, float initialX, float initialY, float offsetX, float offsetY, Align align) {
            super(name, initialX, initialY, 960.0f, 495.5f, offsetX, offsetY, align);
        }

        protected void drawCard(CustomDrawContext ctx, float x, float y, float width, float height) {
            Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
            DrawUtil.drawBlurHud(ctx.getMatrices(), x, y, width, height, 21, BorderRadius.all(4), ColorRGBA.WHITE);
            ctx.drawRoundedRect(x, y, width, height, BorderRadius.all(4), theme.getForegroundColor());
            ctx.drawRoundedBorder(x, y, width, height, 0.1f, BorderRadius.all(4), theme.getForegroundStroke());
            DrawUtil.drawRoundedCorner(ctx.getMatrices(), x, y, width, height, 0.1f, 12f, theme.getColor(), BorderRadius.all(4));
        }
    }

    private static final class ItemCounterElement extends MindCardElement {
        private final Item item;
        private final String title;

        private ItemCounterElement(String name, Item item, String title, float offsetX, float offsetY, Align align) {
            super(name, 0.0f, 0.0f, offsetX, offsetY, align);
            this.item = item;
            this.title = title;
        }

        @Override
        public void render(CustomDrawContext ctx) {
            Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
            Font font = Fonts.MEDIUM.getFont(6);
            int count = countItems(item);
            String value = String.valueOf(count);

            float cardWidth = Math.max(34f, 24f + font.width(value));
            float cardHeight = 16f;
            this.width = cardWidth;
            this.height = cardHeight;

            drawCard(ctx, x, y, cardWidth, cardHeight);
            ctx.pushMatrix();
            ctx.getMatrices().translate(x + 4f, y + 2f, 0f);
            ctx.getMatrices().scale(0.5f, 0.5f, 1f);
            ctx.drawItem(item.getDefaultStack(), 0, 0);
            ctx.popMatrix();

            ctx.drawText(font, value, x + 14f, y + (cardHeight - font.height()) / 2f, theme.getWhite());
        }

        private int countItems(Item item) {
            if (mc.player == null) {
                return 0;
            }

            int count = 0;
            for (ItemStack stack : mc.player.getInventory().main) {
                if (stack.isOf(item)) {
                    count += stack.getCount();
                }
            }
            for (ItemStack stack : mc.player.getInventory().offHand) {
                if (stack.isOf(item)) {
                    count += stack.getCount();
                }
            }
            return count;
        }
    }

    private static final class ArmorPreviewElement extends MindCardElement {
        private ArmorPreviewElement(String name, float initialX, float initialY, float offsetX, float offsetY, Align align) {
            super(name, initialX, initialY, offsetX, offsetY, align);
        }

        @Override
        public void render(CustomDrawContext ctx) {
            if (mc.player == null) {
                return;
            }

            List<ItemStack> armor = List.of(
                    mc.player.getEquippedStack(EquipmentSlot.HEAD),
                    mc.player.getEquippedStack(EquipmentSlot.CHEST),
                    mc.player.getEquippedStack(EquipmentSlot.LEGS),
                    mc.player.getEquippedStack(EquipmentSlot.FEET)
            );

            boolean empty = armor.stream().allMatch(ItemStack::isEmpty);
            float itemWidth = empty ? 64f : 12f + armor.stream().filter(stack -> !stack.isEmpty()).count() * 16f;
            this.width = itemWidth;
            this.height = 18f;

            drawCard(ctx, x, y, width, height);

            if (empty) {
                Font font = Fonts.MEDIUM.getFont(6);
                Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
                ctx.drawText(font, "Armor", x + 6f, y + (height - font.height()) / 2f, theme.getWhiteGray());
                return;
            }

            float drawX = x + 4f;
            for (ItemStack stack : armor) {
                if (stack.isEmpty()) {
                    continue;
                }
                ctx.pushMatrix();
                ctx.getMatrices().translate(drawX, y + 1f, 0f);
                ctx.getMatrices().scale(0.75f, 0.75f, 1f);
                ctx.drawItem(stack, 0, 0);
                ctx.popMatrix();
                drawX += 16f;
            }
        }
    }

    private static final class TimerIndicatorElement extends MindCardElement {
        private float progress = 0f;

        private TimerIndicatorElement(String name, float initialX, float initialY, float offsetX, float offsetY, Align align) {
            super(name, initialX, initialY, offsetX, offsetY, align);
        }

        @Override
        public void render(CustomDrawContext ctx) {
            Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
            Font font = Fonts.MEDIUM.getFont(6);

            float targetProgress = mc.player == null ? 0f : mc.player.getAttackCooldownProgress(0f);
            progress = MathHelper.lerp(0.15f, progress, targetProgress);

            this.width = 74f;
            this.height = 13f;

            drawCard(ctx, x, y, width, height);
            ctx.drawRoundedRect(x + 4f, y + 4.5f, width - 24f, 3f, BorderRadius.all(1.5f), new ColorRGBA(255, 255, 255, 42));
            ctx.drawRoundedRect(x + 4f, y + 4.5f, (width - 24f) * progress, 3f, BorderRadius.all(1.5f), theme.getColor());

            String percent = (int) (progress * 100f) + "%";
            ctx.drawText(font, percent, x + width - 4f - font.width(percent), y + (height - font.height()) / 2f, theme.getWhite());
        }
    }

    private static final class EventScheduleElement extends MindCardElement {
        private EventScheduleElement(String name, float initialX, float initialY, float offsetX, float offsetY, Align align) {
            super(name, initialX, initialY, offsetX, offsetY, align);
        }

        @Override
        public void render(CustomDrawContext ctx) {
            Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
            Font titleFont = Fonts.MEDIUM.getFont(6.5f);
            Font rowFont = Fonts.MEDIUM.getFont(6f);

            this.width = 96f;
            this.height = 46f;

            drawCard(ctx, x, y, width, height);
            ctx.drawText(titleFont, "Events", x + 6f, y + 5f, theme.getWhite());

            drawRow(ctx, rowFont, "AirDrop", nextAirdrop(), 20f);
            drawRow(ctx, rowFont, "Mascot", nextMascot(), 29f);
            drawRow(ctx, rowFont, "Chest", nextChest(), 38f);
        }

        private void drawRow(CustomDrawContext ctx, Font font, String title, String value, float yOffset) {
            Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
            ctx.drawText(font, title, x + 6f, y + yOffset, theme.getWhite());
            ctx.drawText(font, value, x + width - 6f - font.width(value), y + yOffset, theme.getColor());
        }

        private String nextAirdrop() {
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
            List<LocalDateTime> schedule = List.of(
                    now.withHour(9).withMinute(0).withSecond(0),
                    now.withHour(11).withMinute(0).withSecond(0),
                    now.withHour(13).withMinute(0).withSecond(0),
                    now.withHour(15).withMinute(0).withSecond(0),
                    now.withHour(17).withMinute(0).withSecond(0),
                    now.withHour(19).withMinute(0).withSecond(0),
                    now.withHour(21).withMinute(0).withSecond(0),
                    now.withHour(23).withMinute(0).withSecond(0)
            );

            LocalDateTime next = schedule.stream().filter(time -> time.isAfter(now)).findFirst().orElse(now.withHour(9).withMinute(0).plusDays(1));
            return formatCountdown(now, next);
        }

        private String nextMascot() {
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
            LocalDateTime next = now.withHour(15).withMinute(30).withSecond(0);
            if (!next.isAfter(now)) {
                next = next.plusDays(1);
            }
            return formatCountdown(now, next);
        }

        private String nextChest() {
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
            LocalDateTime next = now.withHour((now.getHour() / 6) * 6).withMinute(0).withSecond(0);
            if (!next.isAfter(now)) {
                next = next.plusHours(6);
            }
            return formatCountdown(now, next);
        }

        private String formatCountdown(LocalDateTime now, LocalDateTime target) {
            long hours = ChronoUnit.HOURS.between(now, target);
            long minutes = ChronoUnit.MINUTES.between(now, target) % 60;
            long seconds = ChronoUnit.SECONDS.between(now, target) % 60;

            if (hours > 0) {
                return String.format(Locale.US, "%dh %dm %ds", hours, minutes, seconds);
            }
            if (minutes > 0) {
                return String.format(Locale.US, "%dm %ds", minutes, seconds);
            }
            return String.format(Locale.US, "%ds", seconds);
        }
    }
}
