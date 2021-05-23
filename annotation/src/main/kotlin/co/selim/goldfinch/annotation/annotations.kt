package co.selim.goldfinch.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class GenerateProperties(
  val visibility: Visibility = Visibility.INHERIT
)

enum class Visibility {
  PUBLIC, INTERNAL, INHERIT
}
