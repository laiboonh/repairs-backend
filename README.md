![Github Actions](https://github.com/laiboonh/repairs-backend/actions/workflows/ci_cd.yml/badge.svg)

# Setup

- Install JDK via sdkman https://sdkman.io/
- Heroku defaults to JDK 1.8 so this is the version that we will be using. Same version is used in Github action
- Install `direnv` https://direnv.net/docs/installation.html
- Create `.envrc` file with content `export DATABASE_URL=postgres://<username>:<password>@<host>:<port>/<dbname>`
- Install Postgres 13.5 (default Heroku version) https://www.enterprisedb.com/downloads/postgres-postgresql-downloads
- (Optional) Use instructions from https://devcenter.heroku.com/articles/getting-started-with-scala to set up heroku
  client

# Development

- Install https://github.com/trobert/skunk-intellij if using Intellij
- `sbt test` to run all tests

# Run Locally

- Run App.scala `sbt run App`
- Visit `localhost:8080/api/...` to verify that your app is up

# History

- `plugins.sbt` add in sbt-native-packager plugin for Heroku
- Environment variable `PORT` is to be used in heroku to bind to web server
- Heroku also requires web server `host` to be `0.0.0.0`