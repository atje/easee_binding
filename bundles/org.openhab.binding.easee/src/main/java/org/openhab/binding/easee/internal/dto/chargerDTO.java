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
package org.openhab.binding.easee.internal.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link chargerDTO} holds internal data for a Easee Charger.
 *
 * @author Andreas Tjernsten - Initial contribution
 */
@NonNullByDefault
public class chargerDTO {
    public String id;
    public String name;
    public @Nullable Integer color;
    public @Nullable String createdOn;
    public @Nullable String updatedOn;
    public @Nullable String backPlate;
    public @Nullable String levelOfAccess;
    public @Nullable String productCode;

    public chargerDTO(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
