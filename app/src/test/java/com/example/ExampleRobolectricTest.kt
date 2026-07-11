package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric precisa baixar o runtime android-all (SDK 36) na 1a execucao.
// Em ambiente sem rede (sandbox de build) a init falha em SettingsUtil.kt.
// Rode num CI com acesso a rede pra reativar (remova o @Ignore).
@Ignore("Requer runtime Robolectric (android-all) — indisponivel offline")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Rodape", appName)
  }
}
