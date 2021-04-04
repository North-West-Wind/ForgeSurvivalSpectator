package ml.northwestwind.survivalspectator;

import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.sun.javafx.geom.Vec3d;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.HashMap;
import java.util.UUID;

@Mod(SurvivalSpectator.MOD_ID)
public class SurvivalSpectator {
    public static final String MOD_ID = "survivalspectator";
    private static final HashMap<UUID, Vector3d> positions = Maps.newHashMap();

    public SurvivalSpectator() {
        MinecraftForge.EVENT_BUS.addListener(this::registerCommand);
    }

    @SubscribeEvent
    public void registerCommand(final RegisterCommandsEvent event) {
        SurvivalSpectatorCommand.register(event.getDispatcher());
    }

    public static class SurvivalSpectatorCommand {
        public static void register(CommandDispatcher<CommandSource> dispatcher) {
            dispatcher.register(Commands.literal("s").executes(context -> {
                Entity entity = context.getSource().getEntity();
                if (!(entity instanceof PlayerEntity)) return 1;
                PlayerEntity player = (PlayerEntity) entity;
                boolean spectating = player.isSpectator();
                if (spectating && !positions.containsKey(player.getUUID())) player.setGameMode(GameType.SURVIVAL);
                else if (!spectating && !positions.containsKey(player.getUUID())) {
                    positions.put(player.getUUID(), player.position());
                    player.setGameMode(GameType.SPECTATOR);
                } else if (spectating && positions.containsKey(player.getUUID())) {
                    Vector3d pos = positions.get(player.getUUID());
                    player.teleportTo(pos.x, pos.y, pos.z);
                    player.setGameMode(GameType.SURVIVAL);
                } else {
                    positions.put(player.getUUID(), player.position());
                    player.setGameMode(GameType.SPECTATOR);
                }
                return 1;
            }));
        }
    }
}
