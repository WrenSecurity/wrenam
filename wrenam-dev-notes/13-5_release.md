# Creating a stable 13.5 release

The last version in the master branch of the original source part of the 13.5.X family is 13.5.0 RC11 (see commit db7f9a3f4f46). Judging from ForgeRock's public issue tracker RC17 was the last RC release before 13.5.0 final was released. Starting from RC11 the rest of the RC's were probably developed on a separate branch from master (which would make sense).  Below is a list of issues that are indentified in ForgeRock's public issue tracker in these 6 RC versions (RC11 through RC17). In order for us to create a stable 13.5.0 version we need to address these issues.

13.5.X Security related:

* [Issue #201608-01: Open Redirect](https://backstage.forgerock.com/knowledge/kb/article/a25759331)
  * OPENAM-9360 Not available in public issue tracker. 
    * `master` branch: `cc56c55b6a9d836dbd7f22e9f1a0ac6e77cc5067`
  * OPENAM-9519 Not available in public issue tracker. 
    * `master` branch: `85b062588cae666bae050ac1e524acb35bf05d7d`

* [Issue #201608-02: Server-side Request Forgery](https://backstage.forgerock.com/knowledge/kb/article/a25759331)
  * OPENAM-9479 Not available in public issue tracker. 
    * `master` branch: unknown.

The following issues were supposedly fixed in 13.5. We need to verify the are actually fixed in our source and weren't fixed in our RC11 - RC17 gap:

* [Issue #201605-01: Credential Forgery](https://backstage.forgerock.com/knowledge/kb/article/a66655124)
  * OPENAM-9389 Not available in public issue tracker. 
    * `master` branch: `c60c5bb51b7a07a62eda0910ae9774bfe49bd003` and `29ae5417b1417103a811052dfc12f28cd23d6f3c`
    * `sustaining/13.5` branch: `c60c5bb51b7a07a62eda0910ae9774bfe49bd003` and `29ae5417b1417103a811052dfc12f28cd23d6f3c`

* [#201605-02: Insufficient Authorization](https://backstage.forgerock.com/knowledge/kb/article/a66655124)
  * OPENAM-9394 Not available in public issue tracker. 
    * `master` branch: `7cfd4613f0d3b69c8ac0a8e9b2ba03528597fee2` and `829e51f34aaae86fc010577e87bc39d7f26877c1`
    * `sustaining/13.5` branch: `7cfd4613f0d3b69c8ac0a8e9b2ba03528597fee2` and `829e51f34aaae86fc010577e87bc39d7f26877c1

* [Issue #201605-03: Authentication Bypass](https://backstage.forgerock.com/knowledge/kb/article/a66655124)
  * OPENAM-7938 Not available in public issue tracker. 
    * `master` branch: `c9ba1a8f3afcf43e26fe5b062ac652b347b283fb`
    * `sustaining/13.5` branch: `c9ba1a8f3afcf43e26fe5b062ac652b347b283fb`

* [Issue #201605-04: Cross-Site Request Forgery (CSRF)](https://backstage.forgerock.com/knowledge/kb/article/a66655124)
  * OPENAM-8575 Not available in public issue tracker.   
    * `master` branch: `4f04731207681973251c52436f07118e7c325e88`
    * `sustaining/13.5` branch: `4f04731207681973251c52436f07118e7c325e88`
  
* [Issue #201605-05: Cross Site Scripting (XSS)](https://backstage.forgerock.com/knowledge/kb/article/a66655124)
  * OPENAM-8951 Not available in public issue tracker. 
    * `master` branch: `440cd3103e8d5b3bb64e8a3d3e54b03ede9f9801`
    * `sustaining/13.5` branch: `440cd3103e8d5b3bb64e8a3d3e54b03ede9f9801`
  * OPENAM-9216 Not available in public issue tracker. 
    * `master` branch: `7bed76a091ac06e51ede059942f9ebc9d9a3166a`
    * `sustaining/13.5` branch: `7bed76a091ac06e51ede059942f9ebc9d9a3166a`

* [Issue #201605-06: Credentials appear in CTS access log](https://backstage.forgerock.com/knowledge/kb/article/a66655124)
  * OPENAM-8329 Not available in public issue tracker. 
    * `master` branch: `f6f832d45ed206ed045a2e3e541ae0f2fd34cc0c`, `e6c98eb6e2eda3044fde6b95e3513cb7bd0e8de3`, `420873093d86dca59deb712d790a4554a07354c4` and `60333acda641a52f453f938cb4744fe4ac56b5ff` (actual fix).
    * `sustaining/13.5` branch: `f6f832d45ed206ed045a2e3e541ae0f2fd34cc0c`, `420873093d86dca59deb712d790a4554a07354c4` and `60333acda641a52f453f938cb4744fe4ac56b5ff`
  
* [Issue #201605-07: Content Spoofing Vulnerability](https://backstage.forgerock.com/knowledge/kb/article/a66655124)
  * OPENAM-8248 Not available in public issue tracker. 
    * `master` branch: `a6f2ce2d0786fa90fac4d6a6cbd83a19d3573593`
    * `sustaining/13.5` branch: `a6f2ce2d0786fa90fac4d6a6cbd83a19d3573593` 
  * OPENAM-8249 Not available in public issue tracker. 
    * `master` branch: `d95fb8f7f3c2c107e1537d77109e357352be8917` and `780c922759d81da1184f793001c0f73691e033f4`
    * `sustaining/13.5` branch: `d95fb8f7f3c2c107e1537d77109e357352be8917` and `780c922759d81da1184f793001c0f73691e033f4`

RC11:

* ~~[OPENAM-9322] When Trying to set User Self Service I get "Data Validation Failed for Attribute SelfServiceSecretKey Attribute" error~~
  * This turns out not to be a bug. In the public issue tracker of OpenAM it is stated that you need to write over the existing text even though it looks like the test key is set by default.
* ~~[OPENAM-9147] After ugrade, there is added Error record in debug log file on realm deletion~~

RC12:

* [OPENAM-9330] Configure > Server defaults: Descriptions of fields contain property names
* [OPENAM-9332] Configure > Server Defaults: No validation when saving improper (negative) value to number fields
* [OPENAM-9339] Configure > Server defaults: Max connections field has wrong type
* [OPENAM-9355] Configure > Server defaults: Can break Advanced server defaults by saving improper value.
* [OPENAM-9354] oauth2/authorize: Performance regression 13.5.0 vs 12.0.3
* [OPENAM-9349] ForgeRock Authenticator (OATH) Module required field "Name of Issuer" should be validated
* [OPENAM-9341] User Service adminDNStartingView validation and save fails with empty value
* [OPENAM-9335] aud in JWT should be an Array not a String in 13.5 RC12
* [OPENAM-9331] /services/audit/CSV?_action=schema returns 500 after upgrade from 13.0.0 to 13.5
* [OPENAM-9328] AM version is not updated in XUI after upgrade from 13.0.0 to 13.5

RC13:

* [OPENAM-9353] OpenAM 12.0.3 cannot be upgraded to 13.5.0-RC13
* [OPENAM-9363] Can not log in when reverse proxy in front of OpenAM
* [OPENAM-9390] For USS forgotten Password when using AD we can not reset password.
* [OPENAM-9425] WS-Federation active profile fails in subrealm
* [OPENAM-9201] Can't login using Push Authentication from the phone itself

RC14:

* [OPENAM-9370] Configuration dialog stuck after successful configuration on weblogic
* [OPENAM-9387] Clicking on the link in the user registration email results in an 500 Internal Server Error
* [OPENAM-9385] STS cannot use jceks keystore type
* [OPENAM-9386] Possible to create an user with invalid character after upgrade
* [OPENAM-9368] OpenAM unable to determine the version of OpenDJ after upgrade to 13.5.0
* [OPENAM-9378] OpenAM 13.5 now defaults to using FQDN as cookie domain
* [OPENAM-9150] device-print-service-description is not deleted from gobal configuration list after upgrade
* [OPENAM-9085] OpenAM 13.5.0 configuration uses excess memory when configuring OpenDJ
* [OPENAM-9303] URI from response to registering resource set does not work
* [OPENAM-9309] XUI All services with configuration stored in the serverinfo endpoint do not update without a page refresh
* [OPENAM-9204] Sending Password Credentials Grant Token Request gives Internal Server Error
* [OPENAM-9207] Consent page not returned - "Request not valid" page shown
* [OPENAM-9367] Missing classes from com.sun.identity.wss.security after upgrading from 12.0.3 to 13.5.0
* [OPENAM-9357] Upgrading to 13.x does not populate Subject Type in OAuth2Client config, causing an NPE
* [OPENAM-9147] After ugrade, there is added Error record in debug log file on realm deletion

RC15:

* [OPENAM-9387] Clicking on the link in the user registration email results in an 500 Internal Server Error
* [OPENAM-9971] id_token_signed_response_alg and token_endpoint_auth_signing_alg are not separated
* [OPENAM-9406] Some NPE in OAuth2 and OpenID
* [OPENAM-9388] Unable to select recovery codes for device without mouse
* [OPENAM-9384] USS: Services configured in subrealm are not using own configuration but they use the config of those services in main realm

RC16:

* [OPENAM-9408] Upgrade on WebLogic fails with StackOverflowError caused by recursion
* [OPENAM-9391] ES6 conversion breaks server calls
* [OPENAM-9387] Clicking on the link in the user registration email results in an 500 Internal Server Error
* [OPENAM-9399] Can retrieve username or reset password giving just one of two required data
* [OPENAM-9398] User self-service should perform sanity-check on configuration

RC17:

* [OPENAM-9419] The link to the outdated guide how to setup Microsoft Authentication is given in Configure Microsoft Authentication
* [OPENAM-9414] Push Notification can use the GCN/APNS endpoints to obtain the aws cluster
* [OPENAM-9418] Cannot configure oauth2 for Microsoft Authentication with given redirect URL
* [OPENAM-9415] GCN and APNS endpoints are ordered differently to Backstage
* [OPENAM-9279] User registration should return authn success addition properties inline with the authn endpoint


