package ml.northwestwind.survivalspectator.data;

import com.google.common.collect.Maps;
import ml.northwestwind.survivalspectator.entity.FakePlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

public class PositionData extends WorldSavedData {
    private final Map<UUID, Pair<Vector3d, RegistryKey<World>>> positions = Maps.newHashMap();
    private final Map<UUID, UUID> playerPlaceholders = Maps.newHashMap();
    public static final String NAME = "survivalspectator";

    public PositionData() {
        super(NAME);
    }

    public static PositionData get(ServerWorld world) {
        return world.getServer().overworld().getDataStorage().computeIfAbsent(PositionData::new, NAME);
    }

    @Override
    public void load(CompoundNBT tag) {
        ListNBT list = (ListNBT) tag.get("spectators");
        if (list != null) {
            int i = 0;
            while (!list.getCompound(i).isEmpty()) {
                CompoundNBT compound = list.getCompound(i);
                Vector3d pos = new Vector3d(compound.getDouble("x"), compound.getDouble("y"), compound.getDouble("z"));
                RegistryKey<World> dimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(compound.getString("dimension")));
                positions.put(compound.getUUID("uuid"), Pair.of(pos, dimension));
                i++;
            }
        }
        list = (ListNBT) tag.get("fakes");
        if (list != null) {
            int i = 0;
            while (!list.getCompound(i).isEmpty()) {
                CompoundNBT compound = list.getCompound(i);
                playerPlaceholders.put(compound.getUUID("uuid"), compound.getUUID("fake"));
                i++;
            }
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        ListNBT list = new ListNBT();
        int i = 0;
        for (Map.Entry<UUID, Pair<Vector3d, RegistryKey<World>>> entry : positions.entrySet()) {
            CompoundNBT compound = new CompoundNBT();
            compound.putUUID("uuid", entry.getKey());
            Vector3d pos = entry.getValue().getKey();
            compound.putDouble("x", pos.x);
            compound.putDouble("y", pos.y);
            compound.putDouble("z", pos.z);
            compound.putString("dimension", entry.getValue().getValue().getRegistryName().toString());
            list.add(i++, compound);
        }
        tag.put("spectators", list);
        ListNBT fakes = new ListNBT();
        i = 0;
        for (Map.Entry<UUID, UUID> entry : playerPlaceholders.entrySet()) {
            CompoundNBT compound = new CompoundNBT();
            compound.putUUID("uuid", entry.getKey());
            compound.putUUID("fake", entry.getValue());
            fakes.add(i++, compound);
        }
        tag.put("fakes", fakes);
        return tag;
    }

    public boolean contains(UUID uuid) {
        return positions.containsKey(uuid) && playerPlaceholders.containsKey(uuid);
    }

    public void toSpectator(ServerPlayerEntity player) {
        positions.put(player.getUUID(), Pair.of(player.position(), player.level.dimension()));
        FakePlayerEntity fake = FakePlayerEntity.createFake(player.getScoreboardName(), player.getServer(), player.getX(), player.getY(), player.getZ(), player.yRot, player.xRot, player.level.dimension(), GameType.SURVIVAL);
        if (fake != null) playerPlaceholders.put(player.getUUID(), fake.getUUID());
    }

    public void toSurvival(ServerPlayerEntity player) {
        Vector3d pos = positions.get(player.getUUID()).getLeft();
        RegistryKey<World> dimension = positions.get(player.getUUID()).getRight();
        ServerPlayerEntity fake;
        if (!player.level.dimension().equals(dimension)) {
            ServerWorld world = ((ServerWorld) player.level).getServer().getLevel(dimension);
            if (world == null) return;
            fake = (ServerPlayerEntity) world.getEntity(playerPlaceholders.get(player.getUUID()));
            player.changeDimension(world);
        } else {
            fake = (ServerPlayerEntity) player.getLevel().getEntity(playerPlaceholders.get(player.getUUID()));
            player.teleportToWithTicket(pos.x, pos.y, pos.z);
        }
        positions.remove(player.getUUID());

        playerPlaceholders.remove(player.getUUID());
        if (fake == null) return;
        if (fake.removed) player.kill();
        fake.kill();
    }

    public Vector3d getPlayerPos(UUID uuid) {
        return positions.get(uuid).getLeft();
    }

    @Nullable
    public UUID getPlayerByFake(UUID uuid) {
        return playerPlaceholders.entrySet().stream().filter(entry -> entry.getValue().equals(uuid)).findFirst().orElseGet(() -> new NullEntry<>(uuid)).getKey();
    }

    private static class NullEntry<K, V> implements Map.Entry<K, V> {
        private V value;

        public NullEntry(V value) {
            this.value = value;
        }

        @Override
        public K getKey() {
            return null;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            this.value = value;
            return value;
        }
    }
}
