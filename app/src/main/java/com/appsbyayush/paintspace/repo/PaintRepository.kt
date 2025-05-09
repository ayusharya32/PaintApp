package com.appsbyayush.paintspace.repo

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.appsbyayush.paintspace.api.PaintApi
import com.appsbyayush.paintspace.db.PaintDatabase
import com.appsbyayush.paintspace.models.*
import com.appsbyayush.paintspace.paging.gradients.GradientRemoteMediator
import com.appsbyayush.paintspace.paging.graphicelements.GraphicElementRemoteMediator
import com.appsbyayush.paintspace.utils.*
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import networkBoundResource
import java.io.File
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class PaintRepository @Inject constructor (
    private val paintApi: PaintApi,
    private val paintDatabase: PaintDatabase,
    private val appSharedPrefs: SharedPreferences,
    private val app: Application
) {
    companion object {
        private const val TAG = "PaintRepositoryyy"
    }

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val storage: FirebaseStorage = Firebase.storage

    private val paintDao = paintDatabase.getPaintDao()

    suspend fun insertDrawing(drawing: Drawing) {
        drawing.isSynced = false
        paintDao.insertDrawing(drawing)
    }

    suspend fun insertDrawingItems(drawingItems: List<DrawingItem>) {
        paintDao.insertDrawingItemsList(drawingItems)
    }

    suspend fun insertUserGradient(userGradient: UserGradient){
        paintDao.insertUserGradient(userGradient)
    }

    suspend fun insertUserGraphicElement(userGraphicElement: UserGraphicElement){
        paintDao.insertUserGraphicElement(userGraphicElement)
    }

    suspend fun insertUserTextElement(userTextElement: UserTextElement){
        paintDao.insertUserTextElement(userTextElement)
    }

    suspend fun getUserTextElement(elementId: String): UserTextElement? {
        return paintDao.getUserTextElement(elementId)
    }

    suspend fun getDrawings(searchQuery: String): Flow<List<Drawing>> {
        return if(searchQuery.isEmpty()) {
            Log.d(TAG, "getAllDrawings: ${paintDao.getAllUntrashedDrawings().first()}")
            paintDao.getAllUntrashedDrawings()
        } else {
            paintDao.searchDrawings("%$searchQuery%")
        }
    }

    fun getAllTrashedDrawings(): Flow<List<Drawing>> {
        return paintDao.getAllTrashedDrawings()
    }

    suspend fun isDrawingDraftPresent(): Boolean {
        return paintDao.getDrawingItemsCount() > 0
    }

    suspend fun getAllDrawingItems(): List<DrawingItem> {
        return paintDao.getDrawingItems()
    }

    fun searchForGraphicElements(searchQuery: String = ""): Flow<PagingData<GraphicElement>> {
        return Pager(
            config = PagingConfig(pageSize = 20, maxSize = 200),
            remoteMediator = GraphicElementRemoteMediator(searchQuery, paintApi, paintDatabase),
            pagingSourceFactory = { paintDao.getGraphicElementsForSearchQuery(searchQuery) }
        ).flow
    }

    fun searchForGradients(searchQuery: String = ""): Flow<PagingData<Gradient>> {
        return Pager(
            config = PagingConfig(pageSize = 5, maxSize = 200),
            remoteMediator = GradientRemoteMediator(searchQuery, paintApi, paintDatabase),
            pagingSourceFactory = { paintDao.getGradientsForSearchQuery(searchQuery) }
        ).flow
    }

    fun getUserGraphicElements(forcedSync: Boolean = false): Flow<Resource<List<UserGraphicElement>>>
    = networkBoundResource(
        localFetch = { paintDao.getUserGraphicElements() },
        syncWithRemoteDB = { locallySavedElements ->
            val unsyncedElements = locallySavedElements.filter { !it.isSynced }
            syncUserGraphicElements(unsyncedElements)
       },
        saveRemoteDbSyncResult = { latestElementsList ->
            latestElementsList.forEach { element ->
                element.isSynced = true
            }

            paintDao.insertUserGraphicElementList(latestElementsList)
        },
        shouldSync = { locallySavedElements ->
            val lastSyncTimeStamp = getSyncTimestampForQuery(
                Constants.PREFS_KEY_USER_GRAPHIC_ELEMENTS_SYNC_TIMESTAMP
            )

            if(locallySavedElements.any { !it.isSynced } || timeToSync(lastSyncTimeStamp) || forcedSync) {
                 shouldSync()
            } else {
                false
            }
        },
        onSyncFailed = { /* DO NOTHING */ }
    )

    fun getUserGradients(forcedSync: Boolean = false): Flow<Resource<List<UserGradient>>> = networkBoundResource(
        localFetch = { paintDao.getUserGradients() },
        syncWithRemoteDB = { locallySavedUserGradients ->
            val unsyncedElements = locallySavedUserGradients.filter { !it.isSynced }
            syncUserGradients(unsyncedElements)
        },
        saveRemoteDbSyncResult = { latestGradientsList ->
            latestGradientsList.forEach {
                it.isSynced = true
            }

            paintDao.insertUserGradientsList(latestGradientsList)
        },
        shouldSync = { locallySavedUserGradients ->
            val lastSyncTimeStamp = getSyncTimestampForQuery(Constants.PREFS_KEY_USER_GRADIENTS_SYNC_TIMESTAMP)

            if(locallySavedUserGradients.any { !it.isSynced } || timeToSync(lastSyncTimeStamp) || forcedSync) {
                shouldSync()
            } else {
                false
            }
        },
        onSyncFailed = { /* DO NOTHING */ }
    )

    fun getFonts(forcedSync: Boolean = false): Flow<Resource<List<FontItem>>> = networkBoundResource(
        localFetch = { paintDao.getFontItems() },
        syncWithRemoteDB = { locallySavedFonts ->
            getAllFontsFromFirebase().onEach { latestFont ->
                val locallySavedFont = locallySavedFonts.find { it.id == latestFont.id }
                if(locallySavedFont != null) {
                    latestFont.fontFileLocalUri = locallySavedFont.fontFileLocalUri
                }
            }
        },
        saveRemoteDbSyncResult = { latestFontItems ->
            Log.d(TAG, "getFonts: FontsFlow $latestFontItems")
            paintDao.insertFontItemsList(latestFontItems)
        },
        shouldSync = { locallySavedFonts ->
            val lastSyncTimeStamp = getSyncTimestampForQuery(Constants.PREFS_KEY_FONTS_SYNC_TIMESTAMP)
            val shouldSync = if(locallySavedFonts.isEmpty() || timeToSync(lastSyncTimeStamp) || forcedSync) {
                getNetworkStatus(app.applicationContext) != 0
            } else {
                false
            }

            Log.d(TAG, "getFonts: FontsFlow $shouldSync")
            shouldSync
        },
        onSyncFailed = {
            Log.d(TAG, "getFonts: FontsFlow $it")
        /* DO NOTHING */ }
    )

    suspend fun clearDrawingsTable() {
        paintDao.clearDrawingsTable()
    }

    suspend fun clearDrawingItemsTable() {
        paintDao.clearDrawingItemsTable()
    }

    suspend fun clearTrashedDrawingsOlderThan30days() {
        val timestampBefore30Days = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)

        val trashedDrawingsBefore30Days = paintDao.getTrashedDrawingsOlderThanTimestamp(timestampBefore30Days)
        val firebaseDrawingsDeleted = clearTrashedDrawingsFromFirebase(trashedDrawingsBefore30Days)

        if(firebaseDrawingsDeleted) {
            paintDao.clearTrashedDrawingsOlderThanTimestamp(timestampBefore30Days)
        }
    }

    suspend fun syncDrawings(): Boolean = coroutineScope {
        if(!shouldSync()) {
            return@coroutineScope false
        }

        val unsyncedDrawings = paintDao.getAllUnsyncedDrawings()

        val uploadOperations = unsyncedDrawings.map { drawing ->
            launch(Dispatchers.IO) {
                drawing.userId = auth.currentUser?.uid
                drawing.isImageUploaded = if(!drawing.isImageUploaded) {
                    uploadDrawingImageToFirebase(drawing)
                } else {
                    true
                }

                if(drawing.isImageUploaded) {
                    uploadDrawingToFirebase(drawing)
                }
            }
        }

        uploadOperations.joinAll()

        val locallySavedDrawings = paintDao.getAllDrawings().first()
        val latestFetchedDrawings = getAllDrawingsFromFirebase().onEach { currentDrawing ->
            val locallySavedDrawing = locallySavedDrawings.find { it.id == currentDrawing.id }

            locallySavedDrawing?.let { savedDrawing ->
                currentDrawing.localDrawingImgUri = savedDrawing.localDrawingImgUri

                currentDrawing.localShareableDrawingImgContentUri =
                    savedDrawing.localShareableDrawingImgContentUri
            }

            if(currentDrawing.localDrawingImgUri == null) {
                Log.d(TAG, "syncDrawings: Local Drawing Uri NULL")
                saveDrawingImage(currentDrawing)
            }

            currentDrawing.isSynced = true
        }

        paintDao.insertDrawingsList(latestFetchedDrawings)
        return@coroutineScope true
    }

    suspend fun saveDrawingImage(drawing: Drawing) {
        val timeStamp = SimpleDateFormat(
            Constants.DATE_TIME_FORMAT_1,
            Locale.UK
        ).format(System.currentTimeMillis())

        val drawingImagePath = app.applicationContext.getExternalFilesDir(Constants.DIR_DRAWINGS)
            ?.path + "/DRAWING$timeStamp.png"

        drawing.localDrawingImgUri = CommonMethods.saveImageFromUrl(app.applicationContext,
            drawing.drawingImgUrl!!, drawingImagePath)
    }

    private suspend fun getAllDrawingsFromFirebase(): List<Drawing> {
        val querySnapshot = firestore.collection(Constants.COLLECTION_DRAWINGS)
            .whereEqualTo(Constants.FIELD_USER_ID, auth.currentUser?.uid)
            .get()
            .await()

        querySnapshot?.let {
            return it.toObjects()
        }

        return listOf()
    }

    private suspend fun uploadDrawingImageToFirebase(drawing: Drawing): Boolean {
        return drawing.localDrawingImgUri?.let { drawingImageUri ->
            try {
                val imageFile = drawingImageUri.path?.let { File(it) } ?: return false

                val storagePath = "${auth.currentUser?.uid}/drawings/${drawing.id}" +
                        ".${CommonMethods.getExtensionFromFileName(imageFile.name)}"
                val storageRef = storage.getReference(storagePath)

                storageRef.putFile(drawingImageUri).await()
                val downloadUri = storageRef.downloadUrl.await()

                drawing.drawingImgUrl = downloadUri.toString()
                true

            } catch(e: Exception) {
                Log.d(TAG, "uploadDrawingImageToFirebase: $e")
                false
            }
        } ?: false
    }

    private suspend fun uploadDrawingToFirebase(drawing: Drawing) {
        firestore.collection(Constants.COLLECTION_DRAWINGS)
            .document(drawing.id)
            .set(drawing)
            .await()
    }

    suspend fun downloadFontFile(fontItem: FontItem): FontItem {
        val fontFileLocalUri = downloadFontFileFromUrl(fontItem)
        fontItem.fontFileLocalUri = fontFileLocalUri
        paintDao.insertFontItem(fontItem)

        return fontItem
    }

    private suspend fun downloadFontFileFromUrl(fontItem: FontItem): Uri  = withContext(Dispatchers.IO) {
        Log.d(TAG, "onFontItemClicked downloadFontFileFromUrl: Called")

        val fontFileRef = storage.getReferenceFromUrl(fontItem.fontFileUrl)

        val fontFileName = "FONT${fontItem.id.replace("-", "")}" +
                ".${fontFileRef.name.split(".")[1]}"
        val fontFilePath = "${app.applicationContext.getExternalFilesDir("fonts")}/$fontFileName"
        val fontFile = File(fontFilePath)

        fontFileRef.getFile(fontFile).await()
        return@withContext fontFile.toUri()
    }

    private suspend fun syncUserGraphicElements(unsyncedElements:
                                                List<UserGraphicElement>): List<UserGraphicElement> {
        unsyncedElements.forEach {
            it.userId = auth.currentUser?.uid
            uploadUserGraphicElement(it)
        }

        setSyncTimestampForQuery(Constants.PREFS_KEY_USER_GRAPHIC_ELEMENTS_SYNC_TIMESTAMP)
        return getAllUserGraphicElementsFromFirebase()
    }

    private suspend fun syncUserGradients(unsyncedElements: List<UserGradient>): List<UserGradient> {
        unsyncedElements.forEach {
            it.userId = auth.currentUser?.uid
            uploadUserGradient(it)
        }

        setSyncTimestampForQuery(Constants.PREFS_KEY_USER_GRADIENTS_SYNC_TIMESTAMP)
        return getAllUserGradientsFromFirebase()
    }

    private suspend fun getAllFontsFromFirebase(): List<FontItem> {
        val querySnapshot = firestore.collection(Constants.COLLECTION_FONTS)
            .get()
            .await()

        setSyncTimestampForQuery(Constants.PREFS_KEY_FONTS_SYNC_TIMESTAMP)
        querySnapshot?.let {
            return it.toObjects()
        }

        return listOf()
    }

    private suspend fun getAllUserGraphicElementsFromFirebase(): List<UserGraphicElement> {
        val querySnapshot = firestore.collection(Constants.COLLECTION_USER_GRAPHIC_ELEMENTS)
            .whereEqualTo(Constants.FIELD_USER_ID, auth.currentUser?.uid)
            .orderBy(Constants.FIELD_MODIFIED_AT, Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .await()

        querySnapshot?.let {
            return it.toObjects()
        }

        return listOf()
    }

    private suspend fun getAllUserGradientsFromFirebase(): List<UserGradient> {
        val querySnapshot = firestore.collection(Constants.COLLECTION_USER_GRADIENTS)
            .whereEqualTo(Constants.FIELD_USER_ID, auth.currentUser?.uid)
            .orderBy(Constants.FIELD_MODIFIED_AT, Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .await()

        querySnapshot?.let {
            return it.toObjects()
        }

        return listOf()
    }

    private suspend fun uploadUserGraphicElement(userGraphicElement: UserGraphicElement) {
        firestore.collection(Constants.COLLECTION_USER_GRAPHIC_ELEMENTS)
            .document(userGraphicElement.id)
            .set(userGraphicElement)
            .await()
    }

    private suspend fun uploadUserGradient(userGradient: UserGradient) {
        firestore.collection(Constants.COLLECTION_USER_GRADIENTS)
            .document(userGradient.id)
            .set(userGradient)
            .await()
    }

    private suspend fun clearTrashedDrawingsFromFirebase(drawingsList: List<Drawing>): Boolean {
        return try {
            val batch = firestore.batch()

            drawingsList.forEach {
                val docRef = firestore.collection(Constants.COLLECTION_DRAWINGS)
                    .document(it.id)

                batch.delete(docRef)
            }

            batch.commit().await()
            true
        } catch(e: Exception) {
            Log.d(TAG, "clearTrashedDrawingsFromFirebase: $e")
            false
        }
    }

    private suspend fun clearLocalUrisFromElementTables() {
        val userGraphicElements = paintDao.getUserGraphicElements().first()
        val userGradients = paintDao.getUserGradients().first()
        val userTextElements = paintDao.getAllUserTextElements()

        userGraphicElements.forEach {
            it.savedElementUri = null
            it.savedAddedMaskUri= null
        }

        userGradients.forEach {
            it.savedImageUri = null
        }

        userTextElements.forEach {
            it.textImageUri = null
        }

        paintDao.insertUserGraphicElementList(userGraphicElements)
        paintDao.insertUserGradientsList(userGradients)
        paintDao.insertUserTextElementList(userTextElements)
    }

    private fun timeToSync(lastSyncTimestamp: Long): Boolean {
        return if(lastSyncTimestamp != 0L) {
            val timeElapsed = System.currentTimeMillis() - lastSyncTimestamp
            timeElapsed > TimeUnit.HOURS.toMillis(2)
        } else {
            true
        }
    }

    private fun shouldSync(): Boolean {
        return when {
            auth.currentUser == null -> false
            getNetworkStatus(app.applicationContext) == 0 -> false
            else -> true
        }
    }

    /***************************************** AUTH ***********************************************/

    suspend fun firebaseSignInWithCredentials(idToken: String): AuthResult {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        return auth.signInWithCredential(firebaseCredential).await()
    }

    fun getAuthenticatedUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun logoutUser() {
        return auth.signOut()
    }

    /************************************** SHARED PREFS ******************************************/

    private fun getSyncTimestampForQuery(queryKey: String): Long {
        return appSharedPrefs.getLong(queryKey, 0)
    }

    private fun setSyncTimestampForQuery(queryKey: String) {
        val editor = appSharedPrefs.edit()

        editor.putLong(queryKey, System.currentTimeMillis())
        editor.apply()
    }

    fun saveAppSettings(appSettings: AppSettings) {
        val appSettingsJson = Gson().toJson(appSettings)

        val editor = appSharedPrefs.edit()
        editor.putString(Constants.PREFS_KEY_APP_SETTINGS, appSettingsJson)
        editor.apply()
    }

    fun getAppSettings(): AppSettings {
        val appSettingsJson = appSharedPrefs.getString(Constants.PREFS_KEY_APP_SETTINGS, "")

        return if(appSettingsJson.isNullOrEmpty()) AppSettings()
        else Gson().fromJson(appSettingsJson, AppSettings::class.java)
    }

    fun saveCurrentSyncProcessId(processId: String) {
        val editor = appSharedPrefs.edit()
        editor.putString(Constants.PREFS_KEY_CURRENT_SYNC_PROCESS_ID, processId)
        editor.apply()
    }

    fun getCurrentSyncProcessId(): String {
        val currentSyncProcessId = appSharedPrefs.getString(
            Constants.PREFS_KEY_CURRENT_SYNC_PROCESS_ID, "")

        return currentSyncProcessId ?: ""
    }

    fun isOnboardingDone(): Boolean {
        return appSharedPrefs.getBoolean(Constants.PREFS_KEY_ONBOARDING_DONE, false)
    }

    fun setOnboardingDone() {
        val editor = appSharedPrefs.edit()
        editor.putBoolean(Constants.PREFS_KEY_ONBOARDING_DONE, true)
        editor.apply()
    }

    fun resetAllSyncTimestamps() {
        val editor = appSharedPrefs.edit()
        editor.apply {
            putLong(Constants.PREFS_KEY_USER_GRAPHIC_ELEMENTS_SYNC_TIMESTAMP, 0)
            putLong(Constants.PREFS_KEY_USER_GRADIENTS_SYNC_TIMESTAMP, 0)
        }
        editor.apply()
    }

    suspend fun deleteAllTemporaryMediaFiles() {
        val elementsFolder = app.applicationContext.getExternalFilesDir(Constants.DIR_ELEMENTS)

        elementsFolder?.let {
            if(!it.exists() || it.listFiles().isNullOrEmpty()) {
                return
            }

            elementsFolder.deleteRecursively()
            clearLocalUrisFromElementTables()
        }
    }

    suspend fun deleteAllLocalMediaFiles() {
        deleteAllTemporaryMediaFiles()
        val drawingsFolder = app.applicationContext.getExternalFilesDir(Constants.DIR_DRAWINGS)

        drawingsFolder?.let {
            if(!it.exists() || it.listFiles().isNullOrEmpty()) {
                return
            }

            drawingsFolder.deleteRecursively()
        }
    }

    /************************************* APP PERMISSIONS **************************************/

    fun getAppPermissions(): List<AppPermission> {
        val appPermissionsJson = appSharedPrefs.getString(Constants.PREFS_KEY_APP_PERMISSIONS, "")

        if(appPermissionsJson.isNullOrEmpty()) {
            return emptyList()
        }

        val type: Type = object: TypeToken<List<AppPermission>>() {}.type
        return Gson().fromJson(appPermissionsJson, type)
    }

    fun updateAppPermissions(permissions: List<AppPermission>) {
        val editor = appSharedPrefs.edit()
        editor.putString(Constants.PREFS_KEY_APP_PERMISSIONS, Gson().toJson(permissions))
        editor.apply()
    }
}