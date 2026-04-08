# Solution
ソリューション開発_産業未来

## プロジェクト構成

### image-preprocessing（画像前処理アプリ）
鉄骨文字認識システムの画像前処理モジュール（Android アプリ）

- **使用技術**: Kotlin, Android, OpenCV 4.11
- **担当**: 坂井壱謙（画像処理班・前処理担当）
- **ターゲット**: Android API 34（Android 14）

#### 機能
- 鉄骨画像の前処理（グレースケール化、CLAHE コントラスト補正、適応的二値化）
- 処理時間の計測・表示（目標: 3秒以内）
- 18 枚の鉄骨サンプル画像によるテスト

#### ビルド方法
```bash
cd image-preprocessing
./gradlew assembleDebug
```

#### 動作確認済み環境
- Android Studio 2024.3.x（Meerkat）
- JDK 21（Android Studio 同梱 JBR）
- NDK 25.1.8937393
- Gradle 8.4 / AGP 8.3.2
