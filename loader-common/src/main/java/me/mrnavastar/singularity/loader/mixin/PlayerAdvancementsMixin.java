package me.mrnavastar.singularity.loader.mixin;

import me.mrnavastar.singularity.loader.impl.Broker;
import net.minecraft.server.PlayerAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void save(CallbackInfo ci) {
        if (Broker.getSettings().syncPlayerAdvancements) ci.cancel();
    }
}