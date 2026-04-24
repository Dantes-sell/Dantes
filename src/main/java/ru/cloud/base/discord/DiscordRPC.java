package ru.cloud.base.discord;

import by.saskkeee.user.UserInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import ru.cloud.utility.interfaces.IMinecraft;

@Setter
@Getter
public class DiscordRPC implements IMinecraft {

    private static final long APP_ID = 1492616498567577870L;
    private static final String IMAGE_ASSET_KEY = "";
    private static final String LINE_1 = "Build: Release";
    private static final String LINE_2_PREFIX = "UID: ";
    private final long startTime = System.currentTimeMillis() / 1000L;
    private boolean running = false;
    private DiscordInfo info = new DiscordInfo("Unknown", "", "");
    private Thread daemonThread;
    private String cachedUsername = null;
    private String cachedUid = null;

    public void init() {
        boolean connected = DiscordIPC.start(APP_ID, () -> {
            running = true;
            startDaemon();
        });
        if (!connected) return;
    }

    private void startDaemon() {
        daemonThread = new Thread(() -> {
            Thread.currentThread().setName("Discord-RPC");
            while (running) {
                try {
                    tryUpdatePresence();
                } catch (Exception e) {
                    System.err.println("[DiscordRPC] Error: " + e.getMessage());
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        daemonThread.setDaemon(true);
        daemonThread.start();
    }

    private void tryUpdatePresence() {
        if (!DiscordIPC.isConnected()) return;

        boolean inGame = mc.player != null && mc.world != null;

        if (inGame && cachedUsername == null) {
            cachedUsername = mc.player.getName().getString();
            cachedUid = mc.player.getUuidAsString().replace("-", "").substring(0, 8).toUpperCase();
        }

        String details = LINE_1;
        String state = LINE_2_PREFIX + UserInfo.getUID();

        RichPresenceWithButtons rp = new RichPresenceWithButtons();
        rp.setDetails(details);
        rp.setState(state);
        rp.setStart(startTime);
        if (!IMAGE_ASSET_KEY.isBlank()) {
            rp.setLargeImage(IMAGE_ASSET_KEY, "Dantes Client");
        }

        if (!info.avatarUrl().isEmpty()) {
            rp.setSmallImage(info.avatarUrl(), info.userName());
        }

        rp.addButton("Telegram", "https://t.me/+uqfv-JJrsENkZGQ6");
        rp.addButton("Discord", "https://discord.gg/VF4ks4Cw");

        DiscordIPC.setActivity(rp);
    }

    private String getServerIp() {
        try {
            var entry = mc.getCurrentServerEntry();
            if (entry != null && entry.address != null && !entry.address.isBlank()) {
                return entry.address.replace(":25565", "");
            }
            if (mc.getNetworkHandler() != null) {
                var info = mc.getNetworkHandler().getServerInfo();
                if (info != null && info.address != null && !info.address.isBlank()) {
                    return info.address.replace(":25565", "");
                }
            }
        } catch (Exception e) {
            System.err.println("[DiscordRPC] getServerIp error: " + e.getMessage());
        }
        return "Неизвестно";
    }

    public void stopRPC() {
        if (!running) return;
        running = false;
        if (daemonThread != null) daemonThread.interrupt();
        DiscordIPC.stop();
    }

    public record DiscordInfo(String userName, String avatarUrl, String userId) {}

    private static class RichPresenceWithButtons extends RichPresence {
        private final java.util.List<JsonObject> buttons = new java.util.ArrayList<>();

        public void addButton(String label, String url) {
            JsonObject btn = new JsonObject();
            btn.addProperty("label", label);
            btn.addProperty("url", url);
            buttons.add(btn);
        }

        @Override
        public JsonObject toJson() {
            JsonObject o = super.toJson();
            if (!buttons.isEmpty()) {
                JsonArray arr = new JsonArray();
                buttons.forEach(arr::add);
                o.add("buttons", arr);
            }
            return o;
        }
    }
}
