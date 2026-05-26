package dev.zenqrt.bouncybullets.loadout.kit;

import dev.zenqrt.bouncybullets.BouncyBulletsPlugin;
import dev.zenqrt.bouncybullets.event.EventNode;
import dev.zenqrt.bouncybullets.event.PaperEventListener;
import dev.zenqrt.bouncybullets.event.events.GunShootEvent;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGame;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGamePlayer;
import dev.zenqrt.bouncybullets.item.GameItems;
import dev.zenqrt.bouncybullets.item.items.abilities.ActiveAbilityItem;
import dev.zenqrt.bouncybullets.item.items.guns.GunItem;
import dev.zenqrt.bouncybullets.loadout.gun.BulletProperties;
import dev.zenqrt.bouncybullets.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// TODO: Change active ability to marking player and dealing more damage to them for a short period of time
// TODO: Ability: Make every player glowing and the next shot does twice as much damage
final class SniperPlayerClass implements EventPlayerClass {

    private static final GunItem PRIMARY_GUN = GameItems.SNIPER_RIFLE;
    private static final GunItem SECONDARY_GUN = GameItems.PISTOL;
    private static final ActiveAbilityItem ACTIVE_ABILITY = GameItems.SNIPER_ACTIVE_ABILITY;

    private final Map<UUID, Long> lastMoved = new HashMap<>();
    private final Map<UUID, BukkitTask> chargeTasks = new HashMap<>();

    @Override
    public EventNode<Event> registerEvents(BouncyBulletGame game) {
        EventNode<Event> eventNode = EventNode.create();

        eventNode.registerListener(PaperEventListener.builder(PlayerMoveEvent.class)
                .filter(PlayerMoveEvent::hasExplicitlyChangedPosition)
                .filter(event -> !event.hasChangedOrientation())
                .handler(event -> lastMoved.put(event.getPlayer().getUniqueId(), System.currentTimeMillis()))
                .build());
        eventNode.registerListener(PaperEventListener.builder(GunShootEvent.class)
                .filter(event -> event.getGunItem() == GameItems.SNIPER_RIFLE)
                .handler(event -> {
                    Player player = event.getShooter();
                    BouncyBulletGamePlayer gamePlayer = game.findPlayerOrThrow(player.getUniqueId());

                    if (gamePlayer.getLoadout().playerClass() != this)
                        return;

                    BulletProperties bulletProperties = event.getBulletProperties();

                    double originalDamage = bulletProperties.maxDamage();
                    double finalDamage = originalDamage + (originalDamage * (player.getLevel() / 100D));

                    event.setBulletProperties(bulletProperties.withMaxDamage(finalDamage));
                    this.lastMoved.put(player.getUniqueId(), System.currentTimeMillis());
                })
                .build());

        return eventNode;
    }

    @Override
    public void onStartUse(BouncyBulletGame game, BouncyBulletGamePlayer gamePlayer) {
        Player player = gamePlayer.getPlayer();

        this.chargeTasks.put(
                gamePlayer.getUuid(),
                Bukkit.getScheduler().runTaskTimer(
                        BouncyBulletsPlugin.getInstance(),
                        () -> {
                            long interval = System.currentTimeMillis() - this.lastMoved.getOrDefault(player.getUniqueId(), System.currentTimeMillis());
                            float progress = interval / 10000f;

                            if (progress > 1) {
                                if (player.getExp() == 1) {
                                    return;
                                }

                                progress = 1;
                            }

                            player.setExp(progress);

                            int damage = (int) (50 * progress);
                            player.setLevel(damage);
                        },
                0, 1));
    }

    @Override
    public void onStopUse(BouncyBulletGamePlayer gamePlayer) {
        BukkitTask task = this.chargeTasks.remove(gamePlayer.getUuid());

        if (task != null)
            task.cancel();
    }

    @Override
    public String getName() {
        return "Sniper";
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
                EquipmentSlot.LEGS, ItemUtils.createLeatherArmor(Material.LEATHER_LEGGINGS, Color.fromRGB(46, 79, 51)),
                EquipmentSlot.FEET, ItemUtils.createLeatherArmor(Material.LEATHER_BOOTS, Color.fromRGB(42, 48, 43))
        );
    }
}
