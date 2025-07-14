# LOUDS Dictionary Builder & Release Pipeline 🚀

[![Build & Release](https://github.com/KazumaProject/kotlin-louds-english/actions/workflows/build_dictionary.yml/badge.svg)](https://github.com/<OWNER>/<REPO>/actions/workflows/build_dictionary.yml)
![Java 17](https://img.shields.io/badge/Java-17-blue.svg)
![Kotlin 1.9](https://img.shields.io/badge/Kotlin-1.9.x-orange.svg)
![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)

A **Kotlin 17** toolchain that compiles a LOUDS‑compressed predictive dictionary, generates
`reading.dat`, `word.dat`, and `token.dat`, and publishes them as GitHub Release assets on
**every push to `main`**.

---

## ✨ Features

* **LOUDS compression** for in‑memory tries with *O(1)* `rank/select` lookup
* Pure **Gradle 7** build—no shell scripts or external codegen
* `./gradlew run` writes three `.dat` files to `src/main/resources/`
* GitHub Actions auto‑increments tags (`v1`, `v2`, …) and uploads artifacts to the latest release
* Automatically deduplicates a zipped **1‑gram TSV** (`1-grams_score_cost_pos_combined_with_ner.zip`)

## 📂 Project Structure

```text
.github/workflows/          # CI definition (build_dictionary.yml)
src/main/kotlin/
  ├─ dictionary/            # Dictionary loader & model classes
  ├─ louds/                 # LOUDS + SuccinctBitVector implementation
  ├─ prefix/                # Trie with / without term IDs
  ├─ converters/            # Trie → LOUDS converters
  └─ ...
src/main/resources/         # Generated .dat files live here
```

## 🚀 Quick Start

```bash
# Clone & build
$ git clone <repo‑url>
$ cd <repo>
$ ./gradlew clean build --no‑daemon

# Generate data artifacts (.dat)
$ ./gradlew run --no‑daemon
# → reading.dat / word.dat / token.dat appear under src/main/resources/
```

### Try a prediction

```kotlin
val predictions = getPrediction("こう")
predictions.take(5).forEach { println(it) }
```

## ⚙️ CI Pipeline

| Step | Action                           | Purpose                               |
| ---: | -------------------------------- | ------------------------------------- |
|    1 | `actions/checkout@v4`            | Fetch source code                     |
|    2 | `actions/setup-java@v4`          | Install Temurin 17                    |
|    3 | `actions/cache@v4`               | Cache Gradle packages                 |
|    4 | `./gradlew clean build`          | Compile + unit tests                  |
|    5 | `./gradlew run`                  | Produce `.dat` assets                 |
|    6 | `softprops/action-gh-release@v2` | Create/append release & upload assets |

Authentication is handled by the built‑in **`GITHUB_TOKEN`** with `contents: write` permission—no personal access token required.

## 🗄️ LOUDS Cheatsheet

|  Array / Bitmap  | Description                                          |
| ---------------- | ---------------------------------------------------- |
| **LBS**          | Level‑order Unary Degree Sequence—encodes tree shape |
| **labels**       | Parallel `CharArray` storing node labels             |
| **isLeaf**       | Terminal‑node bitmap                                 |
| **termIds**      | (Reading trie only) row index into the token array   |

All bitmaps are wrapped in **`SuccinctBitVector`** to provide `rank`/`select` in *O(1)* time.

## 📑 1‑gram ZIP Format

| Index | Field name | Description                           |
| ----: | ---------- | ------------------------------------- |
|     0 | `reading`  | Hiragana reading                      |
|     1 | `word`     | Surface form                          |
|     2 | `pos`      | Part‑of‑speech tag                    |
|     3 | `cost`     | `Int32` → stored as `Short` (clamped) |

The first **two** lines are treated as headers and skipped automatically (logged as `Skipped header: …`).

## 🔄 Updating the Dictionary

1. Replace **`1-grams_score_cost_pos_combined_with_ner.zip`** with a newer corpus in `src/main/resources/`.
2. Commit & push to **`main`**.
3. GitHub Actions will build, tag, and publish a release with fresh `.dat` assets.

## 📄 License

Distributed under the **MIT License**. See [`LICENSE`](./LICENSE) for details.

## 🖤 Acknowledgements

* [Wikimedia Wikitext‑103](https://huggingface.co/datasets/wikitext) dataset
* Kotlin Standard Library & Gradle 7
* [`softprops/action-gh-release`](https://github.com/softprops/action-gh-release)

---

# LOUDS 辞書ビルダー & 自動リリース 🚀（日本語）

**Kotlin 17** 製のツールチェーンで LOUDS 圧縮予測辞書を構築し、`reading.dat` / `word.dat` / `token.dat` を生成。`main` ブランチへの **Push ごとに GitHub Release** へ自動アップロードします。

## ✨ 特徴

* **LOUDS 圧縮**による高速検索・省メモリ
* **Gradle 7** だけで完結（外部スクリプト不要）
* `./gradlew run` で 3 つの `.dat` が `src/main/resources/` へ出力
* GitHub Actions がタグ (`v1`, `v2` …) を自動生成し、アセットをアップロード
* `1-grams_score_cost_pos_combined_with_ner.zip` を読み込み、重複を排除

## 📂 ディレクトリ構成

```text
.github/workflows/          # CI 設定 (build_dictionary.yml)
src/main/kotlin/
  ├─ dictionary/            # 辞書関連クラス
  ├─ louds/                 # LOUDS & SuccinctBitVector 実装
  ├─ prefix/                # Trie 実装
  ├─ converters/            # Trie → LOUDS 変換器
  └─ ...
src/main/resources/         # 生成された .dat ファイル
```

## 🚀 クイックスタート

```bash
# クローン & ビルド
$ git clone <repo-url>
$ cd <repo>
$ ./gradlew clean build --no-daemon

# .dat を生成
$ ./gradlew run --no-daemon
# → src/main/resources/ にファイルが出力されます
```

### 予測変換を試す

```kotlin
val results = getPrediction("こう")
results.take(5).forEach { println(it) }
```

## ⚙️ CI パイプライン

| Step | Action                           | 目的              |
| ---: | -------------------------------- | --------------- |
|    1 | `actions/checkout@v4`            | ソース取得           |
|    2 | `actions/setup-java@v4`          | Temurin 17 設定   |
|    3 | `actions/cache@v4`               | Gradle キャッシュ    |
|    4 | `./gradlew clean build`          | コンパイル + テスト     |
|    5 | `./gradlew run`                  | `.dat` 生成       |
|    6 | `softprops/action-gh-release@v2` | リリース作成 & アセット添付 |

認証は `contents: write` 権限付き **`GITHUB_TOKEN`** が自動で処理します。

## 🗄️ LOUDS チートシート

| 配列 / ビットマップ | 説明                                        |
| ----------- | ----------------------------------------- |
| **LBS**     | Level‑order Unary Degree Sequence—木構造を符号化 |
| **labels**  | ノード文字を並列配列で格納                             |
| **isLeaf**  | 末端ノード判定ビットマップ                             |
| **termIds** | （読みトライのみ）Token 配列行への ID                   |

すべて **`SuccinctBitVector`** でラップし、`rank/select` を *O(1)* で実装。

## 📑 1‑gram ZIP フォーマット

|  列 | フィールド     | 説明                       |
| -: | --------- | ------------------------ |
|  0 | `reading` | ひらがな読み                   |
|  1 | `word`    | 表層形                      |
|  2 | `pos`     | 品詞タグ                     |
|  3 | `cost`    | `Int32` → `Short` に丸めて格納 |

先頭 **2 行** はヘッダとしてスキップされ、`Skipped header: …` とログ出力されます。

## 🔄 辞書データ更新手順

1. `src/main/resources/` の Zip を新しいコーパスに置き換える
2. `main` ブランチへ Push
3. CI がビルドし、最新の `.dat` を添付したリリースを作成

## 📄 ライセンス

MIT License。詳細は [`LICENSE`](./LICENSE) を参照。

## 🖤 謝辞

* [Wikimedia Wikitext‑103](https://huggingface.co/datasets/wikitext)
* Kotlin 標準ライブラリ & Gradle 7
* [`softprops/action-gh-release`](https://github.com/softprops/action-gh-release)
