package io.c8y.scripts.support

import com.google.common.base.Preconditions
import java.util.*

object Metrics {
    fun gauge(name:String): RichGauge {
        return RichGauge(name);
    }
    fun counter(name:String): RichGauge {
        return RichGauge(name);
    }
}

class RichGauge {
    val name: String

    /**
     * Return the current value of the gauge.
     */
    var value: Double
        private set

    /**
     * Return either an exponential weighted moving average or a simple mean,
     * respectively, depending on whether the weight 'alpha' has been set for this gauge.
     *
     * @return The average over all the accumulated values
     */
    var average: Double
        private set
    var max: Double
        private set
    var min: Double
        private set
    var count: Long
        private set

    /**
     * Return the alpha smoothing constant value.
     */
    var alpha: Double
        private set

    /**
     * Creates an "empty" gauge. The average, max and min will be zero, but this initial
     * value will not be included after the first value has been set on the gauge.
     *
     * @param name the name under which the gauge will be stored.
     */
    constructor(name: String) : this(name, 0.0) {
        count = 0
    }

    constructor(name: String, value: Double) {
        Objects.requireNonNull(name, "The gauge name cannot be null or empty")
        this.name = name
        this.value = value
        average = this.value
        min = this.value
        max = this.value
        alpha = -1.0
        count = 1
    }

    constructor(
        name: String, value: Double, alpha: Double, mean: Double, max: Double,
        min: Double, count: Long
    ) {
        this.name = name
        this.value = value
        this.alpha = alpha
        average = mean
        this.max = max
        this.min = min
        this.count = count
    }

    fun setAlpha(alpha: Double): RichGauge {
        Preconditions.checkArgument(
            alpha == -1.0 || alpha > 0.0 && alpha < 1.0,
            "Smoothing constant must be between 0 and 1, or -1 to use arithmetic mean"
        )
        this.alpha = alpha
        return this
    }

    fun aggregate(current: Double) {
        if (count == 0L) {
            max = current
            min = current
        } else if (current > max) {
            max = current
        } else if (current < min) {
            min = current
        }
        if (alpha > 0.0 && count > 0) {
            average = alpha * current + (1 - alpha) * average
        } else {
            var sum = average * count
            sum += current
            average = sum / (count + 1)
        }
        count++
        value = current
    }

    fun reset(): RichGauge {
        value = 0.0
        max = 0.0
        min = 0.0
        average = 0.0
        count = 0
        return this
    }

    override fun toString(): String {
        val sb = StringBuilder()
        val format = "%100s.%s - %.2f\n"
        sb.append(format.format(name,"val",value))
        sb.append(format.format(name,"avg",average))
        sb.append(format.format(name,"min",min))
        sb.append(format.format(name,"max",max))
        sb.append(format.format(name,"count",count.toDouble()))
        return sb.toString()
    }
}
