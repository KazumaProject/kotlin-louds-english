import converters.Converter
import converters.ConverterWithTermId
import dictionary.BuildDictionary.loadDictionaryFromZip
import dictionary.Dictionary
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
    val dictList = loadDictionaryFromZip()
        .distinctBy { it.reading }
        .sortedBy { it.reading.length }
    println("読み込んだエントリ数: ${dictList.size}")
    buildDictionaries(dictList)
}

private fun buildDictionaries(dictList: List<Dictionary>) {
    val readingTree = PrefixTreeWithTermId()
    val wordTree = PrefixTree()

    for (entry in dictList) {
        readingTree.insert(entry.reading)
        if (entry.word.hasInternalUpperCase()) {
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

    succinctBitVectorLBSReading = SuccinctBitVector(readingLOUDS.LBS)
    succinctBitVectorReadingIsLeaf = SuccinctBitVector(readingLOUDS.isLeaf)
    succinctBitVectorLBSWord = SuccinctBitVector(wordLOUDS.LBS)
    succinctBitVectorTokenArray = SuccinctBitVector(tokenArray.bitvector)
}

private fun getPrediction(input: String): List<Result> {
    val predictions = mutableListOf<Result>()
    // 検索対象となる読みの共通プレフィックスを取得
    val commonPrefixReading = readingLOUDS.predictiveSearch(
        input,
        succinctBitVectorLBSReading
    )

    println("commonPrefixReading: $commonPrefixReading")

    for (readingStr in commonPrefixReading) {
        val nodeIndex = readingLOUDS.getNodeIndex(
            readingStr,
            succinctBitVector = succinctBitVectorLBSReading
        )
        if (nodeIndex > 0) {
            val termId = readingLOUDS.getTermId(
                nodeIndex = nodeIndex,
                succinctBitVector = succinctBitVectorReadingIsLeaf
            )
            // termId に対応する単語リストを取得
            val listToken = tokenArray.getListDictionaryByYomiTermId(
                termId,
                succinctBitVectorTokenArray
            )
            // Result オブジェクトのリストを作成
            val tangoList = listToken.map {
                Result(
                    reading = readingStr,
                    word = when (it.nodeId) {
                        -1 -> readingStr
                        else -> wordLOUDS.getLetter(
                            it.nodeId,
                            succinctBitVector = succinctBitVectorLBSWord
                        )
                    },
                    score = it.wordCost
                )
            }
            // 全体の予測リストに追加
            predictions.addAll(tangoList)
        }
    }
    return predictions
}
