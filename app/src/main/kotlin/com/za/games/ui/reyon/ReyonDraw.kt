package com.za.games.ui.reyon

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.za.games.reyon.Brand
import com.za.games.reyon.Product
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Diziliş ve Denetim ekranlarının ortak raf çizimi. */
internal object ReyonPalette {
    val BoardBg = Color(0xFF0F1628)
    val Plank = Color(0xFF475569)
    val PlankEdge = Color(0xFF64748B)
    val SlotLine = Color(0x22FFFFFF)
    val BlockText = Color(0xFF0F172A)
    val Lock = Color(0x99000000)
    val SelectedRing = Color(0xFFF8FAFC)
    val HighlightRing = Color(0xFF4DE1FF)
    val ViolatedRing = Color(0xFFF87171)
    val HintRing = Color(0xFFFDE68A)
    val FoundRing = Color(0xFF4ADE80)
    val MissRing = Color(0xFFF87171)
    val EmptySlot = Color(0x14FFFFFF)
    val Satisfied = Color(0xFF4ADE80)
    val Violated = Color(0xFFF87171)
}

internal fun brandColor(brand: Brand): Color = Color(brandArgb(brand))

/** Raf geometrisi: göz genişliği, raf yüksekliği, kalas kalınlığı, iç boşluk. */
internal class ShelfGeom(val width: Float, val height: Float, val rows: Int, val cols: Int) {
    val cw = width / cols
    val sh = height / rows
    val plank = sh * 0.12f
    val pad = cw * 0.05f
    val corner = cw * 0.12f
    fun x(col: Int) = col * cw + pad
    fun y(row: Int) = row * sh + pad
    fun w(facings: Int) = facings * cw - 2 * pad
    val h = sh - plank - 2 * pad
}

/** Ölçülmüş blok etiketi; önbellekli. */
internal class BlockLabeler(private val measurer: TextMeasurer, private val cache: HashMap<String, TextLayoutResult>) {
    fun label(text: String, maxWidth: Float, sizeSp: Float, color: Color): TextLayoutResult =
        cache.getOrPut("$text|${maxWidth.toInt()}|${sizeSp.toInt()}|${color.value}") {
            measurer.measure(
                AnnotatedString(text),
                style = TextStyle(fontSize = sizeSp.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                constraints = Constraints(maxWidth = maxWidth.toInt().coerceAtLeast(1)),
            )
        }
}

internal fun DrawScope.drawShelfFrame(g: ShelfGeom) {
    for (r in 0 until g.rows) {
        val y = (r + 1) * g.sh - g.plank
        drawRoundRect(ReyonPalette.Plank, topLeft = Offset(0f, y), size = Size(g.width, g.plank), cornerRadius = CornerRadius(g.plank * 0.3f, g.plank * 0.3f))
        drawRect(ReyonPalette.PlankEdge, topLeft = Offset(0f, y), size = Size(g.width, g.plank * 0.25f))
        for (c in 1 until g.cols) {
            drawLine(ReyonPalette.SlotLine, Offset(c * g.cw, r * g.sh + g.pad), Offset(c * g.cw, y - g.pad), strokeWidth = 1.dp.toPx())
        }
    }
}

/** Bir ürün bloğu: marka rengi, ad, boy noktaları, ★, ağırlık, kilit ve çerçeve. */
internal fun DrawScope.drawBlock(
    g: ShelfGeom,
    path: Path,
    labeler: BlockLabeler,
    name: String,
    product: Product,
    facings: Int,
    row: Int,
    col: Int,
    ring: Color? = null,
    locked: Boolean = false,
    dim: Boolean = false,
) {
    val x = g.x(col)
    val y = g.y(row)
    val w = g.w(facings)
    val h = g.h
    val fill = brandColor(product.brand).let { if (locked || dim) it.copy(alpha = 0.8f) else it }
    drawRoundRect(fill, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(g.corner, g.corner))
    val nameSp = (h * 0.22f / density).coerceIn(8f, 14f)
    val layout = labeler.label(name, w - g.cw * 0.16f, nameSp, ReyonPalette.BlockText)
    drawText(layout, topLeft = Offset(x + (w - layout.size.width) / 2f, y + h * 0.38f - layout.size.height / 2f))
    drawMarks(path, product, x, y, w, h)
    if (locked) drawLock(x + w - h * 0.22f, y + h * 0.16f, h * 0.12f)
    if (ring != null) {
        drawRoundRect(ring, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(g.corner, g.corner), style = Stroke(width = 3.dp.toPx()))
    }
}

/** Göz maskesindeki her satır koşusunun etrafına çerçeve. */
internal fun DrawScope.drawMaskRing(g: ShelfGeom, mask: Int, color: Color, width: Float = 3.dp.toPx()) {
    for (r in 0 until g.rows) {
        var c = 0
        while (c < g.cols) {
            if (mask and (1 shl (r * g.cols + c)) == 0) {
                c++
                continue
            }
            var end = c
            while (end + 1 < g.cols && mask and (1 shl (r * g.cols + end + 1)) != 0) end++
            val x = c * g.cw + g.pad * 0.5f
            val y = r * g.sh + g.pad * 0.5f
            drawRoundRect(color, topLeft = Offset(x, y), size = Size((end - c + 1) * g.cw - g.pad, g.sh - g.plank - g.pad), cornerRadius = CornerRadius(g.corner, g.corner), style = Stroke(width = width))
            c = end + 1
        }
    }
}

/** Boy noktaları, ★ (yüksek marj) ve ağırlık işareti; bloğun alt şeridinde. */
internal fun DrawScope.drawMarks(path: Path, p: Product, x: Float, y: Float, w: Float, h: Float) {
    val r = h * 0.055f
    val cy = y + h * 0.78f
    var cx = x + w * 0.5f - (p.size - 1) * r * 1.6f
    if (p.premium) cx -= r * 2.2f
    if (p.heavy) cx -= r * 2.2f
    repeat(p.size) {
        drawCircle(ReyonPalette.BlockText, radius = r, center = Offset(cx, cy))
        cx += r * 3.2f
    }
    if (p.premium) {
        cx += r * 1.2f
        drawStar(path, cx, cy, r * 2.2f)
        cx += r * 4.4f
    }
    if (p.heavy) {
        cx += r * 0.6f
        drawRoundRect(ReyonPalette.BlockText, topLeft = Offset(cx - r * 1.8f, cy - r * 0.6f), size = Size(r * 3.6f, r * 2.2f), cornerRadius = CornerRadius(r * 0.6f, r * 0.6f))
        drawRect(ReyonPalette.BlockText, topLeft = Offset(cx - r * 0.7f, cy - r * 1.7f), size = Size(r * 1.4f, r * 1.2f))
    }
}

internal fun DrawScope.drawStar(path: Path, cx: Float, cy: Float, r: Float) {
    path.reset()
    for (i in 0 until 10) {
        val a = -PI.toFloat() / 2f + i * PI.toFloat() / 5f
        val rr = if (i % 2 == 0) r else r * 0.45f
        val px = cx + rr * cos(a)
        val py = cy + rr * sin(a)
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, ReyonPalette.BlockText)
}

internal fun DrawScope.drawLock(cx: Float, cy: Float, r: Float) {
    drawRoundRect(ReyonPalette.Lock, topLeft = Offset(cx - r, cy), size = Size(2 * r, r * 1.6f), cornerRadius = CornerRadius(r * 0.3f, r * 0.3f))
    drawCircle(ReyonPalette.Lock, radius = r * 0.7f, center = Offset(cx, cy), style = Stroke(width = r * 0.35f))
}
