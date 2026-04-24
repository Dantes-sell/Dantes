package ru.cloud;

import by.saskkeee.annotations.CompileToNative;
import by.saskkeee.annotations.Entrypoint;
import by.saskkeee.annotations.vmprotect.CompileType;
import by.saskkeee.annotations.vmprotect.VMProtect;
import lombok.Getter;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import ru.cloud.base.account.AccountManager;
import ru.cloud.base.autobuy.AutoBuyManager;
import ru.cloud.base.mainmenu.MainMenuHandler;
import ru.cloud.base.discord.DiscordRPC;

import ru.cloud.base.comand.CommandManager;
import ru.cloud.base.config.ConfigManager;
import ru.cloud.base.filemanager.impl.FriendManager;
import ru.cloud.base.filemanager.impl.StaffManager;
import ru.cloud.base.modules.ModuleManager;
import ru.cloud.base.request.ScriptManager;
import ru.cloud.base.rotation.RotationManager;
import ru.cloud.base.theme.ThemeManager;
import ru.cloud.client.screens.autobuy.items.AutoInventoryBuyScreen;
import ru.cloud.client.screens.menu.MenuScreen;
import ru.cloud.utility.game.player.HealthTracker;
import ru.cloud.utility.game.server.ServerHandler;
import ru.cloud.base.notify.NotifyManager;
import ru.cloud.base.repository.RCTRepository;
import ru.cloud.utility.render.display.shader.DrawUtil;
import ru.cloud.utility.render.display.shader.GlProgram;
import ru.cloud.utility.waveycapes.WaveyCapesBase;

import java.io.File;

/*
    эта паста рвет во всю убивает нищету убивает и деееельта юзераааа бож че ты несешь какая дельта ты че совссем ебанулся???
    ��� ����� �� ������� ���� ��� �������� ��� ��� ��� ���
 */

@Getter
@Entrypoint
public enum Zenith {
    INSTANCE;

    public static final String NAME = "Dantes", VER = "Beta", TYPE = "DEV";
    private static final String MOD_ID = NAME.toLowerCase();
    public static final File DIRECTORY = new File(MinecraftClient.getInstance().runDirectory, Zenith.NAME);

    private AccountManager accountManager;
    private MainMenuHandler mainMenuHandler;
    private ModuleManager moduleManager;

    private ThemeManager themeManager;
    private MenuScreen menuScreen;
    private ScriptManager scriptManager;
    private AutoInventoryBuyScreen autoInventoryBuyScreen;
    private ServerHandler serverHandler;
    private HealthTracker healthTracker;
    private FriendManager friendManager;
    private StaffManager staffManager;
    private RotationManager rotationManager;
    private AutoBuyManager autoBuyManager;

    private NotifyManager notifyManager;
    private CommandManager commandManager;
    private ConfigManager configManager;
    private RCTRepository rctRepository;
    private DiscordRPC discordRPC;

    @CompileToNative
    @VMProtect(type = CompileType.ULTRA)
    public void init() {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> Zenith.getInstance().shutdown()));
        WaveyCapesBase.init();

        friendManager = new FriendManager();
        staffManager = new StaffManager();
        notifyManager = new NotifyManager();
        accountManager = new AccountManager();
        mainMenuHandler = new MainMenuHandler();
        serverHandler = new ServerHandler();
        healthTracker = new HealthTracker();
        rctRepository = new RCTRepository();
        themeManager = new ThemeManager();
        moduleManager = new ModuleManager();

        discordRPC = new DiscordRPC();

        rotationManager = new RotationManager();
        autoBuyManager = new AutoBuyManager();
        commandManager = new CommandManager();
        scriptManager = new ScriptManager();
        menuScreen = new MenuScreen();


        configManager = new ConfigManager(); //не двигать самый последний всегда
        menuScreen.initialize(); //������ ������������

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Zenith.id("after_shader_load");
            }

            @Override
            public void reload(ResourceManager manager) {
                GlProgram.loadAndSetupPrograms();
            }
        });
        DrawUtil.initializeShaders();

    }

    public void shutdown() {
        friendManager.save();
        staffManager.save();
        configManager.save();
        discordRPC.stopRPC();
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static Zenith getInstance() {
        return INSTANCE;
    }

    public RCTRepository getRCTRepository() {
        return rctRepository;
    }

}
