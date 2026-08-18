package app.cash.paparazzi.plugin.test

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

class SecureTextFieldTest {
  @get:Rule
  val paparazzi = Paparazzi()

  @Test
  fun secureTextField() {
    paparazzi.snapshot {
      BasicSecureTextField(
        state = rememberTextFieldState("hunter2"),
        modifier = Modifier.padding(16.dp)
      )
    }
  }
}
