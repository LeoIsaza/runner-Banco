package cl.bice.createStructure.service.test;

import cl.bice.createStructure.to.create.OperationTO;
import cl.bice.createStructure.to.create.RestrictionTO;
import cl.bice.createStructure.to.create.ServiceKarateTO;
import cl.bice.createStructure.utils.UtilsData;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import lombok.SneakyThrows;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class CreateRequestService {

    public void execute(ServiceKarateTO serviceKarateTO) throws IOException {
        String folderRequest = File.separator + "structure" + File.separator +  "request" + File.separator;
        String folderRequestFuntional = File.separator + "funtional" + File.separator +  "request" + File.separator;

        JSONObject request;

        for (OperationTO operation: serviceKarateTO.getOperations()) {
            if(Objects.nonNull(operation.getRequestFuntionalBody())) {
                request = operation.getRequestFuntionalBody().getJsonBody();
            } else {
                request = new JSONObject();
            }

            CreateStructureFolder.createFile(folderRequest + operation.getOperationId() + "-request.js", replaceAleatoriosStructure(converLinesRequest(request)));
            CreateStructureFolder.createFile(folderRequestFuntional + operation.getOperationId() + "-request.js", replaceAleatorios(converLinesRequest(request)));

        }

    }

    private List<String> converLinesRequest(JSONObject json) {
        String jsonString = printJsonModelPretty(json);
        String[] lines = jsonString.split("\\n");
        List<String> fileString = new ArrayList<>();
        fileString.add("function fn() {");
        fileString.add("return JSON.stringify(");
        fileString.addAll(Arrays.asList(lines));
        fileString.add(")");
        fileString.add("}");
        fileString.replaceAll(s -> s.replace("\\u0027", "'"));

        return fileString;
    }

    private List<String> replaceAleatorios(List<String> fileString) {
        fileString.replaceAll(s -> s.replace("\"Aleatorio\"", "variableAleatoria"));

        return fileString;
    }

    private List<String> replaceAleatoriosStructure(List<String> fileString) {
        fileString.replaceAll(s -> s.replace("Aleatorio", "1913259"));
        return fileString;
    }

    public JSONObject getRequest(JSONObject jsonObject) {
        //System.out.println(jsonObject);


        //System.out.println("------------------------------------------");
        JSONObject level;
        String type;
        RestrictionTO restrictionTO;
        Gson gson = new Gson();
        JSONObject json = new JSONObject();
        for (Iterator<String> it = jsonObject.keys(); it.hasNext();) {
            String key = it.next();

            Object object = jsonObject.get(key);
            //System.out.println(key);

            if (object instanceof JSONObject) {
                level = (JSONObject) object;
                type = level.getString("type");

                if (Objects.equals("object", type)) {
                    //System.out.println("key: " + key);
                    json.put(key, parseObject(level, key));
                    continue;
                }

                if (Objects.equals("array", type)) {
                    //System.out.println("key: " + key);
                    json.put(key, parseObject(level, key));
                    continue;
                }

                //System.out.println(level.get(key2));
                restrictionTO = gson.fromJson(level.toString(), RestrictionTO.class);
                //System.out.println(key2 + " - " + restrictionTO.getType());
                json.append(key, restrictionTO.getType());
            }

            //break;

            //System.out.println("------------------------------------------");

        }
        //System.out.println("json");
        //System.out.println(json);
        return json;
    }

    private JSONObject parseObject(JSONObject level, String key) {
        JSONObject json = new JSONObject();
        RestrictionTO restrictionTO;
        Gson gson = new Gson();
        String type;
        for (Iterator<String> it2 = level.keys(); it2.hasNext(); ) {
            String key2 = it2.next();
            if (Objects.equals("type", key2)) {
                continue;
            }
            type = getType(level, key2);
            if (Objects.equals("object", type)) {
                try {
                    System.out.println("------------------------------------------");
                    System.out.println("key2: " + key2);
                    json.put(key2, parseObject((JSONObject) level.get(key2), key2));
                }catch (Exception ex){
                    System.out.println("Hay un objeto dentro pero no es Json");
                }
                System.out.println("------------------------------------------");
                continue;
            }

            if (Objects.equals("array", type)) {
                System.out.println("------------------------------------------");
                System.out.println("key2: " + key2);
                json.append(key2, parseObject((JSONArray) level.get(key2), key2));
                System.out.println("------------------------------------------");
                continue;
            }

            System.out.println(level.get(key2));
            restrictionTO = gson.fromJson(level.get(key2).toString(), RestrictionTO.class);
            System.out.println(key2 + " - " + restrictionTO.getType());
            json.put(key2, getTypeValid(restrictionTO, key2));
        }

        System.out.println("json2");
        System.out.println(json);
        return json;
    }

    private JSONObject parseObject(JSONArray level, String key) {
        JSONObject json = new JSONObject();
        RestrictionTO restrictionTO;
        Gson gson = new Gson();
        String type;
        for (Iterator<Object> it = level.iterator(); it.hasNext(); ) {
            JSONObject object = (JSONObject) it.next();

            for (Iterator<String> it2 = object.keys(); it2.hasNext(); ) {
                String key2 = it2.next();
                if (Objects.equals("type", key2)) {
                    continue;
                }
                System.out.println(object.get(key2).getClass());
                if(Objects.equals(object.get(key2).getClass(), JSONArray.class)){
                    type = ((JSONObject)object.getJSONArray(key2).get(0)).getString("type");
                } else {
                    type = ((JSONObject) object.get(key2)).getString("type");
                }

                if (Objects.equals("object", type)) {
                    json.put(key2, parseObject((JSONObject) object.get(key2), key2));
                    continue;
                }

                if (Objects.equals("array", type)) {
                    json.put(key2, parseObject(((JSONObject)object.getJSONArray(key2).get(0)), key2));
                    continue;
                }
                //System.out.println(level.get(key2));
                restrictionTO = gson.fromJson(object.get(key2).toString(), RestrictionTO.class);
                //System.out.println(key2 + " - " + restrictionTO.getType());
                json.put(key2, getTypeValid(restrictionTO, key2));
            }

        }

        //System.out.println("json2");
        //System.out.println(json);
        return json;
    }

    private String getTypeValid(RestrictionTO restrictionTO, String key) {
        String value = "";
        UtilsData utilsData = new UtilsData();
        switch (restrictionTO.getType()) {
            case "string":
                value = getExample(restrictionTO.getExample(), "hola");
                break;
            case "integer":
                value = getExample(restrictionTO.getExample(), "1");
                break;
            case "date":
                value = getExample(restrictionTO.getExample(), LocalDate.now().toString());
                break;
            default: value = "hola2";

        }

        if(Objects.nonNull(restrictionTO.getValues()) && !restrictionTO.getValues().isEmpty()) {
            value = getOptionEnum(restrictionTO.getValues(), restrictionTO.getExample());
        }

        if(Objects.nonNull(restrictionTO.getFormat())) {
            switch (restrictionTO.getFormat()) {
                case "string":
                    value = getExample(restrictionTO.getExample(), "hola");
                    break;
                case "int32":
                    value = getExample(restrictionTO.getExample(), "1");
                    break;
                case "date":
                    value = getExample(restrictionTO.getExample(), LocalDate.now().toString());
                    break;
                default: value = "hola2";

            }
        }

        //TODO casos de borde con mac and min
        // if(Objects.nonNull(restrictionTO.getMaxLength()) && value.length() > restrictionTO.getMaxLength()) {
        //     value = value.substring(0, restrictionTO.getMaxLength());
        // }

        // if(Objects.nonNull(restrictionTO.getMaxLength()) && Objects.nonNull(restrictionTO.getMinLength())) {
        //     value = utilsData.getString(restrictionTO.getMinLength(), restrictionTO.getMaxLength());
        // }

        //TODO data borde
        // if(Objects.equals("partyType", key)) {
        //     value = "Person";
        // }

        // if(Objects.equals("email", key)) {
        //     value = "hola@hola.com";
        // }

        // String rut = Utils.getRandomRut();

        // if(Objects.equals("customerNumber", key)) {
        //     value = rut;
        // }

        // if(Objects.equals("partyIdentification", key)) {
        //     value = "0" + rut.substring(0,8);
        // }

        return value;
    }

    private String getExample(String example, String defaultValue) {
        if(Objects.isNull(example)) {
            return defaultValue;
        } else {
            return example;
        }
    }

    public String getOptionEnum(List<String> enums, String defaultValue) {
        if(Objects.nonNull(defaultValue)) {
            return defaultValue;
        } else {
            int randIdx = ThreadLocalRandom.current().nextInt(enums.size());
            return enums.get(randIdx);
        }
    }

    @SneakyThrows
    private String printJsonModelPretty(Object val) {
        JsonMapper json = new JsonMapper();
        json.registerModule(new JavaTimeModule());
        json.registerModule(new JsonOrgModule());
        json.enable(SerializationFeature.INDENT_OUTPUT);
        return json.writeValueAsString(val);
    }

    private String getType(Object object, String key) {
        try {
            return ((JSONObject)((JSONObject) object).get(key)).getString("type");
        } catch (Exception ex) {
            JSONObject jsonObject = (JSONObject) object;
            //System.out.println();
            return ((JSONObject)jsonObject.getJSONArray(key).get(0)).getString("type");
        }
    }
}
