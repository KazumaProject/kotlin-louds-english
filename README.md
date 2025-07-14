Build and Release Data Artifacts

A Kotlin-based toolchain that builds a LOUDS-compressed predictive dictionary, generates reading.dat, word.dat, and token.dat, then publishes them as assets on every push to main.

⸻

✨ Features
	•	LOUDS compression for fast lookup with minimal memory.
	•	Pure Kotlin 17 + Gradle build—no external scripts.
	•	A dedicated gradlew run task writes the three .dat files to src/main/resources.
	•	GitHub Actions tags each run (v1, v2, …) and uploads the artifacts to the release.
	•	Reads a zipped 1-gram TSV (1-grams_score_cost_pos_combined_with_ner.zip) and deduplicates by reading.

⸻

📂 Project Structure

.github/workflows/          # CI definition (build_dictionary.yml)
src/main/kotlin/            # All Kotlin sources
  ├─ dictionary/            # Dictionary loader & model classes
  ├─ louds/                 # LOUDS + SuccinctBitVector impl.
  ├─ prefix/                # Trie with & without term IDs
  ├─ converters/            # Trie → LOUDS converters
  └─ ...
src/main/resources/         # Output .dat files live here


⸻

🚀 Quick Start

# Clone & build
$ git clone <repo-url>
$ cd <repo>
$ ./gradlew clean build --no-daemon

# Generate data artifacts
$ ./gradlew run --no-daemon
# → reading.dat / word.dat / token.dat appear under src/main/resources/

Try a prediction

val predictions = getPrediction("こう")
predictions.take(5).forEach { println(it) }


⸻

⚙️ CI Pipeline (GitHub Actions)

Step	Action	Purpose
1	actions/checkout@v4	Fetch source
2	actions/setup-java@v4	Install Temurin 17
3	actions/cache@v4	Cache Gradle packages
4	./gradlew clean build	Compile + test
5	./gradlew run	Produce .dat assets
6	softprops/action-gh-release@v2	Create/append release & upload assets

GITHUB_TOKEN with contents: write takes care of authentication—no manual PAT needed.

⸻

🗄️ LOUDS Cheatsheet
	•	LBS: Level-order unary degree sequence stored as a BitSet.
	•	labels: Parallel CharArray holding node labels.
	•	isLeaf: Terminal-node bitmap.
	•	termIds: Only on the reading trie, maps readings → token array rows.
	•	All bitmaps are wrapped in SuccinctBitVector for O(1) rank/select.

⸻

📑 1-gram ZIP Format

Index	Field	Description
0	reading	Hiragana reading
1	word	Surface form
2	pos	Part-of-speech tag
3	cost	Int32 → stored as Short (clamped)

The first two header lines are skipped automatically (they will be logged as Skipped header: …).

⸻

🔄 Updating the Dictionary
	1.	Replace 1-grams_score_cost_pos_combined_with_ner.zip with a newer corpus.
	2.	Commit & push to main.
	3.	GitHub Actions will publish a new release with fresh assets.

⸻

📄 License

Distributed under the MIT License. See LICENSE for details.

⸻

🖤 Acknowledgements
	•	Wikimedia Wikitext-103 dataset.
	•	Kotlin Standard Library & Gradle 7.
	•	softprops/action-gh-release.

⸻

データアーティファクト自動生成プロジェクト

Kotlin 製 LOUDS 辞書ビルダー。./gradlew run で reading.dat / word.dat / token.dat を生成し、main への Push ごとに GitHub Release へ自動添付します。

⸻

✨ 特徴
	•	LOUDS 圧縮による高速検索・省メモリ。
	•	Kotlin 17 + Gradle だけで完結。
	•	gradlew run が .dat を書き出し。
	•	GitHub Actions がタグ (v1, v2, …) を生成し、アセットをアップロード。
	•	1-gram TSV Zip (1-grams_score_cost_pos_combined_with_ner.zip) から動的に辞書を構築。

⸻

📂 プロジェクト構成

.github/workflows/          # CI (build_dictionary.yml)
src/main/kotlin/            # Kotlin ソース
  ├─ dictionary/            # 辞書関連
  ├─ louds/                 # LOUDS 実装
  ├─ prefix/                # Trie
  ├─ converters/            # Trie → LOUDS 変換
  └─ ...
src/main/resources/         # 生成された .dat


⸻

🚀 クイックスタート

# クローン & ビルド
$ git clone <repo-url>
$ cd <repo>
$ ./gradlew clean build --no-daemon

# .dat 生成
$ ./gradlew run --no-daemon
# → src/main/resources/ 以下に生成されます

予測変換を試す

val results = getPrediction("こう")
results.take(5).forEach { println(it) }


⸻

⚙️ CI パイプライン

Step	Action	目的
1	actions/checkout@v4	ソース取得
2	actions/setup-java@v4	Temurin 17 設定
3	actions/cache@v4	Gradle キャッシュ
4	./gradlew clean build	コンパイル＋テスト
5	./gradlew run	.dat 生成
6	softprops/action-gh-release@v2	リリース作成 & アセット添付

認証は GITHUB_TOKEN（contents: write 権限）で自動処理されます。

⸻

🗄️ LOUDS 概要
	•	LBS: Level-order Unary Degree Sequence を BitSet で保持。
	•	labels: ノード文字を並列配列で格納。
	•	isLeaf: 末端ノード判定ビットマップ。
	•	termIds: 読みトライのみ、TokenArray 行を指す ID。
	•	すべて SuccinctBitVector でラップし、rank/select を O(1) で実装。

⸻

📑 1-gram ZIP フォーマット

列	内容	説明
0	reading	ひらがな読み
1	word	表層形
2	pos	品詞タグ
3	cost	Int32 → Short に丸めて格納

先頭 2 行はヘッダとして読み飛ばされ、ログに Skipped header: が出力されます。

⸻

🔄 辞書データを更新するには
	1.	src/main/resources の Zip を更新。
	2.	main へ Push。
	3.	CI が新しいリリースを作成し、最新 .dat を添付。

⸻

📄 ライセンス

LICENSE に記載の MIT License で公開。

⸻

🖤 謝辞
	•	Wikimedia Wikitext-103 データセット。
	•	Kotlin 標準ライブラリ & Gradle 7。
	•	softprops/action-gh-release.
