# easee Binding

This binding provides access to [Easee](https://easee.com/) car charging robots through integration with [Easee Cloud](https://developer.easee.cloud/). It currently supports read-only data from Easee charger robots registered to your personal cloud account.   

_Note that you need to have a registered Easee Cloud account for access to be possible._

## Supported Things

| Thing Type ID       | Description                                                 |
| ------------------- | ----------------------------------------------------------- |
| account             | The Cloud account access, acts as Bridge for charger things |
| charger             | An Easee car smart charging robot                           |

## Discovery

Once an account bridge has been configured and is online, the binding will discover new chargers automatically.

## Thing Configuration

Supported configuration parameters for account bridges:

| Property                        | Type    | Default | Required | Description                                                     |
|---------------------------------|---------|---------|----------|-----------------------------------------------------------------|
| username                        | String  |         | Yes      | email or telephone number used for login to Easee Cloud account |
| password                        | String  |         | Yes      | password used for login to Easee Cloud account                  |
| pollingInterval                 | Integer | 60      | No       | Polling interval towards Cloud service. Defaults to 60 seconds  |

## Channels

_Here you should provide information about available channel types, what their meaning is and how they can be used._

_Note that it is planned to generate some part of this based on the XML files within ```src/main/resources/OH-INF/thing``` of your binding._

The following read only channels are supported for a charger thing 

| Channel              | Type                   | Description                                                                                 |
|----------------------|------------------------|---------------------------------------------------------------------------------------------|
| state                | String                 | Charger state, see description below                                                        |
| totalpower           | Number:Power           | The total power provided to the vehicle (total for all power lines)                         |
| energyperhour        | Number:Power           | Energy per hour (in kWh) currently provided to the vehicle                                  |
| sessionenergy        | Number:Power           | Total energy provided for this vehicle during current or last charging session              |
| lifetimeenergy       | Number:Power           | The total energy provided through the charger (kWh), since charger was first commissioned   |
| phase1current        | Number:ElectricCurrent | Phase 1 current  (Ampere)                                                                   |
| phase2current        | Number:ElectricCurrent | Phase 2 current  (Ampere)                                                                   |
| phase3current        | Number:ElectricCurrent | Phase 3 current  (Ampere)                                                                   |
| phase1voltage        | Number:ElectricCurrent | Phase 1 voltage  (Volt)                                                                     |
| phase2voltage        | Number:ElectricCurrent | Phase 2 voltage  (Volt)                                                                     |
| phase3voltage        | Number:ElectricCurrent | Phase 3 voltage  (Volt)                                                                     |
| newfirwareavailable  | Switch                 | New firmware for the charger is available if ON, otherwise OFF                              |


The state channel contains of the following string values:

| state value | Description                                                       |
|-------------|-------------------------------------------------------------------|
| waiting     | Waiting for car to connect to charger                             |
| connected   | Car connected, charging not started                               |
| charging    | Car charging ongoing                                              |
| unknown     | The charger state currently unmapped (reverse-engineering needed) |


## Full Example

easee.things:

```
Bridge easee:account:myeasee "Easee cloud account" [ username="<user_name>", password="<password>" ] {
  Things: 
    charger EH123456 "Car Charger" @ "Garage" [ id="EH123456" ]
}
```

easee.items:

```
Group gCarCharger "Car charger group"

String chargerStatus            "Charger status"              (gCarCharger) {channel="easee:charger:dcdaca4318:EH123456:state"}
Switch newFirmwareAvailable     "New charger FW available"    (gCarCharger)            {channel="easee:charger:dcdaca4318:EH123456:newfirwareavailable"}

Number chargerLifetimeEnergy    "Charger lifetime energy"     (gCarCharger)  {channel="easee:charger:dcdaca4318:EH123456:lifetimeenergy"} 
Number chargerEnergyPerHour     "Charger energy per hour"     (gCarCharger)  {channel="easee:charger:dcdaca4318:EH123456:energyperhour"}
Number chargerSessionEnergy     "Session energy"              (gCarCharger)            {channel="easee:charger:dcdaca4318:EH123456:sessionenergy"}
Number chargerTotalPower        "Charger total power"         (gCarCharger)            {channel="easee:charger:dcdaca4318:EH123456:totalpower"}

Number chargerP1Current         "P1 Current"                  (gCarCharger)            {channel="easee:charger:dcdaca4318:EH123456:phase1current"}
Number chargerP2Current         "P2 Current"                  (gCarCharger)            {channel="easee:charger:dcdaca4318:EH123456:phase2current"}
Number chargerP3Current         "P3 Current"                  (gCarCharger)            {channel="easee:charger:dcdaca4318:EH123456:phase3current"}

Number chargerP1Voltage         "P1 Voltage"                  (gCarCharger)            {channel="easee:charger:dcdaca4318:EH123456:phase1voltage"}
Number chargerP2Voltage         "P2 Voltage"                  (gCarCharger)            {channel="easee:charger:dcdaca4318:EH123456:phase2voltage"}
Number chargerP3Voltage         "P3 Voltage"                  (gCarCharger)            {channel="easee:charger:dcdaca4318:EH123456:phase3voltage"}
```

