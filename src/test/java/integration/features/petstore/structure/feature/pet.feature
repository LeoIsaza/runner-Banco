@pet_post @baas
Feature: pet - POST (structure)

  Background:
    * configure ssl = { trustAll: true }
    * url host.petstore_qa

# ==============================================================
# 400 — un caso de borde por cada campo real del body, según su tipo detectado
# ==============================================================

  @TestCase=NEW
  @tagADO=pet_400
  Scenario Outline: pet error cliente - 400 (borde por campo)
    * header Content-Type = 'application/json'
    Given path 'v2','pet'
    And request read('classpath:features/petstore/structure/request/petBody.json')
    * set request.name = _NAME
    * set request.id = _ID
    * set request.status = _STATUS_FIELD
    When method POST
    Then status <_STATUS>
    Examples:
      | read('classpath:features/petstore/structure/data.pet/pet.json') |
