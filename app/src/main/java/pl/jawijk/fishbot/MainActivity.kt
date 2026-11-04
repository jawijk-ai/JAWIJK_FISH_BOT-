package pl.jawijk.wodbot

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

data class FishResource(
    val name: String,
    val details: String = "",
    val imageKeys: List<String> = emptyList()
)

class MainActivity : Activity() {

    private lateinit var gameWebView: WebView
    private lateinit var catalogView: WebView
    private lateinit var gameFrame: FrameLayout

    private lateinit var scanStatus: TextView
    private lateinit var fishSpinner: Spinner
    private lateinit var categorySpinner: Spinner
    private lateinit var timeSpinner: Spinner

    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private var popupWebView: WebView? = null
    private var popupContainer: FrameLayout? = null

    private val handler = Handler(Looper.getMainLooper())

    private val basicFish = mutableListOf<FishResource>()
    private val mystrasFish = mutableListOf<FishResource>()

    private var analysisRunning = false
    private var scanInProgress = false
    private var scanInterval = 30000L

    private var selectedCategory = "Ryby podstawowe"
    private var selectedFish = ""

    private var scanCount = 0

    private val scanRunnable = object : Runnable {
        override fun run() {
            if (analysisRunning && !scanInProgress) {
                scanPage()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CookieManager.getInstance().setAcceptCookie(true)

        createInterface()
        configureWebView()

        loadBasicCatalog()

        gameWebView.loadUrl("https://www.warofdragons.com/")
    }

    private fun createInterface() {

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setBackgroundColor(Color.rgb(25, 25, 25))

        val title = TextView(this)
        title.text = "JAWIJK WOD BOT"
        title.textSize = 21f
        title.setTextColor(Color.WHITE)
        title.gravity = android.view.Gravity.CENTER
        title.setPadding(10, 12, 10, 12)
        layout.addView(title)

        categorySpinner = Spinner(this)

        val categories = arrayOf(
            "Ryby podstawowe",
            "Ryby Mystras"
        )

        categorySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        categorySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedCategory = categories[position]
                    showFishList()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        layout.addView(categorySpinner)

        fishSpinner = Spinner(this)
        layout.addView(fishSpinner)

        timeSpinner = Spinner(this)

        val times = arrayOf(
            "1 sekunda",
            "5 sekund",
            "10 sekund",
            "15 sekund",
            "30 sekund",
            "60 sekund",
            "2 minuty",
            "5 minut"
        )

        timeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            times
        )

        timeSpinner.setSelection(4)

        timeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val intervals = longArrayOf(
                        1000L,
                        5000L,
                        10000L,
                        15000L,
                        30000L,
                        60000L,
                        120000L,
                        300000L
                    )

                    scanInterval = intervals[position]
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        layout.addView(timeSpinner)

        val buttons = LinearLayout(this)
        buttons.orientation = LinearLayout.HORIZONTAL

        startButton = Button(this)
        startButton.text = "START"

        stopButton = Button(this)
        stopButton.text = "STOP"

        startButton.setOnClickListener {
            startAnalysis()
        }

        stopButton.setOnClickListener {
            stopAnalysis()
        }

        buttons.addView(
            startButton,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        buttons.addView(
            stopButton,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        layout.addView(buttons)

        scanStatus = TextView(this)
        scanStatus.text = "Pobieranie biblioteki..."
        scanStatus.textSize = 13f
        scanStatus.setTextColor(Color.GREEN)
        scanStatus.setPadding(10, 8, 10, 8)
        scanStatus.setTextIsSelectable(true)

        layout.addView(scanStatus)

        val zoomButtons = LinearLayout(this)
        zoomButtons.orientation = LinearLayout.HORIZONTAL

        val zoomOut = Button(this)
        zoomOut.text = "−"

        val zoomIn = Button(this)
        zoomIn.text = "+"

        zoomOut.setOnClickListener {
            gameWebView.zoomOut()
        }

        zoomIn.setOnClickListener {
            gameWebView.zoomIn()
        }

        zoomButtons.addView(
            zoomOut,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        zoomButtons.addView(
            zoomIn,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        layout.addView(zoomButtons)

        gameFrame = FrameLayout(this)
        gameWebView = WebView(this)

        gameFrame.addView(
            gameWebView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        layout.addView(
            gameFrame,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        catalogView = WebView(this)
        catalogView.visibility = View.GONE

        layout.addView(
            catalogView,
            LinearLayout.LayoutParams(1, 1)
        )

        setContentView(layout)
    }

    private fun configureWebView() {

        val settings = gameWebView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadsImagesAutomatically = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        gameWebView.webViewClient = WebViewClient()
        gameWebView.webChromeClient = createGameChromeClient()

        catalogView.settings.javaScriptEnabled = true
        catalogView.settings.domStorageEnabled = true
        catalogView.settings.loadsImagesAutomatically = true
    }

    private fun createGameChromeClient(): WebChromeClient {

        return object : WebChromeClient() {

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {

                closePopup()

                val popup = WebView(this@MainActivity)

                popup.settings.javaScriptEnabled = true
                popup.settings.domStorageEnabled = true
                popup.settings.databaseEnabled = true
                popup.settings.javaScriptCanOpenWindowsAutomatically = true
                popup.settings.setSupportMultipleWindows(true)
                popup.settings.useWideViewPort = true
                popup.settings.loadWithOverviewMode = true
                popup.settings.builtInZoomControls = true
                popup.settings.displayZoomControls = false

                popup.webViewClient = WebViewClient()
                popup.webChromeClient = createGameChromeClient()

                val container = FrameLayout(this@MainActivity)

                container.addView(
                    popup,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )

                val closeButton = Button(this@MainActivity)
                closeButton.text = "ZAMKNIJ"

                val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                params.gravity =
                    android.view.Gravity.TOP or
                    android.view.Gravity.END

                container.addView(closeButton, params)

                closeButton.setOnClickListener {
                    closePopup()
                }

                gameFrame.addView(
                    container,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )

                popupWebView = popup
                popupContainer = container

                val transport =
                    resultMsg?.obj as? WebView.WebViewTransport

                if (transport == null) {
                    closePopup()
                    return false
                }

                transport.webView = popup
                resultMsg.sendToTarget()

                return true
            }

            override fun onCloseWindow(window: WebView?) {
                closePopup()
            }
        }
    }

    private fun closePopup() {

        val popup = popupWebView
        val container = popupContainer

        popupWebView = null
        popupContainer = null

        try {
            popup?.stopLoading()
            popup?.destroy()
        } catch (_: Exception) {}

        try {
            if (container != null) {
                gameFrame.removeView(container)
            }
        } catch (_: Exception) {}
    }
        private fun loadBasicCatalog() {

        scanStatus.text = "Pobieranie ryb podstawowych..."

        catalogView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {
                if (url?.contains("id=351") == true) {
                    extractBasicFish()
                }
            }
        }

        catalogView.loadUrl(
            "https://warofdragons.com/info/library/index.php?id=351&obj=cat"
        )
    }

    private fun extractBasicFish() {

        val js = """
            (function() {
                let output = [];
                let seen = new Set();

                function clean(v) {
                    return (v || "").replace(/\s+/g, " ").trim();
                }

                function add(name, detail, keys) {
                    name = clean(name);

                    if (!name || name.length < 3 || name.length > 100) {
                        return;
                    }

                    let id = name.toLowerCase();

                    if (seen.has(id)) return;
                    seen.add(id);

                    output.push({
                        name: name,
                        details: detail,
                        keys: keys
                    });
                }

                let tables = Array.from(document.querySelectorAll("table"));

                let table = tables.find(function(t) {
                    let h = (t.innerText || "").toLowerCase();
                    return h.includes("skill level") &&
                           h.includes("magmar locations") &&
                           h.includes("human locations");
                });

                if (!table) {
                    return JSON.stringify({
                        error: "Nie znaleziono tabeli Fisherman resources",
                        items: []
                    });
                }

                let rows = table.querySelectorAll("tr");

                rows.forEach(function(row) {

                    let cells = row.querySelectorAll("td, th");
                    if (cells.length < 2) return;

                    let level = clean(cells[0].innerText);

                    if (!/^[0-9]+$/.test(level)) return;

                    let fishCell = cells[1];

                    let images = fishCell.querySelectorAll("img");

                    images.forEach(function(img) {

                        let a = img.closest("a");

                        let alt = clean(img.getAttribute("alt"));
                        let title = clean(img.getAttribute("title"));

                        let anchorTitle = a
                            ? clean(a.getAttribute("title"))
                            : "";

                        let anchorText = a
                            ? clean(a.innerText || a.textContent)
                            : "";

                        let src = img.src || "";
                        let href = a ? a.href : "";

                        let keys = [
                            src,
                            img.getAttribute("src") || "",
                            href,
                            alt,
                            title,
                            anchorTitle,
                            anchorText
                        ].filter(Boolean);

                        let name = alt || title ||
                                   anchorTitle || anchorText;

                        if (!name) {
                            let match = href.match(/[?&](?:id|item|obj)=([0-9]+)/i);

                            if (match) {
                                name = "Ryba #" + match[1];
                            } else {
                                let file = src.split("/").pop().split("?")[0];
                                file = file.replace(/\.[^.]+$/, "");
                                file = file.replace(/[_-]+/g, " ");

                                if (file && file.length > 2) {
                                    name = "Ikona ryby: " + file;
                                }
                            }
                        }

                        if (name) {
                            add(
                                name,
                                "Poziom umiejętności: " + level,
                                keys
                            );
                        }
                    });
                });

                return JSON.stringify({
                    error: "",
                    items: output
                });
            })();
        """

        catalogView.evaluateJavascript(js) { result ->

            try {
                if (result == null || result == "null") {
                    throw Exception("Brak odpowiedzi biblioteki")
                }

                val decoded =
                    JSONArray("[$result]").getString(0)

                val data = JSONObject(decoded)

                val array = data.optJSONArray("items")
                    ?: JSONArray()

                basicFish.clear()

                for (i in 0 until array.length()) {

                    val item = array.getJSONObject(i)

                    val keysJson =
                        item.optJSONArray("keys") ?: JSONArray()

                    val keys = mutableListOf<String>()

                    for (j in 0 until keysJson.length()) {
                        val key = keysJson.optString(j)
                        if (key.isNotBlank()) keys.add(key)
                    }

                    basicFish.add(
                        FishResource(
                            name = item.optString("name"),
                            details = item.optString("details"),
                            imageKeys = keys
                        )
                    )
                }

                val error = data.optString("error")

                if (error.isNotBlank()) {
                    scanStatus.text = error
                }

                loadMystrasCatalog()

            } catch (e: Exception) {

                basicFish.clear()

                scanStatus.text =
                    "Błąd biblioteki podstawowej: ${e.message}"

                loadMystrasCatalog()
            }
        }
    }

    private fun loadMystrasCatalog() {

        scanStatus.text = "Pobieranie danych Mystras..."

        catalogView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {
                if (url?.contains("id=470") == true) {
                    extractMystrasFish()
                }
            }
        }

        catalogView.loadUrl(
            "https://warofdragons.com/info/library/index.php?id=470&obj=cat"
        )
    }

    private fun extractMystrasFish() {

        val js = """
            (function() {

                let tables = Array.from(
                    document.querySelectorAll("table")
                );

                let table = tables.find(function(t) {
                    let text = (t.innerText || "").toLowerCase();

                    return text.includes("profession") &&
                           text.includes("resource") &&
                           text.includes("fisherman");
                });

                if (!table) {
                    return JSON.stringify({
                        found: false,
                        images: []
                    });
                }

                let images = [];

                table.querySelectorAll("img").forEach(function(img) {

                    let a = img.closest("a");

                    images.push({
                        src: img.src || "",
                        alt: img.alt || "",
                        title: img.title || "",
                        href: a ? a.href : ""
                    });
                });

                return JSON.stringify({
                    found: true,
                    images: images
                });

            })();
        """

        catalogView.evaluateJavascript(js) { result ->

            try {

                val decoded =
                    JSONArray("[$result]").getString(0)

                val data = JSONObject(decoded)

                mystrasFish.clear()

                val keys = mutableListOf(
                    "mystras_serpentfish",
                    "mystras-serpentfish",
                    "serpentfish"
                )

                val images =
                    data.optJSONArray("images") ?: JSONArray()

                for (i in 0 until images.length()) {

                    val image = images.getJSONObject(i)

                    val values = listOf(
                        image.optString("src"),
                        image.optString("alt"),
                        image.optString("title"),
                        image.optString("href")
                    )

                    for (value in values) {
                        if (
                            value.contains(
                                "serpentfish",
                                ignoreCase = true
                            )
                        ) {
                            keys.add(value)
                        }
                    }
                }

                mystrasFish.add(
                    FishResource(
                        name = "Mystras Serpentfish",
                        details = "Mystras - Fisherman",
                        imageKeys = keys.distinct()
                    )
                )

                showFishList()

                scanStatus.text =
                    "Biblioteka gotowa. Podstawowe: ${basicFish.size}, Mystras: ${mystrasFish.size}"

            } catch (e: Exception) {

                mystrasFish.clear()

                mystrasFish.add(
                    FishResource(
                        name = "Mystras Serpentfish",
                        details = "Mystras - Fisherman",
                        imageKeys = listOf(
                            "mystras_serpentfish",
                            "mystras-serpentfish",
                            "serpentfish"
                        )
                    )
                )

                showFishList()

                scanStatus.text =
                    "Mystras: błąd odczytu strony. ${e.message}"
            }
        }
    }

    private fun showFishList() {

        if (!::fishSpinner.isInitialized) return

        val source = if (
            selectedCategory == "Ryby Mystras"
        ) {
            mystrasFish
        } else {
            basicFish
        }

        val names = source.map { it.name }.distinct()

        val displayNames = if (names.isEmpty()) {
            listOf("Brak rozpoznanych pozycji")
        } else {
            names
        }

        fishSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            displayNames
        )

        fishSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    selectedFish =
                        if (names.isNotEmpty()) {
                            names[position]
                        } else {
                            ""
                        }

                    scanStatus.text =
                        if (selectedFish.isNotEmpty()) {
                            "Wybrany surowiec: $selectedFish"
                        } else {
                            "Brak surowców w tej kategorii."
                        }
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }
    }
        private fun startAnalysis() {

        if (analysisRunning) return

        if (selectedFish.isBlank()) {
            scanStatus.text = "Najpierw wybierz surowiec."
            return
        }

        val source = if (
            selectedCategory == "Ryby Mystras"
        ) {
            mystrasFish
        } else {
            basicFish
        }

        if (source.none { it.name == selectedFish }) {
            scanStatus.text =
                "Wybrany surowiec nie znajduje się w bibliotece."
            return
        }

        analysisRunning = true
        scanInProgress = false
        scanCount = 0

        startButton.isEnabled = false
        stopButton.isEnabled = true

        scanStatus.text =
            "Analiza uruchomiona: $selectedFish"

        handler.removeCallbacks(scanRunnable)
        handler.post(scanRunnable)
    }

    private fun stopAnalysis() {

        analysisRunning = false
        scanInProgress = false

        handler.removeCallbacks(scanRunnable)

        clearHighlights()

        startButton.isEnabled = true
        stopButton.isEnabled = true

        scanStatus.text =
            "Analiza zatrzymana. Liczba skanów: $scanCount"
    }

    private fun scanPage() {

        if (!analysisRunning || scanInProgress) return

        val source = if (
            selectedCategory == "Ryby Mystras"
        ) {
            mystrasFish
        } else {
            basicFish
        }

        val selected = source.firstOrNull {
            it.name == selectedFish
        }

        if (selected == null) {
            scanStatus.text =
                "Nie znaleziono wybranego surowca w bazie."
            stopAnalysis()
            return
        }

        scanInProgress = true

        val keysJson = JSONArray()

        selected.imageKeys.forEach {
            keysJson.put(it)
        }

        val selectedName = JSONObject.quote(selected.name)
        val selectedKeys = keysJson.toString()

        val js = """
            (function() {

                let selectedName = $selectedName;
                let keys = $selectedKeys;

                let images = [];
                let canvases = 0;
                let frames = 0;

                function normalize(value) {
                    return (value || "")
                        .toLowerCase()
                        .replace(/[^a-z0-9]/g, "");
                }

                function search(doc) {

                    try {

                        doc.querySelectorAll("img").forEach(function(img) {

                            if (
                                !img.complete ||
                                img.naturalWidth <= 0 ||
                                img.naturalHeight <= 0
                            ) {
                                return;
                            }

                            let rect = img.getBoundingClientRect();

                            if (
                                rect.width < 8 ||
                                rect.height < 8 ||
                                rect.bottom < 0 ||
                                rect.right < 0 ||
                                rect.top > window.innerHeight ||
                                rect.left > window.innerWidth
                            ) {
                                return;
                            }

                            let a = img.closest("a");

                            let values = [
                                img.src,
                                img.getAttribute("src"),
                                img.alt,
                                img.title,
                                a ? a.href : "",
                                a ? a.title : "",
                                a ? a.innerText : ""
                            ];

                            let haystack = normalize(
                                values.filter(Boolean).join(" ")
                            );

                            let matched = false;

                            for (let i = 0; i < keys.length; i++) {

                                let key = keys[i];

                                if (!key) continue;

                                let normalizedKey = normalize(key);

                                if (
                                    normalizedKey.length > 4 &&
                                    haystack.includes(normalizedKey)
                                ) {
                                    matched = true;
                                    break;
                                }

                                let file = key.split("/").pop()
                                    .split("?")[0];

                                let normalizedFile = normalize(file);

                                if (
                                    normalizedFile.length > 5 &&
                                    haystack.includes(normalizedFile)
                                ) {
                                    matched = true;
                                    break;
                                }
                            }

                            if (
                                !matched &&
                                selectedName.toLowerCase().startsWith("ryba #")
                            ) {

                                let id = selectedName
                                    .replace(/[^0-9]/g, "");

                                if (id.length > 0) {
                                    matched = values.some(function(v) {
                                        return (v || "").includes(id);
                                    });
                                }
                            }

                            if (matched) {
                                images.push(img);
                            }
                        });

                        canvases +=
                            doc.querySelectorAll("canvas").length;

                        doc.querySelectorAll("iframe").forEach(function(frame) {

                            frames++;

                            try {
                                if (frame.contentDocument) {
                                    search(frame.contentDocument);
                                }
                            } catch(e) {}
                        });

                    } catch(e) {}
                }

                search(document);

                if (images.length === 0) {
                    return JSON.stringify({
                        found: false,
                        count: 0,
                        canvases: canvases,
                        frames: frames
                    });
                }

                let marked = [];

                images.forEach(function(img) {

                    let oldOutline = img.style.outline;
                    let oldShadow = img.style.boxShadow;

                    img.style.setProperty(
                        "outline",
                        "5px solid red",
                        "important"
                    );

                    img.style.setProperty(
                        "box-shadow",
                        "0 0 20px red",
                        "important"
                    );

                    marked.push({
                        img: img,
                        outline: oldOutline,
                        shadow: oldShadow
                    });
                });

                setTimeout(function() {

                    marked.forEach(function(item) {
                        item.img.style.setProperty(
                            "outline",
                            "none",
                            "important"
                        );

                        item.img.style.setProperty(
                            "box-shadow",
                            "none",
                            "important"
                        );
                    });

                }, 300);

                setTimeout(function() {

                    marked.forEach(function(item) {

                        item.img.style.setProperty(
                            "outline",
                            "5px solid red",
                            "important"
                        );

                        item.img.style.setProperty(
                            "box-shadow",
                            "0 0 20px red",
                            "important"
                        );
                    });

                }, 450);

                setTimeout(function() {

                    marked.forEach(function(item) {

                        item.img.style.outline = item.outline;
                        item.img.style.boxShadow = item.shadow;
                    });

                }, 900);

                return JSON.stringify({
                    found: true,
                    count: images.length,
                    canvases: canvases,
                    frames: frames
                });

            })();
        """

        gameWebView.evaluateJavascript(js) { result ->

            try {

                if (result == null || result == "null") {
                    throw Exception("Brak odpowiedzi strony gry")
                }

                val decoded =
                    JSONArray("[$result]").getString(0)

                val data = JSONObject(decoded)

                val found = data.optBoolean("found")
                val count = data.optInt("count")
                val canvases = data.optInt("canvases")
                val frames = data.optInt("frames")

                scanCount++

                scanStatus.text = if (found) {

                    "ZNALEZIONO PASUJĄCY OBRAZ\n" +
                    "Surowiec: $selectedFish\n" +
                    "Pasujące obrazy: $count\n" +
                    "Skan nr: $scanCount\n" +
                    "Canvas: $canvases\n" +
                    "Ramki: $frames\n" +
                    "Obrazy oznaczono dwukrotnie."

                } else {

                    "NIE ZNALEZIONO PASUJĄCEGO OBRAZU\n" +
                    "Wybrany surowiec: $selectedFish\n" +
                    "Skan nr: $scanCount\n" +
                    "Canvas: $canvases\n" +
                    "Ramki: $frames\n" +
                    "Strona może używać grafiki Canvas/WebGL."
                }

            } catch (e: Exception) {

                scanStatus.text =
                    "Błąd skanowania: ${e.message}"

            } finally {

                scanInProgress = false

                if (analysisRunning) {

                    handler.removeCallbacks(scanRunnable)

                    handler.postDelayed(
                        scanRunnable,
                        scanInterval
                    )
                }
            }
        }
    }

    private fun clearHighlights() {

        val js = """
            (function() {

                document.querySelectorAll("img").forEach(function(img) {
                    img.style.removeProperty("outline");
                    img.style.removeProperty("box-shadow");
                });

                return true;

            })();
        """

        gameWebView.evaluateJavascript(js, null)
        popupWebView?.evaluateJavascript(js, null)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        val popup = popupWebView

        if (popup != null && popup.canGoBack()) {
            popup.goBack()
            return
        }

        if (popup != null) {
            closePopup()
            return
        }

        if (gameWebView.canGoBack()) {
            gameWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {

        analysisRunning = false
        scanInProgress = false

        handler.removeCallbacks(scanRunnable)

        closePopup()

        try {
            gameWebView.destroy()
            catalogView.destroy()
        } catch (_: Exception) {}

        super.onDestroy()
    }
}
