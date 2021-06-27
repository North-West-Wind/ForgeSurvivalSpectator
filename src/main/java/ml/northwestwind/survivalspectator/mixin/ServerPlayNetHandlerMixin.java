package ml.northwestwind.survivalspectator.mixin;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.filter.IChatFilter;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayNetHandler.class)
public abstract class ServerPlayNetHandlerMixin {
    @Shadow @Final private static Logger LOGGER;

    @Shadow public ServerPlayerEntity player;

    @Shadow @Final private MinecraftServer server;

    @Shadow protected abstract boolean isSingleplayerOwner();

    /**
     * @author Forge
     */
    @Overwrite
    public void onDisconnect(ITextComponent text) {
        this.LOGGER.info("{} lost connection: {}", this.player.getName().getString(), text.getString());
        this.server.invalidateStatus();
        if (!text.getString().equals("Disconnect fake player"))
            this.server.getPlayerList().broadcastMessage((new TranslationTextComponent("multiplayer.player.left", this.player.getDisplayName())).withStyle(TextFormatting.YELLOW), ChatType.SYSTEM, Util.NIL_UUID);
        this.player.disconnect();
        this.server.getPlayerList().remove(this.player);
        IChatFilter ichatfilter = this.player.getTextFilter();
        if (ichatfilter != null) {
            ichatfilter.leave();
        }

        if (this.isSingleplayerOwner()) {
            LOGGER.info("Stopping singleplayer server as player logged out");
            this.server.halt(false);
        }

    }
}
