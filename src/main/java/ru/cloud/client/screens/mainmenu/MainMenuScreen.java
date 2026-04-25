package ru.cloud.client.screens.mainmenu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import ru.cloud.base.font.Font;
import ru.cloud.base.font.Fonts;
import ru.cloud.client.screens.altmanager.AltManagerScreen;
import ru.cloud.utility.interfaces.IMinecraft;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.UIContext;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.render.display.shader.DrawUtil;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("All")
public class MainMenuScreen extends Screen implements IMinecraft {

    private static final String TITLE = "Dantes Client";
    private static final String VERSION = "v1.0 BETA";

    // -- accent palette --------------------------------------------------------
    // primary: purple #9B59B6  secondary: deep-blue #2C3E7A
    private static final ColorRGBA ACCENT1     = new ColorRGBA(120,  60, 200, 255); // violet
    private static final ColorRGBA ACCENT2     = new ColorRGBA( 80,  40, 160, 255); // deep violet
    private static final ColorRGBA GLOW_COLOR  = new ColorRGBA(140,  80, 220, 255); // glow
    private static final ColorRGBA CLOCK_COLOR = new ColorRGBA(190, 160, 255, 255); // soft lavender
    private static final ColorRGBA PARTICLE_C  = new ColorRGBA(160, 100, 255, 180); // particle

    private static final String EASTER_LINE1 = "Добро пожаловать в Dantes Client";
    private static final String EASTER_LINE2 = "Спасибо, что используете наш клиент";
    private static final String EASTER_LINE3 = "Присоединяйтесь к Discord";
    private static final String DISCORD_URL  = "dsc.gg/cloudsosal";
    private static final String DISCORD_LABEL = "  Discord сервер  ";

    private int shakeTime = 0;
    private float shakeOffsetY = 0f;
    private float fadeIn = 0f;

    private int titleClickCount = 0;
    private long lastTitleClickTime = 0L;
    private boolean showEaster = false;
    private float easterAnim = 0f;
    private float discordHover = 0f;

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    private MenuButton singleplayerBtn, multiplayerBtn, altmanagerBtn;
    private SplitButton optionsQuitBtn;

    public MainMenuScreen() {
        super(Text.literal("Main Menu"));
    }

    @Override
    protected void init() {
        fadeIn = 0f;
        particles.clear();
        for (int i = 0; i < 60; i++) particles.add(new Particle());

        int bw = 200, bh = 30;
        singleplayerBtn = new MenuButton("Singleplayer", 0, 0, bw, bh);
        multiplayerBtn  = new MenuButton("Multiplayer",  0, 0, bw, bh);
        altmanagerBtn   = new MenuButton("Alt Manager",   0, 0, bw, bh);
        optionsQuitBtn  = new SplitButton(0, 0, bw, bh, "Options", "Quit");
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        UIContext ctx = UIContext.of(drawContext, mouseX, mouseY, delta);
        if (fadeIn < 1f) fadeIn = Math.min(1f, fadeIn + 0.02f);

        long now = System.currentTimeMillis();
        float animTime = (now % 10000L) / 10000f;
        float sin1 = (float) Math.sin(animTime * Math.PI * 2);
        float sin2 = (float) Math.sin(animTime * Math.PI * 2 + Math.PI / 2);
        float cos1 = (float) Math.cos(animTime * Math.PI * 2);
        float cos2 = (float) Math.cos(animTime * Math.PI * 2 + Math.PI / 2);

        // dark purple-blue animated background
        ColorRGBA c1 = new ColorRGBA(clamp(18+(int)(sin1*8)),  clamp(10+(int)(cos1*6)),  clamp(35+(int)(sin2*12)), 255);
        ColorRGBA c2 = new ColorRGBA(clamp(25+(int)(cos2*10)), clamp(12+(int)(sin1*8)),  clamp(50+(int)(cos1*15)), 255);
        ColorRGBA c3 = new ColorRGBA(clamp(15+(int)(cos1*8)),  clamp( 8+(int)(sin2*6)),  clamp(40+(int)(sin1*12)), 255);
        ColorRGBA c4 = new ColorRGBA(clamp(22+(int)(sin2*10)), clamp(10+(int)(cos2*8)),  clamp(45+(int)(cos1*12)), 255);

        DrawUtil.drawRoundedRect(ctx.getMatrices(), -1, -1, width+2, height+2, BorderRadius.all(0), c1, c2, c3, c4);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), -1, -1, width+2, height+2, BorderRadius.all(0), new ColorRGBA(0,0,0,160));

        for (Particle p : particles) { p.update(); p.render(ctx); }

        if (shakeTime > 0) { shakeTime--; shakeOffsetY = (float)(Math.sin(shakeTime * 0.5) * 3); }
        else shakeOffsetY = 0f;

        Font boldFont = Fonts.BOLD.getFont(20);
        Font medFont  = Fonts.MEDIUM.getFont(9);

        float titleW = boldFont.width(TITLE);
        float titleX = (width - titleW) / 2f;
        float titleBaseY = height / 5f;
        float titleY = titleBaseY + shakeOffsetY;

        ctx.drawText(boldFont, TITLE, titleX + 2, titleY + 2, new ColorRGBA(0,0,0,100));

        float timeAnim = (now % 4000L) / 1500f;
        ctx.drawText(boldFont, TITLE, titleX, titleY, new ColorRGBA(animGradColor(ACCENT1, GLOW_COLOR, timeAnim)));

        LocalTime lt = LocalTime.now();
        String timeStr = String.format("%02d:%02d:%02d", lt.getHour(), lt.getMinute(), lt.getSecond());
        float clockY = titleY + 40;
        ctx.drawText(medFont, timeStr, width/2f - medFont.width(timeStr)/2f + 1, clockY + 1, new ColorRGBA(0,0,0,100));
        ctx.drawText(medFont, timeStr, width/2f - medFont.width(timeStr)/2f, clockY, CLOCK_COLOR);

        float titleH = 54, spacing = 12;
        int bw = 200, bh = 30;
        float buttonsStartY = titleBaseY + titleH + spacing * 2;
        int bx = width/2 - bw/2;

        singleplayerBtn.x = bx; singleplayerBtn.y = (int) buttonsStartY;
        multiplayerBtn.x  = bx; multiplayerBtn.y  = (int)(buttonsStartY + bh + spacing);
        altmanagerBtn.x   = bx; altmanagerBtn.y   = (int)(buttonsStartY + 2*(bh+spacing));
        optionsQuitBtn.x  = bx; optionsQuitBtn.y  = (int)(buttonsStartY + 3*(bh+spacing));
        optionsQuitBtn.width = bw;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, (1 - fadeIn) * 20, 0);
        singleplayerBtn.render(ctx, mouseX, mouseY, fadeIn);
        multiplayerBtn.render(ctx, mouseX, mouseY, fadeIn);
        altmanagerBtn.render(ctx, mouseX, mouseY, fadeIn);
        optionsQuitBtn.render(ctx, mouseX, mouseY, fadeIn);
        ctx.getMatrices().pop();

        Font smallFont = Fonts.MEDIUM.getFont(7);
        float vw = smallFont.width(VERSION);
        ctx.drawText(smallFont, VERSION, width - vw - 10 + 1, height - 20 + 1, new ColorRGBA(0,0,0,150));
        ctx.drawText(smallFont, VERSION, width - vw - 10, height - 20, new ColorRGBA(150,120,220,200));

        // easter egg dialog
        if (showEaster || easterAnim > 0.01f) {
            easterAnim = lerp(easterAnim, showEaster ? 1f : 0f, 0.18f);
            drawEasterDialog(ctx, mouseX, mouseY, easterAnim);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showEaster) {
            // check discord button click
            Font med = Fonts.MEDIUM.getFont(8);
            int bw = 340, bh = 148;
            float bx = (width - bw) / 2f, by = (height - bh) / 2f;
            Font bold = Fonts.BOLD.getFont(9);
            float textY = by + 22, lineH = bold.height() + 5;
            float sepY = textY + lineH*3 + 6;
            int dbW = 170, dbH = 32;
            float dbX = bx + (bw - dbW) / 2f, dbY = sepY + 12;
            if (mouseX >= dbX && mouseX <= dbX + dbW && mouseY >= dbY && mouseY <= dbY + dbH) {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("https://" + DISCORD_URL));
                } catch (Exception ignored) {}
                return true;
            }
            showEaster = false;
            return true;
        }

        Font boldFont = Fonts.BOLD.getFont(20);
        float titleW = boldFont.width(TITLE);
        float titleX = (width - titleW) / 2f;
        float titleY = height / 5f;
        if (mouseX >= titleX && mouseX <= titleX + titleW && mouseY >= titleY && mouseY <= titleY + 54) {
            long now = System.currentTimeMillis();
            if (now - lastTitleClickTime > 1500) titleClickCount = 0;
            lastTitleClickTime = now;
            titleClickCount++;
            if (titleClickCount >= 10) {
                titleClickCount = 0;
                showEaster = true;
                easterAnim = 0f;
            } else {
                shakeTime = 20;
            }
            return true;
        }

        if (singleplayerBtn.isHovered(mouseX, mouseY)) { client.setScreen(new SelectWorldScreen(this)); return true; }
        if (multiplayerBtn.isHovered(mouseX, mouseY))  { client.setScreen(new MultiplayerScreen(this)); return true; }
        if (altmanagerBtn.isHovered(mouseX, mouseY))   { client.setScreen(new AltManagerScreen(this)); return true; }
        if (optionsQuitBtn.isLeftHovered(mouseX, mouseY))  { client.setScreen(new OptionsScreen(this, client.options)); return true; }
        if (optionsQuitBtn.isRightHovered(mouseX, mouseY)) { client.scheduleStop(); return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showEaster && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { showEaster = false; return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    // -- helpers ---------------------------------------------------------------

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private static ColorRGBA blend(ColorRGBA a, ColorRGBA b, float t) {
        return new ColorRGBA(
                (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t),
                (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    private static int animGradColor(ColorRGBA c1, ColorRGBA c2, float t) {
        float cycle = t % 2f; float f = cycle < 1f ? cycle : 2f - cycle;
        return blend(c1, c2, f).getRGB();
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private void drawEasterDialog(UIContext ctx, int mx, int my, float anim) {
        long now = System.currentTimeMillis();

        // backdrop
        DrawUtil.drawRoundedRect(ctx.getMatrices(), 0, 0, width, height, BorderRadius.all(0),
                new ColorRGBA(0, 0, 0, (int)(160 * anim)));

        int bw = 340, bh = 148;
        float bx = (width - bw) / 2f;
        float by = (height - bh) / 2f - (1f - anim) * 18;

        // animated gradient bg
        float at = (now % 10000L) / 10000f;
        float s1=(float)Math.sin(at*Math.PI*2), s2=(float)Math.sin(at*Math.PI*2+Math.PI/2);
        float c1=(float)Math.cos(at*Math.PI*2), c2=(float)Math.cos(at*Math.PI*2+Math.PI/2);
        ColorRGBA bg1 = new ColorRGBA(clamp(22+(int)(s1*8)), clamp(12+(int)(c1*6)), clamp(42+(int)(s2*12)), (int)(255*anim));
        ColorRGBA bg2 = new ColorRGBA(clamp(30+(int)(c2*10)),clamp(14+(int)(s1*8)), clamp(58+(int)(c1*15)),(int)(255*anim));
        ColorRGBA bg3 = new ColorRGBA(clamp(18+(int)(c1*8)), clamp(10+(int)(s2*6)), clamp(48+(int)(s1*12)),(int)(255*anim));
        ColorRGBA bg4 = new ColorRGBA(clamp(26+(int)(s2*10)),clamp(12+(int)(c2*8)), clamp(52+(int)(c1*12)),(int)(255*anim));

        // shadow + outer glow
        DrawUtil.drawShadow(ctx.getMatrices(), bx, by, bw, bh, 18, BorderRadius.all(12),
                ACCENT1.withAlpha((int)(120 * anim)));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), bx-1.5f, by-1.5f, bw+3, bh+3, 1f, BorderRadius.all(13),
                GLOW_COLOR.withAlpha((int)(110 * anim)));

        // panel
        DrawUtil.drawRoundedRect(ctx.getMatrices(), bx, by, bw, bh, BorderRadius.all(12), bg1, bg2, bg3, bg4);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), bx, by, bw, bh, BorderRadius.all(12),
                new ColorRGBA(0, 0, 0, (int)(148 * anim)));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), bx, by, bw, bh, 0.9f, BorderRadius.all(12),
                ACCENT1.withAlpha((int)(150 * anim)));

        // -- text -------------------------------------------------------------
        Font bold = Fonts.BOLD.getFont(9);
        float ta  = (now % 4000L) / 1500f;
        ColorRGBA gradCol = new ColorRGBA(animGradColor(ACCENT1, GLOW_COLOR, ta));

        float textY = by + 22;
        float lineH = bold.height() + 5;
        ctx.drawText(bold, EASTER_LINE1, bx + (bw - bold.width(EASTER_LINE1))/2f, textY,           gradCol.withAlpha((int)(255*anim)));
        ctx.drawText(bold, EASTER_LINE2, bx + (bw - bold.width(EASTER_LINE2))/2f, textY + lineH,   gradCol.withAlpha((int)(235*anim)));
        ctx.drawText(bold, EASTER_LINE3, bx + (bw - bold.width(EASTER_LINE3))/2f, textY + lineH*2, gradCol.withAlpha((int)(210*anim)));

        // separator
        float sepY = textY + lineH*3 + 6;
        DrawUtil.drawRoundedRect(ctx.getMatrices(), bx+24, sepY, bw-48, 0.8f, BorderRadius.all(0.4f),
                ACCENT1.withAlpha((int)(60*anim)));

        // -- Discord button ----------------------------------------------------
        int dbW = 170, dbH = 32;
        float dbX = bx + (bw - dbW) / 2f;
        float dbY = sepY + 12;
        boolean dbHov = mx >= dbX && mx <= dbX+dbW && my >= dbY && my <= dbY+dbH;
        discordHover = lerp(discordHover, dbHov ? 1f : 0f, 0.15f);

        ColorRGBA dBase  = new ColorRGBA(71,  82, 196, 255);
        ColorRGBA dHover = new ColorRGBA(88, 101, 242, 255);
        ColorRGBA dCur   = blend(dBase, dHover, discordHover);

        // drop shadow
        DrawUtil.drawRoundedRect(ctx.getMatrices(), dbX+2, dbY+3, dbW, dbH, BorderRadius.all(9),
                new ColorRGBA(0,0,0,(int)(90*anim)));
        // hover glow
        if (discordHover > 0)
            DrawUtil.drawShadow(ctx.getMatrices(), dbX, dbY, dbW, dbH, 10, BorderRadius.all(9),
                    dHover.withAlpha((int)(discordHover * 140 * anim)));
        // fill
        DrawUtil.drawRoundedRect(ctx.getMatrices(), dbX, dbY, dbW, dbH, BorderRadius.all(9),
                dCur.withAlpha((int)(230*anim)));
        // top gloss strip
        DrawUtil.drawRoundedRect(ctx.getMatrices(), dbX+2, dbY+1, dbW-4, dbH/2f, BorderRadius.all(8),
                new ColorRGBA(255,255,255,(int)(18*anim)));
        // border
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), dbX, dbY, dbW, dbH, 0.8f, BorderRadius.all(9),
                new ColorRGBA(255,255,255,(int)((30 + (int)(discordHover*60))*anim)));

        // -- Discord logo (vectorial) ------------------------------------------
        // body pill
        float lx = dbX + 12, ly = dbY + dbH/2f - 7;
        float lw = 14f, lh = 14f;
        ColorRGBA white = new ColorRGBA(255,255,255,(int)(255*anim));
        DrawUtil.drawSquircle(ctx.getMatrices(), lx, ly, lw, lh, 0.55f, BorderRadius.all(5), white);
        // left ear
        DrawUtil.drawRoundedRect(ctx.getMatrices(), lx + 1f,       ly - 3.5f, 3.5f, 4.5f, BorderRadius.all(2), white);
        // right ear
        DrawUtil.drawRoundedRect(ctx.getMatrices(), lx + lw - 4.5f, ly - 3.5f, 3.5f, 4.5f, BorderRadius.all(2), white);
        // eyes (cutouts in button color)
        ColorRGBA eyeCol = dCur.withAlpha((int)(255*anim));
        DrawUtil.drawRoundedRect(ctx.getMatrices(), lx + 2.5f,      ly + 3.5f, 3f, 3f, BorderRadius.all(1.5f), eyeCol);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), lx + lw - 5.5f, ly + 3.5f, 3f, 3f, BorderRadius.all(1.5f), eyeCol);

        // -- label + url -------------------------------------------------------
        Font med   = Fonts.MEDIUM.getFont(8);
        Font small = Fonts.MEDIUM.getFont(6);
        float labelX = dbX + 12 + lw + 7;
        float totalH = med.height() + 2 + small.height();
        float labelY = dbY + dbH/2f - totalH/2f;
        ctx.drawText(med, "Наш Discord", labelX, labelY,
                new ColorRGBA(255,255,255,(int)(255*anim)));
        ctx.drawText(small, DISCORD_URL, labelX, labelY + med.height() + 2,
                new ColorRGBA(180,190,255,(int)((150 + (int)(discordHover*90))*anim)));
    }

    // -- Particle --------------------------------------------------------------

    private class Particle {
        float x, y, vx, vy, size; int alpha;
        Particle() { reset(); y = random.nextFloat() * height; }
        void reset() {
            x = random.nextFloat() * width; y = -10;
            vx = (random.nextFloat()-0.5f)*0.4f; vy = random.nextFloat()*0.4f+0.2f;
            size = random.nextFloat()*2.5f+0.5f; alpha = random.nextInt(120)+40;
        }
        void update() { x+=vx; y+=vy; if (y>height+10||x<-10||x>width+10) reset(); }
        void render(UIContext ctx) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), (int)x, (int)y, (int)size, (int)size,
                    BorderRadius.all(size/2), PARTICLE_C.withAlpha(alpha));
        }
    }

    // -- MenuButton ------------------------------------------------------------

    private class MenuButton {
        final String name; int x, y, width, height;
        private float hoverAnim = 0f, scale = 1f, glowAnim = 0f;
        MenuButton(String name, int x, int y, int w, int h) { this.name=name; this.x=x; this.y=y; this.width=w; this.height=h; }

        void render(UIContext ctx, int mx, int my, float alpha) {
            boolean hov = isHovered(mx, my);
            float sp = 0.04f;
            if (hov) { hoverAnim=Math.min(1f,hoverAnim+sp); scale=Math.min(1.03f,scale+sp*0.75f); glowAnim=Math.min(1f,glowAnim+sp*2); }
            else     { hoverAnim=Math.max(0f,hoverAnim-sp); scale=Math.max(1f,scale-sp*0.75f);     glowAnim=Math.max(0f,glowAnim-sp*2); }

            ctx.getMatrices().push();
            ctx.getMatrices().translate(x+width/2f, y+height/2f, 0);
            ctx.getMatrices().scale(scale, scale, 1);
            ctx.getMatrices().translate(-(x+width/2f), -(y+height/2f), 0);

            if (glowAnim > 0)
                DrawUtil.drawRoundedRect(ctx.getMatrices(), x-4, y-4, width+8, height+8, BorderRadius.all(10),
                        GLOW_COLOR.withAlpha((int)(glowAnim*70*alpha)));

            DrawUtil.drawRoundedRect(ctx.getMatrices(), x+2, y+2, width, height, BorderRadius.all(7),
                    new ColorRGBA(0,0,0,(int)(100*alpha)));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(7),
                    blend(new ColorRGBA(20,15,40,(int)(140*alpha)), new ColorRGBA(50,30,90,(int)(200*alpha)), hoverAnim));

            if (hoverAnim > 0)
                DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(7),
                        ACCENT1.withAlpha((int)(hoverAnim*120*alpha)));

            Font f = Fonts.MEDIUM.getFont(8);
            ctx.drawText(f, name, x+width/2f-f.width(name)/2f, y+(height-f.height())/2f,
                    new ColorRGBA(220,200,255,(int)(255*alpha)));
            ctx.getMatrices().pop();
        }

        boolean isHovered(double mx, double my) { return mx>=x&&mx<=x+width&&my>=y&&my<=y+height; }
    }

    // -- SplitButton -----------------------------------------------------------

    private class SplitButton {
        int x, y, width, height; final String left, right;
        private float lHov=0f, rHov=0f, lScale=1f, rScale=1f, lGlow=0f, rGlow=0f;
        SplitButton(int x, int y, int w, int h, String l, String r) { this.x=x; this.y=y; this.width=w; this.height=h; this.left=l; this.right=r; }

        void render(UIContext ctx, int mx, int my, float alpha) {
            float sp = 0.04f; int half = width/2; int bw = half-4;
            boolean lh = isLeftHovered(mx,my), rh = isRightHovered(mx,my);
            if (lh) { lHov=Math.min(1f,lHov+sp); lScale=Math.min(1.03f,lScale+sp*0.75f); lGlow=Math.min(1f,lGlow+sp*2); }
            else    { lHov=Math.max(0f,lHov-sp); lScale=Math.max(1f,lScale-sp*0.75f);     lGlow=Math.max(0f,lGlow-sp*2); }
            if (rh) { rHov=Math.min(1f,rHov+sp); rScale=Math.min(1.03f,rScale+sp*0.75f); rGlow=Math.min(1f,rGlow+sp*2); }
            else    { rHov=Math.max(0f,rHov-sp); rScale=Math.max(1f,rScale-sp*0.75f);     rGlow=Math.max(0f,rGlow-sp*2); }
            Font f = Fonts.MEDIUM.getFont(8);
            renderHalf(ctx, x+2,      y, bw, height, lHov, lScale, lGlow, alpha, left,  f);
            renderHalf(ctx, x+half+2, y, bw, height, rHov, rScale, rGlow, alpha, right, f);
        }

        private void renderHalf(UIContext ctx, int bx, int by, int bw, int bh,
                                 float hov, float sc, float glow, float alpha, String label, Font f) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate(bx+bw/2f, by+bh/2f, 0);
            ctx.getMatrices().scale(sc, sc, 1);
            ctx.getMatrices().translate(-(bx+bw/2f), -(by+bh/2f), 0);

            if (glow > 0)
                DrawUtil.drawRoundedRect(ctx.getMatrices(), bx-4, by-4, bw+8, bh+8, BorderRadius.all(10),
                        GLOW_COLOR.withAlpha((int)(glow*70*alpha)));

            DrawUtil.drawRoundedRect(ctx.getMatrices(), bx+2, by+2, bw, bh, BorderRadius.all(7),
                    new ColorRGBA(0,0,0,(int)(100*alpha)));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), bx, by, bw, bh, BorderRadius.all(7),
                    blend(new ColorRGBA(20,15,40,(int)(140*alpha)), new ColorRGBA(50,30,90,(int)(200*alpha)), hov));

            if (hov > 0)
                DrawUtil.drawRoundedRect(ctx.getMatrices(), bx, by, bw, bh, BorderRadius.all(7),
                        ACCENT1.withAlpha((int)(hov*120*alpha)));

            ctx.drawText(f, label, bx+bw/2f-f.width(label)/2f, by+(bh-f.height())/2f,
                    new ColorRGBA(220,200,255,(int)(255*alpha)));
            ctx.getMatrices().pop();
        }

        boolean isLeftHovered(double mx, double my)  { return mx>=x&&mx<x+width/2-1&&my>=y&&my<=y+height; }
        boolean isRightHovered(double mx, double my) { return mx>x+width/2+1&&mx<=x+width&&my>=y&&my<=y+height; }
    }
}

