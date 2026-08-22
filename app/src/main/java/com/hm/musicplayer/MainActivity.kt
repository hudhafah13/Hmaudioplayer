package com.hm.musicplayer

import android.Manifest
import android.content.ComponentName
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

data class Song(val title: String, val artist: String, val album: String, val uri: android.net.Uri)

class MainActivity : ComponentActivity() {
    private val songs = mutableListOf<Song>()
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private lateinit var adapter: ArrayAdapter<String>
    private val filtered = mutableListOf<Song>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        controllerFuture = MediaController.Builder(this, SessionToken(this, ComponentName(this, PlaybackService::class.java))).buildAsync()

        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) loadSongs()
        else ActivityCompat.requestPermissions(this, arrayOf(permission), 42)

        val list = findViewById<ListView>(R.id.songList)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        list.adapter = adapter

        findViewById<EditText>(R.id.search).setOnEditorActionListener { v, _, _ -> filterSongs(v.text.toString()); false }
        findViewById<View>(R.id.search).setOnKeyListener { _, _, _ -> false }

        findViewById<View>(R.id.loading).postDelayed({
            findViewById<View>(R.id.loading).animate().alpha(0f).setDuration(500).withEndAction {
                findViewById<View>(R.id.loading).visibility = View.GONE
            }
        }, 1200)

        list.setOnItemClickListener { _, _, position, _ ->
            if (!controllerFuture.isDone || position >= filtered.size) return@setOnItemClickListener
            val controller = controllerFuture.get()
            val items = songs.map {
                MediaItem.Builder().setUri(it.uri).setMediaMetadata(
                    MediaMetadata.Builder().setTitle(it.title).setArtist(it.artist).setAlbumTitle(it.album).build()
                ).build()
            }
            val selected = songs.indexOf(filtered[position]).coerceAtLeast(0)
            controller.setMediaItems(items, selected, 0L)
            controller.prepare()
            controller.play()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 42 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) loadSongs()
    }

    private fun loadSongs() {
        songs.clear()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM)
        contentResolver.query(collection, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            while (cursor.moveToNext()) {
                val mediaId = cursor.getLong(id)
                songs += Song(
                    cursor.getString(title).takeUnless { it.isNullOrBlank() } ?: "Unknown title",
                    cursor.getString(artist).takeUnless { it.isNullOrBlank() || it == "<unknown>" } ?: "Unknown artist",
                    cursor.getString(album).takeUnless { it.isNullOrBlank() || it == "<unknown>" } ?: "Unknown album",
                    ContentUris.withAppendedId(collection, mediaId)
                )
            }
        }
        filterSongs("")
    }

    private fun filterSongs(query: String) {
        filtered.clear()
        val q = query.trim().lowercase()
        filtered += songs.filter { q.isEmpty() || it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q) }
        adapter.clear()
        adapter.addAll(filtered.map { "${it.title}\n${it.artist} • ${it.album}" })
        adapter.notifyDataSetChanged()
    }
}
