package com.appsbyayush.paintspace.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import com.appsbyayush.paintspace.models.UserGradient
import com.appsbyayush.paintspace.utils.Constants.FILE_EXT_JPG
import com.appsbyayush.paintspace.utils.Constants.FILE_EXT_PNG
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object CommonMethods {
    private const val TAG = "CommonMethods"

    fun getFormattedDateTime(date: Date, format: String): String {
        return try {
            val sdf = SimpleDateFormat(format, Locale.UK)
            sdf.format(date)

        } catch(e: Exception) {
            Log.d(TAG, "getFormattedDateTime: $e")
            "Error in Formatting Date"
        }
    }

    fun getTimeAgoString(date: Date): String {
        val currentTimestamp = System.currentTimeMillis()
        val dateInMillis = date.time

        val differenceInSeconds = (currentTimestamp - dateInMillis) / 1000
        if(differenceInSeconds < 60) {
            return "$differenceInSeconds second${if(differenceInSeconds > 1) "s" else ""} ago"
        }

        val differenceInMinutes = differenceInSeconds / 60
        if(differenceInMinutes < 60) {
            return "$differenceInMinutes minute${if(differenceInMinutes > 1) "s" else ""} ago"
        }

        val differenceInHours = differenceInMinutes / 60
        if(differenceInHours < 24) {
            return "$differenceInHours hour${if(differenceInHours > 1) "s" else ""} ago"
        }

        val differenceInDays = differenceInHours / 24
        if(differenceInDays < 30) {
            return "$differenceInDays day${if(differenceInDays > 1) "s" else ""} ago"
        }

        return ""
    }

    fun isDateOfToday(date: Date): Boolean {
        val todayCalendar = Calendar.getInstance()
        val givenDateCalender = Calendar.getInstance()
        givenDateCalender.time = date

        return (todayCalendar[Calendar.DAY_OF_YEAR] == givenDateCalender[Calendar.DAY_OF_YEAR]
                && todayCalendar[Calendar.YEAR] == givenDateCalender[Calendar.YEAR])
    }

    fun isDateOfYesterday(date: Date): Boolean {
        val yesterdayCalendar = Calendar.getInstance()
        yesterdayCalendar.add(Calendar.DAY_OF_YEAR, -1)
        val givenDateCalender = Calendar.getInstance()
        givenDateCalender.time = date

        return (yesterdayCalendar[Calendar.DAY_OF_YEAR] == givenDateCalender[Calendar.DAY_OF_YEAR]
                && yesterdayCalendar[Calendar.YEAR] == givenDateCalender[Calendar.YEAR])
    }

    @Throws(java.lang.Exception::class)
    fun getImageBitmapFromUri(context: Context, imageUri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        } else {
            val source = ImageDecoder.createSource(
                context.contentResolver,
                imageUri
            )
            ImageDecoder.decodeBitmap(source)
        }
    }

    suspend fun getCompressedImageUri(context: Context, uri: Uri, saveDirName: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                var bitmap = getImageBitmapFromUri(context, uri)
                if(bitmap.width > 1500 || bitmap.height > 1500) {
                    val largerDimension = if(bitmap.width > bitmap.height)  bitmap.width
                        else bitmap.height
                    val scaleFactor = getScaleFactorAccordingToImageLargerDimension(largerDimension,
                        1500.0F)
                    bitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scaleFactor).toInt(),
                        (bitmap.height * scaleFactor).toInt(), false)
                }

                val imageType = context.contentResolver.getType(uri)
                val compressionFormat = getCompressionFormatFromImageType(imageType)

                Log.d(TAG, "getCompressedImageUri: Image Type: $imageType")
                Log.d(TAG, "getCompressedImageUri: CompressFormat: $compressionFormat")

                val fileName = "IMG" + getFormattedDateTime(
                    Calendar.getInstance().time,
                    Constants.DATE_TIME_FORMAT_1
                )

                val filePath = "${context.getExternalFilesDir(saveDirName)}/$fileName" +
                        ".${getFileExtensionFromCompressionFormat(compressionFormat)}"
                val file = File(filePath)

                var outputStream = FileOutputStream(file)

                bitmap.compress(compressionFormat, 100, outputStream)
                Log.d(TAG, "getCompressedImageUri: Image Size With 100% Quality: ${file.length() / 1024} KB")

                val imageSizeInBytes = file.length()
                if(imageSizeInBytes > getBytesFromKb(50)) {
                    file.delete()

                    if(file.createNewFile()) {
                        outputStream = FileOutputStream(file)

                        val compressionQuality = getCompressionQualityAccordingToImageSize(imageSizeInBytes)
                        bitmap.compress(compressionFormat, compressionQuality, outputStream)

                        Log.d(TAG, "getCompressedImageUri: Second Compression Done!" +
                                " with $compressionQuality quality")
                    }
                }

                Log.d(TAG, "getCompressedImageUri: Image Size After Second Compression(if needed):" +
                        " ${file.length() / 1024} KB")

                outputStream.flush()
                outputStream.close()
                Uri.fromFile(file)

            } catch(e: Exception) {
                Log.d(TAG, "getCompressedImageUri Error: ${e.message}")
                return@withContext null
            }
        }
    }

    fun getExtensionFromFileName(fileName: String): String {
        val dotSplitList = fileName.split(".")
        return dotSplitList.last()
    }

    fun showSoftKeyboard(context: Context, view: View) {
        if (view.requestFocus()) {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun hideSoftKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun getDistanceBetweenPoints(x1: Float, x2: Float, y1: Float, y2: Float): Float {
        val xAxisDifference = x2 - x1
        val yAxisDifference = y2 - y1

        return sqrt(xAxisDifference.pow(2) + yAxisDifference.pow(2))
    }

    fun getDistanceBetweenLineAndPoint(
        xCoeffOfLine: Float, yCoeffOfLine: Float, lineConstant: Float,
        pointX: Float, pointY: Float
    ): Float {
        val numerator = abs(xCoeffOfLine * pointX + yCoeffOfLine * pointY + lineConstant)
        val denominator = sqrt(xCoeffOfLine.pow(2) + yCoeffOfLine.pow(2))

        return numerator / denominator
    }

    fun getBytesFromKb(kb: Int) = kb * 1024

    fun getCompressionQualityAccordingToImageSize(imageSizeInBytes: Long): Int {
        return when(imageSizeInBytes) {
            in getBytesFromKb(50)..getBytesFromKb(100) -> 90
            in getBytesFromKb(101)..getBytesFromKb(200) -> 80
            in getBytesFromKb(201)..getBytesFromKb(500) -> 75
            in getBytesFromKb(501)..getBytesFromKb(1024) -> 65
            in getBytesFromKb(1025)..getBytesFromKb(2048) -> 50
            else -> 40
        }
    }

    fun getCompressionFormatFromImageType(imageType: String?): Bitmap.CompressFormat {
        return when(imageType) {
            Constants.IMAGE_TYPE_JPG,  Constants.IMAGE_TYPE_JPEG -> {
                Bitmap.CompressFormat.JPEG
            }
            Constants.IMAGE_TYPE_PNG -> {
                Bitmap.CompressFormat.PNG
            }
            else -> Bitmap.CompressFormat.PNG
        }
    }

    fun getFileExtensionFromCompressionFormat(compressFormat: Bitmap.CompressFormat): String {
        return when(compressFormat) {
            Bitmap.CompressFormat.JPEG -> FILE_EXT_JPG
            else -> FILE_EXT_PNG
        }
    }

    fun getScaleFactorAccordingToImageLargerDimension(largerDimension: Int, resultSizeInPx: Float): Float {
        return DecimalFormat("#.##").format(1 / (largerDimension / resultSizeInPx)).toFloat()
    }

    fun getColorDarkness(color: Int): Double {
        return 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color)) / 255
    }

    suspend fun saveImageFromUrl(context: Context, url: String,
                                 imagePath: String): Uri? = withContext(Dispatchers.IO) {
        val imageBitmap = Glide.with(context)
            .asBitmap()
            .load(url)
            .submit()
            .get()

        return@withContext saveImageFile(imageBitmap, imagePath)
    }

    suspend fun saveGradientImage(gradientDrawable: Drawable,
                                  imagePath: String): Uri? = withContext(Dispatchers.IO) {
        val imageBitmap = gradientDrawable.toBitmap(800, 800, Bitmap.Config.ARGB_8888)
        return@withContext saveImageFile(imageBitmap, imagePath)
    }

    fun getContentUriFromFileUri(context: Context, fileUri: Uri): Uri? {
        val file = fileUri.path?.let { File(it) }

        return file?.let {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                it
            )
        }
    }

    private suspend fun saveImageFile(imageBitmap: Bitmap, imagePath: String): Uri? = withContext(Dispatchers.IO) {
        return@withContext try {
            val imageFile = File(imagePath)
            val outputStream = FileOutputStream(imageFile)
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            imageBitmap.recycle()

            Uri.fromFile(imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}