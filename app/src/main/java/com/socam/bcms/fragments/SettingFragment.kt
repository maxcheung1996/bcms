package com.socam.bcms.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.socam.bcms.R

class SettingFragment : BaseFragment() {
    companion object {
        fun newInstance(): SettingFragment {
            return SettingFragment().apply {
                arguments = Bundle()
            }
        }
    }

    override fun initializeFragment(view: View) {
        Log.d("SettingFragment", "設定片段初始化完成 / Setting fragment initialized")
        // TODO: 未來添加設定功能 / Add settings functionality in the future
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }
}