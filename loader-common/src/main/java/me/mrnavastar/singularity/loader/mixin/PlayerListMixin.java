package me.mrnavastar.singularity.loader.mixin;

import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.loader.Dead;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Inject(method = "setUsingWhiteList", at = @At("TAIL"))
    private void toggle(boolean bl, CallbackInfo ci) {
        Dead.putStaticTopic(Constants.WHITELIST, new DataBundle().put("enabled", bl));
    }

    @Inject(method = "saveAll", at = @At("HEAD"))
    private void save(CallbackInfo ci) {
        Dead.syncPlayerData();
    }
}
