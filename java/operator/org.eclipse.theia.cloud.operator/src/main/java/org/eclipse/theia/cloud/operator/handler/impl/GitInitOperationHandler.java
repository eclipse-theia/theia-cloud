/********************************************************************************
 * Copyright (C) 2023 EclipseSource and others.
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
package org.eclipse.theia.cloud.operator.handler.impl;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.util.LogMessageUtil;
import org.eclipse.theia.cloud.operator.handler.InitOperationHandler;
import org.eclipse.theia.cloud.operator.handler.util.TheiaCloudPersistentVolumeUtil;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.KeyToPath;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.api.model.SecretVolumeSource;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.Deployment;

public class GitInitOperationHandler implements InitOperationHandler {

    protected static final String ETC_THEIA_CLOUD_SSH = "/etc/theia-cloud-ssh";
    protected static final String ID_THEIACLOUD = "id_theiacloud";
    protected static final String SSH_PRIVATEKEY = "ssh-privatekey";
    protected static final String SSH_KEY = "ssh-key";
    protected static final String PASSWORD = "password";
    protected static final String USERNAME = "username";
    protected static final String GIT_PROMPT1 = "GIT_PROMPT1";
    protected static final String GIT_PROMPT2 = "GIT_PROMPT2";
    protected static final String KUBERNETES_IO_SSH_AUTH = "kubernetes.io/ssh-auth";
    protected static final String KUBERNETES_IO_BASIC_AUTH = "kubernetes.io/basic-auth";
    protected static final String HTTPS = "https://";
    protected static final String HTTP = "http://";
    protected static final String IMAGE_ENV_KEY = "GIT_INIT_OPERATION_IMAGE";
    protected static final String DEFAULT_IMAGE = "theiacloud/theia-cloud-git-init:latest";
    protected static final String ID = "git";
    protected static final String INIT_CONTAINER_NAME = "git-init";

    private static final Logger LOGGER = LogManager.getLogger(GitInitOperationHandler.class);

    @Override
    public String operationId() {
	return ID;
    }

    @Override
    public void handleInitOperation(String correlationId, TheiaCloudClient client, Deployment deployment,
	    AppDefinition appDefinition, Session session, List<String> args) {

	if (args.size() < 2 || args.size() > 3) {
	    LOGGER.warn(LogMessageUtil.formatLogMessage(correlationId, MessageFormat.format(
		    "Git init expects 2-3 arguments (repository path, branch, (secret-name)). Passed arguments are: {0}",
		    args.stream().collect(Collectors.joining(",")))));
	    return;
	}

	Optional<String> storageName = AddedHandlerUtil.getStorageName(client, session, correlationId);
	if (storageName.isEmpty()) {
	    LOGGER.warn(LogMessageUtil.formatLogMessage(correlationId,
		    "Git init is only supported for non-ephemeral workspaces"));
	    return;
	}

	List<Container> initContainers = deployment.getSpec().getTemplate().getSpec().getInitContainers();
	List<Volume> volumes = deployment.getSpec().getTemplate().getSpec().getVolumes();

	Container gitInitContainer = new Container();
	initContainers.add(gitInitContainer);

	gitInitContainer.setName(INIT_CONTAINER_NAME);
	gitInitContainer.setImage(getImage());
	String repository = args.get(0);
	String branch = args.get(1);
	List<String> containerArgs = Arrays.asList(repository,
		TheiaCloudPersistentVolumeUtil.getMountPath(appDefinition.getSpec()), branch);
	gitInitContainer.setArgs(containerArgs);
	LOGGER.info(LogMessageUtil.formatLogMessage(correlationId, MessageFormat.format("Git init arguments are: {0}",
		containerArgs.stream().collect(Collectors.joining(",")))));

	SecurityContext securityContext = new SecurityContext();
	gitInitContainer.setSecurityContext(securityContext);

	securityContext.setRunAsUser(Long.valueOf(appDefinition.getSpec().getUid()));
	securityContext.setRunAsGroup(Long.valueOf(appDefinition.getSpec().getUid()));

	VolumeMount volumeMount = AddedHandlerUtil.createUserDataVolumeMount(appDefinition.getSpec());
	gitInitContainer.getVolumeMounts().add(volumeMount);

	Optional<String> secretName = args.size() == 3 ? Optional.of(args.get(2)) : Optional.empty();
	Optional<Secret> secret;
	if (secretName.isPresent()) {
	    Secret k8sSecret = client.kubernetes().secrets().withName(secretName.get()).get();
	    if (k8sSecret == null) {
		LOGGER.warn(LogMessageUtil.formatLogMessage(correlationId,
			MessageFormat.format("No secret with name {0} found.", secretName)));
		return;
	    }

	    String theiaCloudInit = k8sSecret.getMetadata().getLabels().get(THEIA_CLOUD_INIT_LABEL);
	    if (theiaCloudInit == null || !ID.equals(theiaCloudInit)) {
		LOGGER.warn(LogMessageUtil.formatLogMessage(correlationId, MessageFormat
			.format("Secret with name {0} is not configured to be used with Git init.", secretName)));
		return;
	    }

	    String theiaCloudUser = k8sSecret.getMetadata().getAnnotations().get(THEIA_CLOUD_USER_LABEL);
	    if (theiaCloudUser == null || !session.getSpec().getUser().equals(theiaCloudUser)) {
		LOGGER.warn(LogMessageUtil.formatLogMessage(correlationId,
			MessageFormat.format("Secret with name {0} is not configured to be used by user {1}.",
				secretName, session.getSpec().getUser())));
		return;
	    }
	    secret = Optional.of(k8sSecret);
	} else {
	    secret = Optional.empty();
	}

	if (isHTTP(repository)) {
	    if (!injectHTTPRepoCredentials(correlationId, secret, secretName, repository, gitInitContainer)) {
		// problem during injection, return early
		return;
	    }
	} else {
	    if (!injectSSHRepoCredentials(correlationId, secret, secretName, repository, gitInitContainer, volumes)) {
		// problem during injection, return early
		return;
	    }
	}

	// init container is added to the deployment at this point
	// any additional init code (e.g. injecting SSH Keys into the running IDE itself
	// may follow below or may be added by extending this handler)

    }

    protected boolean injectHTTPRepoCredentials(String correlationId, Optional<Secret> secret,
	    Optional<String> secretName, String repository, Container gitInitContainer) {
	/* get username/password from secret */
	boolean injectUsername = false;
	boolean injectPassword = false;

	if (secret.isPresent() && secretName.isPresent()) {
	    if (!KUBERNETES_IO_BASIC_AUTH.equals(secret.get().getType())) {
		LOGGER.warn(LogMessageUtil.formatLogMessage(correlationId, MessageFormat.format(
			"Secret with name {0} is not of type {1}.", secretName.get(), KUBERNETES_IO_BASIC_AUTH)));
		return false;
	    }

	    String[] split = repository.toLowerCase().split(Pattern.quote("://"), 2);
	    if (split.length != 2) {
		LOGGER.error(LogMessageUtil.formatLogMessage(correlationId, MessageFormat
			.format("Failed to check whether repository {0} contains any user information. ", repository)));
		return false;
	    }
	    String repositoryWithoutProtocol = split[1];
	    if (repositoryWithoutProtocol.contains("@")) {
		if (repositoryWithoutProtocol.split(Pattern.quote("@"))[0].contains(":")) {
		    /*
		     * username and password part of URL. keep injectUsername and injectPassword as
		     * false
		     */
		} else {
		    /* username part of url */
		    injectPassword = true;
		}
	    } else {
		injectUsername = true;
		injectPassword = true;
	    }
	}

	LOGGER.info(LogMessageUtil.formatLogMessage(correlationId,
		MessageFormat.format("Inject username: {0}; Inject password: {1}", injectUsername, injectPassword)));

	if (secretName.isPresent()) {
	    String nextEnv = GIT_PROMPT1;
	    if (injectUsername) {
		EnvVar envVar = new EnvVar();
		gitInitContainer.getEnv().add(envVar);
		envVar.setName(nextEnv);
		nextEnv = GIT_PROMPT2;

		EnvVarSource envVarSource = new EnvVarSource();
		envVar.setValueFrom(envVarSource);
		envVarSource.setSecretKeyRef(new SecretKeySelector(USERNAME, secretName.get(), false));
	    }
	    if (injectPassword) {
		EnvVar envVar = new EnvVar();
		gitInitContainer.getEnv().add(envVar);
		envVar.setName(nextEnv);

		EnvVarSource envVarSource = new EnvVarSource();
		envVar.setValueFrom(envVarSource);
		envVarSource.setSecretKeyRef(new SecretKeySelector(PASSWORD, secretName.get(), false));
	    }
	}

	return true;
    }

    protected boolean injectSSHRepoCredentials(String correlationId, Optional<Secret> secret,
	    Optional<String> secretName, String repository, Container gitInitContainer, List<Volume> volumes) {

	if (secret.isPresent() && secretName.isPresent()) {
	    if (!KUBERNETES_IO_SSH_AUTH.equals(secret.get().getType())) {
		LOGGER.warn(LogMessageUtil.formatLogMessage(correlationId, MessageFormat
			.format("Secret with name {0} is not of type {1}.", secretName.get(), KUBERNETES_IO_SSH_AUTH)));
		return false;
	    }

	    /* inject password */
	    EnvVar envVar = new EnvVar();
	    gitInitContainer.getEnv().add(envVar);
	    envVar.setName(GIT_PROMPT1);

	    EnvVarSource envVarSource = new EnvVarSource();
	    envVar.setValueFrom(envVarSource);
	    envVarSource.setSecretKeyRef(new SecretKeySelector(PASSWORD, secretName.get(), false));

	    /* inject ssh key */
	    Volume volume = new Volume();
	    volumes.add(volume);
	    volume.setName(SSH_KEY);
	    SecretVolumeSource secretVolumeSource = new SecretVolumeSource();
	    volume.setSecret(secretVolumeSource);
	    secretVolumeSource.setSecretName(secretName.get());
	    KeyToPath keyToPath = new KeyToPath();
	    secretVolumeSource.getItems().add(keyToPath);
	    keyToPath.setKey(SSH_PRIVATEKEY);
	    keyToPath.setPath(ID_THEIACLOUD);

	    VolumeMount volumeMount = new VolumeMount();
	    gitInitContainer.getVolumeMounts().add(volumeMount);
	    volumeMount.setName(SSH_KEY);
	    volumeMount.setMountPath(ETC_THEIA_CLOUD_SSH);
	    volumeMount.setReadOnly(true);
	}

	return true;
    }

    protected static boolean isHTTP(String repository) {
	String lowerCasedRepo = repository.toLowerCase(Locale.US);
	return (lowerCasedRepo.startsWith(HTTP) || lowerCasedRepo.startsWith(HTTPS));
    }

    protected String getImage() {
	String image = System.getenv(IMAGE_ENV_KEY);
	if (image == null || image.isBlank()) {
	    return DEFAULT_IMAGE;
	}
	return image;
    }

}
