package me.mrnavastar.singularity.common.networking;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class UserCache {
    private final UUID uuid;
    private final String name;
    private final Date expires;
}
