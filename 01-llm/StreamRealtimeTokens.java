/// usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.2

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

// jbang /data/desenvolvimento/ia-for-devs-v2/01-llm/StreamRealtimeTokens.java
static final String MODEL = "openai/gpt-oss-20b";

void main(String[] args) throws Exception {
    var prompt = "Explique brevemente o que é um token em IA.";

    System.out.println("[PROMPT]: \"%s\"%n".formatted(prompt));
    System.out.println("--- Aguardando resposta da LLM real (Exibindo token por token) ---");

    var jsonMapper = new ObjectMapper();

    var requestBody = jsonMapper.writeValueAsString(Map.of(
            "model", MODEL,
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "stream", true // Define que o retorno será no formato SSE possibilitando interceptar cada token de output individualmente
    ));

    var request = HttpRequest.newBuilder()
            .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
            .header("Authorization", "Bearer " + loadApiKey())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

    try (var client = HttpClient.newHttpClient()) {
        var response = client.send(request, HttpResponse.BodyHandlers.ofLines());

        // Iterando sobre o stream à medida que os pacotes chegam da API
        for (var line : (Iterable<String>) response.body()::iterator) {
            if (!line.startsWith("data: ") || line.equals("data: [DONE]")) continue;

            var chunk = jsonMapper.readTree(line.substring("data: ".length()));
            var token = chunk.path("choices").path(0).path("delta").path("content");

            if (!token.isMissingNode() && !token.isNull()) {
                System.out.print(token.asText());
                Thread.sleep(100); // Simula um pequeno delay para melhor visualização do fluxo de tokens
            }
        }
    }

    System.out.println("\n\n--- Fim do fluxo de tokens ---");
}

// Equivalente ao dotenv: usa a variável de ambiente ou lê OPENROUTER_API_KEY do arquivo .env
static String loadApiKey() throws Exception {
    var fromEnv = System.getenv("OPENROUTER_API_KEY");
    if (fromEnv != null && !fromEnv.isBlank()) return fromEnv;

    var envFile = Path.of(".env");
    if (Files.exists(envFile)) {
        for (var line : Files.readAllLines(envFile)) {
            if (line.startsWith("OPENROUTER_API_KEY=")) {
                return line.substring("OPENROUTER_API_KEY=".length());
            }
        }
    }
    return "";
}
