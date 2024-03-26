package com.dothebestmayb.pickpick.swipDeck

import android.content.Context
import android.database.DataSetObserver
import android.os.AsyncTask
import android.util.AttributeSet
import android.view.View
import android.widget.Adapter
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.children
import com.dothebestmayb.pickpick.R
import com.dothebestmayb.pickpick.SwipeDeckAdapter

class SwipeDeck(context: Context, attributeSet: AttributeSet) : FrameLayout(context, attributeSet) {

    private val numberOfCards: Int
    private val rotationDegrees: Float
    private val cardSpacing: Float
    private val renderAbove: Boolean
    private val renderBelow: Boolean
    private val cardGravity: Int
    private val opacityEnd: Float

    private val paddingStart: Int
    private val paddingEnd: Int
    private val paddingTop: Int
    private val paddingBottom: Int

    private var hardwareAccelerationEnabled = true

    private lateinit var mAdapter: Adapter
    private var observer: DataSetObserver? = null
    private var nextAdapterCard = 0
    private var restoreInstanceState = false

    private var leftImageResource: Int = 0
    private var rightImageResource: Int = 0
    private var cardInteraction = false

    private var swipeListener: SwipeListener? = null
    private var eventCallback: SwipeEventCallback? = null

    init {
        val attributes =
            context.theme.obtainStyledAttributes(attributeSet, R.styleable.SwipeDeck, 0, 0)

        numberOfCards = attributes.getInt(R.styleable.SwipeDeck_max_visible, 3)
        rotationDegrees = attributes.getFloat(R.styleable.SwipeDeck_rotation_degrees, 15f)
        cardSpacing = attributes.getDimension(R.styleable.SwipeDeck_card_spacing, 15f)
        renderAbove = attributes.getBoolean(R.styleable.SwipeDeck_render_above, true)
        renderBelow = attributes.getBoolean(R.styleable.SwipeDeck_render_below, false)
        cardGravity = attributes.getInt(R.styleable.SwipeDeck_card_gravity, 0)
        opacityEnd = attributes.getFloat(R.styleable.SwipeDeck_opacity_end, 0.33f)
        attributes.recycle()

        paddingStart = getPaddingStart()
        paddingEnd = getPaddingEnd()
        paddingTop = getPaddingTop()
        paddingBottom = getPaddingBottom()

        // set clipping of view parent to false so cards render outside their view boundary
        // make sure not to clip to padding
        clipToPadding = false
        clipChildren = false

        // TODO 1. 무엇인지 확인 필요
        setWillNotDraw(false)

        if (renderAbove) {
            ViewCompat.setTranslationZ(this, Float.MAX_VALUE)
        }
        if (renderBelow) {
            ViewCompat.setTranslationZ(this, Float.MIN_VALUE)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        // Adapter 설정 여부 확인
        if (::mAdapter.isInitialized && mAdapter.count == 0) {
            nextAdapterCard = 0
            removeAllViewsInLayout()
            return
        }
        for (i in childCount until numberOfCards) {
            addNextCard()
        }
        for (i in 0 until childCount) {
            positionItem(i)
        }
        //position the new children we just added and set up the top card with a listener etc
    }

    private fun addNextCard() {
        if (nextAdapterCard < mAdapter.count) {

            // TODO: Make view recycling work
            // TODO: Instead of removing the view from here and adding it again when it's swiped
            // ... don't remove and add to this instance: don't call removeView & addView in sequence.
            val newBottomChild = mAdapter.getView(nextAdapterCard, null, this)

            // TODO 6. isHardwareAccelerated 대신 hardwareAccelerationEnabled 변수를 만들어서 직접 관리해도 되는지 확인 필요
            if (hardwareAccelerationEnabled) {
                newBottomChild.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }

            // set the initial Y value so card appears from under the deck
            // newBottomChild.setY(paddingTop);
            addAndMeasureChild(newBottomChild)
            nextAdapterCard++
        }
        setupToCard()
    }


    private fun addAndMeasureChild(child: View) {
        val params =
            child.layoutParams ?: LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        //ensure new card is under the deck at the beginning
        child.y = paddingTop.toFloat()

        //every time we add and measure a child refresh the children on screen and order them
        val children = arrayListOf<View>()
        children.add(child)
        for (i in 0 until childCount) {
            children.add(getChildAt(i))
        }

        removeAllViews()

        for (c in children) {
            addViewInLayout(c, -1, params, true)
            val itemWidth = width - (paddingStart + paddingEnd)
            val itemHeight = height - (paddingTop + paddingBottom)
            c.measure(MeasureSpec.EXACTLY or itemWidth, MeasureSpec.EXACTLY or itemHeight)

            // TODO 2. 뭐하는 코드인지 확인필요
            // ensure that if there's a left and right image set their alpha to 0 initially
            // alpha animation is handled in the swipe listener
            if (leftImageResource != 0) {
                child.findViewById<View>(leftImageResource).alpha = 0f
            }
            if (rightImageResource != 0) {
                child.findViewById<View>(rightImageResource).alpha = 0f
            }
        }
        setZTranslations()
    }

    private fun setZTranslations() {
        for ((i, child) in children.withIndex()) {
            child.translationZ = i * 10f
        }
    }

    private fun setupToCard() {
        // TODO: maybe find a better solution this is kind of hacky
        // if there's an extra card on screen that means the top card is still being animated
        // in that case setup the next card along
        val childOffset = childCount - numberOfCards + 1
        val child = getChildAt(childCount - childOffset)

        // this calculation is to get the correct position in the adapter of the current top card
        // the card position on setup top card is currently always the bottom card in the view
        // at any given time.
        val initialX = paddingStart
        val initialY = paddingTop

        if (child != null) {
            // make sure we have a card
            swipeListener = SwipeListener(child, object : SwipeCallBack {
                override fun cardSwipedLeft() {
                    val positionInAdapter = nextAdapterCard - childCount
                    removeTopCard()
                    eventCallback?.cardSwipedLeft(positionInAdapter)
                    addNextCard()
                }

                override fun cardSwipedRight() {
                    val positionInAdapter = nextAdapterCard - childCount
                    removeTopCard()
                    eventCallback?.cardSwipedRight(positionInAdapter)
                    addNextCard()
                }

                override fun cardOffScreen() {
                }

                override fun cardActionDown() {
                    eventCallback?.cardActionDown()
                    cardInteraction = true
                }

                override fun cardActionUp() {
                    eventCallback?.cardActionUp()
                    cardInteraction = false
                }

            }, initialX.toFloat(), initialY.toFloat(), rotationDegrees, opacityEnd)

            // if we specified these image resources, get the views and pass them to the swipe listener
            // for the sake of animating them
            if (leftImageResource != 0) {
                swipeListener?.setLeftView(child.findViewById(leftImageResource))
            }
            if (rightImageResource != 0) {
                swipeListener?.setRightView(child.findViewById(rightImageResource))
            }
            child.setOnTouchListener(swipeListener)
        }
    }

    private fun removeTopCard() {
        // top card is now the last in view children
        val childOffSet = childCount - numberOfCards + 1
        val child = getChildAt(childCount - childOffSet) ?: return
        child.setOnTouchListener(null)
        swipeListener = null

        //this will also check to see if cards are depleted
        removeViewWaitForAnimation(child)
    }

    private fun removeViewWaitForAnimation(child: View) {
        RemoveViewOnAnimCompleted().execute(child)
    }

    inner class RemoveViewOnAnimCompleted : AsyncTask<View, Void, View>() {
        override fun doInBackground(vararg params: View): View {
            android.os.SystemClock.sleep(ANIMATION_TIME)
            return params[0]
        }

        override fun onPostExecute(result: View) {
            super.onPostExecute(result)
            removeView(result)

            // if there are no more children left after top card removal let the callback know
            if (childCount <= 0 && eventCallback != null) {
                eventCallback?.cardsDepleted()
            }
        }
    }

    private fun positionItem(index: Int) {
        val child = getChildAt(index)

        // TODO 4. measuredWidth와 getWidth의 차이 알아보기
        val width = child.measuredWidth
        val height = child.measuredHeight
        val left = (getWidth() - width) / 2
        child.layout(left, paddingTop, left + width, paddingTop + height)
        // layout each child slightly above the previous child (we start with the bottom)
        // TODO 5. 여기 코드 수정해서 뒤에 있는 Item들이 보이는 UI 형태 바꾸기
        val offSet = ((childCount - 1) * cardSpacing) - (index * cardSpacing)

        val duration = if (restoreInstanceState) 0L else 160L
        child.animate()
            .setDuration(duration)
            .y(paddingTop + offSet)

        restoreInstanceState = false
    }

    fun setHardwareAccelerationEnabled(enabled: Boolean) {
        hardwareAccelerationEnabled = enabled
    }

    fun setAdapter(adapter: SwipeDeckAdapter) {
        // TODO Q3. mAdapter 변수를 사용하는 다른 곳에서 null check를 없애기 위해 lateinit var와 이 코드를 사용했는데 어떤가요?
        //  걱정 되는 부분은, SwipeDeck 코드 전체를 읽어보지 않은 사람이 mAdapter가 초기화 되지 않은 곳에서 사용할 수 있는 점이에요.
        if (::mAdapter.isInitialized && observer != null) {
            mAdapter.unregisterDataSetObserver(observer)
        }
        mAdapter = adapter
        // if we're not restoring previous instance state
        if (restoreInstanceState.not()) {
            nextAdapterCard = 0
        }

        observer = object : DataSetObserver() {
            override fun onChanged() {
                super.onChanged()
                // handle data set changes
                // if we need to add any cards at this point (ie. the amount of cards on screen
                // is less than the max number of cards to display) add the cards.
                for (i in childCount until numberOfCards) {
                    addNextCard()
                }
                if (childCount < numberOfCards) {
                    for (i in 0 until childCount) {
                        positionItem(i)
                    }
                }
            }

            override fun onInvalidated() {
                super.onInvalidated()

                nextAdapterCard = 0
                removeAllViews()
                requestLayout()
            }
        }
        adapter.registerDataSetObserver(observer)
        removeAllViewsInLayout()
        requestLayout()
    }

    fun setEvenCallback(swipeEventCallback: SwipeEventCallback) {
        eventCallback = swipeEventCallback
    }

    fun setLeftImage(leftImage: Int) {
        leftImageResource = leftImage
    }

    fun setRightImage(rightImage: Int) {
        rightImageResource = rightImage
    }

    fun swipeTopCardLeft(duration: Int) {
        if (childCount in 1..<numberOfCards + 1) {
            swipeListener?.animateOffScreenLeft(duration.toLong())

            val positionInAdapter = nextAdapterCard - childCount
            removeTopCard()
            eventCallback?.cardSwipedLeft(positionInAdapter)
            addNextCard()
        }
    }

    fun swipeTopCardRight(duration: Int) {
        if (childCount in 1..<numberOfCards + 1) {
            swipeListener?.animateOffScreenRight(duration.toLong())

            val positionInAdapter = nextAdapterCard - childCount
            removeTopCard()
            eventCallback?.cardSwipedRight(positionInAdapter)
            addNextCard()
        }
    }

    companion object {
        const val ANIMATION_TIME = 160L
    }
}