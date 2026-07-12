package com.example.ui.components

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.theme.Cream
import com.example.ui.theme.Terracota
import java.io.File
import java.util.concurrent.Executor

/**
 * Caixa de captura de foto com CameraX + Compose.
 *
 * Mostra preview da câmera traseira e um botão circular grande no rodapé pra
 * disparar a foto. Quando a captura termina com sucesso, invoca [onCaptured]
 * com o arquivo gerado em `context.cacheDir/capture_<timestamp>.jpg` —
 * o caller é responsável por mover/copiar pra filesDir definitivo (usar [CoverFiles.saveFromFile]).
 *
 * Pré-requisitos:
 *  - Permissão CAMERA já concedida (use AccompanistPermissions antes de renderizar)
 *  - libs CameraX habilitadas no build.gradle.kts
 */
@Composable
fun CameraCaptureBox(
    modifier: Modifier = Modifier,
    onCaptured: (File) -> Unit,
    onError: (Throwable) -> Unit = {}
) {
    val context: Context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor: Executor = remember(context) { ContextCompat.getMainExecutor(context) }

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    // Mensagem de erro visível no próprio overlay — garante feedback mesmo
    // quando o caller usa o onError padrão (vazio) do componente.
    var errorMsg by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                errorMsg = "Não deu pra iniciar a câmera. Tenta de novo."
                onError(e)
            }
        }, executor)

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
        }
    }

    Box(modifier = modifier) {
        // Preview
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
        )

        // Frame de detecção (decorativo) — bordas cremes nos cantos
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 240.dp, height = 320.dp)
                .background(Color.Transparent)
        ) {
            // Apenas overlay leve dos cantos
            CornerOverlay(modifier = Modifier.matchParentSize())
        }

        // Botão de captura
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(72.dp)
                .clip(CircleShape)
                .background(Cream)
                .padding(6.dp)
                .clip(CircleShape)
                .background(Terracota)
                // A11y: o botão era um emoji dentro de um Box clicável sem rótulo.
                .semantics { contentDescription = "Tirar foto"; role = Role.Button }
                .clickable {
                    errorMsg = null
                    val outFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                    val output = ImageCapture.OutputFileOptions.Builder(outFile).build()
                    imageCapture.takePicture(
                        output,
                        executor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                                onCaptured(outFile)
                            }
                            override fun onError(exception: ImageCaptureException) {
                                errorMsg = "Falha ao capturar a foto. Tenta de novo."
                                onError(exception)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📷",
                style = MaterialTheme.typography.titleLarge.copy(color = Cream)
            )
        }

        // Feedback de erro visível no overlay (captura ou bind falhou)
        errorMsg?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Terracota)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Cream)
                )
            }
        }
    }
}

@Composable
private fun CornerOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        val cornerLength = 28.dp
        val cornerThickness = 3.dp
        // top-left
        Box(modifier = Modifier.align(Alignment.TopStart).size(cornerLength, cornerThickness).background(Cream))
        Box(modifier = Modifier.align(Alignment.TopStart).size(cornerThickness, cornerLength).background(Cream))
        // top-right
        Box(modifier = Modifier.align(Alignment.TopEnd).size(cornerLength, cornerThickness).background(Cream))
        Box(modifier = Modifier.align(Alignment.TopEnd).size(cornerThickness, cornerLength).background(Cream))
        // bottom-left
        Box(modifier = Modifier.align(Alignment.BottomStart).size(cornerLength, cornerThickness).background(Cream))
        Box(modifier = Modifier.align(Alignment.BottomStart).size(cornerThickness, cornerLength).background(Cream))
        // bottom-right
        Box(modifier = Modifier.align(Alignment.BottomEnd).size(cornerLength, cornerThickness).background(Cream))
        Box(modifier = Modifier.align(Alignment.BottomEnd).size(cornerThickness, cornerLength).background(Cream))
    }
}

