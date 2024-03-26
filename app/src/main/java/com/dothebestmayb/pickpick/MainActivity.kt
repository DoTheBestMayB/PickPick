package com.dothebestmayb.pickpick

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.dothebestmayb.pickpick.databinding.ActivityMainBinding
import com.dothebestmayb.pickpick.swipDeck.SwipeDeck
import com.dothebestmayb.pickpick.swipDeck.SwipeEventCallback

class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private lateinit var cardStack: SwipeDeck
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SwipeDeckAdapter

    private val testData = mutableListOf(1,2,3,4)
    private var num = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cardStack = binding.swipeDeck
        cardStack.setHardwareAccelerationEnabled(true)

        adapter = SwipeDeckAdapter(testData, this)
        cardStack.setAdapter(adapter)


        cardStack.setEvenCallback(object: SwipeEventCallback {
            override fun cardSwipedLeft(position: Int) {
                Log.i(TAG, "card was swiped left, position in adapter: $position")
            }

            override fun cardSwipedRight(position: Int) {
                Log.i(TAG, "card was swiped right, position in adapter: $position")
            }

            override fun cardsDepleted() {
                Log.i(TAG, "no more cards")

            }

            override fun cardActionDown() {
                Log.i(TAG, "cardActionDown")
            }

            override fun cardActionUp() {
                Log.i(TAG, "cardActionUp")
            }
        })
        cardStack.setLeftImage(R.id.left_image)
        cardStack.setRightImage(R.id.right_image)

        binding.btnSwipeLeft.setOnClickListener {
            cardStack.swipeTopCardLeft(180)
        }

        binding.btnSwipeRight.setOnClickListener {
            cardStack.swipeTopCardRight(180)
        }

        binding.btnAddCard.setOnClickListener {
            testData.add(num++)
            adapter.notifyDataSetChanged()
        }
    }
}