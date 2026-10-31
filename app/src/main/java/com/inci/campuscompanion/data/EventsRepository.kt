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

// Field names match the supplied API, rather than the illustrative lecture DTO.
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
data class CampusEvent(val id: String, val title: String, val timeLabel: String,
 val endLabel: String, val locationLabel: String, val description: String)
interface CampusApi {
 @GET("event") suspend fun getEvents(): List<EventDto>
}
class EventsRemoteDataSource(private val api: CampusApi) {
 suspend fun getEvents(): List<EventDto> = api.getEvents()
}
class InvalidEventData : Exception("Invalid event data")
class EventsRepository(private val remote: EventsRemoteDataSource) {
 suspend fun getEvents(): List<CampusEvent> {
  val events = remote.getEvents().map { dto ->
   if (dto.id.isBlank() || dto.name.isBlank()) throw InvalidEventData()
   val start = parseDate(dto.startingDate)
   val end = dto.endingDate?.let(::parseDate)
   if (end != null && end.isBefore(start)) throw InvalidEventData()
   start to CampusEvent(dto.id, dto.name.trim(), format(start), end?.let(::format) ?: "Not specified",
    dto.address?.takeIf { it.isNotBlank() } ?: "Location to be announced",
    dto.description?.takeIf { it.isNotBlank() } ?: "No description available.")
  }
  if (events.map { it.second.id }.distinct().size != events.size) throw InvalidEventData()
  return events.sortedBy { it.first }.map { it.second }
 }
 private fun parseDate(value: String): Instant = try { Instant.parse(value) } catch (_: java.time.format.DateTimeParseException) { throw InvalidEventData() }
 private fun format(value: Instant): String = DateTimeFormatter.ofPattern("EEE, d MMM yyyy • h:mm a z").withZone(ZoneId.systemDefault()).format(value)
}
object AppContainer {
 const val BASE_URL = "https://6ad70838f71f154ac4879fac.mockapi.io/api/v1/events/"
 fun createApi(baseUrl: String = BASE_URL, client: OkHttpClient? = null): CampusApi = Retrofit.Builder()
  .baseUrl(baseUrl)
  .client(client ?: OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build())
  .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()))
  .build().create(CampusApi::class.java)
 val repository by lazy { EventsRepository(EventsRemoteDataSource(createApi())) }
}
