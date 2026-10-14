# Contributing

[English](CONTRIBUTING.md)

이슈와 풀 리퀘스트를 환영한다.

## Setup

- JDK 17+ 및 Maven 3.9+
- `mvn hpi:run`으로 플러그인이 로드된 개발용 Jenkins를 시작한다

## Project layout

- `src/main/java/.../StagefyGlobalVariable.java`는 `stagefy` 글로벌 변수를 등록한다.
- `src/main/resources/io/jenkins/plugins/stagefy/`에는 Groovy 소스(`core/` 스테이지와 스텝, `util/` 헬퍼)가 있다. 이 파일들은 Maven이 컴파일하지 않고 Jenkins가 런타임에 로드하므로 테스트를 통해서만 검증된다.
- `src/test/java/.../StagefyDslTest.java`는 `JenkinsRule`로 파이프라인을 실행하고 빌드 로그를 검증한다.

## Making a change

1. 먼저 `StagefyDslTest`에 실패하는 테스트를 작성한다 (YAML 스테이지를 실행하고 로그나 결과를 확인하는 파이프라인).
2. 변경을 구현한다.
3. `mvn verify`(테스트 및 정적 분석)를 실행하여 통과하는지 확인한다.
4. 동작이나 YAML 문법이 바뀌면 `README.md`(및 `README.ko.md`)를 갱신하고, `CHANGELOG.md`의 *Unreleased* 아래에 항목을 추가한다.
5. 풀 리퀘스트는 하나의 변경에 집중한다.

## Code notes

- 파이프라인 안에 존재하는 Groovy 클래스는 `Serializable`이어야 한다.
- 사용자 오류는 `jenkins.error(...)`로 보고하고 메시지에 스테이지/템플릿 이름을 포함한다.
- YAML 값이 셸에 도달할 때는 신뢰할 수 없는 값으로 취급한다. README의 보안 안내를 참조.
