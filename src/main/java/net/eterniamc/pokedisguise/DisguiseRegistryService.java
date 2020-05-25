package net.eterniamc.pokedisguise;

import com.pixelmonmod.pixelmon.api.pokemon.PokemonSpec;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Justin
 */
public interface DisguiseRegistryService {

    /**
     * Gets whether the player has the disguise
     */
    boolean hasDisguise(Player player, String disguise);

    void save(Player player);

    void giveDisguise(Player player, String disguise);

    /**
     * Only used in tab selecting in commands
     * @param player
     * @return
     */
    default Set<String> getAllDisguises(Player player) {
        return Arrays.stream(EnumSpecies.values()).map(e-> e.name).filter(n -> hasDisguise(player, n)).collect(Collectors.toSet());
    }
}
