package cl.bice.createStructure.service;

import cl.bice.createStructure.to.test.TemplateCampo;
import cl.bice.createStructure.to.to.HeaderTO;
import cl.bice.createStructure.to.to.ResponseTestTO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class CreateTemplate {

    public static void main(String[] args) {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject jsonObject = gson.fromJson(plantilla, JsonObject.class);

        List<TemplateCampo> templateCampoList = new ArrayList<>();

        for (java.util.Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            if (value.isJsonObject()) {
                JsonObject subObject = value.getAsJsonObject();

                templateCampoList.add(TemplateCampo.builder()
                        .feature("Crear Pago")
                        .tipo("object")
                        .nombre(key)
                        .largo(0)
                        .negativo(false)
                        .status(400)
                        .response(ResponseTestTO.builder()
                                .codigo("400")
                                .mensaje(String.format("Error Entrada: %s es invalido", key))
                                .build())
                        .header(HeaderTO.builder()
                                .xRutCliente("0760210048")
                                .xRutUsuario("0099091800")
                                .trxCanal("IE")
                                .build())
                        .build());

                for (java.util.Map.Entry<String, JsonElement> subEntry : subObject.entrySet()) {
                    String subKey = subEntry.getKey();
                    JsonElement subValue = subEntry.getValue();

                    templateCampoList.add(TemplateCampo.builder()
                            .feature("Crear Pago")
                            .tipo(subValue.toString().replace("#", "").replace("\"", ""))
                            .nombre(key + "." + subKey)
                            .largo(10)
                            .negativo(false)
                            .status(400)
                            .response(ResponseTestTO.builder()
                                    .codigo("400")
                                    .mensaje(String.format("Error Entrada: %s es invalido", key + "." + subKey))
                                    .build())
                            .header(HeaderTO.builder()
                                    .xRutCliente("0760210048")
                                    .xRutUsuario("0099091800")
                                    .trxCanal("IE")
                                    .build())
                            .build());
                }

            } else {
                templateCampoList.add(TemplateCampo.builder()
                        .feature("Crear Pago")
                        .tipo(value.toString().replace("#", "").replace("\"", ""))
                        .nombre(key)
                        .largo(10)
                        .negativo(false)
                        .status(400)
                        .response(ResponseTestTO.builder()
                                .codigo("400")
                                .mensaje(String.format("Error Entrada: %s es invalido", key))
                                .build())
                        .header(HeaderTO.builder()
                                .xRutCliente("0760210048")
                                .xRutUsuario("0099091800")
                                .trxCanal("IE")
                                .build())
                        .build());
            }
        }
        System.out.println(gson.toJson(templateCampoList));
    }

    private static String plantilla = "{\n" +
            "    \"cartaCreditoId\": \"#number\",\n" +
            "    \"negociacionId\": \"#number\",\n" +
            "    \"numeroNegociacion\": \"#number\",\n" +
            "    \"fecha\": \"#fecha\",\n" +
            "    \"tipoCambio\": \"#number\",\n" +
            "    \"totalMonto\": \"#number\",\n" +
            "    \"cuenta\": \"#string\",\n" +
            "    \"moneda\": \"#number\",\n" +
            "    \"capital\": {\n" +
            "      \"total\": \"#number\",\n" +
            "      \"monto\": \"#number\"\n" +
            "    },\n" +
            "    \"interes\": {\n" +
            "      \"total\": \"#number\",\n" +
            "      \"compuesto\": \"#number\",\n" +
            "      \"tasaAnual\": \"#number\",\n" +
            "      \"dias\": \"#number\",\n" +
            "      \"desde\": \"#fecha\",\n" +
            "      \"hasta\": \"#fecha\"\n" +
            "    },\n" +
            "    \"comision\": {\n" +
            "      \"moneda\": \"#number\",\n" +
            "      \"interesAdicional\": \"#number\",\n" +
            "      \"interesNegociacion\": \"#number\",\n" +
            "      \"interesFueraPlazo\": \"#number\",\n" +
            "      \"interesRetencionImpuesto\": \"#number\",\n" +
            "      \"impuestoCheque\": \"#number\",\n" +
            "      \"impuestoTimbre\": \"#number\",\n" +
            "      \"gastoSwift\": \"#number\",\n" +
            "      \"iva\": \"#number\",\n" +
            "      \"prepago\": \"#number\",\n" +
            "      \"planilla\": \"#number\",\n" +
            "      \"total\": \"#number\",\n" +
            "      \"cuenta\": \"#string\"\n" +
            "    },\n" +
            "    \"idPago\": \"#number\"\n" +
            "  }\n" +
            "  ";
}
