package com.example.sift

import android.graphics.*
import android.graphics.Bitmap.*
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.sift.databinding.ActivityProcessingBinding
import kotlin.collections.*
import kotlin.math.*


class Processing : AppCompatActivity() {
    var conf = Config.ARGB_8888
    var Width: Int = 0
    var Height: Int = 0

    private lateinit var viewBinding: ActivityProcessingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityProcessingBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
//        setContentView(R.layout.activity_processing)

        val extras = intent.extras
        Height = extras!!.getInt("Height")
        Width = extras!!.getInt("Width")

        var bitmap = BitmapFactory.decodeStream(this@Processing.openFileInput("myImage"))


        /********************************************************************************************/
        // CANNY EDGE DETECTOR START
        /********************************************************************************************/

        var greyscale = RGBtoGreyscale(bitmap)
        Log.e("bitmap height :", bitmap.height.toString())
        Log.e("bitmap width :", bitmap.width.toString())

//        for (j in 0 until 640){
//            for (k in 0 until 480){
//                var colour = bitmap.getPixel(j, k)
//                Log.e("j: ", j.toString())
//                Log.e("k: ", k.toString())
//                Log.e("RED: ", Color.red(colour).toString())
//                Log.e("GRN: ", Color.green(colour).toString())
//                Log.e("BLU: ", Color.blue(colour).toString())
//                Log.e("ALPHA: ", Color.alpha(colour).toString())
//            }
//        }
//         for (j in 0 until 640) {
//            for (k in 0 until 480){
//                Log.e("Greyscale :", greyscale[j][k].toString())
//                k++
//            }
//            j++
//        }

        var kernel = gengaussian_kernel(5, 2)
        var conv = conv2(greyscale, kernel)
        var sobel_x: Array<Array<Int>> = arrayOf(arrayOf(1, 0, -1), arrayOf(2, 0, -2), arrayOf(1, 0, -1))
        var sobel_y: Array<Array<Int>> = arrayOf(arrayOf(1, 2, 1), arrayOf(0, 0, 0), arrayOf(-1, -2, -1))
        var grad_x = conv2(conv, sobel_x)
        var grad_y = conv2(conv, sobel_y)

        var vals: Array<Array<Array<Double>>> = Array(grad_x.size, { Array(grad_x[0].size, { Array(3, {0.0}) } ) } )

        for (y in 0 until greyscale.size){
            for (x in 0 until greyscale[0].size){
                vals[y][x][0] = sqrt(grad_x[y][x].toDouble().pow(2) + grad_y[y][x].toDouble().pow(2))
                var gradient_orientation = atan2(grad_y[y][x].toDouble(), grad_x[y][x].toDouble())
                if (gradient_orientation < 0){
                    vals[y][x][1] = 360 + Math.toDegrees(gradient_orientation)
                }
                else{
                    vals[y][x][1] = Math.toDegrees(gradient_orientation)
                }
            }

        }

        var find_max = 0.0
        var x_max = 0
        var y_max = 0
        for (y in 0 until greyscale.size) {
            for (x in 0 until greyscale[0].size) {
                if (vals[y][x][0] > find_max){
                    x_max = x
                    y_max = y
                    find_max = vals[y][x][0]
                }
            }
        }

        var new_vals: Array<Array<Double>> = Array(vals.size) {Array(vals[0].size) {0.0} }
        for (y in 0 until vals.size) {
            for (x in 0 until vals[0].size) {
                var magnitude = vals[y][x][0]
                if (magnitude > 1){
                    var degree = vals[y][x][1]
                    if (!(cos(Math.toRadians(degree)) % 1.0 == 0.0) && !(sin(Math.toRadians(degree)) % 1.0 == 0.0)){
                        var x_1 = x + cos(Math.toRadians(degree))
                        var y_1 = y + sin(Math.toRadians(degree))
                        var x_2 = x - cos(Math.toRadians(degree))
                        var y_2 = y - sin(Math.toRadians(degree))

                        //First interpolation
                        var x_n1 = x_1.toInt()
                        var x_n2 = x_n1  + 1
                        var y_n1 = y_1.toInt()
                        var y_n2 = y_n1 + 1

                        var x_ratio = x_1 - x_n1
                        var y_ratio = y_1 - y_n1

                        var compare_1 = 0.0
                        var compare_2 = 0.0

                        if ((x_n1 > 0) && (y_n1 > 0) && (y_n2 < greyscale.size) && (x_n2 < greyscale[0].size)){
                            var top_left = vals[y_n1][x_n1][0]
                            var bot_left = vals[y_n2][x_n1][0]

                            var top_right = vals[y_n1][x_n2][0]
                            var bot_right = vals[y_n2][x_n2][0]

                            compare_1 = bilin_interpolation(top_left, bot_left, top_right, bot_right, x_ratio, y_ratio)
                        }

                        //Second interpolation
                        var x_p1 = x_2.toInt()
                        var x_p2 = x_p1 + 1
                        var y_p1 = y_2.toInt()
                        var y_p2 = y_p1 + 1

                        var xp_ratio = x_2 - x_n2
                        var yp_ratio = y_2 - y_n2
                        if ((x_p1 > 0) && (y_p1 > 0) && (y_p2 < greyscale.size) && (x_p2 < greyscale[0].size)){
                            var top_left = vals[y_p1][x_p1][0]
                            var bot_left = vals[y_p2][x_p1][0]

                            var top_right = vals[y_p1][x_p2][0]
                            var bot_right = vals[y_p2][x_p2][0]

                            compare_2 = bilin_interpolation(top_left, bot_left, top_right, bot_right, x_ratio, y_ratio)
                        }
                        if ((vals[y][x][0] > compare_1) && (vals[y][x][0] > compare_2)){
                            new_vals[y][x] = vals[y][x][0]
                        }

                    }
                    else{ //want to compare vertically
                        var direction = cos(Math.toRadians(degree)).toInt()
                        if (direction == 1){
                            if (((y - 1) > 0) && ((y + 1) < greyscale.size)){
                                var compare_1 = vals[y-1][x][0]
                                var compare_2 = vals[y+1][x][0]
                                if ((vals[y][x][0] >= compare_1) && (vals[y][x][0] >= compare_2)){
                                    new_vals[y][x] = vals[y][x][0]
                                }
                            }
                        }
                        else if (direction == 0){
                            if ((x - 1 > 0) && (x + 1 < greyscale[0].size)){
                                var compare_1 = vals[y][x-1][0]
                                var compare_2 = vals[y][x+1][0]
                                if ((vals[y][x][0] >= compare_1) && (vals[y][x][0] >= compare_2)){
                                    new_vals[y][x] = vals[y][x][0]
                                }
                            }
                        }
                    }
                }
            }
        }

        val strong_thres = 0.35 * find_max
        val weak_thres = 0.15 * find_max
        var thresholded: Array<Array<Int>> = Array(new_vals.size) {Array(new_vals[0].size) {0} }

        for (y in 0 until new_vals.size){
            for (x in 0 until new_vals[0].size){
                if (new_vals[y][x] > strong_thres){
                    thresholded[y][x] = 255
                }
                else if(new_vals[y][x] > weak_thres){
                    thresholded[y][x] = 100
                }
            }
        }

        var dst: Array<Array<Int>> = Array(thresholded.size) {Array(thresholded[0].size) {0} }

        for (y in 0 until thresholded.size){
            for (x in 0 until thresholded[0].size){
                if ((x - 1 >= 0) && (x + 1 < thresholded[0].size) && (y - 1 >= 0 ) && (y + 1 < thresholded.size)){
                    var sub_img: Array<Array<Int>> = arrayOf(arrayOf(thresholded[y-1][x-1],thresholded[y-1][x],thresholded[y-1][x+1]),
                        arrayOf(thresholded[y][x-1],thresholded[y][x],thresholded[y][x+1]),
                        arrayOf(thresholded[y+1][x-1],thresholded[y+1][x],thresholded[y+1][x+1]))
                    if (thresholded[y][x] == 100){
                        if ((sub_img[0][0] == 255) or (sub_img[0][1] == 255) or (sub_img[0][2] == 255)
                            or (sub_img[1][0] == 255) or (sub_img[1][1] == 255) or (sub_img[1][2] == 255)
                            or (sub_img[2][0] == 255) or (sub_img[2][1] == 255) or (sub_img[2][2] == 255)){
                            dst[y][x] = 255
                        }
                    }
                    else if (thresholded[y][x] == 255){
                        dst[y][x] = 255
                    }
                    else{
                        dst[y][x] = 0
                    }
                }
            }
        }

        val display = createBitmap(dst.size, dst[0].size, Config.ARGB_8888)
        for (y in 0 until dst.size){
            for (x in 0 until dst[0].size){
                display.setPixel(y, x, Color.argb(255, dst[y][x], dst[y][x], dst[y][x]))
            }
        }

        viewBinding.productDisplay.setImageBitmap(display)

        /********************************************************************************************/
        // CANNY EDGE DETECTOR END
        /********************************************************************************************/

    }


    fun gengaussian_kernel(size: Int, sigma: Int): Array<Array<Int>> {
        var kernel: Array<Array<Int>> = Array(size) {Array(size) {0} }
        var kernelsum: Int = 0
//        if (((size % 2) == 0) or (size < 2)){
//            return null
//        }
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

    fun bilin_interpolation(top_left: Double, bot_left: Double, top_right: Double, bot_right: Double, dis_x: Double, dis_y: Double): Double {
        var left_interpol = (dis_y * top_left) + ((1 - dis_y) * bot_left)
        var right_interpol = (dis_y * top_right) + ((1 - dis_y) * bot_right)
        var bilin_interpol = (dis_x * left_interpol) + ((1.0 - dis_x) * right_interpol)
        return bilin_interpol
    }

//    fun sRGBtoLinear(x_in: Double): Double {
//        if (x_in < 0.04045){
//            return x_in / 12.92
//        }
//        return ((x_in + 0.055) / 1.055).pow(2.4)
//    }
//
//    fun LineartosRGB(y_in: Double): Double {
//        if (y_in <= 0.0031308){
//            return 12.92 * y_in
//        }
//        return (1.055 * y_in.pow(1/2.4)) - 0.055
//    }

//    fun RGBtoGreyscale(Image: Bitmap): Array<Array<Int>> { // row and col REVERSED compared to python which is itself REVERSED
//
//        var reds: Array<Array<Int>> = Array(Height) {Array(Width) {0} }
//        var greens: Array<Array<Int>> = Array(Height) {Array(Width) {0} }
//        var blues: Array<Array<Int>> = Array(Height) {Array(Width) {0} }
//
//        var reds_linear: Array<Array<Double>> = Array(Height) {Array(Width) {0.0} }
//        var greens_linear: Array<Array<Double>> = Array(Height) {Array(Width) {0.0} }
//        var blues_linear: Array<Array<Double>> = Array(Height) {Array(Width) {0.0} }
//
//        var gray_linear: Array<Array<Double>> = Array(Height) {Array(Width) {0.0} }
//
//        var gray_color: Array<Array<Int>> = Array(Height) {Array(Width) {0} }
//
//        var colour: Int = 0
//        for (r in 0 until Height - 1){
//            for (c in 0 until Width - 1){
//                colour = Image.getPixel(c, r)
//                reds[r][c] = Color.red(colour)
//                greens[r][c] = Color.green(colour)
//                blues[r][c] = Color.blue(colour)
//            }
//        }
//
//        for (j in 0 until (Height - 1)){
//            for (k in 0 until (Width - 1)){
//                reds_linear[j][k] = sRGBtoLinear(reds[j][k] / 255.0)
//                greens_linear[j][k] = sRGBtoLinear(greens[j][k] / 255.0)
//                blues_linear[j][k] = sRGBtoLinear(blues[j][k] / 255.0)
//                if (j < 100){
//                    if (k < 100){
//                        Log.e("REDS_LINEAR")
//                    }
//                }
//
//                gray_linear[j][k] = (0.2126 * reds_linear[j][k]) + (0.7152 * greens_linear[j][k]) + (0.0722 * blues_linear[j][k])
//                gray_color[j][k] = (LineartosRGB(gray_linear[j][k] * 255.0)).toInt()
//            }
//        }
//        return gray_color
//    }
    fun RGBtoGreyscale(Image: Bitmap): Array<Array<Int>> {
        var reds: Array<Array<Int>> = Array(Height) {Array(Width) {0} }
        var greens: Array<Array<Int>> = Array(Height) {Array(Width) {0} }
        var blues: Array<Array<Int>> = Array(Height) {Array(Width) {0} }
        var gray_color: Array<Array<Int>> = Array(Height) {Array(Width) {0} }
        var colour: Int = 0
        for (r in 0 until Height - 1){
            for (c in 0 until Width - 1){
                colour = Image.getPixel(c, r)
                reds[r][c] = Color.red(colour)
                greens[r][c] = Color.green(colour)
                blues[r][c] = Color.blue(colour)
            }
        }
        for (j in 0 until (Height - 1)){
            for (k in 0 until (Width - 1)){
                gray_color[j][k] = ((0.2126 * reds[j][k]) + (0.7152 * greens[j][k]) + (0.0722 * blues[j][k])).toInt()
            }
        }
        return gray_color
    }

//    fun revkernel(kernel: Array<Array<Int>>): Array<Array<Int>>{
//        var new_kernel: Array<Array<Int>> = kernel
//        var fun_width = kernel[0].size
//
//        for (i in 0 until fun_width) {
//            for (j in 0 until fun_width) {
//                new_kernel[i][fun_width - 1 - j] = kernel[i][j]
//            }
//        }
//        var temp = new_kernel
//        for (i in 0 until fun_width) {
//            for (j in 0 until fun_width) {
//                new_kernel[fun_width - 1 - i][j] = temp[i][j]
//            }
//        }
//        return new_kernel
//    }

//    fun conv2(pic: Array<Array<Int>>, kernel: Array<Array<Int>>): Array<Array<Int>> { // reversal from python kept because it's too much of a headache to change
//        var pic_conv: Array<Array<Int>> = Array(pic.size) {Array(pic[0].size) {0} }
//        var kernel_rev = revkernel(kernel)
//        val fun_width = pic.size
//        val fun_length = pic[0].size
//        val w_k = kernel_rev.size
//        val w_l = kernel_rev[0].size
//        for (i in 0 until fun_width){
//            for (j in 0 until fun_length){
//                val start_pnt_x = i - (w_k / 2)
//                val start_pnt_y = j - (w_l / 2)
//                for (k in 0 until w_k){
//                    for (l in 0 until w_l){
//                        if ((((start_pnt_x + k )< 0) or ((start_pnt_y + l) < 0)) or (((start_pnt_x + k) > fun_width) or ((start_pnt_y + l) > fun_length))){
//                            pic_conv[i][j] += 0
//                        }
//                        else{
//                            Log.e("PICCONV[I][J]", i.toString())
//                            pic_conv[i][j] += pic[start_pnt_x + k][start_pnt_y + l] * kernel_rev[k][l]
//
//                        }
//                    }
//                }
//            }
//        }
//        return pic_conv
    open fun conv2(data: Array<Array<Int>>, kernel: Array<Array<Int>>): Array<Array<Int>> {
        // 0 is black and 255 is white.
        val size = Height * Width
        var convData = Array(Height) {Array(Width) {0} }

        // Perform single channel 2D Convolution
        // Note that you only need to manipulate data[0:size] that corresponds to luminance
        // The rest data[size:data.length] is ignored since we only want grayscale output
        // ** START YOUR CODE HERE  ** //
//        var revkernel = arrayOfNulls<Int>(kernel.size)
        val revkernel = Array(kernel.size) {Array(kernel[0].size) {0} }
        for (i in kernel.indices) {
            revkernel[kernel.size - 1 - i] = kernel[i]
        }
        val w_k: Int = kernel[0].size
        val h_k: Int = kernel[0].size
        var start_pnt_x = 0
        var start_pnt_y = 0
        for (i in 0 until Height) {
            for (j in 0 until Width) {
                start_pnt_x = j - (w_k / 2)
                start_pnt_y = i - (h_k / 2)
                for (k in 0 until h_k) {
                    for (l in 0 until w_k) {
                        if (start_pnt_x + l < 0 || start_pnt_y + k < 0 || start_pnt_y + k >= Height || start_pnt_x + l >= Width || i * Width + j >= 307200) {
                            convData[0][0] += 0
                        } else {
                            convData[i][j] += ((data[start_pnt_y + k][start_pnt_x + l]) * (revkernel[k][l])).toInt()
                        }
                    }
                }
            }
        }
        return convData
    }

    //    fun dilate3D(img: Bitmap, x: Int, y: Int): {
//
//    }
}