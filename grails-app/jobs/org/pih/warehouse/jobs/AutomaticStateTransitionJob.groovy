package org.pih.warehouse.jobs

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.pih.warehouse.api.StockMovement
import org.pih.warehouse.inventory.StockMovementStatusCode
import org.quartz.DisallowConcurrentExecution
import org.quartz.JobExecutionContext

@DisallowConcurrentExecution
class AutomaticStateTransitionJob {

    def tmsIntegrationService
    def stockMovementService

    static triggers = {
        cron name: 'autoTransitionRequisitionStatusJobCronTrigger',
                cronExpression: ConfigurationHolder.config.openboxes.jobs.automaticStateTransitionJob.cronExpression
    }

    def execute(JobExecutionContext context) {
        log.info "Running automatic state transition job ... "
        if(!ConfigurationHolder.config.openboxes.jobs.automaticStateTransitionJob.enabled) {
            return
        }

        StockMovement criteria = new StockMovement(stockMovementStatusCode: StockMovementStatusCode.PICKING)
        def stockMovements = stockMovementService.getOutboundStockMovements(criteria, [:])
        log.info "Found ${stockMovements.size()} stock movements in PICKING "
        stockMovements.each { StockMovement stockMovement ->
            log.info "stock movement " + stockMovement.identifier + " " + stockMovement.status
            tmsIntegrationService.triggerStockMovementStatusUpdate(stockMovement)
        }
    }
}
