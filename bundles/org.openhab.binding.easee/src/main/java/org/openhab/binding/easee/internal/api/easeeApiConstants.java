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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link easeeApiConstants} class contains constants used for Ease cloud access.
 *
 * @author Andreas Tjernsten - Initial contribution
 */
@NonNullByDefault
public class easeeApiConstants {
    public static final String LOGIN_BASE_URL = "https://api.easee.cloud/api";
    public static final String LOGIN_AUTHORIZE_URL = LOGIN_BASE_URL + "/accounts/login";
    public static final String REFRESH_TOKEN_URL = LOGIN_BASE_URL + "/accounts/refresh_token";
    public static final String GET_CHARGERS_URL = LOGIN_BASE_URL + "/chargers";
    public static final String GET_STATE_URL = LOGIN_BASE_URL + "/state";

    public static final int TOKEN_EXPIRATION_BUFFER = 300; // 5 minute buffer
}
