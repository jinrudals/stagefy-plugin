# Changelog

[English](CHANGELOG.md)

이 프로젝트의 주요 변경 사항. 버전은 CD 워크플로를 통해 배포되며, 릴리스가 만들어지기 전까지 항목은 *Unreleased* 아래에 모은다.

## Unreleased

### Breaking
- `steps` 스테이지의 `modules` 키(environment-modules 로딩)를 제거했다. 대신 `sh` 스텝 안에서 `module load ...`를 실행한다.

### Security
- `sh` 스텝의 `${env.VAR}`는 셸 변수 참조로 전달되므로 값이 셸 코드로 해석되지 않는다.
- `script`, `evaluate`, `when`이 Script Security 샌드박스 밖에서 Groovy를 실행한다는 점을 문서화했다.
- 템플릿 인자(`{arg}`)가 Jenkinsfile의 `${...}`처럼 `sh` 스텝에 그대로 삽입된다는 점을 문서화했다.

### Added
- 스테이지 YAML용 JSON Schema(`schema/stagefy.schema.json`)와 README의 *Editor validation* 섹션을 추가했다.

### Fixed
- `from`이 포함된 스테이지 이름(예: `deploy_from_s3`)이 더 이상 `name from file` 참조로 분리되지 않는다.
- `error()`가 항상 예외를 던지므로, 검증기가 null 데이터로 계속 진행할 수 없다.

### Changed
- 스테이지 ENTER/EXIT 메시지를 빌드 콘솔에 출력한다.

## Earlier
- 최초 플러그인: 공유 라이브러리 구현에서 이전했으며, Jenkins 호스팅, CD, 보안 스캔 설정을 포함한다.
