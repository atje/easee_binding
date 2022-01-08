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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link easeeBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Andreas Tjernsten - Initial contribution
 */
@NonNullByDefault
public class easeeBindingConstants {

    private static final String BINDING_ID = "easee";

    public static final String VENDOR_EASEE = "Easee";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_ACCOUNT = new ThingTypeUID(BINDING_ID, "account");
    public static final ThingTypeUID THING_TYPE_CHARGER = new ThingTypeUID(BINDING_ID, "charger");
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_ACCOUNT, THING_TYPE_CHARGER);
    public static final Set<ThingTypeUID> SUPPORTED_DISCOVERY_THING_TYPES_UIDS = Set.of(THING_TYPE_CHARGER);

    // Charger channels
    public static final class Channels {
        private Channels() {
        }

        public static final String STATE_ID = "state";
        public static final String TOTAL_POWER_ID = "totalpower";
        public static final String ENERGY_PER_HOUR = "energyperhour";
        public static final String SESSION_ENERGY = "sessionenergy";
        public static final String LIFETIME_ENERGY = "lifetimeenergy";
        
        public static final String PHASE1_CURRENT = "phase1current";
        public static final String PHASE2_CURRENT = "phase2current";
        public static final String PHASE3_CURRENT = "phase3current";
        public static final String PHASE1_VOLTAGE = "phase1voltage";
        public static final String PHASE2_VOLTAGE = "phase2voltage";
        public static final String PHASE3_VOLTAGE = "phase3voltage";
        public static final String NEW_FIRMWARE_AVAIL = "newfirwareavailable";    
    }
    
    // Charger configuration parameters
    public static final class Parameters {
        private Parameters() {
        }

        //public static final String "";
    }
}
