package dev.zenqrt.bouncybullets.loadout.kit;

import dev.zenqrt.bouncybullets.BouncyBulletsPlugin;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGame;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGamePlayer;
import dev.zenqrt.bouncybullets.item.GameItems;
import dev.zenqrt.bouncybullets.item.items.abilities.HealerActiveAbilityItem;
import dev.zenqrt.bouncybullets.item.items.guns.GunItem;
import dev.zenqrt.bouncybullets.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class HealerPlayerClass implements PlayerClass {

    private static final GunItem PRIMARY_GUN = GameItems.PISTOL;
    private static final HealerActiveAbilityItem ACTIVE_ABILITY = GameItems.HEALER_ACTIVE_ABILITY;

    private final List<BukkitTask> healTasks = new ArrayList<>();

    @Override
    public void onStartUse(BouncyBulletGame game, BouncyBulletGamePlayer gamePlayer) {
        Player player = gamePlayer.getPlayer();

        PlayerUtils.requireNonNullAttribute(player, Attribute.MAX_ABSORPTION)
                        .setBaseValue(5);

        this.healTasks.add(
                Bukkit.getScheduler().runTaskTimer(
                        BouncyBulletsPlugin.getInstance(),
                        () -> {
                            if (gamePlayer.isDead())
                                return;

                            double maxHealth = PlayerUtils.requireNonNullAttribute(player, Attribute.MAX_HEALTH).getValue();

                            if (player.getHealth() < maxHealth) {
                                player.setHealth(Math.min(maxHealth, player.getHealth() + 1));
                            }
                        },
                        0, 40
                )
        );
    }

    @Override
    public void onStopUse(BouncyBulletGamePlayer gamePlayer) {
        this.healTasks.forEach(BukkitTask::cancel);
        this.healTasks.clear();
    }

    @Override
    public String getName() {
        return "Healer";
    }

    @Override
    public Map<Integer, ItemStack> getItems() {
        return Map.of(
                0, PRIMARY_GUN.buildItemStack(),
                1, ACTIVE_ABILITY.buildItemStack()
        );
    }

    @Override
    public Map<EquipmentSlot, ItemStack> getArmorEquipment() {
        return Map.of(
                EquipmentSlot.HEAD, new ItemStack(Material.FLOWERING_AZALEA),
                EquipmentSlot.CHEST, new ItemStack(Material.GOLDEN_CHESTPLATE),
                EquipmentSlot.LEGS, new ItemStack(Material.CHAINMAIL_LEGGINGS),
                EquipmentSlot.FEET, new ItemStack(Material.IRON_BOOTS)
        );
    }
}
