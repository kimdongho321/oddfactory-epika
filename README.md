# 에피카 음악 지휘

에피카의 도트 캐릭터로 YouTube Music의 재생·일시정지·곡 넘김을 조작하는 Android 앱입니다. 앱에서 실행을 선택하고 Android 설정에서 화면 표시용 접근성 서비스와 음악 연동용 알림 접근을 직접 허용하세요. 음악 제어에는 YouTube Music 앱과 활성 미디어 세션이 필요합니다. 캐릭터만 실행하는 옵션도 있습니다. 캐릭터를 두 번 터치하면 음악 종료 옵션이 열리고 세 번 터치하면 설정·종료 메뉴가 열립니다.

지원 버전은 Android 11 이상입니다.


## 빌드

JDK 21과 Android SDK Platform 35를 설치하고 JAVA_HOME 및 ANDROID_HOME을 지정하세요. Android Studio에서도 이 폴더를 열 수 있습니다. 첫 빌드는 Gradle과 Android 빌드 도구를 내려받습니다.

Windows PowerShell에서 실행합니다.

    .\gradlew.bat :app:assembleDebug

macOS 또는 Linux에서 실행합니다.

    sh ./gradlew :app:assembleDebug

빌드 결과는 app/build/outputs/apk/debug/에 생성됩니다. 앱 설치 후에는 폰에서 설정과 실행을 할 수 있습니다.

## 권리 및 고지

트릭컬 리바이브의 캐릭터와 원작 IP는 에피드게임즈에 권리가 있습니다. 이 저장소의 도트 그림은 비공식 팬 창작이며 개인의 비상업적 교류를 위한 것입니다. 캐릭터 IP와 팬아트에 별도의 범용 재사용 라이선스는 부여하지 않습니다. [에피드게임즈 2차 창작 가이드](https://epidgames.oqupie.com/portal/2399/article/73315)

메이플스토리 Light/Bold 서체는 넥슨코리아의 원본 서체이며 고지는 앱 자산에 포함되어 있습니다. [메이플스토리 서체 사용 조건](https://maplestory.nexon.com/Media/Font)

Gradle Wrapper 고지는 [licenses](licenses/)에 있습니다.
