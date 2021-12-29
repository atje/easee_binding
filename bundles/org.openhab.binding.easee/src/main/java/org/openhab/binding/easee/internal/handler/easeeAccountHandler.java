/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.easee.internal.handler;

import static org.openhab.binding.easee.internal.easeeBindingConstants.*;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MediaType.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.Fields;
import org.openhab.binding.easee.internal.easeeAccountConfiguration;
import org.openhab.core.auth.client.oauth2.AccessTokenRefreshListener;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.auth.client.oauth2.OAuthClientService;
import org.openhab.core.auth.client.oauth2.OAuthFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link easeeAccountHandler} is responsible for communicating with the Easee API based on an account.
 *
 * @author Andreas Tjernsten - Initial contribution, based heavily on MyQ binding
 */
@NonNullByDefault
public class easeeAccountHandler extends BaseBridgeHandler implements AccessTokenRefreshListener {

    /*
     * Easee authentication API endpoints
     */
    private static final String LOGIN_BASE_URL = "https://api.easee.cloud/api";
    private static final String LOGIN_AUTHORIZE_URL = LOGIN_BASE_URL + "/accounts/login";
    private static final String REFRESH_TOKEN_URL = LOGIN_BASE_URL + "/accounts/refresh_token";

    private final Logger logger = LoggerFactory.getLogger(easeeAccountHandler.class);
    private final OAuthFactory oAuthFactory;

    private @Nullable Future<?> pollFuture;

    private @Nullable OAuthClientService oAuthService;
    private Integer pollingIntervall = 60;
    private HttpClient httpClient;
    private String username = "";
    private String password = "";

    public easeeAccountHandler(Bridge bridge, HttpClient httpClient, final OAuthFactory oAuthFactory) {
        super(bridge);
        this.httpClient = httpClient;
        this.oAuthFactory = oAuthFactory;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        try {
            easeeAccountConfiguration config = getConfigAs(easeeAccountConfiguration.class);
            pollingIntervall = config.pollingInterval;
            username = config.username;
            password = config.password;

            updateStatus(ThingStatus.UNKNOWN);
            login();
            startPoll();
        } catch (Exception e) {

        }
    }

    @Override
    public void dispose() {
        stopPoll();
        OAuthClientService oAuthService = this.oAuthService;
        if (oAuthService != null) {
            oAuthService.close();
        }
    }

    @Override
    public void onAccessTokenResponse(AccessTokenResponse tokenResponse) {
        logger.debug("Auth Token Refreshed, expires in {}", tokenResponse.getExpiresIn());
    }

    private synchronized void startPoll() {
        pollFuture = scheduler.scheduleWithFixedDelay(this::poll, 0, pollingIntervall, TimeUnit.SECONDS);
    }

    private synchronized void stopPoll() {
        if (pollFuture != null) {
            pollFuture.cancel(true);
        }
        pollFuture = null;
    }

    private void poll() {
        logger.debug("Polling");
    }

    /**
     * This attempts obtain a @AccessTokenResponse
     *
     * @return AccessTokenResponse token
     * @throws InterruptedException
     * @throws easeeCommunicationException
     * @throws easeeAuthenticationException
     */
    private AccessTokenResponse login()
            throws InterruptedException, easeeCommunicationException, easeeAuthenticationException {
        try {
            logger.debug("Connecting to Easee Cloud service");

            Fields fields = new Fields();
            fields.add("userName", username);
            fields.add("password", password);

            Request request = httpClient.newRequest(LOGIN_AUTHORIZE_URL).method(HttpMethod.POST)
                    .header(HttpHeader.ACCEPT, MediaType.APPLICATION_JSON)
                    .header(HttpHeader.CONTENT_TYPE, "application/*+json").content(new StringContentProvider(
                            "{\"userName\":\"" + username + "\",\"password\":\"" + password + "\"}", "utf-8"));

            ContentResponse response = request.send();

            logger.debug("httpClient ContentResponse:\n{}", response.getContentAsString());

            if (response == null) {
                throw new easeeCommunicationException("Could not load login page");
            }

            if (response.getStatus() != 200) {
                String str = "Status " + response.getStatus() + " Reason: " + response.getReason();
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, str);
                throw new easeeCommunicationException("Check Easee cloud credentials, could not load login page");
            }

            logger.debug("OK Response from Easee cloud");

            Gson gson = new Gson();
            AccessTokenResponse accessTokenResponse = gson.fromJson(response.getContentAsString(),
                    AccessTokenResponse.class);

            if (accessTokenResponse == null) {
                throw new easeeAuthenticationException("Could not parse token response");
            }

            getOAuthService().importAccessTokenResponse(accessTokenResponse);
            updateStatus(ThingStatus.ONLINE);

            return accessTokenResponse;
        } catch (Exception e) {
            throw new easeeCommunicationException(e.getMessage());
        }
    }

    private OAuthClientService getOAuthService() {
        OAuthClientService oAuthService = this.oAuthService;
        if (oAuthService == null || oAuthService.isClosed()) {
            oAuthService = oAuthFactory.createOAuthClientService(getThing().toString(), REFRESH_TOKEN_URL,
                    LOGIN_AUTHORIZE_URL, username, null, null, false);
            oAuthService.addAccessTokenRefreshListener(this);
            this.oAuthService = oAuthService;
        }
        return oAuthService;
    }

    /**
     * Exception for authenticated related errors
     */
    class easeeAuthenticationException extends Exception {
        private static final long serialVersionUID = 1L;

        public easeeAuthenticationException(String message) {
            super(message);
        }
    }

    /**
     * Generic exception for non authentication related errors when communicating with the Easee service.
     */
    class easeeCommunicationException extends IOException {
        private static final long serialVersionUID = 1L;

        public easeeCommunicationException(@Nullable String message) {
            super(message);
        }
    }
}
