package com.charonchui.cyberlink

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.databinding.DataBindingUtil
import com.charonchui.cyberlink.databinding.ActivityControlBinding
import com.charonchui.cyberlink.engine.DLNAContainer
import com.charonchui.cyberlink.engine.MultiPointController
import com.charonchui.cyberlink.inter.IController
import com.charonchui.cyberlink.util.LogUtil
import com.google.gson.reflect.TypeToken
import com.shaoman.customer.helper.JsonEntityGetter
import com.shaoman.customer.model.entity.res.HttpResult
import com.shaoman.customer.model.entity.res.PageInfoResult
import com.shaoman.customer.model.entity.res.VideoEntity
import com.shaoman.customer.util.ThreadUtils
import org.cybergarage.upnp.Device
import java.util.*

class ControlActivity : BaseActivity(R.layout.activity_control), View.OnClickListener {

    private var mController: IController? = null

    private var mDevice: Device? = null
    private val urls: MutableList<String> = ArrayList()
    private var index = 0
    private var mMediaDuration = 0
    private var mPaused = false
    private var mPlaying = false
    private var mStartAutoPlayed = false

    @SuppressLint("HandlerLeak")
    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                AUTO_INCREASING -> {
                    stopAutoIncreasing()
                    getPositionInfo()
                    startAutoIncreasing()
                }
                AUTO_PLAYING -> playNext()
                else -> {
                }
            }
        }
    }

    private fun getMaxVolumn() {
        runInThread {
            val maxVolumnValue = mController?.getMaxVolumeValue(mDevice) ?: 0
            ThreadUtils.runMain {
                if (maxVolumnValue <= 0) {
                    LogUtil.d(TAG, "get max volumn value failed..")
                    binding.sbVoice.max = 100
                } else {
                    LogUtil.d(TAG, "get max volumn value success, the value is "
                            + maxVolumnValue)
                    binding.sbVoice.max = maxVolumnValue
                }
            }
        }
    }

    // Do your things here;
    @get:Synchronized
    private val transportState: Unit
        get() {
            runInThread {
                val transportState = mController!!.getTransportState(mDevice)
                // Do your things here;
                LogUtil.d(TAG, "Get transportState :$transportState")
            }
        }

    /**
     * Get the current play path of the video.
     *
     * @return The video path to play.
     */
    private val currentPlayPath: String
        get() = urls[index]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findView()
        initView()
        loadHttpData()
    }

    private fun loadHttpData() {
        val baseUrl = "http://114.116.247.15:8399"
        val url = "$baseUrl/video/lift"
        val inType = TypeToken.getParameterized(PageInfoResult::class.java, VideoEntity::class.java).type
        val typeToken = TypeToken.getParameterized(HttpResult::class.java, inType)
        val param = hashMapOf<String, Any>(Pair("page", 1), Pair("pageSize", 10000))
        JsonEntityGetter.postJsonEntity<PageInfoResult<VideoEntity>>(url,
                param, typeToken) {
            if (it.status == 0) {
                try {
                    val list = it.data.list
                    urls.clear()
                    for (i in list) {
                        if (!i.getActualUrl().contains("//var")) {
                            LogUtil.e("url=", " i . getActualUrl () = ${i.getActualUrl()}")
                            urls.add(i.getActualUrl())
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        updateVoice()
        getMute()
    }

    override fun onDestroy() {
        super.onDestroy()
        stop()
    }

    private lateinit var binding: ActivityControlBinding

    private fun findView() {
        //ActivityControlBinding
        val content = window.decorView.findViewById<ViewGroup>(Window.ID_ANDROID_CONTENT)
        val rootView = content.getChildAt(0)
        binding = DataBindingUtil.bind(rootView)!!
    }

    private fun initView() {
        setController(MultiPointController())
        mDevice = DLNAContainer.getInstance().selectedDevice
        urls.add("https://shaoman.obs.cn-north-4.myhuaweicloud.com/uploadVideo/video_android_upload_ece3f47a385674bc5aa22cbb3c882f12_1920x1080.mp4")
        if (mController == null || mDevice == null) {
            // usually can't reach here.
            Toast.makeText(applicationContext, "Invalidate operation",
                    Toast.LENGTH_SHORT).show()
            LogUtil.d(TAG, "Controller or Device is null, finish this activity")
            finish()
            return
        }

        // init the state
        getMaxVolumn()
        binding.sbProgress.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                startAutoIncreasing()
                val progress = seekBar.progress
                seek(secToTime(progress))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                stopAutoIncreasing()
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                           fromUser: Boolean) {
                binding.tvCurrent.text = secToTime(progress)
                if (fromUser) {
                    stopAutoIncreasing()
                }
            }
        })
        binding.sbVoice.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                setVoice(seekBar.progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                           fromUser: Boolean) {
                if (progress == 0) {
                    binding.ivMute.visibility = View.VISIBLE
                    binding.ivVolume.visibility = View.GONE
                } else {
                    binding.ivMute.visibility = View.GONE
                    binding.ivVolume.visibility = View.VISIBLE
                }
            }
        })
        binding.ivPre.setOnClickListener(this)
        binding.ivNext.setOnClickListener(this)
        binding.ivGoFast.setOnClickListener(this)
        binding.ivBackFast.setOnClickListener(this)
        binding.flPlay.setOnClickListener(this)
        binding.flVolume.setOnClickListener(this)
        play(currentPlayPath)
    }

    private fun setController(controller: IController) {
        mController = controller
    }

    /**
     * Get current voice value and set it the binding.sbVoice.
     */
    private fun updateVoice() {
        runInThread {
            var currentVoice = mController!!.getVoice(mDevice)
            if (currentVoice == -1) {
                currentVoice = 0
                LogUtil.d(TAG, "get current voice failed")
            } else {
                LogUtil.d(TAG, "get current voice success")
                binding.sbVoice.progress = currentVoice
            }
        }
    }

    private fun runInThread(function: Function0<Unit>) {
        ThreadUtils.runOnBackground {
            function.invoke()
        }
    }

    /**
     * Get if is muted and set the mute image.
     */
    fun getMute() {
        runInThread {
            val mute = mController!!.getMute(mDevice)
            ThreadUtils.runMain {
                if (mute == null) {
                    LogUtil.d(TAG, "get mute failed...")
                    if (binding.sbVoice.progress == 0) {
                        initMuteImg(MUTE)
                    }
                } else {
                    LogUtil.d(TAG, "get mute success")
                    initMuteImg(mute)
                }
            }
        }
    }

    /**
     * Start to play the video.
     *
     * @param path The video path.
     */
    @Synchronized
    private fun play(path: String) {
        // Initial the state.
        mPaused = false
        showPlay(true)
        setCurrentTime(ZEROTIME)
        setTotalTime(ZEROTIME)
        setTitle(path)
        stopAutoIncreasing()
        stopAutoPlaying()

        runInThread {
            val isSuccess = mController!!.play(mDevice, path)
            if (isSuccess) {
                LogUtil.d(TAG, "play success")
            } else {
                LogUtil.d(TAG, "play failed..")
            }
            ThreadUtils.runMain {
                LogUtil.d(TAG,
                        "play success and start to get media duration")
                if (isSuccess) {
                    mPlaying = true
                    startAutoIncreasing()
                }
                showPlay(!isSuccess)
                // Get the media duration and set it to the total time.
                getMediaDuration()
            }
        }
    }

    @Synchronized
    private fun pause() {
        stopAutoIncreasing()
        stopAutoPlaying()
        showPlay(true)
        object : Thread() {
            override fun run() {
                val isSuccess = mController!!.pause(mDevice)
                runOnUiThread {
                    showPlay(isSuccess)
                    if (isSuccess) {
                        mPaused = true
                        mPlaying = false
                        mHandler.removeMessages(AUTO_PLAYING)
                    } else {
                        startAutoIncreasing()
                    }
                }
            }
        }.start()
    }

    @Synchronized
    private fun playNext() {
        index++
        if (index > urls.size - 1) {
            index = 0
        }
        LogUtil.e("controller", "playNext $currentPlayPath")
        play(currentPlayPath)
    }

    @Synchronized
    private fun playPre() {
        index--
        if (index < 0) {
            index = urls.size - 1
        }
        play(currentPlayPath)
    }

    @Synchronized
    private fun goon(pausePosition: String) {
        object : Thread() {
            override fun run() {
                val isSuccess = mController!!.goon(mDevice,
                        pausePosition)
                if (isSuccess) {
                    mPlaying = true
                    LogUtil.d(TAG, "Go on to play success")
                } else {
                    mPlaying = false
                    LogUtil.d(TAG, "Go on to play failed.")
                }
                runOnUiThread {
                    showPlay(!isSuccess)
                    if (isSuccess) {
                        startAutoIncreasing()
                    }
                }
            }
        }.start()
    }

    /**
     * Seek playing position to the target position.
     *
     * @param targetPosition target position like "00:00:00"
     */
    @Synchronized
    private fun seek(targetPosition: String) {
        object : Thread() {
            override fun run() {
                val isSuccess = mController!!.seek(mDevice, targetPosition)
                if (isSuccess) {
                    LogUtil.d(TAG, "seek success")
                    binding.sbProgress.progress = getIntLength(targetPosition)
                } else {
                    LogUtil.d(TAG, "seek failed..")
                }
                runOnUiThread {
                    if (mPlaying) {
                        startAutoIncreasing()
                    } else {
                        stopAutoIncreasing()
                    }
                }
            }
        }.start()
    }

    private fun getPositionInfo() {
        runInThread {
            val position = mController!!.getPositionInfo(mDevice)
            LogUtil.d(TAG, "Get position info and the value is $position")
            if (TextUtils.isEmpty(position)
                    || NOT_IMPLEMENTED == position) {
                return@runInThread
            }
            val currentPosition = getIntLength(position)
            if (currentPosition <= 0 || currentPosition > mMediaDuration) {
                return@runInThread
            }

            runOnUiThread(Runnable {

                binding.sbProgress.progress = getIntLength(position)

                if (currentPosition >= mMediaDuration - 3
                        && mMediaDuration > 0) {
                    if (mStartAutoPlayed) {
                        return@Runnable
                    } else {
                        mStartAutoPlayed = true
                        LogUtil.d(TAG, "start auto play next video")
                        stopAutoPlaying()
                        startAutoPlaying(((mMediaDuration - currentPosition) * 1000).toLong())
                    }
                } else if (mMediaDuration <= 0 || mMediaDuration < currentPosition) {
                    getMediaDuration()
                }
            })
        }
    }

    private fun getMediaDuration() {
        runInThread {
            val mediaDuration = mController?.getMediaDuration(mDevice) ?: ""
            mMediaDuration = getIntLength(mediaDuration)
            LogUtil.d(TAG, "Get media duration and the value is $mMediaDuration")
            ThreadUtils.runMain {
                if (TextUtils.isEmpty(mediaDuration)
                        || NOT_IMPLEMENTED == mediaDuration || mMediaDuration <= 0) {
                    mHandler.postDelayed({
                        LogUtil.e(TAG,
                                "Get media duration failed, retry later."
                                        + "Duration:"
                                        + mediaDuration
                                        + "intLength:"
                                        + mMediaDuration)
                        getMediaDuration()
                    }, RETRY_TIME.toLong())
                    return@runMain
                }
                binding.tvTotal.text = mediaDuration
                binding.sbProgress.max = mMediaDuration
            }
        }
    }

    @Synchronized
    private fun setMute(targetValue: String) {
        runInThread {
            val isSuccess = mController!!.setMute(mDevice, targetValue)
            if (isSuccess) {
                runOnUiThread {
                    initMuteImg(targetValue)
                    getVoice()
                }
            }
        }
    }

    @Synchronized
    private fun setVoice(voice: Int) {
        runInThread {
            val isSuccess = mController!!.setVoice(mDevice, voice)
            if (isSuccess) {
                binding.sbVoice.progress = voice
                if (voice == 0) {
                    initMuteImg(MUTE)
                }
            }
        }
    }

    fun getVoice() {
        runInThread {
            val voice = mController!!.getVoice(mDevice)
            binding.sbVoice.progress = voice
            if (voice == 0) {
                initMuteImg(MUTE)
            }
        }
    }

    @Synchronized
    private fun stop() {
        stopAutoPlaying()
        stopAutoIncreasing()
        runInThread {
            val isSuccess = mController!!.stop(mDevice)
            runOnUiThread { showPlay(isSuccess) }
        }
    }

    /**
     * 快进或快退
     *
     * @param isGo true表示快进，false为快退
     */
    @Synchronized
    private fun fastGoOrBack(isGo: Boolean) {
        stopAutoIncreasing()
        val position = binding.tvCurrent.text.toString()
        var targetLength: Int
        if (isGo) {
            targetLength = getIntLength(position) + 10
            if (targetLength > mMediaDuration) {
                targetLength = mMediaDuration
            }
        } else {
            targetLength = getIntLength(position) - 10
            if (targetLength < 0) {
                targetLength = 0
            }
        }
        binding.sbProgress.progress = targetLength
        seek(secToTime(targetLength))
    }

    private fun setCurrentTime(time: String) {
        binding.tvCurrent.text = time
    }

    private fun setTotalTime(time: String) {
        binding.tvTotal.text = time
    }

    private fun setTitle(title: String) {
        var title: String? = title
        when (index % urls.size) {
            0 -> title = "我们结婚吧"
            1 -> title = "伊能静专访"
            2 -> title = "佟丽娅专访"
            else -> {
            }
        }
        binding.tvTitle.text = title
    }

    private fun startAutoIncreasing() {
        mHandler.sendEmptyMessageDelayed(AUTO_INCREASING, 1000)
    }

    private fun stopAutoIncreasing() {
        mHandler.removeMessages(AUTO_INCREASING)
    }

    private fun startAutoPlaying(interTimes: Long) {
        mHandler.sendEmptyMessageAtTime(AUTO_PLAYING, interTimes)
    }

    private fun stopAutoPlaying() {
        mHandler.removeMessages(AUTO_PLAYING)
    }

    /**
     * Show the play or pause image.
     *
     * @param showPlay True to show the play image otherwise false.
     */
    private fun showPlay(showPlay: Boolean) {
        if (showPlay) {
            binding.ivPlay.visibility = View.VISIBLE
            binding.ivPause.visibility = View.GONE
        } else {
            binding.ivPlay.visibility = View.GONE
            binding.ivPause.visibility = View.VISIBLE
        }
    }

    /**
     * Show the mute or volume image.
     *
     * @param mute 1 is mute, otherwise is 0.
     */
    private fun initMuteImg(mute: String) {
        if (MUTE == mute) {
            binding.ivMute.visibility = View.VISIBLE
            binding.ivVolume.visibility = View.GONE
            binding.sbVoice.progress = 0
        } else if (UNMUTE == mute) {
            binding.ivMute.visibility = View.GONE
            binding.ivVolume.visibility = View.VISIBLE
        }
    }

    /**
     * Convert time from "00:00:00" to seconds.
     *
     * @param length 00:00:00或者00:00
     * @return The length in seconds.
     */
    private fun getIntLength(length: String): Int {
        if (TextUtils.isEmpty(length)) {
            return 0
        }
        val split = length.split(":").toTypedArray()
        var count = 0
        try {
            if (split.size == 3) {
                count += split[0].toInt() * 60 * 60
                count += split[1].toInt() * 60
                count += split[2].toInt()
            } else if (split.size == 2) {
                count += split[0].toInt() * 60
                count += split[1].toInt()
            }
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        return count
    }

    override fun onClick(v: View) {
        when (v) {
            binding.flPlay -> {
                if (mPlaying) {
                    pause()
                    return
                }
                if (mPaused) {
                    val pausePosition = binding.tvCurrent.text.toString().trim { it <= ' ' }
                    goon(pausePosition)
                } else {
                    play(currentPlayPath)
                }
            }
            binding.flVolume -> {
                var targetValue = MUTE
                if (binding.ivMute.visibility == View.VISIBLE) {
                    targetValue = UNMUTE
                    binding.ivMute.visibility = View.GONE
                    binding.ivVolume.visibility = View.VISIBLE
                } else {
                    binding.ivMute.visibility = View.VISIBLE
                    binding.ivVolume.visibility = View.GONE
                    binding.sbVoice.progress = 0
                }
                setMute(targetValue)
            }
            binding.ivPre -> playPre()
            binding.ivNext -> playNext()
            binding.ivGoFast -> fastGoOrBack(true)
            binding.ivBackFast -> fastGoOrBack(false)
            else -> {
            }
        }
    }

    companion object {

        private const val ZEROTIME = "00:00:00"
        private const val MUTE = "1"
        private const val UNMUTE = "0"
        private const val RETRY_TIME = 1000
        private const val NOT_IMPLEMENTED = "NOT_IMPLEMENTED"
        private const val AUTO_INCREASING = 8001
        private const val AUTO_PLAYING = 8002
        private const val TAG = "ControlActivity"

        /**
         * Convert the time in seconds to "00:00:00" style.
         *
         * @param time The time in seconds.
         * @return The formated style like "00:00:00".
         */
        fun secToTime(time: Int): String {
            val timeStr: String?
            val hour: Int
            var minute: Int
            val second: Int
            if (time <= 0) return "00:00:00" else {
                minute = time / 60
                if (minute < 60) {
                    second = time % 60
                    timeStr = "00:" + unitFormat(minute) + ":" + unitFormat(second)
                } else {
                    hour = minute / 60
                    if (hour > 99) return "99:59:59"
                    minute %= 60
                    second = time - hour * 3600 - minute * 60
                    timeStr = (unitFormat(hour) + ":" + unitFormat(minute) + ":"
                            + unitFormat(second))
                }
            }
            return timeStr
        }

        /**
         * Make sure if the parameter is less than 10 to add "0" before it.
         *
         * @param i The number to be formatted.
         * @return The formatted number like "00" or "12";
         */
        private fun unitFormat(i: Int): String {
            return when (i) {
                in 0..9 -> "0$i"
                in 10..60 -> {
                    "" + i
                }
                else -> {
                    "00"
                }
            }
        }
    }
}