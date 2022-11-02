package org.pih.warehouse.dashboard

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import org.grails.plugins.web.taglib.ApplicationTagLib
import org.joda.time.LocalDate
import org.pih.warehouse.LocalizationUtil
import org.pih.warehouse.core.Location
import org.pih.warehouse.inventory.InventorySnapshot
import org.pih.warehouse.inventory.TransactionCode
import org.pih.warehouse.product.ProductAvailability
import org.pih.warehouse.receiving.ReceiptItem
import org.pih.warehouse.requisition.Requisition
import org.pih.warehouse.requisition.RequisitionStatus
import org.pih.warehouse.requisition.RequisitionType
import org.pih.warehouse.shipping.Shipment

@Transactional
class IndicatorDataService {

    def dashboardService
    def dataService
    GrailsApplication grailsApplication
    def messageService

    GraphData getExpirationSummaryData(Location location, def params) {
        // querySize = value of the date filter (1 month, 3 months, etc.)
        // Here it represents the last month we want to show
        // Add + 1 to include today (expired items) as the first point
        Integer querySize = params.querySize ? params.querySize.toInteger() + 1 : 7

        LocalDate date = LocalDate.now()

        List expirationSummary = [0] * querySize

        List linksExpirationSummary = [""] * querySize

        List listLabels = []

        // Fill labels and links
        for (int i = 0; i < querySize; i++) {
            Integer daysCounter = i * 30

            String label = (i == 0) ? "react.dashboard.timeline.today.label" : "react.dashboard.timeline.within${daysCounter}Days.label"
            def monthLabel = [
            code : label,
            message : messageService.getMessage(label)
            ]
            listLabels.push(monthLabel)

            // Expired items
            if (i == 0) {
                linksExpirationSummary[0] = "/inventory/listExpiredStock?status=expired"
            }

            // 1, 3 and 6 months
            if (i == 1 || i == 3 || i == 6) {
                linksExpirationSummary[i] = "/inventory/listExpiringStock?status=within" + daysCounter + "Days"
            }
            // 12 month will be 360 days but will link to 365 in the report
            if (i == 12) {
                linksExpirationSummary[i] = "/inventory/listExpiringStock?status=within365Days"
            }
        }

        List expirationAlerts = dashboardService.getExpirationAlerts(location)

        expirationAlerts.each {
            // Count only items that expire
            if (it.inventoryItem.expires != "never") {
                // The first element of the dataset represents expired items
                if (it.daysToExpiry <= 0) {
                    expirationSummary[0] += 1
                } else {
                    // Verifies if item expires within querySize * 30 days
                    for (int i = 1; i < querySize; i++) {
                        Integer daysCounter = i * 30

                        // if item expires in daysCounter incoming days, count it
                        if (it.daysToExpiry <= daysCounter) {
                            expirationSummary[i] += 1
                        }
                    }
                }
            }
        }

        List<IndicatorDatasets> datasets = [
                new IndicatorDatasets('Expiration(s)', expirationSummary, linksExpirationSummary)
        ]

        IndicatorData indicatorData = new IndicatorData(datasets, listLabels)

        GraphData graphData = new GraphData(indicatorData, "/inventory/listExpiringStock")

        return graphData
    }

    GraphData getFillRate(Location location, def destination, def params) {
        Integer querySize = params.querySize ? params.querySize.toInteger() : 6
        List listFiltersSelected = params.list('listFiltersSelected').toList()
        List listValues = params.list('value').toList()
        String extraCondition = ''
        String conditionStarter = 'where'

        if( listFiltersSelected.contains('category') && listValues.size > 0) {
            extraCondition = """
            join product as p on fr.product_id = p.id 
            join category as c on p.category_id = c.id
            where (
            """
            for(int i = 0; i < listValues.size; i ++) {
                extraCondition = "${extraCondition} c.id = '${listValues[i]}'"
                extraCondition = i<listValues.size - 1 ? "${extraCondition} or" : extraCondition
            }
            conditionStarter = ') and'
        }

        List listLabels = []


        List averageFillRateResult = []
        List requestLinesSubmittedResult = []
        List linesCancelledStockoutResult = []
        List averageTargetFillRate = [0.9]*querySize

        Date today = new Date()
        today.clearTime()
        for (int i = querySize; i > 0; i--) {
            def monthBegin = today.clone()
            def monthEnd = today.clone()
            monthBegin.set(month: today.month - i, date: 1)
            monthEnd.set(month: today.month - i + 1, date: 1)

            String monthLabel = new java.text.DateFormatSymbols().months[monthBegin.month]

            listLabels.push("${monthLabel} ${monthBegin.year + 1900}")

            def averageFillRate = dataService.executeQuery("""
            select avg(fr.fill_rate) FROM fill_rate as fr
            ${extraCondition}
            ${conditionStarter}
            fr.transaction_date <= :monthEnd 
            and fr.transaction_date > :monthBegin 
            and fr.origin_id = :origin
            and (fr.destination_id = :destination OR :destination IS NULL)
            GROUP BY MONTH(fr.transaction_date), YEAR(fr.transaction_date)
            """, [
                'monthEnd'    : monthEnd,
                'monthBegin'  : monthBegin,
                'destination' : destination?.id,
                'origin'      : location.id,
            ]);

            averageFillRate[0] == null ? averageFillRateResult.push(0) : averageFillRateResult.push(averageFillRate[0][0])

            def requestLinesSubmitted = dataService.executeQuery("""
            select count(fr.id) FROM fill_rate as fr
            ${extraCondition}
            ${conditionStarter}
            fr.transaction_date <= :monthEnd 
            and fr.transaction_date > :monthBegin 
            and (fr.destination_id = :destination OR :destination IS NULL) 
            and fr.origin_id = :origin
            GROUP BY MONTH(fr.transaction_date), YEAR(fr.transaction_date)
            """, [
                'monthEnd'    : monthEnd,
                'monthBegin'  : monthBegin,
                'destination' : destination?.id,
                'origin'      : location.id,
            ]);

            requestLinesSubmitted[0] == null ? requestLinesSubmittedResult.push(0) : requestLinesSubmittedResult.push(requestLinesSubmitted[0][0])

            def linesCancelledStockout = dataService.executeQuery("""
            select count(fr.id) FROM fill_rate as fr
            ${extraCondition}
            ${conditionStarter}
            fr.transaction_date <= :monthEnd and fr.transaction_date > :monthBegin 
            and (fr.destination_id = :destination OR :destination IS NULL)
            and fr.origin_id = :origin 
            and fr.fill_rate = 0
            GROUP BY MONTH(fr.transaction_date), YEAR(fr.transaction_date)
            """, [
                'monthEnd'    : monthEnd,
                'monthBegin'  : monthBegin,
                'destination' : destination?.id,
                'origin'      : location.id,
            ]);

            linesCancelledStockout[0] == null ? linesCancelledStockoutResult.push(0) : linesCancelledStockoutResult.push(linesCancelledStockout[0][0])
        }

            averageFillRateResult = averageFillRateResult.collect{ it * 100 }
            averageTargetFillRate = averageTargetFillRate.collect{ it * 100 }

        // Loading the config of the legend
        Map legendConfig = [
            'pointStyle' : 'circle',
        ]

        List<IndicatorDatasets> datasets = [
                new IndicatorDatasets('Request lines submitted', requestLinesSubmittedResult, null, 'bar', 'left-y-axis', legendConfig),
                new IndicatorDatasets('Lines cancelled stock out', linesCancelledStockoutResult, null, 'bar', 'left-y-axis', legendConfig),
                new IndicatorDatasets('Average Fill Rate', averageFillRateResult, null, 'line', 'right-y-axis', legendConfig),
                new IndicatorDatasets('Average of target Fill Rate', averageTargetFillRate, null, 'line', 'right-y-axis', legendConfig),
        ]

        IndicatorData indicatorData = new IndicatorData(datasets, listLabels)

        GraphData graphData = new GraphData(indicatorData)

        return graphData
    }

    GraphData getFillRateSnapshot (Location origin, def params) {
        String listFiltersSelected = params.list('listFiltersSelected').toList()
        List listValues = params.list('value').toList()
        List averageFillRateResult = []
        List listLabels = []
        Date today = new Date()
        today.clearTime()
        String extraCondition = ''
        String conditionStarter = 'where'

        if( listFiltersSelected.contains('category') && listValues.size > 0) {
            extraCondition = """
            join product as p on fr.product_id = p.id 
            join category as c on p.category_id = c.id
            where (
            """
            for(int i = 0; i < listValues.size; i ++) {
                extraCondition = "${extraCondition} c.id = '${listValues[i]}'"
                extraCondition = i<listValues.size - 1 ? "${extraCondition} or" : extraCondition
            }
            conditionStarter = ') and'
        }

        for (int i = 12; i > 0; i--) {
            def monthBegin = today.clone()
            def monthEnd = today.clone()
            monthBegin.set(month: today.month - i, date: 1)
            monthEnd.set(month: today.month - i + 1, date: 1)
            String monthLabel = new java.text.DateFormatSymbols().months[monthBegin.month]
            listLabels.push("${monthLabel} ${monthBegin.year + 1900}")

            def averageFillRate = dataService.executeQuery("""
            select avg(fr.fill_rate) FROM fill_rate as fr
            ${extraCondition}
            ${conditionStarter} 
            fr.transaction_date > :monthBegin
            and fr.transaction_date <= :monthEnd
            and fr.origin_id = :origin 
            GROUP BY MONTH(fr.transaction_date), YEAR(fr.transaction_date)
            """, [

                'monthBegin'  : monthBegin,
                'monthEnd'    : monthEnd,
                'origin'      : origin.id,
                'listValues'  : listValues,
            ]);

            averageFillRate[0] == null ? averageFillRateResult.push(0) : averageFillRateResult.push(averageFillRate[0][0])
        }

        averageFillRateResult = averageFillRateResult.collect{ it * 100 }

        List<IndicatorDatasets> datasets = [
                new IndicatorDatasets('Average Fill Rate', averageFillRateResult, null, 'line'),
        ];

        int averageLastMonth = averageFillRateResult[averageFillRateResult.size - 1]

        ColorNumber colorNumber = new ColorNumber(averageLastMonth, 'Fill Rate Last Month', null, null, 90)
        colorNumber.setConditionalColors(87, colorNumber.value2)
        def variation = colorNumber.value - colorNumber.value2
        colorNumber.value2 = variation > 0 ? "+${variation} %" : "${variation} %"
        colorNumber.value = "${colorNumber.value}%"

        IndicatorData indicatorData = new IndicatorData(datasets, listLabels, colorNumber);

        GraphData graphData = new GraphData(indicatorData, null)

        return graphData;
    }

    List getFillRateDestinations(Location origin) {
        def destinations = dataService.executeQuery("""
            select 
                distinct(destination_id) as id, 
                case when length(location.name) > 30 
                    then concat(substring(location.name, 1, 30), '...')
                    else location.name 
                end as name
            FROM fill_rate as fr
            JOIN location on location.id = fr.destination_id
            where fr.origin_id = :origin 
            """, [
                'origin': origin.id,
        ]);

        return destinations
    }


    GraphData getInventorySummaryData(def location) {
        def inventorySummary = dashboardService.getDashboardAlerts(location);

        def inventoryData = [
                inStockCount    : inventorySummary.inStock,
                overStockCount  : inventorySummary.overStock,
                reoderStockCount: inventorySummary.reorderStock,
                lowStockCount   : inventorySummary.lowStock,
                stockOutCount   : inventorySummary.onHandQuantityZero,
        ];

        List listData = []
        for (item in inventoryData) {
            listData.push(item.value ? item.value : 0)
        }

        List<String> links = [
                "/inventory/listInStock",
                "/inventory/listOverStock",
                "/inventory/listReorderStock",
                "/inventory/listLowStock",
                "/inventory/listQuantityOnHandZero"]

        List<IndicatorDatasets> datasets = [
                new IndicatorDatasets('Inventory Summary', listData, links)
        ];

        IndicatorData indicatorData = new IndicatorData(datasets, ['In stock', 'Above maximum', 'Below reorder', 'Below minimum', 'No longer in stock'])

        GraphData graphData = new GraphData(indicatorData)

        return graphData
    }

    GraphData getSentStockMovements(Location location, def params) {
        Integer querySize = params.querySize ? params.querySize.toInteger() - 1 : 5
        Date today = new Date()
        today.clearTime()

        // queryLimit limits the query and avoid of getting data older than wanted
        Date queryLimit = today.clone()
        queryLimit.set(month: today.month - querySize, date: 1)

        List queryData = Shipment.executeQuery("""SELECT COUNT(s.id), s.destination, 
        MONTH(s.lastUpdated), YEAR(s.lastUpdated) FROM Shipment s WHERE s.origin = :location 
        AND s.currentStatus <> 'PENDING' AND s.lastUpdated > :limit 
        GROUP BY MONTH(s.lastUpdated), YEAR(s.lastUpdated), s.destination""",
                ['location': location, 'limit': queryLimit])
        // queryData gives an array of arrays [[count, destination, month, year], ...] of sent stock

        Map listRes = [:]
        List listLabel = fillLabels(querySize)

        for (item in queryData) {
            // item[0]: item total counted
            // item[1]: item destination
            // item[2]: item month
            // item[3]: item year

            Location itemLocation = item[1]

            // If the destination is new, add it to the list with empty values for now
            if (listRes.get(itemLocation.name) == null) {
                listRes.put(itemLocation.name, new IndicatorDatasets(itemLocation.name, [0] * (querySize + 1)))
            }

            for (int i = querySize; i >= 0; i--) {
                Date tmpDate = today.clone()
                tmpDate.set(month: today.month - i, date: 1)

                // If there is data, update the dataset in the proper position
                if (tmpDate.month == item[2] - 1 && tmpDate.year + 1900 == item[3]) {
                    Integer value = item[0]
                    IndicatorDatasets locationDataset = listRes.get(itemLocation.name)
                    locationDataset.data[querySize - i] = value
                }
            }
        }
        List<IndicatorDatasets> datasets = (List<IndicatorDatasets>) listRes.values().toList()

        IndicatorData indicatorData = new IndicatorData(datasets, listLabel)

        GraphData graphData = new GraphData(indicatorData)

        return graphData
    }

    GraphData getRequisitionsByYear(Location location, def params) {
        def yearTypes = grailsApplication.config.openboxes.dashboard.yearTypes ?: [:]
        def defaultType = yearTypes.fiscalYear ?: yearTypes.calendarYear
        def yearType = params.yearType ? yearTypes[params.yearType] : defaultType

        if (!yearType) {
            throw new IllegalArgumentException("Missing year type definition in configuration")
        }

        def currentDate = new Date()
        def currentYear = currentDate.year
        def startYear = currentYear - 4 // Take only last 5 years into account

        def startDate = new Date("${yearType.start}/${startYear}") // for extracting data
        def endDate = new Date("${yearType.end}/${startYear}") // for determining year interval for data extracted

        // Data fetch
        def data = Requisition.executeQuery("""
            SELECT 
                COUNT(r.id), DATE(r.dateCreated) 
            FROM Requisition r 
            WHERE r.origin = :location AND r.dateCreated >= :startDate AND r.isTemplate = false 
            GROUP BY DATE(r.dateCreated) """, ['location': location, 'startDate': startDate])

        // Prepare list of labels for last 5 years and fill it with 0s by default in case there are no requisitions in a specific year range
        def currentYearFormatted = currentDate.format(yearType.yearFormat).toInteger()
        if (currentDate.month > endDate.month || (currentDate.month == endDate.month && currentDate.date > endDate.date)) {
            currentYearFormatted += 1
        }
        def listLabel = ((currentYearFormatted - 4)..currentYearFormatted).collect { Integer it ->
            yearType.labelYearPrefix + it.toString()
        }
        def results = [:]
        listLabel.each { results[it] = 0 }

        // Process fetched data
        data.each { it ->
            // it[0] = requisition count, it[1] = date created

            // Parse sql date into util date for easy comparison
            def createdAt = new Date(it[1].getTime())
            def year = createdAt.format(yearType.yearFormat).toInteger()
            // Set proper year for year label. If day is after year type's end day, then year += 1 (especially required
            // for year types that has end day in the middle of year, eg. fiscal year)
            if (createdAt.month > endDate.month || (createdAt.month == endDate.month && createdAt.date > endDate.date)) {
                year += 1
            }
            def yearLabel = yearType.labelYearPrefix + year.toString()

            // Add COUNT value into proper results sections
            if (results[yearLabel]) {
                results[yearLabel] += it[0]
            } else {
                results[yearLabel] = it[0]
            }

            // Check if the list with labels contains yearLabel
            if (!listLabel.contains(yearLabel)) {
                listLabel << yearLabel
            }
        }

        List<IndicatorDatasets> datasets = [
            new IndicatorDatasets('Requisition count by year', results.values().toList())
        ]
        IndicatorData indicatorData = new IndicatorData(datasets, listLabel)
        GraphData graphData = new GraphData(indicatorData)
        return graphData
    }

    GraphData getReceivedStockData(Location location, def params) {
        Integer querySize = params.querySize ? params.querySize.toInteger() - 1 : 5
        Date today = new Date()
        today.clearTime()

        Date queryLimit = today.clone()
        queryLimit.set(month: today.month - querySize, date: 1)

        List queryData = Shipment.executeQuery("""SELECT COUNT(s.id), s.origin, 
        MONTH(s.lastUpdated), YEAR(s.lastUpdated) FROM Shipment s WHERE s.destination = :location 
        AND s.currentStatus <> 'PENDING' AND s.lastUpdated > :limit 
        GROUP BY MONTH(s.lastUpdated), YEAR(s.lastUpdated), s.origin""",
                ['location': location, 'limit': queryLimit])

        Map listRes = [:]
        List listLabel = fillLabels(querySize)

        for (item in queryData) {
            // item[0]: item total counted
            // item[1]: item origin
            // item[2]: item month
            // item[3]: item year

            Location itemLocation = item[1]

            // If the origin is new, add it to the list with empty values for now
            if (listRes.get(itemLocation.name) == null) {
                listRes.put(itemLocation.name, new IndicatorDatasets(itemLocation.name, [0] * (querySize + 1)))
            }

            for (int i = querySize; i >= 0; i--) {
                Date tmpDate = today.clone()
                tmpDate.set(month: today.month - i, date: 1)

                // If there is data, update the dataset in the proper position
                if (tmpDate.month == item[2] - 1 && tmpDate.year + 1900 == item[3]) {
                    Integer value = item[0]
                    IndicatorDatasets locationDataset = listRes.get(itemLocation.name)
                    locationDataset.data[querySize - i] = value
                }
            }
        }
        List<IndicatorDatasets> datasets = (List<IndicatorDatasets>) listRes.values().toList()

        IndicatorData indicatorData = new IndicatorData(datasets, listLabel)

        GraphData graphData = new GraphData(indicatorData)

        return graphData
    }

    GraphData getOutgoingStock(Location location) {
        Date today = new Date()
        today.clearTime()
        def m4 = today - 4
        def m7 = today - 7

        def greenData = Requisition.executeQuery("""select count(r) from Requisition r where r.dateCreated > :day and r.origin = :location and r.status <> 'ISSUED'""",
                ['day': m4, 'location': location])

        def yellowData = Requisition.executeQuery("""select count(r) from Requisition r where r.dateCreated >= :dayOne and r.dateCreated <= :dayTwo and r.origin = :location and r.status <> 'ISSUED'""",
                ['dayOne': m7, 'dayTwo': m4, 'location': location])

        def redData = Requisition.executeQuery("""select count(r) from Requisition r where r.dateCreated < :day and r.origin = :location and r.status <> 'ISSUED'""",
                ['day': m7, 'location': location])

        def baseUrl = '/stockMovement/list?direction=OUTBOUND'
        def status = '&status=' + RequisitionStatus.listPending().join('&status=')

        ColorNumber green = new ColorNumber(greenData[0], 'Created < 4 days ago', baseUrl + status + "&createdAfter=${m4.format("MM/dd/yyyy")}")
        ColorNumber yellow = new ColorNumber(yellowData[0], 'Created > 4 days ago', baseUrl + status + "&createdAfter=${m7.format("MM/dd/yyyy")}&createdBefore=${m4.format("MM/dd/yyyy")}")
        ColorNumber red = new ColorNumber(redData[0], 'Created > 7 days ago', baseUrl + status + "&createdBefore=${m7.format("MM/dd/yyyy")}")

        NumbersIndicator numbersIndicator = new NumbersIndicator(green, yellow, red)

        GraphData graphData = new GraphData(numbersIndicator, "/stockMovement/list?receiptStatusCode=PENDING")

        return graphData
    }

    GraphData getIncomingStock(Location location) {

        def query = Shipment.executeQuery("""select s.currentStatus, count(s) from Shipment s where s.destination = :location and s.currentStatus <> 'RECEIVED' group by s.currentStatus""",
                ['location': location]);

        // Initial state
        ColorNumber pending = new ColorNumber(0, 'Pending', '/stockMovement/list?direction=INBOUND&receiptStatusCode=PENDING');
        ColorNumber shipped = new ColorNumber(0, 'Shipped', '/stockMovement/list?direction=INBOUND&receiptStatusCode=SHIPPED');
        ColorNumber partiallyReceived = new ColorNumber(0, 'Partially Received', '/stockMovement/list?direction=INBOUND&receiptStatusCode=PARTIALLY_RECEIVED');

        // Changes each ColorNumber if found in query
        query.each {
            if (it[0].name == 'PENDING') {
                pending.value = it[1]
            } else if (it[0].name == 'SHIPPED') {
                shipped.value = it[1]
            } else if (it[0].name == 'PARTIALLY_RECEIVED') {
                partiallyReceived.value = it[1]
            }
        }

        NumbersIndicator numbersIndicator = new NumbersIndicator(pending, shipped, partiallyReceived)

        GraphData graphData = new GraphData(numbersIndicator, "/stockMovement/list?direction=INBOUND")

        return graphData
    }

    GraphData getDiscrepancy(Location location, def params) {
        Integer querySize = params.querySize ? params.querySize.toInteger() - 1 : 5

        LocalDate queryLimit = LocalDate.now().minusMonths(querySize).withDayOfMonth(1)

        def results = ReceiptItem.executeQuery("""
            select 
                s.id,
                s.shipmentNumber, 
                s.name, 
                si.id,
                si.quantity,
                count(ri.id), 
                s.requisition.id
            from ShipmentItem as si
            left outer join si.receiptItems as ri
            join ri.receipt as r
            join r.shipment as s
            where 
                s.currentStatus = 'RECEIVED'
                and s.destination = :location 
                and r.actualDeliveryDate > :limit 
            group by s.shipmentNumber, s.id, si.id, si.quantity
            having si.quantity <> sum(ri.quantityReceived)
        """ , [
                        'location': location,
                        'limit'   : queryLimit.toDate(),
                ])

        // Transform to map
        results = results.collect {
            [
                    shipmentId    : it[0],
                    shipmentNumber: it[1],
                    shipmentName  : it[2]
            ]
        }

        // Find discrepancies by shipment
        Map discrepenciesByShipmentId =
                results.inject([:]) { map, row ->
                    // Initialize map entry for shipment id
                    if (!map[row.shipmentId])
                        map[row.shipmentId] = row << [count: 0]

                    // Each new shipment row in teh results should increment count
                    map[row.shipmentId].count += 1
                    return map
                }

        List<TableData> tableBody = discrepenciesByShipmentId.keySet().collect {
            def row = discrepenciesByShipmentId[it]
            return new TableData(row.shipmentNumber,
                    row.shipmentName,
                    row.count.toString(),
                    "/stockMovement/show/${row.shipmentId}"
            )
        }

        Table tableData = new Table("Shipment", "Name", "Discrepancy", tableBody)

        GraphData graphData = new GraphData(tableData)

        return graphData;
    }

    GraphData getDelayedShipments(Location location) {
        Date oneWeekAgo = LocalDate.now().minusWeeks(1).toDate()
        Date oneMonthAgo = LocalDate.now().minusMonths(1).toDate()
        Date twoMonthsAgo = LocalDate.now().minusMonths(2).toDate()

        ApplicationTagLib g = grailsApplication.mainContext.getBean('org.grails.plugins.web.taglib.ApplicationTagLib')

        def results = Shipment.executeQuery("""
            select s.shipmentType.id, s.shipmentType.name, s.shipmentNumber, s.name, s.id
            from Shipment as s
            inner join s.currentEvent as e
            where s.destination = :location
            and s.currentStatus in ('SHIPPED', 'PARTIALLY_RECEIVED')
            and (
                (s.shipmentType.id = 1 and e.eventDate < :oneMonthAgo)
                or (s.shipmentType.id = 2 and e.eventDate < :twoMonthsAgo)
                or (s.shipmentType.id in (3, 4) and e.eventDate < :oneWeekAgo)
            )
        """, [
                'location'    : location,
                'oneWeekAgo'  : oneWeekAgo,
                'oneMonthAgo' : oneMonthAgo,
                'twoMonthsAgo': twoMonthsAgo
        ])

        def numberDelayed = [
                air            : 0,
                sea            : 0,
                landAndSuitcase: 0,
        ]

        results = results.collect {
            if (it[0] == '1') numberDelayed['air'] += 1
            else if (it[0] == '2') numberDelayed['sea'] += 1
            else numberDelayed['landAndSuitcase'] += 1
            def shipmentType = LocalizationUtil.getLocalizedString(it[1], new Locale("en"))

            TableData tableData = new TableData(it[2], it[3], null, '/stockMovement/show/' + it[4], g.resource(dir: 'images/icons/shipmentType', file: "ShipmentType${shipmentType}.png"))
            return tableData
        }

        Table table = new Table("Shipment", "Name", null, results)

        ColorNumber delayedShipmentByAir = new ColorNumber(numberDelayed['air'], 'By air')
        ColorNumber delayedShipmentBySea = new ColorNumber(numberDelayed['sea'], 'By sea')
        ColorNumber delayedShipmentByLand = new ColorNumber(numberDelayed['landAndSuitcase'], 'By land')

        NumbersIndicator numbersIndicator = new NumbersIndicator(delayedShipmentByAir, delayedShipmentBySea, delayedShipmentByLand)

        NumberTableData numberTableData = new NumberTableData(table, numbersIndicator)

        GraphData graphData = new GraphData(numberTableData)

        return graphData;
    }

    GraphData getProductsInventoried(Location location) {
        List monthsCount = [3, 6, 9, 12, 0]
        List listPercentageNumbers = []
        Map listErrorSuccessIntervals = [
                3 : [18, 25],
                6 : [36, 50],
                9 : [54, 75],
                12: [75, 95],
                0 : [75, 95],
        ]

        def productInStock = ProductAvailability.executeQuery("""
            SELECT COUNT(distinct pa.product.id) FROM ProductAvailability pa
            WHERE pa.location = :location
            AND pa.quantityOnHand > 0""",
                [
                        'location': location
                ])

        monthsCount.each {
            def subtitle
            def percentage
            def inventoriedProducts

            if (it != 0) {
                subtitle = "< ${it} months"
                LocalDate period = LocalDate.now().minusMonths(it)

                inventoriedProducts = dataService.executeQuery(
                    """
                        SELECT count(distinct p.id)
                        FROM transaction_entry te
                        INNER JOIN inventory_item ii ON te.inventory_item_id = ii.id
                        INNER JOIN product p ON ii.product_id = p.id
                        INNER JOIN transaction t ON te.transaction_id = t.id
                        INNER JOIN transaction_type tt ON t.transaction_type_id = tt.id
                        LEFT JOIN location l ON t.inventory_id = l.inventory_id
                        WHERE l.id = '${location.id}'
                        AND tt.transaction_code = '${TransactionCode.PRODUCT_INVENTORY}'
                        AND t.transaction_date >= '${period}';
                    """
                )
            } else {
                subtitle = "Ever"

                inventoriedProducts = dataService.executeQuery(
                        """
                        SELECT count(distinct p.id)
                        FROM transaction_entry te
                        INNER JOIN inventory_item ii ON te.inventory_item_id = ii.id
                        INNER JOIN product p ON ii.product_id = p.id
                        INNER JOIN transaction t ON te.transaction_id = t.id
                        INNER JOIN transaction_type tt ON t.transaction_type_id = tt.id
                        LEFT JOIN location l ON t.inventory_id = l.inventory_id
                        WHERE l.id = '${location.id}'
                        AND tt.transaction_code = '${TransactionCode.PRODUCT_INVENTORY}';
                    """
                )
            }

            percentage = productInStock[0] == 0 ? 0 : Math.round(inventoriedProducts[0][0] / productInStock[0] * 100)
            ColorNumber colorNumber = new ColorNumber(percentage, subtitle)
            colorNumber.setConditionalColors(listErrorSuccessIntervals.get(it)[0], listErrorSuccessIntervals.get(it)[1])
            colorNumber.value = "${colorNumber.value}%"
            listPercentageNumbers.push(colorNumber)
        }
        MultipleNumbersIndicator multipleNumbersIndicator = new MultipleNumbersIndicator(listPercentageNumbers)

        GraphData productsInventoried = new GraphData(multipleNumbersIndicator)

        return productsInventoried
    }

    GraphData getLossCausedByExpiry(Location location, def params) {

        Integer querySize = params.querySize ? params.querySize.toInteger() - 1 : 5
        LocalDate queryLimit = LocalDate.now().minusMonths(querySize).withDayOfMonth(1)

        def valuesRemovedDueToExpiry = dataService.executeQuery(
            """
                SELECT sum(p.price_per_unit * te.quantity), month(t.transaction_date), year(t.transaction_date)
                FROM transaction_entry te 
                INNER JOIN transaction t ON te.transaction_id = t.id
                INNER JOIN inventory_item ii ON te.inventory_item_id = ii.id
                INNER JOIN product p ON ii.product_id = p.id
                LEFT JOIN location l ON t.inventory_id = l.inventory_id
                WHERE l.id = '${location.id}'
                AND t.transaction_date >= '${queryLimit}'
                AND t.transaction_type_id = 4
                GROUP BY month(t.transaction_date), year(t.transaction_date);
            """
        )

        def valuesNotExpiredLastDayOfMonth = InventorySnapshot.executeQuery("""
            select sum(iis.quantityOnHand * p.pricePerUnit), month(iis.date), year(iis.date)
            from InventorySnapshot as iis
            inner join iis.product as p
            inner join iis.inventoryItem as ii
            where iis.location = :location
            and iis.date >= :limit
            and iis.date = LAST_DAY(iis.date)
            and ii.expirationDate > iis.date
            group by month(iis.date), year(iis.date)
        """,
                [
                        'location': location,
                        'limit'   : queryLimit.toDate()
                ])

        def valuesExpiredLastDayOfMonth = InventorySnapshot.executeQuery("""
            select sum(iis.quantityOnHand * p.pricePerUnit), month(iis.date), year(iis.date)
            from InventorySnapshot as iis
            inner join iis.product as p
            inner join iis.inventoryItem as ii
            where iis.location = :location
            and iis.date >= :limit
            and iis.date = LAST_DAY(iis.date)
            and ii.expirationDate <= iis.date
            group by month(iis.date), year(iis.date)
        """,
                [
                        'location': location,
                        'limit'   : queryLimit.toDate()
                ])

        // Filling the labels
        List listLabels = fillLabels(querySize)

        // Filling the data lists
        // Each data is an array of [value, month, year]
        def filledValuesRemovedDueToExpiry = fillData(valuesRemovedDueToExpiry, querySize, 0, 1, 2)
        def filledValuesNotExpiredLastDayOfMonth = fillData(valuesNotExpiredLastDayOfMonth, querySize, 0, 1, 2)
        def filledValuesExpiredLastDayOfMonth = fillData(valuesExpiredLastDayOfMonth, querySize, 0, 1, 2)

        // Calculating the percentage
        List percentage = [];
        for (int i = 0; i <= querySize; i++) {
            def removedDueToExpiry = filledValuesRemovedDueToExpiry == null ? 0 : filledValuesRemovedDueToExpiry[i]
            def notExpiredLastDayOfMonth = filledValuesNotExpiredLastDayOfMonth == null ? 0 : filledValuesNotExpiredLastDayOfMonth[i]
            def expiredLastDayOfMonth = filledValuesExpiredLastDayOfMonth == null ? 0 : filledValuesExpiredLastDayOfMonth[i]
            def sum = removedDueToExpiry + notExpiredLastDayOfMonth + expiredLastDayOfMonth
            if (sum == 0) {
                percentage.push(0)
            } else {
                percentage.push(Math.round((removedDueToExpiry / sum) * 100) / 100)
            }
        }

        List<IndicatorDatasets> datasets = [
                new IndicatorDatasets('Percentage removed due to expiry', percentage, null, 'line'),
                new IndicatorDatasets('Inventory value not expired last day of month', filledValuesNotExpiredLastDayOfMonth, null, 'bar'),
                new IndicatorDatasets('Inventory value expired last day of month', filledValuesExpiredLastDayOfMonth, null, 'bar'),
                new IndicatorDatasets('Inventory value removed due to expiry', filledValuesRemovedDueToExpiry, null, 'bar'),
        ]

        IndicatorData indicatorData = new IndicatorData(datasets, listLabels)

        GraphData graphData = new GraphData(indicatorData)

        return graphData
    }

    GraphData getPercentageAdHoc(Location location) {
        Calendar calendar = Calendar.instance
        // we need to get all requisitions that were created from the first day of the previous month
        // current month is an edge case that we need to handle
        if (calendar.get(Calendar.MONTH) == 0) {
            calendar.set(calendar.get(Calendar.YEAR) -1, 11, 1, 0, 0, 0)
        } else {
            calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) -1, 1, 0, 0, 0)
        }
        def firstDayOfPreviousMonth = calendar.getTime()
        // first day of current month is needed for time frames in the query
        calendar = Calendar.instance
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 1, 0, 0, 0)
        def firstDayOfCurrentMonth = calendar.getTime()

        List<String> listLabels = []
        List<Integer> listData = []

        def percentageAdHoc = Requisition.executeQuery("""
            select count(r.id), r.type
            from Requisition as r
            where r.origin.id = :location
            and r.requestedDeliveryDate >= :firstDayOfPreviousMonth 
            and r.requestedDeliveryDate < :firstDayOfCurrentMonth
            group by r.type
        """,
                [
                        'location': location.id,
                        'firstDayOfPreviousMonth': firstDayOfPreviousMonth,
                        'firstDayOfCurrentMonth': firstDayOfCurrentMonth,
                ])

        percentageAdHoc.each {
            if (RequisitionType.listRequestTypes().contains(it[1])) {
                listLabels.push(it[1].toString())
                listData.push(it[0])
            }
        }

        List<IndicatorDatasets> datasets = [
                new IndicatorDatasets('Number of requests', listData, null , 'doughnut')
        ]

        IndicatorData indicatorData = new IndicatorData(datasets, listLabels)

        GraphData graphData = new GraphData(indicatorData, '/stockMovement/list?direction=OUTBOUND')

        return graphData
    }

    GraphData getStockOutLastMonth(Location location) {

        List<String> listLabels = []
        List<Integer> listData = []

        def stockOutLastMonth = dataService.executeQuery("""
            select count(pss.product_id), pss.stockout_status 
            from product_stockout_status as pss
            where pss.location_id = :location
            group by pss.stockout_status
        """,
                [
                        'location': location.id,
                ]);

        stockOutLastMonth.each {
                listLabels.push(it[1].toString())
                listData.push(it[0])
        }

        List<IndicatorDatasets> datasets = [
                new IndicatorDatasets('Number of stockout', listData, null , 'doughnut')
        ]

        IndicatorData indicatorData = new IndicatorData(datasets, listLabels)

        GraphData graphData = new GraphData(indicatorData)

        return graphData
    }

    private List fillLabels(int querySize) {
        Date today = new Date()
        today.clearTime()

        List labels = []

        for (int i = querySize; i >= 0; i--) {
            Date tmpDate = today.clone()
            tmpDate.set(month: today.month - i, date: 1)

            String monthLabel = new java.text.DateFormatSymbols().months[tmpDate.month].substring(0, 3)
            String yearLabel = tmpDate.year + 1900
            labels.push("${monthLabel} ${yearLabel}")
        }

        return labels
    }

    private List fillData(List dataList, int querySize, int dataIndex, int monthIndex, int yearIndex) {
        Date today = new Date()
        today.clearTime()

        List filledList = [0] * (querySize + 1)

        dataList.each {
            for (int i = querySize; i >= 0; i--) {
                Date tmpDate = today.clone()
                tmpDate.set(month: today.month - i, date: 1)

                // If there is data, update the dataset in the proper position
                if (tmpDate.month == it[monthIndex] - 1 && tmpDate.year + 1900 == it[yearIndex]) {
                    filledList[querySize - i] = it[dataIndex]
                }
            }
        }

        return filledList;
    }
}
