package rhb.ivanremoenunlago;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.plugin.java.JavaPlugin;
import rhb.ivanremoenunlago.listeners.HealthListener;

public class RankHealthBonus extends JavaPlugin {

    private static RankHealthBonus instance;
    private static LuckPerms luckPerms;

    @Override
    public void onEnable() {
        instance = this;

        // Load LuckPerms API
        try {
            luckPerms = LuckPermsProvider.get();
            getLogger().info("[RankHealthBonus] LuckPerms API loaded successfully.");
        } catch (IllegalStateException e) {
            getLogger().warning("[RankHealthBonus] LuckPerms API not found!");
        }

        // Register listeners
        getServer().getPluginManager().registerEvents(new HealthListener(), this);

        // Save default config
        saveDefaultConfig();
    }

    public static RankHealthBonus getInstance() {
        return instance;
    }

    public static LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
