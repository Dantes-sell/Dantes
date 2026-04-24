package ru.cloud.base.comand.impl.args;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class KeyArgumentType implements ArgumentType<Integer> {

    // Карта имя -> GLFW код
    public static final Map<String, Integer> KEY_MAP = new LinkedHashMap<>();

    static {
        KEY_MAP.put("NONE", GLFW.GLFW_KEY_UNKNOWN);
        for (char c = 'A'; c <= 'Z'; c++) {
            KEY_MAP.put(String.valueOf(c), GLFW.GLFW_KEY_A + (c - 'A'));
        }
        for (int i = 0; i <= 9; i++) {
            KEY_MAP.put("NUM" + i, GLFW.GLFW_KEY_0 + i);
        }
        KEY_MAP.put("F1", GLFW.GLFW_KEY_F1);
        KEY_MAP.put("F2", GLFW.GLFW_KEY_F2);
        KEY_MAP.put("F3", GLFW.GLFW_KEY_F3);
        KEY_MAP.put("F4", GLFW.GLFW_KEY_F4);
        KEY_MAP.put("F5", GLFW.GLFW_KEY_F5);
        KEY_MAP.put("F6", GLFW.GLFW_KEY_F6);
        KEY_MAP.put("F7", GLFW.GLFW_KEY_F7);
        KEY_MAP.put("F8", GLFW.GLFW_KEY_F8);
        KEY_MAP.put("F9", GLFW.GLFW_KEY_F9);
        KEY_MAP.put("F10", GLFW.GLFW_KEY_F10);
        KEY_MAP.put("F11", GLFW.GLFW_KEY_F11);
        KEY_MAP.put("F12", GLFW.GLFW_KEY_F12);
        KEY_MAP.put("SPACE", GLFW.GLFW_KEY_SPACE);
        KEY_MAP.put("LSHIFT", GLFW.GLFW_KEY_LEFT_SHIFT);
        KEY_MAP.put("RSHIFT", GLFW.GLFW_KEY_RIGHT_SHIFT);
        KEY_MAP.put("LCTRL", GLFW.GLFW_KEY_LEFT_CONTROL);
        KEY_MAP.put("RCTRL", GLFW.GLFW_KEY_RIGHT_CONTROL);
        KEY_MAP.put("LALT", GLFW.GLFW_KEY_LEFT_ALT);
        KEY_MAP.put("RALT", GLFW.GLFW_KEY_RIGHT_ALT);
        KEY_MAP.put("TAB", GLFW.GLFW_KEY_TAB);
        KEY_MAP.put("CAPS", GLFW.GLFW_KEY_CAPS_LOCK);
        KEY_MAP.put("INSERT", GLFW.GLFW_KEY_INSERT);
        KEY_MAP.put("DELETE", GLFW.GLFW_KEY_DELETE);
        KEY_MAP.put("HOME", GLFW.GLFW_KEY_HOME);
        KEY_MAP.put("END", GLFW.GLFW_KEY_END);
        KEY_MAP.put("PAGEUP", GLFW.GLFW_KEY_PAGE_UP);
        KEY_MAP.put("PAGEDOWN", GLFW.GLFW_KEY_PAGE_DOWN);
        KEY_MAP.put("UP", GLFW.GLFW_KEY_UP);
        KEY_MAP.put("DOWN", GLFW.GLFW_KEY_DOWN);
        KEY_MAP.put("LEFT", GLFW.GLFW_KEY_LEFT);
        KEY_MAP.put("RIGHT", GLFW.GLFW_KEY_RIGHT);
    }

    public static String keyName(int keyCode) {
        return KEY_MAP.entrySet().stream()
                .filter(e -> e.getValue() == keyCode)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("KEY_" + keyCode);
    }

    public static KeyArgumentType create() {
        return new KeyArgumentType();
    }

    @Override
    public Integer parse(StringReader reader) throws CommandSyntaxException {
        String name = reader.readUnquotedString().toUpperCase();
        Integer code = KEY_MAP.get(name);
        if (code == null) {
            throw new DynamicCommandExceptionType(
                    k -> Text.literal("Неизвестная клавиша: " + k)
            ).create(name);
        }
        return code;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(KEY_MAP.keySet(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("R", "F", "LSHIFT", "F5");
    }
}
