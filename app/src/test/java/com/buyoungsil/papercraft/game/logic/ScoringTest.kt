package com.buyoungsil.papercraft.game.logic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.buyoungsil.papercraft.data.model.Picture
import com.buyoungsil.papercraft.data.model.Piece
import com.buyoungsil.papercraft.data.model.Shape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {

    /** 연습 판의 네모 조각 (8칸 × 8칸) */
    private val square = Shape.Polygon(rectPoly(110f, 110f, 190f, 190f))

    private fun piece(id: String, name: String, adjBig: String = "", adjSmall: String = "") =
        Piece(id, name, square, Color.Red, adjBig, adjSmall)

    // ---------- 테두리 오차 판정 ----------

    @Test
    fun 정확히_맞으면_오차_0() {
        val s = Scoring.score(square, rectPoly(110f, 110f, 190f, 190f))
        assertEquals(0f, s.error, 0.01f)
        assertEquals(Grade.PERFECT, s.grade)
    }

    @Test
    fun 한_칸_옆으로_밀리면_0점5칸() {
        val s = Scoring.score(square, rectPoly(120f, 110f, 200f, 190f))
        assertEquals(5f, s.error, 0.3f)
        assertEquals(Grade.PERFECT, s.grade)
    }

    @Test
    fun 사방으로_한_칸씩_크면_약_1칸_좋아요() {
        val s = Scoring.score(square, rectPoly(100f, 100f, 200f, 200f))
        assertEquals(10.15f, s.error, 0.3f)
        assertEquals(Grade.MEH, s.grade)        // 10을 살짝 넘어서 "아쉬워요"
        assertTrue(s.areaRatio > 1f)            // 크게 자름
    }

    @Test
    fun 가는_돛대를_두_배_폭으로_잘라도_조각_크기와_무관하게_판정() {
        val mast = Shape.Polygon(rectPoly(145f, 72f, 155f, 218f))
        val s = Scoring.score(mast, rectPoly(140f, 72f, 160f, 218f))
        assertEquals(4.52f, s.error, 0.3f)
        assertEquals(Grade.PERFECT, s.grade)
    }

    @Test
    fun 작은_원_눈을_네모로_잘라도_1칸_이내() {
        val eye = Shape.Ellipse(Offset(131f, 120f), 8f, 8f)
        val s = Scoring.score(eye, rectPoly(123f, 112f, 139f, 128f))
        assertEquals(1.0f, s.error, 0.3f)
    }

    @Test
    fun 별_기준() {
        assertEquals(3, Scoring.stars(0.5f))
        assertEquals(2, Scoring.stars(0.9f))
        assertEquals(1, Scoring.stars(1.5f))
        assertEquals(0, Scoring.stars(2.0f))
    }

    // ---------- 완성작 제목 ----------

    @Test
    fun 받침에_따라_이_가() {
        assertEquals("가", TitleMaker.subjectJosa("머리"))
        assertEquals("이", TitleMaker.subjectJosa("몸통"))
        assertEquals("이", TitleMaker.subjectJosa("눈"))
        assertEquals("가", TitleMaker.subjectJosa("귀"))
    }

    @Test
    fun 못_붙인_조각이_있으면_없는() {
        val results = listOf(
            PieceResult(piece("head", "머리"), PieceScore(3f, 1f)),
            PieceResult(piece("earL", "왼쪽 귀"), null),
        )
        val summary = TitleMaker.summarize("고양이", results)
        assertEquals("귀가 없는 고양이", summary.title)
        assertEquals(0, summary.stars)   // (0.3 + 4) / 2 = 2.15칸
    }

    @Test
    fun 크게_자르면_adjBig() {
        val results = listOf(
            PieceResult(piece("head", "머리", adjBig = "머리가 큰"), PieceScore(15f, 1.5f)),
        )
        assertEquals("머리가 큰 고양이", TitleMaker.summarize("고양이", results).title)
    }

    @Test
    fun 크기는_비슷한데_모양이_틀리면_울퉁불퉁한() {
        val results = listOf(
            PieceResult(piece("body", "몸통"), PieceScore(15f, 1.05f)),
        )
        assertEquals("몸통이 울퉁불퉁한 고양이", TitleMaker.summarize("고양이", results).title)
    }

    @Test
    fun 전부_잘_붙이면_완벽한() {
        val results = listOf(
            PieceResult(piece("a", "머리"), PieceScore(2f, 1f)),
            PieceResult(piece("b", "몸통"), PieceScore(4f, 1f)),
        )
        val summary = TitleMaker.summarize("고양이", results)
        assertEquals("완벽한 고양이", summary.title)
        assertEquals(3, summary.stars)
    }

    // ---------- 숫자 규칙 ----------

    @Test
    fun 색종이_장수() {
        val pieces = List(7) { piece("p$it", "조각") }
        val house = Picture("house", "집", "집", 1, "", pieces)
        val practice = house.copy(id = "practice", practice = true)

        assertEquals(9, PaperRules.papersFor(house))
        assertNull(PaperRules.papersFor(practice))
    }

    @Test
    fun 색종이_크기() {
        val wall = Shape.Polygon(rectPoly(82f, 150f, 218f, 260f))         // 136 × 110
        val eye = Shape.Ellipse(Offset(131f, 120f), 8f, 8f)               // 16 × 16
        assertEquals(200f, PaperRules.paperSizeFor(wall), 0.01f)          // 197.2 → 200
        assertEquals(50f, PaperRules.paperSizeFor(eye), 0.01f)            // 최소 50
    }

    @Test
    fun 전면_광고는_3장마다() {
        assertFalse(PaperRules.shouldShowInterstitial(0))
        assertFalse(PaperRules.shouldShowInterstitial(2))
        assertTrue(PaperRules.shouldShowInterstitial(3))
        assertTrue(PaperRules.shouldShowInterstitial(6))
    }
}