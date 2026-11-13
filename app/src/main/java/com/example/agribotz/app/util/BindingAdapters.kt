package com.example.agribotz.app.util

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import com.example.agribotz.R
import com.example.agribotz.app.domain.ApiStatus
import com.google.android.material.textfield.TextInputLayout
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import com.example.agribotz.app.ui.home.GadgetCardUi

/**
 * API status → shows/hides appropriate icons or animations
 */
@BindingAdapter("apiStatus")
fun setApiStatus(imageView: ImageView, status: ApiStatus?) {
    when (status) {
        ApiStatus.LOADING -> {
            imageView.visibility = View.VISIBLE
            imageView.setImageResource(R.drawable.loading_animation)
        }
        ApiStatus.ERROR -> {
            imageView.visibility = View.VISIBLE
            imageView.setImageResource(R.drawable.ic_connection_error)
        }
        ApiStatus.DONE -> {
            imageView.visibility = View.GONE
        }
        else -> imageView.visibility = View.GONE
    }
}

/**
 * Set TextInputLayout error text
 */
@BindingAdapter("errorText")
fun setErrorMessage(view: TextInputLayout, errorMessage: String?) {
    view.error = errorMessage
}

/**
 * Generic enabled/disabled binding with alpha feedback
 */
@BindingAdapter("enabledState")
fun View.setEnabledState(enabled: Boolean) {
    isEnabled = enabled
    isClickable = enabled
    alpha = if (enabled) 1f else 0.35f
}

/**
 * Visible if true, otherwise GONE
 */
@BindingAdapter("visibleIf")
fun View.visibleIf(condition: Boolean?) {
    isVisible = (condition == true)
}

/**
 * Gone if true, otherwise VISIBLE
 */
@BindingAdapter("goneIf")
fun View.goneIf(condition: Boolean?) {
    visibility = if (condition == true) View.GONE else View.VISIBLE
}

/**
 * Drawable resource id → ImageView image
 * (android:src with an Int would be treated as a color and drawn as a solid rectangle)
 */
@BindingAdapter("imageRes")
fun ImageView.setImageRes(@DrawableRes resId: Int) {
    setImageResource(resId)
}

@BindingAdapter(value = ["statusRes", "statusDate"])
fun TextView.setStatusLine(statusRes: Int, statusDate: String?) {
    text = if (!statusDate.isNullOrBlank()) {
        context.getString(statusRes, statusDate)
    }
    else {
        "" // hide text if no date
    }
}

@BindingAdapter("fadeVisible")
fun View.fadeVisible(visible: Boolean) {
    val duration = 300L
    if (visible && this.visibility != View.VISIBLE) {
        this.alpha = 0f
        this.visibility = View.VISIBLE
        animate().alpha(1f).setDuration(duration).start()
    } else if (!visible && this.isVisible) {
        animate().alpha(0f).setDuration(duration).withEndAction {
            this.visibility = View.GONE
        }.start()
    }
}

object ScheduleBindingAdapters {
    @JvmStatic
    @BindingAdapter("dayChipSelected")
    fun setDayChipSelected(view: TextView, isSelected: Boolean?) {
        val selected = isSelected == true
        val context = view.context

        val bgRes = if (selected) {
            R.drawable.bg_chip_selected
        } else {
            R.drawable.bg_chip_unselected
        }
        view.setBackgroundResource(bgRes)

        // Text color
        val textColor = if (selected) {
            ContextCompat.getColor(context, android.R.color.white)
        } else {
            ContextCompat.getColor(context, R.color.colorPrimary)
        }
        view.setTextColor(textColor)
    }
}

/**
 * Crop line → "Crop Name: Tomato  |  Growth Stage: Budding and Flowering"
 * with the two labels in bold. Falls back to "Not set" until the end-user sets the crop data.
 */
@BindingAdapter("cropLine")
fun TextView.setCropLine(gadget: GadgetCardUi?) {
    if (gadget == null) {
        text = ""
        return
    }

    val cropLabel = context.getString(R.string.CROP_NAME) + ": "
    val stageLabel = context.getString(R.string.GROWTH_STAGE) + ": "
    val notSet = context.getString(R.string.Not_Set)

    val builder = SpannableStringBuilder()

    fun appendBold(label: String) {
        val start = builder.length
        builder.append(label)
        builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    appendBold(cropLabel)
    builder.append(gadget.cropName ?: notSet)
    builder.append("  |  ")
    appendBold(stageLabel)
    builder.append(context.getString(gadget.growthStageResId))

    text = builder
}

