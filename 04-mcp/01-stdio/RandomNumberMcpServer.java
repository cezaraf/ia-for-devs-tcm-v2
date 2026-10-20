/// usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS io.modelcontextprotocol.sdk:mcp:1.1.3
//DEPS io.modelcontextprotocol.sdk:mcp-json-jackson3:1.1.3
//DEPS org.slf4j:slf4j-nop:2.0.16

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.concurrent.ThreadLocalRandom;

/*
{"jsonrpc":"2.0","id":0,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}
{"jsonrpc":"2.0","method":"notifications/initialized"}
{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"get_random_number","arguments":{}}}
*/

// claude mcp add random-number -- jbang /home/ubuntu/desenvolvimento/ia-for-devs-tcm-v2/04-mcp/01-stdio/RandomNumberMcpServer.java
void main(String[] args) throws InterruptedException {
    var jsonMapper = McpJsonDefaults.getMapper();
    var transport = new StdioServerTransportProvider(jsonMapper);

    var tool = McpSchema.Tool.builder()
            .name("get_random_number")
            .description("Gera um número aleatório entre 0 e 999")
            .inputSchema(jsonMapper, "{\"type\":\"object\",\"properties\":{}}")
            .build();

    var server = McpServer.sync(transport)
            .serverInfo("MCP de Número Aleatório", "1.0.0")
            .toolCall(tool, (exchange, request) -> {
                int output = ThreadLocalRandom.current().nextInt(0, 1000);
                return McpSchema.CallToolResult.builder()
                        .addTextContent("Número aleatório: %s".formatted(output))
                        .isError(false)
                        .build();
            })
            .build();

    Runtime.getRuntime().addShutdownHook(new Thread(server::closeGracefully));
    Thread.currentThread().join();
}