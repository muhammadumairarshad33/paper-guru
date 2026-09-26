# Paper Guru

Paper Guru is a native Android application built with **Kotlin** and **Jetpack Compose** (Material 3) for browsing and viewing matriculation past examination papers (9th and 10th class subjects including Chemistry, Mathematics, Physics, English, Urdu, Biology, and Computer Science).

## Features

- **Class Selection**: Browse examination papers for 9th and 10th Classes.
- **Subject Directory**: Complete matriculation subject list with localized Urdu subtitles.
- **Year-wise Papers**: Comprehensive past paper archives covering 2018 through 2024.
- **Paper Detail View**: Detailed paper inspection with action triggers for viewing and downloading.
- **Native PDF Viewer**: Fast, native high-resolution PDF rendering powered by Android's `PdfRenderer`.
- **Material 3 Design**: Fully dynamic color theming, edge-to-edge layout, responsive cards, and accessibility support.

## Architecture & Tech Stack

- **Platform**: Android
- **Language**: Kotlin 2.0
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern with StateFlow
- **Assets**: Embedded BISE past examination papers under `app/src/main/assets/papers/`
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with Version Catalog (`libs.versions.toml`)

## Building and Running

Open the project in Android Studio and run the `app` configuration on an Android device or emulator running API 24+.
