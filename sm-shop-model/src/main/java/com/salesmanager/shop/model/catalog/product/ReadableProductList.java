package com.salesmanager.shop.model.catalog.product;

import com.salesmanager.shop.model.entity.ReadableList;

import java.util.ArrayList;
import java.util.List;

public class ReadableProductList extends ReadableList {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<ReadableProduct> products = new ArrayList<ReadableProduct>();
	public void setProducts(List<ReadableProduct> products) {
		this.products = products;
	}
	public List<ReadableProduct> getProducts() {
		return products;
	}

}
