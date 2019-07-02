package net.eterniamc.pokedisguise;

import com.pixelmonmod.pixelmon.api.pokemon.PokemonSpec;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.PermissionService;

/**
 * Created by Justin
 */
public class PermissionDisguiseRegistryService implements DisguiseRegistryService {
    @Override
    public boolean hasDisguise(Player player, PokemonSpec disguise) {
        return player.hasPermission(getPermission(disguise));
    }

    @Override
    public void save(Player player) {}

    @Override
    public void giveDisguise(Player player, PokemonSpec disguise) {
        Sponge.getCommandManager().process(Sponge.getServer().getConsole(), "lp user "+player.getName()+" permission set "+getPermission(disguise)+" true");
    }

    private String getPermission(PokemonSpec from) {
        return "pd."+from.name;
    }
}
