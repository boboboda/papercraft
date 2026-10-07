package com.buyoungsil.papercraftlab.feature.play.table

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.annotation.SuppressLint

/**
 * 자르기 화면 배치만 담당. 무엇을 그릴지는 슬롯으로 받는다.
 *
 *  ■ 세로형 (폰 세로, 태블릿 세로)          ■ 가로형 (폰 가로, 태블릿 가로)
 *  ┌──────────────────────┐              ┌──────┬──────────────┬──────┐
 *  │ info                 │              │ info │              │      │
 *  │ ┌peek┐               │              │      │              │      │
 *  │ └────┘  table        │              │ 조이 │    table     │ 자르기│
 *  │                      │              │ 스틱 │  (peek 좌상단) │ 버튼 │
 *  ├──────────────────────┤              │      │              │      │
 *  │ 조이스틱      자르기 │              └──────┴──────────────┴──────┘
 *  └──────────────────────┘
 *
 * 조작부 크기는 화면 짧은 변 기준으로 정해서 슬롯에 넘겨 준다 (손가락 크기는 화면과 상관없으니 dp 고정 범위).
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TableLayout(
    table: @Composable (Modifier) -> Unit,
    joystick: @Composable (Dp) -> Unit,
    cutButton: @Composable (Dp) -> Unit,
    modifier: Modifier = Modifier,
    peek: @Composable () -> Unit = {},
    info: @Composable () -> Unit = {}
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val shortSide = minOf(maxWidth, maxHeight)
        val controlSize: Dp = when {
            shortSide < 360.dp -> 120.dp
            shortSide < 600.dp -> 140.dp
            else -> 160.dp                     // 태블릿
        }

        if (landscape) {
            Row(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 왼쪽: 정보 + 조이스틱
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.padding(bottom = 8.dp)) { info() }
                    joystick(controlSize)
                }
                // 가운데: 테이블 (+ 참고 창)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp)
                ) {
                    table(Modifier.fillMaxSize())
                    Box(Modifier.align(Alignment.TopStart).padding(8.dp)) { peek() }
                }
                // 오른쪽: 자르기 버튼 (아래쪽 = 엄지 위치)
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    cutButton(controlSize)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) { info() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    table(Modifier.fillMaxSize())
                    Box(Modifier.align(Alignment.TopStart).padding(8.dp)) { peek() }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    joystick(controlSize)
                    cutButton(controlSize)
                }
            }
        }
    }
}