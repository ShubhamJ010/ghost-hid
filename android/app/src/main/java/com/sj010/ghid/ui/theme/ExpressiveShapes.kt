package com.sj010.ghid.ui.theme

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Material 3 Expressive Shape Library.
 * Directly implements the shapes from Google's M3 Expressive catalog:
 * - 4-Lobe Clover
 * - 8-Petal Wavy Scallop
 * - 12-Petal Flower
 * - Soft Diamond
 * - Soft Triangle
 * - Arch Dome / Tunnel
 * - Quarter Circle Fan
 * - Soft Hexagon
 * - Soft Pentagon
 * - Soft Heart
 * - Concave Squircle
 * - Sunburst 8
 */

/** 1. 4-Petal Clover Shape */
class Clover4Shape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) / 2f

        val lobeR = r * 0.46f
        val offset = r * 0.44f

        path.addOval(Rect(cx - lobeR, cy - offset - lobeR, cx + lobeR, cy - offset + lobeR))
        path.addOval(Rect(cx + offset - lobeR, cy - lobeR, cx + offset + lobeR, cy + lobeR))
        path.addOval(Rect(cx - lobeR, cy + offset - lobeR, cx + lobeR, cy + offset + lobeR))
        path.addOval(Rect(cx - offset - lobeR, cy - lobeR, cx - offset + lobeR, cy + lobeR))
        path.addOval(Rect(cx - lobeR * 0.8f, cy - lobeR * 0.8f, cx + lobeR * 0.8f, cy + lobeR * 0.8f))

        return Outline.Generic(path)
    }
}

/** 2. 8-Petal Scallop / Wavy Flower Shape */
class Scallop8Shape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val maxR = min(w, h) / 2f * 0.96f
        val amp = maxR * 0.14f
        val baseR = maxR - amp
        val steps = 80

        for (i in 0 until steps) {
            val angle = (i * 2.0 * PI / steps).toFloat()
            val r = baseR + amp * cos(8 * angle)
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** 3. 12-Petal Flower Shape */
class Scallop12Shape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val maxR = min(w, h) / 2f * 0.96f
        val amp = maxR * 0.12f
        val baseR = maxR - amp
        val steps = 96

        for (i in 0 until steps) {
            val angle = (i * 2.0 * PI / steps).toFloat()
            val r = baseR + amp * cos(12 * angle)
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** 4. Soft Diamond Shape */
class SoftDiamondShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val cr = min(w, h) * 0.22f

        path.moveTo(cx, cr * 0.4f)
        path.quadraticTo(cx, 0f, cx + cr * 0.8f, cr * 0.6f)
        path.lineTo(w - cr * 0.8f, cy - cr * 0.6f)
        path.quadraticTo(w, cy, w - cr * 0.8f, cy + cr * 0.6f)
        path.lineTo(cx + cr * 0.8f, h - cr * 0.6f)
        path.quadraticTo(cx, h, cx - cr * 0.8f, h - cr * 0.6f)
        path.lineTo(cr * 0.8f, cy + cr * 0.6f)
        path.quadraticTo(0f, cy, cr * 0.8f, cy - cr * 0.6f)
        path.close()

        return Outline.Generic(path)
    }
}

/** 5. Soft Triangle Shape */
class SoftTriangleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cr = min(w, h) * 0.2f

        path.moveTo(cx - cr * 0.7f, cr)
        path.quadraticTo(cx, 0f, cx + cr * 0.7f, cr)
        path.lineTo(w - cr * 0.7f, h - cr * 0.5f)
        path.quadraticTo(w, h, w - cr * 1.4f, h)
        path.lineTo(cr * 1.4f, h)
        path.quadraticTo(0f, h, cr * 0.7f, h - cr * 0.5f)
        path.close()

        return Outline.Generic(path)
    }
}

/** 6. Arch Dome / Tunnel Shape */
class ArchDomeShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val w = size.width
        val h = size.height
        val cr = min(w, h) * 0.18f

        path.moveTo(cr, h)
        path.lineTo(w - cr, h)
        path.quadraticTo(w, h, w, h - cr)
        path.lineTo(w, w / 2f)
        path.arcTo(
            rect = Rect(0f, 0f, w, w),
            startAngleDegrees = 0f,
            sweepAngleDegrees = -180f,
            forceMoveTo = false
        )
        path.lineTo(0f, h - cr)
        path.quadraticTo(0f, h, cr, h)
        path.close()

        return Outline.Generic(path)
    }
}

/** 7. Quarter Circle Fan Shape */
class QuarterCircleFanShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val w = size.width
        val h = size.height
        val cr = min(w, h) * 0.2f

        path.moveTo(cr, h)
        path.lineTo(w - cr, h)
        path.quadraticTo(w, h, w, h - cr)
        path.lineTo(w, h * 0.4f)
        path.quadraticTo(w, 0f, w * 0.4f, 0f)
        path.lineTo(cr, 0f)
        path.quadraticTo(0f, 0f, 0f, cr)
        path.lineTo(0f, h - cr)
        path.quadraticTo(0f, h, cr, h)
        path.close()

        return Outline.Generic(path)
    }
}

/** 8. Soft Hexagon Shape */
class SoftHexagonShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) / 2f * 0.95f
        val cr = r * 0.18f

        val sides = 6
        for (i in 0 until sides) {
            val angle = (i * 2 * PI / sides - PI / 6).toFloat()
            val x = cx + (r - cr) * cos(angle)
            val y = cy + (r - cr) * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        return Outline.Generic(path)
    }
}

/** 9. Soft Pentagon Shape */
class SoftPentagonShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) / 2f * 0.94f

        val sides = 5
        for (i in 0 until sides) {
            val angle = (i * 2 * PI / sides - PI / 2).toFloat()
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        return Outline.Generic(path)
    }
}

/** 10. Soft Heart Shape */
class SoftHeartShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val w = size.width
        val h = size.height

        path.moveTo(w * 0.5f, h * 0.88f)
        path.cubicTo(w * 0.12f, h * 0.65f, 0f, h * 0.38f, 0f, h * 0.24f)
        path.cubicTo(0f, h * 0.06f, w * 0.22f, 0f, w * 0.5f, h * 0.22f)
        path.cubicTo(w * 0.78f, 0f, w, h * 0.06f, w, h * 0.24f)
        path.cubicTo(w, h * 0.38f, w * 0.88f, h * 0.65f, w * 0.5f, h * 0.88f)
        path.close()

        return Outline.Generic(path)
    }
}

/** 11. Concave Squircle Shape */
class ConcaveSquircleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val w = size.width
        val h = size.height
        val indent = min(w, h) * 0.10f
        val cr = min(w, h) * 0.14f

        path.moveTo(cr, 0f)
        path.quadraticTo(w * 0.5f, indent, w - cr, 0f)
        path.quadraticTo(w, 0f, w, cr)
        path.quadraticTo(w - indent, h * 0.5f, w, h - cr)
        path.quadraticTo(w, h, w - cr, h)
        path.quadraticTo(w * 0.5f, h - indent, cr, h)
        path.quadraticTo(0f, h, 0f, h - cr)
        path.quadraticTo(indent, h * 0.5f, 0f, cr)
        path.quadraticTo(0f, 0f, cr, 0f)
        path.close()

        return Outline.Generic(path)
    }
}
