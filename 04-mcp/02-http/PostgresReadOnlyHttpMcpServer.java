/// usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS io.modelcontextprotocol.sdk:mcp:1.1.3
//DEPS io.modelcontextprotocol.sdk:mcp-json-jackson3:1.1.3
//DEPS org.eclipse.jetty.ee10:jetty-ee10-servlet:12.1.10
//DEPS org.slf4j:slf4j-simple:2.0.16
//DEPS org.postgresql:postgresql:42.7.4

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// claude mcp add --transport http sqp-ro http://localhost:8181/mcp

void main(String[] args) throws Exception {
    Class.forName("org.postgresql.Driver");

    var jsonMapper = McpJsonDefaults.getMapper();

    var transportProvider =
            HttpServletStreamableServerTransportProvider.builder()
                    .jsonMapper(jsonMapper)
                    .mcpEndpoint("/mcp")
                    .build();

    var executeQueryTool = McpSchema.Tool.builder()
            .name("execute_query")
            .description("Executa uma query SELECT somente leitura no banco SQP (PostgreSQL 16). Apenas SELECT/WITH são permitidos. Retorna no máximo 500 linhas.")
            .inputSchema(jsonMapper, """
                    {
                      "type": "object",
                      "properties": {
                        "query": {
                          "type": "string",
                          "description": "Query SQL (apenas SELECT ou WITH). Ex.: SELECT o.id, o.numero, o.status, o.cliente_id, c.nome FROM ordens o JOIN clientes c ON c.id = o.cliente_id LIMIT 50;"
                        }
                      },
                      "required": ["query"]
                    }
                    """)
            .build();

    McpServer.sync(transportProvider)
            .serverInfo("MCP SQP (read-only PostgreSQL)", "1.0.0")
            .toolCall(executeQueryTool, (exchange, request) -> {
                var arguments = request.arguments();
                var query = arguments != null ? String.valueOf(arguments.get("query")) : null;

                if (query == null || query.isBlank()) {
                    return errorResult("Parâmetro 'query' é obrigatório.");
                }
                if (!isReadOnlyQuery(query)) {
                    return errorResult("Apenas queries SELECT/WITH são permitidas neste MCP.");
                }

                try {
                    var result = runQuery(query);
                    return McpSchema.CallToolResult.builder()
                            .addTextContent(result)
                            .isError(false)
                            .build();
                } catch (SQLException e) {
                    return errorResult("Erro ao executar query: %s".formatted(e.getMessage()));
                }
            })
            .build();

    var jetty = new Server(8181);
    var context = new ServletContextHandler();
    context.setContextPath("/");
    context.addServlet(new ServletHolder(transportProvider), "/mcp/*");
    jetty.setHandler(context);
    jetty.setStopAtShutdown(true);
    jetty.start();
    jetty.join();
}

private static final Pattern READ_ONLY_PATTERN =
        Pattern.compile("^\\s*(select|with)\\b", Pattern.CASE_INSENSITIVE);
private static final Pattern FORBIDDEN_PATTERN =
        Pattern.compile("\\b(insert|update|delete|merge|drop|alter|create|truncate|grant|revoke|exec|execute|call|do|copy|vacuum|reindex|cluster|comment)\\b",
                Pattern.CASE_INSENSITIVE);
private static final int MAX_ROWS = 500;

static boolean isReadOnlyQuery(String query) {
    var stripped = query.replaceAll("--[^\\n]*", " ")
            .replaceAll("/\\*.*?\\*/", " ");
    return READ_ONLY_PATTERN.matcher(stripped).find()
            && !FORBIDDEN_PATTERN.matcher(stripped).find();
}

static String runQuery(String query) throws SQLException {
    var url = "jdbc:postgresql://localhost:5432/sqp";

    try (Connection conn = DriverManager.getConnection(url, "sqp", "sqp_pwd")) {
        conn.setReadOnly(true);
        conn.setAutoCommit(false);

        try (Statement stmt = conn.createStatement()) {
            stmt.setMaxRows(MAX_ROWS);
            stmt.setQueryTimeout(30);

            try (ResultSet rs = stmt.executeQuery(query)) {
                return formatResultSet(rs);
            }
        }
    }
}

static String formatResultSet(ResultSet rs) throws SQLException {
    ResultSetMetaData meta = rs.getMetaData();
    int columnCount = meta.getColumnCount();

    var columns = new ArrayList<String>(columnCount);
    for (int i = 1; i <= columnCount; i++) {
        columns.add(meta.getColumnLabel(i));
    }

    var rows = new ArrayList<List<String>>();
    while (rs.next()) {
        var row = new ArrayList<String>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            Object value = rs.getObject(i);
            row.add(value == null ? "NULL" : value.toString());
        }
        rows.add(row);
    }

    var sb = new StringBuilder();
    sb.append(String.join(" | ", columns)).append('\n');
    sb.append("-".repeat(Math.max(columns.size() * 10, 20))).append('\n');
    for (var row : rows) {
        sb.append(String.join(" | ", row)).append('\n');
    }
    sb.append("\n(").append(rows.size()).append(" linha(s)");
    if (rows.size() == MAX_ROWS) {
        sb.append(" — limite de ").append(MAX_ROWS).append(" atingido");
    }
    sb.append(")");
    return sb.toString();
}

static McpSchema.CallToolResult errorResult(String message) {
    return McpSchema.CallToolResult.builder()
            .addTextContent(message)
            .isError(true)
            .build();
}