package cl.bice.createStructure;

import cl.bice.createStructure.service.test.CreateTestTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicación Spring Boot para generar estructuras de test Karate.
 *
 * Modos de uso:
 *  1. Generar feature desde cURL (NUEVO - lineamientos 2026):
 *     Configurar application.properties (karate.project.root + curl) y ejecutar.
 *
 *  2. Crear tests en Azure DevOps (flujo legado):
 *     Configurar pathRelativeFileOrFolderProcess, azureUser, azurePass.
 */
@SpringBootApplication
public class CreateTestAzureApplication {

    public static void main(String[] args) throws Exception {
        // Si se detecta "curl" en properties o env, usar el generador nuevo
        String curlFromEnv = System.getenv("curl");
        String curlFromProp = System.getProperty("curl", "");

        if ((curlFromEnv != null && !curlFromEnv.trim().isEmpty())
                || !curlFromProp.trim().isEmpty()) {
            // Modo: generar feature desde cURL
            System.out.println("[App] Modo: Generación de Feature desde cURL");
            CreateTestTemplate.main(args);
        } else {
            // Modo: procesamiento Azure (legado)
            System.out.println("[App] Modo: Procesamiento Azure DevOps");
            SpringApplication.run(CreateTestAzureApplication.class, args);
        }
    }
}
