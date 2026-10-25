package com.daiarn.counter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/** Rounded outline icons. Parent controls supply accessible names. */
@Composable
fun CounterGlyph(kind: String, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        withTransform({ scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) }) {
            val stroke = Stroke(1.65f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            fun line(x: Float, y: Float, x2: Float, y2: Float) =
                drawLine(color, Offset(x, y), Offset(x2, y2), 1.65f, StrokeCap.Round)
            fun path(block: Path.() -> Unit) = drawPath(Path().apply(block), color, style = stroke)
            fun drop() {
                path {
                    moveTo(12f, 2.5f); cubicTo(10f, 7f, 5f, 11f, 5f, 15f)
                    cubicTo(5f, 23f, 19f, 23f, 19f, 15f)
                    cubicTo(19f, 11f, 14f, 7f, 12f, 2.5f); close()
                }
                path { moveTo(8f, 14f); quadraticTo(8f, 17f, 10f, 18f) }
            }
            fun poop() {
                path {
                    moveTo(6f, 21f); cubicTo(1f, 21f, 1f, 16f, 5f, 15f)
                    cubicTo(2f, 12f, 5f, 9f, 8f, 9f)
                    cubicTo(6f, 6f, 12f, 7f, 12f, 2f)
                    cubicTo(17f, 4f, 18f, 7f, 16f, 9f)
                    cubicTo(20f, 9f, 22f, 13f, 19f, 15f)
                    cubicTo(23f, 16f, 23f, 21f, 18f, 21f); close()
                }
                drawCircle(color, 1f, Offset(9f, 14f))
                drawCircle(color, 1f, Offset(15f, 14f))
                path { moveTo(9f, 17f); quadraticTo(12f, 20f, 15f, 17f) }
            }
            when (kind) {
                "Account" -> {
                    drawCircle(color, 4f, Offset(12f, 7f), style = stroke)
                    path { moveTo(4f, 21f); cubicTo(4f, 12f, 20f, 12f, 20f, 21f) }
                }
                "Records" -> {
                    path {
                        moveTo(7f, 3f); lineTo(17f, 3f); lineTo(17f, 9f)
                        cubicTo(17f, 16f, 7f, 16f, 7f, 9f); close()
                    }
                    path { moveTo(7f, 5f); lineTo(3f, 5f); lineTo(3f, 8f); quadraticTo(3f, 12f, 8f, 12f) }
                    path { moveTo(17f, 5f); lineTo(21f, 5f); lineTo(21f, 8f); quadraticTo(21f, 12f, 16f, 12f) }
                    line(12f, 15f, 12f, 21f); line(7f, 21f, 17f, 21f)
                }
                "Today" -> {
                    line(3f, 21f, 21f, 21f)
                    drawRoundRect(color, Offset(4f, 11f), Size(3f, 10f), CornerRadius(1f), style = stroke)
                    drawRoundRect(color, Offset(10.5f, 4f), Size(3f, 17f), CornerRadius(1f), style = stroke)
                    drawRoundRect(color, Offset(17f, 8f), Size(3f, 13f), CornerRadius(1f), style = stroke)
                }
                "Home" -> path {
                    moveTo(3f, 10f); lineTo(12f, 3f); lineTo(21f, 10f)
                    lineTo(21f, 21f); lineTo(15f, 21f); lineTo(15f, 14f)
                    lineTo(9f, 14f); lineTo(9f, 21f); lineTo(3f, 21f); close()
                }
                "History" -> {
                    drawRoundRect(color, Offset(3f, 5f), Size(18f, 16f), CornerRadius(3f), style = stroke)
                    line(3f, 10f, 21f, 10f); line(8f, 3f, 8f, 7f); line(16f, 3f, 16f, 7f)
                    line(8f, 14f, 10f, 14f); line(14f, 14f, 16f, 14f); line(8f, 18f, 10f, 18f)
                }
                "Clock" -> {
                    drawCircle(color, 9f, Offset(12f, 12f), style = stroke)
                    line(12f, 7f, 12f, 12f); line(12f, 12f, 16f, 14f)
                }
                "Add" -> { line(5f, 12f, 19f, 12f); line(12f, 5f, 12f, 19f) }
                "Previous" -> path { moveTo(14f, 6f); lineTo(8f, 12f); lineTo(14f, 18f) }
                "Next" -> path { moveTo(10f, 6f); lineTo(16f, 12f); lineTo(10f, 18f) }
                "Edit" -> {
                    path { moveTo(4f, 16f); lineTo(4f, 21f); lineTo(9f, 20f)
                        lineTo(21f, 8f); lineTo(16f, 3f); close() }
                    line(13f, 6f, 18f, 11f)
                }
                "Delete" -> {
                    line(4f, 6f, 20f, 6f); line(9f, 3f, 15f, 3f)
                    path { moveTo(6f, 6f); lineTo(7f, 21f); lineTo(17f, 21f); lineTo(18f, 6f) }
                    line(10f, 10f, 10f, 17f); line(14f, 10f, 14f, 17f)
                }
                "PEE" -> drop()
                "BOTH" -> {
                    withTransform({ translate(0f, 7f); scale(.66f, .66f, Offset.Zero) }) { poop() }
                    withTransform({ translate(12f, 0f); scale(.5f, .5f, Offset.Zero) }) { drop() }
                }
                else -> poop()
            }
        }
    }
}
