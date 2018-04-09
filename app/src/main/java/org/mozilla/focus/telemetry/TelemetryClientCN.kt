package org.mozilla.focus.telemetry

import android.util.Log
import androidx.annotation.VisibleForTesting
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.telemetry.config.TelemetryConfiguration
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TelemetryClientCN(
        private val client: Client
) {
    private val logger = Logger("telemetry/client")

    @Suppress("MagicNumber", "ReturnCount")
    fun uploadPing(configuration: TelemetryConfiguration, path: String, serializedPing: String): Boolean {
        val request = Request(
                url = configuration.serverEndpoint + path,
                method = Request.Method.POST,
                connectTimeout = Pair(configuration.connectTimeout.toLong(), TimeUnit.MILLISECONDS),
                readTimeout = Pair(configuration.readTimeout.toLong(), TimeUnit.MILLISECONDS),
                headers = MutableHeaders(
                        "Content-Type" to "application/json; charset=utf-8",
                        "User-Agent" to configuration.userAgent,
                        "Date" to createDateHeaderValue()
                ),
                body = Request.Body.fromString(serializedPing))

        val status = try {
            client.fetch(request).use { response -> response.status }
        } catch (e: IOException) {
            logger.warn("IOException while uploading ping", e)
            return false
        }

        logger.debug("Ping upload: $status")

        when (status) {
            in 200..299 -> {
                // Known success errors (2xx):
                // 200 - OK. Request accepted into the pipeline.

                // We treat all success codes as successful upload even though we only expect 200.
                return true
            }

            in 400..499 -> {
                // Known client (4xx) errors:
                // 404 - not found - POST/PUT to an unknown namespace
                // 405 - wrong request type (anything other than POST/PUT)
                // 411 - missing content-length header
                // 413 - request body too large (Note that if we have badly-behaved clients that
                //       retry on 4XX, we should send back 202 on body/path too long).
                // 414 - request path too long (See above)

                // Something our client did is not correct. It's unlikely that the client is going
                // to recover from this by re-trying again, so we just log and error and report a
                // successful upload to the service.
                logger.error("Server returned client error code: $status")
                return true
            }

            else -> {
                // Known other errors:
                // 500 - internal error

                // For all other errors we log a warning an try again at a later time.
                logger.warn("Server returned response code: $status")
                return false
            }
        }
    }

    @Suppress("MagicNumber", "ReturnCount")
    fun uploadPing(configuration: TelemetryConfiguration, path: String, serializedPing: String, pingType: String): Boolean {
        val request:Request
        if (pingType == TelemetryChinaPingBuilder.TYPE) {

            val str = serializedPing.replace("\"".toRegex(), "")
            val str2 = str.substring(0, str.length - 3)
            val info = str2.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val pathCN = path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val clientID = info[1].substring(9)
            val device = URLEncoder.encode(info[2].substring(7), "UTF-8")
            val requestUrl: String
            if (info[7] == "top_site") {
                val url = info[8].substring(4, info[8].length - 1).replace("\\", "")
                val urlStr = URLEncoder.encode(url, "UTF-8")
                Log.e("HttpCnTracking", url)
                Log.e("HttpCnTracking", urlStr)
                requestUrl = "https://m.g-fox.cn/cnrocket.gif?" + "clientID=" + clientID + "&device=" + device + "&type=" + info[7] + "&url=" + urlStr + "&documentID=" + pathCN[3] + "&version=" + pathCN[6] + pathCN[7]
            } else {
                requestUrl = "https://m.g-fox.cn/cnrocket.gif?" + "clientID=" + clientID + "&device=" + device + "&type=" + info[7] + "&documentID=" + pathCN[3] + "&version=" + pathCN[6] + pathCN[7] + "lite"
            }

            Log.e("HttpCnTracking", requestUrl)
            request = Request(
                    url = requestUrl,
                    method = Request.Method.GET,
                    connectTimeout = Pair(configuration.connectTimeout.toLong(), TimeUnit.MILLISECONDS),
                    readTimeout = Pair(configuration.readTimeout.toLong(), TimeUnit.MILLISECONDS),
                    headers = MutableHeaders(
                            "Content-Type" to "application/json; charset=utf-8",
                            "User-Agent" to configuration.userAgent,
                            "Date" to createDateHeaderValue()
                    )
            )

        }else {

            request = Request(
                    url = configuration.serverEndpoint + path,
                    method = Request.Method.POST,
                    connectTimeout = Pair(configuration.connectTimeout.toLong(), TimeUnit.MILLISECONDS),
                    readTimeout = Pair(configuration.readTimeout.toLong(), TimeUnit.MILLISECONDS),
                    headers = MutableHeaders(
                            "Content-Type" to "application/json; charset=utf-8",
                            "User-Agent" to configuration.userAgent,
                            "Date" to createDateHeaderValue()
                    ),
                    body = Request.Body.fromString(serializedPing))
        }

        val status = try {
            client.fetch(request).use { response -> response.status }
        } catch (e: IOException) {
            logger.warn("IOException while uploading ping", e)
            return false
        }

        logger.debug("Ping upload: $status")

        when (status) {
            in 200..299 -> {
                // Known success errors (2xx):
                // 200 - OK. Request accepted into the pipeline.

                // We treat all success codes as successful upload even though we only expect 200.
                return true
            }

            in 400..499 -> {
                // Known client (4xx) errors:
                // 404 - not found - POST/PUT to an unknown namespace
                // 405 - wrong request type (anything other than POST/PUT)
                // 411 - missing content-length header
                // 413 - request body too large (Note that if we have badly-behaved clients that
                //       retry on 4XX, we should send back 202 on body/path too long).
                // 414 - request path too long (See above)

                // Something our client did is not correct. It's unlikely that the client is going
                // to recover from this by re-trying again, so we just log and error and report a
                // successful upload to the service.
                logger.error("Server returned client error code: $status")
                return true
            }

            else -> {
                // Known other errors:
                // 500 - internal error

                // For all other errors we log a warning an try again at a later time.
                logger.warn("Server returned response code: $status")
                return false
            }
        }

    }
    @VisibleForTesting
    internal fun createDateHeaderValue(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
        return dateFormat.format(calendar.time)
    }
}
