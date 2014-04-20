package com.gmail.brandonli2010.ToolAndFoodFX;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.*;

public class ToolAndFoodFX extends JavaPlugin implements Listener {
	public void onEnable()
	{
		Bukkit.getPluginManager().registerEvents(this, this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (cmd.getName().equalsIgnoreCase("addeffect"))
		{
			if (!(sender instanceof Player))
			{
				sender.sendMessage("\u00a7cYou must be a player to use this command.");
				return true;
			}
			if (args.length < 3)
			{
				sender.sendMessage("/addeffect <effect type> <level> <duration>");
				return true;
			}
			int lvl;
			int dur;
			PotionEffectType type;
			try
			{
				lvl = Integer.valueOf(args[1]);
				dur = Integer.valueOf(args[2]);
				type = PotionEffectType.getByName(args[0]);
			}
			catch (NumberFormatException e)
			{
				sender.sendMessage("\u00a7cError: invalid number");
				return true;
			}
			if (type == null)
			{
				sender.sendMessage("\u00a7cError: invalid potion effect");
				return true;
			}
			ItemStack out = ((Player) sender).getItemInHand();
			if (out == null)
			{
				sender.sendMessage("\u00a7cYou must be holding an item.");
				return true;
			}
			if (!(isTool(out.getType()) | out.getType().isEdible()))
			{
				sender.sendMessage("\u00a7cYou cannot apply an effect to this item.");
				return true;
			}
			ItemMeta meta = out.getItemMeta();
			if (!meta.hasLore())
			{
				List<String> lore = new ArrayList<String>();
				lore.add("\u00a79\u00a7l\u25B6 Effects \u25C4");
				lore.add(type.getName() + " " + lvl + " " + dur);
				meta.setLore(lore);
			}
			else if (!meta.getLore().get(0).equals("\u00a79\u00a7l\u25B6 Effects \u25C4"))
			{
				List<String> lore = new ArrayList<String>();
				lore.add("\u00a79\u00a7l\u25B6 Effects \u25C4");
				lore.add(type.getName() + " " + lvl + " " + dur);
				meta.setLore(lore);
			}
			else
			{
				boolean exist = false;
				List<String> lore = meta.getLore();
				for (int i = 1; i < lore.size(); i++)
				{
					if (type == PotionEffectType.getByName(lore.get(i).split(" ")[0]))
					{
						exist = true;
						lore.set(i, type.getName() + " " + lvl + " " + dur);
						break;
					}
				}
				if (!exist)
				{
					lore.add(type.getName() + " " + lvl + " " + dur);
				}
				meta.setLore(lore);
			}
			out.setItemMeta(meta);
			sender.sendMessage("\u00a7aYou have added:\n\u00a79" + type.getName() + ", level: " + lvl + ", duration: " + dur + " seconds\n\u00a7cto: " + out.getType().toString());
			return true;
		}
		if (cmd.getName().equalsIgnoreCase("deleffect"))
		{
			if (!(sender instanceof Player))
			{
				sender.sendMessage("\u00a7cYou must be a player to use this command.");
				return true;
			}
			if (args.length != 1)
			{
				sender.sendMessage("/deleffect <effect type|all>");
				return true;
			}
			ItemStack out = ((Player) sender).getItemInHand();
			if (out == null)
			{
				sender.sendMessage("\u00a7cYou must be holding an item.");
				return true;
			}
			if (!(isTool(out.getType()) | out.getType().isEdible()))
			{
				sender.sendMessage("\u00a7cYou cannot delete an effect from this item.");
				return true;
			}
			ItemMeta meta = out.getItemMeta();
			if (args[0].equalsIgnoreCase("all"))
			{
				List<String> lore = new ArrayList<String>();
				lore.add("\u00a79\u00a7l\u25B6 Effects \u25C4");
				meta.setLore(lore);
				out.setItemMeta(meta);
				sender.sendMessage("\u00a7aYou have deleted all effects.");
				return true;
			}
			PotionEffectType type = PotionEffectType.getByName(args[0]);
			if (type == null)
			{
				sender.sendMessage("\u00a7cError: invalid potion effect");
				return true;
			}
			if (!meta.hasLore())
			{
				sender.sendMessage("\u00a7cThere are no effects to delete.");
				return true;
			}
			else if (!meta.getLore().get(0).equals("\u00a79\u00a7l\u25B6 Effects \u25C4"))
			{
				sender.sendMessage("\u00a7cThere are no effects to delete.");
				return true;
			}
			else
			{
				List<String> lore = meta.getLore();
				boolean exist = false;
				for (int i = 1; i < lore.size(); i++)
				{
					if (type == PotionEffectType.getByName(lore.get(i).split(" ")[0]))
					{
						lore.remove(i);
						break;
					}
				}
				if (!exist)
				{
					sender.sendMessage("\u00a7cThat effect was not on the item.");
				}
				meta.setLore(lore);
			}
			out.setItemMeta(meta);
			sender.sendMessage("\u00a7aYou have deleted " + type.getName() + " from " + out.getType().toString());
			return true;
		}
		return false;
	}

	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) 
	{
		List<String> toreturn = new ArrayList<String>();
		toreturn.clear();
		if (command.getName().equalsIgnoreCase("addeffect") | command.getName().equalsIgnoreCase("deleffect"))
		{
			if (args.length == 1)
			{
				for (int i = 1; i < PotionEffectType.values().length; i++)
				{
					toreturn.add(PotionEffectType.values()[i].getName());
				}
			}
			for (int ind = 0; ind < toreturn.size();)
			{
				if (!toreturn.get(ind).toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
				{
					toreturn.remove(ind);
				}
				else
				{
					ind++;
				}
			}
		}
		return toreturn;
	}

	private void ApplyFX(LivingEntity en, List<String> Lore)
	{
		if ((Lore != null) && (Lore.size() > 1) && (Lore.get(0).equals("\u00a79\u00a7l\u25B6 Effects \u25C4")))
		{
			for (int i = 1; i < Lore.size(); i++)
			{
				String[] elem = Lore.get(i).split(" ");
				if (elem.length == 3)
				{
					try {
						en.addPotionEffect(new PotionEffect(PotionEffectType.getByName(elem[0]), Integer.valueOf(elem[2]) * 20, Integer.valueOf(elem[1])));
					} catch(NumberFormatException e) {}
				}
			}
		}
	}

	private boolean isTool(Material m)
	{
		switch (m) {

		case WOOD_SPADE:
		case WOOD_AXE:
		case WOOD_PICKAXE:
		case WOOD_HOE:
		case WOOD_SWORD:

		case STONE_SPADE:
		case STONE_AXE:
		case STONE_PICKAXE:
		case STONE_HOE:
		case STONE_SWORD:

		case IRON_SPADE:
		case IRON_AXE:
		case IRON_PICKAXE:
		case IRON_HOE:
		case IRON_SWORD:

		case GOLD_SPADE:
		case GOLD_AXE:
		case GOLD_PICKAXE:
		case GOLD_HOE:
		case GOLD_SWORD:

		case DIAMOND_SPADE:
		case DIAMOND_AXE:
		case DIAMOND_PICKAXE:
		case DIAMOND_HOE:
		case DIAMOND_SWORD:

		case FLINT_AND_STEEL:
		case SHEARS:
		case FISHING_ROD:
		case STICK:
		case BOW:

			return true;
		default:
			return false;
		}
	}

	@EventHandler
	public void onPlayerItemConsume(PlayerItemConsumeEvent event)
	{
		if (event.getItem().getType().isEdible())
		{
			List<String> Lore = event.getItem().getItemMeta().getLore();
			this.ApplyFX(event.getPlayer(), Lore);
		}
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
	{
		if ((event.getDamager() instanceof Player) & (event.getEntity() instanceof LivingEntity))
		{
			if (isTool(((Player) event.getDamager()).getItemInHand().getType()))
			{
				List<String> Lore = ((Player) event.getDamager()).getItemInHand().getItemMeta().getLore();
				this.ApplyFX((LivingEntity) event.getEntity(), Lore);
			}
		}
		if (event.getDamager().getType() == EntityType.ARROW & (event.getEntity() instanceof LivingEntity))
		{
			if ((((Arrow) event.getDamager()).getShooter() instanceof Player))
			{
				if (((Player) ((Arrow) event.getDamager()).getShooter()).getItemInHand().getType() == Material.BOW)
				{
					List<String> Lore = ((Player) ((Arrow) event.getDamager()).getShooter()).getItemInHand().getItemMeta().getLore();
					this.ApplyFX((LivingEntity) event.getEntity(), Lore);
				}
			}
		}
	}
}
