package com.sj010.ghid.ui.theme

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

/**
 * The official Material 3 Expressive catalogue
 * ([androidx.compose.material3.MaterialShapes]) pre-converted to Compose [Shape]s.
 *
 * Every member is a normalised [RoundedPolygon] from androidx, fitted into the
 * bounds of whatever component clips with it. Names match the androidx catalogue
 * one to one, so there is no second shape definition in this app.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object ExpressiveShapes {

    val Arch: Shape by lazy { MaterialShapes.Arch.toShape() }
    val Arrow: Shape by lazy { MaterialShapes.Arrow.toShape() }
    val Boom: Shape by lazy { MaterialShapes.Boom.toShape() }
    val Bun: Shape by lazy { MaterialShapes.Bun.toShape() }
    val Burst: Shape by lazy { MaterialShapes.Burst.toShape() }
    val Circle: Shape by lazy { MaterialShapes.Circle.toShape() }
    val ClamShell: Shape by lazy { MaterialShapes.ClamShell.toShape() }
    val Clover4Leaf: Shape by lazy { MaterialShapes.Clover4Leaf.toShape() }
    val Clover8Leaf: Shape by lazy { MaterialShapes.Clover8Leaf.toShape() }
    val Cookie4Sided: Shape by lazy { MaterialShapes.Cookie4Sided.toShape() }
    val Cookie6Sided: Shape by lazy { MaterialShapes.Cookie6Sided.toShape() }
    val Cookie7Sided: Shape by lazy { MaterialShapes.Cookie7Sided.toShape() }
    val Cookie9Sided: Shape by lazy { MaterialShapes.Cookie9Sided.toShape() }
    val Cookie12Sided: Shape by lazy { MaterialShapes.Cookie12Sided.toShape() }
    val Diamond: Shape by lazy { MaterialShapes.Diamond.toShape() }
    val Fan: Shape by lazy { MaterialShapes.Fan.toShape() }
    val Flower: Shape by lazy { MaterialShapes.Flower.toShape() }
    val Gem: Shape by lazy { MaterialShapes.Gem.toShape() }
    val Ghostish: Shape by lazy { MaterialShapes.Ghostish.toShape() }
    val Heart: Shape by lazy { MaterialShapes.Heart.toShape() }
    val Oval: Shape by lazy { MaterialShapes.Oval.toShape() }
    val Pentagon: Shape by lazy { MaterialShapes.Pentagon.toShape() }
    val Pill: Shape by lazy { MaterialShapes.Pill.toShape() }
    val PixelCircle: Shape by lazy { MaterialShapes.PixelCircle.toShape() }
    val PixelTriangle: Shape by lazy { MaterialShapes.PixelTriangle.toShape() }
    val Puffy: Shape by lazy { MaterialShapes.Puffy.toShape() }
    val PuffyDiamond: Shape by lazy { MaterialShapes.PuffyDiamond.toShape() }
    val SemiCircle: Shape by lazy { MaterialShapes.SemiCircle.toShape() }
    val Slanted: Shape by lazy { MaterialShapes.Slanted.toShape() }
    val SoftBoom: Shape by lazy { MaterialShapes.SoftBoom.toShape() }
    val SoftBurst: Shape by lazy { MaterialShapes.SoftBurst.toShape() }
    val Square: Shape by lazy { MaterialShapes.Square.toShape() }
    val Sunny: Shape by lazy { MaterialShapes.Sunny.toShape() }
    val Triangle: Shape by lazy { MaterialShapes.Triangle.toShape() }
    val VerySunny: Shape by lazy { MaterialShapes.VerySunny.toShape() }

    /** Expressive silhouettes that stay legible at button size. */
    val buttonShapes: List<Shape> by lazy {
        listOf(
            Clover4Leaf,
            Flower,
            Heart,
            Cookie9Sided,
            Diamond,
            Puffy,
            Arch,
            Sunny,
            Ghostish,
            Cookie12Sided,
            Bun,
            SoftBurst
        )
    }
}

/** Fits a normalised polygon to the bounds of the component clipping with it. */
fun RoundedPolygon.toShape(): Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = this@toShape.toPath()
        val bounds = RectF()
        path.computeBounds(bounds, true)
        val matrix = Matrix()
        val width = if (bounds.width() > 0.0001f) bounds.width() else 1f
        val height = if (bounds.height() > 0.0001f) bounds.height() else 1f
        matrix.postTranslate(-bounds.left, -bounds.top)
        matrix.postScale(size.width / width, size.height / height)
        path.transform(matrix)
        return Outline.Generic(path.asComposePath())
    }
}
