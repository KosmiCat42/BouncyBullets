package dev.zenqrt.bouncybullets.event.listeners;

import dev.zenqrt.bouncybullets.game.GameManager;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGame;
import dev.zenqrt.bouncybullets.item.GameItem;
import dev.zenqrt.bouncybullets.item.GameItems;
import dev.zenqrt.bouncybullets.item.items.guns.GunItem;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class GameItemListeners implements Listener {

    private final GameManager gameManager;

    public GameItemListeners(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Optional<BouncyBulletGame> gameOptional = this.gameManager.findPlayerGame(player.getUniqueId());

        if (event.getHand() != EquipmentSlot.HAND)
            return;

        if (gameOptional.isEmpty())
            return;

        ItemStack itemStack = event.getItem();

        if (itemStack == null)
            return;

        GameItem.findGameItemId(itemStack)
                .ifPresent(
                        gameItemId -> {
                            GameItem gameItem = GameItems.getAllItems().get(gameItemId);

                            gameItem.onInteract(gameOptional.get(), player, itemStack, event);
                        });
    }


    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Optional<BouncyBulletGame> gameOptional = this.gameManager.findPlayerGame(player.getUniqueId());

        if (gameOptional.isEmpty())
            return;

        GunItem.stopAiming(player);

        BouncyBulletGame game = gameOptional.get();
        ItemStack previousItem = player.getInventory().getItem(event.getPreviousSlot());
        ItemStack itemStack = player.getInventory().getItem(event.getNewSlot());

        if (previousItem != null) {
            GameItem.findGameItemId(previousItem)
                    .map(itemId -> GameItems.getAllItems().get(itemId))
                    .ifPresent(gameItem -> gameItem.onUnheld(game, player, previousItem, itemStack));
        }

        if (itemStack != null) {
            GameItem.findGameItemId(itemStack)
                    .map(itemId -> GameItems.getAllItems().get(itemId))
                    .ifPresent(gameItem -> gameItem.onHeld(game, player, itemStack, previousItem));
        }
    }

    @EventHandler
    public void onInventorySlotChange(PlayerInventorySlotChangeEvent event) {
        Player player = event.getPlayer();
        Optional<BouncyBulletGame> gameOptional = this.gameManager.findPlayerGame(player.getUniqueId());

        if (gameOptional.isEmpty())
            return;

        BouncyBulletGame game = gameOptional.get();
        ItemStack itemStack = event.getNewItemStack();
        ItemStack previousItem = event.getOldItemStack();

        if (event.getSlot() != player.getInventory().getHeldItemSlot() || itemStack == previousItem)
            return;

        GameItem previousGameItem = GameItem.findGameItemId(previousItem)
                .map(gameId -> GameItems.getAllItems().get(gameId))
                .orElse(null);

        GameItem newGameItem = GameItem.findGameItemId(itemStack)
                .map(gameId -> GameItems.getAllItems().get(gameId))
                .orElse(null);

        if (newGameItem != null && previousGameItem == newGameItem)
            return;

        if (previousGameItem != null)
            previousGameItem.onUnheld(game, player, previousItem, itemStack);

        if (newGameItem != null)
            newGameItem.onHeld(game, player, itemStack, previousItem);
    }

}
