# 保留 Hilt 注入入口
-keep class * extends androidx.lifecycle.ViewModel
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }

# Compose 编译器已处理保留规则
