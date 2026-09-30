package com.example.paper_guru.data

import android.content.Context
import android.util.Log
import com.example.paper_guru.model.PaperItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.regex.Pattern

data class GitHubAssetInfo(
    val name: String,
    val downloadUrl: String,
    val size: Long
)

data class GitHubReleaseInfo(
    val tagName: String,
    val releaseName: String,
    val publishedAt: String,
    val htmlUrl: String,
    val assets: List<GitHubAssetInfo>,
    val bodyLinks: List<String>
)

object GitHubReleaseManager {
    private const val TAG = "GitHubReleaseManager"
    const val DEFAULT_REPO = "muhammadumairarshad33/paper-guru"

    fun getSavedRepo(context: Context): String {
        val prefs = context.getSharedPreferences("paper_guru_prefs", Context.MODE_PRIVATE)
        return prefs.getString("github_repo", DEFAULT_REPO) ?: DEFAULT_REPO
    }

    fun saveRepo(context: Context, repo: String) {
        val prefs = context.getSharedPreferences("paper_guru_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("github_repo", repo.trim()).apply()
    }

    suspend fun fetchReleases(repoPath: String): Result<List<GitHubReleaseInfo>> =
        withContext(Dispatchers.IO) {
            val cleanRepo = repoPath.trim()
                .removePrefix("https://github.com/")
                .removePrefix("http://github.com/")
                .removeSuffix("/releases")
                .removeSuffix("/")

            val apiUrl = "https://api.github.com/repos/$cleanRepo/releases"
            Log.d(TAG, "Fetching releases from: $apiUrl")

            val jsonRes = HttpDownloadHelper.getJsonString(apiUrl)
            if (jsonRes.isSuccess) {
                val jsonStr = jsonRes.getOrNull() ?: ""
                try {
                    val releasesList = mutableListOf<GitHubReleaseInfo>()
                    val array = JSONArray(jsonStr)

                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val tagName = obj.optString("tag_name", "v1.0.0")
                        val name = obj.optString("name", tagName)
                        val publishedAt = obj.optString("published_at", "")
                        val htmlUrl = obj.optString("html_url", "")
                        val body = obj.optString("body", "")

                        val assets = mutableListOf<GitHubAssetInfo>()
                        val assetsArray = obj.optJSONArray("assets")
                        if (assetsArray != null) {
                            for (j in 0 until assetsArray.length()) {
                                val assetObj = assetsArray.getJSONObject(j)
                                val assetName = assetObj.getString("name")
                                val downloadUrl = assetObj.getString("browser_download_url")
                                val size = assetObj.optLong("size", 0L)
                                assets.add(GitHubAssetInfo(assetName, downloadUrl, size))
                            }
                        }

                        val bodyLinks = extractUrlsFromMarkdown(body)

                        releasesList.add(
                            GitHubReleaseInfo(
                                tagName = tagName,
                                releaseName = name,
                                publishedAt = publishedAt,
                                htmlUrl = htmlUrl,
                                assets = assets,
                                bodyLinks = bodyLinks
                            )
                        )
                    }
                    if (releasesList.isNotEmpty()) {
                        return@withContext Result.success(releasesList)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing releases JSON: ${e.message}")
                }
            }

            // Fallback: If GitHub API is rate limited (HTTP 403), parse the public HTML releases page
            val htmlUrl = "https://github.com/$cleanRepo/releases"
            val htmlRes = HttpDownloadHelper.getJsonString(htmlUrl)
            if (htmlRes.isSuccess) {
                val html = htmlRes.getOrNull() ?: ""
                val assets = mutableListOf<GitHubAssetInfo>()
                val linkPattern = Pattern.compile("href=\"(/$cleanRepo/releases/download/[^\"]+\\.pdf)\"", Pattern.CASE_INSENSITIVE)
                val matcher = linkPattern.matcher(html)
                while (matcher.find()) {
                    val relativePath = matcher.group(1) ?: continue
                    val fullUrl = "https://github.com$relativePath"
                    val assetName = relativePath.substringAfterLast("/")
                    if (assets.none { it.name.equals(assetName, ignoreCase = true) }) {
                        assets.add(GitHubAssetInfo(assetName, fullUrl, 0L))
                    }
                }

                if (assets.isNotEmpty()) {
                    val fallbackRelease = GitHubReleaseInfo(
                        tagName = "v1.0.0",
                        releaseName = "Latest Release",
                        publishedAt = "Recent",
                        htmlUrl = htmlUrl,
                        assets = assets,
                        bodyLinks = emptyList()
                    )
                    return@withContext Result.success(listOf(fallbackRelease))
                }
            }

            Result.failure(jsonRes.exceptionOrNull() ?: Exception("No releases found for $cleanRepo"))
        }

    fun parseFilenameToPaper(fileName: String, remoteUrl: String): PaperItem? {
        val lower = fileName.lowercase()
        if (!lower.endsWith(".pdf")) return null

        val subject = when {
            lower.contains("bio") -> "Biology"
            lower.contains("chem") -> "Chemistry"
            lower.contains("comp") -> "Computer Science"
            lower.contains("eng") -> "English"
            lower.contains("math") || lower.contains("riyazi") || lower.contains("ریاضی") -> "Mathematics"
            lower.contains("phy") -> "Physics"
            lower.contains("urdu") || lower.contains("اردو") -> "Urdu"
            lower.contains("pak") || lower.contains("pst") || lower.contains("مطالعہ") -> "Pak Studies"
            lower.contains("islam") || lower.contains("isl") || lower.contains("اسلامیات") -> "Islamiat"
            lower.contains("tarjuma") || lower.contains("quran") || lower.contains("قرآن") -> "Tarjuma Quran"
            lower.contains("gen") || lower.contains("general") -> "General Science"
            else -> "Urdu"
        }

        val classFolder = when {
            lower.contains("12") || lower.contains("2nd") || lower.contains("inter2") -> "12th"
            lower.contains("11") || lower.contains("1st") || lower.contains("inter1") -> "11th"
            lower.contains("10") || lower.contains("matric2") || lower.contains("ssc2") -> "10th"
            lower.contains("9") || lower.contains("matric1") || lower.contains("ssc1") -> "9th"
            else -> "9th"
        }

        val yearPattern = Pattern.compile("(201[5-9]|202[0-9]|2[0-9])")
        val yearMatcher = yearPattern.matcher(lower)
        val year = if (yearMatcher.find()) {
            val matched = yearMatcher.group(1) ?: "2024"
            if (matched.length == 2) "20$matched" else matched
        } else {
            "2024"
        }

        val board = when {
            lower.contains("lhr") || lower.contains("lahore") -> "LHR"
            lower.contains("rwp") || lower.contains("rawalpindi") -> "RWP"
            lower.contains("fsd") || lower.contains("faisalabad") -> "FSD"
            lower.contains("mlt") || lower.contains("multan") -> "MLT"
            lower.contains("grw") || lower.contains("gujranwala") -> "GRW"
            lower.contains("swl") || lower.contains("sahiwal") -> "SWL"
            lower.contains("bwp") || lower.contains("bahawalpur") -> "BWP"
            lower.contains("sgd") || lower.contains("sargodha") -> "SGD"
            lower.contains("dgk") -> "DGK"
            lower.contains("fbise") || lower.contains("federal") -> "FBISE"
            else -> "LHR"
        }

        return PaperItem(
            classFolderName = classFolder,
            subjectName = subject,
            year = year,
            board = board,
            assetPath = null,
            remoteUrl = remoteUrl,
            isAvailable = true,
            isRemote = true
        )
    }

    private fun extractUrlsFromMarkdown(markdown: String): List<String> {
        val links = mutableListOf<String>()
        val pattern = Pattern.compile("https?://[^\\s\\)\\]]+\\.pdf", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(markdown)
        while (matcher.find()) {
            links.add(matcher.group())
        }
        return links
    }
}
