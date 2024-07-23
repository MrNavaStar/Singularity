package me.mrnavastar.singularity.common.networking;

import java.util.HashSet;

public class Settings {
    public boolean syncPlayerData = true;
    public boolean syncPlayerStats = true;
    public boolean syncPlayerAdvancements = true;

    public HashSet<String> nbtBlacklists = new HashSet<>();

    public Settings() {
        nbtBlacklists.add("singularity.blacklist.location");
        nbtBlacklists.add("singularity.blacklist.spawn");
    }
}
