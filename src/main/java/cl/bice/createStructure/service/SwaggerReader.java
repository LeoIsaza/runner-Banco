package cl.bice.createStructure.service;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Lector "best-effort" de un documento OpenAPI/Swagger v3, 100% opcional.
 *
 * No depende de swagger-parser (evita agregar dependencias pesadas). Navega el
 * JSON crudo con org.json y solo resuelve los casos mas comunes:
 *   - $ref simple a #/components/schemas/X
 *   - requestBody.content.application/json.schema.properties[*].type / .format
 *   - requestBody...schema.required[]
 *
 * Cuatro fuentes posibles, en este orden de prioridad (la primera que funcione gana):
 *   1. URL explicita             -> propiedad/env "swaggerUrl" (ej: un swagger.json publico)
 *   2. Archivo local explicito   -> propiedad/env "swaggerFile"
 *   3. Descubrimiento remoto     -> si "swaggerAutoDiscover" no esta en "false" (por
 *      defecto SI intenta), hace GET al host del cURL sobre rutas comunes de
 *      swagger/openapi (/v3/api-docs, /v2/api-docs, /swagger.json, etc.)
 *   4. Nada -> el generador sigue funcionando SOLO con lo inferido del cURL
 *      (comportamiento por defecto, nunca rompe la generacion).
 *
 * Soporta tanto OpenAPI 3.0 (requestBody.content...) como Swagger 2.0 clasico
 * (parameters[].in == "body", definitions en vez de components/schemas).
 *
 * Usa java.net.HttpURLConnection (no java.net.http.HttpClient) porque el proyecto
 * apunta a Java 8 -- HttpClient recien aparece en Java 11.
 */
public class SwaggerReader {

    private static final int TIMEOUT_MS = 2000;

    /** Rutas comunes donde suele publicarse el OpenAPI/Swagger de un servicio. */
    private static final String[] COMMON_SWAGGER_PATHS = {
            "/v3/api-docs",
            "/v2/api-docs",
            "/v2/swagger.json",
            "/swagger.json",
            "/swagger/v1/swagger.json",
            "/openapi.json",
            "/api-docs"
    };

    public static class FieldInfo {
        public final String type;      // string | integer | boolean | email | date
        public final boolean required;
        public FieldInfo(String type, boolean required) {
            this.type = type;
            this.required = required;
        }
    }

    /** No instanciable. */
    private SwaggerReader() { }

    // ── ENTRY POINT ─────────────────────────────────────────────────────────────

    /**
     * @param swaggerFile   ruta local opcional (si viene vacia, se salta este paso)
     * @param autoDiscover  si true, intenta descubrir el swagger por red cuando no
     *                      hay swaggerFile o este no dio match
     * @param urlPath       URL completa del endpoint (del cURL)
     * @param method        GET/POST/PUT/...
     */
    /**
     * @param swaggerUrl    URL explicita opcional (ej: https://petstore.swagger.io/v2/swagger.json)
     * @param swaggerFile   ruta local opcional (si viene vacia, se salta este paso)
     * @param autoDiscover  si true, intenta descubrir el swagger por red cuando no
     *                      hay swaggerUrl/swaggerFile o ninguno dio match
     * @param urlPath       URL completa del endpoint (del cURL)
     * @param method        GET/POST/PUT/...
     */
    public static Map<String, FieldInfo> resolve(String swaggerUrl, String swaggerFile, boolean autoDiscover,
            String urlPath, String method) {

        if (swaggerUrl != null && !swaggerUrl.trim().isEmpty()) {
            Map<String, FieldInfo> fromUrl = readFromUrl(swaggerUrl.trim(), urlPath, method);
            if (!fromUrl.isEmpty()) return fromUrl;
        }

        if (swaggerFile != null && !swaggerFile.trim().isEmpty()) {
            Map<String, FieldInfo> fromFile = readFromFile(swaggerFile, urlPath, method);
            if (!fromFile.isEmpty()) return fromFile;
        }

        if (autoDiscover) {
            Map<String, FieldInfo> fromRemote = readFromRemote(urlPath, method);
            if (!fromRemote.isEmpty()) return fromRemote;
        }

        return new LinkedHashMap<>();
    }

    /** Compatibilidad: firma anterior sin swaggerUrl. */
    public static Map<String, FieldInfo> resolve(String swaggerFile, boolean autoDiscover,
            String urlPath, String method) {
        return resolve(null, swaggerFile, autoDiscover, urlPath, method);
    }

    /** Compatibilidad: lectura solo desde archivo local (comportamiento anterior). */
    public static Map<String, FieldInfo> read(String swaggerFile, String urlPath, String method) {
        return readFromFile(swaggerFile, urlPath, method);
    }

    // ── FUENTE 0: URL EXPLICITA ──────────────────────────────────────────────────

    private static Map<String, FieldInfo> readFromUrl(String swaggerUrl, String urlPath, String method) {
        if (urlPath == null || method == null) return new LinkedHashMap<>();
        try {
            String body = httpGet(swaggerUrl);
            if (body == null || body.trim().isEmpty()) {
                System.out.println("[SwaggerReader] swaggerUrl no respondio (o vacio): " + swaggerUrl);
                return new LinkedHashMap<>();
            }
            JSONObject doc = new JSONObject(body);
            Map<String, FieldInfo> out = extractFieldInfo(doc, urlPath, method);
            if (!out.isEmpty()) {
                System.out.println("[SwaggerReader] " + out.size() + " campo(s) enriquecidos desde swaggerUrl: " + swaggerUrl);
            } else {
                System.out.println("[SwaggerReader] swaggerUrl respondio pero no dio match para: " + normalizePath(urlPath));
            }
            return out;
        } catch (Exception e) {
            System.out.println("[SwaggerReader] No se pudo leer swaggerUrl, se continua sin el. Motivo: " + e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    // ── FUENTE 1: ARCHIVO LOCAL ─────────────────────────────────────────────────

    private static Map<String, FieldInfo> readFromFile(String swaggerFile, String urlPath, String method) {
        if (swaggerFile == null || swaggerFile.trim().isEmpty()) return new LinkedHashMap<>();
        if (urlPath == null || method == null) return new LinkedHashMap<>();
        try {
            String content = new String(Files.readAllBytes(Paths.get(swaggerFile.trim())), StandardCharsets.UTF_8);
            Map<String, FieldInfo> out = extractFieldInfo(new JSONObject(content), urlPath, method);
            if (!out.isEmpty()) {
                System.out.println("[SwaggerReader] " + out.size() + " campo(s) enriquecidos desde swaggerFile local");
            } else {
                System.out.println("[SwaggerReader] swaggerFile local no dio match para: " + normalizePath(urlPath));
            }
            return out;
        } catch (Exception e) {
            System.out.println("[SwaggerReader] No se pudo leer swaggerFile local, se continua sin el. Motivo: " + e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    // ── FUENTE 2: DESCUBRIMIENTO REMOTO ─────────────────────────────────────────

    private static Map<String, FieldInfo> readFromRemote(String urlPath, String method) {
        String origin = originOf(urlPath);
        if (origin == null) return new LinkedHashMap<>();

        for (String candidate : COMMON_SWAGGER_PATHS) {
            String fullUrl = origin + candidate;
            try {
                String body = httpGet(fullUrl);
                if (body == null || body.trim().isEmpty()) continue;

                JSONObject doc = new JSONObject(body);
                if (!doc.has("paths")) continue; // no es un documento OpenAPI valido

                Map<String, FieldInfo> out = extractFieldInfo(doc, urlPath, method);
                if (!out.isEmpty()) {
                    System.out.println("[SwaggerReader] Swagger descubierto en " + fullUrl
                            + " — " + out.size() + " campo(s) enriquecidos");
                    return out;
                }
                // Documento valido pero sin match para este endpoint: no sigo probando
                // otras rutas (ya encontramos EL swagger del servicio, solo que este
                // endpoint puntual no esta documentado ahi).
                System.out.println("[SwaggerReader] Swagger encontrado en " + fullUrl
                        + " pero sin match para este endpoint, se continua solo con el cURL");
                return new LinkedHashMap<>();
            } catch (Exception e) {
                // Ruta no existe / no responde / no es JSON valido -> probar la siguiente
            }
        }
        System.out.println("[SwaggerReader] No se encontro swagger remoto en " + origin
                + " (rutas probadas: " + String.join(", ", COMMON_SWAGGER_PATHS) + "), se continua solo con el cURL");
        return new LinkedHashMap<>();
    }

    private static String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");
            conn.setInstanceFollowRedirects(true);

            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) return null;

            try (InputStream is = conn.getInputStream()) {
                return readAll(is);
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Lee un InputStream completo como String UTF-8 (sin InputStream.readAllBytes(), no existe en Java 8). */
    private static String readAll(InputStream is) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = is.read(chunk)) != -1) buffer.write(chunk, 0, n);
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    /** Deja solo el origen (scheme+host+port) de una URL completa. */
    private static String originOf(String urlPath) {
        try {
            if (urlPath == null || !urlPath.startsWith("http")) return null;
            URL u = new URL(urlPath.split("\\?")[0]);
            String port = u.getPort() > 0 ? ":" + u.getPort() : "";
            return u.getProtocol() + "://" + u.getHost() + port;
        } catch (Exception e) {
            return null;
        }
    }

    // ── EXTRACCION COMUN (usada por archivo local y por descubrimiento remoto) ──

    private static Map<String, FieldInfo> extractFieldInfo(JSONObject doc, String urlPath, String method) {
        Map<String, FieldInfo> out = new LinkedHashMap<>();
        try {
            if (!doc.has("paths")) return out;
            JSONObject paths = doc.getJSONObject("paths");

            String targetPath = normalizePath(urlPath);
            String basePath = doc.optString("basePath", "");
            String matchedKey = null;
            for (String key : paths.keySet()) {
                String fullKey = basePath.isEmpty() ? key : (basePath + key);
                if (pathsMatch(normalizePath(fullKey), targetPath)) {
                    matchedKey = key;
                    break;
                }
            }
            if (matchedKey == null) return out;

            JSONObject pathItem = paths.getJSONObject(matchedKey);
            String m = method.toLowerCase();
            if (!pathItem.has(m)) return out;
            JSONObject op = pathItem.getJSONObject(m);

            JSONObject schema = resolveRequestBodySchema(op, doc);
            if (schema == null || !schema.has("properties")) return out;

            Set<String> required = new HashSet<>();
            if (schema.has("required")) {
                for (Object r : schema.getJSONArray("required")) {
                    required.add(String.valueOf(r));
                }
            }

            JSONObject props = schema.getJSONObject("properties");
            for (String field : props.keySet()) {
                Object rawProp = props.get(field);
                if (!(rawProp instanceof JSONObject)) continue;
                JSONObject p = (JSONObject) rawProp;
                out.put(field, new FieldInfo(mapType(p), required.contains(field)));
            }
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
        return out;
    }

    /** OpenAPI 3.0 (requestBody.content...) o Swagger 2.0 (parameters[].in == "body"). */
    private static JSONObject resolveRequestBodySchema(JSONObject op, JSONObject doc) {
        // OpenAPI 3.0
        if (op.has("requestBody")) {
            try {
                JSONObject content = op.getJSONObject("requestBody").getJSONObject("content");
                String mediaType = content.has("application/json") ? "application/json" : content.keys().next();
                JSONObject schema = content.getJSONObject(mediaType).getJSONObject("schema");
                return resolveRef(schema, doc);
            } catch (Exception e) {
                return null;
            }
        }
        // Swagger 2.0 clasico: el body es un parametro mas, con "in": "body"
        if (op.has("parameters")) {
            try {
                for (Object o : op.getJSONArray("parameters")) {
                    JSONObject param = (JSONObject) o;
                    if ("body".equals(param.optString("in", ""))) {
                        JSONObject schema = param.getJSONObject("schema");
                        return resolveRef(schema, doc);
                    }
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static JSONObject resolveRef(JSONObject schema, JSONObject doc) {
        if (schema.has("$ref")) {
            String ref = schema.getString("$ref"); // ej: "#/components/schemas/UserDTO"
            String[] parts = ref.replace("#/", "").split("/");
            JSONObject cur = doc;
            for (String p : parts) {
                if (!cur.has(p)) return schema;
                cur = cur.getJSONObject(p);
            }
            return cur;
        }
        return schema;
    }

    private static String mapType(JSONObject prop) {
        String format = prop.optString("format", "");
        String type   = prop.optString("type", "string");
        if ("email".equalsIgnoreCase(format)) return "email";
        if ("date".equalsIgnoreCase(format) || "date-time".equalsIgnoreCase(format)) return "date";
        if ("integer".equals(type) || "number".equals(type)) return "integer";
        if ("boolean".equals(type)) return "boolean";
        return "string";
    }

    /** Deja solo el path, sin host ni query string. */
    private static String normalizePath(String url) {
        try {
            String p = url;
            if (p.startsWith("http")) {
                p = new URL(p.split("\\?")[0]).getPath();
            } else {
                p = p.split("\\?")[0];
            }
            if (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
            return p;
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * Compara segmento a segmento. Un segmento parametrico en swagger ({id}) o
     * un segmento que "parece" un valor de path-param en el cURL (numero largo
     * o UUID) hacen match contra cualquier valor del otro lado.
     */
    private static boolean pathsMatch(String swaggerPath, String curlPath) {
        String[] a = swaggerPath.split("/");
        String[] b = curlPath.split("/");
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            boolean aParam = a[i].startsWith("{") && a[i].endsWith("}");
            boolean bParam = b[i].matches("\\d{4,}") || b[i].matches("[0-9a-fA-F\\-]{36}");
            if (aParam || bParam) continue;
            if (!a[i].equalsIgnoreCase(b[i])) return false;
        }
        return true;
    }
}
