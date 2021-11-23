![Github Actions](https://github.com/laiboonh/repairs-backend/actions/workflows/ci_cd.yml/badge.svg)

# Setup

- Install JDK via sdkman https://sdkman.io/
- Target environment is Heroku which requires `system.properties` file to define JDK version. This should be the version
  you should install. Same version is used in Github action

# Development

- Install https://github.com/trobert/skunk-intellij if using Intellij
- `sbt test` to run all tests