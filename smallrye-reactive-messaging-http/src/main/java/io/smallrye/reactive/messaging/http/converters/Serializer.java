package io.smallrye.reactive.messaging.http.converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.smallrye.reactive.messaging.helpers.ClassUtils;
import io.vertx.mutiny.core.buffer.Buffer;

public abstract class Serializer<I> implements Converter<I, Buffer> {

    private static final List<Serializer<?>> CONVERTERS = new ArrayList<>();

    static {
        CONVERTERS.add(new StringSerializer());
        CONVERTERS.add(new BufferSerializer());
        CONVERTERS.add(new ByteArraySerializer());
        CONVERTERS.add(new ByteBufferSerializer());
        CONVERTERS.add(new JsonArraySerializer());
        CONVERTERS.add(new JsonObjectSerializer());
        //noinspection StaticInitializerReferencesSubClass
        CONVERTERS.add(new CloudEventSerializer());
    }

    @SuppressWarnings("unchecked")
    public static synchronized <I> Serializer<I> lookup(Object payload, String converterClassName) {
        Objects.requireNonNull(payload, "Payload must not be null");
        Optional<Serializer<?>> any = CONVERTERS.stream().filter(s -> ClassUtils
                .isAssignable(payload.getClass(), s.input()))
                .findAny();
        if (any.isPresent()) {
            return (Serializer<I>) any.get();
        }

        if (converterClassName != null) {
            Serializer<I> serializer = instantiate(converterClassName);
            CONVERTERS.add(serializer);
            return serializer;
        }

        throw new IllegalArgumentException(
                "Unable to find a serializer for type: " + payload.getClass() + ", supported types are: "
                        + CONVERTERS.stream().map(s -> s.input().getName()).collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    private static <I> Serializer<I> instantiate(String className) {
        try {
            Class<Serializer<I>> clazz = (Class<Serializer<I>>) Serializer.class.getClassLoader().loadClass(className);
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to load the class " + className + " or unable to instantiate it");
        }
    }

    @Override
    public Class<? extends Buffer> output() {
        return Buffer.class;
    }
}
