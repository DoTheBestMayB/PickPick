package com.dothebestmayb.pickpick.swipDeck

interface SwipeCallBack {
    fun cardSwipedLeft()
    fun cardSwipedRight()
    fun cardOffScreen()
    fun cardActionDown()
    fun cardActionUp()
}
