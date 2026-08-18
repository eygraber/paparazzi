package app.cash.paparazzi.agent

import app.cash.paparazzi.getFieldReflectively
import app.cash.paparazzi.setStaticValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.lang.reflect.Modifier

class FinalFieldStripperTest {
  @Test
  fun stripsFinalFromStaticFieldsOfTargetAndNestedClasses() {
    FinalFieldStripper.stripFinalFromStaticFields(FIXTURE_CLASS)

    // The fixture is referenced only by name so it loads after the stripper is registered;
    // final can only be stripped at class load time.
    val fixtureField = Class.forName(FIXTURE_CLASS).getFieldReflectively("VALUE")
    assertThat(Modifier.isFinal(fixtureField.modifiers)).isFalse()
    assertThat(fixtureField.get(null)).isEqualTo("fixture")
    fixtureField.setStaticValue("fixture changed")
    assertThat(fixtureField.get(null)).isEqualTo("fixture changed")

    val nestedField = Class.forName("$FIXTURE_CLASS\$Nested").getFieldReflectively("VALUE")
    assertThat(Modifier.isFinal(nestedField.modifiers)).isFalse()
    nestedField.setStaticValue("nested changed")
    assertThat(nestedField.get(null)).isEqualTo("nested changed")
  }

  object Fixture {
    @JvmField
    val VALUE: String = "fixture"

    object Nested {
      @JvmField
      val VALUE: String = "nested"
    }
  }

  companion object {
    private const val FIXTURE_CLASS = "app.cash.paparazzi.agent.FinalFieldStripperTest\$Fixture"
  }
}
