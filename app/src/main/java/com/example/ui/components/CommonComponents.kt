package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.Terracota

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSecondary: Boolean = false,
    textColor: Color = Color.White
) {
    val buttonColors = if (isSecondary) {
        ButtonDefaults.outlinedButtonColors(
            contentColor = Terracota
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = Terracota,
            contentColor = Color.White,
            disabledContainerColor = Terracota.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        )
    }

    val border = if (isSecondary) {
        BorderStroke(1.5.dp, Terracota)
    } else {
        null
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(26.dp),
        colors = buttonColors,
        border = border,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color = if (isSecondary) Terracota else textColor
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BookCover(
    coverUrl: String,
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    height: Dp = 150.dp
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(6.dp), clip = true)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
    ) {
        if (coverUrl.isNotEmpty()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = "Capa do livro",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No cover",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Spine fold simulation (left edge subtle shadow gradient)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(6.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
fun MemberAvatar(
    name: String,
    avatarUrl: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    if (avatarUrl.isNotEmpty()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "Avatar de $name",
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(0.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val initials = name.take(2).uppercase()
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                .border(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary
                )
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(
            thickness = 1.dp,
            color = Terracota.copy(alpha = 0.10f)
        )
    }
}

@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
    val cardBorder = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    )
    val cardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = cardColors,
            border = cardBorder,
            elevation = cardElevation
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = cardColors,
            border = cardBorder,
            elevation = cardElevation
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                content = content
            )
        }
    }
}
