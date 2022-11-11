package com.example.sift

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class Processing : AppCompatActivity() {

    val Planes = null
    val Height = null
    val Width = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_processing)

//        val extras = intent.extras
//        if (extras != null) {
//            val Planes = extras.getDoubleArray("Capture")
//            val Height = extras.getInt("Height")
//            val Width = extras.getInt("Width")
//            //The key argument here must match that used in the other activity
//        }
    }


    private fun cmykToRgb(c: Int, m: Int, y: Int, k: Int): IntArray {
        val r = 255 * (1 - c / 100) * (1 - k / 100)
        val g = 255 * (1 - m / 100) * (1 - k / 100)
        val b = 255 * (1 - y / 100) * (1 - k / 100)
        return intArrayOf(r, g, b)
    }
//    object CmykRgbConverter { // just prints it
//        @JvmStatic
//        fun main(args: Array<String>) {
//            val rgb = cmykToRgb(args[0].toInt(), args[1].toInt(), args[2].toInt(), args[3].toInt())
//            System.out.println(Arrays.toString(rgb))
//        }
//    }
}