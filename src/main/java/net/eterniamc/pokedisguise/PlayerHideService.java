package net.eterniamc.pokedisguise;

import org.spongepowered.api.entity.living.player.Player;

public interface PlayerHideService {

    void hide(Player player);

    void show(Player player);
}
