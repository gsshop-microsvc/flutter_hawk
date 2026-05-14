# PRD: Conceal B → A` Lazy Migration

## Introduction

flutter_hawk 플러그인이 사용하는 Conceal 버전이 세 단계로 변천했다:

| 식별자 | 설명 | 상태 |
|---|---|---|
| **A** | 구 Conceal (Maven Central `conceal:1.1.3`, 4KB .so) | 이전 배포판 |
| **B** | 중간 Conceal (JitPack `com.github.GundamD:conceal:v1.1.3-16kb-fixed-3`, `SharedPrefsBackedKeyChain` 패치, .so 없음) | 현재 일부 유저 사용 중 |
| **A`** | 신 Conceal (`android/libs/conceal-1.1.3-16kb.aar`, 16KB 재빌드 .so) | 신규 배포 예정 |

**호환성:**
- A → A`: 복호화 가능 (KeyChain 구조 동일)
- A → B: 복호화 불가
- **B → A`: 복호화 불가** ← 이 PRD가 해결하는 문제

B 유저가 A` 빌드로 업데이트하면 Hawk에 저장된 토큰 등 모든 값을 읽지 못한다. 재로그인 없이 투명하게 이전하는 lazy 마이그레이션 레이어를 플러그인 내부에 구현한다.

## Goals

- B로 암호화된 Hawk 값을 A`로 읽어 자동 재암호화
- 마이그레이션 시도 여부를 기록해 동일 키에 대한 중복 시도 제거
- Hawk/Flutter API 변경 없이 투명하게 동작
- 향후 특정 버전에서 마이그레이션 코드 제거 가능한 구조 유지

## User Stories

### US-001: B KeyChain 클래스 격리 내장
**Description:** As a developer, I need B's patched `SharedPrefsBackedKeyChain` available at runtime without conflicting with A`'s Conceal classes.

**Acceptance Criteria:**
- [ ] JitPack AAR (`com.github.GundamD:conceal:v1.1.3-16kb-fixed-3`)에서 `classes.jar` 추출
- [ ] `SharedPrefsBackedKeyChain` (및 의존 내부 클래스)를 `com.gsshop.mobile.flutter.flutter_hawk.migration` 패키지로 리패키지
- [ ] 리패키지된 `.kt` 또는 `.java` 파일을 플러그인 `src/main/` 하위에 포함
- [ ] A`의 Conceal 클래스와 클래스패스 충돌 없음
- [ ] `./gradlew assembleDebug` 성공

### US-002: 마이그레이션 시도 추적 저장소
**Description:** As a developer, I need to record which keys have already been migration-attempted so we never retry a failed or completed key.

**Acceptance Criteria:**
- [ ] `SharedPreferences` 파일명 `flutter_hawk_migration_v1` 에 시도 완료된 key 집합 저장
- [ ] `get` 호출 시 이 집합에 키가 있으면 B 복호화 시도 없이 즉시 반환
- [ ] 마이그레이션 성공·실패 모두 집합에 기록 (재시도 없음)
- [ ] A` Hawk 스토어와 별도 파일 사용 (충돌 없음)

### US-003: `get` lazy 마이그레이션 흐름
**Description:** As a user, I want my stored tokens to be automatically recovered when I update the app, without re-logging in.

**Acceptance Criteria:**
- [ ] `get(key)` 흐름:
  1. A` Hawk에서 읽기 → 값 있으면 반환 (정상 경로, 마이그레이션 없음)
  2. 마이그레이션 시도 집합에 key 존재 → `""` 반환 (중복 시도 차단)
  3. B Conceal 직접 API로 복호화 시도
  4. 성공 → A` `Hawk.put(key, value)` + 시도 집합에 key 추가 + 값 반환
  5. 실패 → 시도 집합에 key 추가 + `""` 반환
- [ ] `put` / `delete`는 항상 A`만 사용 (마이그레이션 레이어 미개입)
- [ ] B 복호화 예외는 catch 후 로그만 남기고 Flutter에 오류 전파 안 함

### US-004: B Conceal 인스턴스 초기화
**Description:** As a developer, I need to correctly instantiate B's Conceal crypto object using the migrated KeyChain class and A`'s native .so.

**Acceptance Criteria:**
- [ ] `onAttachedToEngine` 시 B용 `Crypto` 인스턴스 별도 생성
  - KeyChain: 리패키지된 `migration.SharedPrefsBackedKeyChain`
  - Native: A`의 `.so` 그대로 사용 (JitPack AAR에 .so 없으므로 추가 번들 불필요)
- [ ] B `Crypto` 인스턴스 초기화 실패 시 마이그레이션 기능만 비활성화, A` 정상 동작 유지
- [ ] B `Crypto` 인스턴스는 `onDetachedFromEngine` 시 정리

### US-005: 마이그레이션 코드 제거 준비
**Description:** As a developer, I want the migration layer structured so it can be deleted cleanly in a future version.

**Acceptance Criteria:**
- [ ] 마이그레이션 관련 코드가 `FlutterHawkPlugin.kt` 내 별도 private 함수 또는 `ConcealMigrationHelper.kt` 클래스로 격리
- [ ] `build.gradle` 주석에 제거 예정 버전 명시 (예: `// TODO: remove after v2.x.x`)
- [ ] `migration/` 패키지 하위 클래스 일괄 삭제로 마이그레이션 레이어 제거 가능

## Functional Requirements

- FR-1: B KeyChain 클래스를 `com.gsshop.mobile.flutter.flutter_hawk.migration` 패키지로 리패키지해 플러그인에 내장한다
- FR-2: `get(key)` 호출 시 A` Hawk miss → 시도 집합 확인 → B 복호화 → A` 저장의 순서로 동작한다
- FR-3: 마이그레이션 시도(성공·실패 무관)는 `SharedPreferences("flutter_hawk_migration_v1")`의 `Set<String>` 에 key를 추가해 영구 기록한다
- FR-4: B 복호화는 Hawk API를 우회하고 Conceal `Crypto` 직접 API(`decrypt(Entity, InputStream, OutputStream)` 등)를 사용한다
- FR-5: `put` / `delete` 메서드는 변경 없이 A` Hawk만 사용한다
- FR-6: B `Crypto` 초기화 실패는 조용히 처리하고 이후 모든 키를 시도 집합에 선제 등록하지 않는다 (초기화 실패와 복호화 실패를 구분)

## Non-Goals

- B 스토어 데이터 삭제 (3C: 삭제 안 함, 단 재시도도 없음)
- iOS 마이그레이션
- A → B 역방향 마이그레이션
- Flutter 레이어 API 변경
- 마이그레이션 진행률 Flutter 콜백

## Technical Considerations

- **B .so 없음 문제:** JitPack AAR에 `.so`가 없으므로 B용 Conceal 인스턴스는 A`의 `.so`를 그대로 로드한다. 암호화 불일치는 `.so`가 아닌 `KeyChain` 키 파생 방식 차이에서 발생하므로, A` `.so` + B `KeyChain` 조합으로 복호화 가능하다 (런타임 검증 필요).
- **클래스 충돌 방지:** JitPack `classes.jar` 전체를 의존으로 추가하면 A`의 `com.facebook.conceal` 클래스와 충돌한다. `SharedPrefsBackedKeyChain` 및 의존 클래스만 추출·리패키지해 소스로 포함한다.
- **Hawk 내부 직렬화 포맷:** Hawk는 값을 `"타입코드@@Base64(암호화데이터)"` 형태로 저장한다. B Conceal로 복호화 후 Hawk 포맷 파싱이 필요하다 — Hawk 소스의 `DataUtil` / `HawkConverter` 참고.
- **Thread safety:** `get`은 메인 스레드에서 호출될 수 있다. 마이그레이션 시도 집합 읽기/쓰기는 동기화 또는 `@WorkerThread` 처리를 고려한다.

## Success Metrics

- B 빌드 사용 유저가 A` 업데이트 후 재로그인 없이 토큰 복구
- 동일 키에 대한 B 복호화 시도가 최대 1회
- A` 정상 경로(`get` 캐시 히트) 성능 영향 없음 (시도 집합 조회 O(1))

## Open Questions

- B `SharedPrefsBackedKeyChain`의 실제 변경 내용 확인 필요: SharedPreferences 파일명·키 alias가 바뀌었는지, 아니면 키 파생 로직 자체가 바뀌었는지. (`check_native_libs.py` 또는 jadx로 JitPack classes.jar 역컴파일 확인)
- A` `.so` + B `KeyChain` 조합으로 실제 복호화되는지 런타임 검증 필요 (단위 테스트 또는 수동 검증)
- 마이그레이션 코드 제거 목표 버전 확정 필요
