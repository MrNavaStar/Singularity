package me.mrnavastar.singularity.common.networking;

import java.util.HashSet;

public class Settings {
    public boolean syncPlayerData = false;
    public boolean syncPlayerStats = false;
    public boolean syncPlayerAdvancements = false;

    public HashSet<String> nbtBlacklists = new HashSet<>();

    public Settings setDefault() {
        syncPlayerData = true;
        syncPlayerStats = true;
        syncPlayerAdvancements = true;

        nbtBlacklists.add("singularity.location");
        nbtBlacklists.add("singularity.spawn");
        return this;
    }
}
