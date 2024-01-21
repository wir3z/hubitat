# Owntracks
OwnTracks app connects your OwnTracks mobile app to Hubitat Elevation for virtual presence triggers

For discussion and more information, visit the Hubitat Community <a href="https://community.hubitat.com/t/release-owntracks/130821" target="_blank">[RELEASE] OwnTracks for Hubitat Presence Detection</a>.

The goal of the integration was to create a central management application for the OwnTracks app.  Mobile app settings and regions can be modified in the Hubitat app and then sent to the mobile app once they report a location.
The main app page contains a 'Member Status' table that displays location health of each member.

## Hubitat Installation
### Automatic Install
Locate 'OwnTracks' in the Hubitat Package Manager to install the application and driver.
### Manual Install
1. Click 'Drivers Code' and the '+ New driver' button on the top right of the screen.
2. Paste the driver code into the screen, and click 'Save' in the top right of the screen.
3. Click 'Apps code' and the '+ New app' button on the top right of the screen.
4. Paste the app code into the screen, and click 'Save' on the top right of the screen.
5. Click the 'OAuth' button on the top right of the screen, leave all fields default and click 'Update'.

### First Time Opening the OwnTracks App
The OwnTracks app needs to be installed into Hubitat before the URL required for locations can be generated.  Click the 'Done' button, and then re-open the OwnTracks app to finish configuration.

## Quick Start Guide
To get presence detection working, three things need to be configured:
1. Install and configure the OwnTracks Mobile app to point to Hubitat
2. Configure a 'Home' region that will be used to determine if the phone is present or not present.
3. Select the family member(s) to monitor their presence.

The defaults in the app should give acceptable results without changes.  It is recommended to configure the sections in the 'Installation' portion of the app and test presence detection before changing additional settings.

Open the Hubitat OwnTracks app, and configure the the sections in 'Installation', below.
	
	
# Owntracks App
# Member Status
- Summary table for all users
	- If the member has reported recently, it is displayed in green.
	- If the member has not reported a location in the set hours, it is displayed in red.
	- If the member is disabled, it will be displayed in grey with a strike through.
- Last Location Report
	- Date/time for the last update from a member.  If the member hasn't reporting after the set number of hours, it is displayed in red, with the number of hours since the last report.
- Last Location Fix
	- Date/time for the last location fix from a member.  If the member hasn't updated it's location after the set number of hours, it is displayed in red, with the number of hours since the last fix.
- The columns indicate the current pending requests for each member.  If 'Pending' is displayed, that user will get updated on the next location report.

# Installation
## Mobile App Configuration
This integration requires the Owntracks app to be installed on your mobile device:  https://owntracks.org/
NOTE:  If you reinstall the OwnTracks app on Hubitat, the host URL below will change, and the mobile devices will need to be updated.
	   This integration currently only supports one device per user for presence detection.  Linking more than one device will cause unreliable presence detection.

- Open the OwnTracks app on the mobile device, and configure the following fields.  Only the settings below need to be changed; leave the rest as defaults.
	### Android
	- Preferences -> Connection
		- Mode -> HTTP
		- Host -> Hubitat Cloud API link - click the 'Mobile App Installation Instructions' box in the Hubitat Owntracks App to get the link
		- Identification ->
			- Username -> Name of the user's phone that you would like to see on the maps (IE: 'Kevin')
			- Device ID -> Optional extra descriptor (IE: 'Phone').  If using OwnTracks recorder, it would be desirable to keep this device ID common across device changes, since it logs 'username/deviceID'.
		- Preferences -> Advanced
			- Remote commands -> Selected
			- Remote configuration -> Selected
	### iOS
	- Tap (i) top left, and select 'Settings'.
		- Mode -> HTTP
		- DeviceID -> 2-character user initials that will be displayed on your map (IE: 'KT').  If using OwnTracks recorder, it would be desirable to keep this device ID common across device changes, since it logs 'username/deviceID'.			
		- UserID -> Name of the user's phone that you would like to see on the maps (IE: 'Kevin')
		- URL -> Hubitat Cloud API link - click the 'Mobile App Installation Instructions' box in the Hubitat Owntracks App to get the link
		- cmd -> Selected
		
- Click the up arrow button in the top right of the map to trigger a 'Send Location Now' to register the device with the Hubitat App.  

## Configure Regions
- Select region to check coordinates.
	- Select a region from the selection box, and click the link to view the pin on Google Maps to confirm the latitude and longitude is correct.  Right click on Google Maps to get the lat/lon.
- Add Regions:
	- Enter the name, detection radius and coordinates.  Click on the screen to expose the 'Save' button.
	- Click the 'Save' button.
	- Click the 'Next' button to leave the screen, or abandon changes if 'Save' was not pressed.
- Edit Regions:
	- Select the region to edit.
	- Click the 'Save' button.
	- Click the 'Next' button to leave the screen, or abandon changes if 'Save' was not pressed.
	- NOTE: Changing the 'Region Name' will create a new region on iOS devices.  The previous named region will need to be manually delete from each device.
- Delete Regions:
	- Select Delete region from Hubitat Only and not from mobile devices to only remove the region from the Hubitat app:
		- regions will need to be manually removed from each mobile device.
	- Select the region(s) to delete.  Click on the screen to expose the 'Delete' button.
	- Click the 'Delete' button.
	- Click the 'Next' button to leave the screen, or abandon changes if 'Delete' was not pressed.
	NOTE:  The region will remain in the Hubitat OwnTracks app, until all users have reported a location to retrieve the deletion notice.
		   Android v4.2.12 does not delete the region remotely and will need to be manually deleted from each device.

- Select your 'Home' place:
	- Select the region where 'Home' is for presence detection.  
	- If the list is empty select 'Add Regions', above, to create a home location.
	- When the member is within the region radius, they will be 'present'.  When they are outside this radius, they will be 'not present'.
	- Click the link to view the pin on Google Maps to confirm the latitude and longitude is correct.  Right click on Google Maps to get the lat/lon.
		   
- Hubitat can retrieve regions from a member's OwnTracks mobile device and merge them into the Hubitat region list. 
	- If member(s) OwnTracks app already has configured regions, select member(s) that you wish to retrieve their regions when they report their location.
	- Those regions will be added to the Hubitat region list to be shared with other members.
	
NOTE: A region named '+follow' is automatically created to allow iOS phones to have transition reporting.  	

## Select Family Member(s) to Monitor
- Select family member(s):  
	- Once a mobile device has connected to the URL link from 'Mobile App Configuration', it will be populated in this list, but is disabled.  Select the user to enable presence detection.

## Select family member(s) to remain private. 
- Select family member(s):  
	- Locations and regions will NOT be shared with other members or the Recorder. 
	- Other member locations will not be shared with private members.
	- Only the 'Home' region will be pushed to private members.
	- Their Hubitat device will only display presence information.
	- If using the secondary hub connection, you will need to select these members as private on that hub.
	
## Display Units
- Select the slider to display all measurements in the Hubitat app in imperial units (mph, mi, ft) instead of metric units (kph, km, m)
- All distances are stored in metric and may have minor rounding errors when converted to imperial for display.
NOTE: The OwnTracks mobile app stores and displays all units in metric.
		
## Additional Hubitat App Settings
- Enter your 'Home' WiFi SSID(s), separated by commas (optional):
	- This will prevent devices from being 'non-present' if currently connected to these WiFi access points.

The defaults for the rest of these settings should be sufficient for verifying operation.
- Restore Defaults
	- Resets the settings to the recommended defaults
- Enable high accuracy reporting when location is between region radius and this value:
	- When a user is between the home geofence radius and this radius, the app will switch the user to use high accuracy/high frequency reporting to ensure presence triggers operate correctly.
- High accuracy reporting is used for home region only when selected, all regions if not selected:
	- If selected, then high accuracy reporting only occurs in the geofence around home.  If deselected, high accuracy reporting will be used on all regions.  Note:  This will increase battery consumption.
- Highlight members on the 'Member Status' that have not reported a location for this many hours (1..168):
	- If a member's has not reported a location, or it's location position fix has been greater than this value, they will be highlighted in red on the table
- Display a warning in the logs if a family member reports a location but is not enabled:
	- If enabled, a warning will be disabled each time a member reports a location, but is not selected.
- Display a warning in the logs if a family member app settings are not configured for optimal operation:
	- If enabled, a warning will be displayed each time a user reports a location with non-optimal phone settings.
- Request a high accuracy location from members on their next location report after a ping/manual update to keep location fresh (Android ONLY):
	- If selected, members reporting stale locations will be requested to send a high accuracy location on their next location report
- Automatically request a high accuracy location from members on their next location report if their 'Last Location Fix' is stale (Android ONLY)
	- If selected, members reporting a ping or manual location will be requested to send a high accuracy location on their next location report


# Optional Features
## Enabling User Thumbnails:  
- Directions to create thumbnails for the mobile app and recorder.
- Display user thumbnails on the map.  Needs to have a 'user.jpg' image uploaded to the 'Settings->File Manager'
	- Maximum resolution 192x192 pixels 
	- Maximum file size of 45kB
	- Sends back each user's thumbnail picture when the send their location report.		
## Enable OwnTracks Recorder
- The optional Owntracks recorder:  https://owntracks.org/booklet/clients/recorder/ can be installed for local tracking.
- HTTP URL of the OwnTracks Recorder will be in the format 'http://enter.your.recorder.ip:8083/pub', assuming using the default port of 8083.
- Follow the directions on the page 'Installing OwnTracks Recorder and Configuring User Card Instructions' to install OwnTrack Recorder and configure user cards.
- When 'Enable location updates to be sent to the Recorder URL' is selected, incoming mobile locations are mirrored to the above URL. 
## Link Secondary Hub
- Allows location updates to be sent to a secondary hub running the OwnTracks app.
- Enter the host URL of the Seconday Hub from the OwnTracks app 'Mobile App Installation Instructions' page.
- When 'Enable location updates to be sent to the secondary hub URL' is selected, incoming mobile locations are mirrored to the above URL. 


# Advanced Mobile App Settings
The default mobile settings provide the best balance of accuracy/power.  To view or modify advanced settings, enable the 'Modify Default Settings' slider.	
NOTE: For settings to be sent to the device, 'Remote configuration' (Android) or 'cmd' (iOS) must be enabled in the mobile app.

- Use GPS for higher accuracy/performance.  NOTE: This will consume more battery but will offer better performance in areas with poor WiFi/Cell coverage. (Android ONLY)
- Modify Default Settings
	- Display the 'Mobile App Location Settings' and 'Mobile App Display Settings'
	
## Mobile App Location Settings	
- Restore Defaults
	- Resets the settings to the recommended defaults
- Using the defaults gives best balance of battery life and accuracy.  If 'Enable high accuracy reporting when location is between region radius and this value' was enabled on the main screen,
	the device will be switched to 'HIGH_POWER' mode with 'Request that the location provider updates no faster than the requested locater interval' when the mobile device is within that region.
- Location reporting mode
	- 'significant' - location updates are based on distance moved and time elapsed.  Best balance for battery consumption and performance.
	- 'move' - Continuously reports locations based on the configured interval.  Will result in much higher battery consumption.
- Do not send a location if the accuracy is greater than the given distance
	- Prevents the phone from returning locations if the accuracy is larger than the entered distance
- Number of days after which location updates from friends are assumed stale and removed
	- If a member stops reporting locations to your mobile phone, they will be marked as stale after this many days
- Device will send a location interval at this heart beat interval (minutes) (Android Only)
	- The mobile will report a location on intervals of this many minutes, regardless if it is moving
- Request that the location provider deliver updates no faster than the requested locater interval
	- Reduces mobile data communications and battery consumption by throttling updates to the locater interval time
- How far the device travels before receiving another location update (Android Only)
	- If the phone has moved further than this distance in the alloted time, it will report a location
- Device will not report location updates faster than this interval (seconds) unless moving
	- Location updates will be reported on this interval unless the mobile is moving
- How often should locations be continuously sent from the device while in 'Move' mode (seconds) 
    - Interval for the mobile to report a location when in 'move' mode.  NOTE:  Faster the updates, the higher the battery consumption.
	
## Mobile App Display Settings	
- Restore Defaults
	- Resets the settings to the recommended defaults
- Replace the 'TID' (tracker ID) with 'username' for displaying a name on the map and recorder
	- Pushes the user name back as the tracker ID to allow the user's names to be shown on each device map.
- Notify about received events
- Include extended data in location reports
	- Returns extra information with each location report (BSSID, SSID, Battery Status, etc.)
- Allow the map to be rotated
- Display the region pins/bubbles on the map
- Show last reported location in ongoing notification banner
- Display Geocoder errors in the notification banner

	
# Maintenance
- Select family member(s) to update location, display and region settings on the next location update:
	- The user will be registered to receive this update once 'Done' is pressed.
- Select family member(s) to send a high accuracy GPS location on next location update (Android ONLY): 
	- The user will be registered to receive this request once 'Done' is pressed.
- Reset to Recommended Default Settings
	NOTE:  Members, Regions, Recorder and Secondary Hub settings will not be deleted.
	- Restore Defaults for All Settings - resets the 'Additional Hub Settings', 'Mobile App Location Settings', and 'Mobile App Display Settings' to the recommended defaults.

	- Restore Defaults for 'Additional Hub Settings' to the recommended defaults.
	- Restore Defaults for 'Mobile App Location Settings' to the recommended defaults.
	- Restore Defaults for 'Mobile App Display Settings' to the recommended defaults.
- Delete Family Members:
	- Deletes selected family members from the app and their corresponding child device.  Ensure no automations are dependent on their device before proceeding.

		
# Logging
- Enable Descriptive Text Logging:
	- Displays periodic messages in the log.  Recommended to be enabled.
- Enable Debug Logging:
	- Displays verbose debug messages in the log for troubleshooting.  Recommended to be disabled.
- Turn off debug logging after this many hours (1..24):
	- Disables debug logging after this many hours elapses

		
		
# Hubitat Driver Configuration
- Once a user has been enabled in the app, a device with the name 'Owntracks - USERNAME' will be created.
- What is displayed on the presence tile battery field:
	- Select what is displayed in the 'battery' field which is displayed at the top of the presence tile from the pull-down menu. This can be battery voltage, location, distance from home, etc.
- Display extended location attributes:
	- Displays additional location attributes (battery status, altitude, accuracy, etc.)
- Enable Logging of location changes:
	- Logs an entry when a change in user location occurs

- Main Attribute Description
	- BSSID : MAC address of the WiFi connected WiFi access point
	- SSID : SSID of the WiFi connected WiFi access point
	- accuracy : accuracy of the location
	- altitude : altitude of the location
	- battery : by default, battery % but can be configured to display different information to be viewed on the presence tiles
	- batteryPercent : battery %
	- batteryStatus : Unknown/Unplugged/Charging/Full
	- dataConnection : WiFi/Mobile
	- distanceFromHome : distance location is from the home coordinates
	- imageURL : URL for the members image card
	- lastLocationtime : timestamp for the last location - this updates on each incoming location
	- lat : latitude of the location
	- location : region the user is currently in, or the lat/lon if outside a region
	- lon : longitude of the location
	- monitoringMode : Significant/Move
	- presence : present/not present
	- since : timestamp on the last location change - only updates if the location has moved
	- sourceTopic : user/device information
	- status : region the user is in, or the distance from home if outside a region
	- transition : region the user arrived/left and the time of the transition
	- triggerSource : Ping/Region/Report Location/Manual/Beacon/Timer/Monitoring/Location
	- verticalAccuracy : vertical accuracy
- Additional Attribute Description for APK 2.4.16.  
	- wifi : on/off
	- batterySaver : 0/1, 1 indicates the phone is in battery saver mode
	- hiberateAllowed : 0/1, 1 indicates the OwnTracks app can pause if unused for a period of time
	- batteryOptimizations : 0/1, 1 indicates that the OwnTracks app has battery optimizations on which will impact performance
	- locationPermissions : 0/1, 1 indicates the location permission settings are configured in a way that will impact performance
