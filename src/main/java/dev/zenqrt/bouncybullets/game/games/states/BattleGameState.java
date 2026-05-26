package dev.zenqrt.bouncybullets.game.games.states;

import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import dev.zenqrt.bouncybullets.BouncyBulletsPlugin;
import dev.zenqrt.bouncybullets.event.EventNode;
import dev.zenqrt.bouncybullets.event.GameEventNodes;
import dev.zenqrt.bouncybullets.event.PaperEventListener;
import dev.zenqrt.bouncybullets.game.base.GameState;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGame;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGamePlayer;
import dev.zenqrt.bouncybullets.item.items.guns.GunItem;
import dev.zenqrt.bouncybullets.loadout.Loadout;
import dev.zenqrt.bouncybullets.loadout.kit.EventPlayerClass;
import dev.zenqrt.bouncybullets.loadout.kit.PlayerClass;
import dev.zenqrt.bouncybullets.map.FreeForAllActiveGameMap;
import dev.zenqrt.bouncybullets.player.GamePlayerList;
import dev.zenqrt.bouncybullets.sidebar.sidebars.BouncyBulletsSidebar;
import dev.zenqrt.bouncybullets.utils.NMSConverter;
import dev.zenqrt.bouncybullets.utils.PlayerUtils;
import dev.zenqrt.bouncybullets.utils.TaskManager;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class BattleGameState extends GameState {

    private static final Sound KILL_SOUND = Sound.sound(org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.MASTER, 1, 1);
    private static final AttributeModifier NO_KNOCKBACK_MODIFIER = new AttributeModifier(
            BouncyBulletsPlugin.createKey("bouncy-bullets_no_kb"),
            100,
            AttributeModifier.Operation.ADD_NUMBER
    );
    private static final AttributeModifier GAME_SPEED_MODIFIER = new AttributeModifier(
            BouncyBulletsPlugin.createKey("bouncy-bullets_game_speed"),
            0.04,
            AttributeModifier.Operation.ADD_NUMBER
    );

    private final EventNode<Event> stateEventNode;
    private final EventNode<PlayerEvent> playerEventNode;
    private final EventNode<EntityEvent> playerEntityEventNode;

    private final Map<UUID, BouncyBulletsSidebar> sidebarMap = new HashMap<>();

    private final TaskManager taskManager;
    private final BouncyBulletGame game;
    private final GamePlayerList players;
    private final FreeForAllActiveGameMap gameMap;

    public BattleGameState(BouncyBulletGame game, GamePlayerList players, FreeForAllActiveGameMap gameMap) {
        this.game = game;
        this.players = players;
        this.gameMap = gameMap;
        this.taskManager = new TaskManager(game.getPlugin());

        this.playerEventNode = GameEventNodes.filteredPlayerEvents(game);
        this.playerEntityEventNode = GameEventNodes.filteredEntityEvents(game);

        this.stateEventNode = EventNode.create();
        this.stateEventNode.addChild(this.playerEventNode);
        this.stateEventNode.addChild(this.playerEntityEventNode);
    }

    @Override
    protected void onStateStart() {
        super.onStateStart();

        registerEvents();

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        scoreboard.getTeams().forEach(Team::unregister);

        Team team = scoreboard.registerNewTeam("players");
        team.setCanSeeFriendlyInvisibles(false);

        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);

        List<Player> initialTopPlayers = players.values().stream()
                .map(BouncyBulletGamePlayer::getPlayer)
                .limit(3)
                .toList();

        this.players.forEach((_, gamePlayer) -> {
            Player player = gamePlayer.getPlayer();

            player.getInventory().clear();

            setupPlayerAttributes(player);
            setupPlayerInventory(player.getInventory(), gamePlayer.getLoadout());

            team.addPlayer(player);

            BouncyBulletsSidebar sidebar = new BouncyBulletsSidebar(this.game.getGameSettings().gameTime(), initialTopPlayers);
            sidebar.addViewer(NMSConverter.serverPlayer(player));

            this.sidebarMap.put(player.getUniqueId(), sidebar);

            Location randomSpawn = gameMap.spawnLocations().get(ThreadLocalRandom.current().nextInt(gameMap.spawnLocations().size())).toLocation(gameMap.world());
            player.teleport(randomSpawn);

            gamePlayer.setAlive(true);
        });

        this.taskManager.runTaskTimer(
                new GameTimerTask(this.game.getGameSettings().gameTime()),
                0, 20
        );
        this.taskManager.runTaskTimer(
                new HUDTask(),
                0, 1
        );
    }

    @Override
    protected void onStateEnd() {
        super.onStateEnd();

        this.stateEventNode.unregisterAllListeners();
        this.taskManager.removeAllTasks();

        this.players.forEach((_, gamePlayer) -> {
            gamePlayer.getLoadout().playerClass().onStopUse(gamePlayer);
            gamePlayer.setAlive(false);

            Player player = gamePlayer.getPlayer();

            AttributeInstance knockbackResistance = PlayerUtils.requireNonNullAttribute(player, Attribute.KNOCKBACK_RESISTANCE);
            AttributeInstance movementSpeed = PlayerUtils.requireNonNullAttribute(player, Attribute.MOVEMENT_SPEED);

            knockbackResistance.removeModifier(NO_KNOCKBACK_MODIFIER);
            movementSpeed.removeModifier(GAME_SPEED_MODIFIER);

            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
            player.clearActivePotionEffects();

            this.sidebarMap.forEach((ignored, sidebar) -> sidebar.removeAllViewers());
            this.sidebarMap.clear();

            player.sendActionBar(Component.empty());
        });
    }

    private void registerEvents() {
        Set<EventPlayerClass> playerClasses = this.players.values().stream()
                .map(gamePlayer -> gamePlayer.getLoadout().playerClass())
                .filter(playerClass -> playerClass instanceof EventPlayerClass)
                .map(playerClass -> (EventPlayerClass) playerClass)
                .collect(Collectors.toSet());

        for (EventPlayerClass playerClass : playerClasses) {
            EventNode<Event> classEventNode = playerClass.registerEvents(this.game);

            this.stateEventNode.addChild(classEventNode);
        }

        this.players.forEach((_, gamePlayer) -> {
            PlayerClass playerClass = gamePlayer.getLoadout().playerClass();
            playerClass.onStartUse(this.game, gamePlayer);
        });

        this.playerEventNode.registerListener(PaperEventListener.builder(PlayerTeleportEvent.class)
                .filter(event -> event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE)
                .handler(event ->  {
                    event.setCancelled(true);

                    Entity spectatorTarget = event.getPlayer().getSpectatorTarget();
                    if (spectatorTarget != null) {
                        event.getPlayer().setSpectatorTarget(spectatorTarget);
                    }
                })
                .build());
        this.playerEventNode.registerListener(PaperEventListener.builder(PlayerStopSpectatingEntityEvent.class)
                .filter(event -> event.getPlayer().getGameMode() == GameMode.SPECTATOR)
                .handler(event -> event.setCancelled(true))
                .build());
        this.playerEntityEventNode.registerListener(PaperEventListener.builder(PlayerDeathEvent.class)
                .filter(event -> players.containsKey(event.getPlayer().getUniqueId()))
                .handler(event -> {
                    event.setCancelled(true);

                    Player player = event.getPlayer();

                    player.setGameMode(GameMode.SPECTATOR);
//                    player.getInventory().clear();
                    player.showTitle(Title.title(Component.text("YOU DIED!", NamedTextColor.RED).decorate(TextDecoration.BOLD), Component.empty(),
                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(5), Duration.ZERO)));
                    player.clearActivePotionEffects();

                    EntityDamageEvent lastDamageEvent = player.getLastDamageCause();

                    if (lastDamageEvent != null) {
                        if (lastDamageEvent.getDamageSource().getCausingEntity() instanceof Player killer && players.containsKey(killer.getUniqueId())) {
                            if (killer != player) {
                                BouncyBulletGamePlayer killerGamePlayer = this.game.findPlayerOrThrow(killer.getUniqueId());

                                killerGamePlayer.addKill();
                                updateSidebar(
                                        killerGamePlayer.getUuid(),
                                        sidebar -> sidebar.setYourKills(killerGamePlayer.getKills())
                                );
                                updateKillsLeaderboard();
                            }

                            player.setSpectatorTarget(killer);
                            killer.playSound(KILL_SOUND, Sound.Emitter.self());
                            players.sendMessage(Component.text(player.getName() + " \uD83D\uDD2B " + killer.getName(), NamedTextColor.RED));
                        }
                    }

                    if (GunItem.isAiming(player))
                        GunItem.stopAiming(player);

                    BouncyBulletGamePlayer gamePlayer = this.game.findPlayerOrThrow(player.getUniqueId());

                    gamePlayer.addDeath();
                    gamePlayer.setAlive(false);

                    this.taskManager.runTaskTimer(
                            new DeathSpectatorTask(gamePlayer, 5),
                            0, 20
                    );
                })
                .build());
        this.stateEventNode.registerListener(PaperEventListener.builder(InventoryClickEvent.class)
                .filter(event -> this.players.containsKey(event.getWhoClicked().getUniqueId()))
                .handler(event -> event.setCancelled(true))
                .build());
        this.stateEventNode.registerListener(PaperEventListener.builder(PlayerDropItemEvent.class)
                .filter(event -> this.game.hasPlayer(event.getPlayer().getUniqueId()))
                .handler(event -> event.setCancelled(true))
                .build());
    }

    private void updateSidebar(UUID uuid, Consumer<BouncyBulletsSidebar> updateHandler) {
        BouncyBulletsSidebar sidebar = this.sidebarMap.get(uuid);
        updateHandler.accept(sidebar);
    }

    private void updateKillsLeaderboard() {
        List<BouncyBulletGamePlayer> topKillers = this.game.getPlayers().values()
                .stream()
                .sorted(Comparator.comparing(BouncyBulletGamePlayer::getKills).reversed())
                .limit(3)
                .toList();

        for (BouncyBulletsSidebar sidebar : this.sidebarMap.values()) {
            for (int i = topKillers.size() - 1; i >= 0; i--) {
                BouncyBulletGamePlayer gamePlayer = topKillers.get(i);

                sidebar.setPlace(i + 1, gamePlayer.getPlayer(), gamePlayer.getKills());
            }
        }
    }

    private Location chooseBestSpawnLocation() {
        return gameMap.spawnLocations().stream()
                .map(location -> location.toLocation(gameMap.world()))
                .max(Comparator.comparing(this::closestDistanceToPlayer))
                .orElseThrow();
    }

    private double closestDistanceToPlayer(Location location) {
        return players.values().stream()
                .mapToDouble(gamePlayer -> location.distance(gamePlayer.getPlayer().getLocation()))
                .min()
                .orElse(0);
    }

    private static void setupPlayerAttributes(Player player) {
        AttributeInstance knockbackResistance = PlayerUtils.requireNonNullAttribute(player, Attribute.KNOCKBACK_RESISTANCE);
        AttributeInstance movementSpeed = PlayerUtils.requireNonNullAttribute(player, Attribute.MOVEMENT_SPEED);

        knockbackResistance.addTransientModifier(NO_KNOCKBACK_MODIFIER);
        movementSpeed.addTransientModifier(GAME_SPEED_MODIFIER);
    }

    private static void setupPlayerInventory(PlayerInventory inventory, Loadout loadout) {
        loadout.giveItems(inventory);
    }

    private final class GameTimerTask implements Runnable {

        private int timeLeft;

        GameTimerTask(int time) {
            this.timeLeft = time;
        }

        @Override
        public void run() {
            if (--timeLeft == 0) {
                game.switchNextState();
            }

            for (BouncyBulletsSidebar sidebar : BattleGameState.this.sidebarMap.values()) {
                sidebar.setGameTime(timeLeft);
            }
        }
    }

    private final class HUDTask implements Runnable {

        @Override
        public void run() {
            for (BouncyBulletGamePlayer gamePlayer : BattleGameState.this.game.getPlayers().values()) {
                gamePlayer.getHud().show(gamePlayer.getPlayer());
            }
        }
    }

    private final class DeathSpectatorTask extends BukkitRunnable {

        private final BouncyBulletGamePlayer gamePlayer;
        private int timeLeft;

        DeathSpectatorTask(BouncyBulletGamePlayer gamePlayer, int respawnTimeSecs) {
            this.gamePlayer = gamePlayer;
            this.timeLeft = respawnTimeSecs;
        }

        @Override
        public void run() {
            Player player = this.gamePlayer.getPlayer();

            if (--this.timeLeft == 0) {
                this.cancel();

//                setupPlayerInventory(player.getInventory(), this.gamePlayer.getLoadout());

                player.setGameMode(GameMode.ADVENTURE);
                player.teleport(chooseBestSpawnLocation());
                player.clearTitle();

                this.gamePlayer.setAlive(true);
                this.gamePlayer.getLoadout().playerClass().onRespawn(this.gamePlayer);
                return;
            }

            player.sendTitlePart(TitlePart.SUBTITLE, Component.text("Respawning in " + this.timeLeft + "..."));
        }
    }
}
