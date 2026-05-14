package com.gsshop.mobile.flutter.flutter_hawk

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.orhanobut.hawk.Hawk

/**
 * Conceal 버전 전환 과정에서 발생한 데이터 마이그레이션 헬퍼.
 *
 * ## 배경
 * Conceal 버전은 세 단계로 변천했다.
 *
 *   A  (구)  : Maven Central conceal:1.1.3 — 4KB 페이지 정렬 .so 포함
 *   B  (중간) : JitPack com.github.GundamD:conceal:v1.1.3-16kb-fixed-3 — .so 없음
 *   A` (신)  : 로컬 conceal-1.1.3-16kb.aar — 16KB 페이지 정렬 .so 포함
 *
 * ## B에서 발생한 문제
 * B AAR에는 libconceal.so가 존재하지 않는다.
 * Hawk는 초기화 시 ConcealEncryption.isAvailable()을 검사하고,
 * .so 로드 실패(UnsatisfiedLinkError) → false 반환 → NoEncryption으로 폴백한다.
 * 결과적으로 B 배포 기간 동안 저장된 모든 값은 암호화 없이 저장됐다.
 *
 * ## 저장 포맷 (Hawk 2.0.1 내부 구조)
 * SharedPreferences 파일명 : "Hawk2"  (HawkBuilder.STORAGE_TAG_DO_NOT_CHANGE)
 * 직렬화 포맷 (HawkSerializer):
 *   "<keyClassName>#<valueClassName>#<dataType>V@<cipherText>"
 *   예) "java.lang.String##0V@ImhlbGxvIg=="
 *
 * NoEncryption의 cipherText = Base64( GsonJson(value).getBytes() )
 *   → 평문이 아닌 Base64 인코딩임에 주의
 *
 * ## 마이그레이션 흐름 (get 호출 시 lazy 실행)
 *   1. A` Hawk(Conceal)로 읽기 시도 → 성공하면 그대로 반환 (정상 경로)
 *   2. 실패 시 이 클래스의 tryMigrate() 호출
 *   3. "Hawk2" SharedPreferences에서 raw 값 읽기
 *   4. Hawk 직렬화 포맷 파싱 → Base64 디코드 → Gson 역직렬화 → 원본 String 복원
 *   5. 복원한 값을 A` Hawk(Conceal 암호화)로 재저장
 *   6. 기존 B 포맷 데이터("Hawk2" 항목) 삭제
 *   7. 시도 완료 기록 → 이후 동일 키 재시도 없음 (성공·실패 무관)
 *
 * TODO: 다음 메이저 릴리즈에서 이 클래스 및 flutter_hawk_migration_v1 SharedPreferences 제거
 */
internal class ConcealMigrationHelper(context: Context) {

    // B 배포 당시 Hawk가 사용하던 SharedPreferences 파일 (HawkBuilder.STORAGE_TAG_DO_NOT_CHANGE)
    private val hawkPrefs = context.getSharedPreferences("Hawk2", Context.MODE_PRIVATE)

    // 키별 마이그레이션 시도 여부 기록 (존재 = 시도 완료, 값 무관)
    private val attemptedPrefs = context.getSharedPreferences(
        "flutter_hawk_migration_v1", Context.MODE_PRIVATE
    )

    private val gson = Gson()

    /**
     * [key]에 해당하는 B 포맷 데이터를 읽어 A`(Conceal 암호화) Hawk로 재저장한다.
     * 마이그레이션 성공 시 복원된 값 반환, 실패 또는 이미 시도한 경우 null 반환.
     * 성공·실패 무관하게 키당 정확히 한 번만 시도한다.
     */
    fun tryMigrate(key: String): String? {
        // 이미 시도한 키는 재시도하지 않는다
        if (attemptedPrefs.contains(key)) return null

        // "Hawk2" SharedPreferences에서 raw 직렬화 값 읽기
        val rawValue = hawkPrefs.getString(key, null)
        if (rawValue == null) {
            // B 배포 이전부터 없던 키이거나 이미 삭제된 경우
            markAttempted(key)
            return null
        }

        // Hawk 직렬화 포맷 파싱: "<keyClass>#<valueClass>#<dataType>V@<cipherText>"
        // limit=3 으로 분할해 base64 페이로드 안의 '#' 에 의한 오분할 방지
        val parts = rawValue.split("#", limit = 3)
        if (parts.size < 3 || parts[0] != "java.lang.String") {
            // flutter_hawk는 String만 저장하므로 다른 타입이면 대상 아님
            markAttempted(key)
            return null
        }

        // parts[2] 형태: "0V@<base64>" — '0' = TYPE_OBJECT, 'V' = NEW_VERSION
        val typeAndCipher = parts[2]
        val atIdx = typeAndCipher.indexOf('@')
        if (atIdx < 0) {
            markAttempted(key)
            return null
        }
        val cipherText = typeAndCipher.substring(atIdx + 1)

        // NoEncryption cipherText = Base64( GsonJson(value).toByteArray() )
        // Base64 디코드 후 JSON 파싱으로 원본 String 복원
        val jsonBytes = try {
            Base64.decode(cipherText, Base64.DEFAULT)
        } catch (e: Exception) {
            // 유효한 Base64가 아니면 B 포맷이 아님 (Conceal 암호화 데이터일 수 있음)
            markAttempted(key)
            return null
        }
        val json = String(jsonBytes)

        val value = try {
            gson.fromJson(json, String::class.java)
        } catch (e: Exception) {
            markAttempted(key)
            return null
        } ?: run {
            markAttempted(key)
            return null
        }

        return try {
            // A` Hawk(Conceal)로 재저장
            Hawk.put(key, value)
            // 마이그레이션 완료 → B 포맷 원본 삭제 (재노출 방지)
            hawkPrefs.edit().remove(key).apply()
            markAttempted(key)
            value
        } catch (e: Exception) {
            // 재저장 실패 시 원본은 보존하고 null 반환
            markAttempted(key)
            null
        }
    }

    private fun markAttempted(key: String) {
        attemptedPrefs.edit().putBoolean(key, true).apply()
    }
}
