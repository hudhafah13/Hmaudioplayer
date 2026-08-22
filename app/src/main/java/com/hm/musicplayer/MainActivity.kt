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
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

 data class Song(val title:String,val artist:String,val album:String,val uri:android.net.Uri)

class MainActivity:ComponentActivity(){
 private val songs=mutableListOf<Song>(); private val filtered=mutableListOf<Song>()
 private lateinit var controllerFuture:ListenableFuture<MediaController>; private lateinit var adapter:ArrayAdapter<String>
 private lateinit var search:EditText; private lateinit var library:TextView; private lateinit var now:TextView
 private var screen="HOME"
 override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main)
  controllerFuture=MediaController.Builder(this,SessionToken(this,ComponentName(this,PlaybackService::class.java))).buildAsync()
  search=findViewById(R.id.search);library=findViewById(R.id.libraryTitle);now=findViewById(R.id.nowTitle)
  val list=findViewById<ListView>(R.id.songList);adapter=ArrayAdapter(this,android.R.layout.simple_list_item_1,mutableListOf());list.adapter=adapter
  val p=if(Build.VERSION.SDK_INT>=33)Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
  if(ContextCompat.checkSelfPermission(this,p)==PackageManager.PERMISSION_GRANTED)loadSongs() else ActivityCompat.requestPermissions(this,arrayOf(p),42)
  search.setOnEditorActionListener{v,_,_->filterSongs(v.text.toString());false}
  findViewById<TextView>(R.id.homeTab).setOnClickListener{showScreen("HOME")};findViewById<TextView>(R.id.favTab).setOnClickListener{showScreen("FAVORITES")};findViewById<TextView>(R.id.playlistTab).setOnClickListener{showScreen("PLAYLISTS")};findViewById<TextView>(R.id.settingsTab).setOnClickListener{showScreen("SETTINGS")}
  findViewById<TextView>(R.id.rescan).setOnClickListener{loadSongs()}
  list.setOnItemClickListener{_,_,pos,_->if(controllerFuture.isDone&&pos<filtered.size){val c=controllerFuture.get();val items=songs.map{MediaItem.Builder().setUri(it.uri).setMediaMetadata(MediaMetadata.Builder().setTitle(it.title).setArtist(it.artist).setAlbumTitle(it.album).build()).build()};val i=songs.indexOf(filtered[pos]);c.setMediaItems(items,i,0);c.prepare();c.play();now.text="NOW PLAYING  •  ${filtered[pos].title}"}}
  findViewById<View>(R.id.loading).postDelayed({findViewById<View>(R.id.loading).animate().alpha(0f).setDuration(450).withEndAction{findViewById<View>(R.id.loading).visibility=View.GONE}},1100)
 }
 private fun showScreen(s:String){screen=s;library.text=when(s){"FAVORITES"->"♡ FAVORITES";"PLAYLISTS"->"☰ PLAYLISTS";"SETTINGS"->"⚙ SETTINGS";else->"LIBRARY"};if(s=="SETTINGS")adapter.clear() else filterSongs(search.text.toString())}
 private fun loadSongs(){songs.clear();val c=MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;val pr=arrayOf(MediaStore.Audio.Media._ID,MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.ALBUM);contentResolver.query(c,pr,"${MediaStore.Audio.Media.IS_MUSIC} != 0",null,"${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use{x->val id=x.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);val t=x.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);val a=x.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);val al=x.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);while(x.moveToNext()){songs+=Song(x.getString(t).takeUnless{it.isNullOrBlank()}?:"Unknown title",x.getString(a).takeUnless{it.isNullOrBlank()||it=="<unknown>"}?:"Unknown artist",x.getString(al).takeUnless{it.isNullOrBlank()||it=="<unknown>"}?:"Unknown album",ContentUris.withAppendedId(c,x.getLong(id)))}};showScreen("HOME")}
 private fun filterSongs(q:String){if(screen!="HOME")return;filtered.clear();val z=q.trim().lowercase();filtered+=songs.filter{z.isEmpty()||it.title.lowercase().contains(z)||it.artist.lowercase().contains(z)||it.album.lowercase().contains(z)};adapter.clear();adapter.addAll(filtered.map{"${it.title}\n${it.artist} • ${it.album}"});adapter.notifyDataSetChanged()}
 override fun onRequestPermissionsResult(r:Int,p:Array<String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==42&&g.firstOrNull()==PackageManager.PERMISSION_GRANTED)loadSongs()}
}
