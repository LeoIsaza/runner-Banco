package cl.bice.createStructure.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PropertiesConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(PropertiesConfiguration.class);
	private static final String FILE_PROPERTIES = "application.properties";
	private static final String FILE_INTRUCTIONS = "instructions.txt";
	private static final String PATH_BASE = System.getProperty("user.dir") + System.getProperty("file.separator");

	@Bean
	public Properties propiedadesExterna() {
		String archivoConfiguracion = PATH_BASE + FILE_PROPERTIES;

		File file = new File(archivoConfiguracion);
		try (InputStream io = new FileInputStream(file);
				Reader reader = new InputStreamReader(io, StandardCharsets.UTF_8);) {
			Properties prop = new Properties();
			prop.load(reader);
			return prop;
		} catch (IOException e) {
			printIntructions();
			createFileProperties();
			createFileInstructions();
			throw new Error("Favor complete los datos del archivo:" + FILE_PROPERTIES + " y vuelva a jecutar");
		}
	}

	private void printIntructions() {
		logger.info("Por favor ingrese los parametros al archvio:" + FILE_PROPERTIES);
		logger.info("artefactId: Corresponde al nombre del servicio");
		logger.info(
				"customField_10506: Corresponde a Tipo de Prueba el codigo 10957 corresponde a una prueba funcional");
		logger.info("customField_10507: Corresponde a Prueba Automatizable el codigo 10959 corresponde a si");
		logger.info("customField_11105: Corresponde a Teams el codigo 11411 corresponde a G.TI Digital - Galaxy");
		logger.info(
				"customLabels: Corresponde a los labels que se desean agregar al Test minimo favor ingresar 1 o varios separados por ,");
		logger.info("customLabels: siempre debe finalizar con ,");
		logger.info("customPriority: Corresponde a la Prioridad el codigo 10005 corresponde a Media");
		logger.info("systemKey: Corresponde al id del sistema en azure SISBICEMX Corresponde a Sistema BICE MX");
		logger.info("azureUser: Corresponde al usuario de azure que se utilizara para crear los Test ej: mmontero");
		logger.info("azurePass: Corresponde a la password del usuario de azure");
		logger.info("azureUrlCreateIssue: Corresponde a la url de la api de azure para crear issues");
		logger.info(
				"azureUrlTransitionIssue: Corresponde a la url de la api de azure realizar las transiciones de una issue");
		logger.info("azureTransitionId: Corresponde al estado del issue el codigo 21 corresponde a AUTOMATED");
		logger.info("azureBrowse: Corresponde a la url de azure para acceder a los issues");
		logger.info(
				"pathRelativeFileOrFolderProcess: Se debe ingresar la ruta absoluta del archivo o carpeta que contiene los .feature a procesar");
		logger.info(
				"Nota1: despues de procesar los archivos .feature se creara un nuevo archvo <nombreArchivo>1.feature, en el cual se agregaran los issues creados");
		logger.info(
				"Nota2: al final de cada ejecucion se creara un nuevo archivo con los link de los issues de tipo Test creados");

	}

	private void createFileInstructions() {

		try {

			String file = PATH_BASE + FILE_INTRUCTIONS;

			List<String> lines = new ArrayList<>();

			lines.add("Por favor ingrese los parametros al archvio:" + FILE_PROPERTIES);
			lines.add("artefactId: Corresponde al nombre del servicio");
			lines.add(
					"customField_10506: Corresponde a Tipo de Prueba el codigo 10957 corresponde a una prueba funcional");
			lines.add("customField_10507: Corresponde a Prueba Automatizable el codigo 10959 corresponde a si");
			lines.add("customField_11105: Corresponde a Teams el codigo 11411 corresponde a G.TI Digital - Galaxy");
			lines.add(
					"customLabels: Corresponde a los labels que se desean agregar al Test minimo favor ingresar 1 o varios separados por ,");
			lines.add("customLabels: siempre debe finalizar con ,");
			lines.add("customPriority: Corresponde a la Prioridad el codigo 10005 corresponde a Media");
			lines.add("systemKey: Corresponde al id del sistema en azure SISBICEMX Corresponde a Sistema BICE MX");
			lines.add("azureUser: Corresponde al usuario de azure que se utilizara para crear los Test ej: mmontero");
			lines.add("azurePass: Corresponde a la password del usuario de azure");
			lines.add("azureUrlCreateIssue: Corresponde a la url de la api de azure para crear issues");
			lines.add(
					"azureUrlTransitionIssue: Corresponde a la url de la api de azure realizar las transiciones de una issue");
			lines.add("azureTransitionId: Corresponde al estado del issue el codigo 21 corresponde a AUTOMATED");
			lines.add("azureBrowse: Corresponde a la url de azure para acceder a los issues");
			lines.add(
					"pathRelativeFileOrFolderProcess: Se debe ingresar la ruta absoluta del archivo o carpeta que contiene los .feature a procesar");
			lines.add(
					"Nota1: despues de procesar los archivos .feature se creara un nuevo archvo <nombreArchivo>1.feature, en el cual se agregaran los issues creados");
			lines.add(
					"Nota2: al final de cada ejecucion se creara un nuevo archivo con los link de los issues de tipo Test creados");

			FileUtils.writeLines(new File(file), lines);

		} catch (Exception e) {
			logger.error("[Sistema] Error al crear archivo: " + FILE_INTRUCTIONS, e);
		}

	}

	private void createFileProperties() {

		try {

			String file = PATH_BASE + FILE_PROPERTIES;

			List<String> lines = new ArrayList<>();
			lines.add("artefactId=");
			lines.add("customField_10506=10957");
			lines.add("customField_10507=10959");
			lines.add("customField_11105=11411");
			lines.add("customLabels=");
			lines.add("customPriority=10005");
			lines.add("systemKey=SISBICEMX");
			lines.add("azureUser=");
			lines.add("azurePass=");
			lines.add("azureUrlCreateIssue=https://azure.bice.cl/rest/api/2/issue/");
			lines.add("azureUrlTransitionIssue=https://azure.bice.cl/rest/api/2/issue/{issueKey}/transitions");
			lines.add("azureTransitionId=21");
			lines.add("azureBrowse=https://azure.bice.cl/browse/");
			lines.add("pathRelativeFileOrFolderProcess=");

			FileUtils.writeLines(new File(file), lines);

		} catch (Exception e) {
			logger.error("[Sistema] Error al crear archivo: " + FILE_PROPERTIES, e);
		}

	}

}
