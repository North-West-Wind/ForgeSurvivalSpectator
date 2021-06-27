package ml.northwestwind.survivalspectator.entity;

import com.mojang.authlib.GameProfile;
import ml.northwestwind.survivalspectator.data.PositionData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.play.server.SEntityHeadLookPacket;
import net.minecraft.network.play.server.SEntityTeleportPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.tileentity.SkullTileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.UUID;

@SuppressWarnings("EntityConstructor")
public class FakePlayerEntity extends ServerPlayerEntity
{
    public Runnable fixStartingPosition = () -> {};
    public boolean isAShadow;

    public static FakePlayerEntity createFake(String username, MinecraftServer server, double d0, double d1, double d2, double yaw, double pitch, RegistryKey<World> dimensionId, GameType gamemode, UUID uuid)
    {
        ServerWorld worldIn = server.getLevel(dimensionId);
        PlayerInteractionManager interactionManagerIn = new PlayerInteractionManager(worldIn);
        GameProfile profile = server.getProfileCache().get(username);
        if (profile == null) return null;
        GameProfile gameprofile = new GameProfile(uuid != null ? uuid : UUID.randomUUID(), username);
        if (profile.getProperties().containsKey("textures")) {
            GameProfile finalGameprofile = gameprofile;
            profile.getProperties().get("textures").forEach(property -> finalGameprofile.getProperties().put("textures", property));
            gameprofile = SkullTileEntity.updateGameprofile(finalGameprofile);
        }
        FakePlayerEntity instance = new FakePlayerEntity(server, worldIn, gameprofile, interactionManagerIn, false);
        instance.fixStartingPosition = () -> instance.moveTo(d0, d1, d2, (float) yaw, (float) pitch);
        server.getPlayerList().placeNewPlayer(new FakeNetworkManager(PacketDirection.SERVERBOUND), instance);
        instance.teleportTo(worldIn, d0, d1, d2, (float)yaw, (float)pitch);
        instance.setHealth(20.0F);
        instance.removed = false;
        instance.maxUpStep = 0.6F;
        interactionManagerIn.setGameModeForPlayer(gamemode);
        server.getPlayerList().broadcastAll(new SEntityHeadLookPacket(instance, (byte) (instance.yHeadRot * 256 / 360)), dimensionId);//instance.dimension);
        server.getPlayerList().broadcastAll(new SEntityTeleportPacket(instance), dimensionId);//instance.dimension);
        instance.getLevel().getChunkSource().move(instance);
        instance.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f); // show all model layers (incl. capes)
        return instance;
    }

    private FakePlayerEntity(MinecraftServer server, ServerWorld worldIn, GameProfile profile, PlayerInteractionManager interactionManagerIn, boolean shadow)
    {
        super(server, worldIn, profile, interactionManagerIn);
        isAShadow = shadow;
    }

    @Override
    protected void playEquipSound(ItemStack stack)
    {
        if (!isUsingItem()) super.playEquipSound(stack);
    }

    @Override
    public void tick()
    {
        if (this.getServer().getTickCount() % 10 == 0)
        {
            this.connection.resetPosition();
            this.getLevel().getChunkSource().move(this);
        }
        super.tick();
        this.doTick();
    }

    private void shakeOff()
    {
        if (getVehicle() instanceof PlayerEntity) stopRiding();
        for (Entity passenger : getIndirectPassengers())
        {
            if (passenger instanceof PlayerEntity) passenger.stopRiding();
        }
    }

    @Override
    public boolean hurt(DamageSource cause, float damage) {
        boolean sup = super.hurt(cause, damage);
        if (sup) {
            PositionData data = PositionData.get(this.getLevel());
            UUID uuid = data.getPlayerByFake(this.getUUID());
            if (uuid != null) {
                Entity entity = this.getLevel().getEntity(uuid);
                if (entity instanceof ServerPlayerEntity) data.toSurvival((ServerPlayerEntity) entity);
            }
        }
        return sup;
    }

    @Override
    public void die(DamageSource cause)
    {
        shakeOff();
        kill();
        PositionData data = PositionData.get(this.getLevel());
        UUID uuid = data.getPlayerByFake(this.getUUID());
        if (uuid != null) {
            Entity entity = this.getLevel().getEntity(uuid);
            if (entity instanceof ServerPlayerEntity) {
                data.toSurvival((ServerPlayerEntity) entity);
                ((ServerPlayerEntity) entity).die(cause);
            }
        }
    }

    @Override
    public void kill()
    {
        kill(new StringTextComponent("Disconnect fake player"));
    }

    public void kill(ITextComponent reason)
    {
        shakeOff();
        this.server.tell(new TickDelayedTask(this.server.getTickCount(), () -> this.connection.onDisconnect(reason)));
    }

    @Override
    public String getIpAddress()
    {
        return "127.0.0.1";
    }
}