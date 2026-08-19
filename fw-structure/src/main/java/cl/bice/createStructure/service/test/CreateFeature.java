package cl.bice.createStructure.service.test;

import cl.bice.createStructure.to.create.OperationTO;
import cl.bice.createStructure.to.create.ServiceKarateTO;
import cl.bice.createStructure.utils.KarateSentence;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreateFeature {


    public void execute(ServiceKarateTO serviceKarateTO) throws IOException {

        String folderFeature =  File.separator + "structure" + File.separator + "feature" + File.separator;
        String folderFeatureFuntional =  File.separator + "funtional" + File.separator + "feature" + File.separator;

        List<String> linesFeature;
        List<String> linesFeature1;


        for(OperationTO operation: serviceKarateTO.getOperations()) {
            linesFeature = new ArrayList<>();
            linesFeature.addAll(getHeaderFeature(serviceKarateTO.getTagName(), operation, "structure"));
            linesFeature.addAll(getScenarioBodyEmpty(serviceKarateTO.getTagName(), operation));
            linesFeature.addAll(getScenarioBodyNotField(serviceKarateTO.getTagName(), operation));
            linesFeature.addAll(getScenarioBodyMax(serviceKarateTO.getTagName(), operation));
            linesFeature.addAll(getScenarioValidacionesXtras(serviceKarateTO.getTagName(), operation));
            CreateStructureFolder.createFile(folderFeature + operation.getOperationId() + ".feature", linesFeature);
            CreateStructureFolder.updateEnvFile("env_dev.json", serviceKarateTO.getTagName(),  serviceKarateTO.getOperations().get(0).getPath().replace("qa","dev"));
            CreateStructureFolder.updateEnvFile("env_qa.json", serviceKarateTO.getTagName(),  serviceKarateTO.getOperations().get(0).getPath().replace("dev","qa"));
            CreateStructureFolder.updateEnvFile("env_prod.json", serviceKarateTO.getTagName(),  serviceKarateTO.getOperations().get(0).getPath().replace("dev","prod").replace("qa","prod"));


            linesFeature1 = new ArrayList<>();
            linesFeature1.addAll(getHeaderFeature(serviceKarateTO.getTagName(), operation, "funtional"));
            linesFeature1.addAll(getScenarioFuntionalSucess(serviceKarateTO.getTagName(), operation));
            CreateStructureFolder.createFile(folderFeatureFuntional + operation.getOperationId() + ".feature", linesFeature1);
        }
    }

    private List<String> getHeaderFeature(String nameService, OperationTO operation,String typeProof) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("@%s @%sAll @%sStructure @%s @%s_%s @Regression",
                System.getenv("nameSpace"),
                System.getenv("nameSpace"),
                nameService,
                nameService,
                nameService,
                operation.getOperationId()));
        lines.add(String.format("Feature: %s - %s",nameService, operation.getOperationId()));
        lines.add("        Background:");
        lines.add("             * def util = Java.type('integration.Utils')");
        //lines.add("             * def variableAleatoria = util.getRut('Aleatorio')");
        lines.add(String.format("              And json body = call read('classpath:integration/features/"+System.getenv("nameSpace")+"/"+typeProof+"/request/%s-request.js')",
                nameService,
                operation.getOperationId()));
        lines.add("            * def amb = karate.read('classpath:integration/env/env_' + karate.env + '.json')");
        lines.add(String.format("            Given url amb.path%s", StringUtils.capitalize(nameService)));
        lines.add("             * configure charset = null");
        if(operation.getHeaders().getJsonBody().keySet().contains("Authorization")){
            lines.add("            * def login = karate.callSingle('classpath:"+ System.getenv("GenerarToken")+"')");
            lines.add("            * def token = login.token");
            lines.add("            * def secret = login.secret");

        }
        lines.add("");

        return lines;
    }

    private List<String> getScenarioBodyEmpty(String nameService, OperationTO operationTO) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("        @%s_%s_body_empty", nameService, operationTO.getOperationId()));
        lines.add(String.format(KarateSentence.SCENARIO, operationTO.getOperationId(),"EMPTY"));
        lines.add(KarateSentence.HEADER);
        if(operationTO.getHeaders().getJsonBody().keySet().contains("Authorization")){
            lines.add("            * header Authorization = 'Bearer ' + token");
            lines.add("            * header Cookie = 'secretKey=' + secret");
        }
        lines.add(KarateSentence.EMPTY_BODY);
        lines.add(KarateSentence.BODY);
        lines.add(String.format(KarateSentence.METHOD, operationTO.getHttpMethod().name()));
        lines.add(KarateSentence.STATUS);
        //lines.add(KarateSentence.RESPONSE);
        lines.add(KarateSentence.RESPONSE_CONTAINS_VALIDATION_EMPTY);
        lines.add(KarateSentence.EXAMPLES);
        lines.add(String.format("                  | read('classpath:integration/features/"+System.getenv("nameSpace")+"/structure/data/%s/testBodyField.json') |",
                nameService, operationTO.getOperationId()));
        lines.add("");

        return lines;
    }

    private List<String> getScenarioBodyNotField(String nameService, OperationTO operationTO) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("        @%s_body_null", nameService));
        lines.add(String.format(KarateSentence.SCENARIO, operationTO.getOperationId(),"NULL"));
        lines.add(KarateSentence.HEADER);
        if(operationTO.getHeaders().getJsonBody().keySet().contains("Authorization")){
            lines.add("            * header Authorization = 'Bearer ' + token");
            lines.add("            * header Cookie = 'secretKey=' + secret");
        }
        lines.add(KarateSentence.REMOVE_BODY);
        lines.add(KarateSentence.BODY);
        lines.add(String.format(KarateSentence.METHOD, operationTO.getHttpMethod().name()));
        lines.add(KarateSentence.STATUS);
        //lines.add(KarateSentence.RESPONSE);
        lines.add(KarateSentence.RESPONSE_CONTAINS_VALIDATION_EMPTY);
        lines.add(KarateSentence.EXAMPLES);
        lines.add(String.format("                  | read('classpath:integration/features/"+System.getenv("nameSpace")+"/structure/data/%s/testBodyField.json') |",
                nameService, operationTO.getOperationId()));
        lines.add("");

        return lines;
    }

    private List<String> getScenarioBodyMax(String nameService, OperationTO operationTO) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("        @%s_body_border_line", nameService));
        lines.add(String.format(KarateSentence.SCENARIO, operationTO.getOperationId(),""));
        lines.add(KarateSentence.HEADER);
        if(operationTO.getHeaders().getJsonBody().keySet().contains("Authorization")){
            lines.add("            * header Authorization = 'Bearer ' + token");
            lines.add("            * header Cookie = 'secretKey=' + secret");
        }
        lines.add(KarateSentence.SET_BODY);
        lines.add(KarateSentence.BODY);
        lines.add(String.format(KarateSentence.METHOD, operationTO.getHttpMethod().name()));
        lines.add(KarateSentence.STATUS);
        lines.add(KarateSentence.RESPONSE);
        lines.add(KarateSentence.RESPONSE_CONTAINS);
        lines.add(KarateSentence.EXAMPLES);
        lines.add(String.format("                  | read('classpath:integration/features/"+System.getenv("nameSpace")+"/structure/data/%s/testBodyBorderLine.json') |",
                nameService, operationTO.getOperationId()));
        lines.add("");

        return lines;
    }

    private List<String> getScenarioValidacionesXtras(String nameService, OperationTO operationTO) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("        @%s_body_validacion_extra", nameService));
        lines.add(String.format(KarateSentence.SCENARIO, operationTO.getOperationId(),""));
        lines.add(KarateSentence.HEADER);
        if(operationTO.getHeaders().getJsonBody().keySet().contains("Authorization")){
            lines.add("            * header Authorization = 'Bearer ' + token");
            lines.add("            * header Cookie = 'secretKey=' + secret");
        }
        lines.add(KarateSentence.SET_BODY_NUM);
        lines.add(KarateSentence.BODY);
        lines.add(String.format(KarateSentence.METHOD, operationTO.getHttpMethod().name()));
        lines.add(KarateSentence.STATUS);
        lines.add(KarateSentence.RESPONSE);
        lines.add(KarateSentence.RESPONSE_CONTAINS);
        lines.add(KarateSentence.EXAMPLES);
        lines.add(String.format("                  | read('classpath:integration/features/"+System.getenv("nameSpace")+"/structure/data/%s/testBodyValidacionesCampos.json') |",
                nameService, operationTO.getOperationId()));
        lines.add("");

        return lines;
    }

    private List<String> getScenarioFuntionalSucess(String nameService, OperationTO operationTO) {
        List<String> lines1 = new ArrayList<>();
        lines1.add(String.format("        @%s_funtional_success", nameService));
        lines1.add(String.format(KarateSentence.SCENARIO, operationTO.getOperationId(),""));
        lines1.add(KarateSentence.HEADER);
        if(operationTO.getHeaders().getJsonBody().keySet().contains("Authorization")){
            lines1.add("            * header Authorization = 'Bearer ' + token");
            lines1.add("            * header Cookie = 'secretKey=' + secret");
        }
        //lines.add(KarateSentence.SET_BODY_NUM);
        lines1.add(KarateSentence.BODY);
        lines1.add(String.format(KarateSentence.METHOD, operationTO.getHttpMethod().name()));
        lines1.add(KarateSentence.STATUS);
        lines1.add(KarateSentence.RESPONSE_CONTAINS_SUCCESS);
        //lines.add(KarateSentence.RESPONSE_CONTAINS_DEEP);
        lines1.add(KarateSentence.EXAMPLES);
        lines1.add(String.format("                  | read('classpath:integration/features/"+System.getenv("nameSpace")+"/funtional/data/%s/testBodyFuntional.json') |",
                nameService, operationTO.getOperationId()));
        lines1.add("");

        return lines1;
    }

    private String getPath(String pathService) {

        StringBuilder string = new StringBuilder();
        String[] paths = pathService.split("/");
        for(String path: paths) {
            if(path.isEmpty()) {
                continue;
            }

            if(path.contains("{partyId}")) {
                string.append(String.format("'%s',", path.replace("{partyId}", "012641526")));
            } else if(path.contains("{currentAccountId}")) {
                string.append(String.format("'%s',", path.replace("{currentAccountId}", "111111111111")));
            } else {
                string.append(String.format("'%s',", path));
            }
        }

        return string.toString();
    }
}
