package org.pih.warehouse.jobs

import org.quartz.DisallowConcurrentExecution
import org.quartz.JobExecutionContext

@DisallowConcurrentExecution
class DataMigrationJob {

    def concurrent = false
    def migrationService
    static triggers = {}

    def execute(JobExecutionContext context) {
        if (JobUtils.shouldExecute(DataMigrationJob)) {
            log.info "Starting data migration job at ${new Date()}"
            def startTime = System.currentTimeMillis()
            migrationService.migrateInventoryTransactions()
            log.info "Finished data migration job in " + (System.currentTimeMillis() - startTime) + " ms"
        }
    }
}
