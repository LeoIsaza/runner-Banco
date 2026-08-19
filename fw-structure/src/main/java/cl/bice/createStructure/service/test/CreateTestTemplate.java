package cl.bice.createStructure.service.test;

import cl.bice.createStructure.to.create.ServiceKarateTO;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import static cl.bice.createStructure.service.CurlToCustomFormat.convert;

public class CreateTestTemplate {

    public static void main(String[] args) throws Exception {

        CreateStructureFolder createStructureFolder = new CreateStructureFolder();
        CreateRequestService createRequestService = new CreateRequestService();
        CreateFeature createFeature = new CreateFeature();
        CreateTestKarate createTestKarate = new CreateTestKarate();

        ServiceKarateTO serviceKarateTO = getServiceKarate();

        serviceKarateTO.setTagName(createStructureFolder.getNameService(serviceKarateTO.getName()));
        //CreateStructureFolder.deleteBaseFolder(serviceKarateTO.getTagName());
        createStructureFolder.createStructureFolder(serviceKarateTO.getTagName());
        System.out.println("nameService :" + serviceKarateTO.getTagName());
        System.out.println("description :" + serviceKarateTO.getDescription());

        createRequestService.execute(serviceKarateTO);
        createFeature.execute(serviceKarateTO);
        createTestKarate.execute(serviceKarateTO);

    }

    private static ServiceKarateTO getServiceKarate() throws Exception {
        String template = convert(    System.getenv("curl"), false);
        System.out.println(template);

        Gson gson = new Gson();
        ServiceKarateTO serviceKarateTO = gson.fromJson(template, ServiceKarateTO.class);
        JSONObject jsonObject = new JSONObject(template);

        JSONArray operations = jsonObject.getJSONArray("operations");
        JSONArray responses;
        for (int i=0; i < operations.length(); i++) {

            if(!((JSONObject) operations.get(i)).isNull("requestBody")) {
                serviceKarateTO
                        .getOperations().get(i)
                        .getRequestBody().setJsonBody(
                                ((JSONObject) operations.get(i)).getJSONObject("requestBody").getJSONObject("jsonBody"));

            }

            if(!((JSONObject) operations.get(i)).isNull("requestFuntionalBody")) {
                serviceKarateTO
                        .getOperations().get(i)
                        .getRequestFuntionalBody().setJsonBody(
                                ((JSONObject) operations.get(i)).getJSONObject("requestFuntionalBody"));

            }

            if(!((JSONObject) operations.get(i)).isNull("headers")) {
                serviceKarateTO
                        .getOperations().get(i)
                        .getHeaders().setJsonBody(
                                ((JSONObject) operations.get(i)).getJSONObject("headers"));
            }

            // responses = ((JSONObject) operations.get(i)).getJSONArray("responsesBodyTO");

           /* for (int b=0; b < responses.length(); b++) {
                serviceKarateTO
                        .getOperations().get(i)
                        .getResponsesBodyTO().get(b).setJsonResponse(((JSONObject) responses.get(b)).getJSONObject("jsonResponse"));
            }*/
        }

        return serviceKarateTO;
    }

    @SneakyThrows
    private static String printJsonModel(Object val) {
        JsonMapper json = new JsonMapper();
        json.registerModule(new JavaTimeModule());
        json.registerModule(new JsonOrgModule());
        return json.writeValueAsString(val);
    }


}
