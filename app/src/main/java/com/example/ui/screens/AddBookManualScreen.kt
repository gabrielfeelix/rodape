package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CameraCaptureBox
import com.example.ui.components.Cover
import com.example.ui.components.TbButton
import com.example.ui.components.TbButtonSize
import com.example.ui.components.TbButtonVariant
import com.example.ui.components.RodapeCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.CoverFiles
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Tela de cadastro manual de livro — fallback quando OL+GB não acharam nada.
 *
 * Fluxo:
 *  - Campos obrigatórios: título, autor
 *  - Opcionais: ISBN, ano, editora, páginas, idioma
 *  - Capa: 3 modos — tirar foto, escolher da galeria, colar URL
 *  - Salvar cria Book(isManual=true) + ClubBook(suggested) e volta pra Suggest
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddBookManualScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onBookCreated: (bookId: String) -> Unit
) {
    val context = LocalContext.current

    // Estado dos campos — rememberSaveable pra não perder o formulário inteiro
    // na rotação/recriação da Activity.
    var titulo by rememberSaveable { mutableStateOf("") }
    var autor by rememberSaveable { mutableStateOf("") }
    var isbn by rememberSaveable { mutableStateOf("") }
    var ano by rememberSaveable { mutableStateOf("") }
    var editora by rememberSaveable { mutableStateOf("") }
    var paginas by rememberSaveable { mutableStateOf("") }
    var idioma by rememberSaveable { mutableStateOf("pt") }

    // Capa: pode ser path local (file://...) ou URL externa
    var coverPathOrUrl by rememberSaveable { mutableStateOf("") }
    var coverUrlInput by rememberSaveable { mutableStateOf("") }
    var showUrlInput by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    // Se o usuário tocou "Tirar foto" e a permissão ainda não estava concedida,
    // abrimos a câmera automaticamente assim que a concessão chegar.
    var pendingOpenCamera by remember { mutableStateOf(false) }
    // Marca que já pedimos a permissão ao menos uma vez — sem isso, "negado
    // permanente" (não concedido + sem rationale) é indistinguível de "nunca
    // perguntado" e o botão de configurações apareceria de cara.
    var permissionRequested by remember { mutableStateOf(false) }

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA) { granted ->
        permissionRequested = true
        if (granted && pendingOpenCamera) {
            pendingOpenCamera = false
            showCamera = true
        }
    }

    // Photo Picker — Android 11+ nativo, backport no AndroidX em <11
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val saved = CoverFiles.saveFromUri(context, uri)
            if (saved != null) coverPathOrUrl = saved
        }
    }

    // Validação
    val isbnDigits = isbn.filter { it.isDigit() || it == 'X' || it == 'x' }
    // Aceita ISBN-10 e ISBN-13 (antes só 13 dígitos rejeitava ISBN-10 legítimo).
    val isbnValid = isbn.isBlank() || isbnDigits.length == 13 || isbnDigits.length == 10
    val anoNum = ano.toIntOrNull()
    val anoValid = ano.isBlank() || (anoNum != null && anoNum in 1000..2100)
    val paginasNum = paginas.toIntOrNull()
    val paginasValid = paginas.isBlank() || (paginasNum != null && paginasNum in 1..10000)
    val formValid = titulo.trim().isNotEmpty() &&
        autor.trim().isNotEmpty() &&
        isbnValid && anoValid && paginasValid

    if (showCamera) {
        // Tela cheia de câmera
        Box(modifier = Modifier.fillMaxSize().background(RodapeTheme.colors.ink)) {
            CameraCaptureBox(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                onCaptured = { file ->
                    val saved = CoverFiles.saveFromFile(context, file)
                    if (saved != null) coverPathOrUrl = saved
                    showCamera = false
                },
                onError = {
                    // Antes fechava a câmera em silêncio — o usuário não sabia
                    // que a captura falhou. Agora avisa pra tentar de novo.
                    showCamera = false
                    Toast.makeText(
                        context,
                        "Não deu pra usar a câmera. Tente de novo.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
            IconButton(
                onClick = { showCamera = false },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(RodapeIcons.Close, contentDescription = "Fechar", tint = RodapeTheme.colors.cream)
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sugerir livro") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(RodapeIcons.Back, contentDescription = "Voltar")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Não achou o livro nas buscas? Cadastre aqui.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = LiterataFontFamily,
                        color = RodapeTheme.colors.muted
                    )
                )
            }

            // ── Capa ──────────────────────────────────────────────────
            item {
                Text(
                    text = "CAPA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = RodapeTheme.colors.muted,
                        letterSpacing = 1.sp
                    )
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preview da capa
                    Box(
                        modifier = Modifier
                            .size(width = 96.dp, height = 144.dp)
                            .clip(RoundedCornerShape(RodapeRadii.xs))
                            .background(RodapeTheme.colors.dividerSoft)
                    ) {
                        Cover(
                            title = titulo.ifBlank { "—" },
                            author = autor.ifBlank { "—" },
                            coverUrl = coverPathOrUrl,
                            width = 96.dp,
                            height = 144.dp
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TbButton(
                            text = "Tirar foto",
                            leadingIcon = RodapeIcons.Camera,
                            onClick = {
                                if (cameraPermission.status.isGranted) {
                                    showCamera = true
                                } else {
                                    // Marca a intenção pra o callback de concessão
                                    // abrir a câmera sozinho (sem exigir 2º toque).
                                    // NÃO setar permissionRequested aqui: o callback
                                    // (rememberPermissionState) já marca APÓS a resposta.
                                    // Setar no toque fazia o aviso "negada/abra config"
                                    // piscar ATRÁS do diálogo do sistema no 1º pedido.
                                    pendingOpenCamera = true
                                    cameraPermission.launchPermissionRequest()
                                }
                            },
                            variant = TbButtonVariant.Outline,
                            size = TbButtonSize.Sm,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TbButton(
                            text = "Da galeria",
                            leadingIcon = RodapeIcons.Image,
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            variant = TbButtonVariant.Outline,
                            size = TbButtonSize.Sm,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TbButton(
                            text = "URL",
                            leadingIcon = RodapeIcons.Link,
                            onClick = { showUrlInput = !showUrlInput },
                            variant = TbButtonVariant.Outline,
                            size = TbButtonSize.Sm,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (cameraPermission.status.shouldShowRationale) {
                    Text(
                        text = "Pra tirar foto da capa, o app precisa de permissão de câmera.",
                        style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else if (permissionRequested && !cameraPermission.status.isGranted) {
                    // Negado permanentemente: launchPermissionRequest() não abre
                    // mais diálogo. Único caminho é as configurações do sistema.
                    Text(
                        text = "Permissão de câmera negada. Abra as configurações do app pra liberar.",
                        style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.muted),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TbButton(
                        text = "Abrir configurações",
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        },
                        variant = TbButtonVariant.Outline,
                        size = TbButtonSize.Sm,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (showUrlInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = coverUrlInput,
                        onValueChange = { coverUrlInput = it },
                        label = { Text("URL da imagem") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    // Habilitado sempre: valida no clique e explica o
                                    // motivo, em vez de ficar apagado sem justificativa.
                                    if (coverUrlInput.trim().startsWith("https://")) {
                                        coverPathOrUrl = coverUrlInput.trim()
                                        showUrlInput = false
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Use um endereço https://",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ) { Text("Usar", color = RodapeTheme.colors.terracota) }
                        }
                    )
                }
            }

            // ── Dados obrigatórios ────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DADOS DO LIVRO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = RodapeTheme.colors.muted,
                        letterSpacing = 1.sp
                    )
                )
            }
            item {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { if (it.length <= 200) titulo = it },
                    label = { Text("Título *") },
                    singleLine = false,
                    maxLines = 2,
                    isError = titulo.isNotEmpty() && titulo.trim().isEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = autor,
                    onValueChange = { if (it.length <= 120) autor = it },
                    label = { Text("Autor *") },
                    placeholder = { Text("Nome do autor (ou autores separados por vírgula)") },
                    singleLine = true,
                    isError = autor.isNotEmpty() && autor.trim().isEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Dados opcionais ───────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "OPCIONAIS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = RodapeTheme.colors.muted,
                        letterSpacing = 1.sp
                    )
                )
            }
            item {
                OutlinedTextField(
                    value = isbn,
                    onValueChange = { if (it.length <= 17) isbn = it },
                    label = { Text("ISBN (10 ou 13 dígitos)") },
                    placeholder = { Text("978…") },
                    singleLine = true,
                    isError = !isbnValid,
                    supportingText = if (!isbnValid) {
                        { Text("ISBN deve ter 10 ou 13 dígitos", color = RodapeTheme.colors.terracota) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ano,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) ano = it },
                        label = { Text("Ano") },
                        singleLine = true,
                        isError = !anoValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = paginas,
                        onValueChange = { if (it.length <= 5 && it.all { c -> c.isDigit() }) paginas = it },
                        label = { Text("Páginas") },
                        singleLine = true,
                        isError = !paginasValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = editora,
                    onValueChange = { if (it.length <= 80) editora = it },
                    label = { Text("Editora") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                // Dropdown simples de idioma
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = when (idioma) {
                            "pt" -> "Português"
                            "en" -> "Inglês"
                            "es" -> "Espanhol"
                            else -> idioma
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Idioma") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("pt" to "Português", "en" to "Inglês", "es" to "Espanhol").forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    idioma = code
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Ações ─────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TbButton(
                        text = "Cancelar",
                        onClick = onNavigateBack,
                        variant = TbButtonVariant.Outline,
                        modifier = Modifier.weight(1f)
                    )
                    TbButton(
                        text = "Sugerir livro",
                        onClick = {
                            viewModel.createManualBook(
                                title = titulo,
                                author = autor,
                                isbn = isbn.takeIf { it.isNotBlank() },
                                anoPublicacao = anoNum,
                                editora = editora,
                                totalPaginas = paginasNum,
                                idioma = idioma,
                                coverPathOrUrl = coverPathOrUrl
                            ) { bookId ->
                                onBookCreated(bookId)
                            }
                        },
                        variant = TbButtonVariant.Terra,
                        enabled = formValid,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (!formValid && (titulo.isNotEmpty() || autor.isNotEmpty())) {
                    Text(
                        text = "Preencha pelo menos título e autor.",
                        style = MaterialTheme.typography.labelSmall.copy(color = RodapeTheme.colors.terracota),
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                    )
                }
            }
        }
    }
}
