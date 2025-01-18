package com.inn.cafe.serviceimpl;

import com.inn.cafe.constants.CafeConstants;
import com.inn.cafe.constants.ProductConstants;
import com.inn.cafe.dao.CategoryDao;
import com.inn.cafe.dao.ProductDao;
import com.inn.cafe.jwt.JwtFilter;
import com.inn.cafe.pojo.Category;
import com.inn.cafe.pojo.Product;
import com.inn.cafe.service.ProductService;
import com.inn.cafe.utils.CafeUtils;
import com.inn.cafe.wrapper.ProductWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductDao productDao;

    @Autowired
    private CategoryDao categoryDao; // Inject CategoryDao for category validation.

    @Autowired
    private JwtFilter jwtFilter;

    @Override
    public ResponseEntity<String> addNewProduct(Map<String, String> requestMap) {
        try {
            // Check if the user has admin privileges
            if (!jwtFilter.isAdmin()) {
                return CafeUtils.getResponseEntity(CafeConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

            // Validate input data
            if (!isValidRequest(requestMap, false)) {
                return CafeUtils.getResponseEntity(ProductConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }

            // Validate category existence
            int categoryId = Integer.parseInt(requestMap.get(ProductConstants.CATEGORY_ID));
            if (categoryExists(categoryId)) {
                return CafeUtils.getResponseEntity(
                        String.format(ProductConstants.CATEGORY_NOT_FOUND, categoryId),
                        HttpStatus.BAD_REQUEST
                );
            }

            // Map the input data to the Product entity and save it
            Product product = mapToProduct(requestMap, false);
            productDao.save(product);
            return CafeUtils.getResponseEntity(ProductConstants.PRODUCT_ADDED_SUCCESS, HttpStatus.OK);

        } catch (Exception ex) {
            ex.printStackTrace();
            return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<List<ProductWrapper>> getAllProduct() {
        try {
            return new ResponseEntity<>(productDao.getAllProduct(), HttpStatus.OK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> updateProduct(Map<String, String> requestMap) {
        try {
            if (jwtFilter.isAdmin()) {
                if (isValidRequest(requestMap, true)) { // Reuse the validation method for consistency
                    int productId = Integer.parseInt(requestMap.get(ProductConstants.ID));
                    Optional<Product> optional = productDao.findById(productId);

                    if (optional.isPresent()) {
                        Product existingProduct = optional.get();

                        // Validate category existence if changed
                        int newCategoryId = Integer.parseInt(requestMap.get(ProductConstants.CATEGORY_ID));
                        if (newCategoryId != existingProduct.getCategory().getId() && categoryExists(newCategoryId)) {
                            return CafeUtils.getResponseEntity(
                                    String.format(ProductConstants.CATEGORY_NOT_FOUND, newCategoryId),
                                    HttpStatus.BAD_REQUEST
                            );
                        }

                        // Map and save updated product details
                        Product product = mapToProduct(requestMap, true);
                        product.setStatus(existingProduct.getStatus()); // Preserve existing status
                        productDao.save(product);

                        return CafeUtils.getResponseEntity(ProductConstants.PRODUCT_UPDATED_SUCCESS, HttpStatus.OK);
                    } else {
                        return CafeUtils.getResponseEntity(
                                String.format(ProductConstants.PRODUCT_NOT_FOUND, productId),
                                HttpStatus.NOT_FOUND
                        );
                    }
                } else {
                    return CafeUtils.getResponseEntity(ProductConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
                }
            } else {
                return CafeUtils.getResponseEntity(CafeConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> deleteProduct(Integer id) {
        try {
            if(jwtFilter.isAdmin()){
              Optional<Product> optional = productDao.findById(id);
              if(optional.isPresent()) {
                productDao.deleteById(id);
                return CafeUtils.getResponseEntity(
                  String.format(ProductConstants.PRODUCT_DELETED_SUCCESS, id), HttpStatus.OK);

              }
              return CafeUtils.getResponseEntity(
                      String.format(ProductConstants.PRODUCT_NOT_FOUND, id), HttpStatus.NOT_FOUND) ;
            }else{
                return CafeUtils.getResponseEntity(CafeConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> updateStatus(Map<String, String> requestMap) {
        try{
            if(jwtFilter.isAdmin()){
                Optional<Product> optional = productDao.findById(Integer.parseInt(requestMap.get("id")));
                if(optional.isPresent()) {
                    productDao.updateProductStatus(requestMap.get("status"), Integer.parseInt(requestMap.get("id")));
                    return CafeUtils.getResponseEntity(ProductConstants.PRODUCT_STATUS_UPDATES_SUCCESS, HttpStatus.OK);

                }
                return CafeUtils.getResponseEntity(
                        String.format(ProductConstants.PRODUCT_NOT_FOUND, Integer.parseInt(requestMap.get("id"))), HttpStatus.NOT_FOUND) ;
            }else{
                return CafeUtils.getResponseEntity(CafeConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }

        }catch (Exception ex){
            ex.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<List<ProductWrapper>> getByCategory(Integer id) {
        try{
            return new ResponseEntity<>(productDao.getProductByCategory(id), HttpStatus.OK);

        }catch(Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Checks if a category with the given ID exists in the database.
     *
     * @param categoryId The ID of the category to check.
     * @return true if the category exists, false otherwise.
     */
    private boolean categoryExists(int categoryId) {
        return !categoryDao.existsById(categoryId); // Leverages Spring Data JPA's native support.
    }

    /**
     * Validates the input request map to ensure all required fields are present and non-empty.
     *
     * @param requestMap Map containing the input data.
     * @param validateId Flag to indicate if the "id" field should also be validated.
     * @return true if the request is valid, false otherwise.
     */
    private boolean isValidRequest(Map<String, String> requestMap, boolean validateId) {
        if (!requestMap.containsKey(ProductConstants.NAME) ||
                !requestMap.containsKey(ProductConstants.CATEGORY_ID) ||
                !requestMap.containsKey(ProductConstants.PRICE) ||
                !requestMap.containsKey(ProductConstants.DESCRIPTION)) {
            return false;
        }

        if (requestMap.get(ProductConstants.NAME).trim().isEmpty() ||
                requestMap.get(ProductConstants.CATEGORY_ID).trim().isEmpty() ||
                requestMap.get(ProductConstants.PRICE).trim().isEmpty() ||
                requestMap.get(ProductConstants.DESCRIPTION).trim().isEmpty()) {
            return false;
        }

        return !(validateId && (!requestMap.containsKey(ProductConstants.ID) || requestMap.get(ProductConstants.ID).trim().isEmpty()));

    }

    /**
     * Maps the input request data to a Product entity.
     *
     * @param requestMap Map containing the input data.
     * @param isUpdate Flag indicating if this is an update operation.
     * @return A Product entity populated with data from the request.
     */
    private Product mapToProduct(Map<String, String> requestMap, boolean isUpdate) {
        Product product = new Product();

        if (isUpdate && requestMap.containsKey(ProductConstants.ID)) {
            product.setId(Integer.parseInt(requestMap.get(ProductConstants.ID).trim()));
        } else {
            product.setStatus("true"); // Default status for new products
        }

        product.setName(requestMap.get(ProductConstants.NAME).trim());
        product.setDescription(requestMap.get(ProductConstants.DESCRIPTION).trim());
        product.setPrice(Integer.parseInt(requestMap.get(ProductConstants.PRICE).trim()));

        Category category = new Category();
        category.setId(Integer.parseInt(requestMap.get(ProductConstants.CATEGORY_ID).trim()));
        product.setCategory(category);

        return product;
    }
}