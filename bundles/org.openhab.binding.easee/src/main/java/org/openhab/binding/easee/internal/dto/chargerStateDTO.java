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

/**
 * The {@link chargerDTO} holds internal state information for a Easee Charger.
 *
 * @author Andreas Tjernsten - Initial contribution
 */
public class chargerStateDTO {
    public boolean smartCharging;
    public boolean cableLocked;
    public Integer chargerOpMode;
    public double totalPower;
    public double sessionEnergy;
    public double energyPerHour;
    public Integer wiFiRSSI;
    public Integer cellRSSI;
    public Integer localRSSI;
    public Integer outputPhase;
    public Integer dynamicCircuitCurrentP1;
    public Integer dynamicCircuitCurrentP2;
    public Integer dynamicCircuitCurrentP3;
    public String latestPulse;
    public Integer chargerFirmware;
    public Integer latestFirmware;
    public double voltage;
    public Integer chargerRAT;
    public boolean lockCablePermanently;
    public double inCurrentT2;
    public double inCurrentT3;
    public double inCurrentT4;
    public double inCurrentT5;
    public Integer outputCurrent;
    public boolean isOnline;
    public double inVoltageT1T2;
    public double inVoltageT1T3;
    public double inVoltageT1T4;
    public double inVoltageT1T5;
    public double inVoltageT2T3;
    public double inVoltageT2T4;
    public double inVoltageT2T5;
    public double inVoltageT3T4;
    public double inVoltageT3T5;
    public double inVoltageT4T5;
    public Integer ledMode;
    public Integer cableRating;
    public Integer dynamicChargerCurrent;
    public double circuitTotalAllocatedPhaseConductorCurrentL1;
    public double circuitTotalAllocatedPhaseConductorCurrentL2;
    public double circuitTotalAllocatedPhaseConductorCurrentL3;
    public double circuitTotalPhaseConductorCurrentL1;
    public double circuitTotalPhaseConductorCurrentL2;
    public double circuitTotalPhaseConductorCurrentL3;
    public String reasonForNoCurrent;
    public boolean wiFiAPEnabled;
    public double lifetimeEnergy;
    public Integer offlineMaxCircuitCurrentP1;
    public Integer offlineMaxCircuitCurrentP2;
    public Integer offlineMaxCircuitCurrentP3;
    public Integer errorCode;
    public Integer fatalErrorCode;
    // * "errors;
    public double eqAvailableCurrentP1;
    public double eqAvailableCurrentP2;
    public double eqAvailableCurrentP3;
    public double deratedCurrent;
    public boolean deratingActive;
}
