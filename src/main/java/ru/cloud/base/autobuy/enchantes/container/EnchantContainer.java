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
        ENCHANT_MAP.put("detection", "???????????");
        ENCHANT_MAP.put("poison", "Яд");
        ENCHANT_MAP.put("vampirism", "Вампиризм");
        ENCHANT_MAP.put("skilled", "????????");
        ENCHANT_MAP.put("smelting", "Автоплавка");
        ENCHANT_MAP.put("magnet", "??????");
        ENCHANT_MAP.put("pinger", "Пингер");
        ENCHANT_MAP.put("web", "???????");
        ENCHANT_MAP.put("buldozing", "?????????");
        ENCHANT_MAP.put("pulling", "??????????");
        ENCHANT_MAP.put("stupor", "??????");
        ENCHANT_MAP.put("demolishing", "����������");
        ENCHANT_MAP.put("returning", "???????");
        ENCHANT_MAP.put("scout", "?????????");

        ENCHANT_MAP.put("minecraft:protection", "??????");
        ENCHANT_MAP.put("minecraft:fire_protection", "?????????????");
        ENCHANT_MAP.put("minecraft:feather_falling", "???????????");
        ENCHANT_MAP.put("minecraft:blast_protection", "Blast ??????");
        ENCHANT_MAP.put("minecraft:projectile_protection", "Projectile ??????");
        ENCHANT_MAP.put("minecraft:thorns", "????");
        ENCHANT_MAP.put("minecraft:soul_speed", "???????? ????");

        ENCHANT_MAP.put("minecraft:respiration", "Подводное дыхание");
        ENCHANT_MAP.put("minecraft:depth_strider", "????????? ??????");
        ENCHANT_MAP.put("minecraft:aqua_affinity", "??????? ? ?????");
        ENCHANT_MAP.put("minecraft:frost_walker", "Ледоход");

        ENCHANT_MAP.put("minecraft:sharpness", "???????");
        ENCHANT_MAP.put("minecraft:smite", "Небесная кара");
        ENCHANT_MAP.put("minecraft:bane_of_arthropods", "??? ?????????????");
        ENCHANT_MAP.put("minecraft:knockback", "??????");
        ENCHANT_MAP.put("minecraft:fire_aspect", "???????? ??????");
        ENCHANT_MAP.put("minecraft:looting", "??????");
        ENCHANT_MAP.put("minecraft:sweeping_edge", "??????? ??????");

        ENCHANT_MAP.put("minecraft:efficiency", "?????????????");
        ENCHANT_MAP.put("minecraft:silk_touch", "???????? ???????");
        ENCHANT_MAP.put("minecraft:unbreaking", "?????????");
        ENCHANT_MAP.put("minecraft:fortune", "Удача");
        ENCHANT_MAP.put("minecraft:mending", "???????");
        ENCHANT_MAP.put("minecraft:impaling", "??????????");

        ENCHANT_MAP.put("minecraft:power", "����");
        ENCHANT_MAP.put("minecraft:punch", "????????????");
        ENCHANT_MAP.put("minecraft:flame", "Воспламенение");
        ENCHANT_MAP.put("minecraft:infinity", "?????????????");
        ENCHANT_MAP.put("minecraft:piercing", "??????????");
        ENCHANT_MAP.put("minecraft:multishot", "??????? ???????");
        ENCHANT_MAP.put("minecraft:quick_charge", "??????? ???????????");

        ENCHANT_MAP.put("minecraft:riptide", "Замедление");
        ENCHANT_MAP.put("minecraft:loyalty", "????????");
        ENCHANT_MAP.put("minecraft:channeling", "???????????");

        ENCHANT_MAP.put("minecraft:luck_of_the_sea", "??????? ????");
        ENCHANT_MAP.put("minecraft:lure", "Приманка");

        ENCHANT_MAP.put("minecraft:binding_curse", "????????? ???????????");
        ENCHANT_MAP.put("minecraft:vanishing_curse", "????????? ??????");
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




