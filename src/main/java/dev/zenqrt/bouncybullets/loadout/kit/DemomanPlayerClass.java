package dev.zenqrt.bouncybullets.loadout.kit;

import dev.zenqrt.bouncybullets.BouncyBulletsPlugin;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGame;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGamePlayer;
import dev.zenqrt.bouncybullets.item.GameItems;
import dev.zenqrt.bouncybullets.item.items.abilities.DemomanActiveAbilityItem;
import dev.zenqrt.bouncybullets.item.items.guns.GunItem;
import dev.zenqrt.bouncybullets.utils.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

final class DemomanPlayerClass implements PlayerClass {

    private static final AttributeModifier SPEED_MODIFIER = new AttributeModifier(
            BouncyBulletsPlugin.createKey("demoman_speed_modifier"),
            -0.02,
            AttributeModifier.Operation.ADD_NUMBER
    );
    private static final GunItem PRIMARY_GUN = GameItems.GRENADE_LAUNCHER;
    private static final GunItem SECONDARY_GUN = GameItems.DESERT_EAGLE;
    private static final DemomanActiveAbilityItem ACTIVE_ABILITY = GameItems.DEMOMAN_ACTIVE_ABILITY;

    @Override
    public void onStartUse(BouncyBulletGame game, BouncyBulletGamePlayer gamePlayer) {
        PlayerUtils.requireNonNullAttribute(gamePlayer.getPlayer(), Attribute.MOVEMENT_SPEED)
                        .addTransientModifier(SPEED_MODIFIER);
    }

    @Override
    public void onStopUse(BouncyBulletGamePlayer gamePlayer) {
        PlayerUtils.requireNonNullAttribute(gamePlayer.getPlayer(), Attribute.MOVEMENT_SPEED)
                .removeModifier(SPEED_MODIFIER);
    }

    @Override
    public String getName() {
        return "Demoman";
    }

    @Override
    public Map<Integer, ItemStack> getItems() {
        return Map.of(
                0, PRIMARY_GUN.buildItemStack(),
                1, SECONDARY_GUN.buildItemStack(),
                2, ACTIVE_ABILITY.buildItemStack()
        );
    }

    @Override
    public Map<EquipmentSlot, ItemStack> getArmorEquipment() {
        return Map.of(
                EquipmentSlot.HEAD, new ItemStack(Material.BLAST_FURNACE),
                EquipmentSlot.CHEST, new ItemStack(Material.NETHERITE_CHESTPLATE),
                EquipmentSlot.LEGS, new ItemStack(Material.CHAINMAIL_LEGGINGS),
                EquipmentSlot.FEET, new ItemStack(Material.IRON_BOOTS)
        );
    }
}
