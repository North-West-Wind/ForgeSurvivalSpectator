package ml.northwestwind.survivalspectator.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import io.netty.buffer.Unpooled;
import ml.northwestwind.survivalspectator.entity.FakePlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.server.*;
import net.minecraft.potion.EffectInstance;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IWorldInfo;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(PlayerList.class)
public abstract class PlayerManagerMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private List<ServerPlayerEntity> players;

    @Shadow public abstract MinecraftServer getServer();

    @Shadow private int viewDistance;

    @Shadow @Nullable public abstract CompoundNBT load(ServerPlayerEntity p_72380_1_);

    @Shadow public abstract void sendPlayerPermissionLevel(ServerPlayerEntity p_187243_1_);

    @Shadow protected abstract void updatePlayerGameMode(ServerPlayerEntity p_72381_1_, @Nullable ServerPlayerEntity p_72381_2_, ServerWorld p_72381_3_);

    @Shadow @Final private Map<UUID, ServerPlayerEntity> playersByUUID;

    @Shadow public abstract void broadcastMessage(ITextComponent p_232641_1_, ChatType p_232641_2_, UUID p_232641_3_);

    @Shadow public abstract void broadcastAll(IPacket<?> p_148540_1_);

    @Shadow public abstract void sendLevelInfo(ServerPlayerEntity p_72354_1_, ServerWorld p_72354_2_);

    @Shadow protected abstract void updateEntireScoreboard(ServerScoreboard p_96456_1_, ServerPlayerEntity p_96456_2_);

    @Shadow @Final private DynamicRegistries.Impl registryHolder;

    @Shadow public abstract int getMaxPlayers();

    /**
     * @author Fabric
     */
    @Overwrite
    public void placeNewPlayer(NetworkManager connection, ServerPlayerEntity player) {
        GameProfile gameProfile = player.getGameProfile();
        PlayerProfileCache userCache = this.server.getProfileCache();
        GameProfile gameProfile2 = userCache.get(gameProfile.getId());
        String string = gameProfile2 == null ? gameProfile.getName() : gameProfile2.getName();
        userCache.add(gameProfile);
        CompoundNBT compoundTag = this.load(player);
        RegistryKey<World> var23;
        if (compoundTag != null) {
            DataResult<RegistryKey<World>> var10000 = DimensionType.parseLegacy(new Dynamic<>(NBTDynamicOps.INSTANCE, compoundTag.get("Dimension")));
            Logger var10001 = LOGGER;
            var10001.getClass();
            var23 = var10000.resultOrPartial(var10001::error).orElse(World.OVERWORLD);
        } else {
            var23 = World.OVERWORLD;
        }

        RegistryKey<World> registryKey = var23;
        ServerWorld serverWorld = this.server.getLevel(registryKey);
        ServerWorld serverWorld3;
        if (serverWorld == null) {
            LOGGER.warn("Unknown respawn dimension {}, defaulting to overworld", registryKey);
            serverWorld3 = this.server.overworld();
        } else {
            serverWorld3 = serverWorld;
        }

        player.setLevel(serverWorld3);
        player.gameMode.setLevel((ServerWorld)player.level);
        String string2 = "local";
        if (connection.getRemoteAddress() != null) {
            string2 = connection.getRemoteAddress().toString();
        }

        LOGGER.info("{}[{}] logged in with entity id {} at ({}, {}, {})", player.getName().getString(), string2, player.getId(), player.getX(), player.getY(), player.getZ());
        IWorldInfo worldProperties = serverWorld3.getLevelData();
        this.updatePlayerGameMode(player, null, serverWorld3);
        ServerPlayNetHandler serverPlayNetworkHandler = new ServerPlayNetHandler(this.server, connection, player);
        GameRules gameRules = serverWorld3.getGameRules();
        boolean bl = gameRules.getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN);
        boolean bl2 = gameRules.getBoolean(GameRules.RULE_REDUCEDDEBUGINFO);
        serverPlayNetworkHandler.send(new SJoinGamePacket(player.getId(), player.gameMode.getGameModeForPlayer(), player.gameMode.getPreviousGameModeForPlayer(), BiomeManager.obfuscateSeed(serverWorld3.getSeed()), worldProperties.isHardcore(), this.server.levelKeys(), this.registryHolder, serverWorld3.dimensionType(), serverWorld3.dimension(), this.getMaxPlayers(), this.viewDistance, bl2, !bl, serverWorld3.isDebug(), serverWorld3.isFlat()));
        serverPlayNetworkHandler.send(new SCustomPayloadPlayPacket(SCustomPayloadPlayPacket.BRAND, (new PacketBuffer(Unpooled.buffer())).writeUtf(this.getServer().getServerModName())));
        serverPlayNetworkHandler.send(new SServerDifficultyPacket(worldProperties.getDifficulty(), worldProperties.isDifficultyLocked()));
        serverPlayNetworkHandler.send(new SPlayerAbilitiesPacket(player.abilities));
        serverPlayNetworkHandler.send(new SHeldItemChangePacket(player.inventory.selected));
        serverPlayNetworkHandler.send(new SUpdateRecipesPacket(this.server.getRecipeManager().getRecipes()));
        serverPlayNetworkHandler.send(new STagsListPacket(this.server.getTags()));
        this.sendPlayerPermissionLevel(player);
        player.getStats().markAllDirty();
        player.getRecipeBook().sendInitialRecipeBook(player);
        this.updateEntireScoreboard(serverWorld3.getScoreboard(), player);
        this.server.forceSynchronousWrites();
        TranslationTextComponent mutableText2;
        if (player.getGameProfile().getName().equalsIgnoreCase(string)) {
            mutableText2 = new TranslationTextComponent("multiplayer.player.joined", player.getDisplayName());
        } else {
            mutableText2 = new TranslationTextComponent("multiplayer.player.joined.renamed", player.getDisplayName(), string);
        }

        if (!(player instanceof FakePlayerEntity)) this.broadcastMessage(mutableText2.withStyle(TextFormatting.YELLOW), ChatType.SYSTEM, Util.NIL_UUID);
        serverPlayNetworkHandler.teleport(player.getX(), player.getY(), player.getZ(), player.yRot, player.xRot);
        this.players.add(player);
        this.playersByUUID.put(player.getUUID(), player);
        this.broadcastAll(new SPlayerListItemPacket(SPlayerListItemPacket.Action.ADD_PLAYER, player));

        for (ServerPlayerEntity serverPlayerEntity : this.players) {
            player.connection.send(new SPlayerListItemPacket(SPlayerListItemPacket.Action.ADD_PLAYER, serverPlayerEntity));
        }

        serverWorld3.addNewPlayer(player);
        this.server.getCustomBossEvents().onPlayerConnect(player);
        this.sendLevelInfo(player, serverWorld3);
        if (!this.server.getResourcePack().isEmpty()) {
            player.sendTexturePack(this.server.getResourcePack(), this.server.getResourcePackHash());
        }

        for (EffectInstance statusEffectInstance : player.getActiveEffects()) {
            serverPlayNetworkHandler.send(new SPlayEntityEffectPacket(player.getId(), statusEffectInstance));
        }

        if (compoundTag != null && compoundTag.contains("RootVehicle", 10)) {
            CompoundNBT compoundTag2 = compoundTag.getCompound("RootVehicle");
            Entity entity = EntityType.loadEntityRecursive(compoundTag2.getCompound("Entity"), serverWorld3, (vehicle) -> !serverWorld3.tryAddFreshEntityWithPassengers(vehicle) ? null : vehicle);
            if (entity != null) {
                UUID uUID2;
                if (compoundTag2.contains("Attach")) {
                    uUID2 = compoundTag2.getUUID("Attach");
                } else {
                    uUID2 = null;
                }

                Iterator<Entity> var21;
                Entity entity3;
                if (entity.getUUID().equals(uUID2)) {
                    player.startRiding(entity, true);
                } else {
                    var21 = entity.getPassengers().iterator();

                    while(var21.hasNext()) {
                        entity3 = var21.next();
                        if (entity3.getUUID().equals(uUID2)) {
                            player.startRiding(entity3, true);
                            break;
                        }
                    }
                }

                if (!player.isPassenger()) {
                    LOGGER.warn("Couldn't reattach entity to player");
                    serverWorld3.onEntityRemoved(entity);
                    var21 = entity.getPassengers().iterator();

                    while(var21.hasNext()) {
                        entity3 = var21.next();
                        serverWorld3.onEntityRemoved(entity3);
                    }
                }
            }
        }

        player.respawn();
    }
}
