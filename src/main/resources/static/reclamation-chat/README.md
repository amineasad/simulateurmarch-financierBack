# Reclamation Chat UI

This chat-like interface lets you send a reclamation (claim) message and receive an automated analysis/response from the Python agent via the Spring Boot API.

## Prerequisites

- Java 17+
- Python available on PATH (for the Python agent)
- Maven (or use Maven Wrapper `mvnw.cmd`) to run Spring Boot

## Run the Backend

1. Ensure Python is installed and accessible as `python`.
2. From the project root:
   - Windows PowerShell: `./mvnw.cmd spring-boot:run`
   - If Maven wrapper fails, install Maven and run: `mvn spring-boot:run`

The API will listen on `http://localhost:8080`.

## Preview the UI (local static server alternative)

If you only want to preview the UI without the backend:

- From `src/main/resources/static`, run `python -m http.server 8080`
- Open `http://localhost:8080/reclamation-chat/index.html`

Note: The Send action will call `/api/reclamations/analyser`; without the Spring Boot app running, you will see an error.

## Use the Chat

- Open `http://localhost:8080/reclamation-chat/index.html`
- Type your reclamation text, press Send
- The UI POSTs `{ "texte": "..." }` to `/api/reclamations/analyser`
- The response shows `reponse_suggeree` or `reponse_agent` from the Python agent

## Troubleshooting

- If the health status shows "Backend: unreachable", ensure the Spring Boot app is running.
- On Windows PowerShell, use `.\n+mvnw.cmd` for the Maven wrapper.
- If the Python agent fails, check the logs in `reclamation_agent.log` and ensure `python` resolves.