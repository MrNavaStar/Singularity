package me.mrnavastar.singularity.common.networking;

import java.util.UUID;

public record Subscription(UUID uuid, String topic) {

    @Override
    public String toString() {
        return topic + ":" + uuid;
    }
}