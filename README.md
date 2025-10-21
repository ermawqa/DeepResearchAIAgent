# Task #1:

- Theme: **Deep Research Agent — Kotlin Implementation**
- My YouTube video with a brief explanation: [Link]
- **What It Does:** This is a **Kotlin-based Deep Research Agent** that:
    - Accepts a research query
    - Sends the query to OpenAI’s ChatGPT (gpt-3.5-turbo) via their API
    - Parses and returns a **structured response**
    - Supports **multiple output formats** (raw, markdown, json)
    - Allows **CLI arguments** and fallback to user prompt
- **How to Run (Gradle Kotlin CLI App)**
    1. Set your **OpenAI API key** as an environment variable before running:


        ```kotlin
        export OPENAI_API_KEY="sk-your-api-key-here"
        ```
        
        Or on Windows PowerShell, use:
        
        ```kotlin
        setx OPENAI_API_KEY "sk-your-api-key-here"
        ```
        
        Run the application:
        
        ```kotlin
        ./gradlew run
        ```
        
        Your code automatically reads the key from the environment:
        
        ```kotlin
        object Secrets {
        val OPENAI_API_KEY = System.getenv("OPENAI_API_KEY") ?: "MISSING_KEY"
        }
        ```
        
        If the variable is not set, it will print "MISSING_KEY" and fail safely.
        
    2. Run via terminal with command-line arguments:
        
        ```kotlin
        ./gradlew run --args="--query 'Explain LangChain' --format markdown"
        ```
        
        Or just run:
        
        ```kotlin
        ./gradlew run
        ```
        
        And it will prompt:
        
        ```kotlin
          No query provided. Please enter your research question:
        ```


**Output Formats**

```
	•	--format raw → prints output to console
	•	--format markdown → saves to research_output.md
	•	--format json → saves to research_output.json
```

**Example Output (Markdown)**

```
## Key Concepts
- LangChain is a framework for chaining LLMs with external tools and memory.

## Tools
- LangChain, VectorDBs, RAG pipelines

## Open Questions
- How to best manage memory and context switching.
```

- **Technologies Used:**
    - Kotlin
    - Gradle
    - OpenAI GPT-3.5-turbo API
    - OkHttp (for HTTP requests)
    - Jackson (for JSON parsing)

- **Agent Architecture**
    - OpenAIClient.kt — sends requests to the API
    - ResearchAgent.kt — core logic for running queries and prompt template for deep research
    - Main.kt — handles CLI, output options, and execution
    - OutputUtils.kt — handles saving to file

- **Reflection:** This project helped me better understand:
    - HTTP request building using OkHttp
    - Prompt engineering for deep topic exploration
    - Structuring agents using clean OOP practices
    - Command-line argument parsing in Kotlin

# Task #2:

### **1. What do you think distinguishes a truly “deep research” agent from a simple “web search + summarize” agent?**

A truly “deep research” agent goes beyond surface-level summarization. While a simple agent might just retrieve top links and summarize them, a deep research agent:

- Understands context, not just keywords
- Connects multiple sources to identify underlying patterns, contradictions, or trends
- Can generate structured insights, not just rephrased content
- Has some form of memory or multistep reasoning, allowing it to refine its responses based on previous queries or subtasks

In short, a deep research agent behaves more like a junior researcher than a search tool.

### **2. If you were to evaluate a research agent like this, how would you approach measuring its quality and effectiveness?**

I’d use a mix of quantitative and qualitative metrics:

- Relevance: Does the answer directly address the question?
- Depth: Does it go beyond obvious facts? Are multiple perspectives included?
- Coherence: Is the information well-structured and logically organized?
- Novelty: Did the agent surface insights or relationships I didn't expect?
- Trustworthiness: Does it cite or imply reliable sources?

### **3. Describe a scenario where your agent would likely fail or struggle.**

Right now, my agent relies only on OpenAI’s LLM with no real-time access to the internet or external databases.

So, it would likely fail in:

- Real-time fact-checking, e.g. “What did the IMF announce yesterday?”
- Highly specialized domains, like legal or medical texts, where citations and accuracy are critical

Also, if the query is ambiguous or lacks context, the LLM might generate a generic or hallucinated response.

### **4. What features or capabilities would you add to enhance your agent and make it work better?**

If I had more time or resources, I would:

- Add retrieval-augmented generation (RAG)

  Combine the LLM with a real-time web search or academic corpus to ground responses.

- Introduce memory or conversation context

  So the agent can track previous questions and refine answers in follow-ups.

- Output citation support

  Show where key insights came from (via link, title, or DOI), like a bibliography.

- Add multi-format export

  Let users export results in Markdown, JSON, PDF, or even slide format.
