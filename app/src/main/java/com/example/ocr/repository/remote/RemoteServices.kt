package com.example.ocr.repository.remote

import com.example.ocr.models.fcmNotification.FcmNotification
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import java.util.HashMap


interface RemoteServices {

//    @Headers("Content-Type:application/x-www-form-urlencoded")
//    @FormUrlEncoded
    @GET("get/venues")
    suspend fun getAllVenues(): Response<ResponseBody>

    @GET("get/gateways/{venueId}")
    suspend fun getGateWays(
        @Path("venueId") venueId:String
    ): Response<ResponseBody>

    @GET("get/beacons/{gateWayId}")
    suspend fun getBeacons(
        @Path("gateWayId") gateWayId:String
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("email") name:String,
        @Field("password") password:String,
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("auth/social-login")
    suspend fun socialLogin(
        @Field("email") name:String,
        @Field("name") password:String,
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("auth/register")
    suspend fun register(
        @Field("name") name:String,
        @Field("email") email:String,
        @Field("password") password:String,
        @Field("password_confirmation") passwordConfirmation:String,
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Field("email") email:String,
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("auth/verify-pin")
    suspend fun verifyPin(
        @Field("email") email:String,
        @Field("pin") pin:String,
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("auth/reset-password")
    suspend fun updateNewPassword(
        @Field("email") email:String,
        @Field("password") password:String,
        @Field("password_confirmation") passwordConfirmation:String,
    ): Response<ResponseBody>

    @GET("get-list")
    suspend fun searchList(
        @Header("Authorization") token:String,
        @Query("long") long:String,
        @Query("lat") lat:String,
        @Query("name") name:String,
    ): Response<ResponseBody>

    @GET("get-beacons/{beacon}")
    suspend fun getBeaconByVenue(
        @Header("Authorization") token:String,
        @Path("beacon") beacon:String
    ): Response<ResponseBody>

    @GET("search-product/{venueId}")
    suspend fun getProductByVenue(
        @Header("Authorization") token:String,
        @Path("venueId") beacon:String,
        @Query("tags") tags:String,
//        @Query("venueId") venueId:String,
//        @Body beacon: RequestBody
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("payProduct/{productId}")
    suspend fun payProduct(
        @Header("Authorization") token:String,
        @Path("productId") productId:String,
        @Field("user_id") userId:String,
    ): Response<ResponseBody>

    @GET("get-location")
    suspend fun getLocation(
        @Header("Authorization") token:String,
        @Query("long") long:String,
        @Query("lat") lat:String,
    ): Response<ResponseBody>


    @GET("get-categories")
    suspend fun getCategories(
        @Header("Authorization") token:String,
    ): Response<ResponseBody>

    @GET("get-beacon-products/{beaconID}")
    suspend fun getBeaconProducts(
        @Header("Authorization") token:String,
        @Path("beaconID") beacon:String
    ): Response<ResponseBody>

    @Multipart
    @POST("api/aws/search-image-before-reg")
    suspend fun searchImageBeforeRegistration(
        @Part name : MultipartBody.Part,
//        @Part nameThumb : MultipartBody.Part,
    ): Response<ResponseBody>

    @GET("api/get-countries")
    suspend fun getAllCountries(): Response<ResponseBody>

    @POST("api/patient-delete/{patientID}")
    suspend fun deletePatientById(
        @Path("patientID") userID: String,
    ): Response<ResponseBody>

    //    @Headers("Content-Type:application/x-www-form-urlencoded")
    @Multipart
    @POST("api/aws/search-image")
    suspend fun searchImage(
        @Part name :MultipartBody.Part ,
    ): Response<ResponseBody>

    @Headers("Content-Type:application/x-www-form-urlencoded" ,"Accept:*/*")
//    @Multipart
    @FormUrlEncoded
    @POST("api/register-user")
    suspend fun addUserRetro(
        @FieldMap data: HashMap<String, Any>,
    ): Response<ResponseBody>

    @Headers("Content-Type:application/x-www-form-urlencoded" ,"Accept:*/*")
//    @Multipart
//    @FormUrlEncoded
    @GET("api/email-check")
    suspend fun checkIfEmailExists(
        @Query("email") email: String,
    ): Response<ResponseBody>

    @Headers("Content-Type:application/x-www-form-urlencoded")
    @FormUrlEncoded
    @POST("api/login")
    suspend fun loginByCredentialsRetro(
        @Field("password" ) pin :String,
        @Field("email" ) email :String,
        @Field("is_email" ) isEmail :Boolean,
    ): Response<ResponseBody>

    @Headers("Content-Type:application/x-www-form-urlencoded")
//    @FormUrlEncoded
    @POST("api/userid-check")
    suspend fun getUserById(
        @Query("id" ) idText :String,
    ): Response<ResponseBody>

    @Headers("Content-Type:application/x-www-form-urlencoded")
    @FormUrlEncoded
    @POST("api/staff-login")
    suspend fun loginByCredentialsStaff(
        @Field("password" ) pin :String,
        @Field("email" ) email :String,
        @Field("is_email" ) isEmail :Boolean,
    ): Response<ResponseBody>

    @Headers("Accept:*/*")
    @Multipart
    @POST("finger/")
    suspend fun addFinger(
    @Part file: MultipartBody.Part,
//        @Part("image\"; filename=\"myfile.jpg\"") file: RequestBody,
    @Part("name_text") userId: RequestBody,
    ): Response<ResponseBody>

    @Headers("Accept:*/*")
//    @Multipart
    @GET("finger_match/{user_id}/")
    suspend fun deleteFingerById(
        @Path("user_id") userId:String,
    ): Response<ResponseBody>

    @Headers(
        "Content-Type: application/json",
        "Authorization: key=AAAA_7DutZI:APA91bHIIrVLbnRYCCmt9xec7Y7XxXHAfRi1DkjxDAPR5l_eumk5tb5Uz0mxBG4N9z0gQTcpHLmNXRxqmqUwFap8t503kLCHzJtew8j_MEvvlAux9x0dveehKFC3LFuEnX9hmQUqiHxV"
    )
    @POST
    suspend fun sendFCMNotification(
        @Url customUrl:String,
        @Body notification: FcmNotification,
    ): Response<ResponseBody>

    @Headers("Accept:*/*")
    @Multipart
    @POST("finger/")
    suspend fun addFingerMultliple(
        @Part file1: MultipartBody.Part,
        @Part file2: MultipartBody.Part,
        @Part file3: MultipartBody.Part,
        @Part("name_text") userId: RequestBody,
    ): Response<ResponseBody>

    @Multipart
    @POST("finger_match/")
    suspend fun matchFinger(
    @Part file: MultipartBody.Part,
    ): Response<ResponseBody>

//    @Headers("Content-Type:application/x-www-form-urlencoded")
    @GET("finger")
    suspend fun getFingersList(
    ): Response<ResponseBody>


    @Headers("Content-Type:application/x-www-form-urlencoded")
    @FormUrlEncoded
    @POST("api/register-user/{user_id}")
    suspend fun updateUserRetro(
        @FieldMap data: HashMap<String, Any>,
        @Path("user_id") userId:String,
    ): Response<ResponseBody>

}