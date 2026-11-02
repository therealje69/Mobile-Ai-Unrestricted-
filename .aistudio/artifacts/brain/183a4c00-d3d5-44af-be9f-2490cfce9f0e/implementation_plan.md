# SovereignVoice — Private Cloud Model Deployment & Full-Duplex Live Voice Studio

SovereignVoice is a private-first Android control center and real-time full-duplex voice client designed for deploying, configuring, and conversing with self-hosted custom, weighted, and abliterated open-weight models on HuggingFace Inference Endpoints and RunPod Serverless infrastructure without third-party filtering intermediaries.

## User Review & Critical Decisions

> [!IMPORTANT]
> All core functional and architectural decisions have been aligned with your selections from the clarification step:

- **Confirmed Decision 1 — Private Cloud Protocols**: Native support for **Custom HuggingFace Dedicated/Serverless Inference Endpoints** and **RunPod Serverless API Endpoints** (`runsync`, `run`, `status`, and streaming/OpenAI-compatible worker routes), with API keys managed via AI Studio Secrets (`BuildConfig`) and per-endpoint overrides.
- **Confirmed Decision 2 — Full-Duplex Voice & Barge-In**: **Real-time Voice Activity Detection (VAD)** with an interruptible full-duplex voice pipeline (`AudioRecord` RMS energy & speech recognition + instant TTS/audio playback cancellation on user barge-in) paired with a live text fallback and transcript drawer.
- **Confirmed Decision 3 — Model Tuning, Deployment & Personas**:
  - **Custom System Prompts, Abliteration/Weight Presets & Sampling Sliders**: Fine-grained control over orthogonalized/abliterated weight adapters, LoRA scales, temperature, top-p, min-p, repetition penalty, and raw system prompt templates.
  - **Cloud Deployment Manager**: Launch, scale, warm-up, and monitor private GPU worker instances (VRAM allocation, cold-start status, quantization format like AWQ/GPTQ/EXL2/GGUF, and active worker telemetry).
  - **Multi-Persona Profiles with Custom Voice Synthesis**: Create and switch between custom personas with dedicated system prompts, pitch/rate/voice engine bindings, and model parameter presets.
  - **Local Conversation History with Exportable Session Logs**: Device-local Room SQLite persistence for all sessions, turn-by-turn latency/token metrics, and one-tap JSON/Markdown log export via Android Share Sheet.

---

## 1. Overview & Core Concept

- **What It Does**: Enables AI researchers, enthusiasts, and privacy-conscious power users to deploy and manage private cloud GPU endpoints (RunPod Serverless & HuggingFace Endpoints), configure unfiltered/abliterated or custom-weighted model parameters, and engage in natural, low-latency, interruptible two-way voice conversations.
- **Target Audience / Persona**: Developers and private AI operators who run custom-weighted, fine-tuned, or orthogonalized open-weight models (e.g., Llama-3-Abliterated, Mistral/Qwen custom merges) on their own cloud compute and want a Gemini Live-style hands-free duplex voice interface.
- **Key Value**: Combines infrastructure orchestration (endpoint health, worker scaling, adapter weights) with an ultra-responsive full-duplex voice interface where speaking naturally interrupts the model mid-sentence—while keeping all transcripts and persona configurations strictly on-device.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Live Duplex Voice Stage (Home Tab)**: Features a central reactive audio orb & frequency waveform visualizer showing real-time states (`IDLE`, `LISTENING_VAD`, `THINKING_INFERENCE`, `SPEAKING_DUPLEX`, `INTERRUPTED_BARGE_IN`). Users can speak hands-free with automatic VAD barge-in, toggle mute/push-to-talk, or expand the live text fallback drawer to type messages and inspect raw prompt payloads.
  2. **Cloud Deployment Manager (Deployments Tab)**: View, create, and control private RunPod Serverless and HuggingFace endpoints. Configure GPU tiers (e.g., A100 80GB, RTX 4090, L40S), quantization (`EXL2`, `AWQ`, `GGUF Q6_K`), container/repo IDs, trigger warm-up pings, and inspect live endpoint latency and worker queue metrics.
  3. **Model Weights, Abliteration & Persona Studio (Tuning Tab)**: Switch or create Multi-Persona Profiles, adjust abliteration/orthogonalization strength, LoRA adapter weights, raw system prompts (with zero hidden pre-prompts), sampling sliders (`Temperature`, `Top-P`, `Min-P`, `Repetition Penalty`, `Max Tokens`), and voice synthesis tuning (`Pitch`, `Speech Rate`, `Voice Timbre`).
  4. **Session Vault & Log Exporter (History Tab)**: Browse locally stored multi-turn conversations, inspect token/latency telemetry per message, replay synthesized responses, or export full session logs as JSON or Markdown via the system Share Sheet.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: **Tactical Cyber-Minimalism / Precision Acoustic Console** — Dark obsidian surfaces with high-contrast telemetry readouts, tactile parameter sliders, and a luminous acoustic visualizer.
  - *Color Palette & Mood*:
    - Background / Surface: Deep Obsidian (`#0A0D12`) & Elevated Slate (`#121721`)
    - Primary Accent: Electric Cyan (`#00F0FF`) for active listening / VAD lock
    - Secondary Accent: Amber Pulse (`#FFB800`) for inference & weight tuning
    - Duplex Speaking / Barge-In Accent: Crimson Neon (`#FF2A6D`) & Emerald Signal (`#00E676`) for active worker telemetry
  - *Typography & Hierarchy*:
    - Display / Headers: **Space Grotesk** (bundled locally via `font-util`) for crisp technical headers and status indicators.
    - Body & Telemetry: **JetBrains Mono** for token metrics, endpoint URLs, and sampling values, paired with **Inter** for clean conversation transcripts.
  - *Component Styling & Layout*: Material 3 dark theme with edge-to-edge insets, subtle 1dp translucent borders, responsive canonical layouts (`NavigationBar` on handhelds, `NavigationRail` on tablets/foldables), and minimum 48dp touch targets.
- **Interactive Feedback & Motion**:
  - Physics-based spring animations on the central duplex voice orb that react dynamically to microphone decibel/RMS levels and TTS playback cadence.
  - Instant visual flash and haptic pulse when user voice triggers a barge-in interruption during model playback.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Real-Time Full-Duplex Voice & Barge-In Pipeline**
  - *Chosen Approach*: Combine Android `SpeechRecognizer` + real-time audio RMS/VAD monitoring with Android `TextToSpeech` (and optional remote audio stream decoding) in a state-machine coordinator (`DuplexVoiceController`). When the model is speaking and the user begins talking above the configurable VAD threshold, playback halts immediately (`tts.stop()` + active HTTP/stream job cancellation) and transitions into active listening.
  - *Why*: Delivers zero-cost, ultra-low-latency interruption on any Android device while supporting custom voice pitch/rate per persona and seamless text fallback when in quiet environments.
  - *Alternatives Considered*: Pure WebRTC server-side audio requires specialized server containers that standard RunPod/HuggingFace text-generation workers do not expose by default; hybrid client-side VAD + streaming inference works universally with any RunPod/HF LLM endpoint.

- **Decision 2: Device-Local Room Database with Zero Cloud Telemetry**
  - *Chosen Approach*: Store all endpoint configurations, abliteration/weight presets, multi-persona profiles, and conversation transcripts in a local **Room SQLite database** with native JSON/Markdown export.
  - *Why*: Aligns with the core privacy expectation of self-hosted uncensored/custom models—ensuring conversation logs and custom system prompts never touch third-party cloud databases.

- **Decision 3: Dual-Mode RunPod & HuggingFace Serverless Client**
  - *Chosen Approach*: Build a dedicated Retrofit/OkHttp networking layer supporting both **RunPod Serverless** (`/v2/{endpoint_id}/runsync`, `/run`, `/health`, and `/openai/v1/chat/completions`) and **HuggingFace Inference Endpoints** (TGI / vLLM payload formats), complete with live connection testing and fallback simulation when an endpoint is offline or warming up.
  - *Why*: Gives users immediate plug-and-play compatibility with real self-hosted endpoints while ensuring every control and duplex conversation flow can be tested immediately in the emulator.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         UI Layer (Jetpack Compose)                        │
│  ┌─────────────────┐ ┌──────────────────┐ ┌──────────────┐ ┌───────────┐  │
│  │  DuplexLiveScreen│ │DeploymentsScreen │ │ TuningStudio │ │SessionVault│  │
│  │ (Orb, VAD, Chat)│ │(RunPod/HF Nodes) │ │(Weights/Pers)│ │(Logs/Exp) │  │
│  └────────┬────────┘ └────────┬─────────┘ └──────┬───────┘ └─────┬─────┘  │
└───────────┼───────────────────┼──────────────────┼───────────────┼────────┘
            │                   │                  │               │
            ▼                   ▼                  ▼               ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                    Presentation & Domain State Layer                      │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                        SovereignVoiceViewModel                      │  │
│  │   StateFlows: DuplexState, ActiveDeployment, ActivePersona,         │  │
│  │               SamplingConfig, SessionMessages, WorkerTelemetry      │  │
│  └──────────────┬───────────────────────────────────┬──────────────────┘  │
│                 │                                   │                     │
│                 ▼                                   ▼                     │
│  ┌──────────────────────────────┐    ┌─────────────────────────────────┐  │
│  │    DuplexVoiceController     │    │   PrivateCloudInferenceClient   │  │
│  │ • Continuous VAD & RMS Meter │    │ • RunPod Serverless REST/Stream │  │
│  │ • Barge-In Interrupt Handler │    │ • HuggingFace TGI/vLLM Adapter  │  │
│  │ • SpeechRecognizer + TTS     │    │ • Abliteration/LoRA Payload Gen │  │
│  └──────────────────────────────┘    └─────────────────────────────────┘  │
└─────────────────────────────────────────┬─────────────────────────────────┘
                                          │
                                          ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                      Persistence Layer (Room SQLite)                      │
│  ┌──────────────────┐ ┌──────────────────┐ ┌───────────────────────────┐  │
│  │ CloudDeployment  │ │  PersonaProfile  │ │ ConversationSession &     │  │
│  │     Entity       │ │  & WeightPreset  │ │   SessionMessage Entity   │  │
│  └──────────────────┘ └──────────────────┘ └───────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State

- **`CloudDeploymentEntity`**: `id`, `name`, `provider` (`RUNPOD_SERVERLESS` | `HUGGINGFACE_ENDPOINT`), `endpointUrl`, `modelRepoId`, `gpuTier`, `quantization`, `abliterationMode` (`ORTHOGONALIZED`, `LORA_WEIGHTED`, `RAW_UNFILTERED`), `status` (`ONLINE`, `COLD_START`, `SCALING`, `OFFLINE`), `avgLatencyMs`, `activeWorkers`, `maxWorkers`.
- **`PersonaProfileEntity`**: `id`, `name`, `roleTag`, `systemPrompt` (raw unfiltered prompt), `abliterationWeight` (`0.0f..1.5f`), `temperature`, `topP`, `minP`, `repetitionPenalty`, `maxTokens`, `voicePitch`, `voiceRate`, `vadSensitivity`.
- **`ConversationSessionEntity` & `SessionMessageEntity`**: Stores multi-turn sessions linked to the deployment and persona used, with per-message `role`, `content`, `timestamp`, `latencyMs`, `tokensPerSec`, and `interruptedByUser` flag.

### Interactive Component & State Mapping

- **Live Duplex Voice & VAD Barge-In**:
  - Tapping **Start Live Duplex** requests `RECORD_AUDIO` permission (with graceful fallback to text-only mode if declined) and transitions `DuplexVoiceState` from `IDLE` to `LISTENING`.
  - When user speech is captured, `DuplexVoiceController` emits live partial transcripts and transitions to `THINKING`, dispatching the raw prompt + persona parameters + abliteration weights to `PrivateCloudInferenceClient`.
  - As the response arrives, `DuplexVoiceState` enters `SPEAKING` while monitoring the microphone VAD threshold; if the user speaks during playback, `triggerBargeIn()` immediately halts TTS, flags the turn as `[Interrupted]`, and captures the user's new utterance.
- **Deployment Lifecycle & Health Checks**:
  - Users can add new RunPod or HuggingFace endpoints, trigger **Wake / Scale Worker**, run a **Ping / Health Check**, and select any deployment as the active inference target.
- **Weight & Sampling Studio**:
  - Real-time sliders update the active `PersonaProfileEntity` in Room and immediately apply to the next turn in the live conversation.
- **Session Log Export**:
  - Tapping **Export Session** formats the full conversation, system prompt, and model weight metadata into JSON or Markdown and launches `Intent.ACTION_SEND` for saving or sharing.
