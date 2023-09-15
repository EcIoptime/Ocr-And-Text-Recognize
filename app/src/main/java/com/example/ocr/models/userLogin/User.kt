package com.example.ocr.models.userLogin

import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import androidx.room.ColumnInfo
import androidx.room.Entity
import com.example.ocr.repository.local.DBConstant
import com.example.ocr.repository.remote.RemoteRepository

@Entity(tableName = "Users")
class User  //    public List<String> getErrors() {
//        return errors;
//    }
//    public void setErrors(List<String> errors) {
//        this.errors = errors;
//    }
{
    @PrimaryKey
    @SerializedName("id")
    @ColumnInfo(name = DBConstant.USER_ID)
    var id = 0

    @SerializedName("firstName")
    @ColumnInfo(name = DBConstant.USER_FIRST_NAME)
    var firstName: String? = null

    @SerializedName("lastName")
    @ColumnInfo(name = DBConstant.USER_LAST_NAME)
    var lastName: String? = null

    @SerializedName("organization_id")
    @ColumnInfo(name = DBConstant.USER_ORGANIZATION)
    var organization = 0

    @SerializedName("organizationName")
    @ColumnInfo(name = DBConstant.USER_ORGANIZATION_NAME)
    var organizationName: String? = null

    @SerializedName("email")
    @ColumnInfo(name = DBConstant.USER_EMAIL)
    var email: String? = null

    @SerializedName("image")
    @ColumnInfo(name = DBConstant.USER_IMAGE)
    private var imgUrl: String? = null

    @SerializedName("feature")
    @ColumnInfo(name = DBConstant.USER_FEATURE)
    private var featureUrl: String? = null

    @SerializedName("password")
    @ColumnInfo(name = DBConstant.PASSWORD)
    var password: String? = null

    @SerializedName("isAdmin")
    @ColumnInfo(name = DBConstant.IS_ADMIN)
    var admin: Boolean? = null

    //    @SerializedName("errors")
    //    @TypeConverters(ErrorConverter.class)
    //    public List<String> errors;
    @SerializedName("isVisitor")
    @ColumnInfo(name = DBConstant.IS_VISITOR)
    var visitor: Boolean? = null
    fun fullName(): String {
        return "$firstName $lastName"
    }

    fun getImgUrl(): String? {
        return if (imgUrl != null) {
            RemoteRepository.BASE_URL + imgUrl
        } else null
    }

    fun setImgUrl(imgUrl: String?) {
        this.imgUrl = imgUrl
    }

    fun getFeatureUrl(): String? {
        return if (featureUrl != null) {
            RemoteRepository.BASE_URL + featureUrl
        } else null
    }

    fun setFeatureUrl(featureUrl: String?) {
        this.featureUrl = featureUrl
    }

    val imgName: String?
        get() = if (imgUrl != null) {
            imgUrl!!.replace(".jpg".toRegex(), "")
        } else null
    val featureName: String?
        get() {
            if (featureUrl != null) {
                val strings = featureUrl!!.split("/").toTypedArray()
                return strings[strings.size - 1]
            }
            return null
        }
    val imageName: String?
        get() {
            if (imgUrl != null) {
                val strings = imgUrl!!.split("/").toTypedArray()
                return strings[strings.size - 1]
            }
            return null
        }
}