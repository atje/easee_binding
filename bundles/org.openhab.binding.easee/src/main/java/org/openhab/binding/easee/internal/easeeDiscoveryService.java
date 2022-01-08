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
package org.openhab.binding.easee.internal;

import static org.openhab.binding.easee.internal.easeeBindingConstants.SUPPORTED_DISCOVERY_THING_TYPES_UIDS;
import static org.openhab.binding.easee.internal.easeeBindingConstants.THING_TYPE_CHARGER;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.easee.internal.dto.chargerDTO;
import org.openhab.binding.easee.internal.handler.easeeAccountHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link easeeDiscoveryService} is responsible for discovering easee things such as a Charger
 * Currently only supports discovery of charger devices. Background discovery is enabled by default.
 *
 * @author Andreas Tjernsten - Initial contribution
 */
@Component(service = DiscoveryService.class, configurationPid = "discovery.easee")
@NonNullByDefault
public class easeeDiscoveryService extends AbstractDiscoveryService implements ThingHandlerService {
    private static final int DISCOVERY_TMO_SECONDS = 5;
    private static final long DISCOVERY_INTERVAL_MINUTES = 15;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private @Nullable ScheduledFuture<?> discoveryScheduler;
    
    @Nullable 
    private easeeAccountHandler accountHandler;

    public easeeDiscoveryService() {
        super(SUPPORTED_DISCOVERY_THING_TYPES_UIDS, DISCOVERY_TMO_SECONDS, true);
    }

    @Nullable
    private ThingUID getBridgeUid() {
        var accountHandler = this.accountHandler;
        if (accountHandler == null) {
            return null;
        } else {
            return accountHandler.getThing().getUID();
        }
    }
    
    @Override
    public void startScan() {
        //        easeeAccountHandler accountHandler = this.accountHandler;
        logger.debug("Starting scan for new chargers");
        if (accountHandler != null) {
            List<chargerDTO> chargers = accountHandler.getChargers();
            if (chargers != null) {
                chargers.forEach(charger -> {
                    logger.debug("Found charger {} during scan, adding to thingDiscovered", charger.id);
                        ThingUID thingUID = new ThingUID(THING_TYPE_CHARGER,
                        accountHandler.getThing().getUID(), charger.id);
                        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withLabel(charger.name)
                                .withBridge(accountHandler.getThing().getUID()).build();
                        thingDiscovered(result);
                });
            } else {
                logger.debug("No chargers found when scanning");
            }
        }
    }


    @Override
    public void startBackgroundDiscovery() {
        logger.debug("Starting background discovery");

        if (discoveryScheduler != null && !discoveryScheduler.isCancelled()) {
            discoveryScheduler.cancel(true);
            this.discoveryScheduler = null;
        }

        discoveryScheduler = scheduler.scheduleWithFixedDelay(this::startScan, 0, DISCOVERY_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stopping background discovery");

        if (discoveryScheduler != null && !discoveryScheduler.isCancelled()) {
            discoveryScheduler.cancel(true);
            this.discoveryScheduler = null;
        }
    }

    @Override
    public void setThingHandler(ThingHandler handler) {
        if (handler instanceof easeeAccountHandler) {
            this.accountHandler = (easeeAccountHandler) handler;
        }
    }


    @Override
    public void activate() {
        super.activate(null);
    }

    
    @Override
    public void deactivate() {
        stopBackgroundDiscovery();
        removeOlderResults(System.currentTimeMillis(), getBridgeUid());
    }


    @Override
    public @Nullable ThingHandler getThingHandler() {
       return accountHandler;
    }
}
