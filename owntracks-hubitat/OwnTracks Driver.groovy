/**
 *  Copyright 2024 Lyle Pakula
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
 *      cog:180,       				    // bearing (degrees)
 *      BSSID:11:22:33:44:55:66, 	    // WiFi AP MAC
 *      SSID:WIFIAP,     			    // WiFi AP SSID
 *      t:u, 						    // trigger: p=ping, c=region, r=reportLocation, u=manual
 *      m:1, 						    // identifies the monitoring mode at which the message is constructed (significant=1, move=2)
 *
 *      // added to packet
 *      currentDistanceFromHome:0.234,  // distance from home in current units
 *      private:false                   // if true, user is configured to not share their information, only their current presence
 *      memberWiFiHome:true             // if true, user is connected to the home wifi SSID
 *      memberAtHome:true               // if true, user is at home
 *
 *      // added in the sided load APK
 *      hib:0,                          // App can pause when unused (1=yes, 0=no)
 *      ps:0,                           // Phone is in power save mode (1=yes, 0=no)
 *      bo:0,                           // App has battery optimizations (1=restricted/optimized, 0=unrestricted)
 *      wifi:1,                         // WiFi is enabled (1=yes, 0=no)
 *      loc:0,                          // 0 = Background location, fine precision, -1 = Background location, coarse precision, -2 = Foreground location, fine precision, -3 = Foreground location, coarse precision, -4 = Disabled
 *      address: 742 Evergreen Terrace  // address for the lat/lon reported
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
 *      currentDistanceFromHome:0.234,
 *  ]
 *
 *
 *  Author: Lyle Pakula (lpakula)
 *
 *  Changelog:
 *  Version    Date            Changes
 *  1.6.4      2024-01-07      - Moved SSID from the extended attributes block.
 *  1.6.5      2024-01-08      - Added last location time field.  Fixed issue where SSID wasn't getting deleted when WiFi was disconnected.  Moved SSID check from driver to app.
 *  1.6.6      2024-01-16      - Added imageURL for user's local thumbnail image
 *  1.6.7      2024-01-17      - Added missing trigger type.
 *  1.6.8      2024-01-18      - Filter the +follow regions.  Added a status attribute to track the last region enter/leave event.
 *  1.6.9      2024-01-20      - Allow for members data to be private.  Only presence and report time is captured.
 *  1.6.10     2024-01-21      - Added address that will be displayed in location instead of lat/lon, if present.  Wifi attribute is removed from devices that are not reporting it.
 *  1.6.11     2024-01-22      - Expose the ENUM variants for monitoringMode, batteryStatus, dataConnection, and triggerSource.
 *  1.6.12     2024-01-23      - Changed battery field to show just the battery level number.
 *  1.6.13     2024-01-25      - Changed battery field for location to show region, address or lat/lon.  Added a battery field to show distance from home.  Removed the 'status' attribute due to redundance. Added HTML MemberLocation tile.
 *  1.6.14     2024-01-26      - Removed setting initialization that was breaking new settings.
 *  1.6.15     2024-01-28      - Create / remove the member tile if the setting is toggled.  Added a 'isImperialUnits' attribute.  Set speed to 0 when it no longer reports.  Created responsive member tile.
 *  1.6.16     2024-01-28      - Added SSID to debug output.
 *  1.6.17     2024-01-28      - Reduced member tile map height to prevent overlapping into the other metrics.
 *  1.6.18     2024-01-28      - Reduced member tile size to prevent overflow.  Re-factored the attribute updates to allow invalid location packets to update non-location information.
 *  1.6.19     2024-01-29      - Fixed issue where SSID was getting stuck holding member at home.
 *  1.6.20     2024-01-29      - Fixed map size after tile size was reduced.
 *  1.6.21     2024-01-29      - Schedule the member tile update to occur later to allow attibutes to save.
 *  1.7.0      2024-01-30      - Move street address logic to app.
 *  1.7.1      2024-01-31      - Refactored flow to prevent dirty location reports from triggering transitions.  Allow enter/leave transition notifications.
 *  1.7.2      2024-02-01      - Clarified the notification settings.
 *  1.7.3      2024-02-01      - 'Since' time was not updating properly after refactor.  Block transition if still connected to home WiFi SSID.
 *  1.7.4      2024-02-03      - Fixed grammar on the transition notifications.  Arrived/Departed buttons update the tranistions.  Moved notification control to the app.
 *  1.7.5      2024-02-03      - Removed OwnTracks prefix from the member tile name when no image is available.
 *  1.7.6      2024-02-04      - Allow device name prefix to be changed.
 *  1.7.7      2024-02-05      - Changed the starting zoom level of the region maps to show house level.
 *  1.7.8      2024-02-07      - Added last locations tile if Recorder is enabled.  Deleted the SSID attribute if the phone switches to mobile data.
 *  1.7.9      2024-02-09      - Added battery capability.
 *  1.7.10     2024-02-10      - Updated logging.
 *  1.7.11     2024-02-11      - Placed location debug under disable/enable slider.
 *  1.7.12     2024-02-15      - Create a Presence Tile.  Removed custom text from battery field.
 *  1.7.13     2024-02-17      - Fixed tile format.
 *  1.7.14     2024-02-26      - Changed presence tile text when colored tiles are enabled.
 *  1.7.15     2024-03-03      - Fixed tile formatting and refactored tile layouts.
 *  1.7.16     2024-03-05      - Added dynamic support for cloud recorder URL.  Added searchable past locations tile.
 *  1.7.17     2024-03-05      - Added ability to change past locations from points to lines.
 *  1.7.18     2024-03-14      - Text cleanup.
 *  1.7.19     2024-03-21      - Fix error on the first time a member is added and the tiles were being generated.
 *  1.7.20     2024-03-23      - Changed tile generation timing.
 *  1.7.21     2024-03-25      - Past locations tile can toggle lines/points from the tile.
 *  1.7.22     2024-03-26      - Presence tile was only updating when a presence change occured, not when the transition changed.
 *  1.7.23     2024-03-28      - Changed the transition phrases to past tense.
 *  1.7.24     2024-03-31      - Presence and member tiles get regenerated automatically on change.
 *  1.7.25     2024-04-01      - Missed a transition phrase on the presence check.
 *  1.7.26     2024-04-07      - Changed cloud/local URL sourcing.
 *  1.7.27     2024-04-14      - Fixed blocked notification if a member leaves home but is still connected to wifi.
 *  1.7.28     2024-04-20      - Change the member name being passed to the notification handler.
 *  1.7.29     2024-04-22      - Added offline data connection.
 *  1.7.30     2024-04-25      - Added virtual switch.
 *  1.7.31     2024-04-27      - Added logging to virtual switch.
 *  1.7.32     2024-05-02      - Added additional status command logging/display.
 *  1.7.33     2024-05-04      - Removed filtering for zero timestamp.
 *  1.7.34     2024-05-06      - Fixed zero battery and zero speed not being debug logged.
 *  1.7.35     2024-06-22      - Move sinceTime from state to attribute.
 *  1.7.36     2024-06-23      - Forced sinceTime attribute to be set to allow consumption by the tile generation.
 *  1.7.37     2024-07-19      - Removed migration code.
 *  1.7.38     2024-08-10      - Added course over ground (bearing).
 *  1.7.39     2024-08-11      - Updated bearing.
 *  1.7.40     2024-08-16      - Updated trigger types.
 *  1.7.41     2024-09-14      - Adjust member location iframe to hide the view on larger map overlay.
 *  1.8.0      2024-09-23      - Up-reved version to match app.
 *  1.8.1      2024-12-14      - Ignore duplicate transition events if they occur within 5-seconds of the previous one.
 *  1.8.2      2024-12-20      - Force an update to the transition attributes to eliminate cache delays.
 *  1.8.3      2025-04-13      - Send the "switch off" during installation.
 *  1.8.4      2025-07-27      - Added a deadband to prevent transition and presence  arrived/departed ping-pong notifications.
 *  1.8.5      2025-08-19      - Fixed timestamp type causing exceptions during transitions.
 *  1.8.6      2025-08-19      - Fixed timestamp type causing exceptions during transitions.
 *  1.8.7      2025-08-20      - Fixed migration issue with transition deadband.
 *  1.8.8      2025-08-26      - Fixed issue with private members.
 **/

import java.text.SimpleDateFormat
import groovy.transform.Field

def driverVersion() { return "1.8.8" }

@Field static final Map MONITORING_MODE = [ 0: "Unknown", 1: "Significant", 2: "Move" ]
@Field static final Map BATTERY_STATUS = [ 0: "Unknown", 1: "Unplugged", 2: "Charging", 3: "Full" ]
@Field static final Map DATA_CONNECTION = [ "w": "WiFi", "m": "Mobile", "o": "Offline"  ]
@Field static final Map TRIGGER_TYPE = [ "p": "Ping", "c": "Region", "r": "Report Location", "u": "Manual", "b": "Beacon", "t": "Timer", "v": "Monitoring", "l": "Region" ]
@Field static final Map PRESENCE_TILE_BATTERY_FIELD = [ 0: "Battery %", 1: "Current Location and Since Time", 2: "Distance from Home", 3: "Last Speed", 4: "Battery Status (Unplugged/Charging/Full)", 5: "Data Connection (WiFi/Mobile)", 6: "Update Trigger (Ping/Region/Report Location/Manual)", 7: "Distance from Home and Since Time" ]
@Field static final Map LOCATION_PERMISION = [ "0": "Background - Fine", "-1": "Background - Coarse", "-2": "Foreground - Fine", "-3": "Foreground - Coarse", "-4": "Disabled" ]
@Field static final Map TRANSITION_DIRECTION = [ "enter": "arrived", "leave": "departed" ]
@Field static final Map TRANSITION_PHRASES = [ "enter": "arrived at", "leave": "departed from" ]

@Field String  CLOUD_URL_SOURCE = "[cloud.hubitat.com]"
@Field Boolean DEFAULT_presenceTileBatteryField = 0
@Field Boolean DEFAULT_displayExtendedAttributes = true
@Field Boolean DEFAULT_displayMemberTile = false
@Field Boolean DEFAULT_displayLastLocationTile = false
@Field Boolean DEFAULT_colorMemberTile = true
@Field Boolean DEFAULT_descriptionTextOutput = true
@Field Boolean DEFAULT_debugOutput = false
@Field Boolean DEFAULT_debugLogAddresses = false
@Field Boolean DEFAULT_logLocationChanges = false
@Field String  DEFAULT_privateLocation = "Private"
@Field Boolean DEFAULT_lastLocationViewTracks = false
@Field Number  DEFAULT_pastLocationSearchWindow = 0.5
@Field Number  DEFAULT_notificationHysteresisSeconds = 90

metadata {
  definition (
      name:        "OwnTracks Driver",
      namespace:   "lpakula",
      author:      "Lyle Pakula",
      importUrl:   "https://raw.githubusercontent.com/wir3z/hubitat/main/owntracks-hubitat/OwnTracks%20Driver.groovy"
  ) {
        capability "Actuator"
        capability "Presence Sensor"
        capability "Battery"
        capability "Switch"

        command    "arrived"
        command    "departed"
        command    "createMemberTile"

        attribute  "switch", "string"

        attribute  "location", "string"
        attribute  "transitionRegion", "string"
        attribute  "transitionTime", "string"
        attribute  "transitionDirection", "string"
        attribute  "since", "string"
        attribute  "sinceTime", "number"
        attribute  "battery", "number"
        attribute  "lastSpeed", "number"
        attribute  "distanceFromHome", "number"
        attribute  "wifi", "string"
        attribute  "lastLocationtime", "string"
        attribute  "imperialUnits", "string"

        attribute  "batterySaver", "string"
        attribute  "hiberateAllowed", "string"
        attribute  "batteryOptimizations", "string"
        attribute  "locationPermissions", "string"

        attribute  "MemberLocation", "string"
        attribute  "PastLocations", "string"
        attribute  "PresenceTile", "string"

        // extended attributes
        attribute  "lat", "number"
        attribute  "lon", "number"
        attribute  "accuracy", "number"
        attribute  "bearing", "number"
        attribute  "verticalAccuracy", "number"
        attribute  "altitude", "number"
        attribute  "sourceTopic", "string"
        attribute  "BSSID", "string"
        attribute  "SSID", "string"
        attribute  "address", "string"
        attribute  "streetAddress", "string"
        attribute  "dataConnection", "enum", [ "WiFi", "Mobile" ]
        attribute  "batteryStatus", "enum", [ "Unknown", "Unplugged", "Charging", "Full" ]
        attribute  "triggerSource", "enum", [ "Ping", "Region", "Report Location", "Manual", "Beacon", "Timer", "Monitoring", "Location" ]
        attribute  "monitoringMode", "enum", [ "Unknown", "Significant", "Move" ]
    }
}

preferences {
    input name: "displayExtendedAttributes", type: "bool", title: "Display extended location attributes", defaultValue: DEFAULT_displayExtendedAttributes
    input name: "displayMemberTile", type: "bool", title: "Create a HTML MemberLocation tile", defaultValue: DEFAULT_displayMemberTile
    input name: "colorMemberTile", type: "bool", title: "Change MemberTile background color based on presence", defaultValue: DEFAULT_colorMemberTile
    input name: "displayLastLocationTile", type: "bool", title: "Create a HTML PastLocations tile", defaultValue: DEFAULT_displayLastLocationTile
    input name: "lastLocationViewTracks", type: "bool", title: "Past locations history defaults to lines instead of points", defaultValue: DEFAULT_lastLocationViewTracks
    input name: "pastLocationSearchWindow", type: "decimal", title: "PastLocations tile search window start date is this many days from current date (0.1..31)", range: "0.1..31.0", defaultValue: DEFAULT_pastLocationSearchWindow
    input name: "notificationHysteresisSeconds", type: "number", title: "Number of seconds the member needs to be in the same region before the new transition update is sent (0..300).  Used to address arrived/departed ping-pong notifications, Default ${DEFAULT_notificationHysteresisSeconds} seconds", range: "0..300", defaultValue: DEFAULT_notificationHysteresisSeconds

    input name: "descriptionTextOutput", type: "bool", title: "Enable Description Text logging", defaultValue: DEFAULT_descriptionTextOutput
    input name: "debugOutput", type: "bool", title: "Enable Debug Logging", defaultValue: DEFAULT_debugOutput
    input name: "debugLogAddresses", type: "bool", title: "Debug Logging Includes Addesses and Location", defaultValue: DEFAULT_debugLogAddresses
    input name: "logLocationChanges", type: "bool", title: "Enable Logging of location changes", defaultValue: DEFAULT_logLocationChanges
}

def installed() {
    log.info "${device.name}: Location Tracker User Driver Installed"
    sendEvent( name: "sinceTime", value: now() )
    sendEvent( name: "switch", value: "off" )
    state.driverVersion = driverVersion()
    state.memberName = ""
    state.homeName = ""
    state.pendingTransitionUpdateData = ""
    state.transitionDeadband = 0
    updated()
}

def updated() {
    logDescriptionText("${device.name}: Location Tracker User Driver has been Updated")
    // generate / remove the member tile as required
    generateTiles()
    // remove the extended attributes if not enabled
    deleteExtendedAttributes(false)
}

def on() {
    sendEvent( name: "switch", value: "on" )
    logDescriptionText("$device.displayName: switch on")
}

def off() {
    sendEvent( name: "switch", value: "off" )
    logDescriptionText("$device.displayName: switch off")
}

def arrived() {
    updatePresenceEvent("enter", "present", (now()/1000).toInteger())
}

def departed() {
    updatePresenceEvent("leave", "not present", (now()/1000).toInteger())
}

def createMemberTile() {
    generateTiles()
}

def updatePresenceEvent(dataEvent, presenceState, dataTst) {
    createTransitionEvent(state.homeName, dataEvent, dataTst)
    createPresenceEvent(presenceState, dataTst)
    generateTiles()
}

def deleteExtendedAttributes(privateMember = false) {
    if (!displayExtendedAttributes || privateMember) {
        device.deleteCurrentState('accuracy')
        device.deleteCurrentState('verticalAccuracy')
        device.deleteCurrentState('altitude')
        device.deleteCurrentState('bearing')
        device.deleteCurrentState('sourceTopic')
        device.deleteCurrentState('dataConnection')
        device.deleteCurrentState('batteryStatus')
        device.deleteCurrentState('BSSID')
        device.deleteCurrentState('triggerSource')
        device.deleteCurrentState('monitoringMode')
    }
}

def deletePrivateExtendedAttributes() {
    device.deleteCurrentState('location')
    device.deleteCurrentState('transitionRegion')
    device.deleteCurrentState('transitionDirection')
    device.deleteCurrentState('transitionTime')
    device.deleteCurrentState('since')
    device.deleteCurrentState('battery')
    device.deleteCurrentState('lastSpeed')
    device.deleteCurrentState('distanceFromHome')
    device.deleteCurrentState('wifi')
    device.deleteCurrentState('batterySaver')
    device.deleteCurrentState('hiberateAllowed')
    device.deleteCurrentState('batteryOptimizations')
    device.deleteCurrentState('locationPermissions')
    device.deleteCurrentState('address')
    device.deleteCurrentState('streetAddress')
}

def updateAttributes(data) {
    // remove the attributes if not enabled or the member is private
    deleteExtendedAttributes(data.private)
    if (data.private) {
        deletePrivateExtendedAttributes()
    } else {
        // display the extended attributes if they were received, but only allow them to be removed on non-transition event
        locationType = (data._type == "location")
        if (data?.acc)         sendEvent (name: "accuracy", value: parent.displayMFtVal(data.acc))             else if (locationType) device.deleteCurrentState('accuracy')
        if (data?.vac)         sendEvent (name: "verticalAccuracy", value: parent.displayMFtVal(data.vac))     else if (locationType) device.deleteCurrentState('verticalAccuracy')
        if (data?.alt)         sendEvent (name: "altitude", value: parent.displayMFtVal(data.alt))             else if (locationType) device.deleteCurrentState('altitude')
        if (data?.cog)         sendEvent (name: "bearing", value: data.cog)                                    else if (locationType) device.deleteCurrentState('bearing')
        if (data?.address) {
            sendEvent (name: "address", value: data.address)
            sendEvent (name: "streetAddress", value: data.streetAddress)
        } else {
            if (locationType)  device.deleteCurrentState('address')
            if (locationType)  device.deleteCurrentState('streetAddress')
        }

        // can be updated all the time
        if (data?.batt)        sendEvent (name: "battery", value: data.batt)                                   else if (locationType) device.deleteCurrentState('battery')
        if (data?.topic)       sendEvent (name: "sourceTopic", value: data.topic)                              else if (locationType) device.deleteCurrentState('sourceTopic')
        if (data?.bs)          sendEvent (name: "batteryStatus", value: BATTERY_STATUS[data.bs])               else if (locationType) device.deleteCurrentState('batteryStatus')
        if (data?.conn)        sendEvent (name: "dataConnection", value: DATA_CONNECTION[data.conn])           else if (locationType) device.deleteCurrentState('dataConnection')
        if (data?.BSSID)       sendEvent (name: "BSSID", value: data.BSSID)                                    else if (locationType) device.deleteCurrentState('BSSID')
        if (data?.t)           sendEvent (name: "triggerSource", value: TRIGGER_TYPE[data.t])                  else if (locationType) device.deleteCurrentState('triggerSource')
        if (data?.m)           sendEvent (name: "monitoringMode", value: MONITORING_MODE[data.m])              else if (locationType) device.deleteCurrentState('monitoringMode')
    }
    // update the coordinates so the member tile can populate correctly
    if (data?.lat) sendEvent (name: "lat", value: data.lat)
    if (data?.lon) sendEvent (name: "lon", value: data.lon)
    // needed for the presence detection check -- if the phone holds onto the SSID, check if we switched to wifi
    if ((data?.SSID) && ((data?.conn) == "w"))  sendEvent (name: "SSID", value: data.SSID) else if (locationType) device.deleteCurrentState('SSID')
}

def updateAdditionalAttributes(member) {
    sendEvent( name: "imperialUnits", value: parent.isimperialUnits() )
    // process the additional status information
    if (member?.wifi != null) {
        sendEvent( name: "wifi", value: (member?.wifi ? "on" : "off") )
        if (member?.wifi == 0) {
            logDebug("Phone has WiFi turned off.  Please turn WiFi on.")
        }
    } else {
        device.deleteCurrentState('wifi')
    }
    // only display the extra phone fields if they are in a non-optimal state
    if (member?.ps == 1) {
        sendEvent( name: "batterySaver", value: "on" )
        logDebug("Phone is currently in battery saver mode")
    } else {
        device.deleteCurrentState('batterySaver')
    }
    if (member?.bo == 1) {
        sendEvent( name: "batteryOptimizations", value: "Optimized/Restricted" )
        logNonOptimalSettings("App setting: 'App battery usage' is 'Optimized' or 'Restricted'.  Please change to 'Unrestricted'")
    } else {
        device.deleteCurrentState('batteryOptimizations')
    }
    if (member?.hib == 1) {
        sendEvent( name: "hiberateAllowed", value: "App can pause" )
        logNonOptimalSettings("App setting: 'Pause app activity if unused' is 'Enabled'.  Please change to 'Disabled'")
    } else {
        device.deleteCurrentState('hiberateAllowed')
    }
    if ((member?.loc != null) && (member?.loc < 0)) {
        sendEvent( name: "locationPermissions", value: LOCATION_PERMISION["${member?.loc}"])
        logNonOptimalSettings("Location permissions currently set to '${LOCATION_PERMISION["${member?.loc}"]}'.  Please change to 'Allow all the time' and 'Use precise location'")
    } else {
        device.deleteCurrentState('locationPermissions')
    }
}

def getCurrentLocation(data) {
    // retrieves the current location from the data packet
    if (data.private) {
        currentLocation = DEFAULT_privateLocation
    } else {
        // if we have a transition event
        if (data._type == "transition") {
            currentLocation = data.desc
        } else {
            // default to display the street address if it was reported (or the default lat,lon if no geocodeing was sent from the app)
            currentLocation = data.streetAddress
            // if we are in a region stored in the app
            if (data.inregions) {
                locationList = ""
                data.inregions.each { place->
                    // filter off the +follow regions
                    if (place[0] != "+") {
                        locationList += "$place,"
                    }
                }
                if (locationList) {
                	// remove the trailing comma
	                currentLocation = locationList.substring(0, locationList.length() - 1)
                }
            }
        }
    }
    return (currentLocation)
}

def createTransitionEvent(dataRegion, dataEvent, dataTst) {
    // set the deadband to a future time
    state.transitionDeadband = dataTst + (notificationHysteresisSeconds == null ? DEFAULT_notificationHysteresisSeconds : notificationHysteresisSeconds)
    // skip duplicate transition events
    if ((TRANSITION_DIRECTION[dataEvent] != device.currentValue('transitionDirection')) || (dataRegion != device.currentValue('transitionRegion'))) {
        dataTime = new SimpleDateFormat("E h:mm a yyyy-MM-dd").format(new Date((long)dataTst * 1000))
        descriptionText = device.displayName +  " has ${TRANSITION_PHRASES[dataEvent]} " + dataRegion
        logDescriptionText("$descriptionText")

        sendEvent( name: "transitionRegion", value: dataRegion )
        sendEvent( name: "transitionTime", value: dataTime )
        sendEvent( name: "transitionDirection", value: TRANSITION_DIRECTION[dataEvent] )
        sendEvent( name: "sinceTime", value: dataTst )
        parent.generateTransitionNotification(state.memberName, TRANSITION_PHRASES[dataEvent], dataRegion, dataTime)
	} else {
    	logDebug("Skipping duplicate transition event '${dataEvent} ${dataRegion} at ${new SimpleDateFormat("E h:mm:ss a yyyy-MM-dd").format(new Date((long)dataTst * 1000))}'.")
	}
}

def createPresenceEvent(presenceState, dataTst) {
    // skip duplicate presence events
    if (device.currentValue("presence") != presenceState) {
        descriptionText = device.displayName + " is " + presenceState
        sendEvent (name: "presence", value: presenceState, descriptionText: descriptionText )
        logDescriptionText("$descriptionText")
        updateSinceTime(dataTst)
        generatePresenceTile()
    } else {
        logDebug("Skipping duplicate presence event '${state.memberName} ${presenceState}' at ${new SimpleDateFormat("E h:mm:ss a yyyy-MM-dd").format(new Date((long)dataTst * 1000))}.")
    }
}

def updateSinceTime (dataTst) {
    // update the timestamp and string date for the last location change
	sendEvent( name: "sinceTime", value: dataTst )
    sendEvent( name: "since", value: new SimpleDateFormat("E h:mm a yyyy-MM-dd").format(new Date((long)dataTst * 1000)) )
}

Boolean generateLocationEvent(member, homeName, data) {
// ADDED IN 1.8.4 for cleanup
if (state.memberPresence) state.remove("memberPresence")
// ADDED IN 1.8.4 for cleanup

    // update the driver version if necessary
    if (state.driverVersion != driverVersion()) {
        state.driverVersion = driverVersion()
    }
    // update the member name if necessary
    if (state.memberName != member.name) {
        state.memberName = member.name
    }
    // update the home name if necessary
    if (state.homeName != homeName) {
        state.homeName = homeName
    }
    if (state.transitionDeadband == null) state.transitionDeadband = 0

    // update the attributes
    updateAttributes(data)
    // update the additional attributes from the forked version
    updateAdditionalAttributes(member)

    //logDebug("Member Data: $data")
    if (data.private) {
        logDebug("Updating '${(data.event ? "Event ${data.event}" : (data.t ? TRIGGER_TYPE[data.t] : "Location"))}' presence for ${device.displayName} -- ${(data.memberAtHome ? "'present'" : "'not present'")}, accuracy: ${parent.displayMFtVal(data.acc)} ${parent.getSmallUnits()} ${(data?.SSID ? ", SSID: ${data.SSID}" : "")}")
    } else {
        logDebug("Updating '${(data.event ? "Event ${data.event}" : (data.t ? TRIGGER_TYPE[data.t] : "Location"))}' presence for ${device.displayName} -- ${(data.memberAtHome ? "'present'" : "'not present'")} (Home Wifi: ${data.memberWiFiHome}, High Accuracy: ${member.dynamicLocaterAccuracy}), " +
                 "${parent.displayKmMiVal(data.currentDistanceFromHome)} ${parent.getLargeUnits()} from Home, ${(data.batt != null ? "Battery: ${data.batt}%, ":"")}${(data.vel != null ? "Velocity: ${parent.displayKmMiVal(data.vel)} ${parent.getVelocityUnits()}, ":"")}" +
                 "accuracy: ${parent.displayMFtVal(data.acc)} ${parent.getSmallUnits() }" +
                 (debugLogAddresses ? ", Location: [${data.lat},${data.lon}] ${(data?.address ? ", Address: [${data.address}]" : "")} ${(data?.streetAddress ? ", Street Address: [${data.streetAddress}]" : "")} " : "") +
                 "${(data?.inregions ? ", Regions: ${data.inregions}" : "")} ${(data?.SSID ? ", SSID: ${data.SSID}" : "")} " )
    }

    // update the last location time
    sendEvent( name: "lastLocationtime", value: new SimpleDateFormat("E h:mm a yyyy-MM-dd").format(new Date()) )

    // get the required data for the current or pending update
    updateData = [
        desc: data.desc,
        event: data.event,
        _type: data._type,
        currentDistanceFromHome: data.currentDistanceFromHome,
        vel: data.vel,
        tst: data.tst,
        memberPresence: (data.memberAtHome ? "present" : "not present"),
        memberAtHome: data.memberAtHome,
        currentLocation: getCurrentLocation(data),
        private: data.private,
    ]

    // if transition message occurs, we are in the same region and the deadband has not expired
    if ((data?.desc == device.currentValue("transitionRegion")) && (data.tst < state.transitionDeadband)) {
        // set the deadband to a future time
        state.transitionDeadband = data.tst + (notificationHysteresisSeconds == null ? DEFAULT_notificationHysteresisSeconds : notificationHysteresisSeconds)
        // store the pending update
        state.pendingTransitionUpdateData = updateData
        // reschedule a update to trigger after the deadband expires
        unschedule(processScheduledTransitionEvent)
        runIn((state.transitionDeadband - data.tst), processScheduledTransitionEvent)
        if (!data.private) logDebug("Scheduling transition update for '${data.event} ${data?.desc} at ${new SimpleDateFormat("E h:mm:ss a yyyy-MM-dd").format(new Date((long)data.tst * 1000))}' to occur in ${(state.transitionDeadband - data.tst)} seconds.")
    } else {
        if (data._type == "transition") {
            // if we had a pending transition update process it first
            processScheduledTransitionEvent()
        }
        // send the latest notification
        processLocationEvent(updateData)
    }

    // if we missed a presence change and we don't have a hysteresis event pending
    if ((device.currentValue("presence") != updateData.memberPresence) && (data.tst > state.transitionDeadband)) {
        createTransitionEvent(state.homeName, (updateData.memberPresence == "present" ? "enter" : "leave"), data.tst)
        createPresenceEvent(updateData.memberPresence, updateData.tst)
        logDescriptionText("Correcting presence of '${state.memberName}' to '${updateData.memberPresence}'.")
    }

    return true
}

void processScheduledTransitionEvent() {
    // cancel any pending tranistion updates
    unschedule(processScheduledTransitionEvent)
	if (state.pendingTransitionUpdateData) processLocationEvent(state.pendingTransitionUpdateData)
    // clear the pending updates
    state.pendingTransitionUpdateData = ""
}

void processLocationEvent(data) {
    // mask private data
    if (!data.private) {
        // if we have a transition event
        if (data._type == "transition") {
            // create the notification event, update the transition and log the message
            createTransitionEvent(data.desc, data.event, data.tst)
        } else {
            // only log if there was a location change
            if (device.currentValue("location") != data.currentLocation) {
                if (logLocationChanges) log.info "${device.displayName} is at $data.currentLocation"
                updateSinceTime(data.tst)
            }
        }
	    sendEvent( name: "distanceFromHome", value: parent.displayKmMiVal(data.currentDistanceFromHome) )
    	sendEvent( name: "lastSpeed", value: (data?.vel ? parent.displayKmMiVal(data.vel).toInteger() : 0) )
    }
    // we only get .desc on a transition event
    if (state.homeName == data.desc) {
        // trigger a presence update
        createPresenceEvent(data.memberPresence, data.tst)
    }
    sendEvent( name: "location", value: data.currentLocation )
    generateMemberTile()
}

def generateTiles() {
    // we need one location to arrive and set the name before we can make the tiles
    if (state.memberName) {
        generateMemberTile()
        generatePastLocationsTile()
        generatePresenceTile()
    }
}

def generateMemberTile() {
    if (displayMemberTile) {
        sendEvent(name: "MemberLocation", value: parent.displayTile(CLOUD_URL_SOURCE, "membermap/${state.memberName.toLowerCase()}"), displayed: true)
    } else {
        device.deleteCurrentState('MemberLocation')
    }
}

def generatePastLocationsTile() {
    if (displayLastLocationTile) {
        sendEvent(name: "PastLocations", value: parent.displayTile(parent.recorderURLType(), "memberpastlocations/${state.memberName.toLowerCase()}"), displayed: true)
    } else {
        device.deleteCurrentState('PastLocations')
    }
}

def generatePresenceTile() {
    sendEvent(name: "PresenceTile", value: parent.displayTile(CLOUD_URL_SOURCE, "memberpresence/${state.memberName.toLowerCase()}"), displayed: true)
}

def generateMember(urlSource) {
    String htmlData = ""
    if (device.currentValue('location') != DEFAULT_privateLocation) {
        long sinceTimeMilliSeconds = device.currentValue("sinceTime")
        tileDate = new SimpleDateFormat("E h:mm a").format(new Date(sinceTimeMilliSeconds * 1000))

        htmlData += """
        <div style="width:100%;height:100%;margin:2px;font-family:arial">
            <div style="background:${(colorMemberTile ? ((device.currentValue("presence") == "present") ? "green" : "#b40000") : "#555555")}">
                <table style="width:100%;font-size:0.8em;color:white">
                    <tr>
                        <td align="left" width=11%>
                            ${parent.insertThumbnailObject(state.memberName, 35, true)}
                        </td>
                        <td align="center" width=79%>
                            ${device.currentValue("location")}</br>
                            ${tileDate}</br>
                        </td>
                        <td align="right" width=20%>
                            ${(colorMemberTile ? "" : ((device.currentValue("presence") == "present") ? "&#10004</br>Present" : "&#10008</br>Not Present"))}
                        </td>
                    </tr>
                </table>
            </div>
            <div style="height:calc(100vh - 125px);width:100%;position:relative;display:flex;justify-content:center;overflow:hidden;">
                <table style="position:absolute;width:calc(100% + 650px);height:100%">
                    <tr align="center">
                        <td>
                            <iframe src="https://maps.google.com/?q=${device.currentValue("lat").toString()},${device.currentValue("lon").toString()}&z=17&output=embed&" style="height:100%;width:100%;border:none;"></iframe>
                        </td>
                    </tr>
                </table>
            </div>
            <table style="width:100%;font-size:0.8em;color:white;background:#555555">
                <caption style="background:#555555">Last Update: ${device.currentValue("lastLocationtime")}</caption>
                <tr align="center">
                    <th width=25%>Distance</th>
                    ${(device.currentValue("lastSpeed") != null) ? "<th width=25%>Speed</th>" : ""}
                    ${(device.currentValue("battery") != null) ? "<th width=25%>Battery</th>" : ""}
                    ${(device.currentValue("dataConnection") != null) ? "<th width=25%>Data</th>" : ""}
                </tr>
                <tr align="center">
                    <td width=25%>${parent.displayKmMiVal(device.currentValue("distanceFromHome"))} ${parent.getLargeUnits()}</td>
                    ${(device.currentValue("lastSpeed") != null) ? "<td width=25%>${parent.displayKmMiVal(device.currentValue("lastSpeed"))} ${parent.getVelocityUnits()}</td>" : ""}
                    ${(device.currentValue("battery") != null) ? "<td width=25%>${device.currentValue("battery")} % ${(device.currentValue("batteryStatus") ? "</br>${device.currentValue("batteryStatus")}" : "")}</td>" : ""}
                    ${(device.currentValue("dataConnection") != null) ? "<td width=25%>${device.currentValue("dataConnection")}</td>" : ""}
                </tr>
            </table>
            ${generateScriptData(urlSource)}
        </div>"""
    }

    return (htmlData)
}

def generatePastLocations() {
    String htmlData = ""
    if (parent.getRecorderURL() && (device.currentValue('location') != DEFAULT_privateLocation)) {
        // split the topic into it's elements.  user is [1], device is [2].
        topicElements = parent.splitTopic(device.currentValue('sourceTopic').toLowerCase())

        htmlData += """
        <div style="width:100%;height:100%;margin:4px;font-family:arial">
            <table style="width:100%;color:white;font-size:0.8em;background:#555555">
                <tr>
                    <td align="left" width=11%>
                        ${parent.insertThumbnailObject(state.memberName, 35, true)}
                    </td>
                    <td align="right" width=21%">
                        Points <input type="radio" id="id-points" value="" onchange="updateUrl()"></br>
                        Lines <input type="radio" id="id-lines" value="" onchange="updateUrl()"></br>
                    </td>
                    <td align="right" width=68%">
                        Start <input type="datetime-local" id="id-startDate" value="" onchange="updateUrl()"></br>
                        End <input type="datetime-local" id="id-endDate" value="" onchange="updateUrl()">
                    </td>
                </tr>
            </table>
            <table style="width:100%;height:calc(100% - 50px)">
                <tr align="center">
                    <td><iframe id="id-iframe" src="" style="height:100%;width:100%;border:none;"></iframe></td>
                </tr>
            </table>
        </div>
        <script>
            // Function to update the URL with the selected date
            function updateUrl() {
                const startDate = new Date(document.getElementById("id-startDate").value).toISOString().slice(0, 16);
                const endDate = new Date(document.getElementById("id-endDate").value).toISOString().slice(0, 16);
                const urlPath = "${parent.getRecorderURL()}/map/index.html?from=" + startDate + "&to=" + endDate + "&format=" + (linesRadio.checked ? "linestring" : "geojson") + "&user=${topicElements[1]}&device=${topicElements[2]}";
                const iframe = document.getElementById("id-iframe");
                iframe.src = urlPath;
            }

            const pointsRadio = document.getElementById("id-points");
            const linesRadio = document.getElementById("id-lines");
            // set the initial state
            if (${lastLocationViewTracks}) {
                pointsRadio.checked;
                linesRadio.checked = !pointsRadio.checked;
            } else {
                linesRadio.checked;
                pointsRadio.checked = !linesRadio.checked;
            }
            // Add event listeners to toggle state
            pointsRadio.addEventListener('click', () => {
                linesRadio.checked = !pointsRadio.checked;
            });
            linesRadio.addEventListener('click', () => {
                pointsRadio.checked = !linesRadio.checked;
            });

            // Set the date picker to the current date
		    const endTime = new Date()
    		const startTime = new Date()
            const timeZoneHourOffset = endTime.getTimezoneOffset() / 60;
            // set the time date selectors with the proper timezone
	    	endTime.setHours(endTime.getHours() - timeZoneHourOffset)
            startTime.setHours(startTime.getHours() - timeZoneHourOffset - ${(pastLocationSearchWindow ? pastLocationSearchWindow*24 : DEFAULT_pastLocationSearchWindow*24)})
            document.getElementById("id-startDate").value = startTime.toISOString().slice(0, 16);
            document.getElementById("id-endDate").value = endTime.toISOString().slice(0, 16);
            // trigger the update to the start URL
            window.onload = updateUrl;
        </script>"""
    }

    return (htmlData)
}

def generatePresence(urlSource) {
    long sinceTimeMilliSeconds = device.currentValue("sinceTime")
    sinceDate = new SimpleDateFormat("E h:mm a").format(new Date(sinceTimeMilliSeconds * 1000))

    String htmlData = """
    <div style="width:100%;height:100%;margin:4px;background:${((device.currentValue('presence') == "present") ? "green" : "#b40000")}">
        <table style="height:100%;width:100%;color:white;font-size:0.8em;font-family:arial">
            <tr align="center" height=33%>
                <td valign="center">${device.currentValue("location")}</td>
            </tr>
            <tr align="center" height=33%>
                <td valign="center">
                    ${parent.insertThumbnailObject(state.memberName, 70, true)}
                </td>
            </tr>
            <tr align="center" height=33%>
                <td valign='center'>${sinceDate}</td>
            </tr>
        </table>
        ${generateScriptData(urlSource)}
    </div>"""

    return (htmlData)
}

def generateScriptData(urlSource) {
    String htmlData = """
	<script>
		lastUpdate = sessionStorage.getItem('lastReportTime') || 0;

		function updateTile() {
			const postData = {};
			postData["action"] = "update";
			postData["payload"] = "${state.memberName}";
			sendDataToHub(postData)
		};

		function sendDataToHub(postData) {
			fetch("${parent.getAttributeURL(urlSource, "apidata")}", {
				method: "POST",
				body: JSON.stringify(postData),
				headers: {
					"Content-type": "application/json; charset=UTF-8"
				}
			})
			.then(response => response.json())
			.then(data => {
				if (lastUpdate != data.lastReportTime) {
					sessionStorage.setItem('lastReportTime', data.lastReportTime);
					// refresh the window
					location.reload(true);
				}
			})
			.catch(error => { console.error('Error fetching data:', error); })
		};

		// Poll for member data every 5000ms
		setInterval(() => {
			updateTile();
		}, 5000);
	</script>"""

    return (htmlData)
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

private logNonOptimalSettings(msg) {
    if (parent.childGetWarnOnNonOptimalSettings()) {
        logWarn(msg)
    } else {
        logDebug(msg)
    }
}
