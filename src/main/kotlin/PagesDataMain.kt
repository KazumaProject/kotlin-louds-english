import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.core.JsonFactory
import dictionary.BuildDictionary
import dictionary.Dictionary
import java.io.File

private const val MAX_WORDS_PER_READING = 8

fun main() {
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
            compareBy<Dictionary> { it.reading }
                .thenBy { it.cost }
                .thenBy { it.word }
        )

    val outputFile = File("docs/suggestions.json")
    outputFile.parentFile.mkdirs()

    var written = 0
    val jsonFactory = JsonFactory()
    jsonFactory.createGenerator(outputFile, JsonEncoding.UTF8).use { gen ->
        gen.writeStartObject()
        gen.writeNumberField("version", 1)
        gen.writeArrayFieldStart("items")

        var currentReading = ""
        var emittedForCurrentReading = 0
        val seenWords = mutableSetOf<String>()

        for (entry in dictList) {
            val reading = entry.reading.lowercase()
            if (reading != currentReading) {
                currentReading = reading
                emittedForCurrentReading = 0
                seenWords.clear()
            }

            val wordKey = entry.word.lowercase()
            if (wordKey in seenWords) {
                continue
            }
            if (emittedForCurrentReading >= MAX_WORDS_PER_READING) {
                continue
            }

            seenWords.add(wordKey)
            emittedForCurrentReading++

            gen.writeStartArray()
            gen.writeString(reading)
            gen.writeString(entry.word)
            gen.writeNumber(entry.cost.toInt())
            gen.writeEndArray()
            written++
        }

        gen.writeEndArray()
        gen.writeEndObject()
    }

    println("Generated ${outputFile.path} with $written entries")
}
