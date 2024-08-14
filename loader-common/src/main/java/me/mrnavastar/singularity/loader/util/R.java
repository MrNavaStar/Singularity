package me.mrnavastar.singularity.loader.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class R {

    private final Object instance;

    public R(Object instance) {
        this.instance = instance;
    }

    public static R of(Object instance) {
        return new R(instance);
    }

    // Search super classes for field
    private Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        if (clazz == null) throw new NoSuchFieldException();

        Field field;
        try {
            field = clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            field = findField(clazz.getSuperclass(), name);
        }
        field.setAccessible(true);
        return field;
    }

    // Search super classes for methods
    private Method findMethod(Class<?> clazz, String name, Class<?>[] argTypes) throws NoSuchMethodException {
        if (clazz == null) throw new NoSuchMethodException();

        Method method;
        try {
            method = clazz.getDeclaredMethod(name, argTypes);
        } catch (NoSuchMethodException e) {
            method = findMethod(clazz.getSuperclass(), name, argTypes);
        }
        method.setAccessible(true);
        return method;
    }

    public <T> T get(String name, Class<T> type) {
        try {
            return type.cast(findField(instance.getClass(), name).get(instance));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public void set(String name, Object value) {
        try {
            findField(instance.getClass(), name).set(instance, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T call(String name, Class<T> returnType, Object... args) {
        try {
            Class<?>[] classes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
            Object returnVal = findMethod(instance.getClass(), name, classes).invoke(instance, args);
            if (returnVal == null || returnType == null) return null;
            return returnType.cast(returnVal);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public R call(String name, Object... args) {
        call(name, null, args);
        return this;
    }
}