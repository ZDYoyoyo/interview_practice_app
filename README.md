# 後端面試題複習（Android）

離線優先、零後端的後端工程師面試題練習 App。

- 題庫是 repo 裡的 [`data/questions.json`](data/questions.json)，App 從 GitHub raw 下載
- 下載後存在手機本機，**離線也能作答**；首次安裝時使用打包在 App 裡的內建題庫
- 作答紀錄、錯題本、SM-2 複習排程都只存在手機（Room / SQLite）

> 開發進度：✅ 第一階段「做題＋錯題本」　✅ 第二階段「SM-2 複習＋統計」　✅ 第三階段「每日提醒＋題庫更新提示」

---

## 目錄

1. [專案架構](#專案架構)
2. [檔案結構](#檔案結構)
3. [題庫 JSON 格式與欄位規則](#題庫-json-格式與欄位規則)
4. [如何手動新增題目](#如何手動新增題目)
5. [本機建置與測試](#本機建置與測試)
6. [題庫同步機制](#題庫同步機制)
7. [學習機制：錯題本、SM-2、弱點與統計](#學習機制錯題本sm-2弱點與統計)
8. [每日提醒與題庫更新提示](#每日提醒與題庫更新提示)
9. [資料庫版本與 Migration](#資料庫版本與-migration)

---

## 專案架構

```
UI（Jetpack Compose 畫面）
   ▲ 訂閱 StateFlow          │ 使用者操作
ViewModel（每個畫面一個，保存畫面狀態）
   ▲                         ▼
Repository（決定資料從哪來：Room / 網路 / 內建檔）
   ▲                         ▼
domain（純 Kotlin 核心邏輯：版本比對、題庫合併、對答案、錯題規則、SM-2、統計、出題策略）
   ▲
Room（SQLite）
```

| 項目 | 選擇 | 理由 |
|---|---|---|
| UI | Jetpack Compose + MVVM | 官方主推；分層和 Spring 的 Controller → Service → Repository 相似 |
| DI | 手動（`AppContainer`） | 沒有註解魔法，所有相依關係一眼看得到 |
| 資料庫 | Room | 編譯期檢查 SQL；概念接近 JPA / MyBatis |
| 設定 | DataStore (Preferences) | 提醒時間、題庫更新提示等小量設定；非同步、以 Flow 讀取 |
| 背景工作 | WorkManager | 每日提醒；App 沒開、重開機後都會照排程執行 |
| 網路 | OkHttp | 只有一個 GET，不需要 Retrofit |
| JSON | kotlinx.serialization | 正確處理 Kotlin null 安全與預設值 |
| 測試 | JUnit4 + Robolectric | 核心邏輯是純 JVM 測試；Room 用 Robolectric 的記憶體資料庫測試，不需要模擬器 |

**設計重點**：`domain/` 不依賴任何 Android 類別，所以核心規則可以直接單元測試。

---

## 檔案結構

```
.
├── .github/workflows/ci.yml        # CI：題庫版本檢查＋單元測試
├── data/questions.json             # 題庫（唯一來源；建置時也會打包成 App 內建題庫）
├── scripts/check_bank_version.sh   # 檢查題庫變動時 version 有沒有遞增
├── gradle/libs.versions.toml       # 所有套件版本
└── app/
    ├── build.gradle.kts
    ├── schemas/                    # Room 資料庫 schema（寫 migration 用，要進版控）
    └── src/
        ├── main/java/io/github/zdyoyoyo/backendquiz/
        │   ├── QuizApp.kt / AppContainer.kt / MainActivity.kt
        │   ├── domain/             # ★ 純 Kotlin
        │   │   ├── model/          Question、Domain（16 個領域代碼）
        │   │   ├── bank/           QuestionValidator、BankVersionPolicy、BankMerger、ContentHasher
        │   │   ├── quiz/           AnswerChecker、QuestionPicker（隨機／依弱點）、QuizMode
        │   │   ├── mistakes/       MistakePolicy（錯題本規則）
        │   │   ├── srs/            Sm2Scheduler（間隔複習排程）
        │   │   ├── stats/          StatsCalculator（各領域正確率、最弱領域）
        │   │   └── reminder/       ReminderSchedule（下次提醒時間）、ReminderPolicy（要不要提醒、說什麼）
        │   ├── data/
        │   │   ├── remote/         BankParser（解析 JSON）、BankRemoteSource（OkHttp 下載）
        │   │   ├── local/          AppDatabase、entity/、dao/、Mappers
        │   │   ├── prefs/          SettingsStore（DataStore：提醒設定、題庫更新提示）
        │   │   └── repository/     BankSyncRepository、PracticeRepository
        │   └── ui/
        │       ├── components/     RichText（程式碼區塊：等寬、不換行、可水平捲動）
        │       ├── text/           TextSegments（把文字切成一般文字／程式碼）
        │       ├── navigation/ home/ quiz/ mistakes/ stats/ settings/ theme/
        │   └── notify/             ReminderWorker、ReminderRunner、ReminderScheduler、Notifications
        └── test/                   # 單元測試（見下方）
```

---

## 題庫 JSON 格式與欄位規則

> 這一節是給**自動產生題庫的流程**看的規格。不符合規則的題目會被 App 跳過，CI 也會失敗。

### 整體結構

```json
{
  "version": 3,
  "updatedAt": "2026-11-24T08:00:00Z",
  "questions": [
    {
      "id": "jvm-volatile-001",
      "domain": "java_concurrency_jvm",
      "sourceTitle": "volatile 的作用與限制",
      "type": "single",
      "stem": "兩個執行緒各執行 10000 次 `count++`，結果為何？\n```java\nstatic volatile int count = 0;\n```",
      "options": [
        { "key": "A", "text": "一定是 20000" },
        { "key": "B", "text": "可能小於 20000" }
      ],
      "answer": ["B"],
      "explanation": "volatile 不保證原子性……",
      "difficulty": 2
    }
  ]
}
```

### 最外層欄位

| 欄位 | 型別 | 必填 | 規則 |
|---|---|---|---|
| `version` | 整數 | ✅ | **每次內容有任何變動都必須 +1**。App 只會套用比本機大的版本；版本沒遞增的話，使用者永遠拿不到更新（CI 會擋） |
| `updatedAt` | 字串 | ✅ | ISO-8601 UTC 時間，例如 `2026-11-24T08:00:00Z`，只用於顯示 |
| `questions` | 陣列 | ✅ | 題目列表，順序不重要 |

### 題目欄位

| 欄位 | 型別 | 必填 | 規則 |
|---|---|---|---|
| `id` | 字串 | ✅ | **全題庫唯一、永遠不改、刪除後不可重複使用**。只能用小寫英數與連字號（`^[a-z0-9]+(-[a-z0-9]+)*$`），最長 64 字元。建議格式：`主題縮寫-流水號`，例如 `jvm-gc-001` |
| `domain` | 字串 | ✅ | 領域代碼，見下表。未知代碼在 App 中會歸到「其他」（CI 會警告失敗） |
| `sourceTitle` | 字串 | 建議 | 原始問答題的標題。同一個原始問答題拆出的多個選擇題，用相同的 `sourceTitle` |
| `type` | 字串 | ✅ | `single`（單選）或 `multi`（多選） |
| `stem` | 字串 | ✅ | 題幹，不可空白。支援 Markdown 子集（見下方） |
| `options` | 陣列 | ✅ | 至少 2 個 `{ "key", "text" }`。`key` 不可重複，建議依序用 `A`、`B`、`C`…。App **不會打亂選項順序** |
| `answer` | 字串陣列 | ✅ | 正確選項的 `key`。**單選題也要用陣列**。`single` 剛好 1 個；`multi` 至少 2 個；值必須存在於 `options` |
| `explanation` | 字串 | 建議 | 解析，支援與 `stem` 相同的格式 |
| `difficulty` | 整數 | ✅ | `1`（基礎）、`2`（中等）、`3`（進階） |

其他未列出的欄位會被忽略，可以安全地加上供產生流程自己使用的欄位。

### 文字格式（`stem`、`explanation`、`options[].text`）

- 程式碼區塊：用三個反引號圍起來，可以標註語言。App 會用**等寬字型、不自動換行、可左右滑動**顯示
  ````
  ```java
  System.out.println("hi");
  ```
  ````
  在 JSON 字串裡換行寫成 `\n`。
- 行內程式碼：`` `HashMap` ``
- 其他 Markdown（粗體、清單等）不會被特別處理，會照原樣顯示。`- ` 開頭的清單本身就很好讀
- `options[].text` 只支援行內程式碼，不支援程式碼區塊

### 領域代碼

| 代碼 | 顯示名稱 | 代碼 | 顯示名稱 |
|---|---|---|---|
| `java_basics` | Java基礎 | `design_patterns_oop` | 設計模式與OOP |
| `java_concurrency_jvm` | Java並發與JVM | `security` | 安全 |
| `spring` | Spring | `linux_deploy` | Linux與部署 |
| `database` | 資料庫 | `testing_git` | 測試與Git |
| `redis_mq` | Redis與MQ | `algorithms` | 演算法 |
| `distributed` | 分散式系統 | `cs_fundamentals` | 計算機概論 |
| `system_design` | 系統設計 | `behavioral` | 行為面試與專案 |
| `network_os` | 網路與作業系統 | `ai_applications` | AI應用 |

### id 不變的重要性

手機上的作答紀錄、錯題本、複習排程**只靠 `id` 對應題目**：

| 情況 | 產生流程該怎麼做 | App 的行為 |
|---|---|---|
| 修正錯字、改寫解析、調整選項 | **沿用原 id** | 視為「更新」，舊紀錄完整保留 |
| 新增題目 | 給一個從沒用過的新 id | 視為「新增」 |
| 刪除題目 | 從陣列移除即可 | 不再出題，但本機紀錄保留；之後同 id 又出現會自動接回 |
| 題目被大幅改成完全不同的題目 | 建議**刪除舊 id、用新 id** | 避免舊的錯題紀錄套到無關的新題目 |

---

## 如何手動新增題目

1. 編輯 `data/questions.json`，在 `questions` 陣列加入新題目，依上方規則填好欄位。
2. 把最外層 `version` **+1**，並更新 `updatedAt`。
3. 本機驗證：
   ```bash
   ./gradlew testDebugUnitTest --tests "*QuestionsJsonTest*"
   ```
   有問題時會列出是哪一題、哪個欄位不合規則。
4. commit 並 push 到 `main`。App 下次啟動，或在首頁按「重新檢查更新」時就會下載新版。

> GitHub raw 有約 5 分鐘的 CDN 快取，push 後可能要稍等一下 App 才看得到新版本。

---

## 本機建置與測試

### 需要的工具

- **Android Studio**（建議最新穩定版，內建 JDK 17+ 與 Android SDK）
- 或命令列：JDK 17 以上 + Android SDK（Platform 35）

### 用 Android Studio 執行

1. `File → Open`，選這個 repo 的根目錄
2. 第一次會自動下載 Gradle 和相依套件，並建立 `local.properties`（記錄 SDK 路徑，不進版控）
3. 上方裝置選單選擇模擬器（`Device Manager` 可以建立一個，例如 Pixel 8 / API 35）或用 USB 連接已開啟「USB 偵錯」的手機
4. 按 ▶ Run

### 命令列

```bash
# 所有單元測試（純 JVM，不需要模擬器或手機）
./gradlew testDebugUnitTest

# 只跑某一類測試
./gradlew testDebugUnitTest --tests "*BankMergerTest*"

# 打包 debug APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleDebug

# 直接安裝到已連接的裝置
./gradlew installDebug
```

測試報告：`app/build/reports/tests/testDebugUnitTest/index.html`

> 第一次跑測試時，Gradle 會下載 Robolectric 需要的 Android 系統 jar（SDK 34、35 各約 150MB，優先從 Google 的 Maven Central 鏡像下載），之後使用 Gradle 快取。Robolectric 以離線模式執行，測試期間不會再連網下載。

### 測試涵蓋範圍

| 測試 | 驗證內容 |
|---|---|
| `BankVersionPolicyTest` | 題庫版本比對：只套用較新的版本、不會倒退 |
| `BankMergerTest` | 新增、更新、移除、重新啟用的判斷 |
| `QuestionValidatorTest` | 題目欄位規則 |
| `BankParserTest` | JSON 解析；壞掉的單題被跳過，不影響其他題 |
| `AnswerCheckerTest` | 單選、多選（全對才算對） |
| `MistakePolicyTest` | 錯題收錄、連對三次移出、移出後再錯會重新收錄 |
| `Sm2SchedulerTest` | SM-2 間隔（1 → 6 → ×EF）、答錯重置、EF 下限、同日重複作答不刷排程 |
| `StatsCalculatorTest` | 各領域正確率、最弱 3 個領域的門檻與排序 |
| `QuestionPickerTest` | 依弱點加權：弱點題被抽中的機率明顯較高、不重複 |
| `MigrationTest` | 資料庫 v1 → v2 升級後舊紀錄保留、已答題目補進排程 |
| `ReminderScheduleTest` | 下一次提醒時間（含跨日、日光節約時間） |
| `ReminderPolicyTest` | 何時提醒、何時不打擾、通知文字 |
| `BankUpdateNoticeTest` | 更新提示文字、多次更新合併 |
| `SettingsStoreTest` | DataStore 讀寫、提示合併與清除 |
| `ReminderRunnerTest` | 背景提醒整合：到期題數、今天已完成不通知、附帶題庫更新 |
| `ReminderSchedulerTest` | WorkManager 排程建立、更新、取消 |
| `TextSegmentsTest` | 程式碼區塊與行內程式碼的切割 |
| `QuestionsJsonTest` | **repo 裡真正的題庫檔**是否完全合法 |
| `BankSyncRepositoryTest` | 題庫更新後，舊的作答紀錄與錯題仍對得上（Room＋Robolectric） |
| `PracticeRepositoryTest` | 作答寫入、錯題收錄與過關、錯題模式出題、今天該複習、自評重算、統計 |

### 換題庫來源（例如測試用分支）

```bash
./gradlew installDebug -PbankUrl=https://raw.githubusercontent.com/zdyoyoyo/interview_practice_app/某分支/data/questions.json
```

### 簽章與敏感檔案

`.gitignore` 已排除 `*.jks`、`*.keystore`、`keystore.properties`、`local.properties`。如果之後要做 release 簽章，請把 keystore 和密碼放在 repo 之外，或放在被忽略的 `keystore.properties` 裡。

---

## 題庫同步機制

```
App 啟動
  ├─ ensureSeeded()：讀 App 內建的 questions.json
  │     └─ 本機沒有題庫，或內建版本較新 → 合併寫入
  └─ syncRemote()：GET raw URL，帶 If-None-Match: <上次的 ETag>
        ├─ 304 Not Modified → 什麼都不做（幾乎不耗流量）
        ├─ 200 → 解析 → version 比本機大？
        │        ├─ 否 → 只記住 ETag
        │        └─ 是 → BankMerger 算出新增、更新、移除 → 單一資料庫交易寫入
        └─ 失敗（沒網路）→ 繼續使用本機題庫
```

- 判斷「更新」的方式：App 對每題的內容計算 SHA-256（`ContentHasher`），同一個 id 的雜湊值改變就算更新。產生流程不需要維護任何修訂號。
- 題目被移除時只會標記 `active = false`。作答紀錄與錯題表**沒有外鍵**連到題目表，所以紀錄永遠不會被連帶刪除。

---

## 學習機制：錯題本、SM-2、弱點與統計

### 錯題本
- 答錯自動收錄；同一題**連續答對 3 次**（任何模式都算）自動移出，也可以手動移出
- 移出後再答錯會重新收錄，歷史答錯次數保留

### SM-2 間隔複習
每道做過的題目都有一個複習排程，首頁顯示「今天該複習幾題」（含過期未複習的）。

| 作答結果 | SM-2 分數 | 效果 |
|---|---|---|
| 答錯 | 2 | 連續次數歸零，明天再複習，難易係數 EF −0.32 |
| 答對＋「猜對」 | 3 | 及格，EF −0.14 |
| 答對＋「想了一下」（預設） | 4 | 及格，EF 不變 |
| 答對＋「秒答」 | 5 | 及格，EF +0.10 |

- 及格時的間隔：第 1 次 1 天、第 2 次 6 天，之後為「上次間隔 × EF」（EF 初始 2.5、下限 1.3）
- **同一天重複作答不會推進排程**：避免一天內連答好幾次就把間隔刷到幾個月後。同日再答錯只會排回明天，不重複扣 EF
- 題庫移除的題目，排程保留但不算進「今天該複習」

### 依弱點出題
每題權重 = 0.1 + (1 − 領域正確率) + 單題錯誤率，兩個比例都用拉普拉斯平滑（(答對+1)/(作答+2)），然後依權重隨機抽題、不重複。弱點題更常出現，但強項題仍有機會被抽到。

### 統計
- 各領域正確率（包含已被題庫移除的題目的歷史紀錄）
- 最弱 3 個領域：作答至少 5 次、且不是全對的領域，正確率由低到高排序；點「練習」會用依弱點模式出該領域的題目

---

## 每日提醒與題庫更新提示

### 每日提醒
- 在 App 右上角 ⚙ 設定中開啟，可選時間（預設 20:00）。Android 13 以上會詢問通知權限
- 使用 WorkManager 的每日週期工作；每次開 App 會依設定重新排程，校正長期誤差。Android 省電機制可能讓實際時間晚幾分鐘到十幾分鐘
- 提醒時也會順便檢查題庫更新
- 通知內容：

| 情況 | 通知 |
|---|---|
| 有待複習題目 | 「今天有 N 題該複習」，附錯題本題數 |
| 沒有待複習、今天還沒練習 | 「今天還沒練習」 |
| 沒有待複習、今天已經練習過 | 「今天的複習都完成了」 |

- 每天一定會發一則通知；有錯題或題庫更新時附在內文
- 設定頁的「立即測試通知」會馬上執行一次

### 題庫更新提示
- 不管是開 App 時還是背景提醒時下載到新版本，都會記錄「新增 / 更新 / 移除 / 格式錯誤略過」的題數
- 首頁上方顯示提示卡，例如「題庫已更新到 v3：新增 5 題、更新 2 題」，按「知道了」關閉
- 還沒看提示又再更新一次時，會合併成一則（版本範圍從最舊到最新）
- 第一次安裝時的初始化不算更新，不會顯示提示

---

## 資料庫版本與 Migration

| 版本 | 變更 |
|---|---|
| v1 | `questions`、`attempts`、`mistakes`、`bank_meta` |
| v2 | 新增 `review_states`（SM-2 排程）；第一階段做過的題目會被補進排程並設為今天到期 |

修改 Entity 時：
1. `AppDatabase` 的 `version` +1
2. 寫一個 `Migration(舊版, 新版)`，SQL 要和 `app/schemas/.../新版.json` 裡 Room 產生的 `createSql` 完全一致
3. 在 `MigrationTest` 加上測試：用舊版 schema 建出舊資料庫、塞資料、用新版開啟並檢查資料

**不要使用 `fallbackToDestructiveMigration()`**，它會在升級時清空作答紀錄。

