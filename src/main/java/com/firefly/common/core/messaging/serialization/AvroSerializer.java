package com.firefly.common.core.messaging.serialization;

import org.apache.avro.Schema;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Implementation of {@link MessageSerializer} that uses Apache Avro for serialization.
 * <p>
 * This serializer can only handle Avro-generated classes that extend {@link SpecificRecordBase}.
 */
@Component
public class AvroSerializer implements MessageSerializer {

    @Override
    public byte[] serialize(Object object) throws SerializationException {
        if (!(object instanceof SpecificRecordBase)) {
            throw new SerializationException("Object must be a SpecificRecordBase instance");
        }

        SpecificRecordBase record = (SpecificRecordBase) object;

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Schema schema = record.getSchema();
            DatumWriter<SpecificRecordBase> datumWriter = new SpecificDatumWriter<>(schema);
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);

            datumWriter.write(record, encoder);
            encoder.flush();

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Failed to serialize Avro to bytes", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> type) throws SerializationException {
        if (!SpecificRecordBase.class.isAssignableFrom(type)) {
            throw new SerializationException("Type must be a SpecificRecordBase class");
        }

        try {
            // Create an instance of the class to get the schema
            SpecificRecordBase record = (SpecificRecordBase) type.getDeclaredConstructor().newInstance();
            Schema schema = record.getSchema();

            DatumReader<SpecificRecordBase> datumReader = new SpecificDatumReader<>(schema);
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(bytes, null);

            return (T) datumReader.read(null, decoder);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize bytes to Avro", e);
        }
    }

    @Override
    public String getContentType() {
        return "application/avro";
    }

    @Override
    public SerializationFormat getFormat() {
        return SerializationFormat.AVRO;
    }

    @Override
    public boolean canHandle(Class<?> type) {
        return SpecificRecordBase.class.isAssignableFrom(type);
    }
}
