package io.github.zdyoyoyo.backendquiz.data.remote

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BankParserTest {

    @Test
    // 壞掉的單題被跳過，其他題照常解析
    fun `broken question is skipped others parsed`() {
        val json = """
            {"version": 2, "updatedAt": "x", "futureField": true, "questions": [
              {"id":"ok","domain":"spring","type":"single","stem":"s","options":[{"key":"A","text":"a"},{"key":"B","text":"b"}],"answer":["A"],"explanation":"e","difficulty":1, "tags": ["新欄位會被忽略"]},
              {"id":"bad-type","domain":"spring","type":"essay","stem":"s","options":[{"key":"A","text":"a"},{"key":"B","text":"b"}],"answer":["A"]},
              {"id":"missing-stem","domain":"spring","type":"single","options":[],"answer":[]}
            ]}
        """.trimIndent()
        val parsed = BankParser.parse(json)
        assertEquals(2, parsed.bank.version)
        assertEquals(listOf("ok"), parsed.bank.questions.map { it.id })
        assertEquals(2, parsed.issues.size)
    }

    @Test
    // 缺少 version 整份拒絕
    fun `missing version rejects whole bank`() {
        assertFailsWith<BankFormatException> { BankParser.parse("""{"questions": []}""") }
    }

    @Test
    // 不是 JSON 整份拒絕
    fun `non json rejects whole bank`() {
        assertFailsWith<BankFormatException> { BankParser.parse("<html>404</html>") }
    }
}
