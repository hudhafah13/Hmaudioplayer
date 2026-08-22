package com.hm.musicplayer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class VisualizerView @JvmOverloads constructor(c:Context,a:AttributeSet?=null):View(c,a){
 private val p=Paint(Paint.ANTI_ALIAS_FLAG);private var phase=0f
 init{p.strokeWidth=8f;p.strokeCap=Paint.Cap.ROUND}
 override fun onDraw(c:Canvas){super.onDraw(c);val n=24;val w=width.toFloat()/n;val mid=height/2f;phase+=0.10f;for(i in 0 until n){val wave=(sin(phase+i*.55f)+1f)/2f;val h=10f+wave*(height*.78f);c.drawLine(i*w+w/2,mid-h/2,i*w+w/2,mid+h/2,p)}postInvalidateDelayed(32)}
}
