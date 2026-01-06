package com.otistran.flash_trade.core.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Utility for generating QR codes from wallet addresses.
 * Uses ZXing library for lightweight QR code generation.
 */
object QRCodeGenerator {

    private const val DEFAULT_SIZE = 512

    /**
     * Generate QR code bitmap from wallet address.
     * @param address Ethereum wallet address (0x...)
     * @param size QR code size in pixels (default 512)
     * @param chainId Optional chain ID for EIP-681 format
     */
    fun generateQRCode(
        address: String,
        size: Int = DEFAULT_SIZE,
        chainId: Long? = null
    ): Bitmap {
        val content = if (chainId != null) {
            "ethereum:$address@$chainId"  // EIP-681 format
        } else {
            address  // Simple address
        }

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size
        )

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                )
            }
        }

        return bitmap
    }

    /**
     * Validate Ethereum address format (basic checksum validation).
     */
    fun isValidEthereumAddress(address: String): Boolean {
        return address.matches(Regex("^0x[a-fA-F0-9]{40}$"))
    }

    /**
     * Extract address from EIP-681 format or plain address.
     */
    fun extractAddress(qrContent: String): String {
        return when {
            qrContent.startsWith("ethereum:") -> {
                qrContent.removePrefix("ethereum:")
                    .substringBefore("@")  // Remove chain ID
                    .substringBefore("?")  // Remove query params
            }
            else -> qrContent
        }
    }
}
