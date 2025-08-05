# SCIM Service Provider (SP) API

## 개요

이 API는 SCIM 2.0 프로토콜을 구현하는 Service Provider(SP)를 제공합니다. Identity Provider(IdP)가 이 API를 호출하여 SP의 사용자 및 그룹 정보를 관리합니다.

## 인증

모든 API 요청에는 Bearer 토큰이 필요합니다. 토큰은 `Authorization` 헤더에 포함되어야 합니다.

```
Authorization: Bearer your-token
```

## 주요 엔드포인트

### 사용자 관리 (SCIM 표준)

- `GET /Users` - 사용자 목록 조회 (페이지네이션 지원)
- `POST /Users` - 사용자 생성
- `GET /Users/{userId}` - 특정 사용자 조회
- `PUT /Users/{userId}` - 사용자 정보 전체 교체
- `PATCH /Users/{userId}` - 사용자 정보 부분 수정
- `DELETE /Users/{userId}` - 사용자 삭제

### 그룹 관리 (SCIM 표준)

- `GET /Groups` - 그룹 목록 조회 (페이지네이션 지원)
- `POST /Groups` - 그룹 생성
- `GET /Groups/{groupId}` - 특정 그룹 조회
- `PUT /Groups/{groupId}` - 그룹 정보 전체 교체
- `PATCH /Groups/{groupId}` - 그룹 정보 부분 수정 (멤버 추가/삭제 등)
- `DELETE /Groups/{groupId}` - 그룹 삭제

### Discovery 엔드포인트 (SCIM 표준)

- `GET /ServiceProviderConfig` - SCIM 서비스 제공자 설정 조회
- `GET /Schemas` - 지원하는 모든 스키마 목록 조회
- `GET /Schemas/{schemaUrn}` - 특정 스키마 상세 조회
- `GET /ResourceTypes` - 지원하는 리소스 타입 목록 조회

## 서버 실행

```bash
./gradlew :sp:run
```

서버는 기본적으로 19090 포트에서 실행됩니다.

## API 문서

OpenAPI 명세는 `/resources/openapi/documentation.yaml` 파일에서 확인할 수 있습니다.

## SCIM 패치 작업 예시

### 사용자 활성 상태 변경

```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
  "Operations": [
    {
      "op": "replace",
      "path": "active",
      "value": false
    }
  ]
}
```

### 그룹에 멤버 추가

```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
  "Operations": [
    {
      "op": "add",
      "path": "members",
      "value": [
        {
          "value": "user-id-123",
          "display": "user@example.com"
        }
      ]
    }
  ]
}
```

### 그룹에서 멤버 제거

```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
  "Operations": [
    {
      "op": "remove",
      "path": "members",
      "value": [
        {
          "value": "user-id-123"
        }
      ]
    }
  ]
}
```
