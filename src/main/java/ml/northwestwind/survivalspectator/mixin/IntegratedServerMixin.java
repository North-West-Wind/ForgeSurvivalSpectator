package ml.northwestwind.survivalspectator.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Shadow private UUID uuid;

    /**
     * @author Forge
     */
    @Overwrite
    public boolean isSingleplayerOwner(GameProfile p_213199_1_) {
        return p_213199_1_.getId().equals(this.uuid);
    }
}
