package com.inci.campuscompanion.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class EventDto(
    val id: String,
    val name: String,
    val startingDate: String,
    val endingDate: String? = null,
    val description: String? = null,
    val address: String? = null,
    val avatar: String? = null,
    val createdAt: String? = null
)

data class CampusEvent(
    val id: String,
    val title: String,
    val startTime: String,
    val endTime: String?,
    val address: String,
    val description: String
)

interface CampusApi {
    @GET("event")
    suspend fun getEvents(): List<EventDto>
}

class EventsRemoteDataSource(private val api: CampusApi) {
    suspend fun getEvents(): List<EventDto> = api.getEvents()
}

class InvalidEventDataException : Exception("The events response contains invalid data")

class EventsRepository(private val remote: EventsRemoteDataSource) {
    suspend fun getEvents(): List<CampusEvent> {
        val parsed = remote.getEvents().map { dto ->
            if (dto.id.isBlank() || dto.name.isBlank()) throw InvalidEventDataException()

            val startsAt = parseDate(dto.startingDate)
            val endsAt = dto.endingDate?.let(::parseDate)
            if (endsAt != null && endsAt.isBefore(startsAt)) throw InvalidEventDataException()

            startsAt to CampusEvent(
                id = dto.id,
                title = dto.name.trim(),
                startTime = format(startsAt),
                endTime = endsAt?.let(::format),
                address = dto.address?.takeIf(String::isNotBlank) ?: "Location to be announced",
                description = dto.description?.takeIf(String::isNotBlank) ?: "No description available."
            )
        }

        if (parsed.map { it.second.id }.distinct().size != parsed.size) {
            throw InvalidEventDataException()
        }
        return parsed.sortedBy { it.first }.map { it.second }
    }

    private fun parseDate(value: String): Instant = try {
        Instant.parse(value)
    } catch (_: Exception) {
        throw InvalidEventDataException()
    }

    private fun format(value: Instant): String = DateTimeFormatter
        .ofPattern("EEE, d MMM yyyy 'at' h:mm a z")
        .withZone(ZoneId.systemDefault())
        .format(value)
}

object EventsNetwork {
    const val BASE_URL = "https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/"

    fun createApi(baseUrl: String = BASE_URL): CampusApi {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CampusApi::class.java)
    }

    val repository: EventsRepository by lazy {
        EventsRepository(EventsRemoteDataSource(createApi()))
    }
}
