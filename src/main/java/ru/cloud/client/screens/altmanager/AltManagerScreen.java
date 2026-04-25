package ru.cloud.client.screens.altmanager;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.cloud.Zenith;
import ru.cloud.base.account.AccountManager;
import ru.cloud.base.account.ClientAccountManager;
import ru.cloud.base.font.Font;
import ru.cloud.base.font.Fonts;
import ru.cloud.utility.interfaces.IMinecraft;
import ru.cloud.utility.math.MathUtil;
import ru.cloud.utility.render.display.SkinHeadLoader;
import ru.cloud.utility.render.display.base.BorderRadius;
import ru.cloud.utility.render.display.base.UIContext;
import ru.cloud.utility.render.display.base.color.ColorRGBA;
import ru.cloud.utility.render.display.shader.DrawUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@SuppressWarnings("All")
public class AltManagerScreen extends Screen implements IMinecraft {

    // -- palette ---------------------------------------------------------------
    private static final ColorRGBA ACCENT      = new ColorRGBA(120,  60, 200, 255);
    private static final ColorRGBA ACCENT_LITE = new ColorRGBA(160, 100, 255, 255);
    private static final ColorRGBA TEXT_MAIN   = new ColorRGBA(220, 200, 255, 255);
    private static final ColorRGBA TEXT_DIM    = new ColorRGBA(140, 110, 190, 255);
    private static final ColorRGBA PANEL_BG    = new ColorRGBA( 14,  10,  28, 200);
    private static final ColorRGBA ENTRY_BG    = new ColorRGBA( 22,  16,  42, 160);
    private static final ColorRGBA BTN_BG      = new ColorRGBA( 28,  18,  55, 180);
    private static final ColorRGBA BTN_HOV     = new ColorRGBA( 55,  30, 110, 220);

    // Minecraft skin UV: face layer 1 = [8/64 .. 16/64] x [8/64 .. 16/64]
    // (used via DrawUtil.drawPlayerHeadWithRoundedShader)
    private static final float SCALE = 1.5f;
    private static final String TITLE = "Alt Manager";

    private final Screen parent;
    private final AccountManager accountManager;
    private final List<String> accounts;

    private boolean isTyping = false;
    private final StringBuilder inputText = new StringBuilder();

    private float scrollOffset = 0f, targetScrollOffset = 0f;
    private float hoverAnimInput = 0f;
    private float[] hoverAnim1, hoverAnim2;
    private int selectedIndex = -1;

    private float createHover = 0f, clearHover = 0f, randomHover = 0f;

    // confirm dialog
    private boolean showConfirm = false;
    private float confirmAnim   = 0f;
    private float yesHover = 0f, noHover = 0f;

    private int shakeTime = 0;
    private float shakeOffsetY = 0f;

    public AltManagerScreen(Screen parent) {
        super(Text.of("Alt Manager"));
        this.parent = parent;
        this.accountManager = Zenith.getInstance().getAccountManager();
        this.accounts = accountManager.getAccounts();
        // restore selected index from last selected account
        String last = accountManager.getLastSelectedAccount();
        if (last == null) {
            // fallback: use current session username
            last = ClientAccountManager.getCurrentUsername();
        }
        if (last != null) {
            for (int i = 0; i < accounts.size(); i++) {
                if (accounts.get(i).equalsIgnoreCase(last)) {
                    selectedIndex = i;
                    break;
                }
            }
        }
    }

    // -- skin head URL ---------------------------------------------------------
    // SkinHeadLoader ��� �������� ������ �� ����� � ��������� �� 128px

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        UIContext ctx = UIContext.of(drawContext, mouseX, mouseY, delta);
        scrollOffset = MathUtil.interpolateSmooth(10, scrollOffset, targetScrollOffset);

        long now = System.currentTimeMillis();
        float at = (now % 10000L) / 10000f;
        float s1=(float)Math.sin(at*Math.PI*2), s2=(float)Math.sin(at*Math.PI*2+Math.PI/2);
        float c1=(float)Math.cos(at*Math.PI*2), c2=(float)Math.cos(at*Math.PI*2+Math.PI/2);

        DrawUtil.drawRoundedRect(ctx.getMatrices(), -1, -1, width+2, height+2, BorderRadius.all(0),
                new ColorRGBA(clamp(18+(int)(s1*8)), clamp(10+(int)(c1*6)), clamp(35+(int)(s2*12)), 255),
                new ColorRGBA(clamp(25+(int)(c2*10)),clamp(12+(int)(s1*8)), clamp(50+(int)(c1*15)),255),
                new ColorRGBA(clamp(15+(int)(c1*8)), clamp( 8+(int)(s2*6)), clamp(40+(int)(s1*12)),255),
                new ColorRGBA(clamp(22+(int)(s2*10)),clamp(10+(int)(c2*8)), clamp(45+(int)(c1*12)),255));
        DrawUtil.drawRoundedRect(ctx.getMatrices(), -1, -1, width+2, height+2, BorderRadius.all(0), new ColorRGBA(0,0,0,170));

        if (shakeTime > 0) { shakeTime--; shakeOffsetY = (float)(Math.sin(shakeTime*0.5)*3); } else shakeOffsetY = 0f;

        Font bold = Fonts.BOLD.getFont(18);
        Font med  = Fonts.MEDIUM.getFont(8);
        Font sem  = Fonts.SEMIBOLD.getFont(8);

        // title
        float tw = bold.width(TITLE), tx = (width-tw)/2f, ty = height/7f + shakeOffsetY;
        float ta = (now % 4000L) / 1500f;
        ctx.drawText(bold, TITLE, tx+1.5f, ty+1.5f, new ColorRGBA(0,0,0,80));
        ctx.drawText(bold, TITLE, tx, ty, new ColorRGBA(animGrad(ACCENT, ACCENT_LITE, ta)));

        int cx = width/2, cy = height/2;
        int inputW=(int)(220*SCALE), inputH=(int)(17*SCALE);
        int inputX=cx-(int)(110*SCALE), inputY=cy-(int)(92*SCALE);

        // input field
        boolean hovIn = MathUtil.isHovered(mouseX, mouseY, inputX, inputY, inputW, inputH);
        hoverAnimInput = lerp(hoverAnimInput, hovIn ? 1f : 0f, 0.15f);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), inputX, inputY, inputW, inputH, BorderRadius.all(5), PANEL_BG);
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), inputX, inputY, inputW, inputH, 0.6f, BorderRadius.all(5),
                blend(new ColorRGBA(60,40,100,80), ACCENT.withAlpha(160), hoverAnimInput));
        if (!isTyping) {
            int dots = (int)(now/500%4);
            StringBuilder ph = new StringBuilder("Enter nickname");
            for (int i=0;i<dots;i++) ph.append(".");
            ctx.drawText(med, ph.toString(), inputX+8, inputY+inputH/2f-med.height()/2f, blend(TEXT_DIM, TEXT_MAIN, hoverAnimInput));
        } else {
            ctx.drawText(med, inputText+((now/500%2)==0?"|":""), inputX+8, inputY+inputH/2f-med.height()/2f, TEXT_MAIN);
        }

        // list panel
        int listX=inputX, listY=cy-(int)(70*SCALE), listW=(int)(220*SCALE), listH=(int)(140*SCALE);
        DrawUtil.drawRoundedRect(ctx.getMatrices(), listX, listY, listW, listH, BorderRadius.all(6), PANEL_BG);
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), listX, listY, listW, listH, 0.6f, BorderRadius.all(6),
                new ColorRGBA(70,45,120,80));

        if (hoverAnim1==null||hoverAnim1.length!=accounts.size()) hoverAnim1=new float[accounts.size()];
        if (hoverAnim2==null||hoverAnim2.length!=accounts.size()) hoverAnim2=new float[accounts.size()];

        ctx.enableScissor(listX+1, listY+1, listX+listW-1, listY+listH-1);

        float sY=listY+6, iH=35*SCALE;
        int eX=cx-(int)(105*SCALE), eW=(int)(140*SCALE), eH=(int)(30*SCALE);
        int bW=(int)(60*SCALE), bH=(int)(13*SCALE);
        int headSize = eH - 8; 

        for (int i=0; i<accounts.size(); i++) {
            float y=sY-scrollOffset+i*iH;
            boolean isSel = i==selectedIndex;

            DrawUtil.drawRoundedRect(ctx.getMatrices(), eX, y, eW+10, eH, BorderRadius.all(5), ENTRY_BG);
            if (isSel) {
                DrawUtil.drawRoundedRect(ctx.getMatrices(), eX, y, eW+10, eH, BorderRadius.all(5), ACCENT.withAlpha(18));
                DrawUtil.drawRoundedBorder(ctx.getMatrices(), eX, y, eW+10, eH, 0.8f, BorderRadius.all(5), ACCENT.withAlpha(200));
            }

            // -- skin head -----------------------------------------------------
            float hx = eX + 4, hy = y + (eH - headSize) / 2f;
            SkinHeadLoader.Entry entry = SkinHeadLoader.get(accounts.get(i));

            if (entry != null && entry.state() == SkinHeadLoader.State.READY) {
                DrawUtil.drawRoundedTexture(ctx.getMatrices(), entry.id(), hx, hy, headSize, headSize,
                        BorderRadius.all(3f), ColorRGBA.WHITE);
            } else {
                
                DrawUtil.drawRoundedRect(ctx.getMatrices(), hx, hy, headSize, headSize, BorderRadius.all(4),
                        blend(ACCENT.withAlpha(140), ACCENT_LITE.withAlpha(140), (float)Math.sin(now*0.001+i)*0.5f+0.5f));
                String letter = accounts.get(i).substring(0,1).toUpperCase();
                ctx.drawText(sem, letter, hx+headSize/2f-sem.width(letter)/2f, hy+headSize/2f-sem.height()/2f, ColorRGBA.WHITE);
            }

            ctx.drawText(sem, accounts.get(i), eX+headSize+10, y+4, isSel ? ACCENT_LITE : TEXT_MAIN);
            ctx.drawText(med, LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    eX+headSize+10, y+16, TEXT_DIM);

            // select btn
            int sBX=eX+eW+(int)(10*SCALE), sBY=(int)y;
            boolean h1=MathUtil.isHovered(mouseX,mouseY,sBX,sBY,bW,bH);
            hoverAnim1[i]=lerp(hoverAnim1[i],h1?1f:0f,0.15f);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), sBX, sBY, bW, bH+2, BorderRadius.all(4),
                    blend(BTN_BG, BTN_HOV, hoverAnim1[i]));
            if (hoverAnim1[i]>0) DrawUtil.drawRoundedBorder(ctx.getMatrices(), sBX, sBY, bW, bH+2, 0.5f,
                    BorderRadius.all(4), ACCENT.withAlpha((int)(hoverAnim1[i]*130)));
            ctx.drawText(med,"Select",sBX+bW/2f-med.width("Select")/2f,sBY+bH/2f-med.height()/2f+1,TEXT_MAIN);

            // delete btn
            int dBX=sBX, dBY=sBY+bH+(int)(3*SCALE);
            boolean h2=MathUtil.isHovered(mouseX,mouseY,dBX,dBY,bW,bH);
            hoverAnim2[i]=lerp(hoverAnim2[i],h2?1f:0f,0.15f);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), dBX, dBY, bW, bH+2, BorderRadius.all(4),
                    blend(BTN_BG, new ColorRGBA(90,20,40,220), hoverAnim2[i]));
            if (hoverAnim2[i]>0) DrawUtil.drawRoundedBorder(ctx.getMatrices(), dBX, dBY, bW, bH+2, 0.5f,
                    BorderRadius.all(4), new ColorRGBA(200,60,80,(int)(hoverAnim2[i]*130)));
            ctx.drawText(med,"Delete",dBX+bW/2f-med.width("Delete")/2f,dBY+bH/2f-med.height()/2f+1,
                    blend(TEXT_MAIN, new ColorRGBA(255,120,140,255), hoverAnim2[i]));
        }
        ctx.disableScissor();

        // bottom buttons
        int bY=listY+listH+(int)(10*SCALE), bTW=(int)(70*SCALE);
        int crX=cx-bTW-(int)(40*SCALE), clX=cx-(bTW/2), rnX=cx+bTW+(int)(-30*SCALE);
        float sp=0.05f;
        createHover=clampAnim(createHover,MathUtil.isHovered(mouseX,mouseY,crX,bY,bTW,inputH),sp);
        clearHover =clampAnim(clearHover, MathUtil.isHovered(mouseX,mouseY,clX,bY,bTW,inputH),sp);
        randomHover=clampAnim(randomHover,MathUtil.isHovered(mouseX,mouseY,rnX,bY,bTW,inputH),sp);
        drawBtn(ctx,med,"+ Create",crX,bY,bTW,inputH,createHover,ACCENT);
        drawBtn(ctx,med,"? Clear All", clX,bY,bTW,inputH,clearHover, new ColorRGBA(160,40,60,255));
        drawBtn(ctx,med,"⟳ Random",rnX,bY,bTW,inputH,randomHover,new ColorRGBA(40,120,160,255));

        // status
        String qtyLine = "accounts: " + accounts.size();
        ctx.drawText(med, qtyLine, (width-med.width(qtyLine))/2f, bY+inputH+(int)(18*SCALE), TEXT_DIM);

        // confirm dialog
        if (showConfirm || confirmAnim > 0.01f) {
            confirmAnim = lerp(confirmAnim, showConfirm ? 1f : 0f, 0.18f);
            drawConfirmDialog(ctx, med, mouseX, mouseY, confirmAnim);
        }
    }

    // -- draw helpers ----------------------------------------------------------

    private void drawBtn(UIContext ctx, Font f, String label, float x, float y, float w, float h, float hov, ColorRGBA accent) {
        DrawUtil.drawRoundedRect(ctx.getMatrices(), x+1.5f, y+1.5f, w, h, BorderRadius.all(5), new ColorRGBA(0,0,0,(int)(60*hov)));
        DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h, BorderRadius.all(5), blend(BTN_BG, accent.withAlpha(200), hov*0.65f));
        if (hov>0) DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, w, h, 0.6f, BorderRadius.all(5), accent.withAlpha((int)(hov*170)));
        ctx.drawText(f, label, x+w/2f-f.width(label)/2f, y+h/2f-f.height()/2f, blend(TEXT_DIM, TEXT_MAIN, hov));
    }

    private void drawConfirmDialog(UIContext ctx, Font med, int mx, int my, float anim) {
        long now = System.currentTimeMillis();

        // -- backdrop ----------------------------------------------------------
        DrawUtil.drawRoundedRect(ctx.getMatrices(), 0, 0, width, height, BorderRadius.all(0),
                new ColorRGBA(0, 0, 0, (int)(150 * anim)));

        // -- panel geometry ----------------------------------------------------
        int bw = 320, bh = 148;
        float bx = (width - bw) / 2f;
        float by = (height - bh) / 2f - (1f - anim) * 20;

        // animated mini-background matching main screen
        float at = (now % 10000L) / 10000f;
        float s1=(float)Math.sin(at*Math.PI*2), s2=(float)Math.sin(at*Math.PI*2+Math.PI/2);
        float c1=(float)Math.cos(at*Math.PI*2), c2=(float)Math.cos(at*Math.PI*2+Math.PI/2);
        ColorRGBA bg1 = new ColorRGBA(clamp(22+(int)(s1*8)), clamp(12+(int)(c1*6)), clamp(42+(int)(s2*12)), (int)(255*anim));
        ColorRGBA bg2 = new ColorRGBA(clamp(30+(int)(c2*10)),clamp(14+(int)(s1*8)), clamp(58+(int)(c1*15)),(int)(255*anim));
        ColorRGBA bg3 = new ColorRGBA(clamp(18+(int)(c1*8)), clamp(10+(int)(s2*6)), clamp(48+(int)(s1*12)),(int)(255*anim));
        ColorRGBA bg4 = new ColorRGBA(clamp(26+(int)(s2*10)),clamp(12+(int)(c2*8)), clamp(52+(int)(c1*12)),(int)(255*anim));

        // outer glow
        DrawUtil.drawRoundedRect(ctx.getMatrices(), bx-5, by-5, bw+10, bh+10, BorderRadius.all(14),
                ACCENT.withAlpha((int)(35 * anim)));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), bx-2, by-2, bw+4, bh+4, 0.8f, BorderRadius.all(13),
                ACCENT.withAlpha((int)(90 * anim)));

        
        DrawUtil.drawRoundedRect(ctx.getMatrices(), bx, by, bw, bh, BorderRadius.all(11), bg1, bg2, bg3, bg4);
        // dark overlay for readability
        DrawUtil.drawRoundedRect(ctx.getMatrices(), bx, by, bw, bh, BorderRadius.all(11),
                new ColorRGBA(0, 0, 0, (int)(140 * anim)));
        // panel border
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), bx, by, bw, bh, 0.8f, BorderRadius.all(11),
                ACCENT.withAlpha((int)(130 * anim)));

        // -- icon circle -------------------------------------------------------
        float icR = 14f;
        float icX = bx + bw / 2f - icR;
        float icY = by + 18;
        DrawUtil.drawRoundedRect(ctx.getMatrices(), icX, icY, icR*2, icR*2, BorderRadius.all(icR),
                blend(ACCENT, ACCENT_LITE, (float)Math.sin(now*0.002)*0.5f+0.5f).withAlpha((int)(180*anim)));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), icX, icY, icR*2, icR*2, 0.8f, BorderRadius.all(icR),
                ACCENT_LITE.withAlpha((int)(160*anim)));
        Font boldIcon = Fonts.BOLD.getFont(12);
        ctx.drawText(boldIcon, "?", icX+icR-boldIcon.width("?")/2f, icY+icR-boldIcon.height()/2f,
                new ColorRGBA(220, 200, 255, (int)(255*anim)));

        // -- title -------------------------------------------------------------
        Font bold = Fonts.BOLD.getFont(10);
        String title = "Удалить все аккаунты?";
        float ta = (now % 4000L) / 1500f;
        ctx.drawText(bold, title, bx + (bw - bold.width(title)) / 2f, by + icR*2 + 26,
                new ColorRGBA(animGrad(ACCENT, ACCENT_LITE, ta)));

        // -- thin separator ----------------------------------------------------
        DrawUtil.drawRoundedRect(ctx.getMatrices(), bx+20, by+icR*2+42, bw-40, 0.8f, BorderRadius.all(0.4f),
                ACCENT.withAlpha((int)(60*anim)));

        // -- buttons -----------------------------------------------------------
        int btnW = 120, btnH = 30;
        float yesX = bx + bw/2f - btnW - 10;
        float noX  = bx + bw/2f + 10;
        float btnY = by + bh - 44;

        yesHover = lerp(yesHover, MathUtil.isHovered(mx, my, yesX, btnY, btnW, btnH) ? 1f : 0f, 0.15f);
        noHover  = lerp(noHover,  MathUtil.isHovered(mx, my, noX,  btnY, btnW, btnH) ? 1f : 0f, 0.15f);

        
        ColorRGBA yesAccent = new ColorRGBA(180, 40, 80, 255);
        if (yesHover > 0)
            DrawUtil.drawRoundedRect(ctx.getMatrices(), yesX-3, btnY-3, btnW+6, btnH+6, BorderRadius.all(10),
                    yesAccent.withAlpha((int)(yesHover * 55 * anim)));
        DrawUtil.drawRoundedRect(ctx.getMatrices(), yesX+1.5f, btnY+1.5f, btnW, btnH, BorderRadius.all(7),
                new ColorRGBA(0, 0, 0, (int)(60 * yesHover * anim)));
        DrawUtil.drawRoundedRect(ctx.getMatrices(), yesX, btnY, btnW, btnH, BorderRadius.all(7),
                blend(new ColorRGBA(28, 18, 55, (int)(200*anim)), yesAccent.withAlpha((int)(200*anim)), yesHover * 0.65f));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), yesX, btnY, btnW, btnH, 0.7f, BorderRadius.all(7),
                yesAccent.withAlpha((int)(yesHover * 180 * anim)));
        ctx.drawText(med, "Удалить всё",
                yesX + btnW/2f - med.width("Удалить всё")/2f, btnY + btnH/2f - med.height()/2f,
                blend(TEXT_DIM, TEXT_MAIN, yesHover).withAlpha((int)(255*anim)));

        
        if (noHover > 0)
            DrawUtil.drawRoundedRect(ctx.getMatrices(), noX-3, btnY-3, btnW+6, btnH+6, BorderRadius.all(10),
                    ACCENT.withAlpha((int)(noHover * 55 * anim)));
        DrawUtil.drawRoundedRect(ctx.getMatrices(), noX+1.5f, btnY+1.5f, btnW, btnH, BorderRadius.all(7),
                new ColorRGBA(0, 0, 0, (int)(60 * noHover * anim)));
        DrawUtil.drawRoundedRect(ctx.getMatrices(), noX, btnY, btnW, btnH, BorderRadius.all(7),
                blend(BTN_BG.withAlpha((int)(200*anim)), BTN_HOV.withAlpha((int)(200*anim)), noHover * 0.65f));
        DrawUtil.drawRoundedBorder(ctx.getMatrices(), noX, btnY, btnW, btnH, 0.7f, BorderRadius.all(7),
                ACCENT.withAlpha((int)(noHover * 180 * anim)));
        ctx.drawText(med, "Отмена",
                noX + btnW/2f - med.width("Отмена")/2f, btnY + btnH/2f - med.height()/2f,
                blend(TEXT_DIM, TEXT_MAIN, noHover).withAlpha((int)(255*anim)));
    }

    // -- mouse / keyboard ------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cx=width/2, cy=height/2;
        int inputW=(int)(220*SCALE), inputH=(int)(17*SCALE);
        int inputX=cx-(int)(110*SCALE), inputY=cy-(int)(92*SCALE);
        int bTW=(int)(70*SCALE);
        int bY=cy-(int)(70*SCALE)+(int)(140*SCALE)+(int)(10*SCALE);
        int crX=cx-bTW-(int)(40*SCALE), clX=cx-(bTW/2), rnX=cx+bTW+(int)(-30*SCALE);

        if (showConfirm) {
            int bw=320, bh=148;
            float bx=(width-bw)/2f, by=(height-bh)/2f;
            int btnW=120, btnH=30;
            float yesX=bx+bw/2f-btnW-10, noX=bx+bw/2f+10, btnY=by+bh-44;
            if (MathUtil.isHovered(mouseX,mouseY,yesX,btnY,btnW,btnH)) {
                accountManager.clearAll(); selectedIndex=-1; hoverAnim1=null; hoverAnim2=null;
                showConfirm=false; confirmAnim=0f; return true;
            }
            if (MathUtil.isHovered(mouseX,mouseY,noX,btnY,btnW,btnH)) { showConfirm=false; confirmAnim=0f; return true; }
            return true;
        }

        Font bold=Fonts.BOLD.getFont(18);
        float tw=bold.width(TITLE), tx=(width-tw)/2f, ty=height/7f;
        if (mouseX>=tx&&mouseX<=tx+tw&&mouseY>=ty&&mouseY<=ty+25) { shakeTime=20; return true; }

        if (MathUtil.isHovered(mouseX,mouseY,inputX,inputY,inputW,inputH)&&!isTyping&&button==0) { isTyping=true; return true; }
        if (!MathUtil.isHovered(mouseX,mouseY,inputX,inputY,inputW,inputH)
                &&!MathUtil.isHovered(mouseX,mouseY,crX,bY,bTW,inputH)
                &&!MathUtil.isHovered(mouseX,mouseY,clX,bY,bTW,inputH)
                &&!MathUtil.isHovered(mouseX,mouseY,rnX,bY,bTW,inputH)
                &&isTyping&&button==0) { isTyping=false; return true; }

        if (MathUtil.isHovered(mouseX,mouseY,crX,bY,bTW,inputH)&&button==0) {
            String name=inputText.toString().trim();
            if (!name.isEmpty()&&accounts.stream().noneMatch(a->a.equalsIgnoreCase(name))) {
                isTyping=false; accountManager.addAccount(name); inputText.setLength(0);
            }
            return true;
        }
        if (MathUtil.isHovered(mouseX,mouseY,clX,bY,bTW,inputH)&&button==0) { showConfirm=true; return true; }
        if (MathUtil.isHovered(mouseX,mouseY,rnX,bY,bTW,inputH)&&button==0) {
            String rnd=generateRandomName();
            if (!accounts.contains(rnd)) accountManager.addAccount(rnd);
            ClientAccountManager.loginAccount(rnd);
            selectedIndex=accounts.indexOf(rnd);
            accountManager.setLastSelectedAccount(rnd);
            return true;
        }

        int listX=inputX, listY=cy-(int)(70*SCALE), listW=(int)(220*SCALE), listH=(int)(140*SCALE);
        if (MathUtil.isHovered(mouseX,mouseY,listX,listY,listW,listH)) {
            float sY=listY+6, iH=35*SCALE;
            int eX=cx-(int)(105*SCALE), eW=(int)(140*SCALE), eH=(int)(30*SCALE);
            int bW=(int)(60*SCALE), bH=(int)(13*SCALE);
            for (int i=0; i<accounts.size(); i++) {
                float y=sY-scrollOffset+i*iH;
                int sBX=eX+eW+(int)(10*SCALE), sBY=(int)y;
                int dBX=sBX, dBY=sBY+bH+(int)(3*SCALE);
                if (MathUtil.isHovered(mouseX,mouseY,sBX,sBY,bW,bH)&&button==0) {
                    ClientAccountManager.loginAccount(accounts.get(i)); selectedIndex=i;
                    accountManager.setLastSelectedAccount(accounts.get(i)); return true;
                }
                if (MathUtil.isHovered(mouseX,mouseY,dBX,dBY,bW,bH)&&button==0) {
                    if (selectedIndex==i) selectedIndex=-1; else if (selectedIndex>i) selectedIndex--;
                    accountManager.removeAccount(accounts.get(i));
                    hoverAnim1=null; hoverAnim2=null;
                    int maxOff=Math.max(0,(accounts.size()*(int)(38*SCALE))-(int)(135*SCALE));
                    targetScrollOffset=Math.max(0,Math.min(targetScrollOffset,maxOff));
                    return true;
                }
                if (MathUtil.isHovered(mouseX,mouseY,eX,(int)y,eW+10,eH)&&button==0) {
                    ClientAccountManager.loginAccount(accounts.get(i)); selectedIndex=i;
                    accountManager.setLastSelectedAccount(accounts.get(i)); return true;
                }
            }
        }
        return super.mouseClicked(mouseX,mouseY,button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int cy=height/2, listY=cy-(int)(70*SCALE), listH=(int)(140*SCALE);
        if (my>=listY&&my<=listY+listH) {
            targetScrollOffset-=sy*(int)(30*SCALE);
            int maxOff=Math.max(0,(accounts.size()*(int)(36*SCALE))-listH);
            targetScrollOffset=Math.max(0,Math.min(targetScrollOffset,maxOff));
            return true;
        }
        return super.mouseScrolled(mx,my,sx,sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (isTyping) {
            boolean ctrl=GLFW.glfwGetKey(mc.getWindow().getHandle(),GLFW.GLFW_KEY_LEFT_CONTROL)==GLFW.GLFW_PRESS
                    ||GLFW.glfwGetKey(mc.getWindow().getHandle(),GLFW.GLFW_KEY_RIGHT_CONTROL)==GLFW.GLFW_PRESS;
            if (ctrl&&key==GLFW.GLFW_KEY_V) {
                String clip=GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
                if (clip!=null&&!clip.isEmpty()) {
                    String f=clip.replaceAll("[^\\w]",""); int max=16-inputText.length();
                    if (max>0) inputText.append(f.length()>max?f.substring(0,max):f);
                }
                return true;
            }
            if (key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER) {
                String name=inputText.toString().trim();
                if (!name.isEmpty()&&accounts.stream().noneMatch(a->a.equalsIgnoreCase(name))) {
                    isTyping=false; accountManager.addAccount(name); inputText.setLength(0);
                }
                return true;
            }
            if (key==GLFW.GLFW_KEY_BACKSPACE&&inputText.length()>0) { inputText.deleteCharAt(inputText.length()-1); return true; }
        }
        if (key==GLFW.GLFW_KEY_ESCAPE&&showConfirm) { showConfirm=false; confirmAnim=0f; return true; }
        return super.keyPressed(key,scan,mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (isTyping&&chr!='\n'&&chr!='\r'&&inputText.length()<16&&(Character.isLetterOrDigit(chr)||chr=='_')) {
            inputText.append(chr); return true;
        }
        return super.charTyped(chr,mods);
    }

    @Override
    public void close() { if (parent!=null) mc.setScreen(parent); else super.close(); }

    // -- helpers ---------------------------------------------------------------

    private static int clamp(int v) { return Math.max(0,Math.min(255,v)); }
    private static float lerp(float a, float b, float t) { return a+(b-a)*t; }
    private static float clampAnim(float v, boolean up, float sp) { return up?Math.min(1f,v+sp):Math.max(0f,v-sp); }
    private static ColorRGBA blend(ColorRGBA a, ColorRGBA b, float t) {
        return new ColorRGBA(
                (int)(a.getRed()  +(b.getRed()  -a.getRed())  *t),
                (int)(a.getGreen()+(b.getGreen()-a.getGreen())*t),
                (int)(a.getBlue() +(b.getBlue() -a.getBlue()) *t),
                (int)(a.getAlpha()+(b.getAlpha()-a.getAlpha())*t));
    }
    private static int animGrad(ColorRGBA c1, ColorRGBA c2, float t) {
        float f=t%2f; f=f<1f?f:2f-f; return blend(c1,c2,f).getRGB();
    }

    private static final String[] PREFIXES = {
        "Dark","Shadow","Silent","Void","Neon","Hyper","Astro","Cyber","Storm","Frost",
        "Blaze","Toxic","Lunar","Solar","Omega","Alpha","Delta","Sigma","Turbo","Stealth"
    };
    private static final String[] NOUNS = {
        "Wolf","Fox","Hawk","Lynx","Viper","Raven","Ghost","Blade","Nova","Pulse",
        "Drift","Surge","Flare","Spike","Claw","Fang","Bolt","Shade","Rift","Core"
    };
    private static final String[] SUFFIXES = { "gg","xd","pro","pvp","hd","op","ez","tv","yt","dev" };

    private String generateRandomName() {
        Random r = new Random();
        String name;
        switch (r.nextInt(5)) {
            case 0: name = PREFIXES[r.nextInt(PREFIXES.length)] + NOUNS[r.nextInt(NOUNS.length)]; break;
            case 1: name = PREFIXES[r.nextInt(PREFIXES.length)].toLowerCase()+"_"+NOUNS[r.nextInt(NOUNS.length)].toLowerCase(); break;
            case 2: name = NOUNS[r.nextInt(NOUNS.length)] + SUFFIXES[r.nextInt(SUFFIXES.length)]; break;
            case 3: name = "x" + NOUNS[r.nextInt(NOUNS.length)] + "x"; break;
            default: name = PREFIXES[r.nextInt(PREFIXES.length)]; int d=r.nextInt(2)+3; for(int i=0;i<d;i++) name+=r.nextInt(10); break;
        }
        if (r.nextFloat()<0.5f) { int d=r.nextInt(3)+1; for(int i=0;i<d;i++) name+=r.nextInt(10); }
        if (name.length()>16) name=name.substring(0,16);
        return name;
    }
}


