package dev.zenqrt.bouncybullets.item.items.guns;

import com.destroystokyo.paper.ParticleBuilder;
import dev.zenqrt.bouncybullets.loadout.gun.BulletProperties;
import dev.zenqrt.bouncybullets.loadout.gun.GunProperties;
import dev.zenqrt.bouncybullets.utils.Sounds;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DyedItemColor;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;

public class HuntingRifleGunItem extends BulletGunItem {

    @SuppressWarnings("UnstableApiUsage")
    public HuntingRifleGunItem(GunProperties gunProperties, BulletProperties bulletProperties) {
        super(
                "hunting_rifle",
                Material.LEATHER_HORSE_ARMOR,
                "Hunting Rifle",
                gunProperties,
                bulletProperties,
                dataComponentsBuilder()
                        .addData(DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor()
                                .color(Color.GRAY)
                                .build())
        );
    }
    @Override
    protected ParticleBuilder getBulletParticleBuilder() {
        return Particle.ELECTRIC_SPARK.builder();
    }

    @Override
    protected Sound getShootingSound() {
        return Sound.sound(Sounds.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, Sound.Source.PLAYER, 1, 0.5F);
    }
}
