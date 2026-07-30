package simulations.petstore

import com.intuit.karate.gatling.PreDef._
import io.gatling.core.Predef._
import scala.concurrent.duration._

class PetSimulation extends Simulation {
  val protocol = karateProtocol()
  val scen = scenario("pet").exec(
    karateFeature("classpath:features/petstore/performance/feature/pet.feature")
  )
  // La feature de performance ya trae 30 filas de datos (data.pet/pet.json);
  // cada Gatling user ejecuta las 30 filas via el Examples de Karate.
  setUp(
    scen.inject(atOnceUsers(1))
  ).protocols(protocol)
}
