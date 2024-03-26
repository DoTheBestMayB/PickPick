package com.dothebestmayb.pickpick.swipDeck

interface SwipeEventCallback {
    fun cardSwipedLeft(position: Int)

    fun cardSwipedRight(position: Int)

    fun cardsDepleted()

    fun cardActionDown()

    fun cardActionUp()
}
