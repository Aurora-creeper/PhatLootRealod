package com.codisimus.plugins.phatloots.loot;

import com.codisimus.plugins.phatloots.PhatLoot;
import com.codisimus.plugins.phatloots.PhatLoots;
import com.codisimus.plugins.phatloots.PhatLootsUtil;
import com.codisimus.plugins.phatloots.gui.Tool;
import java.util.*;
import com.tealcube.minecraft.bukkit.mythicdrops.MythicDropsPlugin;
import com.tealcube.minecraft.bukkit.mythicdrops.api.socketting.GemType;
import com.tealcube.minecraft.bukkit.mythicdrops.socketting.SocketGem;
import com.tealcube.minecraft.bukkit.mythicdrops.socketting.SocketItem;
import com.tealcube.minecraft.bukkit.mythicdrops.utils.SocketGemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * A Gem is a SocketGem ItemStack generated by the plugin MythicDrops
 *
 * @author Codisimus
 */
@SerializableAs("Gem")
public class Gem extends Loot {
    private static final String RANDOM_GEM = "RANDOM";
    private static EnumMap<GemType, ArrayList<String>> gemLists = null;
    public GemType gemType = GemType.ANY;
    public String gemName;
    public int amountLower = 1;
    public int amountUpper = 1;
    
    static {
        instantiateGemLists();
    }

    /**
     * Constructs a new Loot with a RANDOM tier
     */
    public Gem() {
        gemName = RANDOM_GEM;
    }

    /**
     * Constructs a new Loot with the given tier
     *
     * @param gemName The name of the MythicDrops Gem
     */
    public Gem(String gemName) {
        this.gemName = gemName;
    }

    /**
     * Constructs a new Loot with the given tier and amount/durability ranges
     *
     * @param gemName The name of the MythicDrops Gem
     * @param amountLower The lower bound of the amount range
     * @param amountUpper The upper bound of the amount range
     */
    public Gem(String gemName, int amountLower, int amountUpper) {
        this.gemName = gemName;
        this.amountLower = amountLower;
        this.amountUpper = amountUpper;
    }

    /**
     * Constructs a new Unidentified Item from a Configuration Serialized phase
     *
     * @param map The map of data values
     */
    public Gem(Map<String, Object> map) {
        String currentLine = null; //The value that is about to be loaded (used for debugging)
        try {
            Object number = map.get(currentLine = "Probability");
            setProbability((number instanceof Double) ? (Double) number : (Integer) number);
            gemName = (String) map.get(currentLine = "Name");
            gemType = GemType.valueOf((String) map.get(currentLine = "Type"));
            if (map.containsKey(currentLine = "Amount")) {
                amountLower = amountUpper = (Integer) map.get(currentLine);
            } else if (map.containsKey(currentLine = "AmountLower")) {
                amountLower = (Integer) map.get(currentLine);
                amountUpper = (Integer) map.get(currentLine = "AmountUpper");
            }
        } catch (Exception ex) {
            //Print debug messages
            PhatLoots.logger.severe("Failed to load Gem line: " + currentLine);
            PhatLoots.logger.severe("of PhatLoot: " + (PhatLoot.current == null ? "unknown" : PhatLoot.current));
            PhatLoots.logger.severe("Last successfull load was...");
            PhatLoots.logger.severe("PhatLoot: " + (PhatLoot.last == null ? "unknown" : PhatLoot.last));
            PhatLoots.logger.severe("Loot: " + (Loot.last == null ? "unknown" : Loot.last.toString()));
        }
    }

    /**
     * Generates a Gem and adds it to the item list
     *
     * @param lootBundle The loot that has been rolled for
     * @param lootingBonus The increased chance of getting rarer loots
     */
    @Override
    public void getLoot(LootBundle lootBundle, double lootingBonus) {
        int amount = PhatLootsUtil.rollForInt(amountLower, amountUpper);
        while (amount > 0) {
            SocketGem socketGem;
            if (gemName.equalsIgnoreCase(RANDOM_GEM)) {
                switch (gemType) {
                case ANY:
                    socketGem = SocketGemUtil.getRandomSocketGemWithChance();
                    break;
                default:
                    instantiateGemLists();
                    ArrayList<String> gemList = gemLists.get(gemType);
                    String socketGemName = gemList.get(PhatLootsUtil.rollForInt(1, gemList.size() - 1));
                    socketGem = SocketGemUtil.getSocketGemFromName(socketGemName);
                    break;
                }
            } else {
                socketGem = SocketGemUtil.getSocketGemFromName(gemName);
            }

            if (socketGem != null) {
                Material material = SocketGemUtil.getRandomSocketGemMaterial();
                ItemStack si = new SocketItem(material, socketGem);
                lootBundle.addItem(si);
            }
            amount--;
        }
    }

    /**
     * Returns the information of the Gem in the form of an ItemStack
     *
     * @return An ItemStack representation of the Loot
     */
    @Override
    public ItemStack getInfoStack() {
        //A Gem is represented by a Diamond
        ItemStack infoStack = new ItemStack(Material.DIAMOND);

        //Set the display name of the item
        ItemMeta info = Bukkit.getItemFactory().getItemMeta(infoStack.getType());
        info.setDisplayName("§2Gem");

        //Add more specific details of the item
        List<String> details = new ArrayList();
        if (!gemName.equalsIgnoreCase(RANDOM_GEM)) {
            details.addAll(SocketGemUtil.getSocketGemFromName(gemName).getLore());
            details.add("§1-----------------------------");
        }
        details.add("§1Name: §6" + gemName);
        details.add("§1Probability: §6" + getProbability());
        if (amountLower == amountUpper) {
            details.add("§1Amount: §6" + amountLower);
        } else {
            details.add("§1Amount: §6" + amountLower + '-' + amountUpper);
        }

        //Construct the ItemStack and return it
        info.setLore(details);
        infoStack.setItemMeta(info);
        return infoStack;
    }

    /**
     * Toggles a Loot setting depending on the type of Click
     *
     * @param click The type of Click (Only SHIFT_LEFT, SHIFT_RIGHT, and MIDDLE are used)
     * @return true if the Loot InfoStack should be refreshed
     */
    @Override
    public boolean onToggle(ClickType click) {
        int ordinal;
        switch (click) {
        case SHIFT_LEFT: //Next GemType
            ordinal = gemType.ordinal() + 1;
            if (ordinal >= GemType.values().length) {
                ordinal = 0;
            }
            break;
        case SHIFT_RIGHT: //Previous GemType
            ordinal = gemType.ordinal() - 1;
            if (ordinal < 0) {
                ordinal = GemType.values().length - 1;
            }
            break;
        case MIDDLE: //Set to RANDOM
            ordinal = 0;
            break;
        default:
            return false;
        }
        gemType = GemType.values()[ordinal];
        ArrayList<String> gemList = gemLists.get(gemType);
        if (!gemList.contains(gemName)) {
            gemName = RANDOM_GEM;
        }
        return false;
    }

    /**
     * Toggles the MythicDrops Tier depending on the type of Click
     *
     * @param tool The Tool that was used to click
     * @param click The type of Click (Only LEFT, RIGHT, MIDDLE, SHIFT_LEFT, SHIFT_RIGHT, and DOUBLE_CLICK are used)
     * @return true if the Loot InfoStack should be refreshed
     */
    @Override
    public boolean onToolClick(Tool tool, ClickType click) {
        if (!tool.getName().equals("MYTHICDROPS")) {
            return false;
        }

        instantiateGemLists();
        ArrayList<String> gemList = gemLists.get(gemType);

        int index = gemList.indexOf(gemName);
        switch (click) {
        case LEFT: //+1
            index++;
            break;
        case DOUBLE_CLICK: //+9
            index += 9;
            break;
        case RIGHT: //-1
            index += -1;
            break;
        case SHIFT_LEFT: //+100
            index += 100;
            break;
        case SHIFT_RIGHT: //-100
            index += -100;
            break;
        case MIDDLE: //default tier
            index = 0;
            break;
        default:
            return false;
        }

        while (index >= gemList.size()) {
            index -= gemList.size();
        }
        while (index < 0) {
            index += gemList.size();
        }

        gemName = gemList.get(index);
        return true;
    }

    private static void instantiateGemLists() {
        if (gemLists == null) {
            gemLists = new EnumMap(GemType.class);

            //LOAD ALL THE GEMS!
            for (SocketGem socketGem : MythicDropsPlugin.getInstance().getSockettingSettings().getSocketGemMap().values()) {
                GemType gemType = socketGem.getGemType();
                if (gemLists.get(gemType) == null) {
                    gemLists.put(gemType, new ArrayList<String>());
                }
                ArrayList<String> gemList = gemLists.get(gemType);
                gemList.add(socketGem.getName());
            }

            //Create the ANY gem list
            if (gemLists.get(GemType.ANY) == null) {
                gemLists.put(GemType.ANY, new ArrayList<String>());
            }
            ArrayList<String> anyGemList = gemLists.get(GemType.ANY);
            for (GemType gemType : GemType.values()) {
                if (gemType != GemType.ANY) {
                    anyGemList.addAll(gemLists.get(gemType));
                }
            }

            //Sort each list alphabetically
            for (GemType gemType : GemType.values()) {
                ArrayList<String> gemList = gemLists.get(gemType);
                Collections.sort(gemList);
                gemList.add(0, RANDOM_GEM);
            }
        }
    }

    /**
     * Modifies the amount associated with the Loot
     *
     * @param amount The amount to modify by (may be negative)
     * @param both true if both lower and upper ranges should be modified, false for only the upper range
     * @return true if the Loot InfoStack should be refreshed
     */
    @Override
    public boolean modifyAmount(int amount, boolean both) {
        if (both) {
            amountLower += amount;
            if (amountLower < 0) {
                amountLower = 0;
            }
        }
        amountUpper += amount;
        //Upper bound cannot be less than lower bound
        if (amountUpper < amountLower) {
            amountUpper = amountLower;
        }
        return true;
    }

    /**
     * Resets the amount of Loot to 1
     *
     * @return true if the Loot InfoStack should be refreshed
     */
    @Override
    public boolean resetAmount() {
        amountLower = 1;
        amountUpper = 1;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(amountLower);
        if (amountLower != amountUpper) {
            sb.append('-');
            sb.append(amountUpper);
        }

        sb.append(" ");
        sb.append(gemName);
        sb.append(" (Gem) ");

        sb.append(" @ ");
        //Only display the decimal values if the probability is not a whole number
        sb.append(Math.floor(getProbability()) == getProbability() ? String.valueOf((int) getProbability()) : String.valueOf(getProbability()));

        sb.append("%");

        return sb.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Gem)) {
            return false;
        }

        Gem loot = (Gem) object;
        return loot.gemName.equals(gemName)
                && loot.amountLower == amountLower
                && loot.amountUpper == amountUpper;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(this.gemName);
        hash = 31 * hash + this.amountLower;
        hash = 31 * hash + this.amountUpper;
        return hash;
    }

    @Override
    public Map<String, Object> serialize() {
        Map map = new TreeMap();
        map.put("Probability", getProbability());
        map.put("Name", gemName);
        map.put("Type", gemType.name());
        if (amountLower == amountUpper) {
            if (amountLower != 1) {
                map.put("Amount", amountLower);
            }
        } else {
            map.put("AmountLower", amountLower);
            map.put("AmountUpper", amountUpper);
        }
        return map;
    }
}
