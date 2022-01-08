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

import static org.openhab.binding.easee.internal.easeeBindingConstants.Channels.*;
import static org.openhab.binding.easee.internal.easeeBindingConstants.VENDOR_EASEE;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.easee.internal.api.easeeAPI;
import org.openhab.binding.easee.internal.dto.chargerStateDTO;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link easeChargerHandler} is responsible for handling commands for a charger thing.
 *
 * @author Andreas Tjernsten - Initial contribution
 */
@NonNullByDefault
public class easeeChargerHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(easeeChargerHandler.class);

    private @Nullable chargerStateDTO chargerState;
    private @Nullable easeeAPI api;

    public easeeChargerHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        try {
            ThingHandler bridgeHandler = getBridge().getHandler();
            easeeAccountHandler account = (easeeAccountHandler) bridgeHandler;
            if (account == null) {
                logger.error("No Easee API while initiatizing ChargerThing, null");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                        "No Easee account while initiatizing ChargerThing, null");
                return;
            }
            api = account.getAPI();

            updateStatus(ThingStatus.ONLINE);
        } catch (Exception e) {
            logger.error("Exception while initiatizing ChargerThing, error '{}'", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR, e.getMessage());
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            if (command instanceof RefreshType) {
                updateChargerFromCloud();
                return;
            }
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    /**
     * {@link updateChargerFromCloud} updates charger Thing channels and configuration properties based on cloud state
     * information
     * Changes the charger Thing to OFFLINE if there was a problem getting state information or if the charger is
     * disabled (through native app or otherwise)
     * 
     */
    public void updateChargerFromCloud() {
        try {
            String id = getThing().getUID().getId();

            logger.debug("Updating channels for charger {}", id);
            chargerState = api.getChargerState(id);
            if (chargerState == null) {
                logger.debug("Failed to update state from API, Charger {} changed to OFFLINE. Will try again later",
                        id);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Failed to update state from API. Will try again later");
                return;
            }

            if (!chargerState.isOnline) {
                logger.debug("Charger '{}' is cloud offline, setting Charger status to OFFLINE", id);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Cloud reporting charger " + id + " offline");
                return;
            }

            // Put charger back online is it has been offline
            if (getThing().getStatus().equals(ThingStatus.OFFLINE)) {
                updateStatus(ThingStatus.ONLINE);
            }

            // Update configuration properties
            Map<String, String> properties = editProperties();
            properties.put(Thing.PROPERTY_FIRMWARE_VERSION, chargerState.chargerFirmware.toString());
            properties.put(Thing.PROPERTY_VENDOR, VENDOR_EASEE);
            updateProperties(properties);

            // Update channels
            switch (chargerState.chargerOpMode) {

                case 1:
                    updateState(STATE_ID, new StringType("waiting"));
                    break;
                case 2:
                    updateState(STATE_ID, new StringType("connected"));
                    break;
                case 3:
                    updateState(STATE_ID, new StringType("charging"));
                    break;
                case 4:
                    updateState(STATE_ID, new StringType("idle"));
                    break;
                default:
                    updateState(STATE_ID, new StringType("unknown"));
                    logger.debug("chargerOpMode = {}", chargerState.chargerOpMode);
                    break;
            }
            updateState(TOTAL_POWER_ID, new DecimalType(chargerState.totalPower));
            updateState(ENERGY_PER_HOUR, new DecimalType(chargerState.energyPerHour));
            updateState(SESSION_ENERGY, new DecimalType(chargerState.sessionEnergy));
            updateState(LIFETIME_ENERGY, new DecimalType(chargerState.lifetimeEnergy));

            updateState(PHASE1_CURRENT, new DecimalType(chargerState.inCurrentT3));
            updateState(PHASE2_CURRENT, new DecimalType(chargerState.inCurrentT4));
            updateState(PHASE3_CURRENT, new DecimalType(chargerState.inCurrentT5));

            updateState(PHASE1_VOLTAGE, new DecimalType(chargerState.inVoltageT2T3));
            updateState(PHASE2_VOLTAGE, new DecimalType(chargerState.inVoltageT2T4));
            updateState(PHASE3_VOLTAGE, new DecimalType(chargerState.inVoltageT2T5));

            updateState(NEW_FIRMWARE_AVAIL,
                    (chargerState.latestFirmware.equals(chargerState.chargerFirmware)) ? OnOffType.OFF : OnOffType.ON);

        } catch (Exception e) {
            logger.error("Caught exception, message '{}'", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }
}
