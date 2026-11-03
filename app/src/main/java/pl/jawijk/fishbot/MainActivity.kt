package pl.jawijk.fishbot

import android.Manifest
import android.app.*
import android.os.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("fish_data", MODE_PRIVATE)
    }

    private lateinit var fishText: TextView
    private lateinit var timerText: TextView
    private lateinit var historyText: TextView
    private lateinit var minutesInput: EditText

    private var countdown: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                42
            )
        }

        createScreen()
        updateScreen()

        val end = prefs.getLong("timer_end", 0L)

        if (end > System.currentTimeMillis()) {
            runTimer(end)
        }
    }

    private fun createScreen() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(25, 30, 25, 25)
            setBackgroundColor(Color.rgb(16, 24, 32))
        }

        fun addLabel(text: String, size: Float): TextView {
            return TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(5, 12, 5, 12)
                layout.addView(this)
            }
        }

        fun addButton(text: String, action: () -> Unit) {
            val button = Button(this)
            button.text = text
            button.setOnClickListener { action() }

            layout.addView(
                button,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        addLabel("🎣 JAWIJK FISH BOT", 26f)
        addLabel("WAR OF DRAGONS", 17f)

        fishText = addLabel("Złowione ryby: 0", 22f)

        addButton("+ DODAJ RYBĘ") {
            val count = prefs.getInt("fish_count", 0) + 1
            prefs.edit().putInt("fish_count", count).apply()
            addHistory("Złowiono rybę")
            updateScreen()
        }

        addLabel("CZAS PRZYPOMNIENIA (MINUTY)", 16f)

        minutesInput = EditText(this).apply {
            setText("15")
            inputType = 2
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        layout.addView(minutesInput)

        timerText = addLabel("Timer nieaktywny", 22f)

        addButton("▶ START") {
            startTimer()
        }

        addButton("■ STOP") {
            stopTimer()
        }

        addButton("WYZERUJ LICZNIK") {
            prefs.edit().putInt("fish_count", 0).apply()
            addHistory("Wyzerowano licznik")
            updateScreen()
        }

        addLabel("HISTORIA POŁOWÓW", 19f)

        historyText = addLabel("Brak wpisów", 13f)

        addLabel(
            "Aplikacja służy do ręcznego łowienia. " +
            "Nie wykonuje automatycznych działań w grze.",
            12f
        )

        setContentView(
            ScrollView(this).apply {
                addView(layout)
            }
        )
    }

    private fun updateScreen() {
        fishText.text =
            "Złowione ryby: ${prefs.getInt("fish_count", 0)}"

        historyText.text =
            prefs.getString("history", "Brak wpisów")
    }

    private fun addHistory(message: String) {

        val date = SimpleDateFormat(
            "dd.MM HH:mm",
            Locale.getDefault()
        ).format(Date())

        val old = prefs.getString("history", "") ?: ""

        val newHistory =
            "$date - $message\n$old"
                .lines()
                .take(12)
                .joinToString("\n")

        prefs.edit()
            .putString("history", newHistory)
            .apply()
    }

    private fun startTimer() {

        val minutes = minutesInput.text.toString().toLongOrNull()

        if (minutes == null || minutes !in 1..1440) {
            Toast.makeText(
                this,
                "Podaj czas od 1 do 1440 minut",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val end =
            System.currentTimeMillis() + minutes * 60000L

        prefs.edit().putLong("timer_end", end).apply()

        scheduleNotification(end)
        runTimer(end)

        addHistory("Ustawiono timer: $minutes minut")
    }

    private fun scheduleNotification(end: Long) {

        val intent = Intent(this, TimerReceiver::class.java)

        val pending = PendingIntent.getBroadcast(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val alarm = getSystemService(ALARM_SERVICE) as AlarmManager

        alarm.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            end,
            pending
        )
    }

    private fun stopTimer() {

        countdown?.cancel()

        prefs.edit().remove("timer_end").apply()

        val intent = Intent(this, TimerReceiver::class.java)

        val pending = PendingIntent.getBroadcast(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val alarm = getSystemService(ALARM_SERVICE) as AlarmManager
        alarm.cancel(pending)

        timerText.text = "Timer zatrzymany"

        addHistory("Zatrzymano timer")
    }

    private fun runTimer(end: Long) {

        countdown?.cancel()

        countdown = object : CountDownTimer(
            (end - System.currentTimeMillis()).coerceAtLeast(0),
            1000
        ) {

            override fun onTick(left: Long) {

                val seconds = left / 1000

                timerText.text = String.format(
                    Locale.getDefault(),
                    "Pozostało: %02d:%02d",
                    seconds / 60,
                    seconds % 60
                )
            }

            override fun onFinish() {

                timerText.text = "Czas minął! Sprawdź połów."

                prefs.edit().remove("timer_end").apply()
            }
        }.start()
    }
}
