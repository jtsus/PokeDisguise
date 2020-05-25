package net.eterniamc.pokedisguise;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;

public class VanishPlayerHideService implements PlayerHideService {

    @Override
    public void hide(Player player) {
        player.offer(Keys.VANISH, true);
    }

    @Override
    public void show(Player player) {
        player.offer(Keys.VANISH, false);
    }
}
