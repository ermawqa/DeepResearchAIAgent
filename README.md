# Deep Research Agent (Kotlin)

A CLI-based research assistant that processes user queries and generates structured outputs using OpenAI’s API.

## Features

- Accepts user research queries via CLI or interactive prompt
- Generates structured outputs (Markdown, JSON, raw text)
- Modular architecture for query processing and response handling
- Supports flexible output formats and file export

## Architecture

The system is designed with a modular architecture:

- `ResearchAgent.kt` - core logic and prompt handling
- `OpenAIClient.kt` - API communication using OkHttp
- `OutputUtils.kt` - output formatting and file handling
- `Main.kt` - CLI interface and execution flow

This separation ensures maintainability and extensibility.

## Tech Stack

- Kotlin
- Gradle
- OpenAI API (gpt-3.5-turbo)
- OkHttp
- Jackson

## Running the Application

Set your OpenAI API key as an environment variable before running:

`export OPENAI_API_KEY="sk-your-api-key-here"`
Or on Windows PowerShell, use:

`setx OPENAI_API_KEY "sk-your-api-key-here"`
Run the application:

`./gradlew run`
Your code automatically reads the key from the environment:

`object Secrets {
val OPENAI_API_KEY = System.getenv("OPENAI_API_KEY") ?: "MISSING_KEY"
}`
If the variable is not set, it will print "MISSING_KEY" and fail safely.

Run via terminal with command-line arguments:

`./gradlew run --args="--query 'Explain LangChain' --format markdown"`
Or just run:

`./gradlew run`
And it will prompt:

  `No query provided. Please enter your research question:`
Output Formats

	•	--format raw → prints output to console
	•	--format markdown → saves to research_output.md
	•	--format json → saves to research_output.json

---

## Design decisions

- Used a modular architecture to separate API communication, logic, and output handling
- Implemented CLI interface for flexibility and easy testing
- Structured outputs (Markdown/JSON) to improve usability for downstream processing

## Limitations & Future Work

- No external data retrieval (no RAG)
- No persistent memory between queries

Planned improvements:
- Add retrieval-augmented generation (RAG)
- Introduce context memory for multi-step reasoning
- Add citation support
