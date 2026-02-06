# `group` read-only 오류 해결 (앱 프로젝트에서)

이 플러그인(`flutter_hawk`)을 사용하는 **앱 프로젝트**에서  
`Cannot set the value of read-only property 'group'` 오류가 나는 경우,  
앱의 **Android 루트** `build.gradle` / `settings.gradle` 등에서 `resolutionStrategy`로 `group`을 바꾸는 코드가 원인일 수 있습니다.

## 1. 원인

Gradle 7+ 에서는 `DependencyResolveDetails.requested`의 `group`/`name`이 **읽기 전용**이라, 아래처럼 직접 대입하면 오류가 납니다.

```groovy
// ❌ 이렇게 하면 "read-only property 'group'" 오류
configurations.all {
    resolutionStrategy.eachDependency { details ->
        if (details.requested.group == 'com.orhanobut' && details.requested.name == 'hawk') {
            details.requested.group = 'com.github.calm'   // 오류
            details.requested.version = 'v2.1.0-16kb'
        }
    }
}
```

## 2. 해결: `dependencySubstitution` 사용

`group`/`name`을 바꾸고 싶다면 **의존성 치환** API를 써야 합니다.

### 2-1. Hawk를 다른 모듈로 완전히 치환하는 경우

예: `com.orhanobut:hawk` → `com.github.calm:hawk` 로 치환

앱의 **android/build.gradle** (프로젝트 루트)에서:

```groovy
allprojects {
    repositories { ... }

    configurations.all {
        resolutionStrategy.dependencySubstitution {
            substitute module('com.orhanobut:hawk') using module('com.github.calm:hawk:v2.1.0-16kb')
        }
    }
}
```

### 2-2. 버전만 강제하는 경우 (group/name 변경 없음)

`group`/`name`은 그대로 두고 **버전만** 맞추는 경우:

```groovy
configurations.all {
    resolutionStrategy.eachDependency { details ->
        if (details.requested.group == 'com.orhanobut' && details.requested.name == 'hawk') {
            details.useVersion('2.0.1')  // useVersion은 사용 가능
        }
    }
}
```

### 2-3. flutter_hawk는 orhanobut + 16KB Conceal 사용 시

flutter_hawk가 `com.orhanobut:hawk:2.0.1` + 16KB Conceal을 쓰는 구성이면,  
**앱 쪽에서는 hawk의 group을 바꾸지 않는 것**이 좋습니다.  
즉, `details.requested.group = ...` / `requested.version = ...` 같은 대입은 제거하고,  
필요하면 위 2-2처럼 `useVersion`만 쓰거나, 치환이 꼭 필요할 때만 2-1처럼 `dependencySubstitution`을 사용하세요.

## 3. 수정할 파일 위치

- **Flutter 앱**의 Android 쪽 루트: `앱경로/android/build.gradle`
- 또는 `앱경로/android/settings.gradle`에 `dependencyResolutionManagement { ... }` 안에 있는 `resolutionStrategy` 블록

여기서 `eachDependency` 안의 `details.requested.group = ...` / `details.requested.name = ...` 를 제거하고,  
위 2-1 또는 2-2 방식으로 바꾸면 read-only 오류가 사라집니다.
