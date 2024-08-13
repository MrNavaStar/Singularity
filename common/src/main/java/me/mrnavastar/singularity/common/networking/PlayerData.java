package me.mrnavastar.singularity.common.networking;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class PlayerData extends ServerData {
    private UUID player;
}
