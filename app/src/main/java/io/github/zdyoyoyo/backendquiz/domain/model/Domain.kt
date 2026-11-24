package io.github.zdyoyoyo.backendquiz.domain.model

/**
 * 16 個題目領域。JSON 裡存的是 [code]（英文代碼），畫面上顯示 [displayName]。
 *
 * Kotlin 的 enum 和 Java 一樣，只是建構子參數直接寫在類別名稱後面。
 */
enum class Domain(val code: String, val displayName: String) {
    JAVA_BASICS("java_basics", "Java基礎"),
    JAVA_CONCURRENCY_JVM("java_concurrency_jvm", "Java並發與JVM"),
    SPRING("spring", "Spring"),
    DATABASE("database", "資料庫"),
    REDIS_MQ("redis_mq", "Redis與MQ"),
    DISTRIBUTED("distributed", "分散式系統"),
    SYSTEM_DESIGN("system_design", "系統設計"),
    NETWORK_OS("network_os", "網路與作業系統"),
    DESIGN_PATTERNS_OOP("design_patterns_oop", "設計模式與OOP"),
    SECURITY("security", "安全"),
    LINUX_DEPLOY("linux_deploy", "Linux與部署"),
    TESTING_GIT("testing_git", "測試與Git"),
    ALGORITHMS("algorithms", "演算法"),
    CS_FUNDAMENTALS("cs_fundamentals", "計算機概論"),
    BEHAVIORAL("behavioral", "行為面試與專案"),
    AI_APPLICATIONS("ai_applications", "AI應用"),

    /** 不認識的代碼一律歸到這裡，App 不會因為題庫多了新領域就壞掉。 */
    OTHER("other", "其他");

    // companion object ≈ Java 的 static 成員
    companion object {
        /** 合法代碼（不含 OTHER），給驗證器用。 */
        val knownCodes: Set<String> = entries.filter { it != OTHER }.map { it.code }.toSet()

        fun fromCode(code: String): Domain = entries.firstOrNull { it.code == code } ?: OTHER
    }
}
