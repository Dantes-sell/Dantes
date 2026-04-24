package ru.cloud.base.autobuy.enchantes.container;

import ru.cloud.base.autobuy.enchantes.Enchant;
import ru.cloud.base.autobuy.enchantes.custom.EnchantCustom;
import ru.cloud.base.autobuy.enchantes.minecraft.EnchantVanilla;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnchantContainer {
    public static final Map<String, String> ENCHANT_MAP = new HashMap<>();

    static {
        ENCHANT_MAP.put("oxidation", "Окисление");
        ENCHANT_MAP.put("detection", "Текст");
        ENCHANT_MAP.put("poison", "Яд");
        ENCHANT_MAP.put("vampirism", "Вампиризм");
        ENCHANT_MAP.put("skilled", "Текст");
        ENCHANT_MAP.put("smelting", "Автоплавка");
        ENCHANT_MAP.put("magnet", "Текст");
        ENCHANT_MAP.put("pinger", "Пингер");
        ENCHANT_MAP.put("web", "Текст");
        ENCHANT_MAP.put("buldozing", "Текст");
        ENCHANT_MAP.put("pulling", "Текст");
        ENCHANT_MAP.put("stupor", "Текст");
        ENCHANT_MAP.put("demolishing", "Разрушение");
        ENCHANT_MAP.put("returning", "Текст");
        ENCHANT_MAP.put("scout", "Текст");

        ENCHANT_MAP.put("minecraft:protection", "Текст");
        ENCHANT_MAP.put("minecraft:fire_protection", "Текст");
        ENCHANT_MAP.put("minecraft:feather_falling", "Текст");
        ENCHANT_MAP.put("minecraft:blast_protection", "Текст");
        ENCHANT_MAP.put("minecraft:projectile_protection", "Текст");
        ENCHANT_MAP.put("minecraft:thorns", "Текст");
        ENCHANT_MAP.put("minecraft:soul_speed", "Текст");

        ENCHANT_MAP.put("minecraft:respiration", "Подводное дыхание");
        ENCHANT_MAP.put("minecraft:depth_strider", "Текст");
        ENCHANT_MAP.put("minecraft:aqua_affinity", "Текст");
        ENCHANT_MAP.put("minecraft:frost_walker", "Ледоход");

        ENCHANT_MAP.put("minecraft:sharpness", "Текст");
        ENCHANT_MAP.put("minecraft:smite", "Небесная кара");
        ENCHANT_MAP.put("minecraft:bane_of_arthropods", "Текст");
        ENCHANT_MAP.put("minecraft:knockback", "Текст");
        ENCHANT_MAP.put("minecraft:fire_aspect", "Текст");
        ENCHANT_MAP.put("minecraft:looting", "Текст");
        ENCHANT_MAP.put("minecraft:sweeping_edge", "Текст");

        ENCHANT_MAP.put("minecraft:efficiency", "Текст");
        ENCHANT_MAP.put("minecraft:silk_touch", "Текст");
        ENCHANT_MAP.put("minecraft:unbreaking", "Текст");
        ENCHANT_MAP.put("minecraft:fortune", "Удача");
        ENCHANT_MAP.put("minecraft:mending", "Текст");
        ENCHANT_MAP.put("minecraft:impaling", "Текст");

        ENCHANT_MAP.put("minecraft:power", "Сила");
        ENCHANT_MAP.put("minecraft:punch", "Текст");
        ENCHANT_MAP.put("minecraft:flame", "Воспламенение");
        ENCHANT_MAP.put("minecraft:infinity", "Текст");
        ENCHANT_MAP.put("minecraft:piercing", "Текст");
        ENCHANT_MAP.put("minecraft:multishot", "Текст");
        ENCHANT_MAP.put("minecraft:quick_charge", "Текст");

        ENCHANT_MAP.put("minecraft:riptide", "Замедление");
        ENCHANT_MAP.put("minecraft:loyalty", "Текст");
        ENCHANT_MAP.put("minecraft:channeling", "Текст");

        ENCHANT_MAP.put("minecraft:luck_of_the_sea", "Текст");
        ENCHANT_MAP.put("minecraft:lure", "Приманка");

        ENCHANT_MAP.put("minecraft:binding_curse", "Текст");
        ENCHANT_MAP.put("minecraft:vanishing_curse", "Текст");
    }


    public static List<Enchant> parse(String input) {
        List<Enchant> enchants = new ArrayList<>();
        String trimmed = input.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        Pattern pattern = Pattern.compile(
                "(EnchantCustom|EnchantVanilla) \\[checked=([^,]+), level=(\\d+)]"
        );

        Matcher matcher = pattern.matcher(trimmed);
        while (matcher.find()) {
            String className = matcher.group(1);
            String checked = matcher.group(2);
            int level = Integer.parseInt(matcher.group(3));

            String name = ENCHANT_MAP.getOrDefault(checked,checked);

            Enchant enchant;
            if (className.equals("EnchantCustom")) {
                enchant = new EnchantCustom(name, checked, level);
            } else {
                enchant = new EnchantVanilla(name, checked, level);
            }

            enchants.add(enchant);
        }

        return enchants;
    }
}
