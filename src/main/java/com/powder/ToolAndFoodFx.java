package com.powder.ToolAndFoodFX;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.potion.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ToolAndFoodFX extends JavaPlugin implements Listener
{

	protected HashMap<Projectile, ItemStack> proj;
	public static String FXID = "\u00a79» Effects:";
	private static List<String> numbers0To255;
	private static List<String> numbers0To10000;
	private static Set<Integer> customFoods;
	private static Set<Integer> customWeapons;
	private static Set<Integer> customArmor;

	@Override
	public void onEnable()
	{
		ToolAndFoodFX.numbers0To255 = new ArrayList<String>();
		ToolAndFoodFX.numbers0To10000 = new ArrayList<String>();
		for (int i = 0; i < 256; i++)
			ToolAndFoodFX.numbers0To255.add(String.valueOf(i));
		for (int base = 0; base < 5; base++)
			for (int num = 1; num < 10; num++)
				ToolAndFoodFX.numbers0To10000.add(String.valueOf((int) (Math.pow(10, base) * num)));
		this.saveDefaultConfig();
		customFoods = new HashSet<Integer>();
		customWeapons = new HashSet<Integer>();
		customArmor = new HashSet<Integer>();
		for (int i: this.getConfig().getIntegerList("FoodItems")) {
			customFoods.add(i);
		}
		for (int i: this.getConfig().getIntegerList("WeaponItems")) {
			customWeapons.add(i);
		}
		for (int i: this.getConfig().getIntegerList("ArmorItems")) {
			customArmor.add(i);
		}
		this.proj = new HashMap<Projectile, ItemStack>();
		Bukkit.getPluginManager().registerEvents(this, this);
		new projectileRemover(this).runTaskTimer(this, 1, 10);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (cmd.getName().equalsIgnoreCase("addeffect"))
		{
			if (!(sender instanceof Player)) {sender.sendMessage("\u00a7cYou must be a player to use this command."); return true;}
			if (args.length > 4 | args.length < 3) {sender.sendMessage("\u00a76Usage: \u00a7c/addeffect <effect type> <level> <duration> [-wear]"); return true;}
			int lvl, dur;
			PotionEffectType type;
			try
			{
				lvl = Integer.valueOf(args[1]);
				dur = Integer.valueOf(args[2]);
				type = PotionEffectType.getByName(args[0]);
			}
			catch (NumberFormatException e) {sender.sendMessage("\u00a7cError: invalid number"); return true;}
			if (type == null) {sender.sendMessage("\u00a7cError: invalid potion effect"); return true;}
			if (!sender.hasPermission("toolandfoodfx.command.addeffect.*"))
				if (!sender.hasPermission("toolandfoodfx.command.addeffect." + type.getName().toLowerCase()))
					{sender.sendMessage("\u00a74You do not have permission to use this type of effect."); return true;}
			ItemStack out = ((Player) sender).getInventory().getItemInMainHand();
			if (out == null) {sender.sendMessage("\u00a7cYou must be holding an item."); return true;}
			if (!canApplyEffect(out)) {sender.sendMessage("\u00a7cYou cannot apply an effect to this item."); return true;}
			ItemMeta meta = out.getItemMeta();
			String color = "\u00a7c";
			if (args.length == 4 && args[3].equalsIgnoreCase("-wear"))
				color = "\u00a73";
			else if (args.length == 4)
				{sender.sendMessage("\u00a7cInvalid parameter"); return true;}
			if (!meta.hasLore())
			{
				List<String> lore = new ArrayList<String>();
				lore.add(ToolAndFoodFX.FXID);
				lore.add(color + type.getName().charAt(0) + type.getName().toLowerCase().substring(1) + ", " + lvl + ", " + dur);
				meta.setLore(lore);
			}
			else if (!meta.getLore().get(0).contains("Effects"))
			{
				List<String> lore = new ArrayList<String>();
				lore.add(ToolAndFoodFX.FXID);
				lore.add(color + type.getName().charAt(0) + type.getName().toLowerCase().substring(1) + ", " + lvl + ", " + dur);
				meta.setLore(lore);
			}
			else
			{
				boolean exist = false;
				List<String> lore = meta.getLore();
				for (int i = 1; i < lore.size(); i++)
					if (lore.get(i).length() > 2 && lore.get(i).substring(2).split(", ").length == 3 && (type == PotionEffectType.getByName(lore.get(i).substring(2).split(", ")[0]) & lore.get(i).startsWith(color)))
					{
						exist = true;
						lore.set(i, color + type.getName().charAt(0) + type.getName().toLowerCase().substring(1) + ", " + lvl + ", " + dur);
						break;
					}
				if (!exist)
					lore.add(color + type.getName().charAt(0) + type.getName().toLowerCase().substring(1) + ", " + lvl + ", " + dur);
				meta.setLore(lore);
			}
			out.setItemMeta(meta);
			sender.sendMessage("\u00a7bYou have added:\n" + type.getName().toLowerCase() + ", level: " + lvl + ", duration: " + dur + " seconds\nto: " + out.getType().toString());
			return true;
		}
		if (cmd.getName().equalsIgnoreCase("deleffect"))
		{
			if (!(sender instanceof Player)) {sender.sendMessage("\u00a7cYou must be a player to use this command."); return true;}
			if (args.length > 2 | args.length < 1) {sender.sendMessage("\u00a76Usage: \u00a7c/deleffect <effect type|all> [-wear]"); return true;}
			if (!sender.hasPermission("toolandfoodfx.command.deleffect")) {sender.sendMessage("\u00a74You do not have permission to use this command."); return true;}
			ItemStack out = ((Player) sender).getInventory().getItemInMainHand();
			if (out == null) {sender.sendMessage("\u00a7cYou must be holding an item."); return true;}
			if (!canApplyEffect(out)) {sender.sendMessage("\u00a7cYou cannot delete an effect from this item."); return true;}
			ItemMeta meta = out.getItemMeta();
			String color = "\u00a7c";
			if (args.length == 2 && args[1].equalsIgnoreCase("-wear"))
				color = "\u00a73";
			else if (args.length == 2)
				{sender.sendMessage("\u00a7cInvalid parameter"); return true;}
			if (args[0].equalsIgnoreCase("all"))
			{
				List<String> lore = meta.getLore();
				if (lore.size() == 0) {sender.sendMessage("\u00a7cThere are no effects to delete."); return true;}
				lore.clear();
				meta.setLore(lore);
				out.setItemMeta(meta);
				sender.sendMessage("\u00a7aYou have deleted all effects.");
				return true;
			}
			PotionEffectType type = PotionEffectType.getByName(args[0]);
			if (type == null) {sender.sendMessage("\u00a7cError: invalid potion effect"); return true;}
			if (!meta.hasLore()) {sender.sendMessage("\u00a7cThere are no effects to delete."); return true;}
			else if (!meta.getLore().get(0).contains("Effects")) { sender.sendMessage("\u00a7cThere are no effects to delete."); return true;}
			else
			{
				List<String> lore = meta.getLore();
				boolean exist = false;
				for (int i = 1; i < lore.size(); i++)
					if (lore.get(i).length() > 2 && lore.get(i).substring(2).split(", ").length == 3 && (type == PotionEffectType.getByName(lore.get(i).substring(2).split(", ")[0]) & lore.get(i).startsWith(color)))
					{
						lore.remove(i);
						exist = true;
						break;
					}
				if (!exist)
				{
					sender.sendMessage("\u00a7cThat effect was not on the item.");
					return true;
				}
				meta.setLore(lore);
			}
			out.setItemMeta(meta);
			sender.sendMessage("\u00a7aYou have deleted " + type.getName() + " from " + out.getType().toString());
			return true;
		}
		if (cmd.getName().equalsIgnoreCase("reloadtfx"))
		{
			if (!sender.hasPermission("toolandfoodfx.command.reload")) {sender.sendMessage("\u00a74You do not have permission to use this command."); return true;}
			this.reloadConfig();
			customFoods = new HashSet<Integer>();
			customWeapons = new HashSet<Integer>();
			customArmor = new HashSet<Integer>();
			for (int i: this.getConfig().getIntegerList("FoodItems")) {
				customFoods.add(i);
			}
			for (int i: this.getConfig().getIntegerList("WeaponItems")) {
				customWeapons.add(i);
			}
			for (int i: this.getConfig().getIntegerList("ArmorItems")) {
				customArmor.add(i);
			}
			sender.sendMessage("\u00a7aConfig has been reloaded.");
			return true;
		}
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) 
	{
		List<String> toreturn = new ArrayList<String>();
		toreturn.clear();
		if (command.getName().equalsIgnoreCase("addeffect") | command.getName().equalsIgnoreCase("deleffect"))
		{
			if ((args.length == 4 & command.getName().equalsIgnoreCase("addeffect")) | (args.length == 2 & command.getName().equalsIgnoreCase("deleffect")))
				toreturn.add("-wear");
			if (args.length == 2 & command.getName().equalsIgnoreCase("addeffect"))
				toreturn = new ArrayList<String>(ToolAndFoodFX.numbers0To255);
			if (args.length == 3 & command.getName().equalsIgnoreCase("addeffect"))
				toreturn = new ArrayList<String>(ToolAndFoodFX.numbers0To10000);
			if (args.length == 1)
				for (int i = 1; i < PotionEffectType.values().length; i++)
					toreturn.add(PotionEffectType.values()[i].getName());
			for (int ind = 0; ind < toreturn.size();)
				if (!toreturn.get(ind).toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
					toreturn.remove(ind);
				else
					ind++;
		}
		return toreturn;
	}

	protected static void ApplyFX(LivingEntity en, ItemStack item, String pref, boolean tmp)
	{
		if (item.getItemMeta() == null) return;
		List<String> Lore = item.getItemMeta().getLore();
		if (Lore != null && Lore.size() > 1 && Lore.get(0).contains("Effects"))
		{
			for (int i = 1; i < Lore.size(); i++)
			{
				if (!Lore.get(i).startsWith(pref))
					continue;
				String[] elem;
				if (Lore.get(i).startsWith("\u00a7"))
					elem = Lore.get(i).substring(2).split(", ");
				else
					elem = Lore.get(i).split(", ");
				if (elem.length == 3)
				{
					try {
						if (tmp)
							ToolAndFoodFX.AddPotionFX(en, new PotionEffect(PotionEffectType.getByName(elem[0].toUpperCase()), 20, Integer.valueOf(elem[1]), false));
						else
							ToolAndFoodFX.AddPotionFX(en, new PotionEffect(PotionEffectType.getByName(elem[0].toUpperCase()), Integer.valueOf(elem[2]) * 20, Integer.valueOf(elem[1]), false));
					}
					catch(NumberFormatException e) {}
					catch(IllegalArgumentException e) {}
				}
			}
		}
	}

	private static void AddPotionFX(LivingEntity en, PotionEffect e)
	{
		boolean exist = false;
		for (PotionEffect p : en.getActivePotionEffects())
			if (p.getType() == e.getType())
			{
				exist = true;
				if (e.getAmplifier() > p.getAmplifier() | (e.getAmplifier() == p.getAmplifier() & e.getDuration() > p.getDuration()) | p.getDuration() < 20)
					en.addPotionEffect(e, true);
			}
		if (!exist)
			en.addPotionEffect(e);
	}

	@EventHandler (priority = EventPriority.MONITOR)
	public void onPlayerItemConsume(PlayerItemConsumeEvent event)
	{
		if (customFoods.contains(event.getItem().getTypeId()) || event.getItem().getType().isEdible())
		{
			if (!event.getPlayer().hasPermission("toolandfoodfx.use.food")) return;
			ToolAndFoodFX.ApplyFX(event.getPlayer(), event.getItem(), "\u00a7c", false);
		}
	}

	@EventHandler (priority = EventPriority.MONITOR)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
	{
		if (event.getDamager() instanceof Player & event.getEntity() instanceof LivingEntity)
		{
			Player damager = (Player) event.getDamager();
			if (ToolAndFoodFX.isToolOrWeapon(damager.getInventory().getItemInMainHand()))
			{
				if (!damager.hasPermission("toolandfoodfx.use.weapon")) return;
				ToolAndFoodFX.ApplyFX((LivingEntity) event.getEntity(), damager.getInventory().getItemInMainHand(), "\u00a7c", false);
			}
		}
		if (event.getDamager() instanceof LivingEntity & event.getEntity() instanceof Player)
		{
			Player damaged = (Player) event.getEntity();
			if (!damaged.hasPermission("toolandfoodfx.use.armor")) return;
			for (ItemStack i : damaged.getInventory().getArmorContents())
				ToolAndFoodFX.ApplyFX((LivingEntity) event.getDamager(), i, "\u00a7c", false);
		}
		if (event.getDamager() instanceof Projectile & event.getEntity() instanceof LivingEntity)
		{
			Projectile proj = (Projectile) event.getDamager();
			if ((proj.getShooter() instanceof Player) & (this.proj.get(proj) != null))
			{
				ToolAndFoodFX.ApplyFX((LivingEntity) event.getEntity(), this.proj.get(proj), "\u00a7c", false);
				this.proj.remove(proj);
			}
		}
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onProjectileLaunch(ProjectileLaunchEvent event)
	{
		if (event.getEntity().getShooter() instanceof Player & ToolAndFoodFX.isProjectile(event.getEntityType()))
		{
			Player shooter = (Player) event.getEntity().getShooter();
			if (!shooter.hasPermission("toolandfoodfx.use.weapon")) return;
			if (shooter.getInventory().getItemInMainHand().getItemMeta().hasLore())
				this.proj.put(event.getEntity(), shooter.getInventory().getItemInMainHand());
		}
	}

	@SuppressWarnings("deprecation")
	protected boolean canApplyEffect(ItemStack i)
	{
		if (customFoods.contains(i.getTypeId()))
			return true;
		if (customWeapons.contains(i.getTypeId()))
			return true;
		if (customArmor.contains(i.getTypeId()))
			return true;
		if (ToolAndFoodFX.isToolItemOrProjectile(i.getType()))
			return true;
		return false;
	}
	
	protected static boolean isToolItemOrProjectile(Material m) {
		if (ToolAndFoodFX.isTool(m) | m.isEdible()) return true;

		switch(m) {

		case LEATHER_HELMET: case LEATHER_CHESTPLATE: case LEATHER_LEGGINGS: case LEATHER_BOOTS:
		case IRON_HELMET: case IRON_CHESTPLATE: case IRON_LEGGINGS: case IRON_BOOTS:
		case CHAINMAIL_HELMET: case CHAINMAIL_CHESTPLATE: case CHAINMAIL_LEGGINGS: case CHAINMAIL_BOOTS:
		case GOLD_HELMET: case GOLD_CHESTPLATE: case GOLD_LEGGINGS: case GOLD_BOOTS:
		case DIAMOND_HELMET: case DIAMOND_CHESTPLATE: case DIAMOND_LEGGINGS: case DIAMOND_BOOTS:
		case FISHING_ROD: case BOW: case SNOW_BALL: case EGG:

			return true;
		default:
			return false;
		}
	}

	protected static boolean isProjectile(EntityType e)
	{
		switch (e) {

		case ARROW:
		case SNOWBALL:
		case FISHING_HOOK:
		case EGG:

			return true;
		default:
			return false;
		}
	}

	protected static boolean isTool(Material m)
	{
		switch (m) {

		case WOOD_SPADE: case WOOD_AXE: case WOOD_PICKAXE: case WOOD_HOE: case WOOD_SWORD:
		case STONE_SPADE: case STONE_AXE: case STONE_PICKAXE: case STONE_HOE: case STONE_SWORD:
		case IRON_SPADE: case IRON_AXE: case IRON_PICKAXE: case IRON_HOE: case IRON_SWORD:
		case GOLD_SPADE: case GOLD_AXE: case GOLD_PICKAXE: case GOLD_HOE: case GOLD_SWORD:
		case DIAMOND_SPADE: case DIAMOND_AXE: case DIAMOND_PICKAXE: case DIAMOND_HOE: case DIAMOND_SWORD:
		case FLINT_AND_STEEL: case SHEARS: case STICK:

			return true;
		default:
			return false;
		}
	}

	protected static boolean isToolOrWeapon(ItemStack m)
	{
		if (customWeapons.contains(m.getTypeId())) return true;
		if (isTool(m.getType())) return true;
		return false;
	}
}

class projectileRemover extends BukkitRunnable
{
	ToolAndFoodFX plugin;
	public projectileRemover(ToolAndFoodFX instance)
	{
		this.plugin = instance;
	}

	@Override
	public void run()
	{
		try {
			for (Entity e : plugin.proj.keySet())
				if (e.isDead())
					plugin.proj.remove(e);
		}
		catch (ConcurrentModificationException e) {}
		for (Player p : plugin.getServer().getOnlinePlayers())
			if (p.hasPermission("toolandfoodfx.use.wear"))
			{
				for (ItemStack i : p.getInventory().getArmorContents())
					ToolAndFoodFX.ApplyFX(p, i, "\u00a73", true);
				if (p.getInventory().getItemInMainHand() != null)
					if (ToolAndFoodFX.isToolOrWeapon(p.getInventory().getItemInMainHand()) | p.getInventory().getItemInMainHand().getType() == Material.BOW)
						ToolAndFoodFX.ApplyFX(p, p.getInventory().getItemInMainHand(), "\u00a73", true);
			}
	}
}
