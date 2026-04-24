package ru.cloud.client.screens.autobuy.items;

import ru.cloud.base.autobuy.item.ItemBuy;
import ru.cloud.client.screens.menu.settings.api.MenuSetting;

import java.util.List;

public abstract class ExtendAutoInventoryItem extends AutoInventoryItem {
    public ExtendAutoInventoryItem(ItemBuy itemBuy) {
        super(itemBuy);
    }
    public abstract List<MenuSetting> getEnchants();

}
