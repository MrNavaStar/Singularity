package me.mrnavastar.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import me.mrnavastar.singularity.common.Constants;
import org.slf4j.Logger;

@Plugin(
        id = Constants.MOD_ID,
        name = Constants.MOD_NAME,
        version = Constants.VERSION
)
public class Velocity {

    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
    }
}