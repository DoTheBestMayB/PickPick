package com.dothebestmayb.pickpick

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.dothebestmayb.pickpick.swipDeck.BlankActivity
import com.squareup.picasso.Picasso

class SwipeDeckAdapter(
    private val data: List<Int>,
    private val context: Context,
) : BaseAdapter() {
    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(position: Int): Any {
        return data[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = if (convertView == null) {
            // TODO Q1. 원본 코드에서는 SwipeDeckAdapter를 MainActivity 내부에 선언하여 getLayoutInflater 함수를 호출했는데,
            //  클래스를 분리한 현재 코드에서 이렇게 접근해도 되나요?
            val inflater = (context as Activity).layoutInflater
            inflater.inflate(R.layout.item_card, parent, false)
        } else {
            convertView
        }
        val imageView = view.findViewById<ImageView>(R.id.offer_image)
        // TODO 7. Piccaso가 무슨 라이브러리인지, 꼭 필요한지 찾아보기
        Picasso.with(context).load(R.drawable.ssr_card).fit().centerCrop().into(imageView)
        val textView = view.findViewById<TextView>(R.id.sample_text)
        textView.text = getItem(position).toString()

        // TODO Q2. 원본 코드에서는 view.context를 사용했는데, 파라미터로 전달받은 context를 사용하는 것과 어떤 차이가 있나요?
        view.setOnClickListener {
            val intent = Intent(view.context, BlankActivity::class.java)
            view.context.startActivity(intent)
        }

        return view
    }

}
