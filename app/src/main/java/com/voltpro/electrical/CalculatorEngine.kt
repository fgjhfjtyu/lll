package com.voltpro.electrical

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.tan

/** هسته محاسباتی VoltPro؛ خروجی‌ها برای برآورد و آموزش هستند. */
object CalculatorEngine {
    fun calculate(id: String, values: Map<String, Double>, options: Map<String, String>): String {
        fun v(key: String): Double = values[key] ?: throw IllegalArgumentException("مقدار «$key» وارد نشده است.")
        fun optional(key: String, default: Double) = values[key] ?: default
        fun positive(key: String): Double = v(key).also {
            require(it > 0.0) { "همه مقادیر باید بزرگ‌تر از صفر باشند." }
        }
        fun pf(key: String): Double = v(key).also {
            require(it > 0.0 && it <= 1.0) { "ضریب توان باید بیشتر از صفر و حداکثر ۱ باشد." }
        }
        fun factor() = if (options["phase"] == "سه‌فاز") sqrt(3.0) else 2.0
        fun rho() = if (options["material"] == "آلومینیوم") 0.0282 else 0.0175

        return when (id) {
            "ohm" -> {
                val keys = listOf("voltage", "current", "resistance")
                val present = keys.filter { values.containsKey(it) }
                require(present.size == 2) { "دقیقاً دو مقدار از ولتاژ، جریان و مقاومت را وارد کنید." }
                val voltage = values["voltage"]
                val current = values["current"]
                val resistance = values["resistance"]
                when {
                    voltage == null -> {
                        require(current!! > 0 && resistance!! > 0) { "جریان و مقاومت باید مثبت باشند." }
                        "ولتاژ: ${fmt(current!! * resistance!!)} V\nV = I × R"
                    }
                    current == null -> {
                        require(voltage > 0 && resistance!! > 0) { "ولتاژ و مقاومت باید مثبت باشند." }
                        "جریان: ${fmt(voltage / resistance!!)} A\nI = V ÷ R"
                    }
                    else -> {
                        require(voltage > 0 && current > 0) { "ولتاژ و جریان باید مثبت باشند." }
                        "مقاومت: ${fmt(voltage / current)} Ω\nR = V ÷ I"
                    }
                }
            }
            "single_current" -> {
                val p = positive("power"); val u = positive("voltage"); val powerFactor = pf("pf")
                val efficiency = optional("efficiency", 1.0)
                require(efficiency > 0.0 && efficiency <= 1.0) { "بازده باید بیشتر از صفر و حداکثر ۱ باشد." }
                "جریان تقریبی تک‌فاز: ${fmt(p / (u * powerFactor * efficiency))} A\nتوان ظاهری: ${fmt(p / powerFactor)} VA"
            }
            "three_current" -> {
                val p = positive("power"); val u = positive("voltage"); val powerFactor = pf("pf")
                val efficiency = optional("efficiency", 1.0)
                require(efficiency > 0.0 && efficiency <= 1.0) { "بازده باید بیشتر از صفر و حداکثر ۱ باشد." }
                "جریان تقریبی سه‌فاز: ${fmt(p / (sqrt(3.0) * u * powerFactor * efficiency))} A\nتوان ظاهری: ${fmt(p / powerFactor)} VA"
            }
            "three_power" -> {
                val u = positive("voltage"); val i = positive("current"); val powerFactor = pf("pf")
                val apparent = sqrt(3.0) * u * i
                val active = apparent * powerFactor
                val reactive = sqrt((apparent * apparent) - (active * active))
                "توان اکتیو: ${fmt(active / 1000.0)} kW\nتوان ظاهری: ${fmt(apparent / 1000.0)} kVA\nتوان راکتیو: ${fmt(reactive / 1000.0)} kVAr"
            }
            "voltage_drop" -> {
                val i = positive("current"); val length = positive("length"); val area = positive("area")
                val u = positive("voltage"); val powerFactor = optional("pf", 1.0)
                require(powerFactor > 0.0 && powerFactor <= 1.0) { "ضریب توان باید بیشتر از صفر و حداکثر ۱ باشد." }
                val drop = factor() * length * i * rho() / area * powerFactor
                val percent = drop / u * 100.0
                "افت ولتاژ تقریبی: ${fmt(drop)} V\nدرصد افت: ${fmt(percent)} %\nولتاژ انتهای مسیر: ${fmt(u - drop)} V\n\nمدل مقاومتی ساده است؛ راکتانس کابل و شرایط واقعی نصب در آن منظور نشده‌اند."
            }
            "cable_size" -> {
                val i = positive("current"); val length = positive("length"); val u = positive("voltage")
                val maxDrop = positive("max_drop"); val powerFactor = optional("pf", 1.0)
                require(maxDrop < 100.0) { "درصد افت مجاز باید کمتر از ۱۰۰ باشد." }
                require(powerFactor > 0.0 && powerFactor <= 1.0) { "ضریب توان باید بیشتر از صفر و حداکثر ۱ باشد." }
                val allowed = u * maxDrop / 100.0
                val required = factor() * length * i * rho() * powerFactor / allowed
                val standard = listOf(1.5, 2.5, 4.0, 6.0, 10.0, 16.0, 25.0, 35.0, 50.0, 70.0, 95.0, 120.0, 150.0, 185.0, 240.0, 300.0)
                val pick = standard.firstOrNull { it >= required }?.let { "${fmt(it)} mm²" } ?: "بالاتر از 300 mm²"
                "حداقل سطح مقطع محاسباتی: ${fmt(required)} mm²\nپیشنهاد اولیه از سری متداول: $pick\n\nاین انتخاب فقط بر اساس افت ولتاژ است؛ ظرفیت جریان‌دهی، دما، روش نصب، گروه‌بندی، اتصال کوتاه و حفاظت باید جداگانه بررسی شوند."
            }
            "resistance" -> {
                val r1 = positive("r1"); val r2 = positive("r2")
                if (options["connection"] == "موازی") "مقاومت معادل موازی: ${fmt(r1 * r2 / (r1 + r2))} Ω\nR = (R1 × R2) ÷ (R1 + R2)"
                else "مقاومت معادل سری: ${fmt(r1 + r2)} Ω\nR = R1 + R2"
            }
            "capacitor" -> {
                val p = positive("power_kw"); val old = pf("pf1"); val target = pf("pf2")
                require(target > old) { "ضریب توان هدف باید از مقدار فعلی بیشتر باشد." }
                val frequency = optional("frequency", 50.0)
                require(frequency > 0.0) { "فرکانس باید مثبت باشد." }
                val q = p * (tan(acos(old)) - tan(acos(target)))
                "ظرفیت راکتیو موردنیاز: ${fmt(q)} kVAr\nفرکانس مبنا: ${fmt(frequency)} Hz\n\nپله‌بندی، هارمونیک، جریان هجومی و رزونانس بانک خازنی باید بررسی شوند."
            }
            "transformer" -> {
                val u = positive("voltage"); val i = positive("current")
                val kva = if (options["phase"] == "سه‌فاز") sqrt(3.0) * u * i / 1000.0 else u * i / 1000.0
                "توان ظاهری ترانس: ${fmt(kva)} kVA"
            }
            "motor_speed" -> {
                val f = positive("frequency"); val poles = positive("poles")
                require(poles % 2.0 == 0.0) { "تعداد قطب باید عدد زوج باشد." }
                val sync = 120.0 * f / poles
                val rated = values["rated_rpm"]
                if (rated == null) "سرعت سنکرون: ${fmt(sync)} rpm\nبرای محاسبه لغزش، سرعت نامی را وارد کنید."
                else {
                    require(rated > 0.0 && rated < sync) { "سرعت نامی باید مثبت و کمتر از سرعت سنکرون باشد." }
                    "سرعت سنکرون: ${fmt(sync)} rpm\nسرعت نامی: ${fmt(rated)} rpm\nلغزش: ${fmt((sync - rated) / sync * 100.0)} %"
                }
            }
            "energy" -> {
                val kw = positive("power_kw"); val hours = positive("hours"); val tariff = optional("tariff", 0.0)
                require(tariff >= 0.0) { "تعرفه نمی‌تواند منفی باشد." }
                "انرژی مصرفی: ${fmt(kw * hours)} kWh\nهزینه تخمینی: ${fmt(kw * hours * tariff)}\n(واحد هزینه مطابق تعرفه واردشده است.)"
            }
            "star_delta" -> {
                val u = positive("line_voltage")
                "ولتاژ هر سیم‌پیچ در ستاره: ${fmt(u / sqrt(3.0))} V\nولتاژ هر سیم‌پیچ در مثلث: ${fmt(u)} V\n\nقبل از تغییر سربندی، پلاک موتور و ولتاژ مجاز سیم‌پیچ‌ها را کنترل کنید."
            }
            else -> throw IllegalArgumentException("ابزار محاسبه شناخته نشد.")
        }
    }

    private fun fmt(number: Double): String {
        require(number.isFinite()) { "نتیجه محاسبه معتبر نیست." }
        return String.format(java.util.Locale.US, "%.3f", number).trimEnd('0').trimEnd('.')
    }
}
