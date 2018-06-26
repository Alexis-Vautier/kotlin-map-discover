package com.locationspaces.alexis.locationplaces.tools

import android.content.res.Resources
import android.graphics.*
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory

 class CircleDrawer {

    fun convertBitmapToCircle(bitmap: Bitmap, resources: Resources): Bitmap {
        val roundDrawable = RoundedBitmapDrawableFactory.create(resources, bitmap)
        roundDrawable.isCircular = true

        val output = Bitmap.createBitmap(roundDrawable.bitmap!!.width, roundDrawable.bitmap!!.height!!, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint()
        val rect = Rect(0,0,bitmap.width, bitmap.height)
        canvas.drawARGB(0,0,0,0)
        canvas.drawCircle((bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat(), (bitmap.width / 2).toFloat(), paint)
        paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
        canvas.drawBitmap(bitmap,rect,rect,paint)
        return output
    }
}