package dev.zenqrt.bouncybullets.loadout.kit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.function.Consumer;

public enum PlayerClassType {
    STEALTH(new StealthPlayerClass(), Material.IRON_HORSE_ARMOR,
            List.of(
                    Component.text("SMG", NamedTextColor.GRAY),
                    Component.text("Silenced Pistol", NamedTextColor.GRAY),
                    Component.text("Invisibility Cloak", NamedTextColor.LIGHT_PURPLE)
            ),
            List.of(
                    "<gold>Passive Ability: <aqua>Out of Sight",
                    "<gray>While sneaking, your stealth meter fills up. Once the meter is filled, you gain <white>Invisibility<gray>. Running, shooting, or taking damage will reveal yourself.",
                    "",
                    "<gold>Passive Ability: <aqua>Surprise Attack",
                    "<gray>After emerging from being invisible, gain <aqua>Speed II <gray>for <green>5s<gray>."
    )),
    HEALER(new HealerPlayerClass(), Material.SPLASH_POTION, meta -> {
        ((PotionMeta) meta).setBasePotionType(PotionType.HEALING);
    },
            List.of(
                    Component.text("BB-Pistol", NamedTextColor.GRAY),
                    Component.text("Miracle", NamedTextColor.LIGHT_PURPLE)
            ),
            List.of(
                    "<gold>Passive Ability: <aqua>Blessing",
                    "<gray>Regenerate <red>1❤</red> every <green>2 <gray>seconds."
            )),
    SNIPER(new SniperPlayerClass(), Material.ENDER_EYE, List.of(
            Component.text("Sniper Rifle", NamedTextColor.GRAY),
            Component.text("Pistol", NamedTextColor.GRAY)
    ),
            List.of(
                    "<gold>Passive Ability:",
                    "<gray>For each second of standing still, accumulate <red>5% <gray>damage increase, up to <red>50% <gray>maximum. This is shown in the experience bar."
            )),
    DEMOMAN(new DemomanPlayerClass(), Material.TNT, List.of(
            Component.text("Grenade Launcher", NamedTextColor.GRAY),
            Component.text("Desert Eagle", NamedTextColor.GRAY),
            Component.text("Pocket Railgun", NamedTextColor.LIGHT_PURPLE)
    ),
            List.of(
                    "<gold>Passive Ability:",
                    "<gray>Your speed is decreased by some amount."
            )),
    WINGMAN(new WingmanPlayerClass(), Material.ELYTRA,
            List.of(
                    Component.text("Twin Pistols", NamedTextColor.GRAY),
                    Component.text("Bullet Spread", NamedTextColor.LIGHT_PURPLE)
            ),
            List.of(
                    "<gold>Passive Ability: <aqua>Wing's Thrust",
                    "<gray>Gain a charged double jump every <green>10s<gray>, up to a max level of <red>5<gray>. Each charge level launches you further.",
                    "",
                    "<gold>Passive Ability: <aqua>idk what to call this",
                    "<gray>Any shots fired while flying above <green>2 blocks <gray>does <red>50% <gray>more damage."
            )
    ),
    HEAVY(new HeavyPlayerClass(), Material.IRON_CHESTPLATE,
            List.of(
                    Component.text("Minigun", NamedTextColor.GRAY),
                    Component.text("Bulletproof", NamedTextColor.LIGHT_PURPLE)
            ),
            List.of(
                    "<gold>Passive Ability: <aqua>Tank",
                    "<gray>Gain an extra row of <red>20❤<gray>.",
                    "",
                    "<gold>Passive Ability: <aqua>Sluggish",
                    "<gray>Your speed is decreased by some amount."
            )
    ),
    BOUNTY_HUNTER(new BountyHunterPlayerClass(), Material.TARGET,
        List.of(),
        List.of()
    )
//    SIDEWINDER(new SidewinderPlayerClass(), Material.FEATHER,
//            List.of(),
//            List.of()
//    )
    ;

    private final PlayerClass playerClass;
    private final ItemStack icon;
    private final List<Component> itemContents;
    private final List<String> description;

    PlayerClassType(PlayerClass playerClass, Material material, Consumer<ItemMeta> itemMetaHandler, List<Component> itemContents, List<String> description) {
        this.playerClass = playerClass;
        this.itemContents = itemContents;
        this.description = description;

        this.icon = new ItemStack(material);
        this.icon.editMeta(itemMetaHandler);
    }

    PlayerClassType(PlayerClass playerClass, Material material, List<Component> itemContents, List<String> description) {
        this(playerClass, material, _ -> {}, itemContents, description);
    }

    public PlayerClass getPlayerClass() {
        return playerClass;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public List<Component> getItemContents() {
        return itemContents;
    }

    public List<String> getDescription() {
        return description;
    }
}
