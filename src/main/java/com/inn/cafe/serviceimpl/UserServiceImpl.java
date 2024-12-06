package com.inn.cafe.serviceimpl;

import com.google.common.base.Strings;
import com.inn.cafe.constants.CafeConstants;
import com.inn.cafe.constants.UserConstants;
import com.inn.cafe.dao.UserDao;
import com.inn.cafe.jwt.CustomerUsersDetailsService;
import com.inn.cafe.jwt.JwtFilter;
import com.inn.cafe.jwt.JwtUtil;
import com.inn.cafe.pojo.User;
import com.inn.cafe.service.UserService;
import com.inn.cafe.utils.CafeUtils;
import com.inn.cafe.utils.EmailUtils;
import com.inn.cafe.wrapper.UserWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomerUsersDetailsService customerUsersDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private EmailUtils emailUtils;

    @Override
    public ResponseEntity<String> signUp(Map<String, String> requestMap) {
        log.info("Inside signUp {}", requestMap);
        try {
            if (validateSignUpMap(requestMap)) {
                // Check if the user already exists
                User user = userDao.findByEmailId(requestMap.get(UserConstants.EMAIL));
                if (Objects.isNull(user)) {
                    // Save user with encoded password
                    userDao.save(getUserFromMap(requestMap));
                    return CafeUtils.getResponseEntity(UserConstants.USER_REGISTERED_SUCCESS, HttpStatus.OK);
                } else {
                    return CafeUtils.getResponseEntity(UserConstants.EMAIL_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
                }
            } else {
                return CafeUtils.getResponseEntity(CafeConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception ex) {
            log.error("Exception during signUp: {}", ex.getMessage());
            return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> login(Map<String, String> requestMap) {
        log.info("Inside login");
        try {
            // Authenticate the user
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            requestMap.get(UserConstants.EMAIL),
                            requestMap.get(UserConstants.PASSWORD)
                    )
            );

            if (auth.isAuthenticated()) {
                // Get the authenticated user details
                User user = customerUsersDetailsService.getAuthenticatedUser();
                if ("true".equalsIgnoreCase(user.getStatus())) {
                    // Generate JWT token and send it back to the client
                    String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
                    return new ResponseEntity<>("{\"token\":\"" + token + "\"}", HttpStatus.OK);
                } else {
                    return new ResponseEntity<>("{\"message\":\"Wait for admin approval.\"}", HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>("{\"message\":\"Bad Credentials.\"}", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception ex) {
            log.error("Error in login: {}", ex.getMessage());
            return new ResponseEntity<>("{\"message\":\"Bad Credentials.\"}", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<List<UserWrapper>> getAllUser() {
        try {
            // Check if the current user is an admin
            if (jwtFilter.isAdmin()) {
                return new ResponseEntity<>(userDao.getAllUser(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> update(Map<String, String> requestMap) {
        try {
            // Check if the current user is an admin
            if (jwtFilter.isAdmin()) {
                Optional<User> optional = userDao.findById(Integer.parseInt(requestMap.get(UserConstants.ID)));
                if (optional.isPresent()) {
                    userDao.updateStatus(requestMap.get(UserConstants.STATUS), Integer.parseInt(requestMap.get(UserConstants.ID)));
                    sendMailToAllAdmin(requestMap.get(UserConstants.STATUS), optional.get().getEmail(), userDao.getAllAdmin());
                    return CafeUtils.getResponseEntity(UserConstants.USER_STATUS_UPDATED_SUCCESS, HttpStatus.OK);
                } else {
                    return CafeUtils.getResponseEntity(UserConstants.USER_ID_NOT_EXIST, HttpStatus.OK);
                }
            } else {
                return CafeUtils.getResponseEntity(CafeConstants.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Helper method to validate the required fields for sign up
    private boolean validateSignUpMap(Map<String, String> requestMap) {
        return requestMap.containsKey(UserConstants.NAME)
                && requestMap.containsKey(UserConstants.CONTACT_NUMBER)
                && requestMap.containsKey(UserConstants.EMAIL)
                && requestMap.containsKey(UserConstants.PASSWORD);
    }

    // Helper method to convert the sign up map into a User object
    private User getUserFromMap(Map<String, String> requestMap) {
        User user = new User();
        user.setName(requestMap.get(UserConstants.NAME));
        user.setContactNumber(requestMap.get(UserConstants.CONTACT_NUMBER));
        user.setEmail(requestMap.get(UserConstants.EMAIL));
        user.setPassword(passwordEncoder.encode(requestMap.get(UserConstants.PASSWORD))); // Encode the password
        user.setStatus("false"); // Default status is "false" (pending approval)
        user.setRole("user"); // Default role for new users
        return user;
    }

    // Helper method to send email to all admin users
    private void sendMailToAllAdmin(String status, String user, List<String> allAdmin) {
        allAdmin.remove(jwtFilter.getCurrentUser());
        if (status != null && status.equalsIgnoreCase("true")) {
            emailUtils.sendSimpleMessage(jwtFilter.getCurrentUser(), UserConstants.ACCOUNT_APPROVED_SUBJECT, "USER:- " + user + " \n is approved by \nADMIN:-" + jwtFilter.getCurrentUser(), allAdmin);
        } else {
            emailUtils.sendSimpleMessage(jwtFilter.getCurrentUser(), UserConstants.ACCOUNT_DISABLED_SUBJECT, "USER:- " + user + " \n is disabled by \nADMIN:-" + jwtFilter.getCurrentUser(), allAdmin);
        }
    }

    @Override
    public ResponseEntity<String> checkToken() {
        return CafeUtils.getResponseEntity("true", HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> changePassword(Map<String, String> requestMap) {
        try {
            // Get the currently authenticated user
            User userObj = userDao.findByEmail(jwtFilter.getCurrentUser());

            if (userObj != null) {
                // Check if the old password matches the stored password
                if (passwordEncoder.matches(requestMap.get(UserConstants.OLD_PASSWORD), userObj.getPassword())) {
                    // Encode the new password and save it
                    userObj.setPassword(passwordEncoder.encode(requestMap.get(UserConstants.NEW_PASSWORD)));
                    userDao.save(userObj); // Save the changes to the database
                    return CafeUtils.getResponseEntity(UserConstants.PASSWORD_UPDATED_SUCCESS, HttpStatus.OK);
                }
                return CafeUtils.getResponseEntity(UserConstants.INCORRECT_OLD_PASSWORD, HttpStatus.BAD_REQUEST);
            }
            return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception ex) {
            log.error("Error during password change: {}", ex.getMessage());
            return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<String> forgotPassword(Map<String, String> requestMap) {
        try {
            // Find user by email
            User user = userDao.findByEmail(requestMap.get(UserConstants.EMAIL));

            if (user != null && !Strings.isNullOrEmpty(user.getEmail())) {
                // Generate a temporary password
                String tempPassword = CafeUtils.generateRandomPassword(); // Utility method to generate a random password

                // Encode the temporary password before saving to the database
                user.setPassword(passwordEncoder.encode(tempPassword));
                userDao.save(user);

                // Send the temporary password to the user's email
                emailUtils.forgotMail(user.getEmail(),
                        "Credentials by Cafe Management System",
                        "Your temporary password is: " + tempPassword);

                return CafeUtils.getResponseEntity(UserConstants.TEMP_PASSWORD_SENT, HttpStatus.OK);
            } else {
                return CafeUtils.getResponseEntity(UserConstants.USER_NOT_FOUND_EMAIL, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception ex) {
            log.error("Error during forgot password: {}", ex.getMessage());
            ex.printStackTrace();
            return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}