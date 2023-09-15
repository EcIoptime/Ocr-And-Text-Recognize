package com.example.ocr.views.dialogueFragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import androidx.fragment.app.DialogFragment
import com.example.ocr.R
import com.example.ocr.databinding.DialogueFragmentSelectOcrTypeBinding

class SelectOcrTypeDialogue : DialogFragment() {

    var binding: DialogueFragmentSelectOcrTypeBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialogue_fragment_select_ocr_type, container, false)
        binding = DialogueFragmentSelectOcrTypeBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inits(view)
    }

    override fun onStart() {
        super.onStart()
        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        width = displayMetrics.widthPixels
        dialog?.window?.setLayout((width * 0.7).toInt()/*width*//*ViewGroup.LayoutParams.MATCH_PARENT*/, /*height */ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog?.setCanceledOnTouchOutside(true)
    }

    private fun inits(view: View) {
//        binding?.title?.text = "Please Select"
        binding?.closeDialogue?.setOnClickListener { dismiss() }
        binding?.closeBtn?.setOnClickListener {
            noCallBack?.invoke()
            dismiss()
        }
        binding?.yesButton?.setOnClickListener {
            yesCallBack?.invoke()
            dismiss()
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = super.onCreateDialog(savedInstanceState)

        // request a window without the title
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        hideNavItem(dialog)
//        hideStatusBar(dialog)
        return dialog
        //return super.onCreateDialog(savedInstanceState)
    }

    private fun hideStatusBar(dialog: Dialog) {
        if (Build.VERSION.SDK_INT < 16) {
            dialog.window!!.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            val decorView = dialog.window!!.decorView
            val uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
            decorView.systemUiVisibility = uiOptions
        }
    }

    fun hideNavItem(dialog: Dialog) {
        val currentApiVersion = Build.VERSION.SDK_INT
        val flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        // This work only for android 4.4+
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {
            dialog.window?.decorView?.systemUiVisibility = flags
            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            val decorView = dialog.window?.decorView
            decorView?.setOnSystemUiVisibilityChangeListener {
                dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                /*if ((it and View.SYSTEM_UI_FLAG_FULLSCREEN) == View.VISIBLE) {*/
                decorView.systemUiVisibility = flags
                /*}*/
            }
        }
    }

    companion object {
        var width: Int = 0
        var yesCallBack: (() -> Unit)? = null
        var noCallBack: (() -> Unit)? = null
        fun newInstance(yesCallBack: (() -> Unit) ,noCallBack: (() -> Unit)): SelectOcrTypeDialogue {
            val args = Bundle()
            Companion.yesCallBack = yesCallBack
            Companion.noCallBack = noCallBack
            val fragment = SelectOcrTypeDialogue()
            fragment.arguments = args
            return fragment
        }
    }
}
