/**
 *
 */
package cl.bice.createStructure.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import cl.bice.createStructure.to.to.ResponseTestTO;
import cl.bice.createStructure.to.to.TestCaseStringTO;
import cl.bice.createStructure.to.to.TestCaseTO;
import cl.bice.createStructure.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cl.bice.createStructure.to.FeatureTO;

@Service
public class CreateTestService {

	private static final Logger logger = LoggerFactory.getLogger(CreateTestService.class);
	private final SrvAzureService srvAzure;
	private final Properties propiedadesExterna;

	@Autowired
	public CreateTestService(SrvAzureService srvAzure, Properties propiedadesExterna) {
		super();
		this.srvAzure = srvAzure;
		this.propiedadesExterna = propiedadesExterna;
	}

	public void create() throws IOException {

		List<FeatureTO> featureTOs = new ArrayList<>();

		try {

			File fileOrFolderProcess = new File(System.getProperty("user.dir")
					+ propiedadesExterna.getProperty("pathRelativeFileOrFolderProcess"));
			processFileOrFolder(featureTOs, fileOrFolderProcess);


			featureTOs = srvAzure.callService(featureTOs);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			for (FeatureTO featureTO : featureTOs) {
				Files.delete(featureTO.getFile().toPath());
				FileUtils.writeLines(featureTO.getFile(), converLines(gson.toJson(featureTO.getTestCases())));
			}

			logger.info("test: {}", Utils.printJsonModel(featureTOs));

			List<String> reports = new ArrayList<>();

			for (FeatureTO featureTO : featureTOs) {
				for (TestCaseTO testCaseTO : featureTO.getTestCases()) {
					reports.add(propiedadesExterna.getProperty("azureBrowse") + testCaseTO.getIdAzure());
				}
			}

			String fileReport = System.getProperty("user.dir") + File.separator + "testCreados_"
					+ getDate() + ".txt";

			FileUtils.writeLines(new File(fileReport), reports);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void updateTest() throws IOException {

		List<FeatureTO> featureTOs = new ArrayList<>();

		try {

			File inputFile = new File(propiedadesExterna.getProperty("pathFileProcess"));

			File outputFile = new File(System.getProperty("user.dir") + "/testUpdate/test.json");

			List<TestCaseTO> testCaseTOList = readFileOldTest(inputFile);



			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			FileUtils.writeLines(outputFile, converLines(gson.toJson(testCaseTOList)));

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private List<String> converLines(String json) {
		String[] lines = json.split("\\n");
		List<String> fileString = Arrays.asList(lines);
		fileString.replaceAll(s -> s.replace("\\u0027", "'"));
		return fileString;
	}

	private void processFileOrFolder(List<FeatureTO> featureTOs, File folder) throws IOException {

		File[] fList = folder.listFiles();

		if (Objects.isNull(fList)) {
			throw new FileNotFoundException("ruta " + folder + " no existe");
		}

		for (File file : fList) {
			if (file.isFile()) {
				if(file.getParent().contains("data")) {
					System.out.println(file);
					featureTOs.add(readFile(file));

				} else {
					System.out.println("no esta en data");
				}


			} else if (file.isDirectory()) {
				processFileOrFolder(featureTOs, file);
			}
		}
	}

	private FeatureTO readFile(File file) throws IOException {
		Gson gson = new Gson();

		Reader reader = Files.newBufferedReader(Paths.get(file.getPath()));

		TestCaseTO[] testCaseTOS = gson.fromJson(reader, TestCaseTO[].class);

		return FeatureTO.builder()
				.file(file)
				.testCases(Arrays.asList(testCaseTOS))
				.build();
	}

	private List<TestCaseTO> readFileOldTest(File file) throws IOException {
		Gson gson = new Gson();
		List<TestCaseTO> testCaseTOList = new ArrayList<>();

		Reader reader = Files.newBufferedReader(Paths.get(file.getPath()));

		TestCaseStringTO[] testCaseStringTOS = gson.fromJson(reader, TestCaseStringTO[].class);

		for (TestCaseStringTO testCaseStringTO: testCaseStringTOS) {
			ResponseTestTO response = ResponseTestTO.builder()
					.codigo(testCaseStringTO.getCodigo())
					.mensaje(testCaseStringTO.getMensaje())
					.build();
			testCaseTOList.add(TestCaseTO.builder()
					.idAzure(testCaseStringTO.getIdAzure())
					.feature(testCaseStringTO.getFeature())
					.testCase(testCaseStringTO.getTestCase())
					.header(testCaseStringTO.getHeader())
					.campo(testCaseStringTO.getCampo())
					.valor(testCaseStringTO.getValor())
					.status(testCaseStringTO.getStatus())
					//.response(gson.toJson(response))
					.build());
		}

		return testCaseTOList;
	}

	public static String getDate() {
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
		return now.format(formatter).replace(":", "-");
	}

}
