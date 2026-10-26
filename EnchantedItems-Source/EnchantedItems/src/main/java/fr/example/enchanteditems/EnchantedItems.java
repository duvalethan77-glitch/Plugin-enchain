package fr.example.enchanteditems;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public final class EnchantedItems extends JavaPlugin implements Listener {
    private static final String PREFIX = ChatColor.LIGHT_PURPLE + "[EnchantedItems] " + ChatColor.RESET;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("EnchantedItems est activé !");
    }

    @Override
    public void onDisable() {
        getLogger().info("EnchantedItems est désactivé.");
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!getConfig().getBoolean("allow-incompatible-enchantments", true)
                && !getConfig().getBoolean("allow-unusual-items", true)) {
            return;
        }

        AnvilInventory inventory = event.getInventory();
        ItemStack left = inventory.getItem(0);
        ItemStack right = inventory.getItem(1);
        if (left == null || right == null || left.getType() == Material.AIR || right.getType() == Material.AIR) {
            return;
        }

        ItemStack result = left.clone();
        ItemMeta resultMeta = result.getItemMeta();
        if (resultMeta == null) return;

        Map<Enchantment, Integer> incoming = readEnchantments(right);
        if (incoming.isEmpty()) return;

        boolean changed = false;
        for (Map.Entry<Enchantment, Integer> entry : incoming.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int incomingLevel = entry.getValue();

            if (!getConfig().getBoolean("allow-unusual-items", true)
                    && !enchantment.canEnchantItem(result)) {
                continue;
            }

            if (!getConfig().getBoolean("allow-incompatible-enchantments", true)) {
                boolean conflicts = resultMeta.getEnchants().keySet().stream()
                        .anyMatch(existing -> !existing.equals(enchantment) && existing.conflictsWith(enchantment));
                if (conflicts) continue;
            }

            int oldLevel = resultMeta.getEnchantLevel(enchantment);
            int newLevel = oldLevel == incomingLevel ? oldLevel + 1 : Math.max(oldLevel, incomingLevel);
            if (!getConfig().getBoolean("allow-levels-over-max", true)) {
                newLevel = Math.min(newLevel, enchantment.getMaxLevel());
            }

            if (newLevel > oldLevel) {
                resultMeta.addEnchant(enchantment, newLevel, true);
                changed = true;
            }
        }

        if (!changed) return;
        result.setItemMeta(resultMeta);
        event.setResult(result);
        inventory.setRepairCost(Math.max(1, getConfig().getInt("anvil-minimum-cost", 1)));
    }

    private Map<Enchantment, Integer> readEnchantments(ItemStack stack) {
        Map<Enchantment, Integer> result = new HashMap<>();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return result;

        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            result.putAll(bookMeta.getStoredEnchants());
        } else {
            result.putAll(meta.getEnchants());
        }
        return result;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "Commandes :");
            sender.sendMessage(ChatColor.GRAY + "/ei enchant <enchantement> <niveau> " + ChatColor.WHITE + "- enchante l'objet en main");
            sender.sendMessage(ChatColor.GRAY + "/ei reload " + ChatColor.WHITE + "- recharge la configuration");
            return true;
        }

        if (!sender.hasPermission("enchanteditems.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Tu n'as pas la permission.");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage(PREFIX + ChatColor.GREEN + "Configuration rechargée.");
            return true;
        }

        if (args[0].equalsIgnoreCase("enchant")) {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Cette commande doit être utilisée en jeu.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(PREFIX + ChatColor.YELLOW + "Utilisation : /ei enchant <enchantement> <niveau>");
                sender.sendMessage(ChatColor.GRAY + "Exemple : /ei enchant sharpness 10");
                return true;
            }

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Tiens un objet dans ta main.");
                return true;
            }

            String keyText = args[1].toLowerCase(Locale.ROOT);
            if (keyText.startsWith("minecraft:")) keyText = keyText.substring("minecraft:".length());
            Enchantment enchantment;
            try {
                enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(keyText));
            } catch (IllegalArgumentException ex) {
                enchantment = null;
            }

            if (enchantment == null) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Enchantement inconnu : " + args[1]);
                sender.sendMessage(ChatColor.GRAY + "Utilise les identifiants Minecraft, par exemple sharpness, unbreaking ou mending.");
                return true;
            }

            int level;
            try {
                level = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Le niveau doit être un nombre entier.");
                return true;
            }
            if (level < 1 || level > 255) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Le niveau doit être compris entre 1 et 255.");
                return true;
            }
            if (!getConfig().getBoolean("allow-levels-over-max", true) && level > enchantment.getMaxLevel()) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Le niveau maximum de cet enchantement est " + enchantment.getMaxLevel() + ".");
                return true;
            }
            if (!getConfig().getBoolean("allow-unusual-items", true) && !enchantment.canEnchantItem(item)) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Cet enchantement ne peut pas être appliqué à cet objet selon la configuration.");
                return true;
            }

            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Impossible de modifier cet objet.");
                return true;
            }
            meta.addEnchant(enchantment, level, true);
            item.setItemMeta(meta);
            sender.sendMessage(PREFIX + ChatColor.GREEN + "Enchantement appliqué : "
                    + ChatColor.AQUA + keyText + " " + level + ChatColor.GREEN + ".");
            return true;
        }

        sender.sendMessage(PREFIX + ChatColor.RED + "Commande inconnue. Utilise /ei help.");
        return true;
    }
}
