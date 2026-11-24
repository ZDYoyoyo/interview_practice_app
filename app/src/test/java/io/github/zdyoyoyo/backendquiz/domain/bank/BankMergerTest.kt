package io.github.zdyoyoyo.backendquiz.domain.bank

import io.github.zdyoyoyo.backendquiz.TestQuestions.question
import io.github.zdyoyoyo.backendquiz.domain.model.QuestionOption
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BankMergerTest {

    private fun ref(q: io.github.zdyoyoyo.backendquiz.domain.model.Question, active: Boolean = true) =
        LocalQuestionRef(q.id, ContentHasher.hash(q), active)

    @Test
    // 本機是空的：全部視為新增
    fun `empty local - everything is added`() {
        val plan = BankMerger.plan(emptyList(), listOf(question("a"), question("b")))
        assertEquals(listOf("a", "b"), plan.addedIds)
        assertEquals(2, plan.upserts.size)
        assertTrue(plan.retiredIds.isEmpty())
    }

    @Test
    // 同 id 內容改變視為更新，id 不變
    fun `same id with changed content is updated`() {
        val old = question("a", stem = "舊題幹")
        val new = old.copy(stem = "新題幹")
        val plan = BankMerger.plan(listOf(ref(old)), listOf(new))
        assertEquals(listOf("a"), plan.updatedIds)
        assertEquals("a", plan.upserts.single().question.id)
        assertTrue(plan.addedIds.isEmpty())
    }

    @Test
    // 內容沒變就不寫入
    fun `unchanged content is not written`() {
        val q = question("a")
        val plan = BankMerger.plan(listOf(ref(q)), listOf(q))
        assertFalse(plan.hasChanges)
    }

    @Test
    // 題庫刪掉的題目標記為 retired，而不是刪除
    fun `removed question is retired not deleted`() {
        val keep = question("keep")
        val gone = question("gone")
        val plan = BankMerger.plan(listOf(ref(keep), ref(gone)), listOf(keep))
        assertEquals(listOf("gone"), plan.retiredIds)
        assertTrue(plan.upserts.isEmpty())
    }

    @Test
    // 已經 retired 的題目不會重複計算
    fun `already retired question is not counted again`() {
        val gone = question("gone")
        val plan = BankMerger.plan(listOf(ref(gone, active = false)), emptyList())
        assertTrue(plan.retiredIds.isEmpty())
    }

    @Test
    // 被移除的題目又回到題庫：重新啟用
    fun `returning question is reactivated`() {
        val q = question("back")
        val plan = BankMerger.plan(listOf(ref(q, active = false)), listOf(q))
        assertEquals(listOf("back"), plan.reactivatedIds)
        assertEquals(1, plan.upserts.size)
    }

    @Test
    // 混合情境：新增、更新、移除、不變同時發生
    fun `mixed add update retire unchanged`() {
        val same = question("same")
        val changed = question("changed")
        val removed = question("removed")
        val plan = BankMerger.plan(
            local = listOf(ref(same), ref(changed), ref(removed)),
            incoming = listOf(same, changed.copy(explanation = "新解析"), question("new")),
        )
        assertEquals(listOf("new"), plan.addedIds)
        assertEquals(listOf("changed"), plan.updatedIds)
        assertEquals(listOf("removed"), plan.retiredIds)
        assertEquals(setOf("new", "changed"), plan.upserts.map { it.question.id }.toSet())
    }

    @Test
    // 內容雜湊：答案順序不影響，選項文字會影響
    fun `hash ignores answer order but detects option text change`() {
        val q = question(answer = setOf("A", "B"), type = io.github.zdyoyoyo.backendquiz.domain.model.QuestionType.MULTI)
        assertEquals(ContentHasher.hash(q), ContentHasher.hash(q.copy(answer = setOf("B", "A"))))
        assertNotEquals(
            ContentHasher.hash(q),
            ContentHasher.hash(q.copy(options = q.options.map { if (it.key == "C") QuestionOption("C", "改過") else it })),
        )
    }
}
