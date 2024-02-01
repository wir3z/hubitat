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
 *      BSSID:11:22:33:44:55:66, 	    // WiFi AP MAC
 *      SSID:WIFIAP,     			    // WiFi AP SSID
 *      t:u, 						    // trigger: p=ping, c=region, r=reportLocation, u=manual
 *      m:1, 						    // identifies the monitoring mode at which the message is constructed (significant=1, move=2)
 *
 *      // added to packet
 *      currentDistanceFromHome:0.234,  // distance from home in current units
 *      private:false                   // if true, user is configured to not share their information, only their current presence
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
 **/

import java.text.SimpleDateFormat
import groovy.transform.Field

def driverVersion() { return "1.7.1" }

@Field static final Map MONITORING_MODE = [ 0: "Unknown", 1: "Significant", 2: "Move" ]
@Field static final Map BATTERY_STATUS = [ 0: "Unknown", 1: "Unplugged", 2: "Charging", 3: "Full" ]
@Field static final Map DATA_CONNECTION = [ "w": "WiFi", "m": "Mobile" ]
@Field static final Map TRIGGER_TYPE = [ "p": "Ping", "c": "Region", "r": "Report Location", "u": "Manual", "b": "Beacon", "t": "Timer", "v": "Monitoring", "l": "Location" ]
@Field static final Map PRESENCE_TILE_BATTERY_FIELD = [ 0: "Battery %", 1: "Current Location and Since Time", 2: "Distance from Home", 3: "Last Speed", 4: "Battery Status (Unplugged/Charging/Full)", 5: "Data Connection (WiFi/Mobile)", 6: "Update Trigger (Ping/Region/Report Location/Manual)", 7: "Distance from Home and Since Time" ]
@Field static final Map LOCATION_PERMISION = [ "0": "Background - Fine", "-1": "Background - Coarse", "-2": "Foreground - Fine", "-3": "Foreground - Coarse", "-4": "Disabled" ]

@Field Boolean DEFAULT_presenceTileBatteryField = 0
@Field Boolean DEFAULT_displayExtendedAttributes = true
@Field Boolean DEFAULT_displayMemberTile = false
@Field Boolean DEFAULT_colorMemberTile = true
@Field Boolean DEFAULT_descriptionTextOutput = true
@Field Boolean DEFAULT_debugOutput = false
@Field Boolean DEFAULT_logLocationChanges = false
@Field String  DEFAULT_privateLocation = "private"
@Field Boolean DEFAULT_createNotificationOnTransitionEnter = false
@Field Boolean DEFAULT_createNotificationOnTransitionLeave = false

metadata {
  definition (
      name:        "OwnTracks Driver", 
      namespace:   "lpakula", 
      author:      "Lyle Pakula", 
      importUrl:   "https://raw.githubusercontent.com/wir3z/hubitat/main/owntracks-hubitat/OwnTracks%20Driver.groovy"
  ) {
        capability "Actuator"
        capability "Presence Sensor"

        command    "arrived"
        command    "departed"
        command    "createMemberTile"

        attribute  "location", "string"
        attribute  "transitionRegion", "string"
        attribute  "transitionTime", "string"
        attribute  "transitionDirection", "string"
        attribute  "since", "string"
        attribute  "battery", "string"
        attribute  "lastSpeed", "number"
        attribute  "distanceFromHome", "number"
        attribute  "wifi", "string"
        attribute  "lastLocationtime", "string"
        attribute  "imperialUnits", "string"

        attribute  "batterySaver", "string"
        attribute  "hiberateAllowed", "string"
        attribute  "batteryOptimizations", "string"
        attribute  "locationPermissions", "string"

        attribute  "imageURL", "string"
        attribute  "MemberLocation", "string"
      
        // extended attributes
        attribute  "batteryPercent", "number"
        attribute  "lat", "number"
        attribute  "lon", "number"
        attribute  "accuracy", "number"
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
    input name: "presenceTileBatteryField", type: "enum", title: "What is displayed on the presence tile battery field", required: true, options: PRESENCE_TILE_BATTERY_FIELD, defaultValue: DEFAULT_presenceTileBatteryField
    input name: "displayExtendedAttributes", type: "bool", title: "Display extended location attributes", defaultValue: DEFAULT_displayExtendedAttributes
    input name: "displayMemberTile", type: "bool", title: "Create a HTML MemberTile", defaultValue: DEFAULT_displayMemberTile
    input name: "colorMemberTile", type: "bool", title: "Change MemberTile background color based on presence", defaultValue: DEFAULT_colorMemberTile
    input name: "createNotificationOnTransitionEnter", type: "bool", title: "Create a notification if member enters a region", defaultValue: DEFAULT_createNotificationOnTransitionEnter
    input name: "createNotificationOnTransitionLeave", type: "bool", title: "Create a notification if member leaves a region", defaultValue: DEFAULT_createNotificationOnTransitionLeave

    input name: "descriptionTextOutput", type: "bool", title: "Enable Description Text logging", defaultValue: DEFAULT_descriptionTextOutput
    input name: "debugOutput", type: "bool", title: "Enable Debug Logging", defaultValue: DEFAULT_debugOutput
    input name: "logLocationChanges", type: "bool", title: "Enable Logging of location changes", defaultValue: DEFAULT_logLocationChanges
}

def installed() {
    log.info "${device.name}: Location Tracker User Driver Installed"
    state.sinceTime = now()
    state.driverVersion = driverVersion()
    updated()
}

def updated() {
    logDescriptionText("${device.name}: Location Tracker User Driver has been Updated")
    // generate / remove the member tile as required
    generateMemberTile()
    // remove the extended attributes if not enabled
    if (!displayExtendedAttributes) {
        deleteExtendedAttributes(false)
    }
}

def deleteExtendedAttributes(makePrivate) {
    device.deleteCurrentState('batteryPercent')
    device.deleteCurrentState('accuracy')
    device.deleteCurrentState('verticalAccuracy')
    device.deleteCurrentState('altitude')
    device.deleteCurrentState('sourceTopic')
    device.deleteCurrentState('dataConnection')
    device.deleteCurrentState('batteryStatus')
    device.deleteCurrentState('BSSID')
    device.deleteCurrentState('triggerSource')
    device.deleteCurrentState('monitoringMode')
    if (makePrivate) {
        device.deleteCurrentState('location')
        device.deleteCurrentState('transition')
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
}

def arrived() {
    descriptionText = device.displayName +  " has arrived"
    sendEvent (name: "presence", value: "present", descriptionText: descriptionText)
    logDescriptionText("$descriptionText")
    generateMemberTile()
}

def departed() {
    descriptionText = device.displayName +  " has departed"
    sendEvent (name: "presence", value: "not present", descriptionText: descriptionText)
    logDescriptionText("$descriptionText")
    generateMemberTile()
}

def createMemberTile() {
    generateMemberTile()
}

def updateAttributes(data, locationType) {
    // remove the attributes if not enabled or the member is private
    if (data.private || !displayExtendedAttributes) {
         deleteExtendedAttributes(true)    
    } 
    // display the extended attributes if they were received, but only allow them to be removed on non-tranisition eve
    if (!data.private) {
        // requires a valid location report
        if (data.tst != 0) {
            if (data?.acc)     sendEvent (name: "accuracy", value: parent.displayMFtVal(data.acc))         else if (locationType) device.deleteCurrentState('accuracy')
            if (data?.vac)     sendEvent (name: "verticalAccuracy", value: parent.displayMFtVal(data.vac)) else if (locationType) device.deleteCurrentState('verticalAccuracy')
            if (data?.alt)     sendEvent (name: "altitude", value: parent.displayMFtVal(data.alt))         else if (locationType) device.deleteCurrentState('altitude')
            if (data?.address) {
                sendEvent (name: "address", value: data.address)                           
                sendEvent (name: "streetAddress", value: data.streetAddress)
            } else {
                if (locationType) device.deleteCurrentState('address')
                if (locationType) device.deleteCurrentState('streetAddress')
            }
        }
        // can be updated all the time
        if (data?.batt)    sendEvent (name: "batteryPercent", value: data.batt)                            else if (locationType) device.deleteCurrentState('batteryPercent')
        if (data?.topic)   sendEvent (name: "sourceTopic", value: data.topic)                              else if (locationType) device.deleteCurrentState('sourceTopic')
        if (data?.bs)      sendEvent (name: "batteryStatus", value: BATTERY_STATUS[data.bs])               else if (locationType) device.deleteCurrentState('batteryStatus')
        if (data?.conn)    sendEvent (name: "dataConnection", value: DATA_CONNECTION[data.conn])           else if (locationType) device.deleteCurrentState('dataConnection')
        if (data?.BSSID)   sendEvent (name: "BSSID", value: data.BSSID)                                    else if (locationType) device.deleteCurrentState('BSSID')
        if (data?.t)       sendEvent (name: "triggerSource", value: TRIGGER_TYPE[data.t])                  else if (locationType) device.deleteCurrentState('triggerSource')
        if (data?.m)       sendEvent (name: "monitoringMode", value: MONITORING_MODE[data.m])              else if (locationType) device.deleteCurrentState('monitoringMode')
            
        if (locationType) {
            // process the additional setting information
            if (data?.wifi) {
                sendEvent( name: "wifi", value: (data?.wifi ? "on" : "off") )
                if (data?.wifi == 0) {
                    logDebug("Phone has WiFi turned off.  Please turn WiFi on.")
                }
            } else {
                device.deleteCurrentState('wifi')
            }
            // only display the extra phone fields if they are in a non-optimal state
            if (data?.ps == 1) {
                sendEvent( name: "batterySaver", value: "on" )
                logDebug("Phone is currently in battery saver mode")
            } else {
                device.deleteCurrentState('batterySaver')
            }
            if (data?.bo == 1) {
                sendEvent( name: "batteryOptimizations", value: "Optimized/Restricted" )
                logNonOptimalSettings("App settting: 'App battery usage' is 'Optimized' or 'Restricted'.  Please change to 'Unrestricted'")
            } else {
                device.deleteCurrentState('batteryOptimizations')
            }
            if (data?.hib == 1) {
                sendEvent( name: "hiberateAllowed", value: "App can pause" )
                logNonOptimalSettings("App setting: 'Pause app activity if unused' is 'Enabled'.  Please change to 'Disabled'")
            } else {
                device.deleteCurrentState('hiberateAllowed')
            }
            if (data?.loc > 0) {
                sendEvent( name: "locationPermissions", value: LOCATION_PERMISION["${data?.loc}"])
                logNonOptimalSettings("Location permissions currently set to '${LOCATION_PERMISION["${data?.loc}"]}'.  Please change to 'Allow all the time' and 'Use precise location'")
            } else {
                device.deleteCurrentState('locationPermissions')
            }
        }
    }
    
    // needed for the presence detection check
    if (data?.SSID)  sendEvent (name: "SSID", value: data.SSID) else if (locationType) device.deleteCurrentState('SSID')
}

Boolean generatePresenceEvent(data) {
    // update the driver version if necessary
    if (state.driverVersion != driverVersion()) {
        state.driverVersion = driverVersion()
    }
    
    // defaults for the private member case
    descriptionText = ""
    currentLocation = DEFAULT_privateLocation
    
    //logDebug("Member Data: $data")
    if (data.private) {
        logDebug("Updating '${(data.event ? "Event ${data.event}" : (data.t ? TRIGGER_TYPE[data.t] : "Location"))}' presence for ${device.displayName} -- accuracy: ${parent.displayMFtVal(data.acc)} ${parent.getSmallUnits()}")
    } else {
        logDebug("Updating '${(data.event ? "Event ${data.event}" : (data.t ? TRIGGER_TYPE[data.t] : "Location"))}' presence for ${device.displayName} -- ${parent.displayKmMiVal(data.currentDistanceFromHome)} ${parent.getLargeUnits()} from Home, ${(data.batt ? "Battery: ${data.batt}%, ":"")}${(data.vel ? "Velocity: ${parent.displayKmMiVal(data.vel)} ${parent.getVelocityUnits()}, ":"")}accuracy: ${parent.displayMFtVal(data.acc)} ${parent.getSmallUnits() }, Location: [${data.lat},${data.lon}] ${(data?.address ? ", Address: [${data.address}]" : "")} ${(data?.streetAddress ? ", Street Address: [${data.streetAddress}]" : "")} ${(data?.inregions ? ", Regions: ${data.inregions}" : "")} ${(data?.SSID ? ", SSID: ${data.SSID}" : "")}   ")
    }    

    // update the last location time
    locationTime = new SimpleDateFormat("E h:mm a yyyy-MM-dd").format(new Date())
    sendEvent( name: "lastLocationtime", value: locationTime )
    sendEvent( name: "imperialUnits", value: parent.isimperialUnits() )
    
    // update the attributes - only allow attribute deletion on location updates
    updateAttributes(data, (data._type == "location"))
    
    // if we get a blank timestamp, then the phone has no location or this is a ping with high inaccuracy, so do not update any location fields
    if (data.tst != 0) {
        // only update the presence for 'home'
        if (data.currentDistanceFromHome == 0) {
            memberPresence = "present"
        } else {
            memberPresence = "not present"
        }

        // only update the time if there was a state change
        if (previousPresence != memberPresence) {
            state.sinceTime = data.tst
        }

        // update the coordinates so the member tile can populate correctly
        if (data?.lat) sendEvent (name: "lat", value: data.lat) else if (allowAttributeDelete) device.deleteCurrentState('lat')
        if (data?.lon) sendEvent (name: "lon", value: data.lon) else if (allowAttributeDelete) device.deleteCurrentState('lon')       
        
        // only log additional data if the user is not marked as private
        if (!data.private) {
            // if we have a tranistion event
            if (data._type == "transition") {
                currentLocation = data.desc
                if (data.event == "enter") {
                    currentTransition = "arrived $data.desc"
                    if (createNotificationOnTransitionEnter) parent.generateTransitionNotification(device.displayName, data.desc, data.event, locationTime)
                } else {
                    currentTransition = "left $data.desc"
                    if (createNotificationOnTransitionLeave) parent.generateTransitionNotification(device.displayName, data.desc, data.event, locationTime)
                }

                descriptionText = device.displayName +  " has " + currentTransition
                logDescriptionText("$descriptionText")
                // update the transition
                sendEvent( name: "transitionRegion", value: data.desc )	
                sendEvent( name: "transitionTime", value: locationTime )	
                sendEvent( name: "transitionDirection", value: data.event )	
            } else {
                // if we are in a region stored in the app
                if (data.inregions) {
                    locationList = ""
                    data.inregions.each { place->
                        // filter off the +follow regions
                        if (place[0] != "+") {
                            locationList += "$place,"
                        }
                    }
                    // remove the trailing comma
                    currentLocation = locationList.substring(0, locationList.length() - 1) 
                } else {
                    // display the street address if it was reported (or the default lat,lon if no geocodeing was sent from the app)
                    currentLocation = data.streetAddress
                }
                descriptionText = device.displayName +  " is at " + currentLocation
    

                // only log if there was a valid time, a location change and log changes is enabled
                if ((data.tst != 0) && (device.currentValue("location") != currentLocation)) {
                    if (logLocationChanges) log.info "$descriptionText"
                    state.sinceTime = data.tst
                }
            }
    
            long sinceTimeMilliSeconds = state.sinceTime
            sinceDate = new SimpleDateFormat("E h:mm a yyyy-MM-dd").format(new Date(sinceTimeMilliSeconds * 1000))
            tileDate = new SimpleDateFormat("E h:mm a").format(new Date(sinceTimeMilliSeconds * 1000))

            sendEvent( name: "since", value: sinceDate )
            sendEvent( name: "distanceFromHome", value:  parent.displayKmMiVal(data.currentDistanceFromHome) )
            if (data?.vel) {
                sendEvent( name: "lastSpeed", value:  parent.displayKmMiVal(data.vel).toInteger() )
            } else {
                sendEvent( name: "lastSpeed", value:  0 )
            } 

            // we are using the battery field on the presence tile for selectable display
            switch (presenceTileBatteryField) {
                case "0":
                    batteryField = (data?.batt ? data.batt : "")
                break
                case "1":
                    batteryField = currentLocation + " - " + tileDate
                break
                case "2":
                    batteryField = parent.displayKmMiVal(data.currentDistanceFromHome) + " ${parent.getLargeUnits()} from Home"
                break
                case "3":
                    batteryField = (data?.vel ? parent.displayKmMiVal(data.vel) + " ${parent.getVelocityUnits()}" : "")
                break
                case "4":
                    batteryField = (data?.bs ? BATTERY_STATUS[data.bs] : "")
                break
                case "5":
                    batteryField = (data?.conn ? DATA_CONNECTION[data.conn] : "")
                break
                case "6":
                    batteryField = (data?.t ? TRIGGER_TYPE[data.t] : "")
                break
                case "7":
                    batteryField = "${parent.displayKmMiVal(data.currentDistanceFromHome)} ${parent.getLargeUnits()} from Home - " + tileDate
                break
            }

            // deal with the cases where the above data might not come in a particular event, so leave the previous event
            if (batteryField) sendEvent( name: "battery", value: batteryField  )
        
            // create the HTML member tile if it's enabled and allowed -- schedule for 1-second so that the attributes are saved
            runIn(1, generateMemberTile)
        }
        
        // allowed all the time
        sendEvent( name: "presence", value: memberPresence, descriptionText: descriptionText)
        sendEvent( name: "location", value: currentLocation )
    }
   
    return true
}

def generateMemberTile() {
    if (displayMemberTile && (device.currentValue('location') != DEFAULT_privateLocation)) {
        long sinceTimeMilliSeconds = state.sinceTime
        tileDate = new SimpleDateFormat("E h:mm a").format(new Date(sinceTimeMilliSeconds * 1000))        
        
        String tiledata = "";
        tiledata += '<div style="width:100%;height:100%;font-size:0.7em">'
        
        if (colorMemberTile) {
            tiledata += '<div style="background:' + ((device.currentValue('presence') == "present") ? 'green">' : '#b40000">')
        } else {
            tiledata += '<div>'
        }
        tiledata += '<table align="center" style="width:100%">'          
        tiledata += '<tr>'
        
        if (device.currentValue('imageURL') != "false") {
            tiledata += '<td width=20%><img src="' + device.currentValue('imageURL') + '" alt="' + device.displayName + '" width="35" height="35"></td>'
        } else {
            tiledata += '<td width=20%></td>'
        }
        tiledata += '<td width=60% style="padding-top:15px;">'
        tiledata += "${device.currentValue('location')}</br>"
        tiledata += "Since: ${tileDate}</br>"
        tiledata += ((device.currentValue('presence') == "present") ? 'Present' : 'Not Present')
        tiledata += '</td>'
        tiledata += '<td></td>'
        tiledata += '</tr>'
        tiledata += '</table>'
        tiledata += '</div>'

        // mobile shows full 4x height tile in portrait, grey banner at bottom in landscape
//        tiledata += '<table align="center" style="width:100%;height:calc(100% - 250px)">'
        // mobile has bottom metrics hidden in 4x height tile in portrait, full screen in landscape
        tiledata += '<table align="center" style="width:100%;height:calc(100% - 185px)">'
        tiledata += '<tr>'
        tiledata += "<td><iframe src='https://maps.google.com/?q=${device.currentValue('lat').toString()},${device.currentValue('lon').toString()}&output=embed&' style='height:100%;width:100%;border:none;'></iframe></td>"
        tiledata += '</tr>'
        tiledata += '</table>'

        tiledata += '<table align="center" style="width:100%;padding-bottom:15px">'     
        tiledata += "<caption>Last Update: ${device.currentValue('lastLocationtime')}</caption>"
        tiledata += '<tr>'
        tiledata += '<th width=25%>Distance</th>'
        if (device.currentValue('lastSpeed') != null) tiledata += '<th width=25%>Speed</th>'
        if (device.currentValue('batteryPercent') != null) tiledata += '<th width=25%>Battery</th>'
        if (device.currentValue('dataConnection') != null) tiledata += '<th width=25%>Data</th>'
        tiledata += '</tr>'
 
        tiledata += '<tr>'
        tiledata += "<td width=25%>${parent.displayKmMiVal(device.currentValue('distanceFromHome'))} ${parent.getLargeUnits()}</td>"
        if (device.currentValue('lastSpeed') != null)  tiledata += "<td width=25%>${parent.displayKmMiVal(device.currentValue('lastSpeed'))} ${parent.getVelocityUnits()}</td>"
        if (device.currentValue('batteryPercent') != null) tiledata += "<td width=25%>${device.currentValue('batteryPercent')} %" + (device.currentValue('batteryStatus') ? "</br>${device.currentValue('batteryStatus')}" : "") + "</td>"
        if (device.currentValue('dataConnection') != null) tiledata += "<td width=25%>${device.currentValue('dataConnection')}</td>"
        tiledata += '</tr>'
        tiledata += '</table>'
        
        tiledata += '</div>'
        
        // deal with the 1024 byte attribute limit
        if ((tiledata.length() + 11) > 1024) {
            tiledata = "Too much data to display.</br></br>Exceeds maximum tile length by " + ((tiledata.length() + 11) - 1024) + " characters."
        }
 
        sendEvent(name: "MemberLocation", value: tiledata, displayed: true)
    } else {
        device.deleteCurrentState('MemberLocation')
    }
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
