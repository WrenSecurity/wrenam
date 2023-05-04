<p align="center">
  <img alt="Wren:AM logo" src="https://user-images.githubusercontent.com/13997406/204992591-3148357d-5a58-47fa-9be8-0da2460976a6.png" width="50%">
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

# How to use this image

You can run Wren:AM through this command:

    docker run --rm --name wrenam-test -p 8080:8080 wrensecurity/wrenam:latest

Then you can hit http://localhost:8080/auth in your browser.

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
