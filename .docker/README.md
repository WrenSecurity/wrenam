<p align="center">
  <img alt="Wren:AM logo" src="https://user-images.githubusercontent.com/13997406/204992591-3148357d-5a58-47fa-9be8-0da2460976a6.png" width="50%">
</p>

# Wren:AM

[![Organization Website](https://img.shields.io/badge/organization-Wren_Security-c12233)](https://wrensecurity.org)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/WrenSecurity)
[![License](https://img.shields.io/badge/license-CDDL-blue.svg)](https://github.com/WrenSecurity/wrenam/blob/main/LICENSE)
[![Source Code](https://img.shields.io/badge/source_code-GitHub-6e40c9)](https://github.com/WrenSecurity/wrenam)
[![Contributing Guide](https://img.shields.io/badge/contributions-guide-green.svg)](https://github.com/WrenSecurity/wrensec-docs/wiki/Contributor-Guidelines)

Wren:AM is an "all-in-one" access management solution providing strong and adaptive authentication, authorization, single sign-on (SSO), entitlements, federation and web services security.

Wren:AM provides mobile support out of the box, with full OAuth 2.0 and OpenID Connect (OIDC) support - modern protocols that
provide the most efficient method for developing secure native or web-based mobile applications optimized for bandwidth and
CPU.

Wren:AM is one of the projects in the Wren Security Suite, a community initiative that adopted open‐source projects
formerly developed by ForgeRock, which has its own roots in Sun Microsystems’ products.


# How to use this image

You can run Wren:AM with the embedded sample configuration with the following command:

```shell
docker run --rm --name wrenam-test -p 8080:8080 --add-host wrenam.wrensecurity.local:127.0.1.1 \
  -e WRENAM_ADMIN_PASSWORD=password -e WRENAM_INIT_DIR=/srv/wrenam-init/sample \
  -it wrensecurity/wrenam:latest
```

You will need to set *wrenam.wrensecurity.local* to resolve to 127.0.0.1 in your [hosts file](https://en.wikipedia.org/wiki/Hosts_(file))
so you can open [http://wrenam.wrensecurity.local:8080/auth](http://wrenam.wrensecurity.local:8080/auth) in your browser.

Note, that the *sample configuration* contains *unsafe configuration values* and **MUST
NOT** be used as a basis for production deployment.


The following environment variables can alter the default behavior:

* `WRENAM_ADMIN_PASSWORD` - `amAdmin` user's password string (setting it via file in production environment is preferred)
* `WRENAM_ADMIN_PASSWORD_FILE` - path to a file containing `amAdmin` user's password (default `$WRENAM_BASE_DIR/amadmin.pwd`)
* `WRENAM_SERVER_URL` - server instance base URL (default `http://wrenam.wrensecurity.local:8080`}
* `WRENAM_DEPLOYMENT_URI` - servlet context path (default `/auth`)
* `WRENAM_BASE_DIR` - base configuration directory (default `/srv/wrenam`)
* `WRENAM_INIT_DIR` - initial configuration directory (default `/srv/wrenam-init`)
* `WRENAM_INIT_DISABLE` - disable automatic server configuration

Automatic server configuration requires one of `WRENAM_ADMIN_PASWORD` or `WRENAM_ADMIN_PASSWORD_FILE` to be set.


# Automatic initialization

The image can automatically perform new platform configuration, upgrade existing server or make a new Wren:AM
server join the existing platform deployment. Automatic initialization can be disabled by setting the `WRENAM_INIT_DISABLE`
environment property to any value.

When the server is not yet initialized (i.e. there is no `$WRENAM_BASE_DIR$WRENAM_DEPLOYMENT_URI` directory),
the entry point script checks for presence of `init.properties` file within the `$WRENAM_INIT_DIR` and
calls SSO Configuration Tool. This initializes new server by either creating new platform configuration
or by joining existing platform deployment (depending in the contents of [`init.properties`](https://github.com/WrenSecurity/wrenam/blob/main/openam-distribution/openam-distribution-ssoconfiguratortools/src/main/assembly/config/sampleconfiguration)).

Right after the server's initial configuration the entry point script checks for the presence of `config.batch`
file within the `$WRENAM_INIT_DIR` directory. When the batch file is present, the SSO Admin Tools execute it with
the `do-batch` command using `amAdmin` user with credentials from `$WRENAM_ADMIN_PASSWORD_FILE`.


# Acknowledgments

Wren:AM is standing on the shoulders of giants and is a continuation of a prior work:

* OpenSSO by Sun Microsystems
* OpenAM by ForgeRock AS

We'd like to thank them for supporting the idea of open-source software.


# Disclaimer

Please note that the acknowledged parties are not affiliated with this project.
Their trade names, product names and trademarks should not be used to refer to
the Wren Security products, as it might be considered an unfair commercial
practice.

Wren Security is open source and always will be.

[contribute]: https://github.com/WrenSecurity/wrensec-docs/wiki/Contributor-Guidelines
