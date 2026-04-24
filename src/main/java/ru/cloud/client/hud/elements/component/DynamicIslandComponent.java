package ru.cloud.client.hud.elements.component;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import ru.cloud.Zenith;
import ru.cloud.base.animations.base.Animation;
import ru.cloud.base.animations.base.Easing;
import ru.cloud.base.font.Font;
import ru.cloud.base.font.Fonts;
import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import ru.cloud.base.events.impl.other.EventModuleToggle;
import ru.cloud.client.hud.elements.draggable.DraggableHudElement;
import ru.cloud.client.modules.api.Module;
import ru.cloud.utility.render.display.Render2DUtil;
import ru.cloud.utility.render.display.Texture;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.CustomDrawContext;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.render.display.shader.DrawUtil;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DynamicIslandComponent extends DraggableHudElement {

    private static final long  DISPLAY_MS    = 2000L;
    private static final float FONT_SIZE     = 7f;
    private static final float PILL_H        = 16f;   
    private static final float COVER_SIZE    = 11f;   
    private static final float RADIUS        = 7f;
    private static final ColorRGBA BG_COLOR  = new ColorRGBA(10, 10, 10, 230); 

    private static final Color COLOR_GREEN   = new Color(0xFF46FF00, true);
    private static final Color COLOR_RED     = new Color(0xFFFF4242, true);
    private static final Color COLOR_DEFAULT = new Color(0xFFA442FF, true);

    private static volatile String           currentLabel            = "Dantes Client";
    private static volatile Color            currentStatusColor      = COLOR_DEFAULT;
    private static volatile NotificationType currentNotificationType = NotificationType.DEFAULT;
    private static volatile long             resetAt                 = 0L;

    public enum NotificationType {
        SUCCESS(new Color(0xFF46FF00, true)),
        ERROR  (new Color(0xFFFF4242, true)),
        WARNING(new Color(0xFFFFAA00, true)),
        INFO   (new Color(0xFF42A5FF, true)),
        DEFAULT(new Color(0xFFA442FF, true));
        public final Color color;
        NotificationType(Color color) { this.color = color; }
    }

    public static void addNotification(String text, NotificationType type) {
        currentLabel            = text;
        currentNotificationType = type;
        currentStatusColor      = type.color;
        resetAt                 = System.currentTimeMillis() + DISPLAY_MS;
    }

    public static void notifyModuleToggle(String moduleName, boolean enabled) {
        addNotification("\u041c\u043e\u0434\u0443\u043b\u044c " + moduleName + " " + (enabled ? "\u0432\u043a\u043b\u044e\u0447\u0435\u043d" : "\u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d"),
                enabled ? NotificationType.SUCCESS : NotificationType.ERROR);
    }

    public static void strengthNotification() {
        if (mc.player == null) return;
        ItemStack[] slots  = {
                mc.player.getEquippedStack(EquipmentSlot.FEET),
                mc.player.getEquippedStack(EquipmentSlot.LEGS),
                mc.player.getEquippedStack(EquipmentSlot.CHEST),
                mc.player.getEquippedStack(EquipmentSlot.HEAD)
        };
        String[] names = {"\u0411\u043e\u0442\u0438\u043d\u043a\u0438", "\u041f\u043e\u043d\u043e\u0436\u0438", "\u041d\u0430\u0433\u0440\u0443\u0434\u043d\u0438\u043a", "\u0428\u043b\u0435\u043c"};
        for (int i = 0; i < slots.length; i++) {
            ItemStack a = slots[i];
            if (a.isEmpty() || !a.isDamageable()) continue;
            float pct = (float)(a.getMaxDamage() - a.getDamage()) / a.getMaxDamage();
            if (pct <= 0.1f && pct > 0) {
                addNotification(names[i] + " \u0438\u043c\u0435\u0435\u0442 \u043d\u0438\u0437\u043a\u0443\u044e \u043f\u0440\u043e\u0447\u043d\u043e\u0441\u0442\u044c!", NotificationType.WARNING);
                return;
            }
        }
    }

    @Deprecated
    public void showModuleNotification(Module module, boolean enabled) {
        notifyModuleToggle(module.getName(), enabled);
    }

    private final Animation widthAnim    = new Animation(400, 60f, Easing.CUBIC_OUT);
    private final Animation heightAnim   = new Animation(350, PILL_H, Easing.CUBIC_OUT);
    private final Animation scaleAnim    = new Animation(350, 0f, Easing.BAKEK_SMALLER);
    private final Animation progressAnim = new Animation(500, 0f, Easing.CUBIC_OUT);

    private final float[] barH   = {4f, 6f, 3f, 7f};
    private final float[] barTgt = {4f, 6f, 3f, 7f};
    private long lastBarUpdate   = 0L;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean   polling  = new AtomicBoolean(false);
    private volatile long lastPollMs       = 0L;

    private String  trackName    = null;
    private String  artistText   = null;
    private float   mediaProgress = 0f;
    private boolean isPlaying    = false;

    private final Identifier artworkId  = Zenith.id("dynamic_island_cover");
    private byte[]            lastArtwork = null;

    private enum ContentType { NOTIFICATION, BOSS_BAR, MEDIA, DEFAULT }

    private static class PillContent {
        final ContentType type;
        final float targetW;
        PillContent(ContentType type, float targetW) {
            this.type = type; this.targetW = targetW;
        }
    }

    public DynamicIslandComponent(String name, float initialX, float initialY,
                                   float windowWidth, float windowHeight,
                                   float offsetX, float offsetY, Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        EventManager.register(this);
    }

    @EventTarget
    public void onModuleToggle(EventModuleToggle event) {
        notifyModuleToggle(event.getModule().getName(), event.isEnabled());
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return false;
    }


    @Override
    public void tick() {
        if (mc.player == null || mc.world == null) return;
        long now = System.currentTimeMillis();

        // Poll media
        if (now - lastPollMs >= 200L && polling.compareAndSet(false, true)) {
            lastPollMs = now;
            executor.execute(this::pollMedia);
        }

        // Animate wave bars
        if (isPlaying && trackName != null) {
            if (now - lastBarUpdate > 130L) {
                for (int i = 0; i < 4; i++) barTgt[i] = 2f + (float)(Math.random() * 8f);
                lastBarUpdate = now;
            }
        } else {
            for (int i = 0; i < 4; i++) barTgt[i] = 2.5f;
        }
        for (int i = 0; i < 4; i++) barH[i] += (barTgt[i] - barH[i]) * 0.2f;

        // Reset notification
        if (resetAt != 0L && now >= resetAt) {
                currentLabel            = "Dantes Client";
            currentStatusColor      = COLOR_DEFAULT;
            currentNotificationType = NotificationType.DEFAULT;
            resetAt                 = 0L;
        }
    }

    private void pollMedia() {
        try {
            java.util.List<IMediaSession> sessions = MediaPlayerInfo.Instance.getMediaSessions();
            if (sessions == null || sessions.isEmpty()) {
                mc.execute(this::clearMedia);
                return;
            }
            IMediaSession session = sessions.stream()
                    .filter(s -> { try { return s != null && s.getMedia() != null; } catch (Throwable t) { return false; } })
                    .max(Comparator.comparing(s -> { try { return s.getMedia().getPlaying(); } catch (Throwable t) { return false; } }))
                    .orElse(null);
            if (session == null) {
                mc.execute(this::clearMedia);
                return;
            }
            MediaInfo info = session.getMedia();
            if (info == null || info.getTitle() == null || info.getTitle().isEmpty()) {
                mc.execute(this::clearMedia);
                return;
            }
            String  nt   = info.getTitle();
            String  na   = (info.getArtist() != null && !info.getArtist().isEmpty()) ? info.getArtist() : null;
            float   np   = info.getDuration() > 0 ? (float) info.getPosition() / info.getDuration() : 0f;
            boolean npl  = info.getPlaying();
            byte[]  nart;
            try { nart = info.getArtworkPng(); } catch (Throwable t) { nart = null; }
            final byte[] finalNart = nart;
            mc.execute(() -> {
                trackName = nt; artistText = na; mediaProgress = np; isPlaying = npl;
                if (finalNart != null && finalNart.length > 0 && !Arrays.equals(finalNart, lastArtwork)) {
                    lastArtwork = finalNart;
                    try {
                        Render2DUtil.registerTexture(new Texture(artworkId), finalNart);
                    } catch (Throwable t) {
                    }
                }
            });
        } catch (Throwable t) {
            mc.execute(this::clearMedia);
        } finally {
            polling.set(false);
        }
    }

    private void clearMedia() {
        trackName = null; artistText = null; mediaProgress = 0f;
        isPlaying = false; lastArtwork = null;
    }


    @Override
    public void render(CustomDrawContext ctx) {
        if (mc.player == null || mc.world == null) return;

        int screenW = mc.getWindow().getScaledWidth();

        // Expire notification
        if (resetAt != 0L && System.currentTimeMillis() >= resetAt) {
                currentLabel            = "Dantes Client";
            currentStatusColor      = COLOR_DEFAULT;
            currentNotificationType = NotificationType.DEFAULT;
            resetAt                 = 0L;
        }

        Font font = Fonts.MEDIUM.getFont(FONT_SIZE);

        boolean showNotif = System.currentTimeMillis() < resetAt;
        boolean hasMedia  = trackName != null;
        ClientBossBar bossBar = (!showNotif && !hasMedia) ? getPvpBossBar() : null;

        // Target pill width
        float targetW;
        if (showNotif) {
            targetW = 9f + font.width(currentLabel) + 14f;
        } else if (hasMedia) {
            float waveW = 4 * 3f + 3 * 1.5f;
            targetW = 6f + COVER_SIZE + 5f + font.width(buildTrackLabel()) + 6f + waveW + 6f;
        } else if (bossBar != null) {
            BossBarInfo info = extractBossBarInfo(bossBar, font);
            targetW = 10f + info.timeWidth + 8f + info.labelWidth + 10f;
        } else {
            targetW = 8f + font.width(currentLabel) + 14f;
        }

        // Animate pill size
        widthAnim.animateTo(targetW);
        heightAnim.animateTo(PILL_H);
        widthAnim.update();
        heightAnim.update();
        progressAnim.update(mediaProgress);

        
        scaleAnim.update(1f);
        float scale = scaleAnim.getValue();
        float alpha = Math.min(1f, scale * 1.5f); 

        float w = widthAnim.getValue();
        float h = heightAnim.getValue();

        // Clock
        Font clockFont  = Fonts.MEDIUM.getFont(FONT_SIZE);
        String clockStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        float clockW    = clockFont.width(clockStr);

        
        int ping = getPlayerPing();
        boolean showPing = !mc.isInSingleplayer() && ping < 999;
        float pingW = showPing ? 4 * 2.5f + 3 * 1.5f : 0f;

        float pillX = this.x;
        float pillY = this.y;
        if (pillX <= 0f && pillY <= 0f) {
            pillX = screenW / 2f - w / 2f;
            pillY = 5f;
        }
        float totalW  = clockW + 4f + w + (showPing ? 4f + pingW : 0f);
        float originX = pillX - 4f - clockW + totalW / 2f;
        float originY = pillY + h / 2f;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(originX, originY, 0f);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.getMatrices().translate(-originX, -originY, 0f);

        float clockX = pillX - clockW - 4f;
        float clockY = pillY + (h - clockFont.height()) / 2f;

        ctx.drawText(clockFont, clockStr, clockX, clockY, ColorRGBA.WHITE.mulAlpha(alpha));

        DrawUtil.drawBlurHud(ctx.getMatrices(), pillX, pillY, w, h, 18, BorderRadius.all(RADIUS), BG_COLOR);
        ctx.drawRoundedRect(pillX, pillY, w, h, BorderRadius.all(RADIUS), BG_COLOR);

        if (showNotif) {
            renderNotification(ctx, font, pillX, pillY, w, h, alpha);
        } else if (hasMedia) {
            renderMedia(ctx, font, pillX, pillY, w, h, alpha);
        } else if (bossBar != null) {
            renderBossBar(ctx, font, bossBar, pillX, pillY, w, h, alpha);
        } else {
            renderDefault(ctx, font, pillX, pillY, w, h, alpha);
        }

        if (showPing) {
            renderPing(ctx, pillX + w + 4f, pillY, h, ping, alpha);
        }

        ctx.getMatrices().pop();

        this.x = pillX; this.y = pillY; this.width = w; this.height = h;
    }


    private void renderNotification(CustomDrawContext ctx, Font font,
                                     float x, float y, float w, float h, float alpha) {
        ColorRGBA boxColor = toColorRGBA(currentStatusColor).mulAlpha(alpha);
        float boxSize = 7f;
        float boxX    = x + 6f;
        float boxY    = y + (h - boxSize) / 2f;
        ctx.drawRoundedRect(boxX, boxY, boxSize, boxSize, BorderRadius.all(3.5f), boxColor);

        ctx.drawText(font, currentLabel,
                boxX + boxSize + 5f,
                y + (h - font.height()) / 2f,
                ColorRGBA.WHITE.mulAlpha(alpha));
    }


    private void renderMedia(CustomDrawContext ctx, Font font,
                              float x, float y, float w, float h, float alpha) {
        float pad = 5f;

        if (lastArtwork != null) {
            DrawUtil.drawRoundedTexture(ctx.getMatrices(), artworkId,
                    x + pad, y + (h - COVER_SIZE) / 2f,
                    COVER_SIZE, COVER_SIZE,
                    BorderRadius.all(3f), ColorRGBA.WHITE.mulAlpha(alpha));
        } else {
            ctx.drawRoundedRect(x + pad, y + (h - COVER_SIZE) / 2f,
                    COVER_SIZE, COVER_SIZE, BorderRadius.all(3f),
                    new ColorRGBA(80, 80, 80, (int)(200 * alpha)));
        }

        float textX = x + pad + COVER_SIZE + 4f;

        String label  = buildTrackLabel();
        float waveW   = 4 * 3f + 3 * 1.5f;
        float maxTextW = w - (textX - x) - waveW - 8f;
        renderScrollingText(ctx, font, label, textX, y + (h - font.height()) / 2f,
                ColorRGBA.WHITE.mulAlpha(alpha), maxTextW);

        float waveStartX = x + w - waveW - 5f;
        float centerY    = y + h / 2f;
        for (int i = 0; i < 4; i++) {
            float bh = barH[i];
            ctx.drawRoundedRect(
                    waveStartX + i * (3f + 1.5f),
                    centerY - bh / 2f,
                    3f, bh,
                    BorderRadius.all(1f),
                    new ColorRGBA(255, 255, 255, (int)(200 * alpha)));
        }
    }


    private void renderBossBar(CustomDrawContext ctx, Font font, ClientBossBar bar,
                                float x, float y, float w, float h, float alpha) {
        BossBarInfo info = extractBossBarInfo(bar, font);

        float timeBoxW = info.timeWidth + 8f;
        float startX   = x + (w - timeBoxW - 4f - info.labelWidth) / 2f;
        float textY    = y + (h - font.height()) / 2f;

        ctx.drawRoundedRect(startX - 2f, y + 2f, timeBoxW, h - 4f,
                BorderRadius.all(3f), new ColorRGBA(200, 50, 50, (int)(200 * alpha)));
        ctx.drawText(font, info.timeString,
                startX + (timeBoxW - info.timeWidth) / 2f - 2f,
                textY, ColorRGBA.WHITE.mulAlpha(alpha));

        ctx.drawText(font, info.labelText,
                startX + timeBoxW + 4f, textY,
                ColorRGBA.WHITE.mulAlpha(alpha));
    }


    private void renderDefault(CustomDrawContext ctx, Font font,
                                float x, float y, float w, float h, float alpha) {
        ColorRGBA dotColor = toColorRGBA(currentStatusColor).mulAlpha(alpha);
        float dotSize = 6f;
        float dotX    = x + 6f;
        float dotY    = y + (h - dotSize) / 2f;
        ctx.drawRoundedRect(dotX, dotY, dotSize, dotSize, BorderRadius.all(dotSize / 2f), dotColor);

        ctx.drawText(font, currentLabel,
                dotX + dotSize + 4f,
                y + (h - font.height()) / 2f,
                ColorRGBA.WHITE.mulAlpha(alpha));
    }


    private void renderPing(CustomDrawContext ctx, float x, float y, float h, int ping, float alpha) {
        int lit = ping <= 60 ? 4 : ping <= 100 ? 3 : ping <= 150 ? 2 : ping <= 250 ? 1 : 0;
        float[] heights = {3f, 5f, 7f, 9f};
        float barW  = 2.5f;
        float gap   = 1.5f;
        float baseY = y + h - 2f; 

        for (int i = 0; i < 4; i++) {
            float bh = heights[i];
            ColorRGBA c = i < lit
                    ? new ColorRGBA(255, 255, 255, (int)(210 * alpha))
                    : new ColorRGBA(255, 255, 255, (int)(45 * alpha));
            DrawUtil.drawRoundedRect(ctx.getMatrices(),
                    x + i * (barW + gap), baseY - bh,
                    barW, bh, BorderRadius.all(0.8f), c);
        }
    }


    private void renderScrollingText(CustomDrawContext ctx, Font font, String text,
                                      float x, float y, ColorRGBA color, float maxW) {
        float textW = font.width(text);
        float scroll = 0f;
        if (textW > maxW) {
            float scrollMax = textW - maxW;
            float pause = 1000f, dur = 3000f, cycle = (pause + dur) * 2;
            float t = System.currentTimeMillis() % (long) cycle;
            if      (t < pause)           scroll = 0f;
            else if (t < pause + dur)     scroll = easeInOut((t - pause) / dur) * scrollMax;
            else if (t < pause * 2 + dur) scroll = scrollMax;
            else                          scroll = (1f - easeInOut((t - pause * 2 - dur) / dur)) * scrollMax;
        }
        ctx.enableScissor((int) x, (int)(y - 1), (int)(x + maxW), (int)(y + font.height() + 2));
        ctx.drawText(font, text, x - scroll, y, color);
        ctx.disableScissor();
    }

    private float easeInOut(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t < 0.5f ? 2 * t * t : (float)(-1 + (4 - 2 * t) * t);
    }


    private ClientBossBar getPrimaryBossBar() {
        InGameHud hud = mc.inGameHud;
        if (hud == null) return null;
        BossBarHud bbh = hud.getBossBarHud();
        if (bbh == null) return null;
        Map<UUID, ClientBossBar> bars = bbh.bossBars;
        return bars.isEmpty() ? null : bars.values().iterator().next();
    }

    private ClientBossBar getPvpBossBar() {
        InGameHud hud = mc.inGameHud;
        if (hud == null) return null;
        BossBarHud bbh = hud.getBossBarHud();
        if (bbh == null) return null;
        for (ClientBossBar bar : bbh.bossBars.values()) {
            String n = bar.getName().getString().toLowerCase();
            if (n.contains("pvp") || n.contains("\u043f\u0432\u043f") || n.contains("\u043a\u0442") || n.contains("kt")
                    || n.contains("\u0442\u0435\u043b\u0435\u043f\u043e\u0440\u0442\u0430\u0446\u0438\u044f") || n.contains("\u0434\u043e \u043a\u043e\u043d\u0446\u0430")) {
                return bar;
            }
        }
        return null;
    }
    private int getPlayerPing() {
        if (mc.player == null || mc.getNetworkHandler() == null) return 0;
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return e != null ? e.getLatency() : 0;
    }

    private static final Pattern TIMER_PATTERN = Pattern.compile("(\\d+)(?!.*\\d)");

    private BossBarInfo extractBossBarInfo(ClientBossBar bar, Font font) {
        String raw   = bar.getName().getString();
        Matcher m    = TIMER_PATTERN.matcher(raw);
        String time  = m.find() ? m.group(1) + "s" : "??s";
        String label = raw.replaceAll(",?\\s*\u0434\u043e \u043a\u043e\u043d\u0446\u0430.*$", "")
                          .replaceAll("\u0422\u0435\u043b\u0435\u043f\u043e\u0440\u0442\u0430\u0446\u0438\u044f.*", "\u0422\u0435\u043b\u0435\u043f\u043e\u0440\u0442\u0430\u0446\u0438\u044f").trim();
        return new BossBarInfo(time, label, font.width(time), font.width(label));
    }

    private String buildTrackLabel() {
        if (artistText != null && !artistText.isEmpty()) return trackName + " \u2014 " + artistText;
        return trackName != null ? trackName : "";
    }

    private ColorRGBA toColorRGBA(Color c) {
        return new ColorRGBA(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }


    private static class BossBarInfo {
        final String timeString, labelText;
        final float  timeWidth,  labelWidth;
        BossBarInfo(String ts, String lt, float tw, float lw) {
            timeString = ts; labelText = lt; timeWidth = tw; labelWidth = lw;
        }
    }
}


