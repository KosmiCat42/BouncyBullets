package dev.zenqrt.bouncybullets.loadout.kit;

import dev.zenqrt.bouncybullets.event.EventNode;
import dev.zenqrt.bouncybullets.event.PaperEventListener;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGame;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGamePlayer;
import dev.zenqrt.bouncybullets.item.GameItems;
import dev.zenqrt.bouncybullets.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BountyHunterPlayerClass implements EventPlayerClass {
    private static final String TARGET_DISPLAY_ID = "target";
    private final Map<UUID, BouncyBulletGamePlayer> targetData = new HashMap<>();
    private final Map<UUID, List<UUID>> targetToHunterMap = new HashMap<>();
    private final Map<UUID, BukkitTask> targetInfoTasks = new HashMap<>();

    @Override
    public String getName() {
        return "Bounty Hunter";
    }

    @Override
    public Map<Integer, ItemStack> getItems() {
        return Map.of(
                0, GameItems.HUNTING_RIFLE.buildItemStack()
        );
    }

    @Override
    public Map<EquipmentSlot, ItemStack> getArmorEquipment() {
        return Map.of(
                EquipmentSlot.HEAD, new ItemStack(Material.FEATHER),
                EquipmentSlot.CHEST, ItemUtils.createLeatherArmor(Material.LEATHER_CHESTPLATE, Color.GRAY),
                EquipmentSlot.LEGS, new ItemStack(Material.GOLDEN_LEGGINGS),
                EquipmentSlot.FEET, ItemUtils.createLeatherArmor(Material.LEATHER_BOOTS, Color.OLIVE)
        );
    }

    @Override
    public void onStartUse(BouncyBulletGame game, BouncyBulletGamePlayer gamePlayer) {
        assignTarget(game, gamePlayer, ThreadLocalRandom.current());

        this.targetInfoTasks.put(
                gamePlayer.getUuid(),
                new DisplayTargetInfoTask(gamePlayer)
                        .start(game.getPlugin())
        );
    }

    @Override
    public void onStopUse(BouncyBulletGamePlayer gamePlayer) {
        BouncyBulletGamePlayer existingTarget = this.targetData.remove(gamePlayer.getUuid());

        List<UUID> hunters = this.targetToHunterMap.get(existingTarget.getUuid());

        if (hunters != null) {
            if (hunters.size() <= 1)
                this.targetToHunterMap.remove(existingTarget.getUuid());
            else
                hunters.remove(gamePlayer.getUuid());
        }
    }

    @Override
    public EventNode<Event> registerEvents(BouncyBulletGame game) {
        EventNode<Event> eventNode = EventNode.create();
        eventNode.registerListener(PaperEventListener.builder(PlayerDeathEvent.class)
                .handler(event -> {
                    Player player = event.getPlayer();
                    List<UUID> hunters = this.targetToHunterMap.get(player.getUniqueId());

                    if (hunters == null)
                        return;

                    EntityDamageEvent damageEvent = player.getLastDamageCause();

                    if (damageEvent == null)
                        return;

                    Entity causingEntity = damageEvent.getDamageSource().getCausingEntity();

                    if (causingEntity == null)
                        return;

                    BouncyBulletGamePlayer target = this.targetData.get(causingEntity.getUniqueId());

                    if (target == null || !target.getUuid().equals(player.getUniqueId()))
                        return;

                    BouncyBulletGamePlayer killerGamePlayer = game.findPlayerOrThrow(causingEntity.getUniqueId());

                    killerGamePlayer.getPlayer().sendMessage(Component.text("hello you got 5 dollar"));
                    // REWARD 5 $ to hunter but

                }).build()
        );
        return eventNode;
    }

    private void assignTarget(BouncyBulletGame game, BouncyBulletGamePlayer hunter, Random random) {
        if (game.getPlayers().size() <= 1) {
            hunter.getHud().addDisplay(TARGET_DISPLAY_ID, noTargetDisplayText());
            return;
        }

        List<BouncyBulletGamePlayer> players = game.getPlayers().values().stream()
                .toList();

        BouncyBulletGamePlayer target;

        do {
            int targetIndex = random.nextInt(players.size());
            target = players.get(targetIndex);
        } while (target.getUuid() == hunter.getUuid());

        this.targetData.put(hunter.getUuid(), target);
        this.targetToHunterMap.computeIfAbsent(target.getUuid(), _ -> new ArrayList<>())
                .add(hunter.getUuid());
        RelativePositionalInfo info = RelativePositionalInfo.fromLocations(
                hunter.getPlayer().getLocation(),
                target.getPlayer().getLocation()
        );

        hunter.getHud().addDisplay(TARGET_DISPLAY_ID, targetDisplayText(target.getPlayer().getName(), info.distance, info.direction));
    }
    // Ammo: 0/0 | Current Target: Someone (32m >)
    private static Component targetDisplayText(String username, double distance, Direction direction) {
        return Component.text("Current Target: ", NamedTextColor.WHITE)
                .append(Component.text(username, NamedTextColor.RED))
                .append(Component.text(" (" + distance + "m " + direction.icon + ")", NamedTextColor.GRAY));
    }
    private static Component deadTargetDisplayText(String username) {
        return Component.text("Current Target: ", NamedTextColor.WHITE)
                .append(Component.text(username, NamedTextColor.RED))
                .append(Component.text(" (Dead)", NamedTextColor.GRAY));
    }
    private static Component noTargetDisplayText() {
        return Component.text("Current Target: ", NamedTextColor.WHITE)
                .append(Component.text("None", NamedTextColor.DARK_GRAY));
    }


    private class DisplayTargetInfoTask implements Runnable {

        private final BouncyBulletGamePlayer gamePlayer;

        DisplayTargetInfoTask(BouncyBulletGamePlayer gamePlayer) {
            this.gamePlayer = gamePlayer;
        }

        @Override
        public void run() {
            final BouncyBulletGamePlayer target = BountyHunterPlayerClass.this.targetData.get(this.gamePlayer.getUuid());

            if (target == null)
                return;

            if(target.isDead()) {
                this.gamePlayer.getHud().updateDisplay(TARGET_DISPLAY_ID, deadTargetDisplayText(target.getPlayer().getName()));
                return;
            }

            RelativePositionalInfo info = RelativePositionalInfo.fromLocations(
                    this.gamePlayer.getPlayer().getLocation(),
                    target.getPlayer().getLocation()
            );

            this.gamePlayer.getHud().updateDisplay(
                    TARGET_DISPLAY_ID,
                    targetDisplayText(
                            target.getPlayer().getName(),
                            info.distance,
                            info.direction
                    )
            );
            this.gamePlayer.getHud().updateHudText();
        }

        private BukkitTask start(Plugin plugin) {
            return Bukkit.getScheduler().runTaskTimer(plugin, this, 0, 10);
        }
    }

    private record RelativePositionalInfo(double distance, Direction direction) {

        private static RelativePositionalInfo fromLocations(Location location, Location otherLocation) {
            Vector toTarget = otherLocation.subtract(location).toVector();

            Vector lookDir = location.getDirection();
            Vector right = lookDir.clone().crossProduct(new Vector(0, 1, 0));

            double lookDirDot = lookDir.dot(toTarget);
            double rightDot = right.dot(toTarget);

            Direction direction;

            if (Math.abs(lookDirDot) > Math.abs(rightDot))
                direction = lookDirDot > 0 ? Direction.NORTH : Direction.SOUTH;
            else
                direction = rightDot > 0 ? Direction.EAST : Direction.WEST;

            double distance = toTarget.length();

            return new RelativePositionalInfo(distance, direction);
        }

    }

    private enum Direction {
        NORTH('↑'),
        EAST('→'),
        SOUTH('↓'),
        WEST('←')
        ;

        private final char icon;

        Direction(char icon) {
            this.icon = icon;
        }
    }
}
