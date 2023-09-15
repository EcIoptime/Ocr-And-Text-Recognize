package com.example.ocr.repository.local;

/**
 * Created by EslamHussein on 12/28/17.
 */

public interface DBConstant {
    String DB_NAME = "faceSystemDbd";

    // user table name
    String USERS_TABLE_NAME = "Users";

    // user table fields
    String USER_ID = "id";
    String USER_FIRST_NAME = "first_name";
    String USER_LAST_NAME = "last_name";
    String USER_ORGANIZATION = "organization";
    String USER_EMAIL = "email";
    String USER_IMAGE = "image_url";
    String USER_FEATURE = "feature_url";
    String USER_ORGANIZATION_NAME = "organization_name";
    String PASSWORD = "password";
    String IS_ADMIN = "is_admin";
    String IS_VISITOR = "is_visitor";
}
