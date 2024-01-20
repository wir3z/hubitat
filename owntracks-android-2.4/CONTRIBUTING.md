## Building
To build a development version of the app from source, follow the instructions outlined below.

1. Download and install [Android Studio](http://developer.android.com/sdk/index.html)
2. Click the green "<> Code" button, and download zip to the project on to your local development machine.  Unzip the folder to 'C:\owntracks-android'
3. Select 'File -> Open' and browse to to the unzipped file location.  Select the 'C:\owntracks-android\project' subfolder
4. Select 'File -> Sync Project with Gradle files' to download all the dependencies
5. Select 'Build -> Rebuild Project'
6. Select 'View -> Tool Windows -> Logcat'
7. Select 'Run -> Run App'
8. When the app first starts, it will generate an exception logged on startup of the application fingerprint.  It will look like this ```BC:CF:16:C8:4B:5E:5D:2D:DA:B7:35:FF:2A:53:CF:89:83:C2:D9:65;org.owntracks.android.debug```
9. To get the Google Maps functionality working, you'll need a Google Maps API Key. Builds will work without it, but you won't see any map data on the main activity, and you will also see an exception logged on startup. To set the API key:
	1. Go go to https://developers.google.com/maps/documentation/androidA/start
	2. Scroll down to Obtain a Google Maps API key and follow the instructions (currently it's "Step 4. Set up a Google Maps API key")
	3. If you want to restrict your API key to the android app, you'll need to provide the fingerprint. This is the unique fingerprint found in the logcat logs in step 8.
	4. Create or edit the '\project\app\gradle.properties' file.
	5. Change 'google_maps_api_key=' to have your API key. 
10. Select 'Build -> Rebuild Project'
11. Select 'Build -> Build Bundle(s)/APK(s) -> Build APK(s)'
12. If successful, the APK is located here 'C:\owntracks-android\project\app\build\outputs\apk\gms\debug\app-gms-debug.apk'
13. Copy this APK to your Android device, and install.
