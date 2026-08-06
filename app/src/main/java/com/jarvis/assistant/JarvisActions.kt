package com.jarvis.assistant

/**
 * Punct de acces simplu către instanța activă a serviciului de accesibilitate,
 * astfel încât MainActivity să poată trimite acțiuni fără bind/broadcast complicat.
 */
object JarvisActions {
    var serviceInstance: JarvisAccessibilityService? = null
}
