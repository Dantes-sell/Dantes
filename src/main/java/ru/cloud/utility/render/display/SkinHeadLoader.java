package ru.cloud.utility.render.display;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.RandomStringUtils;
import org.lwjgl.BufferUtils;
import ru.cloud.Zenith;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads player skin from Mojang/minotar, crops the head region (face + hat overlay),
 * upscales to 128x128 with nearest-neighbour (pixel-perfect) and registers as GL texture.
 */
public class SkinHeadLoader {

    public enum State { LOADING, READY, FAILED }
    public record Entry(Identifier id, State state) {}

    // skin URLs to try in order
    private static final String[] SKIN_URLS = {
        "https://minotar.net/skin/%s",
        "https://mc-heads.net/skin/%s",
    };

    // built-in fallback skins (Steve / Alex from Mojang CDN)
    public static final String STEVE = "MHF_Steve";
    public static final String ALEX  = "MHF_Alex";

    private static final int OUTPUT_SIZE = 128; // upscale target

    private static final Map<String, Entry> cache = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "SkinHeadLoader");
        t.setDaemon(true);
        return t;
    });

    /** Returns cached entry or starts async load. Never returns null. */
    public static Entry get(String username) {
        if (username == null || username.isBlank()) username = STEVE;

        Entry existing = cache.get(username);
        if (existing != null) return existing;

        final String name = username;
        Identifier id = Zenith.id("skin_head_" + RandomStringUtils.randomAlphanumeric(8).toLowerCase());
        cache.put(name, new Entry(id, State.LOADING));

        executor.submit(() -> loadAsync(name, id));
        return cache.get(name);
    }

    private static void loadAsync(String username, Identifier id) {
        try {
            BufferedImage skin = downloadSkin(username);
            if (skin == null) throw new Exception("all mirrors failed");

            BufferedImage head = extractHead(skin);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(head, "png", baos);
            byte[] png = baos.toByteArray();

            ByteBuffer buf = BufferUtils.createByteBuffer(png.length).put(png);
            buf.flip();
            NativeImage img = NativeImage.read(buf);

            MinecraftClient.getInstance().execute(() -> {
                MinecraftClient.getInstance().getTextureManager()
                        .registerTexture(id, new NativeImageBackedTexture(img));
                cache.put(username, new Entry(id, State.READY));
            });
        } catch (Exception e) {
            System.err.println("[SkinHeadLoader] " + username + " -> " + e.getMessage());
            // try Steve as last resort
            if (!username.equals(STEVE)) {
                Entry steveEntry = get(STEVE);
                cache.put(username, steveEntry); // point to steve's entry
            } else {
                cache.put(username, new Entry(id, State.FAILED));
            }
        }
    }

    /**
     * Tries each mirror URL in order, returns first successful skin image.
     */
    private static BufferedImage downloadSkin(String username) {
        for (String template : SKIN_URLS) {
            try {
                String url = String.format(template, username);
                byte[] raw = download(url);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(raw));
                if (img != null) return img;
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Extracts the head from a 64x64 (or 64x32 legacy) skin texture.
     * Composites face layer (8,8→16,16) + hat overlay (40,8→48,16) at OUTPUT_SIZE.
     */
    private static BufferedImage extractHead(BufferedImage skin) {
        int sw = skin.getWidth();   // should be 64
        int sh = skin.getHeight();  // 64 (modern) or 32 (legacy)

        // pixel coords of face region in the skin atlas
        int fx = (int)(8f  / 64f * sw);
        int fy = (int)(8f  / 64f * sw); // use sw for square atlas
        int fw = (int)(8f  / 64f * sw);
        int fh = (int)(8f  / 64f * sw);

        // pixel coords of hat overlay region
        int hx = (int)(40f / 64f * sw);
        int hy = (int)(8f  / 64f * sw);
        int hw = (int)(8f  / 64f * sw);
        int hh = (int)(8f  / 64f * sw);

        BufferedImage out = new BufferedImage(OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();

        // nearest-neighbour = pixel-perfect, no blur
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_OFF);

        // draw face layer
        g.drawImage(skin, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE, fx, fy, fx+fw, fy+fh, null);

        // draw hat overlay (only if skin is 64x64 and hat region is not fully transparent)
        if (sh >= 32 && hasContent(skin, hx, hy, hw, hh)) {
            g.drawImage(skin, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE, hx, hy, hx+hw, hy+hh, null);
        }

        g.dispose();
        return out;
    }

    /** Returns true if the region has at least one non-transparent pixel. */
    private static boolean hasContent(BufferedImage img, int x, int y, int w, int h) {
        int maxX = Math.min(x + w, img.getWidth());
        int maxY = Math.min(y + h, img.getHeight());
        for (int py = y; py < maxY; py++) {
            for (int px = x; px < maxX; px++) {
                if ((img.getRGB(px, py) >>> 24) > 10) return true;
            }
        }
        return false;
    }

    private static byte[] download(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) throw new Exception("HTTP " + status);
        try (InputStream is = conn.getInputStream()) {
            return is.readAllBytes();
        } finally {
            conn.disconnect();
        }
    }
}
