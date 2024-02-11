import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.example.bluromatic.workers.BlurWorker
import com.example.bluromatic.workers.CleanupWorker
import com.example.bluromatic.workers.SaveImageToFileWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkerInstrumentationTest {

    val keyImageUri = "KEY_IMAGE_URI"
    val imageUri = "android.resource://com.example.bluromatic/drawable/android_cupcake"
    val mockUriInput = Pair(keyImageUri, imageUri)

    private lateinit var _context: Context

    @Before
    fun setUp() {
        _context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun cleanupWorker_doWork_resultSuccess() {
        val cleanupWorker = TestListenableWorkerBuilder<CleanupWorker>(_context).build()
        runBlocking {
            val result = cleanupWorker.doWork()
            assertTrue(result is ListenableWorker.Result.Success)
        }
    }

    @Test
    fun blurWorker_doWork_resultSuccessReturnsUri() {
        val blurWorker = TestListenableWorkerBuilder<BlurWorker>(_context)
            .setInputData(workDataOf(mockUriInput))
            .build()
        runBlocking {
            val result = blurWorker.doWork()
            val resultUri = result.outputData.getString(keyImageUri)
            assertTrue(result is ListenableWorker.Result.Success)
            assertTrue(result.outputData.keyValueMap.containsKey(keyImageUri))
            assertTrue(
                resultUri?.startsWith("file:///data/user/0/com.example.bluromatic/files/blur_filter_outputs/blur-filter-output-")
                    ?: false
            )
        }
    }

    @Test
    fun saveImageToFileWorker_doWork_resultSuccessReturnsUrl() {
        val worker = TestListenableWorkerBuilder<SaveImageToFileWorker>(_context)
            .setInputData(workDataOf(mockUriInput))
            .build()
        runBlocking {
            val result = worker.doWork()
            val resultUri = result.outputData.getString(keyImageUri)
            assertTrue(result is ListenableWorker.Result.Success)
            assertTrue(result.outputData.keyValueMap.containsKey(keyImageUri))
            assertTrue(
                resultUri?.startsWith("content://media/external/images/media/")
                    ?: false
            )
        }
    }
}