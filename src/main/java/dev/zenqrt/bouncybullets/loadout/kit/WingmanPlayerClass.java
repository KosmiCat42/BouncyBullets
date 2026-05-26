package dev.zenqrt.bouncybullets.loadout.kit;

import dev.zenqrt.bouncybullets.event.EventNode;
import dev.zenqrt.bouncybullets.event.PaperEventListener;
import dev.zenqrt.bouncybullets.event.events.GunShootEvent;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGame;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGamePlayer;
import dev.zenqrt.bouncybullets.item.GameItems;
import dev.zenqrt.bouncybullets.item.items.abilities.ActiveAbilityItem;
import dev.zenqrt.bouncybullets.item.items.guns.GunItem;
import dev.zenqrt.bouncybullets.loadout.gun.BulletProperties;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WingmanPlayerClass implements EventPlayerClass {

    private static final GunItem PRIMARY_GUN = GameItems.TWIN_PISTOL;
    private static final ActiveAbilityItem ACTIVE_ABILITY = GameItems.WINGMAN_ACTIVE_ABILITY;

    private static final Sound DOUBLE_JUMP_SOUND = Sound.sound(Key.key("entity.bat.takeoff"), Sound.Source.MASTER, 1, 1.25F);
    private static final int DOUBLE_JUMP_COOLDOWN_TICKS = 100;          // 5 seconds

    private final Map<UUID, BukkitTask> doubleJumpTasks = new HashMap<>();

    @Override
    public EventNode<Event> registerEvents(BouncyBulletGame game) {
        EventNode<Event> eventNode = EventNode.create();

        eventNode.registerListener(PaperEventListener.builder(PlayerToggleFlightEvent.class)
                .filter(event -> isPlayerClass(game, event.getPlayer(), this))
                .filter(PlayerToggleFlightEvent::isFlying)
                .handler(event -> {
                    event.setCancelled(true);

                    Player player = event.getPlayer();

                    double multiplier = player.getLevel() * 0.15;
                    Vector velocity = player.getEyeLocation().getDirection()
                                    .normalize()
                                    .multiply(0.5 + multiplier);

                    player.setVelocity(velocity);
                    player.setAllowFlight(false);
                    player.setLevel(0);
                    player.setExp(0);
                    player.getWorld().playSound(DOUBLE_JUMP_SOUND, player);

                    BukkitTask previousTask = this.doubleJumpTasks.remove(player.getUniqueId());

                    if (previousTask != null)
                        previousTask.cancel();

                    this.doubleJumpTasks.put(
                            player.getUniqueId(),
                            new DoubleJumpCooldownTask(player, DOUBLE_JUMP_COOLDOWN_TICKS)
                                    .runTaskTimer(game.getPlugin(), 0, 1)
                    );
                })
                .build()
        );
        eventNode.registerListener(PaperEventListener.builder(GunShootEvent.class)
                .filter(event -> isPlayerClass(game, event.getShooter(), this))
                .filter(event -> event.getShooter().isGliding())
                .handler(event -> {
                    BulletProperties modified = event.getBulletProperties()
                            .withMaxDamage(
                                    event.getBulletProperties().maxDamage() * 1.5
                            );

                    event.setBulletProperties(modified);
                }).build()
        );

        return eventNode;
    }

    @Override
    public void onRespawn(BouncyBulletGamePlayer gamePlayer) {
        Player player = gamePlayer.getPlayer();

        if (player.getLevel() <= 0)
            return;

        player.setAllowFlight(true);
    }

    @Override
    public void onStartUse(BouncyBulletGame game, BouncyBulletGamePlayer gamePlayer) {
        gamePlayer.getPlayer().setAllowFlight(true);
        gamePlayer.getPlayer().setLevel(0);
        gamePlayer.getPlayer().setExp(0);
    }

    @Override
    public void onStopUse(BouncyBulletGamePlayer gamePlayer) {
        Player player = gamePlayer.getPlayer();

        player.setAllowFlight(false);
        player.setExp(0);
        player.setLevel(0);

        BukkitTask leftoverTask = this.doubleJumpTasks.remove(player.getUniqueId());

        if (leftoverTask != null)
            leftoverTask.cancel();
    }

    @Override
    public String getName() {
        return "Wingman";
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
                EquipmentSlot.HEAD, new ItemStack(Material.NETHERITE_HELMET),
                EquipmentSlot.CHEST, new ItemStack(Material.ELYTRA),
                EquipmentSlot.LEGS, new ItemStack(Material.CHAINMAIL_LEGGINGS),
                EquipmentSlot.FEET, new ItemStack(Material.DIAMOND_BOOTS)
        );
    }

    private class DoubleJumpCooldownTask extends BukkitRunnable {

        private int ticks;
        private final int levelInterval;
        private final Player player;

        DoubleJumpCooldownTask(Player player, int levelInterval) {
            this.player = player;
            this.levelInterval = levelInterval;
            this.ticks = 0;
        }

        @Override
        public void run() {
            if (this.ticks >= this.levelInterval) {
                this.ticks = 0;

                int newLevel = this.player.getLevel() + 1;

                this.player.setLevel(newLevel);
                this.player.setExp(0);
                this.player.setAllowFlight(true);

                if (newLevel >= 5) {
                    this.cancel();
                    WingmanPlayerClass.this.doubleJumpTasks.remove(this.player.getUniqueId());
                }

                return;
            }

            float progress = (float) this.ticks / this.levelInterval;

            this.player.setExp(Math.min(1, progress));
            this.ticks++;
        }
    }

}
