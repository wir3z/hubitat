# Owntracks
OwnTracks app connects your OwnTracks mobile app to Hubitat Elevation for virtual presence triggers

For discussion and more information, visit the Hubitat Community <a href="https://community.hubitat.com/t/release-owntracks/130821" target="_blank">[RELEASE] OwnTracks</a>.


## Hubitat Installation and Setup
1. Click 'Drivers Code' and the '+ New driver' button on the top right of the screen.
2. Paste the driver code into the screen, and click 'Save' in the top right of the screen.
3. Click 'Apps code' and the '+ New app' button on the top right of the screen.
4. Paste the app code into the screen, and click 'Save' on the top right of the screen.
5. Click the 'OAuth' button on the top right of the screen, leave all fields default and click 'Update'.

## Mobile App Configuration
1.  This integration requires the Owntracks app to be installed on your mobile device:  https://owntracks.org/

    - Open the OwnTracks app on the mobile device, and configure the following fields:
	    ### Android
        - Preferences -> Connection
            - Mode -> HTTP
            - Host -> Hubitat Cloud API link - click the 'Mobile App Installation Instructions' box in the Hubitat Owntracks App to get the link
            - Identification ->
                - Username -> Name of the particular user
                - Password -> Not Used
                - Device ID -> Optional extra descriptor (IE: 'Phone').  If using OwnTracks recorder, it would be desirable to keep this device ID common across device changes, since it logs 'username/deviceID'.
                - Tracker ID -> Not Used
            - Preferences -> Advanced
                - Remote commands -> Selected
                - Remote configuration -> Selected
	    ### iOS
        - Tap (i) top left, and select 'Settings'
            - Mode -> HTTP
            - URL -> Hubitat Cloud API link - click the 'Mobile App Installation Instructions' box in the Hubitat Owntracks App to get the link
			- User ID -> Name of the particular user
			- cmd -> Selected
			
    - Click the 'Manual Update' button in the app to register the device with the Hubitat App.  

## Hubitat App Configuration
Open the Owntracks app.

1. Installation
	- Mobile App Installation Instructions:  
		- Lists the 'Mobile App Configuration', above, with the Cloud API link.
	- Select family member(s):  
		- Once a mobile device has connected to the Cloud API link, it will be populated in this list, but is disabled.  Select the user to enable presence detection.
    - Display a warning in the logs if a family member reports a location but is not enabled:
		- If enabled, a warning will be disabled each time a member reports a location, but is not selected.
    - Display a warning in the logs if a family member app settings are not configured for optimal operation:
		- If enabled, a warning will be displayed each time a user reports a location with non-optimal phone settings.
	- Display imperial units instead of metric units
		- If enabled, all units in the Hubitat app and driver will be displayed in imperial units (mph, mi, ft) instead of metric units (kph, km, m)
	- Select your 'Home' place. Use '[Hubitat Location]' to enter a location: 
		- Select the region where 'Home' is for presence detection.  
		- Use '[Hubitat Location]' to use the hub location.  Enter a name for "home" as well as the geofence in meters.
	- Select your 'Home' WiFi SSID (optional).  Used to prevent devices from being 'non-present' if currently connected to this WiFi access point.
	- Enable high accuracy reporting when location is between region radius and this value:
		- When a user is between the home geofence radius and this radius, the app will switch the user to use high accuracy/high frequency reporting to ensure presence triggers operate correctly.
	- High accuracy reporting is used for home region only when selected, all regions if not selected
		- If selected, then high accuracy reporting only occurs in the geofence around home.  If deselected, high accuracy reporting will be used on all regions.  Note:  This will increase battery consumption.
    - Child device will ignore locations if the accuracy is greater than the given meters		
		- Adds a 2-tier location accuracy filter.  A larger value is configured in the mobile app to ensure it calls home, a smaller value is configured here to prevent false presence triggers due to bad accuracy
	- Enable recorder
		- Add the link to the optional Owntracks recorder:  https://owntracks.org/booklet/clients/recorder/
		- For user cards to be used in the recorder, a JSON user card needs to be saved to: 'STORAGEDIR/cards/USER/USER.json' on the recorder, where 'USER' is the member name.
		- A user card can be created by:
			- Enable debug logging for the OwnTracks app, trigger a manual location, and save the JSON from the debug message of: 
			  'OwnTracks: For recorder cards, save this JSON to 'STORAGEDIR/cards/...' to a file named 'USER.json'.  

2. Mobile App Configuration
	NOTE: For settings to be sent to the device, 'Remote configuration' must be enabled in the mobile app.
	- Select family member(s) to update location, display and region settings on the next location update
		- Selected users will get pushed region, location and display settings on their next location update.
	- Select family member(s) to restart their mobile app on next location update
		- Selected users will have their mobile app reset on their next location update.
	a. Regions
		- Update all user's mobile app regions settings on next location update
			- All the regions will be sent to all devices as they report their location
		- Select family member(s) to retrieve their region list on next location update
			- Select user(s) that you wish to retrieve their regions when they report their location.
		- Add Regions:
			- Enter the name, detection radius and coordinates.  Click on the screen to expose the 'Save' button.
			- Click the 'Save' button.
			- Click the 'Next' button to leave the screen, or abandon changes if 'Save' was not pressed.
		- Edit Regions:
			- Select the region to edit.
			- Click the 'Save' button.
			- Click the 'Next' button to leave the screen, or abandon changes if 'Save' was not pressed.
		- Delete Regions:
			- Select the region(s) to delete.  Click on the screen to expose the 'Delete' button.
			- Click the 'Delete' button.
			- Click the 'Next' button to leave the screen, or abandon changes if 'Delete' was not pressed.
			NOTE:  Android Build 420412000 does not delete the region remotely.  Their name will be changed to '-DELETED-', and will need to be manually deleted from each device.
	b. Location
		- Update all user's mobile app location settings on next location update
			- All the location settings will be sent to all devices as they report their location
		- Using the defaults gives best balance of battery life and accuracy.  If 'Enable high accuracy reporting when location is between region radius and this value' was enabled on the main screen,
			the device will be switched to 'HIGH_POWER' mode with 'Request that the location provider updates no faster than the requested locater interval' when the mobile device is within that region.
	c. Display
		- Update all user's mobile app display settings on next location update
			- All the display settings will be sent to all devices as they report their location
		- Display user thumbnails on the map.  Needs to have a 'user.jpg' image of maximum resolution 192x192 pixels uploaded to the 'Settings->File Manager'
			- Sends back each user's thumbnail picture when the send their location report.
		- Replace the 'TID' (tracker ID) with 'username' for displaying a name on the map and recorder
			- Pushes the user name back as the tracker ID to allow the user's names to be shown on each device map.
		- Notify about received events
		- Include extended data in location reports
			- Returns extra information with each location report (BSSID, SSID, Battery Status, etc.)
		- Allow the map to be rotated
		- Display the region pins/bubbles on the map
		- Show last reported location in ongoing notification banner
		- Display Geocoder errors in the notification banner
		
3. Logging
- Enable logging.

4. Delete family member(s)
- Deletes selected family members from the app and their corresponding child device.  Ensure no automations are dependent on their device before proceeding.

## Hubitat Driver Configuration
- Once a user has been enabled in the app, a device with the name 'Owntracks - USERNAME' will be created.
- What is displayed on the presence tile battery field:
	- Select what is displayed in the 'battery' field which is displayed at the top of the presence tile from the pull-down menu. This can be battery voltage, location, distance from home, etc.
- Display extended location attributes:
	- Displays additional location attributes (latitude, longitude, accuracy, etc.)
- Enable Logging of location changes:
	- Logs an entry when a change in user location occurs

