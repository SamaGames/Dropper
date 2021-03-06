package net.samagames.dropper.events.listeners;

import net.samagames.api.SamaGamesAPI;
import net.samagames.dropper.GameType;
import net.samagames.dropper.events.PlayerAFKEvent;
import net.samagames.dropper.level.DropperLevel;
import net.samagames.dropper.level.gui.LevelCategorySelectorGUI;
import net.samagames.tools.Titles;
import net.samagames.tools.chat.ActionBarAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import net.samagames.dropper.Dropper;
import net.samagames.dropper.DropperPlayer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/*
 * This file is part of Dropper.
 *
 * Dropper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dropper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dropper.  If not, see <http://www.gnu.org/licenses/>.
 */
public class PlayerEventsListener implements Listener {

    /**
     * This is the listener of the events called by players.
     * @author Vialonyx
     */

    private Dropper game;
    public PlayerEventsListener(Dropper game) {
        this.game = game;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event){

        if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_AIR)){

            if(event.getItem() != null) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                ItemStack item = event.getItem();
                DropperPlayer gamePlayer = this.game.getRegisteredGamePlayers().get(player.getUniqueId());

                if (item.isSimilar(Dropper.ITEM_QUIT_LEVEL)) {

                    gamePlayer.neutralizePlayer(false);
                    player.getActivePotionEffects().clear();

                    if (gamePlayer.getActiveCooldown() == null) {
                        this.game.usualLevelLeave(player, false);

                    } else {
                        this.game.usualLevelLeave(player, true);
                    }

                } else if(item.isSimilar(Dropper.ITEM_QUIT_GAME)){
                    player.getActivePotionEffects().clear();
                    this.game.usualGameLeave(player);
                    gamePlayer.neutralizePlayer(false);
                } else if(item.isSimilar(Dropper.ITEM_MODE_FREE)) {
                    this.game.usualGameTypeUpdate(player, GameType.FREE);

                } else if(item.isSimilar(Dropper.ITEM_MODE_COMPETITION)){
                    this.game.usualGameTypeUpdate(player, GameType.COMPETITION);

                } else if(item.isSimilar(Dropper.ITEM_SELECTGUI)){
                    SamaGamesAPI.get().getGuiManager().openGui(player, new LevelCategorySelectorGUI(this.game.getInstance()));
                }

            }

        }
    }

    @EventHandler
    public void onPlayerHeldItem(PlayerItemHeldEvent event){

        Player player = event.getPlayer();
        ItemStack inHand = event.getPlayer().getInventory().getItem(event.getNewSlot());

        if(inHand != null){

            if(inHand.isSimilar(Dropper.ITEM_MODE_FREE)){
                ActionBarAPI.sendMessage(player.getUniqueId(), this.game.getItemsDescriptions().get(Dropper.ITEM_MODE_FREE));
            } else if(inHand.isSimilar(Dropper.ITEM_MODE_COMPETITION)){
                ActionBarAPI.sendMessage(player.getUniqueId(), this.game.getItemsDescriptions().get(Dropper.ITEM_MODE_COMPETITION));
            } else if(inHand.isSimilar(Dropper.ITEM_SELECTGUI)){
                ActionBarAPI.sendMessage(player.getUniqueId(), this.game.getItemsDescriptions().get(Dropper.ITEM_SELECTGUI));
            } else if(inHand.isSimilar(Dropper.ITEM_QUIT_LEVEL)){
                ActionBarAPI.sendMessage(player.getUniqueId(), this.game.getItemsDescriptions().get(Dropper.ITEM_QUIT_LEVEL));
            } else if(inHand.isSimilar(Dropper.ITEM_QUIT_GAME)) {
                ActionBarAPI.sendMessage(player.getUniqueId(), this.game.getItemsDescriptions().get(Dropper.ITEM_QUIT_GAME));
            }

        }

    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event){

        if(event.getEntity() instanceof Player){

            Player player = (Player) event.getEntity();
            DropperPlayer dpPlayer = this.game.getPlayer(player.getUniqueId());
            event.setCancelled(true);

            if(dpPlayer.getCurrentLevel() != null && player.getHealth() == 20){

                player.teleport(dpPlayer.getCurrentLevel().getPlayLocation());
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20*5, 4));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 50, 4));
                player.getInventory().clear();
                player.getInventory().setItem(5, Dropper.ITEM_QUIT_LEVEL);
                player.getInventory().setItem(3, Dropper.ITEM_QUIT_GAME);
                Titles.sendTitle(player, 20, 50, 20, "" + ChatColor.RED + ChatColor.BOLD + "W A S T E D !", "Le niveau va redémarrer dans quelques secondes...");

                new BukkitRunnable() {

                    DropperLevel old = dpPlayer.getCurrentLevel();

                    @Override
                    public void run() {

                        // Check if player hasn't use the quit button and restart the level.
                        if(player.getInventory().contains(Dropper.ITEM_QUIT_LEVEL)){
                            player.getInventory().clear();
                            //game.usualLevelLeave(player, false);
                            dpPlayer.neutralizePlayer(false);
                            //game.usualLevelJoin(player, game.getDropperLevel(old.getID() -1));
                        }

                    }
                }.runTaskLater(this.game.getInstance(),100);

                if (dpPlayer.getGameType().equals(GameType.COMPETITION))
                    this.game.usualGameLeave(player);

            }

        }

    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event){

        DropperPlayer dpPlayer = this.game.getPlayer(event.getPlayer().getUniqueId());
        Player player = event.getPlayer();

        if(dpPlayer.isNeutralized()){
            event.setCancelled(true);
        }

        if (player.getLocation().getBlock().getType().equals(org.bukkit.Material.STATIONARY_WATER) && dpPlayer.getCurrentLevel() != null){

            // Neutralize player and set his inventory.
            dpPlayer.neutralizePlayer(true);
            player.getInventory().clear();
            player.getInventory().setItem(3, Dropper.ITEM_QUIT_LEVEL);
            player.getInventory().setItem(5, Dropper.ITEM_QUIT_GAME);

            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20*5, 4));
            Titles.sendTitle(player, 20, 50, 20, "" + ChatColor.GREEN + ChatColor.BOLD + "Bien joué !", "Le niveau suivant va démarrer automatiquement...");

            if(dpPlayer.isOnTutorial()){
                this.game.usualGameLeave(player);
                dpPlayer.setPlayerInTutorial(false);
                return;
            }

            new BukkitRunnable() {


                @Override
                public void run() {

                    //check if player hasn't use the quit button and restart the level.
                    if(player.getInventory().contains(Dropper.ITEM_QUIT_LEVEL)){

                        DropperLevel old = dpPlayer.getCurrentLevel();
                        player.getInventory().clear();
                        game.usualLevelLeave(player, false);
                        dpPlayer.neutralizePlayer(false);

                        if(old.getID() + 1 <= game.getRegisteredLevels().size()+1) {
                            game.usualLevelJoin(player, game.getDropperLevel(old.getID()));
                        } else {
                            game.usualGameLeave(player);
                        }

                    }
                }

            }.runTaskLater(this.game.getInstance(),100);
        }
    }

    @EventHandler
    public void onPlayerAFK(PlayerAFKEvent event){

        if(this.game.getPlayer(event.getPlayer().getUniqueId()).getCurrentLevel() != null){
            this.game.usualGameLeave(event.getPlayer());
        }

    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event){
        event.getPlayer().teleport(this.game.getSpawn());
        this.game.getEffectManager().restoreDefaultEffects(event.getPlayer());
    }

    @EventHandler
    public void onPlayerSwap(PlayerSwapHandItemsEvent event){
        // Preventing players to swap item in hands.
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event){
        // Preventing players to drop anything.
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        // Preventing players to move items in inventory.
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        // Preventing players to place any block.
        event.setCancelled(true);
    }
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event){
        // Preventing players to break any block.
        event.setCancelled(true);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event){
        // Disabling food.
        event.setCancelled(true);
    }

}
