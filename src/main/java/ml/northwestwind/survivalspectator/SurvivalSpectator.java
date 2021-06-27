package ml.northwestwind.survivalspectator;

import com.mojang.brigadier.CommandDispatcher;
import ml.northwestwind.survivalspectator.data.PositionData;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;

@Mod(SurvivalSpectator.MOD_ID)
public class SurvivalSpectator {
    public static final String MOD_ID = "survivalspectator";

    public SurvivalSpectator() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }

    public static class SurvivalSpectatorCommand {
        public static void register(CommandDispatcher<CommandSource> dispatcher) {
            dispatcher.register(Commands.literal("s").executes(context -> handlePlayer(context.getSource().getEntity())));
        }

        private static int handlePlayer(Entity entity) {
            if (!(entity instanceof PlayerEntity)) return 0;
            if (entity.level.isClientSide) return 1;
            PositionData data = PositionData.get((ServerWorld) entity.level);
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            boolean spectating = player.isSpectator();
            if (spectating) data.toSurvival(player);
            else data.toSpectator(player);
            data.setDirty();
            return 1;
        }
    }

    @Mod.EventBusSubscriber
    public static class ForgeEvents {
        @SubscribeEvent
        public static void registerCommand(final RegisterCommandsEvent event) {
            SurvivalSpectatorCommand.register(event.getDispatcher());
        }

        /* @SubscribeEvent
        public static void worldLoad(final WorldEvent.Load event) {
            ServerWorld world = (ServerWorld) event.getWorld();
            PositionData.get(world).reAddFake(world.getServer());
        } */
    }
}
