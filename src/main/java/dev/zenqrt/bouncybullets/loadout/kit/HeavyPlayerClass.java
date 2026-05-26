package dev.zenqrt.bouncybullets.loadout.kit;

import dev.zenqrt.bouncybullets.BouncyBulletsPlugin;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGame;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGamePlayer;
import dev.zenqrt.bouncybullets.item.GameItems;
import dev.zenqrt.bouncybullets.item.items.abilities.ActiveAbilityItem;
import dev.zenqrt.bouncybullets.item.items.guns.GunItem;
import dev.zenqrt.bouncybullets.utils.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class HeavyPlayerClass implements PlayerClass {

    private static final GunItem PRIMARY_GUN = GameItems.MINIGUN;
    private static final ActiveAbilityItem ACTIVE_ABILITY = GameItems.HEAVY_ACTIVE_ABILITY;
    private static final AttributeModifier HEALTH_BUFF_MODIFIER = new AttributeModifier(
            BouncyBulletsPlugin.createKey("heavy_health_buff"),
            20,
            AttributeModifier.Operation.ADD_NUMBER
    );
    private static final AttributeModifier SLOW_MODIFIER = new AttributeModifier(
            BouncyBulletsPlugin.createKey("heavy_speed"),
            -0.04,
            AttributeModifier.Operation.ADD_NUMBER
    );

    @Override
    public String getName() {
        return "Heavy";
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
                EquipmentSlot.HEAD, new ItemStack(Material.TINTED_GLASS),
                EquipmentSlot.CHEST, new ItemStack(Material.NETHERITE_CHESTPLATE),
                EquipmentSlot.LEGS, new ItemStack(Material.IRON_LEGGINGS),
                EquipmentSlot.FEET, new ItemStack(Material.NETHERITE_BOOTS)
        );
    }

    @Override
    public void onStartUse(BouncyBulletGame game, BouncyBulletGamePlayer gamePlayer) {
        Player player = gamePlayer.getPlayer();

        AttributeInstance maxHealth = PlayerUtils.requireNonNullAttribute(player, Attribute.MAX_HEALTH);
        AttributeInstance movementSpeed = PlayerUtils.requireNonNullAttribute(player, Attribute.MOVEMENT_SPEED);

        maxHealth.addTransientModifier(HEALTH_BUFF_MODIFIER);
        player.setHealth(maxHealth.getValue());

        movementSpeed.addTransientModifier(SLOW_MODIFIER);
    }

    @Override
    public void onStopUse(BouncyBulletGamePlayer gamePlayer) {
        Player player = gamePlayer.getPlayer();

        AttributeInstance maxHealth = PlayerUtils.requireNonNullAttribute(player, Attribute.MAX_HEALTH);
        AttributeInstance movementSpeed = PlayerUtils.requireNonNullAttribute(player, Attribute.MOVEMENT_SPEED);

        maxHealth.removeModifier(HEALTH_BUFF_MODIFIER);
        movementSpeed.removeModifier(SLOW_MODIFIER);
    }
}
