package cl.bice.createStructure.service.test;

import cl.bice.createStructure.to.create.*;
import cl.bice.createStructure.to.to.HeaderTO;
import cl.bice.createStructure.to.to.TestCaseTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import lombok.SneakyThrows;

import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class CreateTestKarate {

    public static void main(String[] args) throws IOException {
        CreateTestKarate createTestKarate = new CreateTestKarate();
        createTestKarate.execute(getData());
    }

    public void execute(ServiceKarateTO serviceKarateTO) throws IOException {
        System.out.println("");
        System.out.println("----------------------CreateTestKarate-------------------------");
        System.out.println("");
        System.out.println(new Gson().toJson(serviceKarateTO));
        System.out.println("");
        System.out.println("----------------------CreateTestKarate-------------------------");
        System.out.println("");

        //Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String folderTest =  File.separator + "structure" + File.separator + "data" + File.separator;
        String folderTestFuntional =  File.separator + "funtional" + File.separator + "data" + File.separator;


        for (OperationTO operationTO : serviceKarateTO.getOperations()) {
            CreateStructureFolder.createFile(
                    String.format("%s/%s/testBodyField.json", folderTest, operationTO.getOperationId()),
                    //convertLines(gson.toJson(testCaseTOList))
                    convertLines(printJsonModel(getTestCaseRequestBody(operationTO)))
            );
            CreateStructureFolder.createFile(
                    String.format("%s/%s/testBodyBorderLine.json", folderTest, operationTO.getOperationId()),
                    //convertLines(gson.toJson(testCaseTOList))
                    convertLines(printJsonModel(getTestCaseRequestBodyBorderLine(operationTO)))
            );

            CreateStructureFolder.createFile(
                    String.format("%s/%s/testBodyValidacionesCampos.json", folderTest, operationTO.getOperationId()),
                    //convertLines(gson.toJson(testCaseTOList))
                    convertLines(printJsonModel(getTestCasevalidacionesExtras(operationTO)))
            );
            CreateStructureFolder.createFile(
                    String.format("%s/%s/testBodyFuntional.json", folderTestFuntional, operationTO.getOperationId()),
                    //convertLines(gson.toJson(testCaseTOList))
                    convertLines(printJsonModel(getTestCaseFuntionalSucess(operationTO)))
            );
        }
    }

    public List<TestCaseTO> getTestCaseRequestBody(OperationTO operationTO) {
        JSONObject level;
        String type;
        List<TestCaseTO> testCaseTOList = new ArrayList<>();
        if (Objects.isNull(operationTO.getRequestBody())) {
            return testCaseTOList;
        }

        for (Iterator<String> it = operationTO.getRequestBody().getJsonBody().keys(); it.hasNext(); ) {
            String key = it.next();
            Object object = operationTO.getRequestBody().getJsonBody().get(key);


            if (object instanceof JSONObject) {
                level = (JSONObject) object;
                type = level.getString("type");

                if (Objects.equals("object", type)) {
                    testCaseTOList.addAll(parseObject(level, operationTO, key));
                }

                testCaseTOList.add(TestCaseTO.builder()
                        .idAzure("")
                        .feature(operationTO.getOperationId())
                        .testCase(String.format("NOT FIELD - %s", key))
                        .headers(operationTO.getHeaders().getJsonBody())
                        .campo(key)
                        .status(400)
                        .response(getResponse(key, type, null))
                        .build());
            }
        }
        return testCaseTOList;
    }

    private List<TestCaseTO> parseObject(JSONObject level, OperationTO operationTO, String key) {
        List<TestCaseTO> testCaseTOList = new ArrayList<>();
        String type;
        RestrictionTO restrictionTO;
        boolean required;
        Gson gson = new Gson();
        for (Iterator<String> it2 = level.keys(); it2.hasNext(); ) {
            String key2 = it2.next();
            if (Objects.equals("type", key2)) {
                continue;
            }
            type = getType(level, key2);
            if (Objects.equals("object", type)) {
                testCaseTOList.add(TestCaseTO.builder()
                        .idAzure("")
                        .feature(operationTO.getOperationId())
                        .testCase(String.format("NOT FIELD - %s", key + "." + key2))
                        .headers(operationTO.getHeaders().getJsonBody())
                        .campo(key + "." + key2)
                        .status(400)
                        .response(getResponse(key + "." + key2, type, null))
                        .build());
                try {
                    testCaseTOList.addAll(parseObject((JSONObject) level.get(key2), operationTO, key + "." + key2));
                } catch (Exception ex) {
                    System.out.println("Hay un objeto dentro pero no es Json");
                }
            } else if (Objects.equals("array", type)) {
                restrictionTO = gson.fromJson(getObject(level, key2).toString(), RestrictionTO.class);
                testCaseTOList.add(TestCaseTO.builder()
                        .idAzure("")
                        .feature(operationTO.getOperationId())
                        .testCase(String.format("NOT FIELD - %s", key + "." + key2))
                        .headers(operationTO.getHeaders().getJsonBody())
                        .campo(key + "." + key2)
                        .status(400)
                        .response(getResponse(key + "." + key2, type, restrictionTO))
                        .build());

            } else {
                required = getRequired(level, key2);
                //if (required) {
                restrictionTO = gson.fromJson(getObject(level, key2).toString(), RestrictionTO.class);
                testCaseTOList.add(TestCaseTO.builder()
                        .idAzure("")
                        .feature(operationTO.getOperationId())
                        .testCase(String.format("NOT FIELD - %s", key + "." + key2))
                        .headers(operationTO.getHeaders().getJsonBody())
                        .campo(key + "." + key2)
                        .status(400)
                        .response(getResponse(key + "." + key2, type, restrictionTO))
                        .build());
                //}
            }


        }
        return testCaseTOList;
    }

    public List<TestCaseTO> getTestCaseRequestBodyBorderLine(OperationTO operationTO) {
        JSONObject level;
        String type;
        Gson gson = new Gson();
        List<TestCaseTO> testCaseTOList = new ArrayList<>();
        if (Objects.isNull(operationTO.getRequestBody())) {
            return testCaseTOList;
        }

        if (Objects.equals("closeCurrentAccount", operationTO.getOperationId())) {
            System.out.println("in closeCurrentAccount");
        }

        for (Iterator<String> it = operationTO.getRequestBody().getJsonBody().keys(); it.hasNext(); ) {
            String key = it.next();
            Object object = operationTO.getRequestBody().getJsonBody().get(key);


            if (object instanceof JSONObject) {
                level = (JSONObject) object;
                type = level.getString("type");
                if (Objects.equals("object", type)) {
                    System.out.println("Test creado object");
                    testCaseTOList.add(TestCaseTO.builder()
                            .idAzure("")
                            .feature(operationTO.getOperationId())
                            .testCase(String.format("BORDE FIELD - %s", key))
                            .headers(operationTO.getHeaders().getJsonBody())
                            .campo(key)
                            .status(400)
                            .valor("HOL")
                            .response(getResponse(key, type, new RestrictionTO()))
                            .build());
                    testCaseTOList.addAll(parseObjectBorderLine(level, operationTO, key));
                }
            }
        }
        return testCaseTOList;
    }

    public List<TestCaseTO> getTestCasevalidacionesExtras(OperationTO operationTO) {
        JSONObject level;
        String type;
        Gson gson = new Gson();
        List<TestCaseTO> testCaseTOList = new ArrayList<>();
        if (Objects.isNull(operationTO.getRequestBody())) {
            return testCaseTOList;
        }

        if (Objects.equals("closeCurrentAccount", operationTO.getOperationId())) {
            System.out.println("in closeCurrentAccount");
        }

        for (Iterator<String> it = operationTO.getRequestBody().getJsonBody().keys(); it.hasNext(); ) {
            String key = it.next();
            Object object = operationTO.getRequestBody().getJsonBody().get(key);


            if (object instanceof JSONObject) {
                level = (JSONObject) object;
                type = level.getString("type");
                if (Objects.equals("object", type)) {
                    System.out.println("Test creado object");
                    testCaseTOList.addAll(parseObjectvalidacionesExtras(level, operationTO, key));
                }
            }
        }
        return testCaseTOList;
    }

    public List<TestCaseTO> getTestCaseFuntionalSucess(OperationTO operationTO) {
        List<TestCaseTO> testCaseTOList = new ArrayList<>();
        if (Objects.isNull(operationTO.getRequestBody())) {
            return testCaseTOList;
        }

        testCaseTOList.addAll(parseObjectFuntionalSuccess(operationTO));
        return testCaseTOList;
    }

    private List<TestCaseTO> parseObjectBorderLine(JSONObject level, OperationTO operationTO, String key) {
        List<TestCaseTO> testCaseTOList = new ArrayList<>();
        String type;
        RestrictionTO restrictionTO;
        boolean required;
        Gson gson = new Gson();
        for (Iterator<String> it2 = level.keys(); it2.hasNext(); ) {
            try {
                String key2 = it2.next();
                if (Objects.equals("type", key2)) {
                    continue;
                }
                type = getType(level, key2);
                System.out.println(String.format("procesando key: %s, campo: %s, type: %s ", key, key2, type));
                if (Objects.equals("object", type)) {
                    JSONObject level2 = (JSONObject) level.get(key2);
                    System.out.println("Test creado object");
                    testCaseTOList.add(TestCaseTO.builder()
                            .idAzure("")
                            .feature(operationTO.getOperationId())
                            .testCase(String.format("BORDE FIELD - %s", key + "." + key2))
                            .headers(operationTO.getHeaders().getJsonBody())
                            .campo(key + "." + key2)
                            .status(400)
                            .valor("HOL")
                            .response(getResponseStrctureError(key + "." + key2, type, new RestrictionTO()))
                            .build());
                    List<TestCaseTO> testCaseObjectTOList = parseObjectBorderLine(level2, operationTO, String.format("%s.%s", key, key2));
                    if (Objects.nonNull(testCaseObjectTOList) && !testCaseObjectTOList.isEmpty()) {
                        testCaseTOList.addAll(testCaseObjectTOList);
                    }
                } else if (Objects.equals("array", type)) {
                    JSONObject level2 = ((JSONObject) level.getJSONArray(key2).get(0));
                    System.out.println("Test creado array");
                    testCaseTOList.add(TestCaseTO.builder()
                            .idAzure("")
                            .feature(operationTO.getOperationId())
                            .testCase(String.format("BORDE FIELD - %s", key + "." + key2))
                            .headers(operationTO.getHeaders().getJsonBody())
                            .campo(key + "." + key2)
                            .status(400)
                            .valor("HOL")
                            .response(getResponse(key + "." + key2, type, new RestrictionTO()))
                            .build());
                    List<TestCaseTO> testCaseObjectTOList = parseObjectBorderLine(level2, operationTO, String.format("%s.%s[0]", key, key2));
                    if (Objects.nonNull(testCaseObjectTOList) && !testCaseObjectTOList.isEmpty()) {
                        testCaseTOList.addAll(testCaseObjectTOList);
                    }
                } else {
                    required = getRequired(level, key2);
                    //if (required) {
                    restrictionTO = gson.fromJson(getObject(level, key2).toString(), RestrictionTO.class);
                    if (Objects.equals("string", restrictionTO.getType())) {
                        if (Objects.nonNull(restrictionTO.getPattern())) {
                            if (Objects.equals("\\d+", restrictionTO.getPattern())) {
                                System.out.println("Test creado pattern \\d+");
                                testCaseTOList.add(TestCaseTO.builder()
                                        .idAzure("")
                                        .feature(operationTO.getOperationId())
                                        .testCase(String.format("BORDE FIELD - %s", key + "." + key2))
                                        .headers(operationTO.getHeaders().getJsonBody())
                                        .campo(key + "." + key2)
                                        .status(400)
                                        .valor(String.format("'%s'", getValue(restrictionTO.getMinLength(),
                                                restrictionTO.isNumberString())))
                                        .response(getResponse(key + "." + key2, type, restrictionTO))
                                        .build());
                                restrictionTO.setNumberString(true);
                            }
                        }
                        if (Objects.nonNull(restrictionTO.getMinLength()) && restrictionTO.getMinLength() > 0) {
                            System.out.println("Test creado Min");
                            testCaseTOList.add(TestCaseTO.builder()
                                    .idAzure("")
                                    .feature(operationTO.getOperationId())
                                    .testCase(String.format("BORDE FIELD Caracteres especiales- %s", key + "." + key2))
                                    .headers(operationTO.getHeaders().getJsonBody())
                                    .campo(key + "." + key2)
                                    .status(202)
                                    .valor("ñ'")
                                    .response(getResponse(key + "." + key2, type, restrictionTO))
                                    .build());
                        }

                        if (Objects.nonNull(restrictionTO.getMaxLength())) {
                            System.out.println("Test creado Max");
                            testCaseTOList.add(TestCaseTO.builder()
                                    .idAzure("")
                                    .feature(operationTO.getOperationId())
                                    .testCase(String.format("BORDE FIELD Maximos- %s", key + "." + key2))
                                    .headers(operationTO.getHeaders().getJsonBody())
                                    .campo(key + "." + key2)
                                    .status(400)
                                    .valor(String.format("'%s'", getValue(restrictionTO.getMaxLength() + 1,
                                            restrictionTO.isNumberString())))
                                    .response(getResponse(key + "." + key2, type, restrictionTO))
                                    .build());
                        }

                        if (Objects.nonNull(restrictionTO.getValues()) && !restrictionTO.getValues().isEmpty()) {
                            //for(String value : restrictionTO.getValues()) {
                            System.out.println("Test creado Enum");
                            testCaseTOList.add(TestCaseTO.builder()
                                    .idAzure("")
                                    .feature(operationTO.getOperationId())
                                    .testCase(String.format("BORDE FIELD - %s", key + "." + key2))
                                    .headers(operationTO.getHeaders().getJsonBody())
                                    .campo(key + "." + key2)
                                    .status(400)
                                    .valor("HOL")
                                    .response(getResponse(key + "." + key2, type, restrictionTO))
                                    .build());
                            //}
                        }

                        if ((key2.toLowerCase()).contains("Date")) {
                            //for(String value : restrictionTO.getValues()) {
                            System.out.println("Test validando Fecha");
                            testCaseTOList.add(TestCaseTO.builder()
                                    .idAzure("")
                                    .feature(operationTO.getOperationId())
                                    .testCase(String.format("BORDE FIELD Validando Fecha- %s", key + "." + key2))
                                    .headers(operationTO.getHeaders().getJsonBody())
                                    .campo(key + "." + key2)
                                    .status(400)
                                    .valor("123-Entero-")
                                    .response(getResponse(key + "." + key2, type, restrictionTO))
                                    .build());
                            //}
                        }

                        if ((key2.toLowerCase()).contains("email")) {
                            //for(String value : restrictionTO.getValues()) {
                            System.out.println("Test validando Email");
                            testCaseTOList.add(TestCaseTO.builder()
                                    .idAzure("")
                                    .feature(operationTO.getOperationId())
                                    .testCase(String.format("BORDE FIELD Validando Email- %s", key + "." + key2))
                                    .headers(operationTO.getHeaders().getJsonBody())
                                    .campo(key + "." + key2)
                                    .status(400)
                                    .valor("automatizacion@email")
                                    .response(getResponse(key + "." + key2, type, restrictionTO))
                                    .build());
                            //}
                        }
                    }

                    if (Objects.equals("date", restrictionTO.getFormat())) {
                        System.out.println("Test creado Date String");
                        LocalDate localDate = LocalDate.now();
                        DateTimeFormatter dateTimeFormatter1 = DateTimeFormatter.ofPattern("yyyyMMdd");
                        DateTimeFormatter dateTimeFormatter2 = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                        DateTimeFormatter dateTimeFormatter3 = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                        testCaseTOList.add(TestCaseTO.builder()
                                .idAzure("")
                                .feature(operationTO.getOperationId())
                                .testCase(String.format("Fecha String - %s", key + "." + key2))
                                .headers(operationTO.getHeaders().getJsonBody())
                                .campo(key + "." + key2)
                                .status(400)
                                .valor("HOL")
                                .response(getResponse(key + "." + key2, type, restrictionTO))
                                .build());

                        System.out.println("Test creado Date format invalid YYYYMMDD");
                        testCaseTOList.add(TestCaseTO.builder()
                                .idAzure("")
                                .feature(operationTO.getOperationId())
                                .testCase(String.format("BORDE FIELD - %s", key + "." + key2))
                                .headers(operationTO.getHeaders().getJsonBody())
                                .campo(key + "." + key2)
                                .status(400)
                                .valor(String.format("'%s'", localDate.format(dateTimeFormatter1)))
                                .response(getResponse(key + "." + key2, type, restrictionTO))
                                .build());

                        System.out.println("Test creado Date format invalid YYYY/MM/DD");
                        testCaseTOList.add(TestCaseTO.builder()
                                .idAzure("")
                                .feature(operationTO.getOperationId())
                                .testCase(String.format("BORDE FIELD - %s", key + "." + key2))
                                .headers(operationTO.getHeaders().getJsonBody())
                                .campo(key + "." + key2)
                                .status(400)
                                .valor(String.format("'%s'", localDate.format(dateTimeFormatter2)))
                                .response(getResponse(key + "." + key2, type, restrictionTO))
                                .build());

                        System.out.println("Test creado Date format invalid DD-MM-YYYY");
                        testCaseTOList.add(TestCaseTO.builder()
                                .idAzure("")
                                .feature(operationTO.getOperationId())
                                .testCase(String.format("BORDE FIELD - %s", key + "." + key2))
                                .headers(operationTO.getHeaders().getJsonBody())
                                .campo(key + "." + key2)
                                .status(400)
                                .valor(String.format("'%s'", localDate.format(dateTimeFormatter3)))
                                .response(getResponse(key + "." + key2, type, restrictionTO))
                                .build());
                    }

                    if (Objects.equals("integer", type)) {
                        if (Objects.nonNull(restrictionTO.getMinLength()) && restrictionTO.getMinLength() > 0) {
                            System.out.println("Test creado Integer String");
                            testCaseTOList.add(TestCaseTO.builder()
                                    .idAzure("")
                                    .feature(operationTO.getOperationId())
                                    .testCase(String.format("BORDE FIELD - %s", key + "." + key2))
                                    .headers(operationTO.getHeaders().getJsonBody())
                                    .campo(key + "." + key2)
                                    .status(400)
                                    .valor("HOL")
                                    .response(getResponse(key + "." + key2, type, restrictionTO))
                                    .build());

                            testCaseTOList.add(TestCaseTO.builder()
                                    .idAzure("")
                                    .feature(operationTO.getOperationId())
                                    .testCase(String.format("BORDE FIELD - %s", key + "." + key2))
                                    .headers(operationTO.getHeaders().getJsonBody())
                                    .campo(key + "." + key2)
                                    .status(400)
                                    .valor(String.valueOf(restrictionTO.getMinLength() - 1))
                                    .response(getResponse(key + "." + key2, type, restrictionTO))
                                    .build());
                        }

                    }
                    //}
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                continue;
            }


        }
        return testCaseTOList;
    }

    private List<TestCaseTO> parseObjectvalidacionesExtras(JSONObject level, OperationTO operationTO, String key) {
        List<TestCaseTO> testCaseTOList = new ArrayList<>();
        String type;
        RestrictionTO restrictionTO;
        boolean required;
        Gson gson = new Gson();
        for (Iterator<String> it2 = level.keys(); it2.hasNext(); ) {
            try {
                String key2 = it2.next();
                if (Objects.equals("type", key2)) {
                    continue;
                }
                type = getType(level, key2);
                System.out.println(String.format("procesando key: %s, campo: %s, type: %s ", key, key2, type));
                if (Objects.equals("object", type)) {
                    JSONObject level2 = (JSONObject) level.get(key2);
                    restrictionTO = gson.fromJson(getObject(level, key2).toString(), RestrictionTO.class);
                    try {
                        String[] booleans = System.getenv("CamposBooleanos").split(",");
                        for (String campo : booleans) {
                            if ((key2.toLowerCase()).contains(campo.toLowerCase())) {
                                //for(String value : restrictionTO.getValues()) {
                                System.out.println("Test validando booleanos");
                                testCaseTOList.add(TestCaseTO.builder()
                                        .idAzure("")
                                        .feature(operationTO.getOperationId())
                                        .testCase(String.format("VALIDACIONES EXTRAS Validando Booleanos - %s", key + "." + key2))
                                        .headers(operationTO.getHeaders().getJsonBody())
                                        .campo(key + "." + key2)
                                        .status(400)
                                        .valor("123")
                                        .response(getResponseValidation(key + "." + key2, type, restrictionTO))
                                        .build());
                                //}
                            }
                        }
                    }catch (Exception e){
                        System.out.println("No hay elementos booleanos para analizar"+e.getMessage());

                    }
                    List<TestCaseTO> testCaseObjectTOList = parseObjectvalidacionesExtras(level2, operationTO, String.format("%s.%s", key, key2));
                    if (!testCaseObjectTOList.isEmpty()) {
                        testCaseTOList.addAll(testCaseObjectTOList);
                    }
                } else if (Objects.equals("array", type)) {
                    JSONObject level2 = ((JSONObject) level.getJSONArray(key2).get(0));
                    System.out.println("Test creado array");
                    List<TestCaseTO> testCaseObjectTOList = parseObjectvalidacionesExtras(level2, operationTO, String.format("%s.%s[0]", key, key2));
                    if (!testCaseObjectTOList.isEmpty()) {
                        testCaseTOList.addAll(testCaseObjectTOList);
                    }
                } else {
                    restrictionTO = gson.fromJson(getObject(level, key2).toString(), RestrictionTO.class);
                    if ((key2.toLowerCase()).contains("date")) {
                        //for(String value : restrictionTO.getValues()) {
                        System.out.println("Test validando Fecha");
                        testCaseTOList.add(TestCaseTO.builder()
                                .idAzure("")
                                .feature(operationTO.getOperationId())
                                .testCase(String.format("VALIDACIONES EXTRAS Validando Fecha- %s", key + "." + key2))
                                .headers(operationTO.getHeaders().getJsonBody())
                                .campo(key + "." + key2)
                                .status(400)
                                .valor("\"123\"")
                                .response(getResponseValidation(key + "." + key2, type, restrictionTO))
                                .build());

                        testCaseTOList.add(TestCaseTO.builder()
                                .idAzure("")
                                .feature(operationTO.getOperationId())
                                .testCase(String.format("VALIDACIONES EXTRAS Validando Mes Fecha- %s", key + "." + key2))
                                .headers(operationTO.getHeaders().getJsonBody())
                                .campo(key + "." + key2)
                                .status(400)
                                .valor("\"2025-60-17\"")
                                .response(getResponseValidation(key + "." + key2, type, restrictionTO))
                                .build());

                        testCaseTOList.add(TestCaseTO.builder()
                                .idAzure("")
                                .feature(operationTO.getOperationId())
                                .testCase(String.format("VALIDACIONES EXTRAS Validando Dia Fecha- %s", key + "." + key2))
                                .headers(operationTO.getHeaders().getJsonBody())
                                .campo(key + "." + key2)
                                .status(400)
                                .valor("\"2025-11-50\"")
                                .response(getResponseValidation(key + "." + key2, type, restrictionTO))
                                .build());
                        //}
                    }

                    if ((key2.toLowerCase()).contains("email")) {
                        //for(String value : restrictionTO.getValues()) {
                        System.out.println("Test validando Email");
                        testCaseTOList.add(TestCaseTO.builder()
                                .idAzure("")
                                .feature(operationTO.getOperationId())
                                .testCase(String.format("VALIDACIONES EXTRAS Validando Email- %s", key + "." + key2))
                                .headers(operationTO.getHeaders().getJsonBody())
                                .campo(key + "." + key2)
                                .status(400)
                                .valor("\"automatizacion@email\"")
                                .response(getResponseValidation(key + "." + key2, type, restrictionTO))
                                .build());
                        //}
                    }

                    if ((key2.toLowerCase()).contains("amount") || (key2.toLowerCase()).contains("monto")) {
                        //for(String value : restrictionTO.getValues()) {
                        System.out.println("Test validando Montos negativos");
                        testCaseTOList.add(TestCaseTO.builder()
                                .idAzure("")
                                .feature(operationTO.getOperationId())
                                .testCase(String.format("VALIDACIONES EXTRAS Validando Montos Negativos- %s", key + "." + key2))
                                .headers(operationTO.getHeaders().getJsonBody())
                                .campo(key + "." + key2)
                                .status(400)
                                .valor("\"-1.0\"")
                                .response(getResponseValidation(key + "." + key2, type, restrictionTO))
                                .build());

                        testCaseTOList.add(TestCaseTO.builder()
                                .idAzure("")
                                .feature(operationTO.getOperationId())
                                .testCase(String.format("VALIDACIONES EXTRAS Validando Montos Cero - %s", key + "." + key2))
                                .headers(operationTO.getHeaders().getJsonBody())
                                .campo(key + "." + key2)
                                .status(400)
                                .valor("\"0.00\"")
                                .response(getResponseValidation(key + "." + key2, type, restrictionTO))
                                .build());
                        //}
                    }

                    if ((key2.toLowerCase()).contains("customernumber") || (key2.toLowerCase()).contains("rut")) {
                        //for(String value : restrictionTO.getValues()) {
                        System.out.println("Test validando Rut con menos caracteres");
                        testCaseTOList.add(TestCaseTO.builder()
                                .idAzure("")
                                .feature(operationTO.getOperationId())
                                .testCase(String.format("VALIDACIONES EXTRAS Validando Rut - %s", key + "." + key2))
                                .headers(operationTO.getHeaders().getJsonBody())
                                .campo(key + "." + key2)
                                .status(400)
                                .valor("\"23704259\"")
                                .response(getResponse(key + "." + key2, type, restrictionTO))
                                .build());

                        testCaseTOList.add(TestCaseTO.builder()
                                .idAzure("")
                                .feature(operationTO.getOperationId())
                                .testCase(String.format("VALIDACIONES EXTRAS Validando Rut CON DV Incorrecto- %s", key + "." + key2))
                                .headers(operationTO.getHeaders().getJsonBody())
                                .campo(key + "." + key2)
                                .status(400)
                                .valor("\"237042590\"")
                                .response(getResponse(key + "." + key2, type, restrictionTO))
                                .build());
                    }

                }
                //}
            } catch (Exception e) {
                System.out.println(e.getMessage());
                continue;
            }


        }
        return testCaseTOList;
    }

    private List<TestCaseTO> parseObjectFuntionalSuccess(OperationTO operationTO) {
        List<TestCaseTO> testCaseTOList = new ArrayList<>();
        testCaseTOList.add(TestCaseTO.builder()
                .idAzure("")
                .feature(operationTO.getOperationId())
                .testCase(String.format("CASO EXITOSO FUNCIONAL"))
                .headers(operationTO.getHeaders().getJsonBody())
                .status(201)
                .build());
        return testCaseTOList;
    }


    private String getValue(Integer length, boolean numberString) {
        String generatedString = RandomStringUtils.random(length, !numberString, numberString);

        //System.out.println("generatedString" + generatedString);
        return generatedString;
    }

    private JSONObject getResponse(String key, String type, RestrictionTO restrictionTO) {
        JSONObject response = new JSONObject();
        response.put("customer_full", "#object");
        response.put("fcubs_error_resp", "#array");
        response.put("fcubs_warning_resp", "#array");
        //response.put("validation", getValidations(key, getErrorMessage(type), restrictionTO));

        return response;
    }

    private JSONObject getResponseValidation(String key, String type, RestrictionTO restrictionTO) {
        JSONObject response = new JSONObject();
        response.put("code", 400);
        response.put("message", "Error de validación en request");
        response.put("status", "BAD_REQUEST");
        response.put("validation_errors", "#array");

        //response.put("validation", getValidations(key, getErrorMessage(type), restrictionTO));

        return response;
    }

    private JSONObject getResponseStrctureError(String key, String type, RestrictionTO restrictionTO) {
        JSONObject response = new JSONObject();
        response.put("message", "Revisar la estructura del mensaje");
        //response.put("validation", getValidations(key, getErrorMessage(type), restrictionTO));
        response.put("entityTypeName", "#string");
        response.put("traceId", "#string");
        response.put("status", 400);
        response.put("code", "40000");

        return response;
    }

    private JSONArray getValidations(String key, String message, RestrictionTO restrictionTO) {
        JSONArray jsonArray = new JSONArray();
        JSONObject respose = new JSONObject();

        if (Objects.nonNull(restrictionTO) && Objects.nonNull(restrictionTO.getValues()) && !restrictionTO.getValues().isEmpty()) {
            respose = new JSONObject();
            respose.put("attribute", key);
            respose.put("message", "Este campo debe tener una opción válida");
            jsonArray.put(respose);
        }

        if (Objects.nonNull(restrictionTO) &&
                Objects.nonNull(restrictionTO.getMinLength()) &&
                Objects.nonNull(restrictionTO.getMaxLength()) &&
                (Objects.equals(restrictionTO.getMaxLength(), restrictionTO.getMinLength()))) {
            respose = new JSONObject();
            respose.put("attribute", key);
            respose.put("message", String.format("Este campo debe tener %s caracteres", restrictionTO.getMinLength()));
            jsonArray.put(respose);
        }

        if (Objects.nonNull(restrictionTO) &&
                Objects.nonNull(restrictionTO.getMinLength()) &&
                Objects.nonNull(restrictionTO.getMaxLength()) &&
                (!Objects.equals(restrictionTO.getMaxLength(), restrictionTO.getMinLength())) &&
                restrictionTO.getMinLength() != 0) {
            respose = new JSONObject();
            respose.put("attribute", key);
            respose.put("message", String.format("La longitud de este campo debe estar entre %s y %s caracteres",
                    restrictionTO.getMinLength(), restrictionTO.getMaxLength()));
            jsonArray.put(respose);
        }

        if (key.contains("customerNumber")) {
            respose = new JSONObject();
            respose.put("attribute", key);
            respose.put("message", "El Rut es inválido");
            jsonArray.put(respose);
        }

        respose = new JSONObject();
        respose.put("attribute", key);
        respose.put("message", message);
        jsonArray.put(respose);

        return jsonArray;
    }

    private String getErrorMessage(String type) {
        if (Objects.equals("object", type)) {
            return "El formato del valor ingresado no es válido";
        } else {
            return "Este campo es requerido";
        }

    }

    private String getType(Object object, String key) {
        try {
            return ((JSONObject) ((JSONObject) object).get(key)).getString("type");
        } catch (Exception ex) {
            JSONObject jsonObject = (JSONObject) object;
            return ((JSONObject) jsonObject.getJSONArray(key).get(0)).getString("type");
        }
    }

    private JSONObject getObject(Object object, String key) {
        try {
            return ((JSONObject) ((JSONObject) object).get(key));
        } catch (Exception ex) {
            JSONObject jsonObject = (JSONObject) object;
            return ((JSONObject) jsonObject.getJSONArray(key).get(0));
        }
    }

    private boolean getRequired(Object object, String key) {
        try {
            return ((JSONObject) ((JSONObject) object).get(key)).getBoolean("required");
        } catch (Exception ex) {
            JSONObject jsonObject = (JSONObject) object;
            return ((JSONObject) jsonObject.getJSONArray(key).get(0)).getBoolean("required");
        }
    }

    private List<String> convertLines(String json) {
        String[] lines = json.split("\\n");
        List<String> fileString = Arrays.asList(lines);
        fileString.replaceAll(s -> s.replace("\\u0027", "'"));
        return fileString;
    }

    @SneakyThrows
    private static String printJsonModel(Object val) {
        JsonMapper json = new JsonMapper();
        json.registerModule(new JavaTimeModule());
        json.registerModule(new JsonOrgModule());
        json.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return json.writerWithDefaultPrettyPrinter().writeValueAsString(val);
    }


    private static ServiceKarateTO getData() {
        String body = "{\"name\":\"acl-current-account-deposit\",\"tagName\":\"aclCurrentAccountDeposit\",\"description\":\"Servicio encargado de realizar el abono una cuenta de cliente de acuerdo a la lógica de negocio que se quiere perdurar en la compañía.\",\"operations\":[{\"operationId\":\"create\",\"path\":\"/operations-and-execution-secure/current-account/v1/accounts/{currentAccountId}/deposits\",\"httpMethod\":\"POST\",\"parameters\":[{\"name\":\"currentAccountPathVariable\",\"parameterType\":\"QUERY\",\"restriction\":{\"numberString\":false}}],\"requestBody\":{\"jsonBody\":{\"map\":{\"deposit\":{\"map\":{\"depositTransactionDescription\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"minLength\":0,\"type\":\"string\",\"maxLength\":4000,\"required\":false}},\"depositTransactionDate\":{\"map\":{\"accountableDate\":{\"map\":{\"dateContent\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"format\":\"date\",\"type\":\"string\",\"required\":true}},\"type\":\"object\"}},\"captureDate\":{\"map\":{\"dateContent\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"format\":\"date\",\"type\":\"string\",\"required\":true}},\"type\":\"object\"}},\"type\":\"object\"}},\"source\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"minLength\":0,\"type\":\"string\",\"maxLength\":15,\"required\":true}},\"type\":\"object\",\"depositTransaction\":{\"map\":{\"externalReferenceNumber\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"minLength\":0,\"type\":\"string\",\"maxLength\":35,\"required\":true}},\"type\":\"object\",\"arcProductReference\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"minLength\":0,\"type\":\"string\",\"maxLength\":4,\"required\":false}}}},\"entity\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"minLength\":0,\"type\":\"string\",\"maxLength\":12,\"required\":true}},\"bankBranchLocationReference\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"minLength\":0,\"type\":\"string\",\"maxLength\":3,\"required\":true}},\"depositTransactionAmount\":{\"map\":{\"amountValue\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"minLength\":1,\"type\":\"string\",\"maxLength\":12,\"required\":true}},\"decimalPointPosition\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"format\":\"int32\",\"type\":\"integer\",\"required\":true}},\"type\":\"object\",\"amountCurrency\":{\"map\":{\"type\":\"object\",\"currencyCode\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"minLength\":0,\"type\":\"string\",\"maxLength\":3,\"required\":true}}}}}}}},\"type\":\"object\"}}},\"responsesBodyTO\":[{\"httpStatus\":\"OK\",\"summary\":\"OK\",\"jsonResponse\":{\"map\":{\"deposit\":{\"map\":{\"depositTransactionChecker\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"type\":\"string\",\"required\":true}},\"depositTransactionGroupReference\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"type\":\"string\",\"required\":true}},\"depositTransactionStatus\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"type\":\"string\",\"required\":true}},\"externalReferenceNumber\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"type\":\"string\",\"required\":true}},\"depositTransactionDate\":{\"map\":{\"checkerDate\":{\"map\":{\"dateContent\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"format\":\"date\",\"type\":\"string\",\"required\":false}},\"type\":\"object\"}},\"captureDateTime\":{\"map\":{\"timeContent\":{\"map\":{\"hour\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"format\":\"int32\",\"type\":\"integer\",\"required\":false}},\"nano\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"format\":\"int32\",\"type\":\"integer\",\"required\":false}},\"type\":\"object\",\"minute\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"format\":\"int32\",\"type\":\"integer\",\"required\":false}},\"second\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"format\":\"int32\",\"type\":\"integer\",\"required\":false}}}},\"dateContent\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"format\":\"date\",\"type\":\"string\",\"required\":false}},\"type\":\"object\"}},\"type\":\"object\"}},\"depositTransactionReference\":{\"map\":{\"numberString\":false,\"values\":{\"myArrayList\":[]},\"type\":\"string\",\"required\":true}}}},\"type\":\"object\"}}}]}]}";

        return new Gson().fromJson(body, ServiceKarateTO.class);
    }
}
