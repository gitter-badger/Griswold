package com.github.toxuin.griswold;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import static com.github.toxuin.griswold.Griswold.log;

class Interactor {
    static double basicToolsPrice = 10.0;
    static double basicArmorPrice = 10.0;
    static double enchantmentPrice = 30.0;

    static boolean enableEnchants = true;
    static double addEnchantmentPrice = 50.0;
    static int maxEnchantBonus = 5;
    static boolean clearEnchantments = false;

    private final List<Material> repairableTools = new LinkedList<>();
    private final List<Material> repairableArmor = new LinkedList<>();
    private final List<Material> notEnchantable = new LinkedList<>();

    private final Class craftItemStack = ClassProxy.getClass("inventory.CraftItemStack");
    private final Class enchantmentInstance = ClassProxy.getClass("EnchantmentInstance");
    private final Class enchantmentManager = ClassProxy.getClass("EnchantmentManager");

    Interactor() {
        repairableTools.add(Material.IRON_AXE);
        repairableTools.add(Material.IRON_PICKAXE);
        repairableTools.add(Material.IRON_SWORD);
        repairableTools.add(Material.IRON_HOE);
        repairableTools.add(Material.IRON_SPADE);              // IRON TOOLS

        repairableTools.add(Material.WOOD_AXE);
        repairableTools.add(Material.WOOD_PICKAXE);
        repairableTools.add(Material.WOOD_SWORD);
        repairableTools.add(Material.WOOD_HOE);
        repairableTools.add(Material.WOOD_SPADE);              // WOODEN TOOLS

        repairableTools.add(Material.STONE_AXE);
        repairableTools.add(Material.STONE_PICKAXE);
        repairableTools.add(Material.STONE_SWORD);
        repairableTools.add(Material.STONE_HOE);
        repairableTools.add(Material.STONE_SPADE);             // STONE TOOLS

        repairableTools.add(Material.DIAMOND_AXE);
        repairableTools.add(Material.DIAMOND_PICKAXE);
        repairableTools.add(Material.DIAMOND_SWORD);
        repairableTools.add(Material.DIAMOND_HOE);
        repairableTools.add(Material.DIAMOND_SPADE);           // DIAMOND TOOLS

        repairableTools.add(Material.GOLD_AXE);
        repairableTools.add(Material.GOLD_PICKAXE);
        repairableTools.add(Material.GOLD_SWORD);
        repairableTools.add(Material.GOLD_HOE);
        repairableTools.add(Material.GOLD_SPADE);               // GOLDEN TOOLS

        repairableTools.add(Material.BOW);                      // BOW

        repairableTools.add(Material.FLINT_AND_STEEL);          // ZIPPO
        repairableTools.add(Material.SHEARS);                   // SCISSORS
        repairableTools.add(Material.FISHING_ROD);              // FISHING ROD
        repairableTools.add(Material.BOOK);                     // BOOK
        repairableTools.add(Material.ENCHANTED_BOOK);

        notEnchantable.add(Material.FLINT_AND_STEEL);
        notEnchantable.add(Material.SHEARS);
        notEnchantable.add(Material.FISHING_ROD);

        // ARMORZ!
        repairableArmor.add(Material.LEATHER_BOOTS);
        repairableArmor.add(Material.LEATHER_CHESTPLATE);
        repairableArmor.add(Material.LEATHER_HELMET);
        repairableArmor.add(Material.LEATHER_LEGGINGS);

        repairableArmor.add(Material.CHAINMAIL_BOOTS);
        repairableArmor.add(Material.CHAINMAIL_CHESTPLATE);
        repairableArmor.add(Material.CHAINMAIL_HELMET);
        repairableArmor.add(Material.CHAINMAIL_LEGGINGS);

        repairableArmor.add(Material.IRON_BOOTS);
        repairableArmor.add(Material.IRON_CHESTPLATE);
        repairableArmor.add(Material.IRON_HELMET);
        repairableArmor.add(Material.IRON_LEGGINGS);

        repairableArmor.add(Material.GOLD_BOOTS);
        repairableArmor.add(Material.GOLD_CHESTPLATE);
        repairableArmor.add(Material.GOLD_HELMET);
        repairableArmor.add(Material.GOLD_LEGGINGS);

        repairableArmor.add(Material.DIAMOND_BOOTS);
        repairableArmor.add(Material.DIAMOND_CHESTPLATE);
        repairableArmor.add(Material.DIAMOND_HELMET);
        repairableArmor.add(Material.DIAMOND_LEGGINGS);

        File configFile = new File(Griswold.directory, "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if (configFile.exists()) loadConfigItems(config);
    }

    private void loadConfigItems(final YamlConfiguration config) {
        if (config.isConfigurationSection("CustomItems.Tools")) {
            Set<String> tools = config.getConfigurationSection("CustomItems.Tools").getKeys(false);
            for (String itemId : tools) {
                Material mat;
                if (isInteger(itemId)) {  // IS ITEM ID
                    mat = Material.getMaterial(Integer.parseInt(itemId));
                } else { // IS ITEM NAME
                    mat = Material.getMaterial(itemId);
                }
                if (mat == null) {
                    log.severe("WARNING: YOU HAVE A BAD ITEM ID IN YOUR CustomTools.Tools CONFIG: " + itemId);
                    continue;
                }
                repairableTools.add(mat); // BY NAME
            }
            log.info("Added " + tools.size() + " custom tools from config file");
        }

        if (config.isConfigurationSection("CustomItems.Armor")) {
            Set<String> armor = config.getConfigurationSection("CustomItems.Armor").getKeys(false);
            for (String itemId : armor) {
                Material mat;
                if (isInteger(itemId)) {  // IS ITEM ID
                    mat = Material.getMaterial(Integer.parseInt(itemId));
                } else { // IS ITEM NAME
                    mat = Material.getMaterial(itemId);
                }
                if (mat == null) {
                    log.severe("WARNING: YOU HAVE A BAD ITEM ID IN YOUR CustomTools.Armor CONFIG: " + itemId);
                    continue;
                }
                repairableArmor.add(mat);
            }
            log.info("Added " + armor.size() + " custom armors from config file");
        }
    }

    private boolean isInteger(String number) {
        if (number == null) return false;
        try {
            int i = Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private final Set<Interaction> interactions = new HashSet<>();

    @SuppressWarnings("unchecked")
    public void interact(Player player, Repairer repairman) {
        final ItemStack item = player.getInventory().getItemInMainHand();

        repairman.haggle();

        double price = Math.round(getPrice(repairman, item));

        if (item.getType() == Material.AIR) {
            player.sendMessage(String.format(Lang.name_format, repairman.name) + Lang.chat_noitem);
            return;
        }

        if (!checkCanRepair(player, repairman, item)) {
            player.sendMessage(String.format(Lang.name_format, repairman.name) + Lang.chat_cannot);
            return;
        }

        Interaction interaction = new Interaction(player.getUniqueId(), repairman.entity, item, item.getDurability(), System.currentTimeMillis());

        if (interactions.contains(interaction)) {
            interactFirstTime(interaction, repairman);
        } else interactSecondTime(interaction, repairman);


        // INTERACTS FIRST TIME

        if (interactions.size() > 10) interactions.clear(); // THIS SUCKS, I KNOW

        if (item.getDurability() != 0) {
            // NEEDS REPAIR
            if (!repairman.type.equalsIgnoreCase("enchant") && !repairman.type.equalsIgnoreCase("all")) {
                // CANNOT REPAIR
                player.sendMessage(String.format(Lang.name_format, repairman.name) + Lang.chat_needs_repair);
                return;
            }

            // CAN REPAIR
            interactions.add(interaction);
            if (Griswold.economy != null)
                player.sendMessage(String.format(ChatColor.GOLD + "<" + repairman.name + "> " + ChatColor.WHITE +
                        Lang.chat_cost, price, Griswold.economy.currencyNamePlural()));
            else player.sendMessage(String.format(Lang.name_format, repairman.name) + Lang.chat_free);
            player.sendMessage(String.format(Lang.name_format, repairman.name) + Lang.chat_agreed);
        } else {
            // NEEDS ENCHANT
            if (!(enableEnchants && !notEnchantable.contains(item.getType()) && (repairableTools.contains(item.getType()) || repairableArmor.contains(item.getType())))) {
                // ENCHANTS DISABLED
                player.sendMessage(String.format(Lang.name_format, repairman.name) + Lang.chat_norepair); // NO REPAIR NEEDED, CAN NOT ENCHANT
                return;
            }

            // ENCHANTS ENABLED AND THINGY IS ENCHANTABLE
            price = addEnchantmentPrice;
            if (!repairman.type.equalsIgnoreCase("enchant") && !repairman.type.equalsIgnoreCase("all")) {
                // CANNOT ENCHANT
                player.sendMessage(String.format(Lang.name_format, repairman.name) + Lang.chat_norepair); // NO REPAIR NEEDED, CAN NOT ENCHANT
                return;
            }
            // CAN ENCHANT
            interactions.add(interaction);
            if (Griswold.economy != null)
                player.sendMessage(String.format(String.format(Lang.name_format, repairman.name) +
                        Lang.chat_enchant_cost, price, Griswold.economy.currencyNamePlural()));
            else player.sendMessage(String.format(Lang.name_format, repairman.name) + Lang.chat_enchant_free);
            player.sendMessage(String.format(Lang.name_format, repairman.name) + Lang.chat_agreed);
        }
    }

    private void interactFirstTime(Interaction interaction, Repairer repairman) {

    }

    private void interactSecondTime(Interaction interaction, Repairer repairman) {
        ItemStack item = interaction.getItem();
        Player player = Bukkit.getServer().getPlayer(interaction.getPlayer());
        double price = Math.round(getPrice(repairman, item));

        if (item.getDurability() != 0) { // ITEM NEEDS FIX
            List<String> list = Arrays.asList("armor", "tools", "both", "all");
            if (!list.contains(repairman.type.toLowerCase())) {
                player.sendMessage(String.format(Lang.name_format, repairman.name) + ChatColor.RED + Lang.chat_cannot);
                interactions.remove(interaction); // INVALIDATE INTERACTION
                return;
            }

            try {
                if (!chargeEconomy(player, price, repairman.name)) {
                    player.sendMessage(String.format(Lang.name_format, repairman.name) + ChatColor.RED + Lang.chat_error);
                    interactions.remove(interaction); // INVALIDATE INTERACTION
                    return;
                }
                item.setDurability((short) 0); // REPAIR THE ITEM!
                player.sendMessage(String.format(Lang.name_format, repairman.name) + Lang.chat_done);
                return;
            } catch (NotEnoughMoneyException e) {
                player.sendMessage(String.format(Lang.name_format, repairman.name) + ChatColor.RED + Lang.chat_poor);
            } finally {
                interactions.remove(interaction); // INVALIDATE INTERACTION
            }
            return;

        } else { // ITEM NEEDS ENCHANT
            if (!enableEnchants) {
                player.sendMessage(String.format(Lang.name_format, repairman.name) + ChatColor.RED + Lang.chat_cannot);
                interactions.remove(interaction); // INVALIDATE INTERACTION
                return;
            }
            List<String> suitedTypes = Arrays.asList("enchant", "all");
            if (!suitedTypes.contains(repairman.type.toLowerCase())) {
                player.sendMessage(String.format(Lang.name_format, repairman.name) + ChatColor.RED + Lang.chat_cannot);
                interactions.remove(interaction); // INVALIDATE INTERACTION
                return;
            }

            price = addEnchantmentPrice;

            try {
                if (!chargeEconomy(player, price, repairman.name)) {
                    player.sendMessage(String.format(Lang.name_format, repairman.name) + ChatColor.RED + Lang.chat_error);
                    interactions.remove(interaction); // INVALIDATE INTERACTION
                    return;
                }

                if (clearEnchantments) item.getEnchantments().forEach((enchantToDel, integer) ->
                        item.removeEnchantment(enchantToDel));


                if (item.getType().equals(Material.BOOK)) {
                    ItemStack bookLeftovers = null;
                    if (item.getAmount() > 1)
                        bookLeftovers = new ItemStack(Material.BOOK, item.getAmount() - 1);
                    player.getInventory().remove(item);
                    item.setType(Material.ENCHANTED_BOOK);
                    item.setAmount(1);
                    EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta) item.getItemMeta();
                }



                if (item.getType().equals(Material.BOOK)) {



                } else if (item.getType().equals(Material.ENCHANTED_BOOK)) {
                    bookmeta = (EnchantmentStorageMeta) item.getItemMeta();
                }

                if (list == null) {
                    interactions.remove(interaction); // INVALIDATE INTERACTION
                    player.sendMessage(String.format(Lang.name_format, repairman.name) + Lang.chat_enchant_failed);
                    return;
                }

                if (item.getType().equals(Material.ENCHANTED_BOOK) && bookmeta != null) {
                    bookmeta.addStoredEnchant(Enchantment.getById(Integer.parseInt(idField.get(enchantment).toString())), Integer.parseInt(levelField.get(instance).toString()), true);
                } else {
                    item.addEnchantment(Enchantment.getById(Integer.parseInt(idField.get(enchantment).toString())), Integer.parseInt(levelField.get(instance).toString()));
                }

                if (item.getType().equals(Material.ENCHANTED_BOOK)) {
                    item.setItemMeta(bookmeta);
                    player.getInventory().setItemInMainHand(item);
                    if (bookLeftovers != null) {
                        if (player.getInventory().firstEmpty() == -1) { // INVENTORY FULL, DROP TO PLAYER
                            player.getWorld().dropItemNaturally(player.getLocation(), bookLeftovers);
                        } else player.getInventory().addItem(bookLeftovers);
                    }
                }

                interactions.remove(interaction); // INVALIDATE INTERACTION
                player.sendMessage(String.format(Lang.name_format, repairman.name) + Lang.chat_enchant_success);


            } catch (NotEnoughMoneyException e) {
                player.sendMessage(String.format(Lang.name_format, repairman.name) + ChatColor.RED + Lang.chat_poor);
                return;
            } finally {
                interactions.remove(interaction); // INVALIDATE INTERACTION
            }

        }
    }

    private boolean chargeEconomy(Player player, double amount, String sender) {
        if (Griswold.economy == null) return true; // ALL FINE!
        EconomyResponse r;
        if (Griswold.economy.getBalance(player) < amount) throw new NotEnoughMoneyException();
        player.sendMessage(String.format(Lang.name_format, sender) + Lang.chat_poor);

        r = Griswold.economy.withdrawPlayer(player, amount);
        return r.transactionSuccess();
    }

    private boolean checkCanRepair(Player player, Repairer repairman, ItemStack item) {
        if (repairman.type.equalsIgnoreCase("all")) {
            if (item.getDurability() != 0) {
                if (repairableArmor.contains(item.getType())) {
                    // check for armor perm
                    return player.hasPermission("griswold.armor");
                } else {
                    return (repairableTools.contains(item.getType()) &&       // check tools perm
                            player.hasPermission("griswold.tools"));
                }
            } else {
                return player.hasPermission("griswold.enchant");
            }
        } else if (repairman.type.equalsIgnoreCase("both")) {
            if (repairableArmor.contains(item.getType())) {
                return player.hasPermission("griswold.armor");
            } else {
                return (repairableTools.contains(item.getType()) &&
                        player.hasPermission("griswold.tools"));
            }
        } else if (repairman.type.equalsIgnoreCase("tools")) {
            return player.hasPermission("griswold.tools");
        } else if (repairman.type.equalsIgnoreCase("armor")) {
            return player.hasPermission("griswold.armor");
        } else if (repairman.type.equalsIgnoreCase("enchant")) {
            return player.hasPermission("griswold.enchant");
        }
        return false;
    }

    private double getPrice(Repairer repairman, ItemStack item) {
        if (Griswold.economy == null) return 0.0;
        double price = 0;
        if (repairableTools.contains(item.getType())) price = basicToolsPrice;
        else if (repairableTools.contains(item.getType())) price = basicArmorPrice;

        price += item.getDurability();

        Map<Enchantment, Integer> enchantments = item.getEnchantments();

        if (!enchantments.isEmpty()) {
            for (int i = 0; i < enchantments.size(); i++) {
                Object[] enchantsLevels = enchantments.values().toArray();
                price = price + enchantmentPrice * Integer.parseInt(enchantsLevels[i].toString());
            }
        }
        return price * repairman.cost;
    }

    private void applyRandomEnchantment(ItemStack item) {
        List<Enchantment> candidates = new ArrayList<>();

        Arrays.asList(Enchantment.values()).forEach((ench) -> { // I guess collections are overrated in bukkit
            if (ench.canEnchantItem(item)) candidates.add(ench);
        });

        item.getEnchantments().keySet().forEach((applied) ->
                candidates.removeIf((candidate) -> candidate.conflictsWith(applied)));

        Collections.shuffle(candidates);

        final Enchantment ench = candidates.get(0);
        final int level = (ench.getStartLevel() == ench.getMaxLevel()) ? ench.getMaxLevel() :
                ThreadLocalRandom.current().nextInt(ench.getStartLevel(), ench.getMaxLevel());

        item.addEnchantment(ench, level);
    }

    protected void finalize() throws Throwable {
        repairableArmor.clear();
        repairableTools.clear();
        notEnchantable.clear();
        super.finalize();
    }
}

class Interaction {
    private final UUID player;
    private final Entity repairman;
    private final ItemStack item;
    private final int damage;
    private final long time;
    private boolean valid;

    Interaction(UUID playerId, Entity repairman, ItemStack item, int dmg, long time) {
        this.item = item;
        this.damage = dmg;
        this.player = playerId;
        this.repairman = repairman;
        this.time = time;
        this.valid = true;
    }

    public UUID getPlayer() {
        return player;
    }

    public Entity getRepairman() {
        return repairman;
    }

    public ItemStack getItem() {
        return item;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Interaction)) return false;
        Interaction other = (Interaction) obj;
        int delta = (int) (time - other.time);
        return ((other.item.equals(item)) &&
                (other.valid) &&
                (other.damage == damage) &&
                (other.player.equals(player)) &&
                (other.repairman.equals(repairman)) &&
                (delta < Griswold.timeout));
    }
    
    @Override
    public int hashCode() {
        int result = player != null ? player.hashCode() : 0;
        result = 31 * result + (repairman != null ? repairman.hashCode() : 0);
        result = 31 * result + (item != null ? item.hashCode() : 0);
        result = 31 * result + damage;
        result = 31 * result + (int) (time ^ (time >>> 32));
        result = 31 * result + (valid ? 1 : 0);
        return result;
    }
}