package main.java.me.dniym.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import io.netty.util.internal.ThreadLocalRandom;
import main.java.me.dniym.IllegalStack;
import main.java.me.dniym.checks.BadPotionCheck;
import main.java.me.dniym.enums.Msg;
import main.java.me.dniym.enums.Protections;
import main.java.me.dniym.timers.fTimer;
import main.java.me.dniym.utils.Scheduler;

public class Listener114 implements Listener {

    final Logger LOGGER = LogManager.getLogger("IllegalStack/" + Listener114.class.getSimpleName());
    private static final BlockFace[] faces = {BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH};
    HashMap<UUID, Long> lastTrade = new HashMap<>();
    HashSet<Material> consumables = new HashSet<>();
    IllegalStack plugin;

    public Listener114(IllegalStack illegalStack) {
        plugin = illegalStack;

        Material[] consume = new Material[]{Material.APPLE, Material.BAKED_POTATO, Material.BEETROOT, Material.BEETROOT_SOUP, Material.BREAD, Material.CARROT,
                Material.CHORUS_FRUIT, Material.COOKED_CHICKEN, Material.COOKED_COD, Material.COOKED_MUTTON, Material.COOKED_PORKCHOP, Material.COOKED_SALMON,
                Material.COOKED_RABBIT, Material.COOKIE, Material.DRIED_KELP, Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.GOLDEN_CARROT,
                Material.MELON_SLICE, Material.MUSHROOM_STEW, Material.POISONOUS_POTATO, Material.POTATO, Material.PUFFERFISH, Material.PUMPKIN_PIE, Material.RABBIT_STEW,
                Material.BEEF, Material.CHICKEN, Material.COD, Material.MUTTON, Material.PORKCHOP, Material.RABBIT, Material.SALMON, Material.ROTTEN_FLESH,
                Material.SPIDER_EYE, Material.COOKED_BEEF, Material.SUSPICIOUS_STEW, Material.SWEET_BERRIES, Material.TROPICAL_FISH, Material.MILK_BUCKET,
                Material.POTION};
        consumables.addAll(Arrays.asList(consume));

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChestDispense(BlockDispenseArmorEvent e) {
        if (Protections.DisableChestsOnMobs.isEnabled(e.getBlock().getLocation())) {

            if (e.getItem().getType() == Material.CHEST) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void OnVillagerTransform(EntityTransformEvent e) {

        if (Protections.ZombieVillagerTransformChance.getIntValue() < 100 && !Protections.ZombieVillagerTransformChance.isDisabledInWorld(
                e.getEntity().getWorld())) {
            for (Entity ent : e.getTransformedEntities()) {

                if (ent instanceof ZombieVillager) {
                    if (Protections.ZombieVillagerTransformChance.getIntValue() <= 0) {
                        e.setCancelled(true);
                        return;
                    }

                    int roll = ThreadLocalRandom.current().nextInt(1, 100);

                    if (roll > Protections.ZombieVillagerTransformChance.getIntValue()) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void OnVillagerInteract(PlayerInteractEntityEvent e) {

        if (Protections.PreventVillagerSwimExploit.isEnabled(e.getPlayer())) {
            if (e.getRightClicked() != null && e.getRightClicked() instanceof Villager && e.getRightClicked() instanceof Merchant) {
                boolean cancel = false;

                Villager v = (Villager) e.getRightClicked();

                if (v.getLocation().getBlock().getType() == Material.WATER) {
                    cancel = true;
                }
                Block down = v.getLocation().getBlock();
                if (down.getBlockData() instanceof Waterlogged) {
                    Waterlogged wl = (Waterlogged) down.getBlockData();
                    if (wl.isWaterlogged()) {
                        cancel = true;
                    }
                }
                if (cancel) {
                    e.setCancelled(true);
                    e.getPlayer().sendMessage(Msg.PlayerSwimExploitMsg.getValue());

                }
            }
        }

    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {


        if (Protections.SilkTouchBookExploit.isEnabled(e.getPlayer())) {
            Player p = e.getPlayer();
            if (p != null && p.getInventory().getItemInMainHand() != null) {
                ItemStack is = p.getInventory().getItemInMainHand();
                if (is.getType() == Material.ENCHANTED_BOOK) {
                    EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta) is.getItemMeta();
                    for (Enchantment enchantment : enchantmentStorageMeta.getStoredEnchants().keySet()) {
                        if (enchantment.equals(Enchantment.SILK_TOUCH)) {
                            if (e.getBlock().getType() == Material.CHEST || e.getBlock().getType() == Material.TRAPPED_CHEST) {
                                return;
                            }

                            e.setCancelled(true);
                            fListener.getLog().append2(Msg.SilkTouchBookBlocked.getValue(p, e.getBlock().getType().name()));

                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void LockMerchantTrades(InventoryOpenEvent e) {

        if (Protections.PreventCommandsInBed.isEnabled(e.getPlayer()) && e.getPlayer().isSleeping()) {
            e.setCancelled(true);
            return;
        }
        if (!Protections.VillagerTradeCheesing.isEnabled(e.getPlayer())) {
            return;
        }
        if (e.getInventory() instanceof MerchantInventory mi) {

            if (mi.getHolder() instanceof WanderingTrader) {
                return;
            }
            if (mi.getHolder() instanceof Villager) {
                Villager v = (Villager) mi.getHolder();

                if (v.getVillagerExperience() == 0) {
                    v.setVillagerExperience(1);
                }

            }
        }
    }

    @EventHandler
    public void PreventVillagerRestock(VillagerReplenishTradeEvent e) {
        if (!Protections.VillagerTradeCheesing.isEnabled(e.getEntity().getWorld())) {
            return;
        }
        if (Protections.VillagerRestockTime.getIntValue() <= 0 || !fListener.getIs114()) {
            return;
        }
        //if the villager is not in the list, add them with a new empty timestamp.
        if (!lastTrade.containsKey(e.getEntity().getUniqueId())) {
            lastTrade.put(e.getEntity().getUniqueId(), 0L);
        }
        if (System.currentTimeMillis() >= lastTrade.get(e.getEntity().getUniqueId())) {
            //timestamp is either empty or has expired, allow a restock.
            lastTrade.put(
                    e.getEntity().getUniqueId(),
                    System.currentTimeMillis() + 60000L * Protections.VillagerRestockTime.getIntValue()
            );
        } else {
            //timestamp has not expired do not allow restocking.
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHandSwap(PlayerSwapHandItemsEvent e) {
        if (!Protections.PreventFoodDupe.isEnabled(e.getPlayer())) {
            return;
        }
        ItemStack item1 = e.getMainHandItem();
        ItemStack item2 = e.getOffHandItem();
        if (item1 == null || item2 == null) {
            return;
        }
        if (consumables.contains(item1.getType()) && consumables.contains(item2.getType()) && item1.getType() == item2.getType()) {
            if (item1.getAmount() == 1 && item2.getAmount() == 1) {
                return;
            }
            e.getPlayer().getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            e.getPlayer().getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            e.setCancelled(true);
            Scheduler.runTaskLater(IllegalStack.getPlugin(), () -> {
                e.getPlayer().getInventory().setItemInMainHand(item1);
                e.getPlayer().getInventory().setItemInOffHand(item2);
            }, 3, e.getPlayer());
        }
    }

    @EventHandler
    public void onPortal(PortalCreateEvent event) {
        if (Protections.PreventBedrockDestruction.isEnabled(event.getWorld())) {

            for (BlockState b : event.getBlocks()) {
                if (fListener.getUnbreakable().contains(b.getType())) {
                    //Blocking breaking of unbreakable blocks.
                    LOGGER.info("Portal tried to break an unbreakable block: {}", b.getType().name());
                    event.setCancelled(true);
                    break;
                }
                if (b.getY() > b.getWorld().getMaxHeight()) {
                    //Blocking portals spawning at world height limit, preventing from https://i.imgur.com/mqAXdpU.png
                    event.setCancelled(true);
                    break;
                }
            }
        }


    }

    @EventHandler
    public void onPearlHit(ProjectileHitEvent e) {

        if (Protections.PreventPearlGlassPhasing.isEnabled(e.getEntity().getLocation())) {
            if (!(e.getEntity() instanceof EnderPearl) || e.getHitBlock() == null || !(e
                    .getEntity()
                    .getShooter() instanceof Player) || e.getHitBlockFace() != BlockFace.UP) {
                return;
            }
            for (BlockFace face : faces) {
                Block next = e.getHitBlock().getRelative(face);
                Block above = next.getRelative(BlockFace.UP);
                Player p = ((Player) e.getEntity().getShooter());
                if (!fListener.getAirBlocks().contains(above.getType())) {
                    if (!fListener.getInstance().getGlassBlocks().contains(above.getType())) {
                        continue;
                    }
                    Location l = e.getHitBlock().getRelative(e.getHitBlockFace()).getLocation().add(0.5, 0.5, 0.5);
                    fListener.getInstance().getTeleGlitch().put(p.getUniqueId(), l);
                }
            }
        }
    }

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent e) {
        if (!Protections.PreventFoodDupe.isEnabled(e.getPlayer())) {
            return;
        }
        ItemStack item1 = e.getPlayer().getInventory().getItem(e.getPreviousSlot());
        ItemStack item2 = e.getPlayer().getInventory().getItem(e.getNewSlot());
        if (item1 == null || item2 == null) {
            return;
        }

        if (consumables.contains(item1.getType()) && consumables.contains(item2.getType()) && item1.getType() == item2.getType()) {
            if (item1.getAmount() == 1 && item2.getAmount() == 1) {
                return;
            }
            int tSlot = e.getNewSlot();
            ItemStack tFood = e.getPlayer().getInventory().getItem(tSlot);
            if (tFood == null) {
                tSlot = e.getPreviousSlot();
                tFood = e.getPlayer().getInventory().getItem(tSlot);
            }
            final int slot = tSlot;
            final ItemStack food = tFood;
            e.getPlayer().getInventory().setItem(slot, new ItemStack(Material.AIR));
            Scheduler.runTaskLater(
                    IllegalStack.getPlugin(),
                    () -> e.getPlayer().getInventory().setItem(slot, food),
                    3,
                    e.getPlayer()
            );
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        if (e.getEntity() instanceof FishHook) {
            return;
        }
        if (Protections.PreventProjectileExploit2.isEnabled(e.getEntity().getLocation())) {
            fTimer.trackProjectile(e.getEntity());
        }
        if (Protections.PreventInvalidPotions.isEnabled() && e.getEntity() != null) {
            e.setCancelled(BadPotionCheck.isInvalidPotion(e.getEntity()));
        }

    }

    @EventHandler// (ignoreCancelled = false, priority=EventPriority.LOWEST)
    public void onFallingBlockSpawn(EntitySpawnEvent e) {
        if (e.getEntity() instanceof Projectile && !(e.getEntity() instanceof FishHook) && Protections.PreventProjectileExploit2.isEnabled(
                e.getEntity().getLocation())) {
            fTimer.trackProjectile((Projectile) e.getEntity());
        }

        if (!(e.getEntity() instanceof FallingBlock) || !Protections.PreventVibratingBlocks.isEnabled(e.getLocation())) {
            return;
        }

        fTimer.trackBlock((FallingBlock) e.getEntity());

    }


}
