package dev.zenqrt.bouncybullets.item.items.guns;

import com.destroystokyo.paper.ParticleBuilder;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGame;
import dev.zenqrt.bouncybullets.game.games.BouncyBulletGamePlayer;
import dev.zenqrt.bouncybullets.loadout.gun.BulletProperties;
import dev.zenqrt.bouncybullets.loadout.gun.GunProperties;
import dev.zenqrt.bouncybullets.player.BouncyBulletsHUD;
import dev.zenqrt.bouncybullets.tasks.ShootBulletTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class BulletGunItem extends GunItem {

    private static final int INTERACT_EVENT_TICK_DELAY = 4;

    public BulletGunItem(String key, Material material, String displayName, GunProperties gunProperties, BulletProperties bulletProperties) {
        super(key, material, displayName, gunProperties, bulletProperties);
    }

    public BulletGunItem(String key, Material material, String displayName, GunProperties gunProperties, BulletProperties bulletProperties, DataComponentsBuilder builder) {
        super(key, material, displayName, gunProperties, bulletProperties, builder);
    }

    protected abstract ParticleBuilder getBulletParticleBuilder();

    @Override
    protected void shootProjectile(BouncyBulletGame game, Player player, BulletProperties bulletProperties) {
        new ShootBulletTask(
                player,
                player.getEyeLocation(),
                player.getEyeLocation().getDirection().normalize(),
                bulletProperties,
                GunItem.isAiming(player) ? this.gunProperties.recoilRangeFocused() : this.gunProperties.recoilRange(),
                getBulletParticleBuilder()
        ).runTaskTimer(game.getPlugin(), 0, 1);
    }

    @Override
    protected void useGun(BouncyBulletGame game, BouncyBulletGamePlayer gamePlayer, Player player, ItemStack itemStack) {
        if (super.gunProperties.shootDelayTicks() < INTERACT_EVENT_TICK_DELAY) {
            long shootDivisions = INTERACT_EVENT_TICK_DELAY / super.gunProperties.shootDelayTicks();

            tryShootGun(game, player, gamePlayer.getHud(), itemStack);

            for (int i = 1; i < shootDivisions; i++) {
                Bukkit.getScheduler().runTaskLater(
                        game.getPlugin(),
                        () -> tryShootGun(game, player, gamePlayer.getHud(), itemStack),
                        (long) i * super.gunProperties.shootDelayTicks()
                );
            }

            return;
        }

        tryShootGun(game, player, gamePlayer.getHud(), itemStack);
    }

    private void tryShootGun(BouncyBulletGame game, Player player, BouncyBulletsHUD hud, ItemStack itemStack) {
        long currentGameTime = player.getServer().getCurrentTick();

        if (getAmmo(itemStack) <= 0)
            return;

        if (super.lastShootTicks.containsKey(player.getUniqueId())) {
            long lastShootTick = super.lastShootTicks.get(player.getUniqueId());
            long tickInterval = currentGameTime - lastShootTick;

            if (tickInterval < super.gunProperties.shootDelayTicks()) {
                return;
            }
        }

        shootGun(game, player, hud, itemStack);
        super.lastShootTicks.put(player.getUniqueId(), currentGameTime);
    }
}
