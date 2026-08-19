package cl.bice.createStructure.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurlToCustomFormat {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Forzar tipos "Integer" (según tu ejemplo)
    private static final Set<String> FORCE_INTEGER_FIELDS = new HashSet<>(Arrays.asList(
            "beneficiary_account" // puedes añadir "payer_account", "payer_institution", "numeric_reference", etc.
    ));

    // Marcar como numberString (UUIDs u otros campos que quieres tratar como string numérica)
    private static final Set<String> FORCE_NUMBERSTRING_FIELDS = new HashSet<>(Arrays.asList(
            "id_msg", "id"
    ));

    /**
     * Convierte un comando curl al formato requerido.
     * @param curl Comando curl completo
     * @param strictJson true para producir JSON estricto (válido); false para producir el formato no-estricto que pediste
     * @return String con el documento formateado
     */
    public static String convert(String curl, boolean strictJson) throws Exception {
        String url = extractUrl(curl);
        String method = extractMethod(curl);
        LinkedHashMap<String, String> headers = extractHeaders(curl);
        String body = extractBody(curl);

        // Filtrar headers que no quieres en parameters (ej. Cookie)
        headers.remove("Cookie");

// Derivar name y operationId
        String name = extractNameFromUrl(url).indexOf("?") > 0 ? extractNameFromUrl(url).substring(0,extractNameFromUrl(url).indexOf("?")) : extractNameFromUrl(url);
        String operationId = toCamelCase(name);

        // Parsear el body de ejemplo
        JsonNode example = null;
        if (body != null && !body.trim().isEmpty()) {
            example = MAPPER.readTree(body);
        }

        // Construir esquema a partir del ejemplo
        ObjectNode jsonBodySchema = MAPPER.createObjectNode();
        if (example != null && example.isObject()) {
            ObjectNode schema = buildSchemaFromExample((ObjectNode) example);
            jsonBodySchema.setAll(schema);
        }

        // Generar JSON estricto válido
        return renderStrictJson(name, operationId, url, method, headers, jsonBodySchema, example);
    }

    // =================== Renderers ===================

    private static String renderStrictJson(String name, String operationId, String url, String method,
                                           Map<String, String> headers, ObjectNode schema, JsonNode example) throws Exception {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("name", name);
        root.put("tagName", null);
        root.put("description", "");

        Map<String, Object> op = new LinkedHashMap<>();
        op.put("operationId", operationId);
        op.put("path", url);
        op.put("httpMethod", method);
        op.put("headers", headers);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("jsonBody", MAPPER.readValue(MAPPER.writeValueAsString(schema), Map.class));
        if (example != null) {
            requestBody.put("example", MAPPER.readValue(MAPPER.writeValueAsString(example), Map.class));
        }
        op.put("requestBody", requestBody);
        op.put("requestFuntionalBody", example);


        List<Map<String, Object>> ops = new ArrayList<>();
        ops.add(op);
        root.put("operations", ops);

        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    /**
     * Renderiza el formato no-estricto con:
     * - parameters como una lista de líneas "clave":"valor"
     * - una línea con 'Authorization: Bearer ...'
     * - y luego "Authorization": ""
     */
    private static String renderNonStrict(String name, String operationId, String url, String method,
                                          Map<String, String> headers, ObjectNode schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("\t\"name\" : \"").append(name).append("\",\n");
        sb.append("\t\"tagName\" : null,\n");
        sb.append("\t\"description\" : \"\",\n");
        sb.append("\t\"operations\" : [ {\n");
        sb.append("\t\t\"operationId\" : \"").append(operationId).append("\",\n");
        sb.append("\t\t\"path\" : \"").append(url).append("\",\n");
        sb.append("\t\t\"httpMethod\" : \"").append(method).append("\",\n");
        sb.append("\t\t\"parameters\" : [\n").append(headers).append("],\n");

        // Imprimir headers excepto Authorization (para ubicarlo especial) y Content-Type al final si quieres
        String authValue = headers.get("Authorization");
        String contentType = headers.get("Content-Type");

        // Orden sugerida: todos los X-* primero
        List<String> keys = new ArrayList<>(headers.keySet());
        keys.remove("Content-Type");
        keys.sort((a, b) -> {
            boolean ax = a.startsWith("X-");
            boolean bx = b.startsWith("X-");
            if (ax != bx) return ax ? -1 : 1;
            return a.compareToIgnoreCase(b);
        });

        // X-* headers
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            String v = headers.get(k);
            sb.append("\t\t\t\"").append(k).append("\":\"").append(escape(v)).append("\",\n");
        }

        // Content-Type (si existe)
        if (contentType != null) {
            sb.append("\t\t\t\"Content-Type\":\"").append(escape(contentType)).append("\",\n");
        }

        // requestBody.jsonBody (esquema)
        sb.append("\t\t\"requestBody\" : {\n");
        sb.append("\t\t\t\"jsonBody\" : ");
        sb.append(prettyPrint(schema).replaceAll("(?m)^", "\t\t\t")); // sangría
        sb.append("\n\t\t}\n");

        sb.append("\t}]\n");
        sb.append("}\n");
        return sb.toString()
                // Arreglo de comas finales si quedaron (por la línea de Authorization especial)
                .replaceAll(",\\n\\s*'Authorization:", "\n\t\t\t'Authorization:")
                .replaceAll(",\\n\\s*\\t\\t\\]","\\n\t\t]");
    }

    private static String prettyPrint(JsonNode node) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // =================== Parsers ===================

    private static String extractUrl(String curl) {
        // Soporta: curl --location 'URL' o "URL"
        Pattern p = Pattern.compile("--location\\s+(['\"])(https?://[^'\"]+)\\1", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(curl);
        if (m.find()) return m.group(2);

        // Fallback: primera URL
        p = Pattern.compile("(https?://[^\\s'\"\\\\]+)");
        m = p.matcher(curl);
        if (m.find()) return m.group(1);

        throw new IllegalArgumentException("No se pudo extraer la URL del curl.");
    }

    private static String extractMethod(String curl) {
        // curl suele usar --data/--data-raw para POST
        Pattern pX = Pattern.compile("-X\\s+(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)", Pattern.CASE_INSENSITIVE);
        Matcher m = pX.matcher(curl);
        if (m.find()) return m.group(1).toUpperCase(Locale.ROOT);
        return curl.contains("--data") || curl.contains("--data-raw") ? "POST" : "GET";
    }

    private static LinkedHashMap<String, String> extractHeaders(String curl) {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        Pattern p = Pattern.compile("--header\\s+(['\"])([^:'\"]+)\\s*:\\s*(.*?)\\1", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(curl);
        while (m.find()) {
            String key = m.group(2).trim();
            String value = m.group(3).trim();
            headers.put(key, value);
        }
        return headers;
    }

    private static String extractBody(String curl) {
        Pattern p = Pattern.compile("--data(?:-raw)?\\s+(['\"])(\\{[\\s\\S]*?)\\1", Pattern.DOTALL);
        Matcher m = p.matcher(curl);
        if (m.find()) return m.group(2);
        return null;
    }

    // =================== Schema builder ===================

    private static ObjectNode buildSchemaFromExample(ObjectNode example) {
        ObjectNode schema = MAPPER.createObjectNode();

        // Top-level
        example.fields().forEachRemaining(entry -> {
            String field = entry.getKey();
            JsonNode value = entry.getValue();
            if ("body".equals(field) && value.isObject()) {
                // Objeto anidado
                ObjectNode bodyNode = MAPPER.createObjectNode();

                // Recorremos campos internos del body
                value.fields().forEachRemaining(inner -> {
                    String f = inner.getKey();
                    JsonNode v = inner.getValue();
                    bodyNode.set(f, inferScalarSchema(f, v));
                });
                bodyNode.put("type", "object");
                schema.set("body", bodyNode);
            } else if (value.isValueNode()) {
                schema.set(field, inferScalarSchema(field, value));
            }
        });

        return schema;
    }

    private static ObjectNode inferScalarSchema(String fieldName, JsonNode value) {
        ObjectNode leaf = MAPPER.createObjectNode();

        boolean isUuid = value.isTextual() && looksLikeUUID(value.asText());

        boolean numberString = FORCE_NUMBERSTRING_FIELDS.contains(fieldName) || isUuid;
        String type;

        if (FORCE_INTEGER_FIELDS.contains(fieldName)) {
            type = "Integer";
            numberString = false;
        } else {
            // Heurística simple
            if (value.isNumber()) {
                type = "Integer";
            } else if (value.isTextual() && value.asText().matches("^\\d+$") && value.asText().length() <= 10) {
                type = "Integer";
                numberString = false;
            } else {
                type = "string";
            }
        }

        // Enum para msg_name según tu ejemplo
        if ("msg_name".equals(fieldName) && value.isTextual()) {
            leaf.put("numberString", false);
            leaf.set("values", MAPPER.createArrayNode().add(value.asText()));
        } else {
            leaf.put("numberString", numberString);
            leaf.set("values", MAPPER.createArrayNode());
        }

        leaf.put("minLength", 1);
        leaf.put("type", type);
        leaf.put("maxLength", 100);
        leaf.put("required", true);

        return leaf;
    }

    // =================== Helpers ===================

    private static boolean looksLikeUUID(String s) {
        return s.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    private static String extractNameFromUrl(String url) {
        String path = url.replaceAll("^https?://[^/]+", "");
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        int idx = path.lastIndexOf('/');
        return (idx >= 0) ? path.substring(idx + 1) : path;
    }

    private static String toCamelCase(String name) {
        String[] parts = name.split("[-_]");
        if (parts.length == 0) return name;
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            sb.append(parts[i].substring(0, 1).toUpperCase(Locale.ROOT));
            sb.append(parts[i].substring(1));
        }
        return sb.toString();
    }
}
