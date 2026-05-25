import converters.Converter
import converters.ConverterWithTermId
import dictionary.BuildDictionary
import dictionary.Dictionary
import engine.EnglishEngine
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.serialization.jackson.jackson
import louds.LOUDS
import louds.louds_with_term_id.LOUDSWithTermId
import prefix.PrefixTree
import prefix.prefix_with_term_id.PrefixTreeWithTermId
import succinctBitVector.SuccinctBitVector
import tokenArray.TokenArray
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

data class SuggestionDto(
    val reading: String,
    val word: String,
    val score: Short,
)

data class SuggestResponse(
    val query: String,
    val items: List<SuggestionDto>,
)

fun main() {
    val engine = createEnglishEngine()

    embeddedServer(Netty, host = "0.0.0.0", port = 8080) {
        install(CallLogging)
        install(ContentNegotiation) {
            jackson()
        }

        routing {
            get("/") {
                val html = this::class.java.classLoader
                    .getResource("web/index.html")
                    ?.readText()
                    ?: "<h1>index.html not found</h1>"
                call.respondText(html, ContentType.Text.Html)
            }

            get("/health") {
                call.respond(mapOf("status" to "ok"))
            }

            get("/api/suggest") {
                val query = call.request.queryParameters["q"]?.trim().orEmpty()
                val limit = call.request.queryParameters["limit"]
                    ?.toIntOrNull()
                    ?.coerceIn(1, 20)
                    ?: 10

                if (query.isEmpty()) {
                    call.respond(SuggestResponse(query = "", items = emptyList()))
                    return@get
                }

                val items = engine.getPrediction(query)
                    .sortedBy { it.score.toInt() }
                    .distinctBy { it.word.lowercase() }
                    .take(limit)
                    .map {
                        SuggestionDto(
                            reading = it.reading,
                            word = it.word,
                            score = it.score,
                        )
                    }

                call.respond(SuggestResponse(query = query, items = items))
            }

            get("/{...}") {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Not Found"))
            }
        }
    }.start(wait = true)
}

private fun createEnglishEngine(): EnglishEngine {
    val dictList: List<Dictionary> = (
        BuildDictionary.loadDictionaryFromZip() +
            MANUAL_WORD +
            TERMINAL_COMMAND +
            PYTHON_COMMAND +
            WEB_COMMAND +
            WEB_COMMAND_EXTRA +
            PYTHON_COMMAND_EXTRA +
            DEVOPS_COMMAND +
            UTIL_COMMAND
        )
        .sortedWith(
            compareBy<Dictionary> { it.reading.length }
                .thenBy { it.cost }
        )

    val readingTree = PrefixTreeWithTermId()
    val wordTree = PrefixTree()

    for (entry in dictList) {
        readingTree.insert(entry.reading)
        if (entry.withUpperCase) {
            wordTree.insert(entry.word)
        }
    }

    val readingLOUDS = ConverterWithTermId().convert(readingTree.root).apply { convertListToBitSet() }
    val wordLOUDS = Converter().convert(wordTree.root).apply { convertListToBitSet() }

    ObjectOutputStream(BufferedOutputStream(FileOutputStream("./src/main/resources/reading.dat"))).use {
        readingLOUDS.writeExternalNotCompress(it)
    }
    ObjectOutputStream(BufferedOutputStream(FileOutputStream("./src/main/resources/word.dat"))).use {
        wordLOUDS.writeExternalNotCompress(it)
    }

    val readingLoaded = ObjectInputStream(FileInputStream("./src/main/resources/reading.dat")).use {
        LOUDSWithTermId().readExternalNotCompress(it)
    }
    val wordLoaded = ObjectInputStream(FileInputStream("./src/main/resources/word.dat")).use {
        LOUDS().readExternalNotCompress(it)
    }

    val tokenArray = TokenArray()
    ObjectOutputStream(FileOutputStream("./src/main/resources/token.dat")).use {
        tokenArray.buildTokenArray(dictList, wordLoaded, it)
    }

    val tokenLoaded = ObjectInputStream(FileInputStream("./src/main/resources/token.dat")).use {
        TokenArray().apply { readExternal(it) }
    }

    val succinctBitVectorLBSReading = SuccinctBitVector(readingLoaded.LBS)
    val succinctBitVectorLBSWord = SuccinctBitVector(wordLoaded.LBS)
    val succinctBitVectorReadingIsLeaf = SuccinctBitVector(readingLoaded.isLeaf)
    val succinctBitVectorTokenArray = SuccinctBitVector(tokenLoaded.bitvector)

    return EnglishEngine(
        readingLOUDS = readingLoaded,
        wordLOUDS = wordLoaded,
        tokenArray = tokenLoaded,
        succinctBitVectorLBSReading = succinctBitVectorLBSReading,
        succinctBitVectorLBSWord = succinctBitVectorLBSWord,
        succinctBitVectorReadingIsLeaf = succinctBitVectorReadingIsLeaf,
        succinctBitVectorTokenArray = succinctBitVectorTokenArray,
    )
}
