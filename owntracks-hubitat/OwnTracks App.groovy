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
 *  OwnTracks Application Controller
 *
 *  Connects OwnTracks push events to virtual presence drivers.  This integration is using the HTTP method from OwnTracks (not MQTT)
 *
 *  Documentation:  https://owntracks.org/booklet/
 *  App:            https://github.com/owntracks/android/
 *  Recorder:       https://github.com/owntracks/recorder
 *
 *  Author: Lyle Pakula (lpakula)
 *
 *  Changelog:
 *  Version    Date            Changes
 *  1.6.4      2024-01-07      - Fixed location option defaults not being displayed.  Push the hubitat location to the region list for each mobile user. Added instructions for thumbnail, card and recorder installation.
 *
 */

import groovy.transform.Field
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonBuilder

def appVersion() { return "1.6.4" }

@Field static final Map BATTERY_STATUS = [ "0": "Unknown", "1": "Unplugged", "2": "Charging", "3": "Full" ]
@Field static final Map DATA_CONNECTION = [ "w": "WiFi", "m": "Mobile" ]
@Field static final Map TRIGGER_TYPE = [ "p": "Ping", "c": "Region", "r": "Report Location", "u": "Manual" ]
@Field static final Map TOPIC_FORMAT = [ 0: "topicSource", 1: "userName", 2: "deviceID", 3: "eventType" ]
@Field static final Map LOCATOR_PRIORITY = [ 0: "NO_POWER (best accuracy with zero power consumption)", 1: "LOW_POWER (city level accuracy)", 2: "BALANCED_POWER (block level accuracy based on Wifi/Cell)", 3: "HIGH_POWER (most accurate accuracy based on GPS)" ]
//@Field static final Map DYNAMIC_INTERVALS = [ "pegLocatorFastestIntervalToInterval": false, "locatorPriority": 3, "locatorDisplacement": 50, "locatorInterval": 60 ]
@Field static final Map DYNAMIC_INTERVALS = [ "pegLocatorFastestIntervalToInterval": false, "locatorPriority": 3 ]
//@Field static final Map MONITORING_MODES = [ -1: "Quiet (no events published)", 0: "Manual (user triggered events)", 1: "Significant (standard tracking using Wifi/Cell)", 2: "Move (permanent tracking using GPS)" ]
@Field static final Map MONITORING_MODES = [ 0: "Manual (user triggered events)", 1: "Significant (standard tracking using Wifi/Cell)", 2: "Move (permanent tracking using GPS)" ]

// Main defaults
@Field String  CHILDPREFIX = "OwnTracks - "
@Field String  MQTT_TOPIC_PREFIX = "owntracks"
@Field String  HUBITAT_LOCATION = "[Hubitat Location]"
@Field Number  INVALID_COORDINATE = 999
@Field Number  DEFAULT_RADIUS = 75
@Field Number  DEFAULT_regionHighAccuracyRadius = 750
// Mobile app location defaults
@Field Number  DEFAULT_monitoring = 1
@Field Number  DEFAULT_locatorPriority = 2
@Field Number  DEFAULT_moveModeLocatorInterval = 30
@Field Number  DEFAULT_locatorDisplacement = 50
@Field Number  DEFAULT_locatorInterval = 60
@Field Number  DEFAULT_ignoreInaccurateLocations = 50
@Field Number  DEFAULT_ignoreStaleLocations = 7.0
@Field Number  DEFAULT_ping = 30
@Field Boolean DEFAULT_pegLocatorFastestIntervalToInterval = true
// Mobile app display defaults
@Field Boolean DEFAULT_imageCards = false
@Field Boolean DEFAULT_replaceTIDwithUsername = true
@Field Boolean DEFAULT_notificationEvents = true
@Field Boolean DEFAULT_pubExtendedData = true
@Field Boolean DEFAULT_enableMapRotation = true
@Field Boolean DEFAULT_showRegionsOnMap = true
@Field Boolean DEFAULT_notificationLocation = false
@Field Boolean DEFAULT_notificationGeocoderErrors = false

definition(
    name: "OwnTracks",
    namespace: "lpakula",
    author: "Lyle Pakula",
    description: "OwnTracks app connects your OwnTracks mobile app to Hubitat Elevation for virtual presence triggers",
    importUrl: "https://raw.githubusercontent.com/wir3z/hubitat/main/owntracks-hubitat/OwnTracks%20App.groovy",
    category: "",
    iconUrl: "",
    iconX2Url: "",
    oauth: [displayName: "OwnTracks", displayLink: "https://owntracks.org/"],
    singleInstance: true,
)

preferences {
    page(name: "mainPage")
    page(name: "installationInstructions")
    page(name: "thumbnailCreationInstructions")
    page(name: "configureRecorder")
    page(name: "recorderInstallationInstructions")
    page(name: "configureLocation")
    page(name: "configureDisplay")
    page(name: "configureRegions")
    page(name: "addRegions")
    page(name: "editRegions")
    page(name: "deleteRegions")
    page(name: "deleteMembers")
}

def mainPage() {
    // clear the setting fields
    clearSettingFields()
    // initialize all fields if they are undefined
    initialize()
    def oauthStatus = ""
    //enable OAuth in the app settings or this call will fail
    try{
        if (!state.accessToken) {
            createAccessToken()
        }
    }
    catch (e) {
        oauthStatus = "Edit Apps Code -> OwnTracks.  Select 'oAUTH' in the top right and use defaults to enable oAUTH to continue."
        logError(oauthStatus)
    }

    // clear the http result
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        // if we didn't get a token, display the error and stop
        if (oauthStatus != "") {
    	    section("<h2>${oauthStatus}</h2>") {
            }
        } else {
            section(getFormat("title", "OwnTracks Version ${appVersion()}")) {
            }            
            section(getFormat("box", "Installation")) {
                href(title: "Mobile App Installation Instructions", description: "", style: "page", page: "installationInstructions")
                href(title: "Creating User Thumbnail Instructions", description: "", style: "page", page: "thumbnailCreationInstructions")
                configureUsersHome()
                href(title: "Enable OwnTracks Recorder (Optional)", description: "", style: "page", page: "configureRecorder")
            }

            section(getFormat("box", "Mobile App Configuration")) {
                input "syncMobileSettings", "enum", multiple: true, required:false, title:"Select family member(s) to update location, display and region settings on the next location update. The user will be registered to receive this update once 'Done' is pressed, below, and this list will be automatically cleared.", options: (enabledMembers ? enabledMembers.sort() : enabledMembers)
                input "restartMobileApp", "enum", multiple: true, required:false, title:"Select family member(s) to restart their mobile app on next location update. The user will be registered to receive this update once 'Done' is pressed, below, and this list will be automatically cleared.", options: (enabledMembers ? enabledMembers.sort() : enabledMembers)
                href(title: "Regions", description: "", style: "page", page: "configureRegions")
                href(title: "Location", description: "", style: "page", page: "configureLocation")
                href(title: "Display", description: "", style: "page", page: "configureDisplay")
            }

            section(getFormat("box", "Logging")) {
                input name: "descriptionTextOutput", type: "bool", title: "Enable Description Text logging", defaultValue: true
                input name: "debugOutput", type: "bool", title: "Enable Debug Logging", defaultValue: false
            }
            
            section(getFormat("box", "Delete family member(s)")) {
                href(title: "Delete Family Members", description: "", style: "page", page: "deleteMembers")
            }
        }
    }
}

def installationInstructions() {
    return dynamicPage(name: "installationInstructions", title: "", nextPage: "mainPage") {
        def extUri = fullApiServerUrl().replaceAll("null","webhook?access_token=${state.accessToken}")
        section(getFormat("box", "Mobile App Installation Instructions")) {
            paragraph ("This integration requires the <a href='https://owntracks.org/' target='_blank'>OwnTracks</a> app to be installed on your mobile device.\r\n\r\n" +
                       "     <b>Mobile App Configuration</b>\r" +
                       "     1. Open the OwnTracks app on the mobile device, and configure the following fields:\r" +
                       "          <b>Android</b>\r" +
                       "          <i>Preferences -> Connection\r" +
                       "                 Mode -> HTTP \r" +
                       "                 Host -> <a href='${extUri}'>${extUri}</a> \r" +
                       "                 Identification ->\r" +
                       "                        Username -> Name of the particular user \r" +
                       "                        Password -> Not Used  \r" +
                       "                        Device ID -> Optional extra descriptor (IE: 'Phone').  If using OwnTracks recorder, it would be desirable\r" +
                       "                                               to keep this device ID common across device changes, since it logs 'username/deviceID'. \r" +
                       "                        Tracker ID -> Not Used \r" +
                       "            Preferences -> Advanced\r" +
                       "                Remote commands -> Selected\r" +
                       "                Remote configuration -> Selected</i>\r\n\r\n" +
                       "          <b>iOS</b>\r" +
                       "          <i>Tap (i) top left, and select 'Settings'\r" +
                       "                 Mode -> HTTP \r" +
                       "                 URL -> <a href='${extUri}'>${extUri}</a> \r" +
                       "                 User ID -> Name of the particular user \r" +
                       "                 cmd -> Selected</i>\r\n\r\n" +
                       "     2. Click the 'Manual Update' button in the app to register the device with the Hubitat App."          
                      )
        }
    }
}

def thumbnailCreationInstructions() {
    return dynamicPage(name: "thumbnailCreationInstructions", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Creating User Thumbnail Instructions")) {
            paragraph ("Creating User Thumbnails on the OwnTracks Mobile App and optional OwnTracks Recorder.\r\n\r\n" +
                       "     1. Create a thumbnail for the user at a maximum resolution 192x192 pixels in JPG format using your computer.\r" +
                       "     2. Name the thumbnail 'MyUser.jpg' where 'MyUser' is the same name as the user name entered in the mobile app.\r" +
                       "     3. In Hubitat:\r" +
                       "          a. Navigate to 'Settings->File Manager'.\r" +
                       "          b. Select '+ Choose' and select the 'MyUser.jpg' that was created above.\r" +
                       "          c. Select 'Upload'.\r" +
                       "          d. Repeat for any additional users.\r" +
                       "     4. In the OwnTracks Hubitat app:\r" +
                       "          a. Select 'Display', and enable the 'Display user thumbnails on the map.' and then 'Done'.\r" +
                       "          b. Select all users in the 'Select family member(s) to update location, display and region settings on the next location update.' box, and then 'Done'.\r" +
                       "     5. In the OwnTracks Mobile app:\r" +
                       "          a. Select the manual location button, top right of the map screen.  User thumbnails should now populate on the mobile app map.\r"
                      )
        }
    }
}

def recorderInstallationInstructions() {
    return dynamicPage(name: "recorderInstallationInstructions", title: "", nextPage: "configureRecorder") {
        section(getFormat("box", "Installing and configuring the OwnTracks Recorder using Docker")) {
            paragraph ("<b>NOTE:</b>  Instructions assume that Docker has already been installed and is operational.  Replace <b>[HOME_PATH]</b> with your installation specific docker path.  For OpenSUSE, the <b>[HOME_PATH]</b> is '<b>/var/lib</b>'.  Other installations may have a different home path.\r\n\r\n" +
                       "For the source code and instructions, navigate to <a href='https://github.com/owntracks/docker-recorder/' target='_blank'>OwnTracks Recorder GitHub</a> \r\n\r\n" +
                       "     1. Install OwnTracks Recorder:\r" +
                       "          a. docker pull owntracks/recorder\r\n\r\n" +
                       "     2. Configure OwnTracks Recorder:\r" +
                       "          a. docker volume create recorder_store\r" +
                       "          b. docker volume create config\r" +
                       "          c. Copy (or create if non-existant) the 'recorder.conf' file to '/<b>[HOME_PATH]</b>/docker/volumes/config/_data', which contains the following:\r\n\r\n" +
                       "                 Mode -> HTTP \r" +
                       "                 OTR_STORAGEDIR=\"/<b>[HOME_PATH]</b>/docker/volumes/recorder_store/_data\"\r" +
                       "                 OTR_PORT=0\r" +
                       "                 OTR_HTTPHOST=\"0.0.0.0\"\r" +
                       "                 OTR_HTTPPORT=8083\r" +
                       "                 OTR_TOPICS=\"owntracks/#\"\r" +
                       "                 OTR_SERVERLABEL=\"OwnTracks\"\r\n\r\n" +
                       "          d. docker run -d --restart always --name=owntracks -p 8083:8083 -v recorder_store:/store -v config:/config owntracks/recorder\r\n\r\n" +
                       "     3. The above 'recorder_store' (STORAGEDIR) and 'config' is found here in Docker:\r" +
                       "          a. /<b>[HOME_PATH]</b>/docker/volumes/recorder_store/_data\r" +
                       "          b. /<b>[HOME_PATH]</b>/docker/volumes/config/_data\r\n\r\n"
                      )
        }
        section(getFormat("box", "Adding user cards to OwnTracks Recorder")) {
            paragraph ("1. If user thumbnails have not been added to Hubitat, follow the instructions for 'Creating User Thumbnail Instructions' first:") 
            href(title: "Creating User Thumbnail Instructions", description: "", style: "page", page: "thumbnailCreationInstructions")
            paragraph ("2. Select the slider to generate the enabled user's JSON card data in the Hubitat logs:") 
            input name: "generateMemberCardJSON", type: "bool", title: "Create 'trace' outputs for each enabled member in the Hubitat logs.  Slider will turn off once complete.", defaultValue: false, submitOnChange: true
            if (generateMemberCardJSON) {
                logMemberCardJSON()
                app.updateSetting("generateMemberCardJSON",[value: false, type: "bool"])
            }
            paragraph ("3. In the Hubitat log, look for the 'trace' output that looks like this:\r" +
                       "          For recorder cards, copy the bold JSON text between | |, and save this file to 'STORAGEDIR/cards/MyUser/MyUser.json': \r" + 
                       "          |<b>'{\"_type\":\"card\",\"name\":\"MyUser\",\"face\":\"....\",\"tid\":\"MyUser\"}'</b>|\r\n\r\n" + 
                       "4. Save the <b>'{\"_type\":\"card\",\"name\":\"MyUser\",\"face\":\"....\",\"tid\":\"MyUser\"}'</b> (including beginning and end single quotes) to a text file with the name <b>MyUser.json</b>, as listed in step 3.\r\n\r\n" +
                       "5. To add user cards, copy card file for each user to the following docker path:\r" +
                       "          /<b>[HOME_PATH]</b>/docker/volumes/recorder_store/_data/cards/<b>MyUser/MyUser</b>.json\r\n\r\n" +
                       "6. Alternatively, if you choose to have user/device specific cards, you would name the card 'MyUser-Mydevice.json', and save it in the following docker path:\r" +
                       "          /<b>[HOME_PATH]</b>/docker/volumes/recorder_store/_data/cards/<b>MyUser/Mydevice/MyUser-Mydevice</b>.json\r"
                      )
        }
    }
}

def configureUsersHome() {
    if (state.imperialUnits != imperialUnits) {
        state.imperialUnits = imperialUnits
        // preload the settings field with the proper units
        app.updateSetting("locatorDisplacement", [value: displayMFtVal(state.locatorDisplacement), type: "number"])
        app.updateSetting("ignoreInaccurateLocations", [value: displayMFtVal(state.ignoreInaccurateLocations), type: "number"])
        app.updateSetting("homeGeoFence", [value: displayMFtVal(state.homeGeoFence), type: "number"])
    }
    input "enabledMembers", "enum", multiple: true, required:false, title:"Select family member(s)", options: state.members.name.sort(), submitOnChange: true
    input name: "warnOnDisabledMember", type: "bool", title: "Display a warning in the logs if a family member reports a location but is not enabled", defaultValue: true
    input name: "warnOnMemberSettings", type: "bool", title: "Display a warning in the logs if a family member app settings are not configured for optimal operation", defaultValue: false
    input name: "imperialUnits", type: "bool", title: "Display imperial units instead of metric units", defaultValue: false, submitOnChange: true
    input "homePlace", "enum", multiple: false, title:"Select your 'Home' place.  Use '$HUBITAT_LOCATION' to enter a location.", options: [ "$HUBITAT_LOCATION" ] + (state.places ? state.places.desc.sort() : []), defaultValue: HUBITAT_LOCATION, submitOnChange: true
    if (homePlace == HUBITAT_LOCATION) {
        input "homeName", "text", title: "'Home' name", required: true, defaultValue: "Home"
        input name: "homeGeoFence", type: "number", title: "Distance from home location to indicate 'present' (${displayMFtVal(DEFAULT_RADIUS)}-${displayMFtVal(1000)}${getSmallUnits()})", required: true, range: "${displayMFtVal(DEFAULT_RADIUS)}..${displayMFtVal(1000)}", defaultValue: displayMFtVal(DEFAULT_RADIUS)
        input name: "useHubLocation", type: "bool", title: "Use hub location for 'Home' geofence: ${location.getLatitude()},${location.getLongitude()}", defaultValue: false, submitOnChange: true
        if (!useHubLocation) {
            input name: "homeLat", type: "double", title: "Home Latitude", required: true, range: "-90.0..90.0", defaultValue: location.getLatitude()
            input name: "homeLon", type: "double", title: "Home Longitude", required: true, range: "-180.0..180.0", defaultValue: location.getLongitude()
        }
    }
    input "homeSSID", "string", title:"Enter your 'Home' WiFi SSID(s), separated by commas (optional).  Used to prevent devices from being 'non-present' if currently connected to these WiFi access point.", defaultValue: ""
    input name: "regionHighAccuracyRadius", type: "enum", title: "Enable high accuracy reporting when location is between region radius and this value, Recommended=${displayMFtVal(DEFAULT_regionHighAccuracyRadius)}", required: false, defaultValue: "${DEFAULT_regionHighAccuracyRadius}", options: (imperialUnits ? ['0':'disabled','250':'820 ft','500':'1640 ft','750':'2460 ft','1000':'3280 ft'] : ['0':'disabled','250':'250 m','500':'500 m','750':'750 m','1000':'1000 m'])
    input name: "regionHighAccuracyRadiusHomeOnly", type: "bool", title: "High accuracy reporting is used for home region only when selected, all regions if not selected", defaultValue: true
}

def configureRecorder() {
    return dynamicPage(name: "configureRecorder", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Recorder Configuration")) {
            paragraph("The <a href='https://owntracks.org/booklet/clients/recorder/' target='_blank'>OwnTracks Recorder</a> (optional) can be installed for local tracking.")
            input name: "recorderURL", type: "text", title: "HTTP URL of the OwnTracks Recorder.  It will be in the format <b>'http://enter.your.recorder.ip:8083/pub'</b>, assuming using the default port of 8083.", defaultValue: ""
            input name: "enableRecorder", type: "bool", title: "Enable location updates to be sent to the Recorder URL", defaultValue: false
        }
        section() {
            href(title: "Installing OwnTracks Recorder and Configuring User Card Instructions", description: "", style: "page", page: "recorderInstallationInstructions")
        }
    }
}

def configureLocation() {
    return dynamicPage(name: "configureLocation", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Location Configuration")) {
            input name: "monitoring", type: "enum", title: "Location reporting mode, Recommended=${MONITORING_MODES[DEFAULT_monitoring]}", required: true, options: MONITORING_MODES, defaultValue: DEFAULT_monitoring, submitOnChange: true
            input name: "locatorPriority", type: "enum", title: "Source/power setting for location updates, Recommended=${LOCATOR_PRIORITY[DEFAULT_locatorPriority]}", required: true, options: LOCATOR_PRIORITY, defaultValue: DEFAULT_locatorPriority
            if (monitoring == "2") {
                input name: "moveModeLocatorInterval", type: "number", title: "How often should locations be sent from the device while in 'Move' mode (seconds), Recommended=${DEFAULT_moveModeLocatorInterval}", required: true, range: "2..3600", defaultValue: DEFAULT_moveModeLocatorInterval
                // assign the typical app defaults for other monitoring modes
                if (!locatorDisplacement) app.updateSetting("locatorDisplacement",[value: DEFAULT_locatorDisplacement, type: "number"])
                if (!locatorInterval) app.updateSetting("locatorInterval",[value: DEFAULT_locatorInterval, type: "number"])
            } else {
                input name: "locatorDisplacement", type: "number", title: "How far the device travels (${getSmallUnits()}) before receiving another location update, Recommended=${displayMFtVal(DEFAULT_locatorDisplacement)}  <i><b>This value needs to be less than the minimum configured region radius for automations to trigger'</b></i>", required: true, range: "0..${displayMFtVal(1000)}", defaultValue: displayMFtVal(DEFAULT_locatorDisplacement)
                input name: "locatorInterval", type: "number", title: "Device will not report location updates faster than this interval (seconds) unless moving.  When moving, Android uses this 'locatorInterval/6' or '5-seconds' (whichever is greater, unless 'locatorInterval' is less than 5-seconds, then 'locatorInterval' is used), Recommended=60  <i><b>Requires the device to move the above distance, otherwise no update is sent</b></i>", required: true, range: "0..3600", defaultValue: DEFAULT_locatorInterval
                // IE:  locatorInterval=0-seconds,   then locations every 0-seconds  if moved locatorDisplacement meters
                //      locatorInterval=5-seconds,   then locations every 5-seconds  if moved locatorDisplacement meters
                //      locatorInterval=10-seconds,  then locations every 5-seconds  if moved locatorDisplacement meters
                //      locatorInterval=15-seconds,  then locations every 5-seconds  if moved locatorDisplacement meters
                //      locatorInterval=30-seconds,  then locations every 5-seconds  if moved locatorDisplacement meters
                //      locatorInterval=60-seconds,  then locations every 10-seconds if moved locatorDisplacement meters
                //      locatorInterval=120-seconds, then locations every 20-seconds if moved locatorDisplacement meters
                //      locatorInterval=240-seconds, then locations every 40-seconds if moved locatorDisplacement meters
                // assign the app defaults for move monitoring modes - we will use a larger interval in case the user accidentally switches to 'move mode'
                if (!moveModeLocatorInterval) app.updateSetting("moveModeLocatorInterval",[value: DEFAULT_moveModeLocatorInterval, type: "number"])
            }

            input name: "ignoreInaccurateLocations", type: "number", title: "Do not send a location if the accuracy is greater than the given (${getSmallUnits()}), Recommended=${displayMFtVal(DEFAULT_ignoreInaccurateLocations)}", required: true, range: "0..${displayMFtVal(2000)}", defaultValue: displayMFtVal(DEFAULT_ignoreInaccurateLocations)
            input name: "ignoreStaleLocations", type: "decimal", title: "Number of days after which location updates from friends are assumed stale and removed, Recommended=${DEFAULT_ignoreStaleLocations}", required: true, range: "0.0..7.0", defaultValue: DEFAULT_ignoreStaleLocations
            input name: "ping", type: "number", title: "Device will send a location interval at this heart beat interval (minutes), Recommended=${DEFAULT_ping}", required: true, range: "15..60", defaultValue: DEFAULT_ping
            input name: "pegLocatorFastestIntervalToInterval", type: "bool", title: "Request that the location provider deliver updates no faster than the requested locator interval, Recommended '${DEFAULT_pegLocatorFastestIntervalToInterval}'", defaultValue: DEFAULT_pegLocatorFastestIntervalToInterval
        }
    }
}

def configureDisplay() {
    return dynamicPage(name: "configureDisplay", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Display Configuration")) {
            input name: "imageCards", type: "bool", title: "Display user thumbnails on the map.  Needs to have a 'user.jpg' image of maximum resolution 192x192 pixels uploaded to the 'Settings->File Manager'", defaultValue: DEFAULT_imageCards
            input name: "replaceTIDwithUsername", type: "bool", title: "Replace the 'TID' (tracker ID) with 'username' for displaying a name on the map and recorder", defaultValue: DEFAULT_replaceTIDwithUsername
            input name: "notificationEvents", type: "bool", title: "Notify about received events", defaultValue: DEFAULT_notificationEvents
            input name: "pubExtendedData", type: "bool", title: "Include extended data in location reports", defaultValue: DEFAULT_pubExtendedData
            input name: "enableMapRotation", type: "bool", title: "Allow the map to be rotated", defaultValue: DEFAULT_enableMapRotation
            input name: "showRegionsOnMap", type: "bool", title: "Display the region pins/bubbles on the map", defaultValue: DEFAULT_showRegionsOnMap
            input name: "notificationLocation", type: "bool", title: "Show last reported location in ongoing notification banner", defaultValue: DEFAULT_notificationLocation
            input name: "notificationGeocoderErrors", type: "bool", title: "Display Geocoder errors in the notification banner", defaultValue: DEFAULT_notificationGeocoderErrors
        }
    }
}

def configureRegions() {
    return dynamicPage(name: "configureRegions", title: "", nextPage: "mainPage") {
        // clear the setting fields
        clearSettingFields()        
        section(getFormat("box", "Regions Configuration")) {
            input "getMobileRegions", "enum", multiple: true, required:false, title:"Select family member(s) to retrieve their region list on next location update.  Their regions will be merged into the list stored in the Hubitat app.", options: (enabledMembers ? enabledMembers.sort() : enabledMembers)
        }
        section(getFormat("line", "")) {
            href(title: "Add Regions", description: "", style: "page", page: "addRegions")
            href(title: "Edit Regions", description: "", style: "page", page: "editRegions")
            href(title: "Delete Regions", description: "", style: "page", page: "deleteRegions")
        }
    }
}

def addRegions() {
    return dynamicPage(name: "addRegions", title: "", nextPage: "configureRegions") {
        section(getFormat("box", "Add a Region")) {
            if (state.submit) {
                paragraph "<b>${appButtonHandler("addButton")}</b>"
                state.submit = ""
            }
            input "regionName", "text", title: "Name of region", required: false, submitOnChange: true
            input name: "regionRadius", type: "number", title: "Detection radius for region (${getSmallUnits()})", required: false, range: "${displayMFtVal(50)}..${displayMFtVal(1000)}", defaultValue: displayMFtVal(DEFAULT_RADIUS)
            input name: "regionLat", type: "double", title: "Region Latitude", required: false, range: "-90.0..90.0", defaultValue: location.getLatitude()
            input name: "regionLon", type: "double", title: "Region Longitude", required: false, range: "-180.0..180.0", defaultValue: location.getLongitude()

            if (regionName) {
                input name: "addButton", type: "button", title: "Save", state: "submit"
            }
        }
    }
}

def editRegions() {
    return dynamicPage(name: "editRegions", title: "", nextPage: "configureRegions") {
        section(getFormat("box", "Edit a Region")) {
            if (state.submit) {
                paragraph "<b>${appButtonHandler("editButton")}</b>"
                state.submit = ""
            }
            input "regionName", "enum", multiple: false, required:false, title:"Select region to edit", options: (state.places ? state.places.desc.sort() : []), submitOnChange: true
            if (regionName) {
                // get the place map and assign the current values
                def foundPlace = state.places.find {it.desc==regionName}
                app.updateSetting("regionName",[value:foundPlace.desc,type:"text"])
                app.updateSetting("regionRadius",[value:displayMFtVal(foundPlace.rad.toInteger()),type:"number"])
                app.updateSetting("regionLat",[value:foundPlace.lat,type:"double"])
                app.updateSetting("regionLon",[value:foundPlace.lon,type:"double"])
                // save the name in so we can retrieve the values should it get changed below
                state.previousRegionName = regionName

                input name: "regionName", type: "text", title: "Region Name", required: true
                input name: "regionRadius", type: "number", title: "Detection radius for region (${getSmallUnits()})", required: true, range: "${displayMFtVal(50)}..${displayMFtVal(1000)}"
                input name: "regionLat", type: "double", title: "Region Latitude", required: true, range: "-90.0..90.0"
                input name: "regionLon", type: "double", title: "Region Longitude", required: true, range: "-180.0..180.0"

                input name: "editButton", type: "button", title: "Save", state: "submit"
            }
        }
    }
}

def deleteRegions() {
    return dynamicPage(name: "deleteRegions", title: "", nextPage: "configureRegions") {
        section(getFormat("box", "Delete Region(s)")) {
            if (state.submit) {
                paragraph "<b>${appButtonHandler("deleteButton")}</b>"
                state.submit = ""
            }
            input "regionName", "enum", multiple: true, required:false, title:"Select regions to delete.", options: (state.places ? state.places.desc.sort() : []), submitOnChange: true
            paragraph("<b>NOTE:  Region(s) will be deleted once all enabled users have updated waypoints.</b>")
            if (regionName) {
                input name: "deleteButton", type: "button", title: "Delete", state: "submit"
            }
        }
    }
}

def deleteMembers() {
    return dynamicPage(name: "deleteMembers", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Delete Family Member(s)")) {
            if (state.submit) {
                paragraph "<b>${appButtonHandler("deleteMembersButton")}</b>"
                state.submit = ""
            }
            input "deleteFamilyMembers", "enum", multiple: true, required:false, title:"Select family member(s) to delete.", options: state.members.name.sort(), submitOnChange: true
            
            paragraph("<b>NOTE: Selected user(s) will be deleted from the app and their corresponding child device will be removed.  Ensure no automations are dependent on their device before proceeding!</b>")
            if (deleteFamilyMembers) {
                input name: "deleteMembersButton", type: "button", title: "Delete", state: "submit"
            }
        }
    }
}

String appButtonHandler(btn) {
    def success = false
    switch (btn) {
        case "addButton":
            // check if we are duplicating a region, and delete the name if so
            if (state.places.find {it.desc==regionName}) {
                result = "Region '${regionName}' already exists."
                logWarn(result)
                // clear the region name
                app.removeSetting("regionName")
            } else {
                if (!regionName || !regionLat || !regionLon || !regionRadius) {
                    result = "All fields need to be populated."
                    logWarn(result)
                } else {
                    // create the waypoint map - NOTE: the app keys off the "tst" field as a unique identifier
                    def newPlace = [ "_type": "waypoint", "desc": "${regionName}", "lat": "${regionLat}", "lon": "${regionLon}", "rad": "${convertToMeters(regionRadius)}", "tst": "${(now()/1000).toInteger()}" ]
                    // add the new place
                    state.places << newPlace
                    logDescriptionText("Added place: ${newPlace}")
                    result = "Region '${regionName}' has been added."
                    success = true
                }
            }
        break
        case "editButton":
            // find the existing place to update.
            def foundPlace = state.places.find {it.desc==state.previousRegionName}
            // create the updated waypoint map - NOTE: the app keys off the "tst" field as a unique identifier
            def newPlace = [ "_type": "waypoint", "desc": "${regionName}", "lat": "${regionLat}", "lon": "${regionLon}", "rad": "${convertToMeters(regionRadius)}", "tst": "${foundPlace.tst}" ]
            // overwrite the existing place
            foundPlace << newPlace
            result = "Updating region '${newPlace}'"
            logDescriptionText(result)
            success = true
        break
        case "deleteButton":
            // unvalidate all the places that need to be removed
            regionName.each { desc ->
                place = state.places.find {it.desc==desc}
                // invalidate the coordinates to flag it for deletion
                place.lat = INVALID_COORDINATE
                place.lon = INVALID_COORDINATE
                place.desc = "-DELETED-"
                // check if we are deleting our home location
                if (homePlace == desc) {
                   state.home = []
                }
            }
            result = "Deleting regions '${regionName}'"
            logWarn(result)
            success = true
        break
        case "deleteMembersButton":
            deleteFamilyMembers.each { name ->
                deleteIndex = state.members.findIndexOf {it.name==name}
                def deviceWrapper = getChildDevice(state.members[deleteIndex].id)
                try {
                    deleteChildDevice(deviceWrapper.deviceNetworkId)
                } catch(e) {
                    logDebug("Device for ${name} does not exist.")
                }
                state.members.remove(deleteIndex)               
            }
            result = "Deleting family members '${deleteFamilyMembers}'"
            logWarn(result)
            app.removeSetting("deleteFamilyMembers")
        break
        default:
            result = ""
            logWarn ("Unhandled button: $btn")
        break
    }

    // clear the fields and force the users to update
    if (success) {
        // clear the region fields
        clearSettingFields()
        // force an update of all users
        setWaypointUpdateFlag([ "name":"" ])
    }

    return (result)
}

def clearSettingFields() {
    // clear the setting fields
    app.removeSetting("regionName")
    app.removeSetting("regionRadius")
    app.removeSetting("regionLat")
    app.removeSetting("regionLon")
    state.previousRegionName = ""
    app.removeSetting("deleteFamilyMembers")    
}

def installed() {
    log.info("Installed")
    initialize()
    updated()
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

def initialize() {
    // initialize the system states if undefined
    if (state.accessToken == null) state.accessToken = ""
    if (state.members == null) state.members = []
    if (state.home == null) state.home = []
    if (state.places == null) state.places = []
    if (state.locatorDisplacement == null) state.locatorDisplacement = DEFAULT_locatorDisplacement
    if (state.ignoreInaccurateLocations == null) state.ignoreInaccurateLocations = DEFAULT_ignoreInaccurateLocations
    if (state.homeGeoFence == null) state.homeGeoFence = DEFAULT_RADIUS
    if (state.imperialUnits == null) state.imperialUnits = false
    
    // assign the defaults to the mobile app settings in case the user doesn't click into those screens
    if (monitoring == null) app.updateSetting("monitoring", [value: DEFAULT_monitoring, type: "number"])
    if (locatorPriority == null) app.updateSetting("locatorPriority", [value: DEFAULT_locatorPriority, type: "number"])
    if (moveModeLocatorInterval == null) app.updateSetting("moveModeLocatorInterval", [value: DEFAULT_moveModeLocatorInterval, type: "number"])
    if (locatorDisplacement == null) app.updateSetting("locatorDisplacement", [value: DEFAULT_locatorDisplacement, type: "number"])
    if (locatorInterval == null) app.updateSetting("locatorInterval", [value: DEFAULT_locatorInterval, type: "number"])
    if (ignoreInaccurateLocations == null) app.updateSetting("ignoreInaccurateLocations", [value: DEFAULT_ignoreInaccurateLocations, type: "number"])
    if (ignoreStaleLocations == null) app.updateSetting("ignoreStaleLocations", [value: DEFAULT_ignoreStaleLocations, type: "number"])
    if (ping == null) app.updateSetting("ping", [value: DEFAULT_ping, type: "number"])
    if (pegLocatorFastestIntervalToInterval == null) app.updateSetting("pegLocatorFastestIntervalToInterval", [value: DEFAULT_pegLocatorFastestIntervalToInterval, type: "bool"])
    if (imageCards == null) app.updateSetting("imageCards", [value: DEFAULT_imageCards, type: "bool"])
    if (replaceTIDwithUsername == null) app.updateSetting("replaceTIDwithUsername", [value: DEFAULT_replaceTIDwithUsername, type: "bool"])
    if (notificationEvents == null) app.updateSetting("notificationEvents", [value: DEFAULT_notificationEvents, type: "bool"])
    if (pubExtendedData == null) app.updateSetting("pubExtendedData", [value: DEFAULT_pubExtendedData, type: "bool"])
    if (enableMapRotation == null) app.updateSetting("enableMapRotation", [value: DEFAULT_enableMapRotation, type: "bool"])
    if (showRegionsOnMap == null) app.updateSetting("showRegionsOnMap", [value: DEFAULT_showRegionsOnMap, type: "bool"])
    if (notificationLocation == null) app.updateSetting("notificationLocation", [value: DEFAULT_notificationLocation, type: "bool"])
    if (notificationGeocoderErrors == null) app.updateSetting("notificationGeocoderErrors", [value: DEFAULT_notificationGeocoderErrors, type: "bool"])
}

def updated() {
    unschedule()
    unsubscribe()
    logDescriptionText("Updated")

    // create a presence child device for each member - we will need to manually removed children unless the app is uninstalled
    state.members.each { member->
        // default to false
        syncSettings = false
        // create the child if it doesn't exist
        if (member.id == null) {
            createChild(member.name)
            // force the update to the new device
            syncSettings = true
        }

        // if we selected member(s) to update settings
        if (settings?.syncMobileSettings.find {it==member.name}) {
            syncSettings = true
        }
        // if we selected member(s) to retrieve their regions
        if (settings?.getMobileRegions.find {it==member.name}) {
            member.getRegions = true
        }
        // if we selected member(s) to restart their mobile app
        if (settings?.restartMobileApp.find {it==member.name}) {
            member.restartApp = true
        }
        // if the configuration has changed, trigger the member update
        if (syncSettings) {
            member.updateLocation = true
            member.updateDisplay = true
            member.updateWaypoints = true
        }
    }
    // clear the settings flags to prevent the configurations from being forced to the display on each entry
    app.updateSetting("restartMobileApp",[value:"",type:"enum"])
    app.updateSetting("syncMobileSettings",[value:"",type:"enum"])
    app.updateSetting("getMobileRegions",[value:"",type:"enum"])
    
    // save the values to allow for imperial/metric selection
    state.locatorDisplacement             = convertToMeters(locatorDisplacement)
    state.ignoreInaccurateLocations       = convertToMeters(ignoreInaccurateLocations)
    state.homeGeoFence                    = convertToMeters(homeGeoFence)

    // store the home information
    if (homePlace == HUBITAT_LOCATION) {
        // using a locally entered location
        if (useHubLocation) {
            state.home = [ name:homeName, geofence:(state.homeGeoFence/1000), latitude:location.getLatitude(), longitude:location.getLongitude() ]
        } else {
            state.home = [ name:homeName, geofence:(state.homeGeoFence/1000), latitude:homeLat, longitude:homeLon ]
        }
        // check if we need to add the hubitat home region to the phone regions list
        addDefaultHomeRegionToRegions()
    } else {
        // using a place returned from the device regions
        findPlace = state.places.find {it.desc==homePlace}
        state.home = [ name:findPlace.desc, latitude:findPlace.lat, longitude:findPlace.lon, geofence:(findPlace.rad.toInteger()/1000) ]
    }
}

def addDefaultHomeRegionToRegions() {
    def foundPlace = state.places.find {it.desc==state.home.name}
    // add the location to the regions list to be pushed to the user if it isn't already there
    if (!foundPlace) {
        app.updateSetting("regionName",[value:state.home.name,type:"text"])
        app.updateSetting("regionRadius",[value:displayMFtVal((state.home.geofence*1000).toInteger()),type:"number"])
        app.updateSetting("regionLat",[value:state.home.latitude,type:"double"])
        app.updateSetting("regionLon",[value:state.home.longitude,type:"double"])
        appButtonHandler("addButton")
        clearSettingFields()
    }
}

def refresh() {
}

def childGetWarnOnNonOptimalSettings() {
    // return with the log setting
    return (warnOnMemberSettings)
}

def splitTopic(topic) {
    // split the topic into source, name, deviceID, eventType (if present)
    // TOPIC_FORMAT = [ 0: "topicSource", 1: "userName", 2: "deviceID", 3: "eventType" ]
    //     [0] = "owntracks"
    //     [1] = "username"
    //     [2] = "deviceID"
    //     [3] = event type: "waypoint", "waypoints", "event".  Does not get populated for "location".
   return(data.topic.split("/"))
}

def webhookEventHandler() {
    // Get the user/device from the message header
    String sourceName     = request.headers.'X-limit-u'
    String sourceDeviceID = request.headers.'X-limit-d'
    // default to an empty payload
    result = []

    // catch the exception if no message was sent
    if (!request.body) {
        logError("Username: '${sourceName}' / Device ID: '${sourceDeviceID}' reported no data from the OwnTracks app, aborting.")
    } else if (!sourceName || !sourceDeviceID) {
        // catch the exception if a webhook comes in without being configured properly
        logWarn("Username: '${sourceName}' / Device ID: '${sourceDeviceID}' not configured in the OwnTracks app, aborting.")
    } else {
        // strip the [] around these values
        sourceName = sourceName.substring(1, (sourceName.length()-1))
        sourceDeviceID = sourceDeviceID.substring(1, (sourceDeviceID.length()-1))
        data = parseJson(request.body)
        logDebug("Received update' from user: '$sourceName', deviceID: '$sourceDeviceID', data: $data")

        // check the list for a matching member
        findMember = state.members.find {it.name==sourceName}
        if (!findMember?.id) {
            // add the new user to the list if they don't exist yet.  We will use the current time since not all incoming packets have a timestamp
            if (findMember == null) {
                state.members << [ name:sourceName, deviceID:sourceDeviceID, id:null, timeStamp:(now()/1000).toInteger(), updateWaypoints:true, updateLocation:true, updateDisplay:true, dynamicLocaterAccuracy:false, restartApp:false, getRegions:false ]
            }
            logWarn("User: '${sourceName}' not configured.  Run setup to add new member.")
        } else {
            // only process events from enabled members
            if (settings?.enabledMembers.find {it==sourceName}) {
                // update the device ID should it have changed
                findMember.deviceID=sourceDeviceID
                switch (data._type) {
                    case "location":
                    case "transition":
                        // log the elapsed distance and time
                        logDistanceTraveledAndElapsedTime(findMember, data)
                        // replace the tracker ID with the member name.  NOTE: if the TID is undefined, it will be the last 2-characters of the Device ID
                        if (replaceTIDwithUsername) {
                            data.tid = findMember.name
                        }
                        // send push event to driver
                        updateDevicePresence(findMember, data)
                        // return with the rest of the users positions and waypoints if pending
                        result = sendUpdate(findMember, data)

                        // if the country code was not defined, replace with with hub timezone country
                        if (!data.cc) { data.cc = location.getTimeZone().getID().substring(0, 2).toUpperCase() }
                        // if the course over ground was not defined, replace distance from home
                        if (!data.cog) { data.cog = data.currentDistanceFromHome }
                        // if we have the OwnTracks recorder configured, and the timestamp is valid, pass the location data to it
                        if (recorderURL && enableRecorder && (data.tst != 0)) {
                            def postParams = [ uri: recorderURL, requestContentType: 'application/json', contentType: 'application/json', headers: parsePostHeaders(request.headers), body : (new JsonBuilder(data)).toPrettyString() ]
                            asynchttpPost("recorderCallbackMethod", postParams)
                        }
                    break
                    case "waypoint":
                        // append/update to the places list
                        addPlace(findMember, data)
                    break
                    case "waypoints":
                        // update the places list
                        updatePlaces(findMember, data)
                    break
                    default:
                        logWarn("Unhandled message type: ${data._type}")
                    break
                }
            } else {
                if (warnOnDisabledMember) {
                    logWarn("User: '${sourceName}' not enabled.  Run setup to enable member.")
                } else {
                    logDebug("User: '${sourceName}' not enabled.  Run setup to enable member.")
                }
            }
        }
    }

    // app requires a non-empty JSON response, or it will display HTTP 500
    return render(contentType: "text/html", data: result, status: 200)
}

def parsePostHeaders(postHeaders) {
    def newHeaders = [:]

    // loop through each header and remove the surrounding [], and recreate a new header map
    postHeaders.each { entry->
        String parsedValue = entry.getValue()
        newHeaders.put("${entry.getKey()}", "${parsedValue.substring(1, (parsedValue.length()-1))}")
    }

    return (newHeaders)
}

def recorderCallbackMethod(response, data) {
    if (response.status == 200) {
        logDebug "Posted successfully to OwnTracks recorder URL."
    } else {
        logWarn "OwnTracks Recorder HTTP response: ${response.status}"
    }
}

def updateDevicePresence(member, data) {
    // save the position and timestamp so we can push to other users
    member.latitude  = data?.lat
    member.longitude = data?.lon
    member.timeStamp = data?.tst
    member.battery   = data?.batt
    member.accuracy  = data?.acc
    member.altitude  = data?.alt
    member.speed     = data?.vel
    member.trackerID = data?.tid
    member.wifi      = data?.wifi
    member.hib       = data?.hib
    member.ps        = data?.ps
    member.bo        = data?.bo
    member.loc       = data?.loc
    member.bs        = data?.bs

    // update the presence information for the member
    try {
        // find the appropriate child device based on app id and the device network id
        def deviceWrapper = getChildDevice(member.id)
        logDebug("Updating '${(data.event ? "Event $data.event" : (data.t ? TRIGGER_TYPE[data.t] : "Location"))}' presence for member $deviceWrapper")

        // append the distance from home to the data packet
        data.currentDistanceFromHome = getDistanceFromHome(data)
        if (data.currentDistanceFromHome <= state.home.geofence) {
            data.currentDistanceFromHome = 0.0
            // if there was no defined regions, create a blank list
            if (!data.inregions) {
                data.inregions = []
            }
            // if the home name isn't present, at it to the regions
            if (!data.inregions.find {it==state.home.name}) {
                data.inregions << state.home.name
            }
        }
        // pass the home SSID to the driver
        data.homeSSID = homeSSID
        // update the child information
        deviceWrapper.generatePresenceEvent(data)
    } catch(e) {
        logError("updateDevicePresence: Exception for member: ${member.name}  $e")
    }
}

def checkRegionConfiguration(member, data) {
    // if we configured the high accuracy region
    if (regionHighAccuracyRadius) {
        def closestWaypointRadius = 0
        def closestWaypointDistance = -1
        
        // check if we need to apply this to the home region only, or all regions
        if (regionHighAccuracyRadiusHomeOnly) {
            // only switch to faster reporting when near home
            closestWaypointDistance = (getDistanceFromHome(data)*1000).toDouble()
            closestWaypointRadius = (state.home.geofence*1000).toDouble()
        } else {
             // loop through all the waypoints, and find the one that is closet to the location
            state.places.each { waypoint->
                // check our distance from the waypoint
                distanceToWaypoint = ((haversine(data.lat.toDouble(), data.lon.toDouble(), waypoint.lat.toDouble(), waypoint.lon.toDouble()))*1000).round(0)
                // if we are closest, then save that waypoint
                if ((closestWaypointDistance == -1) || (distanceToWaypoint < closestWaypointDistance)) {
                    closestWaypointDistance = distanceToWaypoint.toDouble()
                    closestWaypointRadius = waypoint.rad.toDouble()
                }
            }
        }

        // catch the exception if no regions have been defined
        if ((closestWaypointDistance == null) || (closestWaypointRadius == null)) {
            closestWaypointRadius = 0
            closestWaypointDistance = -1
            logWarn("Home region is undefined.  Run setup to configure the 'Home' location") 
        }

        // check if we are within our region radius, or greater than the radius + regionHighAccuracyRadius
        if ((closestWaypointDistance < closestWaypointRadius) || (closestWaypointDistance > (closestWaypointRadius + regionHighAccuracyRadius.toDouble()))) {
            // switch to settings.  Recommended locatorPriority=balanced power and pegLocatorFastestIntervalToInterval=true (fixed interval)
            useDynamicLocaterAccuracy = false
            configurationList = [ "_type": "configuration",
                                  "pegLocatorFastestIntervalToInterval": pegLocatorFastestIntervalToInterval,
                                  "locatorPriority": locatorPriority.toInteger(),
                                ]
        } else {
            // switch to locatorPriority=high power and pegLocatorFastestIntervalToInterval=false (dynamic interval)
            useDynamicLocaterAccuracy = true
            configurationList = [ "_type": "configuration",
                                  "pegLocatorFastestIntervalToInterval": DYNAMIC_INTERVALS.pegLocatorFastestIntervalToInterval,
                                  "locatorPriority": DYNAMIC_INTERVALS.locatorPriority,
                                ]
        }
        // check if we had a change, and then update the device configuration
        if (member?.dynamicLocaterAccuracy != useDynamicLocaterAccuracy) {
            // assign the new state
            member.dynamicLocaterAccuracy = useDynamicLocaterAccuracy
            // return with the dynamic configuration
            return( [ "_type":"cmd","action":"setConfiguration", "configuration": configurationList ] )
        } else {
            // return nothing
            return
        }
    }
}

private def getDistanceFromHome(data) {
    // return distance in kilometers, rounded to 3 decimal places (meters)
    return (haversine(data.lat.toDouble(), data.lon.toDouble(), state.home.latitude.toDouble(), state.home.longitude.toDouble()).round(3))
}

private def logDistanceTraveledAndElapsedTime(member, data) {
    // log the elapsed distance and time between location events
    try {
        logDebug ("${app.label}: Delta between location events, member: ${member.name}, Distance: ${displayMFtVal(haversine(data.lat.toDouble(), data.lon.toDouble(), member.latitude.toDouble(), member.longitude.toDouble()).round(3)*1000)} ${getSmallUnits()}, Time: ${data.tst-member.timeStamp} s")
    } catch(e) {
        // only gets here on a first time user
    }
}

def removePlaces() {
    // check if all users have their waypoints updated, and then remove any place with invalidated coordinates

    // default to true
    def deleteEntries = true
    // loop through all the enabled members to see if any have outstanding waypoint updates
    settings?.enabledMembers.each { enabledMember->
        if (state.members.find {it.name==enabledMember}.updateWaypoints) {
            deleteEntries = false;
        }
    }

    // remove all regions flagged for deletion
    if (deleteEntries) {
        deleteIndex = state.places.findIndexOf {it.lat == INVALID_COORDINATE}
        // remove the place from our current list
        if (deleteIndex >= 0) {
            state.places.remove(deleteIndex)
        }
    }
}

def updatePlaces(findMember, data) {
    logDescriptionText("Updating places")

    // loop through all the waypoints
    data.waypoints.each { waypoint->
        addPlace(findMember, waypoint)
    }
    logDebug("Updating places: ${state.places}")
}

def addPlace(findMember, data) {
    // create a new map removing the MQTT topic
    def newPlace = [ "_type": "${data._type}", "desc": "${data.desc}", "lat": "${data.lat}", "lon": "${data.lon}", "rad": "${data.rad}", "tst": "${data.tst}" ]

    // check if we have an existing place with the same timestamp
    place = state.places.find {it.tst==newPlace.tst}

    // no changes to existing place
    if (place == newPlace) {
        logDescriptionText("Skipping, no change to place: ${newPlace}")
    } else {
        // change logging depending if the place previously existed
        if (place) {
            logDescriptionText("${findMember.name} updated place: ${newPlace}")
            // overwrite the existing place
            place << newPlace
        } else {
            logDescriptionText("${findMember.name} added place: ${newPlace}")
            // add the new place
            state.places << newPlace
        }
        // force the users to get the update place list
        setWaypointUpdateFlag(findMember)
    }
}

private def setWaypointUpdateFlag(currentMember) {
    // loop through all the members
    state.members.each { member->
        // don't set the flag for the member that updated the waypoint list
        if (currentMember.name != member.name) {
            member.updateWaypoints = true
            logDebug("Enabling waypoint update for user: ${member.name}")
        }
    }
}

private def sendRestartRequest(currentMember) {
    logDescriptionText("Request app restart for user ${currentMember.name}")

    return ([ "_type":"cmd","action":"restart" ])
}

private def sendReportLocationRequest(currentMember) {
    logDescriptionText("Request location for user ${currentMember.name}")
    // Forces the device to get a GPS fix for higher accuracy (temporarily changes 'locatorPriority' to "HIGH POWER")

    return ([ "_type":"cmd","action":"reportLocation" ])
}

private def sendReportWaypointsRequest(currentMember) {
    logDescriptionText("Request waypoints for user ${currentMember.name}")
    // Requests the waypoints list from the device

    return ([ "_type":"cmd","action":"waypoints" ])
}

private def sendMemberPositions(currentMember, data) {
    def positions = []

    // loop through all the enabled members
    settings?.enabledMembers.each { enabledMember->
        // don't send the members position back to them
        if (currentMember.name != enabledMember) {
            member = state.members.find {it.name==enabledMember}
            // populating the tracker ID field with a name allows the name to be displayed in the Friends list and map bubbles
            positions << [ "_type": "location", "t": "u", "lat": member.latitude, "lon": member.longitude, "tst": member.timeStamp, "tid": member.trackerID, "batt": member.battery, "acc": member.accuracy, "alt": member.altitude, "vel": member.speed, "wifi": member.wifi, "hib": member.hib, "ps": member.ps, "bo": member.bo, "loc": member.loc, "bs": member.bs ]
            // send the image cards for the user if enabled
            card = getMemberCard(member, data)
            if (card) {
                positions << card
            }
        }
    }

    return (positions)
}

private def getMemberCard(member, data) {
    def card = []

    // send the image cards for the user if enabled -- only send on the ping or the manual update to minimize data traffic
    if (imageCards && validLocationType(data.t)) {
        try{
            // append each enabled user's card with encoded image
            card = [ "_type": "card", "name": "${member.name}", "face": "${downloadHubFile("${member.name}.jpg").encodeBase64().toString()}", "tid": "${(member.trackerID == "er" ? member.name : member.trackerID)}" ]
        }
        catch (e) {
            logError("No ${member.name}.jpg image stored in 'Settings->File Manager'")
        }
    }

    return (card)
}

private def logMemberCardJSON() {
    // creates "trace" outputs in the Hubitat logs for each user card to be saved into a .JSON file for OwnTracks Recorder
    settings?.enabledMembers.each { enabledMember->
        member = state.members.find {it.name==enabledMember}
        card = getMemberCard(member, ["t":"u"])
        if (card) {
            // for recorder, this debug must be captured and saved to: <STORAGEDIR>/cards/<user>/<user>.json
            // or use: https://avanc.github.io/owntracks-cards/ to create and save the JSON
            log.trace("For recorder cards, copy the bold JSON text between |  |, and save this file to 'STORAGEDIR/cards/${member.name}/${member.name}.json': |<b>'${(new JsonBuilder(card)).toPrettyString()}'</b>|")
        }
    }        
}

private def sendWaypoints(currentMember) {
    logDescriptionText("Updating waypoints for user ${currentMember.name}")

    return ([ "_type":"cmd","action":"setWaypoints", "waypoints": [ "_type":"waypoints", "waypoints":state.places ] ])
}

private def sendConfiguration(currentMember) {
    logDescriptionText("Updating configuration for user ${currentMember.name}")

    // create the configuration response.  Note: Configuration below are only the HTTP from the exported config.otrc file values based on the build version below
    def configurationList = [
                                "_type" :                               "configuration",
                              //"_build" :                              420412000,                           // current mobile app version

                                // static configuration
                                "mode" :                                3,                                   // Endpoint protocol mode: 0=MQTT, 3=HTTP
                                "autostartOnBoot" :                     true,                                // Autostart the app on device boot
                                "cmd" :                                 true,                                // Respond to cmd messages
                                "remoteConfiguration" :                 true,                                // Allow remote configuration
                                "reverseGeocodeProvider" :              "Device",                            // Reverse Geocode provider -- use device (Google for Android)
                                "allowRemoteLocation" :                 true,                                // required for 'reportLocation' to be processed
                                "connectionTimeoutSeconds" :            30,
                                "debugLog" :                            false,
                                "dontReuseHttpClient" :                 false,
                                "experimentalFeatures" :                [ ],
                                "fusedRegionDetection" :                true,
                                "notificationHigherPriority" :          false,
                                "opencageApiKey" :                      "",
                             // "mapLayerStyle" :                       "GoogleMapDefault",                  // type cast issues from the app debug logs
                             // "osmTileScaleFactor" :                  1.0,                                 // type cast issues from the app debug logs

                                 // user configurations                                                      // These are unique to each user device
                             // "username" :                            "",                                  // Username to be used to create the device
                             // "password" :                            "",                                  // Not used
                             // "deviceId" :                            "",                                  // Not used -- defaults to the OS version (IE: Android 14 is 'Panther')
                             // "tid" :                                 "",                                  // 2-character tracker ID to be displayed on the friends map -- leave blank, and the username is pushed back for the map
                            ]

    def deviceLocatorList = [
                                // dynamic configurations
                             // "url" :                                 "${extUri}",                         // Connection URL to this app.  No point in allow it to be configured since it's static
                                "pegLocatorFastestIntervalToInterval" : pegLocatorFastestIntervalToInterval, // Request that the location provider deliver updates no faster than the requested locator interval
                                "monitoring" :                          monitoring.toInteger(),              // Monitoring mode (quiet, manual, significant, move)
                                "locatorPriority" :                     locatorPriority.toInteger(),         // source/power setting for location updates (no power, low power, balanced power, high power)
                                "locatorDisplacement" :                 locatorDisplacement,                 // How far should the device travel (in metres) before receiving another location
                                "locatorInterval" :                     locatorInterval,                     // How often should locations be requested from the device (seconds)
                                "moveModeLocatorInterval" :             moveModeLocatorInterval,             // How often should locations be requested from the device whilst in Move mode (seconds)
                                "ignoreInaccurateLocations" :           ignoreInaccurateLocations,           // Ignore location, if the accuracy is greater than the given meters.  NOTE: Build 420412000 occasionally reports events with acc=1799.999
                                "ignoreStaleLocations" :                ignoreStaleLocations,                // Number of days after which location updates are assumed stale
                                "ping" :                                ping,                                // Device will send a location interval at this heart beat interval (minutes).  Minimum 15, seems to be fixed at 30 minutes.
                            ]

    def deviceDisplayList = [
                                "notificationLocation" :                notificationLocation,                // Display last reported location and time in ongoing notification
                                "pubExtendedData" :                     pubExtendedData,                     // Include extended data in location reports
                                "notificationEvents" :                  notificationEvents,                  // Notify about received events
                                "enableMapRotation" :                   enableMapRotation,                   // Allow the map to be rotated
                                "showRegionsOnMap" :                    showRegionsOnMap,                    // Display the region pins/bubbles on the map
                                "notificationGeocoderErrors" :          notificationGeocoderErrors,          // Display Geocoder errors in the notification banner
                            ]

    // append the extra app configurations if enabled
    if (currentMember.updateLocation) {
        currentMember.updateLocation = false
        configurationList << deviceLocatorList
    }
    if (currentMember.updateDisplay) {
        currentMember.updateDisplay = false
        configurationList << deviceDisplayList
    }

    def configuration = [ "_type":"cmd","action":"setConfiguration", "configuration": configurationList ]

    logDebug("Updating configuration: ${configuration}")
    return (configuration)
}

private def sendUpdate(currentMember, data) {
    def update = []

    // only send the position updates on a ping or manual update
    if (validLocationType(data.t)) {
        update += sendMemberPositions(currentMember, data)
        // request a more precise GOS location by temporarily changing 'locatorPriority' to "HIGH POWER"
        // we will do this for manual and ping updates if not already in high power mode
        if (locatorPriority != "3") {
            // currently not enabled in the 2.4.x mobile app for http
//            update += sendReportLocationRequest(currentMember)
        }
    }

    if (currentMember?.updateWaypoints) {
        currentMember.updateWaypoints = false
        update += sendWaypoints(currentMember)
    }
    // check if we have any places marked for removal, and clean up the list
    removePlaces()

    if ((currentMember?.updateLocation) || (currentMember?.updateDisplay)) {
        update += sendConfiguration(currentMember)
    } else {
        // change the configuration if necessary
        updateConfig = checkRegionConfiguration(currentMember, data)
        if (updateConfig) {
            update += updateConfig
        }
    }

    // trigger an app restart
    if (currentMember?.restartApp) {
        currentMember.restartApp = false
        update += sendRestartRequest(currentMember)
    }

    // request the member's regions
    if (currentMember?.getRegions) {
        currentMember.getRegions = false
        update += sendReportWaypointsRequest(currentMember)
    }

    logDebug("Updating user: ${currentMember.name} with data: ${update}")
    return (new JsonBuilder(update).toPrettyString())
}

def validLocationType(locationType) {
    // allow update if ping or manual location
    return ((locationType == "p") || (locationType == "u"))
}

private def createChild (name) {
    // the unique ID will be the EPOCH timestamp
    id = now()
    def DNI = "${app.id}.${id}"

    logDescriptionText("Creating OwnTracks Device: $name:$DNI")
    try{
        addChildDevice("lpakula", "OwnTracks Driver", DNI, ["name": "${CHILDPREFIX}${name}", isComponent: false])
        state.members.find {it.name==name}.id = DNI
        logDescriptionText("Child Device Successfully Created")
    }
    catch (e) {
        logError("Child device creation failed with error ${e}")
    }
}

private removeChildDevices(delete) {
    delete.each { 
        try {
            deleteChildDevice(it.deviceNetworkId) 
        } catch(e) {
            logDebug("Device ${it} does not exist.")
        }        
    }
}

def haversine(lat1, lon1, lat2, lon2) {
    def Double R = 6372.8
    // In kilometers
    def Double dLat = Math.toRadians(lat2 - lat1)
    def Double dLon = Math.toRadians(lon2 - lon1)
    lat1 = Math.toRadians(lat1)
    lat2 = Math.toRadians(lat2)

    def Double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2)
    def Double c = 2 * Math.asin(Math.sqrt(a))
    def Double d = R * c
    return(d)
}

def displayKmMiVal(val) {
    return (imperialUnits ? (val.toFloat()*0.621371).round(3) : val.toFloat().round(3))
}

def displayMFtVal(val) {
    // round up and convert to an integer
    return (imperialUnits ? (val.toFloat()*3.28084).round(0).toInteger() : val.toInteger())
}

def convertToMeters(val) {
    // round up and convert to an integer
    return (imperialUnits ? (val.toFloat()*0.3048).round(0).toInteger() : val.toInteger())
}

def getLargeUnits() {
    return (imperialUnits ? "mi" : "km")
}

def getSmallUnits() {
    return (imperialUnits ? "ft" : "m")
}

def getVelocityUnits() {
    return (imperialUnits ? "mph" : "kph")
}

mappings {
	path("/webhook") {
    	action: [
            POST: "webhookEventHandler",
            GET:  "webhookGetHandler",        // used for tesing through a web browser
        ]
    }
}

private def webhookGetHandler() {
    testMember = [ "updateWaypoints":true, "updateLocation":true, "updateDisplay":true, "dynamicLocaterAccuracy":true ]
    result = sendUpdate(testMember, [ "t":"p", "lat":12.345, "lon":-123.45678 ] )
    log.warn "ADDED FOR TESTING THROUGH THE BROWSER LINK "
    return render(contentType: "text/html", data: result, status: 200)
}

private def getFormat(type, myText="", myError="") {
    if (type == "box") return "<div style='color:#ffffff;font-weight: normal;background-color:#3C00BC;padding:5px;padding-left:10px;border: 1px solid #000000;box-shadow: 3px 4px #575757;border-radius: 1px'>${myText}<span style='color:red;padding-left:5px;font-weight:bold'>${myError}</span></div>"
    if (type == "line") return "<hr style='background-color:#3C00BC; height: 1px; border: 0;'/>"
    if (type == "title") return "<h2 style='color:#3C00BC;font-weight: bold'>${myText}</h2>"
    if (type == "button") return "<a style='color:white;text-align:center;font-size:20px;font-weight:bold;background-color:#3C00BC;border:1px solid #000000;box-shadow:3px 4px #575757;border-radius:10px' href='${page}'>${myText}</a>"
}

private def displayHeader(title, data) {
    if (title) {
        if(title.contains("(Paused)")) {
            theName = title - " <span style='color:red'>(Paused)</span>"
        } else {
            theName = title
        }
    }
    section() {
        // display the error message
        if ((data != null) && (data != "")) {
            bMes = "<div style='color:red;text-align:center;font-size:20px;font-weight:bold'>${data}</div>"
        } else {
            bMes = "<div style='color:#3C00BC;text-align:left;font-size:20px;font-weight:bold'>${theName}</div>"
        }
        paragraph "${bMes}"
    }
}

private logDebug(msg) {
    if (settings?.debugOutput) {
        log.debug "${app.label}: $msg"
    }
}

private logDescriptionText(msg) {
    if (settings?.descriptionTextOutput) {
        log.info "${app.label}: $msg"
    }
}

private logWarn(msg) {
    log.warn "${app.label}: $msg"
}

private logError(msg) {
    log.error "${app.label}: $msg"
}
