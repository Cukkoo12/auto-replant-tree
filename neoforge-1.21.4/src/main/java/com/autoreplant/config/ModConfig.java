package com.autoreplant.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean sneakToDisable = true;
    public boolean requireSapling = true;
    public Map<String, Boolean> trees = new LinkedHashMap<>();

    public ModConfig() {
        trees.put("oak", true);
        trees.put("spruce", true);
        trees.put("birch", true);
        trees.put("jungle", true);
        trees.put("acacia", true);
        trees.put("dark_oak", true);
        trees.put("mangrove", true);
        trees.put("cherry", true);
        trees.put("pale_oak", true);
        trees.put("crimson", true);
        trees.put("warped", true);
    }

    public boolean isEnabled(String treeKey) {
        return trees.getOrDefault(treeKey, true);
    }

    public static ModConfig load(Path configPath) {
        try {
            if (Files.exists(configPath)) {
                return GSON.fromJson(Files.readString(configPath), ModConfig.class);
            }
        } catch (Exception ignored) {}
        ModConfig config = new ModConfig();
        config.save(configPath);
        return config;
    }

    public void save(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(this));
        } catch (Exception ignored) {}
    }
}
