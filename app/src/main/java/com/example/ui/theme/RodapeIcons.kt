// Ícones do design — origem: claude-design/icons.jsx (stroke 1.6, round). Tintados via Icon(tint=...).
//
// Nota sobre cor: os vetores usam SolidColor(Color(0xFF000000)) como cor base.
// O componente Icon(tint = ...) aplica um ColorFilter que SUBSTITUI essa cor,
// então qualquer cor sólida serve — preto é apenas a convenção.
// Exceção: Google mantém as cores da marca (não tintar; usar tint = Color.Unspecified).
package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val Black = Color(0xFF000000)

private inline fun icon(
    name: String,
    block: ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply(block).build()

/** Path de contorno (stroke), padrão do design: 1.6px, pontas e junções arredondadas. */
private inline fun ImageVector.Builder.strokePath(
    strokeWidth: Float = 1.6f,
    crossinline block: PathBuilder.() -> Unit,
) {
    path(
        fill = null,
        stroke = SolidColor(Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) { block() }
}

/** Path preenchido (fill), usado por More, StarFill e Google. */
private inline fun ImageVector.Builder.fillPath(
    color: Color = Black,
    crossinline block: PathBuilder.() -> Unit,
) {
    path(fill = SolidColor(color)) { block() }
}

/** circle(cx, cy, r) como dois arcos. */
private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcToRelative(r, r, 0f, true, true, 2 * r, 0f)
    arcToRelative(r, r, 0f, true, true, -2 * r, 0f)
    close()
}

object RodapeIcons {

    // home: M3 11l9-8 9 8M5 10v10h14V10
    val Home: ImageVector by lazy {
        icon("Home") {
            strokePath {
                moveTo(3f, 11f)
                lineToRelative(9f, -8f)
                lineToRelative(9f, 8f)
                moveTo(5f, 10f)
                verticalLineToRelative(10f)
                horizontalLineToRelative(14f)
                verticalLineTo(10f)
            }
        }
    }

    // book: M4 4a2 2 0 0 1 2-2h12v18H6a2 2 0 0 1-2-2zM4 18a2 2 0 0 1 2-2h12
    val Book: ImageVector by lazy {
        icon("Book") {
            strokePath {
                moveTo(4f, 4f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(18f)
                horizontalLineTo(6f)
                arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
                close()
                moveTo(4f, 18f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
                horizontalLineToRelative(12f)
            }
        }
    }

    // calendar: M4 6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v14H4zM4 10h16M9 2v4M15 2v4
    val Calendar: ImageVector by lazy {
        icon("Calendar") {
            strokePath {
                moveTo(4f, 6f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
                horizontalLineToRelative(12f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
                verticalLineToRelative(14f)
                horizontalLineTo(4f)
                close()
                moveTo(4f, 10f)
                horizontalLineToRelative(16f)
                moveTo(9f, 2f)
                verticalLineToRelative(4f)
                moveTo(15f, 2f)
                verticalLineToRelative(4f)
            }
        }
    }

    // user: M4 21c0-4 3.5-7 8-7s8 3 8 7M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z
    val User: ImageVector by lazy {
        icon("User") {
            strokePath {
                moveTo(4f, 21f)
                curveToRelative(0f, -4f, 3.5f, -7f, 8f, -7f)
                reflectiveCurveToRelative(8f, 3f, 8f, 7f)
                moveTo(12f, 12f)
                arcToRelative(4f, 4f, 0f, true, false, 0f, -8f)
                arcToRelative(4f, 4f, 0f, false, false, 0f, 8f)
                close()
            }
        }
    }

    // bell: M6 9a6 6 0 1 1 12 0c0 5 2 6 2 6H4s2-1 2-6zM10 19a2 2 0 0 0 4 0
    val Bell: ImageVector by lazy {
        icon("Bell") {
            strokePath {
                moveTo(6f, 9f)
                arcToRelative(6f, 6f, 0f, true, true, 12f, 0f)
                curveToRelative(0f, 5f, 2f, 6f, 2f, 6f)
                horizontalLineTo(4f)
                reflectiveCurveToRelative(2f, -1f, 2f, -6f)
                close()
                moveTo(10f, 19f)
                arcToRelative(2f, 2f, 0f, false, false, 4f, 0f)
            }
        }
    }

    // chevD: M6 9l6 6 6-6
    val ChevD: ImageVector by lazy {
        icon("ChevD") {
            strokePath {
                moveTo(6f, 9f)
                lineToRelative(6f, 6f)
                lineToRelative(6f, -6f)
            }
        }
    }

    // chevR: M9 6l6 6-6 6
    val ChevR: ImageVector by lazy {
        icon("ChevR") {
            strokePath {
                moveTo(9f, 6f)
                lineToRelative(6f, 6f)
                lineToRelative(-6f, 6f)
            }
        }
    }

    // chevL: M15 6l-6 6 6 6
    val ChevL: ImageVector by lazy {
        icon("ChevL") {
            strokePath {
                moveTo(15f, 6f)
                lineToRelative(-6f, 6f)
                lineToRelative(6f, 6f)
            }
        }
    }

    // chevU: M6 15l6-6 6 6
    val ChevU: ImageVector by lazy {
        icon("ChevU") {
            strokePath {
                moveTo(6f, 15f)
                lineToRelative(6f, -6f)
                lineToRelative(6f, 6f)
            }
        }
    }

    // plus: M12 5v14M5 12h14
    val Plus: ImageVector by lazy {
        icon("Plus") {
            strokePath {
                moveTo(12f, 5f)
                verticalLineToRelative(14f)
                moveTo(5f, 12f)
                horizontalLineToRelative(14f)
            }
        }
    }

    // minus: só o traço horizontal do Plus (steppers −/+).
    val Minus: ImageVector by lazy {
        icon("Minus") {
            strokePath {
                moveTo(5f, 12f)
                horizontalLineToRelative(14f)
            }
        }
    }

    // check: M5 13l4 4L19 7
    val Check: ImageVector by lazy {
        icon("Check") {
            strokePath {
                moveTo(5f, 13f)
                lineToRelative(4f, 4f)
                lineTo(19f, 7f)
            }
        }
    }

    // checkCircle: circle(12,12,10) + M8 12l3 3 5-6
    val CheckCircle: ImageVector by lazy {
        icon("CheckCircle") {
            strokePath {
                circle(12f, 12f, 10f)
                moveTo(8f, 12f)
                lineToRelative(3f, 3f)
                lineToRelative(5f, -6f)
            }
        }
    }

    // lock: rect(5,11,14,10,rx=2) + M8 11V8a4 4 0 1 1 8 0v3
    val Lock: ImageVector by lazy {
        icon("Lock") {
            strokePath {
                moveTo(7f, 11f)
                horizontalLineToRelative(10f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
                verticalLineToRelative(6f)
                arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
                horizontalLineToRelative(-10f)
                arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
                verticalLineToRelative(-6f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
                close()
                moveTo(8f, 11f)
                verticalLineTo(8f)
                arcToRelative(4f, 4f, 0f, true, true, 8f, 0f)
                verticalLineToRelative(3f)
            }
        }
    }

    // search: circle(11,11,7) + M20 20l-4-4
    val Search: ImageVector by lazy {
        icon("Search") {
            strokePath {
                circle(11f, 11f, 7f)
                moveTo(20f, 20f)
                lineToRelative(-4f, -4f)
            }
        }
    }

    // send: M5 12l14-7-5 16-3-7z
    val Send: ImageVector by lazy {
        icon("Send") {
            strokePath {
                moveTo(5f, 12f)
                lineToRelative(14f, -7f)
                lineToRelative(-5f, 16f)
                lineToRelative(-3f, -7f)
                close()
            }
        }
    }

    // smile: circle(12,12,10) + M8 14s1.5 2 4 2 4-2 4-2M9 9h.01M15 9h.01
    val Smile: ImageVector by lazy {
        icon("Smile") {
            strokePath {
                circle(12f, 12f, 10f)
                moveTo(8f, 14f)
                reflectiveCurveToRelative(1.5f, 2f, 4f, 2f)
                reflectiveCurveToRelative(4f, -2f, 4f, -2f)
                moveTo(9f, 9f)
                horizontalLineToRelative(0.01f)
                moveTo(15f, 9f)
                horizontalLineToRelative(0.01f)
            }
        }
    }

    // more: 3 círculos preenchidos r=1.5 em (5,12), (12,12), (19,12)
    val More: ImageVector by lazy {
        icon("More") {
            fillPath {
                circle(5f, 12f, 1.5f)
                circle(12f, 12f, 1.5f)
                circle(19f, 12f, 1.5f)
            }
        }
    }

    // arrow: M5 12h14M13 6l6 6-6 6
    val Arrow: ImageVector by lazy {
        icon("Arrow") {
            strokePath {
                moveTo(5f, 12f)
                horizontalLineToRelative(14f)
                moveTo(13f, 6f)
                lineToRelative(6f, 6f)
                lineToRelative(-6f, 6f)
            }
        }
    }

    // edit: M4 20h4l10-10-4-4L4 16zM14 6l4 4
    val Edit: ImageVector by lazy {
        icon("Edit") {
            strokePath {
                moveTo(4f, 20f)
                horizontalLineToRelative(4f)
                lineToRelative(10f, -10f)
                lineToRelative(-4f, -4f)
                lineTo(4f, 16f)
                close()
                moveTo(14f, 6f)
                lineToRelative(4f, 4f)
            }
        }
    }

    // pin: M12 22s7-7 7-12a7 7 0 1 0-14 0c0 5 7 12 7 12z + circle(12,10,2.5)
    val Pin: ImageVector by lazy {
        icon("Pin") {
            strokePath {
                moveTo(12f, 22f)
                reflectiveCurveToRelative(7f, -7f, 7f, -12f)
                arcToRelative(7f, 7f, 0f, true, false, -14f, 0f)
                curveToRelative(0f, 5f, 7f, 12f, 7f, 12f)
                close()
                circle(12f, 10f, 2.5f)
            }
        }
    }

    // clock: circle(12,12,9) + M12 7v5l3 2
    val Clock: ImageVector by lazy {
        icon("Clock") {
            strokePath {
                circle(12f, 12f, 9f)
                moveTo(12f, 7f)
                verticalLineToRelative(5f)
                lineToRelative(3f, 2f)
            }
        }
    }

    // star: M12 3l2.6 5.6 6 .6-4.5 4.2 1.3 6L12 16.7 6.6 19.4l1.3-6L3.4 9.2l6-.6z
    val Star: ImageVector by lazy {
        icon("Star") {
            strokePath { starPath() }
        }
    }

    // starFill: mesma geometria do star, preenchido e sem stroke
    val StarFill: ImageVector by lazy {
        icon("StarFill") {
            fillPath { starPath() }
        }
    }

    // vote: M4 12l5 5L20 6
    val Vote: ImageVector by lazy {
        icon("Vote") {
            strokePath {
                moveTo(4f, 12f)
                lineToRelative(5f, 5f)
                lineTo(20f, 6f)
            }
        }
    }

    // shelf: M4 5v14M20 5v14M4 9h16M4 14h16M8 5v4M14 5v4M11 14v5M17 14v5
    val Shelf: ImageVector by lazy {
        icon("Shelf") {
            strokePath {
                moveTo(4f, 5f); verticalLineToRelative(14f)
                moveTo(20f, 5f); verticalLineToRelative(14f)
                moveTo(4f, 9f); horizontalLineToRelative(16f)
                moveTo(4f, 14f); horizontalLineToRelative(16f)
                moveTo(8f, 5f); verticalLineToRelative(4f)
                moveTo(14f, 5f); verticalLineToRelative(4f)
                moveTo(11f, 14f); verticalLineToRelative(5f)
                moveTo(17f, 14f); verticalLineToRelative(5f)
            }
        }
    }

    // groups: circle(9,9,3.5) + M2 20c0-3.5 3-6 7-6s7 2.5 7 6 + circle(17,7,2.5) + M22 18c0-2.7-2-4.5-5-4.5
    val Groups: ImageVector by lazy {
        icon("Groups") {
            strokePath {
                circle(9f, 9f, 3.5f)
                moveTo(2f, 20f)
                curveToRelative(0f, -3.5f, 3f, -6f, 7f, -6f)
                reflectiveCurveToRelative(7f, 2.5f, 7f, 6f)
                circle(17f, 7f, 2.5f)
                moveTo(22f, 18f)
                curveToRelative(0f, -2.7f, -2f, -4.5f, -5f, -4.5f)
            }
        }
    }

    // log: M4 6h16M4 12h16M4 18h10
    val Log: ImageVector by lazy {
        icon("Log") {
            strokePath {
                moveTo(4f, 6f); horizontalLineToRelative(16f)
                moveTo(4f, 12f); horizontalLineToRelative(16f)
                moveTo(4f, 18f); horizontalLineToRelative(10f)
            }
        }
    }

    // google: logo multicolor preenchido (cores da marca — NÃO tintar; usar tint = Color.Unspecified)
    val Google: ImageVector by lazy {
        icon("Google") {
            // M22 12.2c0-.8-.1-1.5-.2-2.2H12v4.3h5.6c-.2 1.3-1 2.4-2 3.1v2.6h3.3c1.9-1.8 3.1-4.4 3.1-7.8z
            fillPath(Color(0xFF4285F4)) {
                moveTo(22f, 12.2f)
                curveToRelative(0f, -0.8f, -0.1f, -1.5f, -0.2f, -2.2f)
                horizontalLineTo(12f)
                verticalLineToRelative(4.3f)
                horizontalLineToRelative(5.6f)
                curveToRelative(-0.2f, 1.3f, -1f, 2.4f, -2f, 3.1f)
                verticalLineToRelative(2.6f)
                horizontalLineToRelative(3.3f)
                curveToRelative(1.9f, -1.8f, 3.1f, -4.4f, 3.1f, -7.8f)
                close()
            }
            // M12 22c2.7 0 5-.9 6.6-2.4l-3.3-2.6c-.9.6-2.1 1-3.3 1-2.5 0-4.7-1.7-5.5-4H3.2v2.5C4.8 19.7 8.1 22 12 22z
            fillPath(Color(0xFF34A853)) {
                moveTo(12f, 22f)
                curveToRelative(2.7f, 0f, 5f, -0.9f, 6.6f, -2.4f)
                lineToRelative(-3.3f, -2.6f)
                curveToRelative(-0.9f, 0.6f, -2.1f, 1f, -3.3f, 1f)
                curveToRelative(-2.5f, 0f, -4.7f, -1.7f, -5.5f, -4f)
                horizontalLineTo(3.2f)
                verticalLineToRelative(2.5f)
                curveTo(4.8f, 19.7f, 8.1f, 22f, 12f, 22f)
                close()
            }
            // M6.5 14c-.2-.6-.3-1.3-.3-2s.1-1.4.3-2V7.5H3.2C2.4 9 2 10.4 2 12s.4 3 1.2 4.5L6.5 14z
            fillPath(Color(0xFFFBBC04)) {
                moveTo(6.5f, 14f)
                curveToRelative(-0.2f, -0.6f, -0.3f, -1.3f, -0.3f, -2f)
                reflectiveCurveToRelative(0.1f, -1.4f, 0.3f, -2f)
                verticalLineTo(7.5f)
                horizontalLineTo(3.2f)
                curveTo(2.4f, 9f, 2f, 10.4f, 2f, 12f)
                reflectiveCurveToRelative(0.4f, 3f, 1.2f, 4.5f)
                lineTo(6.5f, 14f)
                close()
            }
            // M12 6c1.4 0 2.7.5 3.7 1.4l2.8-2.8C16.9 3 14.7 2 12 2 8.1 2 4.8 4.3 3.2 7.5L6.5 10c.8-2.3 3-4 5.5-4z
            fillPath(Color(0xFFEA4335)) {
                moveTo(12f, 6f)
                curveToRelative(1.4f, 0f, 2.7f, 0.5f, 3.7f, 1.4f)
                lineToRelative(2.8f, -2.8f)
                curveTo(16.9f, 3f, 14.7f, 2f, 12f, 2f)
                curveTo(8.1f, 2f, 4.8f, 4.3f, 3.2f, 7.5f)
                lineTo(6.5f, 10f)
                curveToRelative(0.8f, -2.3f, 3f, -4f, 5.5f, -4f)
                close()
            }
        }
    }

    // heart: M12 20s-7-4.5-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.5-7 10-7 10z
    val Heart: ImageVector by lazy {
        icon("Heart") {
            strokePath {
                moveTo(12f, 20f)
                reflectiveCurveToRelative(-7f, -4.5f, -7f, -10f)
                arcToRelative(4f, 4f, 0f, false, true, 7f, -2.6f)
                arcTo(4f, 4f, 0f, false, true, 19f, 10f)
                curveToRelative(0f, 5.5f, -7f, 10f, -7f, 10f)
                close()
            }
        }
    }

    // reply: M9 14l-5-5 5-5M4 9h9a7 7 0 0 1 7 7v2
    val Reply: ImageVector by lazy {
        icon("Reply") {
            strokePath {
                moveTo(9f, 14f)
                lineToRelative(-5f, -5f)
                lineToRelative(5f, -5f)
                moveTo(4f, 9f)
                horizontalLineToRelative(9f)
                arcToRelative(7f, 7f, 0f, false, true, 7f, 7f)
                verticalLineToRelative(2f)
            }
        }
    }

    // exit: M9 4H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h4M16 17l5-5-5-5M21 12H9
    val Exit: ImageVector by lazy {
        icon("Exit") {
            strokePath {
                moveTo(9f, 4f)
                horizontalLineTo(5f)
                arcToRelative(2f, 2f, 0f, false, false, -2f, 2f)
                verticalLineToRelative(12f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, 2f)
                horizontalLineToRelative(4f)
                moveTo(16f, 17f)
                lineToRelative(5f, -5f)
                lineToRelative(-5f, -5f)
                moveTo(21f, 12f)
                horizontalLineTo(9f)
            }
        }
    }

    // trophy: M8 5h8v4a4 4 0 0 1-8 0zM8 5H5v2a3 3 0 0 0 3 3M16 5h3v2a3 3 0 0 1-3 3M10 14h4l-1 5h-2z
    val Trophy: ImageVector by lazy {
        icon("Trophy") {
            strokePath {
                moveTo(8f, 5f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(4f)
                arcToRelative(4f, 4f, 0f, false, true, -8f, 0f)
                close()
                moveTo(8f, 5f)
                horizontalLineTo(5f)
                verticalLineToRelative(2f)
                arcToRelative(3f, 3f, 0f, false, false, 3f, 3f)
                moveTo(16f, 5f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(2f)
                arcToRelative(3f, 3f, 0f, false, true, -3f, 3f)
                moveTo(10f, 14f)
                horizontalLineToRelative(4f)
                lineToRelative(-1f, 5f)
                horizontalLineToRelative(-2f)
                close()
            }
        }
    }

    // bars: M4 18V10M10 18V4M16 18v-6M22 18v-9
    val Bars: ImageVector by lazy {
        icon("Bars") {
            strokePath {
                moveTo(4f, 18f); verticalLineTo(10f)
                moveTo(10f, 18f); verticalLineTo(4f)
                moveTo(16f, 18f); verticalLineToRelative(-6f)
                moveTo(22f, 18f); verticalLineToRelative(-9f)
            }
        }
    }

    // info: circle(12,12,10) + ponto(12,8) + haste(12,11→16)
    val Info: ImageVector by lazy {
        icon("Info") {
            strokePath {
                circle(12f, 12f, 10f)
                moveTo(12f, 8f); horizontalLineToRelative(0.01f)
                moveTo(12f, 11f); verticalLineToRelative(5f)
            }
        }
    }

    // warning: triângulo + "!" (haste 10→14 + ponto 17)
    val Warning: ImageVector by lazy {
        icon("Warning") {
            strokePath {
                moveTo(12f, 4f)
                lineTo(21f, 19f)
                horizontalLineTo(3f)
                close()
                moveTo(12f, 10f); verticalLineToRelative(4f)
                moveTo(12f, 17f); horizontalLineToRelative(0.01f)
            }
        }
    }

    // camera: corpo arredondado + saliência superior + lente circle(12,13.5,3)
    val Camera: ImageVector by lazy {
        icon("Camera") {
            strokePath {
                moveTo(9f, 8f)
                lineTo(10f, 6f)
                horizontalLineTo(14f)
                lineTo(15f, 8f)
                horizontalLineTo(19f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
                verticalLineTo(17f)
                arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
                horizontalLineTo(5f)
                arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
                verticalLineTo(10f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
                close()
                circle(12f, 13.5f, 3f)
            }
        }
    }

    // back: seta pra esquerda — M20 12H4 M10 6l-6 6 6 6
    val Back: ImageVector by lazy {
        icon("Back") {
            strokePath {
                moveTo(20f, 12f); horizontalLineTo(4f)
                moveTo(10f, 6f); lineToRelative(-6f, 6f); lineToRelative(6f, 6f)
            }
        }
    }

    // moreV: 3 pontos verticais preenchidos r=1.5 em (12,5),(12,12),(12,19)
    val MoreV: ImageVector by lazy {
        icon("MoreV") {
            fillPath {
                circle(12f, 5f, 1.5f)
                circle(12f, 12f, 1.5f)
                circle(12f, 19f, 1.5f)
            }
        }
    }

    // close: X — M6 6l12 12 M18 6l-12 12
    val Close: ImageVector by lazy {
        icon("Close") {
            strokePath {
                moveTo(6f, 6f); lineTo(18f, 18f)
                moveTo(18f, 6f); lineTo(6f, 18f)
            }
        }
    }

    // share: 3 nós + 2 conexões
    val Share: ImageVector by lazy {
        icon("Share") {
            strokePath {
                circle(18f, 5f, 3f)
                circle(6f, 12f, 3f)
                circle(18f, 19f, 3f)
                moveTo(8.6f, 13.5f); lineToRelative(6.8f, 4f)
                moveTo(15.4f, 6.5f); lineToRelative(-6.8f, 4f)
            }
        }
    }

    // trash: tampa + lata — M4 7h16 M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2 M6 7l1 13...
    val Trash: ImageVector by lazy {
        icon("Trash") {
            strokePath {
                moveTo(4f, 7f); horizontalLineTo(20f)
                moveTo(9f, 7f)
                verticalLineTo(5f)
                arcToRelative(1f, 1f, 0f, false, true, 1f, -1f)
                horizontalLineToRelative(4f)
                arcToRelative(1f, 1f, 0f, false, true, 1f, 1f)
                verticalLineTo(7f)
                moveTo(6f, 7f)
                lineToRelative(1f, 13f)
                arcToRelative(1f, 1f, 0f, false, false, 1f, 1f)
                horizontalLineToRelative(8f)
                arcToRelative(1f, 1f, 0f, false, false, 1f, -1f)
                lineToRelative(1f, -13f)
            }
        }
    }

    // image: moldura + sol + montanhas
    val Image: ImageVector by lazy {
        icon("Image") {
            strokePath {
                moveTo(5f, 4f)
                horizontalLineTo(19f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
                verticalLineTo(18f)
                arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
                horizontalLineTo(5f)
                arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
                verticalLineTo(6f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
                close()
                circle(8.5f, 9f, 1.5f)
                moveTo(4f, 17f)
                lineToRelative(4f, -4f)
                lineToRelative(3f, 3f)
                lineToRelative(4f, -5f)
                lineToRelative(6f, 6f)
            }
        }
    }

    // link (link-2): M9 17H7a5 5 0 0 1 0-10h2 M15 7h2a5 5 0 0 1 0 10h-2 M8 12h8
    val Link: ImageVector by lazy {
        icon("Link") {
            strokePath {
                moveTo(9f, 17f)
                horizontalLineTo(7f)
                arcToRelative(5f, 5f, 0f, false, true, 0f, -10f)
                horizontalLineToRelative(2f)
                moveTo(15f, 7f)
                horizontalLineToRelative(2f)
                arcToRelative(5f, 5f, 0f, false, true, 0f, 10f)
                horizontalLineToRelative(-2f)
                moveTo(8f, 12f)
                horizontalLineToRelative(8f)
            }
        }
    }

    // heartFill: mesma geometria do Heart, preenchida (favorito ativo)
    val HeartFill: ImageVector by lazy {
        icon("HeartFill") {
            fillPath {
                moveTo(12f, 20f)
                reflectiveCurveToRelative(-7f, -4.5f, -7f, -10f)
                arcToRelative(4f, 4f, 0f, false, true, 7f, -2.6f)
                arcTo(4f, 4f, 0f, false, true, 19f, 10f)
                curveToRelative(0f, 5.5f, -7f, 10f, -7f, 10f)
                close()
            }
        }
    }

    // eye: lente (2 quadráticas) + pupila
    val Eye: ImageVector by lazy {
        icon("Eye") {
            strokePath {
                moveTo(3f, 12f)
                quadTo(12f, 4f, 21f, 12f)
                quadTo(12f, 20f, 3f, 12f)
                close()
                circle(12f, 12f, 3.2f)
            }
        }
    }

    // eyeOff: mesma lente + barra diagonal (senha oculta)
    val EyeOff: ImageVector by lazy {
        icon("EyeOff") {
            strokePath {
                moveTo(3f, 12f)
                quadTo(12f, 4f, 21f, 12f)
                quadTo(12f, 20f, 3f, 12f)
                close()
                circle(12f, 12f, 3.2f)
                moveTo(3f, 3f)
                lineTo(21f, 21f)
            }
        }
    }

    // refresh: 2 arcos + 2 cabeças de seta
    val Refresh: ImageVector by lazy {
        icon("Refresh") {
            strokePath {
                moveTo(20f, 8f)
                arcToRelative(8f, 8f, 0f, false, false, -14f, -1f)
                moveTo(4f, 16f)
                arcToRelative(8f, 8f, 0f, false, false, 14f, 1f)
                moveTo(20f, 4f); verticalLineTo(8f); horizontalLineTo(16f)
                moveTo(4f, 20f); verticalLineTo(16f); horizontalLineTo(8f)
            }
        }
    }

    // quote: duas aspas preenchidas (decorativo em Frases)
    val Quote: ImageVector by lazy {
        icon("Quote") {
            fillPath {
                moveTo(6f, 7f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(5f)
                arcToRelative(4f, 4f, 0f, false, true, -4f, 4f)
                verticalLineToRelative(-2f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
                horizontalLineTo(6f)
                close()
                moveTo(14f, 7f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(5f)
                arcToRelative(4f, 4f, 0f, false, true, -4f, 4f)
                verticalLineToRelative(-2f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
                horizontalLineTo(14f)
                close()
            }
        }
    }

    // shield: escudo + check (verificado/moderação)
    val Shield: ImageVector by lazy {
        icon("Shield") {
            strokePath {
                moveTo(12f, 3f)
                lineTo(19f, 6f)
                verticalLineToRelative(5f)
                curveToRelative(0f, 4f, -3f, 6.5f, -7f, 8f)
                curveToRelative(-4f, -1.5f, -7f, -4f, -7f, -8f)
                verticalLineTo(6f)
                close()
                moveTo(9f, 12f)
                lineToRelative(2f, 2f)
                lineToRelative(4f, -4f)
            }
        }
    }
}

// Geometria compartilhada entre Star (stroke) e StarFill (fill).
private fun PathBuilder.starPath() {
    moveTo(12f, 3f)
    lineToRelative(2.6f, 5.6f)
    lineToRelative(6f, 0.6f)
    lineToRelative(-4.5f, 4.2f)
    lineToRelative(1.3f, 6f)
    lineTo(12f, 16.7f)
    lineTo(6.6f, 19.4f)
    lineToRelative(1.3f, -6f)
    lineTo(3.4f, 9.2f)
    lineToRelative(6f, -0.6f)
    close()
}
