package cn.ws9.daoyi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.ws9.daoyi.ui.screens.ClassicsScreen
import cn.ws9.daoyi.ui.screens.ResultScreen
import cn.ws9.daoyi.ui.screens.SettingsScreen
import cn.ws9.daoyi.ui.screens.StartScreen
import cn.ws9.daoyi.ui.theme.CardBg
import cn.ws9.daoyi.ui.theme.Cinnabar
import cn.ws9.daoyi.ui.theme.DaoYiTheme
import cn.ws9.daoyi.ui.theme.Gold
import cn.ws9.daoyi.ui.theme.HairLine
import cn.ws9.daoyi.ui.theme.Ink
import cn.ws9.daoyi.ui.theme.InkMuted
import cn.ws9.daoyi.ui.theme.Paper
import cn.ws9.daoyi.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // targetSdk 35+ 在 Android 15 及以上被系统强制 edge-to-edge，
        // 内容会绘制到状态栏与系统三键导航栏之下（decorFitsSystemWindows 已被忽略）。
        // 因此不再尝试关闭它，而是声明浅色系统栏：让宣纸底色上的状态栏图标与三键保持深色可见。
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        // API 23–25 不支持「浅色导航栏 + 深色图标」，宣纸底色会让白色三键看不见。
        // 这几代系统改为墨色导航栏，图标保持系统白色，清晰可读；API 26+ 仍用浅色方案。
        if (android.os.Build.VERSION.SDK_INT < 26) {
            @Suppress("DEPRECATION")
            window.navigationBarColor = 0xFF1E2320.toInt()
        }

        setContent {
            DaoYiTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val vm: MainViewModel = viewModel()
    val state by vm.ui.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }

    // 不使用 Scaffold：Material3 的 Scaffold 只对自带的 NavigationBar 处理系统栏 insets，
    // 自定义 bottomBar 会原样贴到窗口最底部，与三键导航重叠。
    // 这里显式分成「内容区」与「底部导航」两块，各自只消费属于自己那一侧的 insets，
    // 既不会重叠，也不会出现上下双重留白。
    Column(
        Modifier.fillMaxSize()
            .background(Paper)
            // 横屏时三键位于侧边，左右安全区也要避让（刘海/挖孔同样适用）
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        Box(
            Modifier.weight(1f)
                .fillMaxWidth()
                .statusBarsPadding()
                .imePadding()
        ) {
            when (tab) {
                0 -> StartScreen(state, vm) { tab = 1 }
                1 -> ResultScreen(state) { tab = 3 }
                2 -> ClassicsScreen()
                else -> SettingsScreen(state, vm)
            }
        }
        BottomBar(tab) { tab = it }
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf("起局", "推演", "典籍", "设置")
    val nums = listOf("壹", "贰", "叁", "肆")

    // background 在 navigationBarsPadding 之前：底色会一直铺到系统三键区域，
    // 导航条本体则被抬高到三键之上，视觉上连成一体而不互相覆盖。
    Column(
        Modifier.fillMaxWidth()
            .background(CardBg)
            .navigationBarsPadding()
    ) {
        Box(Modifier.fillMaxWidth().height(0.6.dp).background(HairLine))
        Row(
            Modifier.fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            items.forEachIndexed { i, name ->
                Column(
                    Modifier.weight(1f)
                        .heightIn(min = 48.dp)
                        .clickable { onSelect(i) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.width(20.dp).height(2.dp)
                            .background(
                                if (selected == i) Cinnabar else Color.Transparent,
                                RoundedCornerShape(1.dp)
                            )
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        name, style = MaterialTheme.typography.titleSmall,
                        color = if (selected == i) Ink else InkMuted
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        nums[i], style = MaterialTheme.typography.labelSmall,
                        color = if (selected == i) Gold else InkMuted.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
