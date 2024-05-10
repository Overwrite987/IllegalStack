package main.java.me.dniym.listeners;

import main.java.me.dniym.IllegalStack;
import main.java.me.dniym.enums.Msg;
import main.java.me.dniym.enums.Protections;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class Listener113 implements Listener {

    private static final Logger LOGGER = LogManager.getLogger("IllegalStack/" + Listener113.class.getSimpleName());
    IllegalStack plugin;

    public Listener113(IllegalStack illegalStack) {
        plugin = illegalStack;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        LOGGER.info("Enabling 1.13+ Checks");

    }

    @EventHandler
    public void spawnerSpawnEvent(SpawnerSpawnEvent e) {

        if (Protections.SpawnerReset.isEnabled(e.getLocation())) {

            CreatureSpawner cs = e.getSpawner();
            EntityType et = e.getEntityType();
            if (Protections.ResetSpawnersOfTypeOnSpawn.isWhitelisted(et)) {
                e.setCancelled(true);
                EntityType oldType = cs.getSpawnedType();
                cs.setSpawnedType(EntityType.PIG);
                cs.setBlockData(cs.getBlockData());
                cs.update(true);
                fListener.getLog().append(
                        Msg.StaffMsgSpawnerOnSpawnReset.getValue(oldType.name(), e.getLocation()),
                        Protections.ResetSpawnersOfTypeOnSpawn
                );
            }
        }
    }

    @EventHandler
    public void spawnerChangeCheck(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player plr = event.getPlayer();
        if (Protections.PreventSpawnEggsOnSpawners.isEnabled(plr)) {
            ItemStack is = plr.getInventory().getItemInMainHand();
            if (is.getType().name().toLowerCase().contains("spawn_egg")) {
                Block blk = event.getClickedBlock();
                if (blk.getType() == Material.SPAWNER && !event.getPlayer().isOp()) {
                    plr.sendMessage(Msg.PlayerSpawnEggBlock.getValue());
                    event.setCancelled(true);

                } else if (blk.getType() == Material.SPAWNER) {
                    fListener.getLog().append(
                            Msg.StaffMsgChangedSpawnerType.getValue(plr, is.getType().name()),
                            Protections.PreventSpawnEggsOnSpawners
                    );
                }

            }
        }
    }

}
