package com.vedicapps.mantrajap

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.*
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var themeManager: ThemeManager
    private var currentMantraId: Long = -1L
    private var currentTotalBeads: Int = 0
    private var targetMalas: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        themeManager = ThemeManager(this)
        themeManager.applySavedTheme()

        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val savedLang = prefs.getString("My_Lang", "en") ?: "en"
        val appLocale = LocaleListCompat.forLanguageTags(savedLang)
        AppCompatDelegate.setApplicationLocales(appLocale)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars =
            AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        db = AppDatabase.getDatabase(this)

        currentMantraId = intent.getLongExtra("MANTRA_ID", -1L)

        val rootLayout = findViewById<View>(R.id.playerRoot)
        val nameTxt = findViewById<TextView>(R.id.playerMantraName)
        val countTxt = findViewById<TextView>(R.id.playerCountText)
        val targetTxt = findViewById<TextView>(R.id.playerTargetText)
        val progressHintTxt = findViewById<TextView>(R.id.txtFocusHint)

        lifecycleScope.launch {
            db.mantraDao().getMantraById(currentMantraId).collectLatest { mantra ->
                mantra?.let {
                    currentTotalBeads = it.count
                    targetMalas = it.target
                    val completedMalas = it.count / 108
                    val beadsInCurrentMala = it.count % 108

                    nameTxt.text = it.name

                    val countValue = it.count.toString()
                    val label = " " + getString(R.string.label_chants)
                    val fullText = "$countValue$label"
                    val spannable = SpannableString(fullText)
                    spannable.setSpan(StyleSpan(Typeface.BOLD), 0, countValue.length, 0)
                    spannable.setSpan(RelativeSizeSpan(0.2f), countValue.length, fullText.length, 0)
                    countTxt.text = spannable

                    targetTxt.text = getString(R.string.label_goal_format, it.target)
                    progressHintTxt.text = getString(R.string.label_completed_format, completedMalas, beadsInCurrentMala)

                    if (completedMalas >= it.target) {
                        countTxt.setTextColor(Color.parseColor("#4CAF50"))
                    } else {
                        countTxt.setTextColor(getColorFromAttr(com.google.android.material.R.attr.colorPrimary))
                    }
                }
            }
        }

        // --- HAPTIC ON TOUCH DOWN ---
        rootLayout.isHapticFeedbackEnabled = true
        rootLayout.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // This triggers the instant you touch the screen
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            false // Return false so the click listener still works
        }

        rootLayout.setOnClickListener {
            if (currentMantraId == -1L) return@setOnClickListener

            val nextTotalBeads = currentTotalBeads + 1
            val isMalaComplete = (nextTotalBeads % 108 == 0)
            val completedMalasNow = nextTotalBeads / 108

            if (completedMalasNow >= targetMalas && isMalaComplete) {
                playCompletionSound()
                triggerVibration(600)
                Toast.makeText(this, getString(R.string.toast_target_achieved), Toast.LENGTH_SHORT).show()
            } else if (isMalaComplete) {
                // Extra strong feedback for finishing 108 beads
                it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                triggerVibration(200)
            }
            // Note: Normal haptic is handled by onTouch, so no 'else' needed here!

            lifecycleScope.launch(Dispatchers.IO) {
                db.mantraDao().updateCount(currentMantraId, nextTotalBeads)
            }
        }
    }

    private fun getColorFromAttr(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 1, getString(R.string.menu_reset_count))
        menu?.add(0, 2, 2, getString(R.string.menu_delete_mantra))
        menu?.add(0, 3, 3, getString(R.string.menu_edit_goal))
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> { showResetConfirmation(); true }
            2 -> { showDeleteConfirmation(); true }
            3 -> { showEditTargetDialog(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showResetConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.menu_reset_count))
            .setMessage(getString(R.string.dialog_reset_msg))
            .setPositiveButton(getString(R.string.btn_reset)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.mantraDao().updateCount(currentMantraId, 0)
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.menu_delete_mantra))
            .setMessage(getString(R.string.dialog_delete_confirm_msg))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val mantra = db.mantraDao().getMantraSync(currentMantraId)
                    mantra?.let {
                        db.mantraDao().deleteMantra(it)
                        withContext(Dispatchers.Main) { finish() }
                    }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun showEditTargetDialog() {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.setText(targetMalas.toString())

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.menu_edit_goal))
            .setMessage(getString(R.string.dialog_enter_malas))
            .setView(input)
            .setPositiveButton(getString(R.string.btn_update)) { _, _ ->
                val newTarget = input.text.toString().toIntOrNull() ?: targetMalas
                lifecycleScope.launch(Dispatchers.IO) {
                    val mantra = db.mantraDao().getMantraSync(currentMantraId)
                    mantra?.let { db.mantraDao().insertMantra(it.copy(target = newTarget)) }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun playCompletionSound() {
        try {
            MediaPlayer.create(this, R.raw.bell_sound)?.apply {
                start()
                setOnCompletionListener { release() }
            }
        } catch (e: Exception) {
            Log.e("PlayerActivity", "Sound error: ${e.message}")
        }
    }

    private fun triggerVibration(duration: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") vibrator.vibrate(duration)
        }
    }
}