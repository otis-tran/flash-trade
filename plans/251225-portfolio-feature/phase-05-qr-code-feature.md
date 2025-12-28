# Phase 05: QR Code Feature

**Parent Plan**: plan.md
**Dependencies**: Phase 04 (ViewModel Integration)
**Date**: 2025-12-25
**Priority**: Medium
**Status**: DONE (2025-12-25)

---

## Overview

Add QR code generation for receiving crypto and QR scanner for sending. Implement bottom sheet UI for receive/send actions using ZXing library for lightweight implementation.

---

## Key Insights

- **ZXing (Zebra Crossing)**: Lightweight barcode/QR library, no Google Play Services dependency
- **QR Content**: Wallet address only (simple), or EIP-681 format (advanced: `ethereum:0x...@chainId?value=1`)
- **Camera Permission**: Runtime permission required for scanner
- **Bottom Sheet**: Material3 ModalBottomSheet for receive/send UI

---

## Requirements

1. Add ZXing dependency for QR code generation and scanning
2. Create QR code generator for wallet address (receive feature)
3. Create QR scanner composable with camera preview (send feature)
4. Add bottom sheet UI for receive/send actions
5. Handle camera permissions with rationale
6. Validate scanned addresses (checksum format)

---

## Architecture

```
presentation/feature/portfolio/
├── components/
│   ├── QRCodeView.kt           # Generate QR from address
│   ├── QRScannerView.kt        # Camera + scanner
│   └── ReceiveSendSheet.kt     # Bottom sheet UI
├── PortfolioScreen.kt          # Add bottom sheet trigger
└── PortfolioViewModel.kt       # Add receive/send events

core/util/
└── QRCodeGenerator.kt          # Bitmap generation utility
```

---

## Related Code Files

- `presentation/feature/portfolio/PortfolioScreen.kt` - Add floating action button for receive/send
- `presentation/feature/portfolio/PortfolioEvent.kt` - Add ShowReceiveSheet, ShowSendSheet events
- `presentation/feature/portfolio/PortfolioEffect.kt` - Add NavigateToSend effect

---

## Implementation Steps

### 1. Add ZXing Dependencies

```kotlin
// app/build.gradle.kts
dependencies {
    // QR code generation
    implementation("com.google.zxing:core:3.5.2")

    // Camera for scanning (Jetpack Compose)
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // CameraX ML Kit barcode scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
}
```

**Note**: Using MLKit for scanner (better Compose integration), ZXing for generation only.

### 2. Create QRCodeGenerator Utility

```kotlin
// core/util/QRCodeGenerator.kt
object QRCodeGenerator {

    /**
     * Generate QR code bitmap from wallet address.
     * @param address Ethereum wallet address (0x...)
     * @param size QR code size in pixels
     * @param chainId Optional chain ID for EIP-681 format
     */
    fun generateQRCode(
        address: String,
        size: Int = 512,
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
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK
                    else android.graphics.Color.WHITE
                )
            }
        }

        return bitmap
    }
}
```

### 3. Create QRCodeView Composable

```kotlin
// presentation/feature/portfolio/components/QRCodeView.kt
@Composable
fun QRCodeView(
    address: String,
    chainId: Long,
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
            text = "Receive ${if (chainId == 1L) "Ethereum" else "Linea"} ETH",
            style = MaterialTheme.typography.titleLarge
        )

        Image(
            bitmap = qrBitmap.asImageBitmap(),
            contentDescription = "QR Code for wallet address",
            modifier = Modifier
                .size(256.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(16.dp)
        )

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

        Button(
            onClick = { /* Copy to clipboard */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Copy Address")
        }

        Text(
            text = "Share this QR code to receive crypto",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

### 4. Create QRScannerView with Camera

```kotlin
// presentation/feature/portfolio/components/QRScannerView.kt
@Composable
fun QRScannerView(
    onAddressScanned: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(
                onAddressScanned = { address ->
                    if (isValidEthereumAddress(address)) {
                        onAddressScanned(address)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Scan QR Code",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // Instruction
                Text(
                    text = "Align QR code within frame",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Scanning frame
            ScannerFrame(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.Center)
            )

        } else {
            // Permission denied state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Camera permission required",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Allow camera access to scan QR codes",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    onAddressScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(executor, BarcodeAnalyzer(onAddressScanned))
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("QRScanner", "Camera binding failed", e)
                }
            }, executor)

            previewView
        },
        modifier = modifier
    )
}

private class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull()?.rawValue?.let { value ->
                        // Extract address from EIP-681 format or plain address
                        val address = extractAddress(value)
                        onBarcodeDetected(address)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun extractAddress(qrContent: String): String {
        // Handle EIP-681: ethereum:0x...@chainId or plain address
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

@Composable
private fun ScannerFrame(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.border(
            width = 2.dp,
            color = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    )
}

private fun isValidEthereumAddress(address: String): Boolean {
    return address.matches(Regex("^0x[a-fA-F0-9]{40}$"))
}
```

### 5. Create ReceiveSendSheet Bottom Sheet

```kotlin
// presentation/feature/portfolio/components/ReceiveSendSheet.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveSendSheet(
    address: String,
    chainId: Long,
    onDismiss: () -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Tab row
            var selectedTab by remember { mutableStateOf(0) }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Receive") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Send") }
                )
            }

            // Content
            when (selectedTab) {
                0 -> QRCodeView(
                    address = address,
                    chainId = chainId
                )
                1 -> SendView(onScanClick = onSendClick)
            }
        }
    }
}

@Composable
private fun SendView(
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Send Crypto",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Scan recipient's QR code or enter address manually",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onScanClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Scan QR Code")
        }

        OutlinedButton(
            onClick = { /* Navigate to send screen with manual input */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enter Address Manually")
        }
    }
}
```

### 6. Update PortfolioScreen with FAB

```kotlin
// presentation/feature/portfolio/PortfolioScreen.kt
@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    var showReceiveSendSheet by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (state.hasWallet) {
                FloatingActionButton(
                    onClick = { showReceiveSendSheet = true }
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = "Receive/Send")
                }
            }
        }
    ) { padding ->
        PortfolioContent(
            state = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding)
        )

        // Bottom sheet
        if (showReceiveSendSheet && state.walletAddress != null) {
            ReceiveSendSheet(
                address = state.walletAddress!!,
                chainId = state.currentNetwork.chainId,
                onDismiss = { showReceiveSendSheet = false },
                onSendClick = {
                    showReceiveSendSheet = false
                    showScanner = true
                }
            )
        }

        // QR Scanner (full screen)
        if (showScanner) {
            QRScannerView(
                onAddressScanned = { address ->
                    showScanner = false
                    // TODO: Navigate to send screen with pre-filled address
                },
                onDismiss = { showScanner = false }
            )
        }
    }
}
```

---

## Success Criteria

- [ ] QR code generates correctly for wallet address
- [ ] QR scanner detects addresses from camera
- [ ] EIP-681 format parsed correctly (ethereum:0x...@chainId)
- [ ] Camera permission handled with proper rationale
- [ ] Bottom sheet UI matches Material3 design
- [ ] Invalid addresses rejected with error message

---

## Testing Strategy

```kotlin
@Test
fun `QRCodeGenerator creates valid QR code`() {
    val address = "0x742d35Cc6634C0532925a3b844Bc9e7595f1d8C2"
    val bitmap = QRCodeGenerator.generateQRCode(address, 512)

    assertNotNull(bitmap)
    assertEquals(512, bitmap.width)
    assertEquals(512, bitmap.height)
}

@Test
fun `extractAddress handles EIP-681 format`() {
    val eip681 = "ethereum:0x742d35Cc6634C0532925a3b844Bc9e7595f1d8C2@1"
    val address = extractAddress(eip681)

    assertEquals("0x742d35Cc6634C0532925a3b844Bc9e7595f1d8C2", address)
}

@Test
fun `isValidEthereumAddress rejects invalid addresses`() {
    assertFalse(isValidEthereumAddress("0x123"))  // Too short
    assertFalse(isValidEthereumAddress("742d35Cc"))  // Missing 0x
    assertTrue(isValidEthereumAddress("0x742d35Cc6634C0532925a3b844Bc9e7595f1d8C2"))
}
```

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Camera permission denied | Show rationale, fallback to manual address input |
| QR scanner fails to detect | Add manual address paste option |
| Invalid address scanned | Validate checksum, show error before proceeding |
| Large bitmap causes OOM | Limit QR size to 512px, use RGB_565 config |

---

## Security Considerations

- **Address Validation**: Always validate checksum before using scanned address
- **QR Content**: Warn users if QR contains value/data (potential phishing)
- **Camera Access**: Request permission only when scanner opened (principle of least privilege)
- **Clipboard Access**: Sanitize pasted addresses, validate format
