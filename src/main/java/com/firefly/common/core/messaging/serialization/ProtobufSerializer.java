package com.firefly.common.core.messaging.serialization;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Implementation of {@link MessageSerializer} that uses Google Protocol Buffers for serialization.
 * <p>
 * This serializer can only handle Protobuf-generated classes that extend {@link Message}.
 */
@Component
public class ProtobufSerializer implements MessageSerializer {
    
    @Override
    public byte[] serialize(Object object) throws SerializationException {
        if (!(object instanceof Message)) {
            throw new SerializationException("Object must be a Protobuf-generated class");
        }
        
        Message message = (Message) object;
        return message.toByteArray();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> type) throws SerializationException {
        if (!Message.class.isAssignableFrom(type)) {
            throw new SerializationException("Type must be a Protobuf-generated class");
        }
        
        try {
            // Protobuf messages have a parseFrom method
            Method parseFrom = type.getMethod("parseFrom", byte[].class);
            return (T) parseFrom.invoke(null, bytes);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize Protobuf to object", e);
        }
    }
    
    @Override
    public String getContentType() {
        return "application/x-protobuf";
    }
    
    @Override
    public SerializationFormat getFormat() {
        return SerializationFormat.PROTOBUF;
    }
    
    @Override
    public boolean canHandle(Class<?> type) {
        return Message.class.isAssignableFrom(type);
    }
    
    /**
     * Converts a Protobuf message to JSON.
     *
     * @param message the Protobuf message
     * @return the JSON string
     * @throws SerializationException if conversion fails
     */
    public String toJson(Message message) throws SerializationException {
        try {
            return JsonFormat.printer().print(message);
        } catch (InvalidProtocolBufferException e) {
            throw new SerializationException("Failed to convert Protobuf to JSON", e);
        }
    }
    
    /**
     * Parses JSON into a Protobuf message.
     *
     * @param json the JSON string
     * @param builder the Protobuf message builder
     * @throws SerializationException if parsing fails
     */
    public void fromJson(String json, Message.Builder builder) throws SerializationException {
        try {
            JsonFormat.parser().merge(json, builder);
        } catch (InvalidProtocolBufferException e) {
            throw new SerializationException("Failed to parse JSON to Protobuf", e);
        }
    }
}
