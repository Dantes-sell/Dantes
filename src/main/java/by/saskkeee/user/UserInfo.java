package by.saskkeee.user;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UserInfo {

    private static final LauncherProfile PROFILE = loadProfile();

    public static String getUsername() {
        return PROFILE.username;
    }

    public static String getUID() {
        return PROFILE.uid;
    }

    // Historical name kept for compatibility with existing code.
    public static String getRole() {
        return PROFILE.subscriptionType;
    }

    public static String getSubscriptionType() {
        return PROFILE.subscriptionType;
    }

    private static LauncherProfile loadProfile() {
        LauncherProfile fromSession = readFromSessionFile();
        if (fromSession != null) {
            return fromSession;
        }

        LauncherProfile fromState = readFromStateFile();
        if (fromState != null) {
            return fromState;
        }

        return new LauncherProfile("Empire", "0", "Free access");
    }

    private static LauncherProfile readFromSessionFile() {
        Path file = findFile("launcher-session.json");
        if (file == null) {
            return null;
        }

        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject root = (JsonObject) new JsonParser().parse(json);

            String username = readString(root, "username", "Empire");
            String uid = readString(root, "uid", "0");
            String subscriptionType = readString(root, "subscriptionType", "Free access");
            return new LauncherProfile(username, uid, subscriptionType);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static LauncherProfile readFromStateFile() {
        Path file = findFile("launcher-state.json");
        if (file == null) {
            return null;
        }

        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject root = (JsonObject) new JsonParser().parse(json);
            if (root == null || !root.has("users")) {
                return null;
            }

            JsonArray users = root.getAsJsonArray("users");
            if (users == null || users.size() == 0) {
                return null;
            }

            JsonObject latest = null;
            int maxUid = Integer.MIN_VALUE;
            for (JsonElement element : users) {
                if (!element.isJsonObject()) continue;
                JsonObject obj = element.getAsJsonObject();
                int uid = readInt(obj, "uid", Integer.MIN_VALUE);
                if (latest == null || uid > maxUid) {
                    latest = obj;
                    maxUid = uid;
                }
            }

            if (latest == null) {
                return null;
            }

            String username = readString(latest, "login", "Empire");
            String uid = readString(latest, "uid", "0");
            String subscriptionType = readString(latest, "version", "Free access");
            return new LauncherProfile(username, uid, subscriptionType);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Path findFile(String name) {
        Path[] candidates = new Path[]{
                Paths.get(name),
                Paths.get("..", name),
                Paths.get("..", "..", name)
        };

        for (Path candidate : candidates) {
            try {
                Path normalized = candidate.toAbsolutePath().normalize();
                if (Files.isRegularFile(normalized)) {
                    return normalized;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private static String readString(JsonObject object, String key, String fallback) {
        if (object == null || key == null || !object.has(key)) {
            return fallback;
        }

        try {
            JsonElement element = object.get(key);
            if (element == null || element.isJsonNull()) {
                return fallback;
            }
            String value = element.getAsString();
            return value == null || value.isBlank() ? fallback : value;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static int readInt(JsonObject object, String key, int fallback) {
        if (object == null || key == null || !object.has(key)) {
            return fallback;
        }

        try {
            return object.get(key).getAsInt();
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static final class LauncherProfile {
        private final String username;
        private final String uid;
        private final String subscriptionType;

        private LauncherProfile(String username, String uid, String subscriptionType) {
            this.username = username;
            this.uid = uid;
            this.subscriptionType = subscriptionType;
        }
    }
}
