package org.owntracks.android.preferences

import android.os.Build
import java.util.Locale
import kotlin.reflect.KProperty
import org.owntracks.android.BuildConfig
import org.owntracks.android.preferences.types.AppTheme
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.preferences.types.MqttProtocolLevel
import org.owntracks.android.preferences.types.MqttQos
import org.owntracks.android.preferences.types.StringMaxTwoAlphaNumericChars

interface DefaultsProvider {
  @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
  fun <T> getDefaultValue(preferences: Preferences, property: KProperty<*>): T {
    return when (property) {
      // Injected from the HUBITAT_HTTP_PASSWORD CI/CD variable, "" when absent.
      // Never hardcode it: this repo has a public upstream. Placed up here, not
      // beside the password line, because hubitat-code inserts an override next
      // to that line and the two layers would then conflict.
      Preferences::password -> BuildConfig.DEFAULT_PASSWORD // supersede the variable below
      Preferences::autostartOnBoot -> true
      Preferences::cleanSession -> false
      Preferences::clientId ->
          (preferences.username + preferences.deviceId)
              .replace("\\W".toRegex(), "")
              .lowercase(Locale.getDefault())
      Preferences::connectionTimeoutSeconds -> 30
      Preferences::debugLog -> false
      Preferences::deviceId -> "Phone" // supersede the variable below
      Preferences::deviceId ->
          Build.DEVICE?.replace(" ", "-")
              ?.replace("[^a-zA-Z0-9]+".toRegex(), "")
              ?.lowercase(Locale.getDefault()) ?: "unknown"
      Preferences::discardNetworkLocationThresholdSeconds -> 0
      Preferences::dontReuseHttpClient -> false
      Preferences::enableMapRotation -> true
      Preferences::encryptionKey -> ""
      Preferences::experimentalFeatures -> emptySet<String>()
      Preferences::fusedRegionDetection -> true
      Preferences::firstStart -> true
      Preferences::host -> ""
      Preferences::allowIntentControl -> false
      Preferences::intentAuthKey -> java.util.UUID.randomUUID().toString()
      Preferences::allowConfigurationByURIAndConfigFile -> false
      Preferences::ignoreInaccurateLocations -> 50 // supersede the variable below
      Preferences::ignoreInaccurateLocations -> 0
      Preferences::ignoreStaleLocations -> 7.0f // supersede the variable below
      Preferences::ignoreStaleLocations -> 0f
      Preferences::info -> true
      Preferences::keepalive -> 3600
      Preferences::locatorDisplacement -> 50 // supersede the variable below
      Preferences::locatorDisplacement -> 500
      Preferences::locatorInterval -> 60
      Preferences::locatorPriority -> null
      Preferences::useGNSSInSignificantMonitoringMode -> false
      Preferences::maxImplausibleSpeedKmh -> 0
      Preferences::mode -> ConnectionMode.HTTP // supersede the variable below
      Preferences::mode -> ConnectionMode.MQTT
      Preferences::monitoring -> MonitoringMode.Significant
      Preferences::moveModeLocatorInterval -> 30 // supersede the variable below
      Preferences::moveModeLocatorInterval -> 10
      Preferences::mqttProtocolLevel -> MqttProtocolLevel.MQTT_3_1
      Preferences::notificationEvents -> true
      Preferences::notificationGeocoderErrors -> false // supersede the variable below
      Preferences::notificationGeocoderErrors -> true
      Preferences::notificationHigherPriority -> false
      Preferences::notificationLocation -> false // supersede the variable below
      Preferences::notificationLocation -> true
      Preferences::opencageApiKey -> ""
      Preferences::osmTileScaleFactor -> 1.0f
      Preferences::password -> ""
      Preferences::pegLocatorFastestIntervalToInterval -> true // supersede the variable below
      Preferences::pegLocatorFastestIntervalToInterval -> false
      Preferences::ping -> 15
      Preferences::port -> 8883
      Preferences::extendedData -> true
      Preferences::pubQos -> MqttQos.One
      Preferences::pubRetain -> true
      Preferences::pubTopicBase -> "owntracks/%u/%d"
      Preferences::publishLocationOnConnect -> true // supersede the variable below
      Preferences::publishLocationOnConnect -> false
      Preferences::cmd -> true
      Preferences::remoteConfiguration -> true // supersede the variable below
      Preferences::remoteConfiguration -> false
      Preferences::setupCompleted -> false
      Preferences::showRegionsOnMap -> true // supersede the variable below
      Preferences::showRegionsOnMap -> false
      Preferences::sub -> true
      Preferences::subQos -> MqttQos.Two
      Preferences::subTopic -> DEFAULT_SUB_TOPIC
      Preferences::theme -> AppTheme.Auto
      Preferences::tls -> true
      Preferences::tlsClientCrt -> ""
      Preferences::tid ->
          StringMaxTwoAlphaNumericChars(preferences.deviceId.takeLast(2).ifEmpty { "na" })
      // Injected at build time from the HUBITAT_PRIVATE_URL CI/CD variable, and "" for
      // any build that did not supply one - which is the upstream default, so a
      // build without secrets behaves exactly as stock. Never hardcode a real
      // URL here: ours carries an access token, and this repo has a public
      // upstream. See ownTracksUrl in app/build.gradle.kts.
      Preferences::url -> BuildConfig.DEFAULT_URL
      Preferences::userDeclinedEnableLocationPermissions -> false
      Preferences::userDeclinedEnableBackgroundLocationPermissions -> false
      Preferences::userDeclinedEnableLocationServices -> false
      Preferences::userDeclinedEnableNotificationPermissions -> false
      Preferences::username -> ""
      Preferences::ws -> false
      else -> {
        throw Exception("No default defined for ${property.name}")
      }
    }
        as T
  }

  companion object {
    const val DEFAULT_SUB_TOPIC = "owntracks/+/+"
  }
}
