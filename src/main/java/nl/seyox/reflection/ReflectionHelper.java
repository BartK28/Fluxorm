package nl.seyox.reflection;

import com.google.common.reflect.ClassPath;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ReflectionHelper {

    public static Set<Class<?>> findAllClassesUsingGoogleGuice() throws IOException {
        return ClassPath.from(ClassLoader.getSystemClassLoader())
                .getAllClasses()
                .stream()
                .map(clazz -> {
                    try {
                        return clazz.load();
                    } catch (NoClassDefFoundError e) {
                        // Handle the case where a class can't be loaded
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

}
