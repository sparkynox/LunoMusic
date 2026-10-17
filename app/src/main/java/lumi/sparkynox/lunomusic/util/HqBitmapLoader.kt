package lumi.sparkynox.lunomusic.util

import android.content.Context
import android.net.Uri
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.common.util.BitmapLoader

// loads the artwork for the notification and the lock screen, asking for the big version of it
class HqBitmapLoader(ctx: Context) : BitmapLoader {
    private val inner = DataSourceBitmapLoader(ctx)

    override fun supportsMimeType(mimeType: String) = inner.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray) = inner.decodeBitmap(data)

    override fun loadBitmap(uri: Uri) = inner.loadBitmap(Uri.parse(Img.hq(uri.toString()) ?: uri.toString()))
}
