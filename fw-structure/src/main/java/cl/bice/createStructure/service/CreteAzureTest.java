package cl.bice.createStructure.service;

import cl.bice.createStructure.to.test.TemplateCampo;
import cl.bice.createStructure.to.to.TestCaseTO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreteAzureTest {

    public static void main(String[] args) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        TemplateCampo[] templateCampoList = gson.fromJson(template, TemplateCampo[].class);
        List<TestCaseTO> testCaseTOList = new ArrayList<>();

        for (TemplateCampo templateCampo : templateCampoList) {
            if(templateCampo.getTipo().equals("number")) {
                testCaseTOList.add(TestCaseTO.builder()
                        .idAzure("")
                        .feature(templateCampo.getFeature())
                        .testCase("NUMBER STRING - " + templateCampo.getNombre())
                        .header(templateCampo.getHeader())
                        .campo(templateCampo.getNombre())
                        .valor("HOLA")
                        .status(templateCampo.getStatus())
                        //.response(new Gson().toJson(templateCampo.getResponse()))
                        .build());
                if(templateCampo.getLargo() > 0) {
                    testCaseTOList.add(TestCaseTO.builder()
                            .idAzure("")
                            .feature(templateCampo.getFeature())
                            .testCase("NUMBER MAX " + templateCampo.getLargo() + " - " + templateCampo.getNombre())
                            .header(templateCampo.getHeader())
                            .campo(templateCampo.getNombre())
                            .valor(getValorMax(templateCampo.getLargo()))
                            .status(templateCampo.getStatus())
                            //.response(new Gson().toJson(templateCampo.getResponse()))
                            .build());
                }

                if(!templateCampo.isNegativo()) {
                    testCaseTOList.add(TestCaseTO.builder()
                            .idAzure("")
                            .feature(templateCampo.getFeature())
                            .testCase("NUMBER NEGATIVO - " + templateCampo.getNombre())
                            .header(templateCampo.getHeader())
                            .campo(templateCampo.getNombre())
                            .valor("-1")
                            .status(templateCampo.getStatus())
                            //.response(new Gson().toJson(templateCampo.getResponse()))
                            .build());
                }
            }

            if(templateCampo.getTipo().equals("string")) {
                testCaseTOList.add(TestCaseTO.builder()
                        .idAzure("")
                        .feature(templateCampo.getFeature())
                        .testCase("STRING MAX " + templateCampo.getLargo() + " - " + templateCampo.getNombre())
                        .header(templateCampo.getHeader())
                        .campo(templateCampo.getNombre())
                        .valor(getValorMax(templateCampo.getLargo()))
                        .status(templateCampo.getStatus())
                        //.response(new Gson().toJson(templateCampo.getResponse()))
                        .build());
            }

            if(templateCampo.getTipo().equals("object")) {
                testCaseTOList.add(TestCaseTO.builder()
                        .idAzure("")
                        .feature(templateCampo.getFeature())
                        .testCase("OBJETO STRING - " + templateCampo.getNombre())
                        .header(templateCampo.getHeader())
                        .campo(templateCampo.getNombre())
                        .valor("HOLA")
                        .status(templateCampo.getStatus())
                        //.response(new Gson().toJson(templateCampo.getResponse()))
                        .build());
            }

            if(templateCampo.getTipo().equals("fecha")) {
                testCaseTOList.add(TestCaseTO.builder()
                        .idAzure("")
                        .feature(templateCampo.getFeature())
                        .testCase("FECHA HOLA - " + templateCampo.getNombre())
                        .header(templateCampo.getHeader())
                        .campo(templateCampo.getNombre())
                        .valor("HOLA")
                        .status(templateCampo.getStatus())
                        //.response(new Gson().toJson(templateCampo.getResponse()))
                        .build());
                testCaseTOList.add(TestCaseTO.builder()
                        .idAzure("")
                        .feature(templateCampo.getFeature())
                        .testCase("FECHA INVALIDA INTEGER - " + templateCampo.getNombre())
                        .header(templateCampo.getHeader())
                        .campo(templateCampo.getNombre())
                        .valor("20231219")
                        .status(templateCampo.getStatus())
                        //.response(new Gson().toJson(templateCampo.getResponse()))
                        .build());
                testCaseTOList.add(TestCaseTO.builder()
                        .idAzure("")
                        .feature(templateCampo.getFeature())
                        .testCase("FECHA INVALIDA FORMATO - " + templateCampo.getNombre())
                        .header(templateCampo.getHeader())
                        .campo(templateCampo.getNombre())
                        .valor("2023/12/19")
                        .status(templateCampo.getStatus())
                        //.response(new Gson().toJson(templateCampo.getResponse()))
                        .build());
            }
        }

        File outputFile = new File(System.getProperty("user.dir") + "/testUpdate/test.json");

        FileUtils.writeLines(outputFile, converLines(gson.toJson(testCaseTOList)));
    }

    private static List<String> converLines(String json) {

        String[] lines = json.split("\\n");
        List<String> fileString = Arrays.asList(lines);
        fileString.replaceAll(s -> s.replace("\\u0027", "'"));
        return fileString;
    }

    private static String getValorMax(int largo) {
        StringBuilder valor = new StringBuilder();
        for (int i = -1; i < largo; i++) {
            valor.append("1");
        }
        return valor.toString();
    }

    private static String template = "[\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"cartaCreditoId\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: cartaCreditoId es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"negociacionId\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: negociacionId es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"numeroNegociacion\",\n" +
            "    \"largo\": 2,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: numeroNegociacion es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"fecha\",\n" +
            "    \"nombre\": \"fecha\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: fecha es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"tipoCambio\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: tipoCambio es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"totalMonto\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: totalMonto es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"string\",\n" +
            "    \"nombre\": \"cuenta\",\n" +
            "    \"largo\": 12,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: cuenta es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"moneda\",\n" +
            "    \"largo\": 3,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: moneda es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"object\",\n" +
            "    \"nombre\": \"capital\",\n" +
            "    \"largo\": 0,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: capital es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"capital.total\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: capital.total es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"capital.monto\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: capital.monto es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"object\",\n" +
            "    \"nombre\": \"interes\",\n" +
            "    \"largo\": 0,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: interes es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"interes.total\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: interes.total es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"interes.compuesto\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: interes.compuesto es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"interes.tasaAnual\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: interes.tasaAnual es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"interes.dias\",\n" +
            "    \"largo\": 4,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: interes.dias es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"fecha\",\n" +
            "    \"nombre\": \"interes.desde\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: interes.desde es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"fecha\",\n" +
            "    \"nombre\": \"interes.hasta\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: interes.hasta es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"object\",\n" +
            "    \"nombre\": \"comision\",\n" +
            "    \"largo\": 0,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: comision es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"comision.moneda\",\n" +
            "    \"largo\": 3,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: comision.moneda es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"comision.interesAdicional\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: comision.interesAdicional es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"comision.interesNegociacion\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: comision.interesNegociacion es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"comision.interesFueraPlazo\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: comision.interesFueraPlazo es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"comision.interesRetencionImpuesto\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: comision.interesRetencionImpuesto es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"comision.impuestoCheque\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: comision.impuestoCheque es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"comision.impuestoTimbre\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: comision.impuestoTimbre es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"comision.gastoSwift\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: comision.gastoSwift es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"comision.iva\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: comision.iva es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"comision.prepago\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: comision.prepago es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"comision.planilla\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: comision.planilla es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"comision.total\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: comision.total es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"string\",\n" +
            "    \"nombre\": \"comision.cuenta\",\n" +
            "    \"largo\": 12,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: comision.cuenta es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  },\n" +
            "  {\n" +
            "    \"feature\": \"Crear Pago\",\n" +
            "    \"tipo\": \"number\",\n" +
            "    \"nombre\": \"idPago\",\n" +
            "    \"largo\": 20,\n" +
            "    \"negativo\": false,\n" +
            "    \"status\": 400,\n" +
            "    \"response\": {\n" +
            "      \"codigo\": \"400\",\n" +
            "      \"mensaje\": \"Error Entrada: idPago es invalido\"\n" +
            "    },\n" +
            "    \"header\": {\n" +
            "      \"trx-canal\": \"IE\",\n" +
            "      \"x-rut-cliente\": \"0760210048\",\n" +
            "      \"x-rut-usuario\": \"0099091800\"\n" +
            "    }\n" +
            "  }\n" +
            "]";
}
