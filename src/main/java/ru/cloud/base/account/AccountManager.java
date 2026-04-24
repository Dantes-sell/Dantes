package ru.cloud.base.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import ru.cloud.Zenith;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AccountManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File(Zenith.DIRECTORY, "accounts.json");

    private static class SaveData {
        @SerializedName("accounts")
        List<String> accounts = new ArrayList<>();
        @SerializedName("lastSelected")
        String lastSelected = null;
    }

    private final List<String> accounts = new ArrayList<>();
    private String lastSelectedAccount = null;

    public AccountManager() {
        load();
    }

    public List<String> getAccounts() {
        return accounts;
    }

    public void addAccount(String name) {
        if (name != null && !name.isBlank() && accounts.stream().noneMatch(a -> a.equalsIgnoreCase(name))) {
            accounts.add(name);
            save();
        }
    }

    public void removeAccount(String name) {
        if (name != null && name.equalsIgnoreCase(lastSelectedAccount)) {
            lastSelectedAccount = null;
        }
        accounts.removeIf(a -> a.equalsIgnoreCase(name));
        save();
    }

    public void clearAll() {
        accounts.clear();
        lastSelectedAccount = null;
        save();
    }

    public void setLastSelectedAccount(String name) {
        this.lastSelectedAccount = name;
        save();
    }

    public String getLastSelectedAccount() {
        return lastSelectedAccount;
    }

    private void load() {
        if (!FILE.exists()) return;
        try (FileReader reader = new FileReader(FILE)) {
            // try new format first
            SaveData data = GSON.fromJson(reader, SaveData.class);
            if (data != null) {
                if (data.accounts != null) accounts.addAll(data.accounts);
                lastSelectedAccount = data.lastSelected;
            }
        } catch (Exception ignored) {
            // fallback: try legacy plain list format
            try (FileReader reader = new FileReader(FILE)) {
                com.google.gson.reflect.TypeToken<List<String>> tt = new com.google.gson.reflect.TypeToken<>() {};
                List<String> loaded = GSON.fromJson(reader, tt.getType());
                if (loaded != null) accounts.addAll(loaded);
            } catch (Exception ignored2) {}
        }
    }

    private void save() {
        try {
            FILE.getParentFile().mkdirs();
            SaveData data = new SaveData();
            data.accounts = accounts;
            data.lastSelected = lastSelectedAccount;
            try (FileWriter writer = new FileWriter(FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception ignored) {}
    }
}
