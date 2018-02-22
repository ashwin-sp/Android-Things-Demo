package com.bhaicompany.androidthingsdemo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ImageReader
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Base64
import android.util.Log
import android.view.View
import com.bhaicompany.androidthingsdemo.Retrofit.ImageCallback
import com.bhaicompany.androidthingsdemo.Retrofit.RetrofitCallBuilder
import com.bhaicompany.androidthingsdemo.Util.CameraHandler
import com.bhaicompany.androidthingsdemo.Util.ImagePreprocessor
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.annotations.NotNull
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity(), ImageReader.OnImageAvailableListener {


    private var mImagePreprocessor: ImagePreprocessor? = null
    private var mCameraHandler: CameraHandler? = null

    private var mTtsEngine: TextToSpeech? = null

    private var mBackgroundThread: HandlerThread? = null
    private var mBackgroundHandler: Handler? = null


    private val mReady = AtomicBoolean(false)
    private val PERMISSION_REQUEST_CODE = 100

    private val RANDOM = Random()

    private val UTTERANCE_ID = "com.bhaicompany.androidthingsdemo.UTTERANCE_ID"

    private val SHUTTER_SOUNDS = listOf("Click!","Cheeeeese!","Smile!")

    private var firstCheck : Boolean = false
    private var secondCheck : Boolean = false




   // val faceProperties: Array<String> = arrayOf("skin", "nose", "head", "girl", "eye", "mouth", "child", "ear", "face")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()

        if (ContextCompat.checkSelfPermission(this@MainActivity,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity,
                            Manifest.permission.CAMERA)) {

            } else {
                ActivityCompat.requestPermissions(this@MainActivity,
                        arrayOf(Manifest.permission.CAMERA),
                        PERMISSION_REQUEST_CODE)
            }
        }

    }

    private fun init()
    {
        firstCheck = false
        secondCheck = false

        mBackgroundThread = HandlerThread("BackgroundThread")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
        mBackgroundHandler!!.post(mInitializeOnBackground)
        takePicButton.setOnClickListener({
            if (mReady.get()) {
                // mTtsEngine?.speak("Taking photo", TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
                mReady.set(false)
                mBackgroundHandler?.post(mBackgroundClickHandler)
            } else {
                mTtsEngine?.speak("Photo capture in progress", TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
                Log.i("Logging", "Sorry, processing hasn't finished. Try again in a few seconds")
            }
        })

        /** NO support for opening wifi settings so the following lines may not work now **/
    /*    internet.setOnClickListener({
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        })*/
    }

    private val mInitializeOnBackground = Runnable {
        mImagePreprocessor = ImagePreprocessor()

        mTtsEngine = TextToSpeech(this@MainActivity,
                TextToSpeech.OnInitListener { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        mTtsEngine?.language = Locale.US
                        mTtsEngine?.setOnUtteranceProgressListener(utteranceListener)
                        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val amStreamMusicMaxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, amStreamMusicMaxVol, 0)
                        mTtsEngine?.speak("I'm ready!", TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
                    } else {
                        Log.w("Error: ", "Could not open TTS Engine (onInit status=" + status
                                + "). Ignoring text to speech")
                        mTtsEngine = null
                    }
                })

        mCameraHandler = CameraHandler.getInstance() as CameraHandler
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            mCameraHandler!!.initializeCamera(
                    this@MainActivity, mBackgroundHandler,
                    this@MainActivity)
            mReady.set(true)
        }
        else
        {
            ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.CAMERA),
                    PERMISSION_REQUEST_CODE)
        }
    }

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {
            mReady.set(false)
        }

        override fun onDone(utteranceId: String) {
            mReady.set(true)
        }

        override fun onError(utteranceId: String) {
            mReady.set(true)
        }
    }

    private val mBackgroundClickHandler = Runnable {

        speakShutterSound(mTtsEngine)

        mCameraHandler?.takePicture()
    }

    override fun onImageAvailable(reader: ImageReader?) {
        lateinit var bitmap: Bitmap
        reader?.acquireNextImage().use { image -> bitmap = mImagePreprocessor?.preprocessImage(image)!! }

        runOnUiThread { img_result.setImageBitmap(bitmap) }

        mReady.set(true)

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)


        val byteArray = byteArrayOutputStream .toByteArray()
        val encoded = Base64.encodeToString(byteArray, Base64.DEFAULT)
        checkForProperImage(encoded, bitmap)
    }

    private fun <T> getRandomElement(list: List<T>): T {
        return list[RANDOM.nextInt(list.size)]
    }

    private fun speakShutterSound(tts: TextToSpeech?) {
        tts?.setPitch(1.5f)
        tts?.setSpeechRate(1.5f)
        tts?.speak(getRandomElement(SHUTTER_SOUNDS), TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
        tts?.setPitch(1f)
        tts?.setSpeechRate(1f)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PERMISSION_REQUEST_CODE) {
                mCameraHandler!!.initializeCamera(
                        this@MainActivity, mBackgroundHandler,
                        this@MainActivity)
                mReady.set(true)
            }
        }
        else
        {
            finish()
        }
    }

    private fun checkForProperImage(encodedImage: String, bitmap: Bitmap)
    {
        RetrofitCallBuilder.initRetroBuilder()
        if (isOnline()) {
            runOnUiThread({
                progressBar.visibility = View.VISIBLE
            })

            RetrofitCallBuilder.getData("https://vision.googleapis.com/v1/images:annotate?key=".plus(resources.getString(R.string.key)), encodedImage,  object : ImageCallback {
            override fun updateImage(@NotNull jsonArray: JSONArray) {
                runOnUiThread({
                    progressBar.visibility = View.GONE
                })
                if(jsonArray.length() > 0) {
                    val singleObject = jsonArray.getJSONObject(0)
                    if (singleObject.has("labelAnnotations")) {
                        val labels = singleObject.getJSONArray("labelAnnotations")
                        val desc: ArrayList<String> = ArrayList()
                        val score: ArrayList<Double> = ArrayList()
                        for (i in 0 until labels.length()) {
                            desc.add(labels.getJSONObject(i).getString("description"))
                            score.add(labels.getJSONObject(i).getDouble("score"))
                        }
                        Log.d("desc", desc.toString())
                        Log.d("score", score.toString())
                        if (desc.size>0) {
                            status_text.text = "It might be one of  "
                            for(i in 0 until desc.size)
                            {
                                if(i == desc.size -1)
                                {
                                    status_text.append(" "+ desc[i])
                                }
                                else {
                                    status_text.append(" " + desc[i] + ",")
                                }
                            }
                            mTtsEngine?.speak(resources.getString(R.string.capture_successful), TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
                        } else {
                            status_text.text = resources.getString(R.string.retake)
                            mTtsEngine?.speak(resources.getString(R.string.retake), TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
                        }
                        val imageBytes = Base64.decode(encodedImage, Base64.DEFAULT)
                        val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                 /*       val matrix = Matrix()
                        matrix.postRotate(270.0f)
                        val rotatedBitmap = Bitmap.createBitmap(decodedImage, 0, 0, decodedImage.width, decodedImage.height, matrix, true)*/
                        img_result.setImageBitmap(decodedImage)
                    } else {
                        status_text.text = resources.getString(R.string.retake)
                        mTtsEngine?.speak(resources.getString(R.string.retake), TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
                    }
                }
            }
        })

        }
        else
        {
            Log.d("No internet ", "true")
            mTtsEngine?.speak(resources.getString(R.string.no_internet_connectivity), TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
            status_text.text = resources.getString(R.string.no_internet_connectivity)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mBackgroundThread?.quit()
        } catch (t: Throwable) {
            // close quietly
        }

        mBackgroundThread = null
        mBackgroundHandler = null

        try {
            mCameraHandler?.shutDown()
        } catch (t: Throwable) {
            // close quietly
        }

        mTtsEngine?.stop()
        mTtsEngine?.shutdown()
    }


    private fun getConnectivityStatus(context: Context): Int {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = cm.activeNetworkInfo
        if (null != activeNetwork) {
            if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
                return 1
            }
            if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) {
                return 2
            }
        }
        return 0
    }

    private fun isOnline(): Boolean {

        val conn = getConnectivityStatus(this@MainActivity)
        var status = false
        if (conn == 1 || conn == 2) {
            status = true
        } else if (conn == 0) {
            status = false
        }
        return status
    }

}
