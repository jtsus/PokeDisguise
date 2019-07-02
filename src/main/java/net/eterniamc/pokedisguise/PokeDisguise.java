package net.eterniamc.pokedisguise;

import com.flowpowered.math.vector.Vector3d;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonBase;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonSpec;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityStatue;
import com.pixelmonmod.pixelmon.enums.EnumBoundingBoxMode;
import com.pixelmonmod.pixelmon.enums.EnumSpecies;
import com.pixelmonmod.pixelmon.enums.EnumStatueTextureType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.world.World;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.ClickAction;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Plugin(
        id = "pokedisguise",
        name = "pokedisguise",
        authors = "Justin"
)
public class PokeDisguise {
    private static Map<Player, EntityStatue> disguises = new HashMap<>();
    private DisguiseRegistryService service;

    @Listener
    public void onServerPreInit(GamePreInitializationEvent event) {
        //Sponge.getServiceManager().setProvider(this, DisguiseRegistryService.class, new NBTDisguiseRegistryService());
        Sponge.getServiceManager().setProvider(this, DisguiseRegistryService.class, new PermissionDisguiseRegistryService());
    }

    @Listener
    public void onServerStart(GamePostInitializationEvent event) {
        service = Sponge.getServiceManager().provideUnchecked(DisguiseRegistryService.class);
        Sponge.getCommandManager().register(this,
                CommandSpec.builder()
                        .permission("pokedisguise.disguise")
                        .arguments(
                                GenericArguments.playerOrSource(Text.of("player")),
                                GenericArguments.withSuggestions(
                                        GenericArguments.remainingRawJoinedStrings(Text.of("pokemon")),
                                        src-> service.getAllDisguises((Player) src)
                                )
                        )
                        .executor(((src, args) -> {
                            PokemonSpec spec = new PokemonSpec(args.<String>getOne("pokemon").get());
                            Player player = src.hasPermission("pokedisguise.other") ? args.<Player>getOne("player").get() : (Player)src;
                            if (service.hasDisguise(player, spec) || src.hasPermission("pokedisguise.all")) {
                                disguise(player, spec);
                            } else
                                player.sendMessage(Text.of(TextColors.RED, "You don't own this disguise!"));
                            return CommandResult.empty();
                        }))
                        .child(CommandSpec.builder()
                                        .permission("pokedisguise.give")
                                        .arguments(
                                                GenericArguments.playerOrSource(Text.of("player")),
                                                GenericArguments.withSuggestions(
                                                        GenericArguments.remainingJoinedStrings(Text.of("pokemon")),
                                                        src-> Arrays.stream(EnumSpecies.values()).map(Enum::name).collect(Collectors.toSet())
                                                )
                                        )
                                        .executor(((src, args) -> {
                                            service.giveDisguise(
                                                    args.<Player>getOne("player").get(),
                                                    new PokemonSpec(args.<String>getOne("pokemon").get())
                                            );
                                            src.sendMessage(Text.of("Disguise has been given"));
                                            return CommandResult.empty();
                                        }))
                                        .build(),
                                "give"
                        )
                        .child(CommandSpec.builder()
                                        .executor(((src, args) -> {
                                            if (src instanceof Player) {
                                                Optional.ofNullable(disguises.remove(src)).ifPresent(net.minecraft.entity.Entity::remove);
                                                ((Player) src).offer(Keys.INVISIBLE, false);
                                                ((Player) src).offer(Keys.VANISH, false);
                                            }
                                            return CommandResult.empty();
                                        }))
                                        .build(),
                                "off"
                        )
                        .build(),
                "pdisguise","pokedisguise", "disguise"
        );
        Task.builder().intervalTicks(1).execute(() -> {
            disguises.forEach((player, statue) -> {
                if (!statue.isAlive()) {
                    PokemonBase base = statue.getPokemon();
                    EnumStatueTextureType type = statue.getTextureType();
                    NBTTagCompound nbt = statue.getEntityData();
                    boolean hidden = statue.getEntityData().getBoolean("hidden");
                    statue = new EntityStatue((World)player.getWorld());
                    statue.setPokemon(base);
                    statue.setTextureType(type);
                    statue.getEntityData().putBoolean("hidden", hidden);
                    for (String key : nbt.keySet()) {
                        statue.getEntityData().put(key, nbt.get(key));
                    }
                    ((Entity)statue).setLocation(player.getLocation());
                    try {
                        Field dwBox = EntityStatue.class.getDeclaredField("dwBoundMode");
                        dwBox.setAccessible(true);
                        statue.getDataManager().set((DataParameter<Byte>) dwBox.get(null), (byte) EnumBoundingBoxMode.None.ordinal());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    statue.setAnimate(true);
                    ((World) player.getWorld()).spawnEntity(statue);
                    if (hidden)
                        ((EntityPlayerMP)player).removeEntity(statue);
                    disguises.put(player, statue);
                }
                Vector3d pos = player.getLocation().getPosition();
                statue.setPosition(pos.getX(), pos.getY(), pos.getZ());
                statue.setRotation(((EntityPlayerMP)player).rotationYaw);
                Vector3d speed = player.getVelocity().abs();
                if (!statue.getEntityData().getBoolean("hidden"))
                    player.offer(Keys.INVISIBLE, true);
                statue.setIsFlying(player.get(Keys.IS_FLYING).orElse(false));
                if (speed.getX() + speed.getZ() > .2) {
                    if (!statue.getAnimation().equals("walk"))
                        statue.setAnimation("walk");
                } else
                    statue.setAnimation("idle");
            });
        }).submit(this);
        Task.builder().execute(()-> {
            for (Player player : Sponge.getServer().getOnlinePlayers())
                service.save(player);
        }).interval(1, TimeUnit.MINUTES).submit(this);
    }

    @Listener
    public void onPlayerLeave(ClientConnectionEvent.Disconnect event) {
        Optional.ofNullable(disguises.remove(event.getTargetEntity())).ifPresent(net.minecraft.entity.Entity::remove);
        service.save(event.getTargetEntity());
        event.getTargetEntity().offer(Keys.VANISH, false);
    }

    @Listener
    public void onServerStopping(GameStoppingServerEvent event) {
        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            Optional.ofNullable(disguises.remove(player)).ifPresent(net.minecraft.entity.Entity::remove);
            service.save(player);
        }
    }

    public static void disguise(Player player, PokemonSpec spec) {
        EntityStatue statue = new EntityStatue((World)player.getWorld());
        statue.setPokemon(spec.create());
        spec.apply(statue.getEntityData());
        ((Entity)statue).setLocation(player.getLocation());
        try {
            Field dwBox = EntityStatue.class.getDeclaredField("dwBoundMode");
            dwBox.setAccessible(true);
            statue.getDataManager().set((DataParameter<Byte>) dwBox.get(null), (byte) EnumBoundingBoxMode.None.ordinal());
        } catch (Exception e) {
            e.printStackTrace();
        }
        statue.setAnimate(true);
        player.offer(Keys.VANISH, true);
        ((World) player.getWorld()).spawnEntity(statue);
        Optional.ofNullable(disguises.put(player, statue)).ifPresent(net.minecraft.entity.Entity::remove);
        AtomicReference<Text> message = new AtomicReference<>();
        message.set(Text.builder()
                .append(TextSerializers.FORMATTING_CODE.deserialize("&4&lPokeDisguise &7&l>&r Click &aHERE&r to hide your disguise"))
                .onHover(TextActions.showText(Text.of(TextColors.WHITE, "Hiding your disguise makes it so others can see it but you can't")))
                .onClick(TextActions.executeCallback(src-> {
                    disguises.get(player).getEntityData().putBoolean("hidden", true);
                    ((EntityPlayerMP) player).removeEntity(disguises.get(player));
                    player.offer(Keys.INVISIBLE, false);
                    player.sendMessage(
                            Text.builder()
                                    .append(TextSerializers.FORMATTING_CODE.deserialize("&4&lPokeDisguise &7&l>&r Click &aHERE&r to unhide your disguise"))
                                    .onHover(TextActions.showText(Text.of(TextColors.WHITE, "Unhiding your disguise makes it so you can see it")))
                                    .onClick(TextActions.executeCallback(src1-> {
                                        disguises.get(player).getEntityData().putBoolean("hidden", false);
                                        disguises.get(player).remove();
                                        player.sendMessage(message.get());
                                    }))
                                    .build()
                    );
                }))
                .build());
        player.sendMessage(message.get());
    }
}
