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

    public final String HealthName = "[lang].stat.modifier.health_rank[/lang]";

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Run task on next tick to ensure player data is loaded
        // Ejecuta la tarea en el siguiente tick para asegurarse de que los datos del jugador estén cargados
        Bukkit.getScheduler().runTask(RankHealthBonus.getInstance(), () -> applyHealthBonus(player));
    }

    private void applyHealthBonus(Player player) {
        Bukkit.getLogger().info("[RankHealthBonus] Player joined: " + player.getName());
        // Get LuckPerms user
        // Obtiene el usuario de LuckPerms
        User user = getLuckPermsUser(player);
        if (user == null) return;

        // Calculate total health bonus based on groups
        // Calcula el bonus total de salud según los grupos
        int bonus = calculateBonus(user, player);

        // Apply the calculated bonus to AuraSkills
        // Aplica el bonus calculado a AuraSkills
        applyBonus(player, bonus);
    }

    private User getLuckPermsUser(Player player) {
        LuckPerms lp = RankHealthBonus.getLuckPerms();
        if (lp == null) {
            Bukkit.getLogger().warning("[RankHealthBonus] LuckPerms API not found.");
            // API de LuckPerms no encontrada
            return null;
        }

        User user = lp.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            Bukkit.getLogger().warning("[RankHealthBonus] LuckPerms user not found for player " + player.getName());
            // Usuario de LuckPerms no encontrado para el jugador
        }
        return user;
    }

    private int calculateBonus(User user, Player player) {
        int bonus = 0;

        // Loop through configured groups in config.yml
        // Recorre los grupos configurados en config.yml
        for (String group : RankHealthBonus.getInstance().getConfig()
                .getConfigurationSection("rank-health").getKeys(false)) {

            int extraLife = RankHealthBonus.getInstance().getConfig().getInt("rank-health." + group);

            // Check if user has the group via inheritance nodes
            // Comprueba si el usuario tiene el grupo mediante nodos de herencia
            boolean hasGroup = user.getNodes().stream()
                    .filter(n -> n instanceof InheritanceNode)
                    .map(n -> ((InheritanceNode) n).getGroupName())
                    .anyMatch(g -> g.equalsIgnoreCase(group.replace("group.", "")));

            if (hasGroup) {
                bonus += extraLife; // sum the bonus / suma el bonus
                Bukkit.getLogger().info("[RankHealthBonus] Player " + player.getName() + " has group " + group +
                        ", adding bonus " + extraLife + ". Total bonus: " + bonus);
                // Log only when the player has the group
                // Loguea solo cuando el jugador tiene el grupo
            }
        }

        Bukkit.getLogger().info("[RankHealthBonus] Final total bonus for player " + player.getName() + ": " + bonus);
        // Log final total bonus / Log del bonus total final

        return bonus;
    }

    private void applyBonus(Player player, int bonus) {
        AuraSkillsApi aura = AuraSkillsApi.get();
        if (aura == null) {
            Bukkit.getLogger().warning("[RankHealthBonus] AuraSkills API not found. Bonus not applied");
            // API de AuraSkills no encontrada. Bonus no aplicado
            return;
        }

        SkillsUser userSkills = aura.getUser(player.getUniqueId());
        if (userSkills == null) {
            Bukkit.getLogger().warning("[RankHealthBonus] SkillsUser for player " + player.getName() + " is null. Bonus not applied");
            // SkillsUser es null. Bonus no aplicado
            return;
        }

        // Check existing modifier
        // Comprueba si ya existe un modificador
        StatModifier existing = userSkills.getStatModifiers().values().stream()
                .filter(m -> m.name().equals(HealthName))
                .findFirst()
                .orElse(null);

        if (existing != null && existing.value() == bonus) {
            // If value is the same, ignore
            // Si el valor es el mismo, ignorar
            Bukkit.getLogger().info("[RankHealthBonus] Bonus already applied for player " + player.getName() + ", skipping");
        } else {
            if (existing != null) {
                // Existing modifier exists but value is different, remove it
                // Existe un modificador pero el valor es distinto, eliminarlo
                Bukkit.getLogger().info("[RankHealthBonus] Existing bonus differs for player " + player.getName() + ", removing old modifier");
                userSkills.removeStatModifier(HealthName);
            } else {
                // No existing modifier, adding new one
                // No existe modificador, añadiendo nuevo
                Bukkit.getLogger().info("[RankHealthBonus] No existing bonus found for player " + player.getName() + ", adding new modifier");
            }

            // Add new health bonus
            // Añade nuevo bonus de salud
            userSkills.addStatModifier(new StatModifier(HealthName, Stats.HEALTH, bonus));
            Bukkit.getLogger().info("[RankHealthBonus] Applied " + bonus + " health to AuraSkills stat for player " + player.getName());
        }
    }
}