package me.mrnavastar.singularity.common.networking;

import lombok.ToString;

import java.util.HashSet;

@ToString
public class Settings {
    public boolean syncPlayerData = false;
    public boolean syncPlayerStats = false;
    public boolean syncPlayerAdvancements = false;
    public boolean syncOps = false;
    public boolean syncWhitelist = false;
    public boolean syncBans = false;

    public HashSet<String> nbtBlacklists = new HashSet<>();

    public Settings setDefault() {
        syncPlayerData = true;
        syncPlayerStats = true;
        syncPlayerAdvancements = true;
        syncOps = true;
        syncWhitelist = true;
        syncBans = true;

        nbtBlacklists.add("singularity.location");
        nbtBlacklists.add("singularity.spawn");
        return this;
    }
}
