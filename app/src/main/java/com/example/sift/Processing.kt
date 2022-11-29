package com.example.sift

import android.graphics.*
import android.graphics.Bitmap.*
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.sift.databinding.ActivityProcessingBinding
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.default.math.*
import org.jetbrains.kotlinx.multik.default.math.DefaultMath.argMin
import org.jetbrains.kotlinx.multik.ndarray.data.set
import java.util.*
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
        Width = extras.getInt("Width")

        var bitmap = BitmapFactory.decodeStream(this@Processing.openFileInput("myImage"))


        /********************************************************************************************/
        // CANNY EDGE DETECTOR START
        /********************************************************************************************/

        var greyscale = RGBtoGreyscale(bitmap)
        Log.e("[INFO]", "Bitmap Height:"+bitmap.height.toString())
        Log.e("[INFO]", "Bitmap Width:"+bitmap.width.toString())

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
        Log.e("[STATUS]", "Starting blurring convolution")
        var conv = conv2(greyscale, kernel)
        Log.e("[STATUS]", "Ending blurring convolution")
        var sobel_x: Array<Array<Int>> = arrayOf(arrayOf(1, 0, -1), arrayOf(2, 0, -2), arrayOf(1, 0, -1))
        var sobel_y: Array<Array<Int>> = arrayOf(arrayOf(1, 2, 1), arrayOf(0, 0, 0), arrayOf(-1, -2, -1))
        Log.e("[STATUS]", "Starting gradient convolution")
        var grad_x = conv2(conv, sobel_x)
        var grad_y = conv2(conv, sobel_y)
        Log.e("[STATUS]", "Ending gradient convolution")
        var vals: Array<Array<Array<Double>>> = Array(grad_x.size, { Array(grad_x[0].size, { Array(3, {0.0}) } ) } )

        Log.e("[STATUS]", "Starting gradient and magnitude calculation")
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
        Log.e("[STATUS]", "Ending gradient and magnitude calculation")
        var find_max = 0.0
        var x_max = 0
        var y_max = 0
        Log.e("[STATUS]", "Starting find max")
        for (y in 0 until greyscale.size) {
            for (x in 0 until greyscale[0].size) {
                if (vals[y][x][0] > find_max){
                    x_max = x
                    y_max = y
                    find_max = vals[y][x][0]
                }
            }
        }
        Log.e("[STATUS]", "Ending find max")
        Log.e("[STATUS]", "Starting bilinear interpolation for edge detection")
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
        Log.e("[STATUS]", "Ending bilinear interpolation for edge detection")
        Log.e("[STATUS]", "Starting Thresholded Canny Edge Detection")
        val strong_thres = 0.25 * find_max
        val weak_thres = 0.1 * find_max
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
        Log.e("[STATUS]", "Ending Thresholded Canny Edge Detection")
        var dst: Array<Array<Int>> = Array(thresholded.size) {Array(thresholded[0].size) {0} }
        var num_dst = 0
        Log.e("[STATUS]", "Starting destination Canny Edge Detection")
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
                            num_dst = num_dst + 1
                        }
                    }
                    else if (thresholded[y][x] == 255){
                        dst[y][x] = 255
                        num_dst = num_dst + 1
                    }
                    else{
                        dst[y][x] = 0
                    }
                }
            }
        }
        Log.e("[INFO]", "Total number of non-zero values in destination: $num_dst")
        Log.e("[STATUS]", "Ending destination Canny Edge Detection")

        /********************************************************************************************/
        // CANNY EDGE DETECTOR END

        // GRADIENT INFORMED HOUGH TRANSFORM START
        /********************************************************************************************/
        Log.e("[STATUS]", "Starting gradient informed hough transfrom")
        var dst_grad: Array<Array<Double>> = Array(dst.size) {Array(dst[0].size) {0.0} }
        val num_rhos = 400
        val num_thetas = 400
        val diag_len = ceil(sqrt(((Width * Width) + (Height * Height)).toDouble()))
        Log.e("[INFO]", "Diagonal length of image is: $diag_len")
        val rho_granularity = (2 * diag_len) / num_rhos
        val theta_granularity = 180 / num_thetas

        var rhos = Array(num_rhos) {0.0}
        var thetas = Array(num_thetas) {0.0}
        rhos[0] = -diag_len
        thetas[0] = 0.0

        for (i in 1 until num_rhos){
            rhos[i] = (-diag_len) + (i * rho_granularity)
        }

        for (i in 1 until num_thetas){
            thetas[i] = (0.0) + (i * theta_granularity)
        }

        var cos_thetas = Array(num_thetas) {0.0}
        var sin_thetas = Array(num_thetas) {0.0}

        for (i in 0 until thetas.size){
            cos_thetas[i] = cos(Math.toRadians(thetas[i]))
        }

        for (i in 0 until thetas.size){
            sin_thetas[i] = sin(Math.toRadians(thetas[i]))
        }

        var accumulator_grad: Array<Array<Int>> = Array(rhos.size) {Array(thetas.size) {0} }
        var test = 0
        for (y in 0 until Height){
            for (x in 0 until Width){
                if (dst[y][x] == 255){
                    var theta_raw = vals[y][x][1]
                    if (test < 20) {
                        Log.e("[INFO]", "theta raw is: $theta_raw")
                        //test = test + 1
                    }
                    var i = cos(Math.toRadians(theta_raw))
                    var j = sin(Math.toRadians(theta_raw))
                    var rho_raw = (x * i) + (y * j)
                    if (theta_raw > 180){
                        theta_raw = theta_raw - 180
                        rho_raw = -rho_raw
                    }
                    if (test < 20) {
                        Log.e("[INFO]", "theta corrected is: $theta_raw")
                        //test = test + 1
                    }
                    dst_grad[y][x] = theta_raw
                    var rhos_minus_f = mk.zeros<Double>(rhos.size)
                    for (r in 0 until rhos.size){
                        rhos_minus_f[r] = abs(rhos[r] - rho_raw)
                    }
                    var thetas_minus_f = mk.zeros<Double>(thetas.size)
                    for (r in 0 until thetas.size){
                        thetas_minus_f[r] = abs(thetas[r] - theta_raw)
                    }
                    var rho_idx = argMin(rhos_minus_f)
                    var theta_idx = argMin(thetas_minus_f)
                    if (test < 20) {
                        Log.e("[INFO]", "theta index is: $theta_idx")
                        test = test + 1
                    }
                    accumulator_grad[rho_idx][theta_idx] += 1
                }
            }
        }
        //for (i in 0 until num_rhos) {
         //   Log.e("[INFO]", "accumulator_grad: " + Arrays.toString(accumulator_grad[i]))
        //}
        Log.e("[STATUS]", "Ending gradient informed hough transform")
        /********************************************************************************************/
        // GRADIENT INFORMED HOUGH TRANSFORM END
        // DEBUG NUM LINES (REMOVE WHEN DONE)
        var num_lines = 0
        num_lines = 0
        for (y in 0 until accumulator_grad.size){
            for (x in 0 until accumulator_grad[0].size){
                if (accumulator_grad[y][x] > 0){
                    num_lines += 1
                    accumulator_grad[y][x] = 255
                }
                else{
                    accumulator_grad[y][x] = 0
                }
            }
        }
        Log.e("[INFO]", "Number of lines in accume grad is: $num_lines")
        // EDGE SORTING START
        /********************************************************************************************/
        var workingBitmap: Bitmap? = createBitmap(bitmap)
        val mutableBitmap = workingBitmap!!.copy(Config.ARGB_8888, true)


        var canvas = Canvas(mutableBitmap)
        var paintline = Paint()
        paintline.strokeWidth = 1.0F

        for (y in 0 until accumulator_grad.size) {
            for (x in 0 until accumulator_grad[0].size) {
                if (accumulator_grad[y][x] > 0){
                    var rho = rhos[y]
                    var theta = thetas[x]
                    var a = cos(Math.toRadians(theta))
                    var b = sin(Math.toRadians(theta))
                    var x0 = (a * rho)
                    var y0 = (b * rho)
                    var x1 = (x0 + (diag_len * (-b))).toInt().toFloat()
                    var y1 = (y0 + (diag_len * (a))).toInt().toFloat()
                    var x2 = (x0 - (diag_len * (-b))).toInt().toFloat()
                    var y2 = (y0 - (diag_len * (a))).toInt().toFloat()
                    paintline.color = Color.GREEN
                    canvas.drawLine(x1, y1, x2, y2, paintline)

                }
            }
        }

        var threshold = 6
        Log.e("[STATUS]", "Starting hough transform trimming")
        for (y in 0 until accumulator_grad.size){
            for (x in 0 until accumulator_grad[0].size){
                if (accumulator_grad[y][x] < threshold){
                    accumulator_grad[y][x] = 0
                }
                else if (accumulator_grad[y][x] > threshold){
                    num_lines += 1
                }
            }
        }
        Log.e("[INFO]", "Number of lines detected post threshold is: $num_lines")
    /*
        var workingBitmap: Bitmap? = createBitmap(bitmap)
        val mutableBitmap = workingBitmap!!.copy(Config.ARGB_8888, true)


        var canvas = Canvas(mutableBitmap)
        var paintline = Paint()
        paintline.strokeWidth = 1.0F

        for (y in 0 until accumulator_grad.size) {
            for (x in 0 until accumulator_grad[0].size) {
                if (accumulator_grad[y][x] > 0){
                    var rho = rhos[y]
                    var theta = thetas[x]
                    var a = cos(Math.toRadians(theta))
                    var b = sin(Math.toRadians(theta))
                    var x0 = (a * rho)
                    var y0 = (b * rho)
                    var x1 = (x0 + (diag_len * (-b))).toInt().toFloat()
                    var y1 = (y0 + (diag_len * (a))).toInt().toFloat()
                    var x2 = (x0 - (diag_len * (-b))).toInt().toFloat()
                    var y2 = (y0 - (diag_len * (a))).toInt().toFloat()
                    paintline.color = Color.GREEN
                    canvas.drawLine(x1, y1, x2, y2, paintline)

                }
            }
        }
        */
        var accumed = Array(accumulator_grad.size) { Array(accumulator_grad[0].size) {0} }
        if (num_lines > 50){
            var accumedm = dilate(accumulator_grad, 5, 5)
            accumed = erosion(accumedm, 3, 3)
        }
        else{
            accumed = accumulator_grad
        }

        var accumed_suppressed = suppressor(accumed, 55, 55)

        num_lines = 0
        for (y in 0 until accumulator_grad.size){
            for (x in 0 until accumulator_grad[0].size){
                if (accumed_suppressed[y][x] > 0){
                    num_lines += 1
                    accumed_suppressed[y][x] = 255
                }
                else{
                    accumed_suppressed[y][x] = 0
                }
            }
        }

        accumed = accumed_suppressed
        if (num_lines < 15){
            accumed_suppressed = dilate(accumed, 3, 3)
        }

        // DEBUG NUM LINES (REMOVE WHEN DONE)
        num_lines = 0
        for (y in 0 until accumulator_grad.size){
            for (x in 0 until accumulator_grad[0].size){
                if (accumed_suppressed[y][x] > 0){
                    num_lines += 1
                    accumed_suppressed[y][x] = 255
                }
                else{
                    accumed_suppressed[y][x] = 0
                }
            }
        }
        Log.e("[INFO]", "Number of lines detected post trimming is: $num_lines")
        Log.e("[STATUS]", "Ending hough transform trimming")
        /*
        var workingBitmap: Bitmap? = createBitmap(bitmap)
        val mutableBitmap = workingBitmap!!.copy(Config.ARGB_8888, true)

        var canvas = Canvas(mutableBitmap)
        var paintline = Paint()
        paintline.strokeWidth = 1.0F

        for (y in 0 until accumed_suppressed.size) {
            for (x in 0 until accumed_suppressed[0].size) {
                if (accumed_suppressed[y][x] > 0){
                    var rho = rhos[y]
                    var theta = thetas[x]
                    var a = cos(Math.toRadians(theta))
                    var b = sin(Math.toRadians(theta))
                    var x0 = (a * rho)
                    var y0 = (b * rho)
                    var x1 = (x0 + (diag_len * (-b))).toInt().toFloat()
                    var y1 = (y0 + (diag_len * (a))).toInt().toFloat()
                    var x2 = (x0 - (diag_len * (-b))).toInt().toFloat()
                    var y2 = (y0 - (diag_len * (a))).toInt().toFloat()
                    paintline.color = Color.GREEN
                    canvas.drawLine(x1, y1, x2, y2, paintline)
                    Log.e("[INFO]", "M1 RED Line drawn at: ($x1, $y1) to ($x2, $y2)")
                }
            }
        }*/
        /*
        Log.e("[STATUS]", "Starting DBSCAN")
        var points = points(accumed_suppressed)

        var distance = Array(points.size) {Array(points.size) {0.0} }

        for (j in 0 until points.size){
            for (i in 0 until points.size){
                distance[j][i] = sqrt(((points[j].first - points[i].first).toDouble().pow(2)) + ((points[j].second - points[i].second).toDouble().pow(2)))
            }
        }

        var cluster = Array(points.size) {Array(6) {0} }
        for (i in 0 until points.size){
            cluster[i][0] = points[i].first
            cluster[i][1] = points[i].second
            cluster[i][2] = 0
            cluster[i][3] = -1
            cluster[i][4] = 0
            cluster[i][5] = 0
        }

        var current_clusters = 0
        threshold = 10
        var cp_thres = 0
        var max_points_near = 0
        var point_mat: MutableList<MutableList<Int>> = mutableListOf()
        for (i in 0 until points.size){
            point_mat.add(mutableListOf())
        }
        for (j in 0 until points.size){
            var points_near = 0
            for (i in 0 until points. size){
                if (i != j){
                    if (distance[j][i] < threshold){
                        points_near += 1
                        point_mat[j].add(i)
                    }
                }
            }
            cluster[j][4] = points_near
            if (points_near > cp_thres){
                cluster[j][2] = 1
                Log.e("[INFO]", "The number of points near " + points[j] + "is " + points_near +" IS CORE POINT")
            }
            if (points_near > max_points_near){
                max_points_near = points_near
                Log.e("[INFO]", "The number of points near " + points[j] + "is " + points_near)
            }
        }

        for (j in 0 until points.size){
            for (i in 0 until points.size){
                if (i != j){
                    if (distance[j][i] < threshold){
                        if (cluster[i][2] == 1){
                            cluster[j][5] += 1
                        }
                    }
                }
            }
        }

        var current_cluster = 0
        for (j in 0 until points.size){
            if (cluster[j][2] == 1){
                if (cluster[j][3] == -1){
                    start_cluster(j, cluster, point_mat, current_cluster)
                    current_cluster += 1
                }
            }
        }

        var num_clusters = current_cluster
        var cluster_center = Array(num_clusters) {Array(2) {0} }
        for (i in 0 until num_clusters){
            var x = 0
            var y = 0
            var total = 0
            for (j in 0 until points.size){
                if (cluster[j][3] == i){
                    y += cluster[j][0]
                    x += cluster[j][1]
                    total += 1
                }
            }
            cluster_center[i][0] = (y / total).toInt()
            cluster_center[i][1] = (x / total).toInt()

            Log.e("[INFO]", "Cluster $i center is: (" + cluster_center[i][0] + " " + cluster_center[i][1] + ") ")
        }
        Log.e("[INFO]", "Number of clusters is: $num_clusters")
        Log.e("[STATUS]", "Ending DBSCAN")
         */
        /********************************************************************************************/
        // DRAW LINES V1
        Log.e("[STATUS]", "Starting to draw lines")
        /*
        var workingBitmap: Bitmap? = createBitmap(bitmap)
        val mutableBitmap = workingBitmap!!.copy(Config.ARGB_8888, true)

        var canvas = Canvas(mutableBitmap)
        var paintline = Paint()
        paintline.strokeWidth = 2.0F

        var num_solo = 0
        for (x in 0 until cluster_center.size){
            var rho = rhos[cluster_center[x][0]]
            var theta = thetas[cluster_center[x][1]]
            var a = cos(Math.toRadians(theta))
            var b = sin(Math.toRadians(theta))
            var x0 = (a * rho)
            var y0 = (b * rho)
            var x1 = (x0 + (diag_len * (-b))).toInt().toFloat()
            var y1 = (y0 + (diag_len * (a))).toInt().toFloat()
            var x2 = (x0 - (diag_len * (-b))).toInt().toFloat()
            var y2 = (y0 - (diag_len * (a))).toInt().toFloat()
            paintline.color = Color.RED
            canvas.drawLine(x1, y1, x2, y2, paintline)
            Log.e("[INFO]", "M1 RED Line drawn at: ($x1, $y1) to ($x2, $y2)")
        }

        for (j in 0 until points.size){
            if (cluster[j][3] == -1){
                num_solo += 1
                var rho = rhos[cluster[j][0]]
                var theta = thetas[cluster[j][1]]
                var a = cos(Math.toRadians(theta))
                var b = sin(Math.toRadians(theta))
                var x0 = (a * rho)
                var y0 = (b * rho)
                var x1 = (x0 + (1000 * (-b))).toInt().toFloat()
                var y1 = (y0 + (1000 * (a))).toInt().toFloat()
                var x2 = (x0 - (1000 * (-b))).toInt().toFloat()
                var y2 = (y0 - (1000 * (a))).toInt().toFloat()
                paintline.color = Color.GREEN
                canvas.drawLine(x1, y1, x2, y2, paintline)
                Log.e("[INFO]", "M1 GRN Line drawn at: ($x1, $y1) to ($x2, $y2)")
            }
        }
        Log.e("[INFO]", "Number of solo points is: $num_solo")
        /********************************************************************************************/

        var total_length = num_solo + cluster_center.size
        var total_plotter = Array(total_length) { Array(2) {0} }
        var index = 0
        for (j in 0 until points.size){
            if (cluster[j][3] == - 1){
                total_plotter[index][0] = cluster[j][0]
                total_plotter[index][1] = cluster[j][1]
                index += 1
            }
        }
        for (j in 0 until cluster_center.size){
            total_plotter[index][0] = cluster_center[j][0]
            total_plotter[index][1] = cluster_center[j][1]
            index += 1
        }

        /********************************************************************************************/
        // DRAW LINES V2

        var start_window_s = 30
        var start_window_w = 30
        var continue_window_s = 20
        var continue_window_w = 30
        var difference_threshold_s = 30
        var continue_threshold_s = 40
        var difference_threshold_w = 30
        var continue_threshold_w = 110
        var difference_threshold = 0
        var continue_threshold = 0
        var start_window = 0
        var continue_window = 0

        var points_ol: Array<Array<Array<Double>>> = Array(Height) { Array(Width) { Array(cluster.size + 1) {0.0} } }
        for (j in 0 until total_plotter.size){
            if (j < num_solo){
                difference_threshold = difference_threshold_s
                continue_threshold = continue_threshold_s
                start_window = start_window_s
                continue_window = continue_window_s
            }
            else if (j >= num_solo){
                difference_threshold = difference_threshold_w
                continue_threshold = continue_threshold_w
                start_window = start_window_w
                continue_window = continue_window_w
            }
            var rho = rhos[total_plotter[j][0]]
            var theta = thetas[total_plotter[j][1]]

            if (theta == 0.0){
                theta = 0.001
            }
            var a = cos(Math.toRadians(theta))
            var b = sin(Math.toRadians(theta))

            var x0 = (a * rho)
            var y0 = (b * rho)

            var drawing = false

            for (i in 0 until (3 * diag_len.toInt())){
                var still_draw = false
                var x = (x0 + (i - diag_len) * (-b)).toInt()
                var y = (y0 + (i - diag_len) * (a)).toInt()
                if ((y > 0) and (y < Height) and (x > 0) and (x < Width)){
                    if (!drawing){
                        for (k in 0 until start_window){
                            var loc_y = y - (start_window / 2).toInt() + k
                            for (m in 0 until x){
                                var loc_x = x - (start_window / 2).toInt() + m
                                if ((loc_y > 0) and (loc_y < Height)){
                                    if ((loc_x > 0) and (loc_x < Width)){
                                        if (dst[loc_y][loc_x] == 255){
                                            if (percent_difference(dst_grad[loc_y][loc_x], theta) < difference_threshold){
                                               drawing = true
                                               still_draw = true
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else if (drawing){
                        for (k in 0 until continue_window){
                            var loc_y = y - (continue_window / 2).toInt() + k
                            for (m in 0 until x){
                                var loc_x = x - (continue_window / 2).toInt() + m
                                if ((loc_y > 0) and (loc_y < Height)){
                                    if ((loc_x > 0) and (loc_x < Width)){
                                        if (dst[loc_y][loc_x] == 255){
                                            if (percent_difference(dst_grad[loc_y][loc_x], theta) < continue_threshold){
                                                still_draw = true
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (!still_draw){
                    drawing = false
                }
                if (drawing){
                    if (j < num_solo){
                        mutableBitmap.setPixel(x, y, Color.rgb(255, 0, 0));
                        Log.e("[INFO]", "M2 RED pixel set at: ($x, $y)")
                    }
                    if (j >= num_solo){
                        mutableBitmap.setPixel(x, y, Color.rgb(0, 255, 0));
                        Log.e("[INFO]", "M2 GRN pixel set at: ($x, $y)")
                    }
                    points_ol[y][x][0] += 1.0
                    points_ol[y][x][j + 1] += theta
                }
            }
        }
        Log.e("[STATUS]", "Ending draw lines")
        /********************************************************************************************/
        Log.e("[STATUS]", "Determining intercepts")
        var intercept: MutableList<Pair<Int, Int>> = mutableListOf()
        for (j in 0 until Height){
            for (i in 0 until Width){
                if (points_ol[j][i][0] > 1){
                    var non_zero_angle: MutableList<Double> = mutableListOf()
                    for (k in 0 until total_plotter.size){
                        if (points_ol[j][i][k+1] > 0){
                            non_zero_angle.add(points_ol[j][i][k+1])
                        }
                    }
                    if ((non_zero_angle.size >  1) and (non_zero_angle.size < 3)){
                        if ((percent_difference(non_zero_angle[0], non_zero_angle[1]) > 50.0) and (percent_difference(non_zero_angle[0], non_zero_angle[1]) < 190.0)){
                            intercept.add(Pair(i, j))
                        }
                    }
                }
            }
        }
        Log.e("[INFO]", "Intercepts at: $intercept")
        Log.e("[STATUS]", "Drawing Circles")
        var paintcircle = Paint()
        paintcircle.strokeWidth = 2.0F
        paintcircle.color = Color.RED
        val radius = 5.0F
        for (i in 0 until intercept.size){
            var y = intercept[i].first.toFloat()
            var x = intercept[i].second.toFloat()
            canvas.drawCircle(y, x, radius, paintcircle)
            Log.e("[INFO]", "Circle set at: ($x, $y)")
        }

        */
        /********************************************************************************************/
        // EDGE SORTING END
        /********************************************************************************************/
//        val display = createBitmap(dst.size, dst[0].size, Config.ARGB_8888)
//        for (x in 0 until dst.size){
//            for (y in 0 until dst[0].size){
//                display.setPixel(((dst.size - 1) - x), y, Color.argb(255, dst[x][y], dst[x][y], dst[x][y]))
//            }
//        }

        viewBinding.productDisplay.setImageBitmap(mutableBitmap)
    }

    /********************************************************************************************/
    // HELPER FUNCTIONS
    /********************************************************************************************/

    fun start_cluster(j: Int, cluster: Array<Array<Int>>, point_mat: MutableList<MutableList<Int>>, current_cluster: Int){
        cluster[j][3] = current_cluster
        var indecies_near = point_mat[j]
        var grow_points: MutableList<Int> = mutableListOf()
        for (k in 0 until indecies_near.size){
            var current_point = indecies_near[k]
            if (cluster[current_point][2] == 1){
                cluster[current_point][3] = current_cluster
                grow_points.add(current_point)
            }
        }
        for (i in 0 until grow_points.size){
            grow_cluster(grow_points[i], cluster, point_mat, current_cluster)
        }
    }

    fun grow_cluster(i: Int, cluster: Array<Array<Int>>, point_mat: MutableList<MutableList<Int>>, current_cluster: Int){
        var grow_points_2: MutableList<Int> = mutableListOf()
        var indecies_near_2 = point_mat[i]
        for (d in 0 until indecies_near_2.size){
            var current_point = indecies_near_2[d]
            if (cluster[current_point][2] == 1){
                if (cluster[current_point][3] == -1){
                    grow_points_2.add(current_point)
                    cluster[current_point][3] = current_cluster
                }
            }
        }
        for (e in 0 until grow_points_2.size){
            grow_cluster(grow_points_2[e], cluster, point_mat, current_cluster)
        }
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
                gray_color[j][k] = ((0.3 * reds[j][k]) + (0.59 * greens[j][k]) + (0.11 * blues[j][k])).toInt()
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

    fun dilate(img: Array<Array<Int>>, x: Int, y: Int): Array<Array<Int>> {
        if (((x % 2) != 1) or ((y % 2) != 1)){
            throw Exception("Invalid Window Size")
        }
        val h = img.size
        val w = img[0].size
        var img_return = Array(h) {Array(w) {0} }
        for (i in 0 until h){
            for (j in 0 until w){
                if (img[i][j] > 0){
                    for (k in 0 until y){
                        var loc_y = i - (y / 2).toInt() + k
                        for (m in 0 until x){
                            var loc_x = j - (x / 2).toInt() + m
                            if ((loc_y > 0) and (loc_y < h)){
                                if ((loc_x > 0) and (loc_x < w)){
                                    img_return[loc_y][loc_x] = img[i][j]
                                }
                            }
                        }
                    }
                }
            }
        }
        return img_return
    }

    fun erosion(img: Array<Array<Int>>, x: Int, y: Int): Array<Array<Int>> {
        if (((x % 2) != 1) or ((y % 2) != 1)){
            throw Exception("Invalid Window Size")
        }
        val h = img.size
        val w = img[0].size
        var img_return = Array(h) {Array(w) {0} }
        for (i in 0 until h){
            for (j in 0 until w){
                if (img[i][j] > 0){
                    for (k in 0 until y){
                        var loc_y = i - (y / 2).toInt() + k
                        for (m in 0 until x){
                            var loc_x = j - (x / 2).toInt() + m
                            if ((loc_y > 0) and (loc_y < h)){
                                if ((loc_x > 0) and (loc_x < w)){
                                    if (img[loc_y][loc_x] < img[i][j]){
                                        img_return[i][j] = img[loc_y][loc_x]
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return img_return
    }

    fun suppressor(img: Array<Array<Int>>, x: Int, y: Int): Array<Array<Int>> {
        if (((x % 2) != 1) or ((y % 2) != 1)) {
            throw Exception("Invalid Window Size")
        }
        val h = img.size
        val w = img[0].size
        var img_return = Array(h) { Array(w) { 0 } }
        for (i in 0 until h) {
            for (j in 0 until w) {
                if (img[i][j] > 0){
                    img_return[i][j] = img[i][j]
                    for (k in 0 until y){
                        var loc_y = i - (y / 2).toInt() + k
                        for (m in 0 until x){
                            var loc_x = j - (x / 2).toInt() + m
                            if ((loc_y > 0) and (loc_y < h)){
                                if ((loc_x > 0) and (loc_x < w)){
                                    if (img[loc_y][loc_x] > img[i][j]){
                                        img_return[i][j] = 0
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return img_return
    }

    fun points(img: Array<Array<Int>>): MutableList<Pair<Int, Int>> {
        val h = img.size
        val w = img[0].size
        var points: MutableList<Pair<Int, Int>> = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until h){
            for (j in 0 until w){
                if (img[i][j] > 0){
                    points.add(Pair(i, j))
                }
            }
        }
        return points
    }

    fun percent_difference(t1: Double, t2: Double): Double {
        return (((abs(t1 - t2)) / ((t1 + t2) / 2)) * 100)
    }

}