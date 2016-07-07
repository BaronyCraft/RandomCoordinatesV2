package com.jolbol1.RandomCoordinates.managers;


import com.jolbol1.RandomCoordinates.RandomCoords;
import com.jolbol1.RandomCoordinates.checks.*;
import com.jolbol1.RandomCoordinates.cooldown.Cooldown;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;

/**
 * Created by James on 01/07/2016.
 */
public class Coordinates {

    private SecureRandom random = new SecureRandom();
    private MessageManager messages = new MessageManager();
    private FactionChecker fc = new FactionChecker();
    private GriefPreventionCheck gpc = new GriefPreventionCheck();
    private PlayerRadCheck prc = new PlayerRadCheck();
    private TownyChecker tc = new TownyChecker();
    private WorldBorderChecker wbc = new WorldBorderChecker();
    private WorldGuardCheck wgc = new WorldGuardCheck();
    private Nether nether = new Nether();
    private End end = new End();



    private Location getRandomCoordinates(Player player, int max, int min, World world){
        int randomX;
        int randomZ;
        int spawnX;
        int spawnZ;

        if(RandomCoords.getPlugin().config.get(world.getName() + ".Center.X") != null ) {
            spawnX = RandomCoords.getPlugin().config.getInt(world.getName() + ".Center.X");
        } else {
            spawnX = world.getSpawnLocation().getBlockX();
        }
        if( RandomCoords.getPlugin().config.get(world.getName() + ".Center.Z") != null) {
            spawnZ = RandomCoords.getPlugin().config.getInt(world.getName() + ".Center.Z");
        } else {
            spawnZ = world.getSpawnLocation().getBlockZ();
        }

        if(RandomCoords.getPlugin().config.getString("VanillaBorder").equalsIgnoreCase("true")) {
            WorldBorder border = world.getWorldBorder();
            Location center = border.getCenter();
            spawnX = center.getBlockX();
            spawnZ = center.getBlockZ();
            double size = border.getSize();
            int radius = (int) size / 2;
            if(max > radius) {
                max = radius;
            }
        }
        randomX = getRandomNumberInRange(min, max, player) * modulus();
        randomZ = getRandomNumberInRange(min, max, player) * modulus();
        if(RandomCoords.getPlugin().config.getString("RandomOrg").equalsIgnoreCase("true")) {
            randomX = Integer.valueOf(getRandomOrg(min, max)) * modulus();
            randomZ = Integer.valueOf(getRandomOrg(min, max)) * modulus();
        }
        int defaultY = 90;
        Location preRandom = new Location(world, spawnX + (randomX + 0.5), defaultY, spawnZ + (randomZ + 0.5));
        int highestPoint = world.getHighestBlockYAt(preRandom);
        if(world.getBiome(spawnX, spawnZ).equals(Biome.HELL)) {
            highestPoint = nether.netherY(preRandom);

        }
        Location randomLocation = new Location(world, spawnX + (randomX + 0.5), highestPoint, spawnZ + (randomZ + 0.5));
        Block b = world.getBlockAt(randomLocation);
        return randomLocation;
    }

    private int getRandomNumberInRange(int min, int max, CommandSender sender ) {

        if (min >= max) {
           messages.minTooLarge(sender);
        }

        return random.nextInt((max - min) + 1) + min;
    }

    private int modulus(){
        int modulus = random.nextInt((1 - 0) + 1) + 0;


        if(modulus == 0) {
            modulus = -1;
        }
        return modulus;
    }

    public void finalCoordinates(Player player, int max, int min, World world, CoordType type, double cost) {
        boolean limiter = false;
        double thisCost = cost;
        Location start = player.getLocation();
        double health = player.getHealth();
        int timeBefore = 0;
        int cooldown = 0;
        for (String worlds : RandomCoords.getPlugin().config.getStringList("BannedWorlds")) {
            if (player.getWorld().getName().equals(worlds)) {
                messages.worldBanned(player);
                return;

            }
        }
        if(max == 574272099) {
          if(RandomCoords.getPlugin().config.get(world.getName() + ".Max") != null) {
              max = RandomCoords.getPlugin().config.getInt(world.getName() + ".Max");
          } else {
              max = RandomCoords.getPlugin().config.getInt("MaxCoordinate");          }
       }
        if(min == 574272099) {
            if(RandomCoords.getPlugin().config.get(world.getName() + ".Max") != null) {
                min = RandomCoords.getPlugin().config.getInt(world.getName() + ".Min");
            } else {
                min = RandomCoords.getPlugin().config.getInt("MinCoordinate");
            }
        }

        boolean exitLoop = false;
        int attempts = 0;
        while(!exitLoop) {
            int maxAttempts = RandomCoords.getPlugin().config.getInt("MaxAttempts");
            if(attempts >= maxAttempts) {
                exitLoop = true;
                messages.couldntFind(player);
                return;
            }
            Location location = getRandomCoordinates(player, max, min, world);

            if (!isLocSafe(location)) {
                attempts++;
                exitLoop = false;
            } else {
                exitLoop = true;
                Location locationTP = location.add(0, 2.5, 0);
                if(locationTP.getWorld().getBiome(locationTP.getBlockX(), locationTP.getBlockZ()).equals(Biome.HELL)) {
                    locationTP = location.subtract(0, 1.5, 0);
                }
                if(locationTP.getWorld().getBiome(locationTP.getBlockX(), locationTP.getBlockZ()).equals(Biome.SKY)) {
                    locationTP = end.endCoord(locationTP);
                    int highest = locationTP.getWorld().getHighestBlockYAt(locationTP.getBlockX(), locationTP.getBlockZ());
                    locationTP = new Location(locationTP.getWorld(), locationTP.getBlockX(), highest, locationTP.getBlockZ());
                }
                switch (type) {
                    case COMMAND:
                        if(RandomCoords.getPlugin().config.getString("Command").equalsIgnoreCase("warps")) {
                            locationTP = warpTP(player, world);
                        }
                        if(RandomCoords.getPlugin().config.getStringList("LimiterApplys").contains("Command")) {
                            limiter = isLimiter(player);
                        }
                        if(RandomCoords.getPlugin().config.getDouble("CommandCost") != 0) {
                            thisCost= RandomCoords.getPlugin().config.getDouble("CommandCost");
                        }
                        if(RandomCoords.getPlugin().config.getInt("TimeBeforeTeleport") != 0) {
                            if(RandomCoords.getPlugin().config.getStringList("TimeBeforeApplys").contains("Command")) {
                                timeBefore = RandomCoords.getPlugin().config.getInt("TimeBeforeTeleport");
                            }
                        }
                        if(RandomCoords.getPlugin().config.getInt("CooldownTime") != 0) {
                            if(RandomCoords.getPlugin().config.getStringList("CooldownApplys").contains("Command")) {
                                cooldown = RandomCoords.getPlugin().config.getInt("CooldownTime");
                            } else {
                                Bukkit.broadcastMessage("FUCKEDITUP");
                            }
                        }

                        break;
                    case ALL:
                        if(RandomCoords.getPlugin().config.getString("All").equalsIgnoreCase("warps")) {
                            locationTP = warpTP(player, world);
                        }
                        if(RandomCoords.getPlugin().config.getStringList("LimiterApplys").contains("All")) {
                            limiter = isLimiter(player);
                        }
                        if(RandomCoords.getPlugin().config.getInt("TimeBeforeTeleport") != 0) {
                            if(RandomCoords.getPlugin().config.getStringList("TimeBeforeApplys").contains("All")) {
                                timeBefore = RandomCoords.getPlugin().config.getInt("TimeBeforeTeleport");
                            }
                        }
                        if(RandomCoords.getPlugin().config.getInt("CooldownTime") != 0) {
                            if(RandomCoords.getPlugin().config.getStringList("CooldownApplys").contains("All")) {
                                cooldown = RandomCoords.getPlugin().config.getInt("CooldownTime");
                            }
                        }
                        break;
                    case PLAYER:
                        if(RandomCoords.getPlugin().config.getString("Others").equalsIgnoreCase("warps")) {
                            locationTP = warpTP(player, world);
                        }
                        if(RandomCoords.getPlugin().config.getStringList("LimiterApplys").contains("Others")) {
                            limiter = isLimiter(player);
                        }
                        if(RandomCoords.getPlugin().config.getInt("TimeBeforeTeleport") != 0) {
                            if(RandomCoords.getPlugin().config.getStringList("TimeBeforeApplys").contains("Others")) {
                                timeBefore = RandomCoords.getPlugin().config.getInt("TimeBeforeTeleport");
                            }
                        }
                        if(RandomCoords.getPlugin().config.getInt("CooldownTime") != 0) {
                            if(RandomCoords.getPlugin().config.getStringList("CooldownApplys").contains("Others")) {
                                cooldown = RandomCoords.getPlugin().config.getInt("CooldownTime");
                            }
                        }
                        break;
                    case SIGN:
                        if(RandomCoords.getPlugin().config.getString("Signs").equalsIgnoreCase("warps")) {
                            locationTP = warpTP(player, world);

                            if(RandomCoords.getPlugin().config.getStringList("LimiterApplys").contains("Sign")) {
                                limiter = isLimiter(player);
                            }
                            if(RandomCoords.getPlugin().config.getInt("TimeBeforeTeleport") != 0) {
                                if(RandomCoords.getPlugin().config.getStringList("TimeBeforeApplys").contains("Sign")) {
                                    timeBefore = RandomCoords.getPlugin().config.getInt("TimeBeforeTeleport");
                                }
                            }
                            if(RandomCoords.getPlugin().config.getInt("CooldownTime") != 0) {
                                if(RandomCoords.getPlugin().config.getStringList("CooldownApplys").contains("Sign")) {
                                    cooldown = RandomCoords.getPlugin().config.getInt("CooldownTime");
                                }
                            }

                        }
                        break;
                    case JOIN:
                        if(RandomCoords.getPlugin().config.getString("Join").equalsIgnoreCase("warps")) {
                            locationTP = warpTP(player, world);
                        }
                        break;
                    case PORTAL:
                        if(RandomCoords.getPlugin().config.getString("Portals").equalsIgnoreCase("warps")) {
                            locationTP = warpTP(player, world);
                        }
                        if(RandomCoords.getPlugin().config.getStringList("LimiterApplys").contains("Portal")) {
                            limiter = isLimiter(player);
                        }
                        if(RandomCoords.getPlugin().config.getDouble("PortalCost") != 0) {
                            thisCost = RandomCoords.getPlugin().config.getDouble("CommandCost");
                        }

                        if(RandomCoords.getPlugin().config.getInt("TimeBeforeTeleport") != 0) {
                            if(RandomCoords.getPlugin().config.getStringList("TimeBeforeApplys").contains("Portals")) {
                                timeBefore = RandomCoords.getPlugin().config.getInt("TimeBeforeTeleport");
                            }
                        }
                        if(RandomCoords.getPlugin().config.getInt("CooldownTime") != 0) {
                            if(RandomCoords.getPlugin().config.getStringList("CooldownApplys").contains("Portals")) {
                                cooldown = RandomCoords.getPlugin().config.getInt("CooldownTime");
                            }
                        }

                        break;
                    case WARPS:
                        locationTP = warpTP(player, world);
                        if(RandomCoords.getPlugin().config.getStringList("LimiterApplys").contains("Warps")) {
                            limiter = isLimiter(player);
                        }
                        if(RandomCoords.getPlugin().config.getInt("TimeBeforeTeleport") != 0) {
                            if(RandomCoords.getPlugin().config.getStringList("TimeBeforeApplys").contains("Warps")) {
                                timeBefore = RandomCoords.getPlugin().config.getInt("TimeBeforeTeleport");
                            }
                        }
                        if(RandomCoords.getPlugin().config.getInt("CooldownTime") != 0) {
                            if(RandomCoords.getPlugin().config.getStringList("CooldownApplys").contains("Warps")) {
                                cooldown = RandomCoords.getPlugin().config.getInt("CooldownTime");
                            }
                        }

                        break;

                }
                if(locationTP == null) {
                    return;
                }
                if(player.hasPermission("Random.Cooldown.Bypass") || player.hasPermission("Random.*")) {
                    cooldown = 0;
                }
                if(player.hasPermission("Random.TBT.Bypass") || player.hasPermission("Random.*")) {
                    timeBefore = 0;
                }


                //Test
                if (Cooldown.isInCooldown((player).getUniqueId(), "TimeBefore")) {
                    messages.aboutTo(player, Cooldown.getTimeLeft((player).getUniqueId(), "TimeBefore"));
                    return;
                }
                if (cooldown != 0) {
                    if (!Cooldown.isInCooldown((player).getUniqueId(), "Command")) {
                        if (!Cooldown.isInCooldown((player).getUniqueId(), "TimeBefore")) {
                            if(timeBefore != 0) {
                                Cooldown cTb = new Cooldown((player).getUniqueId(), "TimeBefore", timeBefore);
                                cTb.start();
                                messages.TeleportingIn(player, timeBefore);
                            }
                        }
                        Cooldown c = new Cooldown((player).getUniqueId(), "Command", cooldown + timeBefore);
                        c.start();
                        BukkitScheduler s = RandomCoords.getPlugin().getServer().getScheduler();
                        Location finalLocationTP = locationTP;
                        double finalThisCost = thisCost;
                        s.scheduleSyncDelayedTask(RandomCoords.getPlugin().getInstance(), new Runnable() {
                            @Override
                            public void run() {
                                if (RandomCoords.getPlugin().config.getString("StopOnMove").equalsIgnoreCase("true")) {
                                    if (start.distance(player.getLocation()) > 1) {
                                        messages.youMoved(player);
                                        return;
                                    }
                                }
                                if (RandomCoords.getPlugin().config.getString("StopOnCombat").equalsIgnoreCase("true")) {
                                    if (health > player.getHealth()) {
                                        messages.tookDamage(player);
                                        return;
                                    }
                                }
                                //coordinates.

                                boolean exit = false;
                                while(!exit) {
                                    if(RandomCoords.getPlugin().config.getString("ChunkLoader").equalsIgnoreCase("false")) {
                                        exit = true;
                                    } else {
                                        exit = generateChunk(player, finalLocationTP);
                                    }
                                    if(exit) {
                                        player.teleport(finalLocationTP);
                                    } else {

                                    }
                                }

                                //Bonus Chests?
                                if(RandomCoords.getPlugin().config.getString("BonusChest").equalsIgnoreCase("true")) {
                                    if(RandomCoords.getPlugin().limiter.getString(player.getUniqueId() + ".Chest") == null) {

                                        Location chestLoc = new Location(finalLocationTP.getWorld(), finalLocationTP.getBlockX() + 1.0, finalLocationTP.getBlockY() -2 , finalLocationTP.getBlockZ() + 1.0);
                                        Location airLoc = new Location(chestLoc.getWorld(), chestLoc.getBlockX(), chestLoc.getBlockY() + 1, chestLoc.getBlockZ());
                                        World chestWorld = chestLoc.getWorld();
                                        Block chestBlock = chestLoc.getBlock();
                                        Block airBlock = airLoc.getBlock();
                                        airBlock.setType(Material.AIR);
                                        chestBlock.setType(Material.CHEST);
                                        Chest chest = (Chest) chestBlock.getState();
                                        Inventory chestInv = chest.getInventory();
                                        for (String material : RandomCoords.getPlugin().config.getStringList("BonusChestItems")) {
                                            if (material.contains("Essentials,")) {
                                                if(Bukkit.getPluginManager().getPlugin("Essentials") != null) {
                                                    KitManager kitManager = new KitManager();
                                                    String[] kits = material.split(",");
                                                    for (String kit : kits) {
                                                        if (!kit.equals("Essentials")) {
                                                            kitManager.getKit(player, chest, kit);
                                                            RandomCoords.getPlugin().limiter.set(player.getUniqueId() + ".Chest", "true");
                                                            RandomCoords.getPlugin().saveLimiter();

                                                        }
                                                    }
                                                }
                                            } else {
                                                ItemStack itemStack = new ItemStack(Material.getMaterial(material));
                                                chestInv.addItem(itemStack);
                                                RandomCoords.getPlugin().limiter.set(player.getUniqueId() + ".Chest", "true");
                                                RandomCoords.getPlugin().saveLimiter();
                                            }

                                        }
                                    }



                                }
                                if(RandomCoords.getPlugin().hasPayed(player, finalThisCost)) {
                                    player.teleport(finalLocationTP);
                                } else {
                                    return;
                                }
                                if(!RandomCoords.getPlugin().config.getString("Sound").equalsIgnoreCase("false")) {
                                    String soundName = RandomCoords.getPlugin().config.getString("Sound");
                                    if(Sound.valueOf(soundName) != null) {
                                        player.playSound(finalLocationTP, Sound.valueOf(soundName), 1, 1);
                                    }


                                }
                                if(!RandomCoords.getPlugin().config.getString("Effect").equalsIgnoreCase("false")) {
                                    String effectName = RandomCoords.getPlugin().config.getString("Effect");
                                    if(Effect.valueOf(effectName) != null) {
                                        finalLocationTP.getWorld().playEffect(finalLocationTP, Effect.valueOf(effectName), 1);
                                    }


                                }

                                // Start the Suffocation cooldown check
                                Cooldown c = new Cooldown(player.getUniqueId(), "Invul", 30);
                                c.start();

                                //Start the InvulnerableTime cooldown
                                Cooldown cT = new Cooldown(player.getUniqueId(), "InvulTime", RandomCoords.getPlugin().config.getInt("InvulTime"));
                                cT.start();


                                messages.teleportMessage(player, finalLocationTP);
                            }
                        }, timeBefore * 20L);

                    } else {
                        int secondsLeft = Cooldown.getTimeLeft((player).getUniqueId(), "Command");
                        messages.cooldownMessage(player, secondsLeft);
                        return;
                    }


                } else {
                    if (!Cooldown.isInCooldown((player).getUniqueId(), "TimeBefore")) {
                        if(timeBefore != 0) {
                            Cooldown cTb = new Cooldown((player).getUniqueId(), "TimeBefore", RandomCoords.getPlugin().config.getInt("TimeBeforeTeleport"));
                            cTb.start();
                            messages.TeleportingIn(player, timeBefore);
                        }
                        BukkitScheduler s = RandomCoords.getPlugin().getServer().getScheduler();


                        Location finalLocationTP1 = locationTP;
                        double finalThisCost1 = thisCost;
                        s.scheduleSyncDelayedTask(RandomCoords.getPlugin().getInstance(), new Runnable() {
                            @Override
                            public void run() {
                                if (RandomCoords.getPlugin().config.getString("StopOnMove").equalsIgnoreCase("true")) {
                                    if (start.distance(player.getLocation()) > 1) {
                                        messages.youMoved(player);
                                        return;
                                    }
                                }
                                if (RandomCoords.getPlugin().config.getString("StopOnCombat").equalsIgnoreCase("true")) {
                                    if (health > player.getHealth()) {
                                        messages.tookDamage(player);
                                        return;
                                    }
                                }
                                // coordinates.finalCoordinates((Player) sender, 574272099, 574272099, ((Player) sender).getWorld(), CoordType.COMMAND, 0);

                                boolean exit = false;
                                while(!exit) {
                                    if(RandomCoords.getPlugin().config.getString("ChunkLoader").equalsIgnoreCase("false")) {
                                        exit = true;
                                    } else {
                                        exit = generateChunk(player, finalLocationTP1);
                                    }
                                    if(exit) {
                                        player.teleport(finalLocationTP1);
                                    } else {

                                    }
                                }

                                //Bonus Chests?
                                if(RandomCoords.getPlugin().config.getString("BonusChest").equalsIgnoreCase("true")) {
                                    if(RandomCoords.getPlugin().limiter.getString(player.getUniqueId() + ".Chest") == null) {

                                        Location chestLoc = new Location(finalLocationTP1.getWorld(), finalLocationTP1.getBlockX() + 1.0, finalLocationTP1.getBlockY() -2 , finalLocationTP1.getBlockZ() + 1.0);
                                        Location airLoc = new Location(chestLoc.getWorld(), chestLoc.getBlockX(), chestLoc.getBlockY() + 1, chestLoc.getBlockZ());
                                        World chestWorld = chestLoc.getWorld();
                                        Block chestBlock = chestLoc.getBlock();
                                        Block airBlock = airLoc.getBlock();
                                        airBlock.setType(Material.AIR);
                                        chestBlock.setType(Material.CHEST);
                                        Chest chest = (Chest) chestBlock.getState();
                                        Inventory chestInv = chest.getInventory();
                                        for (String material : RandomCoords.getPlugin().config.getStringList("BonusChestItems")) {
                                            if (material.contains("Essentials,")) {
                                                if(Bukkit.getServer().getPluginManager().getPlugin("Essentials") != null) {
                                                    KitManager kitManager = new KitManager();
                                                    String[] kits = material.split(",");
                                                for (String kit : kits) {
                                                    if (!kit.equals("Essentials")) {
                                                        kitManager.getKit(player, chest, kit);
                                                        RandomCoords.getPlugin().limiter.set(player.getUniqueId() + ".Chest", "true");
                                                        RandomCoords.getPlugin().saveLimiter();
                                                    }
                                                    }
                                                }
                                            } else {
                                                ItemStack itemStack = new ItemStack(Material.getMaterial(material));
                                                chestInv.addItem(itemStack);
                                                RandomCoords.getPlugin().limiter.set(player.getUniqueId() + ".Chest", "true");
                                                RandomCoords.getPlugin().saveLimiter();
                                            }

                                        }
                                    }



                                }
                                if(RandomCoords.getPlugin().hasPayed(player, finalThisCost1)) {
                                    player.teleport(finalLocationTP1);
                                } else {
                                    return;
                                }
                                if(!RandomCoords.getPlugin().config.getString("Sound").equalsIgnoreCase("false")) {
                                    String soundName = RandomCoords.getPlugin().config.getString("Sound");
                                    if(Sound.valueOf(soundName) != null) {
                                        player.playSound(finalLocationTP1, Sound.valueOf(soundName), 1, 1);
                                    }


                                }
                                if(!RandomCoords.getPlugin().config.getString("Effect").equalsIgnoreCase("false")) {
                                    String effectName = RandomCoords.getPlugin().config.getString("Effect");
                                    if(Effect.valueOf(effectName) != null) {
                                        finalLocationTP1.getWorld().playEffect(finalLocationTP1, Effect.valueOf(effectName), 1);
                                    }


                                }

                                // Start the Suffocation cooldown check
                                Cooldown c = new Cooldown(player.getUniqueId(), "Invul", 30);
                                c.start();

                                //Start the InvulnerableTime cooldown
                                Cooldown cT = new Cooldown(player.getUniqueId(), "InvulTime", RandomCoords.getPlugin().config.getInt("InvulTime"));
                                cT.start();


                                messages.teleportMessage(player, finalLocationTP1);
                            }
                        }, timeBefore * 20L);
                    } else {
                        messages.aboutTo(player, Cooldown.getTimeLeft((player).getUniqueId(), "TimeBefore"));
                        return;
                    }


                }



            }
        }
    }

    private boolean isLocSafe(Location location){
        Location loc = location;
        Block block = loc.getBlock();
        Material material = block.getType();
        Location logLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getY() - 1, loc.getBlockZ());
        Block log = logLoc.subtract(0, 1, 0).getBlock();
        Material matt = log.getType();
        Block log2 = logLoc.subtract(0, 1, 0).getBlock();
        Material matt2 = log2.getType();
        if(loc.getWorld().getBiome(loc.getBlockX(), loc.getBlockZ()).equals(Biome.HELL)) {
            if(loc.getY() == 574272099) {
                return false;
            }
        }

        if(matt == Material.LAVA || matt == Material.STATIONARY_LAVA || matt == Material.WATER || matt == Material.STATIONARY_WATER || material == Material.FIRE || material == Material.CACTUS || matt2 == Material.LOG ||matt == Material.LOG ||material ==  Material.LAVA || material == Material.STATIONARY_LAVA || material == Material.WATER || material == Material.STATIONARY_WATER) {
            return false;

        } else {
            if(fc.FactionCheck(loc) && gpc.griefPrevent(loc) && prc.isPlayerNear(loc) && tc.TownyCheck(loc) && wbc.WorldBorderCheck(loc) && wgc.WorldguardCheck(loc)) {

                return true;
             } else {
                return false;
            }
            }
        }

    public String getRandomOrg(int min, int max) {
        URL site;
        String random = null;

        try {
            String web = "https://www.random.org/integers/?num=1&min=" + min + "&max=" + max + "&col=1&base=10&format=plain&rnd=new";
            site = new URL(web);

            try (BufferedReader in = new BufferedReader(new InputStreamReader(site.openStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    random = line;
                }
            }
        } catch (IOException ignored) {

        }

        return random;
    }

    public boolean generateChunk(Player p, Location location) {
        World world = location.getWorld();
        Chunk chunk = world.getChunkAt(location);
        if(chunk.isLoaded()) {
            return true;
        } else {
            chunk.load();
            return false;
        }
    }




    public Location warpTP(Player p, World world) {
        Set<String> list = null;
        Location lom = new Location(p.getLocation().getWorld(), p.getLocation().getX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ(), p.getLocation().getYaw(), p.getLocation().getPitch());

        for (String worlds : RandomCoords.getPlugin().config.getStringList("BannedWorlds")) {
            if (world.getName().equals(worlds)) {
                messages.worldBanned(p);
                return null;

            }
        }
        if(RandomCoords.getPlugin().warps.get("Warps") != null) {
            list = RandomCoords.getPlugin().warps.getConfigurationSection("Warps.").getKeys(false);
        }
        if(list == null) {
            messages.noWarps(p);
            return null;
        }
        Set<String> listCopy = new HashSet<>(list);
        if(listCopy.size() == 0) {
            messages.noWarps(p);
            return null;
        } else if(listCopy.size() != 0) {

            int i = 0;
            String wName;
            double x;
            double y ;
            double z;
            Location location;
            if(RandomCoords.getPlugin().config.getString("WarpCrossWorld").equalsIgnoreCase("false")) {
                for(Object obj : list) {
                    World objWorld = Bukkit.getServer().getWorld(RandomCoords.getPlugin().warps.getString("Warps." + obj.toString() + ".World"));
                    if (objWorld != world) {
                        listCopy.remove(obj);
                    }
                }
            }
            int size = listCopy.size();
            int myRandom = random.nextInt(size);
            for (Object obj : listCopy) {
                if (i == myRandom) {
                    wName = RandomCoords.getPlugin().warps.getString("Warps." + obj.toString() + ".World");
                    x = RandomCoords.getPlugin().warps.getDouble("Warps." + obj.toString() + ".X");
                    y = RandomCoords.getPlugin().warps.getDouble("Warps." + obj.toString() + ".Y");
                    z = RandomCoords.getPlugin().warps.getDouble("Warps." + obj.toString() + ".Z");
                    location = new Location(Bukkit.getServer().getWorld(wName), x, y, z);
                    return location;
                } else {
                    i = i + 1;
                }


            }
        }

        return null;
    }

    public boolean isLimiter(Player player) {
        if(player.hasPermission("Random.Limiter.Byapss")) {
            return true;
        } else if(RandomCoords.getPlugin().config.getInt("Limit") != 0) {
            String uuid = player.getUniqueId().toString();
            int limit = RandomCoords.getPlugin().config.getInt("Limit");
            FileConfiguration limiter = RandomCoords.getPlugin().limiter;
            if(limiter.get(uuid) == null) {
                limiter.set(uuid + ".Uses", 1);
                RandomCoords.getPlugin().saveLimiter();
                return true;
            }
            int used = limiter.getInt(uuid + ".Uses");
            if(used < limit){
                limiter.set(uuid + ".Uses", used + 1);
                RandomCoords.getPlugin().saveLimiter();
                return true;

            } else {
                messages.reachedLimit(player);
                return false;
            }
        } else {
            return true;
        }
    }






}








