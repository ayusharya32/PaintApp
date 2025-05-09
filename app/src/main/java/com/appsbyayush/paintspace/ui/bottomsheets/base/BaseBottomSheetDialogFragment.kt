package com.appsbyayush.paintspace.ui.bottomsheets.base

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.appsbyayush.paintspace.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

abstract class BaseBottomSheetDialogFragment: BottomSheetDialogFragment() {
    companion object {
        private const val TAG = "BaseBottomSheetDiayy"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setOnShowListener { dialog ->
            expandBottomSheetCompletely(dialog)
        }
    }

    override fun getTheme(): Int {
        return R.style.RoundedBottomSheetDialogTheme
    }

    private fun expandBottomSheetCompletely(sheetDialogInterface: DialogInterface) {
        val sheetDialog = sheetDialogInterface as BottomSheetDialog
        val bottomSheet = sheetDialog
            .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetBehavior.peekHeight = bottomSheet.height
    }

    protected fun dismissAfterDelay(delay: Long = 100) = runBlocking {
        delay(delay)
        dismiss()
    }
}