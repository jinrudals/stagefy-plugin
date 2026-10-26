# Stagefy Jenkins Plugin

[English](README.md)

YAML 기반 파이프라인 스테이지 로더를 Jenkins 플러그인(hpi)으로 패키징한 것. `stagefy` 글로벌 변수를 제공하여 Jenkinsfile에서 바로 사용할 수 있다.

## pipeline-as-yaml과의 관계

[pipeline-as-yaml](https://plugins.jenkins.io/pipeline-as-yaml/)은 파이프라인 전체를 YAML로 정의한다. Stagefy는 이를 대체하지 않고 보완하며, **stage 단위**로 YAML을 재사용(`use`, `template`, `iterated` 디렉티브)하는 것이 차별점이다.

## 아키텍처

```
StagefyGlobalVariable (Java, @Extension)       # src/main/java
  └─ StagefyDsl.groovy                          # stagefy.run(file, stage) 진입점
      └─ DslJenkinsContext                      # Jenkins DSL 래퍼
      └─ core/
          ├─ StageFactory                       # YAML → Stage 객체 변환
          ├─ Stage                              # 추상 베이스 클래스
          ├─ StepsStage                         # 순차 스텝 실행 (steps 키)
          ├─ SequentialStage                    # 순차 스테이지 (stages 키)
          ├─ ParallelStage                      # 병렬 스테이지 (parallels 키)
          ├─ Step                               # 추상 베이스 클래스
          ├─ StepFactory                        # YAML → Step 객체 변환
          ├─ ShStep
          ├─ ScriptStep
          ├─ EvaluateStep
          ├─ SetEnvFromFileStep
          └─ UseStageStep                       # 외부 YAML 참조 (use 디렉티브)
      └─ util/
          ├─ EnvResolver                        # ${env.VAR} 치환
          ├─ Template                           # 템플릿 정의 모델
          ├─ TemplateResolver                   # template + arguments 처리
          ├─ IterationResolver                  # iterated 반복 처리
          └─ StageNameBuilder                   # 스테이지 이름 생성
```

`template`과 `iterated`는 별도의 Step 클래스가 아니라 `StepsStage`, `SequentialStage`, `ParallelStage`가 `TemplateResolver`/`IterationResolver`를 사용해 직접 확장한다. Groovy 소스는 Maven이 컴파일하지 않고 Jenkins가 런타임에 로드한다.

## 빌드

```bash
mvn clean package -DskipTests
# target/stagefy.hpi 생성
```

또는 Makefile 사용:

```bash
make build          # mvn clean package -DskipTests
make test           # mvn test
make clean          # mvn clean
make docker-build   # build 후 Docker 이미지 생성
make docker-run     # Jenkins 컨테이너 실행
```

## 사용법

```groovy
// Jenkinsfile
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                script {
                    stagefy.run('Jenkins.yml', 'build')
                }
            }
        }
    }
}
```

```yaml
# Jenkins.yml
build:
  steps:
    - sh: "make build"
    - sh: "make test"
```

`stagefy.run(file, stage)`는 워크스페이스의 YAML 파일을 읽어 지정한 스테이지를 `stage(...)` 블록으로 감싸 실행한다. 선언형 파이프라인에서는 `script {}` 블록 없이 `steps {}`에서 `stagefy file: 'Jenkins.yml', stage: 'build'`로 바로 호출할 수도 있다.

## YAML reference

스테이지는 YAML 파일의 최상위 키이다. 스테이지의 종류는 다음 키 중 하나로 결정된다.

| 키 | 의미 |
|-----|---------|
| `steps` | 스텝 목록을 순서대로 실행 |
| `stages` | 다른 스테이지를 순차 실행 |
| `parallels` | 다른 스테이지를 병렬 실행 |
| `template` | 재사용 가능한 템플릿 선언 (직접 실행 불가) |

`steps` 스테이지의 선택 키: `when` (Groovy 표현식, false이면 스테이지를 건너뜀), `env` (환경 변수 맵), `node` (실행할 노드 레이블).

### Steps

| 스텝 | 설명 |
|------|-------------|
| `sh: "cmd"` | 셸 명령 실행. `${env.VAR}`는 빌드 환경에서 치환되며, 일반 이름은 셸 변수 참조(`${VAR}`)로 전달되므로 값이 셸 코드로 해석되지 않는다. |
| `bat: "cmd"` / `powershell: "cmd"` / `pwsh: "cmd"` | `bat` / `powershell` / `pwsh` 스텝으로 명령 실행. `${env.VAR}`는 `%VAR%`(bat) 또는 `$env:VAR`(PowerShell)로 전달된다. |
| `script: path` | Groovy 파일을 로드하여 `main()` 호출 |
| `evaluate: expr` | Groovy 표현식 평가 |
| `setEnvFromFile: file` | YAML 파일의 `env:` 맵을 빌드 환경으로 읽어 들임 |
| `use: Stage from file.yml` | 다른 파일에 정의된 스테이지 실행. `makeStage: false`를 추가하면 `stage(...)` 래핑을 생략 |
| `template: name` | 템플릿 확장. 인자는 같은 레벨의 키로 전달 |
| `iterated:` | 항목마다 템플릿을 한 번씩 확장 (아래 참조) |

> **보안:** `script`, `evaluate`, `when`은 임의의 Groovy를 Script Security 샌드박스 밖에서 실행한다. 신뢰할 수 있는 출처의 YAML 파일과 스크립트만 로드할 것.
>
> 템플릿 인자(`{name}`)는 Jenkinsfile의 `${...}` 보간처럼 `sh` 스텝에 텍스트로 그대로 삽입된다. 값이 외부에서 오는 경우(브랜치 이름, 빌드 파라미터, `over: env.VAR`)에는 스텝에서 따옴표로 감싸고(`sh: "deploy.sh '{name}'"`) 값에 따옴표나 셸 문법이 포함될 수 없도록 해야 한다. `${env.VAR}`는 다르다. 셸 변수로 전달되며 코드로 해석되지 않는다.

### Stage composition

```yaml
all:
  stages:
    - build
    - test from common.yml        # 다른 파일의 스테이지
    - template: notify            # 스테이지로 사용한 템플릿
      channel: ci
```

`stages:` 대신 `parallels:`를 쓰면 항목을 병렬로 실행한다. 스테이지 이름은 한 블록 안에서 유일해야 하며, 순환 참조는 거부된다.

### Templates and iteration

```yaml
deploy:
  template:
    arguments: [target]
  steps:
    - sh: "./deploy.sh {target}"

release:
  stages:
    - iterated:
        template: deploy
        over: [dev, staging]      # 또는: env.TARGETS  (쉼표로 구분)
```

인자는 템플릿 스텝 안에서 `{name}`으로 참조한다. `over`는 스칼라 목록, 맵 목록(각 키가 인자가 되며, 선택적 `_name` 키로 생성되는 스테이지 이름을 지정), 또는 쉼표로 구분된 목록을 담은 `env.VAR_NAME`일 수 있다. 각 항목은 `<template>_<item>` 이름의 스테이지를 만든다. 템플릿에는 스텝만 포함할 수 있으며 `template`이나 `iterated`를 다시 포함할 수 없다.

## Troubleshooting

| 메시지 | 원인 |
|---------|-------|
| `Stage 'X' not found in 'file'` | 스테이지 이름이 YAML 파일의 최상위 키가 아니거나, 파일이 워크스페이스에 없다 (먼저 checkout). |
| `Stage 'X' has no 'stages', 'parallels', or 'steps' key` | 스테이지에 세 가지 본문 키가 하나도 없다. |
| `Unknown step directive 'x' in stage 'X'` | 스텝이 `sh`, `bat`, `powershell`, `pwsh`, `script`, `setEnvFromFile`, `evaluate`, `use`, `template`, `iterated` 이외의 키를 사용했다. |
| `Template 'T' requires argument 'a' ...` | `template` 스텝이나 `iterated` 항목이 선언된 인자를 모두 제공하지 않았다. |
| `Circular Loop Execution ...` | 스테이지가 서로를 순환 참조한다. |
| `Template stage 'T' cannot be executed directly` | 템플릿은 `template` 또는 `iterated`를 통해서만 사용할 수 있다. |

각 YAML 파일은 빌드당 한 번만 파싱되어 캐시된다. 빌드 중에 생성한 파일은 처음 사용할 때 읽히지만, 이미 읽은 파일은 수정하지 않는다. 수정 내용이 반영되지 않는다.

## 요구 사항

| 항목 | 버전 |
|------|------|
| Java | 17+ |
| Maven | 3.9+ |
| 필수 플러그인 | `workflow-cps`, `workflow-step-api`, `pipeline-stage-step`, `pipeline-utility-steps` |

## Docker 테스트

플러그인이 번들된 Jenkins 이미지를 빌드하여 로컬 테스트:

```bash
make docker-build   # Docker 이미지 빌드
make docker-run     # localhost:8080 에서 Jenkins 실행
```

## 관련 문서

- [설치 가이드](install.ko.md)
- [배포 가이드](deploy.ko.md)
- [변경 이력](CHANGELOG.ko.md)

## 기여

이슈와 풀 리퀘스트를 환영한다. [CONTRIBUTING.ko.md](CONTRIBUTING.ko.md)를 참조하고, 주요 변경 사항은 [CHANGELOG.ko.md](CHANGELOG.ko.md)에 기록한다.

## 라이선스

[MIT License](LICENSE)
