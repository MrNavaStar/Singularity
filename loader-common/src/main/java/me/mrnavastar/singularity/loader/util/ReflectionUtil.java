package me.mrnavastar.singularity.loader.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectionUtil {

    private static final ConcurrentHashMap<String, Field> fieldCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();

    public static <T> T getFieldValue(Object instance, Class<?> clazz, String name, Class<T> returnType) {
        try {
            Field field = fieldCache.get(clazz.getName() + name);
            if (field == null) {
                field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                fieldCache.put(clazz.getName() + name, field);
            }
            return returnType.cast(field.get(instance));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getFieldValue(Object instance, String name, Class<T> returnType) {
        return getFieldValue(instance, instance.getClass(), name, returnType);
    }

    public static <T> T getParentFieldValue(Object instance, String name, Class<T> returnType) {
        return getFieldValue(instance, instance.getClass().getSuperclass(), name, returnType);
    }

    // Can set private and final fields
    public static void setFieldValue(Object instance, Class<?> clazz, String name, Object value) {
        try {
            Field field = fieldCache.get(clazz.getName() + name);
            if (field == null) {
                field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                fieldCache.put(clazz.getName() + name, field);
            }
            field.set(instance, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setFieldValue(Object instance, String name, Class<?> type, Object value) {
        setFieldValue(instance, instance.getClass(), name, value);
    }

    public static void setParentFieldValue(Object instance, String name, Class<?> type, Object value) {
        setFieldValue(instance, instance.getClass().getSuperclass(), name, value);
    }

    public static <T> T invokeMethod(Object instance, String name, Class<T> returnType, Object... args) {
        Class<?>[] classes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
        try {
            Method method = methodCache.get(instance.getClass().getName() + name + Arrays.toString(classes));
            if (method == null) {
                method = instance.getClass().getDeclaredMethod(name, classes);
                method.setAccessible(true);
                methodCache.put(instance.getClass().getName() + name, method);
            }
            if (returnType == null) return null;
            return returnType.cast(method.invoke(instance, args));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}