<!--
  The contents of this file are subject to the terms of the Common Development and
  Distribution License (the License). You may not use this file except in compliance with the
  License.

  You can obtain a copy of the License at legal/CDDLv1.1.txt. See the License for the
  specific language governing permission and limitations under the License.

  When distributing Covered Software, include this CDDL Header Notice in each file and include
  the License file at legal/CDDLv1.1.txt. If applicable, add the following below the CDDL
  Header, with the fields enclosed by brackets [] replaced by your own identifying
  information: "Portions copyright [year] [name of copyright owner]".

  Copyright 2026 Wren Security. All rights reserved.
-->

# AGENTS.md

Guidance for AI agents (and humans) working in this repository. Start with
[`ARCHITECTURE.md`](ARCHITECTURE.md) for the module map, request handling and the main concepts.

## Build and test

Requirements: JDK 17 or later, Maven 3. The build also runs npm for the XUI (`openam-ui/openam-ui-ria`).

```sh
mvn -B verify                              # full build with tests, as in CI (.github/workflows/build-project.yml)
mvn -B package -DskipTests                 # faster full build
mvn -B verify -pl openam-core -am          # one module and what it depends on
mvn -B test -pl openam-oauth2 -Dtest=AuthorizationCodeGrantTypeHandlerTest    # one test class
docker build -t wrenam:local .             # Docker image from the sources (Dockerfile)
```

The deployable WAR is built by `openam-server` (`openam-server/target/`).

Tests use TestNG with Mockito and AssertJ (the Duo module uses JUnit 5). Put tests next to the code
under `src/test/java` in the same package. Checkstyle runs in the build
(`checkstyle/suppressions.xml` for suppressions).

## Code layout and conventions

* Follow the [Java Style Guide](https://github.com/WrenSecurity/wrensec-docs/wiki/Java-Style-Guide)
  and the [JavaScript Style Guide](https://github.com/WrenSecurity/wrensec-docs/wiki/JavaScript-Style-Guide);
  where they leave a choice open, follow the surrounding code. The
  [Contributor Guidelines](https://github.com/WrenSecurity/wrensec-docs/wiki/Contributor-Guidelines)
  cover the pull request process.
* New code goes to `org.forgerock.openam.*` or `org.wrensecurity.*` packages, not to the legacy
  `com.sun.identity.*` / `com.iplanet.*` packages unless it changes existing code there.
* Every source file starts with the CDDL header in the format of the Java Style Guide (section 3.1),
  referring to `legal/CDDLv1.1.txt`. New files get `Copyright [year] Wren Security. All rights
  reserved.`. When changing an existing file, add `Portions Copyright [year] Wren Security.` below
  the other copyright lines, or extend the years of an existing one (`2017-2026`); never remove or
  reformat the copyright lines of others.
* Wire new components with Guice rather than new singletons: a class extending `AbstractModule`,
  annotated `@GuiceModule` and listed in `META-INF/services/com.google.inject.AbstractModule`
  (some older modules use `com.google.inject.Module`). Prefer constructor injection (an `@Inject`
  constructor taking all dependencies) over field or setter injection and over looking up
  `InjectorHolder` - it makes the dependencies explicit and lets tests pass mocks to the
  constructor.
* Administration belongs to the REST API and XUI. Do not add features to the legacy console
  (`openam-console`, JATO) - it is to be removed (`ARCHITECTURE.md`, Administration).
* REST endpoints: add a route in an existing provider (e.g. `CoreRestRouteProvider`) or a new
  `*RestRouteProvider` extending `org.forgerock.openam.rest.AbstractRestRouteProvider`, registered in
  `META-INF/services/org.forgerock.openam.rest.RestRouteProvider` - its routes go under `/json`
  with the shared authentication, realm and audit handling. Do not add a new
  `org.forgerock.openam.http.HttpRouteProvider` for an API; those are the top-level routes
  (`/json` itself, the legacy `/frrest`, the STS). Routes require an SSO token unless explicitly
  made public. Describe resources for the API explorer: a JSON schema next to the resource and texts in
  `openam-i18n/src/main/resources/api-descriptor/`.
* Configuration: settings belong to a service schema in the resources of the module that owns
  the service (e.g. `openam-oauth2/.../OAuth2Provider.xml`, authentication modules
  `amAuth<Name>.xml`; the inherited core services are in
  `openam-server-only/src/main/resources/services/`) - with display names and help in the property file of the same name (e.g.
  `amAuth.properties` for `amAuth.xml`). New services are registered in
  `openam-server-only/src/main/resources/config/serviceNames.properties` and
  `schemaNames.properties`. New attribute names follow the style of their service: in services
  with prefixed names (`iplanet-am-...`, `openam-...`) use the `wrensec-am-` prefix (e.g.
  `wrensec-am-auth-<module>-...`), not the inherited `iplanet-am-` or `forgerock-am-`; in
  services with plain camelCase names (e.g. `OAuth2Provider`, `RadiusServerService`) use
  camelCase without a prefix. The prefix is inherited from the time when service attributes
  were stored as LDAP attributes; it matters for user store attributes (see below). Exception:
  an authentication module's level attribute must be named as
  `AMAuthConfigUtils.getAuthLevelAttribute` expects (`iplanet-am-auth-<module>-auth-level`,
  `forgerock-am-auth-<module>-auth-level` or `sunAMAuth<Module>AuthLevel`).
  Configuration changes for existing installations need an upgrade step (`openam-upgrade`).
* User store schema: new user attributes go to `idRepoService.xml` and to an LDIF for each
  supported directory under `openam-server-only/src/main/webapp/WEB-INF/template/ldif/`
  (`opendj`, `odsee`, `ad`, `adam`, `tivoli`), with an upgrader in
  `org.forgerock.openam.upgrade.DirectoryContentUpgrader` for existing installations.
* New debug log names are registered in `openam-core/src/main/resources/debugfiles.properties`.
* Dependencies: the build verifies PGP signatures of all dependencies against the Wren Security
  key whitelist (`pgpVerifyKeysVersion` in `pom.xml`). A new dependency needs its signing key in
  the whitelist first - a separate change to `wrensec-pgp-whitelist`, then a version bump here.
* User-facing text goes to resource bundles, not into code; XUI texts to
  `openam-ui/openam-ui-ria/src/main/resources/locales/en/translation.json`.
* XML parsing: use the factories in `com.sun.identity.shared.xml.XMLUtils`
  (`getSafeDocumentBuilder`, `getSafeSAXParser`), never a raw `DocumentBuilderFactory`.
* LDAP filters and DNs built from input must be escaped (`Filter` builders,
  `escapeAssertionValue`).
* Outbound HTTPS validates the server certificate and host name; do not add trust-all
  `TrustManager` or `HostnameVerifier` implementations.
* Click pages (`WEB-INF/click.xml`) that must not be reachable once the server is configured
  extend `ProtectedPage`, not `AjaxPage`.

### Adding an authentication module

Specific to authentication modules; the general conventions above apply as well. The Duo module
(`wrenam-auth-duo`) shows steps 1-4, the push modules (`openam-auth-push`) also step 5.

1. A Maven module `openam-authentication/wrenam-auth-<name>` (package
   `org.wrensecurity.wrenam.authentication.modules.<name>`), added to
   `openam-authentication/pom.xml` and as a dependency of `openam-server-only/pom.xml`.
2. The login module, a subclass of `AMLoginModule`, with its service schema `amAuth<Name>.xml`
   and `amAuth<Name>.properties` in the module. The schema needs `resourceName` on `<Schema>`
   and on attributes, and `resourceName="USE-PARENT"` on sub-schemas - without them the REST API
   and XUI cannot manage the module.
3. The module class in the authenticators list of `services/amAuth.xml`, and in the upgrade
   helper `AuthServiceHelper` (`openam-upgrade`) so existing installations get it.
4. The stages: callback definitions in
   `openam-server-only/src/main/webapp/config/auth/default/<Module>.xml` (its `moduleName` equals
   the module class's simple name) and, where the generic
   rendering is not enough, XUI templates
   `openam-ui/openam-ui-ria/src/main/resources/templates/openam/authn/<Module><stage>.html`.
5. If users register devices: a devices resource under
   `openam-core-rest/.../core/rest/devices/` for `/json/users/{user}/devices/<type>`, shown in the
   XUI user dashboard, with the user store schema as described above.

## Security

* Report suspected vulnerabilities privately as described in the Wren Security
  [security policy](https://github.com/WrenSecurity/.github/blob/main/SECURITY.md) - not in public
  issues or pull requests.
* Scripts managed by administrators run on the server (Groovy without a sandbox; see
  `ARCHITECTURE.md`, Scripting) - treat changes to who may manage or run scripts as
  security-relevant.
* Many functions have parallel, often legacy, paths (`ARCHITECTURE.md`, Request handling) - e.g.
  login, self-service and CTS tokens. A security check added or changed on one path must hold on
  the others too, or the others must be removed. CTS contents are for `amadmin` only on every
  interface.
