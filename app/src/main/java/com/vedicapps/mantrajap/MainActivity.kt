package com.vedicapps.mantrajap

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var themeManager: ThemeManager
    private lateinit var drawerLayout: DrawerLayout

    // Adapters
    private val fixedMantraAdapter = MantraAdapter { openPlayer(it) }
    private val customMantraAdapter = MantraAdapter { openPlayer(it) }
    private lateinit var headerFixed: HeaderAdapter
    private lateinit var headerCustom: HeaderAdapter
    private val emptyCustomAdapter = EmptyStateAdapter(false)

    // Flag to control Splash Screen visibility
    private var isDataReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Splash Screen setup (MUST be before super.onCreate)
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isDataReady }

        // 2. Theme and Locale (Language) setup
        themeManager = ThemeManager(this)
        themeManager.applySavedTheme()

        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val savedLang = prefs.getString("My_Lang", "en") ?: "en"
        val appLocale = LocaleListCompat.forLanguageTags(savedLang)
        AppCompatDelegate.setApplicationLocales(appLocale)

        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize UI Elements
        headerFixed = HeaderAdapter(getString(R.string.header_suggested))
        headerCustom = HeaderAdapter(getString(R.string.header_daily))
        db = AppDatabase.getDatabase(this)
        drawerLayout = findViewById(R.id.drawerLayout)

        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        val recyclerView = findViewById<RecyclerView>(R.id.mantraRecyclerView)
        val fab = findViewById<FloatingActionButton>(R.id.addMantraFab)

        val navView = findViewById<NavigationView>(R.id.navView)
        val txtVersion = navView.findViewById<TextView>(R.id.txtAppVersion)

// This logic ensures the text updates even if you change the version in build.gradle
        val version = packageManager.getPackageInfo(packageName, 0).versionName
        txtVersion.text = "Version $version"

        // RecyclerView & ConcatAdapter Setup
        val combinedAdapter = ConcatAdapter(headerFixed, fixedMantraAdapter, headerCustom, emptyCustomAdapter, customMantraAdapter)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = combinedAdapter

        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        setupDrawerContent(navView)

        // 3. Data Observation & Splash Screen Dismissal
        lifecycleScope.launch {
            // Give the user a moment to see the ॐ logo
            delay(1000)

            db.mantraDao().getAllMantras().collect { fullList ->
                val fixedItems = fullList.filter { it.isFixed }
                val customItems = fullList.filter { !it.isFixed }

                fixedMantraAdapter.submitList(fixedItems)
                customMantraAdapter.submitList(customItems)
                emptyCustomAdapter.updateVisibility(customItems.isEmpty())

                // IMPORTANT: Dismiss splash screen only after data is first received
                isDataReady = true
            }
        }

        // 4. Background Sync for Fixed Mantras
        val syncManager = SyncManager(this, db)
        lifecycleScope.launch(Dispatchers.IO) { syncManager.syncFixedMantras() }

        setupSwipeActions(recyclerView)
        fab.setOnClickListener { showAddDialog() }

        // Handle Window Insets for Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupDrawerContent(navigationView: NavigationView) {
        updateDrawerThemeItem(navigationView)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_theme -> {
                    themeManager.toggleTheme()
                    updateDrawerThemeItem(navigationView)
                }
                R.id.nav_language -> showLanguageDialog()
                R.id.nav_share -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        val appUrl = "https://play.google.com/store/apps/details?id=${packageName}"
                        putExtra(Intent.EXTRA_TEXT, "Check out this Vedic Mantra app: $appUrl")
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share via"))
                }
                R.id.nav_rate -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("market://details?id=${packageName}")
                    }
                    try { startActivity(intent) } catch (e: Exception) {
                        startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=${packageName}")))
                    }
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun updateDrawerThemeItem(navView: NavigationView) {
        val isDark = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        navView.menu.findItem(R.id.nav_theme)?.apply {
            setTitle(if (isDark) getString(R.string.menu_theme_light) else getString(R.string.menu_theme_dark))
            setIcon(if (isDark) R.drawable.ic_light_mode else R.drawable.ic_night_mode)
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "हिन्दी (Hindi)")
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_language_title))
            .setItems(languages) { _, which ->
                val localeTag = if (which == 0) "en" else "hi"
                getSharedPreferences("Settings", Context.MODE_PRIVATE).edit().putString("My_Lang", localeTag).apply()
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))
                recreate()
            }.show()
    }

    private fun openPlayer(mantra: Mantra) {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("MANTRA_ID", mantra.id)
        startActivity(intent)
    }

    private fun setupSwipeActions(recyclerView: RecyclerView) {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val swipedMantra = customMantraAdapter.currentList[position]
                if (direction == ItemTouchHelper.LEFT) showDeleteConfirmation(swipedMantra, position)
                else showEditMantraDialog(swipedMantra, position)
            }
            override fun getSwipeDirs(r: RecyclerView, v: RecyclerView.ViewHolder): Int {
                return if (v.bindingAdapter == customMantraAdapter) super.getSwipeDirs(r, v) else 0
            }
            override fun onChildDraw(c: Canvas, r: RecyclerView, v: RecyclerView.ViewHolder, dX: Float, dY: Float, s: Int, a: Boolean) {
                val itemView = v.itemView
                val paint = Paint()
                val textPaint = Paint().apply { color = Color.WHITE; textSize = 42f; isFakeBoldText = true }
                if (dX > 0) {
                    paint.color = Color.parseColor("#4CAF50")
                    c.drawRect(RectF(itemView.left.toFloat(), itemView.top.toFloat(), itemView.left.toFloat() + dX, itemView.bottom.toFloat()), paint)
                    if (dX > 100) c.drawText(getString(R.string.menu_edit_goal), itemView.left + 50f, itemView.top + (itemView.height / 2f) + 15f, textPaint)
                } else if (dX < 0) {
                    paint.color = Color.parseColor("#D32F2F")
                    c.drawRect(RectF(itemView.right.toFloat() + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat()), paint)
                    if (Math.abs(dX) > 100) c.drawText(getString(R.string.menu_delete_mantra), itemView.right - 220f, itemView.top + (itemView.height / 2f) + 15f, textPaint)
                }
                super.onChildDraw(c, r, v, dX, dY, s, a)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun showDeleteConfirmation(mantra: Mantra, position: Int) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_delete_title, mantra.name))
            .setMessage(getString(R.string.dialog_delete_msg))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) { db.mantraDao().deleteMantra(mantra) }
            }
            .setNegativeButton(getString(R.string.btn_cancel)) { _, _ -> customMantraAdapter.notifyItemChanged(position) }
            .setOnCancelListener { customMantraAdapter.notifyItemChanged(position) }
            .show()
    }

    private fun showEditMantraDialog(mantra: Mantra, position: Int) {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(mantra.target.toString())
            setSelection(text.length)
        }
        val container = FrameLayout(this).apply {
            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(60, 40, 60, 40)
            input.layoutParams = params
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_edit_title, mantra.name))
            .setView(container)
            .setPositiveButton(getString(R.string.btn_update)) { _, _ ->
                val newTarget = input.text.toString().toIntOrNull() ?: mantra.target
                lifecycleScope.launch(Dispatchers.IO) { db.mantraDao().insertMantra(mantra.copy(target = newTarget)) }
            }
            .setNegativeButton(getString(R.string.btn_cancel)) { _, _ -> customMantraAdapter.notifyItemChanged(position) }
            .setOnCancelListener { customMantraAdapter.notifyItemChanged(position) }
            .show()
    }

    private fun showAddDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_mantra, null)
        val editName = view.findViewById<EditText>(R.id.editMantraName)
        val editTargetMala = view.findViewById<EditText>(R.id.editMantraTarget)
        val txtInfo = view.findViewById<TextView>(R.id.txtTotalChantsInfo)

        // Set initial text to 0
        txtInfo.text = getString(R.string.total_beads_info, 0)

        editTargetMala.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Convert input to number, multiply by 108, and update the UI
                val malas = s.toString().toIntOrNull() ?: 0
                val total = malas * 108
                txtInfo.text = getString(R.string.total_beads_info, total)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.btn_add_new))
            .setView(view)
            .setPositiveButton(getString(R.string.btn_save)) { _, _ ->
                val name = editName.text.toString()
                val targetInput = editTargetMala.text.toString().toIntOrNull() ?: 1
                if (name.isNotBlank()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.mantraDao().insertMantra(Mantra(name = name, target = targetInput, isFixed = false))
                    }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
}