package com.dothebestmayb.pickpick.swipDeck

import android.animation.Animator
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.animation.OvershootInterpolator
import kotlin.math.abs

class SwipeListener(
    private val card: View,
    private val callback: SwipeCallBack,
    private val initialX: Float,
    private val initialY: Float,
    private val rotationDegrees: Float,
    private val opacityEnd: Float,
) : View.OnTouchListener {
    private val paddingLeft: Int = (card.parent as ViewGroup).paddingLeft
    private val parent: ViewGroup = card.parent as ViewGroup

    private var mActivePointerId: Int = 0
    private var initialXPress = 0f
    private var initialYPress = 0f

    private var leftView: View? = null
    private var rightView: View? = null

    private var deactivated: Boolean = false
    private var click = true

    private val cardAnimator = object: Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) {
        }

        override fun onAnimationEnd(animation: Animator) {
            callback.cardOffScreen()
        }

        override fun onAnimationCancel(animation: Animator) {
        }

        override fun onAnimationRepeat(animation: Animator) {
        }

    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (deactivated) {
            return false
        }
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                //gesture has begun
                click = true

                v.clearAnimation()

                mActivePointerId = event.getPointerId(0)
                initialXPress = event.x
                initialYPress = event.y

                if (event.findPointerIndex(mActivePointerId) == 0) {
                    callback.cardActionDown()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // gesture is in progress
                val pointerIndex = event.findPointerIndex(mActivePointerId)
                if (pointerIndex != 0) {
                    return true
                }
                // calculate distance moved
                val dx = event.getX(pointerIndex) - initialXPress
                val dy = event.getY(pointerIndex) - initialYPress

                // throw away the move in this case as it seems to be wrong
                // TODO: figure out why this is the case
                if (initialXPress == 0f && initialYPress == 0f) {
                    // makes sure the pointer is valid
                    return true
                }
                val posX = card.x + dx
                val posY = card.y + dy

                // in this circumstance consider the motion a click
                // TODO 3. 뭐하는 로직인지 확인 필요
                if (abs(dx + dy) > 5) {
                    click = false
                }

                card.x = posX
                card.y = posY

                val distObjectX = posX - initialX
                card.rotation = rotationDegrees * 2f * distObjectX / parent.width

                val alpha = (posX - paddingLeft) / (parent.width * opacityEnd)
                rightView?.alpha = alpha
                leftView?.alpha = -alpha
            }

            MotionEvent.ACTION_UP -> {
                //  gesture has finished
                //  check to see if card has moved beyond the left or right bounds or reset
                //  card position
                checkCardForEvent()

                if (event.findPointerIndex(mActivePointerId) == 0) {
                    callback.cardActionUp()
                }

                // check if this is a click event and then perform a click
                // this is a workaround, android doesn't play well with multiple listeners
                if (click) {
                    v.performClick()
                }
            }
            else -> return false
        }
        return true
    }

    fun setLeftView(view: View) {
        leftView = view
    }

    fun setRightView(view: View) {
        rightView = view
    }

    private fun checkCardForEvent() {
        if (cardBeyondLeftBorder()) {
            animateOffScreenLeft(SwipeDeck.ANIMATION_TIME)
                .setListener(cardAnimator)
            callback.cardSwipedLeft()
            deactivated = true
        } else if (cardBeyondRightBorder()) {
            animateOffScreenRight(SwipeDeck.ANIMATION_TIME)
                .setListener(cardAnimator)
            callback.cardSwipedRight()
            deactivated = true
        } else {
            resetCardPosition()
        }
    }

    fun animateOffScreenLeft(duration: Long): ViewPropertyAnimator {
        return card.animate()
            .setDuration(duration)
            .x(-parent.width.toFloat())
            .y(0f)
            .rotation(-30f)
    }

    fun animateOffScreenRight(duration: Long): ViewPropertyAnimator {
        return card.animate()
            .setDuration(duration)
            .x(parent.width * 2f)
            .y(0f)
            .rotation(30f)
    }

    private fun cardBeyondLeftBorder(): Boolean {
        // check if cards middle is beyond the left quarter of the screen
        return (card.x + card.width / 2) < parent.width / 4f
    }

    private fun cardBeyondRightBorder(): Boolean {
        // check if card middle is beyond the right quarter of the screen
        return (card.x + card.width / 2) > parent.width / 4f * 3
    }

    private fun resetCardPosition(): ViewPropertyAnimator {
        rightView?.alpha = 0f
        leftView?.alpha = 0f
        return card.animate()
            .setDuration(200)
            .setInterpolator(OvershootInterpolator(1.5f))
            .x(initialX)
            .y(initialY)
            .rotation(0f)
    }

}
