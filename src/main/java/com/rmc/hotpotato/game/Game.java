package com.rmc.hotpotato.game;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author angel
 */
public class Game implements Listener {
    private final Random random = new Random();
    private GameState gameState;
    private final Plugin plugin;
    public int gameTask;
    public int startTask;
    public int runTask;
    private static final int MIN_PLAYERS = 3;

    private final List<Player> playersInGame = new ArrayList<>();

    private final List<Player> spectators = new ArrayList<>();
    private final List<Player> hotPlayers = new ArrayList<>();
    private boolean gameInProgress = false;
    private boolean startRunTask = true;

    public void addHotPlayer(Player player) {
        hotPlayers.add(player);
    }

    public void removeHotPlayer(Player player) {
        hotPlayers.remove(player);
    }

    public void addPlayerToGame(Player player) {
        playersInGame.add(player);
    }

    public void removePlayerFromGame(Player player) {
        playersInGame.remove(player);
    }

    public void addSpectator(Player player) {
        spectators.add(player);
    }

    public void removeSpectator(Player player) {
        spectators.remove(player);
    }


    @EventHandler
    public void playerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        player.removePotionEffect(PotionEffectType.SPEED);
        player.getInventory().clear();
        GameState gameState = getGameState();

        if (gameState == GameState.WAITING) {
            handlePlayerJoinWaiting(player);
            return;
        }

        if (gameState == GameState.RUNNING && !spectators.contains(player)) {
            addSpectator(player);
            spectator(player);
        }
    }

    private void handlePlayerJoinWaiting(Player player) {
        player.setDisplayName("§8[§7生土豆§8] " + player.getName());
        addPlayerToGame(player);

        if (playersInGame.size() >= MIN_PLAYERS) {
            startCountdown();
        }
    }


    private void startCountdown() {
        // 开始倒计时
        gameInProgress = true;
        // 在这里执行倒计时逻辑
        if (startRunTask) {
            startRunTask = false;
            Bukkit.broadcastMessage("§c游戏将在 60s 后开始");
            startTask = Bukkit.getScheduler().runTaskLater(plugin, this::gameStart, 1200).getTaskId();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        quit(player);
        int count = 0;
        if (hotPlayers.contains(player) && playersInGame.contains(player)) {
            removePlayerFromGame(player);
            for (Player hotPlayer : hotPlayers) {
                if (hotPlayer.isOnline()) {
                    count++;
                } else {
                    removeHotPlayer(player);
                }
                if (count <= 0) {
                    selectRandomPlayersAsHot();
                }
            }
        }
        if (gameState == GameState.WAITING) {
            if (playersInGame.size() < MIN_PLAYERS && gameInProgress) {
                cancelCountdown();
            }
        }
    }

    public void quit(Player player) {
        if (playersInGame.contains(player)) {
            removePlayerFromGame(player);
        }
    }


    private void cancelCountdown() {
        // 取消倒计时
        gameInProgress = false;
        startRunTask = true;
        // 在这里执行取消倒计时的逻辑，如清除倒计时任务等
        Bukkit.broadcastMessage("§c玩家不足，倒计时已取消");
        Bukkit.getScheduler().cancelTask(startTask);
    }

    private void gameStart() {
        // 游戏开始
        Bukkit.broadcastMessage("游戏开始！");
        // 在这里执行游戏开始的逻辑
        setGameState(GameState.RUNNING);
        run();
    }

    public void selectRandomPlayersAsHot() {
        int numPlayersToAddToHot;
        int totalPlayers = playersInGame.size();

        if (totalPlayers >= 5) {
            numPlayersToAddToHot = 2 + (totalPlayers - 5) / 3;
        } else {
            numPlayersToAddToHot = 1;
        }
        numPlayersToAddToHot = Math.min(numPlayersToAddToHot, totalPlayers);

        List<Player> availablePlayers = new ArrayList<>(playersInGame);
        availablePlayers.removeAll(hotPlayers);

        for (int i = 0; i < numPlayersToAddToHot; i++) {
            if (availablePlayers.isEmpty()) {
                break; // 如果没有可选的玩家了，直接跳出循环
            }
            int randomIndex = random.nextInt(availablePlayers.size());
            Player randomPlayer = availablePlayers.get(randomIndex);
            addHotPlayer(randomPlayer);
            availablePlayers.remove(randomPlayer); // 从可选玩家列表中移除已经选择的玩家
        }
    }



    public void teleportHot() {
        Player randomHotPlayer = hotPlayers.get(random.nextInt(hotPlayers.size()));
        Location hotPlayerLocation = randomHotPlayer.getLocation();
        for (Player player : playersInGame) {
            player.teleport(hotPlayerLocation);
        }
    }

    public void run() {
        runTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if(!hotPlayers.isEmpty()){
            List<Player> playersToRemove = new ArrayList<>();
            new ArrayList<>(hotPlayers).forEach(hotPlayer -> {
                spectator(hotPlayer);
                if (playersInGame.contains(hotPlayer)) {
                    playersToRemove.add(hotPlayer);
                }
                playExplosionEffect(hotPlayer);
            });
            playersToRemove.forEach(this::removePlayerFromGame);
            }
            selectRandomPlayersAsHot();
            teleportHot();
        }, 0L, 1200L);
        // 20tick = 1s
        // 60s = 1200tick
    }



    private void playExplosionEffect(Player hotPlayer) {
        if (playersInGame.contains(hotPlayer) && hotPlayer.isOnline()) {
            Location hotPlayerLocation = hotPlayer.getLocation();
            World world = hotPlayer.getWorld();
            // 播放爆炸声音
            world.playSound(hotPlayerLocation, Sound.EXPLODE, 1, 1);
            // 生成爆炸粒子效果
            world.playEffect(hotPlayerLocation, Effect.EXPLOSION_LARGE, null);
        }
    }


    public void spectator(Player player) {
        player.getInventory().clear(); // 清空玩家背包
        player.getEquipment().setHelmet(null); // 移除玩家头上的装备
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setDisplayName("§8[§7熟透了§8] " + player.getName() + "§f");
        addSpectator(player);
        for (Player inGame : playersInGame) {
            inGame.hidePlayer(player);
        }
    }

    @EventHandler
    public void entityDamageEvent(EntityDamageEvent e) {
        e.setDamage(0);
        Entity damagedEntity = e.getEntity();
        if (!(damagedEntity instanceof Player)) {
            return;
        }
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }
        Entity attacker = ((EntityDamageByEntityEvent) e).getDamager();

        if (!(attacker instanceof Player)) {
            return;
        }

        Player att = (Player) attacker;
        Player damagedPlayer = (Player) damagedEntity;

        if (spectators.contains(att) || spectators.contains(damagedPlayer)) {
            e.setCancelled(true);
            return;
        }

        if (hotPlayers.contains(damagedEntity) && hotPlayers.contains(att)) {
            e.setCancelled(true);
            return;
        }
        if (playersInGame.contains(damagedPlayer) && hotPlayers.contains(att)) {
            removeHotPlayer(att);
            att.getInventory().clear();
            att.getEquipment().setHelmet(null);
            att.removePotionEffect(PotionEffectType.SPEED);
            addHotPlayer(damagedPlayer);
        }

        if (spectators.contains(att)) {
            e.setCancelled(true);
        }
    }


    public void showPlayer(Player player) {
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setDisplayName("§8[§7生土豆§8] " + player.getName() + "§f");
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.showPlayer(player);
        }
    }

    public Game(Plugin plugin) {
        this.gameState = GameState.INITIALIZING;
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        checkGameState();
        setGameState(GameState.WAITING);
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public GameState getGameState() {
        return this.gameState;
    }

    public void checkGameState() {
        gameTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
            if (gameState == GameState.RUNNING) {
                if (playersInGame.size() == 1) {
                    gameOver();
                }
            }
            if (gameState == GameState.END) {
                gameSave();
            }
            for (Player player : onlinePlayers) {
                if (hotPlayers.contains(player) && !spectators.contains(player)) {

                    // 玩家在 hotPlayers 中但不在 spectators 中
                    player.getInventory().clear(); // 清空玩家背包
                    player.getEquipment().setHelmet(new ItemStack(Material.TNT)); // 给玩家头上戴上TNT
                    // 将0-9物品栏填满马铃薯
                    ItemStack potato = new ItemStack(Material.POTATO_ITEM);

                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,9999,2,false),false);
                    for (int i = 0; i < 9; i++) {
                        player.getInventory().setItem(i, potato);
                    }
                }
                if (spectators.contains(player)) {
                    // 玩家在 spectators 中
                    player.removePotionEffect(PotionEffectType.SPEED);
                    player.getInventory().clear(); // 清空玩家背包
                    player.getEquipment().setHelmet(null); // 移除玩家头上的装备
                }
            }

        }, 0L, 20L);
    }

    public void gameOver() {
        Bukkit.getScheduler().cancelTask(runTask);
        List<Player> players = new ArrayList<>(playersInGame);

        if (players.isEmpty()) {
            return;
        }

        Player winner = players.get(0);

        Bukkit.getServer().broadcastMessage("§b恭喜 " + winner.getName() + " §c是唯一的胜利者");

        setGameState(GameState.END);
        for (Player hotPlayer : new ArrayList<>(hotPlayers)) {
            removeHotPlayer(hotPlayer);
            hotPlayer.getInventory().clear();
            hotPlayer.getEquipment().setHelmet(null);
        }

        for (Player spectator : new ArrayList<>(spectators)) {
            removeSpectator(spectator);
            spectator.getInventory().clear();
            spectator.getEquipment().setHelmet(null);
        }

        for (Player inGame : new ArrayList<>(playersInGame)) {
            removePlayerFromGame(inGame);
            inGame.getInventory().clear();
            inGame.getEquipment().setHelmet(null);
        }

        setGameState(GameState.INITIALIZING);
        gameInProgress = false;
        startRunTask = true;

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            showPlayer(onlinePlayer);
            handlePlayerJoinWaiting(onlinePlayer);
            onlinePlayer.removePotionEffect(PotionEffectType.SPEED);
        }
        setGameState(GameState.WAITING);
    }


    public void gameSave() {
        //执行保存游戏当前数据的逻辑
    }

}
