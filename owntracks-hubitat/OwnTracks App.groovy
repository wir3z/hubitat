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
 *  1.6.5      2024-01-07      - Added secondary hub link.
 *  1.6.6      2024-01-07      - Fixed secondary hub link.
 *  1.6.7      2024-01-08      - Fixed WiFI SSID check that was giving improper "present" when away.
 *  1.6.8      2024-01-09      - Prevent extra Android diagnostic fields from being sent to mobile devices that do not support them.
 *  1.6.9      2024-01-10      - Cleaned up trackerID sent to map.  Removed default hubitat location due to overlap and confusion.
 *  1.6.10     2024-01-11      - Removed the -Delete- name from deleted regions which was preventing iOS from deleting.  Send the users own location/user card back to them so their thumbnail displays on the map.  Fixed iOS crash when receiving invalid data.
 *  1.6.11     2024-01-12      - Added a ability to enable each user to see their own image card on the map.  NOTE:  iOS users will see themselves twice.  Added a delete region from Hubitat only setting.  Added how-to information to the respective sections.  Added member status block.
 *  1.6.12     2024-01-13      - Removed the ability to enable each user to see their own image card on the map due to stability issues.  Added user selectable warning time to mark stale location reports on the Members Status table.  
 *                             - Added location report to the Member status table.  Added ability to check the pin location of regions on Google Maps.
 *  1.6.13     2024-01-14      - Fixed exception with first time configure of the app.  Disabled the 'restart mobile app' due to the OwnTracks Android 2.4.x not starting the ping service after the remote restart.  Added the ability to have the mobile app send a high accuracy location on the next report. Added an auto-request high accuracy location for stale Android locations.
 *  1.6.14     2024-01-15      - Refactored the layout to make it simpler to install/navigate.  Added the ability to reset the app back to the recommended defaults.  Added ability to request a higher accuracy location on a ping/manual location (Android Only).
 *  1.6.15     2024-01-16      - Added support for driver to retrieve local file URL.  Fixed issue when home place was deleted.  Provided quick selection to switch locator priority on main screen.
 *  1.6.16     2024-01-17      - Fixed issue where assigning home would get cleared.
 *  1.6.18     2024-01-17      - Changed home to use timestamp to allow name change.  NOTE: breaking change -- home must be re-selected from the list.  Added an automatic +follow region for iOS transition tracking.
 */

import groovy.transform.Field
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonBuilder
import java.text.SimpleDateFormat

def appVersion() { return "1.6.18" }

@Field static final Map BATTERY_STATUS = [ "0": "Unknown", "1": "Unplugged", "2": "Charging", "3": "Full" ]
@Field static final Map DATA_CONNECTION = [ "w": "WiFi", "m": "Mobile" ]
@Field static final Map TRIGGER_TYPE = [ "p": "Ping", "c": "Region", "r": "Report Location", "u": "Manual" ]
@Field static final Map TOPIC_FORMAT = [ 0: "topicSource", 1: "userName", 2: "deviceID", 3: "eventType" ]
@Field static final Map LOCATOR_PRIORITY = [ 0: "NO_POWER (best accuracy with zero power consumption)", 1: "LOW_POWER (city level accuracy)", 2: "BALANCED_POWER (block level accuracy based on Wifi/Cell)", 3: "HIGH_POWER (most accurate accuracy based on GPS)" ]
@Field static final Map DYNAMIC_INTERVALS = [ "pegLocatorFastestIntervalToInterval": false, "locatorPriority": 3 ]
//@Field static final Map MONITORING_MODES = [ 0: "Manual (user triggered events)", 1: "Significant (standard tracking using Wifi/Cell)", 2: "Move (permanent tracking using GPS)" ]
@Field static final Map MONITORING_MODES = [ 1: "Significant (standard tracking using Wifi/Cell)", 2: "Move (permanent tracking using GPS)" ]
@Field static final Map IOS_PLUS_FOLLOW = [ "rad":1000, "tst":1700000000, "_type":"waypoint", "lon":0.0, "lat":0.0, "desc":"+follow" ]

// Main defaults
@Field String  CHILDPREFIX = "OwnTracks - "
@Field String  MQTT_TOPIC_PREFIX = "owntracks"
@Field Number  INVALID_COORDINATE = 999
@Field String  DEFAULT_homeName = "Home"
@Field Number  DEFAULT_RADIUS = 75
@Field Number  DEFAULT_regionHighAccuracyRadius = 750
@Field Number  DEFAULT_wifiPresenceKeepRadius = 0.750
@Field Boolean DEFAULT_imperialUnits = false
@Field Boolean DEFAULT_regionHighAccuracyRadiusHomeOnly = true
@Field Boolean DEFAULT_warnOnDisabledMember = true
@Field Boolean DEFAULT_warnOnMemberSettings = false
@Field Number  DEFAULT_warnOnNoUpdateHours = 12
@Field Number  DEFAULT_staleLocationWatchdogInterval = 900
@Field Boolean DEFAULT_highAccuracyOnPing = true
@Field Boolean DEFAULT_autoRequestLocation = true
@Field Boolean DEFAULT_highPowerMode = false
@Field Boolean DEFAULT_advancedMode = false
@Field Boolean DEFAULT_descriptionTextOutput = true
@Field Boolean DEFAULT_debugOutput = false
@Field Number  DEFAULT_debugResetHours = 1
// Mobile app location defaults
@Field Number  DEFAULT_monitoring = 1
@Field Number  DEFAULT_locatorPriority = 2
@Field Number  DEFAULT_moveModeLocatorInterval = 30
@Field Number  DEFAULT_locatorDisplacement = 50
@Field Number  DEFAULT_locatorInterval = 60
@Field Number  DEFAULT_ignoreInaccurateLocations = 150
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
    page(name: "configureHubApp")
    page(name: "installationInstructions")
    page(name: "thumbnailCreation")
    page(name: "configureRecorder")
    page(name: "configureSecondaryHub")
    page(name: "recorderInstallationInstructions")
    page(name: "configureLocation")
    page(name: "configureDisplay")
    page(name: "configureRegions")
    page(name: "addRegions")
    page(name: "editRegions")
    page(name: "deleteRegions")
    page(name: "deleteMembers")
    page(name: "resetDefaults")
}

def mainPage() {
    // clear the setting fields
    clearSettingFields()
    app.removeSetting("regionToCheck")
        
    // initialize all fields if they are undefined
    initialize(false)
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
        section(getFormat("title", "OwnTracks Version ${appVersion()}")) {
        }            
        // if we didn't get a token, display the error and stop
        if (oauthStatus != "") {
            section("<h2>${oauthStatus}</h2>") {}
        } else if (state.installed != true) {
            section("<h3>Select '<b>Done</b>' to finsh the initial app installation and then re-select the OwnTracks app to finish configuration.</h3>") {}
        } else {
            section(getFormat("box", "Member Status")) {
                displayMemberStatus()
            }            
            section(getFormat("box", "Installation")) {
                href(title: "Mobile App Installation Instructions", description: "", style: "page", page: "installationInstructions")
                href(title: "Configure Regions", description: "", style: "page", page: "configureRegions")
                input "enabledMembers", "enum", multiple: true, title:(enabledMembers ? '<div>' : '<div style="color:#ff0000">') + 'Select family member(s) to monitor</div>', options: (state.members ? state.members.name.sort() : []), submitOnChange: true
                input name: "imperialUnits", type: "bool", title: "Display imperial units instead of metric units", defaultValue: DEFAULT_imperialUnits, submitOnChange: true
                href(title: "Addtional Hubitat App Settings", description: "", style: "page", page: "configureHubApp")
            }
            section(getFormat("box", "Optional Features")) {
                href(title: "Enabling User Thumbnails", description: "", style: "page", page: "thumbnailCreation")
                href(title: "Enable OwnTracks Recorder", description: "", style: "page", page: "configureRecorder")
                href(title: "Link Secondary Hub", description: "", style: "page", page: "configureSecondaryHub")
            }
            section(getFormat("box", "Advanced Mobile App Settings")) {
                paragraph("The default mobile settings provide the best balance of accuracy/power.  To view or modify advanced settings, enable 'Modify Default Settings'.")
                input name: "highPowerMode", type: "bool", title: "Use GPS for higher accuracy/performance.  <b>NOTE:</b> This will consume more battery but will offer better performance in areas with poor WiFi/Cell coverage. (<b>Android ONLY</b>)", defaultValue: DEFAULT_highPowerMode, submitOnChange: true
                checkLocatorPriority()
                input name: "advancedMode", type: "bool", title: "Modify Default Settings", defaultValue: DEFAULT_advancedMode, submitOnChange: true
                if (advancedMode) {
// Restart is only applicable to Android.  Current Android 2.4.x will restart the app, but fails to restart the ping service
//                input "restartMobileApp", "enum", multiple: true, title:"Select family member(s) to restart their mobile app on next location update. The user will be registered to receive this update once 'Done' is pressed, below, and this list will be automatically cleared.", options: (enabledMembers ? enabledMembers.sort() : enabledMembers)
                    href(title: "Mobile App Location Settings", description: "", style: "page", page: "configureLocation")
                    href(title: "Mobile App Display Settings", description: "", style: "page", page: "configureDisplay")
                }
            }
            section(getFormat("box", "Maintenance")) {
                input "syncMobileSettings", "enum", multiple: true, title:"Select family member(s) to update location, display and region settings on the next location update. The user will be registered to receive this update once 'Done' is pressed, below, and this list will be automatically cleared.", options: (enabledMembers ? enabledMembers.sort() : enabledMembers)
                input "requestLocation", "enum", multiple: true, title:"Select family member(s) to send a high accuracy GPS location on next location update (<b>Android ONLY</b>). The user will be registered to receive this request once 'Done' is pressed, below, and this list will be automatically cleared.", options: (enabledMembers ? enabledMembers.sort() : enabledMembers)
                href(title: "Reset to Recommended Default Settings", description: "", style: "page", page: "resetDefaults")
                href(title: "Delete Family Members", description: "", style: "page", page: "deleteMembers")
            }
            section(getFormat("box", "Logging")) {
                input name: "descriptionTextOutput", type: "bool", title: "Enable Description Text logging", defaultValue: DEFAULT_descriptionTextOutput
                input name: "debugOutput", type: "bool", title: "Enable Debug Logging", defaultValue: DEFAULT_debugOutput
                input name: "debugResetHours", type: "number", title: "Turn off debug logging after this many hours (1..24)", range: "1..24", defaultValue: DEFAULT_debugResetHours
            }
        }
    }
}

def checkLocatorPriority() {
    // check if we need to 
    if (state.highPowerMode != highPowerMode) {
        state.highPowerMode = highPowerMode
        // remove the old enum so we can re-assign it
        app.removeSetting("locatorPriority")
        // change the setting
        if (highPowerMode) {
            app.updateSetting("locatorPriority", [value: DYNAMIC_INTERVALS.locatorPriority, type: "number"])
        } else {
            app.updateSetting("locatorPriority", [value: DEFAULT_locatorPriority, type: "number"])
        }
        // trigger the mobile location update
        setUpdateFlag([ "name":"" ], "updateLocation", true)
        logWarn("Changing locator priority to ${LOCATOR_PRIORITY[locatorPriority.toInteger()]}")
    }
}

def configureHubApp() {
    return dynamicPage(name: "configureHubApp", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Configure Hubitat App")) {
            if (state.imperialUnits != imperialUnits) {
                state.imperialUnits = imperialUnits
                // preload the settings field with the proper units
                app.updateSetting("locatorDisplacement", [value: displayMFtVal(state.locatorDisplacement), type: "number"])
                app.updateSetting("ignoreInaccurateLocations", [value: displayMFtVal(state.ignoreInaccurateLocations), type: "number"])
                app.updateSetting("homeGeoFence", [value: displayMFtVal(state.homeGeoFence), type: "number"])
            }
            input "homeSSID", "string", title:"Enter your 'Home' WiFi SSID(s), separated by commas.  Used to prevent devices from being 'non-present' if currently connected to these WiFi access point(s).", defaultValue: ""
            input name: "regionHighAccuracyRadius", type: "enum", title: "Enable high accuracy reporting when location is between region radius and this value, Recommended=${displayMFtVal(DEFAULT_regionHighAccuracyRadius)}", defaultValue: "${DEFAULT_regionHighAccuracyRadius}", options: (imperialUnits ? [0:'disabled',250:'820 ft',500:'1640 ft',750:'2461 ft',1000:'3281 ft'] : [0:'disabled',250:'250 m',500:'500 m',750:'750 m',1000:'1000 m'])
            input name: "regionHighAccuracyRadiusHomeOnly", type: "bool", title: "High accuracy reporting is used for home region only when selected, all regions if not selected", defaultValue: DEFAULT_regionHighAccuracyRadiusHomeOnly
            input name: "warnOnNoUpdateHours", type: "number", title: "Highlight members on the 'Member Status' that have not reported a location for this many hours (1..168)", range: "1..168", defaultValue: DEFAULT_warnOnNoUpdateHours
            input name: "warnOnDisabledMember", type: "bool", title: "Display a warning in the logs if a family member reports a location but is not enabled", defaultValue: DEFAULT_warnOnDisabledMember
            input name: "warnOnMemberSettings", type: "bool", title: "Display a warning in the logs if a family member app settings are not configured for optimal operation", defaultValue: DEFAULT_warnOnMemberSettings
            input name: "highAccuracyOnPing", type: "bool", title: "Request a high accuracy location from members on their next location report after a ping/manual update to keep location fresh (<b>Android ONLY</b>)", defaultValue: DEFAULT_highAccuracyOnPing
            input name: "autoRequestLocation", type: "bool", title: "Automatically request a high accuracy location from members on their next location report if their 'Last Location Fix' is stale (<b>Android ONLY</b>)", defaultValue: DEFAULT_autoRequestLocation
        }
    }
}

def installationInstructions() {
    return dynamicPage(name: "installationInstructions", title: "", nextPage: "mainPage") {
        def extUri = fullApiServerUrl().replaceAll("null","webhook?access_token=${state.accessToken}")
        section(getFormat("box", "Mobile App Installation Instructions")) {
            paragraph ("This integration requires the <a href='https://owntracks.org/' target='_blank'>OwnTracks</a> app to be installed on your mobile device.\r" +
                       "<b>NOTE:</b>  If you reinstall the OwnTracks app on Hubitat, the host URL below will change, and the mobile devices will need to be updated. \r" +
                       "             This integration currently only supports one device per user for presence detection.  Linking more than one device will cause unreliable presence detection. \r\n\r\n" +
                       "     <b>Mobile App Configuration</b>\r" +
                       "     1. Open the OwnTracks app on the mobile device, and configure the following fields.  <b>Only the settings below need to be changed; leave the rest as defaults.</b>\r" +
                       "          <b>Android</b>\r" +
                       "          <i>Preferences -> Connection\r" +
                       "                 Mode -> HTTP \r" +
                       "                 Host -> <a href='${extUri}'>${extUri}</a> \r" +
                       "                 Identification ->\r" +
                       "                        Username -> Name of the user's phone (IE: 'Kevin') \r" +
                       "                        Device ID -> Optional extra descriptor (IE: 'Phone').  If using OwnTracks recorder, it would be desirable\r" +
                       "                                               to keep this device ID common across device changes, since it logs 'username/deviceID'. \r" +
                       "            Preferences -> Advanced\r" +
                       "                Remote commands -> Selected\r" +
                       "                Remote configuration -> Selected</i>\r\n\r\n" +
                       "          <b>iOS</b>\r" +
                       "          <i>Tap (i) top left, and select 'Settings'.  Only the settings below need to be changed.\r" +
                       "                 Mode -> HTTP \r" +
                       "                 DeviceID -> 2-character user initials that will be displayed on your map (IE: 'KT').  If using OwnTracks recorder, it would be desirable to keep this device ID common across device changes, since it logs 'username/deviceID'. \r" +
                       "                 UserID -> Name of the user's phone (IE: 'Kevin') \r" +
                       "                 URL -> <a href='${extUri}'>${extUri}</a> \r" +
                       "                 cmd -> Selected</i>\r\n\r\n" +
                       "     2. Click the up arrow button in the top right of the map to trigger a 'Send Location Now' to register the device with the Hubitat App."          
                      )
        }
    }
}

def thumbnailCreation() {
    return dynamicPage(name: "thumbnailCreation", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Enabling User Thumbnails")) {
            paragraph ("Creating User Thumbnails on the OwnTracks Mobile App and optional OwnTracks Recorder.\r\n\r\n" +
                       "     1. Create a thumbnail for the user at a maximum resolution 192x192 pixels in JPG format using your computer.\r" +
                       "     2. Name the thumbnail 'MyUser.jpg' where 'MyUser' is the same name as the user name (case sensitive) entered in the mobile app.\r" +
                       "     3. In Hubitat:\r" +
                       "          a. Navigate to 'Settings->File Manager'.\r" +
                       "          b. Select '+ Choose' and select the 'MyUser.jpg' that was created above.\r" +
                       "          c. Select 'Upload'.\r" +
                       "          d. Repeat for any additional users.\r" +
                       "     4. In the OwnTracks Hubitat app:\r" +
                       "          a. Select 'Display', and enable the 'Display user thumbnails on the map.' and then 'Done'.\r" +
                       "          b. Select all users in the 'Select family member(s) to update location, display and region settings on the next location update.' box, and then 'Done'.\r" +
                       "     5. In the OwnTracks Mobile app:\r" +
                       "          a. Select the 'Send Location Now' button, top right of the map screen.  User thumbnails should now populate on the mobile app map.\r"
                      )
            input name: "imageCards", type: "bool", title: "Display user thumbnails on the map.  Needs to have a 'user.jpg' image of maximum resolution 192x192 pixels uploaded to the 'Settings->File Manager'", defaultValue: DEFAULT_imageCards
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
            href(title: "Creating User Thumbnail Instructions", description: "", style: "page", page: "thumbnailCreation")
            paragraph ("2. Select the slider to generate the enabled user's JSON card data in the Hubitat logs:") 
            input name: "generateMemberCardJSON", type: "bool", title: "Create 'trace' outputs for each enabled member in the Hubitat logs.  Slider will turn off once complete.", defaultValue: false, submitOnChange: true
            if (generateMemberCardJSON) {
                logMemberCardJSON()
                app.updateSetting("generateMemberCardJSON",[value: false, type: "bool"])
            }
            paragraph ("3. In the Hubitat log, look for the 'trace' output that looks like this:\r" +
                       "          For recorder cards, copy the bold JSON text between | |, and save this file to 'STORAGEDIR/cards/myuser/myuser.json' (user name is in lower case): \r" + 
                       "          |<b>{\"_type\":\"card\",\"name\":\"MyUser\",\"face\":\"....\",\"tid\":\"MyUser\"}</b>|\r\n\r\n" + 
                       "4. Save the <b>{\"_type\":\"card\",\"name\":\"MyUser\",\"face\":\"....\",\"tid\":\"MyUser\"}</b> to a text file with the name <b>myuser.json</b> (user name is in lower case), as listed in step 3.\r\n\r\n" +
                       "5. Create the cards folder if it does not exist:\r" +
                       "          /<b>[HOME_PATH]</b>/docker/volumes/recorder_store/_data/cards\r\n\r\n" +
                       "6. To add user cards, copy card file for each user to the following docker path:\r" +
                       "          /<b>[HOME_PATH]</b>/docker/volumes/recorder_store/_data/cards/<b>myuser/myuser</b>.json\r\n\r\n" +
                       "7. Alternatively, if you choose to have user/device specific cards, you would name the card 'myuser-mydevice.json' (user/device name is in lower case), and save it in the following docker path:\r" +
                       "          /<b>[HOME_PATH]</b>/docker/volumes/recorder_store/_data/cards/<b>myuser/mydevice/myuser-mydevice</b>.json\r"
                      )
        }
    }
}

def configureRecorder() {
    return dynamicPage(name: "configureRecorder", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Recorder Configuration")) {
            paragraph("The <a href='https://owntracks.org/booklet/clients/recorder/' target='_blank'>OwnTracks Recorder</a> (optional) can be installed for local tracking.")
            input name: "recorderURL", type: "text", title: "HTTP URL of the OwnTracks Recorder.  It will be in the format <b>'http://enter.your.recorder.ip:8083/pub'</b>, assuming using the default port of 8083.", defaultValue: ""
            input name: "enableRecorder", type: "bool", title: "Enable location updates to be sent to the Recorder URL", defaultValue: false, submitOnChange: true
            if (!recorderURL) {
                app.updateSetting("enableRecorder",[value: false, type: "bool"])
            }
        }
        section() {
            href(title: "Installing OwnTracks Recorder and Configuring User Card Instructions", description: "", style: "page", page: "recorderInstallationInstructions")
        }
    }
}

def configureSecondaryHub() {
    return dynamicPage(name: "configureSecondaryHub", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Secondary Hub Configuration")) {
            paragraph ("Allows for OwnTracks to daisy chain to multiple hubs.\r" +
                       "1. On the secondary hub, paste the host/URL from 'Mobile App Installation Instructions -> Mobile App Configuration' below.\r" +
                       "2. Select the slider to enable mobile updates to be sent to the secondary hub UR as they arrive.\r" +
                       "3. Additional hubs can be daisy chained by repeating the above on the third hub, and adding it to the second hub.\r"
            )
            input name: "secondaryHubURL", type: "text", title: "Host URL of the Seconday Hub from the OwnTracks app 'Mobile App Installation Instructions' page.", defaultValue: ""
            input name: "enableSecondaryHub", type: "bool", title: "Enable location updates to be sent to the secondary hub URL", defaultValue: false, submitOnChange: true
            if (!secondaryHubURL) {
                app.updateSetting("enableSecondaryHub",[value: false, type: "bool"])
            }
        }
    }
}

def configureLocation() {
    return dynamicPage(name: "configureLocation", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Mobile App Location Configuration")) {
            input name: "monitoring", type: "enum", title: "Location reporting mode, Recommended=${MONITORING_MODES[DEFAULT_monitoring]}", required: true, options: MONITORING_MODES, defaultValue: DEFAULT_monitoring, submitOnChange: true
            // This is replaced with high accuracy selection on the main page
//            input name: "locatorPriority", type: "enum", title: "Source/power setting for location updates, Recommended=${LOCATOR_PRIORITY[DEFAULT_locatorPriority]}", required: true, options: LOCATOR_PRIORITY, defaultValue: DEFAULT_locatorPriority, submitOnChange: true
            input name: "ignoreInaccurateLocations", type: "number", title: "Do not send a location if the accuracy is greater than the given (${getSmallUnits()}) (0..${displayMFtVal(2000)}) Recommended=${displayMFtVal(DEFAULT_ignoreInaccurateLocations)}", required: true, range: "0..${displayMFtVal(2000)}", defaultValue: displayMFtVal(DEFAULT_ignoreInaccurateLocations)
            input name: "ignoreStaleLocations", type: "decimal", title: "Number of days after which location updates from friends are assumed stale and removed (0.0..7.0), Recommended=${DEFAULT_ignoreStaleLocations}", required: true, range: "0.0..7.0", defaultValue: DEFAULT_ignoreStaleLocations
            input name: "ping", type: "number", title: "Device will send a location interval at this heart beat interval (minutes) (15..360), Recommended=${DEFAULT_ping}", required: true, range: "15..60", defaultValue: DEFAULT_ping
            input name: "pegLocatorFastestIntervalToInterval", type: "bool", title: "Request that the location provider deliver updates no faster than the requested locator interval, Recommended '${DEFAULT_pegLocatorFastestIntervalToInterval}'", defaultValue: DEFAULT_pegLocatorFastestIntervalToInterval
            paragraph("<h3><b>Settings for Significant Monitoring Mode</b></h3>")
            input name: "locatorDisplacement", type: "number", title: "How far the device travels (${getSmallUnits()}) before receiving another location update, Recommended=${displayMFtVal(DEFAULT_locatorDisplacement)}  <i><b>This value needs to be less than the minimum configured region radius for automations to trigger.</b></i>", required: true, range: "0..${displayMFtVal(1000)}", defaultValue: displayMFtVal(DEFAULT_locatorDisplacement)
            input name: "locatorInterval", type: "number", title: "Device will not report location updates faster than this interval (seconds) unless moving.  When moving, Android uses this 'locatorInterval/6' or '5-seconds' (whichever is greater, unless 'locatorInterval' is less than 5-seconds, then 'locatorInterval' is used), Recommended=60  <i><b>Requires the device to move the above distance, otherwise no update is sent.</b></i>", required: true, range: "0..3600", defaultValue: DEFAULT_locatorInterval
            // IE:  locatorInterval=0-seconds,   then locations every 0-seconds  if moved locatorDisplacement meters
            //      locatorInterval=5-seconds,   then locations every 5-seconds  if moved locatorDisplacement meters
            //      locatorInterval=10-seconds,  then locations every 5-seconds  if moved locatorDisplacement meters
            //      locatorInterval=15-seconds,  then locations every 5-seconds  if moved locatorDisplacement meters
            //      locatorInterval=30-seconds,  then locations every 5-seconds  if moved locatorDisplacement meters
            //      locatorInterval=60-seconds,  then locations every 10-seconds if moved locatorDisplacement meters
            //      locatorInterval=120-seconds, then locations every 20-seconds if moved locatorDisplacement meters
            //      locatorInterval=240-seconds, then locations every 40-seconds if moved locatorDisplacement meters          
            // assign the app defaults for move monitoring modes - we will use a larger interval in case the user accidentally switches to 'move mode'
            paragraph("<h3><b>Settings for Move Monitoring Mode</b></h3>")
            input name: "moveModeLocatorInterval", type: "number", title: "How often should locations be continuously sent from the device while in 'Move' mode (seconds) (2..3600), Recommended=${DEFAULT_moveModeLocatorInterval}.  <i><b>'Move' mode will result in higher battery consumption.</b></i>", required: true, range: "2..3600", defaultValue: DEFAULT_moveModeLocatorInterval
        }
        setUpdateFlag([ "name":"" ], "updateLocation", true)
    }
}

def configureDisplay() {
    return dynamicPage(name: "configureDisplay", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Mobile App Display Configuration")) {
            input name: "replaceTIDwithUsername", type: "bool", title: "Replace the 'TID' (tracker ID) with 'username' for displaying a name on the map and recorder", defaultValue: DEFAULT_replaceTIDwithUsername
            input name: "notificationEvents", type: "bool", title: "Notify about received events", defaultValue: DEFAULT_notificationEvents
            input name: "pubExtendedData", type: "bool", title: "Include extended data in location reports", defaultValue: DEFAULT_pubExtendedData
            input name: "enableMapRotation", type: "bool", title: "Allow the map to be rotated", defaultValue: DEFAULT_enableMapRotation
            input name: "showRegionsOnMap", type: "bool", title: "Display the region pins/bubbles on the map", defaultValue: DEFAULT_showRegionsOnMap
            input name: "notificationLocation", type: "bool", title: "Show last reported location in ongoing notification banner", defaultValue: DEFAULT_notificationLocation
            input name: "notificationGeocoderErrors", type: "bool", title: "Display Geocoder errors in the notification banner", defaultValue: DEFAULT_notificationGeocoderErrors
        }
        setUpdateFlag([ "name":"" ], "updateDisplay", true)
    }
}

def configureRegions() {
    return dynamicPage(name: "configureRegions", title: "", nextPage: "mainPage") {
        // clear the setting fields
        clearSettingFields()        
        section(getFormat("box", "Configure Regions")) {
            input "regionToCheck", "enum", multiple: false, title:"Select region to check coordinates.  ${(regionToCheck ? "<a href='https://maps.google.com/?q=${state.places.find {it.desc==regionToCheck}?.lat},${state.places.find {it.desc==regionToCheck}?.lon}' target='_blank'>Click to verify the coordinates of '${regionToCheck}'.</a>" : "")}", options: (state.places ? state.places.desc.sort() : []), submitOnChange: true
            href(title: "Add Regions", description: "", style: "page", page: "addRegions")
            href(title: "Edit Regions", description: "", style: "page", page: "editRegions")
            href(title: "Delete Regions", description: "", style: "page", page: "deleteRegions")
        }
        section(getFormat("line", "")) {
            input "homePlace", "enum", multiple: false, title:(homePlace ? '<div>' : '<div style="color:#ff0000">') + "Select your 'Home' place. ${(homePlace ? "<a href='https://maps.google.com/?q=${state.places.find {it.tst==homePlace}?.lat},${state.places.find {it.tst==homePlace}?.lon}' target='_blank'>Click to verify the coordinates of '${state.places.find {it.tst==homePlace}?.desc}.'</a>" : "Use 'Configure Regions'->'Add Regions' to create a home location.")}" + '</div>', options: (state.places ? state.places.collectEntries{[it.tst, it.desc]} : []), submitOnChange: true
//            input "homePlace", "enum", multiple: false, title:"Select your 'Home' place. ", options: (state.places ? state.places.collectEntries{[it.tst, it.desc]} : []), submitOnChange: true
            assignHome()
            input "getMobileRegions", "enum", multiple: true, title:"Hubitat can retrieve regions from a member's OwnTracks mobile device and merge them into the Hubitat region list. Select family member(s) to retrieve their region list on next location update.", options: (enabledMembers ? enabledMembers.sort() : enabledMembers)
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
            paragraph ("1. Add the region to be information.\r" +
                       "2. Once 'Save' is selected, all enabled members will automatically receive the changes on their next location report.\r" 
            )
            input "regionName", "text", title: "Name of region", submitOnChange: true
            input name: "regionRadius", type: "number", title: "Detection radius for region (${getSmallUnits()}) (${displayMFtVal(50)}..${displayMFtVal(1000)})", range: "${displayMFtVal(50)}..${displayMFtVal(1000)}", defaultValue: displayMFtVal(DEFAULT_RADIUS)
            input name: "regionLat", type: "double", title: "Region Latitude (-90.0..90.0)", range: "-90.0..90.0", defaultValue: location.getLatitude()
            input name: "regionLon", type: "double", title: "Region Longitude (-180.0..180.0)", range: "-180.0..180.0", defaultValue: location.getLongitude()

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
            paragraph ("1. Select the region to be edited.\r" +
                       "2. Once 'Save' is selected, all enabled members will automatically receive the changes on their next location report.\r" 
            )
            input "regionName", "enum", multiple: false, title:"Select region to edit", options: (state.places ? state.places.desc.sort() : []), submitOnChange: true
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
                input name: "regionLat", type: "double", title: "Region Latitude (-90.0..90.0)", required: true, range: "-90.0..90.0"
                input name: "regionLon", type: "double", title: "Region Longitude (-180.0..180.0)", required: true, range: "-180.0..180.0"

                input name: "editButton", type: "button", title: "Save", state: "submit"
            }
        }
    }
}

def deleteRegions() {
    return dynamicPage(name: "deleteRegions", title: "", nextPage: "configureRegions") {
        section(getFormat("box", "Delete Region(s)")) {
            if (state.submit) {
                paragraph "<b>${getFormat("redText", appButtonHandler("deleteButton"))}</b>"
                state.submit = ""
            }
            if (deleteFromHubitatOnly) {
                paragraph ("1. Select the region(s) to be deleted from Hubitat.\r" +
                           "2. Manually delete the region(s) from each mobile device.\r" 
                )
            } else {
                paragraph ("1. Select the region(s) to be deleted from Hubitat and the mobile devices.\r" +
                           "2. The region(s) will be assigned an invalid lat/lon.\r" + 
                           "3. The region(s) will remain in the list until <b>ALL</b> enabled users have sent a location report.\r" + 
                           "4. Once the last user has sent a location report, the region(s) will be deleted from Hubitat.\r"
                )
                paragraph("<b>NOTE:  OwnTracks Android 2.4.12 does not delete regions, and requires them to be manually deleted from the mobile device.</b>")
            }
            input name: "deleteFromHubitatOnly", type: "bool", title: "Delete region from Hubitat <b>ONLY</b> and not the mobile devices.", defaultValue: false, submitOnChange: true
            input "regionName", "enum", multiple: true, title:"Select regions to delete.", options: (state.places ? state.places.desc.sort() : []), submitOnChange: true
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
                paragraph "<b>${getFormat("redText", appButtonHandler("deleteMembersButton"))}</b>"
                state.submit = ""
            }
            input "deleteFamilyMembers", "enum", multiple: true, title:"Select family member(s) to delete.", options: state.members.name.sort(), submitOnChange: true
            
            paragraph("<b>NOTE: Selected user(s) will be deleted from the app and their corresponding child device will be removed.  Ensure no automations are dependent on their device before proceeding!</b>")
            if (deleteFamilyMembers) {
                input name: "deleteMembersButton", type: "button", title: "Delete", state: "submit"
            }
        }
    }
}

def resetDefaults() {
    return dynamicPage(name: "resetDefaults", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Reset to Recommended Default Settings")) {
            if (state.submit) {
                paragraph "<b>${getFormat("redText", appButtonHandler("resetDefaultsButton"))}</b>"
                state.submit = ""
            }
            paragraph("Reset Hubitat and Mobile Settings to Recommended Defaults.  <b>NOTE:  Members, Regions, Recorder and Secondary Hub settings will not be deleted.</b>")
            input name: "resetDefaultsButton", type: "button", title: "Reset", state: "submit"
        }
    }
}

String appButtonHandler(btn) {
    def success = false
    def updateMember = false
    
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
                    updateMember = true
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
            updateMember = true
        break
        case "deleteButton":
            // unvalidate all the places that need to be removed
            regionName.each { desc ->
                place = state.places.find {it.desc==desc}
                // check if we are deleting our home location
                if (homePlace == place.tst) {
                    state.home = []
                    app.removeSetting("homePlace")
                }
                if (deleteFromHubitatOnly) {
                    // remove the place from our current list but don't trigger a member update
                    deleteIndex = state.places.findIndexOf {it.desc==desc}
                    if (deleteIndex >= 0) {
                        state.places.remove(deleteIndex)
                    }
                    updateMember = false
                } else {
                    // invalidate the coordinates to flag it for deletion.  iOS is checking the name as a key, Android the timestamp
                    place.lat = INVALID_COORDINATE
                    place.lon = INVALID_COORDINATE
                    updateMember = true
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
        case "resetDefaultsButton":
            initialize(true)
            result = "Settings reset to recommended defaults."
            logWarn(result)
        break
        default:
            result = ""
            logWarn ("Unhandled button: $btn")
        break
    }

    if (success) {
        // clear the setting fields
        clearSettingFields()
        // force an update of all users
        setUpdateFlag([ "name":"" ], "updateWaypoints", updateMember)
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
    initialize(true)
    // clear the flag to indicate we finish installing the app -- we need this to have the map install fully in order to get a fixed URL for the mobile app
    state.installed = false
    updated()
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

def initialize(forceDefaults) {
    // initialize the system states if undefined
    if (state.accessToken == null) state.accessToken = ""
    if (state.members == null) state.members = []
    if (state.home == null) state.home = []
    if (state.places == null) state.places = []
    if (state.locatorDisplacement == null) state.locatorDisplacement = DEFAULT_locatorDisplacement
    if (state.ignoreInaccurateLocations == null) state.ignoreInaccurateLocations = DEFAULT_ignoreInaccurateLocations
    if (state.homeGeoFence == null) state.homeGeoFence = DEFAULT_RADIUS
    if (state.imperialUnits == null) state.imperialUnits = DEFAULT_imperialUnits
    if (state.highPowerMode == null) state.highPowerMode = DEFAULT_highPowerMode
    if (forceDefaults && (state.home == [])) app.removeSetting("homePlace")
    
    // in order to set the default enum's we need to remove the existing setting
    if (forceDefaults) {
        app.removeSetting("monitoring")
        app.removeSetting("locatorPriority")
        app.removeSetting("regionHighAccuracyRadius")
    }
    
    // assign hubitat defaults
    if (homeSSID == null) app.updateSetting("homeSSID", [value: "", type: "string"])
    if (imperialUnits == null) app.updateSetting("imperialUnits", [value: DEFAULT_imperialUnits, type: "bool"])
    if (regionHighAccuracyRadius == null) app.updateSetting("regionHighAccuracyRadius", [value: DEFAULT_regionHighAccuracyRadius, type: "number"])
    if (forceDefaults) app.updateSetting("descriptionTextOutput", [value: DEFAULT_descriptionTextOutput, type: "bool"])
    if (forceDefaults) app.updateSetting("debugOutput", [value: DEFAULT_debugOutput, type: "bool"])
    if (forceDefaults || (regionHighAccuracyRadiusHomeOnly == null)) app.updateSetting("regionHighAccuracyRadiusHomeOnly", [value: DEFAULT_regionHighAccuracyRadiusHomeOnly, type: "bool"])
    if (forceDefaults || (warnOnDisabledMember == null)) app.updateSetting("warnOnDisabledMember", [value: DEFAULT_warnOnDisabledMember, type: "bool"])
    if (forceDefaults || (warnOnMemberSettings == null)) app.updateSetting("warnOnMemberSettings", [value: DEFAULT_warnOnMemberSettings, type: "bool"])
    if (forceDefaults || (warnOnNoUpdateHours == null)) app.updateSetting("warnOnNoUpdateHours", [value: DEFAULT_warnOnNoUpdateHours, type: "number"])
    if (forceDefaults || (debugResetHours == null)) app.updateSetting("debugResetHours", [value: DEFAULT_debugResetHours, type: "number"])
    if (forceDefaults || (highAccuracyOnPing == null)) app.updateSetting("DEFAULT_highAccuracyOnPing", [value: DEFAULT_highAccuracyOnPing, type: "bool"])    
    if (forceDefaults || (autoRequestLocation == null)) app.updateSetting("autoRequestLocation", [value: DEFAULT_autoRequestLocation, type: "bool"])
    if (forceDefaults || (advancedMode == null)) app.updateSetting("advancedMode", [value: DEFAULT_advancedMode, type: "bool"])
    if (forceDefaults || (highPowerMode == null)) app.updateSetting("highPowerMode", [value: DEFAULT_highPowerMode, type: "bool"])
    
    // assign the defaults to the mobile app settings
    if (monitoring == null) app.updateSetting("monitoring", [value: DEFAULT_monitoring, type: "number"])
    if (locatorPriority == null) app.updateSetting("locatorPriority", [value: DEFAULT_locatorPriority, type: "number"])
    if (forceDefaults || (moveModeLocatorInterval == null)) app.updateSetting("moveModeLocatorInterval", [value: DEFAULT_moveModeLocatorInterval, type: "number"])
    if (forceDefaults || (locatorDisplacement == null)) app.updateSetting("locatorDisplacement", [value: DEFAULT_locatorDisplacement, type: "number"])
    if (forceDefaults || (locatorInterval == null)) app.updateSetting("locatorInterval", [value: DEFAULT_locatorInterval, type: "number"])
    if (forceDefaults || (ignoreInaccurateLocations == null)) app.updateSetting("ignoreInaccurateLocations", [value: DEFAULT_ignoreInaccurateLocations, type: "number"])
    if (forceDefaults || (ignoreStaleLocations == null)) app.updateSetting("ignoreStaleLocations", [value: DEFAULT_ignoreStaleLocations, type: "decimal"])
    if (forceDefaults || (ping == null)) app.updateSetting("ping", [value: DEFAULT_ping, type: "number"])
    if (forceDefaults || (pegLocatorFastestIntervalToInterval == null)) app.updateSetting("pegLocatorFastestIntervalToInterval", [value: DEFAULT_pegLocatorFastestIntervalToInterval, type: "bool"])
    if (forceDefaults || (imageCards == null)) app.updateSetting("imageCards", [value: DEFAULT_imageCards, type: "bool"])
    if (forceDefaults || (replaceTIDwithUsername == null)) app.updateSetting("replaceTIDwithUsername", [value: DEFAULT_replaceTIDwithUsername, type: "bool"])
    if (forceDefaults || (notificationEvents == null)) app.updateSetting("notificationEvents", [value: DEFAULT_notificationEvents, type: "bool"])
    if (forceDefaults || (pubExtendedData == null)) app.updateSetting("pubExtendedData", [value: DEFAULT_pubExtendedData, type: "bool"])
    if (forceDefaults || (enableMapRotation == null)) app.updateSetting("enableMapRotation", [value: DEFAULT_enableMapRotation, type: "bool"])
    if (forceDefaults || (showRegionsOnMap == null)) app.updateSetting("showRegionsOnMap", [value: DEFAULT_showRegionsOnMap, type: "bool"])
    if (forceDefaults || (notificationLocation == null)) app.updateSetting("notificationLocation", [value: DEFAULT_notificationLocation, type: "bool"])
    if (forceDefaults || (notificationGeocoderErrors == null)) app.updateSetting("notificationGeocoderErrors", [value: DEFAULT_notificationGeocoderErrors, type: "bool"])
    // add the iOS +follow location to allow for tranistion updates    
    addPlace([ "name":"" ], IOS_PLUS_FOLLOW, false) 
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
        // if we selected member(s) to restart their mobile app
        if (settings?.requestLocation.find {it==member.name}) {
            member.requestLocation = true
        }
        
        // if the configuration has changed, trigger the member update
        if (syncSettings) {
            member.updateLocation = true
            member.updateDisplay = true
            member.updateWaypoints = true
        }
    }
    // clear the settings flags to prevent the configurations from being forced to the display on each entry
    app.updateSetting("requestLocation",[value:"",type:"enum"])
    app.updateSetting("restartMobileApp",[value:"",type:"enum"])
    app.updateSetting("syncMobileSettings",[value:"",type:"enum"])
    app.updateSetting("getMobileRegions",[value:"",type:"enum"])
    
    // save the values to allow for imperial/metric selection
    state.locatorDisplacement             = convertToMeters(locatorDisplacement)
    state.ignoreInaccurateLocations       = convertToMeters(ignoreInaccurateLocations)

    // store the home information
    assignHome()
    
    // if we have selected to automatically request a high accuracy location fix, schedule the watchdog
    if (autoRequestLocation) {
        locationFixWatchdog()
    }
    // set the flag to indicate we installed the app
    state.installed = true
    // clear the debug logging if set
    if (debugOutput) {
        runIn(debugResetHours*3600, resetLogging)
    }
}

def refresh() {
}

def childGetWarnOnNonOptimalSettings() {
    // return with the log setting
    return (warnOnMemberSettings)
}

def getImageURL(memberName) {
    // return with path to the user card   
    return ("http://${location.hubs[0].getDataValue("localIP")}/local/${memberName}.jpg")
}

def assignHome() {
    findPlace = state.places.find {it.tst == homePlace}
    if (findPlace) {
        state.home = [ name:findPlace.desc, latitude:findPlace.lat, longitude:findPlace.lon, geofence:(findPlace.rad.toInteger()/1000), tst:findPlace.tst ]
    } else {
        state.home = []
        logError("No 'Home' location has been defined.  Create a 'Home' region to enable presence detection.")
    }
}

def locationFixWatchdog() {
    logDebug("Check members for stale locations.")
    // update each member with their last report times
    checkStaleMembers()
    // reschedule the watchdog
    runIn(DEFAULT_staleLocationWatchdogInterval, locationFixWatchdog)
}

def checkStaleMembers() {
    // loop through all the members
    state.members.each { member->
        // generate the stale report times
        if (member.lastReportTime) {               
            long lastReportTime = member.lastReportTime.toLong()
            member.lastReportDate = new SimpleDateFormat("E h:mm a   yyyy-MM-dd").format(new Date(lastReportTime))
            // true if no update the selected number of hours
            member.staleReport = ((now() - lastReportTime) > (warnOnNoUpdateHours * 3600000))
            // number of hours since last report
            member.numberHoursReport = ((now() - lastReportTime) / 3600000).toInteger()
        } else {
            // force a stale report if no time was reported
            member.staleReport = true
            member.lastReportDate = "None"
            member.numberHoursReport = "?"
        }
        // generate the stale location times
        if (member.timeStamp) {       
            long lastFixTime = member.timeStamp.toLong() * 1000
            member.lastFixDate = new SimpleDateFormat("E h:mm a   yyyy-MM-dd").format(new Date(lastFixTime))
            // true if no update the selected number of hours
            member.staleFix = ((now() - lastFixTime) > (warnOnNoUpdateHours * 3600000))
            // number of hours since last report
            member.numberHoursFix = ((now() - lastFixTime) / 3600000).toInteger()
        } else {
            // force a stale fix if no time was reported
            member.staleFix = true
            member.lastFixDate = "None"
            member.numberHoursFix = "?"
        }
    
        // if auto request location is enabled and the position fix is stale, flag the user
        if (autoRequestLocation && member.staleFix) {
            member.requestLocation = true
            logDescriptionText("${member.name}'s position is stale.  Requesting a high accuracy location update.")
        }
    }
}

def displayMemberStatus() {
    String tableData = "";
    
    if (state.members) {
        tableData += '<div style="overflow-x:auto;">'
        tableData += '<font size=3><table align="left" style="width:100%">'    
        tableData += '<col width="170">'   
    
        tableData += '<tr>'
        tableData += '<th>Member</th>'
        tableData += '<th>Last Location Report</th>'
        tableData += '<th>Last Location Fix</th>'
        tableData += '<th>Update Region</th>'
        tableData += '<th>Update Location</th>'
        tableData += '<th>Update Display</th>'
        tableData += '<th>Get Regions</th>'
        tableData += '<th>Request Location</th>'
//        tableData += '<th>Restart App</th>'
        tableData += '</tr>'
    
        // update each member with their last report times
        checkStaleMembers()
        // loop through all the members
        state.members.each { member->
            // check if member is enabled
            memberEnabled = settings?.enabledMembers.find {it==member.name}
                    
            tableData += '<tr>'
            tableData += (memberEnabled ? ((member.staleFix || member.staleReport) ? '<td style="color:#ff0000">' + member.name + '</td>' : '<td style="color:#017000">' + member.name + '</td>') : '<td style="color:#b3b3b3"><s>' + member.name + '</s></td>')
            tableData += (memberEnabled ? ((member.staleReport ? '<td style="color:#ff0000">' + member.lastReportDate + ' (' + member.numberHoursReport + ' hrs ago)' : '<td>' + member.lastReportDate) + '</td>') : '<td style="color:#b3b3b3"><s>' + member.lastReportDate + '</s></td>')
            tableData += (memberEnabled ? ((member.staleFix ? '<td style="color:#ff0000">' + member.lastFixDate + ' (' + member.numberHoursFix + ' hrs ago)' : '<td>' + member.lastFixDate) + '</td>') : '<td style="color:#b3b3b3"><s>' + member.lastFixDate + '</s></td>')
            tableData += (memberEnabled ? (member.updateWaypoints ? '<td style="color:#ff9900">Pending' : '<td>No') + '</td>' : '<td style="color:#b3b3b3"><s>--</s></td>')
            tableData += (memberEnabled ? (member.updateLocation ? '<td style="color:#ff9900">Pending' : '<td>No') + '</td>' : '<td style="color:#b3b3b3"><s>--</s></td>')
            tableData += (memberEnabled ? (member.updateDisplay ? '<td style="color:#ff9900">Pending' : '<td>No') + '</td>' : '<td style="color:#b3b3b3"><s>--</s></td>')
            tableData += (memberEnabled ? (member.getRegions ? '<td style="color:#ff9900">Pending' : '<td>No') + '</td>' : '<td style="color:#b3b3b3"><s>--</s></td>')
            tableData += (memberEnabled ? (member.requestLocation ? '<td style="color:#ff9900">Pending' : '<td>No') + '</td>' : '<td style="color:#b3b3b3"><s>--</s></td>')
//            tableData += (memberEnabled ? (member.restartApp ? '<td style="color:#ff9900">Pending' : '<td>No') + '</td>' : '<td style="color:#b3b3b3"><s>--</s></td>')
            tableData += '</tr>'
        }
        tableData += '</table></font>'
        tableData += '</div>'
    } else {
        tableData = "<h3>'Select family member(s) to monitor' to add members.</h3>"
    }
    paragraph( tableData )
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
                state.members << [ name:sourceName, deviceID:sourceDeviceID, id:null, timeStamp:(now()/1000).toInteger(), updateWaypoints:true, updateLocation:true, updateDisplay:true, dynamicLocaterAccuracy:false, restartApp:false, getRegions:false, requestLocation:false ]
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
                        // Pass the location to a secondary hub with OwnTracks running
                        if (secondaryHubURL && enableSecondaryHub) {
                            def postParams = [ uri: secondaryHubURL, requestContentType: 'application/json', contentType: 'application/json', headers: parsePostHeaders(request.headers), body : (new JsonBuilder(data)).toPrettyString() ]
                            asynchttpPost("httpCallbackMethod", postParams)
                        }

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
                            asynchttpPost("httpCallbackMethod", postParams)
                        }
                    break
                    case "waypoint":
                        // append/update to the places list
                        addPlace(findMember, data, true)
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

def httpCallbackMethod(response, data) {
    if (response.status == 200) {
        logDebug "Posted successfully to OwnTracks URL."
    } else {
        logWarn "OwnTracks HTTP response: ${response.status}, with error: ${response.getErrorMessage()}"
    }
}

def isSSIDMatch(dataString, deviceID) {
    result = false
    // check for matching SSID in the list
    if (dataString && deviceID.currentValue("SSID")) {
        // split the list in tokens, and trim the leading/trailing whitespace
        SSIDList = dataString.split(',')
        SSIDList.each { SSID ->
            if (SSID.trim() == deviceID.currentValue("SSID")) {
                result = true
            }
        }
    }
    
    return (result)
}

def updateDevicePresence(member, data) {
    // save the position and timestamp so we can push to other users
    member.lastReportTime = now()
    member.latitude       = data?.lat
    member.longitude      = data?.lon
    member.timeStamp      = data?.tst
    member.battery        = data?.batt
    member.accuracy       = data?.acc
    member.altitude       = data?.alt
    member.speed          = data?.vel
    member.trackerID      = data?.tid
    member.bs             = data?.bs
    member.wifi           = data?.wifi
    member.hib            = data?.hib
    member.ps             = data?.ps
    member.bo             = data?.bo
    member.loc            = data?.loc

    // update the presence information for the member
    try {
        // find the appropriate child device based on app id and the device network id
        def deviceWrapper = getChildDevice(member.id)
        logDebug("Updating '${(data.event ? "Event $data.event" : (data.t ? TRIGGER_TYPE[data.t] : "Location"))}' presence for member $deviceWrapper")
        // update the image URL
        deviceWrapper.sendEvent( name: "imageURL", value: getImageURL(member.name) )

        // check if the user defined a home place
        if (state.home) {
            // append the distance from home to the data packet
            data.currentDistanceFromHome = getDistanceFromHome(data)
    
            // if we are within our home geofence or connected to a listed SSID and within the next geofence
            if ( (data.currentDistanceFromHome <= state.home.geofence) || ((data.currentDistanceFromHome < DEFAULT_wifiPresenceKeepRadius) && isSSIDMatch(homeSSID, deviceWrapper)) ) {
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
        } else {
            data.currentDistanceFromHome = 0.0
            logWarn("No 'Home' location has been defined.  Create a 'Home' region to enable presence detection.")
        }
        
        // update the child information
        deviceWrapper.generatePresenceEvent(data)
    } catch(e) {
        logError("updateDevicePresence: Exception for member: ${member.name}  $e")
    }
}

def checkRegionConfiguration(member, data) {
    // if we configured the high accuracy region and a home has been defined
    if (regionHighAccuracyRadius && state.home) {
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

        // check if we are outside our region radius, and within our greater than the radius + regionHighAccuracyRadius
        return (createConfiguration(member, ((closestWaypointDistance > closestWaypointRadius) && (closestWaypointDistance < (closestWaypointRadius + regionHighAccuracyRadius.toDouble())))))
    }
}

def createConfiguration(member, useDynamicLocaterAccuracy) {
    // check if we need to force a high accuracy update
    if (useDynamicLocaterAccuracy) {
        // switch to locatorPriority=high power and pegLocatorFastestIntervalToInterval=false (dynamic interval)
        configurationList = [ "_type": "configuration",
                             "pegLocatorFastestIntervalToInterval": DYNAMIC_INTERVALS.pegLocatorFastestIntervalToInterval,
                             "locatorPriority": DYNAMIC_INTERVALS.locatorPriority,
                            ]
    } else {
        // switch to settings.  Recommended locatorPriority=balanced power and pegLocatorFastestIntervalToInterval=true (fixed interval)
        configurationList = [ "_type": "configuration",
                             "pegLocatorFastestIntervalToInterval": pegLocatorFastestIntervalToInterval,
                             "locatorPriority": locatorPriority.toInteger(),
                            ]
    }  

    // check if we had a change, and then update the device configuration
    if (member.requestLocation || (member?.dynamicLocaterAccuracy != useDynamicLocaterAccuracy)) {
        // assign the new state
        member.dynamicLocaterAccuracy = useDynamicLocaterAccuracy
        // clear the flag since this is a high accuracy request
        member.requestLocation = false        
        // return with the dynamic configuration
        return( [ "_type":"cmd","action":"setConfiguration", "configuration": configurationList ] )
    } else {
        // return nothing
        return
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
        addPlace(findMember, waypoint, true)
    }
    logDebug("Updating places: ${state.places}")
}

def addPlace(findMember, data, verboseAdd) {
    // create a new map removing the MQTT topic
    def newPlace = [ "_type": "${data._type}", "desc": "${data.desc}", "lat": "${data.lat}", "lon": "${data.lon}", "rad": "${data.rad}", "tst": "${data.tst}" ]

    // check if we have an existing place with the same timestamp
    place = state.places.find {it.tst==newPlace.tst}
    
    // no changes to existing place
    if (place == newPlace) {
        if (verboseAdd) {
            logDescriptionText("Skipping, no change to place: ${newPlace}")
        }
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
        setUpdateFlag(findMember, "updateWaypoints", true)
    }
}

private def setUpdateFlag(currentMember, newSetting, newValue) {
    // loop through all the enabled members
    settings?.enabledMembers.each { enabledMember->    
        member = state.members.find {it.name==enabledMember}
        // don't set the flag for the member that triggered the update
        if (currentMember.name != member.name) {
            member."$newSetting" = newValue
            logDebug("${newSetting} for user ${member.name}: ${newValue}")
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
        // we need to send the originating member's location back to them for their user card to be displayed on the map
        member = state.members.find {it.name==enabledMember}
        // Don't send the member's location back to them.  NOTE: iOS users will make a duplicate of themselves, and Android has a lag between the app displayed thumbnail and the current phone location
        if (currentMember != member) {
            // populating the tracker ID field with a name allows the name to be displayed in the Friends list and map bubbles and load the OwnTrack support parameters
            def memberLocation = [ "_type": "location", "t": "u", "lat": member.latitude, "lon": member.longitude, "tst": member.timeStamp ]

            // check if fields are valid before adding
            if (member.trackerID != null)   memberLocation["tid"]  = member.trackerID
            if (member.battery != null)     memberLocation["batt"] = member.battery
            if (member.accuracy != null)    memberLocation["acc"]  = member.accuracy
            if (member.altitude != null)    memberLocation["alt"]  = member.altitude
            if (member.speed != null)       memberLocation["vel"]  = member.speed
            if (member.bs != null)          memberLocation["bs"]   = member.bs
        
            // populate the additional data fields if supported by the current member
            if (currentMember.wifi != null) memberLocation["wifi"] = member.wifi
            if (currentMember.hib  != null) memberLocation["hib"]  = member.hib
            if (currentMember.ps   != null) memberLocation["ps"]   = member.ps
            if (currentMember.bo   != null) memberLocation["bo"]   = member.bo
            if (currentMember.loc  != null) memberLocation["loc"]  = member.loc

            positions << memberLocation
        
            // send the image cards for the user if there is one, and we aren't sending commands, -- only send on the ping or the manual update to minimize data traffic
            if (validLocationType(data.t)) {
                card = getMemberCard(member)
                if (!sendCmdToMember(currentMember) && card) {
                    positions << card
                }
            }
        }
    }

    return (positions)
}

private def getMemberCard(member) {
    def card = []

    // send the image cards for the user if enabled
    if (imageCards) {
        try{
            // append each enabled user's card with encoded image
            card = [ "_type": "card", "name": "${member.name}", "face": "${downloadHubFile("${member.name}.jpg").encodeBase64().toString()}", "tid": "${member.name}" ]
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
        card = getMemberCard(member)
        if (card) {
            // for recorder, this debug must be captured and saved to: <STORAGEDIR>/cards/<user>/<user>.json
            // or use: https://avanc.github.io/owntracks-cards/ to create and save the JSON
            log.trace("For recorder cards, copy the bold JSON text between |  |, and save this file to 'STORAGEDIR/cards/${member.name}/${member.name}.json': |<b>${(new JsonBuilder(card)).toPrettyString()}</b>|")
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
                            ]

    def deviceLocatorList = [
                                // dynamic configurations
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
        // request a more precise GPS location by temporarily changing 'locatorPriority' to "HIGH POWER"
        // we will do this for manual and ping updates if not already in high power mode
        if (locatorPriority != "3") {
            // currently not enabled in the 2.4.x mobile app for http
//            update += sendReportLocationRequest(currentMember)
        }
        // if we enabled a high accuracy location fix, then mark the user
        if (highAccuracyOnPing) {
            currentMember.requestLocation = true
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
        // switch the phone to a high accuracy report for one location request
        if (currentMember?.requestLocation) {
            logDescriptionText("Requesting a high accuracy location update for ${currentMember.name}")            
            updateConfig = createConfiguration(currentMember, true)
        } else {
            // dynamically change the configuration as necessary
            updateConfig = checkRegionConfiguration(currentMember, data)
        }
        if (updateConfig) {
            update += updateConfig
        }
    }

    // trigger an app restart
    if (currentMember?.restartApp) {
        currentMember.restartApp = false
        // Only supported on Android.  When this is sent, the app restarts, but the ping service does not        
//        update += sendRestartRequest(currentMember)
    }

    // request the member's regions
    if (currentMember?.getRegions) {
        currentMember.getRegions = false
        update += sendReportWaypointsRequest(currentMember)
    }

    logDebug("Updating user: ${currentMember.name} with data: ${update}")
    return (new JsonBuilder(update).toPrettyString())
}

private def sendCmdToMember(currentMember) {
    // check if there are commands to send to the member
    if ((currentMember?.updateWaypoints) || (currentMember?.updateLocation) || (currentMember?.updateDisplay) || (currentMember?.restartApp) || (currentMember?.getRegions)) {
        return (true)
    } else {
        return (false)
    }
}

def validLocationType(locationType) {
    // allow update if ping or manual location
    return ((locationType == "p") || (locationType == "u"))
}

private def createChild(name) {
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
    log.warn "ADDED FOR TESTING THROUGH THE BROWSER LINK - not currently handled"
    return render(contentType: "text/html", data: result, status: 200)
}

private def getFormat(type, myText="", myError="") {
    if (type == "box") return "<div style='color:#ffffff;font-weight: normal;background-color:#3C00BC;padding:5px;padding-left:10px;border: 1px solid #000000;box-shadow: 3px 4px #575757;border-radius: 1px'>${myText}<span style='color:red;padding-left:5px;font-weight:bold'>${myError}</span></div>"
    if (type == "line") return "<hr style='background-color:#3C00BC; height: 1px; border: 0;'/>"
    if (type == "title") return "<h2 style='color:#3C00BC;font-weight: bold'>${myText}</h2>"
    if (type == "button") return "<a style='color:white;text-align:center;font-size:20px;font-weight:bold;background-color:#3C00BC;border:1px solid #000000;box-shadow:3px 4px #575757;border-radius:10px' href='${page}'>${myText}</a>"
    if (type == "redText") return "<div style='color:#ff0000'>${myText}</div>"
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

def resetLogging() {
    // clear the logging
    app.updateSetting("debugOutput", [value: false, type: "bool"])
    logWarn("Debug logging disabled.")
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
