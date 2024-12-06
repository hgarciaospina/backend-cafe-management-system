package com.inn.cafe.dao;

import com.inn.cafe.pojo.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * DAO interface for Category entity.
 * Provides methods for custom queries and default CRUD operations.
 */
public interface CategoryDao extends JpaRepository<Category, Integer> {

    /**
     * Retrieves all categories that are linked to active products.
     *
     * @return List of categories.
     */
    List<Category> getAllCategory();

    /**
     * Checks if a category with the given ID exists.
     *
     * @param id The ID of the category to check.
     * @return true if the category exists, false otherwise.
     */
    boolean existsById(Integer id); // This is handled natively by Spring Data JPA
}
