package me.mrnavastar.singularity.loader.impl;

import com.mojang.authlib.GameProfileRepository;
import net.minecraft.server.players.GameProfileCache;

import java.io.File;

public class SynchronizedUserCache extends GameProfileCache {

    public SynchronizedUserCache(GameProfileRepository gameProfileRepository, File file) {
        super(gameProfileRepository, file);
    }
}
