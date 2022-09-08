package org.pih.warehouse.jobs

import org.quartz.DisallowConcurrentExecution

@DisallowConcurrentExecution
class DataCleaningJob {

    def concurrent = false
    def shipmentService
    static triggers = JobUtils.getTriggers(DataCleaningJob)

    def execute(context) {

        if (!JobUtils.shouldExecute(DataCleaningJob)) {
            return
        }

        log.debug "Starting data cleaning job at ${new Date()}"
        def startTime = System.currentTimeMillis()
        shipmentService.bulkUpdateShipments()
        log.debug "Finished data cleaning job in " + (System.currentTimeMillis() - startTime) + " ms"
    }
}
