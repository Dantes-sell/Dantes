package ru.cloud.base.autobuy;

import lombok.Getter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import ru.cloud.base.autobuy.enchantes.Enchant;
import ru.cloud.base.autobuy.enchantes.container.EnchantContainer;
import ru.cloud.base.autobuy.item.EnchantItemBuy;
import ru.cloud.base.autobuy.item.ItemBuy;
import ru.cloud.base.autobuy.item.NbtItemBuy;
import ru.cloud.base.autobuy.item.SkinItemBuy;

import java.awt.*;
import java.util.*;
import java.util.List;

@Getter
public class AutoBuyManager {

    private ArrayList<ItemBuy> vanilla = new ArrayList<>();

    private ArrayList<ItemBuy> funtime = new ArrayList<>();
    private ArrayList<ItemBuy> hollyworld = new ArrayList<>();

    public AutoBuyManager() {
        {
            List<Enchant> crusherHelmetEnchants = EnchantContainer.parse("[EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:protection, level=5], EnchantVanilla [checked=minecraft:fire_protection, level=5], EnchantVanilla [checked=minecraft:projectile_protection, level=5], EnchantVanilla [checked=minecraft:respiration, level=3], EnchantVanilla [checked=minecraft:mending, level=1], EnchantVanilla [checked=minecraft:blast_protection, level=5], EnchantVanilla [checked=minecraft:aqua_affinity, level=1]]");
            ;

            EnchantItemBuy crusherHELMET = new EnchantItemBuy(Items.NETHERITE_HELMET.getDefaultStack(), "???? ?????????", ItemBuy.Category.FUNTIME);
            crusherHelmetEnchants.forEach(crusherHELMET::addEnchant);

            List<Enchant> crusherChestplateEnchants = EnchantContainer.parse("[EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:projectile_protection, level=5], EnchantVanilla [checked=minecraft:protection, level=5], EnchantVanilla [checked=minecraft:fire_protection, level=5], EnchantVanilla [checked=minecraft:blast_protection, level=5], EnchantVanilla [checked=minecraft:mending, level=1]]");
            EnchantItemBuy crusherCHESTPLATE = new EnchantItemBuy(Items.NETHERITE_CHESTPLATE.getDefaultStack(), "????????? ?????????", ItemBuy.Category.FUNTIME);
            crusherChestplateEnchants.forEach(crusherCHESTPLATE::addEnchant);


            EnchantItemBuy crusherLEGGINGS = new EnchantItemBuy(Items.NETHERITE_LEGGINGS.getDefaultStack(), "?????? ?????????", ItemBuy.Category.FUNTIME);
            crusherChestplateEnchants.forEach(crusherLEGGINGS::addEnchant);

            List<Enchant> crusherBootsEnchants = EnchantContainer.parse("[EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:soul_speed, level=3], EnchantVanilla [checked=minecraft:protection, level=5], EnchantVanilla [checked=minecraft:fire_protection, level=5], EnchantVanilla [checked=minecraft:depth_strider, level=3], EnchantVanilla [checked=minecraft:feather_falling, level=4], EnchantVanilla [checked=minecraft:projectile_protection, level=5], EnchantVanilla [checked=minecraft:blast_protection, level=5], EnchantVanilla [checked=minecraft:mending, level=1]]");

            EnchantItemBuy crusherBoots = new EnchantItemBuy(Items.NETHERITE_BOOTS.getDefaultStack(), "??????? ?????????", ItemBuy.Category.FUNTIME);
            crusherBootsEnchants.forEach(crusherBoots::addEnchant);


            List<Enchant> crusherSwordEnchants = EnchantContainer.parse("[EnchantCustom [checked=oxidation, level=2], EnchantCustom [checked=detection, level=3], EnchantCustom [checked=poison, level=3], EnchantCustom [checked=vampirism, level=2], EnchantCustom [checked=skilled, level=3], EnchantVanilla [checked=minecraft:looting, level=5], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:fire_aspect, level=2], EnchantVanilla [checked=minecraft:sweeping_edge, level=3], EnchantVanilla [checked=minecraft:smite, level=7], EnchantVanilla [checked=minecraft:sharpness, level=7], EnchantVanilla [checked=minecraft:bane_of_arthropods, level=7], EnchantVanilla [checked=minecraft:mending, level=1]]");

            EnchantItemBuy crusherSword = new EnchantItemBuy(Items.NETHERITE_SWORD.getDefaultStack(), "??? ?????????", ItemBuy.Category.FUNTIME);
            crusherSwordEnchants.forEach(crusherSword::addEnchant);

            List<Enchant> crusherPickaxeEnchants = EnchantContainer.parse("[EnchantCustom [checked=skilled, level=3], EnchantCustom [checked=smelting, level=1], EnchantCustom [checked=magnet, level=1], EnchantCustom [checked=pinger, level=1], EnchantCustom [checked=web, level=1], EnchantCustom [checked=buldozing, level=2], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:efficiency, level=10], EnchantVanilla [checked=minecraft:mending, level=1], EnchantVanilla [checked=minecraft:fortune, level=5]]");
            EnchantItemBuy crusherPickaxe = new EnchantItemBuy(Items.NETHERITE_PICKAXE.getDefaultStack(), "????? ?????????", ItemBuy.Category.FUNTIME);
            crusherPickaxeEnchants.forEach(crusherPickaxe::addEnchant);

            List<Enchant> crusherCrossbowEnchants = EnchantContainer.parse("[EnchantVanilla [checked=minecraft:unbreaking, level=3], EnchantVanilla [checked=minecraft:mending, level=1], EnchantVanilla [checked=minecraft:multishot, level=1], EnchantVanilla [checked=minecraft:piercing, level=5], EnchantVanilla [checked=minecraft:quick_charge, level=3]]");
            EnchantItemBuy crusherCrossbow = new EnchantItemBuy(Items.CROSSBOW.getDefaultStack(), "??????? ?????????", ItemBuy.Category.FUNTIME);
            crusherCrossbowEnchants.forEach(crusherCrossbow::addEnchant);

            List<Enchant> crusherTridentEnchants = EnchantContainer.parse("[EnchantCustom [checked=detection, level=3], EnchantCustom [checked=poison, level=3], EnchantCustom [checked=demolishing, level=1], EnchantCustom [checked=returning, level=1], EnchantCustom [checked=oxidation, level=2], EnchantCustom [checked=pulling, level=2], EnchantCustom [checked=stupor, level=3], EnchantCustom [checked=vampirism, level=2], EnchantCustom [checked=skilled, level=3], EnchantCustom [checked=scout, level=3], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:fire_aspect, level=2], EnchantVanilla [checked=minecraft:loyalty, level=3], EnchantVanilla [checked=minecraft:impaling, level=5], EnchantVanilla [checked=minecraft:channeling, level=1], EnchantVanilla [checked=minecraft:sharpness, level=7], EnchantVanilla [checked=minecraft:mending, level=1]]");

            EnchantItemBuy crusherTrident = new EnchantItemBuy(Items.TRIDENT.getDefaultStack(), "???????? ?????????", ItemBuy.Category.FUNTIME);
            crusherTridentEnchants.forEach(crusherTrident::addEnchant);
            ItemStack medikStak = Items.SPLASH_POTION.getDefaultStack();
            medikStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(new Color(214, 0, 191).getRGB()), List.of(), Optional.empty()
            ));
            NbtItemBuy medik = new NbtItemBuy(medikStak, "Зелье Медика", ItemBuy.Category.FUNTIME, "custom_potion_effects", "[{ambient:0b,amplifier:2b,duration:900,id:\"minecraft:health_boost\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:2b,duration:900,id:\"minecraft:regeneration\",show_icon:1b,show_particles:1b}]");

            ItemStack pobedilkaStak = Items.SPLASH_POTION.getDefaultStack();
            pobedilkaStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(new Color(11, 188, 4).getRGB()), List.of(), Optional.empty()
            ));
            NbtItemBuy pobedilka = new NbtItemBuy(pobedilkaStak, "?????????", ItemBuy.Category.FUNTIME, "custom_potion_effects", "[{ambient:0b,amplifier:1b,duration:3600,id:\"minecraft:health_boost\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:0b,duration:18000,id:\"minecraft:invisibility\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:1b,duration:1200,id:\"minecraft:regeneration\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:0b,duration:1200,id:\"minecraft:resistance\",show_icon:1b,show_particles:1b}]");

            ItemStack agentStak = Items.SPLASH_POTION.getDefaultStack();
            agentStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(new Color(246, 250, 78).getRGB()), List.of(), Optional.empty()
            ));
            NbtItemBuy agent = new NbtItemBuy(agentStak, "?????", ItemBuy.Category.FUNTIME, "custom_potion_effects", "[{ambient:0b,amplifier:0b,duration:18000,id:\"minecraft:fire_resistance\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:0b,duration:3600,id:\"minecraft:haste\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:0b,duration:18000,id:\"minecraft:invisibility\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:2b,duration:18000,id:\"minecraft:speed\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:2b,duration:6000,id:\"minecraft:strength\",show_icon:1b,show_particles:1b}]");

            ItemStack killerStak = Items.SPLASH_POTION.getDefaultStack();
            killerStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(new Color(135, 0, 0).getRGB()), List.of(), Optional.empty()
            ));
            NbtItemBuy killer = new NbtItemBuy(killerStak, "??????", ItemBuy.Category.FUNTIME, "custom_potion_effects", "[{ambient:0b,amplifier:0b,duration:3600,id:\"minecraft:resistance\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:3b,duration:1800,id:\"minecraft:strength\",show_icon:1b,show_particles:1b}]");

            ItemStack kislotaStak = Items.SPLASH_POTION.getDefaultStack();
            kislotaStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(new Color(164, 252, 76).getRGB()), List.of(), Optional.empty()
            ));
            NbtItemBuy kislota = new NbtItemBuy(kislotaStak, "???????", ItemBuy.Category.FUNTIME, "custom_potion_effects", "[{ambient:0b,amplifier:1b,duration:1000,id:\"minecraft:poison\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:3b,duration:1800,id:\"minecraft:slowness\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:2b,duration:1800,id:\"minecraft:weakness\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:4b,duration:600,id:\"minecraft:wither\",show_icon:1b,show_particles:1b}]");
            //  NbtItemBuy silka3 = new NbtItemBuy(Items.POTION.getDefaultStack(), "Option","Option", ItemBuy.Category.FUNTIME,"custom_potion_effects","[{ambient:1b,amplifier:2b,duration:3600,id:\"minecraft:strength\",show_icon:1b,show_particles:1b},{ambient:1b,amplifier:2b,duration:3600,id:\"minecraft:speed\",show_icon:1b,show_particles:1b}]");

            ItemStack regenkaStak = Items.SPLASH_POTION.getDefaultStack();
            regenkaStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(new Color(243, 59, 128).getRGB()), List.of(), Optional.empty()
            ));
            NbtItemBuy regenka = new NbtItemBuy(regenkaStak, "???????", ItemBuy.Category.FUNTIME, "custom_potion_effects", "[{ambient:1b,amplifier:1b,duration:1,id:\"minecraft:instant_health\",show_icon:1b,show_particles:1b},{ambient:1b,amplifier:0b,duration:600,id:\"minecraft:regeneration\",show_icon:1b,show_particles:1b}]");

            ItemStack otrigStak = Items.SPLASH_POTION.getDefaultStack();
            otrigStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(new Color(16737792).getRGB()), List.of(), Optional.empty()
            ));
            NbtItemBuy otrig = new NbtItemBuy(otrigStak, "??????", ItemBuy.Category.FUNTIME, "CustomPotionColor", "16737792");

            ItemStack ice_arrowStak = Items.TIPPED_ARROW.getDefaultStack();
            ice_arrowStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(65535), List.of(), Optional.empty()
            ));
            NbtItemBuy ice_arrow = new NbtItemBuy(ice_arrowStak, "Ледяная стрела", ItemBuy.Category.FUNTIME, "custom_potion_effects", "[{ambient:0b,amplifier:50b,duration:100,id:\"minecraft:slowness\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:0b,duration:120,id:\"minecraft:weakness\",show_icon:1b,show_particles:1b}]");
            ItemStack gowno_arrowStak = Items.TIPPED_ARROW.getDefaultStack();
            gowno_arrowStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(0), List.of(), Optional.empty()
            ));

            NbtItemBuy gowno_arrow = new NbtItemBuy(gowno_arrowStak, "Тёмная стрела", ItemBuy.Category.FUNTIME, "custom_potion_effects", "[{ambient:0b,amplifier:0b,duration:40,id:\"minecraft:blindness\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:0b,duration:100,id:\"minecraft:nausea\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:0b,duration:200,id:\"minecraft:weakness\",show_icon:1b,show_particles:1b}]");


            NbtItemBuy plast = new NbtItemBuy(Items.DRIED_KELP.getDefaultStack(), "?????", ItemBuy.Category.FUNTIME, "stratum", "1b");
            NbtItemBuy trapka = new NbtItemBuy(Items.NETHERITE_SCRAP.getDefaultStack(), "Трапка", ItemBuy.Category.FUNTIME, "trap", "1b");
            NbtItemBuy desoritation = new NbtItemBuy(Items.ENDER_EYE.getDefaultStack(), "???????", ItemBuy.Category.FUNTIME, "desorientation", "1b");
            NbtItemBuy yawka = new NbtItemBuy(Items.SUGAR.getDefaultStack(), "????", ItemBuy.Category.FUNTIME, "sheerdust", "1b");
            NbtItemBuy bogAura = new NbtItemBuy(Items.PHANTOM_MEMBRANE.getDefaultStack(), "???-????", ItemBuy.Category.FUNTIME, "godsaura", "1b");

            SkinItemBuy andromed = new SkinItemBuy("?????????", ItemBuy.Category.FUNTIME, "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NTEwODQzNywKICAicHJvZmlsZUlkIiA6ICIzMjNiYjlkYzkwZWU0Nzk5YjUxYzE3NjRmZDRhNjI3OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJOcGllIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzQ0ZmZlM2YzNThmMjA5YmFkOGZmZjRkYzQ4MjQ1ZDliYWYwYTAzMWIzYzFlZTZiNzU4NDYwYTMzOWIxNTE5ZTIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==");
            SkinItemBuy pandora = new SkinItemBuy("???????", ItemBuy.Category.FUNTIME, "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NTY2NTExNCwKICAicHJvZmlsZUlkIiA6ICJkNzJlNGJjZDIyZGI0NjQ4OTUxNTc0M2UyYTRmMWFjMCIsCiAgInByb2ZpbGVOYW1lIiA6ICJhdnZheSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84ZTUxZTY1ZWI0MDUyNzcyMzgyYzllNTA3YTU0YmRlZDQzZTM5Zjc1NWI1ZGRmNTViM2YzOTQ0M2NlZDQ2N2Y0IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=");
            SkinItemBuy titan = new SkinItemBuy("?????", ItemBuy.Category.FUNTIME, "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM1NDQ1NTE5MiwKICAicHJvZmlsZUlkIiA6ICJkOTcwYzEzZTM4YWI0NzlhOTY1OGM1ZDQ1MjZkMTM0YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJDcmltcHlMYWNlODUxMjciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFlOTY5ODQ1OGI3ODQxYzk2YWU0ZjI0ZWM4NGFlMDE3MjQxMDA2NDFjNTY0ZTJhN2IxODVmNDA2ZThlZDIzIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=");
            SkinItemBuy appolon = new SkinItemBuy("???????", ItemBuy.Category.FUNTIME, "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NjYyNTM0NywKICAicHJvZmlsZUlkIiA6ICJhMjk1ODZmYmU1ZDk0Nzk2OWZjOGQ4ZGE0NzlhNDNlZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJMZXZlMjQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjQxMTdiNjAxOGZlZjBkNTE1NjcyMTczZTNiMjZlNjYwZDY1MWU1ODc2YmE2ZDAzZTUzNDIyNzBjNDliZWM4MCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9");
            SkinItemBuy astrei = new SkinItemBuy("??????", ItemBuy.Category.FUNTIME, "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NTA2MjQwNywKICAicHJvZmlsZUlkIiA6ICJlMzcxMWU2Y2E0ZmY0NzA4YjY5ZjhiNGZlYzNhZjdhMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNckJ1cnN0IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzFhNWFhZGQ1MmE1ZmFiOTcwODgxNDUxYWRmNTZmYmI0OTNhMzU4NTZlYTk2ZjU0ZTMyZWVhNjYyZDc4N2VkMjAiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==");
            SkinItemBuy sirius = new SkinItemBuy("??????", ItemBuy.Category.FUNTIME, "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NjY2Mzg3NiwKICAicHJvZmlsZUlkIiA6ICI3NGEwMzQxNWY1OTI0ZTA4YjMyMGM2MmU1NGE3ZjJhYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNZXp6aXIiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDgxMzYzNWJkODZiMTcxYmJlMTQzYWQ3MWUwOTAyMjkyNjQ5Y2IzYWI4NDQwZWQwMGY4NWNhNmNhMzgyOTkzNiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9");
            SkinItemBuy himera = new SkinItemBuy("??????", ItemBuy.Category.FUNTIME, "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NjE4MTEwOSwKICAicHJvZmlsZUlkIiA6ICJiNzRiMGQzNTBkNTk0NTU4YmYyYjBlMDJlYmE4NjE4NCIsCiAgInByb2ZpbGVOYW1lIiA6ICJCcmFuZG9uYnBtMjg0IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzlmYWJlZWQ0MjRiMjUyYTg5NDVhNjQ0MmI0NjJkNWYzMTQ3MDFhODE2ZGEyZDBhNjljY2RmY2ZkNzQ2ZTU4OGUiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==");

            NbtItemBuy grani = new NbtItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "?????", ItemBuy.Category.FUNTIME, "AttributeModifiers", "?????");

            NbtItemBuy dedal = new NbtItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "?????", ItemBuy.Category.FUNTIME, "AttributeModifiers", "?????");

            NbtItemBuy triton = new NbtItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "??????", ItemBuy.Category.FUNTIME, "AttributeModifiers", "??????");
            NbtItemBuy garmon = new NbtItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "??????", ItemBuy.Category.FUNTIME, "AttributeModifiers", "??????");

            NbtItemBuy fenix = new NbtItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "??????", ItemBuy.Category.FUNTIME, "AttributeModifiers", "??????");

            NbtItemBuy ehidna = new NbtItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "??????", ItemBuy.Category.FUNTIME, "AttributeModifiers", "??????");

            NbtItemBuy krush = new NbtItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "????", ItemBuy.Category.FUNTIME, "AttributeModifiers", "????");
            NbtItemBuy karatel = new NbtItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "????????", ItemBuy.Category.FUNTIME, "AttributeModifiers", "????????");

            funtime.add(crusherHELMET);
            funtime.add(crusherCHESTPLATE);
            funtime.add(crusherLEGGINGS);
            funtime.add(crusherBoots);
            funtime.add(crusherSword);
            funtime.add(crusherPickaxe);
            funtime.add(crusherCrossbow);
            funtime.add(crusherTrident);

            funtime.add(medik);
            funtime.add(pobedilka);
            funtime.add(agent);
            funtime.add(killer);
            funtime.add(kislota);
            funtime.add(regenka);
            funtime.add(ice_arrow);
            funtime.add(gowno_arrow);
            funtime.add(otrig);

            funtime.add(plast);
            funtime.add(trapka);
            funtime.add(desoritation);
            funtime.add(yawka);
            funtime.add(bogAura);

            funtime.add(andromed);
            funtime.add(pandora);
            funtime.add(titan);
            funtime.add(appolon);
            funtime.add(astrei);
            funtime.add(sirius);
            funtime.add(himera);

            funtime.add(grani);
            funtime.add(dedal);
            funtime.add(triton);
            funtime.add(garmon);
            funtime.add(fenix);
            funtime.add(ehidna);
            funtime.add(krush);
            funtime.add(karatel);
        }

        vanilla.add(new ItemBuy(Items.ENCHANTED_GOLDEN_APPLE.getDefaultStack(), "Зачарованное золотое яблоко", ItemBuy.Category.ANY));
        vanilla.add(new ItemBuy(Items.GOLDEN_APPLE.getDefaultStack(), "Золотое яблоко", ItemBuy.Category.ANY));
        vanilla.add(new ItemBuy(Items.ENDER_PEARL.getDefaultStack(), "Эндер-жемчуг", ItemBuy.Category.ANY));
        vanilla.add(new ItemBuy(Items.CHORUS_FRUIT.getDefaultStack(), "Плод хоруса", ItemBuy.Category.ANY));
        vanilla.add(new ItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Тотем бессмертия", ItemBuy.Category.ANY));
        vanilla.add(new ItemBuy(Items.EXPERIENCE_BOTTLE.getDefaultStack(), "Пузырёк опыта", ItemBuy.Category.ANY));
        vanilla.add(new ItemBuy(Items.ELYTRA.getDefaultStack(), "Элитры", ItemBuy.Category.ANY));

        {

            List<Enchant> eternityHelmetEnchants = EnchantContainer.parse("[EnchantCustom [checked=enchantments:impenetrable-enchant-custom, level=1], EnchantCustom [checked=minecraft:aqua_affinity, level=1], EnchantCustom [checked=minecraft:blast_protection, level=5], EnchantCustom [checked=minecraft:fire_protection, level=5], EnchantCustom [checked=minecraft:mending, level=1], EnchantCustom [checked=minecraft:projectile_protection, level=5], EnchantCustom [checked=minecraft:protection, level=5], EnchantCustom [checked=minecraft:respiration, level=3], EnchantCustom [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:aqua_affinity, level=1], EnchantVanilla [checked=minecraft:blast_protection, level=5], EnchantVanilla [checked=minecraft:fire_protection, level=5], EnchantVanilla [checked=minecraft:respiration, level=3], EnchantVanilla [checked=minecraft:projectile_protection, level=5], EnchantVanilla [checked=minecraft:protection, level=5], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:mending, level=1]]");
            EnchantItemBuy eternityHELMET = new EnchantItemBuy(Items.NETHERITE_HELMET.getDefaultStack(), "Шлем eternity", ItemBuy.Category.HOLLYWORLD);
            eternityHelmetEnchants.forEach(eternityHELMET::addEnchant);

            List<Enchant> eternityChestplateEnchants = EnchantContainer.parse("[EnchantCustom [checked=enchantments:impenetrable-enchant-custom, level=1], EnchantCustom [checked=minecraft:blast_protection, level=5], EnchantCustom [checked=minecraft:fire_protection, level=5], EnchantCustom [checked=minecraft:mending, level=1], EnchantCustom [checked=minecraft:projectile_protection, level=5], EnchantCustom [checked=minecraft:protection, level=5], EnchantCustom [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:projectile_protection, level=5], EnchantVanilla [checked=minecraft:protection, level=5], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:blast_protection, level=5], EnchantVanilla [checked=minecraft:fire_protection, level=5], EnchantVanilla [checked=minecraft:mending, level=1]]");
            EnchantItemBuy eternityCHESTPLATE = new EnchantItemBuy(Items.NETHERITE_CHESTPLATE.getDefaultStack(), "Нагрудник eternity", ItemBuy.Category.HOLLYWORLD);
            eternityChestplateEnchants.forEach(eternityCHESTPLATE::addEnchant);


            EnchantItemBuy eternityLEGGINGS = new EnchantItemBuy(Items.NETHERITE_LEGGINGS.getDefaultStack(), "?????? Eternity", ItemBuy.Category.HOLLYWORLD);
            eternityChestplateEnchants.forEach(eternityLEGGINGS::addEnchant);

            List<Enchant> eternityBootsEnchants = EnchantContainer.parse("[EnchantCustom [checked=minecraft:blast_protection, level=5], EnchantCustom [checked=minecraft:depth_strider, level=3], EnchantCustom [checked=minecraft:feather_falling, level=4], EnchantCustom [checked=minecraft:fire_protection, level=5], EnchantCustom [checked=minecraft:mending, level=1], EnchantCustom [checked=minecraft:projectile_protection, level=5], EnchantCustom [checked=minecraft:protection, level=5], EnchantCustom [checked=minecraft:soul_speed, level=3], EnchantCustom [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:depth_strider, level=3], EnchantVanilla [checked=minecraft:blast_protection, level=5], EnchantVanilla [checked=minecraft:fire_protection, level=5], EnchantVanilla [checked=minecraft:projectile_protection, level=5], EnchantVanilla [checked=minecraft:protection, level=5], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:soul_speed, level=3], EnchantVanilla [checked=minecraft:feather_falling, level=4], EnchantVanilla [checked=minecraft:mending, level=1]]");
            EnchantItemBuy eternityBoots = new EnchantItemBuy(Items.NETHERITE_BOOTS.getDefaultStack(), "Ботинки eternity", ItemBuy.Category.HOLLYWORLD);
            eternityBootsEnchants.forEach(eternityBoots::addEnchant);


            List<Enchant> eternitySwordEnchants = EnchantContainer.parse("[EnchantCustom [checked=enchantments:critical-enchant-custom, level=2], EnchantCustom [checked=enchantments:destroyer-enchant-custom, level=2], EnchantCustom [checked=enchantments:rich-enchant-custom, level=1], EnchantCustom [checked=minecraft:bane_of_arthropods, level=7], EnchantCustom [checked=minecraft:fire_aspect, level=2], EnchantCustom [checked=minecraft:looting, level=5], EnchantCustom [checked=minecraft:mending, level=1], EnchantCustom [checked=minecraft:sharpness, level=7], EnchantCustom [checked=minecraft:smite, level=7], EnchantCustom [checked=minecraft:sweeping, level=3], EnchantCustom [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:sweeping_edge, level=3], EnchantVanilla [checked=minecraft:bane_of_arthropods, level=7], EnchantVanilla [checked=minecraft:looting, level=5], EnchantVanilla [checked=minecraft:sharpness, level=7], EnchantVanilla [checked=minecraft:fire_aspect, level=2], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:smite, level=7], EnchantVanilla [checked=minecraft:mending, level=1]]");
            EnchantItemBuy eternitySword = new EnchantItemBuy(Items.NETHERITE_SWORD.getDefaultStack(), "??? Eternity", ItemBuy.Category.HOLLYWORLD);
            eternitySwordEnchants.forEach(eternitySword::addEnchant);

            List<Enchant> eternityPickaxeEnchants = EnchantContainer.parse("[EnchantCustom [checked=enchantments:drill-enchant-custom, level=2], EnchantCustom [checked=enchantments:exp-enchant-custom, level=3], EnchantCustom [checked=enchantments:filter-enchant-custom, level=1], EnchantCustom [checked=enchantments:foundry-enchant-custom, level=1], EnchantCustom [checked=enchantments:internal-enchant-custom, level=1], EnchantCustom [checked=enchantments:magnet-enchant-custom, level=1], EnchantCustom [checked=minecraft:efficiency, level=10], EnchantCustom [checked=minecraft:fortune, level=5], EnchantCustom [checked=minecraft:mending, level=1], EnchantCustom [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:efficiency, level=10], EnchantVanilla [checked=minecraft:fortune, level=5], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:mending, level=1]]");
            EnchantItemBuy eternityPickaxe = new EnchantItemBuy(Items.NETHERITE_PICKAXE.getDefaultStack(), "Кирка eternity", ItemBuy.Category.HOLLYWORLD);
            eternityPickaxeEnchants.forEach(eternityPickaxe::addEnchant);

            List<Enchant> eternityCrossbowEnchants = EnchantContainer.parse("[EnchantCustom [checked=enchantments:stun-enchant-custom, level=2], EnchantCustom [checked=minecraft:multishot, level=1], EnchantCustom [checked=minecraft:piercing, level=5], EnchantCustom [checked=minecraft:quick_charge, level=3], EnchantCustom [checked=minecraft:unbreaking, level=3], EnchantVanilla [checked=minecraft:multishot, level=1], EnchantVanilla [checked=minecraft:piercing, level=5], EnchantVanilla [checked=minecraft:quick_charge, level=3], EnchantVanilla [checked=minecraft:unbreaking, level=3]]");
            EnchantItemBuy eternityCrossbow = new EnchantItemBuy(Items.CROSSBOW.getDefaultStack(), "??????? Eternity", ItemBuy.Category.HOLLYWORLD);
            eternityCrossbowEnchants.forEach(eternityCrossbow::addEnchant);

            List<Enchant> eternityTridentEnchants = EnchantContainer.parse("[EnchantCustom [checked=minecraft:impaling, level=5], EnchantCustom [checked=minecraft:looting, level=5], EnchantCustom [checked=minecraft:loyalty, level=3], EnchantCustom [checked=minecraft:mending, level=1], EnchantCustom [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:loyalty, level=3], EnchantVanilla [checked=minecraft:looting, level=5], EnchantVanilla [checked=minecraft:impaling, level=5], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:mending, level=1]]");
            EnchantItemBuy eternityTrident = new EnchantItemBuy(Items.TRIDENT.getDefaultStack(), "???????? Eternity", ItemBuy.Category.HOLLYWORLD);
            eternityTridentEnchants.forEach(eternityTrident::addEnchant);





            SkinItemBuy cerberusSphere = new SkinItemBuy("????? ???????", ItemBuy.Category.HOLLYWORLD, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjA5NWE3ZmQ5MGRhYTFiYmU3MDY5MDg5NzQwZTA1ZDBiZmM2NjI5NmVlM2M0MGVlNzFhNGUwYTY2MTZiMmJiYyJ9fX0=");
            SkinItemBuy fleshSphere = new SkinItemBuy("????? ?????", ItemBuy.Category.HOLLYWORLD, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzc0MDBlYTE5ZGJkODRmNzVjMzlhZDY4MjNhYzRlZjc4NmYzOWY0OGZjNmY4NDYwMjM2NmFjMjliODM3NDIyIn19fQ==");
            SkinItemBuy imortaliti = new SkinItemBuy("??????????", ItemBuy.Category.HOLLYWORLD, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODNlZDRjZTIzOTMzZTY2ZTA0ZGYxNjA3MDY0NGY3NTk5ZWViNTUzMDdmN2VhZmU4ZDkyZjQwZmIzNTIwODYzYyJ9fX0=");
             ItemStack golubSphere = new SkinItemBuy("", ItemBuy.Category.HOLLYWORLD, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGM5MzY1NjQyYzZlZGRjZmVkZjViNWUxNGUyYmM3MTI1N2Q5ZTRhMzM2M2QxMjNjNmYzM2M1NWNhZmJmNmQifX19").getItemStack();
            NbtItemBuy damageSphere = new NbtItemBuy(golubSphere,"????? ?????", ItemBuy.Category.HOLLYWORLD, "sphereEffect","{\"lvl\":3,\"nbtName\":\"hms-damage\"}");
            NbtItemBuy speedSphere = new NbtItemBuy(golubSphere,"????? ????????", ItemBuy.Category.HOLLYWORLD, "sphereEffect","{\"lvl\":3,\"nbtName\":\"hms-speed\"}");
            NbtItemBuy eternitySphere = new NbtItemBuy(golubSphere,"????? Eternity", ItemBuy.Category.HOLLYWORLD,"sphereEffect","{\"lvl\":2,\"nbtName\":\"hms-speed\"},{\"lvl\":2,\"nbtName\":\"hms-armor\"},{\"lvl\":2,\"nbtName\":\"hms-damage\"}");

            NbtItemBuy trapka = new NbtItemBuy(Items.POPPED_CHORUS_FRUIT.getDefaultStack(), "Трапка", ItemBuy.Category.HOLLYWORLD, "pyrotechnic-item", "ALTERNATIVE_TRAP");
            NbtItemBuy explosionTrapka = new NbtItemBuy(Items.PRISMARINE_SHARD.getDefaultStack(), "???????? ??????", ItemBuy.Category.HOLLYWORLD, "pyrotechnic-item", "EXPLOSIVE_TRAP");
            NbtItemBuy stan = new NbtItemBuy(Items.NETHER_STAR.getDefaultStack(), "????", ItemBuy.Category.HOLLYWORLD, "pyrotechnic-item", "STUN_STAR");
            NbtItemBuy explosionBum = new NbtItemBuy(Items.FIRE_CHARGE.getDefaultStack(), "???????? ???", ItemBuy.Category.HOLLYWORLD, "kringeItems", "ExplosiveStuff");
         //   NbtItemBuy bogAura = new NbtItemBuy(Items.PHANTOM_MEMBRANE.getDefaultStack(), "Option", ItemBuy.Category.HOLLYWORLD, "godsaura", "1b");

            hollyworld.add(eternityHELMET);
            hollyworld.add(eternityCHESTPLATE);
            hollyworld.add(eternityLEGGINGS);
            hollyworld.add(eternityBoots);
            hollyworld.add(eternitySword);
            hollyworld.add(eternityPickaxe);
            hollyworld.add(eternityCrossbow);
            hollyworld.add(eternityTrident);
            hollyworld.add(cerberusSphere);
            hollyworld.add(fleshSphere);
            hollyworld.add(imortaliti);
            hollyworld.add(damageSphere);
            hollyworld.add(speedSphere);
            hollyworld.add(eternitySphere);
            hollyworld.add(trapka);
            hollyworld.add(explosionTrapka);
            hollyworld.add(stan);
            hollyworld.add(explosionBum);
//            hollyworld.add(bogAura);


        }


    }


}


