package com.socam.bcms.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for RfidModule data from API
 * Maps directly to RfidModule database table
 * Based on actual API response structure from /Rfids/{projId}/List
 */
data class RfidModuleDto(
    @SerializedName("Id")
    val id: String,
    @SerializedName("ProjId")
    val projId: String?,
    @SerializedName("ContractNo")
    val contractNo: String?,
    @SerializedName("ManufacturerId")
    val manufacturerId: String?,
    @SerializedName("TagId")
    val tagId: String?,
    @SerializedName("IsActivated")
    val isActivated: Int,
    @SerializedName("ActivatedDate")
    val activatedDate: String?,
    @SerializedName("Bctype")
    val bcType: String,
    @SerializedName("RfidtagNo")
    val rfidTagNo: String?,
    @SerializedName("StepCode")
    val stepCode: String?,
    @SerializedName("Category")
    val category: String?,
    @SerializedName("Subcategory")
    val subcategory: String?,
    @SerializedName("SupplierId")
    val supplierId: String?,
    @SerializedName("ConcreteGrade")
    val concreteGrade: String?,
    @SerializedName("ASN")
    val asn: String?,
    @SerializedName("SerialNo")
    val serialNo: String?,
    @SerializedName("WorkingNo")
    val workingNo: Int?,
    @SerializedName("ManufacturingDate")
    val manufacturingDate: String?,
    @SerializedName("ProductNo")
    val productNo: String?,
    @SerializedName("RscompanyId")
    val rsCompanyId: String?,
    @SerializedName("RsinspectionDate")
    val rsInspectionDate: String?,
    @SerializedName("CastingDate")
    val castingDate: String?,
    @SerializedName("FirstCastingDate")
    val firstCastingDate: String?,
    @SerializedName("SecondCastingDate")
    val secondCastingDate: String?,
    @SerializedName("WaterproofingInstallationDate")
    val waterproofingInstallationDate: String?,
    @SerializedName("InternalFinishDate")
    val internalFinishDate: String?,
    @SerializedName("DeliveryDate")
    val deliveryDate: String?,
    @SerializedName("BatchNo")
    val batchNo: String?,
    @SerializedName("LicensePlateNo")
    val licensePlateNo: String?,
    @SerializedName("GpsDeviceId")
    val gpsDeviceId: String?,
    @SerializedName("SiteArrivalDate")
    val siteArrivalDate: String?,
    @SerializedName("SiteInstallationDate")
    val siteInstallationDate: String?,
    @SerializedName("RoomId")
    val roomId: String?,
    @SerializedName("RoomInput")
    val roomInput: String?,
    @SerializedName("Floor")
    val floor: String?,
    @SerializedName("Region")
    val region: String?,
    @SerializedName("ChipFailureSa")
    val chipFailureSa: Int,
    @SerializedName("ChipFailureSi")
    val chipFailureSi: Int,
    @SerializedName("Dispose")
    val dispose: Int,
    @SerializedName("CreatedDate")
    val createdDate: String,
    @SerializedName("CreatedBy")
    val createdBy: String?,
    @SerializedName("UpdatedDate")
    val updatedDate: String?,
    @SerializedName("UpdatedBy")
    val updatedBy: String?,
    @SerializedName("IsCompleted10")
    val isCompleted10: Int,
    @SerializedName("Remark10")
    val remark10: String?,
    @SerializedName("IsCompleted20")
    val isCompleted20: Int,
    @SerializedName("Remark20")
    val remark20: String?,
    @SerializedName("IsCompleted30")
    val isCompleted30: Int,
    @SerializedName("Remark30")
    val remark30: String?,
    @SerializedName("IsCompleted40")
    val isCompleted40: Int,
    @SerializedName("Remark40")
    val remark40: String?,
    @SerializedName("IsCompleted50")
    val isCompleted50: Int,
    @SerializedName("Remark50")
    val remark50: String?,
    @SerializedName("IsCompleted55")
    val isCompleted55: Int,
    @SerializedName("Remark55")
    val remark55: String?,
    @SerializedName("IsCompleted60")
    val isCompleted60: Int,
    @SerializedName("Remark60")
    val remark60: String?,
    @SerializedName("IsCompleted70")
    val isCompleted70: Int,
    @SerializedName("Remark70")
    val remark70: String?,
    @SerializedName("IsCompleted80")
    val isCompleted80: Int,
    @SerializedName("Remark80")
    val remark80: String?
)

/**
 * Request body for RfidModule API
 * Keep ComponentTagRequest name as it matches the API endpoint structure
 */
data class RfidModuleRequest(
    @SerializedName("Bctype")
    val bctype: List<String>
)
