package me.mrnavastar.singularity.loader.mixin;

import me.mrnavastar.singularity.loader.impl.sync.SynchronizedMinecraft;
import me.mrnavastar.singularity.loader.impl.sync.SynchronizedWhiteList;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Inject(method = "setUsingWhiteList", at = @At("TAIL"))
    private void toggle(boolean bl, CallbackInfo ci) {
        SynchronizedWhiteList.setEnabled(bl);
    }

    @Inject(method = "saveAll", at = @At("HEAD"))
    private void save(CallbackInfo ci) {
        SynchronizedMinecraft.syncPlayerData();
    }
}
