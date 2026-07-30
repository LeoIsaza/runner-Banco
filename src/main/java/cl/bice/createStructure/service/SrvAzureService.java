/**
 *
 */
package cl.bice.createStructure.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import cl.bice.createStructure.to.CustomfieldTO;
import cl.bice.createStructure.to.FieldTO;
import cl.bice.createStructure.to.IssuetypeTO;
import cl.bice.createStructure.to.PriorityTO;
import cl.bice.createStructure.to.ProjectTO;
import cl.bice.createStructure.to.to.TestCaseTO;
import cl.bice.createStructure.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

import cl.bice.createStructure.to.FeatureTO;
import cl.bice.createStructure.to.IssueTO;
import cl.bice.createStructure.to.ResponseAzureTO;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SrvAzureService {

    private static final Logger logger = LoggerFactory.getLogger(SrvAzureService.class);
    private final RestTemplate restTemplate;
    private final Properties propiedadesExterna;

    @Autowired
    public SrvAzureService(RestTemplate restTemplate, Properties propiedadesExterna) {
        super();
        this.restTemplate = restTemplate;
        this.propiedadesExterna = propiedadesExterna;
    }

    public List<FeatureTO> callService(List<FeatureTO> featureTOs) {

        try {

            String urlAzure = propiedadesExterna.getProperty("azureUrlCreateIssue");
            ResponseEntity<ResponseAzureTO> respuesta = new ResponseEntity<>(getResponseAzureTO(), HttpStatus.OK);;

            String nameTest;
            for (FeatureTO featureTO : featureTOs) {
                for (TestCaseTO testCaseTO : featureTO.getTestCases()) {
                    nameTest = String.format("%s - %s", testCaseTO.getFeature(), testCaseTO.getTestCase());

                    logger.info("Creando el Test: {}", nameTest);

                    IssueTO issueTO = getIssueTO();
                    issueTO.getFields().setSummary(nameTest);
                    issueTO.getFields().setDescription(nameTest);

                    logger.info("url create test azure: {}", urlAzure);
                    logger.info("request: {}", Utils.printJsonModel(issueTO));


                    HttpEntity<IssueTO> httpEntity = new HttpEntity<>(issueTO, getHttpHeaders());

                    if ("false".equalsIgnoreCase(propiedadesExterna.getProperty("azureMock"))) {
                        respuesta = restTemplate.postForEntity(
                                urlAzure, httpEntity, ResponseAzureTO.class);
                    }

                    logger.info("response azure: {}", new Gson().toJson(respuesta.getBody()));

                    logger.info("Creado el Test: {}{}", propiedadesExterna.getProperty("azureBrowse"),
                            respuesta.getBody().getKey());
                    logger.info("Cambiado estado Test: {}", respuesta.getBody().getKey());
                    setTransition(respuesta.getBody().getKey());
                    logger.info("Test: {} Estado: AUTOMATED", respuesta.getBody().getKey());
                    testCaseTO.setIdAzure(respuesta.getBody().getKey());
                }
            }

        } catch (Exception e) {
            logger.error("Error al crear issue", e);
        }

        return featureTOs;

    }

    private ResponseAzureTO getResponseAzureTO() {

        String key = String.valueOf(new Random().nextInt(999999));

        return ResponseAzureTO.builder()
                .id(key)
                .self(propiedadesExterna.getProperty("azureUrlCreateIssue") + key)
                .key(propiedadesExterna.getProperty("systemKey") + "-" + key)
                .build();
    }

    private IssueTO getIssueTO() {
        IssueTO issueTO = new IssueTO();

        FieldTO fieldTO = new FieldTO();
        fieldTO.setCustomfield10506(new CustomfieldTO(propiedadesExterna.getProperty("customField_10506")));
        fieldTO.setCustomfield10507(new CustomfieldTO(propiedadesExterna.getProperty("customField_10507")));
        fieldTO.setCustomfield11105(new CustomfieldTO(propiedadesExterna.getProperty("customField_11105")));
        fieldTO.setDescription("");
        fieldTO.setIssuetype(new IssuetypeTO("Test"));
        fieldTO.setLabels(new ArrayList<>(Arrays
                .asList((propiedadesExterna.getProperty("customLabels") + propiedadesExterna.getProperty("artefactId"))
                        .split(","))));
        fieldTO.setPriority(new PriorityTO(propiedadesExterna.getProperty("customPriority")));
        fieldTO.setProject(new ProjectTO(propiedadesExterna.getProperty("systemKey")));
        fieldTO.setSummary("");

        issueTO.setFields(fieldTO);

        return issueTO;
    }

//    public List<TestCaseTO> callServiceMock(List<TestCaseTO> list) {
//
//        try {
//
//            String nameTest;
//
//            for (TestCaseTO testCaseTO : list) {
//
//                nameTest = String.format("%s - %s",testCaseTO.getFeature(), testCaseTO.getTestCase());
//
//                logger.info("Creando el Test: {}", nameTest);
//
//                IssueTO issueTO = getIssueTO();
//                issueTO.getFields().setSummary(nameTest);
//                issueTO.getFields().setDescription(nameTest);
//
//                logger.info("request azure: {}", Utils.printJsonModel(issueTO));
//
//                Random rand = new Random();
//
//                int key = rand.nextInt(9999);
//
//                logger.info("Creado el Test: {}{}", propiedadesExterna.getProperty("azureBrowse"),
//                        key);
//                logger.info("Cambiado estado Test: {}", key);
//                setTransition(key + "");
//                logger.info("Test: {} Estado: AUTOMATED", key);
//                testCaseTO.setIdAzure(key + "");
//
//
//            }
//
//        } catch (Exception e) {
//            logger.error("Error al crear issue", e);
//        }
//
//        return list;
//
//    }

    private void setTransition(String issueKey) {

        try {

            String urlAzure = propiedadesExterna.getProperty("azureUrlTransitionIssue");
            Map<String, Object> urlVar = new HashMap<>();
            urlVar.put("issueKey",issueKey);
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(urlAzure).uriVariables(urlVar);

            String request = getRequestTransition();
            logger.info("url transition azure: {}", uriBuilder.toUriString());
            logger.info("request azure: {}", request);
            HttpEntity<String> httpEntity = new HttpEntity<>(request, getHttpHeaders());

            Map<String, String> params = getParamRequest(issueKey);

            if("false".equalsIgnoreCase(propiedadesExterna.getProperty("azureMock"))) {
                restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.POST, httpEntity, Void.class);
            }

        } catch (Exception e) {
            logger.error("Error al realizar transicion issue", e);
        }

    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBasicAuth(propiedadesExterna.getProperty("azureUser"),
                propiedadesExterna.getProperty("azurePass"));
        return httpHeaders;
    }

    private String getRequestTransition() {
        return "{\"fields\": {},\"transition\": {\"id\": \"" + propiedadesExterna.getProperty("azureTransitionId") + "\"}}";
    }

    private Map<String, String> getParamRequest(String issueKey) {
        Map<String, String> params = new HashMap<>();
        params.put("issueKey", issueKey);
        return params;
    }

}
