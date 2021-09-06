<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="mobile" />
    <title><warehouse:message code="default.inbound.label" default="Inbound"/></title>
</head>

<body>

<div class="row g-0 mb-2">

    <div class="col col-md-6">
        <g:form controller="mobile" action="inboundList" method="GET">
            <div class="form-group">
                <g:select class="btn btn-outline-secondary"
                          name="status" from="${['TRANSIT':'IN_TRANSIT'].entrySet()}"
                          value="${params.status}"
                          optionKey="value" optionValue="value"
                          noSelection="['':' - Choose a Status - ']"/>
                <input type="submit" value="Filter" class="btn btn-outline-primary">
            </div>
        </g:form>
    </div>
    <div class="col col-md-6">
        <button type="button" class="btn btn-outline-primary float-end"
            data-bs-toggle="modal" data-bs-target="#inboundModal"><i class="fa fa-file-import"></i> Import Orders</button>
    </div>
</div>
<div class="row g-0">
    <div class="col">
        <table class="table table-borderless table-striped">
            <thead>
                <tr>
                    <th><g:message code="stockMovement.status.label" default="Status"/></th>
                    <th><g:message code="requisition.orderNumber.label"/></th>
                    <th><g:message code="stockMovement.origin.label"/></th>
                    <th><g:message code="stockMovement.destination.label" default="Destination"/></th>
                    <th><g:message code="stockMovement.requestedDeliveryDate.label" default="Requested Delivery Date"/></th>
                    <th><g:message code="default.actions.label" default="Actions"/></th>
                </tr>
            </thead>
            <tbody>
            <g:each var="stockMovement" in="${stockMovements}">
                <tr>
                    <td>
                        <a href="${createLink(controller: 'stockMovement', action: 'show', id: stockMovement?.id)}" class="text-decoration-none text-reset">
                            <g:if test="${stockMovement?.shipment?.currentEvent?.eventType?.eventCode}">
                                <div class="badge bg-primary">
                                    ${stockMovement.shipment?.currentEvent?.eventType?.eventCode}
                                </div>
                                <div>
                                    <small><g:formatDate date="${stockMovement.shipment?.currentEvent?.eventDate}" format="MMM dd hh:mm a"/></small>
                                </div>
                            </g:if>
                            <g:else>
                                <div class="badge bg-primary">${stockMovement?.status}</div>
                            </g:else>
                        </a>
                    </td>
                    <td>
                        <a href="${createLink(controller: 'mobile', action: 'inboundDetails', id: stockMovement?.id)}" class="text-decoration-none text-reset">
                            ${stockMovement.identifier}
                        </a>
                    </td>
                    <td>
                        ${stockMovement?.origin?.name}
                    </td>
                    <td>
                        ${stockMovement?.destination?.name} ${stockMovement?.destination?.locationNumber}
                    </td>
                    <td>
                        <g:formatDate date="${stockMovement?.expectedDeliveryDate}" format="dd MMM yyyy"/>
                    </td>
                    <td>
                        <a href="${createLink(controller: 'mobile', action: 'inboundDetails', id: stockMovement?.id)}" class="btn btn-link">
                            <button class="btn btn-outline-primary">Details</button>
                        </a>
                    </td>
                </tr>
            </g:each>
            </tbody>
        </table>
        <div class="pagination">
            <g:paginate total="${stockMovements.totalCount}"/>
        </div>
    </div>
</div>

<!-- Modal -->
<div class="modal fade" id="inboundModal" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" aria-labelledby="staticBackdropLabel" aria-hidden="true">
    <g:uploadForm class="needs-validation" action="importData" enctype="multipart/form-data">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="createInboundBack">Create Inbound Order</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <input name="type" type="hidden" value="inbound"/>
                    <g:hiddenField name="location.id" value="${session.warehouse.id }"/>
                    <input class="form-control" type="file" name="xlsFile" required>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    <button type="submit" class="btn btn-primary">Upload</button>
                </div>
            </div>
        </div>
    </g:uploadForm>
</div>

</body>
</html>