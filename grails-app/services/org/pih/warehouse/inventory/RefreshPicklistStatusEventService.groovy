/**
 * Copyright (c) 2012 Partners In Health.  All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 **/
package org.pih.warehouse.inventory

import org.pih.warehouse.picklist.Picklist
import org.springframework.context.ApplicationListener
class RefreshPicklistStatusEventService implements ApplicationListener<RefreshPicklistStatusEvent> {

    boolean transactional = true
    def grailsApplication
    def picklistService

    void onApplicationEvent(RefreshPicklistStatusEvent event) {
        log.info "Application event $event has been published! " + event.properties
        Picklist picklist = event?.source
        picklistService.triggerPicklistStatusUpdate(picklist)
    }
}