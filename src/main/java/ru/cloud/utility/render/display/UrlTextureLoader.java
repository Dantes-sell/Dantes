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

public class UrlTextureLoader {

    public enum State { LOADING, READY, FAILED }

    public record Entry(Identifier id, State state) {}

    private static final Map<String, Entry> cache = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "UrlTextureLoader");
        t.setDaemon(true);
        return t;
    });

    public static Entry get(String url) {
        if (url == null || url.isBlank()) return null;

        Entry existing = cache.get(url);
        if (existing != null) return existing;

        String texId = "url_tex_" + RandomStringUtils.randomAlphanumeric(8).toLowerCase();
        Identifier id = Zenith.id(texId);
        cache.put(url, new Entry(id, State.LOADING));

        executor.submit(() -> {
            try {
                // 1. download raw bytes (follow redirects, browser headers)
                byte[] raw = download(url);

                
                BufferedImage src = ImageIO.read(new ByteArrayInputStream(raw));
                if (src == null) throw new Exception("ImageIO could not decode image");

                
                BufferedImage square = cropToSquare(src);

                // 4. convert to ARGB PNG bytes that NativeImage can read
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(square, "png", baos);
                byte[] pngBytes = baos.toByteArray();

                ByteBuffer buf = BufferUtils.createByteBuffer(pngBytes.length).put(pngBytes);
                buf.flip();
                NativeImage img = NativeImage.read(buf);

                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().getTextureManager()
                            .registerTexture(id, new NativeImageBackedTexture(img));
                    cache.put(url, new Entry(id, State.READY));
                });
            } catch (Exception e) {
                System.err.println("[UrlTextureLoader] " + url + " -> " + e.getMessage());
                cache.put(url, new Entry(id, State.FAILED));
            }
        });

        return cache.get(url);
    }

    public static void invalidate(String url) {
        cache.remove(url);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Crop image to a centered square (cover behaviour). */
    private static BufferedImage cropToSquare(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int size = Math.min(w, h);
        int x = (w - size) / 2;
        int y = (h - size) / 2;

        
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, size, size, x, y, x + size, y + size, null);
        g.dispose();
        return out;
    }

    /** Download bytes, following redirects, with browser-like headers. */
    private static byte[] download(String url) throws Exception {
        String current = url;
        for (int hop = 0; hop < 8; hop++) {
            HttpURLConnection conn = (HttpURLConnection) URI.create(current).toURL().openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/124.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept",
                    "image/avif,image/webp,image/apng,image/jpeg,image/*,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setRequestProperty("Referer", "https://www.google.com/");
            conn.setRequestProperty("sec-fetch-dest", "image");
            conn.setRequestProperty("sec-fetch-mode", "no-cors");

            int status = conn.getResponseCode();

            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String loc = conn.getHeaderField("Location");
                conn.disconnect();
                if (loc == null) throw new Exception("Redirect without Location");
                if (!loc.startsWith("http")) loc = URI.create(current).resolve(loc).toString();
                current = loc;
                continue;
            }

            if (status < 200 || status >= 300)
                throw new Exception("HTTP " + status);

            try (InputStream is = conn.getInputStream()) {
                return is.readAllBytes();
            } finally {
                conn.disconnect();
            }
        }
        throw new Exception("Too many redirects");
    }
}

