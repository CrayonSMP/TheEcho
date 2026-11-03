package de.tamashiimon.theecho;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.tamashiimon.theecho.models.Config;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLoader {

    private static final YAMLMapper MAPPER = YAMLMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE) // debugMode -> debug-mode
            .build();

    private ConfigLoader() {}

    public static Config load(JavaPlugin plugin) throws IOException {
        Path file = plugin.getDataFolder().toPath().resolve("config.yml");
        if (Files.notExists(file)) {
            plugin.saveResource("config.yml", false);
        }
        return MAPPER.readValue(file.toFile(), Config.class);
    }
}