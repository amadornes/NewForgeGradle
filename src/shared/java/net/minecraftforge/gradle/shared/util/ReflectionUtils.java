package net.minecraftforge.gradle.shared.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.UnaryOperator;

public class ReflectionUtils {

    public static <T> void alter(Object target, String name, UnaryOperator<T> operator) {
        try {
            Object prev = target;
            Field f = null;
            for (String n : name.split("\\.")) {
                f = findField(target.getClass(), n);
                if(f == null) throw new IllegalStateException("Could not find '" + name + "'");
                f.setAccessible(true);
                prev = target;
                target = f.get(target);
            }
            if(f == null) throw new IllegalStateException("Could not find '" + name + "'");
            f.set(prev, operator.apply((T) target));
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        while (clazz != Object.class) {
            for (Field f : clazz.getDeclaredFields()) {
                if (f.getName().equals(name)) {
                    return f;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /**
     * Invokes a method (can be private).
     */
    public static <T> T invoke(Object target, Class<?> type, String name, Object... args) {
        try {
            Method method = type.getDeclaredMethod(name);
            method.setAccessible(true);
            return (T) method.invoke(target, args);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
