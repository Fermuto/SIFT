package com.example.sift

import android.graphics.Bitmap
import android.graphics.Bitmap.*
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlin.collections.*
import kotlin.math.*

class Processing : AppCompatActivity() {
    var conf = Config.ARGB_8888
    var bitmap: Bitmap = createBitmap(640, 480, conf)

    var Width: Int = 0
    var Height: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_processing)

        val extras = intent.extras

        bitmap = BitmapFactory.decodeStream(this@Processing.openFileInput("myImage"))
        Height = extras!!.getInt("Height")
        Width = extras!!.getInt("Width")
    }


    fun gengaussian_kernel(size: Int, sigma: Int): Array<Array<Int>>? {
        var kernel: Array<Array<Int>> = arrayOf()
        var kernelsum: Int = 0
        if (((size % 2) == 0) or (size < 2)){
            return null
        }
        var k = (size - 1) / 2
        for (i in 0 until size){
            for (j in 0 until size){
                var x: Double = i - (size - 1) / 2.0
                var y: Double = j - (size - 1) / 2.0
                kernel[i][j] = exp((-1) * ((x.pow(2)) + (y.pow(2))) / ((2.0 * sigma).pow(2))).toInt()
                kernelsum += kernel[i][j]
            }
        }
        for (i in 0 until size){
            for (j in 0 until size){
                kernel[i][j] = kernel[i][j] / kernelsum
            }
        }
        return kernel
    }

    fun bilin_interpolation(top_left: Float, bot_left: Float, top_right: Float, bot_right: Float, dis_x: Float, dis_y: Float): Float? {
        var left_interpol = (dis_y * top_left) + ((1 - dis_y) * bot_left)
        var right_interpol = (dis_y * top_right) + ((1 - dis_y) * bot_right)
        var bilin_interpol = (dis_x * left_interpol) + ((1 - dis_x) * right_interpol)
        return bilin_interpol
    }

    fun sRGBtoLinear(x_in: Double): Double {
        if (x_in < 0.04045){
            return x_in / 12.92
        }
        return ((x_in + 0.055) / 1.055).pow(2.4)
    }

    fun LineartosRGB(y_in: Double): Double {
        if (y_in <= 0.0031308){
            return 12.92 * y_in
        }
        return (1.055 * y_in.pow(1/2.4)) - 0.055
    }

    fun RGBtoGreyscale(Image: Bitmap): Array<Array<Int>> {

        var reds: Array<Array<Int>> = arrayOf()
        var greens: Array<Array<Int>> = arrayOf()
        var blues: Array<Array<Int>> = arrayOf()

        var reds_linear: Array<Array<Double>> = arrayOf()
        var greens_linear: Array<Array<Double>> = arrayOf()
        var blues_linear: Array<Array<Double>> = arrayOf()

        var gray_linear: Array<Array<Double>> = arrayOf()

        var gray_color: Array<Array<Int>> = arrayOf()

        var colour: Int = 0
        for (r in 0 until Height){
            for (c in 0 until Width){
                colour = Image.getPixel(c, r)
                reds[r][c] = Color.red(colour)
                greens[r][c] = Color.green(colour)
                blues[r][c] = Color.blue(colour)
            }
        }

        for (j in 0 until (Width - 1)){
            for (k in 0 until (Height - 1)){
                reds_linear[j][k] = sRGBtoLinear(reds[j][k] / 255.0)
                greens_linear[j][k] = sRGBtoLinear(greens[j][k] / 255.0)
                blues_linear[j][k] = sRGBtoLinear(blues[j][k] / 255.0)

                gray_linear[j][k] = (0.2126 * reds_linear[j][k]) + (0.7152 * greens_linear[j][k]) + (0.0722 * blues_linear[j][k])
                gray_color[j][k] = (LineartosRGB(gray_linear[j][k] * 255.0)).toInt()
            }
        }
        return gray_color
    }

    fun revkernel(kernel: Array<Array<Int>>): Array<Array<Int>>{
        var new_kernel: Array<Array<Int>> = arrayOf()
        val fun_width = kernel[0].size
        for (i in 0 until fun_width){
            for (j in 0 until fun_width){

            }
        }
    }
    fun conv2(pic: Array<Array<Int>>, kernel: Array<Array<Int>>): Array<Array<Int>> {
        var pic_conv: Array<Array<Int>> = arrayOf()

    }

    //    fun dilate3D(img: Bitmap, x: Int, y: Int): {
//
//    }
}