package com.otistran.flash_trade.presentation.feature.portfolio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otistran.flash_trade.core.util.QRCodeGenerator

/**
 * QR Code display composable for receiving crypto.
 * Shows QR code for wallet address with copy button.
 */
@Composable
fun QRCodeView(
    address: String,
    chainId: Long,
    networkName: String,
    onCopyAddress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val qrBitmap = remember(address, chainId) {
        QRCodeGenerator.generateQRCode(
            address = address,
            size = 512,
            chainId = chainId
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Receive $networkName",
            style = MaterialTheme.typography.titleLarge,
            fontSize = 20.sp
        )

        // QR Code with white background
        androidx.compose.foundation.Image(
            bitmap = qrBitmap.asImageBitmap(),
            contentDescription = "QR Code for wallet address",
            modifier = Modifier
                .size(256.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentScale = ContentScale.Fit
        )

        // Address display
        Text(
            text = address,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        )

        // Copy button
        Button(
            onClick = onCopyAddress,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Copy Address")
        }

        Text(
            text = "Share this QR code to receive crypto",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
