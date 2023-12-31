/**
 *  Copyright 2023 Lyle Pakula
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  OwnTracks Driver
 *
 *  Connects OwnTracks push events to virtual presence drivers.
 *
 *  Author: Lyle Pakula (lpakula)
 *  Date: 2023-12-31
 *
 *  // events are received with the following structure
 *  For 'Location':
 *  [
 *      _type:location, 			    // beacon, card, cmd, configuration, encrypted, location, lwt, transition, waypoint, waypoints
 *      acc:12, 					    // accuracy (m)
 *      vac:0, 						    // vertical accuracy of the alt element (m)
 *      tst:1698361486, 			    // UNIX epoch timestamp in seconds of the location fix
 *      created_at:1698361612, 		    // identifies the time at which the message is constructed (vs. tst which is the timestamp of the GPS fix)
 *      alt:1000, 					    // altitude
 *      lat:50.0000000,				    // latitude
 *      lon:-100.0000000, 			    // longitude
 *      inregions:[Home,City],   	    // Current region
 *      topic:owntracks/User/Panther, 	// MQTT topic:  owntracks/<username>/<deviceID>
 *      batt:79, 					    // battery %
 *      bs:1, 						    // Battery Status 0=unknown, 1=unplugged, 2=charging, 3=full
 *      tid:45, 					    // tracker ID
 *      conn:w, 					    // connection type: w=wifi, m=mobile data
 *      vel:0, 						    // velocity (kph)
 *      BSSID:11:22:33:44:55:66, 	    // WiFi AP MAC
 *      SSID:WIFIAP,     			    // WiFi AP SSID
 *      t:u, 						    // trigger: p=ping, c=region, r=reportLocation, u=manual
 *      m:1, 						    // identifies the monitoring mode at which the message is constructed (significant=1, move=2)
 *
 *      // added to packet
 *      homeName:Home,
 *      currentDistanceFromHome:0.234,
 *
 *      // added in the sided load APK
 *      hib:0,                          // App can pause when unused (1=yes, 0=no)
 *      ps:0,                           // Phone is in power save mode (1=yes, 0=no)
 *      bo:0,                           // App has battery optimizations (1=restricted/optimized, 0=unrestricted)
 *      wifi:1,                         // WiFi is enabled (1=yes, 0=no)
 *      loc:0,                          // 0 = Background location, fine precision, -1 = Background location, coarse precision, -2 = Foreground location, fine precision, -3 = Foreground location, coarse precision, -4 = Disabled
 *  ]
 *
 *  For 'Transition':
 *  [
 *      _type:transition,
 *      acc:12,
 *      event:leave,
 *      wtst:1698629024,
 *      tst:1702140057,
 *      lat:50.0000000,
 *      lon:-100.0000000,
 *      tid:45,
 *      t:l,
 *      topic:owntracks/User/Panther,
 *      desc:Home
 *
 *      // added to packet
 *      homeName:Home,
 *      currentDistanceFromHome:0.234,
 *  ]
 **/

import java.text.SimpleDateFormat
import groovy.transform.Field

def driverVersion() { return "1.5.1" }

@Field static final Map BATTERY_STATUS = [ 0: "Unknown", 1: "Unplugged", 2: "Charging", 3: "Full" ]
@Field static final Map DATA_CONNECTION = [ "w": "WiFi", "m": "Mobile" ]
@Field static final Map TRIGGER_TYPE = [ "p": "Ping", "c": "Region", "r": "Report Location", "u": "Manual" ]
@Field static final Map PRESENCE_TILE_BATTERY_FIELD = [ 0: "Battery %", 1: "Current Location and Since Time", 2: "Distance from Home", 3: "Last Speed", 4: "Battery Status (Unplugged/Charging/Full)", 5: "Data Connection (WiFi/Mobile)", 6: "Update Trigger (Ping/Region/Report Location/Manual)" ]
@Field static final Map LOCATION_PERMISION = [ "0": "Background - Fine", "-1": "Background - Coarse", "-2": "Foreground - Fine", "-3": "Foreground - Coarse", "-4": "Disabled" ]

metadata {
  definition (name: "OwnTracks Driver", namespace: "lpakula", author: "Lyle Pakula", importUrl: "") {
        capability "Actuator"
        capability "Presence Sensor"

        command "arrived"
        command "departed"

        attribute "location", "string"
        attribute "since", "string"
        attribute "battery", "string"
        attribute "lastSpeed", "number"
        attribute "distanceFromHome", "number"
        attribute "wifi", "string"

        attribute "batterySaver", "string"
        attribute "hiberateAllowed", "string"
        attribute "batteryOptimizations", "string"
        attribute "locationPermissions", "string"
    }
}

preferences {
    input name: "presenceTileBatteryField", type: "enum", title: "What is displayed on the presence tile battery field", required: true, options: PRESENCE_TILE_BATTERY_FIELD, defaultValue: "1"

    input name: "descriptionTextOutput", type: "bool", title: "Enable Description Text logging", defaultValue: true
    input name: "logLocationChanges", type: "bool", title: "Enable Logging of location changes", defaultValue: false
    input name: "debugOutput", type: "bool", title: "Enable Debug Logging", defaultValue: false
}

def installed() {
    log.info "${device.name}: Location Tracker User Driver Installed"
    state.sinceTime = now()
    updated()
}

def updated() {
    logDescriptionText("${device.name}: Location Tracker User Driver has been Updated")
}

def arrived() {
    descriptionText = device.displayName +  " has arrived"
    sendEvent (name: "presence", value: "present", descriptionText: descriptionText)
    logDescriptionText("$descriptionText")
}

def departed() {
    descriptionText = device.displayName +  " has departed"
    sendEvent (name: "presence", value: "not present", descriptionText: descriptionText)
    logDescriptionText("$descriptionText")
}

def updatePresence(data) {
    def previousPresence = device.currentValue('presence')

    // invalidate the time stamp if our accuracy is poor
    if (data.acc > parent.childGetLocationAccuracyFilter()) {
        data.tst = 0
        logDebug("Suppressing location event due to location accuracy of ${data.acc} m > ${parent.childGetLocationAccuracyFilter()} m")
    }

    // only update if the incoming packet was of valid accuracy
    if (data.tst != 0) {
        // only update the presence for 'home'
        if (data.inregions.find {it==data.homeName}) {
            memberPresence = "present"
        } else {
            memberPresence = "not present"
        }

        // only update the time if there was a state change
        if (previousPresence != memberPresence) {
            state.sinceTime = data.tst
        }
    } else {
        // echo back the past value
        memberPresence = previousPresence
    }

    // return with the updated presence
    return (memberPresence)
}

Boolean generatePresenceEvent(data) {
//    logDebug("Member Data: $data")
    logDebug("Updating '${(data.event ? "Event ${data.event}" : (data.t ? TRIGGER_TYPE[data.t] : "Location"))}' presence for ${device.displayName} -- ${data.currentDistanceFromHome.toFloat().round(3)} km from Home, Battery: ${data.batt}%, Velocity: ${data.vel} kph, accuracy: ${data.acc.toInteger()} m, Location: ${data.lat},${data.lon}")

    // if we have a push event, there is limited data to process
    if (data._type == "transition") {
        // check if we need to update the presence
        memberPresence = updatePresence(data)
        currentLocation = (( data.event == "enter" ) ? "arrived $data.desc" : "left $data.desc")
        descriptionText = device.displayName +  " has " + currentLocation
        logDescriptionText("$descriptionText")
    } else {
        // check if we need to update the presence
        memberPresence = updatePresence(data)

        // if we are in a region stored in the app
        if (data.inregions) {
            locationList = ""
            data.inregions.each { place->
                locationList += "$place,"
            }
            // remove the trailing comma
            currentLocation = locationList.substring(0, locationList.length() - 1)
        } else {
            currentLocation = "${data.currentDistanceFromHome.toFloat().round(1)} km from Home"
        }
        descriptionText = device.displayName +  " is at " + currentLocation


        // process the additional setting information
        sendEvent( name: "wifi", value: (data?.wifi ? "on" : "off") )
        if (data?.wifi == 0) {
            logDebug("Phone has WiFi turned off.  Please turn WiFi on.")
            //logWarn("Phone has WiFi turned off.  Please turn WiFi on.")
        }
        // only display the extra phone fields if they are in a non-optimal state
        if (data?.ps == 1) {
            sendEvent( name: "batterySaver", value: "on" )
            logDebug("Phone is currently in battery saver mode")
            //logWarn("Phone is currently in battery saver mode")
        } else {
            device.deleteCurrentState('batterySaver')
        }
        if (data?.bo == 1) {
            sendEvent( name: "batteryOptimizations", value: "Optimized/Restricted" )
            logDebug("App settting: 'App battery usage' is 'Optimized' or 'Restricted'.  Please change to 'Unrestricted'")
            //logWarn("App settting: 'App battery usage' is 'Optimized' or 'Restricted'.  Please change to 'Unrestricted'")
        } else {
            device.deleteCurrentState('batteryOptimizations')
        }
        if (data?.hib == 1) {
            sendEvent( name: "hiberateAllowed", value: "App can pause" )
            logDebug("App setting: 'Pause app activity if unused' is 'Enabled'.  Please change to 'Disabled'")
            //logWarn("App setting: 'Pause app activity if unused' is 'Enabled'.  Please change to 'Disabled'")
        } else {
            device.deleteCurrentState('hiberateAllowed')
        }
        if (data?.loc != 0) {
            sendEvent( name: "locationPermissions", value: LOCATION_PERMISION["${data?.loc}"])
            logDebug("Location permisions currently set to '${LOCATION_PERMISION["${data?.loc}"]}'.  Please change to 'Allow all the time' and 'Use precise location'")
            //logWarn("Location permisions currently set to '${LOCATION_PERMISION["${data?.loc}"]}'.  Please change to 'Allow all the time' and 'Use precise location'")
        } else {
            device.deleteCurrentState('locationPermissions')
        }

        // only log if there was a location change and log changes is enabled
        if (device.currentValue("location") != currentLocation) {
            if (logLocationChanges) log.info "$descriptionText"
            state.sinceTime = data.tst
        }
    }

    // if we get a blank timestamp, then the phone has no location or this is a ping with high inaccuracy, so do not update any location fields
    if (data.tst != 0) {
        long sinceTimeMilliSeconds = state.sinceTime
        sinceDate = new SimpleDateFormat("E h:mm a yyyy-MM-dd").format(new Date(sinceTimeMilliSeconds * 1000))
        tileDate = new SimpleDateFormat("E h:mm a").format(new Date(sinceTimeMilliSeconds * 1000))

        sendEvent( name: "since", value: sinceDate )
        sendEvent( name: "location", value: currentLocation )
        sendEvent (name: "presence", value: memberPresence, descriptionText: descriptionText)
        sendEvent( name: "distanceFromHome", value:  data.currentDistanceFromHome )
        if (data.vel) sendEvent( name: "lastSpeed", value:  data.vel.toInteger() )

        // we are using the battery field on the presence tile for selectable display
        switch (presenceTileBatteryField) {
            case "0":
                batteryField = (data.batt ? "Battery " + data.batt + "%" : "")
            break
            case "1":
                batteryField = currentLocation + " - " + tileDate
            break
            case "2":
                batteryField = data.currentDistanceFromHome + " km from Home"
            break
            case "3":
                batteryField = (data.vel ? data.vel + " kph" : "")
            break
            case "4":
                batteryField = (data.bs ? BATTERY_STATUS[data.bs] : "")
            break
            case "5":
                batteryField = (data.conn ? DATA_CONNECTION[data.conn] : "")
            break
            case "6":
                batteryField = (data.t ? TRIGGER_TYPE[data.t] : "")
            break
        }

        // deal with the cases where the above data might not come in a particular event, so leave the previous event
        if (batteryField) sendEvent( name: "battery", value: batteryField  )
    }

    return true
}

private logDebug(msg) {
    if (settings?.debugOutput) {
        log.debug "$msg"
    }
}

private logDescriptionText(msg) {
    if (settings?.descriptionTextOutput) {
        log.info "$msg"
    }
}

private logWarn(msg) {
    log.warn "$device.displayName: $msg"
}

private logError(msg) {
    log.error "$device.displayName: $msg"
}
