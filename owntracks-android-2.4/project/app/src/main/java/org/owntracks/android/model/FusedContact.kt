package org.owntracks.android.model

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import org.owntracks.android.BR
import org.owntracks.android.location.LatLng
import org.owntracks.android.model.messages.MessageCard
import org.owntracks.android.model.messages.MessageLocation
import timber.log.Timber

class FusedContact(id: String?) : BaseObservable() {
    @get:Bindable
    val id: String = if (id != null && id.isNotEmpty()) id else "NOID"

    @get:Bindable
    var messageLocation: MessageLocation? = null
    internal var messageCard: MessageCard? = null

    @get:Bindable
    @set:Bindable
    var imageProvider = 0

    @get:Bindable
    var tst: Long = 0
        private set

    @get:Bindable
    var userAppPermissions = "Ok"

    fun setMessageLocation(messageLocation: MessageLocation): Boolean {
        if (tst > messageLocation.timestamp) return false
        Timber.v("update contact:%s, tst:%s", id, messageLocation.timestamp)
        messageLocation.setContact(this) // Allows to update fusedLocation if geocoder of messageLocation changed
        this.messageLocation = messageLocation
        tst = messageLocation.timestamp
        notifyMessageLocationPropertyChanged()
        return true
    }

    fun notifyMessageLocationPropertyChanged() {
        if (messageLocation != null) {
            Timber.d("Geocode location updated for %s: %s", id, messageLocation!!.geocode)
        }
        notifyPropertyChanged(BR.fusedName)
        notifyPropertyChanged(BR.messageLocation)
        notifyPropertyChanged(BR.geocodedLocation)
        notifyPropertyChanged(BR.fusedLocationAccuracy)
        notifyPropertyChanged(BR.tst)
        notifyPropertyChanged(BR.trackerId)
        notifyPropertyChanged(BR.id)
    }

    @get:Bindable
    val geocodedLocation: String?
        get() = messageLocation?.geocode

    @Bindable
    fun getMessageCard(): MessageCard? {
        return messageCard
    }

    @get:Bindable
    val fusedName: String
        get() = messageCard?.name ?: trackerId

    @get:Bindable
    val fusedLocationAccuracy: Int
        get() = messageLocation?.accuracy ?: 0

    @get:Bindable
    val trackerId: String
        get() = messageLocation?.trackerId ?: id.replace("/", "").let {
            return if (it.length > 2) {
                it.substring(it.length - 2)
            } else {
                it
            }
        }
    val latLng: LatLng?
        get() = messageLocation?.run { LatLng(latitude, longitude) }

    override fun toString(): String {
        return "FusedContact $id ($fusedName)"
    }

    fun getBatteryCharging(): Boolean {
        if ((messageLocation?.batteryStatus ?:0) == BatteryStatus.CHARGING) {
            return(true)
        } else {
            return(false)
        }
    }

    fun getWifiOn(): Boolean {
        if ((messageLocation?.wifistate ?:0) == 0) {
            return (false)
        } else {
            return (true)
        }
    }
    fun getWifiEnabledDisabled(): String {
        if ((messageLocation?.wifistate ?:0) == 0) {
            return ("Off")
        } else {
            return ("On")
        }
    }

    fun getAppPermissionsOk(): Boolean {
        if (userAppPermissions == "Ok") {
            return(true)
        } else {
            return(false)
        }
    }

    fun getAppPermissions(): String {
        if ((messageLocation?.locationPermission ?:0) != 0) {
            userAppPermissions = "Location Restrictions"
        } else if ((messageLocation?.batteryOptimizations ?:0) == 1) {
            userAppPermissions = "Battery Restrictions"
        } else if ((messageLocation?.appHibernation ?:0) == 1) {
            userAppPermissions = "Can Pause"
        } else if ((messageLocation?.powerSave ?:0) == 1) {
            userAppPermissions = "Battery Saver"
        } else {
            userAppPermissions = "Ok"
        }
        return(userAppPermissions)
    }
}
