package com.example.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String) : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

/** Modelo de finca/caficultor en Firestore (`/fincas/{fincaId}`). */
@Parcelize
@Keep
@IgnoreExtraProperties
data class FincaProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    @get:PropertyName("isPublic")
    @PropertyName("isPublic")
    val isPublic: Boolean = true,
    val ownerName: String = "",
    val recordType: String = "finca", // "finca" | "caficultor"
    val recordCode: String = "",
    val name: String = "",
    val subtitle: String = "",
    val location: String = "",
    val relatedCode: String = "",
    val areaHectares: Double = 0.0,
    val details: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) : Parcelable {
    val hasValidCoordinates: Boolean
        get() = latitude != null && longitude != null &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)

    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "isPublic" to true,
            "recordType" to recordType,
            "recordCode" to recordCode.take(40),
            "name" to name.take(120),
            "location" to location.take(180),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (ownerName.isNotBlank()) map["ownerName"] = ownerName.take(120)
        if (subtitle.isNotBlank()) map["subtitle"] = subtitle.take(180)
        if (relatedCode.isNotBlank()) map["relatedCode"] = relatedCode.take(60)
        if (areaHectares > 0.0) map["areaHectares"] = areaHectares
        if (details.isNotBlank()) map["details"] = details.take(500)
        if (latitude != null && longitude != null) {
            map["latitude"] = latitude
            map["longitude"] = longitude
        }
        return map
    }

    fun toUpdateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "ownerName" to ownerName.take(120),
            "recordType" to recordType,
            "recordCode" to recordCode.take(40),
            "name" to name.take(120),
            "subtitle" to subtitle.take(180),
            "location" to location.take(180),
            "relatedCode" to relatedCode.take(60),
            "areaHectares" to areaHectares.coerceAtLeast(0.0),
            "details" to details.take(500),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (latitude != null && longitude != null) {
            map["latitude"] = latitude
            map["longitude"] = longitude
        } else {
            map["latitude"] = FieldValue.delete()
            map["longitude"] = FieldValue.delete()
        }
        return map
    }
}

/** Modelo de registro de cosecha en Firestore (`/users/{userId}/cosechas/{cosechaId}`). */
@Parcelize
@Keep
@IgnoreExtraProperties
data class CosechaRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val recordCode: String = "",
    val workerName: String = "",
    val farmCode: String = "",
    val farmName: String = "",
    val date: String = "",
    val quantityKg: Double = 0.0,
    val details: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) : Parcelable {
    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "recordCode" to recordCode.take(40),
            "workerName" to workerName.take(120),
            "farmCode" to farmCode.take(60),
            "date" to date.take(20),
            "quantityKg" to quantityKg,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (farmName.isNotBlank()) map["farmName"] = farmName.take(120)
        if (details.isNotBlank()) map["details"] = details.take(500)
        return map
    }

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "workerName" to workerName.take(120),
        "farmCode" to farmCode.take(60),
        "farmName" to farmName.take(120),
        "date" to date.take(20),
        "quantityKg" to quantityKg,
        "details" to details.take(500),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

/** Modelo de actividad de bitácora de cultivo en Firestore (`/cultivos/{cultivoId}`). */
@Parcelize
@Keep
@IgnoreExtraProperties
data class CultivoActivity(
    @DocumentId val id: String = "",
    val userId: String = "",
    @get:PropertyName("isPublic")
    @PropertyName("isPublic")
    val isPublic: Boolean = true,
    val authorName: String = "",
    val recordCode: String = "",
    val activityType: String = "Siembra", // "Siembra" | "Abono" | "Poda" | "Control fitosanitario" | "Cosecha"
    val name: String = "",
    val farmId: String = "",
    val farmCode: String = "",
    val farmName: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val quantity: Double = 0.0,
    val details: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) : Parcelable {
    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "isPublic" to true,
            "recordCode" to recordCode.take(40),
            "activityType" to activityType,
            "name" to name.take(120),
            "farmCode" to farmCode.take(60),
            "date" to date.take(20),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (authorName.isNotBlank()) map["authorName"] = authorName.take(120)
        if (farmId.isNotBlank()) map["farmId"] = farmId.take(128)
        if (farmName.isNotBlank()) map["farmName"] = farmName.take(120)
        if (startTime.isNotBlank()) map["startTime"] = startTime.take(10)
        if (endTime.isNotBlank()) map["endTime"] = endTime.take(10)
        if (quantity > 0.0) map["quantity"] = quantity
        if (details.isNotBlank()) map["details"] = details.take(500)
        return map
    }

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "authorName" to authorName.take(120),
        "activityType" to activityType,
        "name" to name.take(120),
        "farmId" to farmId.take(128),
        "farmCode" to farmCode.take(60),
        "farmName" to farmName.take(120),
        "date" to date.take(20),
        "startTime" to startTime.take(10),
        "endTime" to endTime.take(10),
        "quantity" to quantity.coerceAtLeast(0.0),
        "details" to details.take(500),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

typealias BitacoraRecord = CultivoActivity

@Parcelize
@Keep
@IgnoreExtraProperties
data class CostoLine(
    val category: String = "Insumos",
    val item: String = "",
    val qty: Double = 1.0,
    val unit: Double = 0.0,
) : Parcelable {
    val subtotal: Double get() = qty * unit

    fun toMap(): Map<String, Any> = mapOf(
        "category" to category.take(40),
        "item" to item.take(120),
        "qty" to qty.coerceAtLeast(0.0),
        "unit" to unit.coerceAtLeast(0.0),
    )
}

@Parcelize
@Keep
@IgnoreExtraProperties
data class CostoCalculation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val recordCode: String = "",
    val name: String = "",
    val farmCode: String = "",
    val farmName: String = "",
    val date: String = "",
    val estimatedKg: Double = 0.0,
    val offerPricePerKg: Double = 0.0,
    val totalAmount: Double = 0.0,
    val lines: List<CostoLine> = emptyList(),
    val details: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) : Parcelable {
    fun toCreateMap(uid: String): Map<String, Any> {
        val safeLines = lines.take(30).map { it.toMap() }
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "recordCode" to recordCode.take(40),
            "name" to name.take(120),
            "date" to date.take(20),
            "estimatedKg" to estimatedKg.coerceAtLeast(0.0),
            "totalAmount" to totalAmount.coerceAtLeast(0.0),
            "lines" to safeLines,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (farmCode.isNotBlank()) map["farmCode"] = farmCode.take(60)
        if (farmName.isNotBlank()) map["farmName"] = farmName.take(120)
        if (offerPricePerKg > 0.0) map["offerPricePerKg"] = offerPricePerKg
        if (details.isNotBlank()) map["details"] = details.take(1000)
        return map
    }

    fun toUpdateMap(): Map<String, Any> {
        val safeLines = lines.take(30).map { it.toMap() }
        return mapOf(
            "name" to name.take(120),
            "farmCode" to farmCode.take(60),
            "farmName" to farmName.take(120),
            "date" to date.take(20),
            "estimatedKg" to estimatedKg.coerceAtLeast(0.0),
            "offerPricePerKg" to offerPricePerKg.coerceAtLeast(0.0),
            "totalAmount" to totalAmount.coerceAtLeast(0.0),
            "lines" to safeLines,
            "details" to details.take(1000),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
    }
}

@Keep
@IgnoreExtraProperties
data class VisitaAgronomica(
    @DocumentId val id: String = "",
    val userId: String = "",
    @get:PropertyName("isPublic")
    @PropertyName("isPublic")
    val isPublic: Boolean = true,
    val recordCode: String = "",
    val engineerName: String = "",
    val farmId: String = "",
    val farmCode: String = "",
    val farmName: String = "",
    val farmOwnerId: String = "",
    val farmOwnerName: String = "",
    val location: String = "",
    val date: String = "",
    val diagnosis: String = "",
    val activitiesPerformed: String = "",
    val recommendations: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "isPublic" to true,
            "recordCode" to recordCode.take(40),
            "engineerName" to engineerName.take(120),
            "diagnosis" to diagnosis.take(240),
            "date" to date.take(20),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (farmId.isNotBlank()) map["farmId"] = farmId.take(128)
        if (farmCode.isNotBlank()) map["farmCode"] = farmCode.take(60)
        if (farmName.isNotBlank()) map["farmName"] = farmName.take(120)
        if (farmOwnerId.isNotBlank()) map["farmOwnerId"] = farmOwnerId.take(128)
        if (farmOwnerName.isNotBlank()) map["farmOwnerName"] = farmOwnerName.take(120)
        if (location.isNotBlank()) map["location"] = location.take(180)
        if (activitiesPerformed.isNotBlank()) map["activitiesPerformed"] = activitiesPerformed.take(600)
        if (recommendations.isNotBlank()) map["recommendations"] = recommendations.take(600)
        return map
    }

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "engineerName" to engineerName.take(120),
        "farmId" to farmId.take(128),
        "farmCode" to farmCode.take(60),
        "farmName" to farmName.take(120),
        "farmOwnerId" to farmOwnerId.take(128),
        "farmOwnerName" to farmOwnerName.take(120),
        "location" to location.take(180),
        "date" to date.take(20),
        "diagnosis" to diagnosis.take(240),
        "activitiesPerformed" to activitiesPerformed.take(600),
        "recommendations" to recommendations.take(600),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class InquietudTecnica(
    @DocumentId val id: String = "",
    val userId: String = "",
    @get:PropertyName("isPublic")
    @PropertyName("isPublic")
    val isPublic: Boolean = true,
    val recordCode: String = "",
    val authorName: String = "",
    val authorRole: String = "Caficultor",
    val farmId: String = "",
    val farmCode: String = "",
    val farmName: String = "",
    val subject: String = "",
    val question: String = "",
    val status: String = "abierta", // "abierta" | "respondida"
    val date: String = "",
    val replyText: String = "",
    val replyByUid: String = "",
    val replyByName: String = "",
    val replyDate: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "isPublic" to true,
            "recordCode" to recordCode.take(40),
            "authorName" to authorName.take(120),
            "authorRole" to authorRole.ifBlank { "Caficultor" }.take(60),
            "subject" to subject.take(140),
            "question" to question.take(800),
            "status" to "abierta",
            "date" to date.take(20),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (farmId.isNotBlank()) map["farmId"] = farmId.take(128)
        if (farmCode.isNotBlank()) map["farmCode"] = farmCode.take(60)
        if (farmName.isNotBlank()) map["farmName"] = farmName.take(120)
        return map
    }

    fun toAuthorUpdateMap(): Map<String, Any> = mapOf(
        "subject" to subject.take(140),
        "question" to question.take(800),
        "farmId" to farmId.take(128),
        "farmCode" to farmCode.take(60),
        "farmName" to farmName.take(120),
        "date" to date.take(20),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    companion object {
        fun toReplyMap(
            replyText: String,
            replyByUid: String,
            replyByName: String,
            replyDate: String,
        ): Map<String, Any> = mapOf(
            "replyText" to replyText.trim().take(800),
            "replyByUid" to replyByUid,
            "replyByName" to replyByName.trim().take(120),
            "replyDate" to replyDate.trim().take(20),
            "status" to "respondida",
            "updatedAt" to FieldValue.serverTimestamp(),
        )
    }
}

@Keep
@IgnoreExtraProperties
data class RecolectorPostulacion(
    @DocumentId val id: String = "",
    val userId: String = "",
    val recordCode: String = "",
    val contactName: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val location: String = "",
    val experience: String = "Sin experiencia",
    val availability: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "recordCode" to recordCode.take(40),
        "contactName" to contactName.take(100),
        "contactEmail" to contactEmail.take(120),
        "contactPhone" to contactPhone.take(30),
        "location" to location.take(100),
        "experience" to experience.take(60),
        "availability" to availability.take(180),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "contactName" to contactName.take(100),
        "contactEmail" to contactEmail.take(120),
        "contactPhone" to contactPhone.take(30),
        "location" to location.take(100),
        "experience" to experience.take(60),
        "availability" to availability.take(180),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class ProveedorItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    @get:PropertyName("isPublic")
    @PropertyName("isPublic")
    val isPublic: Boolean = true,
    val ownerName: String = "",
    val recordCode: String = "",
    val name: String = "",
    val category: String = "",
    val productsServices: String = "",
    val priceReference: String = "",
    val commercialContact: String = "",
    val location: String = "",
    val details: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    val hasValidCoordinates: Boolean
        get() = latitude != null && longitude != null &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)

    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "isPublic" to true,
            "recordCode" to recordCode.take(40),
            "name" to name.take(120),
            "productsServices" to productsServices.take(300),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (ownerName.isNotBlank()) map["ownerName"] = ownerName.take(120)
        if (category.isNotBlank()) map["category"] = category.take(80)
        if (priceReference.isNotBlank()) map["priceReference"] = priceReference.take(100)
        if (commercialContact.isNotBlank()) map["commercialContact"] = commercialContact.take(100)
        if (location.isNotBlank()) map["location"] = location.take(180)
        if (details.isNotBlank()) map["details"] = details.take(500)
        if (latitude != null && longitude != null) {
            map["latitude"] = latitude
            map["longitude"] = longitude
        }
        return map
    }

    fun toUpdateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "ownerName" to ownerName.take(120),
            "name" to name.take(120),
            "category" to category.take(80),
            "productsServices" to productsServices.take(300),
            "priceReference" to priceReference.take(100),
            "commercialContact" to commercialContact.take(100),
            "location" to location.take(180),
            "details" to details.take(500),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (latitude != null && longitude != null) {
            map["latitude"] = latitude
            map["longitude"] = longitude
        } else {
            map["latitude"] = FieldValue.delete()
            map["longitude"] = FieldValue.delete()
        }
        return map
    }
}

@Keep
@IgnoreExtraProperties
data class CompradorItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    @get:PropertyName("isPublic")
    @PropertyName("isPublic")
    val isPublic: Boolean = true,
    val actorType: String = "comprador", // "comprador" | "vendedor"
    val recordCode: String = "",
    val name: String = "",
    val purchaseType: String = "",
    val offerAmountCop: Double = 0.0,
    val priceUnit: String = "kg pergamino seco",
    val priceDate: String = "",
    val location: String = "",
    val details: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    val hasValidCoordinates: Boolean
        get() = latitude != null && longitude != null &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)

    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "isPublic" to true,
            "actorType" to actorType,
            "recordCode" to recordCode.take(40),
            "name" to name.take(120),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (purchaseType.isNotBlank()) map["purchaseType"] = purchaseType.take(180)
        if (offerAmountCop > 0.0) map["offerAmountCop"] = offerAmountCop
        if (priceUnit.isNotBlank()) map["priceUnit"] = priceUnit.take(40)
        if (priceDate.isNotBlank()) map["priceDate"] = priceDate.take(20)
        if (location.isNotBlank()) map["location"] = location.take(180)
        if (details.isNotBlank()) map["details"] = details.take(500)
        if (latitude != null && longitude != null) {
            map["latitude"] = latitude
            map["longitude"] = longitude
        }
        return map
    }

    fun toUpdateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "actorType" to actorType,
            "name" to name.take(120),
            "purchaseType" to purchaseType.take(180),
            "offerAmountCop" to offerAmountCop.coerceAtLeast(0.0),
            "priceUnit" to priceUnit.take(40),
            "priceDate" to priceDate.take(20),
            "location" to location.take(180),
            "details" to details.take(500),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (latitude != null && longitude != null) {
            map["latitude"] = latitude
            map["longitude"] = longitude
        } else {
            map["latitude"] = FieldValue.delete()
            map["longitude"] = FieldValue.delete()
        }
        return map
    }
}

/** Modelo de registro de precios del café en Firestore (`/users/{userId}/precios_cafe/{precioId}`). */
@Parcelize
@Keep
@IgnoreExtraProperties
data class PrecioCafeRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val recordCode: String = "",
    val date: String = "",
    val source: String = "FNC",
    val pricePerCargaCop: Double = 0.0,
    val pricePerKgCop: Double = 0.0,
    val nyFuturesCentsLb: Double = 0.0,
    val usdCopTrm: Double = 0.0,
    val notes: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) : Parcelable {
    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "recordCode" to recordCode.take(40),
            "date" to date.take(20),
            "source" to source.take(60),
            "pricePerCargaCop" to pricePerCargaCop,
            "pricePerKgCop" to pricePerKgCop,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (nyFuturesCentsLb > 0.0) map["nyFuturesCentsLb"] = nyFuturesCentsLb
        if (usdCopTrm > 0.0) map["usdCopTrm"] = usdCopTrm
        if (notes.isNotBlank()) map["notes"] = notes.take(400)
        return map
    }

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "date" to date.take(20),
        "source" to source.take(60),
        "pricePerCargaCop" to pricePerCargaCop,
        "pricePerKgCop" to pricePerKgCop,
        "nyFuturesCentsLb" to nyFuturesCentsLb.coerceAtLeast(0.0),
        "usdCopTrm" to usdCopTrm.coerceAtLeast(0.0),
        "notes" to notes.take(400),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

/** Mapa de capacidades de permisos por rol persistido en Firestore (`/roles` y `/users`). */
@Parcelize
@Keep
@IgnoreExtraProperties
data class RoleCapabilities(
    val manageOwnFarms: Boolean = false,
    val logOwnCropActivities: Boolean = false,
    val recordOwnHarvests: Boolean = false,
    val calculateOwnCosts: Boolean = false,
    val manageCollectors: Boolean = false,
    val createInquiries: Boolean = false,
    val registerAgronomyVisits: Boolean = false,
    val replyInquiries: Boolean = false,
    val publishProviderCatalog: Boolean = false,
    val publishBuyerPrices: Boolean = false,
    val recordCoffeePrices: Boolean = false,
    val viewCommunity: Boolean = true,
) : Parcelable {
    fun toMap(): Map<String, Any> = mapOf(
        "manageOwnFarms" to manageOwnFarms,
        "logOwnCropActivities" to logOwnCropActivities,
        "recordOwnHarvests" to recordOwnHarvests,
        "calculateOwnCosts" to calculateOwnCosts,
        "manageCollectors" to manageCollectors,
        "createInquiries" to createInquiries,
        "registerAgronomyVisits" to registerAgronomyVisits,
        "replyInquiries" to replyInquiries,
        "publishProviderCatalog" to publishProviderCatalog,
        "publishBuyerPrices" to publishBuyerPrices,
        "recordCoffeePrices" to recordCoffeePrices,
        "viewCommunity" to viewCommunity,
    )
}

@Parcelize
@Keep
@IgnoreExtraProperties
data class RoleDefinition(
    @DocumentId val id: String = "",
    val roleId: String = "",
    val displayName: String = "",
    val aliasLabel: String = "",
    val description: String = "",
    @get:PropertyName("isActive")
    @PropertyName("isActive")
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val permissions: RoleCapabilities = RoleCapabilities(),
) : Parcelable

enum class AppRole(
    val roleId: String,
    val displayName: String,
    val aliasLabel: String,
    val shortUpperLabel: String,
    val summary: String,
    val ownFocusBullets: List<String>,
    val communityFocusText: String,
    val sortOrder: Int,
    val capabilities: RoleCapabilities,
) {
    CAFICULTOR(
        roleId = "caficultor",
        displayName = "Caficultor",
        aliasLabel = "Agricultor",
        shortUpperLabel = "CAFICULTOR",
        summary = "Gestiona exclusivamente tus fincas, bitácoras de cultivo, cosechas, costos y consultas técnicas.",
        ownFocusBullets = listOf(
            "Crear y editar tus propias fincas y su ubicación en el mapa",
            "Registrar labores en tu bitácora de cultivo",
            "Llevar cosechas por recolector y calculadora de costos",
            "Enviar inquietudes técnicas a ingenieros agrónomos",
        ),
        communityFocusText = "Visualiza fincas de otros caficultores, visitas de agrónomos, catálogo de proveedores y precios de compradores (solo lectura).",
        sortOrder = 1,
        capabilities = RoleCapabilities(
            manageOwnFarms = true,
            logOwnCropActivities = true,
            recordOwnHarvests = true,
            calculateOwnCosts = true,
            manageCollectors = true,
            createInquiries = true,
            recordCoffeePrices = true,
            viewCommunity = true,
        ),
    ),
    AGRONOMO(
        roleId = "agronomo",
        displayName = "Ingeniero agrónomo",
        aliasLabel = "Agrónomo",
        shortUpperLabel = "AGRÓNOMO",
        summary = "Revisa cualquier finca de la comunidad, registra visitas técnicas y responde inquietudes.",
        ownFocusBullets = listOf(
            "Seleccionar cualquier finca y registrar visitas agronómicas",
            "Publicar diagnósticos, labores ejecutadas y recomendaciones",
            "Responder inquietudes técnicas abiertas por caficultores",
        ),
        communityFocusText = "Consulta fincas, bitácoras de cultivo, proveedores y precios del día (sin editar datos de fincas ni precios).",
        sortOrder = 2,
        capabilities = RoleCapabilities(
            registerAgronomyVisits = true,
            replyInquiries = true,
            viewCommunity = true,
        ),
    ),
    PROVEEDOR(
        roleId = "proveedor",
        displayName = "Proveedor",
        aliasLabel = "Proveedor",
        shortUpperLabel = "PROVEEDOR",
        summary = "Publica y administra tu catálogo de insumos, fertilizantes, precios y punto comercial.",
        ownFocusBullets = listOf(
            "Publicar productos, insumos, precios de referencia y contacto",
            "Marcar tu punto de venta de insumos en el mapa territorial",
            "Actualizar disponibilidad y portafolio comercial",
        ),
        communityFocusText = "Consulta fincas, bitácoras, visitas técnicas y precios del mercado cafetero (solo lectura).",
        sortOrder = 3,
        capabilities = RoleCapabilities(
            publishProviderCatalog = true,
            viewCommunity = true,
        ),
    ),
    COMPRADOR(
        roleId = "comprador",
        displayName = "Comprador",
        aliasLabel = "Comprador",
        shortUpperLabel = "COMPRADOR",
        summary = "Publica tus precios diarios de compra por carga/kg y ubica tu punto de acopio en el mapa.",
        ownFocusBullets = listOf(
            "Publicar y actualizar precios diarios de compra de café",
            "Registrar tu historial de precios, futuros NY y TRM",
            "Ubicar tu punto de compra o comercialización en el mapa",
        ),
        communityFocusText = "Consulta fincas, bitácoras de producción, visitas agronómicas y proveedores (solo lectura).",
        sortOrder = 4,
        capabilities = RoleCapabilities(
            publishBuyerPrices = true,
            recordCoffeePrices = true,
            viewCommunity = true,
        ),
    );

    fun toDefaultDefinition(): RoleDefinition = RoleDefinition(
        id = roleId,
        roleId = roleId,
        displayName = displayName,
        aliasLabel = aliasLabel,
        description = summary,
        isActive = true,
        sortOrder = sortOrder,
        permissions = capabilities,
    )

    companion object {
        fun fromRoleOrId(roleId: String?, roleName: String?): AppRole {
            val cleanId = roleId.orEmpty().trim().lowercase()
            if (cleanId.isNotEmpty()) {
                entries.firstOrNull { it.roleId == cleanId }?.let { return it }
            }
            val cleanName = roleName.orEmpty().trim()
            return when {
                cleanName.equals("Agricultor", ignoreCase = true) -> CAFICULTOR
                cleanName.equals("Caficultor", ignoreCase = true) -> CAFICULTOR
                cleanName.contains("agrónomo", ignoreCase = true) ||
                    cleanName.contains("agronomo", ignoreCase = true) -> AGRONOMO
                cleanName.equals("Proveedor", ignoreCase = true) -> PROVEEDOR
                cleanName.equals("Comprador", ignoreCase = true) -> COMPRADOR
                else -> CAFICULTOR
            }
        }
    }
}

/** Modelo de usuario en Firestore (`/users/{userId}`) con rol bloqueado y mapa de permisos. */
@Parcelize
@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "San José de Albán, Nariño",
    val roleId: String = AppRole.CAFICULTOR.roleId,
    val role: String = AppRole.CAFICULTOR.displayName,
    val roleLocked: Boolean = true,
    val permissions: RoleCapabilities = AppRole.CAFICULTOR.capabilities,
    val farmName: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) : Parcelable {
    val resolvedAppRole: AppRole
        get() = AppRole.fromRoleOrId(roleId, role)

    fun toCreateMap(uid: String, authEmail: String): Map<String, Any> {
        val resolvedRole = AppRole.fromRoleOrId(roleId, role)
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "fullName" to fullName.trim().take(120),
            "phone" to phone.trim().take(30),
            "location" to location.trim().ifBlank { "San José de Albán, Nariño" }.take(150),
            "roleId" to resolvedRole.roleId,
            "role" to resolvedRole.displayName,
            "roleLocked" to true,
            "permissions" to resolvedRole.capabilities.toMap(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        val resolvedEmail = email.ifBlank { authEmail }.trim()
        if (resolvedEmail.isNotBlank()) map["email"] = resolvedEmail.take(150)
        if (farmName.isNotBlank()) map["farmName"] = farmName.trim().take(120)
        return map
    }

    fun toUpdateMap(authEmail: String): Map<String, Any> {
        // El rol, roleId, roleLocked y permissions son inmutables tras el registro.
        val map = mutableMapOf<String, Any>(
            "fullName" to fullName.trim().take(120),
            "phone" to phone.trim().take(30),
            "location" to location.trim().ifBlank { "San José de Albán, Nariño" }.take(150),
            "farmName" to farmName.trim().take(120),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        val resolvedEmail = email.ifBlank { authEmail }.trim()
        if (resolvedEmail.isNotBlank()) map["email"] = resolvedEmail.take(150)
        return map
    }
}

data class PendingUserRegistration(
    val fullName: String,
    val phone: String,
    val location: String,
    val roleId: String,
    val role: String,
    val farmName: String = "",
)
