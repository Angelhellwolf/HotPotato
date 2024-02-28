package com.rmc.hotpotato;

import com.rmc.hotpotato.game.Game;
import com.rmc.hotpotato.game.GameState;
import com.rmc.hotpotato.player.Block_Interaction;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author angel
 */
public final class HotPotato extends JavaPlugin {
    private Game game;

    @Override
    public void onEnable() {
        game = new Game(this);
        new Block_Interaction(this);

    }

    @Override
    public void onDisable() {
        game.setGameState(GameState.END);
        Bukkit.getScheduler().cancelTask(game.gameTask);
    }
}
