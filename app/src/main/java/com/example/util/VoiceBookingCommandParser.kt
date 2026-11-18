package com.example.util

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val VOICE_PARSER_TAG = "VoiceBookingParser"

/**
 * Structured booking command parsed from natural language speech (English or Hindi/Hinglish).
 */
data class ParsedVoiceBookingCommand(
    val rawTranscript: String,
    val isBookingIntent: Boolean = true,
    val intentCategory: String = "Photography", // "Photography", "Modeling", "Video Production"
    val serviceType: String = "Photography",
    val preferredDate: String = "",
    val preferredTime: String = "10:00 AM (Morning Key Light Bay)",
    val location: String = "Studio (Talwara Khurd - 4K Lighting Bay)",
    val extractedClientName: String = "",
    val extractedPhoneNumber: String = "",
    val extractedNotes: String = "",
    val confidenceSummary: String = "",
    val spokenConfirmationEn: String = "",
    val spokenConfirmationHi: String = "",
    val isConfirmCommand: Boolean = false,
    val parsedByEngine: String = "ON_DEVICE_NLU"
)

/**
 * Hybrid voice-to-booking natural language command parser:
 * 1. Deterministic bilingual (English + Hindi/Hinglish) rule-based parser (`parseLocally`) for instant offline accuracy.
 * 2. Optional Firebase AI Logic (`gemini-3.8-flash` with `Schema.obj(...)`) structured JSON refinement (`parseWithGeminiAndFallback`)
 *    when network + App Check are available, gracefully falling back to on-device NLU on timeout or error.
 */
object VoiceBookingCommandParser {

    val STANDARD_TIME_SLOTS = listOf(
        "10:00 AM (Morning Key Light Bay)",
        "12:00 PM (Midday High-Key Flash)",
        "02:30 PM (Editorial Softbox Bay)",
        "04:30 PM (Golden Hour Outdoor)",
        "06:30 PM (Dark & Rim Mood Studio)"
    )

    val LOCATION_STUDIO = "Studio (Talwara Khurd - 4K Lighting Bay)"
    val LOCATION_OUTDOOR = "Outdoor (Sirsa & Ellenabad Region)"
    val LOCATION_HYBRID = "Hybrid (Studio + Outdoor)"
    val LOCATION_CLIENT = "Client Location (Custom Set)"

    private val bookingJsonSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "isBookingIntent" to Schema.boolean(description = "True if user wants to book/schedule/reserve a shoot session or update details"),
                "isConfirmCommand" to Schema.boolean(description = "True if user is merely saying yes/confirm/book now to confirm an existing preview"),
                "intentCategory" to Schema.enumeration(
                    values = listOf("Photography", "Modeling", "Video Production"),
                    description = "Primary studio service category"
                ),
                "serviceType" to Schema.string(description = "Specific package name matching Star Artist Studio services"),
                "preferredDate" to Schema.string(description = "Resolved YYYY-MM-DD date based on referenceDate"),
                "preferredTime" to Schema.string(description = "Resolved studio time slot or HH:MM AM/PM"),
                "location" to Schema.string(description = "Resolved shoot location"),
                "extractedClientName" to Schema.string(description = "Client name if mentioned, otherwise empty"),
                "extractedPhoneNumber" to Schema.string(description = "Phone digits if mentioned, otherwise empty"),
                "extractedNotes" to Schema.string(description = "Creative requirements, outfit, theme, lighting, or special requests"),
                "spokenConfirmationEn" to Schema.string(description = "Natural 1-sentence English confirmation summary"),
                "spokenConfirmationHi" to Schema.string(description = "Natural 1-sentence Hindi confirmation summary")
            ),
            optionalProperties = listOf(
                "extractedClientName",
                "extractedPhoneNumber",
                "extractedNotes",
                "spokenConfirmationEn",
                "spokenConfirmationHi"
            )
        )
    }

    private val geminiParserModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.1f
                responseMimeType = "application/json"
                responseSchema = bookingJsonSchema
            }
        )
    }

    /**
     * Parses spoken commands with Gemini 3.8 Flash structured output and falls back cleanly
     * to instant deterministic on-device parsing if offline, timed out, or unconfigured.
     */
    suspend fun parseWithGeminiAndFallback(
        transcript: String,
        referenceCalendar: Calendar = Calendar.getInstance()
    ): ParsedVoiceBookingCommand {
        val localResult = parseLocally(transcript, referenceCalendar)
        val trimmed = transcript.trim()
        if (trimmed.length < 4 || localResult.isConfirmCommand) {
            return localResult
        }

        return try {
            val dateFmt = SimpleDateFormat("yyyy-MM-dd (EEEE)", Locale.ENGLISH)
            val referenceDateLabel = dateFmt.format(referenceCalendar.time)
            val prompt = buildString {
                appendLine("You are the Voice Booking Parser for STAR ARTIST STUDIO (Talwara Khurd, Sirsa, Haryana).")
                appendLine("Reference Today Date: $referenceDateLabel")
                appendLine("User Voice Command: \"$trimmed\"")
                appendLine()
                appendLine("Map the command into a structured booking JSON:")
                appendLine("- Photography packages: \"Photography\", \"Photography (High-Fashion Editorial)\", \"Photography (Royal Pre-Wedding & Bridal)\", \"Photography (Commercial Product & Catalog)\", \"Photography (Portrait & Glamour Stills)\"")
                appendLine("- Modeling packages: \"Modeling\", \"Modeling (Portfolio & Comp-Card Shoot)\", \"Modeling (Agency Audition Profile)\", \"Modeling (Runway & Lookbook Session)\"")
                appendLine("- Video packages: \"Video Production\", \"Video Production (4K Cinema Music Video Direction)\", \"Video Production (Commercial Brand Ad Film)\", \"Video Production (Fashion Model Reel in 4K HDR)\"")
                appendLine("- Standard studio time slots: ${STANDARD_TIME_SLOTS.joinToString(", ")}")
                appendLine("- Locations: \"$LOCATION_STUDIO\", \"$LOCATION_OUTDOOR\", \"$LOCATION_HYBRID\", \"$LOCATION_CLIENT\"")
                appendLine("- Resolve relative dates (tomorrow, next Saturday, this Friday, kal, parso) relative to Reference Today Date in YYYY-MM-DD format.")
            }

            val response = withTimeout(12_000L) {
                geminiParserModel.generateContent(prompt)
            }
            val rawJson = response.text?.trim()
            if (rawJson.isNullOrBlank()) {
                return localResult
            }

            val json = JSONObject(rawJson)
            val aiCategory = json.optString("intentCategory", localResult.intentCategory)
                .takeIf { it in listOf("Photography", "Modeling", "Video Production") }
                ?: localResult.intentCategory
            val aiService = json.optString("serviceType", localResult.serviceType)
                .trim()
                .ifEmpty { localResult.serviceType }
            val aiDate = json.optString("preferredDate", localResult.preferredDate)
                .trim()
                .takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
                ?: localResult.preferredDate
            val aiTime = json.optString("preferredTime", localResult.preferredTime)
                .trim()
                .ifEmpty { localResult.preferredTime }
            val aiLocation = json.optString("location", localResult.location)
                .trim()
                .ifEmpty { localResult.location }
            val aiName = json.optString("extractedClientName", "")
                .trim()
                .ifEmpty { localResult.extractedClientName }
            val aiPhone = json.optString("extractedPhoneNumber", "")
                .filter { it.isDigit() }
                .takeIf { it.length >= 8 }
                ?: localResult.extractedPhoneNumber
            val aiNotes = json.optString("extractedNotes", "")
                .trim()
                .ifEmpty { localResult.extractedNotes }
            val aiConfirmEn = json.optString("spokenConfirmationEn", "").trim()
            val aiConfirmHi = json.optString("spokenConfirmationHi", "").trim()

            ParsedVoiceBookingCommand(
                rawTranscript = trimmed,
                isBookingIntent = json.optBoolean("isBookingIntent", localResult.isBookingIntent),
                intentCategory = aiCategory,
                serviceType = aiService,
                preferredDate = aiDate,
                preferredTime = snapToStudioSlotIfClose(aiTime),
                location = aiLocation,
                extractedClientName = aiName,
                extractedPhoneNumber = aiPhone,
                extractedNotes = aiNotes,
                confidenceSummary = "$aiCategory • $aiDate • ${snapToStudioSlotIfClose(aiTime)}",
                spokenConfirmationEn = aiConfirmEn.ifEmpty {
                    buildSpokenConfirmationEn(aiService, aiDate, snapToStudioSlotIfClose(aiTime), aiLocation)
                },
                spokenConfirmationHi = aiConfirmHi.ifEmpty {
                    buildSpokenConfirmationHi(aiService, aiDate, snapToStudioSlotIfClose(aiTime))
                },
                isConfirmCommand = json.optBoolean("isConfirmCommand", localResult.isConfirmCommand),
                parsedByEngine = "GEMINI_3_8_FLASH"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(VOICE_PARSER_TAG, "Using deterministic voice NLU fallback: ${e.message}")
            localResult
        }
    }

    /**
     * Deterministic, zero-latency bilingual NLU parser for spoken English & Hindi/Hinglish booking commands.
     */
    fun parseLocally(
        transcript: String,
        referenceCalendar: Calendar = Calendar.getInstance()
    ): ParsedVoiceBookingCommand {
        val raw = transcript.trim()
        val lower = raw.lowercase(Locale.ENGLISH)

        // Check if it's a pure confirmation utterance ("yes confirm", "book it", "confirm booking", "हाँ बुक करें")
        val confirmOnlyPatterns = listOf(
            "confirm booking", "confirm shoot", "confirm it", "yes confirm", "book it now",
            "book now", "go ahead", "submit booking", "हाँ बुक करें", "कन्फर्म करें", "बुक कर दो"
        )
        val isPureConfirm = confirmOnlyPatterns.any { lower == it || (lower.contains(it) && raw.split(" ").size <= 5) } ||
            lower == "confirm" || lower == "yes" || lower == "हाँ"

        val (category, serviceType) = detectCategoryAndPackage(lower, raw)
        val date = extractDateIso(lower, referenceCalendar)
        val time = extractTimeSlot(lower)
        val location = extractLocation(lower, raw)
        val phone = extractPhoneNumber(raw)
        val clientName = extractClientName(raw, lower)
        val notes = buildExtractedNotes(raw, serviceType, date, time)

        val confirmEn = buildSpokenConfirmationEn(serviceType, date, time, location)
        val confirmHi = buildSpokenConfirmationHi(serviceType, date, time)

        return ParsedVoiceBookingCommand(
            rawTranscript = raw,
            isBookingIntent = raw.isNotBlank(),
            intentCategory = category,
            serviceType = serviceType,
            preferredDate = date,
            preferredTime = time,
            location = location,
            extractedClientName = clientName,
            extractedPhoneNumber = phone,
            extractedNotes = notes,
            confidenceSummary = "$category • $date • $time",
            spokenConfirmationEn = confirmEn,
            spokenConfirmationHi = confirmHi,
            isConfirmCommand = isPureConfirm,
            parsedByEngine = "ON_DEVICE_NLU"
        )
    }

    private fun detectCategoryAndPackage(lower: String, raw: String): Pair<String, String> {
        val hasModeling = containsAny(
            lower,
            "model", "modeling", "modelling", "comp card", "comp-card", "compcard",
            "runway", "ramp", "audition", "agency", "lookbook", "portfolio shoot",
            "talent shoot", "मॉडल", "मॉडलिंग", "ऑडिशन", "रैंप"
        )
        val hasVideo = containsAny(
            lower,
            "video", "music video", "cinema", "4k film", "commercial ad", "ad film",
            "reel", "reels", "documentary", "drone", "gimbal", "production",
            "वीडियो", "रील", "शूटिंग वीडियो"
        )
        val hasPhotography = containsAny(
            lower,
            "photo", "photography", "photoshoot", "photo shoot", "portrait", "headshot",
            "bridal", "pre-wedding", "pre wedding", "prewedding", "wedding", "editorial",
            "fashion shoot", "product shoot", "catalog", "catalogue", "glamour", "still",
            "फोटो", "फोटोग्राफी", "फोटोशूट", "ब्राइडल", "प्री-वेडिंग"
        )

        return when {
            // Check explicit Modeling first if comp-card/runway/audition/modeling mentioned without explicit video/bridal photo
            hasModeling && !containsAny(lower, "bridal", "pre-wedding", "pre wedding", "product") -> {
                val pkg = when {
                    containsAny(lower, "audition", "agency", "casting", "ऑडिशन", "एजेंसी") ->
                        "Modeling (Agency Audition Profile)"
                    containsAny(lower, "comp card", "comp-card", "compcard", "portfolio", "पोर्टफोलियो", "कॉम्प कार्ड") ->
                        "Modeling (Portfolio & Comp-Card Shoot)"
                    containsAny(lower, "runway", "ramp", "lookbook", "रैंप", "रनवे") ->
                        "Modeling (Runway & Lookbook Session)"
                    else -> "Modeling"
                }
                "Modeling" to pkg
            }
            hasVideo && !hasPhotography -> {
                val pkg = when {
                    containsAny(lower, "music video", "song", "album", "cinema") ->
                        "Video Production (4K Cinema Music Video Direction)"
                    containsAny(lower, "commercial", "brand", "ad ", "advertisement") ->
                        "Video Production (Commercial Brand Ad Film)"
                    containsAny(lower, "reel", "instagram", "hdr") ->
                        "Video Production (Fashion Model Reel in 4K HDR)"
                    else -> "Video Production"
                }
                "Video Production" to pkg
            }
            else -> {
                val pkg = when {
                    containsAny(lower, "editorial", "high fashion", "high-fashion", "vogue", "couture") ->
                        "Photography (High-Fashion Editorial)"
                    containsAny(lower, "bridal", "pre-wedding", "pre wedding", "prewedding", "wedding", "lehenga", "ब्राइडल", "प्री-वेडिंग") ->
                        "Photography (Royal Pre-Wedding & Bridal)"
                    containsAny(lower, "product", "catalog", "catalogue", "ecommerce", "e-commerce", "jewellery", "jewelry") ->
                        "Photography (Commercial Product & Catalog)"
                    containsAny(lower, "portrait", "headshot", "glamour", "family", "studio portrait") ->
                        "Photography (Portrait & Glamour Stills)"
                    else -> "Photography"
                }
                "Photography" to pkg
            }
        }
    }

    private fun extractDateIso(lower: String, referenceCalendar: Calendar): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

        // 1. Explicit ISO yyyy-MM-dd
        Regex("""\b(20\d{2}-\d{2}-\d{2})\b""").find(lower)?.let { match ->
            return match.groupValues[1]
        }

        // 2. Numeric dd/MM/yyyy or dd-MM-yyyy
        Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](20\d{2})\b""").find(lower)?.let { match ->
            val day = match.groupValues[1].toIntOrNull()
            val month = match.groupValues[2].toIntOrNull()
            val year = match.groupValues[3].toIntOrNull()
            if (day != null && month != null && year != null && month in 1..12 && day in 1..31) {
                return String.format(Locale.ENGLISH, "%04d-%02d-%02d", year, month, day)
            }
        }

        // 3. Natural month + day ("november 25", "25th november", "dec 5")
        val months = mapOf(
            "january" to 0, "jan" to 0, "february" to 1, "feb" to 1, "march" to 2, "mar" to 2,
            "april" to 3, "apr" to 3, "may" to 4, "june" to 5, "jun" to 5,
            "july" to 6, "jul" to 6, "august" to 7, "aug" to 7, "september" to 8, "sep" to 8, "sept" to 8,
            "october" to 9, "oct" to 9, "november" to 10, "nov" to 10, "december" to 11, "dec" to 11
        )
        val monthPattern = months.keys.joinToString("|")
        Regex("""\b($monthPattern)\s+(\d{1,2})(?:st|nd|rd|th)?\b""").find(lower)?.let { match ->
            val monthIdx = months[match.groupValues[1]]
            val day = match.groupValues[2].toIntOrNull()
            if (monthIdx != null && day != null && day in 1..31) {
                return resolveUpcomingMonthDay(referenceCalendar, monthIdx, day, dateFormat)
            }
        }
        Regex("""\b(\d{1,2})(?:st|nd|rd|th)?\s+(?:of\s+)?($monthPattern)\b""").find(lower)?.let { match ->
            val day = match.groupValues[1].toIntOrNull()
            val monthIdx = months[match.groupValues[2]]
            if (monthIdx != null && day != null && day in 1..31) {
                return resolveUpcomingMonthDay(referenceCalendar, monthIdx, day, dateFormat)
            }
        }

        // 4. Relative day phrases (check "day after tomorrow" / "parso" BEFORE "tomorrow")
        val cal = (referenceCalendar.clone() as Calendar)
        when {
            containsAny(lower, "day after tomorrow", "parso", "परसों") -> {
                cal.add(Calendar.DAY_OF_YEAR, 2)
                return dateFormat.format(cal.time)
            }
            containsAny(lower, "tomorrow", "tommorow", "kal", "कल") -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                return dateFormat.format(cal.time)
            }
            containsAny(lower, "today", "aaj", "आज", "tonight") -> {
                return dateFormat.format(cal.time)
            }
            containsAny(lower, "next week", "अगले हफ्ते", "agle hafte") -> {
                cal.add(Calendar.DAY_OF_YEAR, 7)
                return dateFormat.format(cal.time)
            }
            containsAny(lower, "this weekend", "weekend", "वीकेंड") -> {
                while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                return dateFormat.format(cal.time)
            }
        }

        // 5. Day of week ("monday", "next saturday", "this friday", "शनिवार")
        val weekdays = mapOf(
            "sunday" to Calendar.SUNDAY, "रविवार" to Calendar.SUNDAY,
            "monday" to Calendar.MONDAY, "सोमवार" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY, "मंगलवार" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY, "बुधवार" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY, "गुरुवार" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY, "शुक्रवार" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY, "शनिवार" to Calendar.SATURDAY
        )
        weekdays.entries.firstOrNull { (name, _) -> lower.contains(name) }?.let { (_, targetDay) ->
            val forceNextWeek = lower.contains("next ")
            cal.add(Calendar.DAY_OF_YEAR, 1)
            while (cal.get(Calendar.DAY_OF_WEEK) != targetDay) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            if (forceNextWeek) {
                // If "next Friday" and Friday was within 3 days, push another week only if user explicitly distinguished; otherwise nearest upcoming day works intuitively
                val diffDays = ((cal.timeInMillis - referenceCalendar.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
                if (diffDays <= 2) {
                    cal.add(Calendar.DAY_OF_YEAR, 7)
                }
            }
            return dateFormat.format(cal.time)
        }

        // Default to tomorrow if user did not mention a date
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return dateFormat.format(cal.time)
    }

    private fun resolveUpcomingMonthDay(
        referenceCalendar: Calendar,
        monthIdx: Int,
        day: Int,
        dateFormat: SimpleDateFormat
    ): String {
        val candidate = (referenceCalendar.clone() as Calendar).apply {
            set(Calendar.MONTH, monthIdx)
            set(Calendar.DAY_OF_MONTH, day)
        }
        val refZeroed = (referenceCalendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (candidate.before(refZeroed)) {
            candidate.add(Calendar.YEAR, 1)
        }
        return dateFormat.format(candidate.time)
    }

    private fun extractTimeSlot(lower: String): String {
        // Explicit 12-hour clock time ("10 am", "2:30 pm", "4:30 p.m.", "6 o'clock")
        Regex("""\b(\d{1,2})(?::(\d{2}))?\s*(a\.?m\.?|p\.?m\.?)\b""").find(lower)?.let { match ->
            var hour = match.groupValues[1].toIntOrNull() ?: 10
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val period = match.groupValues[3].replace(".", "").uppercase(Locale.ENGLISH)
            val hour24 = when {
                period == "PM" && hour < 12 -> hour + 12
                period == "AM" && hour == 12 -> 0
                else -> hour
            }
            return mapHourMinuteToSlot(hour24, minute)
        }

        // Explicit 24-hour clock time ("16:30", "14:00")
        Regex("""\b(\d{1,2}):(\d{2})\b""").find(lower)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull()
            val minute = match.groupValues[2].toIntOrNull()
            if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                val effectiveHour24 = if (hour in 1..7) hour + 12 else hour
                return mapHourMinuteToSlot(effectiveHour24, minute)
            }
        }

        // Words like "at 4", "at 11", "4 baje"
        Regex("""\b(?:at|around|by|baje)\s+(\d{1,2})\b|\b(\d{1,2})\s*(?:baje|o'?clock)\b""").find(lower)?.let { match ->
            val rawHour = (match.groupValues[1].ifEmpty { match.groupValues[2] }).toIntOrNull()
            if (rawHour != null && rawHour in 1..12) {
                val effectiveHour24 = when {
                    rawHour in 1..8 -> rawHour + 12
                    else -> rawHour
                }
                return mapHourMinuteToSlot(effectiveHour24, 0)
            }
        }

        // Time-of-day cues
        return when {
            containsAny(lower, "golden hour", "sunset", "evening", "shaam", "शाम") ->
                "04:30 PM (Golden Hour Outdoor)"
            containsAny(lower, "night", "dark", "rim light", "रात") ->
                "06:30 PM (Dark & Rim Mood Studio)"
            containsAny(lower, "afternoon", "post lunch", "दोपहर बाद") ->
                "02:30 PM (Editorial Softbox Bay)"
            containsAny(lower, "noon", "midday", "lunch", "दोपहर") ->
                "12:00 PM (Midday High-Key Flash)"
            containsAny(lower, "morning", "subah", "सुबह") ->
                "10:00 AM (Morning Key Light Bay)"
            else -> "10:00 AM (Morning Key Light Bay)"
        }
    }

    private fun mapHourMinuteToSlot(hour24: Int, minute: Int): String {
        val totalMins = hour24 * 60 + minute
        return when {
            totalMins in (9 * 60 + 30)..(10 * 60 + 45) -> "10:00 AM (Morning Key Light Bay)"
            totalMins in (11 * 60 + 30)..(12 * 60 + 45) -> "12:00 PM (Midday High-Key Flash)"
            totalMins in (14 * 60)..(15 * 60 + 15) -> "02:30 PM (Editorial Softbox Bay)"
            totalMins in (16 * 60)..(17 * 60 + 15) -> "04:30 PM (Golden Hour Outdoor)"
            totalMins in (18 * 60)..(19 * 60 + 15) -> "06:30 PM (Dark & Rim Mood Studio)"
            else -> {
                val h12 = when {
                    hour24 == 0 -> 12
                    hour24 > 12 -> hour24 - 12
                    else -> hour24
                }
                val amPm = if (hour24 >= 12) "PM" else "AM"
                String.format(Locale.ENGLISH, "%02d:%02d %s", h12, minute, amPm)
            }
        }
    }

    private fun snapToStudioSlotIfClose(timeStr: String): String {
        val upper = timeStr.uppercase(Locale.ENGLISH)
        return when {
            upper.startsWith("10:00 AM") -> STANDARD_TIME_SLOTS[0]
            upper.startsWith("12:00 PM") -> STANDARD_TIME_SLOTS[1]
            upper.startsWith("02:30 PM") || upper.startsWith("2:30 PM") -> STANDARD_TIME_SLOTS[2]
            upper.startsWith("04:30 PM") || upper.startsWith("4:30 PM") -> STANDARD_TIME_SLOTS[3]
            upper.startsWith("06:30 PM") || upper.startsWith("6:30 PM") -> STANDARD_TIME_SLOTS[4]
            else -> timeStr
        }
    }

    private fun extractLocation(lower: String, raw: String): String {
        return when {
            containsAny(lower, "hybrid", "both studio and outdoor", "indoor and outdoor") ->
                LOCATION_HYBRID
            containsAny(lower, "outdoor", "sirsa", "ellenabad", "heritage", "garden", "park", "location shoot", "बाहर", "आउटडोर") ->
                LOCATION_OUTDOOR
            containsAny(lower, "my home", "my venue", "client location", "custom set", "on-site", "on site", "hotel", "resort", "venue", "घर पर") ->
                LOCATION_CLIENT
            else -> LOCATION_STUDIO
        }
    }

    private fun extractPhoneNumber(raw: String): String {
        val compact = raw.replace(Regex("[\\s-]+"), "")
        Regex("""(?:\+?91)?([6-9]\d{9})""").find(compact)?.let {
            return it.groupValues[1]
        }
        Regex("""(\d{10,12})""").find(compact)?.let {
            val digits = it.groupValues[1]
            return if (digits.length == 12 && digits.startsWith("91")) digits.takeLast(10) else digits
        }
        return ""
    }

    private fun extractClientName(raw: String, lower: String): String {
        val namePatterns = listOf(
            Regex("""(?:my name is|this is|i am|i'm|booking for|name is)\s+([A-Z][a-z]+(?:\s+[A-Z][a-z]+)?)"""),
            Regex("""(?:my name is|this is|i am|i'm|name is)\s+([a-z]{2,15}(?:\s+[a-z]{2,15})?)""", RegexOption.IGNORE_CASE)
        )
        for (regex in namePatterns) {
            regex.find(raw)?.let { match ->
                val candidate = match.groupValues[1].trim()
                val candidateLower = candidate.lowercase(Locale.ENGLISH)
                val stopWords = setOf("a", "an", "the", "photography", "modeling", "video", "shoot", "session", "tomorrow", "today")
                val cleaned = candidate.split(" ")
                    .takeWhile { it.lowercase(Locale.ENGLISH) !in stopWords }
                    .joinToString(" ")
                    .trim()
                if (cleaned.length >= 2 && cleaned.lowercase(Locale.ENGLISH) !in stopWords && !candidateLower.startsWith("booking")) {
                    return cleaned.split(" ").joinToString(" ") { word ->
                        word.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase(Locale.ENGLISH) else ch.toString() }
                    }
                }
            }
        }
        return ""
    }

    private fun buildExtractedNotes(
        raw: String,
        serviceType: String,
        date: String,
        time: String
    ): String {
        return "Voice Command: \"$raw\""
    }

    private fun buildSpokenConfirmationEn(
        serviceType: String,
        date: String,
        time: String,
        location: String
    ): String {
        val cleanLoc = location.substringBefore(" (")
        val cleanTime = time.substringBefore(" (")
        return "Ready to book $serviceType on $date at $cleanTime ($cleanLoc). Review and tap Confirm to reserve."
    }

    private fun buildSpokenConfirmationHi(
        serviceType: String,
        date: String,
        time: String
    ): String {
        val cleanTime = time.substringBefore(" (")
        return "$date को $cleanTime पर $serviceType सत्र बुक करने के लिए तैयार है। विवरण जांचकर कन्फर्म करें।"
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }
}
