package com.example.launch.common

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class SuccessDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_DURATION = "duration"

        fun newInstance(duration: Long): SuccessDialogFragment {
            val fragment = SuccessDialogFragment()
            val args = Bundle()
            args.putLong(ARG_DURATION, duration)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
    
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        val duration = arguments?.getLong(ARG_DURATION) ?: 0L

        // Root Layout
        val root = FrameLayout(requireContext())
        root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Green Circle Background
        val circleSize = (200 * resources.displayMetrics.density).toInt()
        val circle = FrameLayout(requireContext())
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.OVAL
        bg.setColor(Color.parseColor("#4CAF50")) // Material Green
        circle.background = bg
        
        val params = FrameLayout.LayoutParams(circleSize, circleSize)
        params.gravity = Gravity.CENTER
        root.addView(circle, params)

        // Text
        val textView = TextView(requireContext())
        textView.text = "Finished\n${duration}ms"
        textView.gravity = Gravity.CENTER
        textView.setTextColor(Color.WHITE)
        textView.textSize = 24f
        textView.typeface = android.graphics.Typeface.DEFAULT_BOLD
        
        val textParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textParams.gravity = Gravity.CENTER
        circle.addView(textView, textParams)

        return root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}
