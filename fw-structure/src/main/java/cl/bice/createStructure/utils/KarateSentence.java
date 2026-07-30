package cl.bice.createStructure.utils;

/**
 * Constantes de sintaxis Karate — Nuevos lineamientos COE QA 2026
 */
public class KarateSentence {

    private KarateSentence() {
        throw new IllegalStateException("Utility class");
    }

    // ── FEATURE TAGS ───────────────────────────────────────────────────────────
    /** @{svcShort}_{method} @baas */
    public static final String FEATURE_TAG_BAAS   = "@%s_%s @baas";
    /** @obapi_{component}_{method} @obapis */
    public static final String FEATURE_TAG_OBAPI  = "@obapi_%s_%s @obapis";
    /** Feature: {operationId} - {METHOD} */
    public static final String FEATURE_DECLARATION = "Feature: %s - %s";

    // ── BACKGROUND ─────────────────────────────────────────────────────────────
    public static final String BG_OPEN           = "  Background:";
    public static final String BG_SSL_TRUST_ALL  = "    * configure ssl = { trustAll: true }";
    public static final String BG_SSL_KEYSTORE   = "    * configure ssl = { keyStore: 'classpath:cert/%s', keyStorePassword: '%s', keyStoreType: 'pkcs12' }";
    public static final String BG_URL            = "    * url host.%s";
    public static final String BG_TOKEN_FEATURE  = "    * def token_feature = read('classpath:utils/steps/get_token.feature@get_token')";

    // ── HEADERS EN ESCENARIO ───────────────────────────────────────────────────
    /** Siempre PRIMERO: obtener token */
    public static final String HDR_TOKEN_DEF     = "    * def token = call token_feature { 'username': 'user', 'password': 'pass' }";
    public static final String HDR_AUTHORIZATION = "    * header Authorization = `Bearer ${token}`";
    public static final String HDR_CONTENT_TYPE  = "    * header Content-Type = 'application/json'";
    public static final String HDR_X_TARGET_UNIT = "    * header X-Target-Unit = 'OBDX_BU'";
    public static final String HDR_X_TOKEN_TYPE  = "    * header X-Token-Type = 'JWT'";
    /** header genérico */
    public static final String HDR_GENERIC       = "    * header %s = '%s'";

    // ── PERFORMANCE ────────────────────────────────────────────────────────────
    public static final String TAG_PERFORMANCE   = "  @performance=%s @env=perf";
    public static final String SCENARIO_PERF     = "  Scenario: performance";
    public static final String PERF_PARAM        = "    * def param = karate.get('__gatling.PARAM', 'default')";

    // ── SCENARIO OUTLINE ───────────────────────────────────────────────────────
    public static final String TAG_TEST_CASE     = "  @TestCase=NEW";
    public static final String TAG_ADO           = "  @tagADO=%s_%s";
    public static final String SCENARIO_OUTLINE  = "  Scenario Outline: %s %s";

    // ── PASOS KARATE ───────────────────────────────────────────────────────────
    public static final String GIVEN_PATH         = "    Given path %s";
    public static final String AND_REQUEST_EXT    = "    And request read('classpath:feature/%s/%s/request/%s_%s.json')";
    public static final String AND_REQUEST_INLINE = "    And request";
    public static final String DOCSTRING          = "      \"\"\"";
    public static final String WHEN_METHOD        = "    When method %s";
    public static final String THEN_STATUS        = "    Then status <_STATUS>";
    public static final String THEN_STATUS_200    = "    Then status 200";
    public static final String AND_MATCH_SCHEMA   = "    And match response == schema";
    public static final String AND_MATCH_HEADER   = "    And match responseHeaders['Content-Type'][0] contains 'application/json'";
    public static final String AND_MATCH_ERROR    = "    And match response.error == '<_ERROR_MESSAGE>'";

    // ── EXAMPLES ───────────────────────────────────────────────────────────────
    public static final String EXAMPLES           = "    Examples:";

    // ── SEPARADORES DE SECCIÓN ─────────────────────────────────────────────────
    public static final String SEP               = "# ==============================================================";
    public static final String SEC_PERF          = "# PERFORMANCE - SIEMPRE PRIMERO (después del Background)";
    public static final String SEC_200           = "# ESCENARIOS 200 - Casos de éxito";
    public static final String SEC_400           = "# ESCENARIOS 400 - Errores de cliente (validación)";
    public static final String SEC_500           = "# ESCENARIOS 500 - Errores de servidor";
    public static final String SEC_SCHEMA        = "# SCHEMA - Validación de contrato completo";
    public static final String SEC_HEADER        = "# HEADER - Validación de headers de respuesta";
}
