import converters.Converter
import converters.ConverterWithTermId
import dictionary.BuildDictionary
import dictionary.Dictionary
import engine.EnglishEngine
import louds.LOUDS
import louds.louds_with_term_id.LOUDSWithTermId
import prefix.PrefixTree
import prefix.prefix_with_term_id.PrefixTreeWithTermId
import result.Result
import succinctBitVector.SuccinctBitVector
import tokenArray.TokenArray
import java.io.*

private lateinit var readingLOUDS: LOUDSWithTermId
private lateinit var wordLOUDS: LOUDS
private lateinit var tokenArray: TokenArray
private lateinit var succinctBitVectorLBSReading: SuccinctBitVector
private lateinit var succinctBitVectorReadingIsLeaf: SuccinctBitVector
private lateinit var succinctBitVectorTokenArray: SuccinctBitVector
private lateinit var succinctBitVectorLBSWord: SuccinctBitVector

fun main() {
    val dictList: List<Dictionary> = (BuildDictionary.loadDictionaryFromZip() + MANUAL_WORD + TERMINAL_COMMAND)
        .sortedWith(
            compareBy<Dictionary> { it.reading.length }
                .thenBy { it.cost }
        )
    println("読み込んだエントリ数: ${dictList.size}")
    buildDictionaries(dictList)
    val englishEngine = EnglishEngine(
        readingLOUDS = readingLOUDS,
        wordLOUDS = wordLOUDS,
        tokenArray = tokenArray,
        succinctBitVectorLBSReading = succinctBitVectorLBSReading,
        succinctBitVectorLBSWord = succinctBitVectorLBSWord,
        succinctBitVectorReadingIsLeaf = succinctBitVectorReadingIsLeaf,
        succinctBitVectorTokenArray = succinctBitVectorTokenArray
    )
    println("result: ${englishEngine.getPrediction("the")}")
    println("result: ${englishEngine.getPrediction("im")}")
    println("result: ${englishEngine.getPrediction("ios")}")
}

private fun buildDictionaries(dictList: List<Dictionary>) {
    val readingTree = PrefixTreeWithTermId()
    val wordTree = PrefixTree()

    for (entry in dictList) {
        readingTree.insert(entry.reading)
        if (entry.withUpperCase) {
            if (entry.reading == "im") {
                println("im: ${entry.word}")
            }
            wordTree.insert(entry.word)
        }
    }

    val readingLOUDSTemp = ConverterWithTermId().convert(readingTree.root)
    val wordLOUDSTemp = Converter().convert(wordTree.root)
    readingLOUDSTemp.convertListToBitSet()
    wordLOUDSTemp.convertListToBitSet()
    val objectOutputReading =
        ObjectOutputStream(BufferedOutputStream(FileOutputStream("./src/main/resources/reading.dat")))
    val objectOutputWord = ObjectOutputStream(BufferedOutputStream(FileOutputStream("./src/main/resources/word.dat")))

    readingLOUDSTemp.writeExternalNotCompress(objectOutputReading)
    wordLOUDSTemp.writeExternalNotCompress(objectOutputWord)

    val objectInputReading = ObjectInputStream(FileInputStream("./src/main/resources/reading.dat"))
    val objectInputWord = ObjectInputStream(FileInputStream("./src/main/resources/word.dat"))

    readingLOUDS = LOUDSWithTermId().readExternalNotCompress(objectInputReading)
    wordLOUDS = LOUDS().readExternalNotCompress(objectInputWord)
    val tokenArrayTemp = TokenArray()
    val objectOutput = ObjectOutputStream(FileOutputStream("./src/main/resources/token.dat"))
    tokenArrayTemp.buildTokenArray(dictList, wordLOUDS, objectOutput)
    val objectInput = ObjectInputStream(FileInputStream("./src/main/resources/token.dat"))
    tokenArray = TokenArray()
    tokenArray.readExternal(objectInput)
    println("size ${tokenArray.bitvector.size()}")
    succinctBitVectorLBSReading = SuccinctBitVector(readingLOUDS.LBS)
    succinctBitVectorReadingIsLeaf = SuccinctBitVector(readingLOUDS.isLeaf)
    succinctBitVectorLBSWord = SuccinctBitVector(wordLOUDS.LBS)
    succinctBitVectorTokenArray = SuccinctBitVector(tokenArray.bitvector)
}
