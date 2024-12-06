package com.inn.cafe.serviceimpl;

import com.google.common.base.Strings;
import com.inn.cafe.constants.CafeConstants;
import com.inn.cafe.constants.CategoryConstants;
import com.inn.cafe.dao.CategoryDao;
import com.inn.cafe.jwt.JwtFilter;
import com.inn.cafe.pojo.Category;
import com.inn.cafe.service.CategoryService;
import com.inn.cafe.utils.CafeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private JwtFilter jwtFilter;

    @Override
    public ResponseEntity<String> addNewCategory(Map<String, String> requestMap) {
        try {
            if (jwtFilter.isAdmin()) {
                if (validateCategoryMap(requestMap, false)) {
                    categoryDao.save(getCategoryFromMap(requestMap, false));
                    return CafeUtils.getResponseEntity(CategoryConstants.CATEGORY_ADDED_SUCCESS, HttpStatus.OK);
                }
            } else {
                return CafeUtils.getResponseEntity(CafeConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean validateCategoryMap(Map<String, String> requestMap, boolean validateId) {
        boolean hasName = requestMap.containsKey(CategoryConstants.NAME);
        boolean hasValidId = validateId && requestMap.containsKey(CategoryConstants.ID);
        return hasName && (!validateId || hasValidId);
    }

    private Category getCategoryFromMap(Map<String, String> requestMap, boolean idAdd) {
        Category category = new Category();
        if (idAdd) {
            category.setId(Integer.parseInt(requestMap.get(CategoryConstants.ID)));
        }
        category.setName(requestMap.get(CategoryConstants.NAME));
        return category;
    }

    @Override
    public ResponseEntity<List<Category>> getAllCategory(String filterValue) {
        try {
            if (!Strings.isNullOrEmpty(filterValue) && filterValue.equalsIgnoreCase("true")) {
                log.info("Inside if");
                return new ResponseEntity<>(categoryDao.getAllCategory(), HttpStatus.OK);
            }
            return new ResponseEntity<>(categoryDao.findAll(), HttpStatus.OK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> updateCategory(Map<String, String> requestMap) {
        try {
            if (jwtFilter.isAdmin()) {
                if (validateCategoryMap(requestMap, true)) {
                    Optional<Category> optional = categoryDao.findById(Integer.parseInt(requestMap.get(CategoryConstants.ID)));
                    if (optional.isPresent()) {
                        categoryDao.save(getCategoryFromMap(requestMap, true));
                        return CafeUtils.getResponseEntity(CategoryConstants.CATEGORY_UPDATED_SUCCESS, HttpStatus.OK);
                    } else {
                        // Use String.format to insert the ID dynamically
                        String errorMessage = String.format(CategoryConstants.CATEGORY_NOT_FOUND, Integer.parseInt(requestMap.get(CategoryConstants.ID)));
                        return CafeUtils.getResponseEntity(errorMessage, HttpStatus.NOT_FOUND);
                    }
                }
                return CafeUtils.getResponseEntity(CafeConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            } else {
                return CafeUtils.getResponseEntity(CafeConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}