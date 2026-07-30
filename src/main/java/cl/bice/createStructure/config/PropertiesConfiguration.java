package cl.bice.createStructure.config;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Configuration
public class PropertiesConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesConfiguration.class);
    private static final String FILE_PROPERTIES = "application.properties";
    private static final String PATH_BASE = System.getProperty("user.dir") + System.getProperty("file.separator");

    @Bean
    public Properties propiedadesExterna() {
        String archivoConfiguracion = PATH_BASE + FILE_PROPERTIES;
        File file = new File(archivoConfiguracion);

        try (InputStream io = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(io, StandardCharsets.UTF_8)) {
            Properties prop = new Properties();
            prop.load(reader);
            printConfiguracion(prop);
            return prop;
        } catch (IOException e) {
            printInstrucciones();
            createFileProperties();
            throw new Error("Favor complete los datos del archivo: " + FILE_PROPERTIES + " y vuelva a ejecutar");
        }
    }

    private void printConfiguracion(Properties prop) {
        logger.info("=== Configuración cargada ===");
        logger.info("karate.project.root : {}", prop.getProperty("karate.project.root", "[NO CONFIGURADO]"));
        logger.info("namespace           : {}", prop.getProperty("namespace", "baas"));
        logger.info("api.type            : {}", prop.getProperty("api.type", "auto"));
        String curl = prop.getProperty("curl", "");
        if (!curl.isEmpty()) {
            logger.info("curl                : {} ...", curl.substring(0, Math.min(80, curl.length())));
        } else {
            logger.info("curl                : [vacío — configurar antes de generar]");
        }
        logger.info("============================");
    }

    private void printInstrucciones() {
        logger.info("=== INSTRUCCIONES ===");
        logger.info("Complete el archivo application.properties con:");
        logger.info("  karate.project.root = ruta absoluta al proyecto Karate destino");
        logger.info("  namespace           = baas | obapis");
        logger.info("  api.type            = auto | obapi | services");
        logger.info("  curl                = el cURL del endpoint a testear");
        logger.info("====================");
    }

    private void createFileProperties() {
        try {
            String file = PATH_BASE + FILE_PROPERTIES;
            List<String> lines = new ArrayList<>();
            lines.add("##==============================================================");
            lines.add("## CONFIGURACION DEL GENERADOR DE FEATURES KARATE");
            lines.add("##==============================================================");
            lines.add("");
            lines.add("##--- RUTAS ---");
            lines.add("karate.project.root=");
            lines.add("pathRelativeFileOrFolderProcess=");
            lines.add("");
            lines.add("##--- IDENTIFICACION DEL SERVICIO ---");
            lines.add("namespace=baas");
            lines.add("api.type=auto");
            lines.add("");
            lines.add("##--- CURL ---");
            lines.add("curl=");
            lines.add("");
            lines.add("##--- AZURE DEVOPS (opcional) ---");
            lines.add("azureUser=");
            lines.add("azurePass=");
            lines.add("azureMock=true");
            lines.add("azureUrlCreateIssue=https://TU-DOMINIO-AZURE/rest/api/2/issue/");
            lines.add("azureUrlTransitionIssue=https://TU-DOMINIO-AZURE/rest/api/2/issue/{issueKey}/transitions");
            lines.add("azureTransitionId=21");
            lines.add("azureBrowse=https://TU-DOMINIO-AZURE/browse/");
            lines.add("customField_10506=10957");
            lines.add("customField_10507=10959");
            lines.add("customField_11105=11411");
            lines.add("customLabels=");
            lines.add("customPriority=10005");
            lines.add("systemKey=");
            lines.add("artefactId=");
            lines.add("");
            lines.add("##--- TIMEOUT ---");
            lines.add("connection.timeout=15000");
            lines.add("request.timeout=15000");
            lines.add("read.timeout=15000");
            FileUtils.writeLines(new File(file), lines);
        } catch (Exception e) {
            logger.error("Error al crear {}", FILE_PROPERTIES, e);
        }
    }
}
