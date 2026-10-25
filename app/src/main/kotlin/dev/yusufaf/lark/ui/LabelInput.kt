package dev.yusufaf.lark.ui

import android.app.Activity
import android.app.RemoteInput
import android.content.Intent
import android.view.inputmethod.EditorInfo
import androidx.activity.result.ActivityResult
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.WearableRemoteInputExtender
import dev.yusufaf.lark.core.normalizeLabel

private const val LABEL_KEY = "label"

/** The system text-input screen (keyboard, voice, handwriting). RemoteInput can't prefill it with the current label. */
fun labelInputIntent(): Intent {
    val input = WearableRemoteInputExtender(RemoteInput.Builder(LABEL_KEY).setLabel("Label"))
        .setInputActionType(EditorInfo.IME_ACTION_DONE)
        .get()
        .build()
    return RemoteInputIntentHelper.putRemoteInputsExtra(
        RemoteInputIntentHelper.createActionRemoteInputIntent(),
        listOf(input),
    )
}

/** The entered label, or null when the user backed out. */
fun labelFromResult(result: ActivityResult): String? {
    if (result.resultCode != Activity.RESULT_OK) return null
    val text = result.data?.let(RemoteInput::getResultsFromIntent)?.getCharSequence(LABEL_KEY) ?: return null
    return normalizeLabel(text)
}
