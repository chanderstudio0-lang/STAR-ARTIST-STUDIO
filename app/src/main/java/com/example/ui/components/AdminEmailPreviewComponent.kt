package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.ModelRegistration
import com.example.data.model.ShootBooking
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioElevatedSurface
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.SuccessGreen
import com.example.util.StudioContactDetails

enum class EmailPreviewType {
    BOOKING,
    MODEL_APPLICATION,
    COMPANY_REQUEST
}

data class EmailHighlightCard(
    val label: String,
    val value: String,
    val sub: String = "",
    val highlightPositive: Boolean = false,
    val highlightError: Boolean = false
)

data class EmailDetailRow(
    val label: String,
    val value: String
)

data class EmailPreviewPayload(
    val type: EmailPreviewType,
    val from: String,
    val to: String,
    val subject: String,
    val preheader: String,
    val eyebrow: String,
    val heading: String,
    val intro: String,
    val statusBadge: String,
    val isPositiveBadge: Boolean,
    val highlights: List<EmailHighlightCard>,
    val detailRows: List<EmailDetailRow>,
    val notesTitle: String,
    val notesContent: String,
    val ctaLabel: String,
    val secondaryActionLabel: String?,
    val plainText: String,
    val htmlDocument: String,
    val isSample: Boolean,
    val recordSummary: String
)

/**
 * Pure Kotlin builder matching the Cloud Functions branded Black & Gold HTML email alerts
 * for Shoot Bookings, 18+ Model Applications, and Company Model Requirement Requests.
 */
object AdminEmailPreviewBuilder {
    private const val STUDIO_NAME = "STAR ARTIST STUDIO"
    private const val STUDIO_TAGLINE = "Luxury Fashion • Modeling • Bridal & Editorial Production"
    private const val STUDIO_LOCATION = "Talwara Khurd, Sirsa, Haryana - 125102"
    private const val DASHBOARD_URL = "https://gen-lang-client-0548683163.web.app/#admin"
    private const val FOOTER_TEXT =
        "Automated Admin Alert • Restricted strictly to the authorized Studio Administrator."

    fun escapeHtml(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    fun sanitizeHeader(input: String, maxLen: Int = 140): String = input
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
        .take(maxLen)

    fun buildBookingPreview(booking: ShootBooking?): EmailPreviewPayload {
        val isSample = booking == null
        val id = booking?.id?.ifBlank { "bk_preview_01" } ?: "bk_preview_sample_01"
        val clientName = sanitizeHeader(booking?.clientName?.ifBlank { "Simranpreet Kaur" } ?: "Simranpreet Kaur", 80)
        val serviceType = sanitizeHeader(
            booking?.serviceType?.ifBlank { "High-Fashion Editorial — Pro Package" }
                ?: "High-Fashion Editorial — Pro Package",
            80
        )
        val preferredDate = booking?.preferredDate?.ifBlank { "2026-12-14" } ?: "2026-12-14"
        val preferredTime = booking?.preferredTime?.ifBlank { "11:00 AM" } ?: "11:00 AM"
        val contactNumber = booking?.contactNumber?.ifBlank { "+91 9812890081" } ?: "+91 9812890081"
        val location = booking?.location?.ifBlank { "Studio (Talwara Khurd, Sirsa)" } ?: "Studio (Talwara Khurd, Sirsa)"
        val status = booking?.status?.ifBlank { "Pending" } ?: "Pending"
        val notes = booking?.notes?.trim()?.ifBlank {
            if (isSample) "3 couture wardrobe changes, golden rim lighting, and 4K behind-the-scenes portrait reel." else ""
        } ?: ""

        val subject = sanitizeHeader("[New Booking] $clientName — $serviceType ($preferredDate)", 160)
        val preheader = "New booking from $clientName for $serviceType on $preferredDate."
        val intro =
            "$clientName submitted a new booking request for $serviceType. Open the Admin Console with your verified Studio Admin Gmail account to confirm or manage this session."

        val rows = listOf(
            EmailDetailRow("Booking ID", id),
            EmailDetailRow("Client Name", clientName),
            EmailDetailRow("Contact Number", contactNumber),
            EmailDetailRow("Service Package", serviceType),
            EmailDetailRow("Preferred Date", preferredDate),
            EmailDetailRow("Preferred Time", preferredTime),
            EmailDetailRow("Shoot Location", location),
            EmailDetailRow("Status", status),
            EmailDetailRow("Client Notes", notes.ifBlank { "None" })
        )

        val highlights = listOf(
            EmailHighlightCard("Event Date", preferredDate, preferredTime),
            EmailHighlightCard("Package", serviceType),
            EmailHighlightCard("Client", clientName)
        )

        val plainText = buildString {
            appendLine("$STUDIO_NAME — New Shoot Booking Request")
            appendLine()
            rows.forEach { appendLine("${it.label}: ${it.value}") }
            appendLine()
            append("Review in Admin Dashboard: $DASHBOARD_URL")
        }

        val html = renderHtmlDocument(
            subject = subject,
            preheader = preheader,
            eyebrow = "$STUDIO_NAME • BOOKING ALERT",
            heading = "New Shoot Booking Request",
            intro = intro,
            statusBadge = status.uppercase(),
            highlights = highlights,
            rows = rows,
            notesTitle = "Client Notes & Shoot Brief",
            notesContent = notes,
            ctaLabel = "Open Admin Bookings"
        )

        return EmailPreviewPayload(
            type = EmailPreviewType.BOOKING,
            from = "$STUDIO_NAME Alerts <${StudioContactDetails.ADMIN_EMAIL}>",
            to = StudioContactDetails.ADMIN_EMAIL,
            subject = subject,
            preheader = preheader,
            eyebrow = "$STUDIO_NAME • BOOKING ALERT",
            heading = "New Shoot Booking Request",
            intro = intro,
            statusBadge = status.uppercase(),
            isPositiveBadge = status.equals("Confirmed", true) || status.equals("Completed", true),
            highlights = highlights,
            detailRows = rows,
            notesTitle = "Client Notes & Shoot Brief",
            notesContent = notes,
            ctaLabel = "Open Admin Bookings",
            secondaryActionLabel = "Call Client",
            plainText = plainText,
            htmlDocument = html,
            isSample = isSample,
            recordSummary = "$clientName — $serviceType"
        )
    }

    fun buildModelApplicationPreview(model: ModelRegistration?): EmailPreviewPayload {
        val isSample = model == null
        val id = model?.id?.ifBlank { "mdl_preview_01" } ?: "mdl_preview_sample_01"
        val fullName = sanitizeHeader(model?.fullName?.ifBlank { "Aarav Sharma" } ?: "Aarav Sharma", 80)
        val age = model?.age ?: 22L
        val isAdult = age >= 18L
        val ageLabel = "$age yrs (${if (isAdult) "18+ Eligible" else "Under 18 Flag"})"
        val gender = model?.gender?.ifBlank { "Male" } ?: "Male"
        val city = sanitizeHeader(model?.city?.ifBlank { "Sirsa, Haryana" } ?: "Sirsa, Haryana", 80)
        val phoneNumber = model?.phoneNumber?.ifBlank { "+91 9812890081" } ?: "+91 9812890081"
        val skills = model?.skills?.trim()?.ifBlank {
            if (isSample) "Editorial Print, Ramp Walk, Ethnic & Bridal Couture, Commercial Music Video" else "Not specified"
        } ?: "Not specified"
        val portfolioUrl = model?.portfolioUrls?.trim()?.ifBlank {
            if (isSample) "https://gen-lang-client-0548683163.web.app/#gallery" else "None provided"
        } ?: "None provided"
        val consentGiven = model?.consentGiven ?: true
        val consentText = if (consentGiven) "Signed & Confirmed (18+)" else "Missing Consent"
        val status = model?.status?.ifBlank { "pending" } ?: "pending"

        val subject = sanitizeHeader("[New 18+ Model Application] $fullName — $city", 160)
        val preheader = "New 18+ model application from $fullName ($city) — $ageLabel."
        val intro =
            "$fullName from $city applied for the Official Studio Roster. Open the Admin Console to review their 18+ verification checklist and portfolio."
        val verified = isAdult && consentGiven
        val statusBadge = if (verified) "18+ VERIFIED" else "REVIEW REQUIRED"

        val rows = listOf(
            EmailDetailRow("Application ID", id),
            EmailDetailRow("Applicant Name", fullName),
            EmailDetailRow("Age Verification", ageLabel),
            EmailDetailRow("Gender", gender),
            EmailDetailRow("City", city),
            EmailDetailRow("Phone Number", phoneNumber),
            EmailDetailRow("Skills & Experience", skills),
            EmailDetailRow("Portfolio Link", portfolioUrl),
            EmailDetailRow("18+ Consent", consentText),
            EmailDetailRow("Status", status)
        )

        val highlights = listOf(
            EmailHighlightCard("Applicant", fullName, city),
            EmailHighlightCard("18+ Check", ageLabel, highlightPositive = isAdult, highlightError = !isAdult),
            EmailHighlightCard("Consent", consentText, highlightPositive = consentGiven, highlightError = !consentGiven)
        )

        val plainText = buildString {
            appendLine("$STUDIO_NAME — New 18+ Model Application")
            appendLine()
            rows.forEach { appendLine("${it.label}: ${it.value}") }
            appendLine()
            append("Verify in Admin Dashboard: $DASHBOARD_URL")
        }

        val html = renderHtmlDocument(
            subject = subject,
            preheader = preheader,
            eyebrow = "$STUDIO_NAME • 18+ CASTING ALERT",
            heading = "New 18+ Model Application",
            intro = intro,
            statusBadge = statusBadge,
            highlights = highlights,
            rows = rows,
            notesTitle = "Model Skills & Roster Notes",
            notesContent = if (skills != "Not specified") skills else "",
            ctaLabel = "Review Model Application"
        )

        return EmailPreviewPayload(
            type = EmailPreviewType.MODEL_APPLICATION,
            from = "$STUDIO_NAME Alerts <${StudioContactDetails.ADMIN_EMAIL}>",
            to = StudioContactDetails.ADMIN_EMAIL,
            subject = subject,
            preheader = preheader,
            eyebrow = "$STUDIO_NAME • 18+ CASTING ALERT",
            heading = "New 18+ Model Application",
            intro = intro,
            statusBadge = statusBadge,
            isPositiveBadge = verified,
            highlights = highlights,
            detailRows = rows,
            notesTitle = "Model Skills & Roster Notes",
            notesContent = if (skills != "Not specified") skills else "",
            ctaLabel = "Review Model Application",
            secondaryActionLabel = if (portfolioUrl.startsWith("http")) "View Portfolio" else null,
            plainText = plainText,
            htmlDocument = html,
            isSample = isSample,
            recordSummary = "$fullName ($city)"
        )
    }

    fun buildCompanyRequestPreview(
        companyName: String = "Royal Heritage Couture Events",
        organizationType: String = "Event Organizer",
        contactPerson: String = "Vikram Malhotra",
        mobileNumber: String = "+91 9812890081",
        email: String = "casting@royalheritageevents.in",
        address: String = "Hissar Road Commercial Complex, Sirsa, Haryana",
        modelRequirement: String = "Both (Male & Female)",
        modelsCount: Int = 6,
        eventType: String = "Fashion Show",
        eventDate: String = "2026-12-22",
        eventLocation: String = "Grand Courtyard Banquet, Sirsa",
        budget: String = "₹85,000",
        requestedModels: String = "Ananya Verma, Kabir Gill",
        detailedRequirements: String =
            "6 verified 18+ runway models (3 female, 3 male) for Winter Bridal Couture Showcase. Call time 4:00 PM; styling & refreshments included.",
        status: String = "Pending"
    ): EmailPreviewPayload {
        val cleanCompany = sanitizeHeader(companyName, 100)
        val cleanEvent = sanitizeHeader(eventType, 60)
        val safeCount = modelsCount.coerceAtLeast(1)
        val subject = sanitizeHeader(
            "[Company Booking] $cleanCompany — $safeCount $modelRequirement ($eventDate)",
            160
        )
        val preheader = "$cleanCompany requested $safeCount $modelRequirement for $cleanEvent ($eventDate)."
        val intro =
            "$cleanCompany submitted a model requirement booking for $cleanEvent ($safeCount • $modelRequirement). Review and coordinate directly from the Admin Console."
        val statusBadge = "$safeCount MODELS • ${status.uppercase()}"

        val rows = buildList {
            add(EmailDetailRow("Request ID", "cmp_preview_sample_01"))
            add(EmailDetailRow("Company / Organizer", "$cleanCompany ($organizationType)"))
            add(EmailDetailRow("Contact Person", contactPerson))
            add(EmailDetailRow("Mobile Number", mobileNumber))
            add(EmailDetailRow("Business Email", email))
            add(EmailDetailRow("Company Address", address))
            add(EmailDetailRow("Requirement", "$safeCount × $modelRequirement"))
            add(EmailDetailRow("Event Type", cleanEvent))
            add(EmailDetailRow("Event Date", eventDate))
            add(EmailDetailRow("Event Location", eventLocation))
            add(EmailDetailRow("Estimated Budget", budget))
            if (requestedModels.isNotBlank()) {
                add(EmailDetailRow("Requested Roster Models", requestedModels))
            }
            add(EmailDetailRow("Status", status))
        }

        val highlights = listOf(
            EmailHighlightCard("Organization", cleanCompany, cleanEvent),
            EmailHighlightCard("Models Needed", "$safeCount • $modelRequirement"),
            EmailHighlightCard("Date & Budget", eventDate, budget)
        )

        val plainText = buildString {
            appendLine("$STUDIO_NAME — New Company Model Requirement Booking")
            appendLine()
            rows.forEach { appendLine("${it.label}: ${it.value}") }
            if (detailedRequirements.isNotBlank()) {
                appendLine()
                appendLine("Detailed Requirements: $detailedRequirements")
            }
            appendLine()
            append("Manage in Admin Dashboard: $DASHBOARD_URL")
        }

        val html = renderHtmlDocument(
            subject = subject,
            preheader = preheader,
            eyebrow = "$STUDIO_NAME • CORPORATE BOOKING ALERT",
            heading = "New Company Model Requirement",
            intro = intro,
            statusBadge = statusBadge,
            highlights = highlights,
            rows = rows,
            notesTitle = "Detailed Event & Model Requirements",
            notesContent = detailedRequirements,
            ctaLabel = "Manage Company Requests"
        )

        return EmailPreviewPayload(
            type = EmailPreviewType.COMPANY_REQUEST,
            from = "$STUDIO_NAME Alerts <${StudioContactDetails.ADMIN_EMAIL}>",
            to = StudioContactDetails.ADMIN_EMAIL,
            subject = subject,
            preheader = preheader,
            eyebrow = "$STUDIO_NAME • CORPORATE BOOKING ALERT",
            heading = "New Company Model Requirement",
            intro = intro,
            statusBadge = statusBadge,
            isPositiveBadge = status.equals("Approved", true),
            highlights = highlights,
            detailRows = rows,
            notesTitle = "Detailed Event & Model Requirements",
            notesContent = detailedRequirements,
            ctaLabel = "Manage Company Requests",
            secondaryActionLabel = "Call Organizer",
            plainText = plainText,
            htmlDocument = html,
            isSample = true,
            recordSummary = "$cleanCompany — $cleanEvent"
        )
    }

    private fun renderHtmlDocument(
        subject: String,
        preheader: String,
        eyebrow: String,
        heading: String,
        intro: String,
        statusBadge: String,
        highlights: List<EmailHighlightCard>,
        rows: List<EmailDetailRow>,
        notesTitle: String,
        notesContent: String,
        ctaLabel: String
    ): String {
        val highlightCells = highlights.joinToString("") { h ->
            val subHtml = if (h.sub.isNotBlank()) {
                "<div style=\"font-size:11px;color:#C9C2B5;margin-top:2px;\">${escapeHtml(h.sub)}</div>"
            } else ""
            "<td width=\"33.33%\" valign=\"top\" style=\"padding:0 4px;\">" +
                "<div style=\"background:#0F0E0C;border:1px solid #2e2718;border-radius:12px;padding:10px;\">" +
                "<div style=\"font-size:10px;font-weight:700;color:#D4AF37;text-transform:uppercase;\">${escapeHtml(h.label)}</div>" +
                "<div style=\"font-size:13px;font-weight:800;color:#F5F5F4;margin-top:4px;\">${escapeHtml(h.value)}</div>" +
                subHtml +
                "</div></td>"
        }

        val rowHtml = rows.joinToString("") { r ->
            "<tr><td style=\"padding:10px 14px;border-bottom:1px solid #262115;color:#C9C2B5;font-size:12px;font-weight:700;width:36%;\">${escapeHtml(r.label)}</td>" +
                "<td style=\"padding:10px 14px;border-bottom:1px solid #262115;color:#F5F5F4;font-size:13px;\">${escapeHtml(r.value)}</td></tr>"
        }

        val notesHtml = if (notesContent.isNotBlank()) {
            "<div style=\"margin-top:14px;padding:12px 14px;border-radius:10px;background:#18140c;border:1px solid #352b14;border-left:3px solid #D4AF37;\">" +
                "<div style=\"font-size:11px;font-weight:800;color:#D4AF37;text-transform:uppercase;margin-bottom:4px;\">${escapeHtml(notesTitle)}</div>" +
                "<div style=\"font-size:13px;color:#F5F5F4;line-height:1.5;\">${escapeHtml(notesContent)}</div></div>"
        } else ""

        return """<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8" /><title>${escapeHtml(subject)}</title></head>
<body style="margin:0;padding:24px 12px;background:#0C0C0C;color:#F5F5F4;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
  <div style="display:none;max-height:0;overflow:hidden;">${escapeHtml(preheader)}</div>
  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:620px;margin:0 auto;background:#151412;border:1px solid #D4AF37;border-radius:16px;overflow:hidden;">
    <tr><td style="height:4px;background:#D4AF37;"></td></tr>
    <tr><td style="padding:22px 24px;border-bottom:1px solid #302714;">
      <div style="font-size:11px;font-weight:800;color:#D4AF37;letter-spacing:0.12em;">${escapeHtml(eyebrow)} • ${escapeHtml(statusBadge)}</div>
      <h1 style="margin:6px 0;font-size:22px;color:#F3D87C;">${escapeHtml(heading)}</h1>
      <p style="margin:0;font-size:13.5px;color:#C9C2B5;line-height:1.55;">${escapeHtml(intro)}</p>
    </td></tr>
    <tr><td style="padding:20px 24px;">
      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin-bottom:14px;"><tr>$highlightCells</tr></table>
      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#0F0E0C;border:1px solid #2e2718;border-radius:12px;">$rowHtml</table>
      $notesHtml
      <div style="text-align:center;margin-top:20px;">
        <a href="$DASHBOARD_URL" style="display:inline-block;padding:12px 24px;border-radius:999px;background:#D4AF37;color:#141002;font-weight:800;text-decoration:none;">${escapeHtml(ctaLabel)}</a>
      </div>
    </td></tr>
    <tr><td style="padding:14px 24px;background:#100f0d;border-top:1px solid #282216;text-align:center;">
      <div style="font-size:12px;font-weight:700;color:#D4AF37;">$STUDIO_NAME</div>
      <div style="font-size:11px;color:#C9C2B5;">$STUDIO_LOCATION • $STUDIO_TAGLINE</div>
      <div style="font-size:10.5px;color:#8a8172;margin-top:4px;">$FOOTER_TEXT</div>
    </td></tr>
  </table>
</body>
</html>"""
    }
}

@Composable
fun AdminEmailPreviewSection(
    bookings: List<ShootBooking>,
    models: List<ModelRegistration>,
    language: AppLanguage
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val isHindi = language == AppLanguage.HINDI

    var selectedType by remember { mutableStateOf(EmailPreviewType.BOOKING) }
    var showPlainText by remember { mutableStateOf(false) }
    var selectedRecordIndex by remember { mutableIntStateOf(0) } // 0 = Sample, 1..N = Live record

    val liveCount = when (selectedType) {
        EmailPreviewType.BOOKING -> bookings.size
        EmailPreviewType.MODEL_APPLICATION -> models.size
        EmailPreviewType.COMPANY_REQUEST -> 0
    }
    val safeIndex = if (selectedRecordIndex in 0..liveCount) selectedRecordIndex else 0

    val preview = remember(selectedType, safeIndex, bookings, models) {
        when (selectedType) {
            EmailPreviewType.BOOKING ->
                AdminEmailPreviewBuilder.buildBookingPreview(
                    if (safeIndex > 0) bookings.getOrNull(safeIndex - 1) else null
                )
            EmailPreviewType.MODEL_APPLICATION ->
                AdminEmailPreviewBuilder.buildModelApplicationPreview(
                    if (safeIndex > 0) models.getOrNull(safeIndex - 1) else null
                )
            EmailPreviewType.COMPANY_REQUEST ->
                AdminEmailPreviewBuilder.buildCompanyRequestPreview()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("admin_email_preview_section"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Automated Email Notification System Controls (synced with studioConfig/emailAlerts)
        item {
            AdminAutomatedEmailAlertsCard(
                pendingBookingsCount = bookings.count { it.status.equals("Pending", ignoreCase = true) },
                pendingModelsCount = models.count { it.status.equals("pending", ignoreCase = true) },
                isHindi = isHindi
            )
        }

        // Header Banner & Safe Preview Badge
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Email Preview Mode",
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "ईमेल नोटिफिकेशन प्रीव्यू" else "Notification Email Preview",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(SuccessGreen.copy(alpha = 0.16f))
                                .border(1.dp, SuccessGreen.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isHindi) "सुरक्षित प्रीव्यू" else "SAFE PREVIEW",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF6EE7B7)
                            )
                        }
                    }
                    Text(
                        text = if (isHindi)
                            "बुकिंग, 18+ मॉडल आवेदन और कंपनी अनुरोध के लिए भेजे जाने वाले ब्रांडेड HTML ईमेल को भेजने से पहले देखें।"
                        else
                            "Inspect the exact branded Black & Gold email alerts for bookings, 18+ model applications, and company requests before delivery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC9C2B5)
                    )
                }
            }
        }

        // Template Type Selector Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == EmailPreviewType.BOOKING,
                    onClick = {
                        selectedType = EmailPreviewType.BOOKING
                        selectedRecordIndex = 0
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("email_preview_type_booking"),
                    label = { Text(if (isHindi) "शूट बुकिंग" else "Booking Alert", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = Color.Black,
                        selectedLeadingIconColor = Color.Black
                    )
                )
                FilterChip(
                    selected = selectedType == EmailPreviewType.MODEL_APPLICATION,
                    onClick = {
                        selectedType = EmailPreviewType.MODEL_APPLICATION
                        selectedRecordIndex = 0
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("email_preview_type_model"),
                    label = { Text(if (isHindi) "18+ मॉडल आवेदन" else "Model 18+ Alert", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = Color.Black,
                        selectedLeadingIconColor = Color.Black
                    )
                )
                FilterChip(
                    selected = selectedType == EmailPreviewType.COMPANY_REQUEST,
                    onClick = {
                        selectedType = EmailPreviewType.COMPANY_REQUEST
                        selectedRecordIndex = 0
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("email_preview_type_company"),
                    label = { Text(if (isHindi) "कंपनी अनुरोध" else "Company Request", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = Color.Black,
                        selectedLeadingIconColor = Color.Black
                    )
                )
            }
        }

        // Data Source & Format Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showPlainText = !showPlainText },
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("email_preview_toggle_format")
                ) {
                    Text(
                        text = if (showPlainText) {
                            if (isHindi) "ब्रांडेड HTML देखें" else "Show Branded HTML"
                        } else {
                            if (isHindi) "प्लेन टेक्स्ट देखें" else "Show Plain Text"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (liveCount > 0) {
                    OutlinedButton(
                        onClick = { selectedRecordIndex = (safeIndex + 1) % (liveCount + 1) },
                        border = BorderStroke(1.dp, StudioOutline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("email_preview_cycle_record")
                    ) {
                        Text(
                            text = if (safeIndex == 0) {
                                if (isHindi) "सैंपल डेटा • बदलें" else "Sample • Use Live ($liveCount)"
                            } else {
                                if (isHindi) "लाइव #$safeIndex/$liveCount • अगला" else "Live #$safeIndex/$liveCount • Next"
                            },
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Email Envelope Metadata + Copy Buttons
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioElevatedSurface),
                border = BorderStroke(1.dp, StudioOutline),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EnvelopeMetaRow("FROM", preview.from)
                    EnvelopeMetaRow("TO", preview.to)
                    EnvelopeMetaRow("SUBJECT", preview.subject, valueColor = GoldLight, bold = true)
                    EnvelopeMetaRow("SNIPPET", preview.preheader, valueColor = Color(0xFFB5AEA0))
                    HorizontalDivider(color = StudioOutline, modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(preview.subject))
                                Toast.makeText(
                                    context,
                                    if (isHindi) "ईमेल विषय कॉपी किया गया" else "Email subject copied",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("email_preview_copy_subject")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isHindi) "विषय कॉपी" else "Copy Subject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(preview.htmlDocument))
                                Toast.makeText(
                                    context,
                                    if (isHindi) "HTML टेम्पलेट कॉपी किया गया" else "Branded HTML copied",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("email_preview_copy_html")
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isHindi) "HTML कॉपी" else "Copy HTML", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Rendered Branded Email Card OR Plain Text Fallback
        item {
            if (showPlainText) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0A09)),
                    border = BorderStroke(1.dp, StudioOutline),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_preview_plain_text_card")
                ) {
                    Text(
                        text = preview.plainText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFF5F5F4),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                BrandedEmailVisualCard(preview = preview)
            }
        }
    }
}

@Composable
private fun EnvelopeMetaRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFFF5F5F4),
    bold: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GoldPrimary,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BrandedEmailVisualCard(preview: EmailPreviewPayload) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151412)),
        border = BorderStroke(1.dp, GoldPrimary),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("email_preview_branded_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Gold Accent Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Brush.horizontalGradient(listOf(GoldPrimary, GoldLight, GoldPrimary)))
            )

            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF211B0F), Color(0xFF151412))))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFF261F0E))
                                .border(1.dp, GoldPrimary, RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("SA", color = GoldLight, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "STAR ARTIST STUDIO",
                                color = GoldLight,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "Luxury Fashion • Modeling • 4K Cinema",
                                color = Color(0xFFC9C2B5),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (preview.isPositiveBadge) Color(0xFF0F291E) else Color(0xFF2A2210))
                            .border(
                                1.dp,
                                if (preview.isPositiveBadge) Color(0xFF34D399) else GoldPrimary,
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = preview.statusBadge,
                            color = if (preview.isPositiveBadge) Color(0xFF6EE7B7) else GoldLight,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Text(
                    text = preview.eyebrow,
                    color = GoldPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = preview.heading,
                    color = GoldLight,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = preview.intro,
                    color = Color(0xFFC9C2B5),
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }

            HorizontalDivider(color = Color(0xFF302714))

            // Body Content
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 3-Column KPI Highlights
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    preview.highlights.forEach { card ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F0E0C))
                                .border(1.dp, Color(0xFF2E2718), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = card.label.uppercase(),
                                color = GoldPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = card.value,
                                color = when {
                                    card.highlightError -> Color(0xFFFCA5A5)
                                    card.highlightPositive -> Color(0xFF6EE7B7)
                                    else -> Color(0xFFF5F5F4)
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (card.sub.isNotBlank()) {
                                Text(
                                    text = card.sub,
                                    color = Color(0xFFC9C2B5),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Specification Table
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F0E0C))
                        .border(1.dp, Color(0xFF2E2718), RoundedCornerShape(12.dp))
                ) {
                    preview.detailRows.forEachIndexed { idx, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (idx % 2 == 1) Color(0xFF13110E) else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = row.label,
                                color = Color(0xFFC9C2B5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(0.38f)
                            )
                            Text(
                                text = row.value,
                                color = Color(0xFFF5F5F4),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(0.62f)
                            )
                        }
                        if (idx < preview.detailRows.lastIndex) {
                            HorizontalDivider(color = Color(0xFF262115))
                        }
                    }
                }

                // Optional Highlighted Notes Callout
                if (preview.notesContent.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF18140C))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = preview.notesTitle.uppercase(),
                            color = GoldPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = preview.notesContent,
                            color = Color(0xFFF5F5F4),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                // CTA Preview Row (non-destructive visual button preview)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(GoldPrimary)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = preview.ctaLabel,
                            color = Color(0xFF141002),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    if (!preview.secondaryActionLabel.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .border(1.dp, GoldPrimary, RoundedCornerShape(999.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = preview.secondaryActionLabel,
                                color = GoldLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Branded Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF100F0D))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "STAR ARTIST STUDIO",
                    color = GoldPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Talwara Khurd, Sirsa, Haryana - 125102",
                    color = Color(0xFFC9C2B5),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Automated Admin Alert • Restricted strictly to the authorized Studio Administrator.",
                    color = Color(0xFF8A8172),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AdminAutomatedEmailAlertsCard(
    pendingBookingsCount: Int,
    pendingModelsCount: Int,
    isHindi: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lockedRecipient = "chanderstudio0@gmail.com"
    var bookingAlertsEnabled by remember { mutableStateOf(true) }
    var modelAlertsEnabled by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    val firestore = remember(context) {
        try {
            FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
        } catch (_: Exception) {
            null
        }
    }

    LaunchedEffect(firestore) {
        if (firestore == null) return@LaunchedEffect
        try {
            val snap = firestore.collection("studioConfig").document("emailAlerts").get().await()
            if (snap.exists()) {
                bookingAlertsEnabled = snap.getBoolean("bookingAlertsEnabled") ?: true
                modelAlertsEnabled = snap.getBoolean("modelAlertsEnabled") ?: true
            }
        } catch (_: Exception) {
            // Keep defaults when offline or uninitialized
        }
    }

    fun persistSettings(nextBooking: Boolean, nextModel: Boolean) {
        bookingAlertsEnabled = nextBooking
        modelAlertsEnabled = nextModel
        if (firestore == null) return
        scope.launch {
            isSaving = true
            try {
                firestore.collection("studioConfig").document("emailAlerts").set(
                    mapOf(
                        "bookingAlertsEnabled" to nextBooking,
                        "modelAlertsEnabled" to nextModel,
                        "companyAlertsEnabled" to true,
                        "browserAlertsEnabled" to true,
                        "recipientEmail" to lockedRecipient,
                        "updatedByEmail" to lockedRecipient,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Toast.makeText(
                    context,
                    if (isHindi) "ईमेल अलर्ट सेटिंग्स सहेजी गईं" else "Email alert settings saved",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    if (isHindi) "सेटिंग्स सहेजने में त्रुटि" else "Unable to save alert settings",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isSaving = false
            }
        }
    }

    val isActive = bookingAlertsEnabled || modelAlertsEnabled

    Card(
        colors = CardDefaults.cardColors(containerColor = StudioCardSurface),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_auto_email_alerts_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = if (isHindi) "स्वचालित ईमेल अलर्ट" else "Automated Email Alerts",
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "स्वचालित ईमेल अलर्ट" else "Automated Email Alerts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isActive) SuccessGreen.copy(alpha = 0.16f)
                            else Color(0xFFF59E0B).copy(alpha = 0.16f)
                        )
                        .border(
                            1.dp,
                            if (isActive) SuccessGreen.copy(alpha = 0.55f)
                            else Color(0xFFF59E0B).copy(alpha = 0.55f),
                            RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isActive) {
                            if (isHindi) "सक्रिय" else "ACTIVE"
                        } else {
                            if (isHindi) "रुका हुआ" else "PAUSED"
                        },
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isActive) Color(0xFF6EE7B7) else Color(0xFFFCD34D)
                    )
                }
            }

            Text(
                text = if (isHindi)
                    "प्राप्तकर्ता (सुरक्षित): $lockedRecipient • लंबित बुकिंग: $pendingBookingsCount • लंबित मॉडल: $pendingModelsCount"
                else
                    "Locked Recipient: $lockedRecipient • Pending Bookings: $pendingBookingsCount • Pending Models: $pendingModelsCount",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFC9C2B5)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHindi) "नई बुकिंग अनुरोध अलर्ट" else "New Booking Request Alerts",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Text(
                        text = if (isHindi) "नई शूट बुकिंग पर तुरंत ईमेल भेजें" else "Email admin on every new booking submission",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9689)
                    )
                }
                Switch(
                    checked = bookingAlertsEnabled,
                    enabled = !isSaving,
                    onCheckedChange = { checked -> persistSettings(checked, modelAlertsEnabled) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("admin_alert_toggle_booking"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = GoldPrimary
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHindi) "नया 18+ मॉडल आवेदन अलर्ट" else "New 18+ Model Application Alerts",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Text(
                        text = if (isHindi) "नए मॉडल आवेदन पर तुरंत ईमेल भेजें" else "Email admin on every new 18+ model application",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9689)
                    )
                }
                Switch(
                    checked = modelAlertsEnabled,
                    enabled = !isSaving,
                    onCheckedChange = { checked -> persistSettings(bookingAlertsEnabled, checked) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("admin_alert_toggle_model"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = GoldPrimary
                    )
                )
            }
        }
    }
}

