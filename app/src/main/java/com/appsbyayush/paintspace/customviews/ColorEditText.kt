package com.appsbyayush.paintspace.customviews

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.models.FontItem
import com.appsbyayush.paintspace.models.UserTextElement
import com.appsbyayush.paintspace.utils.CommonMethods
import com.appsbyayush.paintspace.utils.Constants
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ColorEditText(
    context: Context,
    attrs: AttributeSet
): AppCompatEditText(context, attrs) {

    companion object {
        private const val TAG = "ColorEditTextyy"
    }

    private val defaultPaintFlags = paintFlags

    private var currentRectF = RectF()
    private var prevRectF = RectF()
    private var path = Path()

    private var lineIndex: Int = 0
    private var backgroundPaint: Paint

    private var currentTypeface: Typeface = typeface

    private var fontColorWithoutBackground: Int = ContextCompat.getColor(context, R.color.grey_400)
    private var fontColorWithBackground: Int = getTextColorAccordingToBackground(fontColorWithoutBackground)

    private var broadestLine: Float = 0F

    private var currentTextElement: UserTextElement? = null

    var textBold: Boolean = false
        set(value) {
            field = value
            setAppliedStylesToText()
        }
    var textItalic: Boolean = false
        set(value) {
            field = value
            setAppliedStylesToText()
        }
    var textUnderline: Boolean = false
        set(value) {
            field = value
            setAppliedStylesToText()
        }

    var backgroundPadding = 0F
    var radius = 30F
    var currentTextAlignment = TextAlignment.CENTER
    var addBackgroundToText = false

    init {
        backgroundPadding = backgroundPadding.coerceAtLeast(30F)

        backgroundPaint = Paint().apply {
            isAntiAlias = true
            color = fontColorWithoutBackground
            style = Paint.Style.FILL_AND_STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 10F
        }
    }

    override fun onDraw(canvas: Canvas) {
        checkLinesAndRestrictInput()
        if(addBackgroundToText) {
            drawTextBackground(canvas)
        }

        super.onDraw(canvas)
    }

    private fun checkLinesAndRestrictInput() {
        if(lineCount > 12) {
            val lineIndex = 11
            val currentLineEnd = layout.getLineEnd(lineIndex) - 1

            setText(text?.substring(0, currentLineEnd))
            setSelection(currentLineEnd)
        }
    }

    private fun drawTextBackground(canvas: Canvas?) {
        if(canvas == null || lineCount == 0 || text.isNullOrEmpty()) {
            return
        }

        broadestLine = 0F

        for(currentLineIndex in 0 until lineCount) {
            this.lineIndex = currentLineIndex

            val lineRect = Rect()
            getLineBounds(currentLineIndex, lineRect)

            val currentLineStart = layout.getLineStart(currentLineIndex)
            val currentLineEnd = layout.getLineEnd(currentLineIndex)
            val currentLineText = text!!.substring(currentLineStart, currentLineEnd).trim()

            val textWidth = paint.measureText(currentLineText)

            val currentTextLeft: Float
            val currentTextRight: Float

            when(currentTextAlignment) {
                TextAlignment.START -> {
                    currentTextLeft = (lineRect.left.toFloat() - backgroundPadding)
                        .coerceAtLeast(10F)
                    currentTextRight = (textWidth + backgroundPadding + 40).coerceAtMost(right.toFloat())
                }

                TextAlignment.CENTER -> {
                    currentTextLeft = (((lineRect.right - textWidth) / 2) - backgroundPadding)
                        .coerceAtLeast(10F)
                    currentTextRight = ((lineRect.right - currentTextLeft) + backgroundPadding)
                        .coerceAtMost(width - 10F)
                }

                TextAlignment.END -> {
                    currentTextLeft = ((lineRect.right - textWidth) - backgroundPadding)
                        .coerceAtLeast(10F)
                    currentTextRight = (lineRect.right + backgroundPadding)
                        .coerceAtMost(width - 10F)
                }
            }

            currentRectF = RectF(currentTextLeft, lineRect.top.toFloat(),
                currentTextRight, lineRect.bottom.toFloat())

            drawLineBackground(canvas)

            val lineBackgroundWidth = currentTextRight - currentTextLeft

            if(broadestLine < lineBackgroundWidth) {
                broadestLine = lineBackgroundWidth
            }
            prevRectF = RectF(currentRectF)
        }
    }

    private fun drawLineBackground(canvas: Canvas) {
        if(lineIndex == 0) {
            canvas.drawRoundRect(currentRectF, radius, radius, backgroundPaint)
            return
        }

        path.reset()

        when(currentTextAlignment) {
            TextAlignment.START -> drawBackgroundForTextAlignStart(canvas)
            TextAlignment.CENTER -> drawBackgroundForTextAlignCenter(canvas)
            TextAlignment.END -> drawBackgroundForTextAlignEnd(canvas)
        }
    }

    private fun drawBackgroundForTextAlignStart(canvas: Canvas) {
        val pathStartX = currentRectF.left
        val pathStartY = currentRectF.top + currentRectF.height() / 2

        path.moveTo(pathStartX, pathStartY)

        // Cover left-top Gap in rounded corner
        path.addRect(
            currentRectF.left,
            prevRectF.top + prevRectF.height() / 2,
            currentRectF.left + radius,
            currentRectF.top + currentRectF.height() / 2,
            Path.Direction.CW
        )

        path.moveTo(currentRectF.left, currentRectF.top)

        val widthDifference = currentRectF.width() - prevRectF.width()

        if(widthDifference < -75) {
            // Make right-top outward curve towards previous line
            path.lineTo(currentRectF.right + radius, currentRectF.top)
            path.quadTo(currentRectF.right, currentRectF.top, currentRectF.right,
                currentRectF.top + radius)

        } else if(widthDifference > 75) {
            // Make right-top inward curve towards previous line
            path.lineTo(prevRectF.right - radius, currentRectF.top)
            path.lineTo(prevRectF.right - radius, prevRectF.bottom - radius)
            path.lineTo(prevRectF.right, prevRectF.bottom - radius)
            path.quadTo(prevRectF.right, prevRectF.bottom, prevRectF.right + radius, currentRectF.top)
            path.lineTo(currentRectF.right - radius, currentRectF.top)
            path.quadTo(currentRectF.right, currentRectF.top, currentRectF.right, currentRectF.top + radius)

        } else {
            // Make slight curve for small width differences
            path.lineTo(prevRectF.right - radius, currentRectF.top)
            path.lineTo(prevRectF.right - radius, prevRectF.bottom - radius)
            path.lineTo(prevRectF.right, prevRectF.bottom - radius)

            path.cubicTo(prevRectF.right, prevRectF.bottom, currentRectF.right, currentRectF.top,
                currentRectF.right, currentRectF.top + radius)
        }

        // Make rounded corners on background
        path.lineTo(currentRectF.right, currentRectF.bottom - radius)
        path.quadTo(currentRectF.right, currentRectF.bottom,
            currentRectF.right - radius, currentRectF.bottom)
        path.lineTo(currentRectF.left + radius, currentRectF.bottom)
        path.quadTo(currentRectF.left, currentRectF.bottom,
            currentRectF.left, currentRectF.bottom - radius)
        path.lineTo(currentRectF.left, currentRectF.top)

        canvas.drawPath(path, backgroundPaint)
    }

    private fun drawBackgroundForTextAlignCenter(canvas: Canvas) {
        val pathStartX = currentRectF.left
        val pathStartY = currentRectF.top + currentRectF.height() / 2

        path.moveTo(pathStartX, pathStartY)
        path.lineTo(currentRectF.left, currentRectF.top + radius)

        val widthDifference = currentRectF.width() - prevRectF.width()

        if(widthDifference < -100) {
            path.quadTo(currentRectF.left, currentRectF.top, currentRectF.left - radius,
                currentRectF.top)
            path.lineTo(currentRectF.right + radius, currentRectF.top)
            path.quadTo(currentRectF.right, currentRectF.top,
                currentRectF.right, currentRectF.top + radius)

        } else if(widthDifference > 100) {
            path.quadTo(currentRectF.left, currentRectF.top,
                currentRectF.left + radius, currentRectF.top)
            path.lineTo(prevRectF.left - radius, currentRectF.top)
            path.quadTo(prevRectF.left, prevRectF.bottom, prevRectF.left, prevRectF.bottom - radius)
            path.lineTo(prevRectF.right, prevRectF.bottom - radius)
            path.quadTo(prevRectF.right, currentRectF.top,
                prevRectF.right + radius, currentRectF.top)
            path.lineTo(currentRectF.right - radius, currentRectF.top)
            path.quadTo(currentRectF.right, currentRectF.top, currentRectF.right, currentRectF.top + radius)

        } else {
            path.cubicTo(currentRectF.left, currentRectF.top, prevRectF.left, prevRectF.bottom,
                prevRectF.left, prevRectF.bottom - radius)

            path.lineTo(prevRectF.right, prevRectF.bottom - radius)

            path.cubicTo(prevRectF.right, prevRectF.bottom, currentRectF.right, currentRectF.top,
                currentRectF.right, currentRectF.top + radius)
        }

        path.lineTo(currentRectF.right, currentRectF.bottom - radius)
        path.quadTo(currentRectF.right, currentRectF.bottom,
            currentRectF.right - radius, currentRectF.bottom)
        path.lineTo(currentRectF.left + radius, currentRectF.bottom)
        path.quadTo(currentRectF.left, currentRectF.bottom, currentRectF.left, currentRectF.bottom - radius)
        path.lineTo(pathStartX, pathStartY)

        canvas.drawPath(path, backgroundPaint)
    }

    private fun drawBackgroundForTextAlignEnd(canvas: Canvas) {
        val pathStartX = currentRectF.right
        val pathStartY = currentRectF.top + currentRectF.height() / 2

        path.moveTo(pathStartX, pathStartY)

        // Cover right-top Gap in rounded corner
        path.addRect(
            currentRectF.right - radius,
            prevRectF.top + prevRectF.height() / 2,
            currentRectF.right,
            currentRectF.top + currentRectF.height() / 2,
            Path.Direction.CW
        )

        path.moveTo(currentRectF.right, currentRectF.top)

        // Make rounded corners on background
        path.lineTo(currentRectF.right, currentRectF.bottom - radius)
        path.quadTo(currentRectF.right, currentRectF.bottom,
            currentRectF.right - radius, currentRectF.bottom)
        path.lineTo(currentRectF.left + radius, currentRectF.bottom)
        path.quadTo(currentRectF.left, currentRectF.bottom,
            currentRectF.left, currentRectF.bottom - radius)
        path.lineTo(currentRectF.left, currentRectF.top + radius)


        val widthDifference = currentRectF.width() - prevRectF.width()

        if(widthDifference < -75) {
            // Make left-top outward curve towards previous line
            path.quadTo(currentRectF.left, currentRectF.top, currentRectF.left - radius,
                currentRectF.top)

        } else if(widthDifference > 75) {
            // Make left-top inward curve towards previous line
            path.quadTo(currentRectF.left, currentRectF.top, currentRectF.left + radius,
                currentRectF.top)
            path.lineTo(prevRectF.left - radius, currentRectF.top)
            path.quadTo(prevRectF.left, prevRectF.bottom, prevRectF.left, prevRectF.bottom - radius)
            path.lineTo(prevRectF.left + radius, prevRectF.bottom - radius)
            path.lineTo(prevRectF.left + radius, prevRectF.bottom)

        } else {
            // Make slight curves for small width difference
            path.cubicTo(currentRectF.left, currentRectF.top, prevRectF.left, prevRectF.bottom,
                prevRectF.left, prevRectF.bottom - radius)
        }

        canvas.drawPath(path, backgroundPaint)
    }

    private fun setAppliedStylesToText() {
        val textStyle = when {
            textBold && !textItalic -> Typeface.BOLD
            !textBold && textItalic -> Typeface.ITALIC
            textBold && textItalic -> Typeface.BOLD_ITALIC
            else -> Typeface.NORMAL
        }

        setTypeface(currentTypeface, textStyle)

        paintFlags = if(textUnderline) {
            paintFlags or Paint.UNDERLINE_TEXT_FLAG
        } else {
            defaultPaintFlags
        }
    }

    private fun updateBackgroundAndTextColor() {
        if(addBackgroundToText) {
            backgroundPaint.color = fontColorWithoutBackground
            fontColorWithBackground = getTextColorAccordingToBackground(backgroundPaint.color)

            setTextColor(fontColorWithBackground)

        } else {
            setTextColor(fontColorWithoutBackground)
        }
        invalidate()
    }

    private fun getTextColorAccordingToBackground(color: Int): Int {
        val colorDarkness = CommonMethods.getColorDarkness(color)
        val newColorId = when {
            colorDarkness >= 0 && colorDarkness < 0.4 -> {
                R.color.black
            }
            colorDarkness >= 0.4 && colorDarkness < 0.6 -> {
                R.color.grey_200
            }
            else -> {
                R.color.white
            }
        }

        return ContextCompat.getColor(context, newColorId)
    }

    private fun getBroadestLineWithoutBackground(): Float {
        var broadestLineWithoutBackground = 0F
        for(currentLineIndex in 0 until lineCount) {
            val lineRect = Rect()
            getLineBounds(currentLineIndex, lineRect)

            val currentLineStart = layout.getLineStart(currentLineIndex)
            val currentLineEnd = layout.getLineEnd(currentLineIndex)
            val currentLineText = text!!.substring(currentLineStart, currentLineEnd).trim()

            val textWidth = paint.measureText(currentLineText)

            if(broadestLineWithoutBackground < textWidth) {
                broadestLineWithoutBackground = textWidth
            }
        }

        return broadestLineWithoutBackground
    }

    private fun getTextStartXForSaving(srcBitmapWidth: Int, extraGapForImage: Int): Int {
        return if(srcBitmapWidth > broadestLine) {
            val totalGapLength = srcBitmapWidth - broadestLine

            when(currentTextAlignment) {
                TextAlignment.START -> 0
                TextAlignment.CENTER -> {
                    val gapLengthOnEachSide = totalGapLength / 2
                    val totalGapOnEachSide = gapLengthOnEachSide - extraGapForImage
                    if(totalGapOnEachSide > 0) totalGapOnEachSide.toInt() else 0
                }
                TextAlignment.END -> {
                    val totalGapOnLeft = totalGapLength - extraGapForImage
                    if(totalGapOnLeft > 0) totalGapOnLeft.toInt() else 0
                }
            }
        } else {
            0
        }
    }

    private fun getTextStartYForSaving(firstLineRect: Rect, extraGapForImage: Int): Int {
        val textTopWithGap = firstLineRect.top - extraGapForImage
        return if(textTopWithGap > 0) textTopWithGap else 0
    }

    private fun getFinalTextImageWidth(srcBitmapWidth: Int, textStartX: Int, extraGapForImage: Int): Int {
        val imageWidthAccordingToBroadestLineWithGap = broadestLine + (extraGapForImage * 1.5)
        val maxWidthAccordingToTextStartX = srcBitmapWidth - textStartX

        return if(maxWidthAccordingToTextStartX > imageWidthAccordingToBroadestLineWithGap)
            imageWidthAccordingToBroadestLineWithGap.toInt() else maxWidthAccordingToTextStartX
    }

    private fun getFinalTextImageHeight(srcBitmapHeight: Int, lastLineRect: Rect,
                                        textStartY: Int, extraGapForImage: Int): Int {
        val imageHeightAccordingToLastLineWithGap = lastLineRect.bottom + extraGapForImage
        val maxHeightAccordingToTextStartY = srcBitmapHeight - textStartY

        val textEndY = if(maxHeightAccordingToTextStartY > imageHeightAccordingToLastLineWithGap)
            imageHeightAccordingToLastLineWithGap else maxHeightAccordingToTextStartY

        return textEndY - textStartY
    }

    private fun getCurrentUserTextElement(textImageUri: Uri): UserTextElement {
        return UserTextElement(
            id = currentTextElement?.id ?: UUID.randomUUID().toString(),
            text = text.toString(),
            textImageUri = textImageUri,
            hasBackground = addBackgroundToText,
            textAlignment = currentTextAlignment,
            typeface = currentTypeface,
            textColor = if(addBackgroundToText) fontColorWithBackground else fontColorWithoutBackground,
            backgroundColor = backgroundPaint.color,
            textBold = textBold,
            textItalic = textItalic,
            textUnderline = textUnderline
        )
    }

    /******************************** PUBLIC FUNCTIONS **************************************/

    fun toggleTextBackground() {
        addBackgroundToText = !addBackgroundToText
        updateBackgroundAndTextColor()

        invalidate()
    }

    fun setTextAndBackgroundColor(color: Int) {
        fontColorWithoutBackground = color
        fontColorWithBackground = getTextColorAccordingToBackground(color)

        updateBackgroundAndTextColor()
    }

    fun setFont(fontItem: FontItem) {
        fontItem.fontFileLocalUri?.let { fontUri ->
            fontUri.path?.let { fontFilePath ->
                val fontFile = File(fontFilePath)

                currentTypeface = Typeface.createFromFile(fontFile)
                typeface = currentTypeface
            }
        }
    }

    fun changeTextAlignment() {
        currentTextAlignment = when(currentTextAlignment) {
            TextAlignment.START -> {
                gravity = Gravity.CENTER
                TextAlignment.CENTER
            }
            TextAlignment.CENTER -> {
                gravity = Gravity.CENTER or Gravity.END
                TextAlignment.END
            }
            TextAlignment.END -> {
                gravity = Gravity.CENTER or Gravity.START
                TextAlignment.START
            }
        }
    }

    fun getCurrentTextElement() = currentTextElement

    fun setCurrentTextElement(userTextElement: UserTextElement) {
        userTextElement.let {
            currentTextElement = it

            setText(it.text)

            addBackgroundToText = it.hasBackground

            currentTypeface = it.typeface
            typeface = it.typeface

            currentTextAlignment = it.textAlignment
            gravity = getGravityAccordingToCurrentTextAlignment()

            textBold = it.textBold
            textItalic = it.textItalic
            textUnderline = it.textUnderline

            if(it.hasBackground) {
                fontColorWithBackground = it.textColor
            } else {
                fontColorWithoutBackground = it.textColor
            }

            backgroundPaint.color = it.backgroundColor
            updateBackgroundAndTextColor()
        }
    }

    private fun getGravityAccordingToCurrentTextAlignment(): Int {
        return when(currentTextAlignment) {
            TextAlignment.START -> {
                Gravity.CENTER or Gravity.START
            }
            TextAlignment.CENTER -> {
                Gravity.CENTER
            }
            TextAlignment.END -> {
                Gravity.CENTER or Gravity.END
            }
        }
    }

    fun saveTextImage(): UserTextElement? {
        if (lineCount <= 0 || text.isNullOrEmpty()) {
            return null
        }

        currentTextElement?.let { textElement ->
            textElement.textImageUri?.path?.let {
                File(it).delete()
            }
        }

        /*COPYING PAINTING ON BITMAP CANVAS*/
        val srcBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bitmapCanvas = Canvas(srcBitmap)

//        if (background != null) {
//            background.draw(bitmapCanvas)
//        }
        draw(bitmapCanvas)

        val firstLineRect = Rect()
        getLineBounds(0, firstLineRect)

        val lastLineRect = Rect()
        getLineBounds(lineCount - 1, lastLineRect)

        val extraGapForImage = 20

        if (!addBackgroundToText) {
            broadestLine = getBroadestLineWithoutBackground()
        }

        val textStartX = getTextStartXForSaving(srcBitmap.width, extraGapForImage)
        val textStartY = getTextStartYForSaving(firstLineRect, extraGapForImage)

        val finalImageWidth = getFinalTextImageWidth(srcBitmap.width, textStartX, extraGapForImage)
        val finalImageHeight =
            getFinalTextImageHeight(srcBitmap.height, lastLineRect, textStartY, extraGapForImage)

        val drawBitmap = Bitmap.createBitmap(
            srcBitmap, textStartX, textStartY,
            finalImageWidth, finalImageHeight
        )

        /*COMPRESSING BITMAP AND SAVING AS PNG IMAGE*/
        val timeStamp = SimpleDateFormat(
            Constants.DATE_TIME_FORMAT_1,
            Locale.UK
        ).format(System.currentTimeMillis())

        val filePath =
            context.getExternalFilesDir(Constants.DIR_ELEMENTS)?.path + "/TEXT$timeStamp.png"
        val file = File(currentTextElement?.textImageUri?.path ?: filePath)

        return try {
            val outputStream = FileOutputStream(file)
            drawBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            drawBitmap.recycle()

            getCurrentUserTextElement(Uri.fromFile(file))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    enum class TextAlignment {
        START,
        CENTER,
        END
    }
}