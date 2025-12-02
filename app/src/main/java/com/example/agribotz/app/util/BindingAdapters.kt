package com.example.agribotz.app.util

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import com.example.agribotz.R
import com.example.agribotz.app.domain.ApiStatus
import com.google.android.material.textfield.TextInputLayout

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

