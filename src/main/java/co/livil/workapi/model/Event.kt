package co.livil.workapi.model

import co.livil.workapi.utils.DateHelper
import com.squareup.moshi.Json
import moe.banana.jsonapi2.JsonApi
import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.recur.RecurrenceRule
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * @see EventSerializerTest for sample JSON
 */
@JsonApi(type = "event")
data class Event(
    @field:Json(name = "name") var name: String = "",
    @field:Json(name = "description") var description: String = "",
    @field:Json(name = "location") var location: String = "",
    @field:Json(name = "start_date_time") var startDateTime: String = "",
    @field:Json(name = "start_timezone") var startTimezone: String = "",
    @field:Json(name = "end_date_time") var endDateTime: String = "",
    @field:Json(name = "end_timezone") var endTimezone: String = "",
    @field:Json(name = "all_day") var allDay: Boolean = false,
    @field:Json(name = "recurrence") var recurrence: String = "",
    @field:Json(name = "created_at") val createdAt: String = "",
    @field:Json(name = "updated_at") val updatedAt: String = "",
    @field:Json(name = "attendees") var attendees: List<Attendee>? = null
) : WorkApiResource() {

    /**
     * If the event is a base event and has multiple occurrences, they are generated here
     *
     * If no dates are supplied, this function will return the occurrences with range of the
     * current day (00:00:00 to 23:59:59)
     */
    fun occurrences(
        from: LocalDateTime = DateHelper.startOfDay(),
        until: LocalDateTime = DateHelper.endOfDay(),
        maxInstances: Int = 10
    ): List<Event> {
        if (recurrence.isEmpty()) { return emptyList() }

        val rule: RecurrenceRule = RecurrenceRule(recurrence)
        val start: DateTime = DateTime(from.toEpochSecond(ZoneOffset.UTC) * 1000)

        val iterator = rule.iterator(start)
        var count = maxInstances
        val occurrences = mutableListOf<Event>()
        val diff = DateHelper.fromIsoDateString(endDateTime).toEpochSecond(ZoneOffset.UTC) - DateHelper.fromIsoDateString(startDateTime).toEpochSecond(ZoneOffset.UTC)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        while (iterator.hasNext() && (!rule.isInfinite || count-- > 0)) {
            val occurrenceStart: Long = iterator.nextDateTime().timestamp
            if (occurrenceStart >= until.toEpochSecond(ZoneOffset.UTC) * 1000) { break }

            val occurrenceEnd: Long = occurrenceStart + (diff * 1000)
            val startDate = LocalDateTime.ofEpochSecond(occurrenceStart / 1000, 0, ZoneOffset.UTC)
            val endDate = LocalDateTime.ofEpochSecond(occurrenceEnd / 1000, 0, ZoneOffset.UTC)

            val occurrence = this.copy(
                startDateTime = startDate.format(formatter),
                endDateTime = endDate.format(formatter)
            )

            occurrences.add(occurrence)
        }

        return occurrences
    }
}