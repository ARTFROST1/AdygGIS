package com.adygyes.app.presentation.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.adygyes.app.R

/**
 * Utility functions for sharing app and rating in Google Play
 * 
 * Note: For dynamic settings from Admin Panel, use AppSettingsManager
 * and pass URLs/emails directly to these functions.
 */
object ShareUtils {

    // Fallback values (used if dynamic settings not available)
    @Deprecated("Use AppSettingsManager for dynamic values")
    const val WEBSITE_URL = "https://adyggis.vercel.app"
    @Deprecated("Use AppSettingsManager for dynamic values")
    const val SUPPORT_EMAIL = "frostmoontechsmc@gmail.com"
    @Deprecated("Use AppSettingsManager for dynamic values")
    const val TELEGRAM_URL = "https://t.me/MaykopTech"

    // Google Play (fill these when the app is published)
    private const val GOOGLE_PLAY_APP_ID = "com.adygyes.app"
    private const val GOOGLE_PLAY_WEB_URL = "https://play.google.com/store/apps/details?id=com.adygyes.app"
    private const val GOOGLE_PLAY_MARKET_URL = "market://details?id=$GOOGLE_PLAY_APP_ID"
    
    /**
     * Share app with others via system share dialog
     * @param context Android context
     * @param websiteUrl Website URL (from AppSettingsManager)
     * @param googlePlayUrl Google Play URL (from AppSettingsManager)
     */
    fun shareApp(context: Context, websiteUrl: String = WEBSITE_URL, googlePlayUrl: String = GOOGLE_PLAY_WEB_URL) {
        val googlePlayLine = if (googlePlayUrl.isNotBlank()) {
            "\n\n📱 Google Play: $googlePlayUrl"
        } else {
            ""
        }

        val shareText = """
            Попробуйте AdygGIS - приложение для изучения достопримечательностей Адыгеи!
            
            🌐 Сайт: $websiteUrl$googlePlayLine
        """.trimIndent()
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        
        context.startActivity(
            Intent.createChooser(shareIntent, "Поделиться приложением")
        )
    }
    
    /**
     * Open Google Play to rate the app
     * Tries to open Google Play app first, falls back to browser if not available
     * @param context Android context
     * @return true if successful, false if error occurred
     */
    fun rateApp(context: Context): Boolean {
        if (GOOGLE_PLAY_WEB_URL.isBlank()) {
            return false
        }

        return try {
            // Try to open Google Play app directly
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_PLAY_MARKET_URL))
            marketIntent.addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
            context.startActivity(marketIntent)
            true
        } catch (e: ActivityNotFoundException) {
            // If Google Play app is not installed, open in browser
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_PLAY_WEB_URL))
                context.startActivity(browserIntent)
                true
            } catch (e: Exception) {
                // Failed to open both Google Play and browser
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Open email client with pre-filled support email
     * @param context Android context
     * @param supportEmail Email address for support
     */
    fun openEmail(context: Context, supportEmail: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$supportEmail")
            putExtra(Intent.EXTRA_SUBJECT, "AdygGIS - Вопрос/Отзыв")
        }
        
        try {
            context.startActivity(Intent.createChooser(intent, "Отправить email"))
        } catch (e: Exception) {
            // Email client not available - try copying to clipboard instead
            copyToClipboard(context, supportEmail, context.getString(R.string.contact_email_copied))
        }
    }
    
    /**
     * Open URL in browser
     * @param context Android context
     * @param url URL to open
     */
    fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Copy text to clipboard
     * @param context Android context
     * @param text Text to copy
     * @param label Label for clipboard entry
     */
    private fun copyToClipboard(context: Context, text: String, label: String = "Текст") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
    }
}
