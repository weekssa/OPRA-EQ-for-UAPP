package com.weekssa.opraeqforuapp.ui

import android.content.Context
import androidx.annotation.StringRes

sealed interface UiText {
    data class Resource(
        @param:StringRes val resourceId: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    /** Text supplied by an external/data-layer error boundary that cannot be localized here. */
    data class Dynamic(val value: String) : UiText
}

fun Context.resolve(uiText: UiText): String = when (uiText) {
    is UiText.Resource -> getString(uiText.resourceId, *uiText.args.toTypedArray())
    is UiText.Dynamic -> uiText.value
}
