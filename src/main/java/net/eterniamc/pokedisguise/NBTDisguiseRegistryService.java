package net.eterniamc.pokedisguise;

import com.pixelmonmod.pixelmon.api.pokemon.PokemonSpec;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.entity.living.player.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Justin
 */
public class NBTDisguiseRegistryService implements DisguiseRegistryService {
    private static File file = new File("./config/disguises");
    public Map<Player, NBTTagCompound> data = new HashMap<>();

    public NBTTagCompound getPlayerData(Player player) {
        if (data.containsKey(player))
            return data.get(player);
        else {
            try {
                NBTTagCompound nbt = CompressedStreamTools.read(new File(file,player.getIdentifier()+".dat"));
                if (nbt == null)
                    nbt = new NBTTagCompound();
                data.put(player, nbt);
                return nbt;
            } catch(Exception e) {
                if (new File(file,player.getIdentifier()+".dat").exists()) {
                    e.printStackTrace();
                }
                NBTTagCompound nbt = new NBTTagCompound();
                data.put(player, nbt);
                save(player);
                return nbt;
            }
        }
    }

    @Override
    public boolean hasDisguise(Player player, PokemonSpec disguise) {
        return getPlayerData(player).contains(disguise.name) && new PokemonSpec().readFromNBT(getPlayerData(player).getCompound(disguise.name)).matches(disguise.create());
    }

    @Override
    public void giveDisguise(Player player, PokemonSpec disguise) {
        NBTTagCompound nbt = getPlayerData(player);
        nbt.put(disguise.name, disguise.writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void save(Player player) {
        if (getPlayerData(player).size() != 0) {
            try {
                File file = new File(this.file, player.getIdentifier() + ".dat");
                if (!file.exists()) {
                    file.getParentFile().mkdir();
                    file.createNewFile();
                }
                CompressedStreamTools.write(getPlayerData(player), file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Set<String> getAllDisguises(Player player) {
        return getPlayerData(player).keySet();
    }
}
