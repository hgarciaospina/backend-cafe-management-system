package com.inn.cafe.constants;

/**
 * Constants specific to the Product module.
 */
public class ProductConstants {

    public ProductConstants() {
    }

    // Keys for request maps
    public static final String NAME = "name";
    public static final String CATEGORY_ID = "categoryId";
    public static final String PRICE = "price";
    public static final String DESCRIPTION = "description";
    public static final String ID = "id";

    // Messages
    public static final String PRODUCT_ADDED_SUCCESS = "Product Added Successfully.";
    public static final String PRODUCT_UPDATED_SUCCESS = "Product Updated Successfully.";
    public static final String PRODUCT_NOT_FOUND = "Product with id %d does not exist.";
    public static final String CATEGORY_NOT_FOUND = "Category with id %d does not exist.";
    public static final String INVALID_DATA = "Invalid data";
}
