package cl.bice.createStructure.utils;

public class KarateSentence {

    private KarateSentence() {
        throw new IllegalStateException("Utility class");
    }

    public static final String SCENARIO = "        Scenario Outline: <idTestAzure> - %s - %s <testCase>";
    public static final String PATH = "            Given path %s";
    public static final String HEADER = "              And headers <headers>";
    public static final String BODY = "              And request body";
    public static final String EMPTY_BODY = "              And set body.<campo> = ''";
    public static final String REMOVE_BODY = "              And remove body.<campo>";
    public static final String SET_BODY = "              And set body.<campo> = \"<valor>\"";

    public static final String SET_BODY_NUM = "              And set body.<campo> = <valor>";

    public static final String POST = "             When method POST";
    public static final String METHOD = "             When method %s";
    public static final String STATUS = "             Then status <status>";
    public static final String RESPONSE = "              And match response == <response>";
    public static final String RESPONSE_CONTAINS_SUCCESS = "                And match response.message contains \"Operación realizada correctamente\"";
    public static final String RESPONSE_CONTAINS = "                And match response.type contains \"REQUIRED_VALUE\"\n"+
            "              And match response.detail contains  ('<campo>').replaceAll(\"_\",\"\")";
    public static final String RESPONSE_CONTAINS_VALIDATION_EMPTY = "                And match response.type contains \"REQUIRED_VALUE\"\n"+
            "              And match response.title contains  \"El campo \"+'<campo>'+ \" es requerido\"\n"+
            "              And match response.detail contains  \"El campo '\"+'<campo>'+ \"' es obligatorio\"\n"+
            "              And match response.status == <status>";
    public static final String RESPONSE_CONTAINS_DEEP = "              And match response.message contains 'Error:Field validation for '\n"+
            "              And match response.message.toLowerCase() contains  ('<campo>').replaceAll(\"_\",\"\")";
    public static final String EXAMPLES = "        Examples:";
}
