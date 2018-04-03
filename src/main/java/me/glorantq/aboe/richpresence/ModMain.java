package me.glorantq.aboe.richpresence;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLModDisabledEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import lombok.Getter;
import lombok.Setter;
import me.glorantq.aboe.richpresence.commands.DiscordShutdownCommand;
import me.glorantq.aboe.richpresence.commands.I18NToggleCommand;
import me.glorantq.aboe.richpresence.i18n.ResourceBundleWrapper;
import me.glorantq.aboe.richpresence.i18n.UTF8Control;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.stringtemplate.v4.ST;

import java.io.ByteArrayOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;


@Mod(modid = "aboe-discord", version = ModMain.MOD_VERSION, canBeDeactivated = true, acceptableRemoteVersions = "*")
public class ModMain {
    static final String MOD_VERSION = "1.6.1";
    private static final String GAME_VERSION = "1.7.10";

    private static ModMain INSTANCE;
    public static ModMain getInstance() {
        return INSTANCE;
    }

    private final Logger logger = LogManager.getLogger("ABOE-Discord");

    private final @Getter String serverListUrl = "https://raw.githubusercontent.com/ABitOfEverything/ABitOfEverythingConfigs/master/discord/official_servers.json";
    private final @Getter String clientId = "429671204169842708";

    private @Getter DiscordRPC discordRPC;
    private DiscordEventHandlers eventHandlers;
    private PlayState lastAppliedState = null;

    private @Getter Configuration configuration;
    private ResourceBundle bundle = null;
    private @Setter @Getter boolean i18nEnabled = false;

    private final List<String> officialServerAddresses = new ArrayList<>();

    @Mod.EventHandler
    public void onModPreInit(FMLPreInitializationEvent event) {
        configuration = new Configuration(event.getSuggestedConfigurationFile());
        configuration.load();

        i18nEnabled = configuration.getBoolean("i18nenabled", Configuration.CATEGORY_GENERAL, false, "");

        logger.info("Starting with i18n: {}", i18nEnabled);
    }

    @Mod.EventHandler
    public void onModInit(FMLInitializationEvent event) {
        INSTANCE = this;
        discordRPC = DiscordRPC.INSTANCE;

        String gameLanguage = Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode();
        applyLocale(gameLanguage);

        eventHandlers = new DiscordEventHandlers();
        eventHandlers.ready = new DiscordEventHandlers.OnReady() {
            @Override
            public void accept() {
                onDiscordReady();
            }
        };

        discordRPC.Discord_Initialize(clientId, eventHandlers, true, "");

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    discordRPC.Discord_RunCallbacks();
                    try {
                        Thread.sleep(2000);
                    } catch (Exception ignored) {
                        discordRPC.Discord_Shutdown();
                    }
                }
            }
        }, "DiscordUpdaterThread").start();

        try {
            URL listUrl = new URL(serverListUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) listUrl.openConnection();

            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            IOUtils.copy(urlConnection.getInputStream(), responseStream);

            String response = new String(responseStream.toByteArray(), "UTF-8");

            JSONObject jsonRoot = (JSONObject) new JSONParser().parse(response);
            JSONArray ipList = (JSONArray) jsonRoot.get("official_ips");

            for (Object o : ipList) {
                officialServerAddresses.add(o.toString());
            }

            logger.info("Loaded {} official servers!", officialServerAddresses.size());
        } catch (Exception e) {
            logger.error("Failed to get official server list!", e);
        }

        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);

        ClientCommandHandler.instance.registerCommand(new I18NToggleCommand());
        ClientCommandHandler.instance.registerCommand(new DiscordShutdownCommand());

        logger.info("Loaded ABOE-Discord!");
    }

    @Mod.EventHandler
    public void onModDisable(FMLModDisabledEvent event) {
        logger.info("Disconnecting from Discord...");

        discordRPC.Discord_ClearPresence();
        discordRPC.Discord_Shutdown();
    }

    @SubscribeEvent
    public void onSinglePlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        applyState(PlayState.SP);
        logger.info("Joined SP!");
    }

    @SubscribeEvent
    public void onMultiPlayerJoined(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        SocketAddress remoteSocketAddress = event.manager.getSocketAddress();

        if (!(remoteSocketAddress instanceof InetSocketAddress)) {
            return;
        }

        InetSocketAddress remoteAddress = (InetSocketAddress) remoteSocketAddress;
        InetAddress address = remoteAddress.getAddress();

        logger.info("Connected to MP/{}", address.getHostAddress());

        if (officialServerAddresses.contains(address.getHostAddress())) {
            applyState(PlayState.OFFICIAL_MP);
            logger.info("Joined official MP!");
        } else {
            applyState(PlayState.UNOFFICIAL_MP);
            logger.info("Joined unofficial MP!");
        }
    }

    @SubscribeEvent
    public void onMultiPlayerDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        logger.info("Player disconnected from MP!");
        applyState(PlayState.MENU);
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiMainMenu) {
            String gameLanguage = Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode();

            String bundleLanguage = bundle.getLocale().getLanguage() + "_" + bundle.getLocale().getCountry().toUpperCase();
            if (!gameLanguage.equalsIgnoreCase(bundleLanguage)) {
                logger.info("Game config changed, reloading language bundle! Was {}, new {}", bundleLanguage, gameLanguage);

                applyLocale(gameLanguage);
            }

            logger.info("Player is back to main menu!");
            applyState(PlayState.MENU);
        }
    }

    private void applyLocale(String gameLanguage) {
        applyLocale(gameLanguage, false);
    }

    public void applyLocale(String gameLanguage, boolean reapplyState) {
        if(!i18nEnabled) {
            gameLanguage = "en_GB";
        }

        String[] parts = gameLanguage.split("_");

        try {
            bundle = ResourceBundle.getBundle("languages/aboediscord_lang", new Locale(parts[0], parts[1]), getClass().getClassLoader(), new UTF8Control());
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.info("Applied language: {}", gameLanguage);

        if(reapplyState) {
            applyState(lastAppliedState);
        }
    }

    private void onDiscordReady() {
        logger.info("DiscordRPC Connected!");

        applyState(PlayState.MENU);
    }

    private void applyState(PlayState state) {
        lastAppliedState = state;

        if (state == null) {
            discordRPC.Discord_ClearPresence();
            return;
        }

        DiscordRichPresence discordRichPresence = new DiscordRichPresence();

        discordRichPresence.largeImageKey = state.getLargeKey();
        discordRichPresence.largeImageText = processLocalizedKey(state.getLargeTooltip(), state);

        discordRichPresence.smallImageKey = state.getSmallKey();
        discordRichPresence.smallImageText = processLocalizedKey(state.getSmallTooltip(), state);

        discordRichPresence.state = processLocalizedKey(state.getState(), state);
        discordRichPresence.details = processLocalizedKey(state.getDetails(), state);

        discordRichPresence.startTimestamp = System.currentTimeMillis() / 1000;

        discordRPC.Discord_UpdatePresence(discordRichPresence);

        logger.info("Updated presence to state: {}", state.name());
    }

    private String processLocalizedKey(String base, PlayState state) {
        if (bundle == null) {
            return base;
        }

        return processResourceTemplate(new ST(base), state).render();
    }

    private ST processResourceTemplate(ST stringTemplate, PlayState playState) {
        stringTemplate.add("resources", new ResourceBundleWrapper(bundle));

        String renderedTemplate = stringTemplate.render();
        stringTemplate = new ST(renderedTemplate);

        stringTemplate.add("username", Minecraft.getMinecraft().getSession().getUsername());
        stringTemplate.add("pack_version", MOD_VERSION);
        stringTemplate.add("mc_version", GAME_VERSION);
        stringTemplate.add("state", playState.name());
        stringTemplate.add("language", Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode());

        return stringTemplate;
    }
}
