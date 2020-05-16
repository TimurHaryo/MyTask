package com.abdhilabs.mytask.adapter

import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abdhilabs.mytask.R
import com.abdhilabs.mytask.data.model.Task
import com.abdhilabs.mytask.utils.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("listTask")
fun bindTaskRecyclerView(recyclerView: RecyclerView, task: List<Task>?) {
    val adapter = recyclerView.adapter as TaskAdapter
    if (task != null) {
        adapter.differ.submitList(task)
        adapter.notifyDataSetChanged()
        recyclerView.visibility = VISIBLE
    } else {
        recyclerView.visibility = GONE
    }
}

@BindingAdapter("isLayoutEmpty")
fun bindLayoutIsEmpty(layout: LinearLayout, isEmpty: Boolean) {
    if (isEmpty) {
        layout.visibility = VISIBLE
    } else {
        layout.visibility = GONE
    }
}

@BindingAdapter("onLayoutDateDeadline")
fun bindWarningLayout(layout: ConstraintLayout, dateDeadline: String?) {
    if (isDeadlineToday(dateDeadline)) {
        layout.setBackgroundResource(R.drawable.bg_rounded_layout_red)
    } else {
        layout.setBackgroundResource(R.drawable.bg_rounded_layout_white)
    }
}

@BindingAdapter("onIconDateDeadline")
fun bindWarningIcon(imageView: AppCompatImageView, dateDeadline: String?) {
    if (isDeadlineToday(dateDeadline)) {
        imageView.setBackgroundResource(R.drawable.ic_clock_alert)
    } else {
        imageView.setBackgroundResource(R.drawable.ic_document)
    }
}

@BindingAdapter("onTextTitleDateDeadline")
fun bindWarningTitleText(textView: TextView, dateDeadline: String?) {
    if (isDeadlineToday(dateDeadline)) {
        textView.setTextColor(textView.resources.getColor(R.color.whiteColor, null))
    } else {
        textView.setTextColor(textView.resources.getColor(R.color.blueSkyColor, null))
    }
}

@BindingAdapter("onTextSubTitleDateDeadline")
fun bindWarningSubTitleText(textView: TextView, dateDeadline: String?) {
    if (isDeadlineToday(dateDeadline)) {
        textView.text = textView.resources.getString(R.string.text_deadline, dateDeadline)
        textView.setTextColor(textView.resources.getColor(R.color.whiteColor, null))
    } else {
        textView.text = textView.resources.getString(R.string.text_deadline, dateDeadline)
        textView.setTextColor(textView.resources.getColor(R.color.redColor, null))
    }
}

private fun isDeadlineToday(dateDeadline: String?): Boolean {
    val format = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("in", "ID"))
    if (dateDeadline != null) {
        val deadline = format.parse(dateDeadline)!!.time
        val formatToday = DateTimeFormatter.getCurrent()
        val today = format.parse(formatToday)!!.time
        return deadline == today
    }
    return false
}