# 🧠 Self-Correcting Reasoning LLM (Offline Android App)

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android-brightgreen?style=for-the-badge&logo=android)  
![Language](https://img.shields.io/badge/Language-Kotlin-orange?style=for-the-badge&logo=kotlin)  
![Models](https://img.shields.io/badge/Models-Gemma%203%201B%20%7C%20SmolLM%20135M-blue?style=for-the-badge)  
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

**Learning Self-Correcting Reasoning in LLMs — Fully Offline, No Human Feedback**

</div>

---

## 🚀 Overview

This project is an **offline Android application** that implements a **self-correcting reasoning mechanism** for Large Language Models (LLMs).

It allows models to **detect, analyze, and fix their own mistakes** without relying on human feedback or internet connectivity.

---

## 🎯 Problem Statement

Large Language Models often produce:

- Hallucinated answers  
- Logical inconsistencies  
- Overconfident incorrect outputs  

Traditional solutions depend on **human feedback (RLHF)** which is:

- Expensive  
- Biased  
- Not scalable  

---

## 💡 Solution

This project introduces a **self-correction loop** where the model improves its reasoning iteratively.

### 🔁 Workflow

1. Generate initial reasoning  
2. Analyze confidence signals  
3. Detect potential errors  
4. Regenerate incorrect steps  
5. Repeat until final answer  

---

## 🧠 Models Used

| Model | Size | Purpose |
|------|------|--------|
| **Gemma 3 1B** | ~529MB | High-quality reasoning |
| **SmolLM 135M** | ~100–150MB | Fast & lightweight |

### 🔍 Key Insight

- Larger model → Better reasoning accuracy  
- Smaller model → Faster & efficient  
- Both can perform **self-correction**

---

## ⚙️ Self-Correction Algorithm

```kotlin
fun generateWithSelfCorrection(prompt: String, maxIterations: Int = 3): String {
    var currentPrompt = prompt
    var finalAnswer = ""
    
    for (iteration in 0 until maxIterations) {
        val reasoning = llm.generate(currentPrompt)
        
        if (reasoning.contains("wait") || 
            reasoning.contains("actually") || 
            reasoning.contains("correction")) {
            
            currentPrompt = "$prompt\nPrevious reasoning was incorrect.\nPlease fix it:"
        } 
        
        else if (reasoning.length > 200 || iteration == maxIterations - 1) {
            finalAnswer = reasoning
            break
        } 
        
        else {
            currentPrompt = "$prompt\nStep-by-step reasoning:\n$reasoning\nTherefore:"
        }
    }
    return finalAnswer
}
✨ Features
🤖 Fully Offline AI (No Internet Required)
🔄 Self-Correcting Reasoning Loop
🔀 Multi-Model Support (Gemma + SmolLM)
💬 Chat Interface with Streaming
💾 Local Database (Room)
📥 Model Downloader
⚙️ Custom Settings (Temperature, Tokens)
📤 Export Chat History (JSON)

📱 Architecture
User Input
   ↓
Model Selection (Gemma / SmolLM)
   ↓
Initial Reasoning
   ↓
Confidence Analysis
   ↓
Error Detection
   ↓
Regeneration Loop
   ↓
Final Answer

🛠️ Tech Stack
Language: Kotlin
Platform: Android (API 26+)
Database: Room
Models: Gemma 3 1B, SmolLM 135M
UI: Jetpack Compose / XML

📦 Installation
git clone https://github.com/your-username/self-correcting-llm.git
cd self-correcting-llm

🧪 Research Insight

This project explores:
Can LLMs improve their reasoning without external supervision?

Observations
Models can detect their own mistakes
Keywords like "wait", "actually" indicate correction
Iterative reasoning improves accuracy

🔮 Future Improvements
Entropy-based confidence scoring
Model benchmarking (speed vs accuracy)
Visualization of reasoning steps
Fine-tuned correction triggers

🤝 Contributing
Contributions are welcome!

Fork the repo
Create a new branch
Commit changes
Open a pull request

⭐ Support
If you found this useful:

⭐ Star the repository
🍴 Fork it
🚀 Share it
