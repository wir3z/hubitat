package org.owntracks.android.preferences

import android.os.Build
import java.util.Locale
import kotlin.reflect.KProperty
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
      Preferences::autostartOnBoot -> true
      Preferences::cleanSession -> false
      Preferences::clientId ->
          (preferences.username + preferences.deviceId)
              .replace("\\W".toRegex(), "")
              .lowercase(Locale.getDefault())
      Preferences::connectionTimeoutSeconds -> 30
      Preferences::debugLog -> false
	  Preferences::deviceId -> "Phone"
      Preferences::discardNetworkLocationThresholdSeconds -> 0
      Preferences::dontReuseHttpClient -> false
      Preferences::enableMapRotation -> true
      Preferences::encryptionKey -> ""
      Preferences::experimentalFeatures -> emptySet<String>()
      Preferences::fusedRegionDetection -> true
      Preferences::firstStart -> true
      Preferences::host -> ""
      Preferences::ignoreInaccurateLocations -> 50
      Preferences::ignoreStaleLocations -> 7.0f
      Preferences::info -> true
      Preferences::keepalive -> 3600
      Preferences::locatorDisplacement -> 50
      Preferences::locatorInterval -> 60
      Preferences::locatorPriority -> null
      Preferences::mode -> ConnectionMode.HTTP
      Preferences::monitoring -> MonitoringMode.Significant
      Preferences::moveModeLocatorInterval -> 30
      Preferences::mqttProtocolLevel -> MqttProtocolLevel.MQTT_3_1
      Preferences::notificationEvents -> true
      Preferences::notificationGeocoderErrors -> false
      Preferences::notificationHigherPriority -> false
      Preferences::notificationLocation -> false
      Preferences::opencageApiKey -> ""
      Preferences::osmTileScaleFactor -> 1.0f
      Preferences::password -> ""
      Preferences::pegLocatorFastestIntervalToInterval -> true
      Preferences::ping -> 15
      Preferences::port -> 8883
      Preferences::extendedData -> true
      Preferences::pubQos -> MqttQos.One
      Preferences::pubRetain -> true
      Preferences::pubTopicBase -> "owntracks/%u/%d"
      Preferences::publishLocationOnConnect -> true
      Preferences::cmd -> true
      Preferences::remoteConfiguration -> true
      Preferences::setupCompleted -> false
      Preferences::showRegionsOnMap -> true
      Preferences::sub -> true
      Preferences::subQos -> MqttQos.Two
      Preferences::subTopic -> DEFAULT_SUB_TOPIC
      Preferences::theme -> AppTheme.Auto
      Preferences::tls -> true
      Preferences::tlsClientCrt -> ""
      Preferences::tid ->
          StringMaxTwoAlphaNumericChars(preferences.deviceId.takeLast(2).ifEmpty { "na" })
      Preferences::url -> ""
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
