package com.appsbyayush.paintspace.customviews

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.models.*
import com.appsbyayush.paintspace.utils.CommonMethods
import com.appsbyayush.paintspace.utils.Constants
import com.appsbyayush.paintspace.utils.UserGestureDetector
import com.appsbyayush.paintspace.utils.enums.BrushType
import com.appsbyayush.paintspace.utils.enums.DrawingImageType
import com.appsbyayush.paintspace.utils.enums.DrawingItemType
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class DrawingView(
    context: Context,
    attrs: AttributeSet
): View(context, attrs) {

    companion object {
        private const val TAG = "DrawingViewyy"

        private const val DEFAULT_BRUSH_SIZE = 20F
        private const val DEFAULT_STROKE_COLOR: Int = Color.GRAY
        private const val DEFAULT_BG_COLOR: Int = Color.WHITE
        private const val TOUCH_TOLERANCE = 4f
    }

    private var pathStartX = 0.0F
    private var pathStartY = 0.0F

    private var drawingItemsList: MutableList<DrawingItem> = mutableListOf()
    private var deletedBrushDrawingItemsList: MutableList<DrawingItem> = mutableListOf()
    private var currentBrushDrawingItem: DrawingItem? = null

    private var currentStrokeWidth = DEFAULT_BRUSH_SIZE
    private var currentStrokeColor = DEFAULT_STROKE_COLOR

    private var distanceOfSelectedItemRectLeftFromTouchStart: Float = 0.0F
    private var distanceOfSelectedItemRectTopFromTouchStart: Float = 0.0F
    private var distanceOfSelectedItemRectRightFromTouchStart: Float = 0.0F
    private var distanceOfSelectedItemRectBottomFromTouchStart: Float = 0.0F

    private val _currentSelectedItemFlow: MutableStateFlow<DrawingItem?> = MutableStateFlow(null)
    val currentSelectedItemFlow = _currentSelectedItemFlow.asStateFlow()

    private val _loadingFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loadingFlow = _loadingFlow.asStateFlow()

    private var lastDeletedSelectedItemAndIndex: Pair<Int, DrawingItem>? = null

    private var currentDrawingBitmap: Bitmap? = null

    var currentDrawing: Drawing? = null
        set(value) {
            field = value
            if(value != null) invalidate()
        }

    var currentBrushType = BrushType.SIMPLE
        private set

    var brushSelected: Boolean = false
    var selectionMode: Boolean = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        setBackgroundColor(Color.WHITE)

        drawCurrentDrawingOnCanvas(canvas)

        for(drawingItem in drawingItemsList) {
            drawItemOnCanvas(canvas, drawingItem)
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let { motionEvent ->
            when(motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    onMotionEventActionDown(motionEvent)
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    onMotionEventActionPointerDown(motionEvent)
                }

                MotionEvent.ACTION_MOVE -> {
                    onMotionEventActionMove(motionEvent)
                }

                MotionEvent.ACTION_POINTER_UP ->{
                    onMotionEventActionPointerUp(motionEvent)
                }

                MotionEvent.ACTION_UP -> {
                    onMotionEventActionUp(motionEvent)
                }
            }
        }

        invalidate()
        return true
    }

    private fun drawCurrentDrawingOnCanvas(canvas: Canvas?) {
       canvas?.let { drawingCanvas ->
           currentDrawing?.localDrawingImgUri?.let { drawingImageUri ->
                if(currentDrawingBitmap == null) {
                    currentDrawingBitmap = CommonMethods.getImageBitmapFromUri(context, drawingImageUri)
                        .copy(Bitmap.Config.ARGB_8888, false)
                }

               currentDrawingBitmap?.let {
                   drawingCanvas.drawBitmap(it, 0F, 0F, null)
               }
           }
       }
    }

    private fun drawItemOnCanvas(canvas: Canvas?, drawingItem: DrawingItem, drawToSave: Boolean = false) {
        canvas?.let { drawingCanvas ->
            when(drawingItem.type) {
                DrawingItemType.BRUSH -> drawPathOnCanvas(drawingCanvas, drawingItem, drawToSave)

                DrawingItemType.IMAGE -> drawImageOnCanvas(drawingCanvas, drawingItem, drawToSave)

                DrawingItemType.TEXT -> drawTextOnCanvas(drawingCanvas, drawingItem, drawToSave)
            }
        }
    }

    private fun drawPathOnCanvas(canvas: Canvas, drawingItem: DrawingItem, drawToSave: Boolean) {
        drawingItem.drawingPath?.let { currentDrawingPath ->
            if(drawingItem.eraseBrushOn) {
                canvas.drawPath(currentDrawingPath.path, getErasePaint(currentDrawingPath))
                return
            }

            val drawingPaint = getBaseDrawingPaint().apply {
                color = currentDrawingPath.strokeColor
                strokeWidth = currentDrawingPath.strokeWidth
                maskFilter = getMaskFilterFromBrushType(currentDrawingPath.brushType,
                    currentDrawingPath.strokeWidth)
            }
            canvas.drawPath(currentDrawingPath.path, drawingPaint)
        }
    }

    private fun drawImageOnCanvas(canvas: Canvas, drawingItem: DrawingItem, drawToSave: Boolean) {
        drawingItem.drawingImage?.let { currentDrawingImage ->
            val bitmap = currentDrawingImage.bitmap
                ?: CommonMethods.getImageBitmapFromUri(context, currentDrawingImage.uri)

            val bitmapMatrix = getDrawingImageMatrix(currentDrawingImage, bitmap.width, bitmap.height)

            if(!drawToSave) {
                canvas.drawBitmap(bitmap, bitmapMatrix, getImageDrawingPaint(drawingItem))
            } else {
                canvas.drawBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, false),
                    bitmapMatrix, getImageDrawingPaint(drawingItem))
            }

            if(_currentSelectedItemFlow.value == drawingItem && !drawToSave) {
                canvas.drawRect(currentDrawingImage.rectF, getSelectedBorderPaint())
            }
        }
    }

    private fun drawTextOnCanvas(canvas: Canvas, drawingItem: DrawingItem, drawToSave: Boolean) {
        drawingItem.drawingText?.let { currentDrawingText ->
            val bitmap = currentDrawingText.bitmap
                ?: CommonMethods.getImageBitmapFromUri(context, currentDrawingText.textImageUri)

            val bitmapMatrix = getDrawingTextMatrix(currentDrawingText, bitmap.width, bitmap.height)

            if(!drawToSave) {
                canvas.drawBitmap(bitmap, bitmapMatrix, getImageDrawingPaint(drawingItem))
            } else {
                canvas.drawBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, false),
                    bitmapMatrix, getImageDrawingPaint(drawingItem))
            }

            if(_currentSelectedItemFlow.value == drawingItem && !drawToSave) {
                canvas.drawRect(currentDrawingText.rectF, getSelectedBorderPaint())
            }
        }
    }

    private fun onMotionEventActionDown(motionEvent: MotionEvent) {
        val touchX = motionEvent.x
        val touchY = motionEvent.y

        if(brushSelected) {
            clearDeletedBrushDrawingItems()
            brushStart(touchX, touchY)
            return
        }

        if(selectionMode) {
            _currentSelectedItemFlow.value = findSelectedItem(motionEvent)
            calculateRectSidesDistancesFromTouchStart(touchX, touchY)

            userGestureDetector.onTouchEvent(event = motionEvent)
        }
    }

    private fun onMotionEventActionPointerDown(motionEvent: MotionEvent) {
        if(brushSelected) {
            return
        }

        if(selectionMode) {
            userGestureDetector.onTouchEvent(event = motionEvent)
        }
    }

    private fun onMotionEventActionMove(motionEvent: MotionEvent) {
        val touchX = motionEvent.x
        val touchY = motionEvent.y

        if(brushSelected) {
            brushMove(touchX, touchY)
            return
        }

        if(selectionMode) {
            userGestureDetector.onTouchEvent(event = motionEvent)
        }
    }

    private fun onMotionEventActionPointerUp(motionEvent: MotionEvent) {
        if(brushSelected) {
            return
        }

        if(selectionMode) {
            onScaleOrRotateGestureCompleted()
            userGestureDetector.onTouchEvent(event = motionEvent)
        }
    }

    private fun onMotionEventActionUp(motionEvent: MotionEvent) {
        val touchX = motionEvent.x
        val touchY = motionEvent.y

        if(brushSelected) {
            brushUp(touchX, touchY)
            return
        }

        if(selectionMode) {
            userGestureDetector.onTouchEvent(event = motionEvent)
        }
    }

    private fun brushStart(x: Float, y: Float) {
        val drawingPath = getCurrentDrawingPath()

        currentBrushDrawingItem = DrawingItem(
            type = DrawingItemType.BRUSH,
            drawingPath = drawingPath,
            eraseBrushOn = currentBrushType == BrushType.ERASER
        )

        currentBrushDrawingItem?.let {
            drawingItemsList.add(it)

            drawingPath.apply {
                path.reset()
                path.moveTo(x, y)

                pathPoints.add(PointF(x, y))
            }

            pathStartX = x
            pathStartY = y
        }
    }

    private fun brushMove(x: Float, y: Float) {
        currentBrushDrawingItem?.let {
            it.drawingPath?.let { currentDrawingPath ->
                val dx = abs(x - pathStartX)
                val dy = abs(y - pathStartY)

                if(dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                    currentDrawingPath.path.quadTo(pathStartX, pathStartY,
                        (x + pathStartX) / 2, (y + pathStartY) / 2)

                    currentDrawingPath.pathPoints.add(PointF(x, y))

                    pathStartX = x
                    pathStartY = y
                }
            }
        }
    }

    private fun brushUp(x: Float, y: Float) {
        currentBrushDrawingItem?.let {
            it.drawingPath?.let { currentDrawingPath ->
                currentDrawingPath.apply {
                    path.lineTo(x, y)
                    pathPoints.add(PointF(x, y))
                }
            }
        }
    }

    private fun initiateSelectedItemDragging(event: MotionEvent) {
        _currentSelectedItemFlow.value?.let { selectedItem ->
            if(selectedItem.isLocked) {
                return
            }

            val rect = if(selectedItem.type == DrawingItemType.IMAGE) {
                selectedItem.drawingImage?.rectF
            } else {
                selectedItem.drawingText?.rectF
            }

            rect?.let { itemRect ->
                itemRect.left = event.x - distanceOfSelectedItemRectLeftFromTouchStart
                itemRect.top = event.y - distanceOfSelectedItemRectTopFromTouchStart
                itemRect.right = event.x + distanceOfSelectedItemRectRightFromTouchStart
                itemRect.bottom = event.y + distanceOfSelectedItemRectBottomFromTouchStart
            }
        }

        invalidate()
    }

    private fun changeSelectedItemSize(scaleFactor: Float) {
        _currentSelectedItemFlow.value?.let { selectedItem ->
            if(selectedItem.isLocked) {
                return
            }

            var originalImageWidth = 0F
            var originalImageHeight = 0F

            val rect = if(selectedItem.type == DrawingItemType.IMAGE) {
                selectedItem.drawingImage?.let { currentDrawingImage ->
                    originalImageWidth = currentDrawingImage.width
                    originalImageHeight = currentDrawingImage.height

                    currentDrawingImage.rectF
                }
            } else {
                selectedItem.drawingText?.let { currentDrawingText ->
                    originalImageWidth = currentDrawingText.width
                    originalImageHeight = currentDrawingText.height

                    currentDrawingText.rectF
                }
            }


            rect?.let { itemRect ->
                val newImageWidth = originalImageWidth * scaleFactor
                val newImageHeight = originalImageHeight * scaleFactor

                if(newImageWidth > width * 3 || newImageHeight > height * 3
                    || newImageWidth < 150 || newImageHeight < 150) {
                    return
                }

                val widthDifference = abs(newImageWidth - itemRect.width())
                val heightDifference = abs(newImageHeight - itemRect.height())

                if(newImageWidth <= itemRect.width() && newImageHeight <= itemRect.height()) {
                    // Size Decreased
                    itemRect.left = itemRect.left + widthDifference / 2
                    itemRect.top = itemRect.top + heightDifference / 2
                    itemRect.right = itemRect.right - widthDifference / 2
                    itemRect.bottom = itemRect.bottom - heightDifference / 2

                } else {
                    itemRect.left = itemRect.left - widthDifference / 2
                    itemRect.top = itemRect.top - heightDifference / 2
                    itemRect.right = itemRect.right + widthDifference / 2
                    itemRect.bottom = itemRect.bottom + heightDifference / 2
                }
                invalidate()
            }
        }
    }

    private fun rotateSelectedItem(rotatedAngle: Float) {
        _currentSelectedItemFlow.value?.let { selectedItem ->
            if(selectedItem.isLocked) {
                return
            }

            if(selectedItem.type == DrawingItemType.IMAGE) {
                selectedItem.drawingImage?.let {
                    it.newAngleChange = rotatedAngle
                }
            }

            if(selectedItem.type == DrawingItemType.TEXT) {
                selectedItem.drawingText?.let {
                    it.newAngleChange = rotatedAngle
                }
            }

            invalidate()
        }
    }

    private fun calculateRectSidesDistancesFromTouchStart(touchStartX: Float, touchStartY: Float) {
        _currentSelectedItemFlow.value?.let { selectedItem ->
            val itemRect = if(selectedItem.type == DrawingItemType.IMAGE) {
                selectedItem.drawingImage?.rectF
            } else {
                selectedItem.drawingText?.rectF
            }

            itemRect?.let { rect ->
                distanceOfSelectedItemRectLeftFromTouchStart = CommonMethods.getDistanceBetweenLineAndPoint(
                    xCoeffOfLine = 1.0F,
                    yCoeffOfLine = 0.0F,
                    lineConstant = -rect.left,
                    pointX = touchStartX,
                    pointY = touchStartY
                )

                distanceOfSelectedItemRectRightFromTouchStart = CommonMethods.getDistanceBetweenLineAndPoint(
                    xCoeffOfLine = 1.0F,
                    yCoeffOfLine = 0.0F,
                    lineConstant = -rect.right,
                    pointX = touchStartX,
                    pointY = touchStartY
                )

                distanceOfSelectedItemRectTopFromTouchStart = CommonMethods.getDistanceBetweenLineAndPoint(
                    xCoeffOfLine = 0.0F,
                    yCoeffOfLine = 1.0F,
                    lineConstant = -rect.top,
                    pointX = touchStartX,
                    pointY = touchStartY
                )

                distanceOfSelectedItemRectBottomFromTouchStart = CommonMethods.getDistanceBetweenLineAndPoint(
                    xCoeffOfLine = 0.0F,
                    yCoeffOfLine = 1.0F,
                    lineConstant = -rect.bottom,
                    pointX = touchStartX,
                    pointY = touchStartY
                )
            }
        }
    }

    private fun onScaleOrRotateGestureCompleted() {
        // Update Item Dimensions and Clear
        _currentSelectedItemFlow.value?.let { selectedItem ->
            if(selectedItem.type == DrawingItemType.IMAGE) {
                selectedItem.drawingImage?.let { currentDrawingImage ->
                    currentDrawingImage.height = currentDrawingImage.rectF.height()
                    currentDrawingImage.width = currentDrawingImage.rectF.width()

                    currentDrawingImage.rotateAngle += currentDrawingImage.newAngleChange
                    currentDrawingImage.newAngleChange = 0F
                }

            }

            if(selectedItem.type == DrawingItemType.TEXT) {
                selectedItem.drawingText?.let { currentDrawingText ->
                    currentDrawingText.height = currentDrawingText.rectF.height()
                    currentDrawingText.width = currentDrawingText.rectF.width()

                    currentDrawingText.rotateAngle += currentDrawingText.newAngleChange
                    currentDrawingText.newAngleChange = 0F
                }
            }
        }

        _currentSelectedItemFlow.value = null
    }

    private fun findSelectedItem(event: MotionEvent): DrawingItem? {
        return drawingItemsList.findLast { drawingItem ->
            return@findLast when(drawingItem.type) {
                DrawingItemType.BRUSH -> false

                DrawingItemType.IMAGE -> {
                    drawingItem.drawingImage?.rectF?.contains(event.x, event.y) == true
                }

                DrawingItemType.TEXT -> {
                    drawingItem.drawingText?.rectF?.contains(event.x, event.y) == true
                }
            }
        }
    }

    private fun getCurrentDrawingPath(): DrawingPath {
        return DrawingPath(
            pathPoints = mutableListOf(),
            path = Path(),
            strokeColor = currentStrokeColor,
            strokeWidth = currentStrokeWidth,
            brushType = currentBrushType
        )
    }

    private fun getMaskFilterFromBrushType(brushType: BrushType?,
                                           strokeWidth: Float = currentStrokeWidth): MaskFilter? {
        val blurWidth = if(strokeWidth > 30) 30F else strokeWidth

        return when(brushType) {
            BrushType.SIMPLE -> null
            BrushType.NORMAL -> BlurMaskFilter(blurWidth * 2, BlurMaskFilter.Blur.NORMAL)
            BrushType.OUTLINE -> BlurMaskFilter(blurWidth, BlurMaskFilter.Blur.OUTER)
            BrushType.SOLID -> BlurMaskFilter(blurWidth * 2, BlurMaskFilter.Blur.SOLID)
            else -> null
        }
    }

    private fun getBaseDrawingPaint(): Paint {
        return Paint().apply {
            isAntiAlias = true
            color = currentStrokeColor
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = currentStrokeWidth
            maskFilter = getMaskFilterFromBrushType(currentBrushType)
        }
    }

    private fun getErasePaint(drawingPath: DrawingPath): Paint {
        return Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND

            color = Color.WHITE
            strokeWidth = drawingPath.strokeWidth
//            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            maskFilter = null
        }
    }

    private fun getSelectedBorderPaint(): Paint {
        return Paint().apply {
            isAntiAlias = true
            strokeWidth = 3F
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = ContextCompat.getColor(context, R.color.grey_200)
        }
    }

    private fun getImageDrawingPaint(drawingItem: DrawingItem): Paint {
        return Paint().apply {
            isAntiAlias = true
            strokeWidth = 3F
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            alpha = 255 - drawingItem.transparency
        }
    }

    private fun getDrawingImageMatrix(drawingImage: DrawingImage, bitmapWidth: Int, bitmapHeight: Int): Matrix {
        return Matrix().apply {
            preTranslate(drawingImage.rectF.left, drawingImage.rectF.top)

            postRotate(
                -(drawingImage.rotateAngle + drawingImage.newAngleChange),
                drawingImage.rectF.centerX(),
                drawingImage.rectF.centerY())

            preScale(
                drawingImage.rectF.width() / bitmapWidth,
                drawingImage.rectF.height() / bitmapHeight)
        }
    }

    private fun getDrawingTextMatrix(drawingText: DrawingText, bitmapWidth: Int, bitmapHeight: Int): Matrix {
        return Matrix().apply {
            preTranslate(drawingText.rectF.left, drawingText.rectF.top)

            postRotate(
                -(drawingText.rotateAngle + drawingText.newAngleChange),
                drawingText.rectF.centerX(),
                drawingText.rectF.centerY())

            preScale(
                drawingText.rectF.width() / bitmapWidth,
                drawingText.rectF.height() / bitmapHeight)
        }
    }

    private fun getDrawingImageRect(drawingImageBitmap: Bitmap): RectF {
        val largerDimensionOfBitmap = if(drawingImageBitmap.width > drawingImageBitmap.height)
            drawingImageBitmap.width else drawingImageBitmap.height
        val scaleFactorToDrawImage = CommonMethods
            .getScaleFactorAccordingToImageLargerDimension(largerDimensionOfBitmap, 600.0F)

        val updatedImageHeight = drawingImageBitmap.height * scaleFactorToDrawImage
        val updatedImageWidth = drawingImageBitmap.width * scaleFactorToDrawImage

        val rectLeft = (width / 2) - (updatedImageWidth / 2)
        val rectTop = (height / 2) - (updatedImageHeight / 2)
        val rectRight = rectLeft + updatedImageWidth
        val rectBottom = rectTop + updatedImageHeight

        return RectF(rectLeft, rectTop, rectRight, rectBottom)
    }

    private fun checkIfItemOverlapsSelectedItem(drawingItem: DrawingItem): Boolean {
        return _currentSelectedItemFlow.value?.let { currentSelectedItem ->
            var itemOverlaps = false

            val selectedItemRectF = if(currentSelectedItem.type == DrawingItemType.IMAGE) {
                currentSelectedItem.drawingImage?.rectF
            } else {
                currentSelectedItem.drawingText?.rectF
            }

            selectedItemRectF?.let { selectedRectF ->
                val itemRectF = when(drawingItem.type) {
                    DrawingItemType.BRUSH -> {
                        RectF().let {
                            drawingItem.drawingPath?.path?.computeBounds(it, true)
                            it
                        }
                    }
                    DrawingItemType.IMAGE -> drawingItem.drawingImage?.rectF
                    DrawingItemType.TEXT -> drawingItem.drawingText?.rectF
                }

                val rectF = RectF(selectedRectF)
                itemOverlaps = itemRectF != null && rectF.intersect(itemRectF)
            }

            return@let itemOverlaps
        } ?: false
    }

    private fun generatePathFromPathPoints(pathPoints: List<PointF>): Path {
        val path = Path()

        pathPoints.forEachIndexed { index, currentPoint ->
            if(index == 0) {
                path.reset()
                path.moveTo(currentPoint.x, currentPoint.y)
                return@forEachIndexed
            }

            if(index == pathPoints.size - 1) {
                path.lineTo(currentPoint.x, currentPoint.y)
                return@forEachIndexed
            }

            val prevPoint = pathPoints[index - 1]
            path.quadTo(prevPoint.x, prevPoint.y,
                (currentPoint.x + prevPoint.x) / 2, (currentPoint.y + prevPoint.y) / 2)
        }

        return path
    }

    /*********************************** PUBLIC FUNCTIONS *****************************************/

    fun getBrushSize() = currentStrokeWidth

    fun setBrushSize(width: Float) {
        currentStrokeWidth = if(width < 4F) 4F else width
    }

    fun getBrushColor() = currentStrokeColor

    fun setBrushColor(color: Int) {
        currentStrokeColor = color
    }

    fun setBrushType(brushType: BrushType) {
        currentBrushType = brushType
    }

    fun undoLastOperation() {
        if(drawingItemsList.isEmpty()) {
            return
        }

        val lastBrushDrawingItem = drawingItemsList.findLast { it.type == DrawingItemType.BRUSH }
        if(lastBrushDrawingItem != null) {
            deletedBrushDrawingItemsList.add(lastBrushDrawingItem)
            drawingItemsList.remove(lastBrushDrawingItem)

            invalidate()
        }
    }

    fun redoLastOperation() {
        if(deletedBrushDrawingItemsList.isEmpty()) {
            return
        }

        val lastDeletedBrushDrawingItem = deletedBrushDrawingItemsList.last()
        drawingItemsList.add(lastDeletedBrushDrawingItem)
        deletedBrushDrawingItemsList.removeAt(deletedBrushDrawingItemsList.lastIndex)

        invalidate()
    }

    fun clearDeletedBrushDrawingItems() {
        deletedBrushDrawingItemsList.clear()
    }

    fun setSelectedItemTransparency(transparency: Int) {
        _currentSelectedItemFlow.value?.let { currentSelectedItem ->
            currentSelectedItem.transparency = transparency

            invalidate()
        }
    }

    fun isSelectedItemAtFirstPosition(): Boolean {
        return _currentSelectedItemFlow.value?.let { currentSelectedItem ->
            currentSelectedItem.id == drawingItemsList.first().id
        } ?: false
    }

    fun isSelectedItemAtLastPosition(): Boolean {
        return _currentSelectedItemFlow.value?.let { currentSelectedItem ->
            currentSelectedItem.id == drawingItemsList.last().id
        } ?: false
    }

    fun bringSelectedItemForward(bringToFront: Boolean = false) {
        _currentSelectedItemFlow.value?.let { currentSelectedItem ->
            if(isSelectedItemAtLastPosition()) {
                return
            }

            val indexOfSelectedItem = drawingItemsList.indexOf(currentSelectedItem)
            val itemsAboveSelectedItem = drawingItemsList.subList(indexOfSelectedItem + 1,
                drawingItemsList.size)

            if(itemsAboveSelectedItem.isEmpty()) {
                return
            }

            val itemToBringSelectedItemAboveOf = try {
                if(bringToFront) {
                    itemsAboveSelectedItem.last { checkIfItemOverlapsSelectedItem(it) }
                } else {
                    itemsAboveSelectedItem.first { checkIfItemOverlapsSelectedItem(it) }
                }
            } catch(e: Exception) {
                null
            } ?: return

            drawingItemsList.remove(currentSelectedItem)
            val newIndexPosition = drawingItemsList.indexOf(itemToBringSelectedItemAboveOf) + 1

            drawingItemsList.add(newIndexPosition, currentSelectedItem)
            _currentSelectedItemFlow.value = null
            _currentSelectedItemFlow.value = currentSelectedItem
            invalidate()
        }
    }

    fun moveSelectedItemBackward(sendToBack: Boolean = false) {
        _currentSelectedItemFlow.value?.let { currentSelectedItem ->
            if(isSelectedItemAtFirstPosition()) {
                return
            }

            val indexOfSelectedItem = drawingItemsList.indexOf(currentSelectedItem)
            val itemsBelowSelectedItem = drawingItemsList.subList(0, indexOfSelectedItem)

            if(itemsBelowSelectedItem.isEmpty()) {
                return
            }

            val itemToBringSelectedItemAboveOf = try {
                if(sendToBack) {
                    itemsBelowSelectedItem.first { checkIfItemOverlapsSelectedItem(it) }
                } else {
                    itemsBelowSelectedItem.last { checkIfItemOverlapsSelectedItem(it) }
                }
            } catch(e: Exception) {
                null
            } ?: return

            drawingItemsList.remove(currentSelectedItem)

            val newIndexPosition = drawingItemsList.indexOf(itemToBringSelectedItemAboveOf)

            drawingItemsList.add(newIndexPosition, currentSelectedItem)
            _currentSelectedItemFlow.value = null
            _currentSelectedItemFlow.value = currentSelectedItem
            invalidate()
        }
    }

    fun toggleSelectedItemLock() {
        _currentSelectedItemFlow.update { currentSelectedItem ->
            currentSelectedItem?.apply {
                isLocked = !isLocked
            }
        }
    }

    fun deleteSelectedItem() {
        _currentSelectedItemFlow.value?.let { currentSelectedItem ->
            val indexOfSelectedItem = drawingItemsList.indexOf(currentSelectedItem)
            lastDeletedSelectedItemAndIndex = Pair(indexOfSelectedItem, currentSelectedItem)

            drawingItemsList.remove(currentSelectedItem)
            _currentSelectedItemFlow.value = null

            invalidate()
        }
    }

    fun undoLastSelectedItemDeletion() {
        lastDeletedSelectedItemAndIndex?.let { (itemIndex, item) ->
            drawingItemsList.add(itemIndex, item)
            _currentSelectedItemFlow.value = item

            invalidate()
        }
    }

    fun isSelectedItemTextElement(): Boolean {
        _currentSelectedItemFlow.value?.let { currentSelectedItem ->
            return currentSelectedItem.type == DrawingItemType.TEXT

        } ?: return false
    }

    fun addDrawingImageItem(imageUri: Uri) {
        val drawingImageBitmap = CommonMethods.getImageBitmapFromUri(context, imageUri)
        val drawingImageRect = getDrawingImageRect(drawingImageBitmap)

        val drawingImage = DrawingImage(
            uri = imageUri,
            rectF = drawingImageRect,
            height = drawingImageRect.height(),
            width = drawingImageRect.width(),
            bitmap = drawingImageBitmap,
            imageType = DrawingImageType.SIMPLE
        )

        val drawingImageItem = DrawingItem(
            type = DrawingItemType.IMAGE,
            drawingImage = drawingImage
        )

        drawingItemsList.add(drawingImageItem)
        invalidate()
    }

    fun addDrawingImageItem(userGraphicElement: UserGraphicElement) {
        userGraphicElement.savedElementUri?.let { elementUri ->
            val elementBitmap = CommonMethods.getImageBitmapFromUri(context, elementUri)
            val drawingImageRect = getDrawingImageRect(elementBitmap)

            val drawingImage = DrawingImage(
                uri = elementUri,
                rectF = drawingImageRect,
                height = drawingImageRect.height(),
                width = drawingImageRect.width(),
                bitmap = elementBitmap,

                savedElementId = userGraphicElement.id,
                imageType = DrawingImageType.USER_GRAPHIC_ELEMENT,
                graphicElementType = userGraphicElement.type
            )

            val drawingImageItem = DrawingItem(
                type = DrawingItemType.IMAGE,
                drawingImage = drawingImage
            )

            drawingItemsList.add(drawingImageItem)
            invalidate()
        }
    }

    fun addDrawingImageItem(userGradient: UserGradient) {
        userGradient.savedImageUri?.let { imageUri ->
            val elementBitmap = CommonMethods.getImageBitmapFromUri(context, imageUri)
            val drawingImageRect = getDrawingImageRect(elementBitmap)

            val drawingImage = DrawingImage(
                uri = imageUri,
                rectF = drawingImageRect,
                height = drawingImageRect.height(),
                width = drawingImageRect.width(),
                bitmap = elementBitmap,

                savedElementId = userGradient.id,
                imageType = DrawingImageType.USER_GRAPHIC_ELEMENT
            )

            val drawingImageItem = DrawingItem(
                type = DrawingItemType.IMAGE,
                drawingImage = drawingImage
            )

            drawingItemsList.add(drawingImageItem)
            invalidate()
        }
    }

    fun addTextDrawingItem(userTextElement: UserTextElement) {
        userTextElement.textImageUri?.let { textImageUri ->
            val drawingTextBitmap = CommonMethods.getImageBitmapFromUri(context, textImageUri)

            val largerDimensionOfBitmap = if(drawingTextBitmap.width > drawingTextBitmap.height)
                drawingTextBitmap.width else drawingTextBitmap.height
            val scaleFactorToDrawImage = CommonMethods
                .getScaleFactorAccordingToImageLargerDimension(largerDimensionOfBitmap, 600.0F)

            val updatedImageHeight = drawingTextBitmap.height * scaleFactorToDrawImage
            val updatedImageWidth = drawingTextBitmap.width * scaleFactorToDrawImage

            val rectLeft = (width / 2) - (updatedImageWidth / 2)
            val rectTop = (height / 2) - (updatedImageHeight / 2)
            val rectRight = rectLeft + updatedImageWidth
            val rectBottom = rectTop + updatedImageHeight

            val drawingTextRect = RectF(rectLeft, rectTop, rectRight, rectBottom)

            val drawingText = DrawingText(
                textElementId = userTextElement.id,
                textImageUri = textImageUri,
                rectF = drawingTextRect,
                height = drawingTextRect.height(),
                width = drawingTextRect.width(),
                bitmap = drawingTextBitmap
            )

            val drawingTextItem = DrawingItem(
                type = DrawingItemType.TEXT,
                drawingText = drawingText
            )

            drawingItemsList.add(drawingTextItem)
            invalidate()
        }
    }

    fun getCurrentSelectedItem(): DrawingItem? = _currentSelectedItemFlow.value

    fun setCurrentSelectedItem(item: DrawingItem?) {
        _currentSelectedItemFlow.value = item
    }

    fun isEmpty() = drawingItemsList.isEmpty()

    fun getAllDrawingItems() = drawingItemsList

    fun setDrawingItems(drawingItems: List<DrawingItem>) {
        _loadingFlow.value = true
        drawingItemsList = drawingItems.toMutableList()

        drawingItemsList.forEach { drawingItem ->
            when(drawingItem.type) {
                DrawingItemType.IMAGE -> {
                    drawingItem.drawingImage?.let { drawingImage ->
                        drawingImage.bitmap = CommonMethods
                            .getImageBitmapFromUri(context, drawingImage.uri)
                    }
                }

                DrawingItemType.TEXT -> {
                    drawingItem.drawingText?.let { drawingText ->
                        drawingText.bitmap = CommonMethods
                            .getImageBitmapFromUri(context, drawingText.textImageUri)
                    }
                }

                DrawingItemType.BRUSH -> {
                    drawingItem.drawingPath?.let {
                        it.path = generatePathFromPathPoints(it.pathPoints)
                    }
                }
            }
        }

        invalidate()
        _loadingFlow.value = false
    }

    fun saveDrawingImage(): Uri? {
        currentDrawing?.let { drawing ->
            drawing.localDrawingImgUri?.path?.let {
                File(it).delete()
            }
        }

        /*COPYING PAINTING ON BITMAP CANVAS*/
        val srcBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bitmapCanvas = Canvas(srcBitmap)

        if(currentDrawing == null) {
            bitmapCanvas.drawColor(Color.WHITE)
        } else {
            drawCurrentDrawingOnCanvas(bitmapCanvas)
        }

        for(drawingItem in drawingItemsList) {
            drawItemOnCanvas(bitmapCanvas, drawingItem, true)
        }

        /*COMPRESSING BITMAP AND SAVING AS PNG IMAGE*/
        val timeStamp = SimpleDateFormat(
            Constants.DATE_TIME_FORMAT_1,
            Locale.UK
        ).format(System.currentTimeMillis())

        val filePath =
            context.getExternalFilesDir(Constants.DIR_DRAWINGS)?.path + "/DRAWING$timeStamp.png"
        val file = File(currentDrawing?.localDrawingImgUri?.path ?: filePath)

        return try {
            val outputStream = FileOutputStream(file)
            srcBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            srcBitmap.recycle()

            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private var userGestureDetector: UserGestureDetector =
        UserGestureDetector(object: UserGestureDetector.GestureEventListener {
            override fun onDragGestureDetected(event: MotionEvent) {
                initiateSelectedItemDragging(event)
            }

            override fun onScaleGestureDetected(scaleFactor: Float) {
                changeSelectedItemSize(scaleFactor)
            }

            override fun onRotateGestureDetected(rotatedAngle: Float) {
                rotateSelectedItem(rotatedAngle)
            }
        })
}