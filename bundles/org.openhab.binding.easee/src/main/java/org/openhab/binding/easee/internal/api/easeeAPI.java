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

import static org.openhab.binding.easee.internal.api.easeeApiConstants.GET_CHARGERS_URL;
import static org.openhab.binding.easee.internal.api.easeeApiConstants.LOGIN_AUTHORIZE_URL;
import static org.openhab.binding.easee.internal.api.easeeApiConstants.REFRESH_TOKEN_URL;
import static org.openhab.binding.easee.internal.api.easeeApiConstants.TOKEN_EXPIRATION_BUFFER;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link easeeAPI} handles all Easee Cloud communication, including automatic refresh of authentication
 * security token.
 *
 * @author Andreas Tjernsten - Initial contribution
 */
@NonNullByDefault
public class easeeAPI {
    private final Logger logger = LoggerFactory.getLogger(easeeAPI.class);

    private final HttpClient httpClient;
    private final String uniqueID;
    private final String username;
    private final String password;
    private final Gson gson = new Gson();

    private @Nullable AccessTokenResponse accessTokenResponse;

    /**
     * Create {@link easeeAPI} instance
     * 
     * @param httpClient HttpClient to use for could access
     * @param uniqueID unique ID for this instance
     * @param username Easee cloud username
     * @param password Easee cloud password
     */
    public easeeAPI(HttpClient httpClient, String uniqueID, String username,
            String password) {
        this.httpClient = httpClient;
        this.uniqueID = uniqueID;
        this.username = username;
        this.password = password;
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

            accessTokenResponse.setCreatedOn(LocalDateTime.now());
            this.accessTokenResponse = accessTokenResponse;

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
        logger.debug("getCheckedResponse");
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
     * {@link getAndCheckAccessTokenResponse} gets stored AccessTokenResponse, or null if there was a problem
     * The method checks if the accessToken has expired and tries to refresh it if necessary.
     * 
     * @return AccessTokenResponse
     */
    @Nullable
    private AccessTokenResponse getAndCheckAccessTokenResponse() {
        AccessTokenResponse accessTokenResponse = null;

        logger.debug("getAndCheckAccessTokenResponse");
        try {
            accessTokenResponse = this.accessTokenResponse;

            if ((accessTokenResponse == null)
                    || 
                (accessTokenResponse != null
                    && !accessTokenResponse.isExpired(LocalDateTime.now(), TOKEN_EXPIRATION_BUFFER))) {
                return accessTokenResponse;
            }

           // Assuming that the accessToken is valid but has expired. Refresh needed
           logger.debug("Refreshing access token for Easee cloud user '{}'", username);

           Request request = httpClient.newRequest(REFRESH_TOKEN_URL).method(HttpMethod.POST)
                   .header(HttpHeader.ACCEPT, MediaType.APPLICATION_JSON)
                   .header("Authorization", authTokenHeader(accessTokenResponse))
                   .header(HttpHeader.CONTENT_TYPE, "application/*+json").content(new StringContentProvider(
                           "{\"accessToken\":\"" + accessTokenResponse.getAccessToken() + "\",\"refreshToken\":\"" + accessTokenResponse.getRefreshToken() + "\"}", "utf-8"));

           ContentResponse response = getCheckedResponse(request);


           AccessTokenResponse newAccessTokenResponse = gson.fromJson(response.getContentAsString(),
                    AccessTokenResponse.class);

           if (newAccessTokenResponse == null) {
               logger.trace("Content response: {}", response.getContentAsString());
               logger.error("Could not parse token response from server. Set TRACE level to see full response");
               return null;
            }

            logger.debug("Easee Cloud Auth Token Refreshed for {}, expires in {}", uniqueID,
                    newAccessTokenResponse.getExpiresIn());
 
            newAccessTokenResponse.setCreatedOn(LocalDateTime.now());
            this.accessTokenResponse = newAccessTokenResponse;

            return this.accessTokenResponse;

        } catch (Exception e) {
            logger.error("getAndCheckAccessTokenResponse, exception {}", e.getMessage());
            return null;
        }
    }

    public void close() {
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
