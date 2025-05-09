import com.appsbyayush.paintspace.utils.Resource

import kotlinx.coroutines.flow.*

/**
 * PERFORMING CACHING USING NetworkBoundResource
 *
 * STEP 1 -> Check if there is data present in local database
 * STEP 2 -> Check if there is need to sync remotely
 * STEP 3 -> IF shouldSync() = true; Sync with remote database and save result in local DB
 * STEP 4 -> IF shouldSync() = false; No need to sync, just return locally fetched data
 * STEP 5 -> Emit all values
 */

inline fun <ResultType, RequestType> networkBoundResource(
    crossinline localFetch: () -> Flow<ResultType>,
    crossinline syncWithRemoteDB: suspend (ResultType) -> RequestType,
    crossinline saveRemoteDbSyncResult: suspend (RequestType) -> Unit,
    crossinline shouldSync: (ResultType) -> Boolean = { true },
    crossinline onSyncFailed: (Throwable) -> Unit = { }
) = flow {
    val data = localFetch().first()

    val response = if (shouldSync(data)) {
        emit(Resource.Loading())

        try {
            val syncResult = syncWithRemoteDB(data)
            saveRemoteDbSyncResult(syncResult)

            localFetch().map { Resource.Success(it) }

        } catch (t: Throwable) {
            onSyncFailed(t)

            localFetch().map { Resource.Error(t, it) }
        }

    } else {
        localFetch().map { Resource.Success(it) }
    }

    emitAll(response)
}