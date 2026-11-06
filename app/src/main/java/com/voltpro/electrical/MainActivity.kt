package com.voltpro.electrical

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*

private data class Field(val key: String, val title: String, val hint: String, val optional: Boolean = false)
private data class Option(val key: String, val title: String, val items: List<String>, val default: String)
private data class Tool(val id: String, val title: String, val about: String, val fields: List<Field>, val options: List<Option> = emptyList())

class MainActivity : Activity() {
    private val backgroundColor = Color.rgb(11, 18, 32)
    private val cardColor = Color.rgb(24, 36, 56)
    private val inputColor = Color.rgb(35, 49, 72)
    private val teal = Color.rgb(18, 184, 166)
    private val white = Color.rgb(244, 248, 252)
    private val gray = Color.rgb(177, 192, 210)
    private lateinit var body: LinearLayout
    private lateinit var toolPicker: Spinner
    private lateinit var form: LinearLayout
    private lateinit var output: TextView
    private val editBoxes = linkedMapOf<String, EditText>()
    private val optionSpinners = linkedMapOf<String, Spinner>()

    private val singleThree = Option("phase", "نوع سیستم", listOf("تک‌فاز", "سه‌فاز"), "سه‌فاز")
    private val material = Option("material", "جنس هادی", listOf("مس", "آلومینیوم"), "مس")
    private val tools = listOf(
        Tool("ohm", "قانون اهم", "با واردکردن دو مقدار، مقدار سوم محاسبه می‌شود.", listOf(
            Field("voltage", "ولتاژ (V)", "مقدار مجهول را خالی بگذارید"),
            Field("current", "جریان (A)", "مقدار مجهول را خالی بگذارید"),
            Field("resistance", "مقاومت (Ω)", "مقدار مجهول را خالی بگذارید")
        )),
        Tool("single_current", "جریان بار تک‌فاز", "محاسبه جریان با ضریب توان و بازده.", listOf(
            Field("power", "توان اکتیو (W)", "مثلاً 2000"), Field("voltage", "ولتاژ (V)", "مثلاً 230"),
            Field("pf", "ضریب توان", "مثلاً 0.9"), Field("efficiency", "بازده (اختیاری)", "خالی = 1", true)
        )),
        Tool("three_current", "جریان بار سه‌فاز", "محاسبه جریان خط برای بار سه‌فاز متعادل.", listOf(
            Field("power", "توان اکتیو (W)", "مثلاً 7500"), Field("voltage", "ولتاژ خط (V)", "مثلاً 400"),
            Field("pf", "ضریب توان", "مثلاً 0.85"), Field("efficiency", "بازده (اختیاری)", "خالی = 1", true)
        )),
        Tool("three_power", "توان‌های سه‌فاز", "محاسبه توان اکتیو، ظاهری و راکتیو.", listOf(
            Field("voltage", "ولتاژ خط (V)", "مثلاً 400"), Field("current", "جریان خط (A)", "مثلاً 12"), Field("pf", "ضریب توان", "مثلاً 0.85")
        )),
        Tool("voltage_drop", "افت ولتاژ کابل", "برآورد افت ولتاژ با مدل مقاومتی ساده.", listOf(
            Field("current", "جریان بار (A)", "مثلاً 20"), Field("length", "طول یک‌طرفه (m)", "مثلاً 35"),
            Field("area", "سطح مقطع (mm²)", "مثلاً 4"), Field("voltage", "ولتاژ سیستم (V)", "مثلاً 230 یا 400"),
            Field("pf", "ضریب توان (اختیاری)", "خالی = 1", true)
        ), listOf(singleThree, material)),
        Tool("cable_size", "پیشنهاد سطح مقطع کابل", "تخمین سطح مقطع بر اساس افت ولتاژ مجاز؛ جریان‌دهی باید جداگانه کنترل شود.", listOf(
            Field("current", "جریان بار (A)", "مثلاً 25"), Field("length", "طول یک‌طرفه (m)", "مثلاً 40"),
            Field("voltage", "ولتاژ سیستم (V)", "مثلاً 400"), Field("max_drop", "حداکثر افت مجاز (%)", "مثلاً 3"),
            Field("pf", "ضریب توان (اختیاری)", "خالی = 1", true)
        ), listOf(singleThree, material)),
        Tool("resistance", "مقاومت معادل", "محاسبه مقاومت معادل دو مقاومت سری یا موازی.", listOf(
            Field("r1", "مقاومت اول (Ω)", "مثلاً 10"), Field("r2", "مقاومت دوم (Ω)", "مثلاً 20")
        ), listOf(Option("connection", "نوع اتصال", listOf("سری", "موازی"), "سری"))),
        Tool("capacitor", "اصلاح ضریب توان", "محاسبه kVAr تقریبی بانک خازنی.", listOf(
            Field("power_kw", "توان اکتیو (kW)", "مثلاً 30"), Field("pf1", "ضریب توان فعلی", "مثلاً 0.75"),
            Field("pf2", "ضریب توان هدف", "مثلاً 0.95"), Field("frequency", "فرکانس (Hz، اختیاری)", "خالی = 50", true)
        )),
        Tool("transformer", "ظرفیت ترانسفورماتور", "محاسبه توان ظاهری بر حسب kVA.", listOf(
            Field("voltage", "ولتاژ (V)", "مثلاً 400"), Field("current", "جریان (A)", "مثلاً 50")
        ), listOf(singleThree)),
        Tool("motor_speed", "سرعت موتور القایی", "محاسبه سرعت سنکرون و لغزش موتور.", listOf(
            Field("frequency", "فرکانس (Hz)", "مثلاً 50"), Field("poles", "تعداد قطب", "مثلاً 2، 4، 6 یا 8"),
            Field("rated_rpm", "سرعت نامی (rpm، اختیاری)", "برای محاسبه لغزش", true)
        )),
        Tool("energy", "مصرف انرژی و هزینه", "محاسبه مصرف kWh و هزینه تقریبی.", listOf(
            Field("power_kw", "توان (kW)", "مثلاً 2.5"), Field("hours", "مدت کارکرد (ساعت)", "مثلاً 8"),
            Field("tariff", "تعرفه هر kWh (اختیاری)", "خالی = فقط انرژی", true)
        )),
        Tool("star_delta", "ولتاژ سیم‌پیچ ستاره/مثلث", "محاسبه ولتاژ هر سیم‌پیچ از ولتاژ خط.", listOf(
            Field("line_voltage", "ولتاژ خط شبکه (V)", "مثلاً 400")
        ))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        makeLayout()
        renderTool(0)
    }

    private fun d(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun bg(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = d(radius).toFloat() }

    private fun txt(value: String, size: Float, color: Int = white, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); gravity = Gravity.RIGHT
        textDirection = View.TEXT_DIRECTION_RTL
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun makeLayout() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(backgroundColor); setPadding(d(18), d(18), d(18), d(12)) }
        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false }
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, d(14)) }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        body.addView(txt("VOLTPRO  /  ELECTRICAL TOOLKIT", 12f, teal, true))
        body.addView(txt("دستیار محاسبات برق", 27f, white, true).apply { setPadding(0, d(6), 0, d(3)) })
        body.addView(txt("ابزار کاربردی برق صنعتی و ساختمان • آفلاین", 13f, gray))
        body.addView(gap(16))

        val selectorCard = panel()
        selectorCard.addView(txt("انتخاب ابزار محاسباتی", 14f, gray, true))
        toolPicker = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, tools.map { it.title })
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = renderTool(position)
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        selectorCard.addView(toolPicker)
        body.addView(selectorCard)
        body.addView(gap(12))

        form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(form)

        val resultCard = panel()
        resultCard.addView(txt("نتیجه محاسبه", 15f, teal, true))
        output = txt("مقادیر را وارد کنید و دکمه محاسبه را بزنید.", 15f)
        output.setLineSpacing(d(5).toFloat(), 1f)
        output.setPadding(0, d(10), 0, 0)
        resultCard.addView(output)
        body.addView(resultCard)
        body.addView(gap(12))
        body.addView(txt("هشدار مهندسی: نتایج این برنامه ابزار کمکی و آموزشی هستند، نه جایگزین استانداردهای برق، پلاک تجهیزات یا تأیید متخصص. پیش از اجرا، انتخاب کابل و تجهیزات حفاظتی باید با شرایط واقعی نصب بررسی شود.", 12f, gray))
        root.addView(txt("VoltPro • نسخه 1.0.0", 11f, gray).apply { gravity = Gravity.CENTER })
    }

    private fun panel() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = bg(cardColor, 18); setPadding(d(16), d(14), d(16), d(14)) }
    private fun gap(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, d(height)) }

    private fun renderTool(index: Int) {
        if (!::form.isInitialized) return
        val tool = tools[index.coerceIn(tools.indices)]
        form.removeAllViews(); editBoxes.clear(); optionSpinners.clear()

        val intro = panel()
        intro.addView(txt(tool.title, 19f, white, true))
        intro.addView(txt(tool.about, 12f, gray).apply { setPadding(0, d(5), 0, 0) })
        form.addView(intro); form.addView(gap(10))

        val inputPanel = panel()
        inputPanel.addView(txt("اطلاعات ورودی", 15f, teal, true))
        tool.fields.forEach { field ->
            inputPanel.addView(txt(if (field.optional) "${field.title} • اختیاری" else field.title, 13f, gray, true).apply { setPadding(0, d(12), 0, d(5)) })
            val edit = EditText(this).apply {
                hint = field.hint; textSize = 15f; setTextColor(white); setHintTextColor(Color.rgb(125, 143, 165))
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
                singleLine = true; gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                textDirection = View.TEXT_DIRECTION_LTR; setPadding(d(12), d(8), d(12), d(8)); background = bg(inputColor, 10)
            }
            inputPanel.addView(edit, LinearLayout.LayoutParams(-1, d(50)))
            editBoxes[field.key] = edit
        }
        tool.options.forEach { option ->
            inputPanel.addView(txt(option.title, 13f, gray, true).apply { setPadding(0, d(13), 0, d(4)) })
            val spinner = Spinner(this)
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, option.items)
            spinner.setSelection(option.items.indexOf(option.default).coerceAtLeast(0))
            inputPanel.addView(spinner)
            optionSpinners[option.key] = spinner
        }
        form.addView(inputPanel); form.addView(gap(10))

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val calculate = Button(this).apply {
            text = "محاسبه کن"; isAllCaps = false; setTextColor(Color.WHITE); background = bg(teal, 11)
            setOnClickListener { calculate(tool) }
        }
        val clear = Button(this).apply {
            text = "پاک کردن"; isAllCaps = false; setTextColor(white); background = bg(inputColor, 11)
            setOnClickListener { editBoxes.values.forEach { it.text.clear() }; output.text = "مقادیر پاک شدند." }
        }
        buttons.addView(calculate, LinearLayout.LayoutParams(0, d(52), 1f).apply { marginEnd = d(5) })
        buttons.addView(clear, LinearLayout.LayoutParams(0, d(52), 1f).apply { marginStart = d(5) })
        form.addView(buttons)
        output.text = "مقادیر را وارد کنید و دکمه محاسبه را بزنید."
        output.setTextColor(white)
    }

    private fun normalizeNumber(raw: String): String = raw
        .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3').replace('۴', '4')
        .replace('۵', '5').replace('۶', '6').replace('۷', '7').replace('۸', '8').replace('۹', '9')
        .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3').replace('٤', '4')
        .replace('٥', '5').replace('٦', '6').replace('٧', '7').replace('٨', '8').replace('٩', '9')
        .replace(',', '.')

    private fun calculate(tool: Tool) {
        try {
            val values = linkedMapOf<String, Double>()
            tool.fields.forEach { field ->
                val raw = normalizeNumber(editBoxes[field.key]?.text?.toString()?.trim().orEmpty())
                if (raw.isNotEmpty()) {
                    val number = raw.toDoubleOrNull() ?: throw IllegalArgumentException("مقدار «${field.title}» معتبر نیست.")
                    require(number.isFinite()) { "مقدار واردشده معتبر نیست." }
                    values[field.key] = number
                } else if (!field.optional && tool.id != "ohm") {
                    throw IllegalArgumentException("فیلد «${field.title}» را تکمیل کنید.")
                }
            }
            val options = optionSpinners.mapValues { it.value.selectedItem?.toString().orEmpty() }
            output.text = CalculatorEngine.calculate(tool.id, values, options)
            output.setTextColor(white)
        } catch (e: IllegalArgumentException) {
            output.text = "خطا در ورودی‌ها:\n${e.message ?: "مقادیر را بررسی کنید."}"
            output.setTextColor(Color.rgb(255, 170, 115))
        } catch (_: Exception) {
            output.text = "خطای غیرمنتظره رخ داد. ورودی‌ها را بررسی کنید."
            output.setTextColor(Color.rgb(255, 170, 115))
        }
    }
}
