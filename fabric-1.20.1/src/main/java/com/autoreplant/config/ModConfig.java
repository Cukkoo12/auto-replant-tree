package com.autoreplant.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean sneakToDisable = true;
    public boolean requireSapling = true;
    public Map<String, Boolean> trees = new HashMap<>();

    public ModConfig() {
        trees.put("oak", true);
        trees.put("spruce", true);
        trees.put("birch", true);
        trees.put("jungle", true);
        trees.put("acacia", true);
        trees.put("dark_oak", true);
        trees.put("mangrove", true);
        trees.put("cherry", true);
        trees.put("crimson", true);
        trees.put("warped", true);
    }

    public boolean isEnabled(String treeType) {
        return trees.getOrDefault(treeType, true);
    }

    public static ModConfig load(Path path) {
        try {
            if (Files.exists(path)) {
                return GSON.fromJson(Files.readString(path), ModConfig.class);
            }
        } catch (Exception e) { /* ignore */ }
        ModConfig config = new ModConfig();
        config.save(path);
        return config;
    }

    public void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this));
        } catch (Exception e) { /* ignore */ }
    }
}
