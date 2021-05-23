package co.selim.goldfinch.example

import co.selim.goldfinch.annotation.GenerateProperties
import co.selim.goldfinch.annotation.Visibility
import java.time.LocalDate

@GenerateProperties(visibility = Visibility.INTERNAL)
data class Person(
  val name: String,
  val dateOfBirth: LocalDate,
)

fun main() {
  val selim = Person("Selim", LocalDate.of(1970, 1, 1))

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
}
