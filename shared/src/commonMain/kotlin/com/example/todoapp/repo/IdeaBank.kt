package com.example.todoapp.repo

import com.example.todoapp.domain.IdeaLibrary
import com.example.todoapp.domain.Ideas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import todoapp.shared.generated.resources.Res

/** The bundled ideas library (spec 8), read once and kept. */
object IdeaBank {

    private val lock = Mutex()
    private var library: IdeaLibrary? = null

    @OptIn(ExperimentalResourceApi::class)
    suspend fun get(): IdeaLibrary = lock.withLock {
        library ?: withContext(Dispatchers.Default) {
            Ideas.parse(Res.readBytes("files/ideas.json").decodeToString())
        }.also { library = it }
    }
}
