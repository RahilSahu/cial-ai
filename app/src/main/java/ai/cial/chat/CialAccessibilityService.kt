package ai.cial.chat

import ai.cial.chat.polish.OutputMode
import ai.cial.chat.polish.PolishClient
import ai.cial.chat.polish.Tone
import ai.cial.chat.polish.TranscriptCleaner
import ai.cial.chat.settings.CialSettings
import ai.cial.chat.ui.MainActivity
import ai.cial.chat.ui.WaveformView
import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The Cial.ai assistant. It watches only for editable text field focus so it
 * can offer its microphone bubble, and it inserts text the user approved into
 * that focused field. See PRIVACY.md for the full disclosure.
 */
class CialAccessibilityService : AccessibilityService() {

    private lateinit var settings: CialSettings
    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelView: View? = null

    private var targetNode: AccessibilityNodeInfo? = null
    private var lastContentRefreshMs = 0L

    private var speechRecognizer: SpeechRecognizer? = null
    private var cancelling = false

    private var originalTranscript: String = ""
    private var panelMode: OutputMode = OutputMode.ENGLISH
    private var panelTone: Tone = Tone.NEUTRAL

    // Panel sub-views, valid while the panel is open.
    private var stateRecording: View? = null
    private var stateProcessing: View? = null
    private var stateReview: View? = null
    private var stateError: View? = null
    private var waveform: WaveformView? = null
    private var recordingStatus: TextView? = null
    private var reviewNote: TextView? = null
    private var reviewText: EditText? = null
    private var errorText: TextView? = null
    private var openAppButton: Button? = null
    private var modeChips: LinearLayout? = null
    private var toneChips: LinearLayout? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        settings = CialSettings(this)
        windowManager = getSystemService(WindowManager::class.java)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (panelView != null) return // The panel owns the interaction right now.
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == "com.android.systemui" || isLauncherPackage(pkg)) {
            clearTarget()
            hideBubble()
            return
        }
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> refreshFocusedNode()
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val now = System.currentTimeMillis()
                if (bubbleView == null && now - lastContentRefreshMs > 400) {
                    lastContentRefreshMs = now
                    refreshFocusedNode()
                }
            }
        }
    }

    override fun onInterrupt() {
        hidePanel()
        hideBubble()
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        hidePanel()
        hideBubble()
        clearTarget()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun isLauncherPackage(pkg: String): Boolean =
        pkg.contains("launcher") || pkg == "com.miui.home" ||
            pkg == "com.google.android.apps.nexuslauncher"

    // ------------------------------------------------------------------
    // Focus tracking
    // ------------------------------------------------------------------

    private fun refreshFocusedNode() {
        val root = rootInActiveWindow
        if (root == null) {
            clearTarget()
            hideBubble()
            return
        }
        val rootPkg = root.packageName?.toString()
        if (rootPkg == packageName || rootPkg == "com.android.systemui" ||
            (rootPkg != null && isLauncherPackage(rootPkg))
        ) {
            clearTarget()
            hideBubble()
            return
        }
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable && focused.isFocused) {
            setTarget(focused)
            showBubble()
        } else {
            focused?.recycle()
            clearTarget()
            hideBubble()
        }
    }

    private fun setTarget(node: AccessibilityNodeInfo) {
        if (targetNode != node) {
            targetNode?.recycle()
            targetNode = node
        }
    }

    private fun clearTarget() {
        targetNode?.recycle()
        targetNode = null
    }

    // ------------------------------------------------------------------
    // Floating bubble
    // ------------------------------------------------------------------

    private fun showBubble() {
        if (bubbleView != null || panelView != null) return
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_bubble, null)
        val metrics = resources.displayMetrics
        val sizePx = (56 * metrics.density).toInt()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = metrics.widthPixels - sizePx - (14 * metrics.density).toInt()
            y = (metrics.heightPixels * 0.38f).toInt()
        }
        val button = view.findViewById<ImageButton>(R.id.bubble_button)
        attachDragAndTap(button, params)
        try {
            windowManager.addView(view, params)
            bubbleView = view
            bubbleParams = params
        } catch (e: Exception) {
            bubbleView = null
            bubbleParams = null
        }
    }

    private fun hideBubble() {
        bubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // Already detached.
            }
        }
        bubbleView = null
        bubbleParams = null
    }

    private fun attachDragAndTap(view: View, params: WindowManager.LayoutParams) {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    if (kotlin.math.abs(dx) > 12 || kotlin.math.abs(dy) > 12) moved = true
                    if (moved) {
                        params.x = startX + dx
                        params.y = startY + dy
                        bubbleView?.let { windowManager.updateViewLayout(it, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) openPanel()
                    true
                }
                else -> false
            }
        }
    }

    // ------------------------------------------------------------------
    // Voice panel
    // ------------------------------------------------------------------

    private fun openPanel() {
        if (panelView != null) return
        hideBubble()
        panelMode = settings.outputMode
        panelTone = settings.tone
        val view = LayoutInflater.from(this).inflate(R.layout.overlay_panel, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        }
        bindPanelViews(view)
        try {
            windowManager.addView(view, params)
            panelView = view
        } catch (e: Exception) {
            panelView = null
            return
        }
        startDictation()
    }

    private fun bindPanelViews(view: View) {
        stateRecording = view.findViewById(R.id.state_recording)
        stateProcessing = view.findViewById(R.id.state_processing)
        stateReview = view.findViewById(R.id.state_review)
        stateError = view.findViewById(R.id.state_error)
        waveform = view.findViewById(R.id.waveform)
        recordingStatus = view.findViewById(R.id.recording_status)
        reviewNote = view.findViewById(R.id.review_note)
        reviewText = view.findViewById(R.id.review_text)
        errorText = view.findViewById(R.id.error_text)
        openAppButton = view.findViewById(R.id.btn_open_app)
        modeChips = view.findViewById(R.id.mode_chips)
        toneChips = view.findViewById(R.id.tone_chips)

        view.findViewById<ImageButton>(R.id.panel_close).setOnClickListener {
            cancelListening()
            hidePanel()
        }
        view.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            cancelListening()
            hidePanel()
        }
        view.findViewById<Button>(R.id.btn_done).setOnClickListener {
            speechRecognizer?.stopListening()
        }
        view.findViewById<Button>(R.id.btn_retry).setOnClickListener { startDictation() }
        openAppButton?.setOnClickListener {
            hidePanel()
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
        view.findViewById<Button>(R.id.btn_copy).setOnClickListener {
            copyToClipboard(reviewText?.text?.toString().orEmpty())
            Toast.makeText(this, R.string.copied_toast, Toast.LENGTH_SHORT).show()
        }
        view.findViewById<Button>(R.id.btn_insert).setOnClickListener {
            val text = reviewText?.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) insertIntoTarget(text)
            hidePanel()
        }
        buildChips()
    }

    private fun buildChips() {
        val modeContainer = modeChips ?: return
        val toneContainer = toneChips ?: return
        modeContainer.removeAllViews()
        toneContainer.removeAllViews()
        OutputMode.entries.forEach { mode ->
            modeContainer.addView(makeChip(mode.label, mode == panelMode) {
                if (panelMode != mode) {
                    panelMode = mode
                    buildChips()
                    repolish()
                }
            })
        }
        Tone.entries.forEach { tone ->
            toneContainer.addView(makeChip(tone.label, tone == panelTone) {
                if (panelTone != tone) {
                    panelTone = tone
                    buildChips()
                    repolish()
                }
            })
        }
    }

    private fun makeChip(label: String, selected: Boolean, onClick: () -> Unit): Button {
        val density = resources.displayMetrics.density
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 13f
            setPadding((14 * density).toInt(), (6 * density).toInt(), (14 * density).toInt(), (6 * density).toInt())
            setBackgroundResource(if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip)
            setTextColor(ContextCompat.getColor(this@CialAccessibilityService, R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = (8 * density).toInt() }
            setOnClickListener { onClick() }
        }
    }

    private fun showState(state: View?) {
        listOf(stateRecording, stateProcessing, stateReview, stateError).forEach {
            it?.visibility = if (it == state) View.VISIBLE else View.GONE
        }
    }

    private fun hidePanel() {
        panelView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // Already detached.
            }
        }
        panelView = null
        stateRecording = null
        stateProcessing = null
        stateReview = null
        stateError = null
        waveform = null
        recordingStatus = null
        reviewNote = null
        reviewText = null
        errorText = null
        openAppButton = null
        modeChips = null
        toneChips = null
        handler.post { refreshFocusedNode() }
    }

    // ------------------------------------------------------------------
    // Speech recognition
    // ------------------------------------------------------------------

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            recordingStatus?.text = getString(R.string.listening)
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) {
            waveform?.onRmsChanged(rmsdB)
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            if (cancelling) {
                cancelling = false
                return
            }
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    showError(getString(R.string.nothing_heard), showOpenApp = false)
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    showError(getString(R.string.mic_permission_needed), showOpenApp = true)
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    showError(
                        "Speech recognition hit a network problem. Check your connection and try again.",
                        showOpenApp = false
                    )
                SpeechRecognizer.ERROR_AUDIO ->
                    showError("There was a problem with the microphone audio. Please try again.", showOpenApp = false)
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    showError("The speech recognizer is busy. Wait a moment and try again.", showOpenApp = false)
                else ->
                    showError("Speech recognition failed (error $error). Please try again.", showOpenApp = false)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val raw = matches?.firstOrNull().orEmpty()
            val cleaned = TranscriptCleaner.clean(raw)
            if (TranscriptCleaner.isBlank(cleaned)) {
                showError(getString(R.string.nothing_heard), showOpenApp = false)
                return
            }
            originalTranscript = cleaned
            onTranscriptReady(cleaned)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull().orEmpty()
            if (partial.isNotBlank()) {
                recordingStatus?.text = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun startDictation() {
        cancelling = false
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            showError(getString(R.string.mic_permission_needed), showOpenApp = true)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showError(getString(R.string.speech_unavailable), showOpenApp = false)
            return
        }
        showState(stateRecording)
        waveform?.reset()
        recordingStatus?.text = getString(R.string.listening)
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(recognitionListener)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, settings.inputLanguage.localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            showError("Could not start listening: ${e.message ?: "unknown error"}", showOpenApp = false)
        }
    }

    private fun cancelListening() {
        cancelling = true
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            // Recognizer already gone.
        }
    }

    private fun showError(message: String, showOpenApp: Boolean) {
        if (panelView == null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            return
        }
        errorText?.text = message
        openAppButton?.visibility = if (showOpenApp) View.VISIBLE else View.GONE
        showState(stateError)
    }

    // ------------------------------------------------------------------
    // Polish and review
    // ------------------------------------------------------------------

    private fun onTranscriptReady(transcript: String) {
        if (!settings.hasApiKey) {
            showReview(transcript, getString(R.string.no_key_note))
            return
        }
        runPolish(transcript)
    }

    private fun repolish() {
        if (!settings.hasApiKey || originalTranscript.isBlank()) return
        runPolish(originalTranscript)
    }

    private fun runPolish(transcript: String) {
        showState(stateProcessing)
        serviceScope.launch {
            val result = PolishClient(settings).polish(transcript, panelMode, panelTone)
            if (panelView == null) return@launch
            result.fold(
                onSuccess = { polished -> showReview(polished, note = null) },
                onFailure = { error ->
                    showReview(
                        transcript,
                        getString(R.string.polish_failed_note, error.message ?: "unknown error")
                    )
                }
            )
        }
    }

    private fun showReview(text: String, note: String?) {
        reviewText?.setText(text)
        reviewText?.setSelection(reviewText?.text?.length ?: 0)
        if (note.isNullOrBlank()) {
            reviewNote?.visibility = View.GONE
        } else {
            reviewNote?.visibility = View.VISIBLE
            reviewNote?.text = note
        }
        showState(stateReview)
    }

    // ------------------------------------------------------------------
    // Insert
    // ------------------------------------------------------------------

    private fun insertIntoTarget(text: String) {
        val node = targetNode
        var inserted = false
        if (node != null) {
            val existing = node.text?.toString().orEmpty()
            val combined = when {
                existing.isEmpty() -> text
                existing.endsWith(" ") || existing.endsWith("\n") -> existing + text
                else -> "$existing $text"
            }
            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    combined
                )
            }
            inserted = try {
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            } catch (e: Exception) {
                false
            }
        }
        if (!inserted) {
            copyToClipboard(text)
            Toast.makeText(this, R.string.insert_fallback_toast, Toast.LENGTH_LONG).show()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("Cial.ai text", text))
    }
}
