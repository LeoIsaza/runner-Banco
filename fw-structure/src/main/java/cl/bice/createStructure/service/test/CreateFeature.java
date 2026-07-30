package cl.bice.createStructure.service.test;

import cl.bice.createStructure.service.SwaggerReader;
import cl.bice.createStructure.to.create.OperationTO;
import cl.bice.createStructure.to.create.ServiceKarateTO;
import cl.bice.createStructure.utils.KarateSentence;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Genera, por cada operación, 3 capas de test — igual a como ya lo hacía tu
 * proyecto real ("updated.zip"): functional / performance / structure — cada
 * una con su carpeta de datos ("data.{operationId}") separada de su feature.
 *
 * Por operación genera:
 *   features/{ns}/functional/data.{opId}/{opId}.json   (fila con el caso 200)
 *   features/{ns}/functional/feature/{opId}.feature      (200 + schema + headers + 500)
 *   features/{ns}/functional/request/{opId}Body.json     (body real, literal)
 *   features/{ns}/performance/data.{opId}/{opId}.json    (30 filas)
 *   features/{ns}/performance/feature/{opId}.feature
 *   features/{ns}/structure/data.{opId}/{opId}.json      (1 fila por campo x caso de borde)
 *   features/{ns}/structure/feature/{opId}.feature
 *   simulations/{ns}/{OpId}Simulation.scala
 *   utils/steps/get_token.feature  (solo OBAPI)
 */
public class CreateFeature {

    private final CreateStructureFolder folder;

    public CreateFeature(CreateStructureFolder folder) {
        this.folder = folder;
    }

    // ── ENTRY POINT ─────────────────────────────────────────────────────────────

    public void execute(ServiceKarateTO serviceKarateTO, String namespace, String apiType, String ambiente) throws IOException {
        for (OperationTO operation : serviceKarateTO.getOperations()) {
            String method   = operation.getHttpMethod().name().toLowerCase();
            String METHOD   = method.toUpperCase();
            String opId     = operation.getOperationId();
            String urlPath  = operation.getPath();
            boolean isObapi = "obapi".equalsIgnoreCase(apiType);
            String envKey   = deriveEnvKey(urlPath, isObapi);
            String opTag    = opId.replace("-", "_").toLowerCase();

            System.out.println("[CreateFeature] Generando 3 capas (functional/performance/structure): " + opId + " [" + apiType + "]");

            // 0. Estructura de carpetas
            folder.createStructureFolder(namespace, opId);

            PathInfo pathInfo = buildKaratePath(urlPath);
            List<String> commonHeaders = buildScenarioHeaders(isObapi, operation);
            List<FieldSpec> fields = extractFieldSpecs(operation);

            // ── FUNCTIONAL ───────────────────────────────────────────────────────
            String bodyFile = folder.functionalRequestPath(namespace) + File.separator + opId + "Body.json";
            folder.createFile(bodyFile, Collections.singletonList(buildInlineBody(operation)));

            List<Map<String, Object>> functionalRows = new ArrayList<>();
            Map<String, Object> okRow = new LinkedHashMap<>();
            for (PathParam p : pathInfo.params) okRow.put(p.name, p.exampleValue);
            okRow.put("_STATUS", 200);
            functionalRows.add(okRow);
            String functionalDataFile = folder.functionalDataPath(namespace, opId) + File.separator + opId + ".json";
            folder.createFile(functionalDataFile, Collections.singletonList(toJsonArray(functionalRows)));

            List<String> functionalFeature = buildFunctionalFeature(
                    namespace, opId, METHOD, opTag, pathInfo, isObapi, envKey, commonHeaders);
            String functionalFeatureFile = folder.functionalFeaturePath(namespace) + File.separator + opId + ".feature";
            folder.createFile(functionalFeatureFile, functionalFeature);

            // ── PERFORMANCE (30 filas) ───────────────────────────────────────────
            List<Map<String, Object>> perfRows = buildPerformanceRows(fields, pathInfo, 30);
            String perfDataFile = folder.performanceDataPath(namespace, opId) + File.separator + opId + ".json";
            folder.createFile(perfDataFile, Collections.singletonList(toJsonArray(perfRows)));

            String perfBodyFile = folder.performanceRequestPath(namespace) + File.separator + opId + "Body.json";
            folder.createFile(perfBodyFile, Collections.singletonList(buildInlineBody(operation)));
            String perfReqRead = "    And request read('classpath:features/" + namespace + "/performance/request/" + opId + "Body.json')";

            List<String> perfFeature = buildPerformanceFeature(
                    namespace, opId, METHOD, opTag, pathInfo, isObapi, envKey, commonHeaders, perfReqRead, fields);
            String perfFeatureFile = folder.performanceFeaturePath(namespace) + File.separator + opId + ".feature";
            folder.createFile(perfFeatureFile, perfFeature);

            // ── STRUCTURE (borde por campo x tipo) ───────────────────────────────
            List<Map<String, Object>> structureRows = buildStructureRows(fields, pathInfo);
            String structureDataFile = folder.structureDataPath(namespace, opId) + File.separator + opId + ".json";
            folder.createFile(structureDataFile, Collections.singletonList(toJsonArray(structureRows)));

            String structureBodyFile = folder.structureRequestPath(namespace) + File.separator + opId + "Body.json";
            folder.createFile(structureBodyFile, Collections.singletonList(buildInlineBody(operation)));
            String structureReqRead = "    And request read('classpath:features/" + namespace + "/structure/request/" + opId + "Body.json')";

            List<String> structureFeature = buildStructureFeature(
                    namespace, opId, METHOD, opTag, pathInfo, isObapi, envKey, commonHeaders, structureReqRead, fields);
            String structureFeatureFile = folder.structureFeaturePath(namespace) + File.separator + opId + ".feature";
            folder.createFile(structureFeatureFile, structureFeature);

            // ── SIMULATION (Gatling) ─────────────────────────────────────────────
            String simFile = folder.simulationsPath(namespace) + File.separator
                    + CreateStructureFolder.toPascalCase(opId) + "Simulation.scala";
            if (!new File(simFile).exists()) {
                folder.createFile(simFile, buildSimulation(namespace, opId));
            }

            // ── TOKEN (solo OBAPI) ────────────────────────────────────────────────
            if (isObapi) {
                String tokenFile = folder.utilsStepsPath() + File.separator + "get_token.feature";
                if (!new File(tokenFile).exists()) {
                    folder.createFile(tokenFile, buildGetTokenFeature(envKey));
                }
            }

            // ── ENV ───────────────────────────────────────────────────────────────
            folder.updateEnv(ambiente, envKey, extractOrigin(urlPath));
        }
    }

    // ── FUNCTIONAL FEATURE ───────────────────────────────────────────────────────

    private List<String> buildFunctionalFeature(String namespace, String opId, String METHOD, String opTag,
            PathInfo pathInfo, boolean isObapi, String envKey, List<String> commonHeaders) {
        List<String> lines = new ArrayList<>();

        lines.add(isObapi
                ? String.format(KarateSentence.FEATURE_TAG_OBAPI, opTag, METHOD.toLowerCase())
                : String.format(KarateSentence.FEATURE_TAG_BAAS, opTag, METHOD.toLowerCase()));
        lines.add(String.format(KarateSentence.FEATURE_DECLARATION, opId, METHOD) + " (functional)");
        lines.add("");
        lines.add(KarateSentence.BG_OPEN);
        lines.add(KarateSentence.BG_SSL_TRUST_ALL);
        lines.add(String.format(KarateSentence.BG_URL, envKey));
        if (isObapi) lines.add(KarateSentence.BG_TOKEN_FEATURE);
        lines.add("");

        String reqRead  = "    And request read('classpath:features/" + namespace + "/functional/request/" + opId + "Body.json')";
        String dataRead = "      | read('classpath:features/" + namespace + "/functional/data." + opId + "/" + opId + ".json') |";

        lines.add(KarateSentence.SEP);
        lines.add("# 200 + SCHEMA + HEADERS — camino feliz con el body real del cURL");
        lines.add(KarateSentence.SEP);
        lines.add("");
        lines.add(KarateSentence.TAG_TEST_CASE);
        lines.add(String.format(KarateSentence.TAG_ADO, opTag, "200"));
        lines.add(String.format(KarateSentence.SCENARIO_OUTLINE, opId, "exitoso - 200"));
        lines.addAll(commonHeaders);
        lines.add(String.format(KarateSentence.GIVEN_PATH, pathInfo.karatePathString));
        lines.add(reqRead);
        lines.add(String.format(KarateSentence.WHEN_METHOD, METHOD));
        lines.add(KarateSentence.THEN_STATUS);
        lines.add("    And match response == '#present'");
        lines.add(KarateSentence.AND_MATCH_HEADER);
        lines.add(KarateSentence.EXAMPLES);
        lines.add(dataRead);
        lines.add("");
        lines.add("  # 'match response == \"#present\"' es una aserción genérica y segura (no inventa");
        lines.add("  # nombres de campo). Si tenés el contrato real (swaggerFile), reemplazala por");
        lines.add("  # 'match response == schema' con el schema real del servicio.");
        lines.add("");

        lines.add(KarateSentence.SEP);
        lines.add("# 500 — best-effort: ajustá el body/condición según cómo tu servicio dispare un error real");
        lines.add(KarateSentence.SEP);
        lines.add("");
        lines.add(KarateSentence.TAG_TEST_CASE);
        lines.add(String.format(KarateSentence.TAG_ADO, opTag, "500"));
        lines.add(String.format(KarateSentence.SCENARIO_OUTLINE, opId, "error servidor - 500 (ajustar)"));
        lines.addAll(commonHeaders);
        lines.add(String.format(KarateSentence.GIVEN_PATH, pathInfo.karatePathString));
        lines.add(KarateSentence.AND_REQUEST_INLINE);
        lines.add(KarateSentence.DOCSTRING);
        lines.add("      { \"trigger_error\": true }");
        lines.add(KarateSentence.DOCSTRING);
        lines.add(String.format(KarateSentence.WHEN_METHOD, METHOD));
        lines.add(KarateSentence.THEN_STATUS);
        lines.add(KarateSentence.EXAMPLES);
        lines.add("      | _STATUS |");
        lines.add("      | 500     |");

        return lines;
    }

    // ── PERFORMANCE FEATURE ──────────────────────────────────────────────────────

    private List<String> buildPerformanceFeature(String namespace, String opId, String METHOD, String opTag,
            PathInfo pathInfo, boolean isObapi, String envKey, List<String> commonHeaders, String reqReadLine,
            List<FieldSpec> fields) {
        List<String> lines = new ArrayList<>();

        lines.add(String.format(KarateSentence.TAG_PERFORMANCE, opTag));
        lines.add(String.format(KarateSentence.FEATURE_DECLARATION, opId, METHOD) + " (performance)");
        lines.add("");
        lines.add(KarateSentence.BG_OPEN);
        lines.add(KarateSentence.BG_SSL_TRUST_ALL);
        lines.add(String.format(KarateSentence.BG_URL, envKey));
        if (isObapi) lines.add(KarateSentence.BG_TOKEN_FEATURE);
        lines.add("");

        String dataRead = "      | read('classpath:features/" + namespace + "/performance/data." + opId + "/" + opId + ".json') |";

        lines.add("  Scenario Outline: performance - " + opId);
        lines.addAll(commonHeaders);
        lines.add(String.format(KarateSentence.GIVEN_PATH, pathInfo.karatePathString));
        lines.add(reqReadLine);
        lines.addAll(buildSetFieldLines(fields));
        lines.add(String.format(KarateSentence.WHEN_METHOD, METHOD));
        lines.add(KarateSentence.THEN_STATUS);
        lines.add(KarateSentence.EXAMPLES);
        lines.add(dataRead);

        return lines;
    }

    // ── STRUCTURE FEATURE ────────────────────────────────────────────────────────

    private List<String> buildStructureFeature(String namespace, String opId, String METHOD, String opTag,
            PathInfo pathInfo, boolean isObapi, String envKey, List<String> commonHeaders, String reqReadLine,
            List<FieldSpec> fields) {
        List<String> lines = new ArrayList<>();

        lines.add(isObapi
                ? String.format(KarateSentence.FEATURE_TAG_OBAPI, opTag, METHOD.toLowerCase())
                : String.format(KarateSentence.FEATURE_TAG_BAAS, opTag, METHOD.toLowerCase()));
        lines.add(String.format(KarateSentence.FEATURE_DECLARATION, opId, METHOD) + " (structure)");
        lines.add("");
        lines.add(KarateSentence.BG_OPEN);
        lines.add(KarateSentence.BG_SSL_TRUST_ALL);
        lines.add(String.format(KarateSentence.BG_URL, envKey));
        if (isObapi) lines.add(KarateSentence.BG_TOKEN_FEATURE);
        lines.add("");

        String dataRead = "      | read('classpath:features/" + namespace + "/structure/data." + opId + "/" + opId + ".json') |";

        lines.add(KarateSentence.SEP);
        lines.add("# 400 — un caso de borde por cada campo real del body, según su tipo detectado");
        lines.add(KarateSentence.SEP);
        lines.add("");
        lines.add(KarateSentence.TAG_TEST_CASE);
        lines.add(String.format(KarateSentence.TAG_ADO, opTag, "400"));
        lines.add(String.format(KarateSentence.SCENARIO_OUTLINE, opId, "error cliente - 400 (borde por campo)"));
        lines.addAll(commonHeaders);
        lines.add(String.format(KarateSentence.GIVEN_PATH, pathInfo.karatePathString));
        lines.add(reqReadLine);
        lines.addAll(buildSetFieldLines(fields));
        lines.add(String.format(KarateSentence.WHEN_METHOD, METHOD));
        lines.add(KarateSentence.THEN_STATUS);
        lines.add(KarateSentence.EXAMPLES);
        lines.add(dataRead);

        return lines;
    }

    /** Después de leer el body real y válido, sobreescribe cada campo con el valor de esa
     *  fila de Examples (nulo/vacío/tipo real incluido) — evita depender de expresiones
     *  embebidas dentro de un .json externo, que Karate no evalúa igual que en un docstring. */
    private List<String> buildSetFieldLines(List<FieldSpec> fields) {
        List<String> lines = new ArrayList<>();
        for (FieldSpec f : fields) {
            lines.add("    * set request." + f.name + " = " + f.karateVar);
        }
        return lines;
    }

    // ── HEADERS POR ESCENARIO (reales del cURL) ──────────────────────────────────

    private List<String> buildScenarioHeaders(boolean isObapi, OperationTO operation) {
        List<String> h = new ArrayList<>();
        if (isObapi) {
            h.add(KarateSentence.HDR_TOKEN_DEF);
            h.add(KarateSentence.HDR_AUTHORIZATION);
            h.add(KarateSentence.HDR_CONTENT_TYPE);
            h.add(KarateSentence.HDR_X_TARGET_UNIT);
            h.add(KarateSentence.HDR_X_TOKEN_TYPE);
        }
        org.json.JSONObject realHeaders = (operation.getHeaders() != null) ? operation.getHeaders().getJsonBody() : null;
        boolean hasContentType = false;
        if (realHeaders != null) {
            for (String key : realHeaders.keySet()) {
                if ("Content-Type".equalsIgnoreCase(key)) hasContentType = true;
                if (isObapi && "Content-Type".equalsIgnoreCase(key)) continue;
                h.add(String.format(KarateSentence.HDR_GENERIC, key, realHeaders.get(key)));
            }
        }
        if (!isObapi && !hasContentType) {
            h.add(KarateSentence.HDR_CONTENT_TYPE);
        }
        return h;
    }

    // ── PATH BUILDER ────────────────────────────────────────────────────────────

    PathInfo buildKaratePath(String urlPath) {
        int qIdx = urlPath.indexOf('?');
        if (qIdx > 0) urlPath = urlPath.substring(0, qIdx);
        if (urlPath.startsWith("http")) {
            try {
                java.net.URL u = new java.net.URL(urlPath);
                urlPath = u.getPath();
            } catch (Exception e) { /* ignorar */ }
        }

        String[] segments = urlPath.split("/");
        List<String> kParts = new ArrayList<>();
        List<PathParam> params = new ArrayList<>();
        Pattern pathParamPat = Pattern.compile("\\{([^}]+)\\}");

        for (String seg : segments) {
            if (seg.isEmpty()) continue;
            Matcher m = pathParamPat.matcher(seg);
            if (m.matches()) {
                String paramName = "_" + m.group(1).replaceAll("([A-Z])", "_$1").toUpperCase();
                if (!paramName.endsWith("_ID")) paramName += "_ID";
                params.add(new PathParam(paramName, "REPLACE_ME"));
                kParts.add("<" + paramName + ">");
            } else {
                kParts.add("'" + seg + "'");
            }
        }

        String karatePathString = String.join(",", kParts);
        return new PathInfo(karatePathString, params);
    }

    static class PathInfo {
        final String karatePathString;
        final List<PathParam> params;
        PathInfo(String k, List<PathParam> p) { karatePathString = k; params = p; }
    }

    static class PathParam {
        final String name;
        final String exampleValue;
        PathParam(String n, String v) { name = n; exampleValue = v; }
    }

    // ── REQUEST BODY LITERAL (functional) ───────────────────────────────────────

    private String buildInlineBody(OperationTO operation) {
        if (operation.getRequestFuntionalBody() != null
                && operation.getRequestFuntionalBody().getJsonBody() != null) {
            return operation.getRequestFuntionalBody().getJsonBody().toString(2);
        }
        return "{ }";
    }

    // ── CAMPOS + TIPOS (compartido entre performance y structure) ────────────────

    static class FieldSpec {
        final String name;
        final String karateVar;
        final String type;
        final Object validValue;
        FieldSpec(String name, String karateVar, String type, Object validValue) {
            this.name = name; this.karateVar = karateVar; this.type = type; this.validValue = validValue;
        }
    }

    static class BorderCase {
        final String label;
        final Object value;
        BorderCase(String label, Object value) { this.label = label; this.value = value; }
    }

    /** Columnas reservadas del Examples — ningún campo real puede terminar generando este nombre. */
    private static final Set<String> RESERVED_KARATE_VARS =
            new HashSet<>(Arrays.asList("_STATUS", "_CASO", "_ITER"));

    /** "cuentaTransaccion" -> "_CUENTA_TRANSACCION". Si el campo se llama justo "status"/"caso"/"iter"
     *  (choca con una columna reservada del Examples), se le agrega el sufijo _FIELD para no pisarla. */
    private String toKarateVar(String fieldName) {
        String base = "_" + fieldName.replaceAll("([A-Z])", "_$1").toUpperCase().replaceAll("^_+", "");
        return RESERVED_KARATE_VARS.contains(base) ? base + "_FIELD" : base;
    }

    private List<FieldSpec> extractFieldSpecs(OperationTO operation) {
        List<FieldSpec> out = new ArrayList<>();
        if (operation.getRequestBody() == null || operation.getRequestBody().getJsonBody() == null) return out;

        org.json.JSONObject schema = operation.getRequestBody().getJsonBody();
        org.json.JSONObject example = (operation.getRequestFuntionalBody() != null)
                ? operation.getRequestFuntionalBody().getJsonBody() : null;

        Map<String, SwaggerReader.FieldInfo> swaggerFields = SwaggerReader.resolve(
                swaggerUrl(), swaggerFile(), swaggerAutoDiscover(), operation.getPath(),
                operation.getHttpMethod() != null ? operation.getHttpMethod().name() : "POST");

        for (String key : schema.keySet()) {
            Object rawSpec = schema.get(key);
            if (!(rawSpec instanceof org.json.JSONObject)) continue;
            org.json.JSONObject spec = (org.json.JSONObject) rawSpec;

            String type = spec.optString("type", "string");
            if ("object".equals(type)) continue; // fuera de alcance v1

            if (swaggerFields.containsKey(key)) {
                type = swaggerFields.get(key).type;
            }

            String karateVar = toKarateVar(key);
            Object exampleVal = (example != null && example.has(key) && !example.isNull(key)) ? example.get(key) : null;
            Object validValue = (exampleVal != null) ? exampleVal : defaultValidFor(type);
            out.add(new FieldSpec(key, karateVar, type, validValue));
        }
        return out;
    }

    private Object defaultValidFor(String type) {
        switch (type) {
            case "integer": return 1;
            case "boolean": return true;
            case "email":   return "usuario@ejemplo.com";
            case "date":    return "2026-01-01";
            default:        return "valor_ejemplo";
        }
    }

    /** Casos de borde estándar por tipo de dato — 100% genérico, sin nombres de campo específicos. */
    private List<BorderCase> boundaryCasesFor(String type) {
        List<BorderCase> cs = new ArrayList<>();
        switch (type) {
            case "email":
                cs.add(new BorderCase("nulo", org.json.JSONObject.NULL));
                cs.add(new BorderCase("vacio", ""));
                cs.add(new BorderCase("formato_invalido", "sin-arroba"));
                break;
            case "date":
                cs.add(new BorderCase("nulo", org.json.JSONObject.NULL));
                cs.add(new BorderCase("vacio", ""));
                cs.add(new BorderCase("formato_invalido", "32-13-9999"));
                break;
            case "integer":
                cs.add(new BorderCase("nulo", org.json.JSONObject.NULL));
                cs.add(new BorderCase("tipo_invalido", "INVALIDO"));
                cs.add(new BorderCase("negativo", -1));
                break;
            case "boolean":
                cs.add(new BorderCase("nulo", org.json.JSONObject.NULL));
                cs.add(new BorderCase("tipo_invalido", "INVALIDO"));
                break;
            default: // string
                cs.add(new BorderCase("nulo", org.json.JSONObject.NULL));
                cs.add(new BorderCase("vacio", ""));
        }
        return cs;
    }

    // ── FILAS DE DATOS (data.{opId}/{opId}.json) ─────────────────────────────────

    private List<Map<String, Object>> buildStructureRows(List<FieldSpec> fields, PathInfo pathInfo) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (fields.isEmpty()) {
            Map<String, Object> r = new LinkedHashMap<>();
            for (PathParam p : pathInfo.params) r.put(p.name, p.exampleValue);
            r.put("_CASO", "sin_body_parseable");
            r.put("_STATUS", 400);
            rows.add(r);
            return rows;
        }
        for (FieldSpec target : fields) {
            for (BorderCase bc : boundaryCasesFor(target.type)) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (PathParam p : pathInfo.params) row.put(p.name, p.exampleValue);
                for (FieldSpec f : fields) {
                    row.put(f.karateVar, f.name.equals(target.name) ? bc.value : f.validValue);
                }
                row.put("_CASO", target.name + "_" + bc.label);
                row.put("_STATUS", 400);
                rows.add(row);
            }
        }
        return rows;
    }

    private List<Map<String, Object>> buildPerformanceRows(List<FieldSpec> fields, PathInfo pathInfo, int n) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("_ITER", i);
            for (PathParam p : pathInfo.params) row.put(p.name, p.exampleValue);
            for (FieldSpec f : fields) row.put(f.karateVar, f.validValue);
            row.put("_STATUS", 200);
            rows.add(row);
        }
        return rows;
    }

    private String toJsonArray(List<Map<String, Object>> rows) {
        org.json.JSONArray arr = new org.json.JSONArray();
        for (Map<String, Object> row : rows) {
            org.json.JSONObject o = new org.json.JSONObject();
            for (Map.Entry<String, Object> e : row.entrySet()) {
                o.put(e.getKey(), e.getValue());
            }
            arr.put(o);
        }
        return arr.toString(2);
    }

    /** URL explicita a un swagger/OpenAPI publico o interno (env var o application.properties). */
    private String swaggerUrl() {
        String v = System.getenv("swaggerUrl");
        if (v != null && !v.trim().isEmpty()) return v.trim();
        v = System.getProperty("swaggerUrl");
        return v != null ? v.trim() : "";
    }

    /** Ruta a un swagger/OpenAPI local opcional (env var o application.properties). */
    private String swaggerFile() {
        String v = System.getenv("swaggerFile");
        if (v != null && !v.trim().isEmpty()) return v.trim();
        v = System.getProperty("swaggerFile");
        return v != null ? v.trim() : "";
    }

    private boolean swaggerAutoDiscover() {
        String v = System.getenv("swaggerAutoDiscover");
        if (v == null || v.trim().isEmpty()) v = System.getProperty("swaggerAutoDiscover");
        if (v == null || v.trim().isEmpty()) return true;
        return !"false".equalsIgnoreCase(v.trim());
    }

    // ── GATLING SIMULATION ──────────────────────────────────────────────────────

    private List<String> buildSimulation(String namespace, String opId) {
        String cls = CreateStructureFolder.toPascalCase(opId);
        List<String> lines = new ArrayList<>();
        lines.add("package simulations." + namespace);
        lines.add("");
        lines.add("import com.intuit.karate.gatling.PreDef._");
        lines.add("import io.gatling.core.Predef._");
        lines.add("import scala.concurrent.duration._");
        lines.add("");
        lines.add("class " + cls + "Simulation extends Simulation {");
        lines.add("  val protocol = karateProtocol()");
        lines.add("  val scen = scenario(\"" + opId + "\").exec(");
        lines.add("    karateFeature(\"classpath:features/" + namespace + "/performance/feature/" + opId + ".feature\")");
        lines.add("  )");
        lines.add("  // La feature de performance ya trae 30 filas de datos (data." + opId + "/" + opId + ".json);");
        lines.add("  // cada Gatling user ejecuta las 30 filas via el Examples de Karate.");
        lines.add("  setUp(");
        lines.add("    scen.inject(atOnceUsers(1))");
        lines.add("  ).protocols(protocol)");
        lines.add("}");
        return lines;
    }

    // ── GET TOKEN FEATURE ────────────────────────────────────────────────────────

    private List<String> buildGetTokenFeature(String envKey) {
        List<String> lines = new ArrayList<>();
        lines.add("@get_token");
        lines.add("Feature: Obtener token de autenticación OBAPI");
        lines.add("");
        lines.add("  Scenario: get_token");
        lines.add("    * def username = __arg.username");
        lines.add("    * def password = __arg.password");
        lines.add("    Given url host." + envKey);
        lines.add("    And path 'digx-infra','login','v1','appLogin'");
        lines.add("    And header Content-Type = 'application/json'");
        lines.add("    And header X-Token-Type = 'JWT'");
        lines.add("    And request { \"username\": \"#(username)\", \"password\": \"#(password)\" }");
        lines.add("    When method POST");
        lines.add("    Then status 200");
        lines.add("    * def token = response.token.value");
        return lines;
    }

    // ── HELPERS ─────────────────────────────────────────────────────────────────

    private String deriveEnvKey(String urlPath, boolean isObapi) {
        try {
            String host;
            if (urlPath.startsWith("http")) {
                java.net.URL u = new java.net.URL(urlPath);
                host = u.getHost().split("\\.")[0];
            } else {
                return isObapi ? "obapi_qa" : "api_baas";
            }
            Map<String, String> known = new HashMap<>();
            known.put("api-baas-qa-test", "api_baas");
            known.put("api-baas-qa",      "api_baas");
            known.put("obapi-qa",         "obapi_qa");
            return known.getOrDefault(host,
                    host.replaceAll("-qa[^.]*$", "").replace("-", "_") + "_qa");
        } catch (Exception e) {
            return isObapi ? "obapi_qa" : "api_baas";
        }
    }

    private String extractOrigin(String urlPath) {
        try {
            if (!urlPath.startsWith("http")) return urlPath;
            java.net.URL u = new java.net.URL(urlPath);
            String port = u.getPort() > 0 ? ":" + u.getPort() : "";
            return u.getProtocol() + "://" + u.getHost() + port;
        } catch (Exception e) {
            return urlPath;
        }
    }
}
