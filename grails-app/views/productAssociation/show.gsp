
<%@ page import="org.pih.warehouse.product.ProductAssociation" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="custom" />
        <g:set var="entityName" value="${warehouse.message(code: 'productAssociation.label', default: 'ProductAssociation')}" />
        <title><warehouse:message code="default.show.label" args="[entityName]" /></title>
    </head>
    <body>
        <div class="body">
            <g:if test="${flash.message}">
	            <div class="message">${flash.message}</div>
            </g:if>
            <g:hiddenField id="productAssociationId" name="productAssociationInstance" value="${productAssociationInstance?.id}"/>
            <div class="box">
                <h2><warehouse:message code="default.show.label" args="[entityName]" /></h2>
                <table>
                    <tbody>
                    
                        <tr class="prop">
                            <td valign="top" class="name"><warehouse:message code="productAssociation.id.label" default="Id" /></td>
                            
                            <td valign="top" class="value">${fieldValue(bean: productAssociationInstance, field: "id")}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name"><warehouse:message code="productAssociation.code.label" default="Code" /></td>
                            
                            <td valign="top" class="value">${productAssociationInstance?.code?.encodeAsHTML()}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name"><warehouse:message code="productAssociation.associatedProduct.label" default="Associated Product" /></td>
                            
                            <td valign="top" class="value"><g:link controller="product" action="show" id="${productAssociationInstance?.associatedProduct?.id}">${productAssociationInstance?.associatedProduct?.encodeAsHTML()}</g:link></td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name"><warehouse:message code="productAssociation.quantity.label" default="Quantity" /></td>
                            
                            <td valign="top" class="value">${fieldValue(bean: productAssociationInstance, field: "quantity")}</td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name"><warehouse:message code="productAssociation.comments.label" default="Comments" /></td>
                            
                            <td valign="top" class="value">${fieldValue(bean: productAssociationInstance, field: "comments")}</td>
                            
                        </tr>

                        <tr class="prop">
                            <td class="name">
                                <warehouse:message code="productAssociation.mutualAssociation.label" default="Two-way Association"/>
                            </td>
                            <td class="value">
                                <g:if test="${productAssociationInstance?.mutualAssociation}">
                                    ${warehouse.message(code:'default.yes.label')}
                                </g:if>
                                <g:else>
                                    ${warehouse.message(code:'default.no.label')}
                                </g:else>
                            </td>
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name"><warehouse:message code="productAssociation.dateCreated.label" default="Date Created" /></td>
                            
                            <td valign="top" class="value"><format:datetime obj="${productAssociationInstance?.dateCreated}" /></td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name"><warehouse:message code="productAssociation.lastUpdated.label" default="Last Updated" /></td>
                            
                            <td valign="top" class="value"><format:datetime obj="${productAssociationInstance?.lastUpdated}" /></td>
                            
                        </tr>
                    
                        <tr class="prop">
                            <td valign="top" class="name"><warehouse:message code="productAssociation.product.label" default="Product" /></td>
                            
                            <td valign="top" class="value"><g:link controller="product" action="show" id="${productAssociationInstance?.product?.id}">${productAssociationInstance?.product?.encodeAsHTML()}</g:link></td>
                            
                        </tr>
                    
                    
						<tr class="prop">
                        	<td valign="top"></td>
                        	<td valign="top">                         
					            <div class="buttons left">
					                <g:form>
					                    <g:hiddenField name="id" value="${productAssociationInstance?.id}" />
					                    <g:actionSubmit class="button" action="edit" value="${warehouse.message(code: 'default.button.edit.label', default: 'Edit')}" />
                                        <g:if test="${productAssociationInstance?.mutualAssociation}">
                                            <button type="button"
                                                    class="button"
                                                    onclick="$('#product-association-delete-dialog')
                                                      .data('productAssociationId', `${productAssociationInstance?.id}`)
                                                      .dialog('open')">
                                                ${warehouse.message(code: 'default.button.delete.label', default: 'Delete')}
                                            </button>
                                        </g:if>
                                        <g:else>
                                            <g:actionSubmit class="button" action="delete" value="${warehouse.message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('${warehouse.message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
                                        </g:else>
					                </g:form>
					            </div>
							</td>
						</tr>                    
                    </tbody>
                </table>
            </div>
            <g:render template="/productAssociation/productAssociationDeleteDialog" />
        </div>
    </body>
</html>
