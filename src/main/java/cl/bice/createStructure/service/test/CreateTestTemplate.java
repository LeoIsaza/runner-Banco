package cl.bice.createStructure.service.test;

import cl.bice.createStructure.service.CurlToCustomFormat;
import cl.bice.createStructure.to.create.ServiceKarateTO;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Entry point del generador de features Karate.
 *
 * Configurar application.properties:
 *   karate.project.root  = /ruta/al/proyecto/karate
 *   namespace            = baas | obapis
 *   api.type             = auto | obapi | services
 *   curl                 = {cURL completo del endpoint}
 *
 * Flujo:
 *   1. Lee el cURL desde application.properties (o variable de entorno "curl")
 *   2. CurlToCustomFormat.convert() → ServiceKarateTO (JSON intermedio)
 *   3. DetectApiType: auto-detecta OBAPI vs Services por URL y headers
 *   4. CreateStructureFolder: crea carpetas en karate.project.root
 *   5. CreateFeature: genera .feature + request JSON + schema + Gatling + get_token
 */
public class CreateTestTemplate {

    public static void main(String[] args) throws Exception {
        Properties props = loadProperties();
        Map<String, String> cli = parseArgs(args);

        // Los argumentos de linea de comandos (--namespace, --project-dir, --curl, --swagger,
        // --api-type) tienen prioridad sobre application.properties; ambos son opcionales.
        // Tambien se aceptan alias por variable de entorno con estos nombres (uso comun
        // en IntelliJ "Edit Configurations" -> Environment variables):
        //   nameSpace           = alias de namespace
        //   pathRelativeFolder  = alias de karate.project.root / --project-dir
        //   ambiente            = qa | dev (default qa) -> elige env_qa.json o env_dev.json
        //   planId / suiteId    = opcionales, solo referencia Azure DevOps, no afectan la generacion
        String curl          = firstNonEmpty(cli.get("curl"), getCurl(props));
        String karateRoot    = firstNonEmpty(cli.get("project-dir"),
                firstNonEmpty(System.getenv("pathRelativeFolder"), props.getProperty("karate.project.root", ""))).trim();
        String namespace     = firstNonEmpty(cli.get("namespace"),
                firstNonEmpty(System.getenv("nameSpace"), props.getProperty("namespace", "baas"))).trim();
        String apiTypeConfig = firstNonEmpty(cli.get("api-type"), props.getProperty("api.type", "auto")).trim();
        String ambiente      = firstNonEmpty(System.getenv("ambiente"), props.getProperty("ambiente", "qa")).trim().toLowerCase();
        if (!ambiente.equals("qa") && !ambiente.equals("dev")) ambiente = "qa";
        System.setProperty("ambiente", ambiente); // para que CreateStructureFolder sepa que env_*.json actualizar

        // swaggerFile es opcional: prioridad env var > --swagger > application.properties.
        // Se expone como System property para que CreateFeature (y SwaggerReader) lo lean.
        String swaggerFromEnv = System.getenv("swaggerFile");
        if (swaggerFromEnv == null || swaggerFromEnv.trim().isEmpty()) {
            String swaggerResolved = firstNonEmpty(cli.get("swagger"), props.getProperty("swaggerFile", "")).trim();
            if (!swaggerResolved.isEmpty()) System.setProperty("swaggerFile", swaggerResolved);
        }

        // swaggerAutoDiscover: por defecto SI intenta buscar el swagger por red si no
        // hay swaggerFile (o no dio match). Se puede apagar con --no-swagger-discover
        // o swaggerAutoDiscover=false en application.properties.
        String autoDiscoverFromEnv = System.getenv("swaggerAutoDiscover");
        if (autoDiscoverFromEnv == null || autoDiscoverFromEnv.trim().isEmpty()) {
            String autoDiscoverResolved = cli.containsKey("no-swagger-discover") ? "false"
                    : firstNonEmpty(cli.get("swagger-auto-discover"), props.getProperty("swaggerAutoDiscover", "true")).trim();
            System.setProperty("swaggerAutoDiscover", autoDiscoverResolved);
        }

        // Validaciones
        if (curl == null || curl.trim().isEmpty()) {
            System.err.println("ERROR: 'curl' no configurado en application.properties");
            System.err.println("       Pega el cURL del endpoint en el campo 'curl='");
            System.exit(1);
        }
        if (karateRoot.isEmpty()) {
            System.err.println("ERROR: 'karate.project.root' (o la variable de entorno 'pathRelativeFolder') no configurado");
            System.err.println("       Ej: karate.project.root=/Users/nombre/proyecto-karate/src/test/java");
            System.exit(1);
        }

        printHeader(karateRoot, namespace, apiTypeConfig);

        // 1. Parsear cURL → ServiceKarateTO
        ServiceKarateTO svc = parseFromCurl(curl);

        // Nombre del componente/endpoint: por defecto se deriva de la URL, pero se puede
        // forzar con --component (o la propiedad "namespace.operation") para evitar nombres
        // raros derivados automaticamente (ej. forzar "apiAbono" en vez de "api-abono").
        String componentOverride = firstNonEmpty(cli.get("component"), props.getProperty("namespace.operation", "")).trim();
        if (!componentOverride.isEmpty()) svc.setName(componentOverride);

        svc.setTagName(CreateStructureFolder.toCamelCase(svc.getName()));

        // 2. Detectar tipo de API
        String apiType = detectApiType(apiTypeConfig, svc, curl);
        svc.setObapi("obapi".equals(apiType));

        System.out.println("  Tipo detectado : " + apiType.toUpperCase());
        System.out.println("  Componente     : " + svc.getName());
        System.out.println("  tagName        : " + svc.getTagName());
        System.out.println("  Operaciones    : " + (svc.getOperations() != null ? svc.getOperations().size() : 0));
        System.out.println("=============================================\n");

        // 3. Generar estructura y archivos
        CreateStructureFolder folder  = new CreateStructureFolder(karateRoot);
        CreateFeature         feature = new CreateFeature(folder);
        feature.execute(svc, namespace, apiType, ambiente);

        System.out.println("\n=============================================");
        System.out.println("✅  GENERACIÓN COMPLETADA");
        System.out.println("    Archivos en: " + karateRoot + "/features/" + namespace + "/{functional,performance,structure}");
        System.out.println("    Commit en rama: {id_historia}_" + svc.getName() + "_1.0");
        System.out.println("=============================================\n");
    }

    // ── ARGUMENTOS CLI (opcionales) ────────────────────────────────────────────────

    /**
     * Soporta --clave=valor y --clave valor. Claves reconocidas:
     * --namespace, --project-dir, --curl, --swagger, --api-type
     */
    static Map<String, String> parseArgs(String[] args) {
        Map<String, String> out = new HashMap<>();
        if (args == null) return out;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a == null || !a.startsWith("--")) continue;
            String key = a.substring(2);
            String val;
            int eq = key.indexOf('=');
            if (eq >= 0) {
                val = key.substring(eq + 1);
                key = key.substring(0, eq);
            } else if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                val = args[++i];
            } else {
                val = "";
            }
            out.put(key, val);
        }
        return out;
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && !a.trim().isEmpty()) return a;
        return b != null ? b : "";
    }

    // ── DETECCIÓN OBAPI vs SERVICES ──────────────────────────────────────────────

    /**
     * Reglas de detección (en orden de prioridad):
     * 1. api.type != auto → usar el valor configurado
     * 2. URL contiene "obapi" → OBAPI
     * 3. cURL contiene X-Token-Type o X-Target-Unit → OBAPI
     * 4. El resto → Services/BaaS
     */
    static String detectApiType(String configured, ServiceKarateTO svc, String curl) {
        if (!"auto".equalsIgnoreCase(configured)) {
            System.out.println("  Tipo forzado   : " + configured);
            return configured.toLowerCase();
        }
        // Por path de la operación
        if (svc.getOperations() != null && !svc.getOperations().isEmpty()) {
            String path = svc.getOperations().get(0).getPath();
            if (path != null && path.toLowerCase().contains("obapi")) {
                System.out.println("  Auto-detect    : OBAPI (URL contiene 'obapi')");
                return "obapi";
            }
        }
        // Por headers del cURL
        if (curl.contains("X-Token-Type") || curl.contains("X-Target-Unit")) {
            System.out.println("  Auto-detect    : OBAPI (headers JWT detectados)");
            return "obapi";
        }
        System.out.println("  Auto-detect    : Services/BaaS");
        return "services";
    }

    // ── PARSEO DEL CURL ──────────────────────────────────────────────────────────

    private static ServiceKarateTO parseFromCurl(String curl) throws Exception {
        System.out.println("[Parser] Procesando cURL...");
        String json = CurlToCustomFormat.convert(curl, true);

        Gson gson  = new Gson();
        ServiceKarateTO svc = gson.fromJson(json, ServiceKarateTO.class);

        // Pasar los JSONObjects complejos (no los maneja Gson bien)
        JSONObject jsonObj    = new JSONObject(json);
        JSONArray  operations = jsonObj.getJSONArray("operations");

        for (int i = 0; i < operations.length(); i++) {
            JSONObject op = (JSONObject) operations.get(i);

            if (!op.isNull("requestBody") && op.getJSONObject("requestBody").has("jsonBody")
                    && !op.getJSONObject("requestBody").isNull("jsonBody")) {
                svc.getOperations().get(i).getRequestBody()
                   .setJsonBody(op.getJSONObject("requestBody").getJSONObject("jsonBody"));
            }
            if (!op.isNull("requestFuntionalBody") && op.get("requestFuntionalBody") instanceof JSONObject) {
                svc.getOperations().get(i).getRequestFuntionalBody()
                   .setJsonBody((JSONObject) op.get("requestFuntionalBody"));
            }
            if (!op.isNull("headers") && op.get("headers") instanceof JSONObject) {
                svc.getOperations().get(i).getHeaders()
                   .setJsonBody((JSONObject) op.get("headers"));
            }
        }

        // Propagar envKey y origin al svc si los tiene
        if (jsonObj.has("envKey"))  svc.setEnvKey(jsonObj.getString("envKey"));
        if (jsonObj.has("origin"))  svc.setOrigin(jsonObj.getString("origin"));
        if (jsonObj.has("isObapi")) svc.setObapi(jsonObj.getBoolean("isObapi"));

        return svc;
    }

    // ── PROPERTIES ──────────────────────────────────────────────────────────────

    private static Properties loadProperties() throws IOException {
        String path = System.getProperty("user.dir") + File.separator + "application.properties";
        File file = new File(path);

        Properties props = new Properties();
        InputStream stream;

        if (file.exists()) {
            System.out.println("[Config] Leyendo: " + path);
            stream = new FileInputStream(file);
        } else {
            System.out.println("[Config] Leyendo desde classpath");
            stream = CreateTestTemplate.class.getClassLoader()
                     .getResourceAsStream("application.properties");
        }

        if (stream == null) throw new FileNotFoundException("No se encontró application.properties");

        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        return props;
    }

    private static String getCurl(Properties props) {
        String fromEnv = System.getenv("curl");
        if (fromEnv != null && !fromEnv.trim().isEmpty()) return fromEnv.trim();
        return props.getProperty("curl", "").trim();
    }

    private static void printHeader(String root, String ns, String apiType) {
        System.out.println("\n=============================================");
        System.out.println(" KARATE FEATURE GENERATOR (generico)");
        System.out.println(" 1 feature por operacion, 6 secciones: performance/200/400/500/schema/header");
        System.out.println("=============================================");
        System.out.println("  Proyecto destino : " + root);
        System.out.println("  Namespace        : " + ns);
        System.out.println("  Tipo configurado : " + apiType);
    }

    @SneakyThrows
    private static String printJsonModel(Object val) {
        JsonMapper json = new JsonMapper();
        json.registerModule(new JavaTimeModule());
        json.registerModule(new JsonOrgModule());
        return json.writeValueAsString(val);
    }
}
