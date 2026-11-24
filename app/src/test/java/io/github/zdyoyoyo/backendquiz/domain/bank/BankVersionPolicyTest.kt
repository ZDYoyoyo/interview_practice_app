package io.github.zdyoyoyo.backendquiz.domain.bank

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BankVersionPolicyTest {

    @Test
    // 第一次啟動（本機沒有題庫）一定套用
    fun `applies when no local bank (first launch)`() {
        assertTrue(BankVersionPolicy.shouldApply(localVersion = null, incomingVersion = 1))
    }

    @Test
    // 遠端版本較新才套用
    fun `applies when incoming version is newer`() {
        assertTrue(BankVersionPolicy.shouldApply(localVersion = 3, incomingVersion = 4))
    }

    @Test
    // 相同版本不重複套用
    fun `skips same version`() {
        assertFalse(BankVersionPolicy.shouldApply(localVersion = 3, incomingVersion = 3))
    }

    @Test
    // 較舊版本不會讓題庫倒退
    fun `never downgrades to older version`() {
        assertFalse(BankVersionPolicy.shouldApply(localVersion = 5, incomingVersion = 4))
    }
}
