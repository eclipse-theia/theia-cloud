package org.eclipse.theia.cloud.common.serialization;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

import java.util.List;

/**
 * <p>
 * A Jackson {@link BeanSerializerModifier} that modifies the serialization of sensitive data. It assigns the
 * {@link SensitiveDataSerializer} to fields annotated with {@link SensitiveData}. The serializer is assigned for
 * regular and null value serialization to prevent leaking information.
 * </p>
 * <p>
 * To use this serializer modifier, it must be registered with Jackson's
 * {@link com.fasterxml.jackson.databind.ObjectMapper ObjectMapper}.
 * </p>
 */
public class SensitiveDataBeanSerializerModifier extends BeanSerializerModifier {
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
            List<BeanPropertyWriter> beanProperties) {
        for (BeanPropertyWriter writer : beanProperties) {
            // Check if the field has the @SensitiveData annotation
            if (writer.getMember().getAnnotation(SensitiveData.class) != null) {
                writer.assignNullSerializer(new SensitiveDataSerializer(writer.getType()));
                writer.assignSerializer(new SensitiveDataSerializer(writer.getType()));
            }
        }
        return beanProperties;
    }
}
