package com.wraith.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wraith.WraithClient;
import com.wraith.event.EventTarget;
import com.wraith.event.events.TickEvent;
import com.wraith.module.Module;
import com.wraith.module.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Profile-based JSON config, one file per profile under config/wraith/profiles.
 *
 * Format is intentionally flat and stringly-typed:
 *   { "modules": { "Zoom": { "enabled": true, "bind": 346, "settings": { "Distance": "4.0" } } } }
 * It stays diffable and a bad value can never brick a load - every parse is guarded.
 */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int AUTOSAVE_TICKS = 20 * 15;

    private final Path root;
    private final Path profiles;
    private String profile = "default";
    private boolean dirty;
    private int sinceSave;

    public ConfigManager() {
        root = FabricLoader.getInstance().getConfigDir().resolve("wraith");
        profiles = root.resolve("profiles");
        WraithClient.bus().register(this);
    }

    public Path root() { return root; }
    public List<String> profiles() {
        List<String> names = new ArrayList<>();
        try (var stream = Files.list(profiles)) {
            stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                String name = p.getFileName().toString();
                names.add(name.substring(0, name.length() - ".json".length()));
            });
        } catch (IOException ignored) {
        }
        if (names.isEmpty()) names.add("default");
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public String currentProfile() { return profile; }
    public void markDirty() { dirty = true; }

    public void readDefaults() {
        try {
            Files.createDirectories(profiles);
            Path marker = root.resolve("state.json");
            if (Files.exists(marker)) {
                JsonObject json = JsonParser.parseString(Files.readString(marker, StandardCharsets.UTF_8)).getAsJsonObject();
                if (json.has("profile")) profile = json.get("profile").getAsString();
            }
        } catch (Exception e) {
            WraithClient.log().warn("Could not read Wraith state, using profile 'default'", e);
        }
    }

    public void save() {
        JsonObject modules = new JsonObject();
        for (Module module : WraithClient.modules().all()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("enabled", module.isEnabled());
            entry.addProperty("bind", module.bind());
            entry.addProperty("extended", module.isExtended());
            JsonObject values = new JsonObject();
            for (Setting<?> setting : module.settings()) {
                try {
                    values.addProperty(setting.name(), setting.encode());
                } catch (Exception e) {
                    WraithClient.log().warn("Skipping unreadable setting {}/{}", module.name(), setting.name());
                }
            }
            entry.add("settings", values);
            entry.add("hud", WraithClient.hud().encode(module.name()));
            modules.add(module.name(), entry);
        }
        JsonObject json = new JsonObject();
        json.addProperty("version", WraithClient.VERSION);
        json.addProperty("profile", profile);
        json.add("modules", modules);

        try {
            Files.createDirectories(profiles);
            try (Writer writer = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
            try (Writer writer = Files.newBufferedWriter(root.resolve("state.json"), StandardCharsets.UTF_8)) {
                JsonObject state = new JsonObject();
                state.addProperty("profile", profile);
                GSON.toJson(state, writer);
            }
            dirty = false;
        } catch (IOException e) {
            WraithClient.log().error("Failed to write profile '{}'", profile, e);
        }
    }

    public void load(String name) {
        Path path = profiles.resolve(safe(name) + ".json");
        if (!Files.exists(path)) {
            profile = safe(name);
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            profile = safe(name);
            JsonObject modules = json.has("modules") ? json.getAsJsonObject("modules") : new JsonObject();
            for (Module module : WraithClient.modules().all()) {
                if (!modules.has(module.name())) continue;
                JsonObject entry = modules.getAsJsonObject(module.name());
                JsonObject values = entry.has("settings") ? entry.getAsJsonObject("settings") : new JsonObject();
                for (Setting<?> setting : module.settings()) {
                    if (values.has(setting.name())) setting.decode(values.get(setting.name()).getAsString());
                }
                WraithClient.hud().decode(module.name(), entry.get("hud"));
                // Apply toggles after settings so modules see their real values.
                module.setEnabled(entry.has("enabled") && entry.get("enabled").getAsBoolean());
                if (entry.has("bind")) module.setBind(entry.get("bind").getAsInt());
                if (entry.has("extended")) module.setExtended(entry.get("extended").getAsBoolean());
            }
            // Anything absent from the file must not stay stuck on from a previous profile.
            for (Module module : WraithClient.modules().all()) {
                if (!modules.has(module.name())) module.setEnabled(false);
            }
        } catch (Exception e) {
            WraithClient.log().error("Failed to read profile '{}' - defaults kept", name, e);
        }
    }

    public void setProfile(String name) {
        profile = safe(name);
        load(profile);
        WraithClient.modules().gui().flash("Profile: " + profile);
    }

    public void newProfile(String name) {
        profile = safe(name);
        save();
    }

    private Path file() { return profiles.resolve(safe(profile) + ".json"); }

    private static String safe(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }

    @EventTarget
    private void onTick(TickEvent event) {
        if (!dirty) { sinceSave = 0; return; }
        if (++sinceSave >= AUTOSAVE_TICKS) {
            sinceSave = 0;
            save();
        }
    }
}
