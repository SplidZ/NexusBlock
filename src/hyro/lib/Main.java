package hyro.lib;

import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import hyro.lib.commands.nexusblock;
import hyro.lib.listeners.BlockDestroy;
import hyro.lib.structures.Nexus;
import hyro.lib.utils.TabComplete;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static hyro.lib.utils.Utils.getLocation;

public class Main extends JavaPlugin {
    public static File file;
    public static Plugin instance;
    public static FileConfiguration fileConfig;
    public static HashMap<String, Nexus> nexuses = new HashMap<String, Nexus>();

    @Override
    public void onEnable() {
        HologramsAPI.getHolograms(this).forEach((hologram) -> {
            hologram.delete();
        });

        instance = this;

        saveDefaultConfig();
        createConfig();

        getLogger().info("§cNexusBlock by Hyro has been §aenabled §8(§c0.1.0§8)");

        loadBlocks();
        loadCommands();
        getServer().getPluginManager().registerEvents(new BlockDestroy(), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("§cNexusBlock by Hyro has been disabled §8(§c0.1.0§8)");

        nexuses.forEach((material, nexus) -> {
            nexus.hologram.delete();
            try {
                fileConfig.load(file);

                fileConfig.set("nexuses."+material+".health", nexus.health);
                fileConfig.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
        });
    }

    private void createConfig() {
        this.file = new File(getDataFolder(), "config.yml");
        if (!this.file.exists()) {
            this.file.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }

        this.fileConfig = (FileConfiguration)new YamlConfiguration();
        try {
            this.fileConfig.load(this.file);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {}
    }

    /**
     * Loaders
     */
    private void loadCommands() {
        this.getCommand("nexusblock").setTabCompleter(new TabComplete());
        this.getCommand("nexusblock").setExecutor((CommandExecutor) new nexusblock());
    }

    private static void loadBlocks() {
        ConfigurationSection section = fileConfig.getConfigurationSection("nexuses");
        section.getKeys(false).forEach(block -> {
            String path = "nexuses."+block;

            Material material = Material.getMaterial(block);
            if(material == null) {
                Bukkit.getLogger().warning("[NexusBlock] Bad material in " + block);
                return;
            }

            String name = fileConfig.getString(path+".name").replaceAll("&","§");
            String info = fileConfig.getString(path+".info").replaceAll("&","§");
            String respawn = fileConfig.getString(path+".respawn");

            String maxHealth = fileConfig.getString(path+".maxHealth");
            String healthBar = fileConfig.getString(path+".healthBar").replaceAll("&","§");
            Integer health = fileConfig.getInt(path+".health");

            Location location = getLocation(path+".location");
            List<String> rewards = fileConfig.getStringList(path+".rewards");

            if(material == Material.BEDROCK) location.getWorld().getBlockAt(location).setType(material);

            nexuses.put(block, new Nexus(
                    material, name, info, maxHealth, healthBar, health, respawn, location, rewards
            ));
        });
    }

    public static void reloadPlugin() {
        nexuses.forEach((material, nexus) -> {
            nexuses.remove(material);
            nexus.hologram.delete();
            try {
                fileConfig.load(file);

                fileConfig.set("nexuses."+material+".health", nexus.health);
                fileConfig.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidConfigurationException e) {
                e.printStackTrace();
            }
        });

        loadBlocks();
    }
}
