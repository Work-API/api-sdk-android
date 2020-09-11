package co.livil.workapi.model

import android.text.Html
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import moe.banana.jsonapi2.HasMany
import moe.banana.jsonapi2.JsonApi

/**
 * @see EmailSerializerTest for sample JSON
 */
@JsonApi(type = "email")
data class Email(
    @field:Json(name = "thread_id") var threadId: String? = "",
    @field:Json(name = "subject") var subject: String = "",
    @field:Json(name = "sender") var sender: Recipient = Recipient(),
    @field:Json(name = "to_recipients") var toRecipients: MutableList<Recipient> = mutableListOf(),
    @field:Json(name = "cc_recipients") var ccRecipients: MutableList<Recipient> = mutableListOf(),
    @field:Json(name = "bcc_recipients") var bccRecipients: MutableList<Recipient> = mutableListOf(),
    @field:Json(name = "flags") var flags: Flags = Flags(),
    @field:Json(name = "body") var body: EmailBody = EmailBody(),
    @field:Json(name = "labels") var labels: MutableList<String> = mutableListOf(),
    @field:Json(name = "received_at") var receivedAt: Long = 0,

    @field:Json(name = "mailboxes") var mailboxes: HasMany<Mailbox>? = HasMany(),
    @field:Json(name = "email_attachments") var emailAttachments: HasMany<EmailAttachment>? = HasMany()
) : WorkApiResource(), IMatchable {
    @Transient var urgent : Boolean = false

    fun subjectLabel(): String {
        return if (urgent) {
            "$subject [URGENT]"
        } else {
            subject
        }
    }

    fun processBodyContent() {
        if (hasPlaintextContent()) {
            splitContent(body.plainText!!)?.let { body.segments.addAll(it) }
        } else if (hasHtmlContent()) {
            strippedHtmlContent()?.let {
                splitContent(it)?.let { it1 -> body.segments.addAll(it1) }
            }
        }
    }

    private fun hasPlaintextContent(): Boolean {
        val plainTextEmpty= body.plainText?.isNotEmpty()
        return plainTextEmpty ?: false
    }

    private fun hasHtmlContent(): Boolean {
        val htmlEmpty= body.html?.isNotEmpty()
        return htmlEmpty ?: false
    }

    private fun strippedHtmlContent(): List<String>? {
        return body.html?.map {
            return@map Html
                .fromHtml(it, Html.FROM_HTML_MODE_COMPACT)
                .toString()
                .replace(Regex("(?s)<!--.*?-->"), "")
        }
    }

    private fun splitContent(content: List<String>): MutableList<String>? {
        val processed = mutableListOf<String>()
        content.forEach {
            it.split("\r\n\r\n", "\n\n").forEach { str -> processed.add(str.trim()) }
        }

        return processed.filter { it.isNotEmpty() }.toMutableList()
    }

    fun getToRecipientsLabel(): String {
        return recipientsLabel(toRecipients)
    }

    fun getCcRecipientsLabel(): String {
        return recipientsLabel(ccRecipients)
    }

    fun getBccRecipientsLabel(): String {
        return recipientsLabel(bccRecipients)
    }

    fun getSummary(): String {
        return "\"$subject\" from ${getSenderLabel()}"
    }

    fun getSenderLabel(): String {
        return if (sender.name.isNotEmpty()) {
            sender.name
        } else {
            sender.address
        }
    }

    private fun recipientsLabel(recipients: List<Recipient>): String {
        return recipients.joinToString(", ") {
            if (it.name.isNotEmpty()) {
                it.name
            } else {
                it.address
            }
        }
    }

    override fun matchesAccount(account: String): Boolean {
        // TODO("Not yet implemented")
        return false
    }

    override fun matchesContact(contact: String): Boolean {
        val regex = Regex.fromLiteral(contact)
        return sender.address.matches(regex) ||
               sender.name.matches(regex)
    }

    override fun matchesKeyword(keyword: String): Boolean {
        val regex = Regex.fromLiteral(keyword)
        return body.segments.any { it.matches(regex) }
    }
}

@JsonClass(generateAdapter = true)
data class Flags(
    @Json(name = "seen") var seen: Boolean = false,
    @Json(name = "flagged") var flagged: Boolean = false
)

@JsonClass(generateAdapter = true)
data class EmailBody(
    /**
     * `segments` is the list of readable strings derived from `plainText` and/or `html`
     *
     * TODO: Trigger auto generation of segments on (de)serialization
     */
    @Json(name = "segments") var segments: MutableList<String> = mutableListOf(),

    /**
     * `plainText` and `html` correspond to the blocks of text present in the original email
     *
     * Can be safely ignored
     */
    @Json(name = "plain_text") var plainText: MutableList<String>? = null,
    @Json(name = "html") var html: MutableList<String>? = null
)