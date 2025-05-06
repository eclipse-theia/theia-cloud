package org.eclipse.theia.cloud.common.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * A Jackson {@link JsonSerializer} that redacts sensitive data. It suppresses the serialization of sensitive data by
 * writing a predefined value instead. It must be used as the serializer for normal and null values of sensitive data.
 */
public class SensitiveDataSerializer extends JsonSerializer<Object> {

    public static String REDACTED_STRING = "***";
    public static int REDACTED_NUMBER = 0;
    public static boolean REDACTED_BOOLEAN = false;

    protected JavaType propertyType;

    public SensitiveDataSerializer(JavaType propertyType) {
        this.propertyType = propertyType;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (propertyType.isTypeOrSubTypeOf(String.class)) {
            gen.writeString(REDACTED_STRING);
        } else if (propertyType.isContainerType()) {
            if (propertyType.isMapLikeType()) {
                gen.writeStartObject();
                gen.writeEndObject();
            } else if (propertyType.isArrayType() || propertyType.isCollectionLikeType()) {
                gen.writeStartArray();
                gen.writeEndArray();
            } else {
                gen.writeNull();
            }
            // Check value.getClass in case of primitive number types (e.g. int, long, double, etc.)
            // This is necessary because the propertyType for these is not a subtype of Number but the class of the
            // boxed value is.
        } else if (propertyType.isTypeOrSubTypeOf(Number.class)
                || (value != null && Number.class.isAssignableFrom(value.getClass()))) {
            gen.writeNumber(REDACTED_NUMBER);
            // Check value.getClass in case of primitive boolean type
        } else if (propertyType.isTypeOrSubTypeOf(Boolean.class)
                || (value != null && Boolean.class.isAssignableFrom(value.getClass()))) {
            gen.writeBoolean(REDACTED_BOOLEAN);
        } else {
            gen.writeNull();
        }
    }
}
