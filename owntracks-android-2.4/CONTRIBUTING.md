## Building
To build a development version of the app from source, follow the instructions outlined below.

1. Download and install [Android Studio](http://developer.android.com/sdk/index.html)
2. Go to the codebase:  https://github.com/wir3z/owntracks-android/tree/v2.4
3. Click the green "<> Code" button, and download zip to the project on to your local development machine.  Unzip the folder to 'C:\owntracks-android'
4. Select 'File -> Open' and browse to to the unzipped file location.  Select the 'C:\owntracks-android\project' subfolder
5. Select 'File -> Sync Project with Gradle files' to download all the dependencies
    **NOTE:** Only upgrade Gradle to 7.4.2.  Version 8.x.x is not compatible with this code base.
6. Select 'Tools->SDK manager', and on the side menu of that window, select 'Android SDK'. Check the box for Android 14.
7. Select 'Build -> Rebuild Project'
8. Select 'View -> Tool Windows -> Logcat'
9. Select 'Run -> Run App'
10. When the app first starts, it will generate an exception logged on startup of the application fingerprint.  It will look like this ```BC:CF:16:C8:4B:5E:5D:2D:DA:B7:35:FF:2A:53:CF:89:83:C2:D9:65;org.owntracks.android.debug```
11. To get the Google Maps functionality working, you'll need a Google Maps API Key. Builds will work without it, but you won't see any map data on the main activity, and you will also see an exception logged on startup. To set the API key:
	1. Go go to https://developers.google.com/maps/documentation/androidA/start
	2. Scroll down to Obtain a Google Maps API key and follow the instructions (currently it's "Step 4. Set up a Google Maps API key")
    3. If you want to restrict your API key to the android app, you'll need to provide the fingerprint. This is the unique fingerprint found in the logcat logs in step 8.
    4. Change the settings to match the following:
    5. It can take several hours for the API key to become active.  Be patient!
12. Edit the 'C:\owntracks-android\project\project\app\gradle.properties' file.
13. Change 'google_maps_api_key=' to have your Google Maps API key
14. Select 'Build -> Rebuild Project'
15. Select 'Build -> Build Bundle(s)/APK(s) -> Build APK(s)'
16. If successful, the APK is located here 'C:\owntracks-android\project\app\build\outputs\apk\gms\debug\app-gms-debug.apk'
17. Copy this APK to your Android device, and install.
