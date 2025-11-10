package com.learneveryday.app.utils

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListener

object AnimationHelper {
    
    /**
     * Fade in animation with scale
     */
    fun fadeInWithScale(view: View, delay: Long = 0) {
        view.alpha = 0f
        view.scaleX = 0.9f
        view.scaleY = 0.9f
        
        ViewCompat.animate(view)
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setStartDelay(delay)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()
    }
    
    /**
     * Slide in from right animation
     */
    fun slideInFromRight(view: View, delay: Long = 0) {
        view.translationX = view.width.toFloat()
        view.alpha = 0f
        
        ViewCompat.animate(view)
            .translationX(0f)
            .alpha(1f)
            .setDuration(400)
            .setStartDelay(delay)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }
    
    /**
     * Slide in from bottom animation
     */
    fun slideInFromBottom(view: View, delay: Long = 0) {
        view.translationY = 100f
        view.alpha = 0f
        
        ViewCompat.animate(view)
            .translationY(0f)
            .alpha(1f)
            .setDuration(400)
            .setStartDelay(delay)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }
    
    /**
     * Pulse animation for highlighting
     */
    fun pulse(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.05f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.05f, 1f)
        
        scaleX.duration = 300
        scaleY.duration = 300
        
        scaleX.start()
        scaleY.start()
    }
    
    /**
     * Shake animation for errors
     */
    fun shake(view: View) {
        val context = view.context
        val animation = AnimationUtils.loadAnimation(context, 
            android.R.anim.slide_in_left)
        animation.duration = 50
        animation.repeatCount = 3
        animation.repeatMode = android.view.animation.Animation.REVERSE
        view.startAnimation(animation)
    }
    
    /**
     * Shimmer loading effect
     */
    fun startShimmer(view: View) {
        ObjectAnimator.ofFloat(view, "alpha", 0.3f, 1f, 0.3f).apply {
            duration = 1500
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }
    
    /**
     * Stop shimmer effect
     */
    fun stopShimmer(view: View) {
        view.clearAnimation()
        view.alpha = 1f
    }
    
    /**
     * Rotate animation
     */
    fun rotate(view: View, degrees: Float = 360f) {
        ViewCompat.animate(view)
            .rotation(degrees)
            .setDuration(500)
            .start()
    }
}
