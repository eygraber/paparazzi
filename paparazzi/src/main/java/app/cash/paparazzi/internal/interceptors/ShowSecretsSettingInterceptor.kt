package app.cash.paparazzi.internal.interceptors

internal object ShowSecretsSettingShouldShowTouchInputInterceptor {
  @JvmStatic
  fun intercept(): Boolean = false
}

internal object ShowSecretsSettingShouldShowPhysicalInputInterceptor {
  @JvmStatic
  fun intercept(): Boolean = false
}

internal object ShowSecretsSettingRegisterCallbackInterceptor {
  @JvmStatic
  fun intercept(): Runnable = Runnable {}
}
