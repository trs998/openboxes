package org.pih.warehouse.jobs

import org.quartz.DisallowConcurrentExecution
import org.quartz.JobExecutionContext

@DisallowConcurrentExecution
class UpdateExchangeRatesJob {

    def concurrent = false
    def currencyService
    static triggers = JobUtils.getTriggers(UpdateExchangeRatesJob)

    def execute(JobExecutionContext context) {
        if (JobUtils.shouldExecute(UpdateExchangeRatesJob)) {
            log.info "Starting job at ${new Date()}"
            def startTime = System.currentTimeMillis()
            currencyService.updateExchangeRates()
            log.info "Finished running job in " + (System.currentTimeMillis() - startTime) + " ms"
        }
    }
}
