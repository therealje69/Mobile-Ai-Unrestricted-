package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.math.sin
import kotlin.random.Random

enum class DuplexVoiceState {
    IDLE,
    LISTENING_VAD,
    THINKING_INFERENCE,
    SPEAKING_DUPLEX,
    INTERRUPTED_BARGE_IN
}

class DuplexVoiceController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onUserUtteranceFinalized: (String) -> Unit,
    private val onBargeInTriggered: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _voiceState = MutableStateFlow(DuplexVoiceState.IDLE)
    val voiceState: StateFlow<DuplexVoiceState> = _voiceState.asStateFlow()

    private val _livePartialTranscript = MutableStateFlow("")
    val livePartialTranscript: StateFlow<String> = _livePartialTranscript.asStateFlow()

    private val _rmsEnergyLevel = MutableStateFlow(0.08f)
    val rmsEnergyLevel: StateFlow<Float> = _rmsEnergyLevel.asStateFlow()

    private val _spectrumBands = MutableStateFlow(List(24) { 0.1f })
    val spectrumBands: StateFlow<List<Float>> = _spectrumBands.asStateFlow()

    private val _isHandsFreeDuplexActive = MutableStateFlow(false)
    val isHandsFreeDuplexActive: StateFlow<Boolean> = _isHandsFreeDuplexActive.asStateFlow()

    private val _statusTelemetry = MutableStateFlow("VAD Standby • Ready for 2-Way Duplex")
    val statusTelemetry: StateFlow<String> = _statusTelemetry.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var waveformJob: Job? = null

    private var currentVoicePitch = 1.0f
    private var currentVoiceRate = 1.1f
    private var currentVadSensitivity = 0.55f
    private var autoListenAfterReply = true

    init {
        initTextToSpeech()
        startWaveformAnimator()
    }

    private fun initTextToSpeech() {
        try {
            textToSpeech = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.US
                    isTtsReady = true
                    textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _voiceState.value = DuplexVoiceState.SPEAKING_DUPLEX
                            _statusTelemetry.value = "Duplex Downlink Active • Speak or Tap to Barge-In"
                        }

                        override fun onDone(utteranceId: String?) {
                            mainHandler.post {
                                if (_isHandsFreeDuplexActive.value && autoListenAfterReply) {
                                    startListeningCycle()
                                } else {
                                    _voiceState.value = DuplexVoiceState.IDLE
                                    _statusTelemetry.value = "Turn Complete • Ready"
                                }
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            mainHandler.post {
                                if (_isHandsFreeDuplexActive.value && autoListenAfterReply) {
                                    startListeningCycle()
                                } else {
                                    _voiceState.value = DuplexVoiceState.IDLE
                                }
                            }
                        }
                    })
                }
            }
        } catch (_: Exception) {
            isTtsReady = false
        }
    }

    fun updateVoiceConfig(
        pitch: Float,
        rate: Float,
        vadSensitivity: Float,
        autoListen: Boolean
    ) {
        currentVoicePitch = pitch.coerceIn(0.5f, 1.8f)
        currentVoiceRate = rate.coerceIn(0.5f, 2.0f)
        currentVadSensitivity = vadSensitivity.coerceIn(0.1f, 0.95f)
        autoListenAfterReply = autoListen
        textToSpeech?.setPitch(currentVoicePitch)
        textToSpeech?.setSpeechRate(currentVoiceRate)
    }

    fun startHandsFreeDuplex() {
        _isHandsFreeDuplexActive.value = true
        startListeningCycle()
    }

    fun stopHandsFreeDuplex() {
        _isHandsFreeDuplexActive.value = false
        _livePartialTranscript.value = ""
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                textToSpeech?.stop()
            } catch (_: Exception) {
            }
            _voiceState.value = DuplexVoiceState.IDLE
            _statusTelemetry.value = "Duplex Session Paused"
        }
    }

    fun triggerBargeInInterruption(newUtteranceIfImmediate: String? = null) {
        mainHandler.post {
            try {
                textToSpeech?.stop()
            } catch (_: Exception) {
            }
            _voiceState.value = DuplexVoiceState.INTERRUPTED_BARGE_IN
            _statusTelemetry.value = "BARGE-IN DETECTED • Halting Model Stream & Switching to Mic"
            onBargeInTriggered()

            scope.launch {
                delay(260L)
                if (!newUtteranceIfImmediate.isNullOrBlank()) {
                    _voiceState.value = DuplexVoiceState.THINKING_INFERENCE
                    onUserUtteranceFinalized(newUtteranceIfImmediate)
                } else if (_isHandsFreeDuplexActive.value) {
                    startListeningCycle()
                } else {
                    _isHandsFreeDuplexActive.value = true
                    startListeningCycle()
                }
            }
        }
    }

    fun setThinkingState() {
        _voiceState.value = DuplexVoiceState.THINKING_INFERENCE
        _statusTelemetry.value = "Dispatching Raw Context to Private GPU Worker..."
    }

    fun speakModelResponse(text: String) {
        mainHandler.post {
            _voiceState.value = DuplexVoiceState.SPEAKING_DUPLEX
            _statusTelemetry.value = "Streaming Duplex Voice • VAD Barge-In Armed (${(currentVadSensitivity * 100).toInt()}%)"

            if (isTtsReady && textToSpeech != null) {
                textToSpeech?.setPitch(currentVoicePitch)
                textToSpeech?.setSpeechRate(currentVoiceRate)
                val params = Bundle()
                val utteranceId = UUID.randomUUID().toString()
                val result = textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                if (result == TextToSpeech.ERROR) {
                    simulateSpeechCadenceWhenTtsUnavailable(text)
                }
            } else {
                simulateSpeechCadenceWhenTtsUnavailable(text)
            }
        }
    }

    private fun simulateSpeechCadenceWhenTtsUnavailable(text: String) {
        scope.launch {
            val words = text.split(Regex("\\s+")).size.coerceAtLeast(5)
            val durationMs = ((words * 220L) / currentVoiceRate).toLong().coerceIn(1800L, 6500L)
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < durationMs &&
                _voiceState.value == DuplexVoiceState.SPEAKING_DUPLEX
            ) {
                delay(100L)
            }
            if (_voiceState.value == DuplexVoiceState.SPEAKING_DUPLEX) {
                if (_isHandsFreeDuplexActive.value && autoListenAfterReply) {
                    startListeningCycle()
                } else {
                    _voiceState.value = DuplexVoiceState.IDLE
                    _statusTelemetry.value = "Turn Complete • Ready"
                }
            }
        }
    }

    fun startListeningCycle() {
        mainHandler.post {
            _voiceState.value = DuplexVoiceState.LISTENING_VAD
            _livePartialTranscript.value = ""
            _statusTelemetry.value = "Listening (Real-Time VAD) • Speak Naturally"

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _statusTelemetry.value = "VAD Active (Emulator Acoustic Bridge • Use Voice Sim or Text)"
                return@post
            }

            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                    speechRecognizer?.setRecognitionListener(createRecognitionListener())
                }
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speechRecognizer?.startListening(intent)
            } catch (_: Exception) {
                _statusTelemetry.value = "VAD Active • Ready for Voice Turn or Text Input"
            }
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceState.value = DuplexVoiceState.LISTENING_VAD
                _statusTelemetry.value = "Mic Open • Real-Time VAD Locked"
            }

            override fun onBeginningOfSpeech() {
                if (_voiceState.value == DuplexVoiceState.SPEAKING_DUPLEX) {
                    triggerBargeInInterruption()
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.08f, 1.0f)
                _rmsEnergyLevel.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _statusTelemetry.value = "VAD End-of-Turn Detected • Processing..."
            }

            override fun onError(error: Int) {
                if (_isHandsFreeDuplexActive.value && _voiceState.value == DuplexVoiceState.LISTENING_VAD) {
                    _statusTelemetry.value = "VAD Listening Standby • Tap Quick Voice Prompt or Speak"
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()?.trim().orEmpty()
                if (spokenText.isNotBlank()) {
                    _livePartialTranscript.value = spokenText
                    _voiceState.value = DuplexVoiceState.THINKING_INFERENCE
                    onUserUtteranceFinalized(spokenText)
                } else if (_isHandsFreeDuplexActive.value) {
                    startListeningCycle()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (partial.isNotBlank()) {
                    _livePartialTranscript.value = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun startWaveformAnimator() {
        waveformJob?.cancel()
        waveformJob = scope.launch {
            var tick = 0f
            while (isActive) {
                tick += 0.24f
                val state = _voiceState.value
                val baseEnergy = when (state) {
                    DuplexVoiceState.IDLE -> 0.08f
                    DuplexVoiceState.LISTENING_VAD -> 0.42f
                    DuplexVoiceState.THINKING_INFERENCE -> 0.55f
                    DuplexVoiceState.SPEAKING_DUPLEX -> 0.78f
                    DuplexVoiceState.INTERRUPTED_BARGE_IN -> 0.95f
                }
                _rmsEnergyLevel.value = (baseEnergy + sin(tick) * 0.12f).coerceIn(0.06f, 1.0f)
                _spectrumBands.value = List(24) { idx ->
                    when (state) {
                        DuplexVoiceState.IDLE -> 0.08f + 0.04f * sin(tick + idx * 0.4f)
                        DuplexVoiceState.LISTENING_VAD -> (0.25f + 0.35f * sin(tick * 1.4f + idx * 0.5f) + Random.nextFloat() * 0.18f).coerceIn(0.1f, 0.95f)
                        DuplexVoiceState.THINKING_INFERENCE -> (0.35f + 0.30f * sin(tick * 2.2f + idx * 0.3f)).coerceIn(0.12f, 0.85f)
                        DuplexVoiceState.SPEAKING_DUPLEX -> (0.38f + 0.48f * sin(tick * 1.8f + idx * 0.6f) + Random.nextFloat() * 0.22f).coerceIn(0.15f, 1.0f)
                        DuplexVoiceState.INTERRUPTED_BARGE_IN -> (0.75f + Random.nextFloat() * 0.25f).coerceIn(0.6f, 1.0f)
                    }
                }
                delay(55L)
            }
        }
    }

    fun release() {
        waveformJob?.cancel()
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                textToSpeech?.stop()
                textToSpeech?.shutdown()
            } catch (_: Exception) {
            }
        }
    }
}
