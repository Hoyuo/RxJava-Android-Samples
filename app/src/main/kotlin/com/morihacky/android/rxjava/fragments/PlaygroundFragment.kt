package com.morihacky.android.rxjava.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import com.morihacky.android.rxjava.R

class PlaygroundFragment : BaseFragment() {

    private var logsList: ListView? = null
    private var adapter: LogAdapter? = null

    private var logs: MutableList<String> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_concurrency_schedulers, container, false)

        logsList = view?.findViewById(R.id.list_threading_log) as ListView
        setupLogger()

        val b: Button = view.findViewById(R.id.btn_start_operation)
        b.setOnClickListener { log("Button clicked") }

        return view
    }

    // -----------------------------------------------------------------------------------
    // Method that help wiring up the example (irrelevant to RxJava)

    private fun log(logMsg: String) {

        if (isCurrentlyOnMainThread()) {
            logs.add(0, "$logMsg (main thread) ")
            adapter?.clear()
            adapter?.addAll(logs)
        } else {
            logs.add(0, "$logMsg (NOT main thread) ")

            // You can only do below stuff on main thread.
            Handler(Looper.getMainLooper()).post {
                adapter?.clear()
                adapter?.addAll(logs)
            }
        }
    }

    private fun setupLogger() {
        logs = ArrayList()
        adapter = LogAdapter(context!!, ArrayList())
        logsList?.adapter = adapter
    }

    private fun isCurrentlyOnMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }

    private inner class LogAdapter(context: Context, logs: List<String>) : ArrayAdapter<String>(
            context,
            R.layout.item_log,
            R.id.item_log,
            logs)
}