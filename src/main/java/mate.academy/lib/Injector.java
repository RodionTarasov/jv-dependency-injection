package mate.academy.lib;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import mate.academy.service.FileReaderService;
import mate.academy.service.ProductParser;
import mate.academy.service.ProductService;
import mate.academy.service.impl.FileReaderServiceImpl;
import mate.academy.service.impl.ProductParserImpl;
import mate.academy.service.impl.ProductServiceImpl;

public class Injector {
    private static final Injector injector = new Injector();
    private final Map<Class<?>, Class<?>> interfaceImplementation = new HashMap<>();
    private final Map<Class<?>, Object> instances = new HashMap<>();

    private Injector() {
        interfaceImplementation.put(FileReaderService.class, FileReaderServiceImpl.class);
        interfaceImplementation.put(ProductParser.class, ProductParserImpl.class);
        interfaceImplementation.put(ProductService.class, ProductServiceImpl.class);
    }

    public static Injector getInjector() {
        return injector;
    }

    public Object getInstance(Class<?> type) {
        Class<?> clazz = findImplementation(type);
        checkOfComponent(clazz);
        if (clazz.isInterface()) {
            return instances.get(clazz);
        }
        Object instance = createNewInstance(clazz);
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Inject.class)) {
                Object fieldInstance = getInstance(field.getType());
                field.setAccessible(true);
                try {
                    field.set(instance, fieldInstance);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        instances.put(clazz, instance);
        return instance;
    }

    private Object createNewInstance(Class<?> clazz) {
        if (instances.containsKey(clazz)) {
            return instances.get(clazz);
        }
        try {
            Constructor<?> constructor = clazz.getConstructor();
            Object instance = constructor.newInstance();
            instances.put(clazz, instance);
            return instance;
        } catch (NoSuchMethodException | InvocationTargetException
                 | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("can't create a new instance of" + clazz.getName(), e);
        }
    }

    private Class<?> findImplementation(Class<?> type) {
        if (type.isInterface()) {
            Class<?> impl = interfaceImplementation.get(type);
            if (impl == null) {
                throw new RuntimeException("No implementation found for " + type.getName());
            }
            return impl;
        }
        return type;
    }

    private void checkOfComponent(Class<?> interfaceClazz) {
        if (!interfaceClazz.isAnnotationPresent(Component.class)) {
            try {
                throw new ReflectiveOperationException("Class" + interfaceClazz.getName()
                        + "is not marked with annotation @Component");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
