package com.buyoungsil.papercraftlab.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 일반 화면(메뉴, 버튼, 글자)에 쓰는 색. 밝은/어두운 모드에 따라 바뀜 */
private val LightColors = lightColorScheme(
    background = Color(0xFFE7E2D6),       // 전체 바탕 (따뜻한 종이색)
    onBackground = Color(0xFF2E2A26),
    surface = Color(0xFFFBF8F1),          // 카드, 시트
    onSurface = Color(0xFF2E2A26),
    surfaceVariant = Color(0xFFCFC7B6),   // 보조 버튼, 구분선
    onSurfaceVariant = Color(0xFF6F685C), // 설명 글자
    primary = Color(0xFF2E6FB7),          // 주요 버튼
    onPrimary = Color.White,
    secondary = Color(0xFFD8452F),        // 강조 (경고, 삐뚤 판정)
    onSecondary = Color.White,
)

private val DarkColors = darkColorScheme(
    background = Color(0xFF2A2622),
    onBackground = Color(0xFFF1EBDD),
    surface = Color(0xFF3A342E),
    onSurface = Color(0xFFF1EBDD),
    surfaceVariant = Color(0xFF4A433B),
    onSurfaceVariant = Color(0xFFB5AC9B),
    primary = Color(0xFF4A8DD6),
    onPrimary = Color.White,
    secondary = Color(0xFFFF7A63),
    onSecondary = Color.White,
)

/**
 * 게임 화면 안의 물건 색. 실제 종이·책상이라 어두운 모드에서도 바뀌지 않음.
 */
object PaperColors {
    val Wood = Color(0xFFC9A77C)          // 자르는 책상
    val SketchPaper = Color(0xFFFBF8F1)   // 밑그림 도화지
    val SketchGrid = Color(0xFFEAE3D3)    // 밑그림 모눈
    val Guide = Color(0xFFA89F8F)         // 밑그림 점선
    val PaperGrid = Color(0x66FFFFFF)     // 색종이 위 모눈 (반투명 흰색)
    val CutLine = Color(0xFF2E2A26)       // 자른 선
    val Ghost = Color(0xFF2E6FB7)         // 대보기 중인 조각 테두리
    val Tip = Color(0xFFFFF3C4)           // 연습 판 말풍선
    val TipText = Color(0xFF4A3B00)
}

/** 판정 등급별 색 */
object GradeColors {
    val Perfect = Color(0xFF2E9E5B)
    val Good = Color(0xFF2E6FB7)
    val Meh = Color(0xFFC98A12)
    val Bad = Color(0xFFD8452F)
    val Star = Color(0xFFE8A317)
}

@Composable
fun PaperCraftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}