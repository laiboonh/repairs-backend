![Github Actions](https://github.com/laiboonh/repairs-backend/actions/workflows/ci_cd.yml/badge.svg)

# Setup

- Install JDK via sdkman https://sdkman.io/
- Heroku defaults to JDK 1.8 so this is the version that we will be using. Same version is used in Github action
- Install SBT via sdkman
- Install `direnv` https://direnv.net/docs/installation.html
- Copy `.envrc.template` to `.envrc` file and edit values to suit your environment
- Install Postgres 13.5 (default Heroku version) https://www.enterprisedb.com/downloads/postgres-postgresql-downloads
- (Optional) Use instructions from https://devcenter.heroku.com/articles/getting-started-with-scala to set up heroku
  client

# Development

- Install https://github.com/trobert/skunk-intellij if using Intellij
- `sbt test` to run all tests

# Run Locally

- `sbt run App`
- Visit `localhost:8080/api/...` to verify that your app is up

# Production Ops

To facilitate interaction with Postgres in Heroku

- `admin.Tasks` was created to facilitate this
- `sbt console` to have sbt console in Local environment
- `heroku run sbt console` to have a sbt console in Heroku environment
- Execute `AdminTasks.run` in sbt console

# History

- `plugins.sbt` add in sbt-native-packager plugin for Heroku
- Environment variable `PORT` is to be used in heroku to bind to web server
- Heroku also requires web server `host` to be `0.0.0.0`
- Heroku Postgres requires communication to be over SSL. So we have to turn on SSL for skunk session
- http://www.slf4j.org/codes.html#StaticLoggerBinder slf4j provider must be included in build path
- Added https://github.com/typelevel/kind-projector to support higher-kinded types
- POSTMAN script to setup global variable which will be used in other request for
  authorization `const token = pm.response.headers.find(h => h.key == 'Authorization').value; pm.globals.set("TOKEN", token);`