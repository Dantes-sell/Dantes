package ru.cloud.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.Identifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.input.EventMouse;
import ru.cloud.base.events.impl.input.EventSetScreen;
import ru.cloud.base.events.impl.other.EventModuleToggle;
import ru.cloud.base.events.impl.player.EventUpdate;
import ru.cloud.base.events.impl.render.EventHudRender;
import ru.cloud.base.font.Font;
import ru.cloud.base.font.Fonts;
import ru.cloud.base.theme.Theme;
import ru.cloud.client.hud.elements.component.DynamicIslandComponent;
import ru.cloud.client.hud.elements.component.TargetHudComponent;
import ru.cloud.client.hud.elements.draggable.DraggableHudElement;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;
import ru.cloud.utility.game.other.TextUtil;
import ru.cloud.utility.game.server.ServerHandler;
import ru.cloud.utility.render.display.Keyboard;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.CustomDrawContext;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@ModuleAnnotation(name = "HudBeta", category = Category.RENDER, description = "Rockstar HUD")
public final class HudBeta extends Module {

    public static final HudBeta INSTANCE = new HudBeta();

    private final BooleanSetting dynamicIsland = new BooleanSetting("DynamicIsland", true);
    private final BooleanSetting targetHud = new BooleanSetting("TargetHud", true);
    private final BooleanSetting staffList = new BooleanSetting("StaffList", true);
    private final BooleanSetting keybinds = new BooleanSetting("Keybinds", true);
    private final BooleanSetting potions = new BooleanSetting("Potions", true);
    private final BooleanSetting worldInfo = new BooleanSetting("WorldInfo", true);
    private final BooleanSetting playerInfo = new BooleanSetting("PlayerInfo", true);

    private final DynamicIslandComponent dynamicIslandComponent =
            new DynamicIslandComponent("HudBetaIsland", 0f, 5f, 960f, 495.5f, 0f, 0f, DraggableHudElement.Align.TOP_CENTER);
    private final TargetHudComponent targetHudComponent =
            new TargetHudComponent("HudBetaTarget", 392f, 108f, 960f, 495.5f, 0f, 0f, DraggableHudElement.Align.TOP_LEFT);

    private final RockstarKeybindsElement keybindsElement =
            new RockstarKeybindsElement("HudBetaKeybinds", 18f, 10f, 116f, 18f);
    private final RockstarPotionsElement potionsElement =
            new RockstarPotionsElement("HudBetaPotions", 826f, 10f, 116f, 18f, DraggableHudElement.Align.TOP_RIGHT);
    private final RockstarStaffElement staffElement =
            new RockstarStaffElement("HudBetaStaff", 18f, 38f, 116f, 18f);
    private final InlineCard worldCard =
            new InlineCard("HudBetaWorld", 421f, 78f, 118f, 16f, DraggableHudElement.Align.TOP_CENTER);
    private final InlineCard playerCard =
            new InlineCard("HudBetaPlayer", 421f, 98f, 118f, 16f, DraggableHudElement.Align.TOP_CENTER);

    private final List<DraggableHudElement> draggableElements = new ArrayList<>();
    private DraggableHudElement draggingElement;
    private float dragOffsetX;
    private float dragOffsetY;

    private HudBeta() {
        draggableElements.add(dynamicIslandComponent);
        draggableElements.add(keybindsElement);
        draggableElements.add(staffElement);
        draggableElements.add(potionsElement);
        draggableElements.add(targetHudComponent);
        draggableElements.add(worldCard);
        draggableElements.add(playerCard);
    }

    @Override
    public JsonObject save() {
        JsonObject object = super.save();
        JsonObject elementsObject = new JsonObject();
        for (DraggableHudElement element : draggableElements) {
            elementsObject.add(element.getName(), element.save());
        }
        object.add("HudElements", elementsObject);
        return object;
    }

    @Override
    public void load(JsonObject object) {
        super.load(object);
        if (object.has("HudElements") && object.get("HudElements").isJsonObject()) {
            JsonObject elementsObject = object.getAsJsonObject("HudElements");
            for (DraggableHudElement element : draggableElements) {
                if (elementsObject.has(element.getName()) && elementsObject.get(element.getName()).isJsonObject()) {
                    element.load(elementsObject.getAsJsonObject(element.getName()));
                }
            }
        }
    }

    @EventTarget
    public void onHudRender(EventHudRender event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        CustomDrawContext ctx = event.getContext();
        float width = mc.getWindow().getScaledWidth();
        float height = mc.getWindow().getScaledHeight();

        if (!(mc.currentScreen instanceof ChatScreen) && draggingElement != null) {
            draggingElement.release();
            draggingElement = null;
        }

        if (dynamicIsland.isEnabled()) {
            dynamicIslandComponent.render(ctx);
        }

        if (keybinds.isEnabled()) {
            keybindsElement.render(ctx);
        }

        if (staffList.isEnabled()) {
            staffElement.render(ctx);
        }

        if (potions.isEnabled()) {
            potionsElement.render(ctx);
        }

        if (targetHud.isEnabled()) {
            targetHudComponent.render(ctx);
        }

        if (worldInfo.isEnabled()) {
            worldCard.configure("I", buildWorldText(), 118f, 16f);
            worldCard.render(ctx);
        }

        if (playerInfo.isEnabled()) {
            playerCard.configure("2", buildPlayerText(), 118f, 16f);
            playerCard.render(ctx);
        }

        if (mc.currentScreen instanceof ChatScreen && draggingElement != null) {
            Vector2f mousePos = getScaledMouse();
            draggingElement.set(ctx, mousePos.getX() - dragOffsetX, mousePos.getY() - dragOffsetY, Interface.INSTANCE, width, height);
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

        Vector2f mousePos = getScaledMouse();
        double mouseX = mousePos.getX();
        double mouseY = mousePos.getY();

        if (event.getAction() == 1 && event.getButton() == 0) {
            List<DraggableHudElement> reversed = new ArrayList<>(draggableElements);
            Collections.reverse(reversed);
            for (DraggableHudElement element : reversed) {
                if (!shouldRenderElement(element)) {
                    continue;
                }
                if (mouseX >= element.getX() && mouseX <= element.getX() + element.getWidth()
                        && mouseY >= element.getY() && mouseY <= element.getY() + element.getHeight()) {
                    draggingElement = element;
                    dragOffsetX = (float) mouseX - element.getX();
                    dragOffsetY = (float) mouseY - element.getY();
                    break;
                }
            }
        } else if (event.getAction() == 0) {
            if (draggingElement != null) {
                draggingElement.release();
            }
            draggingElement = null;
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
    public void onUpdate(EventUpdate event) {
        for (DraggableHudElement element : draggableElements) {
            element.tick();
        }
    }

    @EventTarget
    public void onModuleToggle(EventModuleToggle event) {
        if (dynamicIsland.isEnabled() && !event.getModule().getName().equals("Menu")) {
            dynamicIslandComponent.showModuleNotification(event.getModule(), event.isEnabled());
        }
    }

    public static boolean shouldHideBossBar() {
        return INSTANCE.isEnabled() && INSTANCE.dynamicIsland.isEnabled();
    }

    private Vector2f getScaledMouse() {
        float mouseX = (float) (mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth());
        float mouseY = (float) (mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight());
        return new Vector2f(mouseX, mouseY);
    }

    private boolean shouldRenderElement(DraggableHudElement element) {
        if (element == dynamicIslandComponent) return dynamicIsland.isEnabled();
        if (element == targetHudComponent) return targetHud.isEnabled();
        if (element == keybindsElement) return keybinds.isEnabled();
        if (element == potionsElement) return potions.isEnabled();
        if (element == staffElement) return staffList.isEnabled();
        if (element == worldCard) return worldInfo.isEnabled();
        if (element == playerCard) return playerInfo.isEnabled();
        return false;
    }

    private String buildWorldText() {
        ServerHandler serverHandler = Zenith.getInstance().getServerHandler();
        String coords = String.format(Locale.US, "%d %d %d",
                Math.round(mc.player.getX()), Math.round(mc.player.getY()), Math.round(mc.player.getZ()));
        String server = shortServerName(serverHandler.getServer());
        String tps = TextUtil.formatNumber(serverHandler.getTPS()).replace(",", ".").replace(".0", "");
        return coords + " вЂў " + server + " вЂў " + tps + " TPS";
    }

    private String buildPlayerText() {
        double deltaX = mc.player.getX() - mc.player.prevX;
        double deltaZ = mc.player.getZ() - mc.player.prevZ;
        String fps = String.valueOf(mc.getCurrentFps());
        String bps = String.format(Locale.US, "%.2f", Math.hypot(deltaX, deltaZ) * 20.0).replace(",", ".");
        return fps + " FPS вЂў " + bps + " BPS";
    }

    private String shortServerName(String server) {
        return switch (server) {
            case "HolyWorld" -> "HW";
            case "ReallyWorld" -> "RW";
            case "CopyTime" -> "CT";
            case "FunTime" -> "FT";
            default -> "ST";
        };
    }

    private static final class InlineCard extends DraggableHudElement {
        private String icon = "I";
        private String content = "";
        private float cardWidth;
        private float cardHeight;

        private InlineCard(String name, float x, float y, float width, float height, Align align) {
            super(name, x, y, width, height, 0f, 0f, align);
            this.cardWidth = width;
            this.cardHeight = height;
            this.width = width;
            this.height = height;
        }

        public void configure(String icon, String content, float width, float height) {
            this.icon = icon;
            this.content = content;
            this.cardWidth = width;
            this.cardHeight = height;
            this.width = width;
            this.height = height;
        }

        @Override
        public void render(CustomDrawContext ctx) {
            Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
            Font iconFont = Fonts.ICONS.getFont(6f);
            Font font = Fonts.MEDIUM.getFont(6f);

            DrawUtil.drawBlurHud(ctx.getMatrices(), x, y, cardWidth, cardHeight, 18, BorderRadius.all(6), ColorRGBA.WHITE);
            ctx.drawRoundedRect(x, y, cardWidth, cardHeight, BorderRadius.all(6), new ColorRGBA(24, 20, 16, 205));
            ctx.drawRoundedBorder(x, y, cardWidth, cardHeight, 0.1f, BorderRadius.all(6), new ColorRGBA(255, 255, 255, 26));

            float iconBox = 10f;
            float iconX = x + 5f;
            float iconY = y + (cardHeight - iconBox) / 2f;
            ctx.drawRoundedRect(iconX, iconY, iconBox, iconBox, BorderRadius.all(iconBox / 2f),
                    theme.getForegroundLight().withAlpha(150));
            ctx.drawText(iconFont, icon,
                    iconX + (iconBox - iconFont.width(icon)) / 2f,
                    y + (cardHeight - iconFont.height()) / 2f,
                    theme.getWhite());
            ctx.drawText(font, content, iconX + iconBox + 4f, y + (cardHeight - font.height()) / 2f, theme.getWhite());
        }
    }

    private abstract static class RockstarListElement extends DraggableHudElement {
        private final String title;
        private final Identifier icon;

        private RockstarListElement(String name, String title, Identifier icon, float x, float y, float width, float height, Align align) {
            super(name, x, y, width, height, 0f, 0f, align);
            this.title = title;
            this.icon = icon;
            this.width = width;
            this.height = height;
        }

        @Override
        public final void render(CustomDrawContext ctx) {
            List<RowData> rows = buildRows();
            boolean editing = mc.currentScreen instanceof ChatScreen;
            if (rows.isEmpty() && !editing) {
                return;
            }

            Font titleFont = Fonts.REGULAR.getFont(7f);
            Font rowFont = Fonts.REGULAR.getFont(7f);

            this.width = Math.max(92f, computeWidth(rowFont, rows));
            this.height = rows.isEmpty() ? 18f : 18f + 5f + rows.size() * 18f;

            float drawHeight = Math.max(20f, this.height);
            ColorRGBA bg = new ColorRGBA(23, 19, 15, 212);
            ColorRGBA stroke = new ColorRGBA(255, 255, 255, 24);
            ColorRGBA separator = new ColorRGBA(255, 255, 255, 13);
            ColorRGBA textColor = new ColorRGBA(240, 240, 240, 255);

            ctx.drawClientRect(x, y, width, drawHeight, 1.0f, 0.0f, 7.0f);
            ctx.drawRoundedBorder(x, y, width, drawHeight, 0.1f, BorderRadius.all(6), stroke);

            ctx.drawText(titleFont, title, x + 7f, y + (18f - titleFont.height()) / 2f + 0.5f, textColor);
            ctx.drawTexture(icon, x + width - 15f, y + 5f, 8f, 8f, textColor.withAlpha(220));

            if (!rows.isEmpty()) {
                ctx.drawRect(x, y + 18f, width, 4f, separator);
            }

            float rowY = y + 22f;
            for (int i = 0; i < rows.size(); i++) {
                RowData row = rows.get(i);
                if (i > 0) {
                    ctx.drawRect(x, rowY, width, 0.5f, separator);
                }

                float contentY = rowY + 0.5f;
                float textX = x + 7f;
                if (row.effect != null) {
                    ctx.drawSpriteStretched(RenderLayer::getGuiTextured, mc.getStatusEffectSpriteManager().getSprite(row.effect.getEffectType()),
                            (int) (x + 7f), (int) (contentY + 5f), 8, 8);
                    textX += 12f;
                } else if (row.bulletColor != null) {
                    ctx.drawRoundedRect(x + 7f, contentY + 7f, 4f, 4f, BorderRadius.all(2f), row.bulletColor);
                    textX += 9f;
                }

                ctx.drawText(rowFont, row.left, textX, contentY + (18f - rowFont.height()) / 2f, textColor);
                if (!row.right.isEmpty()) {
                    float rightX = x + width - 7f - rowFont.width(row.right);
                    ctx.drawText(rowFont, row.right, rightX, contentY + (18f - rowFont.height()) / 2f, row.rightColor);
                }
                rowY += 18f;
            }
        }

        private float computeWidth(Font rowFont, List<RowData> rows) {
            float maxWidth = width;
            for (RowData row : rows) {
                float iconOffset = row.effect != null ? 12f : row.bulletColor != null ? 9f : 0f;
                float rowWidth = 14f + iconOffset + rowFont.width(row.left) + (row.right.isEmpty() ? 0f : rowFont.width(row.right) + 12f);
                maxWidth = Math.max(maxWidth, rowWidth);
            }
            return maxWidth;
        }

        protected abstract List<RowData> buildRows();
    }

    private static final class RockstarKeybindsElement extends RockstarListElement {
        private RockstarKeybindsElement(String name, float x, float y, float width, float height) {
            super(name, "Клавиши", Zenith.id("icons/hudbeta/keybinds.png"), x, y, width, height, Align.TOP_LEFT);
        }

        @Override
        protected List<RowData> buildRows() {
            List<RowData> rows = new ArrayList<>();
            List<Module> modules = new ArrayList<>(Zenith.getInstance().getModuleManager().getModules());
            modules.removeIf(module -> !module.isEnabled() || module.getKeyCode() == -1);
            modules.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));

            for (Module module : modules) {
                rows.add(new RowData(module.getName(), Keyboard.getKeyName(module.getKeyCode()), null,
                        new ColorRGBA(240, 240, 240, 255), null));
            }
            return rows;
        }
    }

    private static final class RockstarPotionsElement extends RockstarListElement {
        private RockstarPotionsElement(String name, float x, float y, float width, float height, Align align) {
            super(name, "Potions", Zenith.id("icons/hudbeta/potion.png"), x, y, width, height, align);
        }

        @Override
        protected List<RowData> buildRows() {
            if (mc.player == null) {
                return List.of();
            }

            Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
            List<RowData> rows = new ArrayList<>();
            for (StatusEffectInstance effect : effects) {
                StatusEffect statusEffect = effect.getEffectType().value();
                String left = I18n.translate(statusEffect.getTranslationKey()) + " " + (effect.getAmplifier() > 0 ? effect.getAmplifier() + 1 : "");
                String right = formatDuration(effect.getDuration());
                rows.add(new RowData(left.trim(), right, effect, Zenith.getInstance().getThemeManager().getCurrentTheme().getColor(), null));
            }
            return rows;
        }

        private String formatDuration(int durationTicks) {
            int totalSeconds = durationTicks / 20;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    private static final class RockstarStaffElement extends RockstarListElement {
        private static final Set<String> STAFF_PREFIXES = Collections.unmodifiableSet(new LinkedHashSet<>(List.of(
                "helper", "moder", "staff", "admin", "curator"
        )));

        private RockstarStaffElement(String name, float x, float y, float width, float height) {
            super(name, "Staff", Zenith.id("icons/hudbeta/player.png"), x, y, width, height, Align.TOP_LEFT);
        }

        @Override
        protected List<RowData> buildRows() {
            if (mc.getNetworkHandler() == null) {
                return List.of();
            }

            List<RowData> rows = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                if (entry.getProfile() == null) {
                    continue;
                }

                String name = entry.getProfile().getName();
                Text display = entry.getDisplayName();
                String displayText = display == null ? name : display.getString();
                String lower = displayText.toLowerCase(Locale.ROOT);
                boolean manualStaff = Zenith.getInstance().getStaffManager().isStaff(name);
                boolean prefixed = STAFF_PREFIXES.stream().anyMatch(lower::contains);
                if (!manualStaff && !prefixed) {
                    continue;
                }

                if (!seen.add(name)) {
                    continue;
                }

                String status = entry.getGameMode() == GameMode.SPECTATOR ? "VANISH" : "ONLINE";
                ColorRGBA color = entry.getGameMode() == GameMode.SPECTATOR
                        ? new ColorRGBA(255, 170, 90, 255)
                        : Zenith.getInstance().getThemeManager().getCurrentTheme().getColor();
                rows.add(new RowData(name, status, null, color,
                        entry.getGameMode() == GameMode.SPECTATOR ? new ColorRGBA(255, 170, 90, 255) : new ColorRGBA(90, 255, 140, 255)));
            }

            rows.sort(Comparator.comparing(row -> row.left.toLowerCase(Locale.ROOT)));
            return rows;
        }
    }

    private record RowData(String left, String right, StatusEffectInstance effect, ColorRGBA rightColor, ColorRGBA bulletColor) {
        private RowData {
            if (right == null) {
                right = "";
            }
            if (rightColor == null) {
                rightColor = new ColorRGBA(240, 240, 240, 255);
            }
        }
    }
}
