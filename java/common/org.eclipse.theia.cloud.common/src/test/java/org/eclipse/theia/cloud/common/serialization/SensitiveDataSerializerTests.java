package org.eclipse.theia.cloud.common.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link SensitiveDataSerializer}.
 */
class SensitiveDataSerializerTest {

    private JsonGenerator jsonGenerator;
    private SerializerProvider serializerProvider;
    private TypeFactory typeFactory;

    @BeforeEach
    void setUp() {
        jsonGenerator = mock(JsonGenerator.class);
        serializerProvider = mock(SerializerProvider.class);
        typeFactory = TypeFactory.defaultInstance();
    }

    @Test
    void serialize_StringType() throws IOException {
        JavaType stringType = typeFactory.constructType(String.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(stringType);

        serializer.serialize("test", jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeString(SensitiveDataSerializer.REDACTED_STRING);
    }

    @Test
    void serialize_StringType_Null() throws IOException {
        JavaType stringType = typeFactory.constructType(String.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(stringType);

        serializer.serialize(null, jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeString(SensitiveDataSerializer.REDACTED_STRING);
    }

    @Test
    void serialize_MapType() throws IOException {
        JavaType mapType = typeFactory.constructMapType(HashMap.class, String.class, Object.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(mapType);

        serializer.serialize(new HashMap<>(), jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeStartObject();
        verify(jsonGenerator).writeEndObject();
    }

    @Test
    void serialize_MapType_Null() throws IOException {
        JavaType mapType = typeFactory.constructMapType(HashMap.class, String.class, Object.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(mapType);

        serializer.serialize(null, jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeStartObject();
        verify(jsonGenerator).writeEndObject();
    }

    @Test
    void serialize_ArrayType() throws IOException {
        JavaType arrayType = typeFactory.constructArrayType(String.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(arrayType);

        serializer.serialize(new String[] { "test" }, jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeStartArray();
        verify(jsonGenerator).writeEndArray();
    }

    @Test
    void serialize_ArrayType_Null() throws IOException {
        JavaType arrayType = typeFactory.constructArrayType(String.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(arrayType);

        serializer.serialize(null, jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeStartArray();
        verify(jsonGenerator).writeEndArray();
    }

    @Test
    void serialize_NumberType() throws IOException {
        JavaType numberType = typeFactory.constructType(Integer.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(numberType);

        serializer.serialize(123, jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeNumber(SensitiveDataSerializer.REDACTED_NUMBER);
    }

    @Test
    void serialize_NumberType_Null() throws IOException {
        JavaType numberType = typeFactory.constructType(Integer.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(numberType);

        serializer.serialize(null, jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeNumber(SensitiveDataSerializer.REDACTED_NUMBER);
    }

    @Test
    void serialize_PrimitiveIntType() throws IOException {
        JavaType intType = typeFactory.constructType(int.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(intType);

        serializer.serialize(123, jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeNumber(SensitiveDataSerializer.REDACTED_NUMBER);
    }

    @Test
    void serialize_PrimitiveLongType() throws IOException {
        JavaType longType = typeFactory.constructType(long.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(longType);

        serializer.serialize(123L, jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeNumber(SensitiveDataSerializer.REDACTED_NUMBER);
    }

    @Test
    void serialize_PrimitiveFloatType() throws IOException {
        JavaType floatType = typeFactory.constructType(float.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(floatType);

        serializer.serialize(123.45f, jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeNumber(SensitiveDataSerializer.REDACTED_NUMBER);
    }

    @Test
    void serialize_PrimitiveDoubleType() throws IOException {
        JavaType doubleType = typeFactory.constructType(double.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(doubleType);

        serializer.serialize(123.45, jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeNumber(SensitiveDataSerializer.REDACTED_NUMBER);
    }

    @Test
    void serialize_BooleanType() throws IOException {
        JavaType booleanType = typeFactory.constructType(Boolean.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(booleanType);

        serializer.serialize(true, jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeBoolean(SensitiveDataSerializer.REDACTED_BOOLEAN);
    }

    @Test
    void serialize_BooleanType_Null() throws IOException {
        JavaType booleanType = typeFactory.constructType(Boolean.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(booleanType);

        serializer.serialize(null, jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeBoolean(SensitiveDataSerializer.REDACTED_BOOLEAN);
    }

    @Test
    void serialize_PrimitiveBooleanType() throws IOException {
        JavaType booleanType = typeFactory.constructType(boolean.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(booleanType);

        serializer.serialize(true, jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeBoolean(SensitiveDataSerializer.REDACTED_BOOLEAN);
    }

    @Test
    void serialize_UnsupportedType() throws IOException {
        JavaType unsupportedType = typeFactory.constructType(Object.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(unsupportedType);

        serializer.serialize(new Object(), jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeNull();
    }

    @Test
    void serialize_UnsupportedType_Null() throws IOException {
        JavaType unsupportedType = typeFactory.constructType(Object.class);
        SensitiveDataSerializer serializer = new SensitiveDataSerializer(unsupportedType);

        serializer.serialize(null, jsonGenerator, serializerProvider);
        verify(jsonGenerator).writeNull();
    }
}