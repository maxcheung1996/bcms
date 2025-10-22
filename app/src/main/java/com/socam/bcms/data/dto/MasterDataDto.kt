package com.socam.bcms.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs for Master Data endpoints
 */

// Regions (Block) - GET /Masters/{projId}/Locations/Regions
data class MasterRegionDto(
    @SerializedName("Key")
    val key: String,
    @SerializedName("Value")
    val value: String
)

// Floors - GET /Masters/{projId}/Locations/Floors  
data class MasterFloorDto(
    @SerializedName("Key")
    val key: String,
    @SerializedName("Value")
    val value: String
)

// Concrete Grades - GET /Masters/{projId}/Concretes/Grades
data class MasterConcreteGradeDto(
    @SerializedName("Id")
    val id: Int,
    @SerializedName("Grade")
    val grade: String,
    @SerializedName("IsDefault")
    val isDefault: Int
)

// Locations (Units) - GET /Masters/{projId}/Locations/List
data class MasterLocationDto(
    @SerializedName("RoomId")
    val roomId: String,
    @SerializedName("ProjId")
    val projId: String,
    @SerializedName("RegionFloorCode")
    val regionFloorCode: String?,
    @SerializedName("Region")
    val region: String?,
    @SerializedName("Floor")
    val floor: String?,
    @SerializedName("RegionFloorSort")
    val regionFloorSort: Int?,
    @SerializedName("AreaLocationCode")
    val areaLocationCode: String?,
    @SerializedName("AreaGroup")
    val areaGroup: String?,
    @SerializedName("LocationType")
    val locationType: String?,
    @SerializedName("AreaLocationSort")
    val areaLocationSort: Int?,
    @SerializedName("Room")
    val room: String?,
    @SerializedName("Remarks")
    val remarks: String?,
    @SerializedName("RoomSort")
    val roomSort: Int?,
    @SerializedName("RoomRfid")
    val roomRfid: Int,
    @SerializedName("FloorPlanFileGuid")
    val floorPlanFileGuid: String?
)

// Categories & Subcategories - GET /Masters/{projId}/Bcs/Categories
data class MasterCategoryDto(
    @SerializedName("Bctype")
    val bctype: String,
    @SerializedName("IsSubcategory")
    val isSubcategory: Int,
    @SerializedName("Category")
    val category: String,
    @SerializedName("DescEN")
    val descEN: String?,
    @SerializedName("DescTC")
    val descTC: String?,
    @SerializedName("DescSC")
    val descSC: String?,
    @SerializedName("IsDefault")
    val isDefault: Int
)

// Companies (RSCompany & Hinge Supplier) - GET /Masters/{projId}/Companies/List
data class MasterCompanyDto(
    @SerializedName("Id")
    val id: String,
    @SerializedName("Type")
    val type: String, // Manufacturer, HingeSupplier, RSCompany
    @SerializedName("BCType")
    val bcType: String?,
    @SerializedName("RefCode")
    val refCode: String?,
    @SerializedName("NameEN")
    val nameEN: String?,
    @SerializedName("NameTC")
    val nameTC: String?,
    @SerializedName("NameSC")
    val nameSC: String?,
    @SerializedName("AddressEN")
    val addressEN: String?,
    @SerializedName("AddressTC")
    val addressTC: String?,
    @SerializedName("AddressSC")
    val addressSC: String?,
    @SerializedName("GpsLat")
    val gpsLat: Double?,
    @SerializedName("GpsLong")
    val gpsLong: Double?,
    @SerializedName("IsDefault")
    val isDefault: Int
)

// Workflow Steps - GET /Masters/{projId}/WorkFlows/Steps/FullList
data class MasterWorkflowStepDto(
    @SerializedName("Step")
    val step: String,
    @SerializedName("Portion")
    val portion: Int,
    @SerializedName("Bctype")
    val bctype: String,
    @SerializedName("CanUpdate")
    val canUpdate: Int,
    @SerializedName("TypeEN")
    val typeEN: String?,
    @SerializedName("TypeTC")
    val typeTC: String?,
    @SerializedName("TypeSC")
    val typeSC: String?,
    @SerializedName("StepDescEN")
    val stepDescEN: String?,
    @SerializedName("StepDescTC")
    val stepDescTC: String?,
    @SerializedName("StepDescSC")
    val stepDescSC: String?,
    @SerializedName("AllowField")
    val allowField: List<String>
)

// Contracts - GET /Masters/Contracts/List?projid={projId}
data class MasterContractDto(
    @SerializedName("ProjId")
    val projId: String,
    @SerializedName("ContractNo")
    val contractNo: String,
    @SerializedName("ContractorNameEN")
    val contractorNameEN: String?,
    @SerializedName("ContractorNameTC")
    val contractorNameTC: String?,
    @SerializedName("ContractorNameSC")
    val contractorNameSC: String?,
    @SerializedName("ContractDescEN")
    val contractDescEN: String?,
    @SerializedName("ContractDescTC")
    val contractDescTC: String?,
    @SerializedName("ContractDescSC")
    val contractDescSC: String?,
    @SerializedName("ContractStartDate")
    val contractStartDate: String?,
    @SerializedName("ContractEndDate")
    val contractEndDate: String?
)

// Authentication DTOs
data class LoginRequest(
    @SerializedName("UserName")
    val userName: String,
    @SerializedName("Password")
    val password: String
)

data class LoginResponse(
    @SerializedName("Status")
    val status: Int,
    @SerializedName("Message")
    val message: String,
    @SerializedName("Token")
    val token: String,
    @SerializedName("UserName")
    val userName: String,
    @SerializedName("Role")
    val role: String
)
