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

  Copyright 2026 Wren Security.
-->

# Wren:AM architecture

An overview for contributors and for tools that analyse the code: what the parts are, how a request
travels through them, and where to look. It describes the code as it is, including parts inherited
from OpenSSO and OpenAM that are older than the rest.

## Overview

Wren:AM is an access management server: authentication, single sign-on (SSO), authorization
(policies and entitlements), federation (SAML 2.0, WS-Federation and legacy SAML 1.x / ID-FF),
OAuth 2.0 and OpenID Connect provider, User-Managed Access (UMA) and a security token service
(STS).

* Java 17, Jakarta EE, deployed as one web application (WAR) into a servlet container - Tomcat 10
  in the official Docker image, under the context path `/auth` by convention.
* Maven multi-module build (`pom.xml`), parent `org.wrensecurity:wrensec-parent`.
* The code has three generations, still visible in package names and styles:
  * `com.sun.identity.*`, `com.iplanet.*` - OpenSSO: the core services, the service management
    framework, legacy federation, the JATO-based admin console;
  * `org.forgerock.openam.*` - OpenAM: REST API, OAuth 2.0, CTS, scripting, XUI, Guice wiring;
  * `org.wrensecurity.*` and newer changes - Wren:AM (e.g. the Duo authentication module).

## Modules

| Module | Content |
|---|---|
| `openam-server` | assembles the deployable WAR from `openam-server-only` and `openam-console` (`src/assemble/merge.xml`) |
| `openam-server-only` | the web application: `WEB-INF/web.xml`, service schemas (`src/main/resources/services/*.xml`), configuration templates, JSPs |
| `openam-console` | legacy admin console on unsupported libraries (JATO, Sun Web Console) - to be replaced, see [Administration](#administration) |
| `openam-core` | core services: authentication, sessions, identity repository, service management (SMS), policy, setup (`com.sun.identity.setup`) |
| `openam-shared` | utilities shared by server and client SDK (XML, encoding, debug, redirect URL validation) |
| `openam-rest`, `openam-core-rest` | REST API framework and the core REST resources (`/json/*`) |
| `openam-http`, `openam-restlet` | HTTP integration (routing, filters) and the Restlet customisation used by OAuth 2.0 |
| `openam-authentication` | authentication modules, one sub-module each (LDAP, AD, OATH, Push, SAML2, OAuth/social, Duo, ...) |
| `openam-oauth2`, `openam-oauth2-saml2`, `openam-uma` | OAuth 2.0 / OpenID Connect provider, SAML2 bearer grant, UMA |
| `openam-federation` | federation: protocol implementations (`openam-federation-library`), their integration into the server (`OpenFM`), IdP discovery, fedlet |
| `openam-entitlements` | policy and entitlement engine |
| `openam-coretoken` | Core Token Service (CTS): token persistence in LDAP |
| `openam-tokens` | token types and serialisation |
| `openam-scripting` | script engines (Rhino JavaScript, Groovy), script storage, sandbox |
| `openam-sts` | REST STS (in the server) and SOAP STS (separate WAR) |
| `openam-audit` | audit event handling and audit handler configuration |
| `openam-notifications*` | notification broker and websocket transport |
| `openam-selfservice` | user self-service (registration, forgotten password and username) |
| `openam-push-notification`, `openam-radius`, `openam-dashboard` | push authentication service, RADIUS server and client, user dashboard |
| `openam-datastore`, `openam-ldap-utils` | identity data store implementations, LDAP helpers |
| `openam-ui` | XUI, the JavaScript user interface (`openam-ui-ria`, built with npm and gulp) |
| `openam-common-auth-ui`, `openam-server-auth-ui` | authentication UI templates |
| `openam-cli`, `openam-clientsdk`, `openam-tools` | `ssoadm` command line tool, Java client SDK, build tools |
| `openam-upgrade`, `openam-schema` | configuration upgrade, schemas |
| `openam-annotations`, `openam-slf4j`, `openam-i18n`, `openam-test-utils` | supporting code |
| `openam-time-travel`, `openam-examples`, `openam-samples` | development and example code, not part of production builds |

## Runtime and configuration

* **Bootstrap.** On start the server looks for its bootstrap file in the configuration directory
  (`com.sun.identity.configuration.directory`, `/srv/wrenam` in the Docker image). Until the
  server is configured, `AMSetupFilter` routes requests to the configurator
  (`/config/configurator`, `com.sun.identity.setup.AMSetupServlet`), which installs the
  configuration store and writes the bootstrap files. The command-line configurator tools
  (`ssoconf`) drive the same servlet.
* **Configuration store.** All configuration lives in an LDAP directory - embedded Wren:DS or an
  external one - and is managed through the Service Management System (SMS): each service is
  described by an XML schema in `openam-server-only/src/main/resources/services/` (e.g.
  `amAuth.xml`, `amSession.xml`, `OAuth2Provider.xml`) with global, organisation (realm) and
  instance attributes. Server defaults are in
  `openam-server-only/src/main/webapp/WEB-INF/template/sms/serverdefaults.properties`.
* **Realms.** Configuration and identities are organised in realms, a tree under the top-level
  realm `/`. Each realm has its own authentication, data stores, policies, OAuth 2.0 and
  federation configuration.
* **Identity repository.** Users, groups and agents are read and written through the IdRepo
  layer (`com.sun.identity.idm`), with data store plugins for LDAPv3 directories (Wren:DS, Active
  Directory, ...) configured per realm. The LDAP schema Wren:AM needs in user stores is provided
  as LDIF per directory type (`WEB-INF/template/ldif/`) and upgraded by
  `DirectoryContentUpgrader`.
* **Sites.** Several servers can form a site behind a load balancer, sharing the configuration
  store and the CTS. A stateful session belongs to the server that created it; the load balancer
  cookie (`amlbcookie`, the server ID) keeps a client on that server. Other servers of the site
  read the session from the CTS (with session failover enabled) and send changes to the owning
  server over HTTP. An authentication in progress is likewise bound to one server
  (`AMAuthCookie`), and requests that arrive elsewhere are forwarded to it.
* **Dependency injection.** Newer code is wired with Guice: modules annotated `@GuiceModule` and
  listed in `META-INF/services/com.google.inject.AbstractModule` (or `com.google.inject.Module`),
  accessed through `InjectorHolder`. Older
  code uses singletons and factories.

## Request handling

Servlet and filter mappings are in `openam-server-only/src/main/webapp/WEB-INF/web.xml` (and
`openam-console/.../web.xml` for the console). Filters on all requests: `AMSetupFilter`,
`ResponseValidationFilter`, `SetHeadersFilter`, `AuditContextFilter`.

| Family | Paths | Implementation |
|---|---|---|
| REST API | `/json/*` | CREST resources behind a CHF router; routes contributed by `*RouteProvider` classes via `META-INF/services/org.forgerock.openam.http.HttpRouteProvider` (see `openam-rest/.../RestHttpRouteProvider.java`) |
| OAuth 2.0 / OIDC, UMA, XACML | `/oauth2/*`, `/uma/*`, `/xacml/*` | Restlet applications (`openam-oauth2/.../OAuth2RouterProvider.java`) |
| User interface | `/XUI/*` (XUI), `/UI/*` (legacy login), `/login`, `/logout` | XUI talks to the REST API |
| SAML 2.0 and other federation protocols | `/SSORedirect`, `/SSOPOST`, `/Consumer`, `/WSFederationServlet/*`, ... | servlets and JSPs in `openam-federation` and `openam-server-only` |
| Agent and client SDK services | `/sessionservice`, `/policyservice`, `/authservice`, `/namingservice`, `/notificationservice`, `/jaxrpc/*`, `/identityservices/*` | PLL (platform low level) XML-over-HTTP services and SOAP |
| Notifications | `/notifications` | websocket |
| Admin console and tools | console servlets, `/ssoadm.jsp`, `Debug.jsp`, ... | `openam-console`, JSPs in `openam-server-only` |

REST endpoints require an SSO token by default (an authentication filter on the router), read
from the session cookie or a header of the same name; individual routes opt out where the
operation is public (e.g. authentication). Resources describe themselves through API descriptors
(JSON schemas and texts in `openam-i18n`), which the API explorer in XUI shows.

## Administration

There are two administration user interfaces:

* **XUI** (`openam-ui/openam-ui-ria`, `admin` views) on top of the REST API - the direction for
  all administration. It covers realms (applications and agents, authentication, policies, data
  stores, identities, scripts, services, sessions), global configuration, and servers and sites.
* **The legacy console** (`openam-console`), built on JATO and the Sun Web Console components
  ("cc") - libraries that have been unmaintained for well over a decade (versions `2005-05-04`
  and `2008-08-08`), repackaged for Jakarta, with patched JATO classes kept in
  `openam-console/src/main/java/com/iplanet/jato`. Some administration still exists only here,
  e.g. federation entities and circles of trust, which XUI links to.

Everything is to move to the REST API and XUI, and the legacy console is to be removed. Do not
add features to the console; when a change touches functionality available only there, prefer
adding it to the REST API and XUI.

`ssoadm` (`openam-cli`) is the command-line administration tool; `ssoadm.jsp` exposes it over HTTP
and is disabled by default.

## Authentication

* An **authentication chain** is an ordered list of **modules**, each with a criteria flag
  (required, requisite, sufficient, optional). Modules are listed in
  `services/amAuth.xml`; each has its own service schema and lives in `openam-authentication` or
  `openam-core`.
* The engine is `com.sun.identity.authentication` (`AuthContext`, `LoginState`, the
  `AMLoginModule` base class). XUI drives it through `/json/authenticate`, a callback protocol in
  JSON; the legacy UI and the PLL `/authservice` reach the same engine.
* Each module's stages are described by callback definitions in
  `openam-server-only/src/main/webapp/config/auth/default/<Module>.xml`; XUI renders a stage with
  a matching template (`templates/openam/authn/<Module><stage>.html`) or a generic one. Modules may
  send client-side JavaScript to the browser (e.g. device fingerprinting).
* Authentication devices registered by users - OATH tokens, push devices, trusted devices - are
  stored as user attributes and managed through `/json/users/{user}/devices/*`
  (`openam-core-rest/.../core/rest/devices/`) and the XUI user dashboard.
* The realm of a request comes from the URL path - `/json/realms/root/realms/<subrealm>/...`
  (`RealmRoutingFactory`; `root` is the top-level realm, nested realms repeat `realms/<name>`;
  the older `/json/<realm>/...` form is legacy) and `#login/<realm>` in XUI - the `realm` query
  parameter, or a DNS alias configured for a realm. The chain or module to run
  can be selected by the client with login parameters (`service`, `module`, `authlevel`, ...);
  required strength is enforced through authentication levels, e.g. in policy conditions.
* After a successful login, post-authentication plugins (`AMPostAuthProcessInterface`) may run.
  Internal "special users" (`com.sun.identity.authentication.special.users`, e.g. `dsameuser`)
  are used by the server and its tools.
* Successful authentication creates a session.

## Sessions and tokens

* **SSO session.** Identified by the session cookie (`iPlanetDirectoryPro` by default). Stateful
  by default: the session lives on its home server, and also in the CTS when session failover is
  enabled. Stateless
  sessions, per realm, carry the session state in a signed and optionally encrypted JWT in the
  cookie (`services/amSession.xml`).
* **Core Token Service.** `openam-coretoken` stores tokens - sessions, OAuth 2.0 tokens, SAML2
  data, UMA - in LDAP (the configuration store by default, or a dedicated store).
* **OAuth 2.0 tokens.** Stateful (stored in CTS) by default, or stateless JWTs per OAuth2
  provider configuration (`services/OAuth2Provider.xml`).
* **Keys.** Signing and encryption keys come from the keystore in the configuration directory,
  created from `openam-server-only/src/main/webapp/WEB-INF/template/keystore/` at setup; secrets
  for HMAC signing are generated at setup. Operators replace the keystore for production.

## Authorization

* **Policies.** The entitlement engine (`openam-entitlements`) evaluates policies grouped in
  applications, with resource types, subjects, conditions (including scripted conditions) and
  response attributes. REST: `/json/policies`, `/json/applications`, `/json/resourcetypes`;
  agents use the policy service, by default against the application (policy set)
  `iPlanetAMWebAgentService`. A deny from any matching policy wins over allows; a decision can
  carry advice, e.g. to authenticate at a higher level.
* **Policy agents.** Web and Java agents enforce decisions in front of protected applications;
  they are identities (agent profiles) and call the session and policy services.
* **Delegated administration.** Privileges such as RealmAdmin, PolicyAdmin, AgentAdmin,
  FederationAdmin, EntitlementAdmin, LogAdmin and COTAdmin are granted per realm
  (`services/amDelegation.xml`); `amadmin` is the super user.

## OAuth 2.0, OpenID Connect and UMA

`openam-oauth2` implements the provider per realm: authorization code (with PKCE), implicit,
resource owner password, client credentials, device flow, JWT bearer (with SAML2 bearer in
`openam-oauth2-saml2`), refresh tokens, token introspection and
revocation, OpenID Connect (ID tokens, userinfo, discovery, dynamic client registration, session
management). Clients are agent identities. Plugins for scopes, claims (also as scripts) and
response types are configurable per provider. `openam-uma` adds UMA on top.

## Federation

`openam-federation` implements SAML 2.0 (IdP and SP; redirect, POST, artifact, SOAP and ECP
bindings; single logout; name ID management; attribute and authentication queries), WS-Federation,
SAML 1.x and ID-FF / Liberty. Entities, their metadata and circles of trust are configured per
realm. XML parsing goes through the safe parser factories in `com.sun.identity.shared.xml.XMLUtils`.

| Sub-module | Content |
|---|---|
| `openam-federation-library` | the protocol implementations: `com.sun.identity.saml2`, `saml`, `liberty`, `federation` (ID-FF), `wsfederation`, `xacml`, `multiprotocol` (single logout across protocols), `cot` (circles of trust), `xmlenc`, `fedlet` |
| `OpenFM` | integration into the server (`Wren:FM`): the default **mappers** that decide which local account an incoming assertion maps to and which attributes are sent (`saml2.plugins.DefaultSPAccountMapper`, `DefaultIDPAttributeMapper`, and their SAML 1.x and WS-Federation counterparts), SMS configuration plugins, console workflow tasks (create hosted and remote entities, import metadata), SAE authentication, Liberty web service glue, XACML mappers |
| `openam-idpdiscovery`, `openam-idpdiscovery-war` | the SAML IdP discovery service |
| `openam-fedlet-unconfigured-war` | the fedlet, a lightweight SAML SP library for applications |

Mappers and adapters are pluggable per entity; custom implementations configured by an
administrator run in the server.

`openam-federation-library` and `openam-shared` are also published as libraries and used outside
the server - by the fedlet and by Wren:IG (`openig-saml`). Changes to them affect those consumers
too.

## Scripting

`openam-scripting` runs scripts written in JavaScript (Rhino) or Groovy in these contexts:
server-side authentication (Scripted module), client-side authentication (run in the browser),
policy conditions and OIDC claims. Scripts are stored per realm and managed through
`/json/scripts` and the console. Rhino scripts run in a sandbox with a class whitelist and
blacklist (`scripting.xml`); Groovy scripts are not sandboxed
(`org.forgerock.openam.scripting.sandbox`), so the privilege to manage scripts amounts to running
code on the server.

## Extension points

Custom Java code configured by an administrator runs inside the server. The main interfaces:

| Interface | Purpose |
|---|---|
| `com.sun.identity.authentication.spi.AMLoginModule` | authentication module |
| `com.sun.identity.authentication.spi.AMPostAuthProcessInterface` | post-authentication plugin |
| `com.sun.identity.idm.IdRepo` | identity data store |
| `com.sun.identity.entitlement.EntitlementCondition` | policy condition |
| `org.forgerock.oauth2.core.ScopeValidator` | OAuth 2.0 scope handling and claims |
| `com.iplanet.dpro.session.service.QuotaExhaustionAction` | action when a session quota is reached |
| SAML2 and WS-Fed adapters and mappers (`com.sun.identity.saml2.plugins`, `...wsfederation.plugins`) | federation customisation |
| audit event handlers | audit sinks |

Scripts (see Scripting) cover the same ground for authentication, policy conditions and OIDC
claims without deploying Java code.

## Audit and logging

`openam-audit` produces audit events (access, activity, authentication, configuration) through
configurable handlers - CSV, JSON, JDBC, Syslog, JMS, Elasticsearch, Splunk. Debug logging is
separate (`com.sun.identity.shared.debug.Debug`, `Debug.jsp`).

## Build and tests

See [`AGENTS.md`](AGENTS.md) for commands and conventions.
