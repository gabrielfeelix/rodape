package com.example

import android.app.Application
import com.example.data.remote.Supabase

class RodapeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Toca no singleton pra forcar warm-up (lazy thread-safe).
        Supabase.client
    }
}
