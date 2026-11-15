package dev.fieldreporter.platform

import dev.fieldreporter.core.domain.Location
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyNearestTenMeters
import platform.Foundation.NSError
import platform.darwin.NSObject

/**
 * One-shot "where am I" lookup. Asks for permission on first use (the
 * `NSLocationWhenInUseUsageDescription` text is in Info.plist), then reports one fix, or null if
 * permission is denied or no fix can be found.
 */
@OptIn(ExperimentalForeignApi::class)
class LocationLookup {
    private val manager = CLLocationManager().apply { desiredAccuracy = kCLLocationAccuracyNearestTenMeters }
    private var pending: ((Location?) -> Unit)? = null

    // CoreLocation holds its delegate weakly, so this object keeps it alive.
    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            if (pending != null) proceed()
        }

        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val fix = (didUpdateLocations.lastOrNull() as? CLLocation)?.coordinate?.useContents {
                Location(latitude = latitude, longitude = longitude)
            }
            finish(fix)
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) = finish(null)
    }

    init {
        manager.delegate = delegate
    }

    /** @param onResult called once, on the main thread. */
    fun lookup(onResult: (Location?) -> Unit) {
        pending?.invoke(null) // a newer lookup replaces one still waiting
        pending = onResult
        proceed()
    }

    private fun proceed() {
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization() // continues in the delegate
            kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> manager.requestLocation()
            else -> finish(null) // denied or restricted
        }
    }

    private fun finish(fix: Location?) {
        val callback = pending ?: return
        pending = null
        callback(fix)
    }
}
