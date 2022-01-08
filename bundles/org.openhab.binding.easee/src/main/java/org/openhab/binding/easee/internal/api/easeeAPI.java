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
package org.openhab.binding.easee.internal.api;

import static org.openhab.binding.easee.internal.api.easeeApiConstants.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.easee.internal.dto.chargerDTO;
import org.openhab.binding.easee.internal.dto.chargerStateDTO;
import org.openhab.core.auth.client.oauth2.AccessTokenRefreshListener;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.auth.client.oauth2.OAuthClientService;
import org.openhab.core.auth.client.oauth2.OAuthFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * The {@link easeeAPI} handles all Easee Cloud communication, including automatic refresh of authentication
 * security token.
 *
 * @author Andreas Tjernsten - Initial contribution
 */
@NonNullByDefault
public class easeeAPI implements AccessTokenRefreshListener {
    private final Logger logger = LoggerFactory.getLogger(easeeAPI.class);

    private final OAuthFactory oAuthFactory;
    private final HttpClient httpClient;
    private final String uniqueID;
    private final String username;
    private final String password;
    private final Gson gson = new Gson();

    private @Nullable OAuthClientService oAuthService;

    /**
     * Create {@link easeeAPI} instance
     * 
     * @param httpClient HttpClient to use for could access
     * @param oAuthFactory OAuthFactory for creation of OAuthClientService (this is done when {@link authenticateUser}
     *            is called)
     * @param uniqueID unique ID for this instance
     * @param username Easee cloud username
     * @param password Easee cloud password
     */
    public easeeAPI(HttpClient httpClient, OAuthFactory oAuthFactory, String uniqueID, String username,
            String password) {
        this.httpClient = httpClient;
        this.oAuthFactory = oAuthFactory;
        this.uniqueID = uniqueID;
        this.username = username;
        this.password = password;
    }

    @Override
    public void onAccessTokenResponse(AccessTokenResponse tokenResponse) {
        logger.debug("Easee Could Auth Token Refreshed for {}, expires in {}", uniqueID, tokenResponse.getExpiresIn());
    }

    /**
     * {@link authenticateUser} authenticates user towards Easee cloud using credentials provided upon easeeAPI instance
     * creation.
     * Also creates a OAuthClientService for handling authentication token refresh.
     * Throws an exception if any part of authentication fails.
     * 
     * @throws easeeAuthenticationException
     * @throws easeeCommunicationException
     */
    public void authenticateUser() throws easeeAuthenticationException, easeeCommunicationException {
        try {
            logger.debug("Authenticating Easee cloud user '{}' using provided password", username);

            Request request = httpClient.newRequest(LOGIN_AUTHORIZE_URL).method(HttpMethod.POST)
                    .header(HttpHeader.ACCEPT, MediaType.APPLICATION_JSON)
                    .header(HttpHeader.CONTENT_TYPE, "application/*+json").content(new StringContentProvider(
                            "{\"userName\":\"" + username + "\",\"password\":\"" + password + "\"}", "utf-8"));

            ContentResponse response = request.send();

            if (response == null) {
                throw new easeeCommunicationException("null reponse from authentication request");
            }

            if (response.getStatus() != HttpStatus.OK_200) {
                logger.debug("Status ({}), Reason '{}'", response.getStatus(), response.getReason());
                throw new easeeCommunicationException(
                        "Check Easee cloud credentials, could not authenticate user. Set DEBUG level to see full response");
            }

            logger.debug("Cloud authentication OK");

            AccessTokenResponse accessTokenResponse = gson.fromJson(response.getContentAsString(),
                    AccessTokenResponse.class);

            if (accessTokenResponse == null) {
                logger.trace("Content response: {}", response.getContentAsString());
                throw new easeeAuthenticationException(
                        "Could not parse token response from server. Set TRACE level to see full response");
            }

            if (oAuthService == null || oAuthService.isClosed()) {
                logger.debug("Creating oAuthService instance");
                oAuthService = oAuthFactory.createOAuthClientService(uniqueID, REFRESH_TOKEN_URL, LOGIN_AUTHORIZE_URL,
                        username, null, null, false);
            }

            accessTokenResponse.setCreatedOn(LocalDateTime.now());
            oAuthService.importAccessTokenResponse(accessTokenResponse);

        } catch (Exception e) {
            throw new easeeCommunicationException(e.getMessage());
        }
    }

    /**
     * {@link getChargers} Retrieves chargers from Easee cloud
     * 
     * @return List<chargerDTO>
     * @throws easeeCommunicationException if JSON reponse cannot be converted to chargerDTO object
     */
    public List<chargerDTO> getChargers() {
        List<chargerDTO> chargers = null;

        logger.debug("Retrieving chargers from Easee cloud");

        try {
            AccessTokenResponse accessTokenResponse = getAndCheckAccessTokenResponse();
            if (accessTokenResponse == null) {
                return new ArrayList<chargerDTO>();
            }

            Request request = httpClient.newRequest(GET_CHARGERS_URL).method(HttpMethod.GET)
                    .header(HttpHeader.ACCEPT, MediaType.APPLICATION_JSON)
                    .header("Authorization", authTokenHeader(accessTokenResponse));

            ContentResponse response = getCheckedResponse(request);
            if (response == null) {
                return new ArrayList<chargerDTO>();
            }

            Type chargerListType = new TypeToken<ArrayList<chargerDTO>>() {
            }.getType();
            chargers = gson.fromJson(response.getContentAsString(), chargerListType);

            if (chargers == null) {
                throw new easeeCommunicationException("null chargers list when parsing getChargers response");
            }
            return chargers;
        } catch (Exception e) {
            logger.warn("Exception in getChargers(): {}", e.getMessage());
            return new ArrayList<chargerDTO>();
        }
    }

    /**
     * {@link getChargerState} gets charger state from Easee cloud
     * 
     * @param chargerId the charger ID to fetch state from
     * @return chargerStateDTO state object or null if there was an error
     * @throws easeeCommunicationException
     */
    @Nullable
    public chargerStateDTO getChargerState(String chargerId) {
        chargerStateDTO chargerState = null;

        logger.debug("Retrieving charger state from Easee cloud");

        try {
            AccessTokenResponse accessTokenResponse = getAndCheckAccessTokenResponse();
            if (accessTokenResponse == null) {
                return null;
            }

            final String URL = GET_CHARGERS_URL + "/" + chargerId + "/state";
            Request request = httpClient.newRequest(URL).method(HttpMethod.GET)
                    .header(HttpHeader.ACCEPT, MediaType.APPLICATION_JSON)
                    .header("Authorization", authTokenHeader(accessTokenResponse));

            ContentResponse response = getCheckedResponse(request);
            if (response == null) {
                return null;
            }

            chargerState = gson.fromJson(response.getContentAsString(), chargerStateDTO.class);

            if (chargerState == null) {
                throw new easeeCommunicationException("null charger state list");
            }
            return chargerState;
        } catch (Exception e) {
            logger.warn("Exception in getChargerState: {}", e.getMessage());
            return null;
        }
    }

    /**
     * {@link getCheckedResponse} makes request towards cloud API and checks that the response was ok
     * 
     * @param request to be made
     * @return response from server, or null if there was a problem with the request
     */
    @Nullable
    private ContentResponse getCheckedResponse(Request request) {
        try {
            if (request == null) {
                logger.error("null request in getCheckedResponse()");
                return null;
            }

            ContentResponse response = request.send();

            if (response != null) {
                if (response.getStatus() != HttpStatus.OK_200) {
                    logger.warn("Status ({}), Reason '{}'", response.getStatus(), response.getReason());
                    return null;
                }
                logger.trace("Request: {}\nResponse: {}", request.toString(), response.getContentAsString());
            }

            return response;

        } catch (Exception e) {
            logger.warn("Failed request to cloud, exception {}", e.getMessage());
            return null;

        }
    }

    /**
     * {@link getAndCheckAccessTokenResponse} gets AccessTokenResponse from oAuthService, or null if there was a problem
     * 
     * @return AccessTokenResponse
     */
    @Nullable
    private AccessTokenResponse getAndCheckAccessTokenResponse() {
        AccessTokenResponse accessTokenResponse = null;

        try {
            OAuthClientService oAuthService = this.oAuthService;

            if (oAuthService == null) {
                logger.error("No oAuthService for Easee account, null");
                return null;
            }

            accessTokenResponse = oAuthService.getAccessTokenResponse();

            if (accessTokenResponse == null) {
                logger.error("Failed to retrieve accessToken from OAuthService for Easee account, null");
                return null;
            }

            return accessTokenResponse;

        } catch (Exception e) {
            logger.error("Failed to get accessTokenResponse, exception {}", e.getMessage());
            return null;
        }
    }

    public void close() {
        if (oAuthService != null) {
            oAuthService.close();
        }
    }

    private String authTokenHeader(AccessTokenResponse tokenResponse) {
        return tokenResponse.getTokenType() + " " + tokenResponse.getAccessToken();
    }

    /**
     * {@link easeeAuthenticationException} for Easee authentication related errors
     */
    class easeeAuthenticationException extends Exception {
        private static final long serialVersionUID = 1L;

        public easeeAuthenticationException(String message) {
            super(message);
        }
    }

    /**
     * {@link easeeCommunicationException} for other non-recoverable errors when communicating with the Easee service.
     */
    class easeeCommunicationException extends IOException {
        private static final long serialVersionUID = 1L;

        public easeeCommunicationException(@Nullable String message) {
            super(message);
        }
    }
}
