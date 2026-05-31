package com.haoze.keynote.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import com.haoze.keynote.data.db.NoteDatabase
import com.haoze.keynote.data.db.entity.BillEntity
import com.haoze.keynote.data.db.entity.NoteWithTags
import com.haoze.keynote.data.db.entity.ScheduleEntity
import com.haoze.keynote.data.repository.BillRepository
import com.haoze.keynote.data.repository.NoteRepository
import com.haoze.keynote.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    fun writeToDownloads(context: Context, fileName: String, mimeType: String, content: ByteArray) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/KeyNote")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw Exception("无法创建文件: $fileName")
        context.contentResolver.openOutputStream(uri)?.use { it.write(content) }
            ?: throw Exception("无法写入文件: $fileName")
    }

    // ========== 笔记导出 ==========

    enum class NoteExportFormat { MARKDOWN, TXT, PDF }

    suspend fun exportNotes(
        context: Context,
        startDate: Long? = null,
        endDate: Long? = null,
        tagIds: List<Long>? = null,
        format: NoteExportFormat = NoteExportFormat.MARKDOWN
    ): Int {
        val db = NoteDatabase.getDatabase(context)
        val repository = NoteRepository(db.noteDao(), db.tagDao(), PreferencesManager(context))

        val notes: List<NoteWithTags> = when {
            startDate != null && endDate != null && !tagIds.isNullOrEmpty() ->
                repository.getActiveNotesByDateRangeAndTags(startDate, endDate, tagIds).first()
            startDate != null && endDate != null ->
                repository.getActiveNotesByDateRange(startDate, endDate).first()
            else ->
                repository.getAllActiveNotesWithTags().first()
        }

        if (notes.isEmpty()) throw Exception("没有符合条件的笔记")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        notes.forEach { noteWithTags ->
            val safeTitle = noteWithTags.note.title
                .ifBlank { "无标题" }
                .replace(Regex("[/\\\\:*?\"<>|]"), "_")
            val timestamp = dateFormat.format(Date(noteWithTags.note.createdAt))

            when (format) {
                NoteExportFormat.MARKDOWN -> {
                    val fileName = "$safeTitle-$timestamp.md"
                    val content = buildMarkdown(noteWithTags)
                    writeToDownloads(context, fileName, "text/markdown", content.toByteArray(Charsets.UTF_8))
                }
                NoteExportFormat.TXT -> {
                    val fileName = "$safeTitle-$timestamp.txt"
                    val content = buildTxt(noteWithTags)
                    writeToDownloads(context, fileName, "text/plain", content.toByteArray(Charsets.UTF_8))
                }
                NoteExportFormat.PDF -> {
                    val fileName = "$safeTitle-$timestamp.pdf"
                    val content = generatePdf(noteWithTags)
                    writeToDownloads(context, fileName, "application/pdf", content)
                }
            }
        }
        return notes.size
    }

    private fun buildMarkdown(noteWithTags: NoteWithTags): String = buildString {
        appendLine("# ${noteWithTags.note.title}")
        appendLine()
        if (noteWithTags.tags.isNotEmpty()) {
            appendLine("标签: ${noteWithTags.tags.joinToString(" ") { "#${it.name}" }}")
            appendLine()
        }
        appendLine(noteWithTags.note.content)
    }

    private fun buildTxt(noteWithTags: NoteWithTags): String = buildString {
        appendLine(noteWithTags.note.title)
        appendLine()
        if (noteWithTags.tags.isNotEmpty()) {
            appendLine("标签: ${noteWithTags.tags.joinToString(", ") { it.name }}")
            appendLine()
        }
        appendLine(noteWithTags.note.content)
    }

    private fun generatePdf(noteWithTags: NoteWithTags): ByteArray {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        val paint = Paint().apply {
            textSize = 12f
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            textSize = 18f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val tagPaint = Paint().apply {
            textSize = 10f
            isAntiAlias = true
            color = android.graphics.Color.GRAY
        }

        var y = 40f
        val margin = 40f
        val maxWidth = 595f - margin * 2

        canvas.drawText(noteWithTags.note.title, margin, y, titlePaint)
        y += 30f

        if (noteWithTags.tags.isNotEmpty()) {
            val tagText = "标签: ${noteWithTags.tags.joinToString(", ") { it.name }}"
            canvas.drawText(tagText, margin, y, tagPaint)
            y += 20f
        }
        y += 10f

        val lines = noteWithTags.note.content.split("\n")
        for (line in lines) {
            if (y > 800f) {
                document.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                page = document.startPage(newPageInfo)
                canvas = page.canvas
                y = 40f
            }
            if (line.isBlank()) {
                y += 14f
                continue
            }
            val chars = line.toList()
            var currentLine = ""
            for (ch in chars) {
                val testLine = currentLine + ch
                if (paint.measureText(testLine) > maxWidth) {
                    canvas.drawText(currentLine, margin, y, paint)
                    y += 16f
                    currentLine = ch.toString()
                    if (y > 800f) {
                        document.finishPage(page)
                        val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                        page = document.startPage(newPageInfo)
                        canvas = page.canvas
                        y = 40f
                    }
                } else {
                    currentLine = testLine
                }
            }
            if (currentLine.isNotBlank()) {
                canvas.drawText(currentLine, margin, y, paint)
                y += 16f
            }
        }

        document.finishPage(page)
        val outputStream = ByteArrayOutputStream()
        document.writeTo(outputStream)
        document.close()
        return outputStream.toByteArray()
    }

    // ========== 账单导出 ==========

    suspend fun exportBills(
        context: Context,
        startDate: Long? = null,
        endDate: Long? = null,
        categoryIds: List<Long>? = null
    ): Int {
        val db = NoteDatabase.getDatabase(context)
        val repository = BillRepository(db.billDao(), db.categoryDao())

        val bills: List<BillEntity> = when {
            startDate != null && endDate != null && !categoryIds.isNullOrEmpty() ->
                repository.getBillsByDateRangeAndCategory(startDate, endDate, categoryIds)
            startDate != null && endDate != null ->
                repository.getBillsByDateRange(startDate, endDate)
            else ->
                repository.getAllBillsList()
        }

        if (bills.isEmpty()) throw Exception("没有符合条件的账单")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val csvContent = buildString {
            appendLine("消费项目,金额,时间")
            bills.forEach { bill ->
                val item = bill.item.replace("\"", "\"\"")
                appendLine("\"${item}\",${bill.amount},${dateFormat.format(Date(bill.date))}")
            }
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val fileName = "账单导出-$dateStr.csv"
        writeToDownloads(context, fileName, "text/csv", csvContent.toByteArray(Charsets.UTF_8))
        return bills.size
    }

    // ========== 日程导出 ==========

    enum class ScheduleExportFormat { ICS, CSV }

    suspend fun exportSchedules(
        context: Context,
        startDate: Long? = null,
        endDate: Long? = null,
        format: ScheduleExportFormat = ScheduleExportFormat.ICS
    ): Int {
        val db = NoteDatabase.getDatabase(context)
        val repository = ScheduleRepository(db.scheduleDao())

        val schedules: List<ScheduleEntity> = when {
            startDate != null && endDate != null ->
                repository.getSchedulesByDateRangeList(startDate, endDate)
            else ->
                repository.getAllSchedulesList()
        }

        if (schedules.isEmpty()) throw Exception("没有符合条件的日程")

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        when (format) {
            ScheduleExportFormat.ICS -> {
                val fileName = "日程导出-$dateStr.ics"
                val content = generateIcs(schedules)
                writeToDownloads(context, fileName, "text/calendar", content.toByteArray(Charsets.UTF_8))
            }
            ScheduleExportFormat.CSV -> {
                val fileName = "日程导出-$dateStr.csv"
                val content = generateScheduleCsv(schedules)
                writeToDownloads(context, fileName, "text/csv", content.toByteArray(Charsets.UTF_8))
            }
        }
        return schedules.size
    }

    private fun generateIcs(schedules: List<ScheduleEntity>): String = buildString {
        val dateTimeFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
        appendLine("BEGIN:VCALENDAR")
        appendLine("VERSION:2.0")
        appendLine("PRODID:-//KeyNote//Export//CN")
        appendLine("CALSCALE:GREGORIAN")
        schedules.forEach { schedule ->
            appendLine("BEGIN:VEVENT")
            appendLine("DTSTART:${dateTimeFormat.format(Date(schedule.date))}")
            if (schedule.endDate != null) {
                appendLine("DTEND:${dateTimeFormat.format(Date(schedule.endDate))}")
            }
            appendLine("SUMMARY:${escapeIcs(schedule.title)}")
            if (!schedule.location.isNullOrBlank()) {
                appendLine("LOCATION:${escapeIcs(schedule.location)}")
            }
            if (!schedule.description.isNullOrBlank()) {
                appendLine("DESCRIPTION:${escapeIcs(schedule.description)}")
            }
            appendLine("UID:keynote-${schedule.id}@haoze")
            appendLine("DTSTAMP:${dateTimeFormat.format(Date())}")
            appendLine("END:VEVENT")
        }
        appendLine("END:VCALENDAR")
    }

    private fun escapeIcs(text: String): String {
        return text.replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
    }

    private fun generateScheduleCsv(schedules: List<ScheduleEntity>): String = buildString {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        appendLine("标题,开始时间,结束时间,地点,描述")
        schedules.forEach { schedule ->
            val title = schedule.title.replace("\"", "\"\"")
            val start = dateFormat.format(Date(schedule.date))
            val end = if (schedule.endDate != null) dateFormat.format(Date(schedule.endDate)) else ""
            val location = (schedule.location ?: "").replace("\"", "\"\"")
            val desc = (schedule.description ?: "").replace("\"", "\"\"")
            appendLine("\"$title\",\"$start\",\"$end\",\"$location\",\"$desc\"")
        }
    }
}
