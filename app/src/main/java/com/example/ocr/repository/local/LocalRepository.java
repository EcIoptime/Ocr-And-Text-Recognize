package com.example.ocr.repository.local;

import com.example.ocr.models.fingerData.EntityDao;

public class LocalRepository {
    private final MyPrefManager prefManager;
    private final LocalDB localDB;
    private UsersDao usersDao;
    public EntityDao entityDao;

    public LocalRepository(LocalDB localDB, MyPrefManager app_data) {
        this.usersDao = localDB.usersDao();
        this.entityDao = localDB.entityDao();
        this.localDB = localDB;
        this.prefManager = app_data;
    }

    public MyPrefManager getPrefManager() {
        return prefManager;
    }


//    public void setUserCredentials(UserLoginResult result) {
//        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
//
//        Gson gson = new Gson();
//
//        String json = gson.toJson(result.user);
//
//        prefsEditor.remove(Constants.USER_CREDENTIALS);
//
//        if (result.user != null) {
//            prefsEditor.putString(Constants.USER_CREDENTIALS, json);
//        }
//        if (result.accessToken != null) {
//            prefsEditor.putString(Constants.USER_ACCESS_TOKEN, result.accessToken);
//        }
//        if (result.refreshToken != null) {
//            prefsEditor.putString(Constants.USER_REFRESH_TOKEN, result.refreshToken);
//        }
//        prefsEditor.apply();
//    }


//    public void setDeviceMetaData(DeviceMetaData result) {
//        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
//        Gson gson = new Gson();
//        String json = gson.toJson(result);
//        prefsEditor.remove(Constants.DEVICE_META_DATA);
//        prefsEditor.putString(Constants.DEVICE_META_DATA, json);
//        prefsEditor.apply();
//    }
//
//    public DeviceMetaData getDeviceMetaData() {
//        if (sharedPreferences.contains(Constants.DEVICE_META_DATA)) {
//            final Gson gson = new Gson();
//            return gson.fromJson(sharedPreferences.getString(Constants.DEVICE_META_DATA, ""), DeviceMetaData.class);
//        }
//        return null;
//    }


    public int nukeTable() {
        return usersDao.nukeTable();
    }

    public void savePassword(String text) {
//        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
//        prefsEditor.remove(Constants.PASSWORD);
//        prefsEditor.putString(Constants.PASSWORD, text);
//        prefsEditor.apply();
    }

    public String getPassword() {
//        if (sharedPreferences.contains(Constants.PASSWORD)) {
//            final Gson gson = new Gson();
//            return sharedPreferences.getString(Constants.PASSWORD, "");
//        }
        return null;
    }
}
