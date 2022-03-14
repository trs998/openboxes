/**
 * Copyright (c) 2012 Partners In Health.  All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 **/
package org.pih.warehouse.product

import org.pih.warehouse.auth.AuthService
import org.pih.warehouse.core.ProductPrice
import org.pih.warehouse.core.UnitOfMeasure
import org.pih.warehouse.core.User

class ProductPackage implements Comparable<ProductPackage>, Serializable {

    def beforeInsert = {
        createdBy = updatedBy = AuthService.currentUser.get()
    }

    def beforeUpdate = {
        updatedBy = AuthService.currentUser.get()
    }

    String id
    String name                // Name of product as it appears on the package
    String description        // Description of the package
    String gtin                // Global trade identification number
    Integer quantity        // Number of units (each) in the box
    UnitOfMeasure uom        // Unit of measure of the package (e.g. box, case, etc)
    ProductPrice productPrice

    ProductSupplier productSupplier

    // Auditing
    Date dateCreated
    Date lastUpdated
    User createdBy
    User updatedBy

    static belongsTo = [product: Product, productSupplier: ProductSupplier]
    static mapping = {
        id generator: 'uuid'
        cascade productPrice: "all-delete-orphan"
    }

    static constraints = {
        name(nullable: true)
        description(nullable: true)
        gtin(nullable: true)
        uom(nullable: true)
        productPrice(nullable: true, unique: true)
        createdBy(nullable: true)
        updatedBy(nullable: true)
    }

    String toString() {
        return name
    }

    ProductPrice createOrGetProductPrice() {
        if (!productPrice) {
            productPrice = new ProductPrice()
            productPrice.productPackage = this
        }
        return productPrice
    }

    /**
     * Sort by quantity
     */
    @Override
    int compareTo(ProductPackage other) {
        return other.quantity <=> quantity
    }

    static PROPERTIES = [
            "id"                 : "id",
            "productCode"        : "product.productCode",
            "productSupplierCode": "productSupplier.code",
            "name"               : "name",
            "description"        : "description",
            "gtin"               : "gtin",
            "uomCode"            : "uom.code",
            "quantity"           : "quantity",
            "price"              : "productPrice.price"
    ]

}
