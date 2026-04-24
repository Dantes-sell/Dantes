package ru.cloud.client.modules.api.setting.impl;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import ru.cloud.client.modules.api.setting.Setting;

import java.util.function.Supplier;

public class StringSetting extends Setting {

    @Getter
    @Setter
    private String value;

    @Getter
    private final String description;

    @Getter
    private final int maxLength;

    public StringSetting(String name, String defaultValue) {
        super(name);
        this.value = defaultValue;
        this.description = "";
        this.maxLength = 128;
    }

    public StringSetting(String name, String description, String defaultValue) {
        super(name);
        this.value = defaultValue;
        this.description = description;
        this.maxLength = 128;
    }

    public StringSetting(String name, String description, String defaultValue, int maxLength) {
        super(name);
        this.value = defaultValue;
        this.description = description;
        this.maxLength = maxLength;
    }

    public StringSetting(String name, String description, String defaultValue, int maxLength, Supplier<Boolean> visible) {
        super(name);
        this.value = defaultValue;
        this.description = description;
        this.maxLength = maxLength;
        setVisible(visible);
    }

    @Override
    public void safe(JsonObject propertiesObject) {
        propertiesObject.addProperty(name, value);
    }

    @Override
    public void load(JsonObject propertiesObject) {
        this.value = propertiesObject.get(name).getAsString();
    }
}
