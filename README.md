# BaaS Services — Karate Feature Generator

Aplicación Spring Boot que genera automáticamente, a partir de un cURL real,
la estructura de tests Karate en **3 capas separadas — functional /
performance / structure —**, cada una con su carpeta de datos y su feature
propios, igual a como ya lo hacía tu proyecto real — sin ningún nombre ni dato
atado a una empresa en particular.

Soporta dos tipos de API:
- **OBAPI** — Oracle Banking APIs con JWT (`X-Token-Type`, `X-Target-Unit`, token vía `get_token.feature`)
- **Services / BaaS** — Microservicios REST internos (solo `Content-Type`)

---

## ¿Cómo usar?

### 1. Configurar `application.properties` (en la raíz del proyecto)

```properties
# Ruta absoluta al proyecto Karate destino — la carpeta "integration" misma,
# NO la carpeta del proyecto ni "src/test/java" a secas.
# Ej: .../mi-proyecto-karate/src/test/java/integration
karate.project.root=/Users/nombre/Documents/GitHub/mi-proyecto-karate/src/test/java/integration

# Namespace: baas | obapis
namespace=baas

# Tipo de API: auto (recomendado), obapi, services
api.type=auto

# Pegar aquí el cURL completo
curl=curl --location 'https://mi-servicio-qa.midominio.com/api/v1/recurso' \
--header 'Content-Type: application/json' \
--data '{...}'
```

### 2. Ejecutar

```bash
mvn spring-boot:run
```

O con el JAR:
```bash
java -jar baas-services-structure-test-framework-1.0.0.jar
```

### 3. (Alternativa) Todo por línea de comandos, sin editar el .properties

```bash
java -jar baas-services-structure-test-framework-1.0.0.jar \
  --project-dir=/ruta/al/proyecto-karate/src/test/java \
  --namespace=baas \
  --component=apiPagos \
  --curl="curl --location 'https://api.midominio.com/v1/pagos' --header 'Content-Type: application/json' --data '{...}'" \
  --swagger=/ruta/al/openapi.json \
  --no-swagger-discover
```

Argumentos soportados: `--namespace`, `--project-dir`, `--curl`, `--api-type`,
`--component` (fuerza el nombre del endpoint en vez de derivarlo de la URL),
`--swagger` (ruta local a un OpenAPI) y `--no-swagger-discover` (desactiva el
descubrimiento remoto de swagger). Cualquiera que no se pase, se toma de
`application.properties`.

### 4. (Alternativa) IntelliJ — Run Configuration lista para usar

El proyecto trae una Run Configuration ya armada
(**`.idea/runConfigurations/`**), así que al abrirlo en IntelliJ aparece en
el dropdown de configuraciones sin que tengas que crear nada:
**"Generar feature - ejemplo jsonplaceholder"**. Solo descomprimir, abrir el
proyecto (dejar que Maven importe las dependencias) y correrla — genera el
`.feature` de ejemplo dentro de `ejemplo-destino-karate/`.

Para usarla con TU propio endpoint, andá a **Run → Edit Configurations** y
editá estas variables de entorno:

| Variable | Uso |
|---|---|
| `curl` | cURL completo del endpoint (obligatoria) |
| `nameSpace` | Namespace / carpeta padre (ej: `apiAbono`) |
| `pathRelativeFolder` | Ruta a `...\src\test\java\integration` de tu proyecto Karate destino |
| `ambiente` | `qa` o `dev` (opcional; default `qa`) — decide si actualiza `env_qa.json` o `env_dev.json` |
| `planId` / `suiteId` | (opcional, referencia Azure DevOps — no se usan en la generación) |
| `swaggerUrl` | (opcional) URL pública/interna a un swagger.json/openapi.json (ej: `https://petstore.swagger.io/v2/swagger.json`) |
| `swaggerFile` | (opcional) ruta local a un `api-docs.json` para refinar tipos de campo |
| `swaggerAutoDiscover` | `true`/`false` (default `true`) — si no hay `swaggerUrl` ni `swaggerFile`, intenta descubrirlo por red |

> Nota: si al abrir el proyecto IntelliJ marca el módulo de la Run
> Configuration como inválido (pasa si tu versión de IntelliJ nombra los
> módulos distinto al importar Maven), solo hace falta reseleccionarlo una
> vez en el dropdown "Use classpath of module" — el resto de la
> configuración (env vars, main class) queda igual.


---

## Detección automática OBAPI vs Services

El generador detecta el tipo de API por:

| Indicador | Tipo |
|-----------|------|
| URL contiene `obapi` | OBAPI |
| Header `X-Token-Type` presente | OBAPI |
| Header `X-Target-Unit` presente | OBAPI |
| Ninguno de los anteriores | Services/BaaS |

También se puede forzar con `api.type=obapi` o `api.type=services`.

---

## Validación de campos del body (detección de tipo + casos de borde)

Por cada campo del body real (del cURL) se detecta un tipo y, según ese tipo,
se generan casos de borde distintos en la sección `400` del `.feature`:

| Tipo detectado | Cómo se detecta | Casos de borde generados |
|---|---|---|
| `string` | valor por defecto | nulo, vacío |
| `email` | nombre del campo contiene `email`/`correo`/`mail`, o el valor tiene formato `algo@algo.com` | nulo, vacío, formato inválido (sin `@`) |
| `integer` (number) | el valor es numérico | nulo, texto donde debería ir número, negativo |
| `boolean` | el valor es `true`/`false` | nulo, valor no booleano |
| `date` | nombre del campo contiene `fecha`/`date`, o el valor tiene formato de fecha | nulo, formato inválido |

Cada caso de borde genera una fila en la tabla `Examples`, aislando **un solo
campo** por fila (el resto queda con su valor válido de ejemplo).

### Swagger/OpenAPI opcional — para afinar los tipos

Si el cURL no alcanza para inferir bien un tipo (ej. un campo numérico que en
el ejemplo vino como texto), el generador puede usar un Swagger/OpenAPI para
corregirlo. Soporta tanto **OpenAPI 3.0** como **Swagger 2.0** clásico (el de
`https://petstore.swagger.io/v2/swagger.json`, por ejemplo — con `basePath` y
el body como parámetro `"in": "body"` en vez de `requestBody`). Hay cuatro
fuentes, en este orden de prioridad:

1. **URL explícita** — `swaggerUrl=https://petstore.swagger.io/v2/swagger.json`
   (o cualquier swagger público/interno). El generador hace `GET` directo a
   esa URL exacta. **Esta es la que ponés cuando ya sabés dónde vive el
   swagger del servicio** (no necesitás adivinar rutas).
2. **Archivo local explícito** — `swaggerFile=/ruta/al/openapi.json`, para
   cuando tenés el contrato guardado en disco en vez de una URL.
3. **Descubrimiento remoto automático** (activado por defecto, solo si no
   diste `swaggerUrl` ni `swaggerFile`) — hace `GET` al mismo host del cURL
   sobre rutas comunes: `/v3/api-docs`, `/v2/api-docs`, `/swagger.json`,
   `/swagger/v1/swagger.json`, `/openapi.json`, `/api-docs`.
4. **Solo el cURL** — si ninguna de las anteriores encuentra nada (o el
   endpoint no está documentado ahí), el generador sigue funcionando igual,
   sin fallar, usando solo lo que infirió del cURL.

Para desactivar el paso 3 (por ejemplo si no querés que el generador salga a
la red al correr), configurar `swaggerAutoDiscover=false`.

```properties
swaggerUrl=
swaggerFile=
swaggerAutoDiscover=true
```

**Probado con el Petstore público**: con
`swaggerUrl=https://petstore.swagger.io/v2/swagger.json` y un cURL a
`POST https://petstore.swagger.io/v2/pet`, el generador toma el `id` como
`integer` real (del swagger, no adivinado) y arma sus 3 casos de borde
(nulo / tipo inválido / negativo), mientras que `name`/`status` quedan como
`string` (nulo / vacío) — igual con cualquier otro swagger, público o interno.

---

## Archivos generados

Estructura por 3 capas — **functional / performance / structure** — igual a
como ya lo hacía tu proyecto real, cada una con su carpeta de datos separada
del feature:

```
{pathRelativeFolder}/                          (ej: .../src/test/java/integration)
  features/
    {namespace}/
      functional/
        data.{operationId}/
          {operationId}.json      ← 1 fila (el caso 200), para el Examples
        feature/
          {operationId}.feature   ← 200 + schema + headers + 500 (best-effort)
        request/
          {operationId}Body.json  ← body real y literal del cURL
      performance/
        data.{operationId}/
          {operationId}.json      ← 30 filas (mismos valores reales + _ITER 1..30)
        feature/
          {operationId}.feature   ← lee el body real de request/ y lo pisa con * set
        request/
          {operationId}Body.json  ← el MISMO body real y literal (JSON normal, sin marcadores)
      structure/
        data.{operationId}/
          {operationId}.json      ← 1 fila por cada campo x caso de borde
        feature/
          {operationId}.feature   ← lee el body real de request/ y lo pisa con * set
        request/
          {operationId}Body.json  ← el MISMO body real y literal (JSON normal, sin marcadores)
  utils/steps/
    get_token.feature                 ← Solo para OBAPI
  env/
    env_qa.json | env_dev.json        ← Acumulativo, agrega el host sin borrar nada

{pathRelativeFolder}/../simulations/            (HERMANA de "integration", no adentro)
  {namespace}/
    {OperationId}Simulation.scala     ← Gatling, corre la feature de performance
```

Los 3 `.feature` de una misma operación leen su body real y válido desde
`request/{operationId}Body.json` (sin marcadores — es el JSON tal cual salió
del cURL) y, en performance/structure, lo pisan campo por campo con
`* set request.campo = _VAR` usando el valor de esa fila de `Examples:`
(leída de `data.{operationId}/{operationId}.json`). No se usan expresiones
`#(...)` dentro de un `.json` externo: Karate no las evalúa igual quen cuando
están inline en el feature, así que en vez de eso se lee el body real y se
sobreescribe con `set` — el `null`/número/string real de cada fila queda
igual de bien representado.

---

## Escenarios generados por capa

Todos los escenarios son de **tipo Outline** (lineamiento obligatorio).

**functional/feature/{operationId}.feature**

| Tag | Descripción |
|-----|-------------|
| `@TestCase=NEW @tagADO={op}_200` | Caso exitoso con el body real del cURL — incluye la validación de schema (genérica y segura: `match response == '#present'`, reemplazable por un schema real si tenés Swagger) y de headers en el mismo escenario |
| `@TestCase=NEW @tagADO={op}_500` | Error de servidor — **best-effort**, marcado explícitamente para que ajustes cómo tu servicio realmente dispara un 500 |

**performance/feature/{operationId}.feature**

| Tag | Descripción |
|-----|-------------|
| `@performance={op} @env=perf` | 1 Scenario Outline, Examples con las 30 filas de `data.{op}/{op}.json` |

**structure/feature/{operationId}.feature**

| Tag | Descripción |
|-----|-------------|
| `@TestCase=NEW @tagADO={op}_400` | Un caso de borde por cada campo real del body según su tipo (nulo/vacío para string, +formato inválido para email/fecha, +tipo inválido/negativo para número, +tipo inválido para booleano) |

---

## Diferencias OBAPI vs Services en la feature generada

**OBAPI** — Background:
```karate
Background:
  * configure ssl = { trustAll: true }
  * url host.obapi_qa
  * def token_feature = read('classpath:utils/steps/get_token.feature@get_token')
```

**OBAPI** — Cada escenario (token SIEMPRE PRIMERO):
```karate
* def token = call token_feature { 'username': 'user', 'password': 'pass' }
* header Authorization = `Bearer ${token}`
* header Content-Type = 'application/json'
* header X-Target-Unit = 'OBDX_BU'
* header X-Token-Type = 'JWT'
```

**Services/BaaS** — Background:
```karate
Background:
  * configure ssl = { trustAll: true }
  * url host.api_baas
```

**Services/BaaS** — Cada escenario: los headers **reales del cURL** (todos,
no solo `Content-Type`) — ej. `X-Channel`, `trx-canal`, `trx-id`, etc.

---

## Reglas críticas aplicadas

- ✅ Token NUNCA hardcodeado — siempre vía `get_token.feature`
- ✅ URL NUNCA hardcodeada — siempre `host.{key}` desde `env_qa.json`
- ✅ Path con comas: `Given path 'seg1','v1','seg2'`
- ✅ Máximo 19 columnas en Examples (límite Azure DevOps)
- ✅ Body >19 params → archivo externo en `request/`
- ✅ Scenarios siempre tipo **Outline**
- ✅ Performance **SIEMPRE primero** después del Background
- ✅ Token llamado **antes** de Content-Type y otros headers

---

## Comandos Maven para ejecutar tests

```bash
# Todos los tests
mvn clean test

# Por escenario específico
mvn clean test 'Dkarate.options=--tags @tagADO=createtdpayIn_200'

# Por tipo de API
mvn clean test 'Dkarate.options=--tags @obapis'
mvn clean test 'Dkarate.options=--tags @baas'

# Performance (Gatling)
mvn clean test-compile gatling:test \
  -Dgatling.simulationClass=simulations.baas.CuentaCorrienteSimulation \
  -DfeaturePath=feature/baas/cuenta-corriente/post_retiros-cajero.feature@performance=retiros-cajero
```

---

## GitFlow

Branch: `{id_historia}_{componente}_{version}`

Ejemplos:
- `12345_ms-pagos_1.0`
- `67890_cuenta-corriente-retiro-cajero_1.0`
- `11111_tdpay_1.0`

