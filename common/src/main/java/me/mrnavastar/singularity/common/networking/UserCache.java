package me.mrnavastar.singularity.common.networking;

import java.util.Date;
import java.util.UUID;

public record UserCache(UUID uuid, String name, Date expires) {
}
