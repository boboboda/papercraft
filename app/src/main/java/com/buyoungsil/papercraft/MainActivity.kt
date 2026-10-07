package com.buyoungsil.papercraft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.buyoungsil.papercraft.core.ads.AdManager
import com.buyoungsil.papercraft.core.ui.theme.PaperCraftTheme
import com.buyoungsil.papercraft.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ★ Activity 에는 생성자 주입이 안 되므로 필드 주입 (Hilt 가 onCreate 전에 채워 줌)
    @Inject lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        adManager.initialize()          // ★ 광고 SDK 시작 + 보상형·전면 미리 불러오기 (여러 번 불려도 한 번만 실행)

        setContent {
            PaperCraftTheme {
                AppNavHost(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }
    }
}