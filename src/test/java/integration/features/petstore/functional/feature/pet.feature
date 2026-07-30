@pet_post @baas
Feature: pet - POST (functional)

  Background:
    * configure ssl = { trustAll: true }
    * url host.petstore_qa

# ==============================================================
# 200 + SCHEMA + HEADERS — camino feliz con el body real del cURL
# ==============================================================

  @TestCase=NEW
  @tagADO=pet_200
  Scenario Outline: pet exitoso - 200
    * header Content-Type = 'application/json'
    Given path 'v2','pet'
    And request read('classpath:features/petstore/functional/request/petBody.json')
    When method POST
    Then status <_STATUS>
    And match response == '#present'
    And match responseHeaders['Content-Type'][0] contains 'application/json'
    Examples:
      | read('classpath:features/petstore/functional/data.pet/pet.json') |

  # 'match response == "#present"' es una aserción genérica y segura (no inventa
  # nombres de campo). Si tenés el contrato real (swaggerFile), reemplazala por
  # 'match response == schema' con el schema real del servicio.

# ==============================================================
# 500 — best-effort: ajustá el body/condición según cómo tu servicio dispare un error real
# ==============================================================

  @TestCase=NEW
  @tagADO=pet_500
  Scenario Outline: pet error servidor - 500 (ajustar)
    * header Content-Type = 'application/json'
    Given path 'v2','pet'
    And request
      """
      { "trigger_error": true }
      """
    When method POST
    Then status <_STATUS>
    Examples:
      | _STATUS |
      | 500     |
