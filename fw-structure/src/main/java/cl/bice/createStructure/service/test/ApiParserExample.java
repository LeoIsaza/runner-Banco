package cl.bice.createStructure.service.test;


import cl.bice.createStructure.to.create.HttpMethod;
import cl.bice.createStructure.to.create.OperationTO;
import cl.bice.createStructure.to.create.ParameterTO;
import cl.bice.createStructure.to.create.ParameterType;
import cl.bice.createStructure.to.create.RequestBodyTO;
import cl.bice.createStructure.to.create.ResponseBodyTO;
import cl.bice.createStructure.to.create.RestrictionTO;
import cl.bice.createStructure.to.create.ServiceKarateTO;
import cl.bice.createStructure.utils.RestTemplateWithCert;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.SneakyThrows;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ApiParserExample {
    private static final List<String> NAMES_REMOVE = Arrays.asList("cause", "stackTrace", "suppressed", "localizedMessage");

    //private static final String URL_API_DOC = "http://localhost:"+System.getenv("puerto")+"/v3/api-docs";
    private static final String URL_API_DOC = "https://api-baas-dev-test.bice.local/"+System.getenv("servicio")+"/v3/api-docs";


    public static void main(String[] args) {
        execute();
    }

    public static String execute() {
        String swaggerJson = getSwagger();
        SwaggerParseResult result = new OpenAPIParser().readContents(
                swaggerJson,
                null,
                null);

        System.out.println(swaggerJson);

        OpenAPI openAPI = result.getOpenAPI();

        //System.out.println(printJsonModel(openAPI));

        if (Objects.isNull(openAPI.getTags())) {
            Tag tag = new Tag();
            tag.setName(System.getenv("servicio"));
            tag.setDescription("");
            List<Tag> tags = new ArrayList<>();
            tags.add(tag);
            openAPI.setTags(tags);
            //throw new RuntimeException("must define @tag service in controller \n @Tag(name = \"name service\", description = \"description service\")");
        }

        ServiceKarateTO serviceKarateTO = ServiceKarateTO.builder()
                .description(openAPI.getTags().get(0).getDescription())
                .name(openAPI.getTags().get(0).getName())
                .operations(new ArrayList<>())
                .build();

        OperationTO operation;
        ParameterTO parameter;

        //System.out.println("Description: " + openAPI.getInfo().getDescription());
        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            operation = OperationTO.builder()
                    .path(entry.getKey())
                    .parameters(new ArrayList<>())
                    .build();
            //System.out.println("____________________________________________________\n");
            //System.out.println("path: " + entry.getKey());
            for (Map.Entry<PathItem.HttpMethod, Operation> op : entry.getValue().readOperationsMap().entrySet()) {

                operation.setOperationId(op.getValue().getOperationId());
                operation.setHttpMethod(HttpMethod.valueOf(String.valueOf(op.getKey())));
                System.out.println(op.getKey() + " - " + op.getValue().getOperationId() + " - ");
                System.out.println("Parameters ->");
                if (Objects.nonNull(op.getValue().getParameters())) {
                    for (Parameter p : op.getValue().getParameters()) {
                        parameter = ParameterTO.builder().build();
                        ParameterType paramType;
                        JSONObject paramObject;
                        String name;
                        if (p instanceof PathParameter) {
                            paramType = ParameterType.PATH;
                            paramObject = getParamObject(openAPI, p, false);
                            name = paramObject.keys().next();
                        } else if (p instanceof QueryParameter) {
                            paramType = ParameterType.QUERY;
                            paramObject = getParamObject(openAPI, p, false);
                            name = p.getName();
                        } else if (p instanceof HeaderParameter) {
                            paramType = ParameterType.HEADER;
                            paramObject = getParamObject(openAPI, p, false);
                            name = p.getName();
                        } else {
                            paramType = ParameterType.NONE;
                            paramObject = new JSONObject();
                            name = p.getName();
                        }

                        //System.out.println(p.getName() + " : " + paramType + " : " + paramObject);
                        parameter.setName(name);
                        parameter.setParameterType(paramType);
                        parameter.setRestriction(getRestrictionTO(paramObject));
                        operation.getParameters().add(parameter);

                    }
                }

                operation.setRequestBody(getBodyDetails(openAPI, op.getValue().getRequestBody()));
                operation.setResponsesBodyTO(getResponseDetails(openAPI, op.getValue().getResponses()));
                //operation.setRequestFuntionalBody();
                serviceKarateTO.getOperations().add(operation);

            }
            //System.out.println("____________________________________________________\n");
            //TDDO eliminar break
            //break;
        }
        System.out.println(printJsonModel(serviceKarateTO));
        return printJsonModel(serviceKarateTO);
    }

    private static String getSwagger(){
        try {
            System.setProperty("http.proxyHost","127.0.0.1");
            System.setProperty("http.proxyPort","8888");
            RestTemplateWithCert restTemplateWithCert = new RestTemplateWithCert();
            RestTemplate restTemplate = restTemplateWithCert.createRestTemplate();
            HttpHeaders httpHeaders = new HttpHeaders();
            HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);



            URI uri = URI.create(URL_API_DOC);

            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    org.springframework.http.HttpMethod.GET,
                    httpEntity,
                    String.class
            );
            System.out.println("paso petición");
            return response.getBody();
        }catch (Exception e){
            System.out.println("Ojo, aquí entro y no hay swagger");
            return null;
        }
    }

    private static RestrictionTO getRestrictionTO(JSONObject paramObject) {
        try {
            return new Gson().fromJson(String.valueOf(paramObject), RestrictionTO.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String getKeyParameterTypePATH(String paramObject) {
        try {
            JSONObject jo = new JSONObject(paramObject);
            return jo.keys().next();
        } catch (Exception ex) {
            return null;
        }
    }

    private static RequestBodyTO getBodyDetails(OpenAPI openAPI, RequestBody requestBody) {
        //System.out.println("RequestBody:");
        //System.out.println("-----------");

        if (Objects.isNull(requestBody)) {
            //System.out.println("null");
            return null;
        }
        String mediaType = "";
        for (Map.Entry<String, MediaType> mediaTypeEntry : requestBody.getContent().entrySet()) {
            mediaType = mediaTypeEntry.getKey();
            break;
        }
        String tag = "";
        JSONObject json = new JSONObject();
        if (Objects.nonNull(requestBody.getContent().get(mediaType).getSchema().get$ref())) {
            String[] tags = requestBody.getContent().get(mediaType).getSchema().get$ref().split("/");
            tag = tags[tags.length - 1];
            getJsonBody(openAPI, tag, json);
        } else {
            Map<String, Schema> properties2 = requestBody.getContent().get(mediaType).getSchema().getProperties();
            System.out.println(properties2);
            for (Map.Entry<String, Schema> property : properties2.entrySet()) {
                String[] tags = property.getValue().get$ref().split("/");
                tag = tags[tags.length - 1];
                getJsonBody(openAPI, tag, json);
            }
        }
        json.put("type", "object");
        return RequestBodyTO.builder()
                .jsonBody(json)
                .build();
    }

    private static void getJsonBody(OpenAPI openAPI, String tag, JSONObject json) {
        List<String> filedsRequires = openAPI.getComponents().getSchemas().get(tag).getRequired();
        Map<String, Schema> properties = openAPI.getComponents().getSchemas().get(tag).getProperties();
        for (Map.Entry<String, Schema> mapSchema : properties.entrySet()) {
            System.out.println(mapSchema.getKey());
            String type = mapSchema.getValue().getType();
            if(Objects.isNull(type)) {
                if(Objects.nonNull(mapSchema.getValue().getTypes())){
                    type = mapSchema.getValue().getTypes().iterator().next().toString();
                }
            }
            if (Objects.equals("string", type) ||
                    Objects.equals("integer", type) ||
                    Objects.equals("number", type)
            ) {
                json.put(mapSchema.getKey(), getJson(mapSchema, filedsRequires,false));
            } else {
                JSONObject json2 = getSchemaObject(openAPI, mapSchema.getValue(), mapSchema.getKey(),false);
                json2.put("type", "object");
                json.put(mapSchema.getKey(), json2);
            }
        }

    }

    private static List<ResponseBodyTO> getResponseDetails(OpenAPI openAPI, ApiResponses responses) {
        List<ResponseBodyTO> responseBodyTOList = new ArrayList<>();
        //System.out.println("Responses:");
        //System.out.println("-----------");
        for (Map.Entry<String, ApiResponse> response : responses.entrySet()) {
            //System.out.println(response.getKey() + ": " + response.getValue().getDescription());
            String mediaType = "";
            for (Map.Entry<String, MediaType> mediaTypeEntry : response.getValue().getContent().entrySet()) {
                mediaType = mediaTypeEntry.getKey();
                break;
            }

            String[] tags = response.getValue().getContent().get(mediaType).getSchema().get$ref().split("/");
            String tag = tags[tags.length - 1];
            Map<String, Schema> properties = openAPI.getComponents().getSchemas().get(tag).getProperties();
            List<String> filedsRequires = openAPI.getComponents().getSchemas().get(tag).getRequired();
            JSONObject json = new JSONObject();
            for (Map.Entry<String, Schema> mapSchema : properties.entrySet()) {
                System.out.println(mapSchema.getKey());

                if (NAMES_REMOVE.contains(mapSchema.getKey())) {
                    continue;
                }

                String type = mapSchema.getValue().getType();
                if(Objects.isNull(type)) {
                    if(Objects.nonNull(mapSchema.getValue().getTypes())){
                        type = mapSchema.getValue().getTypes().iterator().next().toString();
                    }
                }

                if (Objects.equals("string", type) ||
                        Objects.equals("integer", type) ||
                        Objects.equals("number", type)
                ) {
                    json.put(mapSchema.getKey(), getJson(mapSchema, filedsRequires, true));
                } else {
                    json.put("type", "object");
                    json.put(mapSchema.getKey(), getSchemaObject(openAPI, mapSchema.getValue(), mapSchema.getKey(), true));
                }
            }

            //System.out.println(json);

            responseBodyTOList.add(ResponseBodyTO.builder()
                    .httpStatus(HttpStatus.valueOf(Integer.parseInt(response.getKey())))
                    .jsonResponse(json)
                    .summary(response.getValue().getDescription())
                    .build());
        }

        return responseBodyTOList;
    }

    private static JSONObject getParamObject(OpenAPI openAPI, Parameter parameter, boolean response) {
        JSONObject paramObject;
        switch (parameter.getStyle()) {
            case FORM:
            case SIMPLE:
                if (Objects.isNull(parameter.getSchema().get$ref())) {
                    paramObject = getJson(parameter.getName(), "");
                } else {
                    paramObject = getSchemaObject(openAPI, parameter.getSchema(), response);
                }
                break;
            default:
                paramObject = getJson(null, null);
                break;
        }
        return paramObject;
    }

    private static JSONObject getSchemaObject(OpenAPI openAPI, Schema schema, String key, boolean response) {
        JSONObject json = new JSONObject();

        if (Objects.isNull(schema.get$ref())) {
            return getJson(key, schema, new ArrayList<>(), response);
        }

        String[] tags = schema.get$ref().split("/");
        String tag = tags[tags.length - 1];
        List<String> filedsRequires = openAPI.getComponents().getSchemas().get(tag).getRequired();
        Map<String, Schema> properties = openAPI.getComponents().getSchemas().get(tag).getProperties();
        for (Map.Entry<String, Schema> mapSchema : properties.entrySet()) {
            System.out.println(mapSchema.getKey());
            String type = mapSchema.getValue().getType();
            if(Objects.isNull(type)) {
                if(Objects.nonNull(mapSchema.getValue().getTypes())){
                    type = mapSchema.getValue().getTypes().iterator().next().toString();
                }
            }

            if (Objects.equals("string", type) ||
                    Objects.equals("integer", type) ||
                    Objects.equals("number", type)) {
                json.put(mapSchema.getKey(), getJson(mapSchema, filedsRequires, response));
            } else if (Objects.equals("array", type)) {
                JSONObject json2 = getSchemaObject(openAPI, mapSchema.getValue().getItems(), key, response);
                json2.put("type", "array");
                json.append(mapSchema.getKey(), json2);
            } else {
                System.out.println(mapSchema.getKey());
                JSONObject json2 = getSchemaObject(openAPI, mapSchema.getValue(), mapSchema.getKey(), response);
                json2.put("type", "object");
                json.put(mapSchema.getKey(), json2);
                //System.out.println(json);

            }
        }

        return json;
    }

    private static JSONObject getJson(Map.Entry<String, Schema> schema, List<String> filedsRequires, boolean response) {
        return getJson(schema.getKey(), schema.getValue(), filedsRequires, response);
    }

    private static JSONObject getJson(String key, Schema schema, List<String> filedsRequires, boolean response) {

        boolean required = false;

        if(Objects.nonNull(filedsRequires) && !filedsRequires.isEmpty()) {
            required = filedsRequires.contains(key);
        }

        String type = schema.getType();
        if(Objects.isNull(type)) {
            if(Objects.nonNull(schema.getTypes())){
                type = schema.getTypes().iterator().next().toString();
            }
        }

        JSONObject json = new JSONObject();
        json.put("type", type);
        json.put("values", checkNull(schema.getEnum(), response));
        json.put("format", checkNull(schema.getFormat()));
        json.put("maxLength", Objects.nonNull(schema.getMaxLength())? schema.getMaxLength(): Objects.nonNull(schema.getMaximum()) ? schema.getMaximum().intValue(): schema.getMaximum() );
        json.put("minLength", Objects.nonNull(schema.getMinLength())? schema.getMinLength(): Objects.nonNull(schema.getMinimum()) ? schema.getMinimum().intValue(): schema.getMinimum() );
        json.put("pattern", checkNull(schema.getPattern()));
        json.put("numberString", Objects.equals(type, "integer"));
        json.put("required", required);
        if("date".equalsIgnoreCase(schema.getFormat()) && !response) {
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
            Date date = (Date) schema.getExample();
            if(Objects.nonNull(date)) {
                json.put("example", format1.format(date));
            }
        } else {
            json.put("example", schema.getExample());
        }


        return json;

    }

    private static JSONObject getJson(String key, String type) {

        JSONObject json = new JSONObject();
        List<Object> list = new ArrayList<>();
        json.put("type", type);
        json.put("values", list);
        json.put("format", Optional.empty());
        json.put("maxLength", Optional.empty());
        json.put("minLength", Optional.empty());
        json.put("pattern", Optional.empty());
        json.put("numberString", false);
        json.put("required", false);

        return json;
    }

    private static List<String> checkNull(List<String> anEnum, boolean response) {
        if (Objects.isNull(anEnum) || response) {
            return new ArrayList<>();
        } else {
            return anEnum;
        }
    }

    private static String checkNull(String value) {
        if (Objects.isNull(value)) {
            return null;
        } else {
            return value;
        }
    }

    private static JSONObject getSchemaObject(OpenAPI openAPI, Schema schema, boolean response) {
        System.out.println(printJsonModel(schema));;
        JSONObject json = new JSONObject();
        System.out.println(schema.get$ref());
//        if(Objects.isNull(schema.get$ref())) {
//            json.put(e.getKey(), getJson(e, filedsRequires, response));
//        }
        String[] tags = schema.get$ref().split("/");
        String tag = tags[tags.length - 1];
        List<String> filedsRequires = openAPI.getComponents().getSchemas().get(tag).getRequired();
        Map<String, Schema> properties = openAPI.getComponents().getSchemas().get(tag).getProperties();
        for (Map.Entry<String, Schema> e : properties.entrySet()) {
            if (Objects.equals("string", e.getValue().getType()) ||
                    Objects.equals("integer", e.getValue().getType()) ||
                    Objects.equals("number", e.getValue().getType())) {
                json.put(e.getKey(), getJson(e, filedsRequires, response));
            } else {
                json.put(e.getKey(), getSchemaObject(openAPI, e.getValue(), e.getKey(), response));
            }
        }

        return json;
    }

    @SneakyThrows
    private static String printJsonModel(Object val) {
        JsonMapper json = new JsonMapper();
        json.registerModule(new JavaTimeModule());
        json.registerModule(new JsonOrgModule());
        return json.writeValueAsString(val);
    }
}
