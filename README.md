![Github Actions](https://github.com/laiboonh/repairs-backend/actions/workflows/ci_cd.yml/badge.svg)

# Setup

- Install JDK via sdkman https://sdkman.io/
- Heroku defaults to JDK 1.8 so this is the version that we will be using. Same version is used in Github action
- Use instructions from https://devcenter.heroku.com/articles/getting-started-with-scala to set up heroku client

# History

- `system.properties` was added to specify JDK to be used in Heroku
- `plugins.sbt` add in sbt-native-packager plugin for Heroku

# Development

- Install https://github.com/trobert/skunk-intellij if using Intellij
- `sbt test` to run all tests

# Run Locally

### sbt-native-packager

- To test run it locally `sbt stage` followed by `./target/universal/stage/bin/repairs-backend`
- Visit `localhost:8080/api/...` to verify that your app is up

### heroku

- `sbt compile stage`
- `heroku local web`