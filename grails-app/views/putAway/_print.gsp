<%@ page import="org.pih.warehouse.core.RoleType" %>
<%@ page contentType="text/html;charset=UTF-8" defaultCodec="html" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
    <style>

        thead {display: table-header-group;}
        tr {page-break-inside: avoid; page-break-after: auto;}
        td {vertical-align: top; }
        th { background-color: lightgrey; font-weight: bold;}
        h2 { text-align: center; }
        body { font-size: 11px; }
        * {
            padding: 0;
            margin: 0;
        }
        div.header {
            display: block;
            text-align: center;
            position: running(header);
        }
        div.footer {
            display: block;
            text-align: center;
            position: running(footer);
        }

        @page {
            size: letter; /*letter landscape*/
            background: white;
            @top-center { content: element(header) }
            @bottom-center { content: element(footer) }
        }
        .small {font-size: xx-small;}
        .line{border-bottom: 1px solid black}
        .logo { width: 20px; height: 20px; }
        .canceled { text-decoration: line-through; }
        .page-start {
            -fs-page-sequence: start;
            page-break-before: avoid;
        }
        table {
            -fs-table-paginate: paginate;
            page-break-inside: avoid;
            border: thin solid black;
            border-collapse: collapse;
            border-spacing: 0;
            margin: 5px;

        }
        .page-content {
            page-break-before: always;
            page-break-after: avoid;

        }

        .page-header {
            page-break-before: avoid;
        }

        /* forces a page break */
        .break {page-break-after:always}

        span.page:before { content: counter(page); }
        span.pagecount:before { content: counter(pages); }
        body {
            font: 11px "lucida grande", verdana, arial, helvetica, sans-serif;
        }
        table td, table th {
            padding: 5px;
            border: thin solid black;
        }
    </style>

</head>

<body>
    <div class="footer">
        <div id="page-footer" class="small">
            <div>
                Page <span class="page" /> of <span class="pagecount" />
            </div>
        </div>
    </div>

    <div class="content">
        <table style="white-space: nowrap;">
            <tr>
                <th>
                    <g:message code="putawayOrder.label" />
                </th>
                <td>
                    ${jsonObject?.putawayNumber}
                </td>
            </tr>
            <tr>
                <th><g:message code="putawayOrder.createdBy.label"/></th>
                <td width="50%">${jsonObject?.orderedBy}</td>
            </tr>
            <tr>
                <th><g:message code="default.dateCreated.label"/></th>
                <td width="50%">${jsonObject?.dateCreated}</td>
            </tr>
        </table>
        <h2><warehouse:message code="putawayOrder.label"/></h2>
        <table>
            <thead>
                <tr>
                    <th>Code</th>
                    <th style="width: 100px;" >Name</th>
                    <th>Lot/Serial No.</th>
                    <th>Expiry</th>
                    <th style="width: 30px;">Total Qty</th>
                    <th style="width: 50px;">Putaway Quantity</th>
                    <th style="width: 50px;">Preferred Bin</th>
                    <th style="width: 70px;">Current Bins</th>
                    <th style="width: 130px;">Putaway Bin</th>
                </tr>
            </thead>
            <g:each var="putawayItem" in="${jsonObject.putawayItems}">
                <g:if test="${putawayItem.splitItems.empty}">
                    <tr>
                        <format:td_spacewrap>${putawayItem["product.productCode"]}</format:td_spacewrap>
                        <format:td_spacewrap>${putawayItem["product.name"]}</format:td_spacewrap>
                        <format:td_spacewrap>${putawayItem["inventoryItem.lotNumber"]}</format:td_spacewrap>
                        <format:td_spacewrap>${putawayItem["inventoryItem.expirationDate"]}</format:td_spacewrap>
                        <format:td_spacewrap>${putawayItem?.quantity}</format:td_spacewrap>
                        <format:td_spacewrap>${putawayItem?.quantity}</format:td_spacewrap>
                        <format:td_spacewrap>${(putawayItem["preferredBin.zoneName"] ? putawayItem["preferredBin.zoneName"] + ": " : '') + putawayItem["preferredBin.name"]}</format:td_spacewrap>
                        <format:td_spacewrap>${putawayItem["currentBins"]}</format:td_spacewrap>
                        <format:td_spacewrap>${(putawayItem["putawayLocation.zoneName"] ? putawayItem["putawayLocation.zoneName"] + ": " : '') + putawayItem["putawayLocation.name"]}</format:td_spacewrap>
                    </tr>
                </g:if>
                <g:else>
                    <g:each var="splitItem" in="${putawayItem.splitItems}" status="status">
                        <tr>
                            <format:td_spacewrap>${status==0 ? putawayItem["product.productCode"] : ""}</format:td_spacewrap>
                            <format:td_spacewrap>${status==0 ? putawayItem["product.name"]: ""}</format:td_spacewrap>
                            <format:td_spacewrap>${status==0 ? putawayItem["inventoryItem.lotNumber"]: ""}</format:td_spacewrap>
                            <format:td_spacewrap>${status==0 ? putawayItem["inventoryItem.expirationDate"]: ""}</format:td_spacewrap>
                            <format:td_spacewrap>${status==0 ? putawayItem?.quantity: ""}</format:td_spacewrap>
                            <format:td_spacewrap>${splitItem["quantity"]?:""}</format:td_spacewrap>
                            <format:td_spacewrap>${(putawayItem["preferredBin.zoneName"] ? putawayItem["preferredBin.zoneName"] + ": " : '') + putawayItem["preferredBin.name"]}</format:td_spacewrap>
                            <format:td_spacewrap>${putawayItem["currentBins"]}</format:td_spacewrap>
                            <format:td_spacewrap>${(splitItem["putawayLocation.zoneName"] ? splitItem["putawayLocation.zoneName"] + ": " : '') + splitItem["putawayLocation.name"]}</format:td_spacewrap>
                        </tr>
                    </g:each>

                </g:else>
            </g:each>
        </table>

        <br />

        <table style="white-space: nowrap;">
            <tr>
                <th><g:message code="putawayOrder.completedBy.label"/></th>
                <td width="50%"></td>
            </tr>
            <tr>
                <th><g:message code="putawayOrder.putawayDate.label"/></th>
                <td></td>
            </tr>
            <tr>
                <th><g:message code="default.signature.label"/></th>
                <td></td>
            </tr>
        </table>


</div>

</body>
</html>
