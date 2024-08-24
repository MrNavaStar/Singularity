package me.mrnavastar.singularity.loader.util;

import lombok.Setter;

import java.util.function.Function;

public class Mappings {

    @Setter
    private static boolean dev = true;
    @Setter
    private static Function<String, String> mapper = null;

    public static String of(String s) {
        if (dev || mapper == null) return s;
        return mapper.apply(s);
    }
}
