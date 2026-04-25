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
        ENCHANT_MAP.put("detection", "Detection");
        ENCHANT_MAP.put("poison", "Яд");
        ENCHANT_MAP.put("vampirism", "Вампиризм");
        ENCHANT_MAP.put("skilled", "Skilled");
        ENCHANT_MAP.put("smelting", "Автоплавка");
        ENCHANT_MAP.put("magnet", "Magnet");
        ENCHANT_MAP.put("pinger", "Пингер");
        ENCHANT_MAP.put("web", "Web");
        ENCHANT_MAP.put("buldozing", "Bulldozing");
        ENCHANT_MAP.put("pulling", "Pulling");
        ENCHANT_MAP.put("stupor", "Stupor");
        ENCHANT_MAP.put("demolishing", "Разрушение");
        ENCHANT_MAP.put("returning", "Returning");
        ENCHANT_MAP.put("scout", "Scout");

        ENCHANT_MAP.put("minecraft:protection", "Protection");
        ENCHANT_MAP.put("minecraft:fire_protection", "Fire Protection");
        ENCHANT_MAP.put("minecraft:feather_falling", "Feather Falling");
        ENCHANT_MAP.put("minecraft:blast_protection", "Blast Protection");
        ENCHANT_MAP.put("minecraft:projectile_protection", "Projectile Protection");
        ENCHANT_MAP.put("minecraft:thorns", "Thorns");
        ENCHANT_MAP.put("minecraft:soul_speed", "Soul Speed");

        ENCHANT_MAP.put("minecraft:respiration", "Подводное дыхание");
        ENCHANT_MAP.put("minecraft:depth_strider", "Depth Strider");
        ENCHANT_MAP.put("minecraft:aqua_affinity", "Aqua Affinity");
        ENCHANT_MAP.put("minecraft:frost_walker", "Ледоход");

        ENCHANT_MAP.put("minecraft:sharpness", "Sharpness");
        ENCHANT_MAP.put("minecraft:smite", "Небесная кара");
        ENCHANT_MAP.put("minecraft:bane_of_arthropods", "Bane of Arthropods");
        ENCHANT_MAP.put("minecraft:knockback", "Knockback");
        ENCHANT_MAP.put("minecraft:fire_aspect", "Fire Aspect");
        ENCHANT_MAP.put("minecraft:looting", "Looting");
        ENCHANT_MAP.put("minecraft:sweeping_edge", "Sweeping Edge");

        ENCHANT_MAP.put("minecraft:efficiency", "Efficiency");
        ENCHANT_MAP.put("minecraft:silk_touch", "Silk Touch");
        ENCHANT_MAP.put("minecraft:unbreaking", "Unbreaking");
        ENCHANT_MAP.put("minecraft:fortune", "Удача");
        ENCHANT_MAP.put("minecraft:mending", "Mending");
        ENCHANT_MAP.put("minecraft:impaling", "Impaling");

        ENCHANT_MAP.put("minecraft:power", "Сила");
        ENCHANT_MAP.put("minecraft:punch", "Punch");
        ENCHANT_MAP.put("minecraft:flame", "Воспламенение");
        ENCHANT_MAP.put("minecraft:infinity", "Infinity");
        ENCHANT_MAP.put("minecraft:piercing", "Piercing");
        ENCHANT_MAP.put("minecraft:multishot", "Multishot");
        ENCHANT_MAP.put("minecraft:quick_charge", "Quick Charge");

        ENCHANT_MAP.put("minecraft:riptide", "Замедление");
        ENCHANT_MAP.put("minecraft:loyalty", "Loyalty");
        ENCHANT_MAP.put("minecraft:channeling", "Channeling");

        ENCHANT_MAP.put("minecraft:luck_of_the_sea", "Luck of the Sea");
        ENCHANT_MAP.put("minecraft:lure", "Приманка");

        ENCHANT_MAP.put("minecraft:binding_curse", "Curse of Binding");
        ENCHANT_MAP.put("minecraft:vanishing_curse", "Curse of Vanishing");
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

