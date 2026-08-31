# Security Policy

## Supported versions

This is an actively developed portfolio project. Only the latest `main` branch
receives security fixes.

## Reporting a vulnerability

**Please do not open a public issue for security vulnerabilities.**

Report privately through GitHub's
[private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability):
go to the **Security** tab of this repository and click **Report a vulnerability**.

Please include:

- a description of the issue and its impact,
- steps to reproduce,
- affected component (event generator, Flink processor, dashboard, infrastructure),
- any suggested fix, if you have one.

## What to expect

- Acknowledgement of your report within **7 days**.
- An assessment and, if confirmed, a fix on `main`.
- Credit in the release notes for the fix, unless you prefer to remain anonymous.

## Scope

This project is a learning and demonstration pipeline. It is **not intended for
production use** and ships with intentionally simplified security (e.g. a
single-node Kafka broker with no authentication, local-only Docker networking).
Issues that only apply to running this stack as-is in production are out of scope;
issues in the application code, dependencies, or CI configuration are in scope.
