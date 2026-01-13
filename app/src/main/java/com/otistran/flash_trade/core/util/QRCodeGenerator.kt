package com.otistran.flash_trade.core.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Generates QR codes for wallet addresses using EIP-831 format.
 */
object QRCodeGenerator {

    private const val DEFAULT_SIZE = 512

    /**
     * Generate QR code bitmap from wallet address.
     *
     * @param address Ethereum wallet address (0x...)
     * @param size QR code size in pixels (default 512)
     * @return Bitmap containing the QR code
     */
    fun generateQRCode(address: String, size: Int = DEFAULT_SIZE): Bitmap {
        val content = "ethereum:$address"

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        return bitmap
    }
}

