package io.github.alxiw.reactivecurrencies.presentation.currency

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import io.github.alxiw.reactivecurrencies.presentation.R
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max

class CurrencyCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val res: Resources = resources
    private val density get() = resources.displayMetrics.density

    /** Reads a dimension attr and converts it to dp (used for text sizes). */
    private fun TypedArray.dimenAttrDp(index: Int, @DimenRes defRes: Int) =
        getDimension(index, res.getDimension(defRes)) / density

    /** Reads a color attr, falling back to a color resource. */
    private fun TypedArray.colorAttr(index: Int, @ColorRes defRes: Int) =
        getColor(index, ContextCompat.getColor(context, defRes))

    private val innerPadding = res.getDimension(R.dimen.inner_padding)
    private val doubleInnerPadding = innerPadding * 2
    private val halfInnerPadding = innerPadding / 2
    private val threeFourthsInnerPadding = innerPadding + innerPadding / 2

    lateinit var value: EditText
    private lateinit var iconFlag: AppCompatTextView
    private lateinit var shortName: TextView
    private lateinit var longName: TextView
    private lateinit var sign: TextView

    init {
        context.obtainStyledAttributes(attrs, R.styleable.CurrencyCardView).use { array ->
            iconFlag = AppCompatTextView(context).apply {
                textSize = array.dimenAttrDp(R.styleable.CurrencyCardView_iconSize, R.dimen.icon_size)
                setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
            }
            addView(iconFlag)

            shortName = TextView(context).apply {
                textSize = array.dimenAttrDp(
                    R.styleable.CurrencyCardView_shortNameTextSize,
                    R.dimen.short_text_size
                )
                setTextColor(
                    array.colorAttr(R.styleable.CurrencyCardView_shortNameTextColor, R.color.shortNameTextColor)
                )
                maxLines = 1
            }
            addView(shortName)

            longName = TextView(context).apply {
                textSize = array.dimenAttrDp(
                    R.styleable.CurrencyCardView_longNameTextSize,
                    R.dimen.long_text_size
                )
                setTextColor(
                    array.colorAttr(R.styleable.CurrencyCardView_longNameTextColor, R.color.longNameTextColor)
                )
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            addView(longName)

            value = EditText(context).apply {
                textSize = array.dimenAttrDp(
                    R.styleable.CurrencyCardView_valueTextSize,
                    R.dimen.value_text_size
                )
                setTextColor(
                    array.colorAttr(R.styleable.CurrencyCardView_valueTextColor, R.color.valueTextColor)
                )
                maxLines = 1
                filters = arrayOf<InputFilter>(LengthFilter(MAX_VALUE_LENGTH))
                inputType = InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL or
                        InputType.TYPE_NUMBER_FLAG_SIGNED
                imeOptions = EditorInfo.IME_ACTION_UNSPECIFIED
                val enabled = array.getBoolean(R.styleable.CurrencyCardView_enableInput, false)
                isEnabled = enabled
                if (!enabled) {
                    background = null
                }
            }
            addView(value)

            sign = TextView(context).apply {
                textSize = array.dimenAttrDp(
                    R.styleable.CurrencyCardView_signTextSize,
                    R.dimen.sign_text_size
                )
                setTextColor(
                    array.colorAttr(R.styleable.CurrencyCardView_signTextColor, R.color.signTextColor)
                )
                maxLines = 1
            }
            addView(sign)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        measureChild(shortName, widthMeasureSpec, heightMeasureSpec)
        measureChild(longName, widthMeasureSpec, heightMeasureSpec)
        measureChild(iconFlag, widthMeasureSpec, heightMeasureSpec)
        measureChild(sign, widthMeasureSpec, heightMeasureSpec)
        measureChild(value, widthMeasureSpec, heightMeasureSpec)

        val iconHeight = iconFlag.measuredHeight.toFloat()
        val nameTextBlockHeight = doubleInnerPadding +
                longName.measuredHeight +
                halfInnerPadding +
                shortName.measuredHeight +
                doubleInnerPadding
        val height = paddingTop + max(iconHeight, nameTextBlockHeight) + paddingBottom
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height.toInt())
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val centerY = (bottom - top) / 2
        val measuredIconSize = iconFlag.measuredHeight
        val halfIconSize = measuredIconSize / 2
        val contentStartX = doubleInnerPadding.toInt() + measuredIconSize + innerPadding.toInt()

        iconFlag.layout(
            doubleInnerPadding.toInt(),
            centerY - halfIconSize,
            doubleInnerPadding.toInt() + measuredIconSize,
            centerY + halfIconSize
        )

        longName.layout(
            contentStartX,
            doubleInnerPadding.toInt(),
            contentStartX + longName.measuredWidth,
            doubleInnerPadding.toInt() + longName.measuredHeight
        )

        shortName.layout(
            contentStartX,
            doubleInnerPadding.toInt() + longName.measuredHeight + halfInnerPadding.toInt(),
            contentStartX + shortName.measuredWidth,
            doubleInnerPadding.toInt() + longName.measuredHeight +
                    halfInnerPadding.toInt() + shortName.measuredHeight
        )

        val signAndValueCenterY = centerY + threeFourthsInnerPadding.toInt()
        val rightEdgeWithPadding = right - doubleInnerPadding.toInt()

        sign.layout(
            rightEdgeWithPadding - sign.measuredWidth,
            signAndValueCenterY - sign.measuredHeight / 2,
            rightEdgeWithPadding,
            signAndValueCenterY + sign.measuredHeight / 2
        )

        val valueRightEdge = rightEdgeWithPadding - sign.measuredWidth
        value.layout(
            valueRightEdge - value.measuredWidth,
            signAndValueCenterY - value.measuredHeight / 2,
            valueRightEdge,
            signAndValueCenterY + value.measuredHeight / 2
        )
    }

    fun setLongName(name: String) {
        longName.text = name
    }

    fun setShortName(name: String) {
        shortName.text = name
    }

    fun setIcon(icon: String) {
        iconFlag.text = icon
    }

    fun setValue(sum: BigDecimal) {
        val scaled = sum.setScale(ROUND_VALUE_AFTER_DOT, RoundingMode.HALF_UP)
        value.setText(scaled.toPlainString())
    }

    fun setSign(signText: String) {
        sign.text = signText
    }

    companion object {
        private const val ROUND_VALUE_AFTER_DOT = 2
        private const val MAX_VALUE_LENGTH = 20
    }
}
