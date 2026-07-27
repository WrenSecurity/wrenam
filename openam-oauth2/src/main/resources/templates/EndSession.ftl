<!DOCTYPE html>
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

    Copyright 2026 Wren Security
-->
<html lang="en">
    <head>
        <meta charset="utf-8">
        <meta http-equiv="X-UA-Compatible" content="IE=edge">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="description" content="OIDC End Session">
        <title>OIDC Logout</title>
    </head>

    <body style="display:none">
        <div id="wrapper">Loading...</div>
        <footer id="footer" class="footer"></footer>
        <script type="text/javascript">
            pageData = {
                stage: "${stage?js_string}",
                baseUrl : "${baseUrl?js_string}/XUI",
                <#if realm??>realm: "${realm?js_string}",</#if>
                <#if locale??>locale: "${locale?js_string}",</#if>
                <#if clientName??>clientName: "${clientName?js_string}",</#if>
                <#if userName??>userName: "${userName?js_string}",</#if>
                <#if idTokenHint??>idTokenHint: "${idTokenHint?js_string}",</#if>
                <#if postLogoutRedirectUri??>postLogoutRedirectUri: "${postLogoutRedirectUri?js_string}",</#if>
                <#if state??>state: "${state?js_string}",</#if>
                <#if csrf??>csrf: "${csrf?js_string}",</#if>
                <#if error??>
                error: {
                    <#if error_description??>description: "${error_description?js_string}",</#if>
                    message: "${error?js_string}"
                },
                </#if>
            };
        </script>
        <script data-main="${baseUrl?html}/XUI/main-end-session" src="${baseUrl?html}/XUI/libs/requirejs.js"></script>
    </body>
</html>
