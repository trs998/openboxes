<%@ page import="org.pih.warehouse.order.OrderItemStatusCode" %>
<%@ page import="org.pih.warehouse.order.OrderType" %>
<%@ page import="org.pih.warehouse.order.OrderTypeCode" %>
<%@ page import="org.pih.warehouse.core.Constants;" %>


<script>
  $(document).ready(function() {
    $("#orderItemsDetailsFilter").keyup(function(event){
      const filterCell = 1; // product name
      const filterValue = $("#orderItemsDetailsFilter")
        .val()
        .toUpperCase();
      filterTableItemDetails(filterCell, filterValue)
    });

  });
  function filterTableItemDetails(cellIndex, filterValue) {
    const tableRows = $("#order-items-details tr.dataRowItemDetails");
    // Loop through all table rows, and hide those who don't match the search query
    $.each(tableRows, function(index, currentRow) {
      // If filter matches text value then we display, otherwise hide
      const txtValue = $(currentRow)
        .find("td")
        .eq(cellIndex)
        .text();
      if (txtValue.toUpperCase().indexOf(filterValue) > -1) {
        $(currentRow).show();
      } else {
        $(currentRow).hide();
      }
    });
  }
</script>

<div class="item-details-table">
    <g:if test="${orderInstance.orderType != OrderType.findByCode(Constants.PUTAWAY_ORDER)}">
        <div class="filters-container">
            <label class="name"><warehouse:message code="inventory.filterByProduct.label"/></label>
            <div>
                <input type="text" id="orderItemsDetailsFilter" class="text large" placeholder="Filter by product name"/>
            </div>
        </div>
    </g:if>
    <div id="tab-content" class="box">
        <h2>
            <warehouse:message code="order.itemDetails.label" default="Item Details"/>
        </h2>
        <g:if test="${orderInstance?.orderItems }">
            <table class="table table-bordered" id="order-items-details">
                <thead>
                <tr class="odd">
                    <th><warehouse:message code="product.productCode.label" /></th>
                    <th><warehouse:message code="product.label" /></th>
                    <th class="center"><warehouse:message code="product.supplierCode.label"/></th>
                    <th class="center"><warehouse:message code="product.manufacturer.label"/></th>
                    <th class="center"><warehouse:message code="product.manufacturerCode.label"/></th>
                    <th class="center"><warehouse:message code="orderItem.quantity.label"/></th>
                    <th class="center"><warehouse:message code="product.uom.label"/></th>
                    <th class="center"><warehouse:message code="orderItem.recipient.label"/></th>
                    <th class="center"><warehouse:message code="orderItem.estimatedReadyDate.label"/></th>
                    <th class="center"><warehouse:message code="orderItem.actualReadyDate.label"/></th>
                    <th class="center"><warehouse:message code="orderItem.budgetCode.label"/></th>
                </tr>
                </thead>
                <tbody>
                <g:each var="orderItem" in="${orderInstance?.orderItems?.sort { a,b -> a.dateCreated <=> b.dateCreated ?: a.orderIndex <=> b.orderIndex }}" status="i">
                    <g:set var="isItemCanceled" value="${orderItem.orderItemStatusCode == OrderItemStatusCode.CANCELED}"/>
                    <g:if test="${!isItemCanceled || orderInstance?.orderType==OrderType.findByCode(OrderTypeCode.PURCHASE_ORDER.name())}">
                        <tr class="order-item ${(i % 2) == 0 ? 'even' : 'odd'} dataRowItemDetails" style="${isItemCanceled ? 'background-color: #ffcccb;' : ''}">
                            <td>
                                ${orderItem?.product?.productCode?:""}
                            </td>
                            <td class="order-item-product">
                                <g:link controller="inventoryItem" action="showStockCard" params="['product.id':orderItem?.product?.id]">
                                    <format:product product="${orderItem?.product}"/>
                                    <g:renderHandlingIcons product="${orderItem?.product}" />
                                </g:link>
                            </td>
                            <g:if test="${!isItemCanceled}">
                                <td class="center">
                                    ${orderItem?.productSupplier?.supplierCode}
                                </td>
                                <td class="center">
                                    ${orderItem?.productSupplier?.manufacturerName}
                                </td>
                                <td class="center">
                                    ${orderItem?.productSupplier?.manufacturerCode}
                                </td>
                                <td class="center">
                                    ${orderItem?.quantity }
                                </td>
                                <td class="center">
                                    ${orderItem?.unitOfMeasure}
                                </td>
                                <td class="center">
                                    ${orderItem?.recipient}
                                </td>
                                <td class="center">
                                    <g:formatDate date="${orderItem?.estimatedReadyDate}" format="dd/MMM/yyyy"/>
                                </td>
                                <td class="center">
                                    <g:formatDate date="${orderItem?.actualReadyDate}" format="dd/MMM/yyyy"/>
                                </td>
                                <td class="center">
                                    ${orderItem?.budgetCode?.code}
                                </td>
                            </g:if>
                            <g:else>
                                <td colspan="9"></td>
                            </g:else>
                        </tr>
                    </g:if>
                </g:each>
                </tbody>
            </table>
        </g:if>
        <g:else>
            <div class="fade center empty"><warehouse:message code="default.noItems.label" /></div>
        </g:else>
    </div>
</div>
