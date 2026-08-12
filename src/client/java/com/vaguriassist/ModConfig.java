package com.vaguriassist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "vaguriassist.json");

	public String vk = "https://vk.ru/hanrasen";
	public String allowedNick = "Hanrasen";
	public boolean DEBUG_MODE = false;
	public boolean hudEnabled = true;
	public boolean autoUnfreeze = false;
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

	public static ModConfig INSTANCE = new ModConfig();

	public static ModConfig get() {
		return INSTANCE;
	}

	public static void load() {
		if (CONFIG_FILE.exists()) {
			try (FileReader reader = new FileReader(CONFIG_FILE)) {
				ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
				if (loaded != null) {
					INSTANCE = loaded;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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
