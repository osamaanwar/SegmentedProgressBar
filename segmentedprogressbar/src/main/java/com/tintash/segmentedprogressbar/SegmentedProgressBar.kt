package com.tintash.segmentedprogressbar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.annotation.ColorInt
import android.support.annotation.IntDef
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View

@Suppress("unused")
class SegmentedProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    companion object {
        private const val DEFAULT_SEGMENTS_COUNT = 3
        private const val DEFAULT_SEGMENT_SPACING_DP = 2f
        private const val DEFAULT_SEGMENT_STROKE_WIDTH_DP = 10f
        private const val DEFAULT_PROGRESS = 0

        private const val DESIRED_WIDTH_DP = 100f
    }

    /**
     *
     */
    private var viewWidth = 0
        set(value) {
            field = value
            singleSegmentWidth = calculateSingleSegmentWidth()
        }

    /**
     *
     */
    private var viewHeight = 0
        set(value) {
            field = value
            midHeight = value.toFloat() / 2
        }

    /**
     *
     */
    private val utils = Utils(context)

    /**
     *
     */
    private var segmentsCount = DEFAULT_SEGMENTS_COUNT
        set(value) {
            field = value

            progress = progress
            totalSpacing = calculateTotalSpacing()
        }

    /**
     *
     */
    private var segmentSpacing = utils.convertDpToPixel(DEFAULT_SEGMENT_SPACING_DP)
        set(value) {
            field = value

            totalSpacing = calculateTotalSpacing()
        }

    /**
     *
     */
    private var segmentStrokeWidth = utils.convertDpToPixel(DEFAULT_SEGMENT_STROKE_WIDTH_DP)
        set(value) {
            field = value
            backgroundPaint.strokeWidth = value
            progressPaint.strokeWidth = value
        }

    /**
     *
     */
    var progress = DEFAULT_PROGRESS
        set(value) {
            field = when {
                value < 0 -> throw IllegalStateException("Progress cannot be negative")
                value > segmentsCount -> throw IllegalStateException("Progress out of bounds")
                else -> value
            }
            invalidate()
        }

    /**
     *
     */
    private var totalSpacing = calculateTotalSpacing()
        set(value) {
            field = value
            singleSegmentWidth = calculateSingleSegmentWidth()
        }

    /**
     *
     */
    private var midHeight = 0f

    /**
     *
     */
    private var singleSegmentWidth = calculateSingleSegmentWidth()

    @SegmentStyle
    var segmentStyle = SegmentStyle.ROUNDED_EDGES
        set(value) {
            field = value

            backgroundPaint.strokeCap = if (value == SegmentStyle.SQUARED) Paint.Cap.SQUARE else Paint.Cap.ROUND
            progressPaint.strokeCap = if (value == SegmentStyle.SQUARED) Paint.Cap.SQUARE else Paint.Cap.ROUND
        }

    private val backgroundPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.background_color)
        style = Paint.Style.FILL
        isDither = true
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeWidth = segmentStrokeWidth
    }

    private val progressPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.progress_color)
        style = Paint.Style.FILL
        isDither = true
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeWidth = segmentStrokeWidth
    }

    init {
        attrs?.let { attributeSet ->
            val typedArray = context.obtainStyledAttributes(
                attributeSet, R.styleable.SegmentedProgressBar,
                0, 0
            )

            backgroundPaint.color = typedArray.getColor(
                R.styleable.SegmentedProgressBar_segment_empty_color,
                ContextCompat.getColor(context, R.color.background_color)
            )
            progressPaint.color = typedArray.getColor(
                R.styleable.SegmentedProgressBar_segment_filled_color,
                ContextCompat.getColor(context, R.color.progress_color)
            )

            segmentsCount = typedArray.getInt(R.styleable.SegmentedProgressBar_segment_count, DEFAULT_SEGMENTS_COUNT)
            progress = typedArray.getInt(R.styleable.SegmentedProgressBar_progress, DEFAULT_PROGRESS)

            segmentSpacing = typedArray.getDimension(
                R.styleable.SegmentedProgressBar_segment_spacing,
                utils.convertDpToPixel(DEFAULT_SEGMENT_SPACING_DP)
            )
            segmentStrokeWidth = typedArray.getDimension(
                R.styleable.SegmentedProgressBar_segment_stroke_width,
                utils.convertDpToPixel(DEFAULT_SEGMENT_STROKE_WIDTH_DP)
            )

            segmentStyle = typedArray.getInt(R.styleable.SegmentedProgressBar_segment_style, SegmentStyle.ROUNDED_EDGES)

            typedArray.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize.toFloat()
            MeasureSpec.AT_MOST -> Math.min(calculateDesiredWidth(), widthSize.toFloat())
            MeasureSpec.UNSPECIFIED -> calculateDesiredWidth()
            else -> calculateDesiredWidth()
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize.toFloat()
            MeasureSpec.AT_MOST -> Math.min(calculateDesiredHeight(), heightSize.toFloat())
            MeasureSpec.UNSPECIFIED -> calculateDesiredHeight()
            else -> calculateDesiredHeight()
        }

        setMeasuredDimension(width.toInt(), height.toInt())
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val startingPosition = canvas?.drawSegment(getViewStart(), progressPaint, 0, progress)
            ?: getViewStart()
        canvas?.drawSegment(startingPosition, backgroundPaint, progress, segmentsCount)
    }

    private fun Canvas.drawSegment(aStartingPosition: Float, paint: Paint, startingIndex: Int, finalIndex: Int): Float {
        var index = startingIndex
        var startingPosition = aStartingPosition

        while (index < finalIndex) {

            when (segmentStyle) {
                SegmentStyle.ROUNDED_EDGES -> {
                    val isTopLeftRounded = index == 0
                    val isBottomLeftRounded = index == 0

                    val isTopRightRounded = index == segmentsCount - 1
                    val isBottomRightRounded = index == segmentsCount - 1

                    val path = utils.getRoundedRectPath(
                        startingPosition,
                        midHeight - (segmentStrokeWidth / 2),
                        startingPosition + singleSegmentWidth,
                        midHeight + (segmentStrokeWidth / 2),
                        segmentStrokeWidth,
                        segmentStrokeWidth,
                        isTopLeftRounded,
                        isTopRightRounded,
                        isBottomRightRounded,
                        isBottomLeftRounded
                    )

                    this.drawPath(path, paint)
                }
                SegmentStyle.SQUARED -> {
                    val stoppingPosition = startingPosition + singleSegmentWidth
                    drawRect(
                        startingPosition,
                        midHeight - (segmentStrokeWidth / 2),
                        stoppingPosition,
                        midHeight + (segmentStrokeWidth / 2),
                        paint
                    )
                }
                SegmentStyle.ROUNDED -> {
                    val path = utils.getRoundedRectPath(
                        startingPosition,
                        midHeight - (segmentStrokeWidth / 2),
                        startingPosition + singleSegmentWidth,
                        midHeight + (segmentStrokeWidth / 2),
                        segmentStrokeWidth,
                        segmentStrokeWidth,
                        true,
                        true,
                        true,
                        true
                    )

                    this.drawPath(path, paint)
                }
            }

            startingPosition += singleSegmentWidth + segmentSpacing
            index++
        }

        return startingPosition
    }

    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(width, height, oldw, oldh)

        viewWidth = width
        viewHeight = height
    }

    private fun getAvailableViewWidth() = viewWidth - (paddingStart + paddingEnd)

    private fun getViewStart() = paddingStart.toFloat()

    private fun calculateTotalSpacing() = segmentSpacing * (segmentsCount - 1)

    private fun calculateSingleSegmentWidth() = (getAvailableViewWidth() - totalSpacing) / segmentsCount

    private fun calculateDesiredHeight() = segmentStrokeWidth + paddingTop + paddingBottom

    private fun calculateDesiredWidth() = utils.convertDpToPixel(DESIRED_WIDTH_DP) + paddingStart + paddingEnd

    fun setTotalSegmentCount(count: Int) {
        segmentsCount = count
        invalidate()
    }

    fun setSpacing(dp: Float) {
        segmentSpacing = utils.convertDpToPixel(dp)
        invalidate()
    }

    fun setStrokeWidth(dp: Float) {
        segmentStrokeWidth = utils.convertDpToPixel(dp)
        invalidate()
    }

    fun setEmptyColor(@ColorInt color: Int) {
        backgroundPaint.color = color
        invalidate()
    }

    fun setFilledColor(@ColorInt color: Int) {
        progressPaint.color = color
        invalidate()
    }

    @Retention(AnnotationRetention.SOURCE)
    @MustBeDocumented
    @IntDef(
        SegmentStyle.SQUARED,
        SegmentStyle.ROUNDED,
        SegmentStyle.ROUNDED_EDGES
    )
    annotation class SegmentStyle {
        companion object {
            const val SQUARED = 0
            const val ROUNDED = 1
            const val ROUNDED_EDGES = 2
        }
    }
}