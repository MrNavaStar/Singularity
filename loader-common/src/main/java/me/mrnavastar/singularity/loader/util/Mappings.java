package me.mrnavastar.singularity.loader.util;

import lombok.Setter;

public class Mappings {

    @Setter
    private static boolean dev = true;

    public static String of(String s1, String s2) {
        return dev ? s1 : s2;
    }
}
