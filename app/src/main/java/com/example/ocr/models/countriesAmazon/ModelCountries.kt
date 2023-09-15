package com.example.ocr.models.countriesAmazon

import com.google.gson.annotations.SerializedName

data class ModelCountries(
    @SerializedName("id")
    var countryId:String,
    @SerializedName("name")
    var name:String,
    @SerializedName("nicename")
    var nicename:String,
    @SerializedName("phonecode")
    var dialingCode:String,
    @SerializedName("iso")
    var isoCode:String,
    @SerializedName("iso3")
    var iso3Code:String,
    @SerializedName("numcode")
    var numcode:String,
){
    override fun toString(): String {
        return "$nicename"
    }
}
