# Stagefy Jenkins Plugin

[English](README.md)

YAML 기반 파이프라인 스테이지 로더를 Jenkins 플러그인(hpi)으로 패키징한 것. `stagefy` 글로벌 변수를 제공하여 Jenkinsfile에서 바로 사용할 수 있다.

## pipeline-as-yaml과의 관계

[pipeline-as-yaml](https://plugins.jenkins.io/pipeline-as-yaml/)은 파이프라인 전체를 YAML로 정의한다. Stagefy는 이를 대체하지 않고 보완하며, **stage 단위**로 YAML을 재사용(`use`, `template`, `iterated` 디렉티브)하는 것이 차별점이다.

## 아키텍처

```
StagefyGlobalVariable (Java, @Extension)
  └─ StagefyDsl.groovy          # stagefy.run(file, stage) 진입점
      └─ DslJenkinsContext       # Jenkins DSL 래퍼
      └─ core/
          ├─ StageFactory        # YAML → Stage 객체 변환
          ├─ Stage / Step        # 추상 베이스 클래스
          ├─ StepsStage          # 순차 스텝 실행 (steps 키)
          ├─ SequentialStage     # 순차 스테이지 (stages 키)
          ├─ ParallelStage       # 병렬 스테이지 (parallels 키, iterated 지원)
          └─ StepFactory         # YAML → Step 객체 변환
              ├─ ShStep
              ├─ ScriptStep
              ├─ EvaluateStep
              ├─ SetEnvFromFileStep
              └─ UseStageStep    # 외부 YAML 참조 (use 디렉티브)
      └─ util/
          ├─ EnvResolver         # ${env.VAR} 치환
          ├─ Template            # 템플릿 정의 모델
          ├─ TemplateResolver    # template + arguments 처리
          ├─ IterationResolver   # iterated 반복 처리
          └─ StageNameBuilder    # 스테이지 이름 생성
```

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

지원 스텝: `sh`, `script`, `evaluate`, `setEnvFromFile`, `use` (`use: Stage from file.yaml`, 선택적 `makeStage: false`). `stagefy.run(file, stage)`는 지정한 스테이지를 `stage(...)` 블록으로 감싸 실행한다. YAML 스키마(`stages`, `parallels`, `template`, `iterated`, `when`, `env`, `node`)는 [영문 README](README.md#yaml-reference)를 참조.

## 요구 사항

| 항목 | 버전 |
|------|------|
| Jenkins | 2.541.1+ |
| Java | 17+ |
| Maven | 3.9+ |
| 필수 플러그인 | workflow-cps, pipeline-stage-step, pipeline-utility-steps |

## Docker 테스트

플러그인이 번들된 Jenkins 이미지를 빌드하여 로컬 테스트:

```bash
make docker-build   # Docker 이미지 빌드
make docker-run     # localhost:8080 에서 Jenkins 실행
```

## 관련 문서

- [설치 가이드](install.ko.md)
- [배포 가이드](deploy.ko.md)

## 라이선스

[MIT License](LICENSE)
