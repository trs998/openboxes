/**
 * Copyright (c) 2012 Partners In Health.  All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 **/
package org.pih.warehouse.core

import org.pih.warehouse.importer.CSVUtils
import org.pih.warehouse.inventory.InventoryItem
import org.pih.warehouse.inventory.InventoryLevel
import org.pih.warehouse.inventory.InventoryStatus
import org.pih.warehouse.inventory.TransactionCode
import org.pih.warehouse.inventory.TransactionEntry
import org.pih.warehouse.requisition.RequisitionItem

import java.text.SimpleDateFormat

class DashboardService {

    boolean transactional = true

    def productAvailabilityService

    /**
     * Get fast moving items based on requisition data.
     *
     * @param location
     * @param date
     * @param max
     * @return
     */
    def getFastMovers(Location location, Date date, Integer max) {
        def startTime = System.currentTimeMillis()
        def data = [:]
        try {
            data.location = location.id
            data.startDate = date - 30
            data.endDate = date

            def criteria = RequisitionItem.createCriteria()
            def results = criteria.list {
                requisition {
                    eq("origin", location)
                    between("dateRequested", date - 30, date)
                }
                projections {
                    groupProperty("product")
                    countDistinct('id', "occurrences")
                    sum("quantity", "quantity")
                }
                order('occurrences', 'desc')
                order('quantity', 'desc')
                if (max) {
                    maxResults(max)
                }
            }

            def quantityMap = productAvailabilityService.getCurrentInventory(location)

            def count = 1
            data.results = results.collect {

                def quantityOnHand = quantityMap[it[0]] ?: 0
                [
                        rank             : count++,
                        id               : it[0].id,
                        productCode      : it[0].productCode,
                        name             : it[0].name,
                        requisitionCount : it[1],
                        quantityRequested: it[2],
                        quantityOnHand   : quantityOnHand,
                ]
            }
            data.responseTime = (System.currentTimeMillis() - startTime) + " ms"


        } catch (Exception e) {
            log.error("Error occurred while getting requisition items " + e.message, e)
            data = e.message
        }
        return data
    }


    def getExpirationSummary(location) {
        def expirationSummary = [:]
        def expirationAlerts = getExpirationAlerts(location)
        def daysToExpiry = [30, 60, 90, 180, 365]
        expirationAlerts.each {
            if(it.inventoryItem.expires != "never") {
                if (it.daysToExpiry > 0) {
                    if (it.daysToExpiry > 365) {
                        expirationSummary["greaterThan365Days"] = expirationSummary["greaterThan365Days"] ? expirationSummary["greaterThan365Days"] + 1 : 1
                    }
                    daysToExpiry.each { day ->
                        if (it.daysToExpiry <= day ) {
                            expirationSummary["within${day}Days"] =  expirationSummary["within${day}Days"] ? expirationSummary["within${day}Days"] + 1 : 1
                        }
                    }
                } else {
                    expirationSummary["expired"] = expirationSummary["expired"] ? expirationSummary["expired"] + 1 : 1
                }
            }
        }

        // FIXME Clean this up a bit
        expirationSummary["totalExpiring"] = expirationSummary.findAll {
            it.key != "never"
        }.values().sum()

        return expirationSummary
    }

    def getExpirationAlerts(location) {
        def startTime = System.currentTimeMillis()

        def expirationAlerts = []
        def today = new Date()
        def quantityMap = productAvailabilityService.getQuantityOnHandByInventoryItem(location)
        quantityMap.each { key, value ->
            if (value > 0) {
                def daysToExpiry = key.expirationDate ? (key.expirationDate - today) : null
                expirationAlerts << [id            : key.id, lotNumber: key.lotNumber, quantity: value,
                                     expirationDate: key.expirationDate, daysToExpiry: daysToExpiry,
                                     product       : key.product.toJson(), inventoryItem: key.toJson()
                ]
            }
        }

        log.info "Expiration alerts: " + (System.currentTimeMillis() - startTime) + " ms"

        return expirationAlerts

    }

    /**
     * Get all expired inventory items for the given category and location.
     *
     * @param category
     * @param location
     * @return
     */
    List getExpiredStock(Category category, Location location) {

        long startTime = System.currentTimeMillis()

        // Stock that has already expired
        def expiredStock = InventoryItem.findAllByExpirationDateLessThan(new Date(), [sort: 'expirationDate', order: 'desc'])

        log.debug expiredStock

        Map<InventoryItem, Integer> quantityMap =
                productAvailabilityService.getQuantityOnHandByInventoryItem(location)
        expiredStock = expiredStock.findAll { quantityMap[it] > 0 }

        // FIXME poor man's filter
        if (category) {
            expiredStock = expiredStock.findAll { item -> item?.product?.category == category }
        }

        log.debug "Get expired stock: " + (System.currentTimeMillis() - startTime) + " ms"
        return expiredStock

    }

    /**
     * Get all inventory items that are expiring within the given threshold.
     *
     * @param category the category filter
     * @param threshold the threshold filter
     * @return a list of inventory items
     */
    List getExpiringStock(Category category, Location location, String expirationStatus) {
        long startTime = System.currentTimeMillis()

        def today = new Date()
        today.clearTime()
        // Get all stock expiring ever (we'll filter later)
        def expiringStock = InventoryItem.findAllByExpirationDateGreaterThanEquals(today + 1, [sort: 'expirationDate', order: 'asc'])
        def quantityMap = productAvailabilityService.getQuantityOnHandByInventoryItem(location)
        expiringStock = expiringStock.findAll { quantityMap[it] > 0 }
        if (category) {
            expiringStock = expiringStock.findAll { item -> item?.product?.category == category }
        }

        if (expirationStatus) {
            def daysToExpiry = [30, 60, 90, 180, 365]

            expiringStock = expiringStock.findAll { inventoryItem ->
                if (expirationStatus == "greaterThan365Days") {
                    inventoryItem.expirationDate - today > 365
                } else if (expirationStatus == "expired") {
                    inventoryItem.expirationDate - today <= 0
                } else inventoryItem
            }

            daysToExpiry.each { day ->
                if (expirationStatus == "within${day}Days") {
                    expiringStock = expiringStock.findAll { inventoryItem ->
                        inventoryItem.expirationDate - today <= day
                    }
                }
            }
        }

        log.debug "Get expiring stock: " + (System.currentTimeMillis() - startTime) + " ms"
        return expiringStock
    }

    def getInventoryItems(Location location) {
        def inventoryByProduct = productAvailabilityService.getInventoryByProduct(location)
        def inventoryLevelMap = InventoryLevel.findAllByInventory(location.inventory).groupBy {
            it.product
        }

        def inventoryItems = []
        inventoryByProduct.each { product, inventoryItem ->
            def inventoryLevel = inventoryLevelMap[product]?.first()
            def inventoryStatus

            if (inventoryLevel) {
                inventoryStatus = inventoryLevel?.statusMessage(inventoryItem.quantityOnHand) ?: "${inventoryLevel?.id}"
            } else {
                inventoryStatus = inventoryItem.quantityOnHand > 0 ? "IN_STOCK" : "STOCK_OUT"
            }

            inventoryItems << [
                    status: inventoryStatus,
                    product: product,
                    quantity: inventoryItem.quantityOnHand,
                    quantityAvailableToPromise: inventoryItem.quantityAvailableToPromise,
                    inventoryLevel: inventoryLevel
            ]
        }

        return inventoryItems
    }

    def getReconditionedStock(Location location) {
        long startTime = System.currentTimeMillis()
        def inventoryItems = getInventoryItems(location)
        def reconditionedStock = inventoryItems.findAll { it.product.reconditioned }
        log.debug "Get reconditioned stock: " + (System.currentTimeMillis() - startTime) + " ms"
        return reconditionedStock
    }


    def getTotalStock(Location location) {
        long startTime = System.currentTimeMillis()
        def inventoryItems = getInventoryItems(location)
        log.debug "Get total stock: " + (System.currentTimeMillis() - startTime) + " ms"
        return inventoryItems
    }

    def getInStock(Location location) {
        long startTime = System.currentTimeMillis()
        def inventoryItems = getInventoryItems(location)
        def inStock = inventoryItems.findAll { it.quantity > 0 }
        log.debug "Get in stock: " + (System.currentTimeMillis() - startTime) + " ms"
        return inStock
    }


    def getTotalStockValue(Location location) {
        def hitCount = 0
        def missCount = 0
        def totalCount = 0
        def totalStockValue = 0.0
        def stockValueByProduct = [:]
        if (location.inventory) {
            def quantityMap = productAvailabilityService.getCurrentInventory(location)
            quantityMap.each { product, quantity ->
                if (product.pricePerUnit) {
                    def stockValueForProduct = product.pricePerUnit * quantity
                    if (stockValueForProduct > 0) {
                        stockValueByProduct[product] = stockValueForProduct
                        totalStockValue += stockValueForProduct
                    }
                    hitCount++
                } else {
                    missCount++
                }
            }
            totalCount = quantityMap?.keySet()?.size()
        }
        return [totalStockValue: totalStockValue, hitCount: hitCount, missCount: missCount, totalCount: totalCount, stockValueByProduct: stockValueByProduct]

    }

    String exportLatestInventoryDate(location) {
        def formatDate = new SimpleDateFormat("dd/MMM/yyyy hh:mm:ss")
        def sw = new StringWriter()

        def inventoryItems = getTotalStock(location)
        def products = inventoryItems.collect { it.product }

        def latestInventoryDates = TransactionEntry.executeQuery("""
                select ii.product.id, max(t.transactionDate)
                from TransactionEntry as te
                left join te.inventoryItem as ii
                left join te.transaction as t
                where t.inventory = :inventory
                and t.transactionType.transactionCode in (:transactionCodes)
                group by ii.product
                """,
                [inventory: location.inventory, transactionCodes: [TransactionCode.PRODUCT_INVENTORY, TransactionCode.INVENTORY]])


        // Convert to map
        def latestInventoryDateMap = [:]
        latestInventoryDates.each {
            latestInventoryDateMap[it[0]] = it[1]
        }

        def inventoryLevelMap = [:]
        def inventoryLevels = InventoryLevel.findAllByInventory(location.inventory)
        inventoryLevels.each { inventoryLevel ->
            inventoryLevelMap[inventoryLevel.product] = inventoryLevel
        }

        def records = products.collect { product ->
            def latestInventoryDate = latestInventoryDateMap[product.id]
            return [
                "Product Code"           : product?.productCode,
                "Name"                   : product?.name,
                "ABC"                    : inventoryLevelMap[product]?.abcClass,
                "Most Recent Stock Count": CSVUtil.formatDate(latestInventoryDate),
                "QoH"                    : quantityMap[product],
                "Unit of Measure"        : product?.unitOfMeasure
            ]
        }
        return CSVUtils.dumpMaps(records)
    }


    def getDashboardAlerts(Location location) {
        log.info "Dashboard alerts for ${location}"

        long startTime = System.currentTimeMillis()
        def quantityMap = productAvailabilityService.getCurrentInventory(location)
        def inventoryLevelMap = InventoryLevel.findAllByInventory(location.inventory).groupBy {
            it.product
        }

        def totalStock = quantityMap
        def reconditionedStock = quantityMap.findAll { it.key.reconditioned }
        def onHandQuantityZero = quantityMap.findAll { it.value <= 0 }
        def inStock = quantityMap.findAll { it.value > 0 }

        def outOfStock = quantityMap.findAll { product, quantity ->
            def inventoryLevel = inventoryLevelMap[product]?.first()
            inventoryLevel?.status >= InventoryStatus.SUPPORTED && quantity <= 0
        }

        def lowStock = quantityMap.findAll { product, quantity ->
            def inventoryLevel = inventoryLevelMap[product]?.first()
            def minQuantity = inventoryLevelMap[product]?.first()?.minQuantity
            inventoryLevel?.status >= InventoryStatus.SUPPORTED && minQuantity && quantity > 0 && quantity <= minQuantity
        }

        def reorderStock = quantityMap.findAll { product, quantity ->
            def inventoryLevel = inventoryLevelMap[product]?.first()
            def reorderQuantity = inventoryLevelMap[product]?.first()?.reorderQuantity
            def minQuantity = inventoryLevelMap[product]?.first()?.minQuantity
            inventoryLevel?.status >= InventoryStatus.SUPPORTED && reorderQuantity && minQuantity > 0 && quantity <= reorderQuantity
        }

        def healthyStock = quantityMap.findAll { product, quantity ->
            def inventoryLevel = inventoryLevelMap[product]?.first()
            def reorderQuantity = inventoryLevelMap[product]?.first()?.reorderQuantity
            def maxQuantity = inventoryLevelMap[product]?.first()?.maxQuantity
            inventoryLevel?.status >= InventoryStatus.SUPPORTED && quantity > reorderQuantity && quantity <= maxQuantity
        }


        def overStock = quantityMap.findAll { product, quantity ->
            def inventoryLevel = inventoryLevelMap[product]?.first()
            def maxQuantity = inventoryLevelMap[product]?.first()?.maxQuantity
            inventoryLevel?.status >= InventoryStatus.SUPPORTED && maxQuantity && quantity > maxQuantity
        }

        def outOfStockClassA = quantityMap.findAll { product, quantity ->
            def inventoryLevel = inventoryLevelMap[product]?.first()
            inventoryLevel?.status >= InventoryStatus.SUPPORTED && quantity <= 0 && inventoryLevel?.abcClass == "A"
        }

        def outOfStockClassB = quantityMap.findAll { product, quantity ->
            def inventoryLevel = inventoryLevelMap[product]?.first()
            inventoryLevel?.status >= InventoryStatus.SUPPORTED && quantity <= 0 && inventoryLevel?.abcClass == "B"
        }

        def outOfStockClassC = quantityMap.findAll { product, quantity ->
            def inventoryLevel = inventoryLevelMap[product]?.first()
            inventoryLevel?.status >= InventoryStatus.SUPPORTED && quantity <= 0 && inventoryLevel?.abcClass == "C"
        }

        def outOfStockClassNone = quantityMap.findAll { product, quantity ->
            def inventoryLevel = inventoryLevelMap[product]?.first()
            inventoryLevel?.status >= InventoryStatus.SUPPORTED && quantity <= 0 && inventoryLevel?.abcClass == null
        }

        log.debug "Get low stock: " + (System.currentTimeMillis() - startTime) + " ms"

        [lowStock               : lowStock.keySet().size(),
         lowStockCost           : getTotalCost(lowStock),
         reorderStock           : reorderStock.keySet().size(),
         reorderStockCost       : getTotalCost(reorderStock),
         overStock              : overStock.keySet().size(),
         overStockCost          : getTotalCost(overStock),
         totalStock             : totalStock.keySet().size(),
         totalStockCost         : getTotalCost(totalStock),
         reconditionedStock     : reconditionedStock.keySet().size(),
         reconditionedStockCost : getTotalCost(reconditionedStock),
         healthyStock           : healthyStock.keySet().size(),
         healthyStockCost       : getTotalCost(healthyStock),
         outOfStock             : outOfStock.keySet().size(),
         outOfStockCost         : getTotalCost(outOfStock),
         outOfStockClassA       : outOfStockClassA.keySet().size(),
         outOfStockCostClassA   : getTotalCost(outOfStockClassA),
         outOfStockClassB       : outOfStockClassB.keySet().size(),
         outOfStockCostClassB   : getTotalCost(outOfStockClassB),
         outOfStockClassC       : outOfStockClassC.keySet().size(),
         outOfStockCostClassC   : getTotalCost(outOfStockClassC),
         outOfStockClassNone    : outOfStockClassNone.keySet().size(),
         outOfStockCostClassNone: getTotalCost(outOfStockClassNone),

         onHandQuantityZero     : onHandQuantityZero.keySet().size(),
         onHandQuantityZeroCost : getTotalCost(onHandQuantityZero),
         inStock                : inStock.keySet().size(),
         inStockCost            : getTotalCost(inStock),
        ]
    }


    def getTotalCost(quantityMap) {
        def totalCost = 0
        quantityMap.each { k, v ->
            totalCost += k.pricePerUnit ?: 0 * v ?: 0
        }
        return totalCost
    }

    def getInventoryStatus(Location location) {
        def quantityMap = productAvailabilityService.getCurrentInventory(location)
        def inventoryLevelMap = InventoryLevel.findAllByInventory(location.inventory).groupBy {
            it.product
        }
        def inventoryStatusMap = [:]
        quantityMap.each { product, quantity ->
            def inventoryLevel = inventoryLevelMap[product]?.first()
            if (inventoryLevel) {
                inventoryStatusMap[product] = inventoryLevel?.statusMessage(quantity) ?: "${inventoryLevel?.id}"
            } else {
                inventoryStatusMap[product] = quantity > 0 ? "IN_STOCK" : "STOCK_OUT"
            }

        }
        return inventoryStatusMap
    }

    /**
     * Get inventory status, inventory level and quantity on hand for all products.
     *
     * @param location
     * @return
     */
    def getInventoryStatusAndLevel(Location location) {
        def quantityMap = productAvailabilityService.getCurrentInventory(location)
        def inventoryLevelMap = InventoryLevel.findAllByInventory(location.inventory).groupBy {
            it.product
        }
        def inventoryStatusMap = [:]
        quantityMap.each { product, quantity ->
            def inventoryLevel = inventoryLevelMap[product]?.first()
            def status = inventoryLevel?.statusMessage(quantity) ?: "NONE"
            inventoryStatusMap[product] = [inventoryLevel: inventoryLevel, status: status, onHandQuantity: quantity]
        }
        return inventoryStatusMap
    }

    def getQuantityOnHandZero(Location location) {
        long startTime = System.currentTimeMillis()
        def inventoryItems = getInventoryItems(location)

        def stockOut = inventoryItems.findAll { it.quantity <= 0 }

        log.info "Get quantity on hand zero: " + (System.currentTimeMillis() - startTime) + " ms"
        return stockOut

    }

    def getOutOfStock(Location location, String abcClass) {
        long startTime = System.currentTimeMillis()
        def inventoryItems = getInventoryItems(location)

        def stockOut = inventoryItems.findAll { inventoryItem ->
            def inventoryLevel = inventoryItem.inventoryLevel
            def quantity = inventoryItem.quantity

            if (abcClass)
                inventoryLevel?.status >= InventoryStatus.SUPPORTED && quantity <= 0 && (abcClass == inventoryLevel.abcClass)
            else
                inventoryLevel?.status >= InventoryStatus.SUPPORTED && quantity <= 0
        }

        log.info "Get stock out: " + (System.currentTimeMillis() - startTime) + " ms"
        return stockOut
    }

    def getLowStock(Location location) {
        long startTime = System.currentTimeMillis()
        def inventoryItems = getInventoryItems(location)
        log.info("getInventoryItems: " + (System.currentTimeMillis() - startTime) + " ms")

        def lowStock = inventoryItems.findAll { inventoryItem ->
            def quantity = inventoryItem.quantity
            def inventoryLevel = inventoryItem.inventoryLevel
            def minQuantity = inventoryLevel?.minQuantity

            inventoryLevel?.status >= InventoryStatus.SUPPORTED && minQuantity && quantity <= minQuantity
        }
        log.info "Get low stock: " + (System.currentTimeMillis() - startTime) + " ms"
        return lowStock
    }

    def getReorderStock(Location location) {
        long startTime = System.currentTimeMillis()
        def inventoryItems = getInventoryItems(location)

        def reorderStock = inventoryItems.findAll { inventoryItem ->
            def quantity = inventoryItem.quantity
            def inventoryLevel = inventoryItem.inventoryLevel
            def reorderQuantity = inventoryLevel?.reorderQuantity

            inventoryLevel?.status >= InventoryStatus.SUPPORTED && reorderQuantity && quantity <= reorderQuantity
        }
        log.info "Get reorder stock: " + (System.currentTimeMillis() - startTime) + " ms"
        return reorderStock
    }

    def getReorderReport(Location location) {
        long startTime = System.currentTimeMillis()
        ArrayList<Map> inventoryItems = getInventoryItems(location)

        ArrayList<Map> reorderStock = inventoryItems.findAll { Map inventoryItem ->
            def inventoryLevel = inventoryItem?.inventoryLevel as InventoryLevel

            if (inventoryLevel?.binLocation != null) {
                return false;
            }

            def minQuantity = inventoryLevel?.minQuantity

            if(minQuantity == null) {
                return false
            }

            def reorderQuantity = inventoryLevel?.reorderQuantity

            if(reorderQuantity == null) {
                return false
            }

            def quantityATP = inventoryItem.quantityAvailableToPromise as Number

            if (quantityATP >= minQuantity || quantityATP >= reorderQuantity) {
                return false
            }

            def quantityToOrder = null
            def expectedReorderCost = null
            def unitCost =  inventoryLevel?.product?.costPerUnit

            if (inventoryLevel?.maxQuantity != null) {
                quantityToOrder = inventoryLevel.maxQuantity - quantityATP

                if(unitCost != null) {
                    expectedReorderCost = quantityToOrder * unitCost
                }
            }
            inventoryItem.put("quantityToOrder", quantityToOrder);
            inventoryItem.put("unitCost", unitCost);
            inventoryItem.put("expectedReorderCost", expectedReorderCost);

            true
        }
        log.info "Get simple reorder stock: " + (System.currentTimeMillis() - startTime) + " ms"
        return reorderStock
    }

    def getOverStock(Location location) {
        long startTime = System.currentTimeMillis()
        def inventoryItems = getInventoryItems(location)

        def overStock = inventoryItems.findAll { inventoryItem ->
            def quantity = inventoryItem.quantity
            def inventoryLevel = inventoryItem.inventoryLevel
            def maxQuantity = inventoryLevel?.maxQuantity

            inventoryLevel?.status >= InventoryStatus.SUPPORTED && maxQuantity && quantity > maxQuantity
        }
        log.info "Get over stock: " + (System.currentTimeMillis() - startTime) + " ms"
        return overStock
    }

    def getHealthyStock(Location location) {
        long startTime = System.currentTimeMillis()
        def inventoryItems = getInventoryItems(location)

        def healthyStock = inventoryItems.findAll { inventoryItem ->
            def quantity = inventoryItem.quantity
            def inventoryLevel = inventoryItem.inventoryLevel
            def reorderQuantity = inventoryLevel?.reorderQuantity
            def maxQuantity = inventoryLevel?.maxQuantity

            inventoryLevel?.status >= InventoryStatus.SUPPORTED && quantity > reorderQuantity && quantity <= maxQuantity
        }
        log.info "Get healthy stock: " + (System.currentTimeMillis() - startTime) + " ms"
        return healthyStock
    }
}
