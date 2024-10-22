package org.eclipse.theia.cloud.service.serialization;

import jakarta.inject.Singleton;

import org.eclipse.theia.cloud.common.serialization.SensitiveDataBeanSerializerModifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.quarkus.jackson.ObjectMapperCustomizer;

/**
 * Registers the {@link SensitiveDataBeanSerializerModifier} in the Jackson ObjectMapper used by Quarkus to respect the
 * {@link org.eclipse.theia.cloud.common.serialization.SensitiveData SensitiveData} annotation.
 */
@Singleton
public class SensitiveDataCustomSerializerRegistration implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        SimpleModule module = new SimpleModule();
        module.setSerializerModifier(new SensitiveDataBeanSerializerModifier());
        objectMapper.registerModule(module);
    }
}
