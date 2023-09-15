package com.example.ocr.repository.remote


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

import com.example.ocr.common.AppController

import com.example.ocr.models.addFinger.ModelAddFinger
import com.example.ocr.models.countriesAmazon.ModelCountries
import com.example.ocr.models.faceSearch.ModelFaceSearch
import com.example.ocr.models.faceSearchForRegistration.ModelFaceSearchUserFound
import com.example.ocr.models.faceSearchForRegistration.User
import com.example.ocr.models.faceSearchUserNotFound.FaceSearchUserNotFound
import com.example.ocr.models.faceSearchUserNotFound.UserImageData
import com.example.ocr.models.fcmNotification.FCMTopics
import com.example.ocr.models.fcmNotification.FcmNotification
import com.example.ocr.models.fingerData.FingerPatterns
import com.example.ocr.models.fingersList.ModelGetFingerList
import com.example.ocr.models.userExists.ModelUserExists
import com.iopime.telemedicines.models.userRegistration.UserRegistration
import com.example.ocr.utilities.LoginType
import com.example.ocr.utilities.convertIntToBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class RemoteRepository {
    protected fun <T> create(clazz: Class<T>?, baseUrl: String, loginApi: Boolean = false): T {
        return retrofit(baseUrl, loginApi).create(clazz)
    }

    private fun retrofit(baseUrl: String, loginApi: Boolean): Retrofit {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
//        val sp = AppController.getInstance().prefManager
        val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .retryOnConnectionFailure(true)
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val original = chain.request()
                val request: Request.Builder = original.newBuilder()
                    .addHeader("Connection", "close")
                    .addHeader("Authorization", "Bearer ${AppController.myPrefManager?.normalUserToken}")
                    .method(original.method, original.body)

//                chain.proceed(request.build())
                var response = chain.proceed(request.build())
                if (response.code == 401 && loginApi == false) {
                    AppController.repository?.localRepository?.prefManager?.isRemember = false
                    AppController.repository?.localRepository?.prefManager?.isLogin = false
                    AppController.repository?.localRepository?.prefManager?.currentUser = null
                    AppController.repository?.localRepository?.prefManager?.isIntroShown = false

                    AppController.repository?.localRepository?.prefManager?.loginType = LoginType.NONE.name

                    AppController.myPrefManager?.normalUserToken = ""

                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(AppController.instance, "Token expired Please login again", Toast.LENGTH_SHORT).show()
                    }

//                    val intent = Intent(AppController.instance, LoginSignUpScreen::class.java)
//                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                    AppController.instance?.startActivity(intent)
                }
                response
            })
            .connectionSpecs(ArrayList(Arrays.asList(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS)))
            .build()
        val gsonBuilder = GsonBuilder()
        val customGson = gsonBuilder.create()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(customGson))
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build()
    }


    protected fun <T> createFinger(clazz: Class<T>?, baseUrl: String, loginApi: Boolean = false): T {
        return retrofitFinger(baseUrl, loginApi).create(clazz)
    }

    private fun retrofitFinger(baseUrl: String, loginApi: Boolean): Retrofit {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        // Create an SSL socket factory with the trust manager
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        // Create a hostname verifier that verifies all hostnames
        val hostnameVerifier = HostnameVerifier { hostname, sslSession ->
            Log.i("test", "hostname name verifier ${hostname}")
            hostname == "3.144.253.41"
        }

//        val sp = AppController.getInstance().prefManager
        val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier(hostnameVerifier)
            .retryOnConnectionFailure(true)
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val original = chain.request()
                val request: Request.Builder = original.newBuilder()
                    .addHeader("Connection", "keep-alive")
                    .method(original.method, original.body)

//                chain.proceed(request.build())
                var response = chain.proceed(request.build())
                response
            })
            .hostnameVerifier { hostname, session -> true }
            .readTimeout(3, TimeUnit.MINUTES)
            .connectionSpecs(ArrayList(Arrays.asList(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS)))
            .build()
//        okHttpClient.setHostnameVerifier(HostnameVerifier { hostname, session -> true })

        val gsonBuilder = GsonBuilder()
        val customGson = gsonBuilder.create()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(customGson))
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build()
    }

    // Create a trust manager that trusts all certificates
    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            // Do nothing
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            // Do nothing
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return emptyArray()
        }
    })


    suspend fun fetchVenues(): Response<ResponseBody> {
        return create(RemoteServices::class.java, BASE_URL).getAllVenues()
    }

    suspend fun fetchGateways(venueId: String): Response<ResponseBody> {
        return create(RemoteServices::class.java, BASE_URL).getGateWays(venueId)
    }

    suspend fun fetchBeacons(gateWayId: String): Response<ResponseBody> {
        return create(RemoteServices::class.java, BASE_URL).getBeacons(gateWayId)
    }

    suspend fun login(userName: String, password: String): Response<ResponseBody> {
        return create(RemoteServices::class.java, BASE_URL).login(userName, password)
    }

    suspend fun socialLogin(userName: String, name: String): Response<ResponseBody> {
        return create(RemoteServices::class.java, BASE_URL).socialLogin(userName, name)
    }

    suspend fun register(userName: String, email: String, password: String, passwordConfirmation: String): Response<ResponseBody> {
        return create(RemoteServices::class.java, BASE_URL).register(userName, email, password, passwordConfirmation)
    }

    suspend fun forgotPassword(email: String): Response<ResponseBody> {
        return create(RemoteServices::class.java, BASE_URL).forgotPassword(email)
    }

    suspend fun verifyPin(email: String, pin: String): Response<ResponseBody> {
        return create(RemoteServices::class.java, BASE_URL).verifyPin(email, pin)
    }

    suspend fun updateNewPassword(email: String, password: String, passwordConfirmation: String): Response<ResponseBody> {
        return create(RemoteServices::class.java, BASE_URL).updateNewPassword(email, password, passwordConfirmation)
    }

    suspend fun searchList(long: String, lat: String, name: String): Response<ResponseBody> {
        return create(RemoteServices::class.java, BASE_URL)
            .searchList("Bearer ${AppController.myPrefManager?.modelLogin?.accessToken}", long, lat, name)
    }

    suspend fun getBeaconByVenue(beaconId: String): Response<ResponseBody> {
        return create(RemoteServices::class.java, BASE_URL)
            .getBeaconByVenue("Bearer ${AppController.myPrefManager?.modelLogin?.accessToken}", beaconId)
    }

    suspend fun getProductByVenue(venueId: String, searchQuery: String): Response<ResponseBody> {
//        var jsonObj = JSONObject()
//        jsonObj.put("tags", "${searchQuery}")
//        jsonObj.put("venueId", "${venueId}")
//        val body: RequestBody = jsonObj.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        return create(RemoteServices::class.java, BASE_URL)
            .getProductByVenue("Bearer ${AppController.myPrefManager?.modelLogin?.accessToken}", venueId, searchQuery)
    }

    suspend fun payProduct(productId: String, userId: String): Response<ResponseBody> {
        return create(RemoteServices::class.java, BASE_URL)
            .payProduct("Bearer ${AppController.myPrefManager?.modelLogin?.accessToken}", productId, userId)
    }

    suspend fun getLocation(long: String, lat: String): Response<ResponseBody> {
        return create(RemoteServices::class.java, BASE_URL)
            .getLocation("Bearer ${AppController.myPrefManager?.modelLogin?.accessToken}", long, lat)
    }

    suspend fun getCategories(): Response<ResponseBody> {
        return create(RemoteServices::class.java, BASE_URL)
            .getCategories("Bearer ${AppController.myPrefManager?.modelLogin?.accessToken}")
    }

    suspend fun getBeaconProducts(beaconId: String): Response<ResponseBody> {
        return create(RemoteServices::class.java, BASE_URL)
            .getBeaconProducts("Bearer ${AppController.myPrefManager?.modelLogin?.accessToken}", beaconId)
    }


    private fun getUserDetails(detailObject: JSONObject?, data: UserRegistration?): UserRegistration? {

        if (detailObject?.has("surname") == true) data?.sureName = detailObject.optString("surname", "") ?: ""
        if (detailObject?.has("longitude") == true) data?.longitude = detailObject.optString("longitude", "") ?: ""
        if (detailObject?.has("latitude") == true) data?.latitude = detailObject.optString("latitude", "") ?: ""
        if (detailObject?.has("date_of_birth") == true) data?.dateOfBirth = detailObject.optString("date_of_birth", "") ?: ""
        if (detailObject?.has("nationality") == true) data?.nationality = detailObject.optString("nationality", "") ?: ""
        if (detailObject?.has("language") == true) data?.language = detailObject.optString("language", "") ?: ""
        if (detailObject?.has("race") == true) data?.race = detailObject.optString("race", "") ?: ""
        if (detailObject?.has("martial_status") == true) data?.martialStatus = detailObject.optString("martial_status", "") ?: ""
        if (detailObject?.has("occupation_type") == true) data?.occupationType = detailObject.optString("occupation_type", "") ?: ""
        if (detailObject?.has("designation") == true) data?.designation = detailObject.optString("designation", "") ?: ""
        if (detailObject?.has("home_address_1") == true) data?.homeAddress1 = detailObject.optString("home_address_1", "") ?: ""
        if (detailObject?.has("home_address_2") == true) data?.homeAddress2 = detailObject.optString("home_address_2", "") ?: ""
        if (detailObject?.has("home_address_3") == true) data?.homeAddress3 = detailObject.optString("home_address_3", "") ?: ""
        if (detailObject?.has("identity_type") == true) data?.identityType = detailObject.optString("identity_type", "") ?: ""
        if (detailObject?.has("employer_name") == true) data?.employerName = detailObject.optString("employer_name", "") ?: ""
        if (detailObject?.has("employer_country_id") == true) data?.employerCountryId = detailObject.optString("employer_country_id", "") ?: ""
        if (detailObject?.has("work_number") == true) data?.workNumber = detailObject.optString("work_number", "") ?: ""
        if (detailObject?.has("work_address") == true) data?.workAddress = detailObject.optString("work_address", "") ?: ""
        if (detailObject?.has("kin_surname") == true) data?.kinSurname = detailObject.optString("kin_surname", "") ?: ""
        if (detailObject?.has("employer_sur_name") == true) data?.employerSureName = detailObject.optString("employer_sur_name", "") ?: ""
        if (detailObject?.has("patient_id") == true) data?.patientId = detailObject.optString("patient_id", "") ?: ""
        if (detailObject?.has("age") == true) data?.age = detailObject.optString("age", "") ?: ""
        if (detailObject?.has("gender") == true) data?.gender = detailObject.optString("gender", "") ?: ""
        if (detailObject?.has("country_id") == true) data?.personalDetailCountryId = detailObject.optString("country_id", "") ?: ""
        if (detailObject?.has("dialing_code") == true) data?.dialingCode = detailObject.optString("dialing_code", "") ?: ""
        if (detailObject?.has("document_id") == true) data?.documentId = detailObject.optString("document_id", "") ?: ""
        if (detailObject?.has("document_type") == true) data?.documentType = detailObject.optString("document_type", "") ?: ""
        if (detailObject?.has("issue_state_country_id") == true) data?.issueStateCountryId = detailObject.optString("issue_state_country_id", "") ?: ""
        if (detailObject?.has("expiry_date") == true) data?.expiryDate = detailObject.optString("expiry_date", "") ?: ""
        if (detailObject?.has("personal_detail_country_id") == true) data?.personalDetailCountryId = detailObject.optString("personal_detail_country_id", "") ?: ""
        if (detailObject?.has("next_of_kin") == true) data?.nextOfKin = detailObject.optString("next_of_kin", "") ?: ""
        if (detailObject?.has("next_of_kin_number") == true) data?.nextOfKinNumber = detailObject.optString("next_of_kin_number", "") ?: ""
        if (detailObject?.has("next_of_kin_country_id") == true) data?.nextOfKinCountryId = detailObject.optString("next_of_kin_country_id", "") ?: ""
        if (detailObject?.has("kinRelation") == true) data?.kinRelation = detailObject.optString("kinRelation", "") ?: ""
        if (detailObject?.has("clinic_id") == true) data?.clinicId = detailObject.optString("clinic_id", "") ?: ""
        if (detailObject?.has("country_address") == true) data?.countryAddress = detailObject.optString("country_address", "") ?: ""
        if (detailObject?.has("next_of_kin_dob") == true) data?.nextOfKinDob = detailObject.optString("next_of_kin_dob", "") ?: ""
        if (detailObject?.has("next_of_kin_gender") == true) data?.nextOfKinGender = detailObject.optString("next_of_kin_gender", "") ?: ""
        if (detailObject?.has("next_of_kin_title") == true) data?.nextOfKinTitle = detailObject.optString("next_of_kin_title", "") ?: ""
        if (detailObject?.has("city") == true) data?.city = detailObject.optString("city", "") ?: ""
        if (detailObject?.has("address_line") == true) data?.addressLine = detailObject.optString("address_line", "") ?: ""
        if (detailObject?.has("residence") == true) data?.residence = detailObject.optString("residence", "") ?: ""
        if (detailObject?.has("nationality_id") == true) data?.nationalityId = detailObject.optString("nationality_id", "") ?: ""
        if (detailObject?.has("postal_code") == true) data?.postalCode = detailObject.optString("postal_code", "") ?: ""
        if (detailObject?.has("foriegn_national") == true) data?.foriegnNational = detailObject.optInt("foriegn_national", 0).toString().convertIntToBoolean()
        if (detailObject?.has("preferred_language") == true) data?.preferredLanguage = detailObject.optString("preferred_language", "") ?: ""
        if (detailObject?.has("personal_other_name") == true) data?.personalOtherName = detailObject.optString("personal_other_name", "") ?: ""
//        data?.nationalityId = detailObject?.optString("nationality_id", "") ?: ""
//        data?.patientGroup = detailObject?.optString("patient_group", "") ?: ""
//        data?.contactNumber = detailObject?.optString("phone_number" ,"") ?:""

        return data
    }



    suspend fun getFingersList(success: (isExists: ModelGetFingerList?) -> Unit, exception: (Exception) -> Unit) {
        try {
            val response = createFinger(RemoteServices::class.java, BASE_URL_FINGER, true)
                .getFingersList()
            if (response.isSuccessful) {
                val data = GsonBuilder().setLenient().serializeNulls().create().fromJson(response.body()?.string(), ModelGetFingerList::class.java)
                success.invoke(data)
            } else {
                success.invoke(null)
            }
        } catch (e: Exception) {
            Log.i("test", "getFingersList  ${e}")
            exception.invoke(e)
        }
    }


    private fun getFilePart(index: Int, bitmap: Bitmap): MultipartBody.Part {
        val file = File(AppController.instance?.cacheDir, "finger_print${index}.png")
        file.delete()

        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        out.flush()
        out.close()
        return MultipartBody.Part.createFormData("image${index}", file.name, file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
    }

    private fun getFingerPrintSavedLocally(index: Int, bitmap: Bitmap): File {
        val file = File(AppController.instance?.cacheDir, "finger_print${index}.png")
        file.delete()

        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        out.flush()
        out.close()
        return file
    }


    companion object {
        const val BASE_URL = "http://ec2-3-144-138-200.us-east-2.compute.amazonaws.com/"
        const val BASE_URL_FINGER = "https://3.144.253.41/"
    }
}