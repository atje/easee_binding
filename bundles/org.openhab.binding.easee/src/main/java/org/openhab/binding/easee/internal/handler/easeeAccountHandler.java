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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.easee.internal.api.easeeAPI;
import org.openhab.binding.easee.internal.dto.chargerDTO;
import org.openhab.binding.easee.internal.easeeAccountConfiguration;
import org.openhab.binding.easee.internal.easeeDiscoveryService;
import org.openhab.core.auth.client.oauth2.OAuthFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link easeeAccountHandler} implements a bridge towards Easee cloud, representing a registered account.
 * When active, it will trigger updates of all defined charger Things with an intervall defined by the configuration
 * parameter pollingInterval
 *
 * @author Andreas Tjernsten - Initial contribution, based heavily on MyQ binding
 */
@NonNullByDefault
public class easeeAccountHandler extends BaseBridgeHandler implements ThingHandler {

    private final Logger logger = LoggerFactory.getLogger(easeeAccountHandler.class);
    private final HttpClient httpClient;

    private @Nullable Future<?> pollFuture;
    private @Nullable easeeAPI api;
    private List<chargerDTO> chargers = new ArrayList<>();

    private Integer pollingIntervall = 60;

    public easeeAccountHandler(Bridge bridge, final HttpClient httpClient, final OAuthFactory oAuthFactory) {
        super(bridge);
        this.httpClient = httpClient;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        try {
            easeeAccountConfiguration config = getConfigAs(easeeAccountConfiguration.class);
            pollingIntervall = config.pollingInterval;

            api = new easeeAPI(httpClient, getThing().toString(), config.username, config.password);

            updateStatus(ThingStatus.UNKNOWN);
            api.authenticateUser();
            updateStatus(ThingStatus.ONLINE);
            chargers = api.getChargers();
            logger.info("Got {} chargers from cloud service (enable TRACE level to see full list)", chargers.size());
            logger.trace("chargers: {}", chargers.toString());

            for (Thing thing : getThing().getThings()) {
                ThingHandler handler = thing.getHandler();
                if (handler != null) {
                    ((easeeChargerHandler) handler).initialize();
                }
            }
            
            startPoll();

        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
        }
    }

    @Nullable
    public List<chargerDTO> getChargers() {
        return chargers;
    }

    @Override
    public void dispose() {
        stopPoll();
        if (api != null) {
            api.close();
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(easeeDiscoveryService.class);
    }

    @Nullable
    public easeeAPI getAPI() {
        return api;
    }

    private synchronized void startPoll() {
        if (pollFuture != null) {
            logger.debug("Polling restarted");
            pollFuture.cancel(true);
        }
        pollFuture = scheduler.scheduleWithFixedDelay(this::poll, 0, pollingIntervall, TimeUnit.SECONDS);
    }

    private synchronized void stopPoll() {
        if (pollFuture != null) {
            logger.debug("Polling stopped");
            pollFuture.cancel(true);
        }
        pollFuture = null;
    }

    private void poll() {
        logger.debug("Polling - calling updateChargerChannels on all Charger Things");
        for (Thing thing : getThing().getThings()) {
            ThingHandler handler = thing.getHandler();
            if (handler != null) { 
                ((easeeChargerHandler) handler).updateChargerFromCloud();
            }
        }
    }
}
