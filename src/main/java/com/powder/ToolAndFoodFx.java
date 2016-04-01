package com.powder.ToolAndFoodFX;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.*;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ToolAndFoodFX extends JavaPlugin implements Listener
{
	public static String FXID = "\u00a79» Effects:";
	private static List<String> numbers0To255;
	private static List<String> numbers0To10000;
	private static Set<String> customFoods;
	private static Set<String> customWeapons;
	private static Set<String> customArmor;
	
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
		customFoods = new HashSet<String>();
		customWeapons = new HashSet<String>();
		customArmor = new HashSet<String>();
		for (String i: this.getConfig().getStringList("FoodItems")) {
			customFoods.add(i.toUpperCase());
		}
		for (String i: this.getConfig().getStringList("WeaponItems")) {
			customWeapons.add(i.toUpperCase());
		}
		for (String i: this.getConfig().getStringList("ArmorItems")) {
			customArmor.add(i.toUpperCase());
		}
		//this.proj = new HashMap<Projectile, ItemStack>();
		Bukkit.getPluginManager().registerEvents(this, this);
		new EffectApplicationTimer(this).runTaskTimer(this, 1, 10);
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
			customFoods = new HashSet<String>();
			customWeapons = new HashSet<String>();
			customArmor = new HashSet<String>();
			for (String i: this.getConfig().getStringList("FoodItems")) {
				customFoods.add(i.toUpperCase());
			}
			for (String i: this.getConfig().getStringList("WeaponItems")) {
				customWeapons.add(i.toUpperCase());
			}
			for (String i: this.getConfig().getStringList("ArmorItems")) {
				customArmor.add(i.toUpperCase());
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
	
	protected static boolean isEffectHolderItem(ItemStack item) {
		if (item == null || !item.hasItemMeta()) return false;
		if (!item.getItemMeta().hasLore()) return false;
		List<String> Lore = item.getItemMeta().getLore();
		if (Lore != null && Lore.size() > 1 && Lore.get(0).contains("Effects"))
			return true;
		return false;
	}
	
	protected static void applyEffects(Entity en, ItemStack item, String pref, boolean tmp)
	{
		if (item == null || !item.hasItemMeta()) return;
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
							ToolAndFoodFX.tryAddIndividualEffect(en, new PotionEffect(PotionEffectType.getByName(elem[0].toUpperCase()), 20, Integer.valueOf(elem[1]), false));
						else
							ToolAndFoodFX.tryAddIndividualEffect(en, new PotionEffect(PotionEffectType.getByName(elem[0].toUpperCase()), Integer.valueOf(elem[2]) * 20, Integer.valueOf(elem[1]), false));
					}
					catch(NumberFormatException e) {}
					catch(IllegalArgumentException e) {}
				}
			}
		}
	}

	private static void tryAddIndividualEffect(Entity target, PotionEffect e)
	{
		if (target instanceof LivingEntity) {
			LivingEntity en = (LivingEntity) target;
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
		} else if (target instanceof TippedArrow) {
			TippedArrow en = (TippedArrow) target;
			boolean exist = false;
			for (PotionEffect p : en.getCustomEffects())
				if (p.getType() == e.getType())
				{
					exist = true;
					if (e.getAmplifier() > p.getAmplifier() | (e.getAmplifier() == p.getAmplifier() & e.getDuration() > p.getDuration()) | p.getDuration() < 20)
						en.addCustomEffect(e, true);
				}
			if (!exist)
				en.addCustomEffect(e, true);
		}
	}

	@EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
	public void onPlayerItemConsume(PlayerItemConsumeEvent event)
	{
		if (ToolAndFoodFX.isFood(event.getItem()))
		{
			if (!event.getPlayer().hasPermission("toolandfoodfx.use.food")) return;
			ToolAndFoodFX.applyEffects(event.getPlayer(), event.getItem(), "\u00a7c", false);
		}
	}

	@EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
	{
		if (event.getDamager() instanceof Player & event.getEntity() instanceof LivingEntity)
		{
			Player damager = (Player) event.getDamager();
			if (ToolAndFoodFX.isWeapon(damager.getInventory().getItemInMainHand()))
			{
				if (!damager.hasPermission("toolandfoodfx.use.weapon")) return;
				ToolAndFoodFX.applyEffects((LivingEntity) event.getEntity(), damager.getInventory().getItemInMainHand(), "\u00a7c", false);
			}
		}
		if (event.getDamager() instanceof LivingEntity & event.getEntity() instanceof Player)
		{
			Player damaged = (Player) event.getEntity();
			if (!damaged.hasPermission("toolandfoodfx.use.armor")) return;
			for (ItemStack i : damaged.getInventory().getArmorContents())
				ToolAndFoodFX.applyEffects((LivingEntity) event.getDamager(), i, "\u00a7c", false);
		}
		/*if (event.getDamager() instanceof Projectile & event.getEntity() instanceof LivingEntity)
		{
			Projectile proj = (Projectile) event.getDamager();
			if ((proj.getShooter() instanceof Player) & (this.proj.get(proj) != null))
			{
				ToolAndFoodFX.ApplyFX((LivingEntity) event.getEntity(), this.proj.get(proj), "\u00a7c", false);
				this.proj.remove(proj);
			}
		}*/
	}
	
	/*@EventHandler
	public void onProjectileLaunch(ProjectileLaunchEvent event)
	{
		if (event.getEntity().getShooter() instanceof Player & ToolAndFoodFX.isProjectile(event.getEntityType()))
		{
			Player shooter = (Player) event.getEntity().getShooter();
			if (!shooter.hasPermission("toolandfoodfx.use.weapon")) return;
			if (shooter.getInventory().getItemInMainHand().getItemMeta().hasLore())
				this.proj.put(event.getEntity(), shooter.getInventory().getItemInMainHand());
		}
	}*/
	
	@EventHandler
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
	    if(event.getItem().hasMetadata("no_pickup")) {
	        event.setCancelled(true);
	    }
	}
	
	@EventHandler
	public void onEntityShootBow(EntityShootBowEvent event) {
		if (event.getEntity() instanceof Player) {
			Player shooter = (Player) event.getEntity();
			if (!shooter.hasPermission("toolandfoodfx.use.weapon")) return;
			if (!ToolAndFoodFX.isEffectHolderItem(shooter.getInventory().getItemInMainHand())) return;
			TippedArrow arrow = (TippedArrow)((ProjectileSource) event.getEntity()).launchProjectile(TippedArrow.class, event.getProjectile().getVelocity());
			arrow.setMetadata("no_pickup", new FixedMetadataValue(this, true));
			ToolAndFoodFX.applyEffects(arrow, shooter.getInventory().getItemInMainHand(), "\u00a7c", false);
			event.setProjectile(arrow);
		}
	}

	protected boolean canApplyEffect(ItemStack i)
	{
		if (isFood(i) || isArmor(i) || isWeapon(i))
			return true;
		return false;
	}
	
	@SuppressWarnings("deprecation")
	protected static boolean isArmor(ItemStack m) {
		switch(m.getType()) {
		
		case LEATHER_HELMET: case LEATHER_CHESTPLATE: case LEATHER_LEGGINGS: case LEATHER_BOOTS:
		case IRON_HELMET: case IRON_CHESTPLATE: case IRON_LEGGINGS: case IRON_BOOTS:
		case CHAINMAIL_HELMET: case CHAINMAIL_CHESTPLATE: case CHAINMAIL_LEGGINGS: case CHAINMAIL_BOOTS:
		case GOLD_HELMET: case GOLD_CHESTPLATE: case GOLD_LEGGINGS: case GOLD_BOOTS:
		case DIAMOND_HELMET: case DIAMOND_CHESTPLATE: case DIAMOND_LEGGINGS: case DIAMOND_BOOTS:
		case PUMPKIN: case SKULL: case ELYTRA: case BOW: case SHIELD:
			return true;
		default:
			break;
		}
		
		if (customArmor.contains(String.valueOf(m.getTypeId())) || customArmor.contains(m.getType().name())) return true;
		return false;
	}

	@SuppressWarnings("deprecation")
	protected static boolean isWeapon(ItemStack m)
	{
		switch (m.getType()) {

		case WOOD_SPADE: case WOOD_AXE: case WOOD_PICKAXE: case WOOD_HOE: case WOOD_SWORD:
		case STONE_SPADE: case STONE_AXE: case STONE_PICKAXE: case STONE_HOE: case STONE_SWORD:
		case IRON_SPADE: case IRON_AXE: case IRON_PICKAXE: case IRON_HOE: case IRON_SWORD:
		case GOLD_SPADE: case GOLD_AXE: case GOLD_PICKAXE: case GOLD_HOE: case GOLD_SWORD:
		case DIAMOND_SPADE: case DIAMOND_AXE: case DIAMOND_PICKAXE: case DIAMOND_HOE: case DIAMOND_SWORD:
		case FLINT_AND_STEEL: case SHEARS: case STICK:

			return true;
		default:
			break;
		}
	
		if (customWeapons.contains(String.valueOf(m.getTypeId())) || customWeapons.contains(m.getType().name())) return true;
		return false;
	}
	
	@SuppressWarnings("deprecation")
	protected static boolean isFood(ItemStack m) {
		if (m.getType().isEdible()) return true;
		if (customFoods.contains(String.valueOf(m.getTypeId())) || customFoods.contains(m.getType().name())) return true;
		return false;
	}

	/*protected static boolean isProjectile(EntityType e)
	{
		switch (e) {

		case SNOWBALL:
		case FISHING_HOOK:
		case EGG:

			return true;
		default:
			return false;
		}
	}*/

}

class EffectApplicationTimer extends BukkitRunnable
{
	ToolAndFoodFX plugin;
	public EffectApplicationTimer(ToolAndFoodFX instance)
	{
		this.plugin = instance;
	}

	@Override
	public void run()
	{
		for (Player p : plugin.getServer().getOnlinePlayers())
			if (p.hasPermission("toolandfoodfx.use.wear"))
			{
				for (ItemStack i : p.getInventory().getArmorContents())
					ToolAndFoodFX.applyEffects(p, i, "\u00a73", true);
				if (p.getInventory().getItemInMainHand() != null)
					if (ToolAndFoodFX.isWeapon(p.getInventory().getItemInMainHand()) | p.getInventory().getItemInMainHand().getType() == Material.BOW)
						ToolAndFoodFX.applyEffects(p, p.getInventory().getItemInMainHand(), "\u00a73", true);
			}
	}
}
