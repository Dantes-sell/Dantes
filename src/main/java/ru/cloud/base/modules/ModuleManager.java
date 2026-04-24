package ru.cloud.base.modules;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;


import ru.cloud.base.events.impl.input.EventKey;
import ru.cloud.client.modules.api.Module;
import ru.cloud.client.modules.impl.combat.*;
import ru.cloud.client.modules.impl.misc.*;
import ru.cloud.client.modules.impl.movement.*;
import ru.cloud.client.modules.impl.render.*;
import ru.cloud.client.modules.impl.combat.*;
import ru.cloud.client.modules.impl.misc.*;
import ru.cloud.client.modules.impl.movement.*;
import ru.cloud.client.modules.impl.player.FastBreak;
import ru.cloud.client.modules.impl.player.HitSound;
import ru.cloud.client.modules.impl.player.NoDelay;
import ru.cloud.client.modules.impl.player.NoPush;
import ru.cloud.client.modules.impl.player.AutoDodge;
import ru.cloud.client.modules.impl.player.AutoTpAccept;
import ru.cloud.client.modules.impl.player.ChestStealer;
import ru.cloud.client.modules.impl.player.ChatBulling;
import ru.cloud.client.modules.impl.player.ClickPearl;
import ru.cloud.client.modules.impl.player.ObsidianFarm;
import ru.cloud.client.modules.impl.render.*;
import ru.cloud.utility.interfaces.IMinecraft;
import ru.cloud.client.modules.impl.player.AutoTool;
import ru.cloud.client.modules.impl.player.AutoArmor;
import ru.cloud.client.modules.impl.player.Blink;


import java.util.*;

@Getter
public final class ModuleManager implements IMinecraft {

    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        init();
        EventManager.register(this);
    }

    private void init() {
        registerCombat();
        registerMovement();
        registerRender();
        registerPlayer();
        registerMisc();
    }

    private void registerCombat() {

        registerModule(AntiBot.INSTANCE);
        registerModule(Aura.INSTANCE);
        registerModule(AuraV2.INSTANCE);
        registerModule(MaceAura.INSTANCE);
        registerModule(AutoAnchor.INSTANCE);
        registerModule(AutoCart.INSTANCE);
        registerModule(AutoCrystal.INSTANCE);
        registerModule(CrystalOptimizer.INSTANCE);
        registerModule(AutoSwap.INSTANCE);

        registerModule(AutoTotem.INSTANCE);
        registerModule(ShiftTap.INSTANCE);
        registerModule(Velocity.INSTANCE);
        registerModule(TargetPearl.INSTANCE);
        registerModule(WebTrap.INSTANCE);
        registerModule(SyncTps.INSTANCE);
    }

    private void registerMovement() {

        registerModule(AutoSprint.INSTANCE);
        registerModule(AirStuck.INSTANCE);
        registerModule(CatFly.INSTANCE);
        registerModule(DragonFly.INSTANCE);
        registerModule(ElytraBooster.INSTANCE);
        registerModule(ElytraResolver.INSTANCE);
        registerModule(ElyrtaPredict.INSTANCE);
        registerModule(ElytraRecast.INSTANCE);
        registerModule(Fly.INSTANCE);
        registerModule(GrimGlide.INSTANCE);
        registerModule(GuiWalk.INSTANCE);
        registerModule(NoSlow.INSTANCE);
        registerModule(Speed.INSTANCE);
        registerModule(Strafe.INSTANCE);
        registerModule(Spider.INSTANCE);
        registerModule(ru.cloud.client.modules.impl.movement.Timer.INSTANCE);
        registerModule(WebIgnore.INSTANCE);
        registerModule(WaterSpeed.INSTANCE);
    }

    private void registerRender() {
        registerModule(Interface.INSTANCE);
        registerModule(HudBeta.INSTANCE);
        registerModule(AntiInvisible.INSTANCE);

        registerModule(Menu.INSTANCE);
        registerModule(NoRender.INSTANCE);
        registerModule(ArmorDurability.INSTANCE);
        registerModule(ChorusTeleportEsp.INSTANCE);
        registerModule(NameTags.INSTANCE);
        registerModule(Predictions.INSTANCE);
        registerModule(BlockESP.INSTANCE);
        registerModule(FireworkEsp.INSTANCE);
        registerModule(SwingAnimation.INSTANCE);
        registerModule(Crosshair.INSTANCE);
        registerModule(Arrows.INSTANCE);
        registerModule(ViewModel.INSTANCE);
        registerModule(WorldTweaks.INSTANCE);
        registerModule(CrystalDamageEsp.INSTANCE);
        registerModule(CustomParticle.INSTANCE);
        registerModule(EntityESP.INSTANCE);
        registerModule(ChinaHat.INSTANCE);
        registerModule(JumpCircle.INSTANCE);
        registerModule(Trails.INSTANCE);
        registerModule(TargetESP.INSTANCE);
        registerModule(InventoryAnimation.INSTANCE);
    }

    private void registerPlayer() {
        registerModule(AutoTool.INSTANCE);
        registerModule(AutoArmor.INSTANCE);
        registerModule(Blink.INSTANCE);
        registerModule(ClickPearl.INSTANCE);
        registerModule(NoDelay.INSTANCE);
        registerModule(FastBreak.INSTANCE);
        registerModule(NoPush.INSTANCE);
        registerModule(HitSound.INSTANCE);
        registerModule(AutoDodge.INSTANCE);
        registerModule(AutoTpAccept.INSTANCE);
        registerModule(ChestStealer.INSTANCE);
        registerModule(ChatBulling.INSTANCE);
        registerModule(ObsidianFarm.INSTANCE);
    }

    private void registerMisc() {
        registerModule(UnHook.INSTANCE);
        registerModule(ServerHelper.INSTANCE);
        registerModule(ElytraHelper.INSTANCE);
        registerModule(ItemScroller.INSTANCE);
        registerModule(ChatSpammer.INSTANCE);
        registerModule(ClickAction.INSTANCE);
        registerModule(FreeCam.INSTANCE);
        registerModule(CameraTweaks.INSTANCE);
        registerModule(AutoAuth.INSTANCE);
        registerModule(AutoDuels.INSTANCE);
        registerModule(AHHelper.INSTANCE);
        registerModule(AutoSbor.INSTANCE);
        registerModule(NoInteract.INSTANCE);
        registerModule(AutoAccept.INSTANCE);
        registerModule(AutoRespawn.INSTANCE);
        registerModule(NameProtect.INSTANCE);
        registerModule(ClanUpgrade.INSTANCE);
        registerModule(DiscordRPCModule.INSTANCE);

    }

    private void registerModule(Module module) {
        modules.add(module);
    }


    public Module getModule(String name) {
        return modules.stream()
                .filter(module -> module.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public Set<Module> getActiveModules() {
        Set<Module> active = new HashSet<>();
        for (Module module : modules) {
            if (module.isEnabled()) active.add(module);
        }
        return active;
    }


    @EventTarget
    public void onKey(EventKey event) {
        if (UnHook.UNHOOKED) return;
        if (mc.currentScreen != null || event.getAction() != GLFW.GLFW_PRESS) return;

        for (Module module : modules) {
            if (module.getKeyCode() == event.getKeyCode()
                    && module.getKeyCode() != GLFW.GLFW_KEY_UNKNOWN) {
                module.toggle();
            }
        }
    }
}

