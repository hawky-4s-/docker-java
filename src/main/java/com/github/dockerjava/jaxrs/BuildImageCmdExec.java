package com.github.dockerjava.jaxrs;

import static javax.ws.rs.client.Entity.entity;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.model.AuthConfigurations;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.core.async.JsonStreamProcessor;
import com.github.dockerjava.jaxrs.async.AbstractCallbackNotifier;
import com.github.dockerjava.jaxrs.async.POSTCallbackNotifier;

public class BuildImageCmdExec extends AbstrAsyncDockerCmdExec<BuildImageCmd, BuildResponseItem> implements
        BuildImageCmd.Exec {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildImageCmdExec.class);

    public BuildImageCmdExec(WebTarget baseResource) {
        super(baseResource);
    }

    private Invocation.Builder resourceWithOptionalAuthConfig(BuildImageCmd command, Invocation.Builder request) {
        AuthConfigurations authConfigs = command.getBuildAuthConfigs();
        if (authConfigs != null) {
            request = request.header("X-Registry-Config", registryConfigs(authConfigs));
        }
        return request;
    }

    @Override
    protected AbstractCallbackNotifier<BuildResponseItem> callbackNotifier(BuildImageCmd command,
            ResultCallback<BuildResponseItem> resultCallback) {

        WebTarget webTarget = getBaseResource().path("/build");
        String dockerFilePath = command.getPathToDockerfile();

        if (dockerFilePath != null && command.getRemote() == null && !"Dockerfile".equals(dockerFilePath)) {
            webTarget = webTarget.queryParam("dockerfile", dockerFilePath);
        }
        if (command.getTag() != null) {
            webTarget = webTarget.queryParam("t", command.getTag());
        }
        if (command.getRemote() != null) {
            webTarget = webTarget.queryParam("remote", command.getRemote().toString());
        }
        if (command.isQuiet()) {
            webTarget = webTarget.queryParam("q", "true");
        }
        if (command.hasNoCacheEnabled()) {
            webTarget = webTarget.queryParam("nocache", "true");
        }
        if (command.hasPullEnabled()) {
            webTarget = webTarget.queryParam("pull", "true");
        }
        if (!command.hasRemoveEnabled()) {
            webTarget = webTarget.queryParam("rm", "false");
        }
        if (command.isForcerm()) {
            webTarget = webTarget.queryParam("forcerm", "true");
        }
        if (command.getMemory() != null) {
            webTarget = webTarget.queryParam("memory", command.getMemory());
        }
        if (command.getMemswap() != null) {
            webTarget = webTarget.queryParam("memswap", command.getMemswap());
        }
        if (command.getCpushares() != null) {
            webTarget = webTarget.queryParam("cpushares", command.getCpushares());
        }
        if (command.getCpusetcpus() != null) {
            webTarget = webTarget.queryParam("cpusetcpus", command.getCpusetcpus());
        }

        webTarget.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);
        webTarget.property(ClientProperties.CHUNKED_ENCODING_SIZE, 1024 * 1024);

        LOGGER.trace("POST: {}", webTarget);

        return new POSTCallbackNotifier<>(new JsonStreamProcessor<>(BuildResponseItem.class), resultCallback,
                resourceWithOptionalAuthConfig(command, webTarget.request()).accept(MediaType.TEXT_PLAIN), entity(
                        command.getTarInputStream(), "application/tar"));
    }
}
