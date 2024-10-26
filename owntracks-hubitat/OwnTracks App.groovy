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
 *  1.6.19     2024-01-18      - Ignore incoming +follow regions from users.  Changed the +follow region to match the locatorInterval setting.
 *  1.6.20     2024-01-19      - Fixed a fail to install crash from the +follow maintenance.
 *  1.6.21     2024-01-20      - Fixed issue where it was impossible to edit a different region after selecting one.  Added the ability to have private members to not receive member updates or regions.  Added note to edit regions for iOS devices. Added the ability to reset location and display to default.  Home location cleanup.
 *  1.6.22     2024-01-21      - Updated the add/edit/delete flow.  Add a banner to the member status table and delete screen for regions pending deletion.
 *  1.6.23     2024-01-22      - Add a red information banner to delete old +follow regions if the locater interval changed.  Fixed issue where a home region mismatch would be displayed when a user left home.
 *  1.6.24     2024-01-23      - Expose the member delete button to eliminate confusion.
 *  1.6.25     2024-01-24      - Removed nag warning about home region mismatch.
 *  1.6.26     2024-01-26      - Added direct links to the file manager and logs in the setup screens.  Added reverse geocode address support.
 *  1.6.27     2024-01-28      - Fixed error when configuring the geocode provider for the first time.
 *  1.6.28     2024-01-28      - Added 6-decimal place rounding to geocode lat/lon.
 *  1.6.29     2024-01-29      - Store the users past address, and re-use that instead of a geocode lookup if their current coordinates are within 10m of that location.
 *  1.6.30     2024-01-29      - Fixed typo.
 *  1.6.31     2024-01-29      - Prevent exceptions when converting units if a null was passed.
 *  1.6.32     2024-01-30      - Updated member attributes before address lookup to prevent errors.  Added a warning to Member Status if no home place is defined.
 *  1.7.0      2024-01-30      - Moved street address logic to app.
 *  1.7.1      2024-01-31      - Fixed issue where geocode location would get stuck and never request a new address.  Added enter/leave transition notification.
 *  1.7.2      2024-02-01      - Moved the notification selection box to the main screen.  Fix issue where Geoapify geocodes added leading spaces to fields.
 *  1.7.3      2024-02-02      - Pass distance from home directly to driver for better logging.
 *  1.7.4      2024-02-03      - Changed the notification message.  Moved notification control to app.
 *  1.7.5      2024-02-03      - Remove the place from the full address.
 *  1.7.6      2024-02-04      - Allow device name prefix to be changed.
 *  1.7.7      2024-02-04      - Fixed error on some hubs with the new prefix change.
 *  1.7.8      2024-02-04      - Removed dynamic prefix display in the settings.
 *  1.7.9      2024-02-04      - Updated OwnTracks Frontend instructions.
 *  1.7.10     2024-02-05      - Updated the disabled member warning instructions in the logs.  Changed the starting zoom level of the region maps to show house level.
 *  1.7.11     2024-02-07      - Recorder configuration URL no longer requires the /pub, and will automatically be corrected in the setting.  Added common member driver for friends location tile.  Added setting to select WiFi SSID keep radius.
 *  1.7.13     2024-02-08      - Fixed null exceptions on update to 1.7.11.
 *  1.7.14     2024-02-08      - Addressed migration issues.  Change the "high accuracy location message" to debug.
 *  1.7.15     2024-02-08      - Only update the device prefix if one is defined.
 *  1.7.16     2024-02-08      - Add error protection on device prefix change.
 *  1.7.17     2024-02-09      - Changed the device name creation to work on all hub versions.  Only create member devices once the user has been enabled.
 *  1.7.18     2024-02-09      - Allow changing of the arrived/left notifications.
 *  1.7.19     2024-02-10      - Updated logging.  Removed the request high accuracy location selection box due to it being redundant.
 *  1.7.20     2024-02-10      - Mobile app location settings failed to switch units to imperial if required.
 *  1.7.21     2024-02-10      - Mobile app location settings failed to switch units to imperial when reset to defaults.
 *  1.7.22     2024-02-11      - Mobile app location settings in imperial mode would pull from the wrong units.
 *  1.7.23     2024-02-19      - Increased the wifi SSID distance check selector to allow larger distances.
 *  1.7.24     2024-02-21      - Added direct device links to the member table.
 *  1.7.25     2024-02-25      - Only add geocode locations to region list if there is no current region list.
 *  1.7.26     2024-02-26      - Changed layout to collapse menu items for cleaner look.  Added Family map using Google Maps API.  Added Google Maps API to region creation to allow for radius' to be viewed.
 *  1.7.27     2024-03-03      - Minor changes to screen layout. Created html links for direct member tile access.
 *  1.7.28     2024-03-05      - Added dynamic support for cloud recorder URL.  Hide recorder cloud links when not using https.
 *  1.7.29     2024-03-07      - Added an info box when a member is selected in Google maps.  Fixed secondary hubs not receiving region updates.  Added a send regions to secondary button.
 *  1.7.30     2024-03-15      - Updated Google family map to have info windows when a user is clicked, and tracking ability.  Added a configuration map when the Google Maps API key is entered.  Fixed exception when no recorder URL is present.
 *  1.7.31     2024-03-16      - Fixed exception when configure regions was selected with no Google Maps API key.
 *  1.7.32     2024-03-18      - Moved region and address selectors directly to the config map.
 *  1.7.33     2024-03-19      - Added a service member to allow for secondary hub region transfers.
 *  1.7.34     2024-03-21      - Fixed dashboard tiles not automatically updating.
 *  1.7.35     2024-03-23      - Refactored Google Friends map to dynamically update.
 *  1.7.36     2024-03-24      - Fixed Google Friends map info box.
 *  1.7.37     2024-03-24      - Shuffled menu tabs and text to make the flow more intuitive.
 *  1.7.38     2024-03-25      - Updated Recorder instructions to include notes about using Google maps and reverse geocode keys.
 *  1.7.39     2024-03-28      - Fixed issue with secondary hub link.
 *  1.7.40     2024-03-31      - Presence and member tiles get regenerated automatically on change.
 *  1.7.41     2024-04-06      - Detect the incoming phone OS and prevent +follow regions from being sent to Android.
 *  1.7.42     2024-04-07      - Refactored layout and section labels to group recommend vs optional configurations.
 *  1.7.43     2024-04-07      - Changed cloud/local URL sourcing.  Fixed Google Family map local URL not displaying members.
 *  1.7.44     2024-04-09      - Changed collapsible sections to retain past state.
 *  1.7.45     2024-04-15      - Added per region notification granularity.  Fixed issue where notifications were only sent if they were set for leave.
 *  1.7.46     2024-04-17      - Fixed notifications not working if the device prefix was not blank.
 *  1.7.47     2024-04-20      - Changed name displayed in notifications.
 *  1.7.48     2024-04-22      - Fixed lat/lon rounding when adding a place.  Added support for Android 2.5.x.  Fixed issues with region types that was preventing iOS from updating regions.
 *  1.7.49     2024-04-24      - Rolled back region migration.
 *  1.7.50     2024-04-24      - Fixed region migration.
 *  1.7.51     2024-04-27      - Added API key validation to the setup screen.  Added Member command API links.
 *  1.7.52     2024-04-28      - Regions are now deleted from mobile before sending new ones to eliminate duplicate region names.
 *  1.7.53     2024-05-02      - Fixed issue where transition messages was assigning null to speed.
 *  1.7.54     2024-05-03      - Prevent the clear waypoints command on 2.4.x.
 *  1.7.55     2024-05-04      - Removed support for 2.4.17 forked version.
 *  1.7.56     2024-05-04      - Cloud links for Recorder were being displayed when they should not have.
 *  1.7.57     2024-05-11      - Fixed higher accuracy reporting wasn't happening after the 2.5.x migration changes.  Fixed an error if a user notification was saved, with no selected regions.
 *  1.7.58     2024-05-20      - When using the dynamic region config map, creating more than one region at a time would result in duplicates.  Testing the map API key with no members would result in an exception and not display the map.
 *  1.7.59     2024-07-02      - Support locatorPriority in 2.5.x.
 *  1.7.60     2024-07-03      - When trackerID was changed to two characters, the thumbnail image was not displayed.  Fixed markers on Google Family Map.
 *  1.7.61     2024-07-03      - If no thumbnails are configured, Google Family Map displays a random color on each members marker.
 *  1.7.62     2024-07-06      - Added ability to change the member and region pin colors on the maps.
 *  1.7.62     2024-07-06      - Added ability to change the member and region pin colors on the maps.
 *  1.7.63     2024-07-11      - Google Family Map accuracy and speed was not being converted to imperial.
 *  1.7.64     2024-07-13      - Configuration map was not converting displayed radius back to feet on a save.
 *  1.7.65     2024-07-19      - Removed specialized support for Android 2.4.x.
 *  1.7.66     2024-07-30      - Added selectable member glyph colors.  Added member history to the Google Family Map.
 *  1.7.67     2024-07-31      - Fixed exception when exiting the app before history was created.
 *  1.7.68     2024-07-31      - Added history radius size adjustment.
 *  1.7.69     2024-08-04      - Split the thumbnail and history sync to fix cloud data limitation.  Changed history dot fading scheme.  Prevent map from panning to selected member when history is open.
 *  1.7.70     2024-08-06      - Added connecting lines to history with directional arrows.  Fixed history point zoom.
 *  1.7.71     2024-08-07      - Added scaling to history lines and directional arrows.
 *  1.7.72     2024-08-08      - Added increased past history stored at a slower recording interval.  Added slider to disable cloud web links.
 *  1.7.73     2024-08-10      - Fixed exception with long history if the app was not opened after the updated.
 *  1.7.74     2024-08-10      - Fixed course over ground.  Fixed exception on new install without previous history.
 *  1.7.75     2024-08-10      - Fixed exception on new install without previous history.
 *  1.7.76     2024-08-10      - Fixed exception on new install without previous history.  Calculates speed if returned speed was 0.  Added directional bearing to Google Map.
 *  1.7.77     2024-08-11      - Calculates bearing if returned bearing was 0.  Dynamically change the speed icon on Google Map based on speed.
 *  1.7.78     2024-08-11      - Bearing calculation was inverted.
 *  1.7.79     2024-08-18      - Reduced saved address to street address only.  Added trip markers to history.  Don't save locations with repeated 0 speed or similar bearing.  Fixed speed calculations if phone returned 0 speed.
 *  1.7.80     2024-08-18      - Added trip odometer.
 *  1.7.81     2024-08-18      - Added trip stats to history points.
 *  1.7.82     2024-08-20      - Improved trip stats display.  Give full trip stats when a trip line is selected.  Disable auto zoom/centering when the map is panned or a history point is opened.
 *  1.7.83     2024-08-21      - Disable auto zoom/centering when the map is panned or a history point is opened and member is being tracked.
 *  1.7.84     2024-08-24      - Re-worked the zoom/auto zoom controls.  Set the minimum history speed limit to <2KPH to reduce noisy location points.  Prevent calculating speed on rapidly arriving locations.
 *  1.7.85     2024-08-25      - Selecting a trip will bring it into focus.
 *  1.7.86     2024-08-25      - Selecting trips when all member trips are visible will bring it into focus.
 *  1.7.87     2024-08-25      - Fixed exception in trip numbering when member has no history.
 *  1.7.88     2024-08-26      - Fixed exception if a member was deleted and past settings were not cleared.
 *  1.7.89     2024-08-31      - Refactored zoom and history selection to Google maps.  Added a user configurable distance for the auto-zoom in Google maps.  Added stale member notifications.
 *  1.7.90     2024-09-02      - Added member deactivation to clear the mobile URL and waypoints.  Prevent location updates over 5-minutes old from triggering member presence.
 *  1.7.91     2024-09-08      - Added member friend groups.
 *  1.7.92     2024-09-14      - When a member info box was open on Google maps, it wouldn't automatically refresh.  Add more descriptive app permission warnings to the info box.
 *  1.7.93     2024-09-18      - Members were not getting sorted based on last location time.  Fixed Google maps member order to display the last reported member and member in focus on top.
 *  1.7.94     2024-09-19      - Recreates missing member devices should they be deleted from the Hubitat device menu and not the app.
 *  1.8.0      2024-09-23      - Member status now indicates configurations that will impact location performance.  Fix issue where history compression was not properly removing markers at direction transitions.  Google Friends map will auto-update when the main app updates.
 *  1.8.1      2024-09-24      - Member status would inaccurately indicate a permission error for iOS phones.
 *  1.8.2      2024-10-12      - Return last member locations in a JSON message when the mobile app setup URL is requested.  Allow high power mode to be disabled when in a region.
 *  1.8.4      2024-10-13      - Added missing "members" in JSON.
 *  1.8.5      2024-10-26      - Cleanup migration.  Fixed issue if thumbnails were enabled, but no image files were loaded in the hub.
*/

import groovy.transform.Field
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonBuilder
import java.text.SimpleDateFormat

def appVersion() { return "1.8.5" }

@Field static final Map BATTERY_STATUS = [ "0": "Unknown", "1": "Unplugged", "2": "Charging", "3": "Full" ]
@Field static final Map DATA_CONNECTION = [ "w": "WiFi", "m": "Mobile", "o": "Offline"  ]
@Field static final Map TRIGGER_TYPE = [ "p": "Ping", "c": "Region", "r": "Report Location", "u": "Manual", "b": "Beacon", "t": "Timer", "v": "Monitoring", "l": "Region" ]
@Field static final Map TOPIC_FORMAT = [ 0: "topicSource", 1: "userName", 2: "deviceID", 3: "eventType" ]
@Field static final Map LOCATOR_PRIORITY = [ "NoPower": "NO_POWER (best accuracy with zero power consumption)", "LowPower": "LOW_POWER (city level accuracy)", "BalancedPowerAccuracy": "BALANCED_POWER (block level accuracy based on Wifi/Cell)", "HighAccuracy": "HIGH_POWER (most accurate accuracy based on GPS)" ]
@Field static final Map DYNAMIC_INTERVALS = [ "pegLocatorFastestIntervalToInterval": false, "locatorPriority": "HighAccuracy" ]
//@Field static final Map MONITORING_MODES = [ 0: "Manual (user triggered events)", 1: "Significant (standard tracking using Wifi/Cell)", 2: "Move (permanent tracking using GPS)" ]
@Field static final Map MONITORING_MODES = [ 1: "Significant (standard tracking using Wifi/Cell)", 2: "Move (permanent tracking using GPS)" ]
@Field static final Map IOS_PLUS_FOLLOW = [ "rad":50, "tst":1700000000, "_type":"waypoint", "lon":0.0, "lat":0.0, "desc":"+follow" ]
@Field static final Map GEOCODE_PROVIDERS = [ 0: "Disabled", 1: "Google", 2: "Geoapify", 3: "Opencage" ]
@Field static final Map GEOCODE_ADDRESS = [ 1: "https://maps.googleapis.com/maps/api/geocode/json", 2: "https://api.geoapify.com/v1/geocode/", 3: "https://api.opencagedata.com/geocode/v1/json" ]
@Field static final Map GEOCODE_REQUEST = [ 1: "?address=", 2: "search?text=", 3: "?q=" ]
@Field static final Map REVERSE_GEOCODE_REQUEST_LAT = [ 1: "?latlng=", 2: "reverse?lat=", 3: "?q=" ]
@Field static final Map REVERSE_GEOCODE_REQUEST_LON = [ 1: ",", 2: "&lon=", 3: "," ]
@Field static final Map GEOCODE_KEY = [ 1: "&key=", 2: "&format=json&apiKey=", 3: "&key=" ]
@Field static final Map ADDRESS_JSON = [ 1: "formatted_address", 2: "formatted", 3: "formatted" ]
@Field static final Map GEOCODE_USAGE_COUNTER = [ 1: "googleUsage", 2: "geoapifyUsage", 3: "opencageUsage" ]
@Field static final Map GEOCODE_QUOTA = [ 1: 40000, 2: 3000, 3: 2500 ]
@Field static final Map GEOCODE_QUOTA_INTERVAL_DAILY = [ 1: false, 2: true, 3: true ]
@Field static final Map GEOCODE_API_KEY_LINK = [ 1: "<a href='https://developers.google.com/maps/documentation/directions/get-api-key/' target='_blank'>Sign up for a Google API Key</a>", 2: "<a href='https://apidocs.geoapify.com/docs/geocoding/reverse-geocoding/#about' target='_blank'>Sign up for a Geoapify API Key</a>", 3: "<a href='https://opencagedata.com/api#quickstart' target='_blank'>Sign up for a Opencage API Key</a>" ]
@Field static final List URL_SOURCE = [ "[cloud.hubitat.com]", "[local.com]" ]
@Field static final Map COLLECT_PLACES = [ "desc": 0, "desc_tst": 1, "map" : 2 ]

// Main defaults
@Field String  HUBITAT_CLOUD_URL = "cloud.hubitat.com"
@Field String  DEFAULT_APP_THEME_COLOR = "#191970"
@Field String  DEFAULT_MEMBER_PIN_COLOR = "MidnightBlue"         // "#191970" - "MidnightBlue"
@Field String  DEFAULT_MEMBER_GLYPH_COLOR = "Purple"             // "#800080" - "Brown"
@Field String  DEFAULT_REGION_PIN_COLOR = "FireBrick"            // "#b22222" - "FireBrick"
@Field String  DEFAULT_REGION_NEW_PIN_COLOR = "red"              // "#ff0000" - "Red"
@Field String  DEFAULT_REGION_NEW_GLYPH_COLOR = "black"          // "#000000" - "Black"
@Field String  DEFAULT_REGION_HOME_GLYPH_COLOR = "DarkSlateGrey" // "#2f4f4f" - "DarkSlateGrey"
@Field String  DEFAULT_REGION_GLYPH_COLOR = "Maroon"             // "#800000" - "Maroon"
@Field Number  DEFAULT_memberHistoryLength = 60
@Field Number  DEFAULT_maxMemberHistoryLength = 60
@Field Number  DEFAULT_memberHistoryScale = 1.0
@Field Number  DEFAULT_memberHistoryStroke = 1.0
@Field Number  DEFAULT_memberHistoryRepeat = 300
@Field Boolean DEFAULT_displayAllMembersHistory = false
@Field Boolean DEFAULT_removeMemberMarkersWithSameBearing = true
@Field Number  DEFAULT_memberMarkerBearingDifferenceDegrees = 10
@Field Number  DEFAULT_memberTripIdleMarkerTime = 15
@Field Number  DEFAULT_memberBoundsRadius = 100
@Field Number  memberMaximumLocationAgeMinutes = 5
@Field Number  memberHistoryMinimumSpeed = 3
@Field Number  memberVelocityMinimumTimeDifference = 5
@Field String  memberBeginMarker = "b"
@Field String  memberMiddleMarker = "m"
@Field String  memberEndMarker = "e"
@Field Number  GOOGLE_MAP_API_QUOTA = 28500
@Field String  GOOGLE_MAP_API_KEY_LINK = "<a href='https://developers.google.com/maps/documentation/directions/get-api-key/' target='_blank'>Sign up for a Google API Key</a>"
@Field String  RECORDER_PUBLISH_FOLDER = "/pub"
@Field String  MQTT_TOPIC_PREFIX = "owntracks"
@Field Number  INVALID_COORDINATE = 999
@Field String  COMMON_CHILDNAME = "OwnTracks"
@Field String  ANDROID_USER_AGENT = "Owntracks-Android"
@Field String  DEFAULT_CHILDPREFIX = "OwnTracks - "
@Field Number  DEFAULT_RADIUS = 75
@Field Number  DEFAULT_regionHighAccuracyRadius = 750
@Field Number  DEFAULT_wifiPresenceKeepRadius = 750
@Field Boolean DEFAULT_imperialUnits = false
@Field Boolean DEFAULT_disableCloudLinks = false
@Field Boolean DEFAULT_regionHighAccuracyRadiusHomeOnly = true
@Field Boolean DEFAULT_warnOnDisabledMember = true
@Field Boolean DEFAULT_warnOnMemberSettings = false
@Field Number  DEFAULT_warnOnNoUpdateHours = 12
@Field Number  DEFAULT_staleLocationWatchdogInterval = 900
@Field Boolean DEFAULT_highAccuracyOnPing = true
@Field Boolean DEFAULT_highPowerMode = true
@Field Boolean DEFAULT_lowPowerModeInRegion = false
@Field Number  DEFAULT_googleMapsZoom = 0
@Field String  DEFAULT_googleMapsMember = "null"
@Field Boolean DEFAULT_descriptionTextOutput = true
@Field Boolean DEFAULT_debugOutput = false
@Field Number  DEFAULT_debugResetHours = 1
@Field Number  DEFAULT_geocodeProvider = 0
@Field Boolean DEFAULT_geocodeFreeOnly = true
@Field Number  DEFAULT_geocodeLookupHysteresis = 0.010
@Field Boolean DEFAULT_mapFreeOnly = true
@Field Boolean DEFAULT_useCustomNotificationMessage = false
@Field String  DEFAULT_notificationMessage = "NAME EVENT REGION at TIME"
@Field Boolean DEFAULT_manualDeleteBehavior = false
@Field Number  DEFAULT_globalGroupNumber = 0
@Field String  DEFAULT_globalGroupName = "Default"
@Field String  DEFAULT_groupNames = "Group "
@Field Number  DEFAULT_maxGroups = 5

// Mobile app location defaults
@Field Number  DEFAULT_monitoring = 1
@Field String  DEFAULT_locatorPriority = "BalancedPowerAccuracy"
@Field Number  DEFAULT_moveModeLocatorInterval = 30
@Field Number  DEFAULT_locatorDisplacement = 50
@Field Number  DEFAULT_locatorInterval = 60
@Field Number  DEFAULT_ignoreInaccurateLocations = 150
@Field Number  DEFAULT_ignoreStaleLocations = 7
@Field Number  DEFAULT_ping = 30
@Field Boolean DEFAULT_pegLocatorFastestIntervalToInterval = true
// Mobile app display defaults
@Field Boolean DEFAULT_imageCards = false
@Field Boolean DEFAULT_replaceTIDwithUsername = true
@Field Boolean DEFAULT_notificationEvents = true
@Field Boolean DEFAULT_extendedData = true
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
    page(name: "configureNotifications")
    page(name: "configureRecorder")
    page(name: "configureSecondaryHub")
    page(name: "recorderInstallationInstructions")
    page(name: "advancedHub")
    page(name: "advancedLocation")
    page(name: "advancedDisplay")
    page(name: "configureRegions")
    page(name: "configureGroups")
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
    // if we selected a user to retrieve their regions, set the flag so the table updates
    updateGetRegion()

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
                displayMissingHomePlace()
                displayRegionsPendingDelete()
            }
            section() {
                input name: "sectionInstall", type: "button", title: getSectionTitle(state.show.install, "Installation and Configuration"), submitOnChange: true, style: getSectionStyle()
                if (state.show.install) {
                    href(title: "Mobile App Installation Instructions", description: "", style: "page", page: "installationInstructions")
                    href(title: "Configure Hubitat App - WiFi Settings, Units, Location Performance, Device Prefix, Geocode and Google Map API keys", description: "", style: "page", page: "configureHubApp")
                    href(title: "Configure Regions - Add, Edit, Delete, Assign 'Home'", description: "", style: "page", page: "configureRegions")
                    href(title: "Configure Groups - Assign Friend Groups", description: "", style: "page", page: "configureGroups")
                    input "enabledMembers", "enum", multiple: true, title:(enabledMembers ? '<div>' : '<div style="color:#ff0000">') + "Select family member(s) to monitor.  Member device will be created and configured once 'Done' is pressed, below.</div>", options: (state.members ? state.members.name.sort() : []), submitOnChange: true
                    input "privateMembers", "enum", multiple: true, title:(privateMembers ? '<div style="color:#ff0000">' : '<div>') + 'Select family member(s) to remain private.  Locations and regions will <B>NOT</b> be shared with other members or the Recorder.  Their Hubitat device will only display presence information.</div>', options: (state.members ? state.members.name.sort() : []), submitOnChange: true
                    href(title: "Configure Stale Member and Region Arrived/Departed Notifications", description: "", style: "page", page: "configureNotifications")
                    if (enabledMembers) {
                        enabledMembers.each { name ->
                            member = state.members.find {it.name==name}
                            // cancel any deactivation
                            member?.remove("deactivate")
                        }
                    }
                }
                input name: "sectionLinks", type: "button", title: getSectionTitle(state.show.links, "Dashboard Web Links"), submitOnChange: true, style: getSectionStyle()
                if (state.show.links) {
                    paragraph ("<b>Direct dashboard links for use in a web browser.</b>")
                    input name: "disableCloudLinks", type: "bool", title: "Disable cloud links", defaultValue: DEFAULT_disableCloudLinks, submitOnChange: true
                    URL_SOURCE.each{ source->
                        if ((source != URL_SOURCE[0]) || (disableCloudLinks != true)) {
                            paragraph ((source == URL_SOURCE[0] ? "<h2>Cloud Links</h2>" : "<h2>Local Links</h2>"))
                            if (googleMapsAPIKey) {
                                paragraph ("<b>Google family map:</b></br>&emsp;<a href='${getAttributeURL(source, "googlemap")}" + "&member='" + ">${getAttributeURL(source, "googlemap")}" + "&member=" + "</a></br>")
                                paragraph ("<b>Region configuration map:</b></br>&emsp;<a href='${getAttributeURL(source, "configmap")}'>${getAttributeURL(source, "configmap")}</a></br>")
                            }
                            if (state.members) {
                                urlList = ""
                                state.members.each { member->
                                    urlList += "${member.name}:</br>&emsp;<a href='${getAttributeURL(source, "membermap/${member.name.toLowerCase()}")}'>${getAttributeURL(source, "membermap/${member.name.toLowerCase()}")}</a></br>"
                                }
                                paragraph ("<b>Member location map:</b></br>${urlList}")
                            }
                            if (state.members) {
                                urlList = ""
                                state.members.each { member->
                                    urlList += "${member.name}:</br>&emsp;<a href='${getAttributeURL(source, "memberpresence/${member.name.toLowerCase()}")}'>${getAttributeURL(source, "memberpresence/${member.name.toLowerCase()}")}</a></br>"
                                }
                                paragraph ("<b>Member Presence:</b></br>${urlList}")
                            }
                            if (recorderURL) {
                                // only display the recorder links if it's a local URL or if it's https (required for the cloud link)
                                if ((source != URL_SOURCE[0]) || isHTTPsURL(getRecorderURL())) {
                                    paragraph ("<b>OwnTracks Recorder family map:</b></br>&emsp;<a href='${getAttributeURL(source, "recordermap")}'>${getAttributeURL(source, "recordermap")}</a>")

                                    if (state.members) {
                                        urlList = ""
                                        state.members.each { member->
                                            urlList += "${member.name}:</br>&emsp;<a href='${getAttributeURL(source, "memberpastlocations/${member.name.toLowerCase()}")}'>${getAttributeURL(source, "memberpastlocations/${member.name.toLowerCase()}")}</a></br>"
                                        }
                                        paragraph ("<b>OwnTracks Recorder member past locations:</b></br>${urlList}")
                                    }
                                }
                            }
                        }
                    }
                }
                input name: "sectionCommands", type: "button", title: getSectionTitle(state.show.commands, "Member Command API Links"), submitOnChange: true, style: getSectionStyle()
                if (state.show.commands) {
                    paragraph ("<b>Member API command links for advanced integrations and virtual switch controls.</b>")
                    URL_SOURCE.each{ source->
                        paragraph ((source == URL_SOURCE[0] ? "<h2>Cloud Links</h2>" : "<h2>Local Links</h2>"))
                        if (state.members) {
                            urlList = ""
                            state.members.each { member->
                                urlList += "${member.name}</br>"
                                urlList += "&emsp;'On':&emsp;<a href='${getAttributeURL(source, "membercmd/${member.name.toLowerCase()}/on")}'>${getAttributeURL(source, "membercmd/${member.name.toLowerCase()}/on")}</a></br>"
                                urlList += "&emsp;'Off':&emsp;<a href='${getAttributeURL(source, "membercmd/${member.name.toLowerCase()}/off")}'>${getAttributeURL(source, "membercmd/${member.name.toLowerCase()}/off")}</a></br>"
                                urlList += "&emsp;'Arrived':&emsp;<a href='${getAttributeURL(source, "membercmd/${member.name.toLowerCase()}/arrived")}'>${getAttributeURL(source, "membercmd/${member.name.toLowerCase()}/arrived")}</a></br>"
                                urlList += "&emsp;'Departed':&emsp;<a href='${getAttributeURL(source, "membercmd/${member.name.toLowerCase()}/departed")}'>${getAttributeURL(source, "membercmd/${member.name.toLowerCase()}/departed")}</a></br>"
                            }
                            paragraph ("<b>Member On/Off/Arrived/Departed:</b></br>${urlList}")
                        }
                    }
                }
                input name: "sectionOptional", type: "button", title: getSectionTitle(state.show.optional, "Optional Features - Thumbnails, Recorder, Secondary Hub"), submitOnChange: true, style: getSectionStyle()
                if (state.show.optional) {
                    href(title: "Enabling User Thumbnails", description: "", style: "page", page: "thumbnailCreation")
                    href(title: "Enable OwnTracks Recorder", description: "", style: "page", page: "configureRecorder")
                    href(title: "Link Secondary Hub", description: "", style: "page", page: "configureSecondaryHub")
                }
                input name: "sectionAdvanced", type: "button", title: getSectionTitle(state.show.advanced, "Advanced Settings - Hub and Mobile"), submitOnChange: true, style: getSectionStyle()
                if (state.show.advanced) {
                    paragraph("The default settings provide the best balance of accuracy/power.  To view or modify advanced settings, select the items below.")
                    href(title: "Hub App Settings", description: "", style: "page", page: "advancedHub")
                    href(title: "Mobile App Location Settings", description: "", style: "page", page: "advancedLocation")
                    href(title: "Mobile App Display Settings", description: "", style: "page", page: "advancedDisplay")
                }
                input name: "sectionMaintenance", type: "button", title: getSectionTitle(state.show.maintenance, "Maintenance - Sync Member Settings, Reset to Defaults, Deactivate and Delete Members"), submitOnChange: true, style: getSectionStyle()
                if (state.show.maintenance) {
                    input "syncMobileSettings", "enum", multiple: true, title:"Select family member(s) to update location, display and region settings on the next location update. The user will be registered to receive this update once 'Done' is pressed, below, and this list will be automatically cleared.", options: (enabledMembers ? enabledMembers.sort() : enabledMembers)
                    href(title: "Recommended Default Settings", description: "", style: "page", page: "resetDefaults")
                    href(title: "Delete or Deactivate Family Members", description: "", style: "page", page: "deleteMembers")
                }
                input name: "sectionLogging", type: "button", title: getSectionTitle(state.show.logging, "Logging"), submitOnChange: true, style: getSectionStyle()
                if (state.show.logging) {
                    input name: "descriptionTextOutput", type: "bool", title: "Enable Description Text logging", defaultValue: DEFAULT_descriptionTextOutput
                    input name: "debugOutput", type: "bool", title: "Enable Debug Logging", defaultValue: DEFAULT_debugOutput
                    input name: "debugResetHours", type: "number", title: "Turn off debug logging after this many hours (1..24)", range: "1..24", defaultValue: DEFAULT_debugResetHours
                }
            }
        }
    }
}

def getLocatorPriority(inRegion) {
    if (highPowerMode && !inRegion) {
        return(DYNAMIC_INTERVALS.locatorPriority)
    } else {
        return(DEFAULT_locatorPriority)
    }
}

def configureHubApp() {
    return dynamicPage(name: "configureHubApp", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Configure Hubitat App")) {
            // deal with changes if the imperial/metric slider was changed
            initializeHub(false)
        }
        section() {
            input name: "sectionHubsettings", type: "button", title: getSectionTitle(state.show.hubsettings, "Hubitat Settings - WiFi Settings, Units, Device Prefix"), submitOnChange: true, style: getSectionStyle()
            if (state.show.hubsettings) {
                input "homeSSID", "string", title:"Enter your 'Home' WiFi SSID(s), separated by commas.  Used to prevent devices from being 'non-present' if currently connected to these WiFi access point(s).", defaultValue: ""
                input name: "wifiPresenceKeepRadius", type: "enum", title: "SSID will only be used for presence detection when a member is within this radius from home, Recommended=${displayMFtVal(DEFAULT_wifiPresenceKeepRadius)}", defaultValue: "${DEFAULT_wifiPresenceKeepRadius}", options: (imperialUnits ? [0:'disabled',250:'820 ft',500:'1640 ft',750:'2461 ft',2000:'1.2 mi',5000:'3.1 mi',10000:'6.2 mi'] : [0:'disabled',250:'250 m',500:'500 m',750:'750 m',2000:'2 km',5000:'5 km',10000:'10 km'])
                input name: "imperialUnits", type: "bool", title: "Display imperial units instead of metric units", defaultValue: DEFAULT_imperialUnits, submitOnChange: true
                input name: "deviceNamePrefix", type: "string", title: "Prefix to be added to each member's device name.  For example, member '<b>Bob</b>' with a prefix of '<b>${DEFAULT_CHILDPREFIX}</b>' will have a device name of '<b>${DEFAULT_CHILDPREFIX}Bob</b>'. Member device name will be updated once the Hubibit app is exited. Enter a space to have no prefix in front of the member name.", defaultValue: DEFAULT_CHILDPREFIX, submitOnChange: true, required: true
            }
            input name: "sectionGeocode", type: "button", title: getSectionTitle(state.show.geocode, "Geocode API Settings - Converts latitude/longitude to address"), submitOnChange: true, style: getSectionStyle()
            if (state.show.geocode) {
                input name: "geocodeProvider", type: "enum", title: "Select the optional geocode provider for address lookups.  Allows location latitude/longitude to be displayed as physical address.", description: "Enter", defaultValue: DEFAULT_geocodeProvider, options: GEOCODE_PROVIDERS, submitOnChange: true
                if (geocodeProvider != "0") {
                    paragraph ("<b><i>Google provides the best accuracy, but offers the least amount of free locations - Google usage quota is reset MONTHLY vs DAILY for the other providers.</i></b>")
                    String provider = GEOCODE_USAGE_COUNTER[geocodeProvider?.toInteger()]
                    usageCounter = state."$provider"
                    input name: "geocodeFreeOnly", type: "bool", title: "Prevent geocode lookups once free quota has been exhausted.  Current usage: <b>${usageCounter}/${GEOCODE_QUOTA[geocodeProvider?.toInteger()]} per ${(GEOCODE_QUOTA_INTERVAL_DAILY[geocodeProvider?.toInteger()] ? "day" : "month")}</b>.", defaultValue: DEFAULT_geocodeFreeOnly
                    paragraph (GEOCODE_API_KEY_LINK[geocodeProvider?.toInteger()] + (geocodeProvider?.toInteger() == 1 ? " -- <i><b>'Geocoding API'</b> must be enabled under <b><a href='https://console.cloud.google.com/apis/dashboard' target='_blank'>API's & Services</a></b>.  Use <b>API restrictions</b> and select <b>Geocoding API</b>.</i>" : ""))
                    input name: "geocodeAPIKey_$geocodeProvider", type: "string", title: "Geocode API key for address lookups:", submitOnChange: true
                    reverseGeocodeTest = reverseGeocode(37.422331,-122.0843455)
                    paragraph ("Geocode API key check: ${(reverseGeocodeTest ? "<div><b>PASSED</b> - $reverseGeocodeTest</div>" : "<div style='color:#ff0000'>FAILED</div>")}")
                }
            }
            input name: "sectionMap", type: "button", title: getSectionTitle(state.show.map, "Google Map API Settings - Creates a combined family map and adds radius bubbles on the 'Region' 'Add/Edit/Delete' page maps"), submitOnChange: true, style: getSectionStyle()
            if (state.show.map) {
                paragraph ("<h3><b>The Google Family Map dashboard URL provides detailed location and trip history for all members.  It requires signing up for a free Google Maps Javascript API key (see below).</b></h3>" +
                       "<h2>Interacting with the map</h2>" +
                       "<h4><b>     Selecting a member marker</b></h4>" +
                       "          1. The map will automatically zoom and center on all family members when opened.\r" +
                       "          2. Clicking a member marker will change tracking to that member.  The map will auto-center based on their incoming locations to ensure they are always in frame.\r" +
                       "          3. The info box will display the last location information.  If the Android app is configured with non-optimal settings for operation, the following warnings will be listed depending on the issue:\r" +
                       "             a. 'App battery usage: Optimized/Restricted.'  Change to 'Unrestricted' for optimal operation.\r" +
                       "             b. 'Permissions: App can pause.'  Disable the slider to prevent Android from pausing the app if it has not be used in a while.\r" +
                       "             c. 'Location permission: Not allowed all the time.'  Change to 'Allow all the time' and 'Use precise location'.\r" +
                       "          3. Clicking on the map will release the member and auto-zoom to fit all members.\r" +
                       "<h4><b>     Selecting member history</b></h4>" +
                       "          1. When a member is selected, their past trip history is displayed fading from dark (newest) to light (oldest).\r" +
                       "          2. Selecting a history point will display information about that trip at that point in time, hide other trips and pause auto-centering.\r" +
                       "          3. Selecting a history line will display information for the entire trip, hide other trips and pause auto-centering.\r" +
                       "          4. While the info window is open, clicking anywhere on the map will display all trips for that member.  Closing the info window will resume auto-centering.\r" +
                       "          5. Trips use numbered based on how new they are in the history.  Trip #1 is the latest trip.\r" +
                       "          6. Trips use three different markers:\r" +
                       "             a. Hollow circle with thick border: Start location for the trip.\r" +
                       "             b. Solid circle: Intermediate location in the trip.\r" +
                       "             a. Hollow circle with thin border: End location for the trip.\r" +
                       "<h4><b>     Bottom banner</b></h4>" +
                       "          1. The bottom banner displays the last date/time that the map received an updated member location.\r" +
                       "          2. If a member was selected, 'Following: <member>' will be displayed.\r" +
                       "          3. If the map was dragged or history point or line was selected, the automatic pan and zoom is paused and 'Auto-Centering Paused' will be displayed.\r" +
                       "          4. Clicking anywhere on the map will resume auto-centering when the next incoming location is received.\r\r"
                )
                paragraph ("<h2>Configuring the Map</h2>")
                paragraph ("If user thumbnails have not been added to Hubitat, follow the instructions for 'Enabling User Thumbnail Instructions' to allow images to be displayed on map pins:")
                href(title: "Enabling User Thumbnails", description: "", style: "page", page: "thumbnailCreation")
                input name: "mapFreeOnly", type: "bool", title: "Prevent generating maps once free quota has been exhausted.  Current usage: <b>${state.mapApiUsage}/${GOOGLE_MAP_API_QUOTA} per month</b>.", defaultValue: DEFAULT_mapFreeOnly
                paragraph (GOOGLE_MAP_API_KEY_LINK + " -- <i><b>'Maps JavaScript API'</b> must be enabled under <b><a href='https://console.cloud.google.com/apis/dashboard' target='_blank'>API's & Services</a></b>.  Use <b>API restrictions</b> and select <b>Maps JavaScript API</b>.</i>")
                input name: "googleMapsAPIKey", type: "string", title: "Google Maps API key for combined family location map and region add/edit/delete pages to display with region radius bubbles:", submitOnChange: true
                paragraph ("<a href='${getAttributeURL("[cloud.hubitat.com]", "googlemap")}' target='_blank'>Test map API key</a>")
                input name: "memberBoundsRadius", type: "number", title: "Map will only auto-zoom to fit members within this distance from home (${getLargeUnits()}) (0..${displayKmMiVal(6400).toInteger()}) Recommended=${displayKmMiVal(DEFAULT_memberBoundsRadius).toInteger()}, Show all members=0", range: "0..${displayKmMiVal(6400).toInteger()}", defaultValue: displayKmMiVal(DEFAULT_memberBoundsRadius).toInteger(), submitOnChange: true
                paragraph ("<h2>Member History and Pin Colors</h2>")
                input name: "memberHistoryLength", type: "number", title: "Number of total past member locations to save (0..${DEFAULT_maxMemberHistoryLength}):", range: "0..${DEFAULT_maxMemberHistoryLength}", defaultValue: DEFAULT_memberHistoryLength
                input name: "memberTripIdleMarkerTime", type: "number", title: "Time in minutes between adjacent history locations to denote an end of trip (5..60):", range: "5..60", defaultValue: DEFAULT_memberTripIdleMarkerTime
                input name: "removeMemberMarkersWithSameBearing", type: "bool", title: "Remove previous history location if member is moving in the same direction.", defaultValue: DEFAULT_removeMemberMarkersWithSameBearing, submitOnChange: true
                if (removeMemberMarkersWithSameBearing) {
                    input name: "memberMarkerBearingDifferenceDegrees", type: "number", title: "Locations with bearings within this number of degrees are removed to reduce history size (0..45):", range: "0..45", defaultValue: DEFAULT_memberMarkerBearingDifferenceDegrees
                }
                input name: "memberHistoryScale", type: "decimal", title: "Scale value for the past member locations dots (1.0..3.0):", range: "1.0..3.0", defaultValue: DEFAULT_memberHistoryScale
                input name: "memberHistoryStroke", type: "decimal", title: "Scale value for the past member locations lines (1.0..3.0):", range: "1.0..3.0", defaultValue: DEFAULT_memberHistoryStroke
                input name: "memberHistoryRepeat", type: "number", title: "Distance between repeat arrows on the history lines. '0' will place a single arrow in the middle of the line (0..1000):", range: "0..1000", defaultValue: DEFAULT_memberHistoryRepeat
                input name: "displayAllMembersHistory", type: "bool", title: "Enable to display all member(s) history on map.  Disable to only display history of selected member on map.", defaultValue: DEFAULT_displayAllMembersHistory
                input name: "memberPinColor", type: "string", title: "<b>Member pin color</b>:  Enter a <a href='https://www.w3schools.com/tags/ref_colornames.asp' target='_blank'>HTML color name</a> (MidnightBlue) or a 6-digit <a href='https://www.w3schools.com/colors/colors_picker.asp' target='_blank'>HTML color code</a> (#191970):", defaultValue: DEFAULT_MEMBER_PIN_COLOR
                input "selectMemberGlyph", "enum", multiple: false, title:"Select family member to change glyph and history color.", options: state.members.name.sort(), submitOnChange: true
                // only clear on a change of selected member
                if (state.selectMemberGlyph != selectMemberGlyph) {
                    app.removeSetting("memberGlyphColor")
                }
                if (selectMemberGlyph) {
                    state.selectMemberGlyph = selectMemberGlyph
                    selectedMember = state.members.find {it.name==selectMemberGlyph}
                    // if we have a defined color, then assign it to the member
                    if (memberGlyphColor) {
                        selectedMember.color = memberGlyphColor
                    }
                    input name: "memberGlyphColor", type: "string", title: "<b>${selectMemberGlyph} glyph and history color</b>:  Enter a <a href='https://www.w3schools.com/tags/ref_colornames.asp' target='_blank'>HTML color name</a> (Purple) or a 6-digit <a href='https://www.w3schools.com/colors/colors_picker.asp' target='_blank'>HTML color code</a> (#800080):", defaultValue: (selectedMember?.color ? selectedMember.color : DEFAULT_MEMBER_GLYPH_COLOR), submitOnChange: true
                }
                paragraph ("<h2>Region Pin Colors</h2>")
                input name: "regionPinColor", type: "string", title: "<b>Region pin color</b>:  Enter a <a href='https://www.w3schools.com/tags/ref_colornames.asp' target='_blank'>HTML color name</a> (DarkRed) or a 6-digit <a href='https://www.w3schools.com/colors/colors_picker.asp' target='_blank'>HTML color code</a> (#b22222):", defaultValue: DEFAULT_REGION_PIN_COLOR
                input name: "regionGlyphColor", type: "string", title: "<b>Region glyph color</b>:  Enter a <a href='https://www.w3schools.com/tags/ref_colornames.asp' target='_blank'>HTML color name</a> (Maroon) or a 6-digit <a href='https://www.w3schools.com/colors/colors_picker.asp' target='_blank'>HTML color code</a> (#800000):", defaultValue: DEFAULT_REGION_GLYPH_COLOR
                input name: "regionHomeGlyphColor", type: "string", title: "<b>Region home glyph color</b>:  Enter a <a href='https://www.w3schools.com/tags/ref_colornames.asp' target='_blank'>HTML color name</a> (WhiteSmoke) or a 6-digit <a href='https://www.w3schools.com/colors/colors_picker.asp' target='_blank'>HTML color code</a> (#2f4f4f):", defaultValue: DEFAULT_REGION_HOME_GLYPH_COLOR
            }
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
                       "                 Host -> <a href='${extUri}' target='_blank'>${extUri}</a> \r" +
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
                       "                 URL -> <a href='${extUri}' target='_blank'>${extUri}</a> \r" +
                       "                 cmd -> Selected</i>\r\n\r\n" +
                       "     2. Click the up arrow button in the top right of the map to trigger a 'Send Location Now' to register the device with the Hubitat App."
                      )
        }
    }
}

def thumbnailCreation() {
    return dynamicPage(name: "thumbnailCreation", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Enabling User Thumbnails")) {
            paragraph ("Creating User Thumbnails for the OwnTracks Mobile App and optional OwnTracks Recorder.\r\n\r\n" +
                       "     1. Create a thumbnail for the user at a maximum resolution 192x192 pixels in JPG format using your computer.\r" +
                       "          a. 96x96 pixels at 96 DPI create a file size of ~5kB which is optimal.\r" +
                       "          b. Large file sizes can cause location timeouts from the mobile device (HTTP error 504).\r" +
                       "     2. Name the thumbnail 'MyUser.jpg' where 'MyUser' is the same name as the user name (case sensitive) entered in the mobile app.\r" +
                       "     3. In Hubitat:\r" +
                       "          a. Navigate to the <a href='http://${location.hubs[0].getDataValue("localIP")}/hub/fileManager' target='_blank'>Hubitat File Manager</a> ('Settings->File Manager').\r" +
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
                       "                 OTR_GEOKEY=\"\"\r" +
                       "                 OTR_BROWSERAPIKEY=\"\"\r" +
                       "                 OTR_SERVERLABEL=\"OwnTracks\"\r\n\r\n" +
                       "                 <b>NOTE:</b> Recorder defaults to OpenStreet Maps.  To use Google maps, add a Google Maps API key between the quotes for OTR_BROWSERAPIKEY.\r" +
                       "                              For reverse Geocode address lookups, add a Google Maps API key between the quotes for OTR_GEOKEY.\r" +
                       "                              Select <b>'Configure Hubitat App'</b> for directons to get API keys: \n"
                       )
                      href(title: "Configure Hubitat App", description: "", style: "page", page: "configureHubApp")
            paragraph ("          d. docker run -d --restart always --name=owntracks -p 8083:8083 -v recorder_store:/store -v config:/config owntracks/recorder\r\n\r\n" +
                       "     3. The above 'recorder_store' (STORAGEDIR) and 'config' is found here in Docker:\r" +
                       "          a. /<b>[HOME_PATH]</b>/docker/volumes/recorder_store/_data\r" +
                       "          b. /<b>[HOME_PATH]</b>/docker/volumes/config/_data\r\n\r\n" +
                       "     4. Access the Owntracks Recorder by opening a web broswer and navigating to 'http://<b>[enter.your.recorder.ip]</b>:8083'.\r"
                      )
        }
        section(getFormat("box", "Installing and configuring the OwnTracks Frontend (optional UI) using Docker")) {
            paragraph ("<b>NOTE:</b>  Instructions assume that Docker has already been installed and is operational and the Owntracks Recorder, above, has been installed an configured.\r\n\r\n" +
                       "For the source code and instructions, navigate to <a href='https://github.com/owntracks/frontend/' target='_blank'>OwnTracks Frontend GitHub</a> \r\n\r\n" +
                       "     1. Install OwnTracks Recorder:\r" +
                       "          a. docker pull owntracks/frontend\r\n\r\n" +
                       "     2. Configure OwnTracks Recorder:\r" +
                       "          a. docker run -d --restart always --name=owntracks_ui -p 8082:80 -e SERVER_HOST=<b>[enter.your.recorder.ip]</b> -e SERVER_PORT=8083 owntracks/frontend\r\n\r\n" +
                       "     3. Access the Owntracks Frontend by opening a web broswer and navigating to 'http://<b>[enter.your.recorder.ip]</b>:8082'.\r"
                      )
        }
        section(getFormat("box", "Adding user cards to OwnTracks Recorder")) {
            paragraph ("1. If user thumbnails have not been added to Hubitat, follow the instructions for 'Enabling User Thumbnail Instructions' first:")
            href(title: "Enabling User Thumbnails", description: "", style: "page", page: "thumbnailCreation")
            paragraph ("2. Select the slider to generate the enabled user's JSON card data in the Hubitat logs:")
            input name: "generateMemberCardJSON", type: "bool", title: "Create 'trace' outputs for each enabled member in the Hubitat logs.  Slider will turn off once complete.  ${(imageCards ? "" : "<div style='color:#ff0000'><b>Thumbnails are disabled.  Select 'Enabling User Thumbnails' to allow thumbnail generation.</b></div>")}", defaultValue: false, submitOnChange: true
            if (generateMemberCardJSON) {
                logMemberCardJSON()
                app.updateSetting("generateMemberCardJSON",[value: false, type: "bool"])
            }
            paragraph ("3. In the <a href='http://${location.hubs[0].getDataValue("localIP")}/logs' target='_blank'>Hubitat log</a>, look for the 'trace' output that looks like this:\r" +
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
            paragraph("The <a href='https://owntracks.org/booklet/clients/recorder/' target='_blank'>OwnTracks Recorder</a> (optional) can be installed for local tracking.  For the Recorder dashboard tiles and links to work outside the home network, the recorder must have a secure URL (https) and be secured with a public certificate.")
            input name: "recorderURL", type: "text", title: "HTTP URL of the OwnTracks Recorder.  It will be in the format <b>'http://enter.your.recorder.ip:8083'</b>, assuming using the default port of 8083.  The app will automatically add the <b>'$RECORDER_PUBLISH_FOLDER'</b> path.", defaultValue: ""
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
                       "2. Select the slider to enable mobile updates to be sent to the secondary hub URL as they arrive.\r" +
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

def advancedHub() {
    return dynamicPage(name: "advancedHub", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Hub App Configuration")) {
            if (state.submit) {
                appButtonHandler(state.submit)
	            state.submit = ""
            }
            input name: "resetHubDefaultsButton", type: "button", title: "Restore Defaults", state: "submit"
            input name: "regionHighAccuracyRadius", type: "enum", title: "Enable high accuracy reporting when location is between region radius and this value, Recommended=${displayMFtVal(DEFAULT_regionHighAccuracyRadius)}", defaultValue: "${DEFAULT_regionHighAccuracyRadius}", options: (imperialUnits ? [0:'disabled',250:'820 ft',500:'1640 ft',750:'2461 ft',1000:'3281 ft',1250:'4101 ft',1500:'4921 ft'] : [0:'disabled',250:'250 m',500:'500 m',750:'750 m',1000:'1000 m',1250:'1250 m',1500:'1500 m'])
            input name: "regionHighAccuracyRadiusHomeOnly", type: "bool", title: "High accuracy reporting is used for home region only when selected, all regions if not selected", defaultValue: DEFAULT_regionHighAccuracyRadiusHomeOnly
            input name: "warnOnNoUpdateHours", type: "number", title: "Highlight members on the 'Member Status' that have not reported a location for this many hours (1..168)", range: "1..168", defaultValue: DEFAULT_warnOnNoUpdateHours
            input name: "warnOnDisabledMember", type: "bool", title: "Display a warning in the logs if a family member reports a location but is not enabled", defaultValue: DEFAULT_warnOnDisabledMember
            input name: "warnOnMemberSettings", type: "bool", title: "Display a warning in the logs if a family member app settings are not configured for optimal operation", defaultValue: DEFAULT_warnOnMemberSettings
        }
    }
}

def advancedLocation() {
    return dynamicPage(name: "advancedLocation", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Mobile App Location Configuration")) {
            if (state.submit) {
                appButtonHandler(state.submit)
	            state.submit = ""
            }
            input name: "resetLocationDefaultsButton", type: "button", title: "Restore Defaults", state: "submit"
            input name: "monitoring", type: "enum", title: "Location reporting mode, Recommended=${MONITORING_MODES[DEFAULT_monitoring]}", required: true, options: MONITORING_MODES, defaultValue: DEFAULT_monitoring, submitOnChange: true
            if (getAndroidMembers()) {
                input name: "ping", type: "number", title: "Device will send a location interval at this heart beat interval (minutes) (15..360), Recommended=${DEFAULT_ping} (<b>Android ONLY</b>)", required: true, range: "15..60", defaultValue: DEFAULT_ping
                input name: "highPowerMode", type: "bool", title: "Use GPS for high accuracy locations.  <b>NOTE:</b> This will consume slightly more battery but will offer better performance in areas with poor WiFi/Cell coverage. (<b>Android ONLY</b>)", defaultValue: DEFAULT_highPowerMode, submitOnChange: true
                // if in high power mode, default to high accuracy pings
                if (highPowerMode) {
                    app.updateSetting("highAccuracyOnPing", [value: true, type: "bool"])
                    input name: "lowPowerModeInRegion", type: "bool", title: "Turn off high accuracy locations when the member is inside a region to reduce location jitter and power consumption. (<b>Android ONLY</b>)", defaultValue: DEFAULT_lowPowerModeInRegion
                } else {
                    input name: "highAccuracyOnPing", type: "bool", title: "Request a high accuracy location from members on their next location report after a ping update to keep location fresh (<b>Android ONLY</b>)", defaultValue: DEFAULT_highAccuracyOnPing
                }
            }
            input name: "ignoreInaccurateLocations", type: "number", title: "Do not send a location if the accuracy is greater than the given (${getSmallUnits()}) (0..${displayMFtVal(2000)}) Recommended=${displayMFtVal(DEFAULT_ignoreInaccurateLocations)}", required: true, range: "0..${displayMFtVal(2000)}", defaultValue: displayMFtVal(DEFAULT_ignoreInaccurateLocations)
            input name: "ignoreStaleLocations", type: "number", title: "Number of days after which location updates from friends are assumed stale and removed (0..7), Recommended=${DEFAULT_ignoreStaleLocations}", required: true, range: "0..7", defaultValue: DEFAULT_ignoreStaleLocations
            input name: "pegLocatorFastestIntervalToInterval", type: "bool", title: "Request that the location provider deliver updates no faster than the requested locater interval, Recommended '${DEFAULT_pegLocatorFastestIntervalToInterval}'", defaultValue: DEFAULT_pegLocatorFastestIntervalToInterval
            paragraph("<h3><b>Settings for Significant Monitoring Mode</b></h3>")
            if (getAndroidMembers()) {
                input name: "locatorDisplacement", type: "number", title: "How far the device travels (${getSmallUnits()}) before receiving another location update, Recommended=${displayMFtVal(DEFAULT_locatorDisplacement)}  <i><b>This value needs to be less than the minimum configured region radius for automations to trigger.</b></i> (<b>Android ONLY</b>)", required: true, range: "0..${displayMFtVal(1000)}", defaultValue: displayMFtVal(DEFAULT_locatorDisplacement)
            }
            input name: "locatorInterval", type: "number", title: "Device will not report location updates faster than this interval (seconds) unless moving.  When moving, Android uses this 'locaterInterval/6' or '5-seconds' (whichever is greater, unless 'locaterInterval' is less than 5-seconds, then 'locaterInterval' is used), Recommended=60  <i><b>Requires the device to move the above distance, otherwise no update is sent.</b></i>", required: true, range: "0..3600", defaultValue: DEFAULT_locatorInterval, submitOnChange: true
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
        setUpdateFlag([ "name":"" ], "updateLocation", true, false)
    }
}

def advancedDisplay() {
    return dynamicPage(name: "advancedDisplay", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Mobile App Display Configuration")) {
            if (state.submit) {
                appButtonHandler(state.submit)
	            state.submit = ""
            }
            input name: "resetDisplayDefaultsButton", type: "button", title: "Restore Defaults", state: "submit"
            input name: "replaceTIDwithUsername", type: "bool", title: "Replace the 'TID' (tracker ID) with 'username' for displaying a name on the map and recorder", defaultValue: DEFAULT_replaceTIDwithUsername
            input name: "notificationEvents", type: "bool", title: "Notify about received events", defaultValue: DEFAULT_notificationEvents
            input name: "extendedData", type: "bool", title: "Include extended data in location reports", defaultValue: DEFAULT_extendedData
            input name: "enableMapRotation", type: "bool", title: "Allow the map to be rotated", defaultValue: DEFAULT_enableMapRotation
            input name: "showRegionsOnMap", type: "bool", title: "Display the region pins/bubbles on the map", defaultValue: DEFAULT_showRegionsOnMap
            input name: "notificationLocation", type: "bool", title: "Show last reported location in ongoing notification banner", defaultValue: DEFAULT_notificationLocation
            input name: "notificationGeocoderErrors", type: "bool", title: "Display Geocoder errors in the notification banner", defaultValue: DEFAULT_notificationGeocoderErrors
        }
        setUpdateFlag([ "name":"" ], "updateDisplay", true, false)
    }
}

def configureRegions() {
    return dynamicPage(name: "configureRegions", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Configure Regions")) {
        }
        if (isMapAllowed(false)) {
            def deviceWrapper = getChildDevice(getCommonChildDNI())
            if (deviceWrapper) {
                deviceWrapper.generateConfigMapTile()
            }
            section() {
                input name: "sectionRegion", type: "button", title: getSectionTitle(state.show.region, "Region Map Instructions and Delete Behavior"), submitOnChange: true, style: getSectionStyle()
                if (state.show.region) {
                    paragraph ("<h2>Add a Region</h2>" +
                               "1. Click the map to drop a pin at a desired location.\r" +
                               "2. Add the region name and radius information.\r" +
                               "3. Once 'Save' is selected, all enabled members will automatically receive the changes on their next location report.\r" +
                               "4. Clicking on the map or another region without saving will remove this pin.\r" +
                               "5. The pin will remain red until it is saved.\r" +
                               "<b>NOTE:</b> If a Google geocode API has been entered, an input box to allow direct address lookup will be displayed."
                    )
                    paragraph ("<h2>Edit a Region</h2>" +
                               "1. Select the pin to be edited or a region from the selection box.\r" +
                               "2. Once 'Save' is selected, all enabled members will automatically receive the changes on their next location report.\r"
                    )
                    paragraph ("<h2>Assign a Home Region</h2>" +
                               "1. Select a pin to be 'Home'.\r" +
                               "2. Select the 'Set Home' button to assign the region.\r" +
                               "3. New pins must be saved before the 'Set Home' button is visible.\r" +
                               "4. The 'Home' pin will be larger with a green glyph.\r"
                    )
                    paragraph ("<h2>Delete a Region</h2>" +
                               "1. Select the pin to be deleted.\r" +
                               "2. Select the 'Delete' button to remove the region from the map.\r" +
                               "3. <b>NOTE:</b> The actual delete behavior will be based on the operation described below.\r"
                    )
                    input name: "manualDeleteBehavior", type: "bool", title: "Manual Delete", defaultValue: DEFAULT_manualDeleteBehavior, submitOnChange: true
                    paragraph("<h3><b>Manual Delete: Region Deleted from Hub Only.  Requires Region to be Manually Deleted from Mobile</b></h3>" +
                              "1. Deleted regions will be deleted from the Hubitat <b>ONLY</b>.\r" +
                              "2. On each mobile phone, find and remove the region that was deleted.\r"
                             )

                    paragraph ("<h3><b>Automatic Delete: Region Deleted from Hub and Mobile after Location Update</b></h3>" +
                               "1. The deleted region will be assigned an invalid lat/lon, but will not be immediately removed.\r" +
                               "2. The region will remain in the Hubitat until <b>ALL</b> enabled users have sent a location report.\r" +
                               "3. Once the last user has sent a location report, the region will be deleted from Hubitat.\r"
                              )
                }
            }
        }
        section() {
            if (isMapAllowed(false)) {
                configMapURL = "${getAttributeURL(URL_SOURCE[0],'configmap')}"
                paragraph("<iframe src='${configMapURL}' style='height: 650px; width: 100%; border: none;'></iframe>")
            } else {
                clearSettingFields()
                paragraph ("<b>Configure a Google Maps API key in 'Additional Hubitat App Settings' -> 'Google Maps Settings' to allow radius bubbles to be displayed around the regions.</b>")
                href(title: "Add Regions", description: "", style: "page", page: "addRegions")
                href(title: "Edit Regions", description: "", style: "page", page: "editRegions")
                href(title: "Delete Regions", description: "", style: "page", page: "deleteRegions")
            }
        }
        // only display if we don't have a Google maps API key, or our quota is expired
        if (!isMapAllowed(false)) {
            section(getFormat("line", "")) {
                input "homePlace", "enum", multiple: false, title:(homePlace ? '<div>' : '<div style="color:#ff0000">') + "Select your 'Home' place. ${(homePlace ? "" : "Use 'Configure Regions'->'Add Regions' to create a home location.")}" + '</div>', options: getNonFollowRegions(COLLECT_PLACES["desc_tst"]), submitOnChange: true
                paragraph("<iframe src='${getRegionMapLink(getHomeRegion())}' style='height: 500px; width: 100%; border: none;'></iframe>")
            }
        }
        checkForHome()
        section(getFormat("line", "")) {
            input "getMobileRegions", "enum", multiple: true, title:"Hubitat can retrieve regions from a member's OwnTracks mobile device and merge them into the Hubitat region list. Select family member(s) to retrieve their region list on next location update.", options: getEnabledAndNotHiddenMembers()
            if (secondaryHubURL && enableSecondaryHub) {
                if (state.submit) {
                    paragraph "<b>${appButtonHandler(state.submit)}</b>"
                    state.submit = ""
                }
                input name: "sendRegionsToSecondaryButton", type: "button", title: "Send Region List to Secondary Hub", state: "submit"
                input name: "getRegionsFromSecondaryButton", type: "button", title: "Retrieve Region List from Secondary Hub", state: "submit"
            }
        }
    }
}

def configureGroups() {
    return dynamicPage(name: "configureGroups", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Configure Groups")) {
            paragraph ("<h3>Groups are used to create isolated friend 'bubbles' to prevent locations being shared with all members.</h3>" +
                       "For example,\r" +
                       "     There are two defined groups with 5 members:\r" +
                       "         - Default: A, B, C, D\r" +
                       "         - Group 1: B, E\r" +
                       "     1. Members A, B, C, D will see each other, but not member E\r" +
                       "     2. Member B will see members A, B, C, D and E\r" +
                       "     3. Member E will only see members B and E\r\r" +
                       "How the app interacts with members in isolated friend groups:\r" +
                       "     1. Presence detection only occurs for members in the 'Default' group.\r" +
                       "     2. Regions are only shared with members in the 'Default' group.\r" +
                       "     3. Only members in the 'Default' group are sent to the OwnTracks Recorder.\r" +
                       "     4. Member locations are shared with other members in intersecting groups.\r\r" +
                       "How the Google Friend Map interacts with members in isolated friend groups:\r\n" +
                       "     1. By default, only members in the 'Default' group are displayed.\r" +
                       "     2. If a member name is passed in the '&member=' suffix of the URL, they will see all members in the groups that the member is assigned to.\r" +
                       "     3. In the above example, using the URL suffix of '&member=E' would only show members B and E on the Google map."
                      )
        }
        section() {
            if (state.submit) {
                paragraph "<b>${appButtonHandler(state.submit)}</b>"
                state.submit = ""
            }
            input name: "resetGroupsButton", type: "button", title: "Reset Member Groups and Names", state: "submit"
            input "selectGroup", "enum", multiple: false, title: "Select group to edit name or assign members.", options: state.groups.collectEntries{[it.id, it.name]}, submitOnChange: true
            if (selectGroup) {
                app.updateSetting("selectFamilyMembers",[value:state.members.findAll{it.groups.find{it == selectGroup}}.name.sort(),type:"enum"])
                app.updateSetting("groupName",[value:state.groups.find{it.id == selectGroup}.name,type:"text"])
                input "selectFamilyMembers", "enum", multiple: true, title:"Select family member(s) to assign to this group.", options: state.members.name.sort()
                // prevent editing the default group name
                if (selectGroup != "${DEFAULT_globalGroupNumber}") {
                    input name: "groupName", type: "text", title: "Group Name", required: true
                }

                input name: "saveGroupButton", type: "button", title: "Save Settings", state: "submit"
            }
        }
    }
}

def configureNotifications() {
    return dynamicPage(name: "configureNotifications", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Configure Stale Member Notifications")) {
            input "notificationStaleList", "capability.notification", title: "Select device(s) to get notifications when members stop reporting locations and go stale.", multiple: true, required: false, offerAll: true, submitOnChange: true
        }
        section(getFormat("box", "Configure Region Arrived/Departed Notifications")) {
            input "notificationList", "capability.notification", title: "Global enable/disable of notification devices.  Select per member enter/leave notifications below for these devices.", multiple: true, required: false, offerAll: true, submitOnChange: true
        }
        section(getFormat("line", "")) {
            if (state.submit) {
                paragraph "<b>${appButtonHandler(state.submit)}</b>"
                state.submit = ""
            }
            input "selectFamilyMembers", "enum", multiple: false, title:"Select family member to change arrived/departed notifications.", options: state.members.name.sort(), submitOnChange: true
            // only clear on a change of selected member
            if (state.selectFamilyMembers != selectFamilyMembers) {
                app.removeSetting("notificationEnter")
                app.removeSetting("notificationEnterRegions")
                app.removeSetting("notificationLeave")
                app.removeSetting("notificationLeaveRegions")
            }
            if (selectFamilyMembers) {
                input name: "clearNotificationsButton", type: "button", title: "Clear Settings", state: "submit"
                state.selectFamilyMembers = selectFamilyMembers
                input "notificationEnter", "enum", title: "Select device(s) to get notifications when this member <b>enters</b> selected region(s).", multiple: true, offerAll: true, required: false, options: notificationList.collect{entry -> entry.displayName}, defaultValue: state.members.find {it.name==selectFamilyMembers}?.enterDevices
                input "notificationEnterRegions", "enum", multiple: true, offerAll: true, title: "Trigger notifications when this member <b>enters</b> these region(s).", options: getNonFollowRegions(COLLECT_PLACES["desc_tst"]), defaultValue: state.members.find {it.name==selectFamilyMembers}?.enterRegions

                input "notificationLeave", "enum", title: "Select device(s) to get notifications when this member <b>leaves</b> selected region(s).", multiple: true, offerAll: true, required: false, options: notificationList.collect{entry -> entry.displayName}, defaultValue: state.members.find {it.name==selectFamilyMembers}?.leaveDevices
                input "notificationLeaveRegions", "enum", multiple: true, offerAll: true, title: "Trigger notifications when this member <b>leaves</b> these region(s).", options: getNonFollowRegions(COLLECT_PLACES["desc_tst"]), defaultValue: state.members.find {it.name==selectFamilyMembers}?.leaveRegions
                input name: "saveNotificationsButton", type: "button", title: "Save Settings", state: "submit"
            }
        }
        section(getFormat("line", "")) {
            input name: "useCustomNotificationMessage", type: "bool", title: "Use a custom notification message.  The default message format is '<b>$DEFAULT_notificationMessage</b>'", defaultValue: DEFAULT_useCustomNotificationMessage, submitOnChange: true
            if (useCustomNotificationMessage) {
                input name: "notificationMessage", type: "textarea", title: "Enter notification message.  Variables are case sensitive.", defaultValue: DEFAULT_notificationMessage, submitOnChange: true
            }
        }
    }
}

def memberInGlobalMemberGroup(member) {
    // check if the member is part of the global member group (undefined or zero), or is a secondary hub update
    if ((member.name == COMMON_CHILDNAME) || (member?.groups == null) || (member?.groups?.find {it=="${DEFAULT_globalGroupNumber}"})) {
        return(true)
    } else {
        return(false)
    }
}

def getMatchingGroupMembersForMember(member) {
    groupMembers = []
    // find all enabled members that contain the passed in member's group
    settings?.enabledMembers.each { enabled->
        enabledMember = state.members.find {it.name==enabled}
        // if the member is part of the enabled members group, add them to the map
        if (!(settings?.privateMembers.find {it==enabled}) && ((member?.groups == null) || (enabledMember?.groups?.intersect(member?.groups)))) {
            groupMembers << enabledMember
        }
    }

    return(groupMembers)
}

def getEnabledAndNotHiddenMembers() {
    allowedMembers = []
    // build a list of enabled and not hidden members, and deal with global membership
    settings?.enabledMembers.each { enabled->
        enabledMember = state.members.find {it.name==enabled}
        if (!(settings?.privateMembers.find {it==enabled}) && memberInGlobalMemberGroup(enabledMember)) {
            allowedMembers << enabled
        }
    }
    if (allowedMembers) {
        return (allowedMembers.sort())
    } else {
        return (allowedMembers)
    }
}

def getEnabledAndNotHiddenMemberData(memberName) {
    member = state.members.find {it.name.toLowerCase()==memberName?.toLowerCase()}
    // in case no member name is passed in, only allow global members
    if (member == null) {
        member = []
        member << [ groups:"${DEFAULT_globalGroupNumber}" ]
    }

    return((getMatchingGroupMembersForMember(member)?.sort { it.lastReportTime }))
}

def getNonFollowRegions(collectRegions) {
    allowedRegions = []

    // build a list of regions that don't start with '+' to screen off the '+follow' iOS regions
    state.places.each { place->
        if (place.desc[0] != "+") {
            allowedRegions << place
        }
    }

    if (allowedRegions) {
        switch (collectRegions) {
            case 0:
                // collecton of names
                return (allowedRegions.desc.sort())
            break
            case 1:
                // collecton of names and timestamp
                return (allowedRegions.collectEntries{[it.tst, it.desc]})
            break
        }
    }

    // entire map or blank map
    return (allowedRegions)
}

def getHomeRegion() {
    return ((homePlace ? state.places.find {it.tst==homePlace.toInteger()} : []))
}

def getAttributeURL(urlSource, path) {
    // remove the []
    urlSource = urlSource.substring(1, (urlSource.length()-1))

    // get the cloud or local URL
    if (fullApiServerUrl().indexOf(urlSource, 0) >= 0) {
        return(fullApiServerUrl().replaceAll("null","${path}?access_token=${state.accessToken}"))
    } else {
        return(getFullLocalApiServerUrl() + "/${path}" + "?access_token=${state.accessToken}")
    }
}

def displayRegionsPendingDelete() {
    // get the names of any regions that are pending deletion
    pendingDelete = state?.places.findAll{it.lat == INVALID_COORDINATE}.collect{place -> place.desc}
    if (pendingDelete) {
        paragraph "<div style='color:#ff0000'><b>${pendingDelete} pending deletion once all members report a location update.</b></div>"
    }
}

def displayMissingHomePlace() {
    // display an error if the home place is missing
    if (!homePlace) {
        paragraph "<div style='color:#ff0000'><b>'Home' place not set. Click 'Installation and Configuration' -> 'Configure Regions' to select or add a home location.</b></div>"
    }
}

def addRegions() {
    return dynamicPage(name: "addRegions", title: "", nextPage: "configureRegions") {
        section(getFormat("box", "Add a Region")) {
            if (state.submit) {
                paragraph "<b>${appButtonHandler(state.submit)}</b>"
                state.submit = ""
            }
            paragraph ("1. Add the region to be information.\r" +
                       "2. Once 'Save' is selected, all enabled members will automatically receive the changes on their next location report.\r"
            )
            if (geocodeProvider == "0") {
                paragraph ("<b>Configure a geocode provider in 'Additional Hubitat App Settings' -> 'Geocode Settings' to enable address to latitude/longitude lookup.</b>")
            } else {
                input "regionAddress", "text", title: "Enter address to populate the latitude/longitude.  Confirm the location is correct using the map below.", submitOnChange: true
                if (regionAddress) {
                    (addressLat, addressLon) = geocode(regionAddress)
                    app.updateSetting("regionLat",[value:addressLat,type:"double"])
                    app.updateSetting("regionLon",[value:addressLon,type:"double"])
                }
            }
        }
        section(getFormat("line", "")) {
            // assign defaults so the map populates properly
            if (settings["regionLat"] == null) settings["regionLat"] = location.getLatitude()
            if (settings["regionLon"] == null) settings["regionLon"] = location.getLongitude()

            //createRegionMap(lat,lon,rad)
            paragraph("<iframe src='${getRegionMapLink(createRegionMap(settings["regionLat"],settings["regionLon"],settings["regionRadius"]))}' style='height: 500px; width:100%; border: none;'></iframe>")
            input "regionName", "text", title: "Name", submitOnChange: true
            input name: "regionRadius", type: "number", title: "Detection radius (${getSmallUnits()}) (${displayMFtVal(50)}..${displayMFtVal(1000)})", range: "${displayMFtVal(50)}..${displayMFtVal(1000)}", defaultValue: displayMFtVal(DEFAULT_RADIUS), submitOnChange: true
            input name: "regionLat", type: "double", title: "Latitude (-90.0..90.0)", range: "-90.0..90.0", defaultValue: location.getLatitude(), submitOnChange: true
            input name: "regionLon", type: "double", title: "Longitude (-180.0..180.0)", range: "-180.0..180.0", defaultValue: location.getLongitude(), submitOnChange: true
            input name: "addRegionButton", type: "button", title: "Save", state: "submit"
        }
    }
}

def addRegionToEdit() {
    input "regionToEdit", "enum", multiple: false, title:"Select region to edit", options: getNonFollowRegions(COLLECT_PLACES["desc"]), submitOnChange: true
}

def editRegions() {
    return dynamicPage(name: "editRegions", title: "", nextPage: "configureRegions") {
        section(getFormat("box", "Edit a Region")) {
            if (state.submit) {
                paragraph "<b>${appButtonHandler(state.submit)}</b>"
                state.submit = ""
            }
            paragraph ("1. Select the region to be edited.\r" +
                       "2. Once 'Save' is selected, all enabled members will automatically receive the changes on their next location report.\r"
            )
            addRegionToEdit()
            if (regionToEdit) {
                // get the place map and assign the current values
                def foundPlace = state.places.find {it.desc==regionToEdit}
                app.updateSetting("regionName",[value:foundPlace.desc,type:"text"])
                if (state.previousRegionName != regionName) {
                    app.updateSetting("regionRadius",[value:displayMFtVal(foundPlace.rad.toInteger()),type:"number"])
                    app.updateSetting("regionLat",[value:foundPlace.lat,type:"double"])
                    app.updateSetting("regionLon",[value:foundPlace.lon,type:"double"])
                }
                // save the name in so we can retrieve the values should it get changed below
                state.previousRegionName = regionName
                paragraph("<iframe src='${getRegionMapLink(createRegionMap(settings["regionLat"],settings["regionLon"],settings["regionRadius"]))}' style='height: 500px; width:100%; border: none;'></iframe>")
                input name: "regionName", type: "text", title: "Name", required: true
                input name: "regionRadius", type: "number", title: "Detection radius (${getSmallUnits()})", required: true, range: "${displayMFtVal(50)}..${displayMFtVal(1000)}", submitOnChange: true
                input name: "regionLat", type: "double", title: "Latitude (-90.0..90.0)", required: true, range: "-90.0..90.0", submitOnChange: true
                input name: "regionLon", type: "double", title: "Longitude (-180.0..180.0)", required: true, range: "-180.0..180.0", submitOnChange: true
                input name: "editRegionButton", type: "button", title: "Save", state: "submit"
            }
        }
    }
}

def deleteRegions() {
    return dynamicPage(name: "deleteRegions", title: "", nextPage: "configureRegions") {
        section(getFormat("box", "Delete Region")) {
            if (state.submit) {
                paragraph "<b>${getFormat("redText", appButtonHandler(state.submit))}</b>"
                state.submit = ""
            }
            displayRegionsPendingDelete()
            input "regionName", "enum", multiple: false, title:"Select region to delete", options: getNonFollowRegions(COLLECT_PLACES["desc"]), submitOnChange: true
            if (regionName) {
                deleteRegion = state.places.find {it.desc==regionName}
                paragraph("<iframe src='${getRegionMapLink(createRegionMap(deleteRegion?.lat,deleteRegion?.lon,deleteRegion?.rad))}' style='height: 500px; width:100%; border: none;'></iframe>")
                paragraph("<h3><b>Delete Region from Hub Only - Manually Delete Region from Mobile</b></h3>" +
                          "1. Click the 'Delete Region from Hubitat ONLY' button.\r" +
                          "2. On each mobile phone, find and delete the region selected above.\r"
                )
                input name: "deleteRegionFromHubButton", type: "button", title: "Delete Region from Hubitat ONLY", state: "submit"
                paragraph ("<h3><b>Automatically Delete Region from Hub and Mobile after Location Update</b></h3>" +
                           "1. Selected region will be assigned an invalid lat/lon.\r" +
                           "2. The region will remain in the list until <b>ALL</b> enabled users have sent a location report.\r" +
                           "3. Once the last user has sent a location report, the region will be deleted from Hubitat.\r"
                )
                input name: "deleteRegionFromAllButton", type: "button", title: "Delete Region from Hubitat and Mobile(s)", state: "submit"
            }
        }
    }
}

def sendRegionsToSecondaryHub() {
    data = [ "_type":"waypoints", "waypoints":state.places ]
    def postParams = [ uri: secondaryHubURL?.trim(), requestContentType: 'application/json', contentType: 'application/json', headers: ["X-limit-u" : COMMON_CHILDNAME, "X-limit-d" : COMMON_CHILDNAME], body : (new JsonBuilder(data)).toPrettyString() ]
    asynchttpPost("httpCallbackMethod", postParams)
}

def retrieveRegionsFromSecondaryHub() {
    data = sendReportWaypointsRequest([ name:COMMON_CHILDNAME ])
    def postParams = [ uri: secondaryHubURL?.trim(), requestContentType: 'application/json', contentType: 'application/json', headers: ["X-limit-u" : COMMON_CHILDNAME, "X-limit-d" : COMMON_CHILDNAME], body : (new JsonBuilder(data)).toPrettyString() ]
    asynchttpPost("httpCallbackMethod", postParams)
}

def getDisabledMembers() {
    disabledMembers = []
    state.members.each { member->
        if (!settings?.enabledMembers.find {it==member.name}) {
            disabledMembers << member
        }
    }
    return(disabledMembers)
}

def deleteMembers() {
    return dynamicPage(name: "deleteMembers", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Deactivate Family Member(s)")) {
            if (state.submit) {
                paragraph "<b>${getFormat("redText", appButtonHandler(state.submit))}</b>"
                state.submit = ""
            }
            input "selectDeactivateFamilyMembers", "enum", multiple: true, title:"Select family member(s) to delete their phone URL and waypoints.  Only members that are not enabled are listed.", options: getDisabledMembers()?.name?.sort(), submitOnChange: true
            paragraph("<b>NOTE: Selected user(s) will be deactivated on their next location update.  They will no longer be able to reach the OwnTracks app without configuration!</b>")
            input name: "deactivateMembersButton", type: "button", title: "Deactivate", state: "submit"
        }
        section(getFormat("box", "Delete Family Member(s)")) {
            if (state.submit) {
                paragraph "<b>${getFormat("redText", appButtonHandler(state.submit))}</b>"
                state.submit = ""
            }
            input "selectFamilyMembers", "enum", multiple: true, title:"Select family member(s) to delete.", options: state.members.name.sort(), submitOnChange: true

            paragraph("<b>NOTE: Selected user(s) will be deleted from the app and their corresponding child device will be removed.  Ensure no automations are dependent on their device before proceeding!</b>")
            input name: "deleteMembersButton", type: "button", title: "Delete", state: "submit"
        }
    }
}

def resetDefaults() {
    return dynamicPage(name: "resetDefaults", title: "", nextPage: "mainPage") {
        section(getFormat("box", "Reset to Recommended Default Settings")) {
            if (state.submit) {
                paragraph "<b>${getFormat("redText", appButtonHandler(state.submit))}</b>"
                state.submit = ""
            }
            paragraph("Reset Hubitat and Mobile Settings to Recommended Defaults.  <b>NOTE:  Members, Regions, Recorder and Secondary Hub settings will not be deleted.</b>")
            input name: "resetAllDefaultsButton", type: "button", title: "Restore Defaults for All Settings", state: "submit"
        }
        section(getFormat("line", "")) {
            input name: "resetHubDefaultsButton", type: "button", title: "Restore Defaults for 'Additional Hub App Settings'", state: "submit"
            input name: "resetLocationDefaultsButton", type: "button", title: "Restore Defaults for 'Mobile App Location Settings'", state: "submit"
            input name: "resetDisplayDefaultsButton", type: "button", title: "Restore Defaults for 'Mobile App Display Settings'", state: "submit"
        }
    }
}

String appButtonHandler(btn) {
    def success = false
    def updateMember = false
    def result = ""

    switch (btn) {
        case "addRegionButton":
            // check if there are any regions selected
            if (regionName) {
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
                        def newPlace = [ "_type": "waypoint", "desc": "${regionName}", "lat": regionLat.toDouble().round(6), "lon": regionLon.toDouble().round(6), "rad": convertToMeters(regionRadius), "tst": (now()/1000).toInteger() ]
                        // add the new place
                        state.places << newPlace
                        result = "Region '${regionName}' has been added."
                        logDescriptionText(result)
                        success = true
                        updateMember = true
                    }
                }
            }
        break
        case "editRegionButton":
            // find the existing place to update.
            def foundPlace = state.places.find {it.desc==state.previousRegionName}
            // create the updated waypoint map - NOTE: the app keys off the "tst" field as a unique identifier
            def newPlace = [ "_type": "waypoint", "desc": "${regionName}", "lat": regionLat.toDouble().round(6), "lon": regionLon.toDouble().round(6), "rad": convertToMeters(regionRadius), "tst": foundPlace.tst ]
            // overwrite the existing place
            foundPlace << newPlace
            result = "Updating region '${newPlace.desc}'"
            logDescriptionText(result)
            success = true
            updateMember = true
        break
        case "deleteRegionFromAllButton":
        case "deleteRegionFromHubButton":
            // check if there are any regions selected
            if (regionName) {
                // unvalidate all the places that need to be removed
                place = state.places.find {it.desc==regionName}
                // check if we are deleting our home location
                if (homePlace?.toInteger() == place.tst) {
                    app.removeSetting("homePlace")
                }
                if (btn == "deleteRegionFromHubButton") {
                    // remove the place from our current list but don't trigger a member update
                    deleteIndex = state.places.findIndexOf {it.desc==regionName}
                    if (deleteIndex >= 0) {
                        state.places.remove(deleteIndex)
                    }
                    updateMember = false
                    result = "Deleting region '${regionName}' from Hubitat <b>ONLY</b>.  Manually remove '${regionName}' from each mobile."
                } else {
                    // invalidate the coordinates to flag it for deletion
                    place.lat = INVALID_COORDINATE
                    place.lon = INVALID_COORDINATE
                    updateMember = true
                    result = "Deleting region '${regionName}' from Hubitat and each mobile once they <b>ALL</b> report a location."
                }
                logWarn(result)
                success = true
            }
        break
        case "sendRegionsToSecondaryButton":
            sendRegionsToSecondaryHub()
            result = "Regions sent to secondary hub."
        break
        case "getRegionsFromSecondaryButton":
            retrieveRegionsFromSecondaryHub()
            result = "Regions requested from secondary hub."
        case "deactivateMembersButton":
            if (selectDeactivateFamilyMembers) {
                selectDeactivateFamilyMembers.each { name ->
                    member = state.members.find {it.name==name}
                    // flag the member for deactivation
                    member["deactivate"] = 1
                }
                result = "Deactivating family members '${selectDeactivateFamilyMembers}'"
                logWarn(result)
                app.removeSetting("selectDeactivateFamilyMembers")
            }
        break
        case "deleteMembersButton":
            if (selectFamilyMembers) {
                selectFamilyMembers.each { name ->
                    deleteIndex = state.members.findIndexOf {it.name==name}
                    def deviceWrapper = getChildDevice(state.members[deleteIndex].id)
                    try {
                        deleteChildDevice(deviceWrapper.deviceNetworkId)
                    } catch(e) {
                        logDebug("Device for ${name} does not exist.")
                    }
                    state.members.remove(deleteIndex)
                }
                result = "Deleting family members '${selectFamilyMembers}'"
                logWarn(result)
                app.removeSetting("selectFamilyMembers")
            }
        break
        case "resetGroupsButton":
            initializeGroupList(true)
            result = "Default grouping and names assigned to groups"
        break
        case "saveGroupButton":
            if (selectGroup) {
                group = state.groups.find{it.id == selectGroup}
                // update the name
                if (groupName?.trim()?.length()) {
                    if (group.name != groupName?.trim()) {
                        group.name = groupName?.trim()
                    }
                }
                // add/remove the group id from the member groups
                state.members.each { member ->
                    // first remove it
                    member.groups.remove(group.id)
                    // if we have a match add it back
                    if (selectFamilyMembers.find {it==member.name}) {
                        member.groups << group.id
                    }
                }
                result = "Updated group name and members"
            }
        break
        case "clearNotificationsButton":
            if (selectFamilyMembers) {
                member = state.members.find {it.name==selectFamilyMembers}
                member.enterDevices = null
                member.enterRegions = null
                member.leaveDevices = null
                member.leaveRegions = null
                state.selectFamilyMembers = null
                result = "Cleared notification settings for family member '${selectFamilyMembers}'"
            }
        break
        case "saveNotificationsButton":
            if (selectFamilyMembers) {
                member = state.members.find {it.name==selectFamilyMembers}
                if (notificationEnter)         member.enterDevices = notificationEnter - "Toggle All On/Off"
                if (notificationEnterRegions)  member.enterRegions = notificationEnterRegions - "Toggle All On/Off"
                if (notificationLeave)         member.leaveDevices = notificationLeave - "Toggle All On/Off"
                if (notificationLeaveRegions)  member.leaveRegions = notificationLeaveRegions - "Toggle All On/Off"
                result = "Updated notification settings for family member '${selectFamilyMembers}'"
            }
        break
        case "resetAllDefaultsButton":
            initialize(true)
            result = "All settings reset to recommended defaults."
            logWarn(result)
        break
        case "resetHubDefaultsButton":
            initializeHub(true)
            result = "Hub settings reset to recommended defaults."
            logWarn(result)
        break
        case "resetLocationDefaultsButton":
            initializeMobileLocation(true)
            result = "Mobile location settings reset to recommended defaults."
            logWarn(result)
        break
        case "resetDisplayDefaultsButton":
            initializeMobileDisplay(true)
            result = "Mobile display settings reset to recommended defaults."
            logWarn(result)
        break
        case "sectionInstall":
            state.show.install = state.show.install ? false : true
        break
        case "sectionLinks":
            state.show.links = state.show.links ? false : true
        break
        case "sectionCommands":
            state.show.commands = state.show.commands ? false : true
        break
        case "sectionOptional":
            state.show.optional = state.show.optional ? false : true
        break
        case "sectionAdvanced":
            state.show.advanced = state.show.advanced ? false : true
        break
        case "sectionMaintenance":
            state.show.maintenance = state.show.maintenance ? false : true
        break
        case "sectionLogging":
            state.show.logging = state.show.logging ? false : true
        break
        case "sectionHubsettings":
            state.show.hubsettings = state.show.hubsettings ? false : true
        break
        case "sectionGeocode":
            state.show.geocode = state.show.geocode ? false : true
        break
        case "sectionMap":
            state.show.map = state.show.map ? false : true
        break
        case "sectionRegion":
            state.show.region = state.show.region ? false : true
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
        setUpdateFlag([ "name":"" ], "updateWaypoints", updateMember, true)
    }

    return (result)
}

def clearSettingFields() {
    // clear the setting fields
    app.removeSetting("regionToEdit")
    app.removeSetting("regionAddress")
    app.removeSetting("regionName")
    app.removeSetting("regionRadius")
    app.removeSetting("regionLat")
    app.removeSetting("regionLon")
    app.removeSetting("selectDeactivateFamilyMembers")
    app.removeSetting("selectFamilyMembers")
    app.removeSetting("notificationEnter")
    app.removeSetting("notificationEnterRegions")
    app.removeSetting("notificationLeave")
    app.removeSetting("notificationLeaveRegions")
    app.removeSetting("selectMemberGlyph")
    app.removeSetting("memberGlyphColor")
    app.removeSetting("selectGroup")
    app.removeSetting("groupName")
    state.previousRegionName = null
    state.selectFamilyMembers = null
    state.selectMemberGlyph = null
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
    if (state.show == null) state.show = [ install: true, links: false, commands: false, optional: false, advanced: false, maintenance: false, logging: false, hubsettings: false, geocode: false, map: false, region: false ]
    if (state.accessToken == null) state.accessToken = ""
    if (state.members == null) state.members = []
    if (state.places == null) state.places = []
    if (state.mapApiUsage == null) state.mapApiUsage = 0
    if (state.lastGoogleFriendsLocationTime == null) state.lastGoogleFriendsLocationTime = 0
    if (state.lastReportTime == null) state.lastReportTime = new SimpleDateFormat("E h:mm a yyyy-MM-dd").format(new Date())
    if (state.googleMapsZoom == null) state.googleMapsZoom = DEFAULT_googleMapsZoom
    if (state.googleMapsMember == null) state.googleMapsMember = DEFAULT_googleMapsMember
    GEOCODE_USAGE_COUNTER.eachWithIndex { entry, index ->
        String provider = GEOCODE_USAGE_COUNTER[index+1]
        if (state."$provider" == null) {
            state."$provider" = 0
        }
    }

    // assign hubitat defaults
    if (homeSSID == null) app.updateSetting("homeSSID", [value: "", type: "string"])
    if (imperialUnits == null) app.updateSetting("imperialUnits", [value: DEFAULT_imperialUnits, type: "bool"])
    if (disableCloudLinks == null) app.updateSetting("disableCloudLinks", [value: DEFAULT_disableCloudLinks, type: "bool"])
    if (deviceNamePrefix == null) app.updateSetting("deviceNamePrefix", [value: DEFAULT_CHILDPREFIX, type: "string"])
    if (forceDefaults || (imageCards == null)) app.updateSetting("imageCards", [value: DEFAULT_imageCards, type: "bool"])
    if (forceDefaults || (highPowerMode == null)) app.updateSetting("highPowerMode", [value: DEFAULT_highPowerMode, type: "bool"])
    if (forceDefaults || (lowPowerModeInRegion == null)) app.updateSetting("lowPowerModeInRegion", [value: DEFAULT_lowPowerModeInRegion, type: "bool"])
    if (forceDefaults) app.updateSetting("descriptionTextOutput", [value: DEFAULT_descriptionTextOutput, type: "bool"])
    if (forceDefaults) app.updateSetting("debugOutput", [value: DEFAULT_debugOutput, type: "bool"])
    if (forceDefaults || (debugResetHours == null)) app.updateSetting("debugResetHours", [value: DEFAULT_debugResetHours, type: "number"])

    // assign the defaults to the hub settings
    initializeHub(forceDefaults)
    // assign the defaults to the mobile app location settings
    initializeMobileLocation(forceDefaults)
    // assign the defaults to the mobile app display settings
    initializeMobileDisplay(forceDefaults)
    // add the iOS +follow location to allow for transition updates
    updatePlusFollow()
}

def initializeHub(forceDefaults) {
    if (forceDefaults) {
        app.removeSetting("regionHighAccuracyRadius")
        app.removeSetting("wifiPresenceKeepRadius")
        app.removeSetting("geocodeProvider")
        app.removeSetting("selectMemberGlyph")
        app.removeSetting("memberGlyphColor")
    }
    if (forceDefaults || (regionHighAccuracyRadius == null)) app.updateSetting("regionHighAccuracyRadius", [value: DEFAULT_regionHighAccuracyRadius, type: "number"])
    if (forceDefaults || (wifiPresenceKeepRadius == null)) app.updateSetting("wifiPresenceKeepRadius", [value: DEFAULT_wifiPresenceKeepRadius, type: "number"])
    if (forceDefaults || (regionHighAccuracyRadiusHomeOnly == null)) app.updateSetting("regionHighAccuracyRadiusHomeOnly", [value: DEFAULT_regionHighAccuracyRadiusHomeOnly, type: "bool"])
    if (forceDefaults || (warnOnNoUpdateHours == null)) app.updateSetting("warnOnNoUpdateHours", [value: DEFAULT_warnOnNoUpdateHours, type: "number"])
    if (forceDefaults || (warnOnDisabledMember == null)) app.updateSetting("warnOnDisabledMember", [value: DEFAULT_warnOnDisabledMember, type: "bool"])
    if (forceDefaults || (warnOnMemberSettings == null)) app.updateSetting("warnOnMemberSettings", [value: DEFAULT_warnOnMemberSettings, type: "bool"])
    if (forceDefaults || (highAccuracyOnPing == null)) app.updateSetting("highAccuracyOnPing", [value: DEFAULT_highAccuracyOnPing, type: "bool"])
    if (forceDefaults || (geocodeProvider == null)) app.updateSetting("geocodeProvider", [value: DEFAULT_geocodeProvider, type: "number"])
    if (forceDefaults || (geocodeFreeOnly == null)) app.updateSetting("geocodeFreeOnly", [value: DEFAULT_geocodeFreeOnly, type: "bool"])
    if (forceDefaults || (useCustomNotificationMessage == null)) app.updateSetting("useCustomNotificationMessage", [value: DEFAULT_useCustomNotificationMessage, type: "bool"])
    if (forceDefaults || (notificationMessage == null)) app.updateSetting("notificationMessage", [value: DEFAULT_notificationMessage, type: "string"])
    if (forceDefaults || (mapFreeOnly == null)) app.updateSetting("mapFreeOnly", [value: DEFAULT_mapFreeOnly, type: "bool"])
    if (forceDefaults || (manualDeleteBehavior == null)) app.updateSetting("manualDeleteBehavior", [value: DEFAULT_manualDeleteBehavior, type: "bool"])
    if (forceDefaults || (memberPinColor == null)) app.updateSetting("memberPinColor", [value: DEFAULT_MEMBER_PIN_COLOR, type: "string"])
    if (forceDefaults || (regionPinColor == null)) app.updateSetting("regionPinColor", [value: DEFAULT_REGION_PIN_COLOR, type: "string"])
    if (forceDefaults || (regionGlyphColor == null)) app.updateSetting("regionGlyphColor", [value: DEFAULT_REGION_GLYPH_COLOR, type: "string"])
    if (forceDefaults || (regionHomeGlyphColor == null)) app.updateSetting("regionHomeGlyphColor", [value: DEFAULT_REGION_HOME_GLYPH_COLOR, type: "string"])
    if (forceDefaults || (memberHistoryLength == null)) app.updateSetting("memberHistoryLength", [value: DEFAULT_memberHistoryLength, type: "number"])
    if (forceDefaults || (memberHistoryScale == null)) app.updateSetting("memberHistoryScale", [value: DEFAULT_memberHistoryScale, type: "decimal"])
    if (forceDefaults || (memberHistoryStroke == null)) app.updateSetting("memberHistoryStroke", [value: DEFAULT_memberHistoryStroke, type: "decimal"])
    if (forceDefaults || (memberHistoryRepeat == null)) app.updateSetting("memberHistoryRepeat", [value: DEFAULT_memberHistoryRepeat, type: "number"])
    if (forceDefaults || (displayAllMembersHistory == null)) app.updateSetting("displayAllMembersHistory", [value: DEFAULT_displayAllMembersHistory, type: "bool"])
    if (forceDefaults || (memberTripIdleMarkerTime == null)) app.updateSetting("memberTripIdleMarkerTime", [value: DEFAULT_memberTripIdleMarkerTime, type: "number"])
    if (forceDefaults || (memberMarkerBearingDifferenceDegrees == null)) app.updateSetting("memberMarkerBearingDifferenceDegrees", [value: DEFAULT_memberMarkerBearingDifferenceDegrees, type: "number"])
    if (forceDefaults || (removeMemberMarkersWithSameBearing == null)) app.updateSetting("removeMemberMarkersWithSameBearing", [value: DEFAULT_removeMemberMarkersWithSameBearing, type: "bool"])
    if (forceDefaults || (memberBoundsRadius == null)) app.updateSetting("memberBoundsRadius", [value: DEFAULT_memberBoundsRadius, type: "number"])

    if (forceDefaults || (state.memberBoundsRadius == null)) state.memberBoundsRadius = DEFAULT_memberBoundsRadius
    // if we are in imperial, convert the distances for displaying
    if (forceDefaults || (state.imperialUnitsHub != imperialUnits)) {
        state.imperialUnitsHub = imperialUnits
        // preload the settings field with the proper units
        app.updateSetting("memberBoundsRadius", [value: displayKmMiVal(state.memberBoundsRadius).toInteger(), type: "number"])
    }
    // convert back to metric if in imperial to send out to the map
    state.memberBoundsRadius       = convertToKilometers(memberBoundsRadius)
    // assign the default groups
    initializeGroupList(forceDefaults)
}

def initializeMobileLocation(forceDefaults) {
    if (forceDefaults) {
        app.removeSetting("monitoring")
    }
    if (forceDefaults || (monitoring == null)) app.updateSetting("monitoring", [value: DEFAULT_monitoring, type: "number"])
    if (forceDefaults || (ignoreInaccurateLocations == null)) app.updateSetting("ignoreInaccurateLocations", [value: DEFAULT_ignoreInaccurateLocations, type: "number"])
    if (forceDefaults || (ignoreStaleLocations == null)) app.updateSetting("ignoreStaleLocations", [value: DEFAULT_ignoreStaleLocations, type: "number"])
    if (forceDefaults || (ping == null)) app.updateSetting("ping", [value: DEFAULT_ping, type: "number"])
    if (forceDefaults || (pegLocatorFastestIntervalToInterval == null)) app.updateSetting("pegLocatorFastestIntervalToInterval", [value: DEFAULT_pegLocatorFastestIntervalToInterval, type: "bool"])
    if (forceDefaults || (locatorDisplacement == null)) app.updateSetting("locatorDisplacement", [value: DEFAULT_locatorDisplacement, type: "number"])
    if (forceDefaults || (locatorInterval == null)) app.updateSetting("locatorInterval", [value: DEFAULT_locatorInterval, type: "number"])
    if (forceDefaults || (moveModeLocatorInterval == null)) app.updateSetting("moveModeLocatorInterval", [value: DEFAULT_moveModeLocatorInterval, type: "number"])

    if (forceDefaults || (state.locatorDisplacement == null)) state.locatorDisplacement = DEFAULT_locatorDisplacement
    if (forceDefaults || (state.ignoreInaccurateLocations == null)) state.ignoreInaccurateLocations = DEFAULT_ignoreInaccurateLocations
    if (forceDefaults || (state.imperialUnits == null)) state.imperialUnits = DEFAULT_imperialUnits
    // if we are in imperial, convert the distances for displaying
    if (forceDefaults || (state.imperialUnits != imperialUnits)) {
        state.imperialUnits = imperialUnits
        // preload the settings field with the proper units
        app.updateSetting("locatorDisplacement", [value: displayMFtVal(state.locatorDisplacement), type: "number"])
        app.updateSetting("ignoreInaccurateLocations", [value: displayMFtVal(state.ignoreInaccurateLocations), type: "number"])
    }
    // convert back to metric if in imperial to send out to the phone
    state.locatorDisplacement             = convertToMeters(locatorDisplacement)
    state.ignoreInaccurateLocations       = convertToMeters(ignoreInaccurateLocations)
}

def initializeMobileDisplay(forceDefaults) {
    if (forceDefaults || (replaceTIDwithUsername == null)) app.updateSetting("replaceTIDwithUsername", [value: DEFAULT_replaceTIDwithUsername, type: "bool"])
    if (forceDefaults || (notificationEvents == null)) app.updateSetting("notificationEvents", [value: DEFAULT_notificationEvents, type: "bool"])
    if (forceDefaults || (extendedData == null)) app.updateSetting("extendedData", [value: DEFAULT_extendedData, type: "bool"])
    if (forceDefaults || (enableMapRotation == null)) app.updateSetting("enableMapRotation", [value: DEFAULT_enableMapRotation, type: "bool"])
    if (forceDefaults || (showRegionsOnMap == null)) app.updateSetting("showRegionsOnMap", [value: DEFAULT_showRegionsOnMap, type: "bool"])
    if (forceDefaults || (notificationLocation == null)) app.updateSetting("notificationLocation", [value: DEFAULT_notificationLocation, type: "bool"])
    if (forceDefaults || (notificationGeocoderErrors == null)) app.updateSetting("notificationGeocoderErrors", [value: DEFAULT_notificationGeocoderErrors, type: "bool"])
}

def initializeGroupList(forceDefaults) {
    // recreate the default groups
    if (forceDefaults || (state?.groups == null)) {
        state.groups = []
        for (id=0; id<DEFAULT_maxGroups; id++) {
            if (id == DEFAULT_globalGroupNumber) {
                groupName = DEFAULT_globalGroupName
            } else {
                groupName = DEFAULT_groupNames + id
            }
            state.groups << [ "name": "${groupName}", "id": "${id}" ]
        }
        if (state?.members) {
	        state.members.each { member->
                // assign the default group number to all members
                member.groups = []
                member.groups << "${DEFAULT_globalGroupNumber}"
            }
        }
    }
}

def updatePlusFollow() {
    // create the +follow with the time interval prefix
    plusFollow = IOS_PLUS_FOLLOW
    plusFollow.desc = "+${locatorInterval}follow"

    // if the +follow location changed
    deletePlace = state.places.find {it.desc[0] == plusFollow.desc[0]}
    if (deletePlace?.desc != plusFollow.desc) {
        logDescriptionText("Deleting place: ${deletePlace}")
        state.places.remove(deletePlace)
        // add the new one
        addPlace([ "name":"" ], plusFollow, false)
    }
}

def updateGetRegion() {
	state.members.each { member->
        // if we selected member(s) to retrieve their regions
        if (settings?.getMobileRegions.find {it==member.name}) {
            member.getRegions = true
        }
    }
    app.updateSetting("getMobileRegions",[value:"",type:"enum"])
}

def updated() {
    unschedule()
    unsubscribe()
    logDescriptionText("Updated")

    // cleanup up the recorder URL if necessary by removing the trailing /pub or /
    formatRecorderURL()

    // create the common child if it doesn't exist
    createCommonChild()

    // create a presence child device for each enabled member - we will need to manually removed children unless the app is uninstalled
    settings?.enabledMembers.each { enabledMember->
        member = state.members.find {it.name==enabledMember}
        // default to false
        syncSettings = false
        // create the child if it doesn't exist
        if ((member.id == null) || (getChildDevice(member.id) == null)) {
            createChild(member.name)
            // force the update to the new device
            syncSettings = true
        } else {
            // update the child name if the prefix changed
            updateChildName(member)
        }

        // if we selected member(s) to update settings
        if (settings?.syncMobileSettings.find {it==member.name}) {
            syncSettings = true
        }
        // if the configuration has changed, trigger the member update
        if (syncSettings) {
            member.updateLocation = true
            member.updateDisplay = true
            // only global members get waypoint updates
            if (memberInGlobalMemberGroup(member)) {
                member.updateWaypoints = true
            }
        }
        // remove excessive history events
        pruneMemberHistory(member)
    }
    // clear the settings flags to prevent the configurations from being forced to the display on each entry
    app.updateSetting("syncMobileSettings",[value:"",type:"enum"])

    // check to see if home was assigned
    checkForHome()

    // schedule the watchdog to automatically request a high accuracy location fix for stale locations
    locationFixWatchdog()

    // set the flag to indicate we installed the app
    state.installed = true
    // clear the debug logging if set
    if (debugOutput) {
        runIn(debugResetHours*3600, resetLogging)
    }

    /*
    0/2 0 0 * * * *
    XXX                  Every 2 seconds
        X                during minute zero
          X              during hour zero
            X            any day of the month
              X          every month
                X        every day of the week
                  X      every year
    */
    // get the time zone offset so we can schedule at midnight GMT
    def timeZoneOffset = (location.timeZone.rawOffset) / (3600 * 1000)
    if (timeZoneOffset < 0) {
        timeZoneOffset = 24 + timeZoneOffset
    }
    schedule("0 0 $timeZoneOffset * * ? *", dailyScheduler)
    // refresh the maps nightly
    schedule("0 0 0 * * ? *", nightlyMaintenance)
    nightlyMaintenance()
    removePlaces()
}

def refresh() {
}

def pruneMemberHistory(member) {
    if (memberHistoryLength == 0) {
        member.history = []
    } else {
        // first remove the oldest of the history buffer is full
        while (member?.history?.size() > (memberHistoryLength != null ? memberHistoryLength : DEFAULT_memberHistoryLength)) {
            member.history.remove(0)
        }
        try {
            // calculate the trip numbers and populate the history
            tripNumber = 1;
            for (i=(member.history.size-1); i>=0; i--) {
                // if we have no trip started yet
                if ((member.history.mkr[i] == memberBeginMarker) && (i == (member.history.size-1))) {
                    tripNumber = 0;
                }
                // increment the trip marker
                if (member.history.mkr[i] == memberEndMarker) {
                    tripNumber++;
                }
                memberHistory = member.history[i]
                memberHistory["tp"] = tripNumber;
            }
        } catch (e) {
            // do nothing -- once we have configured and received enough history points, this will succeed
        }
    }
}

def childGetWarnOnNonOptimalSettings() {
    // return with the log setting
    return (warnOnMemberSettings)
}

def getImageURL(memberName) {
    if (imageCards) {
        return ("http://${location.hubs[0].getDataValue("localIP")}/local/${memberName}.jpg")
    } else {
        return ("")
    }
}

def getEmbeddedImage(memberName) {
    def thumbnail = ""
    if (imageCards) {
        try {
            thumbnail = "data:image/png;base64," + downloadHubFile("${memberName}.jpg").encodeBase64().toString()
        } catch (e) {
            // use the default blank if no thumbnail was found
        }
    }
    return (thumbnail)
}

def getRecorderURL() {
    // return with recorder URL
    return (settings?.recorderURL)
}

def formatRecorderURL() {
    // cleanup up the recorder URL if necessary by removing the trailing /pub or /
    properURL = recorderURL?.trim()?.minus(RECORDER_PUBLISH_FOLDER)
    if (recorderURL != properURL) {
        app.updateSetting("recorderURL",[value: properURL, type: "string"])
    }
}

def generateStaleNotification() {
    // collect the names of the members that are stale
    staleMembers = state?.members.findAll{it.staleReport == true}.collect{member -> member.name}
    // send notification to mobile if selected
    if (staleMembers && notificationStaleList) {
        def date = new Date()
        def dateFormat = "hh:mm a yyyy-MM-dd"
        SimpleDateFormat newDate = new SimpleDateFormat(dateFormat)
        messageToSend = "Members ${staleMembers} have stale locations ${newDate.format(date)}"
        notificationStaleList.each { val ->
            val.deviceNotification(messageToSend)
        }
    }
}

def generateTransitionNotification(memberName, transitionEvent, transitionRegion, transitionTime) {
    member = state.members.find {it.name==memberName}
    place = state.places.find {it.desc==transitionRegion}
    if (transitionEvent == "arrived at") {
        notificationDevices = member?.enterDevices
        notificationDeviceRegion = member?.enterRegions.find {it.toInteger()==place.tst}
    } else {
        notificationDevices = member?.leaveDevices
        notificationDeviceRegion = member?.leaveRegions.find {it.toInteger()==place.tst}
    }

    if (useCustomNotificationMessage) {
        // parse the notification message
        messageToSend = notificationMessage
    } else {
        messageToSend = DEFAULT_notificationMessage
    }
    // parse the notification message
    messageToSend = messageToSend.replace("NAME",   "${memberName}")
    messageToSend = messageToSend.replace("EVENT",  "${transitionEvent}")
    messageToSend = messageToSend.replace("REGION", "${transitionRegion}")
    messageToSend = messageToSend.replace("TIME",   "${transitionTime}")

    // send notification to mobile if selected
    if (notificationDevices && notificationDeviceRegion) {
        notificationList.each { val ->
            if (notificationDevices.find {it==val.displayName}) {
                val.deviceNotification(messageToSend)
            }
        }
    }
}

def checkForHome() {
    if (!homePlace) {
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
        if (member.staleFix) {
            member.requestLocation = true
            logDebug("${member.name}'s position is stale.  Requesting a high accuracy location update.")
        }
    }
    // sort by last report time
    state.members?.sort { it.lastReportTime }
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
        tableData += '<th>Update Configuration</th>'
        tableData += '<th>Get Regions</th>'
        tableData += '<th>Request Location</th>'
        tableData += '</tr>'

        // update each member with their last report times
        checkStaleMembers()
        // loop through all the members
        state.members.each { member->
            // check if member is enabled
            memberEnabled = settings?.enabledMembers.find {it==member.name}
            deviceWrapper = getChildDevice(member.id)
            memberName = "<a href='" + "http://${location.hubs[0].getDataValue('localIP')}/device/edit/" + deviceWrapper?.getId() + "' target='_blank'>" + member.name + "</a>"

            tableData += '<tr>'
            tableData += '<td>' + (memberEnabled ? memberName : '<s>' + memberName + '</s>') + '</td>'
            tableData += (memberEnabled ? ((member.staleReport ? '<td style="color:#ff0000">' + member.lastReportDate + ' (' + member.numberHoursReport + ' hrs ago)' : '<td>' + member.lastReportDate) + '</td>') : '<td style="color:#b3b3b3"><s>' + member.lastReportDate + '</s></td>')
            tableData += (memberEnabled ? ((member.staleFix ? '<td style="color:#ff0000">' + member.lastFixDate + ' (' + member.numberHoursFix + ' hrs ago)' : '<td>' + member.lastFixDate) + '</td>') : '<td style="color:#b3b3b3"><s>' + member.lastFixDate + '</s></td>')
            tableData += (memberEnabled ? (member.updateWaypoints ? '<td style="color:#ff9900">Pending' : '<td>No') + '</td>' : '<td style="color:#b3b3b3"><s>--</s></td>')
            tableData += (memberEnabled ? ((member.updateLocation | member.updateDisplay) ? '<td style="color:#ff9900">Pending' : '<td>No') + '</td>' : '<td style="color:#b3b3b3"><s>--</s></td>')
            tableData += (memberEnabled ? (member.getRegions ? '<td style="color:#ff9900">Pending' : '<td>No') + '</td>' : '<td style="color:#b3b3b3"><s>--</s></td>')
            tableData += (memberEnabled ? (member.requestLocation ? '<td style="color:#ff9900">Pending' : '<td>No') + '</td>' : (member?.deactivate ? '<td style="color:#ff0000">' + (member?.deactivate == 1 ? 'Deactivating' : 'Deactivated') +'</td>' : '<td style="color:#b3b3b3"><s>--</s></td>'))
            tableData += '</tr>'
            // display members in non-optimal configurations
            tableData += '<tr style="color:#ff0000">'
            tableData += '<td></td>'
            tableData += '<td>'
            tableData += ((member?.cmd == 0) ? '* OwnTracks app "remote configuration" disabled<br>' : '')
            tableData += ((member?.bo == 1) ? '* Battery usage is set to "optimized" or "restricted"<br>' : '')
            tableData += ((member?.hib == 1) ? '* "Pause app activity if unused" is enabled<br>' : '')
            tableData += (((member?.loc != null) && (member?.loc < 0)) ? '* Location permission is not set to "Allow all the time" and "Use precise location"<br>' : '')
            tableData += ((member?.ps == 1) ? '* Phone in battery saver mode' : '')
            tableData += '</td>'
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
   return(topic.split("/"))
}

def nightlyMaintenance() {
    // runs at midnight to refresh the map contents
    createRecorderFriendsLocationTile()
    createGoogleFriendsLocationTile()
    // loop through all the enabled members
    settings?.enabledMembers.each { enabledMember->
        member = state.members.find {it.name==enabledMember}
        deviceWrapper = getChildDevice(member.id)
        deviceWrapper?.generatePastLocationsTile()
    }
    // check if we need to notify on stale locations
    generateStaleNotification()
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
        logError("Username: '${sourceName}' / Device ID: '${sourceDeviceID}' not configured in the OwnTracks app - Deactivating unknown user.  Ensure the 'Username' and 'Device ID' are set on the OwnTracks mobile app.")
        result = sendDeactivateUpdate([ "name":"${sourceDeviceID}" ])
    } else {
        // strip the [] around these values
        sourceName = sourceName.substring(1, (sourceName.length()-1))
        sourceDeviceID = sourceDeviceID.substring(1, (sourceDeviceID.length()-1))
        // check if this a message from the service device.  If not, check for a matching member
        if (sourceName == COMMON_CHILDNAME) {
            findMember = [ name:COMMON_CHILDNAME, deviceID:COMMON_CHILDNAME, id:getCommonChildDNI() ]
        } else {
            findMember = state.members.find {it.name==sourceName}
        }

        data = parseJson(request.body)
        logDebug("Received update' from user: '$sourceName', deviceID: '$sourceDeviceID', data: $data")

        if (!findMember?.id) {
            // add the new user to the list if they don't exist yet.  We will use the current time since not all incoming packets have a timestamp
            if (findMember == null) {
                state.members << [ name:sourceName, deviceID:sourceDeviceID, id:null, timeStamp:(now()/1000).toInteger(), updateWaypoints:false, updateLocation:false, updateDisplay:false, dynamicLocaterAccuracy:false, getRegions:false, requestLocation:false ]
            }
            logWarn("User: '${sourceName}' not configured.  To enable this member, open the Hubitat OwnTracks app, select '${sourceName}' in 'Select family member(s) to monitor' box and then click 'Done'.")
        } else {
            // only process events from enabled members, or the service member
            if ((settings?.enabledMembers.find {it==sourceName}) || (sourceName == COMMON_CHILDNAME)) {
                // Pass the location to a secondary hub if configured
                if (secondaryHubURL && enableSecondaryHub) {
                    def postParams = [ uri: secondaryHubURL?.trim(), requestContentType: 'application/json', contentType: 'application/json', headers: parsePostHeaders(request.headers), body : (new JsonBuilder(data)).toPrettyString() ]
                    asynchttpPost("httpCallbackMethod", postParams)
                }
                // update the device ID should it have changed
                findMember.deviceID = sourceDeviceID
                result = parseMessage(request.headers, data, findMember);
            } else {
                if (warnOnDisabledMember) {
                    logWarn("User: '${sourceName}' not enabled.  To enable this member, open the Hubitat OwnTracks app, select '${sourceName}' in 'Select family member(s) to monitor' box and then click 'Done'.")
                    // if the member is flagged for deactivation
                    if (findMember?.deactivate == 1) {
                        result = sendDeactivateUpdate(findMember)
                    }
                } else {
                    logDebug("User: '${sourceName}' not enabled.  To enable this member, open the Hubitat OwnTracks app, select '${sourceName}' in 'Select family member(s) to monitor' box and then click 'Done'.")
                }
            }
        }
    }

    // app requires a non-empty JSON response, or it will display HTTP 500
    payload = new JsonBuilder(result).toPrettyString()
    return render(contentType: "text/html", data: payload, status: 200)
}

def parseMessage(headers, data, member) {
    // default to an empty payload
    payload = []

    switch (data._type) {
        case "location":
        case "transition":
            // store the last report time for the Google friends map
            state.lastReportTime = new SimpleDateFormat("E h:mm a yyyy-MM-dd").format(new Date())
            updateMemberAttributes(headers, data, member)
            // flag the data as private if necessary, but let the raw message pass to the secondary hub to be filtered
            data.private = ((settings?.privateMembers.find {it==member.name}) ? true : false)

            // log the elapsed distance and time
            logDistanceTraveledAndElapsedTime(member, data)
            // calculate how many minutes the incoming location message is from current time
            deltaTst = (((member.lastReportTime / 1000) - member?.timeStamp) / 60).toInteger()
            // send push event to driver if the incoming location isn't stale
            if (deltaTst < memberMaximumLocationAgeMinutes) {
                updateDevicePresence(member, data)
            } else {
                logDescriptionText("Location update for user ${member.name} is ${deltaTst} minutes old.  Skipping presence update.")
            }
            // return with the rest of the users positions and waypoints if pending
            payload = sendUpdate(member, data)
            // if the country code was not defined, replace with with hub timezone country
            if (!data.cc) { data.cc = location.getTimeZone().getID().substring(0, 2).toUpperCase() }
            // if we have the OwnTracks recorder configured, and the timestamp is valid, and the user is not marked as private, pass the location data to it
            if (recorderURL && enableRecorder && !data.private && memberInGlobalMemberGroup(member)) {
                def postParams = [ uri: recorderURL + RECORDER_PUBLISH_FOLDER, requestContentType: 'application/json', contentType: 'application/json', headers: parsePostHeaders(headers), body : (new JsonBuilder(data)).toPrettyString() ]
                asynchttpPost("httpCallbackMethod", postParams)
            }
            break
        case "waypoint":
            // append/update to the places list for global members only
            if (memberInGlobalMemberGroup(member)) {
                addPlace(member, data, true)
            } else {
                logDescriptionText("User ${member.name} is not in the default group.  Skipping waypoint update.")
            }
        break
        case "waypoints":
            // update the places list for global members only
            if (memberInGlobalMemberGroup(member)) {
                updatePlaces(member, data)
            } else {
                logDescriptionText("User ${member.name} is not in the default group.  Skipping waypoint update.")
            }
        break
        case "cmd":
            // parse the action
            switch (data.action) {
                case "setWaypoints":
                    // update the waypoint list from the payload
                    updatePlaces(member, data.waypoints)
                break
                case "waypoints":
                    // send waypoints
                    payload = sendWaypoints(member)
                break
                case "restart":
                case "reportLocation":
                case "setConfiguration":
                case "dump":
                default:
                    // do nothing
                break
            }
        break
        case "status":
            // update the member status from the payload
            updateStatus(member, data)
        break
        case "card":
        break
        default:
            logWarn("Unhandled message type: ${data._type}")
        break
    }

    return (payload)
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
        responseData = response?.getJson()
        responseHeaders = response?.getHeaders()
        if (responseData) {
            // parse the response
            try {
                // for map of maps
                for (i=0; i<responseData.size(); i++) {
                    // ignore the returning member positions
                    if (responseData[i]._type != "location") {
                        parseMessage(responseHeaders, responseData[i], [ name:COMMON_CHILDNAME ])
                    }
                }
            } catch(e) {
                // single map
                parseMessage(responseHeaders, responseData, [ name:COMMON_CHILDNAME ])
            }
        }
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

def calcMemberVelocity(member, data) {
    try {
        travelDistance = haversine(member.latitude.toDouble(), member.longitude.toDouble(),data.lat.toDouble(), data.lon.toDouble())
        // calcuate the member odometer
        member.odo = (member.odo ? (member.odo + travelDistance).round(1) : travelDistance.round(1))
        // if we received a speed -- wifi location updates will return with 0 speed, so ignore those and calculated based on the distance moved
        if (data?.vel > 0) {
            member.speed = data.vel
        } else {
            // TODO - if we get a noisy location that jumps, we can get an artifically inflated calculated speed
            timeDifference = data.tst - member.timeStamp
            // only calculate speed on specific location types, and if the time difference between locations is large enough
            if (validLocationType(data.t) && (timeDifference >= memberVelocityMinimumTimeDifference)) {
                // calculate the speed between the new and previous location point
                member.speed = (travelDistance / ((timeDifference) / 3600)).toInteger()
            } else {
                // prevent high calculated speed between multiple manual points returned in succession
                member.speed = 0
            }
            data.vel = member.speed
        }
        // if we received a bearing
        if (data?.cog) {
            member.bearing = data.cog
        } else {
            // calculate the bearing between the new and previous location point
            member.bearing = angleFromCoordinate(member.latitude.toDouble(), member.longitude.toDouble(),data.lat.toDouble(), data.lon.toDouble()).toInteger()
            data.cog = member.bearing
        }
    } catch (e) {
        member.speed = 0
        member.bearing = 0
    }
}

def getCompassDifference(bearing1, bearing2) {
    difference = Math.abs(bearing1 - bearing2)
    if (difference > 180) {
        return (360 - difference)
    } else {
        return (difference)
    }
}

def removeHistoryPoint(member) {
    // calculate the angle between the three points
    d12 = haversine(member.latitude.toDouble(), member.longitude.toDouble(),member.history[member.history.size-1].lat.toDouble(), member.history[member.history.size-1].lng.toDouble())
    d13 = haversine(member.latitude.toDouble(), member.longitude.toDouble(),member.history[member.history.size-2].lat.toDouble(), member.history[member.history.size-2].lng.toDouble())
    d23 = haversine(member.history[member.history.size-1].lat.toDouble(), member.history[member.history.size-1].lng.toDouble(),member.history[member.history.size-2].lat.toDouble(), member.history[member.history.size-2].lng.toDouble())

    // check if the angle deviation is within range
    if ((180 - Math.toDegrees(Math.acos(((d12*d12) + (d23*d23) - (d13*d13)) / (2*d12*d23)))) <= memberMarkerBearingDifferenceDegrees) {
        return (true)
    } else {
        return (false)
    }
}

def getHistoryMarker(member, data) {
    // default to the "middle" marker
    marker = memberMiddleMarker
    try {
        historyLength = member.history.size - 1
        removeMarker = false
        // check if enough time has elapsed between points to denote a trip end
        if ((member.timeStamp - member.history.tst[historyLength]) > (60 * memberTripIdleMarkerTime.toInteger())) {
            // check if the last marker was a begin marker, if so remove it so it can be replaced with a new begin marker
            if (member.history.mkr[historyLength] == memberBeginMarker) {
                removeMarker = true
            } else {
                // change the last marker to "end"
                historyPoint = member.history[historyLength]
                historyPoint.mkr = memberEndMarker
            }
            // return "begin" for the next marker
            marker = memberBeginMarker
            // reset the odometer
            member.odo = 0
        } else {
            // TODO - may need to add a duration check between these samples in case stop and go traffic erases them
            // if the last two history points are below minimum velocity
            if ((member.speed <= memberHistoryMinimumSpeed) && (member.history.spd[historyLength] <= memberHistoryMinimumSpeed)) {
                removeMarker = true
            }
            // if we are travelling in roughly the same direction - check the last two points so that we do not remove a marker during a direction transition
            if (removeMemberMarkersWithSameBearing && removeHistoryPoint(member)) {
                removeMarker = true
            }
        }

        // remove the last history point so it can be replaced with the new one
        if (removeMarker) {
            // replace the marker with the same type as deleted
            marker = member.history.mkr[historyLength]
            member.history.remove(historyLength)
        }
    } catch (e) {
    }

    return(marker)
}

def updateMemberAttributes(headers, data, member) {
    // round to 6-decimal places
    data.lat = data?.lat?.toDouble()?.round(6)
    data.lon = data?.lon?.toDouble()?.round(6)
    // replace the tracker ID with the member name.  NOTE: if the TID is undefined, it will be the last 2-characters of the Device ID
    if (replaceTIDwithUsername) {
        data.tid = member.name
    }

    // pre-seed the member lat/lon for the first update address lookup
    if (member.latitude == null)  member.latitude = data?.lat
    if (member.longitude == null) member.longitude = data?.lon
    // do a reverse lookup for the address if it doesn't exist, and we have an API enabled
    updateAddress(member, data)
    // add the street address and regions, if they exist
    addStreetAddressAndRegions(data)
    // calculate the speed and odometer
    calcMemberVelocity(member, data)

    // save the position and timestamp so we can push to other users
    member.appVersion               = headers.'User-agent'.toString()
    member.lastReportTime           = now()
    member.latitude                 = data?.lat
    member.longitude                = data?.lon
    member.timeStamp                = data?.tst
    member.accuracy                 = data?.acc

    // these are not present in transition messages
    if (data?.tid  != null)         member.trackerID = data.tid
    if (data?.batt != null)         member.battery   = data.batt
    if (data?.alt  != null)         member.altitude  = data.alt
    if (data?.bs   != null)         member.bs        = data.bs
    if (data?.conn != null)         member.conn      = data.conn

    // save the history
    if (member?.history == null) {
        member.history = []
    }

    // only save history on valid location types (ignore region and manual types)
    if (validLocationType(data.t)) {
        // first create the new member location so that the getHistoryMarker can clean up repeating events as necessary
        def memberLocation = [ "lat": member.latitude, "lng": member.longitude, "acc": member.accuracy, "cog": member.bearing, "spd": member.speed, "odo": member.odo, "tst": member.timeStamp, "loc": data.streetAddress, "mkr": getHistoryMarker(member, data) ]
        try {
            // if the history buffer is full
            if (member.history.size == memberHistoryLength.toInteger()) {
                // first check if the second location is not a middle marker, if so then locations 1 and 3 are begin/end of the oldest trip
                if (member.history.mkr[1] == memberMiddleMarker) {
                    member.history.remove(1)
                } else {
                    // remove that last trip
                    member.history.remove(0)
                    member.history.remove(0)
                }
            }
        } catch(e) {
            // do nothing -- once we have configured and received enough history points, this will succeed
        }
        // add to the end of the list
        member.history << memberLocation
        // remove the oldest of the history buffer if over full
        pruneMemberHistory(member)
    } else {
        logDebug("Skipping history point save for trigger source '${(data.t ? TRIGGER_TYPE[data.t] : "Location")}', member: ${member.name}")
    }
}

def updateDevicePresence(member, data) {
    // update the presence information for the member
    try {
        // find the appropriate child device based on app id and the device network id
        def deviceWrapper = getChildDevice(member.id)
        logDebug("Updating '${(data.event ? "Event $data.event" : (data.t ? TRIGGER_TYPE[data.t] : "Location"))}' presence for member $deviceWrapper")
        // check if the user defined a home place
        if (homePlace) {
            // append the distance from home to the data packet
            data.currentDistanceFromHome = getDistanceFromHome(data)
            // check if the member is within our home geofence
            memberHubHome = (data.currentDistanceFromHome <= ((getHomeRegion().rad.toDouble()) / 1000))
            // or the mobile is reporting the member is home
            memberMobileHome = (data?.inregions.find {it==getHomeRegion().desc} || ((data?.desc == getHomeRegion().desc) && (data?.event == 'enter')))
            // needed for safe migration from 1.7.11
            if (wifiPresenceKeepRadius == null) app.updateSetting("wifiPresenceKeepRadius", [value: DEFAULT_wifiPresenceKeepRadius, type: "number"])
            // or connected to a listed SSID and within the next geofence
            data.memberWiFiHome = (data.currentDistanceFromHome < (wifiPresenceKeepRadius?.toDouble() / 1000)) && isSSIDMatch(homeSSID, deviceWrapper)

            // if either the hub or the mobile reports it is home, then make the member present
            if (memberHubHome || memberMobileHome || data.memberWiFiHome) {
                data.memberAtHome = true
                // if the home name isn't present, at it to the regions
                addRegionToInregions(getHomeRegion().desc, data)
            } else {
                data.memberAtHome = false
            }
        } else {
            data.currentDistanceFromHome = 0.0
            logWarn("No 'Home' location has been defined.  Create a 'Home' region to enable presence detection.")
        }
        // update the child information
        deviceWrapper.generatePresenceEvent(member, getHomeRegion().desc, data)
    } catch(e) {
        logError("updateDevicePresence: Exception for member: ${member.name}  $e")
    }
}

def addRegionToInregions(place, data) {
    // if there was no defined regions, create a blank list
    if (!data.inregions) {
        data.inregions = []
    }
    // if the region isn't present, then add it
    if (!data.inregions.find {it==place}) {
        data.inregions << place
    }
}

def addStreetAddressAndRegions(data) {
    try {
        addressList = data.address?.split(',')
        // trim whitespace to allow the parser to work
        addressList[0] = addressList[0]?.trim()
        addressList[1] = addressList[1]?.trim()
        // The address will be:
        // place, street address
        // street address, city
        // lat, lon
        // if the first digit of the first entry is not a number, but the second is, then we were returned a place, street adress
        if ( !((addressList[0])[0])?.isNumber() && (((addressList[1])[0])?.isNumber() || (addressList.size() > 4)) ) {
            // save the place to the region list if we don't already have a region defined
            if (!data.inregions) addRegionToInregions(addressList[0], data)
            data.streetAddress = addressList[1]
            // remove the place from the address
            data.address = data.address.substring(data.address.indexOf(",") + 1)?.trim()
        } else {
            // if the first entry is not a number, then we have a street address
            if (!addressList[0]?.isNumber()) {
                data.streetAddress = addressList[0]
            } else {
                // pass through since it is a lat,lon or a format we don't know how to parse
                data.streetAddress = data.address
            }
        }
    } catch (e) {
        // pass the address through
        data.streetAddress = data.address
    }
}

def checkRegionConfiguration(member, data) {
    // if we configured the high accuracy region and a home has been defined
    if (regionHighAccuracyRadius && homePlace) {
        def closestWaypointRadius = 0
        def closestWaypointDistance = -1

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
        // check if the member is inside a region and we are in high power mode, and we want to reduce power inside regions
        if (highPowerMode && lowPowerModeInRegion) {
            currentlyInRegion = (closestWaypointDistance <= closestWaypointRadius)
        } else {
            currentlyInRegion = false
        }

        // check if we need to apply this to the home region only then we will overwrite the all regions
        if (regionHighAccuracyRadiusHomeOnly) {
            // only switch to faster reporting when near home
            closestWaypointDistance = (getDistanceFromHome(data)*1000).toDouble()
            closestWaypointRadius = getHomeRegion()?.rad.toDouble()
        }

        // catch the exception if no regions have been defined
        if ((closestWaypointDistance == null) || (closestWaypointRadius == null)) {
            closestWaypointRadius = 0
            closestWaypointDistance = -1
            logWarn("Home region is undefined.  Run setup to configure the 'Home' location")
        }

        // check if we are outside our region radius, and within our greater than the radius + regionHighAccuracyRadius
        return (createConfiguration(member, currentlyInRegion, ((closestWaypointDistance > closestWaypointRadius) && (closestWaypointDistance < (closestWaypointRadius + regionHighAccuracyRadius.toDouble())))))
    }
}

def createConfiguration(member, currentlyInRegion, useDynamicLocaterAccuracy) {
    def updateConfiguration = false

    // check if we need to force a high accuracy update
    if (useDynamicLocaterAccuracy) {
        // switch to locatorPriority=high power and pegLocatorFastestIntervalToInterval=false (dynamic interval)
        configurationList = [ "_type": "configuration",
                             "pegLocatorFastestIntervalToInterval": DYNAMIC_INTERVALS.pegLocatorFastestIntervalToInterval,
                             "locatorPriority": DYNAMIC_INTERVALS.locatorPriority,
                            ]
    } else {
        // switch to settings.  Recommended locatorPriority=balanced power and pegLocatorFastestIntervalToInterval=true (fixed interval)
        // if in a region, use balanced power to prevent location jitter
        configurationList = [ "_type": "configuration",
                             "pegLocatorFastestIntervalToInterval": pegLocatorFastestIntervalToInterval,
                             "locatorPriority": getLocatorPriority(currentlyInRegion),
                            ]
    }
    if (member?.dynamicLocaterAccuracy != useDynamicLocaterAccuracy) {
        // assign the new state
        member.dynamicLocaterAccuracy = useDynamicLocaterAccuracy
        updateConfiguration = true
    }
    if (member?.currentlyInRegion != currentlyInRegion) {
        // assign the new state
        member.currentlyInRegion = currentlyInRegion
        updateConfiguration = true
    }
    if (updateConfiguration) {
        // return with the dynamic configuration
        return( [ "_type":"cmd","action":"setConfiguration", "configuration": configurationList ] )
    } else {
        // return nothing
        return
    }
}

def sendClearURLConfiguration(currentMember) {
    logDescriptionText("Clear URL for user ${currentMember.name}")
    // remove the URL to prevent the device from calling home
    configurationList = [ "_type": "configuration", "url": "" ]

    return( [ "_type":"cmd","action":"setConfiguration", "configuration": configurationList ] )
}

private def getDistanceFromHome(data) {
    // return distance in kilometers, rounded to 3 decimal places (meters)
    distance = 0.0
    try {
        distance = haversine(data.lat.toDouble(), data.lon.toDouble(), getHomeRegion()?.lat.toDouble(), getHomeRegion()?.lon.toDouble()).round(3)
    } catch (e) {
        logError("Unable to get distance from home.  Confirm a 'Home' region is assigned. Error reported: $e")
    }

    return (distance)
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
        if (state.members.find {it.name==enabledMember}?.updateWaypoints) {
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

    // remove regions from each member's notification list if they no longer exist
    state?.members.each { member->
        if (member.enterRegions) {
            member.enterRegions -= removeDeletedPlaces(member.enterRegions)
        }
        if (member.leaveRegions) {
            member.leaveRegions -= removeDeletedPlaces(member.leaveRegions)
        }
    }
}

def removeDeletedPlaces(regionList) {
    removeList = []
    regionList.each { entry ->
        if (state.places.find {it.tst==entry.toInteger()} == null) {
            // add to the list to be removed
            removeList += entry
        }
    }

    return(removeList)
}

def updatePlaces(findMember, data) {
    // only add places from non-private members
    if (!(settings?.privateMembers.find {it==findMember.name})) {
        logDescriptionText("Updating places")

        // loop through all the waypoints
        data.waypoints.each { waypoint->
            addPlace(findMember, waypoint, true)
        }
        logDebug("Updating places: ${state.places}")
    } else {
        logDebug("Ignoring waypoints due to private member.")
    }
}

def addPlace(findMember, data, verboseAdd) {
    // only add places from non-private members
    if (!(settings?.privateMembers.find {it==findMember.name})) {
        // create a new map removing the MQTT topic
        def newPlace = [ "_type": "${data._type}", "desc": "${data.desc}", "lat": data.lat.toDouble().round(6), "lon": data.lon.toDouble().round(6), "rad": data.rad, "tst": data.tst ]

        // check if we have an existing place with the same timestamp
        place = state.places.find {it.tst==newPlace.tst}

        // no changes to existing place, or a member is returing the +follow region
        if ((place == newPlace) || (findMember.name && (data?.desc[0] == "+"))) {
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
            setUpdateFlag(findMember, "updateWaypoints", true, true)
        }
    } else {
        logDebug("Ignoring waypoint due to private member.")
    }
}

def updateStatus(findMember, data) {
    findMember.wifi = data?.android?.wifi
    findMember.hib  = data?.android?.hib
    findMember.ps   = data?.android?.ps
    findMember.bo   = data?.android?.bo
    findMember.loc  = data?.android?.loc
    findMember.cmd  = (data?.android?.cmd != null ? data?.android?.cmd : 1)
    if (findMember.cmd == 0) {
        logWarn("'Remote Configuration' not enabled.  Open the Owntracks app for ${findMember.name} and select 'Preferences -> Advanced-> Remote configuration'")
    }

    /*
    log.debug (data?.iOS?.deviceSystemName)
    log.debug (data?.iOS?.deviceUserInterfaceIdiom)
    log.debug (data?.iOS?.localeUsesMetricSystem)
    log.debug (data?.iOS?.backgroundRefreshStatus)
    log.debug (data?.iOS?.deviceSystemVersion)
    log.debug (data?.iOS?.altimeterAuthorizationStatus)
    log.debug (data?.iOS?.deviceModel)
    log.debug (data?.iOS?.locale)
    log.debug (data?.iOS?.version)
    log.debug (data?.iOS?.altimeterIsRelativeAltitudeAvailable)
    log.debug (data?.iOS?.locationManagerAuthorizationStatus)
    log.debug (data?.iOS?.deviceIdentifierForVendor)
    */
    logDebug("Updating status: ${findMember.name}")
}

private def setUpdateFlag(currentMember, newSetting, newValue, globalOnly) {
    // loop through all the enabled members
    settings?.enabledMembers.each { enabledMember->
        member = state.members.find {it.name==enabledMember}
        // filter out group members if the request is for global members only
        if ((globalOnly && memberInGlobalMemberGroup(member)) || !globalOnly) {
            // don't set the flag for the member that triggered the update
            if (currentMember.name != member.name) {
                member."$newSetting" = newValue
                logDebug("${newSetting} for user ${member.name}: ${newValue}")
            }
        }
    }
}

private def sendReportLocationRequest(currentMember) {
    logDescriptionText("Request location for user ${currentMember.name}")
    // Forces the device to get a GPS fix for higher accuracy

    return ([ "_type":"cmd","action":"reportLocation" ])
}

private def sendReportWaypointsRequest(currentMember) {
    logDescriptionText("Request waypoints for user ${currentMember.name}")
    // Requests the waypoints list from the device

    return ([ "_type":"cmd","action":"waypoints" ])
}

private def sendClearWaypointsRequest(currentMember) {
    logDescriptionText("Clear waypoints for user ${currentMember.name}")
    // Clears the waypoints list from the device

    return ([ "_type":"cmd","action":"clearWaypoints" ])
}

private def sendReportStatusRequest(currentMember) {
    logDescriptionText("Request status for user ${currentMember.name}")
    // Requests the status from the device

    return ([ "_type":"cmd","action":"status" ])
}

private def sendGetConfigurationRequest(currentMember) {
    logDescriptionText("Request configuration for user ${currentMember.name}")
    // Requests the configuration from the device

    return ([ "_type":"cmd","action":"dump" ])
}

private def sendMemberPositions(currentMember, data) {
    def positions = []
    // check if a member has been configured to not see other member locations
    if (!settings?.privateMembers.find {it==currentMember.name}) {
        // loop through all the enabled group members
        groupMembers = getMatchingGroupMembersForMember(currentMember)
        groupMembers?.each { member->
            // Don't send the member's location back to them.  NOTE: iOS users will make a duplicate of themselves, and Android has a lag between the app displayed thumbnail and the current phone location
            // Don't send locations if a member has been configured to not see other member location
            if ((currentMember != member) && !(settings?.privateMembers.find {it==member.name}) ) {
                // populating the tracker ID field with a name allows the name to be displayed in the Friends list and map bubbles and load the OwnTrack support parameters
                def memberLocation = [ "_type": "location", "t": "u", "lat": member.latitude, "lon": member.longitude, "tst": member.timeStamp ]

                // check if fields are valid before adding
                if (member.trackerID != null)   memberLocation["tid"]  = member.trackerID
                if (member.battery != null)     memberLocation["batt"] = member.battery
                if (member.accuracy != null)    memberLocation["acc"]  = member.accuracy
                if (member.altitude != null)    memberLocation["alt"]  = member.altitude
                if (member.speed != null)       memberLocation["vel"]  = member.speed
                if (member.bs != null)          memberLocation["bs"]   = member.bs

                positions << memberLocation

                // send the image cards for the user if there is one, and we aren't sending commands, -- only send on the ping or the manual update to minimize data traffic
                if (validPositionType(data.t)) {
                    card = getMemberCard(member)
                    if (!sendCmdToMember(currentMember) && card) {
                        positions << card
                    }
                }
            }
        }
    } else {
        logDebug("${currentMember.name} is configured to not receive member updates.")
    }

    return (positions)
}

private def getRandomID() {
    return((Math.random() * 0xFFFFFFFF).toInteger())
}

private def getMemberCard(member) {
    def card = []

    // send the image cards for the user if enabled
    if (imageCards) {
        try{
            // append each enabled user's card with encoded image
            card = [ "_type": "card", "name": "${member.name}", "face": "${downloadHubFile("${member.name}.jpg").encodeBase64().toString()}", "tid": "${member.trackerID}" ]
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
    // If the member isn't public, then only send the home place
    if (settings?.privateMembers.find {it==currentMember.name}) {
        return ([ "_type":"cmd","action":"setWaypoints", "waypoints": [ "_type":"waypoints", "waypoints":getHomeRegion() ] ])
    } else {
        // if the member is an android user, then do not send the +follow region
        return ([ "_type":"cmd","action":"setWaypoints", "waypoints": [ "_type":"waypoints", "waypoints":(currentMember?.appVersion?.indexOf(ANDROID_USER_AGENT,0) >= 0 ? getNonFollowRegions(COLLECT_PLACES["map"]) : state.places) ] ])
    }
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
                                "allowRemoteLocation" :                 true,                                // Allow remote location command
                                "reverseGeocodeProvider" :              "Device",                            // Reverse Geocode provider -- use device (Google for Android)
                                "allowRemoteLocation" :                 true,                                // required for 'reportLocation' to be processed
                                "connectionTimeoutSeconds" :            30,
                                "debugLog" :                            false,
                                "dontReuseHttpClient" :                 false,
                                "experimentalFeatures" :                [],
                                "fusedRegionDetection" :                true,
                                "notificationHigherPriority" :          false,
                                "opencageApiKey" :                      "",
                            ]


    def deviceLocatorList = [
                                // dynamic configurations
                                "pegLocatorFastestIntervalToInterval" : pegLocatorFastestIntervalToInterval, // Request that the location provider deliver updates no faster than the requested locator interval
                                "monitoring" :                          monitoring.toInteger(),              // Monitoring mode (quiet, manual, significant, move)
                                "locatorPriority" :                     getLocatorPriority(false),           // source/power setting for location updates (no power, low power, balanced power, high power)
                                "locatorDisplacement" :                 state.locatorDisplacement,           // How far should the device travel (in metres) before receiving another location
                                "locatorInterval" :                     locatorInterval,                     // How often should locations be requested from the device (seconds)
                                "moveModeLocatorInterval" :             moveModeLocatorInterval,             // How often should locations be requested from the device whilst in Move mode (seconds)
                                "ignoreInaccurateLocations" :           state.ignoreInaccurateLocations,     // Ignore location, if the accuracy is greater than the given meters.  NOTE: Build 420412000 occasionally reports events with acc=1799.999
                                "ignoreStaleLocations" :                ignoreStaleLocations,                // Number of days after which location updates are assumed stale
                                "ping" :                                ping,                                // Device will send a location interval at this heart beat interval (minutes).  Minimum 15, seems to be fixed at 30 minutes.
                            ]

    def deviceDisplayList = [
                                "notificationLocation" :                notificationLocation,                // Display last reported location and time in ongoing notification
                                "extendedData" :                        extendedData,                        // Include extended data in location reports
                                "notificationEvents" :                  notificationEvents,                  // Notify about received events
                                "enableMapRotation" :                   enableMapRotation,                   // Allow the map to be rotated
                                "showRegionsOnMap" :                    showRegionsOnMap,                    // Display the region pins/bubbles on the map
                                "notificationGeocoderErrors" :          notificationGeocoderErrors,          // Display Geocoder errors in the notification banner
                            ]

    // if we enabled a high accuracy location fix, then mark the user
    if (highAccuracyOnPing) {
//        configurationList.experimentalFeatures = "showExperimentalPreferenceUI,locationPingUsesHighAccuracyLocationRequest"
        configurationList.experimentalFeatures = "locationPingUsesHighAccuracyLocationRequest"
    } else {
        configurationList.experimentalFeatures = ""
    }

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
    if (validPositionType(data.t)) {
        update += sendMemberPositions(currentMember, data)
    }

    if (currentMember?.updateWaypoints) {
        currentMember.updateWaypoints = false
        update += sendClearWaypointsRequest(currentMember)
        update += sendWaypoints(currentMember)
    }
    // check if we have any places marked for removal, and clean up the list
    removePlaces()

    if ((currentMember?.updateLocation) || (currentMember?.updateDisplay)) {
        update += sendConfiguration(currentMember)
    } else {
        // dynamically change the configuration as necessary
        updateConfig = checkRegionConfiguration(currentMember, data)
        if (updateConfig) {
            update += updateConfig
        }
        // request a high accuracy report for one location request
        if (currentMember?.requestLocation) {
            currentMember.requestLocation = false
            logDescriptionText("Requesting a high accuracy location update for ${currentMember.name}")
            update += sendReportLocationRequest(currentMember)
        }
    }

    // request the member's regions
    if (currentMember?.getRegions) {
        currentMember.getRegions = false
        update += sendReportWaypointsRequest(currentMember)
    }

    // report status on ping message type -- user generated location already generates the status message
    if (data.t == "p") {
        update += sendReportStatusRequest(currentMember)
    }

    logDebug("Updating user: ${currentMember.name} with data: ${update}")
    return (update)
}

private def sendDeactivateUpdate(currentMember) {
    def update = []
    // indidate we have sent the commands
    currentMember["deactivate"] = 2

    // clear the waypoints list and URL
	update += sendClearWaypointsRequest(currentMember)
	update += sendClearURLConfiguration(currentMember)
	logWarn("Deactivating user: ${currentMember.name} with data: ${update}")

    return (update)
}

private def sendCmdToMember(currentMember) {
    // check if there are commands to send to the member
    if ((currentMember?.updateWaypoints) || (currentMember?.updateLocation) || (currentMember?.updateDisplay) || (currentMember?.getRegions)) {
        return (true)
    } else {
        return (false)
    }
}

def getAndroidMembers() {
    // returns a list of Android members
    members = []
    settings?.enabledMembers.each { enabledMember->
        member = state.members.find {it.name==enabledMember}
        if (isAndroidMember(member)) {
            members << member.name
        }
    }

    return(members)
}

def getiOSMembers() {
    // returns a list of iOS members
    members = []
    settings?.enabledMembers.each { enabledMember->
        member = state.members.find {it.name==enabledMember}
        if (member?.appVersion?.toString()?.indexOf(ANDROID_USER_AGENT,0) < 0) {
            members << member.name
        }
    }

    return(members)
}

def isAndroidMember(member) {
    if (member?.appVersion?.toString()?.indexOf(ANDROID_USER_AGENT,0) >= 0) {
        return (true)
    } else {
        return (false)
    }
}

def isAllAndroidMembers() {
    // check if all users are Android
    return(settings?.enabledMembers?.size() == getAndroidMembers()?.size() ? true : false)
}

def validPositionType(locationType) {
    // allow update if ping or manual location
    return ((locationType == "p") || (locationType == "u"))
}

def validLocationType(locationType) {
    // allow if not a region or manual update
    return ((locationType != "l") && (locationType != "u"))
}

private def createRecorderFriendsLocationTile() {
    def deviceWrapper = getChildDevice(getCommonChildDNI())
    if (deviceWrapper) {
        deviceWrapper.generateRecorderFriendsLocationTile()
    }
}

def createGoogleFriendsLocationTile() {
    def deviceWrapper = getChildDevice(getCommonChildDNI())
    if (deviceWrapper) {
        deviceWrapper.generateGoogleFriendsLocationTile()
    }
}

private def getCommonChildDNI() {
    return("${app.id}.${COMMON_CHILDNAME}")
}

private def createCommonChild() {
    // common device to allow the app to do across family member tasks
    def deviceWrapper = getChildDevice(getCommonChildDNI())
    if (!deviceWrapper) {
        logDescriptionText("Creating OwnTracks Common Device: $COMMON_CHILDNAME:${getCommonChildDNI()}")
        try{
            addChildDevice("lpakula", "OwnTracks Common Driver", getCommonChildDNI(), ["name": COMMON_CHILDNAME, isComponent: false])
            logDescriptionText("Common Child Device Successfully Created")
        }
        catch (e) {
            logError("Common Child device creation failed with error ${e}")
        }
    }
}

private def createChild(name) {
    // the unique ID will be the EPOCH timestamp
    id = now()
    def DNI = "${app.id}.${id}"

    logDescriptionText("Creating OwnTracks Device: $name:$DNI")
    try{
        def deviceName = createDeviceName(name)
        addChildDevice("lpakula", "OwnTracks Driver", DNI, ["name": "${deviceName}", isComponent: false])
        state.members.find {it.name==name}.id = DNI
        logDescriptionText("Child Device Successfully Created")
    }
    catch (e) {
        logError("Child device creation failed with error ${e}")
    }
}

private def updateChildName(member) {
    try {
        def deviceWrapper = getChildDevice(member.id)
        def deviceName = createDeviceName(member.name)
        if (deviceWrapper.getName() != deviceName) {
            deviceWrapper.setName(deviceName)
            logWarn("Changing ${member.name} device name to '${deviceName}'")
        } else {
            logDebug("Leaving ${member.name} device name as '${deviceName}'")
        }
    } catch(e) {
        logWarn("Leaving ${member.name} device name as '${deviceName}'")
    }
}

private def createDeviceName(name) {
    if (deviceNamePrefix) {
        deviceName = "${deviceNamePrefix}${name}"
    } else {
        deviceName = name
    }
    return (deviceName?.trim())
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

private angleFromCoordinate(lat1, lon1, lat2, lon2) {
    double deltaLon = Math.toRadians(lon2 - lon1);
    double y = Math.sin(deltaLon) * Math.cos(Math.toRadians(lat2));
    double x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) - Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(deltaLon);
    double angle = Math.toDegrees(Math.atan2(y, x));

    return (angle + 360) % 360; // Normalize to 0-360 degrees
}

def displayKmMiVal(val) {
    return (imperialUnits ? (val?.toFloat()*0.621371)?.round(1) : val?.toFloat()?.round(1))
}

def displayMFtVal(val) {
    // round up and convert to an integer
    return (imperialUnits ? (val?.toFloat()*3.28084)?.round(0)?.toInteger() : val?.toInteger())
}

def convertToKilometers(val) {
    // round up and convert to an integer
    return (imperialUnits ? (val?.toFloat()*1.60934)?.round(0)?.toInteger() : val?.toInteger())
}

def convertToMeters(val) {
    // round up and convert to an integer
    return (imperialUnits ? (val?.toFloat()*0.3048)?.round(0)?.toInteger() : val?.toInteger())
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

def isimperialUnits() {
    return (imperialUnits)
}

private isAddress(address) {
    if (address) {
        addressList = address?.split(',')
        // check if the first two entries in the address are not numbers (lat,lon), then it's an address
        if (!addressList[0]?.isNumber() || !addressList[1]?.isNumber()) {
            return (true)
        }
    }
    // default to not address
    return (false)
}

private def updateAddress(currentMember, data) {
    // check if the incoming coordinates within the hystersis of past coordinates, and we have a previously stored address
    if ((haversine(data.lat, data.lon, currentMember.latitude, currentMember.longitude) < DEFAULT_geocodeLookupHysteresis) && isAddress(currentMember.address) && !isAddress(data.address)) {
        data.address = currentMember.address
    } else {
        // do the address lookup
        data.address = getReverseGeocodeAddress(data)
        currentMember.address = data.address
    }
}

private def getReverseGeocodeAddress(data) {
    try {
        // if we have received an address field from the phone
        if (isAddress(data?.address)) {
            // we already have an address, so pass it back out
            return(data.address)
        }
    } catch (e) {
        // ignore the error and continue
    }

    // do a reverse geocode lookup to get the address
    return(reverseGeocode(data.lat, data.lon))
}

private def reverseGeocode(lat,lon) {
    if ((geocodeProvider != "0") && (geocodeProvider != null) && isGeocodeAllowed()) {
        // generate the reverse loopup URL based on the provider
        lookupUrl = GEOCODE_ADDRESS[geocodeProvider.toInteger()] + REVERSE_GEOCODE_REQUEST_LAT[geocodeProvider.toInteger()] + lat.toDouble().round(6) + REVERSE_GEOCODE_REQUEST_LON[geocodeProvider.toInteger()] + lon.toDouble().round(6) + GEOCODE_KEY[geocodeProvider.toInteger()] + settings["geocodeAPIKey_$geocodeProvider"]?.trim()
        String address = ADDRESS_JSON[geocodeProvider.toInteger()]
        // replace the spaces with %20 to make it URL friendly
        response = syncHttpGet(lookupUrl.replaceAll(" ","%20"))
        if (response != "") {
            logDebug ("Coodindate lookup results in addess: ${response.results."$address"[0]}")
            return(response.results."$address"[0])
        }
    }

    return("$lat,$lon")
}

private def geocode(address) {
    Double lat = 0.0
    Double lon = 0.0
    if ((geocodeProvider != "0") && (geocodeProvider != null) && isGeocodeAllowed()) {
        // generate the forward loopup URL based on the provider
        lookupUrl = GEOCODE_ADDRESS[geocodeProvider.toInteger()] + GEOCODE_REQUEST[geocodeProvider.toInteger()] + address + GEOCODE_KEY[geocodeProvider.toInteger()] + settings["geocodeAPIKey_$geocodeProvider"]?.trim()
        // replace the spaces with %20 to make it URL friendly
        response = syncHttpGet(lookupUrl.replaceAll(" ","%20"))
        if (response != "") {
            switch (geocodeProvider.toInteger()) {
                case 1:
                    // Google
                    lat = response.results.geometry.location.lat[0]
                    lon = response.results.geometry.location.lng[0]
                break
                case 2:
                    // Geoapify
                    lat = response.results.lat[0]
                    lon = response.results.lon[0]
                break
                case 3:
                    // Opencage
                    lat = response.results.geometry.lat[0]
                    lon = response.results.geometry.lng[0]
                break
                default:
                    // do nothing
                break
            }
            lat = lat?.toDouble()?.round(6)
            lon = lon?.toDouble()?.round(6)
            logDescriptionText("Address: '$address' resolves to $lat,$lon")
        }
    } else {
        logWarn("Geocode not configured or quota has been exceeded.  Select 'Additional Hub App Settings' to configure/verify geocode provider.")
    }

    return[lat,lon]
}

def getGoogleMapsAPIKey() {
    return(settings["googleMapsAPIKey"]?.trim())
}

private def isGeocodeAllowed() {
    String provider = GEOCODE_USAGE_COUNTER[geocodeProvider.toInteger()]
    // check if we are allowing paid lookups or we are under our quota and we have a key defined
    if (settings["geocodeAPIKey_$geocodeProvider"]?.trim() && (!geocodeFreeOnly || (state."$provider" < GEOCODE_QUOTA[geocodeProvider.toInteger()]))) {
        // increment the usage counter
        state."$provider"++
        return(true)
    } else {
        return(false)
    }
}

private def isMapAllowed(incrementUsage) {
    String provider = GEOCODE_USAGE_COUNTER[geocodeProvider.toInteger()]
    // check if we are allowing paid lookups or we are under our quota and we have a key defined
    if (getGoogleMapsAPIKey() && (!mapFreeOnly || (state.mapApiUsage < GOOGLE_MAP_API_QUOTA))) {
        if (incrementUsage) {
            // increment the usage counter
            state.mapApiUsage++
        }
        return(true)
    } else {
        return(false)
    }
}

def dailyScheduler() {
    logDescriptionText("Running daily geocode quota maintenance.")
    dayOfMonth = new SimpleDateFormat("d").format(new Date())
    // check if it's the first of the month
    if (dayOfMonth.toInteger() == 1) {
        state.mapApiUsage = 0
    }
    // runs midnight GMT - reset the quota's based on if the provider resets daily or monthly
    GEOCODE_USAGE_COUNTER.eachWithIndex { entry, index ->
        String provider = GEOCODE_USAGE_COUNTER[index+1]
        if (GEOCODE_QUOTA_INTERVAL_DAILY[index+1]) {
            state."$provider" = 0
        } else {
            // check if it's the first of the month
            if (dayOfMonth.toInteger() == 1) {
                state."$provider" = 0
            }
        }
    }
}

def createRegionMap(lat,lon,rad) {
    return(["lat":lat,"lon":lon,"rad":rad])
}

def getRegionMapLink(region) {
    // if we have an API key that is still has quota left
    APIKey = googleMapsAPIKey?.trim()
    if (APIKey && isMapAllowed(true)) {
        return(getFullLocalApiServerUrl() + "/regionmap/${createRegionMap(region?.lat,region?.lon,region?.rad)}?access_token=${state.accessToken}")
    } else {
        return("https://maps.google.com/?q=${region?.lat},${region?.lon}&z=17&output=embed&")
    }
}

def getMemberMapLink(memberName) {
    return(getFullLocalApiServerUrl() + "/membermap/${memberName.toLowerCase()}?access_token=${state.accessToken}")
}

def generateRegionMap() {
    // convert the string back to a map
    def region = evaluate((params.region).replaceAll("%20",""))

    String htmlData = "Google Maps API Not Configured or Quota Exceeded"
    APIKey = getGoogleMapsAPIKey()
    if (APIKey && isMapAllowed(true)) {
        htmlData = """
        <div style="width:100%;height:100%;margin:5px">
            <div id="map" style="width:100%;height:100%;"></div>
            <script>
                function initMap() {
                    const map = new google.maps.Map(
                        document.getElementById("map"),
                        {
                            zoom:17,
                            center:{
                                lat:${region.lat},
                                lng:${region.lon}
                            },
                            mapId:"owntracks",
                        }
                    );
                    // place the region pin
                    const i=new google.maps.marker.PinElement(
                        {
                            scale:1.0,
                            background:"${(regionPinColor == null ? DEFAULT_REGION_PIN_COLOR : regionPinColor)}",
                            borderColor:"${(regionPinColor == null ? DEFAULT_REGION_PIN_COLOR : regionPinColor)}",
                            glyphColor:"${DEFAULT_REGION_GLYPH_COLOR}"
                        }
                    );
                    const infoWindow = new google.maps.InfoWindow();
                    const marker = new google.maps.marker.AdvancedMarkerElement(
                        {
                            map,
                            position:{
                                lat:${region.lat},
                                lng:${region.lon}
                            },
                            content:i.element
                        }
                    );
                    // place the region radius
                    const radius = new google.maps.Circle(
                        {
                            map,
                            center:{
                                lat:${region.lat}, lng:${region.lon}
                            },
                            radius: ${region.rad},
                            strokeColor:"${(regionPinColor == null ? DEFAULT_REGION_PIN_COLOR : regionPinColor)}",
                            strokeOpacity:0.17,
                            strokeWeight:1,
                            fillColor:"${(regionPinColor == null ? DEFAULT_REGION_PIN_COLOR : regionPinColor)}",
                            fillOpacity:0.17
                        }
                    );
                }
             </script>
            <script src="https://maps.googleapis.com/maps/api/js?key=${APIKey}&loading=async&libraries=marker,maps&callback=initMap"></script>
        </div>"""
    }

    return render(contentType: "text/html", data: (insertOwnTracksFavicon() + htmlData))
}

def generateConfigMap() {
    String htmlData = "Google Maps API Not Configured or Quota Exceeded or Cloud Web Links are Disabled"
    APIKey = getGoogleMapsAPIKey()
    if (APIKey && isMapAllowed(true) && isCloudLinkEnabled(request.HOST)) {
        htmlData = """
        <div style="width:100%;height:100%;margin:5px">
            <table style="width:100%">
                <tr>
                    <td align="left"><select id="id-region" style="font-size:1.0em"></select></td>
                    <td align="right"><input id="id-address" placeholder="" size="40" style="font-size:1.0em"></input></td>
                </tr>
            </table>
            <div id="map" style="width:100%;height:95%;"></div>
            <script>
                const places = ["""
                    getNonFollowRegions(COLLECT_PLACES["desc"]).each { region->
                        place = state.places.find {it.desc==region}
                        htmlData += """{lat:${place.lat},lng:${place.lon},rad:${place.rad},desc:"${place.desc}",tst:${place.tst}},"""
                    }
                    htmlData += """
                ];

                // get the params if they were passed
                const urlParams = new URLSearchParams(window.location.search);
                const paramLat = urlParams.get("lat");
                const paramLon = urlParams.get("lon");
                mapLat = ${location.getLatitude()};
                mapLon = ${location.getLongitude()};
                homeRegion = "${getHomeRegion()?.tst}";
                addNewPin = false;
                mapStartingCoordinates();

                function mapStartingCoordinates() {
                    const homeLat = ${getHomeRegion()?.lat};
                    const homeLon = ${getHomeRegion()?.lon};
                    // default to the hub lat/lon
                    // if we passed coordinates, use them, if not, use the home coordinates
                    // preference:  param coordinates -> home coordinates -> hub coordinates
                    if (paramLat && paramLon) {
                        mapLat = paramLat;
                        mapLon = paramLon;
                        addNewPin = true;
                    } else {
                        // checking if it exists is still passing for "" so we do this the messy way
                        if ((homeLat != "") && (homeLat != null) && (homeLon != "") && (homeLon != null)) {
                            mapLat = homeLat;
                            mapLon = homeLon;
                        }
                    }
                };

                function initMap() {
                    const infoWindow = new google.maps.InfoWindow();
                    const markers = [];
                    lastIndex = "";
                    lastPosition = "";
                    updateRegionSelector()
                    addRegionListener()
                    updateAddressInput()
                    addAddressListener()

                    map = new google.maps.Map(
                        document.getElementById("map"),
                        {
                            zoom:17,
                            center: {
                                lat: parseFloat(mapLat),
                                lng: parseFloat(mapLon)
                            },
                            mapId:"owntracks",
                        }
                    );
                    // add pre-existing markers
                    for (let idx = 0; idx < places.length; idx++) {
                        createMarker(places[idx], idx);
                        if ((places[idx].lat == paramLat) && (places[idx].lng == paramLon)) {
                            // matches existing pin, so do not add a new one
                            addNewPin = false;
                        }
                    };
                    // add new marker if lat/lon was passed to the URL, and it was not a pre-existing URL
                    if (addNewPin) {
                        addMarker({ lat: parseFloat(paramLat), lng: parseFloat(paramLon) });
                    }

                    addInfoListener();
                    map.addListener('click', (evt) => {
                        infoWindow.close(map);
                        // first remove any unsaved markers, if any
                        deleteUnsavedMarkers();
                        addMarker({ lat: evt.latLng.lat(), lng: evt.latLng.lng() })
                        updateRegionSelector()
                    });

                    function addMarker(region) {
                        newMarker = { lat: region.lat, lng: region.lng, rad: ${DEFAULT_RADIUS}, desc: '', tst: Math.trunc((new Date().getTime())/1000) };
                        createMarker(newMarker, places.length);
                        displayInfo(region, places.length, true);
                        infoWindow.open(map, markers[markers.length - 1].marker);
                    }

                    function createMarker(markerElement, index) {
                        const pin = new google.maps.marker.PinElement(
                            {
                                background: "${(regionPinColor == null ? DEFAULT_REGION_PIN_COLOR : regionPinColor)}",
                                borderColor: "${(regionPinColor == null ? DEFAULT_REGION_PIN_COLOR : regionPinColor)}",
                            }
                        );
                        // change the pin glyph if it is home or just a region
                        changePinGlyph(index, pin, (markerElement.tst == homeRegion));
                        const marker = new google.maps.marker.AdvancedMarkerElement(
                            {
                                map,
                                position:{
                                    lat: markerElement.lat,
                                    lng: markerElement.lng,
                                },
                                title: markerElement.desc,
                                gmpDraggable: true,
                                content: pin.element,
                                zIndex: markerElement.tst
                            }
                        );
                        const radius = new google.maps.Circle(
                            {
                                map,
                                center:{
                                    lat: markerElement.lat,
                                    lng: markerElement.lng,
                                },
                                radius: markerElement.rad,
                                strokeOpacity: 0.17,
                                strokeWeight: 1,
                                fillOpacity: 0.17
                            }
                        );
                        // set the color based on a new or pre-existing pin
                        changeRadiusColor(radius, index);

                        marker.addListener('drag', function(evt) {
                            radius.setCenter(evt.latLng);
                        });
                        marker.addListener("dragend", (evt) => {
                            displayInfo({ lat: evt.latLng.lat(), lng: evt.latLng.lng() }, index, true);
                            infoWindow.open(map, marker);
                            updateRegionSelector()
                        });
                        marker.addListener("click", (evt) => {
                            displayInfo({ lat: evt.latLng.lat(), lng: evt.latLng.lng() }, index, true);
                            infoWindow.open(map, marker);
                            updateRegionSelector()
                        });

                        // save the markers and radiuses
                        markers.push({ marker, radius, pin });
                    };

                    function changePinGlyph(index, pin, home) {
                        if (home) {
                            pin.glyphColor = "${(regionHomeGlyphColor == null ? DEFAULT_REGION_HOME_GLYPH_COLOR : regionHomeGlyphColor)	}";
                            pin.scale = 2.0;
                        } else {
                            if (index == places.length) {
                                pin.scale = 1.5;
                                pin.background = "${DEFAULT_REGION_NEW_PIN_COLOR}";
                                pin.glyphColor = "${DEFAULT_REGION_NEW_GLYPH_COLOR}";
                            } else {
                                pin.scale = 1.0;
                                pin.background = "${(regionPinColor == null ? DEFAULT_REGION_PIN_COLOR : regionPinColor)}";
                                pin.glyphColor = "${(regionGlyphColor == null ? DEFAULT_REGION_GLYPH_COLOR : regionGlyphColor)}";
                            }
                        }
                    };

                    function changeRadiusColor(radius, index) {
                        // if this is a new (unsaved) pin, then change the color
                        if (index != places.length) {
                            radius.setOptions({
                                strokeColor: "${(regionPinColor == null ? DEFAULT_REGION_PIN_COLOR : regionPinColor)}",
                                fillColor: "${(regionPinColor == null ? DEFAULT_REGION_PIN_COLOR : regionPinColor)}"
                            });
                        }
                    };

                    function deleteUnsavedMarkers() {
                        // we have an unsaved marker on the place, so delete it and remove it from the map of markers
                        if (markers.length > places.length) {
                            markers[markers.length - 1].marker.setMap(null);
                            markers[markers.length - 1].radius.setMap(null);
                            markers.pop();
                        }
                    };

                    function updateRegionSelector() {
                        const regionSelector = document.getElementById("id-region");
                        // clear exisitng and then add the elements from the places list
                        regionSelector.innerHTML = "";
                        const option = document.createElement("option");
                        option.text = "Select a region";
                        regionSelector.appendChild(option);
                        places.forEach((place) => {
                            const option = document.createElement("option");
                            option.text = place.desc;
                            option.value = place.tst;
                            regionSelector.appendChild(option);
                        });
                    }

                    function updateAddressInput() {
                        const address = document.getElementById("id-address");
                        address.value = "";
                        address.placeholder = (${isGeocodeAllowed()} ? "Search for address" : "Requires a Geocode API key for address lookup")
                    }

                    function displayInfo(event, index, addressLookup) {
                        map.setCenter(event);
                        infoWindow.close();
                        infoWindow.setPosition(event);
                        // deal with the first time the map loads and there is no position to display.  The reverseGeocode callback will update this next time.
                        if (lastPosition == "") {
                            lastPosition = "" + event.lat.toFixed(6) + "," + event.lng.toFixed(6) + "";
                        }
                        infoWindow.setContent(infoContent(lastPosition, index));
                        if (addressLookup) {
                            // request the address lookup and populate the window in the callback
                            reverseGeocode(event, index);
                        }
                    };

                    function displayButtons(index) {
                        const nameBox = document.getElementById("id-name");
                        const setHomeButton = document.getElementById("id-sethome");
                        const homeButton = document.getElementById("id-home");
                        const deleteButton = document.getElementById("id-delete");
                        const saveButton = document.getElementById("id-save");

                        if (nameBox.value.trim() == "") {
                            setHomeButton.style.display = "none";
                            homeButton.style.display = "none";
                            deleteButton.style.display = "none";
                            saveButton.style.display = "none";
                        } else {
                            if (markers[index].marker.zIndex == homeRegion) {
                                setHomeButton.style.display = "none";
                                homeButton.style.display = "block";
                            } else {
                                if (index < places.length) {
                                    setHomeButton.style.display = "block";
                                } else {
                                    setHomeButton.style.display = "none";
                                }
                                homeButton.style.display = "none";
                            }
                            if (index < places.length) {
                                deleteButton.style.display = "block";
                            }
                            saveButton.style.display = "block";
                        }
                    };

                    function addRegionListener() {
                        const regionBox = document.getElementById("id-region");
                        regionBox.addEventListener("change", function() {
                            const regionTst = regionBox.options[regionBox.selectedIndex].value;
                            for (let idx = 0; idx < markers.length; idx++) {
                                if (markers[idx].marker.zIndex == regionTst) {
                                    displayInfo({ lat: markers[idx].marker.position.lat, lng: markers[idx].marker.position.lng }, idx, false);
                                    infoWindow.open(map, markers[idx].marker);
                                }
                            }
                        });
                    };

                    function addAddressListener() {
                        const addressBox = document.getElementById("id-address");
                        addressBox.addEventListener("keypress", function() {
                            if (event.key === 'Enter') {
                                address = addressBox.value.trim();
                                if (address) {
                                    dataMap = {};
                                    dataMap["address"] = address;
                                    sendDataToHub(dataMap, "geocode");
                                }
                                // clear the input box
                                updateAddressInput();
                            }
                        });
                    };

                    function reverseGeocode(event, index) {
                        const dataMap = {};
                        dataMap["lat"] = event.lat.toFixed(6);
                        dataMap["lon"] = event.lng.toFixed(6);
                        dataMap["index"] = index;
                        return(sendDataToHub(dataMap, "reversegeocode"));
                    }

                    function addInfoListener() {
                        google.maps.event.addListener(infoWindow, "domready", function () {
                            const saveButton = document.getElementById("id-save");
                            const setHomeButton = document.getElementById("id-sethome");
                            const deleteButton = document.getElementById("id-delete");
                            const homeButton = document.getElementById("id-home");
                            const nameBox = document.getElementById("id-name");
                            const radBox = document.getElementById("id-rad");
                            const markerIndex = document.getElementById("id-index");

                            // check if we need to undo changes to the last marker
                            if (lastIndex) {
                                if (lastIndex != markerIndex.value) {
                                    if (lastIndex < (markers.length-1)) {
                                        // check if we have an unsaved marker to delete, if so, don't try to revert it's information
                                        const lastLatLng = new google.maps.LatLng(places[lastIndex].lat, places[lastIndex].lng);
                                        markers[lastIndex].marker.position = lastLatLng;
                                        markers[lastIndex].radius.setCenter(lastLatLng);
                                        markers[lastIndex].radius.setRadius(places[lastIndex].rad);
                                    }
                                }
                            }
                            lastIndex = markerIndex.value;

                            if (markerIndex.value < (markers.length-1)) {
                                deleteUnsavedMarkers();
                            }

                            // only set the box values if they are blank.
                            if (markerIndex.value < places.length) {
                                nameBox.value = places[markerIndex.value].desc;
                                radBox.value = convertRadiusToFeet(places[markerIndex.value].rad);
                            } else {
                                nameBox.value = "";
                                radBox.value = convertRadiusToFeet(${DEFAULT_RADIUS});
                            }
                            markers[markerIndex.value].radius.setRadius(convertRadiusToMeters(radBox.value));

                            // show/hide the buttons when the window opens
                            displayButtons(markerIndex.value);

                            saveButton.addEventListener("click", function () {
                                console.log("Saving Region: " + nameBox.value);
                                // add a new marker
                                if (markerIndex.value == places.length) {
                                    places[markerIndex.value] = {lat:0.0,lng:0.0,rad:0,desc:"",tst:0};
                                }
                                markers[markerIndex.value].marker.title = nameBox.value;
                                markers[markerIndex.value].marker.rad = convertRadiusToMeters(radBox.value);
                                places[markerIndex.value].desc = markers[markerIndex.value].marker.title;
                                places[markerIndex.value].rad = markers[markerIndex.value].marker.rad;
                                places[markerIndex.value].lat = markers[markerIndex.value].marker.position.lat;
                                places[markerIndex.value].lng = markers[markerIndex.value].marker.position.lng;
                                places[markerIndex.value].tst = markers[markerIndex.value].marker.zIndex;

                                // set the color since the pin is saved
                                changeRadiusColor(markers[markerIndex.value].radius, markerIndex.value);
                                changePinGlyph(markerIndex.value, markers[markerIndex.value].pin, (markers[markerIndex.value].marker.zIndex == homeRegion));
                                displayButtons(markerIndex.value);
                                sendDataToHub(markerDataMap(markers[markerIndex.value]), "save");
                                alert("'" + markers[markerIndex.value].marker.title + "' saved.");
                                updateRegionSelector()
                            });
                            setHomeButton.addEventListener("click", function () {
                                console.log("Setting Home Region: " + markers[markerIndex.value].marker.title);
                                for (let idx = 0; idx < places.length; idx++) {
                                    if (markers[idx].marker.zIndex == homeRegion) {
                                        // switch the home pin
                                        changePinGlyph(idx, markers[idx].pin, false);
                                    }
                                };
                                homeRegion = markers[markerIndex.value].marker.zIndex;
                                changePinGlyph(markerIndex.value, markers[markerIndex.value].pin, true);
                                displayButtons(markerIndex.value);
                                sendDataToHub(markerDataMap(markers[markerIndex.value]), "home");
                            });
                            deleteButton.addEventListener("click", function () {
                                console.log("Deleting Region: " + markers[markerIndex.value].marker.title);
                                markers[markerIndex.value].marker.setMap(null);
                                markers[markerIndex.value].radius.setMap(null);
                                infoWindow.close();
                                sendDataToHub(markerDataMap(markers[markerIndex.value]), "delete");
                                if (markers[markerIndex.value].marker.zIndex == homeRegion) {
                                    alert("'Home' region deleted.\\n\\nCreate or select a location and click 'Set Home' to assign a new 'Home' location.");
                                } else {
                                    alert("'" + markers[markerIndex.value].marker.title + "' deleted.");
                                }
                                // remove the item from the lists
                                markers.splice(markerIndex.value, 1);
                                places.splice(markerIndex.value, 1);
                                lastIndex = "";
                                updateRegionSelector()
                            });
                            nameBox.addEventListener("input", function () {
                                // show/hide the buttons based on the user text
                                displayButtons(markerIndex.value);
                            });
                            radBox.addEventListener("input", function () {
                                markers[markerIndex.value].radius.setRadius(convertRadiusToMeters(radBox.value));
                            });
                        });
                        google.maps.event.addListener(infoWindow, 'closeclick', () => {
                            deleteUnsavedMarkers();
                        });
                    };

                    function convertRadiusToMeters(val) {
                        return (${imperialUnits} ? parseInt(val*0.3048) : parseInt(val))
                    }

                    function convertRadiusToFeet(val) {
                        return (${imperialUnits} ? parseInt(val*3.28084) : parseInt(val))
                    }

                    function markerDataMap(marker) {
                        dataMap = {};
                        dataMap["_type"] = "waypoint";
                        dataMap["desc"] = marker.marker.title;
                        dataMap["lat"] = marker.marker.position.lat;
                        dataMap["lon"] = marker.marker.position.lng;
                        dataMap["tst"] = marker.marker.zIndex;
                        dataMap["rad"] = marker.radius.getRadius();

                        return(dataMap)
                    }

                    function sendDataToHub(dataMap, action) {
                        const postData = {};
                        postData["action"] = action;
                        postData["payload"] = dataMap;

                        fetch("${getAttributeURL(request.headers.Host.toString(), "apidata")}", {
                            method: "POST",
                            body: JSON.stringify(postData),
                            headers: {
                                "Content-type": "application/json; charset=UTF-8"
                            }
                        })
                        .then(response => response.json())
                        .then(data => {
                            switch (data.action) {
                                case "geocode":
                                    center = {};
                                    center["lat"] = parseFloat(data.lat);
                                    center["lng"] = parseFloat(data.lon);
                                    map.setCenter(center);
                                    addMarker({ lat: parseFloat(data.lat), lng: parseFloat(data.lon) });
                                break;
                                case "reversegeocode":
                                    lastPosition = data.address;
                                    infoWindow.setContent(infoContent(data.address, data.index));
                                break;
                                default:
                                    // do nothing
                                break;
                            }
                        })
                        .catch(error => { console.error('Error fetching data:', error); })
                    };

                    function infoContent(position, index) {
                        const contentString =
                        "<table style='width:100%;font-size:1.0em'>" +
        	                "<tr>" +
	        	                "<td align='left'>" +
                                    "<style>#id-delete:hover { background-color:#b40000;color:white }</style>" +
          		                    "<button type='submit' id='id-delete' style='display:none'>Delete</button>" +
    	        	            "</td>" +
            	            "</tr>" +
            	            "<tr>" +
        	    	            "<td><b>Name:</b></td>" +
        		                "<td><input type='text' id='id-name'></td>" +
        	                "</tr>" +
        	                "<tr>" +
            		            "<td><b>Radius (" + "${getSmallUnits()}" + "):</b></td>" +
                                "<td><input type='number' min='" + convertRadiusToFeet(25) + "' max='" + convertRadiusToFeet(10000) + "' step='" + convertRadiusToFeet(25) + "' id='id-rad'></td>" +
             	            "</tr>" +
        	                "<tr>" +
        		                "<td><b>Location:</b></td>" +
        		                "<td>" + position + "</td>" +
        	                "</tr>" +
            	            "<tr>" +
	            	            "<td align='left'>" +
                                    "<style>#id-sethome:hover { background-color:green;color:white }</style>" +
              		                "<button type='submit' id='id-sethome' style='display:none'>Set Home</button>" +
          	    	                "<button type='submit' id='id-home' style='background:green;color:white;display:none'>Home</button>" +
	        	                "</td>" +
	        	                "<td align='right'>" +
                                    "<style>#id-save:hover { background-color:${DEFAULT_APP_THEME_COLOR};color:white }</style>" +
          		                    "<button type='submit' id='id-save' style='display:none'>Save</button>" +
	        	                "</td>" +
            	            "</tr>" +
                        "</table>" +
                        "<input type='hidden' id='id-index' value='" + index + "'>"

                        return(contentString)
                    };
                };
            </script>
            <script src="https://maps.googleapis.com/maps/api/js?key=${APIKey}&loading=async&libraries=marker,maps&callback=initMap"></script>
        </div>"""
    }

    return render(contentType: "text/html", data: (insertOwnTracksFavicon() + htmlData))
}

def displayMemberMap() {
    // find the member from the member name in the parameters - this is returned in lowercase
    member = state.members.find {it.name.toLowerCase()==params.member}
    String htmlData = "Private Member"

    if (isCloudLinkEnabled(request.HOST)) {
        def deviceWrapper = getChildDevice(member.id)
        if (deviceWrapper) {
            displayData = deviceWrapper.generateMember(request.headers.Host.toString())
            // only display if we could retrieve the data
            if (displayData) {
                htmlData = displayData
            }
        }
    } else {
        htmlData = "Cloud web links are disabled."
    }

    return render(contentType: "text/html", data: (insertOwnTracksFavicon() + htmlData))
}

def displayMemberPresence() {
    // find the member from the member name in the parameters - this is returned in lowercase
    member = state.members.find {it.name.toLowerCase()==params.member}
    String htmlData = "Member Not Configured"

    if (isCloudLinkEnabled(request.HOST)) {
        def deviceWrapper = getChildDevice(member.id)
        if (deviceWrapper) {
            displayData = deviceWrapper.generatePresence(request.headers.Host.toString())
            // only display if we could retrieve the data
            if (displayData) {
                htmlData = displayData
            }
        }
    } else {
        htmlData = "Cloud web links are disabled."
    }

    return render(contentType: "text/html", data: (insertOwnTracksFavicon() + htmlData))
}

def displayMemberPastLocations() {
    // find the member from the member name in the parameters - this is returned in lowercase
    member = state.members.find {it.name.toLowerCase()==params.member}
    String htmlData = "OwnTracks Recorder Not Configured or Private Member"

    def deviceWrapper = getChildDevice(member.id)
    if (deviceWrapper) {
        displayData = deviceWrapper.generatePastLocations()
        // only display if we could retrieve the data
        if (displayData) {
            htmlData = displayData
        }
    }

    return render(contentType: "text/html", data: (insertOwnTracksFavicon() + htmlData))
}

def processAPIData() {
    // process incoming data
    response = []
    data = parseJson(request.body)
    if (data.zoom) {
        state.googleMapsZoom = data.zoom
    }
    if (data.member) {
        state.googleMapsMember = data.member
    }
    if (data.action) {
        switch (data.action) {
            case "save":
                // trigger and add/update of the region
                addPlace([ "name":"" ], data.payload, false)
            break;
            case "home":
                // set home to the place matching the timestamp
                app.removeSetting("homePlace")
                app.updateSetting("homePlace", [value: data.payload.tst, type: "number"])
            break;
            case "delete":
                // delete region from hub/mobile or just hub depending on setting
                app.updateSetting("regionName",[value:data.payload.desc,type:"text"])
                if (manualDeleteBehavior) {
                    appButtonHandler("deleteRegionFromHubButton")
                } else {
                    appButtonHandler("deleteRegionFromAllButton")
                }
            break;
            case "geocode":
                (addressLat, addressLon) = geocode(data.payload.address)
                response = ["lat" : addressLat, "lon" : addressLon, "action" : data.action]
            break;
            case "reversegeocode":
                response = ["address" : reverseGeocode(data.payload.lat,data.payload.lon), "index" : data.payload.index, "action" : data.action]
            break;
            case "members":
                // return with the consolidated list of member information
                def memberLocations = []
                publicMembers = getEnabledAndNotHiddenMemberData(data.payload?.member)
                publicMembers.eachWithIndex { member, index ->
                    def deviceWrapper = getChildDevice(member.id)
                    def memberLocation = [
                        "name" :        "${member.name}",
                        "id" :          member.id,
                        "lat" :         member.latitude,
                        "lng" :         member.longitude,
                        "cog" :         member.bearing,
                        "spd" :         deviceWrapper?.currentValue("lastSpeed"),
                        "bat" :         member?.battery,
                        "acc" :         deviceWrapper?.currentValue("accuracy"),
                        "wifi" :        member?.wifi,
                        "ps" :          member?.ps,
                        "hib" :         (member?.hib ? member?.hib : "0"),
                        "bo" :          (member?.bo ? member?.bo : "0"),
                        "per" :         (member?.loc ? member?.loc : "0"),
                        "cmd" :         member?.cmd,
                        "stale" :       member.staleReport,
                        "bs" :          "${member?.bs}",
                        "last" :        "${deviceWrapper?.currentValue("lastLocationtime")}",
                        "since" :       "${deviceWrapper?.currentValue("since")}",
                        "data" :        "${member?.conn}",
                        "loc" :         "${deviceWrapper?.currentValue("location")}",
                        "dfh" :         deviceWrapper?.currentValue("distanceFromHome"),
                        "color":        (member?.color ? member?.color : DEFAULT_MEMBER_GLYPH_COLOR),
                        "history":      (member?.history ? member?.history : []),
                        "zIndex" :      index+1,
                    ]

                    // split out the img since that is a large data payload that we only need once during the page init
                    if (data.payload.request == "img") {
                        memberLocation["img"] = "${getEmbeddedImage(member.name)}"
                        // don't send the history on the initial page load
                        memberLocation["history"] = []
                        // clear the flag to allow the member to sync to the map
                        member.lastMapTime = 0
                    }
                    if (data.payload.request == "sync") {
                        // clear the flag to allow the member to sync to the map
                        member.lastMapTime = 0
                    }
                    // only send new data to the map to minimize data traffic
                    if (member.lastMapTime != member.lastReportTime) {
                        memberLocations << memberLocation
                    }
                    // store the last location report sent to the map
                    member.lastMapTime = member.lastReportTime
                }
                response = [ "members" : memberLocations, "appVersion" : appVersion() ]
            break;
            case "update":
                member = state.members.find {it.name==data.payload}
                response = ["lastReportTime" : member?.lastReportTime]
            break;
            default:
                logWarn("Unhandled API action: ${data.action}")
            break;
        }
    }

    // send back a success response
    return render(contentType: "text/html", data: (new JsonBuilder(response)).toPrettyString(), status: 200)
}

def processMemberAPICommand() {
    try {
        member = state.members.find {it.name.toLowerCase()==params.member.toLowerCase()}
        def deviceWrapper = getChildDevice(member?.id)
        deviceWrapper."${params.cmd}"()
        response = "Command '${params.cmd}' successfully issued for member '${params.member}'."
        status = 200
    } catch (e) {
        logError(e.message)
        response = "Command '${params.cmd}' failed for member '${params.member}'.  Member, member device or command does not exist."
        status = 404
    }

    return render(contentType: "text/html", data: (new JsonBuilder(response)).toPrettyString(), status: status)
}

def retrieveGoogleFriendsMapZoom() {
    return(state.googleMapsZoom)
}

def retrieveGoogleFriendsMapMember() {
    return(state.googleMapsMember)
}

def insertOwnTracksFavicon() {
    return('<link rel="icon" type="image/png" sizes="64x64" href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAE4AAABOCAYAAACOqiAdAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsEAAA7BAbiRa+0AAAqwSURBVHhe7ZxrUFXXFccX9wGXxxUEeSMvUcSKQIJKFDQSWx1NjKRlqo0pbdL46LSZSaeZadL2S2eafsiH9FMmzjhJk7SdtM2YavtBx9ROY6omPqKYaOIrGoQKgiCve7kP7Prvuy+DPO/GvQ9mJr+ZM3efwzkH7v+svdbaa+9D1G2GvkYZm/z8GkXuSYu7fq2bBgODco+frsNGGTkz5N69wbQL5xsI0vkzrXTjf710q9NLrSxay9UuCg4Tzs7CZeUlUTqLl5TsotRMNxUvSieHc/o6zLQJ193poVNHm+nMRy109mQLtUG4m15qa+6mwDDRwjhYvLRsKVyWm0ors6h0SRYtWpJN7iSXPMs6LBfO7wvSkX99QUfeu0ynjlyjc6euU8A/WqjJiI6xU0lFJpVX5VD1mjm0eGWepRZoqXBd7f20+w+n6cDuc/TJ8RbS8Ztttigqq8qmb317AdU1lFFCYoz8iVksE67pcie9/eoJevf1U9Td5ZVH9TFzVhzVP30fbdpeSWlZCfKoOSwR7uLZG/TaS4dp39/OimBgClecgzY2lNMPnq2inIIkedQMxoW78EkbvfrbQ7T/nXPyiFng5zY8sYi2P19NWfnmxDPqTdtbey0VDSDQ7H2rkXaxhff3+uRR/RgV7s+vHKcD734m96wD4u15s5H2/rFRHtGPMeEaP2qmv+48QYNBy4L2HQx4A/TGy0fpy0s35RG9GBHONxDgYHCEujo88sj00HS5i/6y8ySnPfofnhHhPvz3FTq0/6LcmxquOCcPsxLF593wzz+doYuf3pB7+tAeVXG7HY+8TR/svySPqFG6OIvmlaZR3twUSk6Lo5tt/XT1QgePZ9vozLEWeZYaT/78AfrZ7x6Se3rQLlzzlS7aWL6TPH1+eSQyCufPotpH59H91bk0b2GaGNCHwcD/PKc1Jz74kg7uOU+XP2uXP4kM5HR/P72NXLF3Z73D0S7cnjdP0y+f+ofci4xszreefbGWajfM4zGoQx4dDXznwb3n6eUXDooHpMI7x5+m+WXpcu/u0e7jPj58TbYip+6H5bS2fsGEogH8HOc9xuer0vhhs2zpQbtwnze2ylZk5BYl06Oc6auA8/P4OhUufNomW3rQLtzVC2p5E7pn5my16m4Gn7+Kr1Oh6VKnbOlBu3CqlY8H16sJEEb1OlWfOBlG8jgVir6RKltqqF7X3tonW3rQLlzCDLVCIkroU+GW4nUovetEu3CYSFHh2PtXZUuNY/9Ruy41Q29xU7twKenxshUZB3ZPrXqiel2q5qqwduGyctUi5Mn/NokRgQo4/+PDTXIvMmYXzpQtPWgXDtN1KqDY+Mpv3hdzEpGA83C+apGyYtls2dKDduGW1haQM1rttqimQIxD+yauqKBwgPNwvgrx7mhavDJf7ulB+1gVM/D1S3aJaoYqmGCuWVNE+cUplMPjV/jLDk4jrnEOduXzDiEcCqSqVNXm0679W+SeHoxM1rzx+6P00nPvyT118uexcAXDhPuChTvfIX+qBuZdX3x9Az38vVJ5RA9GhGtt7qEnV79FVy+aKVurUFKRQbv2PU6JybHyiB60+ziQnu2m+q33yb3pZfOPK7WLBowIBx7evFA5wuqmYlkOPbShWO7pxZhwszhT3/r88mlZSQSwJGLbCzVGrA0YEw5Uc4RUrbXpAutIHlhdIPf0Y1Q4LEfY8tMltHSV3hxqMmrWFtHmHZVkt5v7ekaFA0grdvx6hZiMsYKS8gz+fTWUmml2xZJx4UBlTS77u2rlAoAqWO4K0awISpYIBzDJ8sQzSyjaNfGEzFTBgkLMn9YaiqIjsUw4+LvvbrufNmwppagoeVATdkeUWFC4scG6QGSZcMCd6KIfPbeco12hPKKHb9aV0PefWap1wnkyLBUO5BSyH/pVDRWW6AkWZUuzOV+rFsslrMRy4QBqY1t/wcEi7e6CBRblbOeHMHdhmjxiHdMiHFhbXyJyvKkGi7iEaHr8J4s5RzSX5E6E5cKhFhMMDorPjQ1lwj9NhZXr54pEF+tJpgPLhRscHKS+Hh91tPVRb88ArflOCc0pUZsjxbKJ1XXF5PMGyOsJaHlfQhVLhYOl4Yt2tvfz5qHeWwOiBPXYU+U0Y2ZkxYAY7tp1DeWUlZtEPl+QPL0+8vT5+IFYq54lwoW7J0SDWN2dXjHZEgzcJme0napqC2jVI5EtacCrR8vXzBECEt/X0+8X90OXtVI8S4QLd8/2673U1tIjhIyNc7KDd4rSNgbj6zYtnPTVynh3jHgBBO9x4boYvgdWmHd29IsX6Ab4wViFNcIFb1M/Cwcrg2VgOQJGEjZUL+QwIj17xqTDpaW1+TRngfSHfJ3dZhNWBsF6uwfESnOrMC4cvhjeGOxjP4RPWNeQaCN4kLtr4ji+Dl16PVvlHcM1bmP/Nv8O+Dmvxy8s0IpgYVw4WEPPLa9YE2xjC0H+hW42FmlZbiofZ+IYL/bmF48ebWCVpis+WvhQuIOeLq9om8a4cD5fgPp62RLY2sDw7jkWK9bNla07WbGuiK119HVR/BDQ9Z1OB/kHgtR10yMEhHWbxKhw6KY+b5C8HPnABHoNMb8sY1R3RQStWJYr90YD8ZxseQG2NFg3rM60vzMmXEi0gEgXEBBiYp2TLo4GWK5QzOINp2B+yqQV3ZCvC73H1SeC0FfU4jDPHfAHucsExJeBX4NlRELlijzZCrF4xP54ODnoIIiERhR+0V0NzLcLzHVV/nuDnIZgg/Wp/Pnly3KGKidpme6IB/IOFg1WDV+HoASrgxWawKiPA3b4H6c9Iv8WBmJt2lEpyu34zFFc24aoCmsPBlg4pUcWOUbWjgCsWkJSikUzWIku0pAxcreJ6OIRQVKKWoESguH/l8xMiRXjYKxJxv8t0Y1xi8NzwaOZytNRFU3Apg3rDm9Ikk1gXDj4OOGkzeekAugUGtLZ2dLYRRhSzphwUfy40UWwIZqi6w4azuhxf2zI+/CeK4oBUYa+oTmL4weNpx7jsoulrRAO1mcS/GshP6c+KMe7Yh0iNcEDNIEx4ZC34YmjFORmB43oZtriMKxDDoehmWogUsXo3YV4/NTRdUJd1aDF4dbCPUSJWp/qAm5VzN6dgY9DYorIGkQizJvu1AqRG7kbLA2+DakPuqlJjAuHLoMvISyAv6CJYVC4Hoc6X/KsOIqNd2p/d2skFlhcqOtgCAVr8GP8ypsufwe/huEVYkBsfLRYAYqgZCoohDEvHFscBEtJTyB3Yozwexj0YxPiTdH6YLW4Hg8hwEMr+NF47qIQb7xCqU6MCwfCERbrclHlhTF4+kNzECgAKMOXiHkM+Z8mELkTEl0iDTFsaEMYG6uORXiKELNdEC1cbgr/BfCFNnbww7uZcPwyIkNk3MPGPxfJNfu0OLYw1PBE9Ja5mxVYKhzAF0dpG8LBYrzs1DGxjCEZuptYtzvMasLChcpT/Om/zYKF/CbGsvBpaFtlaWEsFw6/DQLAgkLV2gFRWoclon4W8LE4LBT8FkCkhN+CRaGNzRntEF1fRE8OBFb4tJFYLtxwkEbA2pCiQMTwJ0pDOA4glMjLxKA9NHmNzcZtpBymo+d4TKtwYyH8GMac6L78lyGphdOfDquaiHtOOERMjGuHylCs170mGrj3hPtKQPR/+yKC81RZy9sAAAAASUVORK5CYII="/>')
}

def generateGoogleFriendsMap() {
    String htmlData = "Google Maps API Not Configured or Quota Exceeded or Cloud Web Links are Disabled"
    APIKey = getGoogleMapsAPIKey()
    if (APIKey && isMapAllowed(true) && isCloudLinkEnabled(request.HOST)) {
        htmlData = """
        <div style="width:100%;height:100%;margin:5px">
            <div id="map" style="width:100%;height:calc(100% - 25px)"></div>
            <div id="footer" style="width:100%;color:white;background:#555555;text-align:center">
                <div id="id-lastTime" style="font-size:0.8em;color:white;font-family:arial;padding:5px"></div>
            </div>
            <script>
                // get the params if they were passed
                const urlParams = new URLSearchParams(window.location.search);
                const paramMember = urlParams.get("member");
                const webAppVersion = "${appVersion()}";
                console.log("Google Friends Map version " + webAppVersion)

                locations = [];
                regions = [];
                currentMember = "null";
                historyMember = "null";
                lastUpdate = "Unknown";

                function getMember(memberName) {
                    // returns the member map based on a name match
                    for (let loc=0; loc<locations.length; loc++) {
                        if (locations[loc].name == memberName) {
                            return(locations[loc]);
                        }
                    }
                    return("null");
                }

                function initMap() {
                    const infoWindow = new google.maps.InfoWindow();
                    const maxGradient = 0.9;
                    const minBearingGradient = 0.1;
                    const minRadiusGradient = 0.1;
                    const minEndpointStrokeGradient = 0.4;
                    const maxZIndex = google.maps.Marker.MAX_ZINDEX - 1000;
                    const markers = [];
                    // -2=close, -1=marker open, 0+=history sample open
                    infoWindowVisible = -2;
                    inhibitAutoZoom = false;
                    tripNumber = 0;
                    tripMember = "null";
                    // get the member data with thumbnail images
                    retrieveMemberLocations("img");
                    // restore the previous zoom level on the init
                    currentZoom = ${retrieveGoogleFriendsMapZoom()};
                    if (!currentZoom) {
                        currentZoom = 2;
                    }

                    homePosition = new google.maps.LatLng(${getHomeRegion()?.lat}, ${getHomeRegion()?.lon});
                    const places = ["""
                        getNonFollowRegions(COLLECT_PLACES["desc"]).each { region->
                            place = state.places.find {it.desc==region}
                            htmlData += """{lat:${place.lat},lng:${place.lon},rad:${place.rad},desc:"${place.desc}",tst:${place.tst}},"""
                        }
                        htmlData += """
                    ];

                    const map = new google.maps.Map(
                        document.getElementById("map"), {
                            zoom: currentZoom,
                            center: {
                                lat: 0, lng: 0
                            },
                            mapId:"owntracks",
                            gestureHandling: "auto",
                        }
                    );

                    map.addListener("click", () => {
                        mapRegionClick();
                    });
                    map.addListener("zoom_changed", () => {
                        // when the user clicks the +/- zoom buttons or we modify zoom by clicking on the marker
                        currentZoom = map.getZoom();
                        updateHistoryZoom(currentZoom);
                        postData = {};
                        postData["zoom"] = currentZoom;
                        sendDataToHub(postData);
                        showHideRegions();
                    });
                    map.addListener("dragstart", () => {
                        // user starting panning the map
                        inhibitAutoZoom = true;
                        updateBottomBanner();
                    });
                    map.addListener("dragend", () => {
                        // user stopped panning the map
                        inhibitAutoZoom = true;
                        updateBottomBanner();
                    });
                    map.addListener("dblclick", () => {
                        // user double clicked the map
                        inhibitAutoZoom = true;
                        updateBottomBanner();
                    });
                    google.maps.event.addListener(infoWindow, "closeclick", () => {
                        infoWindowVisible = -2;
                        tripNumber = 0;
                        historyMember = "null";
                        inhibitAutoZoom = false;
                        updateBottomBanner();
                    });

                    // place the region pins
                    places.forEach(position => {
                        const i=new google.maps.marker.PinElement(
                            {
                                scale:0.5,
                                background:"${(regionPinColor == null ? DEFAULT_REGION_PIN_COLOR : regionPinColor)}",
                                borderColor:"${(regionPinColor == null ? DEFAULT_REGION_PIN_COLOR : regionPinColor)}",
                                glyphColor:"${(regionGlyphColor == null ? DEFAULT_REGION_GLYPH_COLOR : regionGlyphColor)}"
                            }
                        );
                        pin = new google.maps.marker.AdvancedMarkerElement(
                            {
                                map,
                                position:{
                                    lat:position.lat,
                                    lng:position.lng,
                                },
                                title:position.desc,
                                content:i.element
                            }
                        )
                        // place the region radius'
                        const radius = new google.maps.Circle(
                            {
                                map,
                                center:{
                                    lat:position.lat,
                                    lng:position.lng,
                                },
                                radius:position.rad,
                                strokeColor:"${(regionPinColor == null ? DEFAULT_REGION_PIN_COLOR : regionPinColor)}",
                                strokeOpacity:0.17,
                                strokeWeight:1,
                                fillColor:"${(regionPinColor == null ? DEFAULT_REGION_PIN_COLOR : regionPinColor)}",
                                fillOpacity:0.17,
                                visible:true,
                            }
                        )
                        radius.addListener("click", () => {
                            mapRegionClick();
                        });
                        // change the home pin glyph
                        if (position.tst == "${getHomeRegion()?.tst}") {
                            i.glyphColor = "${(regionHomeGlyphColor == null ? DEFAULT_REGION_HOME_GLYPH_COLOR : regionHomeGlyphColor)}"
                            i.scale = 1.3;
                        }
                        regions.push(radius);
                    });

                    function mapRegionClick() {
                        infoWindow.close();
                        // if there is a selected trip, first click unhides the rest of history
                        if (tripNumber > 0) {
                            tripNumber = 0;
                        } else {
                            // otherwise release the map and allow for full history if a trip had been previously selected
                            if (inhibitAutoZoom) {
                                inhibitAutoZoom = false;
                            } else {
                                // then release the member
                                currentMember = "null";
                                historyMember = "null";
                                postData = {};
                                postData["member"] = currentMember;
                                sendDataToHub(postData);
                                infoWindowVisible = -2;
                                changeHistoryFocus(currentMember);
                            }
                            // adjust zoom as necessary
                            fitMapBounds();
                            tripMember = "null";
                        }
                        showHideHistory();
                        updateBottomBanner();
                    };

                    function addMemberMarkers() {
                        currentMember = getMember("${retrieveGoogleFriendsMapMember()}");
                        // place the members on the map
                        for (let member=0; member<locations.length; member++) {
                            const namePin = document.createElement("div");
                            namePin.textContent = locations[member].name;

                            const imagePin = document.createElement("object");
                            imagePin.data = locations[member].img;
                            imagePin.type = "image/jpeg";
                            imagePin.width = "40";
                            imagePin.height = "40";
                            imagePin.appendChild(namePin);

                            const pin = new google.maps.marker.PinElement({
                                scale: 2.5,
                                background: "${(memberPinColor == null ? DEFAULT_MEMBER_PIN_COLOR : memberPinColor)}",
                                borderColor: "${(memberPinColor == null ? DEFAULT_MEMBER_PIN_COLOR : memberPinColor)}",
                                glyphColor: locations[member].color
                            });
                            if (locations[member].img) {
                                pin.glyph = imagePin;
                            }

                            const marker = new google.maps.marker.AdvancedMarkerElement({
                                map,
                                position: {
                                    lat: locations[member].lat,
                                    lng: locations[member].lng
                                },
                                title: locations[member].name,
                                zIndex: (currentMember.name == locations[member].name ? maxZIndex : locations[member].zIndex),
                                content: pin.element
                            });

                            // Add a click listener for each marker, and set up the info window
                            marker.addListener("click", () => {
                                infoWindow.close();
                                infoWindow.setContent(infoContent(locations[member]));
                                infoWindow.open(marker.map, marker);
                                map.setCenter(marker.position);
                                // on the first click, just display the info box
                                if ((currentMember.name == marker.title) && (infoWindowVisible == -1)) {
                                    // on second click, zoom to 14, and then keep zooming in by 3 on each future click
                                    currentZoom = map.getZoom()
                                    if (currentZoom < 14) {
                                        currentZoom = 14;
                                    } else {
                                        currentZoom += 3;
                                        if (currentZoom > 21) {
                                            currentZoom = 21;
                                        }
                                    }
                                    map.setZoom(currentZoom);
                                }

                                // assign the new current member so that the history can update
                                currentMember = getMember(marker.title);

                                // restores the marker index of the previous marker
                                restoreMarkerIndex();
                                // if a marker is clicked, then assign it a higher index so it comes out in front
                                marker.zIndex = maxZIndex;
                                infoWindowVisible = -1;
                                postData = {};
                                postData["zoom"] = currentZoom;
                                postData["member"] = currentMember.name;
                                sendDataToHub(postData);
                                changeHistoryFocus(currentMember);
                                updateBottomBanner();
                                showHideHistory();
                            });

                            // create history circles for the past locations
                            var history = [];
    						for (let past=0; past<${memberHistoryLength}; past++) {
	    						// place the past locations
		    					const radius = new google.maps.Circle(
			    					{
				    					map,
					    				center:{
                                            lat: locations[member].lat,
                                            lng: locations[member].lng
								    	},
									    radius:1 + ((Math.pow(2, (22 - currentZoom))*${memberHistoryScale})/10),
    									strokeColor:locations[member].color,
                                        strokeOpacity:historyGradient(${memberHistoryLength},past,minRadiusGradient),
		    							strokeWeight:2,
			    						fillColor:locations[member].color,
				    					fillOpacity:historyGradient(${memberHistoryLength},past,minRadiusGradient),
                                        visible:false,
                                        zIndex:past
					    			}
						    	)
                                // Add a click listener for each radius, and set up the info window
                                radius.addListener("click", () => {
                                    // user selected a history point
                                    infoWindow.close();
                                    infoWindow.setPosition(radius.getCenter());
                                    infoWindowVisible = radius.zIndex;
                                    infoWindow.setContent(historyContent(locations[member].name, locations[member].history, past, false));
                                    infoWindow.open(radius.map, radius);
                                    historyMember = locations[member].name;
                                    inhibitAutoZoom = true;
                                    updateBottomBanner();
                                    showHideHistory();
                                });
                                const lineSymbol = {
                                    path: google.maps.SymbolPath.FORWARD_CLOSED_ARROW,
                                };
                                const bearingLine = new google.maps.Polyline(
                                    {
                                        path:[
                                            { lat: locations[member].lat, lng: locations[member].lng },
                                            { lat: locations[member].lat, lng: locations[member].lng },
                                        ],
                                        map,
    									strokeColor:locations[member].color,
                                        strokeOpacity:historyGradient(${memberHistoryLength},past,minBearingGradient),
                                        strokeWeight:2.5*${memberHistoryStroke},
			    						fillColor:locations[member].color,
				    					fillOpacity:historyGradient(${memberHistoryLength},past,minBearingGradient),
                                        zIndex:past,
                                        visible:false,
                                        clickable:true,
                                        icons: [{
                                            icon: lineSymbol,
                                            offset: '50%',
                                            repeat: '${memberHistoryRepeat}px',
                                        }]
                                    }
                                );
                                bearingLine.addListener("click", function(event) {
                                    // user selected a history point
                                    infoWindow.close();
                                    infoWindow.setPosition(event.latLng);
                                    infoWindowVisible = bearingLine.zIndex;
                                    infoWindow.setContent(historyContent(locations[member].name, locations[member].history, past, true));
                                    infoWindow.open(bearingLine.map, bearingLine);
                                    historyMember = locations[member].name;
                                    inhibitAutoZoom = true;
                                    updateBottomBanner();
                                    showHideHistory();
                                });
                                history.push({radius,bearingLine});
    						};
                            // save the marker and history
                            markers.push({marker, history});
                        };
                        fitMapBounds();
                        showHideHistory();
                    };


                    function fitMapBounds() {
                        // if a member isn't selected, fit all members
                        if (currentMember == "null") {
                            fitBoundsToMembers();
                        } else {
                            // if a member is selected, and the info window is displaying history
                            if (infoWindowVisible >= 0) {
                                fitBoundsToHistory();
                            } else {
                                // otherwise follow the selected member
                                followLocation();
                            }
                        }
                    };

                    function followLocation() {
                        // refresh the member data
                        currentMember = getMember(currentMember.name);
						// center the map on the member
						center = {};
						center["lat"] = currentMember.lat;
						center["lng"] = currentMember.lng;
						map.setZoom(currentZoom);
						// member is selected, and the window is open
						if (infoWindowVisible == -1) {
							infoWindow.setContent(infoContent(currentMember));
						}
						// member is selected, and the window is closed
						if ((infoWindowVisible < 0) && !inhibitAutoZoom) {
							// recenter the map if the marker is out of bounds, and the history window is closed
							const mapBounds = map.getBounds();
							if (!mapBounds.contains(center)) {
								map.setCenter(center);
							}
						}
                    };

                    function fitBoundsToMembers() {
                        // only fit members if zoom is allowed
                        if (!inhibitAutoZoom) {
                            // check if there are members outside the current view.  This will allow manual zooming
                            const mapBounds = map.getBounds();
                            outsideMapBounds = false;
                            // members that are in range of the search point
                            const distanceBounds = new google.maps.LatLngBounds();
                            withinDistanceBounds = false;
                            // in case no member is in range, center on all
                            const allBounds = new google.maps.LatLngBounds();
                            // default to home
                            distancePosition = homePosition;
                            // check if we passed in a member that we would like bounds around
                            for (let loc=0; loc<markers.length; loc++) {
                                if (paramMember == markers[loc].marker.title.toLowerCase()) {
                                    distancePosition = markers[loc].marker.position;
                                    break;
                                }
                            }

                            // adjust the map bounds to display members
                            for (let loc=0; loc<markers.length; loc++) {
                                allBounds.extend(markers[loc].marker.position);
                                // only apply bounds to members within a geographic radius, or if the check is disabled
                                if (google.maps.geometry.spherical.computeDistanceBetween(distancePosition, markers[loc].marker.position) < ${state?.memberBoundsRadius}*1000) {
                                    withinDistanceBounds = true;
                                    distanceBounds.extend(markers[loc].marker.position);
                                    // check if there is a member off the map
                                    if (!mapBounds.contains(markers[loc].marker.position)) {
                                        outsideMapBounds = true;
                                    }
                                }
                            }

                            if (withinDistanceBounds) {
                                // only fit if a member is off the map but within range
                                if (outsideMapBounds) {
                                    setMapBounds(distanceBounds);
                                }
                            } else {
                                setMapBounds(allBounds);
                            }
                        }
                    };

                    function fitBoundsToHistory() {
						for (let loc=0; loc<markers.length; loc++) {
							if (markers[loc].marker.title == currentMember.name) {
								const bounds = new google.maps.LatLngBounds();
								// adjust the map bounds to display member's history
								for (let past=0; past<markers[loc].history.length; past++) {
									bounds.extend(markers[loc].history[past].radius.center);
								}
								map.fitBounds(bounds);
							}
						}
                    };

                    function setMapBounds(mapBounds) {
                        // set the map bounds, and restrict to a minimum zoom level
                        map.fitBounds(mapBounds);
                        currentZoom = map.getZoom()
                        if (currentZoom > 14) {
                            map.setZoom(14);
                       }
                    };

                    function showHideRegions() {
                        const bounds = map.getBounds();
                        // reduce the region circles opacity if they exceed the viewing window
                        for (let loc=0; loc<regions.length; loc++) {
                            const circleBounds = regions[loc].getBounds();
                            if (bounds.contains(circleBounds.getNorthEast()) && bounds.contains(circleBounds.getSouthWest())) {
                                regions[loc].setOptions({ fillOpacity: 0.17 });
                            } else {
                                regions[loc].setOptions({ fillOpacity: 0.02 });
                            }
                        }
                    };

                    function restoreMarkerIndex() {
                        // assigns an index to a matching member
                        for (let loc=0; loc<locations.length; loc++) {
                            markers[loc].marker.zIndex = locations[loc].zIndex;
                        }
                    };

                    function changeHistoryFocus(member) {
                        // assigns an index to a matching member
                        for (let loc=0; loc<locations.length; loc++) {
                            for (let past=0; past< markers[loc].history.length; past++) {
                                currentIndex = markers[loc].history[past].radius.zIndex;
                                if (markers[loc].marker.title == member.name) {
                                    // select the history
                                    if (currentIndex < maxZIndex) {
                                        markers[loc].history[past].radius.zIndex += maxZIndex;
                                        markers[loc].history[past].bearingLine.zIndex += maxZIndex;
                                    }
                                } else {
                                    // deselect the history
                                    if (currentIndex >= maxZIndex) {
                                        markers[loc].history[past].radius.zIndex -= maxZIndex;
                                        markers[loc].history[past].bearingLine.zIndex -= maxZIndex;
                                    }
                                }
                            }
                        }
                    };

                    function historyGradient(historyLength, historySample, minimumGradient) {
                        var gradient = minimumGradient+((maxGradient-minimumGradient)*(historySample/(historyLength-1)));
                        if (gradient > maxGradient) gradient = maxGradient;
                        return(gradient);
                    };

                    function showHideHistory() {
                        for (let loc=0; loc<markers.length; loc++) {
                            for (let past=0; past<markers[loc].history.length; past++) {
                                if (locations[loc].history[past]?.tst != null) {
                                    tripFocus = false;
                                    // if the member is selected, show all trips if no trip is select, otherwise only show the selected trip
                                    if (tripNumber == locations[loc].history[past].tp) {
                                        tripFocus = true;
                                    }
                                    if (${displayAllMembersHistory}) {
                                        // if no member or trip is selected, display all member history
                                        // if a member and a trip is selected, just display that members trip
                                        // else display all trips for that member
                                        if (((tripMember == "null") && (tripNumber == 0)) || ((markers[loc].marker.title == tripMember) && ((tripNumber == 0) || tripFocus))) {
                                            markers[loc].history[past].radius.setVisible(true);
                                            markers[loc].history[past].bearingLine.setVisible(true);
                                        } else {
                                            markers[loc].history[past].radius.setVisible(false);
                                            markers[loc].history[past].bearingLine.setVisible(false);
                                        }
                                    } else {
                                        if ((markers[loc].marker.title == currentMember.name) && ((tripNumber == 0) || tripFocus)) {
                                            markers[loc].history[past].radius.setVisible(true);
                                            // if the last two markers were the end marker, or the next a beginning, hide the bearing line to the next begin marker
                                            if ((locations[loc].history[past]?.mkr == "${memberBeginMarker}") || ((locations[loc].history[past]?.mkr == "${memberEndMarker}") && (locations[loc].history[past-1]?.mkr == "${memberEndMarker}"))) {
                                                markers[loc].history[past].bearingLine.setVisible(false);
                                            } else {
                                                markers[loc].history[past].bearingLine.setVisible(true);
                                            }
                                        } else {
                                            markers[loc].history[past].radius.setVisible(false);
                                            markers[loc].history[past].bearingLine.setVisible(false);
                                        }
                                    }
                                    // change the radius stroke based on the trip markers
                                    // middle markers and the current begin marker use the default colors
                                    if ((locations[loc].history[past]?.mkr == "${memberMiddleMarker}") || (locations[loc].history[past+1]?.tst == null)) {
                                        markers[loc].history[past].radius.setOptions({
                                            strokeColor:locations[loc].color,
                                            strokeOpacity:historyGradient(${memberHistoryLength},past,(tripFocus ? maxGradient : minRadiusGradient)),
                                            strokeWeight:2,
                                            fillColor:locations[loc].color,
                                            fillOpacity:historyGradient(${memberHistoryLength},past,(tripFocus ? maxGradient : minRadiusGradient)),
                                        });
                                    } else {
                                        if (locations[loc].history[past]?.mkr == "${memberBeginMarker}") {
                                            markers[loc].history[past].radius.setOptions({
                                                strokeOpacity:historyGradient(${memberHistoryLength},past,(tripFocus ? maxGradient : minEndpointStrokeGradient)),
                                                strokeWeight:5,
                                                fillColor:"white",
                                                fillOpacity:1.0,
                                            });
                                        }
                                        if (locations[loc].history[past]?.mkr == "${memberEndMarker}") {
                                            markers[loc].history[past].radius.setOptions({
                                                strokeOpacity:historyGradient(${memberHistoryLength},past,(tripFocus ? maxGradient : minEndpointStrokeGradient)),
                                                strokeWeight:3,
                                                fillColor:"white",
                                                fillOpacity:1.0,
                                            });
                                        }
                                    }
                                    markers[loc].history[past].bearingLine.setOptions({
                                        strokeOpacity:historyGradient(${memberHistoryLength},past,(tripFocus ? maxGradient : minBearingGradient)),
                                        fillOpacity:historyGradient(${memberHistoryLength},past,(tripFocus ? maxGradient : minBearingGradient)),
                                    });
                                } else {
                                    markers[loc].history[past].radius.setVisible(false);
                                    markers[loc].history[past].bearingLine.setVisible(false);
                                }
                            }
                        }
                    };

                    function updateHistoryZoom(zoomLevel) {
                        for (let loc=0; loc<markers.length; loc++) {
                            for (let past=0; past<markers[loc].history.length; past++) {
                                markers[loc].history[past].radius.setRadius(1 + ((Math.pow(2, (22 - zoomLevel))*${memberHistoryScale})/10));
                            }
	                    }
                    };

                    function updateLocations(data) {
                        // check if it is a member update response
						if (data?.members?.length) {
							if (locations.length) {
								// Incoming order may be different due to zIndex changes, so we need to search for matches
                                // first reset so that we can apply the updated zIndex from the incoming data
                                for (let loc=0; loc<locations.length; loc++) {
                                    markers[loc].marker.zIndex = 1
                                }
								for (let mem=0; mem<data.members.length; mem++) {
									for (let loc=0; loc<locations.length; loc++) {
										if (locations[loc].id == data.members[mem].id) {
											// update the location data
											locations[loc] = data.members[mem];
											// update the marker data
											center = {};
											center["lat"] = data.members[mem].lat;
											center["lng"] = data.members[mem].lng;
											markers[loc].marker.position = center;
											// update the past history markers
											for (let past=0; past<data.members[mem].history.length; past++) {
												markers[loc].history[past].radius.setOptions({ center: { lat: data.members[mem].history[past].lat, lng: data.members[mem].history[past].lng }});
												if (past>0) {
													markers[loc].history[past].bearingLine.setPath([ { lat: data.members[mem].history[past-1].lat, lng: data.members[mem].history[past-1].lng }, { lat: data.members[mem].history[past].lat, lng: data.members[mem].history[past].lng } ]);
												}
											}
											// keep the selected member in focus
											if ((markers[loc].marker.title == currentMember.name) || (markers[loc].marker.title == historyMember)) {
												markers[loc].marker.zIndex = maxZIndex;
												// if a history info box is open
												if (infoWindowVisible >= 0) {
													infoWindow.setPosition(markers[loc].history[infoWindowVisible].radius.getCenter());
													infoWindow.setContent(historyContent(markers[loc].marker.title, locations[loc].history, infoWindowVisible, false));
												}
											} else {
    										    markers[loc].marker.zIndex = data.members[mem].zIndex;
											}
										}
									}
									// last person in the list is the one reported the latest location
									lastUpdate = data.members[mem].last;
								}
								showHideHistory();
							} else {
								// if there are no locations, copy the first instance and add the markers
								locations = data.members;
								addMemberMarkers();
								// last person in the list is the one reported the latest location
								lastUpdate = locations[locations.length - 1].last;
							}
    						updateBottomBanner();
                            // recenter the map if necessary
                            fitMapBounds();
						}
                    };

                    function updateBottomBanner() {
                        document.getElementById("id-lastTime").textContent = "Last Update: " + lastUpdate + (currentMember != "null" ? (" | Following: " + currentMember.name) : "") + (inhibitAutoZoom ? " | Auto-Centering Paused" : "");
                    }

                    function retrieveMemberLocations(request) {
                        const dataMap = {};
                        dataMap["request"] = request;
                        dataMap["member"] = paramMember;

                        const postData = {};
                        postData["action"] = "members";
                        postData["payload"] = dataMap;
                        sendDataToHub(postData)
                    };

                    function sendDataToHub(postData) {
                        fetch("${getAttributeURL(request.headers.Host.toString(), "apidata")}", {
                            method: "POST",
                            body: JSON.stringify(postData),
                            headers: {
                                "Content-type": "application/json; charset=UTF-8"
                            }
                        })
                        .then(response => response.json())
                        .then(data => {
                            // check if the web app needs to reload so that it can update
                            if (data.appVersion) {
                                if (data.appVersion != webAppVersion) {
                                    console.log("Reloading Web App");
                                    location.reload();
                                }
                            }
                            // process return data
                            updateLocations(data);
                        })
                        .catch(error => { console.error('Error fetching data:', error); })
                    };

                    // Poll for member data every 5000ms
                    setInterval(() => {
                        retrieveMemberLocations("");
                    }, 5000);
                    // Sync history data in 100ms (one time)
                    setTimeout(() => {
                        retrieveMemberLocations("sync");
                    }, 100);
                };

                function convertMetersToFeet(val) {
                    return (${imperialUnits} ? parseInt(val*3.28084) : parseInt(val))
                }

                function convertKMToMiles(val) {
                    return (${imperialUnits} ? parseInt(val*0.621371) : parseInt(val))
                }

                function getSpeedIcon(val) {
                    if (val == 0)   return("&#129485;")    // standing
                    if (val < 10)   return("&#128694;")    // walking
                    if (val < 30)   return("&#128692;")    // cycling
                    if (val < 200)  return("&#128663;")    // auto
                    return("&#128747;")                    // plane
                }

                function getBearingIcon(val) {
                    if ((val > 22.5)  && (val <= 67.5))  return("&#8599;NE")    // NE
                    if ((val > 67.5)  && (val <= 112.5)) return("&#8594;E")     // E
                    if ((val > 112.5) && (val <= 157.5)) return("&#8600;SE")    // SE
                    if ((val > 157.5) && (val <= 202.5)) return("&#8595;S")     // S
                    if ((val > 202.5) && (val <= 247.5)) return("&#8601;SW")    // SW
                    if ((val > 247.5) && (val <= 292.5)) return("&#8592;W")     // W
                    if ((val > 292.5) && (val <= 337.5)) return("&#8598;NW")    // NW
                    return("&#8593;N")                                          // N
                }

                function infoContent(position) {
                    tripNumber = 0;
                    tripMember = position.name;
                    const contentString =
                    "<table style='width:100%;font-size:1.0em'>" +
                        "<tr>" +
                            "<td align='left'" + (((position.wifi == "0") || (position.hib != "0") || (position.bo != "0") || (position.per != "0")) ? " style='color:red'>" : ">") + (position.wifi != "null" ? (position.wifi == "1" ? "&#128732;" : "<s>&#128732;</s>") : "") + ((location.data == "m") ? "&#128246;" : "") + "</td>" +
                            "<td align='right'" + (position.ps ? " style='color:red'>" : ">") + (position.bs == "2" ? "&#9889;" : "&#128267;") + (position.bat != "null" ? position.bat + "%" : "") + "</td>" +
                        "</tr>" +
                    "</table>" +
                    "<table style='width:100%;font-size:1.0em'>" +
                        "<tr>" +
                            "<td align='left'" + (((position.wifi == "0") || (position.hib != "0") || (position.bo != "0") || (position.per != "0")) ? " style='color:red'>" : ">") + "<b>" + position.name + "</b></td>" +
                            "<td align='right'>" + getBearingIcon(position.cog) + "</td>" +
                        "</tr>" +
                        "<tr>" +
                            "<td align='left'>" + position.loc + "</td>" +
                        "</tr>" +
                        "<tr>" +
                            "<td align='left'>" + "Since: " + position.since + "</td>" +
                        "</tr>" +
                    "</table>" +
                    "<hr>" +
                    "<table style='width:100%;font-size:1.0em'>" +
                        "<tr align='center'>" +
                            (position.dfh != "null" ? "<th width=33%>&#127968;</th>" : "") +
                            "<th width=33%>&#128270;</th>" +
                            "<th width=33%>" + getSpeedIcon(position.spd) + "</th>" +
                        "</tr>" +
                        "<tr align='center'>" +
                            (position.dfh != "null" ? "<td width=33%>" + position.dfh + " ${getLargeUnits()}</td>" : "") +
                            "<td width=33%>" + position.acc + " ${getSmallUnits()}</td>" +
                            "<td width=33%>" + position.spd + " ${getVelocityUnits()}</td>" +
                        "</tr>" +
                    "</table>" +
                    "<hr>" +
                    (position.stale ? "<div style='color:red'>" : "<div>") + "Last: " + position.last + "</div>" +
                    ((position.bo != "0") ? "<div style='color:red'>&#9940;App battery usage: Optimized/Restricted</div>" : "") +
                    ((position.hib != "0") ? "<div style='color:red'>&#9940;Permissions: App can pause</div>" : "") +
                    ((position.per != "0") ? "<div style='color:red'>&#9940;Location permission: Not allowed all the time</div>" : "") +
                    ((position.cmd == 0) ? "<div style='color:red'>&#9940;OwnTracks app setting 'Remote Configuration' is not enabled</div>" : "")

                    return(contentString);
                };

                function formatDateToCustomPattern(date, timeOnly) {
                    const daysOfWeek = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
                    const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

                    const dayOfWeek = daysOfWeek[date.getDay()];
                    const hour = date.getHours();
                    const minute = date.getMinutes();
                    const amPm = hour >= 12 ? "PM" : "AM";
                    const amPmHour = (hour % 12) == 0 ? 12 : (hour % 12)

                    if (timeOnly) {
                        return (amPmHour + ":" + minute.toString().padStart(2, "0") + " " + amPm);
                    } else {
                        return(dayOfWeek + " " + amPmHour + ":" + minute.toString().padStart(2, "0") + " " + amPm + " " + date.getFullYear() + "-" + (date.getMonth() + 1).toString().padStart(2, "0") + "-" + date.getDate().toString().padStart(2, "0"));
                    }
                };

                function getSinceTime(newTime, oldTime) {
                    timeDelta = parseInt((newTime - oldTime) / (60*1000));
                    timeHours = parseInt(timeDelta / 60);
                    timeMinutes = parseInt(timeDelta - (timeHours*60));
                    timeDays = parseInt(timeHours / 24);
                    timeHours = timeHours % 24;
                    // change the return phrase depending on duration
                    if (timeDays == 0) {
                        return(timeHours + " h " + timeMinutes + " m");
                    } else {
                        return(timeDays + " d " + timeHours + " h");
                    }
                };

                function getTripStats(position, index, findFullTrip) {
                    // default to current marker in case there are no beginning markers
                    startTime = new Date(position[index].tst*1000);
                    endTime = new Date(position[index].tst*1000);
                    tripEndFound = false;
                    for (let past=index; past<position.length; past++) {
                        // find the next end marker
                        if (findFullTrip && !tripEndFound && (position[past].mkr == "${memberEndMarker}")) {
                            endTime = new Date(position[past].tst*1000);
                            tripEndFound = true;
                        }
                    }

                    // if the current marker is a begin, then get the time since last trip - reverse the start/end times
                    if (position[index].mkr == "${memberBeginMarker}") {
                        if (index > 0) {
                            startTime = new Date(position[index-1].tst*1000);
                        }
                        tripStatus = "Idle";
                    } else {
                        // start at the current marker, and go backwards until we find the being marker
                        saveBegin = true
                        for (let past=index; past>=0; past--) {
                            if (position[past].mkr == "${memberBeginMarker}") {
                                // store the first begin marker
                                startTime = new Date(position[past].tst*1000);
                                break;
                            }
                        }
                        tripStatus = position[index].odo + " ${getLargeUnits()} Trip #" + position[index].tp;
                    }

                    if ((findFullTrip && !tripEndFound) || (!findFullTrip && (position[index].mkr == "${memberMiddleMarker}"))) {
                        tripEndTime = "...";
                    } else {
                        tripEndTime = formatDateToCustomPattern(endTime, true);
                    }
                    timeDuration = getSinceTime(endTime, startTime);
                    tripTimes = formatDateToCustomPattern(startTime, true) + " - " + tripEndTime + " " + "(" + timeDuration + ")";
                    return[ tripStatus, tripTimes ]
                };

                function historyContent(name, position, index, fullTripStats) {
                    tripNumber = position[index].tp;
                    tripMember = name;
                    historyTime = new Date(position[index].tst*1000);
                    tripStatus = "";
                    tripTimes = "";
                    [tripStatus, tripTimes] = getTripStats(position, index, fullTripStats);
                    const contentString =
                    "<table style='width:100%;font-size:1.0em'>" +
                        "<tr>" +
                            "<td align='left'><b>" + name + "</b></td>" +
                        "</tr>" +
                        "<tr>" +
                            "<td align='left'>" + position[index].loc + "</td>" +
                        "</tr>" +
                        "<tr>" +
                            "<td align='left'>" + "(" + position[index].lat + "," + position[index].lng + ")" + "</td>" +
                        "</tr>" +
                        "<tr>" +
                            "<td align='left'>" + formatDateToCustomPattern(historyTime, false) + "</td>" +
                        "</tr>" +
                    "</table>" +
                    "<hr>" +
                    "<table style='width:100%;font-size:1.0em'>" +
                        "<tr>" +
                            "<td align='left'><b>" + tripStatus + "</b></td>" +
                        "</tr>" +
                        "<tr>" +
                            "<td align='left'>" + tripTimes + "</td>" +
                        "</tr>" +
                    "</table>" +
                    "<hr>" +
                    "<table style='width:100%;font-size:1.0em'>" +
                        "<tr align='center'>" +
                            "<th width=33%>" + getBearingIcon(position[index].cog) + "</th>" +
                            "<th width=33%>&#128270;</th>" +
                            "<th width=33%>" + getSpeedIcon(position[index].spd) + "</th>" +
                        "</tr>" +
                        "<tr align='center'>" +
                            "<td width=33%>" + position[index].cog + "&#176;</td>" +
                            "<td width=33%>" + convertMetersToFeet(position[index].acc) + " ${getSmallUnits()}</td>" +
                            "<td width=33%>" + convertKMToMiles(position[index].spd) + " ${getVelocityUnits()}</td>" +
                        "</tr>" +
                    "</table>" +
                    "<hr>" +
                    "<table style='width:100%;font-size:1.0em'>" +
                        "<tr align='center'>" +
                            "<td width=33%>" + getSinceTime(Date.now(), historyTime) + " ago"  + "</td>" +
                            "<td width=33%>" + "History: " + (position.length - index) + " / " + position.length + "</td>" +
                        "</tr>" +
                    "</table>"

                    return(contentString)
                };
            </script>
            <script src="https://maps.googleapis.com/maps/api/js?key=${APIKey}&loading=async&libraries=marker,maps&callback=initMap"></script>
        </div>"""
    }

    return render(contentType: "text/html", data: (insertOwnTracksFavicon() + htmlData))
}

def isHTTPsURL(url) {
    parsedURL = url?.split(":")

    return(parsedURL[0]?.toLowerCase() == "https")
}

def recorderURLType() {
    // check the recorder URL and switch as necessary
    recorderURL = getRecorderURL()
    // default to local source
    source = URL_SOURCE[1]
    if (recorderURL) {
        if (isHTTPsURL(recorderURL)) {
            // cloud source
            source = URL_SOURCE[0]
        }
    }
    return(source)
}

def generateRecorderFriendsLocation() {
    String htmlData = "OwnTracks Recorder Not Configured"
    if (getRecorderURL()) {
        publicMembers = getEnabledAndNotHiddenMembers()
        urlPath = getRecorderURL() + '/last/index.html'
        htmlData = """
        <div style="width:100%;height:100%;margin:4px;">
            <table align="center" style="width:100%;font-family:arial;padding-top:4px;padding-bottom:5px;">
                <tr>"""
                    publicMembers.each { name-> htmlData += """<td align="center">${insertThumbnailObject(name, 35, true)}</td>"""}
                    htmlData += """
                </tr>
            </table>
            <table align="center" style="width:100%;height:calc(100% - 52px);">
                <tr>
                    <td>
                        <iframe src=${urlPath} style="height:100%;width:100%;border:none;"></iframe>
                    </td>
                </tr>
            </table>
        </div>"""
    }

    return render(contentType: "text/html", data: (insertOwnTracksFavicon() + htmlData))
}

def insertThumbnailObject(memberName, size, embed) {
    if (embed) {
        memberURL = getEmbeddedImage(memberName)
    } else {
        memberURL = getImageURL(memberName)
    }
    return((memberURL ? """<object data="${memberURL}" type="image/jpeg" width="${size}" height="${size}">${memberName}</object>""" : memberName))
}

def displayTile(urlSource, tileSource) {
    String htmlData = ""
    urlPath = getAttributeURL(urlSource, tileSource)

    // create the embedded tile frame
    htmlData += '<div style="width:100%;height:100%">'
    htmlData += "<iframe src=${urlPath} style='width:100%;height:100%;border:none;'></iframe>"
    htmlData += '</div>'
    // the page needs to change on each call, otherwise Hubitat won't push to the dashboard as an attribute change.  So we will embed a hidden random number
    htmlData += '<input type="hidden" value=' + Math.random() + '>'

    return (checkAttributeLimit(htmlData))
}

def checkAttributeLimit(tiledata) {
    // deal with the 1024 byte attribute limit
    if ((tiledata.length() + 11) > 1024) {
        return ("Too much data to display.</br></br>Exceeds maximum tile length by " + ((tiledata.length() + 11) - 1024) + " characters.")
    } else {
        return (tiledata)
    }
}

def generateMemberDeviceStatus() {
    def memberLocations = []
    // loop through all the enabled members
    settings?.enabledMembers.each { enabledMember->
        member = state.members.find {it.name==enabledMember}
        deviceWrapper = getChildDevice(member.id)
        if (deviceWrapper) {
            def memberLocation = [
                "name" :                "${member.name}",
                "SSID" :                "${deviceWrapper?.currentValue("SSID")}",
                "accuracy" :            "${deviceWrapper?.currentValue("accuracy")}",
                "address" :             "${deviceWrapper?.currentValue("address")}",
                "altitude" :            "${deviceWrapper?.currentValue("altitude")}",
                "battery" :             "${deviceWrapper?.currentValue("battery")}",
                "batteryStatus" :       "${deviceWrapper?.currentValue("batteryStatus")}",
                "bearing" :             "${deviceWrapper?.currentValue("bearing")}",
                "dataConnection" :      "${deviceWrapper?.currentValue("dataConnection")}",
                "distanceFromHome" :    "${deviceWrapper?.currentValue("distanceFromHome")}",
                "imperialUnits" :       "${deviceWrapper?.currentValue("imperialUnits")}",
                "lastLocationtime" :    "${deviceWrapper?.currentValue("lastLocationtime")}",
                "lastSpeed" :           "${deviceWrapper?.currentValue("lastSpeed")}",
                "lat" :                 "${deviceWrapper?.currentValue("lat")}",
                "location" :            "${deviceWrapper?.currentValue("location")}",
                "lon" :                 "${deviceWrapper?.currentValue("lon")}",
                "monitoringMode" :      "${deviceWrapper?.currentValue("monitoringMode")}",
                "presence" :            "${deviceWrapper?.currentValue("presence")}",
                "since" :               "${deviceWrapper?.currentValue("since")}",
                "sinceTime" :           "${deviceWrapper?.currentValue("sinceTime")}",
                "sourceTopic" :         "${deviceWrapper?.currentValue("sourceTopic")}",
                "streetAddress" :       "${deviceWrapper?.currentValue("streetAddress")}",
                "switch" :              "${deviceWrapper?.currentValue("switch")}",
                "transitionDirection" : "${deviceWrapper?.currentValue("transitionDirection")}",
                "transitionRegion" :    "${deviceWrapper?.currentValue("transitionRegion")}",
                "transitionTime" :      "${deviceWrapper?.currentValue("transitionTime")}",
                "triggerSource" :       "${deviceWrapper?.currentValue("triggerSource")}",
                "verticalAccuracy" :    "${deviceWrapper?.currentValue("verticalAccuracy")}",
                "wifi" :                "${deviceWrapper?.currentValue("wifi")}",
            ]
            memberLocations << memberLocation
        }
    }

    return(["members" : memberLocations])
}

private def isCloudLinkEnabled(requestURL) {
    if ((requestURL == HUBITAT_CLOUD_URL) && (disableCloudLinks == true)) {
        logWarn("Cloud links are disabled.  Open the Hubitat OwnTracks app, and enable in 'Dashboard Web Links'.")
        return(false)
    } else {
        return(true)
    }
}

mappings {
	path("/webhook") {
    	action: [
            POST: "webhookEventHandler",
            GET:  "webhookGetHandler",        // used for testing through a web browser
        ]
    }
	path("/apidata") {
    	action: [
            POST:  "processAPIData",
        ]
    }
	path("/membercmd/:member/:cmd") {
    	action: [
            GET:  "processMemberAPICommand",
        ]
    }
	path("/googlemap") {
    	action: [
            GET:  "generateGoogleFriendsMap",
        ]
    }
	path("/recordermap") {
    	action: [
            GET:  "generateRecorderFriendsLocation",
        ]
    }
	path("/regionmap/:region") {
    	action: [
            GET:  "generateRegionMap",
        ]
    }
	path("/configmap") {
    	action: [
            GET:  "generateConfigMap",
        ]
    }
	path("/membermap/:member") {
    	action: [
            GET:  "displayMemberMap",
        ]
    }
	path("/memberpresence/:member") {
    	action: [
            GET:  "displayMemberPresence",
        ]
    }
	path("/memberpastlocations/:member") {
    	action: [
            GET:  "displayMemberPastLocations",
        ]
    }
    path("/regionupdate/:region") {
    	action: [
            GET:  "regionUpdate",
        ]
    }
}

def regionUpdate() {
    logWarn "Received 'regionUpdate' ${params.region}"
}

private def webhookGetHandler() {
    if (isCloudLinkEnabled(request.HOST)) {
        logDescriptionText("Web request for member device status.")
        payload = new JsonBuilder(generateMemberDeviceStatus()).toPrettyString()
    } else {
        logDescriptionText("Cloud web links are disabled.")
        payload = []
    }

    return render(contentType: "text/html", data: payload, status: 200)
}

private def syncHttpGet(url) {
    try {
        // limit the timeout since this is a blocking call
        httpGet(uri: url, headers: [timeout: 5]) {
            // return full response:
            //   response.success (true/false)
            //   response.status  (http code)
            //   response.data    (payload)
            response -> result = response
        }
        return (result.data)
    } catch (e) {
        logError(e.message)
        return ("")
    }
}

private def getSectionTitle(sectionState, sectionTitle) {
    return((sectionState ? "&#10134;" : "&#10133;") + " " + sectionTitle)
}

private def getSectionStyle() {
    return("width:100%;font-size:1.0em;text-align:left;color:#ffffff;background-color:${DEFAULT_APP_THEME_COLOR};padding-left:10px;padding-right:10px;margin-top:10px;margin-bottom:10px;border:1px solid #000000;box-shadow:3px 4px #575757;border-radius:1px")
}

private def getFormat(type, myText="", myError="") {
    if (type == "box") return "<div style='color:#ffffff;font-weight:normal;background-color:${DEFAULT_APP_THEME_COLOR};padding:5px;padding-left:10px;margin-right:20px;border:1px solid #000000;box-shadow:3px 4px #575757;border-radius:1px'>${myText}<span style='color:red;padding-left:5px;font-weight:bold'>${myError}</span></div>"
    if (type == "line") return "<hr style='background-color:${DEFAULT_APP_THEME_COLOR}; height:1px;border:0;'/>"
    if (type == "title") return "<h2 style='color:${DEFAULT_APP_THEME_COLOR};font-weight: bold'>${myText}</h2>"
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
