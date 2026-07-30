package cl.bice.createStructure.service.test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Crea la estructura de carpetas y archivos en el proyecto Karate destino.
 *
 * "karateRoot" es la carpeta que le pasás por pathRelativeFolder / --project-dir
 * (ej: .../src/test/java/integration). A partir de ahí, por namespace y por
 * operación, arma 3 capas — functional / performance / structure — cada una
 * con su carpeta de datos ("data.{operationId}") y su carpeta de feature:
 *
 *   {karateRoot}/features/{namespace}/functional/data.{opId}/{opId}.json
 *   {karateRoot}/features/{namespace}/functional/feature/{opId}.feature
 *   {karateRoot}/features/{namespace}/functional/request/{opId}Body.json
 *   {karateRoot}/features/{namespace}/performance/data.{opId}/{opId}.json
 *   {karateRoot}/features/{namespace}/performance/feature/{opId}.feature
 *   {karateRoot}/features/{namespace}/structure/data.{opId}/{opId}.json
 *   {karateRoot}/features/{namespace}/structure/feature/{opId}.feature
 *   {karateRoot}/simulations/{namespace}/{OpId}Simulation.scala
 *   {karateRoot}/utils/steps/get_token.feature   (solo OBAPI)
 *   {karateRoot}/env/env_qa.json | env_dev.json
 */
public class CreateStructureFolder {

    private final String karateRoot;

    public CreateStructureFolder(String karateRoot) {
        this.karateRoot = Objects.requireNonNull(karateRoot, "pathRelativeFolder / karate.project.root no puede ser vacío");
    }

    // ── FUNCTIONAL ──────────────────────────────────────────────────────────────

    public String functionalDataPath(String namespace, String opId) {
        return join(karateRoot, "features", namespace, "functional", "data." + opId);
    }

    public String functionalFeaturePath(String namespace) {
        return join(karateRoot, "features", namespace, "functional", "feature");
    }

    public String functionalRequestPath(String namespace) {
        return join(karateRoot, "features", namespace, "functional", "request");
    }

    // ── PERFORMANCE ─────────────────────────────────────────────────────────────

    public String performanceDataPath(String namespace, String opId) {
        return join(karateRoot, "features", namespace, "performance", "data." + opId);
    }

    public String performanceRequestPath(String namespace) {
        return join(karateRoot, "features", namespace, "performance", "request");
    }

    public String performanceFeaturePath(String namespace) {
        return join(karateRoot, "features", namespace, "performance", "feature");
    }

    // ── STRUCTURE ───────────────────────────────────────────────────────────────

    public String structureDataPath(String namespace, String opId) {
        return join(karateRoot, "features", namespace, "structure", "data." + opId);
    }

    public String structureRequestPath(String namespace) {
        return join(karateRoot, "features", namespace, "structure", "request");
    }

    public String structureFeaturePath(String namespace) {
        return join(karateRoot, "features", namespace, "structure", "feature");
    }

    // ── COMUNES ─────────────────────────────────────────────────────────────────

    /**
     * "simulations" NO va dentro de karateRoot (ej: .../src/test/java/integration) —
     * va como HERMANA de esa carpeta: .../src/test/java/simulations/{namespace}/
     */
    public String simulationsPath(String namespace) {
        File parent = new File(karateRoot).getParentFile();
        String base = (parent != null) ? parent.getPath() : karateRoot;
        return join(base, "simulations", namespace);
    }

    public String utilsStepsPath() {
        return join(karateRoot, "utils", "steps");
    }

    public String envPath() {
        return join(karateRoot, "env");
    }

    // ── CREAR ESTRUCTURA ────────────────────────────────────────────────────────

    public void createStructureFolder(String namespace, String opId) {
        mkdirs(functionalDataPath(namespace, opId));
        mkdirs(functionalFeaturePath(namespace));
        mkdirs(functionalRequestPath(namespace));
        mkdirs(performanceDataPath(namespace, opId));
        mkdirs(performanceRequestPath(namespace));
        mkdirs(performanceFeaturePath(namespace));
        mkdirs(structureDataPath(namespace, opId));
        mkdirs(structureRequestPath(namespace));
        mkdirs(structureFeaturePath(namespace));
        mkdirs(simulationsPath(namespace));
        mkdirs(utilsStepsPath());
        mkdirs(envPath());
        System.out.println("[CreateStructureFolder] Estructura (functional/performance/structure) creada en: " + karateRoot);
    }

    // ── ARCHIVOS ────────────────────────────────────────────────────────────────

    public static void createFile(String absolutePath, List<String> lines) throws IOException {
        File file = new File(absolutePath);
        file.getParentFile().mkdirs();
        FileUtils.writeLines(file, "UTF-8", lines);
        System.out.println("[CreateStructureFolder] Creado: " + absolutePath);
    }

    // ── ENV_QA.JSON / ENV_DEV.JSON ────────────────────────────────────────────────
    /**
     * Actualiza env_{ambiente}.json con el nuevo host. Formato: { "host": { "{envKey}": "https://..." } }
     * Nunca borra hosts existentes — solo agrega el nuevo si no está.
     * Genérico: no siembra hosts de ejemplo de ninguna empresa; solo el real del cURL.
     */
    public void updateEnv(String ambiente, String envKey, String hostUrl) throws IOException {
        String fileName = "env_" + (ambiente == null || ambiente.trim().isEmpty() ? "qa" : ambiente.trim().toLowerCase()) + ".json";
        File envFile = new File(envPath() + File.separator + fileName);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Map<String, Object> root;
        if (envFile.exists()) {
            try (Reader reader = Files.newBufferedReader(Paths.get(envFile.getPath()))) {
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                root = gson.fromJson(reader, type);
                if (root == null) root = new LinkedHashMap<>();
            }
        } else {
            root = new LinkedHashMap<>();
        }

        @SuppressWarnings("unchecked")
        Map<String, String> hosts = (Map<String, String>) root.computeIfAbsent("host", k -> new LinkedHashMap<>());

        if (!hosts.containsKey(envKey)) {
            hosts.put(envKey, hostUrl);
            System.out.println("[CreateStructureFolder] " + fileName + ": agregado host." + envKey + " = " + hostUrl);
        } else {
            System.out.println("[CreateStructureFolder] " + fileName + ": host." + envKey + " ya existe, sin cambios.");
        }

        envFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile(envFile, gson.toJson(root), "UTF-8");
    }

    /** Compatibilidad: alias del método anterior fijado a "qa". */
    public void updateEnvQa(String envKey, String hostUrl) throws IOException {
        updateEnv("qa", envKey, hostUrl);
    }

    // ── UTILIDADES ──────────────────────────────────────────────────────────────

    /** Convierte nombre con guiones a camelCase: "my-service" → "myService" */
    public static String toCamelCase(String name) {
        String[] parts = name.split("[-_]");
        if (parts.length == 0) return name;
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(parts[i].substring(0, 1).toUpperCase())
                  .append(parts[i].substring(1));
            }
        }
        return sb.toString();
    }

    /** Convierte a PascalCase: "my-service" → "MyService" */
    public static String toPascalCase(String name) {
        String camel = toCamelCase(name);
        if (camel.isEmpty()) return camel;
        return camel.substring(0, 1).toUpperCase() + camel.substring(1);
    }

    private void mkdirs(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) System.out.println("[CreateStructureFolder] Carpeta creada: " + path);
        }
    }

    private String join(String base, String... parts) {
        StringBuilder sb = new StringBuilder(base);
        for (String part : parts) {
            sb.append(File.separator).append(part);
        }
        return sb.toString();
    }
}
