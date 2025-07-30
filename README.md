<!--
  The contents of this file are subject to the terms of the Common Development and
  Distribution License (the License). You may not use this file except in compliance with the
  License.

  You can obtain a copy of the License at legal/license.txt. See the License for the
  specific language governing permission and limitations under the License.

  When distributing Covered Software, include this CDDL Header Notice in each file and include
  the License file at legal/license.txt. If applicable, add the following below the CDDL
  Header, with the fields enclosed by brackets [] replaced by your own identifying
  information: "Portions copyright [year] [name of copyright owner]".

  Copyright 2016 ForgeRock AS.
  Portions Copyright 2022 Wren Security.
-->

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13997406/204992546-6797a080-9245-47ec-81d3-300526ee2bb2.png">
    <source media="(prefers-color-scheme: light)" srcset="https://user-images.githubusercontent.com/13997406/204992591-3148357d-5a58-47fa-9be8-0da2460976a6.png">
    <img alt="Wren:AM logo" src="https://user-images.githubusercontent.com/13997406/204992591-3148357d-5a58-47fa-9be8-0da2460976a6.png" width="60%">
  </picture>
</p>

# Wren:AM

[![License](https://img.shields.io/badge/license-CDDL-blue.svg)](https://github.com/WrenSecurity/wrenam/blob/main/LICENSE)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/WrenSecurity)

Wren:AM is an "all-in-one" access management solution providing strong and adaptive authentication, authorization, single sign-on (SSO), entitlements, federation and web services security.

Wren:AM provides mobile support out of the box, with full OAuth 2.0 and OpenID Connect (OIDC) support - modern protocols that
provide the most efficient method for developing secure native or web-based mobile applications optimized for bandwidth and
CPU.

Wren:AM is one of the projects in the Wren Security Suite, a community initiative that adopted open‐source projects
formerly developed by ForgeRock, which has its own roots in Sun Microsystems’ products.

## Contributions

[![Contributing Guide](https://img.shields.io/badge/Contributions-guide-green.svg?style=flat)][contribute]
[![Contributors](https://img.shields.io/github/contributors/WrenSecurity/wrenam)][contribute]
[![Pull Requests](https://img.shields.io/github/issues-pr/WrenSecurity/wrenam)][contribute]
[![Last commit](https://img.shields.io/github/last-commit/WrenSecurity/wrenam.svg)](https://github.com/WrenSecurity/wrenam/commits/main)

## Getting the Wren:AM

You can get Wren:AM Web Application Archive (WAR) in couple of ways:

### Download binary release

The easiest way to get the Wren:AM is to download the latest binary [release](https://github.com/WrenSecurity/wrenam/releases).

### Build the source code

In order to build the project from the command line follow these steps:

**Prepare your Environment**

Following software is needed to build the project:

| Software  | Required Version |
| --------- | -------------    |
| OpenJDK   | 17 and above     |
| Git       | 2.0 and above    |
| Maven     | 3.0 and above    |

**Build the source code**

All project dependencies are hosted in JFrog repository and managed by Maven, so to build the project simply execute Maven *package* goal.

```
$ cd $GIT_REPOSITORIES/wrenan
$ mvn clean package
```

Built binary can be found in `${GIT_REPOSITORIES}/wrenam/openam/openam-server/target/OpenAM-${VERSION}.war`.

### Docker image

You can also run Wren:AM in a Docker container. Official Wren:AM Docker images can be found [here](https://hub.docker.com/r/wrensecurity/wrenam).


## Documentation

Project documentation can be found in our documentation platform ([docs.wrensecurity.org](https://docs.wrensecurity.org/wrenam/latest/index.html)).

Documentation is still work in progress.

## Acknowledgments

Wren:AM is standing on the shoulders of giants and is a continuation of a prior work:

* OpenSSO by Sun Microsystems
* OpenAM by ForgeRock AS

We'd like to thank them for supporting the idea of open-source software.

## Disclaimer

Please note that the acknowledged parties are not affiliated with this project.
Their trade names, product names and trademarks should not be used to refer to
the Wren Security products, as it might be considered an unfair commercial
practice.

Wren Security is open source and always will be.

[contribute]: https://github.com/WrenSecurity/wrensec-docs/wiki/Contributor-Guidelines
