package com.tvpulse.tracker.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.tvpulse.tracker.model.MemberUsageSummary
import com.tvpulse.tracker.model.WeeklyReportData
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.EnumMap
import kotlin.concurrent.thread

object QrGenerator {
    fun generateQrBitmap(content: String, sizePx: Int = 360): ImageBitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 1)
            }
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            val fgColor = 0xFF00E5FF.toInt() // Neon Cyan matching TV Pulse
            val bgColor = 0xFF0A0E17.toInt() // Dark high-contrast background
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) fgColor else bgColor
                }
            }
            Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}

class LocalWeeklyReportHttpServer {
    @Volatile
    private var serverSocket: ServerSocket? = null
    @Volatile
    private var latestReportHtml: String = "<html><body>TV Pulse Weekly Report</body></html>"
    var boundPort: Int = 0
        private set

    fun startIfNeeded() {
        if (serverSocket != null && serverSocket?.isClosed == false) return
        thread(name = "TvPulseReportServer", isDaemon = true) {
            try {
                val socket = ServerSocket(0) // Bind available ephemeral port
                serverSocket = socket
                boundPort = socket.localPort
                while (!socket.isClosed) {
                    val client = socket.accept() ?: continue
                    thread(isDaemon = true) {
                        try {
                            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                            reader.readLine() // Read HTTP GET line
                            val body = latestReportHtml
                            val bytes = body.toByteArray(Charsets.UTF_8)
                            val writer = OutputStreamWriter(client.getOutputStream(), Charsets.UTF_8)
                            writer.write("HTTP/1.1 200 OK\r\n")
                            writer.write("Content-Type: text/html; charset=utf-8\r\n")
                            writer.write("Content-Length: ${bytes.size}\r\n")
                            writer.write("Connection: close\r\n\r\n")
                            writer.write(body)
                            writer.flush()
                        } catch (_: Exception) {
                        } finally {
                            runCatching { client.close() }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun updateReportContent(report: WeeklyReportData, members: List<MemberUsageSummary>) {
        val h = report.totalWeeklyMinutes / 60
        val m = report.totalWeeklyMinutes % 60
        val memberRows = members.joinToString("") { mem ->
            val mh = mem.weeklyUsedMinutes / 60
            val mm = mem.weeklyUsedMinutes % 60
            """
            <div style="display:flex;justify-content:space-between;padding:10px 0;border-bottom:1px solid #262a34;">
              <span><b>${mem.profile.name}</b> (${mem.profile.ageOrRoleTag})</span>
              <span style="color:#00e5ff;font-weight:bold;">${mh}h ${mm}m</span>
            </div>
            """.trimIndent()
        }
        latestReportHtml = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <title>TV Pulse 本周健康周报</title>
            </head>
            <body style="margin:0;padding:20px;background:#0f131c;color:#dfe2ef;font-family:-apple-system,sans-serif;">
              <div style="max-width:480px;margin:0 auto;background:#1c1f29;border-radius:16px;padding:20px;border:1px solid #00e5ff44;">
                <div style="color:#00e5ff;font-size:13px;font-weight:bold;">TV PULSE · 客厅大屏健康周报</div>
                <h2 style="margin:8px 0 4px 0;">第 ${report.weekNumber} 周电视使用洞察</h2>
                <div style="color:#bac9cc;font-size:13px;margin-bottom:16px;">${report.dateRangeText}</div>
                <div style="background:#181b25;border-radius:12px;padding:16px;margin-bottom:16px;">
                  <div style="color:#bac9cc;font-size:13px;">全周累计使用时长</div>
                  <div style="font-size:28px;font-weight:bold;color:#00e5ff;margin-top:4px;">${h} 小时 ${m} 分</div>
                  <div style="color:#7dffa2;font-size:13px;margin-top:4px;">达标天数: ${report.compliantDaysCount}/7 · 黄金档占比 ${report.timeDistribution.primePercent}%</div>
                </div>
                <div style="background:#181b25;border-radius:12px;padding:14px;margin-bottom:16px;font-size:14px;line-height:1.5;">
                  <b style="color:#00e5ff;">AI 管家评语：</b>${report.aiAdvisorComment}
                </div>
                <h3 style="font-size:16px;margin-bottom:8px;">家庭成员本周使用概览</h3>
                $memberRows
              </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun getLanReportUrl(): String? {
        val ip = getDeviceLanIp() ?: return null
        if (boundPort <= 0) return null
        return "http://$ip:$boundPort/report"
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    private fun getDeviceLanIp(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
