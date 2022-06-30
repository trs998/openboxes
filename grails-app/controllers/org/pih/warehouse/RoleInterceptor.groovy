package org.pih.warehouse
import org.pih.warehouse.core.RoleType

/**
 * Copyright (c) 2012 Partners In Health.  All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 * */
class RoleInterceptor {
    def userService
    def dependsOn = [SecurityInterceptor]
    def static changeActions = ['delete', 'create', 'add', 'process', 'save',
                                'update', 'importData', 'receive', 'showRecordInventory', 'withdraw', 'cancel', 'change', 'toggle', 'exportAsCsv']
    def static changeControllers = []

    def static managerActions = [
            'stockMovementItemApi': ['eraseItem']
    ]

    def static adminControllers = ['createProduct', 'admin']
    def static adminActions = [
            'product'      : ['create'],
            'person'       : ['list'],
            'user'         : ['list'],
            'location'     : ['edit'],
            'shipper'      : ['create'],
            'locationGroup': ['create'],
            'locationType' : ['create']
    ]

    def static superuserControllers = []
    def static superuserActions = [
            '*'                         : ['delete'],
            'console'                   : ['index', 'execute'],
            'inventory'                 : ['createInboundTransfer', 'createOutboundTransfer', 'createConsumed', 'editTransaction', 'deleteTransaction', 'saveTransaction'],
            'inventoryItem'             : ['adjustStock', 'transferStock'],
            'productCatalog'            : ['create', 'importProductCatalog'],
            'productType'               : ['edit', 'delete', 'save', 'update'],
            'transactionEntry'          : ['edit', 'delete', 'save', 'update'],
            'user'                      : ['impersonate'],
            'productsConfigurationApi'  : ['downloadCategories', 'importCategories']
    ]

    def static invoiceActions = [
            'invoice': ['*']
    ]

    def static requestorOrManagerActions = [
            'api'                 : ['getAppContext', 'getRequestTypes', 'getMenuConfig'],
            'dashboard'           : ['megamenu'],
            'dashboardApi'        : ['breadcrumbsConfig'],
            'grails'              : ['errors'],
            'localizationApi'     : ['list'],
            'locationApi'         : ['list'],
            'productApi'          : ['list', 'productDemand', 'productAvailabilityAndDemand'],
            'stocklistApi'        : ['list'],
            'stockMovement'       : ['list', 'createRequest'],
            'stockMovementApi'    : ['updateItems', 'create', 'updateStatus', 'read'],
            'stockMovementItemApi': ['getStockMovementItems']
    ]

    public RoleInterceptor() {
            'api'                 : ['getAppContext', 'getRequestTypes', 'getMenuConfig'],
            'dashboard'           : ['megamenu'],
        matchAll().except(uri: '/static/**').except(controller: "errors").except(uri: "/info").except(uri: "/health")
            'grails'              : ['errors'],
            'localizationApi'     : ['list'],
            'locationApi'         : ['list'],
            'productApi'          : ['list', 'productDemand', 'productAvailabilityAndDemand'],
            'stocklistApi'        : ['list'],
            'stockMovement'       : ['list'],
    }

    boolean before() {

        // Anonymous
        if (SecurityInterceptor.actionsWithAuthUserNotRequired.contains(actionName) || actionName == "chooseLocation" ||
                SecurityInterceptor.controllersWithAuthUserNotRequired.contains(controllerName)) {
            return true
        }

        // Authorized users
                def isNotAuthenticated = !userService.isUserInRole(session.user, RoleType.ROLE_AUTHENTICATED)
        def missBrowser = !userService.canUserBrowse(session.user)
        def missManager = needManager(controllerName, actionName) && !userService.isUserManager(session.user)
        def missAdmin = needAdmin(controllerName, actionName) && !userService.isUserAdmin(session.user)
        def missSuperuser = needSuperuser(controllerName, actionName) && !userService.isSuperuser(session.user)
                def hasNoRoleInvoice = needInvoice(controllerName, actionName) && !userService.hasRoleInvoice(session.user)
                def isNotRequestor = needRequestorOrManager(controllerName, actionName) && !userService.isUserRequestor(session.user)
                def isNotRequestorOrManager = needRequestorOrManager(controllerName, actionName) ? !userService.isUserManager(session.user) && !userService.isUserRequestor(session.user) : false

        if (missBrowser || missManager || missAdmin || missSuperuser) {
            log.info("User ${session?.user?.username} does not have access to ${controllerName}/${actionName} in location ${session?.warehouse?.name}")
            redirect(controller: "errors", action: "handleForbidden")
            return false
        }
        return true
    }

    static Boolean needSuperuser(controllerName, actionName) {
        superuserControllers?.contains(controllerName) || superuserActions[controllerName]?.contains(actionName) || superuserActions['*'].any {
            actionName?.startsWith(it)
        }
    }

    static Boolean needAdmin(controllerName, actionName) {
        adminControllers?.contains(controllerName) || adminActions[controllerName]?.contains(actionName) || adminActions['*'].any {
            actionName?.startsWith(it)
        }
    }

    static Boolean needManager(controllerName, actionName) {
        def isChangeAction = changeActions.any {
            actionName?.startsWith(it)
        }
        def isWorkflow = controllerName?.contains("Workflow")
        def isChangeController = changeControllers?.contains(controllerName)
        def isManagerAction = managerActions[controllerName]?.contains(actionName)
        return isChangeAction || isWorkflow || isChangeController || isManagerAction
    }

    static Boolean needInvoice(controllerName, actionName) {
        invoiceActions[controllerName]?.contains("*") || invoiceActions[controllerName]?.contains(actionName)
    }

    static Boolean needRequestorOrManager(controllerName, actionName) {
        requestorOrManagerActions[controllerName]?.contains(actionName)
    }

    static Boolean needAuthenticatedActions(controllerName, actionName) {
        authenticatedActions[controllerName]?.contains(actionName)
    }
}
