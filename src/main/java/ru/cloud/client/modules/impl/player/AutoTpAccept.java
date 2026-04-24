package ru.cloud.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import ru.cloud.Zenith;
import ru.cloud.base.events.impl.server.EventPacket;
import ru.cloud.client.modules.api.Category;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.api.ModuleAnnotation;
import ru.cloud.client.modules.api.setting.impl.BooleanSetting;

import java.util.Locale;

@ModuleAnnotation(name = "AutoTpAccept", category = Category.PLAYER, description = "Автоматически принимает запросы на телепортацию")
public final class AutoTpAccept extends Module {

    public static final AutoTpAccept INSTANCE = new AutoTpAccept();
    private AutoTpAccept() {}

    private static final String[] TP_MESSAGES = {
            "has requested to teleport",
            "has requested teleport",
            "просит телепортироваться",
            "хочет телепортироваться к вам",
            "просит к вам телепортироваться",
            "запрашивает телепорт к вам",
            "просит к вам телепортироваться",
            "запрос на телепортацию",
            "хочет телепортироваться",
            "tpa from",
            "tpahere from"
    };

    private final BooleanSetting friendOnly = new BooleanSetting("Только друзья", "Будет принимать запросы только от друзей", true);

    @EventTarget
    public void onPacket(EventPacket e) {
        if (mc.player == null || mc.world == null) return;
        if (!e.isReceive()) return;
        if (!(e.getPacket() instanceof GameMessageS2CPacket packet)) return;

        String text = packet.content().getString().toLowerCase(Locale.ROOT);

        boolean isTp = false;
        for (String msg : TP_MESSAGES) {
            if (text.contains(msg.toLowerCase(Locale.ROOT))) {
                isTp = true;
                break;
            }
        }
        if (!isTp) return;

        if (friendOnly.isEnabled()) {
            boolean isFriend = false;
            for (String friend : Zenith.getInstance().getFriendManager().getItems()) {
                if (text.contains(friend.toLowerCase(Locale.ROOT))) {
                    isFriend = true;
                    break;
                }
            }
            if (!isFriend) return;
        }

        mc.player.networkHandler.sendChatCommand("tpaccept");
    }
}
