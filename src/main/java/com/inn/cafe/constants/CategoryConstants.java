package com.inn.cafe.constants;

/**
 * Constants specific to the Category module.
 */
public class CategoryConstants {

    private CategoryConstants() {
    }

    // Keys for request maps
    public static final String NAME = "name";
    public static final String ID = "id";

    // Messages with placeholders for IDs
    public static final String CATEGORY_ADDED_SUCCESS = "Category Added Successfully";
    public static final String CATEGORY_UPDATED_SUCCESS = "Category Updated Successfully";
    public static final String CATEGORY_NOT_FOUND = "Category with id %d does not exist.";
}