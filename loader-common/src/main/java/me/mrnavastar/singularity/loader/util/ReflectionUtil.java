package me.mrnavastar.singularity.loader.util;

import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectionUtil {

    private static final ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();

    @SneakyThrows
    public static <T> T invokePrivateMethod(Object instance, String name, Class<T> returnType, Object... args) {
        Method method = methodCache.get(instance.getClass().getName() + name);
        if (method == null) {
            method = instance.getClass().getMethod(name);
            method.setAccessible(true);
            methodCache.put(instance.getClass().getName() + name, method);
        }
        return returnType.cast(method.invoke(instance, args));
    }
}