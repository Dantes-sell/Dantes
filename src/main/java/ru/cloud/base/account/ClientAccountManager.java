package ru.cloud.base.account;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

public class ClientAccountManager {

    private static Field sessionField = null;

    static {
        // Find the Session field in MinecraftClient via reflection (works regardless of mapping)
        for (Field f : MinecraftClient.class.getDeclaredFields()) {
            if (f.getType() == Session.class) {
                f.setAccessible(true);
                sessionField = f;
                break;
            }
        }
    }

    /**
     * Logs in with an offline (cracked) account by replacing the current session.
     */
    public static void loginAccount(String username) {
        if (username == null || username.isBlank()) return;
        if (sessionField == null) return;
        try {
            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
            Session session = new Session(username, uuid, "", Optional.empty(), Optional.empty(), Session.AccountType.LEGACY);
            sessionField.set(MinecraftClient.getInstance(), session);
        } catch (Exception ignored) {}
    }

    public static String getCurrentUsername() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }
}
