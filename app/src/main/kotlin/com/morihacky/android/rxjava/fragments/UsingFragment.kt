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
import android.widget.TextView
import com.morihacky.android.rxjava.R
import io.reactivex.Flowable
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import org.reactivestreams.Publisher
import java.util.ArrayList
import java.util.Random
import java.util.concurrent.Callable

class UsingFragment : BaseFragment() {

    private lateinit var logs: MutableList<String>
    private lateinit var logsList: ListView
    private lateinit var adapter: UsingFragment.LogAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_buffer, container, false)
        logsList = view?.findViewById(R.id.list_threading_log) as ListView

        (view.findViewById(R.id.text_description) as TextView).setText(R.string.msg_demo_using)

        setupLogger()
        (view.findViewById(R.id.btn_start_operation) as Button).setOnClickListener { executeUsingOperation() }
        return view
    }

    private fun executeUsingOperation() {
        val resourceSupplier = Callable<Realm> { Realm() }
        val sourceSupplier = Function<Realm, Publisher<Int>> { realm ->
            Flowable.just(true)
                    .map {
                        realm.doSomething()
                        // i would use the copyFromRealm and change it to a POJO
                        Random().nextInt(50)
                    }
        }
        val disposer = Consumer<Realm> { realm ->
            realm.clear()
        }

        Flowable.using(resourceSupplier, sourceSupplier, disposer)
                .subscribe { i ->
                    log("got a value $i - (look at the logs)")
                }
    }

    inner class Realm {
        init {
            log("initializing Realm instance")
        }

        fun doSomething() {
            log("do something with Realm instance")
        }

        fun clear() {
            // notice how this is called even before you manually "dispose"
            log("cleaning up the resources (happens before a manual 'dispose'")
        }
    }

    // -----------------------------------------------------------------------------------
    // Method that help wiring up the example (irrelevant to RxJava)

    private fun log(logMsg: String) {
        logs.add(0, logMsg)

        // You can only do below stuff on main thread.
        Handler(Looper.getMainLooper()).post {
            adapter.clear()
            adapter.addAll(logs)
        }
    }

    private fun setupLogger() {
        logs = ArrayList<String>()
        adapter = LogAdapter(context!!, ArrayList<String>())
        logsList.adapter = adapter
    }

    private class LogAdapter(context: Context, logs: List<String>) : ArrayAdapter<String>(context,
            R.layout.item_log,
            R.id.item_log,
            logs)
}