package rhb.ivanremoenunlago.listeners;

import dev.aurelium.auraskills.api.stat.StatModifier;
import dev.aurelium.auraskills.api.stat.Stats;
import dev.aurelium.auraskills.api.user.SkillsUser;
import dev.aurelium.auraskills.api.AuraSkillsApi;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import rhb.ivanremoenunlago.RankHealthBonus;

public class HealthListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getLogger().info("[RankHealthBonus] Player joined: " + player.getName()
                + " / Jugador conectado: " + player.getName());
        applyHealthBonus(player);
    }

    private void applyHealthBonus(Player player) {
        int bonus = 0;

        LuckPerms lp = RankHealthBonus.getLuckPerms();
        if (lp == null) {
            Bukkit.getLogger().warning("[RankHealthBonus] LuckPerms API not found! / API de LuckPerms no encontrada!");
            return;
        }

        User user = lp.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            Bukkit.getLogger().warning("[RankHealthBonus] LuckPerms user not loaded for player " + player.getName()
                    + " / Usuario de LuckPerms no cargado para jugador " + player.getName());
            return;
        }

        Bukkit.getLogger().info("[RankHealthBonus] Processing health bonus for player " + player.getName()
                + " / Procesando bonus de salud para jugador " + player.getName());

        // Loop through each configured group in config
        for (String group : RankHealthBonus.getInstance().getConfig()
                .getConfigurationSection("rank-health").getKeys(false)) {

            int extraLife = RankHealthBonus.getInstance().getConfig().getInt("rank-health." + group);
            String cleanGroup = group.replace("group.", "");

            // Check if user has the group
            boolean hasGroup = user.getNodes().stream()
                    .filter(n -> n instanceof InheritanceNode)
                    .map(n -> ((InheritanceNode) n).getGroupName())
                    .anyMatch(g -> g.equalsIgnoreCase(cleanGroup));

            Bukkit.getLogger().info("[RankHealthBonus] Checking group " + cleanGroup
                    + " (bonus " + extraLife + ") for player " + player.getName()
                    + " / Revisando grupo " + cleanGroup + " (bonus " + extraLife + ") para jugador " + player.getName()
                    + ": " + hasGroup);

            if (hasGroup) {
                bonus = Math.max(bonus, extraLife);
                Bukkit.getLogger().info("[RankHealthBonus] Bonus updated to " + bonus
                        + " for player " + player.getName() + " / Bonus actualizado a " + bonus + " para jugador " + player.getName());
            }
        }

        Bukkit.getLogger().info("[RankHealthBonus] Final bonus for player " + player.getName()
                + ": " + bonus + " / Bonus final para jugador " + player.getName() + ": " + bonus);

        AuraSkillsApi aura = AuraSkillsApi.get();
        if (aura != null) {
            SkillsUser userSkills = aura.getUser(player.getUniqueId());
            if (userSkills != null) {
                userSkills.removeStatModifier("rhb_bonus_health");
                userSkills.addStatModifier(new StatModifier("rhb_bonus_health", Stats.HEALTH, bonus));
                Bukkit.getLogger().info("[RankHealthBonus] Applied " + bonus
                        + " health to AuraSkills stat for player " + player.getName()
                        + " / Aplicado " + bonus + " de salud al stat de AuraSkills para jugador " + player.getName());
            } else {
                Bukkit.getLogger().warning("[RankHealthBonus] SkillsUser is null for player " + player.getName()
                        + " / SkillsUser es null para jugador " + player.getName());
            }
        } else {
            Bukkit.getLogger().warning("[RankHealthBonus] AuraSkills API not found! / API de AuraSkills no encontrada!");
        }
    }
}
