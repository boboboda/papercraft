package com.buyoungsil.papercraftlab.data.model

/**
 * 밑그림 한 장 (예: "고양이")
 *
 * @param id 고유 id ("cat"). 화면 이동 경로(play/cat)와 저장 키로 사용
 * @param name 목록에 보이는 이름 ("고양이", "연습: 네모 오리기")
 * @param noun 완성작 제목에 붙는 말 ("〈귀가 작은 고양이〉"의 "고양이")
 * @param level 난이도 1~3 (연습 판은 0)
 * @param description 목록에 보이는 한 줄 설명
 * @param pieces 조각 목록. 앞에 있을수록 아래 레이어 (몸통 → 눈·코 순서)
 * @param practice 연습 판이면 true (색종이 무제한, 단계별 안내, 전면 광고 제외)
 */
data class Picture(
    val id: String,
    val name: String,
    val noun: String,
    val level: Int,
    val description: String,
    val pieces: List<Piece>,
    val practice: Boolean = false,
) {
    /** id로 조각 찾기 */
    fun piece(pieceId: String): Piece? = pieces.firstOrNull { it.id == pieceId }

    companion object {
        /** 밑그림 좌표계 한 변 길이 */
        const val CANVAS = 300f
    }
}