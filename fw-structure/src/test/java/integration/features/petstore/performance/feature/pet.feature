  @performance=pet @env=perf
Feature: pet - POST (performance)

  Background:
    * configure ssl = { trustAll: true }
    * url host.petstore_qa

  Scenario Outline: performance - pet
    * header Content-Type = 'application/json'
    Given path 'v2','pet'
    And request read('classpath:features/petstore/performance/request/petBody.json')
    * set request.name = _NAME
    * set request.id = _ID
    * set request.status = _STATUS_FIELD
    When method POST
    Then status <_STATUS>
    Examples:
      | read('classpath:features/petstore/performance/data.pet/pet.json') |
