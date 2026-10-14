# Changelog

[한국어](CHANGELOG.ko.md)

Notable changes to this project. Versions are published through the CD workflow; entries are grouped under *Unreleased* until a release is cut.

## Unreleased

### Breaking
- Removed the `modules` key on `steps` stages (environment-modules loading). Run `module load ...` inside an `sh` step instead.

### Security
- `${env.VAR}` in `sh` steps is passed as a shell variable reference, so values are never interpreted as shell code.
- Documented that `script`, `evaluate` and `when` run Groovy outside the Script Security sandbox.
- Documented that template arguments (`{arg}`) are inlined into `sh` steps, like `${...}` in a Jenkinsfile.

### Fixed
- Stage names containing `from` (e.g. `deploy_from_s3`) are no longer split as `name from file` references.
- `error()` always throws, so validators cannot continue with null data.

### Changed
- Stage ENTER/EXIT messages are written to the build console.

## Earlier
- Initial plugin: migrated from the shared-library implementation, with Jenkins hosting, CD and security-scan setup.
