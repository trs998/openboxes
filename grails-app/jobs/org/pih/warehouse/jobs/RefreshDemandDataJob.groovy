package org.pih.warehouse.jobs

import org.quartz.DisallowConcurrentExecution
import org.quartz.JobExecutionContext

@DisallowConcurrentExecution
class RefreshDemandDataJob {

    def concurrent = false
    def reportService
    static triggers = JobUtils.getTriggers(RefreshDemandDataJob)

    def execute(JobExecutionContext context) {
        if (JobUtils.shouldExecute(RefreshDemandDataJob)) {
            def startTime = System.currentTimeMillis()
            log.info("Refreshing demand data: " + context.mergedJobDataMap)
            reportService.refreshProductDemandData()
            log.info "Finished refreshing demand data in " + (System.currentTimeMillis() - startTime) + " ms"
        }
    }
}
