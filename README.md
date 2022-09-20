<h1 align="center">
  blindnet devkit<br />
  Privacy Computation Engine
</h1>

<p align=center><img src="https://user-images.githubusercontent.com/7578400/163277439-edd00509-1d1b-4565-a0d3-49057ebeb92a.png#gh-light-mode-only" height="80" /></p>
<p align=center><img src="https://user-images.githubusercontent.com/7578400/163549893-117bbd70-b81a-47fd-8e1f-844911e48d68.png#gh-dark-mode-only" height="80" /></p>

<p align="center">
  <strong>Interpret sensitive data access rights and handle Privacy Requests.</strong>
</p>

<p align="center">
  <a href="https://blindnet.dev"><strong>blindnet.dev</strong></a>
</p>

<p align="center">
  <a href="https://blindnet.dev/docs">Documentation</a>
  &nbsp;•&nbsp;
  <a href="https://github.com/blindnet-io/privacy-computation-engine/issues">Submit an Issue</a>
  &nbsp;•&nbsp;
  <a href="https://join.slack.com/t/blindnet/shared_invite/zt-1arqlhqt3-A8dPYXLbrnqz1ZKsz6ItOg">Online Chat</a>
  <br>
  <br>
</p>

## About

The blindnet devkit **Privacy Computation Engine** (PCE) is the core of your "privacy stack".

It is a service delivering Restful APIs to manage the two core features of the DevKit by:

1. interpreting your rights to hold and treat a particular **[Data Capture](https://blindnet.dev/docs/references/lexicon#data-capture)** at a particular point in time
2. calculating a response to [Data Subjects'](/docs/references/lexicon#data-subject) **[Privacy Requests](https://blindnet.dev/docs/references/lexicon#privacy-request)**.

You'll find its full documentation in the [Computation](https://blindnet.dev/docs/computation) section of [blindnet.dev](https://blindnet.dev).

## Get Started

:rocket: Check out our [introductory tutorial](https://blindnet.dev/docs/tutorial) to familiarize yourself with the blindnet devkit components and understand how they play together.

## Usage

See [/swagger](https://devkit-pce-staging.azurewebsites.net/swagger/) for complete and up-to-date OpenAPI references and documentation.

### Requirements

To run the Privacy Computation Engine locally, make sure you have installed the latest versions of the following tools:

- [sbt](https://www.scala-sbt.org/1.x/docs/Setup.html)
- [docker-compose](https://docs.docker.com/compose/install/)

> **Warning**
>
> Following instructions extensively use Docker.
>
> Make sure the Docker daemon is running and accessible to your current user before anything.
>
> When using Systemd, you can run `sudo systemctl status docker` to check the status of the Docker daemon, and `sudo systemctl start docker` to start it.

### Run Locally

In the root directory, run the following `sbt` command to create a docker image for the application:

```bash
sbt docker:publishLocal
```

Then, run the `scripts/start.sh` script to start a Postgres instance, execute database migrations and run the Privacy Computation Engine:

```bash
./scripts/start.sh
```

Finally, after this script has been executed successfully, you can verify the service is running and available by calling:

```bash
curl -v localhost:9000/v0/health
```

When you're done, make sure to stop and clean up all associated docker containers with:

```bash
./scripts/stop.sh
```

> **Note**
>
> Environment variables are defined in the `.env` file.

### Configuration

Configuration of the PCE can be achieved using its Configuration API. Refer to the [associated section of the documentation](https://blindnet.dev/docs/computation/configuration) for more information.

A default configuration with example values can be found in the [`init-config.sh`](./scripts/init-config.sh) script.

Change the values in this example to fit your specific needs, then run it:

```bash
./scripts/init-config.sh
```

### Development

First, start a Postgres instance and populate it with test data:

```bash
./scripts/start-dev.sh
```

Then, define the required environment variables in your local environment with default values:

```bash
source ./scripts/dev-env.sh
```

Finally, in the same terminal session, start the Privacy Computation Engine with:

```bash
sbt "~core/reStart"
```

When you're done, make sure to stop and clean up the database docker container with:

```bash
./scripts/stop-dev.sh
```

> **Note**
>
> When run in development mode, the PCE database is automatically populated with a default configuration.
> See [./scripts/insert-dev.sql](./scripts/insert-dev.sql) for details.

### Environment Variables

| Name             | Description              | Example                              | Default |
| ---------------- | ------------------------ | ------------------------------------ | :------ |
| API_HOST         | HTTP host                | localhost                            | 0.0.0.0 |
| API_PORT         | HTTP port                | 80                                   | 9000    |
| DB_URI           | jdbc connection string   | jdbc:postgresql://localhost:5432/pce |         |
| DB_USER          | database user            | postgres                             |         |
| DB_PASS          | database user's password | mysecretpassword                     |         |
| APP_CALLBACK_URI | callback api prefix      | localhost:9000/v0                    |         |

## Contributing

Contributions of all kinds are always welcome!

If you see a bug or room for improvement in this project in particular, please [open an issue][new-issue] or directly [fork this repository][fork] to submit a Pull Request.

If you have any broader questions or suggestions, just open a simple informal [DevRel Request][request], and we'll make sure to quickly find the best solution for you.

## Community

> All community participation is subject to blindnet’s [Code of Conduct][coc].

Stay up to date with new releases and projects, learn more about how to protect your privacy and that of our users, and share projects and feedback with our team.

- [Join our Slack Workspace][chat] to chat with the blindnet community and team
- Follow us on [Twitter][twitter] to stay up to date with the latest news
- Check out our [Openness Framework][openness] and [Product Management][product] on Github to see how we operate and give us feedback.

## License

The blindnet devkit privacy-computation-engine is available under [MIT][license] (and [here](https://github.com/blindnet-io/openness-framework/blob/main/docs/decision-records/DR-0001-oss-license.md) is why).

<!-- project's URLs -->

[new-issue]: https://github.com/blindnet-io/privacy-computation-engine/issues/new/choose
[fork]: https://github.com/blindnet-io/privacy-computation-engine/fork

<!-- common URLs -->

[devkit]: https://github.com/blindnet-io/blindnet.dev
[openness]: https://github.com/blindnet-io/openness-framework
[product]: https://github.com/blindnet-io/product-management
[request]: https://github.com/blindnet-io/devrel-management/issues/new?assignees=noelmace&labels=request%2Ctriage&template=request.yml&title=%5BRequest%5D%3A+
[chat]: https://join.slack.com/t/blindnet/shared_invite/zt-1arqlhqt3-A8dPYXLbrnqz1ZKsz6ItOg
[twitter]: https://twitter.com/blindnet_io
[docs]: https://blindnet.dev/docs
[changelog]: CHANGELOG.md
[license]: LICENSE
[coc]: https://github.com/blindnet-io/openness-framework/blob/main/CODE_OF_CONDUCT.md
