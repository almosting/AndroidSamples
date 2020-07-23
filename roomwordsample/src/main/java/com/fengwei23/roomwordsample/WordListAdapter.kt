package com.fengwei23.roomwordsample

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * @author w.feng
 * @date 2020/7/23
 */
class WordListAdapter internal constructor(context: Context) :
  RecyclerView.Adapter<WordListAdapter.WordViewHolder>() {
  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var words = emptyList<Word>()

  inner class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val wordItemView: TextView = itemView.findViewById(R.id.textView)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): WordViewHolder {
    val itemView = inflater.inflate(R.layout.recyclerview_item, parent, false)
    return WordViewHolder(itemView)
  }

  override fun getItemCount(): Int = words.size

  override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
    val current = words[position]
    holder.wordItemView.text = current.word
  }

  internal fun setWords(words: List<Word>) {
    this.words = words
    notifyDataSetChanged()
  }
}