package com.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.ui.components.Avatar
import com.example.ui.components.Cover
import com.example.ui.components.Pill
import com.example.ui.components.PillVariant
import com.example.ui.components.ProgressBar
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonVariant
import com.example.ui.components.TbSectionHeader
import com.example.ui.components.TramabookCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class FoundationScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun foundation_components_render() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TbSectionHeader(title = "Componentes")
                    TbSectionHeader(
                        title = "Lendo agora",
                        action = { Pill(text = "Ver tudo", variant = PillVariant.Outline) },
                    )
                    TbButton(text = "Criar um clube", onClick = {}, variant = TbButtonVariant.Primary)
                    TbButton(text = "Sugerir livro", onClick = {}, variant = TbButtonVariant.Outline)
                    Pill(text = "Atual", variant = PillVariant.Terra)
                    ProgressBar(value = 0.62f)
                    Avatar(name = "Beatriz Almeida")
                    Avatar(name = "Beatriz Almeida", size = 56.dp)
                    Cover(title = "A Hora da Estrela", author = "Clarice Lispector")
                    TramabookCard {
                        Pill(text = "Em leitura", variant = PillVariant.Olive)
                    }
                }
            }
        }
        composeTestRule.onRoot()
            .captureRoboImage(filePath = "src/test/screenshots/foundation.png")
    }
}
