package com.vaguriassist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "vaguriassist.json");

	public String vk = "https://vk.ru/hanrasen";
	public String allowedNick = "Hanrasen";
	public boolean hudEnabled = true;
	public boolean autoUnfreeze = false;
	public boolean autoWarpLogo = true;
	public boolean draggableBanWindow = true;
	public int banWindowX = -1;
	public int banWindowY = -1;
	public int checkWindowX = -1;
	public int checkWindowY = -1;
	public int hudX = -1;
	public int hudY = -1;
	public int timerX = -1;
	public int timerY = -1;
	public float hudScale = 1.0f;
	public float timerScale = 1.0f;
	public float banScale = 1.0f;

	public String apiToken = "";

	// Режим вноса проверок: "offline" (в txt) или "online" (в журнал через API)
	public String checkLogMode = "offline";

	public String toastSide = "left";
	public int toastHeight = 100;
	public int toastY = -1;
	public String toastStyle = "popup";
	public boolean draggableTimer = true;

	// Chat Tab System
	public List<ChatTabData> chatTabs = new ArrayList<>();
	public String activeTabName = "Все";
	public Set<String> blacklistedWords = new HashSet<>();
	public Set<String> blacklistedPlayers = new HashSet<>();
	public List<HighlightEntry> highlightEntries = new ArrayList<>();
	public boolean soundOnMention = true;
	public boolean soundOnPrivateMessage = true;
	public boolean chatHistoryEnabled = true;
	public int maxChatHistoryLines = 500;

	public static ModConfig INSTANCE = new ModConfig();

	public static ModConfig get() {
		return INSTANCE;
	}

	public static void load() {
		if (CONFIG_FILE.exists()) {
			try (FileReader reader = new FileReader(CONFIG_FILE)) {
				Type configType = new TypeToken<ModConfig>() {}.getType();
				ModConfig loaded = GSON.fromJson(reader, configType);
				if (loaded != null) {
					INSTANCE = loaded;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (INSTANCE.chatTabs == null) INSTANCE.chatTabs = new ArrayList<>();
		if (INSTANCE.blacklistedWords == null) INSTANCE.blacklistedWords = new HashSet<>();
		if (INSTANCE.blacklistedPlayers == null) INSTANCE.blacklistedPlayers = new HashSet<>();
		if (INSTANCE.highlightEntries == null) INSTANCE.highlightEntries = new ArrayList<>();
		save();
	}

	public static void save() {
		try {
			CONFIG_FILE.getParentFile().mkdirs();
			try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
				GSON.toJson(INSTANCE, writer);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
