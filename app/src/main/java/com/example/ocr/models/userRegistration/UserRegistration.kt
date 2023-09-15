package com.iopime.telemedicines.models.userRegistration

import com.google.gson.annotations.SerializedName


data class UserRegistration(
    @SerializedName("id")
    var userId:String,
    @SerializedName("name")
    var userName:String,
    @SerializedName("surname")
    var sureName:String,
    @SerializedName("document_id")
    var documentId:String,
    @SerializedName("phone_number")
    var contactNumber:String?= null,
    @SerializedName("age")
    var age:String,
    @SerializedName("gender")
    var gender:String,
    @SerializedName("email")
    var email:String,
    @SerializedName("personal_detail_country_id")
    var personalDetailCountryId:String,

    @SerializedName("pin")
    var pin:String,
    @SerializedName("placeName")
    var placeName: String?,
//    @SerializedName("latitude")
//    var latitude: Double?,
//    @SerializedName("longitude")
//    var longitude: Double?,
    @SerializedName("dialing_code")
    var dialingCode:String,
    @SerializedName("profile_pic")
    var profilePic:String,

    @SerializedName("document_type")
    var documentType:String,
    @SerializedName("expiry_date")
    var expiryDate:String,
    @SerializedName("parent_user")
    var parentUser:String = "-1",
    @SerializedName("is_social_login")
    var isSocialLogin:Boolean =false,
    @SerializedName("card")
    var card:String = "",
    @SerializedName("VaccineVerified")
    var isVaccinatedVerified:Boolean = false,
    @SerializedName("next_of_kin")
    var nextOfKin:String ="",
    @SerializedName("next_of_kin_number")
    var nextOfKinNumber:String ="",
    @SerializedName("home_address")
    var homeAddress:String ="",
    @SerializedName("profile_photo_url")
    var profilePhotoUrl:String ="",

    @SerializedName("covidTestId")
    var covidTestId:String ="",

    @SerializedName("kinRelation")
    var kinRelation:String ="",

    @SerializedName("face_id")
    var faceId:String ="",

    var profilePicBase64:String? ="",

//    @SerializedName("personal_detail_country_id")
//    var personalDetailCountryId:String = "",

    @SerializedName("next_of_kin_country_id")
    var nextOfKinCountryId:String = "",

    @SerializedName("issue_state_country_id")
    var issueStateCountryId:String = "",

    @SerializedName("date_of_birth")
    var dateOfBirth:String = "",

    @SerializedName("nationality")
    var nationality:String = "",

    @SerializedName("language")
    var language:String = "",

    @SerializedName("race")
    var race:String = "",

    @SerializedName("martial_status")
    var martialStatus:String = "",

    @SerializedName("occupation_type")
    var occupationType:String = "",

    @SerializedName("designation")
    var designation:String = "",

    @SerializedName("home_address_1")
    var homeAddress1:String = "",

    @SerializedName("home_address_2")
    var homeAddress2:String = "",

    @SerializedName("home_address_3")
    var homeAddress3:String = "",

    @SerializedName("identity_type")
    var identityType:String = "",

    @SerializedName("employer_name")
    var employerName:String = "",

    @SerializedName("employer_country_id")
    var employerCountryId:String = "",

    @SerializedName("work_number")
    var workNumber:String = "",

    @SerializedName("work_address")
    var workAddress:String = "",

    @SerializedName("kin_surname")
    var kinSurname:String = "",

    @SerializedName("longitude")
    var longitude:String = "",

    @SerializedName("latitude")
    var latitude:String = "",

    @SerializedName("employer_sur_name")
    var employerSureName:String = "",


    var patientId:String = "",

    var supplyCards:String ="",
    var certType:String ="",
    var certNumber:String ="",
    var cardNoZk:String ="",

    @SerializedName("clinic_id")
    var clinicId:String ="",

    @SerializedName("department_id")
    var departmentId:String ="",
    @SerializedName("role_id")
    var roleId:String ="",

    @SerializedName("personal_title")
    var personalTitle:String,

    @SerializedName("driving_license_id")
    var drivingLicenseId:String,
    @SerializedName("patient_group")
    var patientGroup:String,

    @SerializedName("passport_id")
    var passportId:String,

    @SerializedName("personal_other_name")
    var personalOtherName:String,

    @SerializedName("nationality_id")
    var nationalityId:String? = "",

    @SerializedName("residence")
    var residence:String? = "",

    @SerializedName("address_line")
    var addressLine:String? = "",

    @SerializedName("city")
    var city:String? = "",

    @SerializedName("next_of_kin_title")
    var nextOfKinTitle:String? = "",
    @SerializedName("next_of_kin_gender")
    var nextOfKinGender:String? = "",
    @SerializedName("next_of_kin_dob")
    var nextOfKinDob:String? = "",

    @SerializedName("country_address")
    var countryAddress:String? = "",

    @SerializedName("postal_code")
    var postalCode:String? = "",

    @SerializedName("foriegn_national")
    var foriegnNational:Boolean? = false,

    @SerializedName("preferred_language")
    var preferredLanguage:String? = "",



    )