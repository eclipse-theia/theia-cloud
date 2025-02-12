/********************************************************************************
 * Copyright (C) 2025 EclipseSource and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 ********************************************************************************/
package org.eclipse.theia.cloud.service.admin.appdefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.InternalServerErrorException;

import java.util.function.Consumer;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.service.ApplicationProperties;
import org.eclipse.theia.cloud.service.K8sUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;

/**
 * Unit tests for {@link AppDefinitionAdminResource}. Disable authorization via {@link TestSecurity} annotation as this
 * is a unit test of the resource itself. Thus, we do not want authentication interceptors to trigger when calling the
 * resource's methods.
 */
@QuarkusTest
@TestSecurity(authorizationEnabled = false)
public class AppDefinitionAdminResourceTests {

    private static final String APP_ID = "asdfghjkl";

    @InjectMock
    ApplicationProperties applicationProperties;

    @InjectMock
    K8sUtil k8sUtil;

    @Inject
    AppDefinitionAdminResource fixture;

    @BeforeEach
    public void setUp() {
        Mockito.when(applicationProperties.getAppId()).thenReturn(APP_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdate_validAppDefinition_returnsUpdatedDefinition() {
        final String appDefinitionName = "testApp";
        AppDefinitionUpdateRequest request = new AppDefinitionUpdateRequest(APP_ID);
        request.minInstances = 1;
        request.maxInstances = 5;

        Mockito.when(k8sUtil.hasAppDefinition(appDefinitionName)).thenReturn(true);
        Mockito.when(k8sUtil.editAppDefinition(anyString(), eq(appDefinitionName), any(Consumer.class)))
                .thenAnswer(invocation -> {
                    Consumer<AppDefinition> consumer = invocation.getArgument(2);
                    // Create an anonymous AppDefinition implementation using production AppDefinitionSpec.
                    AppDefinition appDef = Mockito.mock(AppDefinition.class);
                    AppDefinitionSpec appDefSpec = new AppDefinitionSpec();
                    Mockito.when(appDef.getSpec()).thenReturn(appDefSpec);
                    consumer.accept(appDef);
                    return appDef;
                });

        AppDefinition result = fixture.update(appDefinitionName, request);
        AppDefinitionSpec resultSpec = result.getSpec();
        assertEquals(1, resultSpec.getMinInstances());
        assertEquals(5, resultSpec.getMaxInstances());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdate_validAppDefinitionNullMaxInstances_returnsUpdatedDefinition() {
        final String appDefinitionName = "testApp";
        AppDefinitionUpdateRequest request = new AppDefinitionUpdateRequest(APP_ID);
        request.minInstances = 1;
        request.maxInstances = null;
        AppDefinitionSpec appDefSpec = new AppDefinitionSpec();
        appDefSpec.setMaxInstances(10);

        Mockito.when(k8sUtil.hasAppDefinition(appDefinitionName)).thenReturn(true);
        Mockito.when(k8sUtil.editAppDefinition(anyString(), eq(appDefinitionName), any(Consumer.class)))
                .thenAnswer(invocation -> {
                    Consumer<AppDefinition> consumer = invocation.getArgument(2);
                    // Create an anonymous AppDefinition implementation using production AppDefinitionSpec.
                    AppDefinition appDef = Mockito.mock(AppDefinition.class);
                    Mockito.when(appDef.getSpec()).thenReturn(appDefSpec);
                    consumer.accept(appDef);
                    return appDef;
                });

        AppDefinition result = fixture.update(appDefinitionName, request);
        AppDefinitionSpec resultSpec = result.getSpec();
        assertEquals(1, resultSpec.getMinInstances());
        // For a null parameter, the value should not be changed.
        assertEquals(10, resultSpec.getMaxInstances());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdate_validAppDefinitionNullMinInstances_returnsUpdatedDefinition() {
        final String appDefinitionName = "testApp";
        AppDefinitionUpdateRequest request = new AppDefinitionUpdateRequest(APP_ID);
        request.minInstances = null;
        request.maxInstances = 5;
        AppDefinitionSpec appDefSpec = new AppDefinitionSpec();
        appDefSpec.setMinInstances(1);

        Mockito.when(k8sUtil.hasAppDefinition(appDefinitionName)).thenReturn(true);
        Mockito.when(k8sUtil.editAppDefinition(anyString(), eq(appDefinitionName), any(Consumer.class)))
                .thenAnswer(invocation -> {
                    Consumer<AppDefinition> consumer = invocation.getArgument(2);
                    // Create an anonymous AppDefinition implementation using production AppDefinitionSpec.
                    AppDefinition appDef = Mockito.mock(AppDefinition.class);
                    Mockito.when(appDef.getSpec()).thenReturn(appDefSpec);
                    consumer.accept(appDef);
                    return appDef;
                });

        AppDefinition result = fixture.update(appDefinitionName, request);
        AppDefinitionSpec resultSpec = result.getSpec();
        // For a null parameter, the value should not be changed.
        assertEquals(1, resultSpec.getMinInstances());
        assertEquals(5, resultSpec.getMaxInstances());
    }

    @Test
    public void testUpdate_appDefinitionNotFound_throwsNotFoundException() {
        String appDefinitionName = "nonexistent";
        AppDefinitionUpdateRequest request = new AppDefinitionUpdateRequest(APP_ID);

        Mockito.when(k8sUtil.hasAppDefinition(appDefinitionName)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> {
            fixture.update(appDefinitionName, request);
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdate_editAppDefinitionThrows_internalServerErrorException() {
        String appDefinitionName = "testApp";
        AppDefinitionUpdateRequest request = new AppDefinitionUpdateRequest(APP_ID);
        request.minInstances = 2;
        request.maxInstances = 10;

        Mockito.when(k8sUtil.hasAppDefinition(appDefinitionName)).thenReturn(true);
        Mockito.when(k8sUtil.editAppDefinition(anyString(), eq(appDefinitionName), any(Consumer.class)))
                .thenThrow(new RuntimeException("Edit failure"));

        assertThrows(InternalServerErrorException.class, () -> {
            fixture.update(appDefinitionName, request);
        });
    }
}