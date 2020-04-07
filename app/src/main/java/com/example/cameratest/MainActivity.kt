package com.example.cameratest

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var photoUri : Uri?= null
    private var bitmap : Bitmap ?= null

    companion object {
        private const val REQUEST_CAMERA = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
    }
    private fun init() {
        checkPermission()

        camera.setOnClickListener {
            takePicture()
        }
    }
    private fun takePicture() {
        var intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        intent.resolveActivity(packageManager)?.apply {
            try{
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    photoUri = getUriAndroidQ()
                }else {
                    photoUri = getUriUnderAndroidQ()
                }
            }catch (e : Exception) {
                e.printStackTrace()
            }
            photoUri?.apply {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(intent, REQUEST_CAMERA)
            }
        }
    }
    private fun getUriAndroidQ() : Uri {
        // 같은날짜 같은시간에 찍으면 파일명이 동일해지니, 초까지 구분해서 이름 생성
        val realTime = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val photoFileName = "photo_$realTime.jpg"

        var uri: Uri? = null

        val contentResolver = applicationContext.contentResolver
        val contentValues = ContentValues()

        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, photoFileName)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/androidQ_Photo/")

        uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        return uri!!
    }
    private fun getUriUnderAndroidQ(): Uri? {
        var realTime = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val photoFileName = "photo_$realTime.jpg"

        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val photoFile = File(dir, "/Camera/$photoFileName")

        return FileProvider.getUriForFile(this, "com.example.cameratest.provider", photoFile)
    }
    private fun checkPermission() {
        var permission = object : PermissionListener{
            override fun onPermissionGranted() {
                Toast.makeText(applicationContext, "권한 허용", Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionDenied(deniedPermissions: ArrayList<String>?) {
                Toast.makeText(applicationContext, "권한 거부", Toast.LENGTH_SHORT).show()
            }
        }

        TedPermission.with(applicationContext)
            .setPermissionListener(permission)
            .setRationaleMessage("카메라 권한 필요함")
            .setDeniedMessage("카메라 권한 거부함")
            .setPermissions(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA
            )
            .check()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        /**
         * 비트맵으로 생성하여 세로로 찍으면 가로로 누움 (lg폰만 그랬음, 옛날,최신 삼성휴대폰(A6도 제대로 됨)
         * 이유는 모르겠음
         * uri로 넘겨주면 안돌아가긴 하지만, 비트맵으로 사용하게될경우 돌려줘야되기 때문에 만듬
         */

        when(requestCode) {
            REQUEST_CAMERA -> {
                if(resultCode == Activity.RESULT_OK) {
                    photoUri?.path?.apply {
                        if(Build.VERSION.SDK_INT < 28) {
                            bitmap = MediaStore.Images.Media.getBitmap(
                                contentResolver,
                                photoUri
                            )
                            var inputStream = contentResolver.openInputStream(photoUri!!)
                            val exifInterface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                ExifInterface(inputStream)
                            } else {
//                                TODO("VERSION.SDK_INT < N")
                                val path = photoUri?.path.toString().replace("/external_files/","/storage/emulated/0/")
                                ExifInterface(path)
                            }
                            var orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

                            bitmap =
                                when(orientation){
                                    ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap!!,90f)
                                    ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap!!,180f)
                                    ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap!!,270f)
                                    else -> bitmap
                                }
                            bitmap?.let {
                                setGlideViewBitmap(it)
                            }
//                            setGlideView(photoUri.toString())
                        }else {
//                            bitmap?.let {
//                                setGlideViewBitmap(it)
//                            }
                            setGlideView(photoUri.toString())
                        }
                    }
                }else{
                    photoUri?.let{
                        photoUri = null
                        contentResolver.delete(it, null, null)
                        return
                    }
                }
            }
        }
    }
    private fun setGlideView(uri : String){
        Glide.with(this).load(uri).into(image)
    }
    private fun setGlideViewBitmap(uri : Bitmap){
        Glide.with(this).load(uri).into(image)
    }
    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

}
