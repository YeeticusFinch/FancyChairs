package com.lerdorf.fancychairs;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

public class FancyChairs extends JavaPlugin implements Listener, TabExecutor {
    
	private File configFile;
	private Map<String, Object> configValues;
    
	boolean requireBothHandsEmpty = false;
	boolean onlySitOnCarpetedChairs = true;
	boolean enableSlabs = true;
	boolean enablePressurePlates = true;
	boolean enableEmptyHandsException = false;
	
	Vector chairOffset = new Vector(0.5, -0.9f, 0.5);
	Vector sitOffset = new Vector(0.5, 0.5f, 0.5);
    
    public void loadConfig() {
    	DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // <-- use block style
        options.setIndent(2);
        options.setPrettyFlow(true);
        
        File pluginFolder = this.getDataFolder();
        if (!pluginFolder.exists()) pluginFolder.mkdirs();
        
        configFile = new File(pluginFolder, "config.yml");

        Yaml yaml = new Yaml(options);

        // If file doesn't exist, create it with defaults
        if (!configFile.exists()) {
            configValues = new HashMap<>();
            configValues.put("requireBothHandsEmpty", requireBothHandsEmpty);
            configValues.put("onlySitOnCarpetedChairs", onlySitOnCarpetedChairs);
            configValues.put("enableSlabs", enableSlabs);
            configValues.put("enablePressurePlates", enablePressurePlates);
            configValues.put("enableEmptyHandsException", enableEmptyHandsException);
            saveConfig(); // Save default config
        }

        try {
            String yamlAsString = Files.readString(configFile.toPath());
            configValues = (Map<String, Object>) yaml.load(yamlAsString);
            if (configValues == null) configValues = new HashMap<>();
        } catch (Exception e) {
            e.printStackTrace();
            configValues = new HashMap<>();
        }

        // Now parse and update values
        try {
            if (configValues.containsKey("requireBothHandsEmpty"))
            	requireBothHandsEmpty = (boolean)configValues.get("requireBothHandsEmpty");
        } catch (Exception e) {
            e.printStackTrace();
        }
        configValues.put("requireBothHandsEmpty", requireBothHandsEmpty);
        
        try {
            if (configValues.containsKey("onlySitOnCarpetedChairs"))
            	onlySitOnCarpetedChairs = (boolean)configValues.get("onlySitOnCarpetedChairs");
        } catch (Exception e) {
            e.printStackTrace();
        }
        configValues.put("onlySitOnCarpetedChairs", onlySitOnCarpetedChairs);

        try {
            if (configValues.containsKey("enableSlabs"))
            	enableSlabs = (boolean)configValues.get("enableSlabs");
        } catch (Exception e) {
            e.printStackTrace();
        }
        configValues.put("enableSlabs", enableSlabs);

        try {
            if (configValues.containsKey("enablePressurePlates"))
            	enablePressurePlates = (boolean)configValues.get("enablePressurePlates");
        } catch (Exception e) {
            e.printStackTrace();
        }
        configValues.put("enablePressurePlates", enablePressurePlates);

        try {
            if (configValues.containsKey("enableEmptyHandsException"))
            	enableEmptyHandsException = (boolean)configValues.get("enableEmptyHandsException");
        } catch (Exception e) {
            e.printStackTrace();
        }
        configValues.put("enableEmptyHandsException", enableEmptyHandsException);

        saveConfig(); // Ensure config is up to date
    }

    public void saveConfig() {
    	DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK); // <-- use block style
        options.setIndent(2);
        options.setPrettyFlow(true);
        
        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(configFile)) {
            yaml.dump(configValues, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onEnable() {
        
        getServer().getPluginManager().registerEvents(this, this);
        
        this.getCommand("fc").setExecutor(this);
        
        loadConfig();
        saveConfig();
        
        /*
    	new BukkitRunnable() {
            @Override
            public void run() {
            	
            }
        }.runTaskTimer(this, 0L, 1L); // Run every 1 tick
        */
        
        getLogger().info("FancyChairs enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("FancyChairs disabled!");
    }
    
    public boolean rightClick(Player p, Block block) {
    	if (p.isSneaking()) {
    		return false;
    	} else {
    		if (block == null) {
    			for (float i = 0; i < 3; i += 0.2f) {
    				Location loc = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(i));
    				if (!loc.getBlock().isPassable()) {
    					block = loc.getBlock();
    					break;
    				}
    			}
    		}
    		if (block != null) {
    			
    			if (isSeatItem(p.getEquipment().getItemInMainHand()) && isCarpetable(block)) {
    				if (hasCarpet(block)) {
    					return true;
    				}
    				// Place carpet on staircase or slab
    				ArmorStand stand = (ArmorStand)block.getWorld().spawnEntity(block.getLocation().clone().add(chairOffset), EntityType.ARMOR_STAND);
    				ItemStack hat = p.getEquipment().getItemInMainHand().clone();
    				hat.setAmount(1);
    				stand.getEquipment().setHelmet(hat);
    				stand.setInvisible(true);
    				stand.setGravity(false);
    				stand.setMarker(true);
    				stand.setInvulnerable(true);
    				stand.addScoreboardTag("FancyChair");
    				p.getEquipment().getItemInMainHand().setAmount(p.getEquipment().getItemInMainHand().getAmount()-1);
    				return true;
    			}
    			
    			// Only sit if it's actually sittable
    			if (!requireBothHandsEmpty || (p.getEquipment().getItemInMainHand().getType() == Material.AIR && p.getEquipment().getItemInOffHand().getType() == Material.AIR)) {
	    			if (isSittable(block) || (enableEmptyHandsException && isCarpetable(block) && p.getEquipment().getItemInMainHand().getType() == Material.AIR && p.getEquipment().getItemInOffHand().getType() == Material.AIR)) {
	    				sit(p, block);
	    				return true;
	    			}
    			}
    		}
    	}
    	return false;
    }
    
    public boolean isSeatItem(ItemStack item) {
    	//Bukkit.broadcastMessage(item.getType().toString().toLowerCase());
    	if (item.getType().toString().toLowerCase().contains("carpet"))
    		return true;
    	else if (enablePressurePlates && item.getType().toString().toLowerCase().contains("_plate"))
    		return true;
    	return false;
    }
    
    public void sit(Player p, Block block) {
    	// Sit Player p on Block block
    	
    	ArmorStand chair = null;
    	for (Entity e : block.getLocation().getWorld().getNearbyEntities(block.getLocation().add(sitOffset), 0.4f, 0.4f, 0.4f)) {
			if (e instanceof ArmorStand stand && stand.getScoreboardTags().contains("TempChair")) {
				chair = stand;
				if (!chair.getPassengers().isEmpty())
				{
					p.sendMessage(ChatColor.RED + "That seat is occupied!");
					return;
				}
				break;
			}
		}
    	if (chair == null) {
    		Vector dir = block.getLocation().subtract(p.getLocation()).toVector().normalize();
    		if (block.getBlockData() instanceof Stairs stairs) {
    			dir = stairs.getFacing().getDirection().multiply(-1);
    		}
    		ArmorStand stand = (ArmorStand)block.getWorld().spawnEntity(block.getLocation().clone().add(sitOffset).setDirection(dir), EntityType.ARMOR_STAND);
			stand.setInvisible(true);
			stand.setGravity(false);
			stand.setMarker(true);
			stand.setInvulnerable(true);
			stand.addScoreboardTag("FancyChair");
			stand.addScoreboardTag("TempChair");
			chair = stand;
    	}
    	chair.addPassenger(p);
    }
    
    public boolean isCarpetable(Block block) {
    	//Bukkit.broadcastMessage(block.getType().toString().toLowerCase());
    	if (block.getBlockData() instanceof Slab slab) {
    		if (!enableSlabs)
    			return false;
    		
    			if (slab.getType() == Slab.Type.DOUBLE || slab.getType() == Slab.Type.TOP) {
        			//Bukkit.broadcastMessage("Double or TOP");
    				return false;
    			} else {
    				return true;
    			}
    		
    	} else if (block.getBlockData() instanceof Stairs stairs) {
    			if (stairs.getHalf() == Bisected.Half.TOP) {
    				return false;
    			} else {
    				return true;
    			}
    	} else {
    		return false;
    	}
    }
    
    public boolean hasCarpet(Block block) {
    	for (Entity e : block.getLocation().getWorld().getNearbyEntities(block.getLocation(), 1, 1, 1)) {
			if (e instanceof ArmorStand stand && stand.getScoreboardTags().contains("FancyChair")) {
				return true;
			}
		}
    	
    	return false;
    }
    
    public boolean isSittable(Block block) {
    	
    	if (!isCarpetable(block))
    		return false;
    	
    	if (onlySitOnCarpetedChairs) {
    		if (!hasCarpet(block))
    			return false;
    	}
    	
		return true;
    }
    
    @EventHandler
	public void onUseEvent(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {

			// Ensure the player is using the main hand
	        //if (event.getHand() == EquipmentSlot.HAND) {
			Block block = null;
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK)
				block = event.getClickedBlock();
			
        	boolean cancelEvent = rightClick(player, block);
	    	if (cancelEvent)
	    	 	event.setCancelled(true);
	        //}
		}
	}
    
    /*
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    	Player player = event.getPlayer();
    	
    	//player.sendMessage("Right Clicked Entity");

        boolean cancelEvent = rightClick(player, null);
		    if (cancelEvent)
		    	event.setCancelled(true);
        
    }*/
    
    @EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
	    Block placedBlock = event.getBlockPlaced();
	    Player player = event.getPlayer();
	    
	    // Check if the placed block is a stair or slab that could become a chair
	    if (isCarpetable(placedBlock)) {
	    	// Don't process this as a right-click interaction when placing blocks
	    	return;
	    }

	    boolean cancelEvent = rightClick(player, null);
	    if (cancelEvent)
	    	event.setCancelled(true);
	    
	    // Player placed a block
	    //player.sendMessage("You placed a " + placedBlock.getType().toString());
	}
    
    // Handle when players dismount from chairs
    @EventHandler
    public void onEntityDismount(VehicleExitEvent event) {
        if (event.getExited() instanceof Player && event.getVehicle() instanceof ArmorStand) {
            ArmorStand stand = (ArmorStand) event.getVehicle();
            
            // Check if this is a temporary chair (no carpet)
            if (stand.getScoreboardTags().contains("TempChair")) {
                // Remove the temporary chair after a short delay to ensure dismount completes
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        stand.remove();
                    }
                }.runTaskLater(this, 2L); // 2 tick delay
            }
        }
    }
    
    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Block block = event.getBlock();
        Location base = block.getLocation();
        Location carpetLoc = base.clone().add(chairOffset);

        for (Entity e : block.getWorld().getNearbyEntities(carpetLoc, 0.5, 1, 0.5)) {
            if (e instanceof ArmorStand stand && stand.getScoreboardTags().contains("FancyChair")) {
                // Drop helmet (carpet)
                if (stand.getEquipment().getHelmet() != null) {
                	ItemStack drop = stand.getEquipment().getHelmet();
                	drop.setAmount(1);
                    block.getWorld().dropItemNaturally(stand.getLocation(), drop);
                }
                stand.remove();
            }
        }

        // Also clean up any "TempChair" armor stands the player was sitting on
        Location sitLoc = base.clone().add(sitOffset);
        for (Entity e : block.getWorld().getNearbyEntities(sitLoc, 0.5, 1, 0.5)) {
            if (e instanceof ArmorStand stand && stand.getScoreboardTags().contains("TempChair")) {
                stand.remove();
            }
        }
    }
    
    @EventHandler
    public void onPistonMove(BlockPistonExtendEvent event) {
        handlePiston(event.getBlocks(), event.getDirection().getDirection());
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        handlePiston(event.getBlocks(), event.getDirection().getDirection().multiply(-1));
    }

    private void handlePiston(List<Block> movedBlocks, Vector movement) {
        for (Block block : movedBlocks) {
            Location origin = block.getLocation();

            for (Vector offset : List.of(chairOffset, sitOffset)) {
                Location armorStandLoc = origin.clone().add(offset);
                for (Entity e : block.getWorld().getNearbyEntities(armorStandLoc, 0.5, 1, 0.5)) {
                    if (e instanceof ArmorStand stand &&
                        (stand.getScoreboardTags().contains("FancyChair") || stand.getScoreboardTags().contains("TempChair"))) {

                        List<Entity> passengers = new ArrayList<>(stand.getPassengers());
                        stand.eject();

                        // Move stand
                        Location newLoc = stand.getLocation().add(movement);
                        stand.teleport(newLoc);

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                            	// Re-seat passengers
                                for (Entity passenger : passengers) {
                                    stand.addPassenger(passenger);
                                }
                            }
                        }.runTaskLater(this, 2L); // 2 tick delay
                        
                    }
                }
            }
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	sender.sendMessage(ChatColor.GREEN + "Reloading FancyChairs config.yml");
    	loadConfig();
    	sender.sendMessage("Config reloaded!");
    	sender.sendMessage("requireBothHandsEmpty: " + requireBothHandsEmpty);
    	sender.sendMessage("onlySitOnCarpetedChairs: " + onlySitOnCarpetedChairs);
    	sender.sendMessage("enableSlabs: " + enableSlabs);
    	sender.sendMessage("enableEmptyHandsException: " + enableEmptyHandsException);
    	return true;
    }
}