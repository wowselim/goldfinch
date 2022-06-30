package co.selim.goldfinch.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class GenerateProperties(
  val visibility: Visibility = Visibility.INHERIT,
  /**
   * Controls whether the generated property containers should be declared inside
   * the sealed interface or on the top level.
   */
  val level: Level = Level.NESTED,
)

enum class Visibility {
  PUBLIC, INTERNAL, INHERIT
}

enum class Level {
  TOP, NESTED
}
