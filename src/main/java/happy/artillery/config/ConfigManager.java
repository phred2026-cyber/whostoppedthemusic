package happy.artillery.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;

/**
 * Loads and saves the Happy Artillery config from config/happy-artillery.json.
 * Call {@link #load()} during mod initialisation before anything reads HAConstants.
 */
public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("happy-artillery");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static HappyArtilleryConfig instance;

    public static HappyArtilleryConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("happy-artillery.json");
        if (Files.exists(path)) {
            try (Reader r = Files.newBufferedReader(path)) {
                instance = GSON.fromJson(r, HappyArtilleryConfig.class);
                if (instance == null) instance = new HappyArtilleryConfig();
                LOGGER.info("[HappyArtillery] Config loaded from {}", path);
            } catch (IOException e) {
                LOGGER.warn("[HappyArtillery] Could not read config, using defaults: {}", e.getMessage());
                instance = new HappyArtilleryConfig();
            }
        } else {
            instance = new HappyArtilleryConfig();
            LOGGER.info("[HappyArtillery] No config found, writing defaults to {}", path);
        }
        save(); // always write so the file exists with all keys visible
    }

    public static void save() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("happy-artillery.json");
        try (Writer w = Files.newBufferedWriter(path)) {
            GSON.toJson(instance, w);
        } catch (IOException e) {
            LOGGER.error("[HappyArtillery] Failed to save config: {}", e.getMessage());
        }
    }
}
