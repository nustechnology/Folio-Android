package com.nus.folio.presentation.sourcedetail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.nus.folio.domain.model.SourceFileLocation
import java.io.File

enum class SourceOriginalOpenResult {
    SUCCESS,
    NO_APP,
    FAILED,
}

object SourceOriginalFileOpener {

    fun open(context: Context, location: SourceFileLocation): SourceOriginalOpenResult =
        when (location) {
            is SourceFileLocation.Remote -> openRemote(context, location.url)
            is SourceFileLocation.Local -> openLocal(context, location)
        }

    private fun openRemote(context: Context, url: String): SourceOriginalOpenResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        return launchIntent(context, intent)
    }

    private fun openLocal(context: Context, location: SourceFileLocation.Local): SourceOriginalOpenResult {
        val file = File(location.absolutePath)
        if (!file.exists()) return SourceOriginalOpenResult.FAILED

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, location.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return launchIntent(context, intent)
    }

    private fun launchIntent(context: Context, intent: Intent): SourceOriginalOpenResult {
        return try {
            if (intent.resolveActivity(context.packageManager) == null) {
                SourceOriginalOpenResult.NO_APP
            } else {
                val viewerIntent = Intent.createChooser(intent, null).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(viewerIntent)
                SourceOriginalOpenResult.SUCCESS
            }
        } catch (_: ActivityNotFoundException) {
            SourceOriginalOpenResult.NO_APP
        } catch (_: Exception) {
            SourceOriginalOpenResult.FAILED
        }
    }
}
