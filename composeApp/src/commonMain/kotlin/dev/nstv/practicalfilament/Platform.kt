package dev.nstv.practicalfilament

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform