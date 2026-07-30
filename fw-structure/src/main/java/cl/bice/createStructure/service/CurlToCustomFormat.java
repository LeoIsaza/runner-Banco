package cl.bice.createStructure.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URL;
import java.util.*;
import java.util.regex.*;

/**
 * Convierte un cURL al formato intermedio ServiceKarateTO.
 *
 * Reconoce automáticamente:
 *  - OBAPI  : host contiene "obapi" o headers X-Token-Type / X-Target-Unit presentes
 *  - Services/BaaS : resto
 *
 * Extrae:
 *  - URL completa (host + path)
 *  - Método HTTP
 *  - Headers (filtra Cookie y Authorization — no se hardcodean)
 *  - Body JSON → schema de tipos
 *  - operationId, name, component, namespace
 */
public class CurlToCustomFormat {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Headers que NUNCA se hardcodean en la feature (lineamiento COE QA 2026) */
    private static final Set<String> HEADERS_EXCLUDE = new HashSet<>(Arrays.asList(
            "Cookie", "Authorization", "Accept-Encoding", "Accept-Language",
            "Accept", "User-Agent", "Connection"
    ));

    // ── ENTRY POINT ─────────────────────────────────────────────────────────────

    public static String convert(String curl, boolean strictJson) throws Exception {
        String fullUrl  = extractUrl(curl);
        String method   = extractMethod(curl);
        Map<String, String> headers = extractHeaders(curl);
        String body     = extractBody(curl);

        // Detectar tipo antes de filtrar headers
        boolean isObapi = detectObapi(fullUrl, headers);

        // Filtrar headers — los OBAPI se manejan via get_token (no hardcodear)
        Map<String, String> filteredHeaders = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (!HEADERS_EXCLUDE.contains(e.getKey())) {
                // Para OBAPI, también excluir X-Target-Unit y X-Token-Type
                // (se agregan automáticamente en la feature)
                if (isObapi && (e.getKey().equals("X-Target-Unit")
                        || e.getKey().equals("X-Token-Type"))) continue;
                filteredHeaders.put(e.getKey(), e.getValue());
            }
        }

        // Derivar nombres desde URL
        UrlInfo urlInfo = parseUrl(fullUrl);

        // Parsear body
        JsonNode example = null;
        if (body != null && !body.trim().isEmpty()) {
            try { example = MAPPER.readTree(body); }
            catch (Exception ex) { System.out.println("[CurlToCustomFormat] Body no es JSON válido: " + ex.getMessage()); }
        }

        // Construir schema del body
        ObjectNode jsonBodySchema = MAPPER.createObjectNode();
        if (example != null && example.isObject()) {
            jsonBodySchema.setAll(buildSchema((ObjectNode) example));
        }

        return renderJson(urlInfo, method, filteredHeaders, jsonBodySchema, example, isObapi);
    }

    // ── DETECCIÓN OBAPI ──────────────────────────────────────────────────────────

    public static boolean detectObapi(String url, Map<String, String> headers) {
        if (url != null && url.toLowerCase().contains("obapi")) return true;
        if (headers.containsKey("X-Token-Type"))  return true;
        if (headers.containsKey("X-Target-Unit")) return true;
        return false;
    }

    // ── URL PARSING ─────────────────────────────────────────────────────────────

    static UrlInfo parseUrl(String fullUrl) {
        try {
            // Quitar query string para analizar el path
            String urlNoQuery = fullUrl.contains("?") ? fullUrl.substring(0, fullUrl.indexOf('?')) : fullUrl;
            URL u = new URL(urlNoQuery);
            String host = u.getHost();
            String port = u.getPort() > 0 ? ":" + u.getPort() : "";
            String origin = u.getProtocol() + "://" + host + port;
            String[] segs = u.getPath().split("/");

            boolean isObapi = host.contains("obapi");

            // Detectar vX en el path
            int vIdx = -1;
            for (int i = 0; i < segs.length; i++) {
                if (segs[i].matches("v\\d+")) { vIdx = i; break; }
            }

            // Filtrar segmentos que son path params (numéricos largos o UUIDs)
            // Para el name y component
            List<String> nonParam = new ArrayList<>();
            List<String> paramValues = new ArrayList<>();
            List<String> paramNames = new ArrayList<>();

            for (String seg : segs) {
                if (seg.isEmpty()) continue;
                if (isPathParam(seg)) {
                    // Crear nombre del param basado en el segmento anterior
                    String prev = nonParam.isEmpty() ? "param" : nonParam.get(nonParam.size()-1);
                    String pName = "_" + toSnakeUpper(prev).replaceAll("S$","") + "_ID";
                    paramValues.add(seg);
                    paramNames.add(pName);
                } else {
                    nonParam.add(seg);
                }
            }

            // lastOp = último segmento no-param
            String lastOp = nonParam.isEmpty() ? "operation" : nonParam.get(nonParam.size()-1);

            // component:
            // OBAPI: segmento DESPUÉS de v1 (ej /v1/tdpay/createtdpayIn → tdpay)
            // BaaS:  segmento ANTES de v1  (ej /bice-current-account-atm-withdrawal/v1/... → bice-current-account-atm-withdrawal)
            String component;
            if (isObapi && vIdx != -1 && vIdx + 1 < segs.length && !segs[vIdx+1].isEmpty()) {
                component = segs[vIdx + 1];
            } else if (!isObapi && vIdx > 0) {
                component = segs[vIdx - 1];
            } else if (nonParam.size() >= 2) {
                component = nonParam.get(nonParam.size() - 2);
            } else {
                component = nonParam.isEmpty() ? "service" : nonParam.get(0);
            }

            // envKey desde el host
            String hp = host.split("\\.")[0];
            Map<String, String> km = new HashMap<>();
            km.put("api-baas-qa-test", "api_baas");
            km.put("api-baas-qa",      "api_baas");
            km.put("obapi-qa",         "obapi_qa");
            String envKey = km.getOrDefault(hp,
                    hp.replaceAll("-qa[^.]*$","").replace("-","_") + "_qa");

            return new UrlInfo(fullUrl, origin, u.getPath(), component, lastOp, envKey,
                    isObapi, paramValues, paramNames);

        } catch (Exception e) {
            // Fallback mínimo
            String name = fullUrl.substring(fullUrl.lastIndexOf('/') + 1);
            if (name.contains("?")) name = name.substring(0, name.indexOf('?'));
            return new UrlInfo(fullUrl, fullUrl, "/", name, name, "api_baas",
                    false, Collections.emptyList(), Collections.emptyList());
        }
    }

    private static boolean isPathParam(String seg) {
        return seg.matches("\\d{4,}") || seg.matches("[0-9a-fA-F]{8}-.*");
    }

    private static String toSnakeUpper(String s) {
        return s.replace("-","_").toUpperCase();
    }

    // ── JSON RENDERER ────────────────────────────────────────────────────────────

    private static String renderJson(UrlInfo ui, String method, Map<String, String> headers,
            ObjectNode schema, JsonNode example, boolean isObapi) throws Exception {

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("name",        ui.component);
        root.put("tagName",     null);
        root.put("description", "");
        root.put("isObapi",     isObapi);
        root.put("envKey",      ui.envKey);
        root.put("origin",      ui.origin);

        Map<String, Object> op = new LinkedHashMap<>();
        op.put("operationId",  ui.lastOp);
        op.put("path",         ui.fullUrl);
        op.put("httpMethod",   method);
        op.put("headers",      headers);

        Map<String, Object> reqBody = new LinkedHashMap<>();
        reqBody.put("jsonBody", MAPPER.readValue(MAPPER.writeValueAsString(schema), Map.class));
        if (example != null) {
            reqBody.put("example", MAPPER.readValue(MAPPER.writeValueAsString(example), Map.class));
        }
        op.put("requestBody",          reqBody);
        op.put("requestFuntionalBody", example);

        List<Map<String, Object>> ops = Collections.singletonList(op);
        root.put("operations", ops);

        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    // ── SCHEMA BUILDER ───────────────────────────────────────────────────────────

    private static ObjectNode buildSchema(ObjectNode example) {
        ObjectNode schema = MAPPER.createObjectNode();
        example.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode val = entry.getValue();
            if (val.isObject()) {
                ObjectNode nested = buildSchema((ObjectNode) val);
                nested.put("type", "object");
                schema.set(key, nested);
            } else {
                schema.set(key, inferLeaf(key, val));
            }
        });
        return schema;
    }

    /**
     * Tipos soportados: string | integer | boolean | email | date
     * Deteccion 100% generica (sin nombres de campo de ningun cliente/empresa):
     *  - por el VALOR del ejemplo (formato de correo, formato de fecha, numero, boolean)
     *  - complementado por palabras clave genericas y bilingues en el nombre del campo
     *    (email/correo/mail, fecha/date) que aplican a cualquier API, no a una en particular.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");
    private static final Pattern DATE_PATTERN  = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}([T ][\\d:.Z+-]+)?$|^\\d{2}/\\d{2}/\\d{4}$");
    private static final Set<String> EMAIL_HINTS = new HashSet<>(Arrays.asList("email", "correo", "mail"));
    private static final Set<String> DATE_HINTS  = new HashSet<>(Arrays.asList("fecha", "date"));

    private static ObjectNode inferLeaf(String field, JsonNode val) {
        ObjectNode leaf = MAPPER.createObjectNode();
        String type = detectType(field, val);
        leaf.put("type",         type);
        leaf.put("numberString", "integer".equals(type));
        leaf.put("required",     true);
        leaf.put("minLength",    1);
        leaf.put("maxLength",    100);
        leaf.put("values",       MAPPER.createArrayNode());
        return leaf;
    }

    private static String detectType(String field, JsonNode val) {
        String fLow = field == null ? "" : field.toLowerCase();
        if (val == null || val.isNull()) {
            if (hasHint(fLow, EMAIL_HINTS)) return "email";
            if (hasHint(fLow, DATE_HINTS))  return "date";
            return "string";
        }
        if (val.isBoolean()) return "boolean";
        if (val.isNumber())  return "integer";

        String text = val.asText();
        if (hasHint(fLow, EMAIL_HINTS) || EMAIL_PATTERN.matcher(text).matches()) return "email";
        if (hasHint(fLow, DATE_HINTS)  || DATE_PATTERN.matcher(text).matches())  return "date";
        if (text.matches("^\\d+$") && text.length() <= 10) return "integer";
        return "string";
    }

    private static boolean hasHint(String fieldLower, Set<String> hints) {
        for (String h : hints) if (fieldLower.contains(h)) return true;
        return false;
    }

    // ── CURL PARSERS ─────────────────────────────────────────────────────────────

    private static String extractUrl(String curl) {
        for (Pattern p : Arrays.asList(
                Pattern.compile("--location\\s+'(https?://[^']+)'"),
                Pattern.compile("--location\\s+\"(https?://[^\"]+)\""),
                Pattern.compile("(https?://[^\\s'\"\\\\]+)"))) {
            Matcher m = p.matcher(curl);
            if (m.find()) return m.group(1).trim();
        }
        throw new IllegalArgumentException("No se pudo extraer la URL del cURL");
    }

    private static String extractMethod(String curl) {
        Matcher m = Pattern.compile("-X\\s+['\"']?(\\w+)").matcher(curl);
        if (m.find()) return m.group(1).toUpperCase();
        Matcher m2 = Pattern.compile("--request\\s+['\"']?(\\w+)").matcher(curl);
        if (m2.find()) return m2.group(1).toUpperCase();
        return (curl.contains("--data") || curl.contains("--data-raw")) ? "POST" : "GET";
    }

    private static Map<String, String> extractHeaders(String curl) {
        Map<String, String> h = new LinkedHashMap<>();
        Matcher m = Pattern.compile("--header\\s+'([^']+)'",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(curl);
        while (m.find()) {
            int ci = m.group(1).indexOf(':');
            if (ci > -1) {
                h.put(m.group(1).substring(0, ci).trim(),
                      m.group(1).substring(ci + 1).trim());
            }
        }
        return h;
    }

    private static String extractBody(String curl) {
        // --data '{...}' o --data-raw '{...}'
        Matcher m = Pattern.compile("--data(?:-raw)?\\s+'([\\s\\S]*?)'(?:\\s*$|\\s*--)",
                Pattern.MULTILINE).matcher(curl);
        if (m.find()) return m.group(1).trim();
        // fallback: encontrar primer { hasta último }
        int bi = curl.indexOf('{'); int be = curl.lastIndexOf('}');
        if (bi != -1 && be > bi) return curl.substring(bi, be + 1);
        return null;
    }

    // ── DTO ─────────────────────────────────────────────────────────────────────

    public static class UrlInfo {
        public final String fullUrl, origin, path, component, lastOp, envKey;
        public final boolean isObapi;
        public final List<String> paramValues, paramNames;

        UrlInfo(String fu, String or, String p, String comp, String lo, String ek,
                boolean ob, List<String> pv, List<String> pn) {
            fullUrl=fu; origin=or; path=p; component=comp; lastOp=lo; envKey=ek;
            isObapi=ob; paramValues=pv; paramNames=pn;
        }
    }
}
