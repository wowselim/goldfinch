package co.selim.goldfinch.example

import co.selim.goldfinch.annotation.GenerateProperties
import co.selim.goldfinch.annotation.Level
import java.time.LocalDate

@GenerateProperties(level = Level.TOP)
internal data class Person(
  val name: String,
  val dateOfBirth: LocalDate,
)

@GenerateProperties
internal data class Animal(val name: String)

fun main() {

  val selim = Person("Selim Dinçer", LocalDate.of(1970, 1, 1))

  selim.properties
    .forEach { property ->
      val message = when (property) {
        is NameProperty -> "Name: ${property.name}"
        is DateOfBirthProperty -> {
          val currentYear = LocalDate.now()
          "Age: ${property.dateOfBirth.until(currentYear).years}"
        }
      }

      println(message)
    }

  val animal = Animal("Bello")

  animal.properties
    .forEach { property ->
      when (property) {
        is AnimalProperty.Name -> println("Name: ${property.name}")
      }
    }
}
