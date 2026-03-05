package com.example.familienwecker.util

import android.content.Context
import com.example.familienwecker.R
import org.json.JSONObject
import java.io.InputStreamReader

data class WhatsNewContent(
    val versionCode: Int,
    val title: String,
    val text: String,
    val buttonText: String
)

class WhatsNewManager(private val context: Context) {

    fun getWhatsNewContent(): WhatsNewContent? {
        return try {
            val inputStream = context.resources.openRawResource(R.raw.whats_new)
            val reader = InputStreamReader(inputStream)
            val jsonString = reader.readText()
            reader.close()
            inputStream.close()

            val json = JSONObject(jsonString)
            val versionCode = json.getInt("versionCode")
            
            // Sprache ermitteln (basierend auf Context, der App-Sprache berücksichtigen sollte)
            val lang = context.getSharedPreferences("FamilienweckerPrefs", Context.MODE_PRIVATE)
                .getString("APP_LANGUAGE", if (java.util.Locale.getDefault().language == "de") "de" else "en")

            if (lang == "de") {
                WhatsNewContent(
                    versionCode = versionCode,
                    title = json.getString("title_de"),
                    text = json.getString("text_de"),
                    buttonText = json.getString("button_text_de")
                )
            } else {
                WhatsNewContent(
                    versionCode = versionCode,
                    title = json.getString("title_en"),
                    text = json.getString("text_en"),
                    buttonText = json.getString("button_text_en")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
