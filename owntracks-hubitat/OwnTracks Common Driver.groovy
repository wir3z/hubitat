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
 *  Allows common across friends operations for the OwnTracks main app.
 *
 *  Author: Lyle Pakula (lpakula)
 *
 *  Changelog:
 *  Version    Date            Changes
 *  1.7.0      2024-02-07      - Created common driver for devices that cover all members.  Added a friends location tile.
 *  1.7.1      2024-02-15      - Fixed friends tile margin.
 *  1.7.2      2024-02-17      - Fixed friends tile text when thumbnails are missing.
 *  1.7.3      2024-02-25      - Changed the friends tile name to 'RecorderFriendsLocation'.  Added a 'GoogleFriendsLocation' tile attribute.
 *  1.7.4      2024-03-03      - Updated map link.  Refactored tile layouts.
 *  1.7.5      2024-03-05      - Added dynamic support for cloud recorder URL.
 *  1.7.6      2024-03-15      - Added configuration map tile.
 *  1.7.7      2024-03-23      - Changed the GoogleFriendsLocation tile generation.
 **/

import java.text.SimpleDateFormat
import groovy.transform.Field

def driverVersion() { return "1.7.7" }

@Field Boolean DEFAULT_displayFriendsTile = false

metadata {
  definition (
      name:        "OwnTracks Common Driver",
      namespace:   "lpakula",
      author:      "Lyle Pakula",
      importUrl:   "https://raw.githubusercontent.com/wir3z/hubitat/main/owntracks-hubitat/OwnTracks%20Common%20Driver.groovy"
  ) {
        capability "Actuator"
        capability "Presence Sensor"
        capability "Momentary"

        attribute  "RecorderFriendsLocation", "string"
        attribute  "GoogleFriendsLocation", "string"
        attribute  "ConfigurationMap", "string"
    }
}

preferences {
    input name: "displayRecorderFriendsLocation", type: "bool", title: "Create an HTML 'RecorderFriendsLocation' tile for all members current location (<b>Requires OwnTracks Recorder</b>)", defaultValue: DEFAULT_displayFriendsTile
    input name: "displayGoogleFriendsLocation", type: "bool", title: "Create an HTML 'GoogleFriendsLocation' tile for all members current location (<b>Requires Google Map API key</b>)", description: "<i>Add a 'Momentary' attribute to the dashboard for the push button refresh.</i>", defaultValue: DEFAULT_displayFriendsTile

    input name: "descriptionTextOutput", type: "bool", title: "Enable Description Text logging", defaultValue: DEFAULT_descriptionTextOutput
    input name: "debugOutput", type: "bool", title: "Enable Debug Logging", defaultValue: DEFAULT_debugOutput
}

def installed() {
    log.info "${device.name}: Location Tracker Common Driver Installed"
    state.driverVersion = driverVersion()
    updated()
}

def updated() {
    logDescriptionText("${device.name}: Location Tracker Common Driver has been Updated")
    // generate / remove the member tile as required
    push()
}

def push() {
    // update the driver version if necessary
    if (state.driverVersion != driverVersion()) {
        state.driverVersion = driverVersion()
    }

    generateRecorderFriendsLocationTile()
    generateGoogleFriendsLocationTile()
    generateConfigMapTile()
}

def generateRecorderFriendsLocationTile() {
    if (displayRecorderFriendsLocation) {
        sendEvent(name: "RecorderFriendsLocation", value: parent.displayTile(parent.recorderURLType(), "recordermap"), displayed: true)
    } else {
        device.deleteCurrentState('RecorderFriendsLocation')
    }
}

def generateGoogleFriendsLocationTile() {
    if (displayGoogleFriendsLocation) {
        sendEvent(name: "GoogleFriendsLocation", value: parent.displayTile("cloud", "googlemap"), displayed: true)
    } else {
        device.deleteCurrentState('GoogleFriendsLocation')
    }
}

def generateConfigMapTile() {
    if (parent.getGoogleMapsAPIKey()) {
        sendEvent(name: "ConfigurationMap", value: parent.displayTile("cloud", "configmap"), displayed: true)
    } else {
        device.deleteCurrentState('ConfigurationMap')
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
