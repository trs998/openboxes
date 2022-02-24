/**
 * Copyright (c) 2012 Partners In Health.  All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 **/
package org.pih.warehouse.auth

import org.pih.warehouse.core.Location
import org.pih.warehouse.core.User

class AuthService {
    boolean transactional = true
    private static ThreadLocal<String> currentUserId = new ThreadLocal<String>()
    private static ThreadLocal<String> currentLocationId = new ThreadLocal<String>()

    static void setCurrentUser(User user) {
        currentUserId.set(user?.id)
    }

    static User getCurrentUser() {
        String id = currentUserId.get()
        if (id == null) {
            return null
        }
        return User.get(id)
    }

    static void clearCurrentUser() {
        currentUserId.remove()
        currentUserId.set(null)
    }

    static void setCurrentLocation(Location location) {
        currentLocationId.set(location?.id)
    }

    static Location getCurrentLocation() {
        String id = currentLocationId.get()
        if (id == null) {
            return null
        }
        return Location.get(id)
    }

    static void clearCurrentLocation() {
        currentLocationId.remove()
        currentLocationId.set(null)
    }
}
