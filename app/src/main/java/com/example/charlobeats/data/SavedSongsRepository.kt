package com.example.charlobeats.data

import android.content.Context
import android.media.MediaMetadataRetriever
import com.example.charlobeats.audio.model.SavedSongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SavedSongsRepository(private val context: Context) {

    private val metadataFile = File(context.filesDir, "saved_songs_meta.json")
    private val exportsDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "BoomBoomExports").apply {
        mkdirs()
    }

    private val _savedSongs = MutableStateFlow<List<SavedSongItem>>(emptyList())
    val savedSongs: StateFlow<List<SavedSongItem>> = _savedSongs.asStateFlow()

    init {
        loadSavedSongs()
    }

    fun loadSavedSongs() {
        val items = mutableListOf<SavedSongItem>()
        val savedMetaMap = mutableMapOf<String, JSONObject>()

        if (metadataFile.exists()) {
            try {
                val jsonStr = metadataFile.readText()
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val path = obj.optString("filePath")
                    if (path.isNotEmpty()) {
                        savedMetaMap[path] = obj
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val files = exportsDir.listFiles()?.filter {
            it.isFile && (it.extension.equals("mp3", true) || it.extension.equals("wav", true) || it.extension.equals("flac", true))
        } ?: emptyList()

        for (file in files) {
            val meta = savedMetaMap[file.absolutePath]
            val title = meta?.optString("title") ?: file.nameWithoutExtension.replace(Regex("_\\d+$"), "")
            val preset = meta?.optString("appliedPreset") ?: "Enhanced"
            val format = meta?.optString("format") ?: file.extension.uppercase()
            val id = meta?.optString("id") ?: file.name
            val createdAt = meta?.optLong("createdAt") ?: file.lastModified()
            val durationMs = meta?.optLong("durationMs") ?: getDurationFallback(file)

            items.add(
                SavedSongItem(
                    id = id,
                    title = title,
                    artist = "Boom-Boom Audio",
                    filePath = file.absolutePath,
                    durationMs = durationMs,
                    fileSizeBytes = file.length(),
                    format = format,
                    appliedPreset = preset,
                    createdAt = createdAt
                )
            )
        }

        // Sort by newest first
        _savedSongs.value = items.sortedByDescending { it.createdAt }
    }

    suspend fun saveSongRecord(item: SavedSongItem) = withContext(Dispatchers.IO) {
        val currentList = _savedSongs.value.toMutableList()
        currentList.removeAll { it.filePath == item.filePath }
        currentList.add(0, item)
        _savedSongs.value = currentList
        persistMetadata(currentList)
    }

    suspend fun deleteSong(item: SavedSongItem) = withContext(Dispatchers.IO) {
        val file = File(item.filePath)
        if (file.exists()) {
            file.delete()
        }
        val currentList = _savedSongs.value.toMutableList()
        currentList.removeAll { it.id == item.id || it.filePath == item.filePath }
        _savedSongs.value = currentList
        persistMetadata(currentList)
    }

    private fun persistMetadata(items: List<SavedSongItem>) {
        try {
            val array = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("artist", item.artist)
                    put("filePath", item.filePath)
                    put("durationMs", item.durationMs)
                    put("fileSizeBytes", item.fileSizeBytes)
                    put("format", item.format)
                    put("appliedPreset", item.appliedPreset)
                    put("createdAt", item.createdAt)
                }
                array.put(obj)
            }
            metadataFile.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getDurationFallback(file: File): Long {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(file.absolutePath)
            val dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 180000L
            mmr.release()
            dur
        } catch (e: Exception) {
            180000L
        }
    }
}
