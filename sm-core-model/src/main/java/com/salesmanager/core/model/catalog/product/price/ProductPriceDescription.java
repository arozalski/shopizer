package com.salesmanager.core.model.catalog.product.price;

import com.salesmanager.core.constants.SchemaConstant;
import com.salesmanager.core.model.common.description.Description;

import javax.persistence.*;

@Entity
@Table(name="PRODUCT_PRICE_DESCRIPTION", schema=SchemaConstant.SALESMANAGER_SCHEMA, uniqueConstraints={
		@UniqueConstraint(columnNames={
			"PRODUCT_PRICE_ID",
			"LANGUAGE_ID"
		})
	}
)
public class ProductPriceDescription extends Description {;
	
	/**
   * 
   */
  private static final long serialVersionUID = 1L;

  public final static String DEFAULT_PRICE_DESCRIPTION = "DEFAULT";
	
	@ManyToOne(targetEntity = ProductPrice.class)
	@JoinColumn(name = "PRODUCT_PRICE_ID", nullable = false)
	private ProductPrice productPrice;
	
	public ProductPriceDescription() {
	}

	public ProductPrice getProductPrice() {
		return productPrice;
	}

	public void setProductPrice(ProductPrice productPrice) {
		this.productPrice = productPrice;
	}


}
