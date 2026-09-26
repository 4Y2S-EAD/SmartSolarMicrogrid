package com.smartsolar.microgrid.member4.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.member4.operator.OperatorReservationsActivity
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class OperatorQrScannerActivity : AppCompatActivity() {
    private val model: OperatorQrViewModel by viewModels()
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var scanGate = AtomicBoolean(false)
    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var analysis: ImageAnalysis? = null
    private var previewUseCase: Preview? = null
    private var startingCamera = false
    private var generation = 0
    private var requestingPermission = false
    private var confirmation: AlertDialog? = null
    private var renderedPhase: QrPhase? = null
    private lateinit var preview: PreviewView
    private lateinit var frame: QrViewfinder
    private lateinit var primary: MaterialButton
    private val permissionPreferences by lazy { getSharedPreferences("m4_camera_permission", MODE_PRIVATE) }

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        requestingPermission = false
        if (granted) render(model.state.value) else showPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The view model retains in-flight verification/results across rotation without retaining raw QR data on disk.
        super.onCreate(savedInstanceState)
        setContentView(R.layout.m4_operator_qr_scanner)
        window.statusBarColor = Color.rgb(7, 62, 53)
        preview = findViewById(R.id.m4QrPreview)
        preview.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        frame = findViewById(R.id.m4QrFrame)
        primary = findViewById(R.id.m4QrPrimary)
        findViewById<View>(R.id.m4QrClose).setOnClickListener { finish() }
        findViewById<View>(R.id.m4QrAgain).setOnClickListener { model.scanAgain() }
        findViewById<View>(R.id.m4QrHistory).setOnClickListener {
            startActivity(Intent(this, OperatorReservationsActivity::class.java).putExtra("view", "completed"))
            finish()
        }
        findViewById<View>(R.id.m4QrTorch).setOnClickListener {
            camera?.let { it.cameraControl.enableTorch(it.cameraInfo.torchState.value != TorchState.ON) }
        }
        preview.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val point = preview.meteringPointFactory.createPoint(event.x, event.y)
                camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
                view.performClick()
            }
            true
        }
        preview.previewStreamState.observe(this) { stream ->
            frame.scanning = stream == PreviewView.StreamState.STREAMING && model.state.value.phase == QrPhase.SCANNING
            findViewById<TextView>(R.id.m4QrScanIndicator).setText(if (frame.scanning) R.string.m4_qr_scanning else R.string.m4_qr_opening)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) { model.state.collect(::render) }
        }
    }

    override fun onResume() {
        // Recheck permission after returning from Settings or from the background.
        super.onResume()
        render(model.state.value)
    }

    override fun onPause() {
        // Release real camera resources immediately when this screen is no longer in the foreground.
        stopCamera()
        super.onPause()
    }

    override fun onDestroy() {
        // Cancel pending camera callbacks and analysis work owned by this activity instance.
        stopCamera()
        confirmation?.dismiss()
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    private fun render(state: QrScreenState) {
        // Only a VALID backend response reveals completion; camera acquisition is a separate permission concern.
        if (state.phase == QrPhase.SCANNING) {
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                findViewById<View>(R.id.m4QrCameraPanel).isVisible = true
                findViewById<View>(R.id.m4QrResultPanel).isVisible = false
                startCamera()
            } else if (!permissionPreferences.getBoolean("requested", false) && !requestingPermission) {
                showPermission()
                requestCameraPermission()
            } else showPermission()
            return
        }
        stopCamera()
        val busy = state.phase in listOf(QrPhase.VERIFYING, QrPhase.COMPLETING)
        showResultPanel()
        findViewById<View>(R.id.m4QrProgress).isVisible = busy
        findViewById<View>(R.id.m4QrStatusBadge).isVisible = !busy
        val positive = state.phase in listOf(QrPhase.VERIFIED, QrPhase.COMPLETED)
        findViewById<TextView>(R.id.m4QrStatusBadge).apply {
            text = when (state.phase) { QrPhase.VERIFIED -> "VERIFIED"; QrPhase.COMPLETED -> "COMPLETED"; else -> "NOT VERIFIED" }
            setTextColor(Color.parseColor(if (positive) "#047857" else "#B42340"))
        }
        findViewById<TextView>(R.id.m4QrResultTitle).text = when (state.phase) {
            QrPhase.VERIFYING -> "Verifying reservation…"
            QrPhase.VERIFIED -> "QR verified"
            QrPhase.COMPLETING -> "Completing transfer…"
            QrPhase.COMPLETED -> "Transfer completed"
            QrPhase.REJECTED -> "Verification rejected"
            else -> "Unable to confirm"
        }
        findViewById<TextView>(R.id.m4QrResultMessage).text = if (busy) "Please wait for server confirmation." else state.message
        findViewById<View>(R.id.m4QrDetailsCard).isVisible = state.reservation != null
        state.reservation?.let { showDetails(it, state.completion) }
        primary.isVisible = state.phase in listOf(QrPhase.VERIFIED, QrPhase.COMPLETING)
        primary.isEnabled = state.phase == QrPhase.VERIFIED
        primary.setText(R.string.m4_qr_complete)
        primary.setOnClickListener { confirmCompletion() }
        findViewById<View>(R.id.m4QrAgain).isVisible = !busy
        findViewById<View>(R.id.m4QrHistory).isVisible = !busy
        if (renderedPhase != state.phase && !busy) {
            findViewById<View>(R.id.m4QrResultPanel).apply {
                alpha = 0f; translationY = dp(10).toFloat()
                animate().alpha(1f).translationY(0f).setDuration(180).start()
            }
        }
        renderedPhase = state.phase
    }

    private fun showDetails(reservation: VerifiedReservation, completion: CompleteTransferResponse?) {
        // Display only authoritative fields returned by the API, never the decoded credential.
        val rows = findViewById<LinearLayout>(R.id.m4QrDetails)
        rows.removeAllViews()
        val details = mutableListOf(
            "STATION" to reservation.stationName,
            "PROSUMER" to "${reservation.prosumerName}\nNIC: ${reservation.prosumerNic}",
            "RESERVATION" to reservation.reservationId,
            "BATTERY SLOT" to "Slot ${reservation.slotNumber} · ${reservation.capacityKwh} kWh capacity",
            "SCHEDULE" to "${reservation.bookingDate}\n${reservation.startTime} – ${reservation.endTime}",
            "STATUS" to (completion?.status ?: reservation.status)
        )
        completion?.let {
            val completedTime = try {
                java.time.Instant.parse(it.completedAt).atZone(java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM))
            } catch (_: Exception) { it.completedAt }
            details.add("COMPLETED AT" to completedTime)
        }
        details.forEachIndexed { index, (label, value) ->
            rows.addView(TextView(this).apply {
                text = label; textSize = 11f; setTextColor(Color.rgb(93, 117, 105))
                setPadding(0, if (index == 0) 0 else dp(18), 0, dp(5))
                letterSpacing = .08f
            })
            rows.addView(TextView(this).apply {
                text = value; textSize = if (index == 0) 20f else 15f
                setTextColor(Color.rgb(22, 61, 50))
                if (index == 0) setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
        }
    }

    private fun confirmCompletion() {
        // Explicit confirmation follows server verification; rapid repeated taps cannot open duplicate dialogs.
        val state = model.state.value
        if (state.phase != QrPhase.VERIFIED || state.reservation == null || confirmation?.isShowing == true) return
        confirmation = AlertDialog.Builder(this).setTitle(R.string.m4_qr_confirm_title)
            .setMessage(getString(R.string.m4_qr_confirm, state.reservation.stationName))
            .setNegativeButton(R.string.m4_qr_cancel, null)
            .setPositiveButton(R.string.m4_qr_confirm_action) { _, _ -> model.complete() }.show()
    }

    private fun requestCameraPermission() {
        // Request only camera access, from the scanner, and remember denial across screen recreation.
        if (requestingPermission) return
        requestingPermission = true
        permissionPreferences.edit().putBoolean("requested", true).apply()
        permission.launch(Manifest.permission.CAMERA)
    }

    private fun showPermission() {
        // A permanent denial routes to app Settings instead of repeatedly showing an ineffective permission request.
        stopCamera()
        showResultPanel()
        findViewById<View>(R.id.m4QrProgress).isVisible = false
        findViewById<View>(R.id.m4QrDetailsCard).isVisible = false
        findViewById<View>(R.id.m4QrAgain).isVisible = false
        findViewById<View>(R.id.m4QrHistory).isVisible = false
        findViewById<TextView>(R.id.m4QrStatusBadge).apply { isVisible = true; text = "CAMERA ACCESS"; setTextColor(Color.rgb(4, 120, 87)) }
        findViewById<TextView>(R.id.m4QrResultTitle).setText(R.string.m4_qr_permission_title)
        val permanent = permissionPreferences.getBoolean("requested", false) &&
            !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        findViewById<TextView>(R.id.m4QrResultMessage).setText(if (permanent) R.string.m4_qr_permission_settings else R.string.m4_qr_permission_message)
        primary.isVisible = true; primary.isEnabled = true
        primary.setText(if (permanent) R.string.m4_qr_settings else R.string.m4_qr_allow)
        primary.setOnClickListener {
            if (permanent) startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            else requestCameraPermission()
        }
    }

    private fun startCamera() {
        // Bind a genuine CameraX Preview plus frame analysis to this foreground activity's lifecycle.
        if (camera != null || startingCamera || isFinishing) return
        startingCamera = true
        val ticket = ++generation
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            if (ticket != generation || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || model.state.value.phase != QrPhase.SCANNING) return@addListener
            try {
                val cameraProvider = future.get()
                provider = cameraProvider
                if (!cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    showCameraError("A rear camera is required to scan a reservation QR."); return@addListener
                }
                val livePreview = Preview.Builder().build().also { it.setSurfaceProvider(preview.surfaceProvider) }
                previewUseCase = livePreview
                val frames = ImageAnalysis.Builder()
                    .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
                        ResolutionStrategy(android.util.Size(1280, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build())
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis = frames
                val decoder = QrFrameDecoder()
                val frameGate = AtomicBoolean(true)
                scanGate = frameGate
                frames.setAnalyzer(cameraExecutor) { image ->
                    try {
                        if (frameGate.get()) {
                            val plane = image.planes[0]
                            val decoded = decoder.decode(image.width, image.height, plane.buffer, plane.rowStride, plane.pixelStride)
                            if (decoded != null && frameGate.compareAndSet(true, false)) {
                                runOnUiThread {
                                    if (ticket == generation && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                        stopCamera()
                                        model.verify(decoded)
                                    }
                                }
                            }
                        }
                    } catch (_: RuntimeException) {
                        // An unreadable frame is skipped; the next camera frame can still be decoded.
                    } finally { image.close() }
                }
                camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, livePreview, frames)
                startingCamera = false
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val torch = findViewById<ImageButton>(R.id.m4QrTorch)
                torch.isVisible = camera?.cameraInfo?.hasFlashUnit() == true
                camera?.cameraInfo?.torchState?.observe(this) { value ->
                    torch.isSelected = value == TorchState.ON
                    torch.contentDescription = getString(if (value == TorchState.ON) R.string.m4_qr_torch_off else R.string.m4_qr_torch_on)
                    torch.alpha = if (value == TorchState.ON) 1f else .65f
                }
                camera?.cameraInfo?.cameraState?.observe(this) { state ->
                    if (state.error != null && model.state.value.phase == QrPhase.SCANNING)
                        showCameraError("Camera is unavailable. Close other camera apps and try again.")
                }
            } catch (_: Exception) { showCameraError("Unable to open the camera. Check camera access and try again.") }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        // Invalidate queued callbacks and release only this scanner's use cases.
        generation++
        startingCamera = false
        scanGate.set(false)
        analysis?.clearAnalyzer()
        camera?.cameraInfo?.torchState?.removeObservers(this)
        camera?.cameraInfo?.cameraState?.removeObservers(this)
        camera?.cameraControl?.enableTorch(false)
        val bound = listOfNotNull(previewUseCase, analysis).toTypedArray()
        if (bound.isNotEmpty()) provider?.unbind(*bound)
        camera = null; analysis = null; previewUseCase = null
        if (::frame.isInitialized) frame.scanning = false
        findViewById<View>(R.id.m4QrTorch)?.isVisible = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun showCameraError(message: String) {
        // Keep failures recoverable without substituting a simulated scanner or manual QR input.
        stopCamera(); showResultPanel()
        findViewById<View>(R.id.m4QrProgress).isVisible = false
        findViewById<View>(R.id.m4QrDetailsCard).isVisible = false
        findViewById<View>(R.id.m4QrAgain).isVisible = false
        findViewById<View>(R.id.m4QrHistory).isVisible = false
        findViewById<TextView>(R.id.m4QrStatusBadge).apply { isVisible = true; text = "CAMERA UNAVAILABLE" }
        findViewById<TextView>(R.id.m4QrResultTitle).text = "Camera could not open"
        findViewById<TextView>(R.id.m4QrResultMessage).text = message
        primary.isVisible = true; primary.isEnabled = true; primary.setText(R.string.m4_qr_retry)
        primary.setOnClickListener { render(model.state.value) }
    }

    private fun showResultPanel() {
        findViewById<View>(R.id.m4QrCameraPanel).isVisible = false
        findViewById<View>(R.id.m4QrResultPanel).isVisible = true
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
