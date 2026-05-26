package dev.zenqrt.bouncybullets.loadout.kit;

import dev.zenqrt.bouncybullets.game.games.BouncyBulletGame;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGamePlayer;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public interface PlayerClass {
    String getName();
    Map<Integer, ItemStack> getItems();
    Map<EquipmentSlot, ItemStack> getArmorEquipment();

    default void onStartUse(BouncyBulletGame game, BouncyBulletGamePlayer gamePlayer) {}
    default void onStopUse(BouncyBulletGamePlayer gamePlayer) {}
    default void onRespawn(BouncyBulletGamePlayer gamePlayer) {}
}
