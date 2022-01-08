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

import static org.openhab.binding.easee.internal.easeeBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.easee.internal.handler.easeeAccountHandler;
import org.openhab.binding.easee.internal.handler.easeeChargerHandler;
import org.openhab.core.auth.client.oauth2.OAuthFactory;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link easeeHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Andreas Tjernsten - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.easee", service = ThingHandlerFactory.class)
public class easeeHandlerFactory extends BaseThingHandlerFactory {
    private final HttpClient httpClient;
    private final OAuthFactory oAuthFactory;

    @Activate
    public easeeHandlerFactory(final @Reference HttpClientFactory httpClientFactory,
            final @Reference OAuthFactory oAuthFactory) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
        this.oAuthFactory = oAuthFactory;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_ACCOUNT.equals(thingTypeUID)) {
            return new easeeAccountHandler((Bridge) thing, httpClient, oAuthFactory);
        }

        if (THING_TYPE_CHARGER.equals(thingTypeUID)) {
            return new easeeChargerHandler(thing);
        }

        return null;
    }
}
