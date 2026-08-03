package com.nus.folio.data.datasource

import android.content.Context
import com.nus.folio.data.util.SafeRelativePath
import com.nus.folio.data.util.SourceMimeTypes
import com.nus.folio.data.util.SourceOriginalFileWriter
import com.nus.folio.domain.model.SourceFileLocation
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.repository.SourceOriginalFileResolver
import java.io.File

class SourceOriginalFileDataSource(
    private val context: Context,
    private val sourceDataSource: SourceDataSource,
) : SourceOriginalFileResolver {

    override suspend fun resolveOriginalFile(spaceId: String, sourceId: String): SourceFileLocation {
        val safeSourceId = SafeRelativePath.requireSourceId(sourceId)
        val detail = sourceDataSource.fetchSourceDetail(spaceId, safeSourceId)
        if (detail.type == SourceType.WEB) {
            return SourceFileLocation.Remote(
                url = webUrls[safeSourceId] ?: DEFAULT_WEB_URL,
            )
        }

        val safeFileName = SafeRelativePath.requireFileName(detail.originalFileName)
        val originalsBase = File(context.cacheDir, ORIGINALS_DIR).also { it.mkdirs() }
        val directory = SafeRelativePath.resolveUnder(originalsBase, safeSourceId).also { it.mkdirs() }
        val target = SafeRelativePath.resolveUnder(directory, safeFileName)
        if (!target.exists()) {
            SourceOriginalFileWriter.writeOriginalFile(
                target = target,
                sourceId = safeSourceId,
                title = detail.title,
                extension = detail.fileExtension,
            )
        }

        return SourceFileLocation.Local(
            absolutePath = target.absolutePath,
            fileName = safeFileName,
            mimeType = SourceMimeTypes.forExtension(detail.fileExtension),
        )
    }

    companion object {
        private const val ORIGINALS_DIR = "source_originals"
        private const val DEFAULT_WEB_URL = "https://en.wikipedia.org/wiki/Artificial_neural_network"

        private val webUrls = mapOf(
            "9" to DEFAULT_WEB_URL,
        )
    }
}
