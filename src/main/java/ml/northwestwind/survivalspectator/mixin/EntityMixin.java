package ml.northwestwind.survivalspectator.mixin;

import ml.northwestwind.survivalspectator.data.PositionData;
import net.minecraft.block.PortalInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public World level;

    @Shadow public abstract UUID getUUID();

    @Shadow public abstract boolean isSpectator();

    @Shadow public float yRot;

    @Shadow public float xRot;

    @Inject(at = @At("HEAD"), method = "findDimensionEntryPoint", cancellable = true)
    public void findDimensionEntryPoint(ServerWorld p_241829_1_, CallbackInfoReturnable<PortalInfo> cir) {
        if (level.isClientSide) return;
        PositionData data = PositionData.get((ServerWorld) level);
        if (data.contains(getUUID()) && !isSpectator()) cir.setReturnValue(new PortalInfo(data.getPlayerPos(getUUID()), Vector3d.ZERO, yRot, xRot));
    }
}
