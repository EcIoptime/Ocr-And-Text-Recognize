package com.example.ocr.models.faceSearchForRegistration

import com.google.gson.annotations.SerializedName

data class User(

	@field:SerializedName("dialing_code")
	val dialingCode: String? = null,

	@field:SerializedName("kinRelation")
	val kinRelation: Any? = null,

	@field:SerializedName("parent_user")
	val parentUser: String? = null,

	@field:SerializedName("gender")
	val gender: String? = null,

	@field:SerializedName("created_at")
	val createdAt: String? = null,

	@field:SerializedName("current_team_id")
	val currentTeamId: Any? = null,

	@field:SerializedName("next_of_kin")
	val nextOfKin: String? = null,

	@field:SerializedName("document_id")
	val documentId: String? = null,

	@field:SerializedName("is_doctor")
	val isDoctor: String? = null,

	@field:SerializedName("VaccineVerified")
	val vaccineVerified: String? = null,

	@field:SerializedName("deactivated")
	val deactivated: String? = null,

	@field:SerializedName("is_admin")
	val isAdmin: Int? = null,

	@field:SerializedName("updated_at")
	val updatedAt: String? = null,

	@field:SerializedName("home_address")
	val homeAddress: String? = null,

	@field:SerializedName("face_id")
	val faceId: String? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("clinic_id")
	val clinicId: Any? = null,

	@field:SerializedName("email")
	val email: String? = null,

	@field:SerializedName("document_type")
	val documentType: String? = null,

	@field:SerializedName("profile_photo_url")
	val profilePhotoUrl: String? = null,

	@field:SerializedName("is_staff")
	val isStaff: Int? = null,

	@field:SerializedName("department_id")
	val departmentId: Any? = null,

	@field:SerializedName("next_of_kin_number")
	val nextOfKinNumber: String? = null,

	@field:SerializedName("expiry_date")
	val expiryDate: Any? = null,

	@field:SerializedName("profile_pic")
	val profilePic: String? = null,

	@field:SerializedName("covidTestId")
	val covidTestId: Any? = null,

	@field:SerializedName("email_verified_at")
	val emailVerifiedAt: Any? = null,

	@field:SerializedName("is_social_login")
	val isSocialLogin: String? = null,

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("phone_number")
	val phoneNumber: String? = null,

	@field:SerializedName("profile_photo_path")
	val profilePhotoPath: Any? = null,

	@field:SerializedName("age")
	val age: String? = null,

	@field:SerializedName("country_id")
	val countryId: Int? = null,

	@field:SerializedName("card")
	val card: String? = null
)