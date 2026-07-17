<p align="center">
  <img src="assets/backgroundremoved.png" alt="Axiom Logo" width="200px">
</p>

<h1 align="center">Axiom</h1>

<p align="center">
  <strong>🤖 Assistente de IA Autônomo para Android</strong>
</p>

<p align="center">
  <em>Seu assistente pessoal. Open source. 100% autônomo.</em>
</p>

<p align="center">
  <a href="https://github.com/Zoni1998/axiom/releases"><img src="https://img.shields.io/github/v/release/Zoni1998/axiom?style=for-the-badge&color=00FF88&labelColor=0D1117&logo=android&logoColor=white" alt="Release"></a>
  <a href="https://github.com/Zoni1998/axiom/stargazers"><img src="https://img.shields.io/github/stars/Zoni1998/axiom?style=for-the-badge&color=FFD700&labelColor=0D1117&logo=github&logoColor=white" alt="Stars"></a>
  <a href="https://github.com/Zoni1998/axiom/blob/main/LICENSE"><img src="https://img.shields.io/github/license/Zoni1998/axiom?style=for-the-badge&color=7C4DFF&labelColor=0D1117" alt="License"></a>
</p>

---

## 🎯 O que é o Axiom?

O Axiom não é só mais um chatbot. É um **agente de IA totalmente autônomo** que vive no seu Android e realmente *faz coisas* por você.

> *"Me avisa se vai chover amanhã e manda mensagem pro meu irmão avisando que vou me atrasar."*

O Axiom vai **planejar** isso em 3 passos, **executar** cada um, **verificar** os resultados e **se adaptar** se algo falhar — tudo sem você levantar um dedo.

---

## ✨ Funcionalidades

### 🧠 Motor de Agente Autônomo
| Capacidade | Descrição |
|---|---|
| **Auto-planejamento** | Divide comandos complexos em passos sequenciais |
| **Re-avaliação** | Monitora resultados e replaneja quando algo falha |
| **Detecção de intenção composta** | Identifica comandos com múltiplas ações |
| **Resolução de contatos** | Busca inteligente com apelidos ("liga pro pai") |

### 🛠️ Modelos On-Device (LiteRT-LM)
| Capacidade | Descrição |
|---|---|
| **Download em background** | Download real com Pausa/Retomar, velocidade, ETA |
| **Verificação de integridade** | SHA-256 + verificação de compatibilidade |
| **Importação local** | Importa modelos `.task` ou `.litertlm` offline |

### 📱 Controle Total do Dispositivo
| Ação | Exemplos |
|---|---|
| **Sistema** | Brilho, WiFi, Bluetooth, Lanterna, Volume, Screenshot |
| **Comunicação** | Chamadas, SMS, WhatsApp, Email |
| **Produtividade** | Alarmes, Timers, Lembretes, Calendário, Notas |
| **Navegação** | Google Maps, Uber |
| **Mídia** | Tocar música, YouTube, câmera |

### 👁️ Motor de Visão
Captura screenshots via Accessibility API e alimenta LLMs com capacidade de visão para análise de tela em tempo real.

### 🗄️ Sistema de Memória Multi-Camada
- **Working** (contexto atual)
- **Episodic** (resultados de tarefas passadas)
- **Semantic** (fatos e preferências de longo prazo)
- **Procedural** (macros e workflows definidos pelo usuário)

### 🎙️ Interface de Voz
- Wake word offline — diga *"Axiom"* para ativar
- Speech-to-text para comandos mãos-livres
- Text-to-speech com vozes premium

### 🎨 UI Premium
- Design glassmórfico escuro
- Preto profundo + Verde neon (#00FF88) + Roxo (#7C4DFF)
- Animações fluidas com Jetpack Compose
- Modo escuro por padrão

---

## 🔌 Provedores LLM Suportados

| Provedor | Modelos | Tipo |
|---|---|---|
| 🟢 **Google Gemini** | Gemini 2.0 Flash, Pro, Nano | Cloud + On-device |
| 🟣 **Anthropic Claude** | Claude Sonnet 4, Opus 4 | Cloud |
| 🔵 **OpenAI** | GPT-4o, o3 | Cloud |
| ⚡ **Groq** | LLaMA 3, Mixtral | Cloud |
| 🔷 **DeepSeek** | DeepSeek V3, R1 | Cloud |
| 🟠 **Mistral AI** | Mistral Large, Medium | Cloud |
| 🌐 **OpenRouter** | 200+ modelos via API unificada | Cloud |
| 🏠 **Ollama** | Qualquer modelo local | Local |
| 🔧 **Custom OpenAI** | Qualquer endpoint compatível | Self-hosted |

> **Fallback inteligente**: Se seu provedor principal falhar, o Axiom tenta automaticamente o próximo.

---

## ⚡ Instalação

### Pré-requisitos
- **JDK 21+**
- **Android SDK 35** (Android 15)

### Build & Instalar

```bash
git clone https://github.com/Zoni1998/axiom.git
cd axiom
./gradlew assembleDebug
# APK em: app/build/outputs/apk/debug/app-debug.apk
```

---

## 📜 Licença

Apache License 2.0 — mesmo código do OpenDroid original por Yashab Alam.

Fork mantido por [Victor (Zoni1998)](https://github.com/Zoni1998).

---

<p align="center">
  Feito com ❤️ baseado no <a href="https://github.com/yashab-cyber/opendroid"><strong>OpenDroid</strong></a>
</p>
