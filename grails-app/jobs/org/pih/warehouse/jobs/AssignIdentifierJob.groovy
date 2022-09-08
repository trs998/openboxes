package org.pih.warehouse.jobs

import org.quartz.DisallowConcurrentExecution

@DisallowConcurrentExecution
class AssignIdentifierJob {

    def identifierService

    static triggers = JobUtils.getTriggers(AssignIdentifierJob)

    def execute() {

        if (!JobUtils.shouldExecute(AssignIdentifierJob)) {
            return
        }

        identifierService.assignProductIdentifiers()
        identifierService.assignShipmentIdentifiers()
        identifierService.assignReceiptIdentifiers()
        identifierService.assignOrderIdentifiers()
        identifierService.assignRequisitionIdentifiers()
        identifierService.assignTransactionIdentifiers()
    }
}
