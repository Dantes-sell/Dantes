package ru.cloud.client.screens.loading;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.cloud.base.font.Font;
import ru.cloud.base.font.Fonts;
import ru.cloud.client.screens.mainmenu.MainMenuScreen;
import ru.cloud.utility.interfaces.IMinecraft;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.UIContext;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("All")
public class LoadingScreen extends Screen implements IMinecraft {

    private static final ColorRGBA ACCENT      = new ColorRGBA(120,  60, 200, 255);
    private static final ColorRGBA ACCENT_LITE = new ColorRGBA(160, 100, 255, 255);
    private static final ColorRGBA TEXT_MAIN   = new ColorRGBA(220, 200, 255, 255);
    private static final ColorRGBA TEXT_DIM    = new ColorRGBA(130, 100, 180, 255);
    private static final ColorRGBA TEXT_OK     = new ColorRGBA( 80, 220, 130, 255);
    private static final ColorRGBA TEXT_WARN   = new ColorRGBA(220, 180,  60, 255);
    private static final ColorRGBA PARTICLE_C  = new ColorRGBA(160, 100, 255, 180);

    private static final String CLIENT_NAME = "Dantes Client";
    private static final String VERSION     = "v1.0 BETA";

    // -- stage definitions -----------------------------------------------------
    // Each stage: { label, duration_ms, log lines... }
    private static final Object[][] STAGE_DEF = {
        { "Bootstrapping JVM environment",        350,
            "Allocating native heap...",
            "JIT compiler: tiered compilation ON",
            "Unsafe memory access: enabled" },
        { "Initializing Fabric loader",           400,
            "Fabric API 0.119.3 detected",
            "Mixin subsystem: ready",
            "Entrypoints resolved: 3" },
        { "Loading core systems",                 500,
            "EventManager registered",
            "ThemeManager: DARK theme loaded",
            "FriendManager: 0 entries",
            "StaffManager: initialized" },
        { "Patching Minecraft internals",         600,
            "Applying 47 mixins...",
            "ClientPlayerEntity: patched",
            "MinecraftClient: patched",
            "InGameHud: patched",
            "Render pipeline: hooked" },
        { "Loading module registry",              700,
            "Combat modules: 10 loaded",
            "Movement modules: 8 loaded",
            "Render modules: 15 loaded",
            "Player modules: 7 loaded",
            "Misc modules: 16 loaded",
            "Total: 56 modules registered" },
        { "Compiling MSDF font shaders",          550,
            "Font BOLD: compiled",
            "Font MEDIUM: compiled",
            "Font SEMIBOLD: compiled",
            "Font ROUND_BOLD: compiled",
            "Icons font: compiled" },
        { "Initializing render pipeline",         650,
            "RoundedRect shader: OK",
            "Border shader: OK",
            "Blur shader: OK",
            "Gradient shader: OK",
            "Texture shader: OK",
            "MSDF font shader: OK" },
        { "Loading rotation engine",              450,
            "AimManager: initialized",
            "RotationManager: registered",
            "Prediction model: loaded",
            "Smoothing profiles: 3" },
        { "Applying configuration",               400,
            "Config directory: found",
            "current_config.dantes: decrypted",
            "Module states: restored",
            "Keybinds: applied",
            "Theme: restored" },
        { "Connecting to services",               600,
            "Discord RPC: connecting...",
            "Discord RPC: connected",
            "Account manager: 0 accounts",
            "Server handler: ready" },
        { "Finalizing startup",                   350,
            "HUD elements: initialized",
            "AutoBuy manager: ready",
            "Script engine: ready",
            "Startup complete." },
    };

    // -- runtime state ---------------------------------------------------------
    private final long startTime = System.currentTimeMillis();

    private int   stageIdx      = 0;   // current stage index
    private int   logLineIdx    = 0;   // next log line within stage
    private long  stageStart    = 0;   // when current stage started
    private long  logLineTimer  = 0;   // when next log line fires

    private float progress      = 0f;  // smooth 0..1
    private float targetProg    = 0f;

    private float fadeOut       = 0f;
    private boolean leaving     = false;
    private boolean allDone     = false;

    // accumulated log (label + lines)
    private final List<LogEntry> log = new ArrayList<>();

    // particles
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    // spinner
    private float spinAngle = 0f;

    public LoadingScreen() {
        super(Text.literal("Loading"));
        stageStart    = System.currentTimeMillis();
        logLineTimer  = System.currentTimeMillis() + 80;
    }

    @Override
    protected void init() {
        particles.clear();
        for (int i = 0; i < 55; i++) particles.add(new Particle());
    }

    // -- tick ------------------------------------------------------------------

    private void updateStages() {
        if (allDone) return;
        long now = System.currentTimeMillis();

        if (stageIdx >= STAGE_DEF.length) {
            allDone = true;
            targetProg = 1f;
            return;
        }

        Object[] stage = STAGE_DEF[stageIdx];
        long stageDur = (long)(int) stage[1];

        // fire log lines evenly across stage duration
        int totalLines = stage.length - 2;  // lines start at index 2
        long lineInterval = totalLines > 0 ? stageDur / (totalLines + 1) : stageDur;

        if (now >= logLineTimer && logLineIdx < totalLines) {
            log.add(new LogEntry((String) stage[2 + logLineIdx], LogType.INFO));
            logLineIdx++;
            logLineTimer = now + lineInterval;
        }

        // advance stage when duration elapsed
        if (now - stageStart >= stageDur) {
            log.add(new LogEntry("? " + stage[0], LogType.OK));
            stageIdx++;
            logLineIdx = 0;
            stageStart = now;
            if (stageIdx < STAGE_DEF.length) {
                long nextDur = (long)(int) STAGE_DEF[stageIdx][1];
                int nextLines = STAGE_DEF[stageIdx].length - 2;
                logLineTimer = now + (nextLines > 0 ? nextDur / (nextLines + 1) : nextDur);
                log.add(new LogEntry("» " + STAGE_DEF[stageIdx][0] + "...", LogType.STAGE));
            }
        }

        targetProg = (float) stageIdx / STAGE_DEF.length
                   + (float)(now - stageStart) / (float)(int) STAGE_DEF[Math.min(stageIdx, STAGE_DEF.length-1)][1]
                   / STAGE_DEF.length;
        targetProg = Math.min(targetProg, 1f);
    }

    // -- render ----------------------------------------------------------------

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        updateStages();

        UIContext ctx = UIContext.of(drawContext, mouseX, mouseY, delta);
        long now = System.currentTimeMillis();

        progress  = lerp(progress, targetProg, 0.04f);
        spinAngle += 4f;

        // leave after done + short pause
        if (allDone && progress > 0.995f && !leaving) leaving = true;
        if (leaving) {
            fadeOut = Math.min(1f, fadeOut + 0.018f);
            if (fadeOut >= 1f) { mc.setScreen(new MainMenuScreen()); return; }
        }

        // -- background --------------------------------------------------------
        float at = (now % 10000L) / 10000f;
        float s1=(float)Math.sin(at*Math.PI*2), s2=(float)Math.sin(at*Math.PI*2+Math.PI/2);
        float c1=(float)Math.cos(at*Math.PI*2), c2=(float)Math.cos(at*Math.PI*2+Math.PI/2);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), -1,-1, width+2, height+2, BorderRadius.all(0),
                new ColorRGBA(clamp(18+(int)(s1*8)), clamp(10+(int)(c1*6)), clamp(35+(int)(s2*12)),255),
                new ColorRGBA(clamp(25+(int)(c2*10)),clamp(12+(int)(s1*8)), clamp(50+(int)(c1*15)),255),
                new ColorRGBA(clamp(15+(int)(c1*8)), clamp( 8+(int)(s2*6)), clamp(40+(int)(s1*12)),255),
                new ColorRGBA(clamp(22+(int)(s2*10)),clamp(10+(int)(c2*8)), clamp(45+(int)(c1*12)),255));
        DrawUtil.drawRoundedRect(ctx.getMatrices(), -1,-1, width+2, height+2, BorderRadius.all(0),
                new ColorRGBA(0,0,0,165));

        for (Particle p : particles) { p.update(); p.render(ctx); }

        Font bold  = Fonts.BOLD.getFont(22);
        Font med   = Fonts.MEDIUM.getFont(8);
        Font small = Fonts.MEDIUM.getFont(7);
        Font tiny  = Fonts.MEDIUM.getFont(6);

        // -- title -------------------------------------------------------------
        float ta = (now % 4000L) / 1500f;
        float tw = bold.width(CLIENT_NAME);
        float tx = (width - tw) / 2f;
        float ty = height / 2f - 95;
        ctx.drawText(bold, CLIENT_NAME, tx+2, ty+2, new ColorRGBA(0,0,0,80));
        ctx.drawText(bold, CLIENT_NAME, tx, ty, new ColorRGBA(animGrad(ACCENT, ACCENT_LITE, ta)));

        // -- current stage label + spinner -------------------------------------
        String stageLabel = stageIdx < STAGE_DEF.length
                ? (String) STAGE_DEF[stageIdx][0]
                : "Done.";
        float stageLabelAlpha = allDone ? 1f : 0.55f + 0.45f*(float)Math.abs(Math.sin(now*0.0025));

        // spinner dots
        float spinCx = (width - med.width(stageLabel)) / 2f - 14;
        float spinCy = height / 2f - 22;
        int dotCount = 8;
        for (int d = 0; d < dotCount; d++) {
            float angle = (float)(d * Math.PI * 2 / dotCount) + spinAngle * 0.06f;
            float dx = spinCx + (float)Math.cos(angle) * 5;
            float dy = spinCy + (float)Math.sin(angle) * 5;
            float dotAlpha = (float)(d + 1) / dotCount;
            DrawUtil.drawRoundedRect(ctx.getMatrices(), dx-1, dy-1, 2.5f, 2.5f, BorderRadius.all(1.25f),
                    ACCENT_LITE.withAlpha((int)(dotAlpha * 200 * (allDone ? 0 : 1))));
        }

        ctx.drawText(med, stageLabel,
                (width - med.width(stageLabel)) / 2f,
                height / 2f - 28,
                TEXT_DIM.withAlpha((int)(220 * stageLabelAlpha)));

        // -- progress bar ------------------------------------------------------
        float barW = Math.min(480, width * 0.6f);
        float barH = 6f;
        float barX = (width - barW) / 2f;
        float barY = height / 2f - 10;

        // track bg
        DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, barW, barH, BorderRadius.all(barH/2),
                new ColorRGBA(25, 16, 50, 220));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), barX, barY, barW, barH, 0.5f, BorderRadius.all(barH/2),
                ACCENT.withAlpha(50));

        // fill
        float fillW = Math.max(barH, barW * progress);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, fillW, barH, BorderRadius.all(barH/2),
                ACCENT, ACCENT, ACCENT_LITE, ACCENT_LITE);

        // animated shimmer on fill
        float shimmerPos = ((now % 1200L) / 1200f) * (fillW + 30) - 15;
        if (shimmerPos > 0 && shimmerPos < fillW) {
            ctx.enableScissor((int)barX, (int)barY, (int)(barX+fillW), (int)(barY+barH));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), barX+shimmerPos-8, barY-1, 16, barH+2,
                    BorderRadius.all(barH/2), new ColorRGBA(255,255,255,55));
            ctx.disableScissor();
        }

        // glow tip
        if (fillW > barH*2) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), barX+fillW-10, barY-4, 14, barH+8,
                    BorderRadius.all(6), ACCENT_LITE.withAlpha(70));
        }

        // -- percent + stage count ---------------------------------------------
        String pct = (int)(progress * 100) + "%";
        String stageCount = stageIdx + " / " + STAGE_DEF.length;
        ctx.drawText(med, pct,
                (width - med.width(pct)) / 2f,
                barY + barH + 7, TEXT_MAIN);
        ctx.drawText(tiny, stageCount,
                barX + barW - tiny.width(stageCount),
                barY + barH + 7, TEXT_DIM);

        // -- log panel ---------------------------------------------------------
        float logPanelW = Math.min(520, width * 0.65f);
        float logPanelH = 90f;
        float logPanelX = (width - logPanelW) / 2f;
        float logPanelY = barY + barH + 26;

        DrawUtil.drawRoundedRect(ctx.getMatrices(), logPanelX, logPanelY, logPanelW, logPanelH,
                BorderRadius.all(6), new ColorRGBA(14, 9, 28, 180));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), logPanelX, logPanelY, logPanelW, logPanelH,
                0.5f, BorderRadius.all(6), ACCENT.withAlpha(40));

        ctx.enableScissor((int)logPanelX+1, (int)logPanelY+1,
                (int)(logPanelX+logPanelW)-1, (int)(logPanelY+logPanelH)-1);

        float lineH = tiny.height() + 3.5f;
        int maxLines = (int)(logPanelH / lineH);
        int logStart = Math.max(0, log.size() - maxLines);
        float lx = logPanelX + 10;

        for (int i = logStart; i < log.size(); i++) {
            LogEntry entry = log.get(i);
            int age = log.size() - 1 - i;
            float lineAlpha = Math.max(0.15f, 1f - age * 0.12f);
            float ly = logPanelY + 6 + (i - logStart) * lineH;

            ColorRGBA col;
            switch (entry.type) {
                case OK:    col = TEXT_OK;   break;
                case STAGE: col = ACCENT_LITE; break;
                case WARN:  col = TEXT_WARN; break;
                default:    col = TEXT_DIM;  break;
            }
            ctx.drawText(tiny, entry.text, lx, ly, col.withAlpha((int)(255 * lineAlpha)));
        }

        ctx.disableScissor();

        // blinking cursor at bottom of log
        if (!allDone && (now / 500) % 2 == 0) {
            float cursorY = logPanelY + 6 + Math.min(log.size(), maxLines) * lineH;
            if (cursorY < logPanelY + logPanelH - 4)
                ctx.drawText(tiny, "_", lx, cursorY, ACCENT.withAlpha(180));
        }

        // -- version -----------------------------------------------------------
        float vw = tiny.width(VERSION);
        ctx.drawText(tiny, VERSION, width - vw - 10, height - 16, TEXT_DIM);

        // -- fade-out ----------------------------------------------------------
        if (fadeOut > 0) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), 0, 0, width, height, BorderRadius.all(0),
                    new ColorRGBA(0, 0, 0, (int)(255 * easeIn(fadeOut))));
        }
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause()      { return false; }

    // -- helpers ---------------------------------------------------------------

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
    private static float lerp(float a, float b, float t) { return a + (b-a)*t; }
    private static float easeIn(float t) { return t*t; }
    private static ColorRGBA blend(ColorRGBA a, ColorRGBA b, float t) {
        return new ColorRGBA(
                (int)(a.getRed()  +(b.getRed()  -a.getRed())  *t),
                (int)(a.getGreen()+(b.getGreen()-a.getGreen())*t),
                (int)(a.getBlue() +(b.getBlue() -a.getBlue()) *t),
                (int)(a.getAlpha()+(b.getAlpha()-a.getAlpha())*t));
    }
    private static int animGrad(ColorRGBA c1, ColorRGBA c2, float t) {
        float f = t%2f; f = f<1f?f:2f-f; return blend(c1,c2,f).getRGB();
    }

    // -- types -----------------------------------------------------------------

    private enum LogType { INFO, OK, STAGE, WARN }
    private record LogEntry(String text, LogType type) {}

    private class Particle {
        float x, y, vx, vy, size; int alpha;
        Particle() { reset(); y = random.nextFloat() * height; }
        void reset() {
            x = random.nextFloat()*width; y = -10;
            vx = (random.nextFloat()-0.5f)*0.35f; vy = random.nextFloat()*0.35f+0.15f;
            size = random.nextFloat()*2f+0.5f; alpha = random.nextInt(100)+30;
        }
        void update() { x+=vx; y+=vy; if(y>height+10||x<-10||x>width+10) reset(); }
        void render(UIContext ctx) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), (int)x,(int)y,(int)size,(int)size,
                    BorderRadius.all(size/2), PARTICLE_C.withAlpha(alpha));
        }
    }
}
