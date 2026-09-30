package com.example.paper_guru.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object HttpDownloadHelper {

    suspend fun getJsonString(urlString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "PaperGuru-StudentPortal/1.0")
            conn.setRequestProperty("Accept", "application/json, text/html, */*")

            val code = conn.responseCode
            if (code in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP Error $code: ${conn.responseMessage}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadToFile(urlString: String, outputFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "PaperGuru-StudentPortal/1.0")

            if (conn.responseCode in 200..299) {
                outputFile.parentFile?.mkdirs()
                conn.inputStream.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Result.success(outputFile)
            } else {
                Result.failure(Exception("Download failed with HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
