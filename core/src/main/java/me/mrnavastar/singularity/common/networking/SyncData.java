package me.mrnavastar.singularity.common.networking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class SyncData {

    @Getter
    private UUID id;
    @Getter
    private String name;
    private final HashMap<String, byte[]> map = new HashMap<>();

    public void put(String key, byte[] data) {
        map.put(key, data);
    }

    public byte[] get(String key) {
        return map.get(key);
    }
}