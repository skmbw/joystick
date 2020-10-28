package com.xuershangda.joystick.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.MotionEvent
import com.xuershangda.joystick.R
import com.xuershangda.joystick.listener.FingerTouchViewListener
import kotlin.math.abs

class FingerPaintImageView @JvmOverloads constructor(context: Context,
                                                     attrs: AttributeSet? = null,
                                                     defStyleAttr: Int = 0,
                                                     defStyleRes: Int = 0) :
        AppCompatImageView(context, attrs, defStyleAttr) {

    private enum class BrushType {
        BLUR, EMBOSS, NORMAL
    }

    private val defaultStrokeColor = Color.RED
    private val defaultStrokeWidth = 36F
    private val defaultTouchTolerance = 4F
    private val defaultBitmapPaint = Paint(Paint.DITHER_FLAG)
    private var brushBitmap: Bitmap? = null
    private var brushCanvas: Canvas? = null
    private var countDrawn = 0
    private var currentBrush = BrushType.NORMAL
    var fingerTouchViewListener: FingerTouchViewListener? = null

    var inEditMode = false

    private val defaultEmboss: EmbossMaskFilter by lazy {
        EmbossMaskFilter(floatArrayOf(1F, 1F, 1F), 0.4F, 6F, 3.5F)
    }
    private val defaultBlur: BlurMaskFilter by lazy {
        BlurMaskFilter(5F, BlurMaskFilter.Blur.NORMAL)
    }

    var strokeColor = defaultStrokeColor
        set(value) {
            field = value
            pathPaint.color = value
        }

    var strokeWidth = defaultStrokeWidth
        set(value) {
            field = value
            pathPaint.strokeWidth = value
        }

    private val matrixValues = FloatArray(9)
        get() = field.apply { imageMatrix.getValues(this) }

    var touchTolerance = defaultTouchTolerance

    private val pathPaint = Paint().also {
        it.isAntiAlias = true
        it.isDither = true
        it.color = strokeColor
        it.style = Paint.Style.STROKE
        it.strokeJoin = Paint.Join.ROUND
        it.strokeCap = Paint.Cap.ROUND
        it.strokeWidth = strokeWidth
    }

    private var currentX = 0f
    private var currentY = 0f
    private var paths: MutableList<Pair<Path, Paint>> = mutableListOf()

    init {
        if (attrs != null) {
            val typedArray = context.theme.obtainStyledAttributes(attrs,
                    R.styleable.FingerPaintImageView, defStyleAttr, defStyleRes)
            try {
                strokeColor = typedArray.getColor(R.styleable.FingerPaintImageView_strokeColor, defaultStrokeColor)
                strokeWidth = typedArray.getDimension(R.styleable.FingerPaintImageView_strokeWidth, defaultStrokeWidth)
                inEditMode = typedArray.getBoolean(R.styleable.FingerPaintImageView_inEditMode, false)
                touchTolerance = typedArray.getFloat(R.styleable.FingerPaintImageView_touchTolerance, defaultTouchTolerance)
            } finally {
                typedArray.recycle()
            }
        }
    }

    /**
     * Get current screen's width and height
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        brushBitmap = Bitmap.createBitmap(w,
                h,
                Bitmap.Config.ARGB_8888)
        brushCanvas = Canvas(brushBitmap)
    }

    /**
     * If there are any paths drawn on top of the image, this will return a bitmap with the original
     * content plus the drawings on top of it. Otherwise, the original bitmap will be returned.
     */
    override fun getDrawable(): Drawable? {
        return super.getDrawable()?.let {
            if (!isModified()) return it

            val inverse = Matrix().apply { imageMatrix.invert(this) }
            val scale = FloatArray(9).apply { inverse.getValues(this) }[Matrix.MSCALE_X]

            // draw original bitmap
            val result = Bitmap.createBitmap(it.intrinsicWidth, it.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            it.draw(canvas)

            val transformedPath = Path()
            val transformedPaint = Paint()
            paths.forEach { (path, paint) ->
                path.transform(inverse, transformedPath)
                transformedPaint.set(paint)
                transformedPaint.strokeWidth *= scale
                canvas.drawPath(transformedPath, transformedPaint)
            }
            BitmapDrawable(resources, result)
        }
    }

    private fun getCurrentPath() = paths.lastOrNull()?.first

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (inEditMode) {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    handleTouchStart(event)
                    fingerTouchViewListener?.onActionDown(event.x, event.y)
                    invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    handleTouchMove(event)
                    fingerTouchViewListener?.onTouch(event.x, event.y)
                    invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    handleTouchEnd()
                    fingerTouchViewListener?.onActionUp(event.x, event.y)
                    countDrawn++
                    invalidate()
                }
            }
        }
        return true
    }

    private fun handleTouchStart(event: MotionEvent) {
        val sourceBitmap = super.getDrawable() ?: return

        val xTranslation = matrixValues[Matrix.MTRANS_X]
        val yTranslation = matrixValues[Matrix.MTRANS_Y]
        val scale = matrixValues[Matrix.MSCALE_X]

        val imageBounds = RectF(
                xTranslation,
                yTranslation,
                xTranslation + sourceBitmap.intrinsicWidth * scale,
                yTranslation + sourceBitmap.intrinsicHeight * scale)

        // make sure drawings are kept within the image bounds
        if (imageBounds.contains(event.x, event.y)) {
            paths.add(Path().also { it.moveTo(event.x + 1, event.y + 1) } to Paint(pathPaint))
            currentX = event.x
            currentY = event.y
        }
    }

    private fun handleTouchMove(event: MotionEvent) {
        val sourceBitmap = super.getDrawable() ?: return

        val xTranslation = matrixValues[Matrix.MTRANS_X]
        val yTranslation = matrixValues[Matrix.MTRANS_Y]
        val scale = matrixValues[Matrix.MSCALE_X]

        val xPos = event.x.coerceIn(xTranslation, xTranslation + sourceBitmap.intrinsicWidth * scale)
        val yPos = event.y.coerceIn(yTranslation, yTranslation + sourceBitmap.intrinsicHeight * scale)

        val dx = abs(xPos - currentX)
        val dy = abs(yPos - currentY)

        if (dx >= touchTolerance || dy >= touchTolerance) {
            getCurrentPath()?.quadTo(currentX, currentY, (xPos + currentX) / 2, (yPos + currentY) / 2)
            currentX = xPos
            currentY = yPos
        }
    }

    fun handleTouchEnd() {
        getCurrentPath()?.lineTo(currentX, currentY)
    }

    /**
     * 在坐标（x, y）点处，画一个点
     *
     * @param x 屏幕x轴坐标
     * @param y 屏幕y轴坐标
     */
    fun drawPoint(x: Float, y: Float) {
        val sourceBitmap = super.getDrawable() ?: return

        val xTranslation = matrixValues[Matrix.MTRANS_X]
        val yTranslation = matrixValues[Matrix.MTRANS_Y]
        val scale = matrixValues[Matrix.MSCALE_X]
        // 对缩放进行矫正，确保x值在两个参数之间，小于最小取最小，大于最大取最大
        val xPos = x.coerceIn(xTranslation, xTranslation + sourceBitmap.intrinsicWidth * scale)
        val yPos = y.coerceIn(yTranslation, yTranslation + sourceBitmap.intrinsicHeight * scale)

        paths.add(Path().also { it.moveTo(x, y) } to Paint(pathPaint))
        val path = getCurrentPath()
        path?.lineTo(xPos, yPos)
        invalidate() // 调用这个才会开始绘制
    }

    fun drawLine(startX: Float, startY: Float, x: Float, y: Float) {
        val sourceBitmap = super.getDrawable() ?: return

        val xTranslation = matrixValues[Matrix.MTRANS_X]
        val yTranslation = matrixValues[Matrix.MTRANS_Y]
        val scale = matrixValues[Matrix.MSCALE_X]
        // 对缩放进行矫正，确保x值在两个参数之间，小于最小取最小，大于最大取最大
        val xPos = x.coerceIn(xTranslation, xTranslation + sourceBitmap.intrinsicWidth * scale)
        val yPos = y.coerceIn(yTranslation, yTranslation + sourceBitmap.intrinsicHeight * scale)

        setStroke()

        val path = Path().also { it.moveTo(startX, startY) }
        paths.add(path to Paint(pathPaint))
        path.lineTo(xPos, yPos)
        invalidate()

        resetStroke()
    }

    fun setStroke() {
        this.strokeColor = Color.BLACK
        this.strokeWidth = 4F
    }

    fun resetStroke() {
        this.strokeColor = defaultStrokeColor
        this.strokeWidth = defaultStrokeWidth
    }

    fun drawArrow(startX: Float, startY: Float, x: Float, y: Float) {
        val sourceBitmap = super.getDrawable() ?: return

        val xTranslation = matrixValues[Matrix.MTRANS_X]
        val yTranslation = matrixValues[Matrix.MTRANS_Y]
        val scale = matrixValues[Matrix.MSCALE_X]
        // 对缩放进行矫正，确保x值在两个参数之间，小于最小取最小，大于最大取最大
        val xPos = x.coerceIn(xTranslation, xTranslation + sourceBitmap.intrinsicWidth * scale)
        val yPos = y.coerceIn(yTranslation, yTranslation + sourceBitmap.intrinsicHeight * scale)

        val path = Path().also { it.moveTo(startX, startY) }
        path.lineTo(xPos, yPos)
        invalidate()
    }

    fun scaleX(): Float {
        return matrixValues[Matrix.MSCALE_X]
    }

    fun scaleY(): Float {
        return matrixValues[Matrix.MSCALE_Y]
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        brushBitmap?.eraseColor(Color.TRANSPARENT)
        brushCanvas?.drawColor(Color.TRANSPARENT)
        canvas?.save()
        for (index in paths.indices) {
            val path = paths[index]
            if (index >= countDrawn) {
                path.second.maskFilter =
                        when (currentBrush) {
                            BrushType.EMBOSS -> defaultEmboss
                            BrushType.BLUR -> defaultBlur
                            BrushType.NORMAL -> null
                        }
            }
            brushCanvas?.drawPath(paths[index].first, paths[index].second)
        }
        canvas?.drawBitmap(brushBitmap, 0f, 0f, defaultBitmapPaint)
        canvas?.restore()
    }

    /**
     * Enable normal mode
     */
    fun normal() {
        currentBrush = BrushType.NORMAL
    }

    /**
     * Change brush type to emboss
     */
    fun emboss() {
        currentBrush = BrushType.EMBOSS
    }

    /**
     * Change brush type to blur
     */
    fun blur() {
        currentBrush = BrushType.BLUR
    }

    /**
     * Removes the last full path from the view.
     */
    fun undo() {
        paths.takeIf { it.isNotEmpty() }?.removeAt(paths.lastIndex)
        countDrawn--
        invalidate()
    }

    /**
     * Returns true if any paths are currently drawn on the image, false otherwise.
     */
    fun isModified(): Boolean {
        return if (paths != null) {
            paths.isNotEmpty()
        } else {
            false
        }
    }

    /**
     * Clears all existing paths from the image.
     */
    fun clear() {
        paths.clear()
        countDrawn = 0
        invalidate()
    }
}