package com.itis.ocrapp.data.model

data class DocumentData(
    val documentType: String,
    val fullName: String? = null,
    val dateOfBirth: String? = null,
    val sex: String? = null,
    val dateOfIssue: String? = null,
    val dateOfExpiry: String? = null,
    val documentNumber: String? = null,
    val placeOfBirth: String? = null,
    val idCardNumber: String? = null,
    val address: String? = null,
    val licenseClass: String? = null,
    val placeOfOrigin: String? = null,
    val nationality: String? = null
)