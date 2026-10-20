/// usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS io.modelcontextprotocol.sdk:mcp:1.1.3
//DEPS io.modelcontextprotocol.sdk:mcp-json-jackson3:1.1.3
//DEPS org.eclipse.jetty.ee10:jetty-ee10-servlet:12.1.10
//DEPS org.slf4j:slf4j-nop:2.0.16

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;

import java.util.concurrent.ThreadLocalRandom;

// jbang /home/ubuntu/desenvolvimento/ia-for-devs-tcm-v2/04-mcp/02-http/RandomNumberHttpMcpServer.java
// claude mcp add --transport http random-number http://localhost:8080/mcp
void main(String[] args) throws Exception {
    var jsonMapper = McpJsonDefaults.getMapper();

    var transportProvider =
            HttpServletStreamableServerTransportProvider.builder()
                    .jsonMapper(jsonMapper)
                    .mcpEndpoint("/mcp")
                    .build();

    var tool = McpSchema.Tool.builder()
            .name("get_random_number")
            .description("Gera um número aleatório entre 0 e 999")
            .inputSchema(jsonMapper, "{\"type\":\"object\",\"properties\":{}}")
            .build();

    McpServer.sync(transportProvider)
            .serverInfo("MCP de Número Aleatório", "1.0.0")
            .toolCall(tool, (exchange, request) -> {
                System.out.print("Solicitada a geração de número aleatório: ");
                int output = ThreadLocalRandom.current().nextInt(0, 1000);
                System.out.println(output);
                return McpSchema.CallToolResult.builder()
                        .addTextContent("Número aleatório: %s".formatted(output))
                        .isError(false)
                        .build();
            })
            .build();

    var jetty = new Server(8080);
    var context = new ServletContextHandler();
    context.setContextPath("/");
    context.addServlet(new ServletHolder(transportProvider), "/mcp/*");
    jetty.setHandler(context);
    jetty.setStopAtShutdown(true);
    jetty.start();
    jetty.join();
}
