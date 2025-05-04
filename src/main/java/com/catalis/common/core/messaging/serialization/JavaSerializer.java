package com.catalis.common.core.messaging.serialization;

import org.springframework.stereotype.Component;

import java.io.*;

/**
 * Implementation of {@link MessageSerializer} that uses Java serialization.
 * <p>
 * This serializer can only handle objects that implement {@link Serializable}.
 */
@Component
public class JavaSerializer implements MessageSerializer {

    @Override
    public byte[] serialize(Object object) throws SerializationException {
        if (!(object instanceof Serializable)) {
            throw new SerializationException("Object must implement Serializable");
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Failed to serialize object to bytes", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> type) throws SerializationException {
        if (!Serializable.class.isAssignableFrom(type)) {
            throw new SerializationException("Type must implement Serializable");
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            Object obj = ois.readObject();
            if (!type.isInstance(obj)) {
                throw new SerializationException("Deserialized object is not of the expected type");
            }
            return (T) obj;
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Failed to deserialize bytes to object", e);
        }
    }

    @Override
    public String getContentType() {
        return "application/java-serialized-object";
    }

    @Override
    public SerializationFormat getFormat() {
        return SerializationFormat.JAVA;
    }

    @Override
    public boolean canHandle(Class<?> type) {
        return Serializable.class.isAssignableFrom(type);
    }
}
