# wren:AM

[![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/WrenSecurity/Lobby)

wren:AM is a CDDL-licensed, community fork of ForgeRock's OpenAM product (which in turn was a fork of Sun's / Oracle's OpenSSO). Though our project originated with code that ForgeRock, Oracle and Sun previously released, we are not affiliated with them in any way. 

ForgeRock no longer releases any of the most recent versions of their software under an open-source license. ForgeRock’s “Community Edition” versions are much older versions then their commercial offerings. Join our community for the latest and greatest. 

See [wrensecurity.org](http://wrensecurity.org/) for more information or join us on [Gitter](https://gitter.im/WrenSecurity/Lobby) and have a chat with us!

## sustaining/13.5 branch

This branch, the `sustaining/13.5` branch, is a stable branch intended primarily for bug and security fixes.

## Building wren:AM 

In order to build wren:AM from source you need the following software installed:

* Java (OpenJDK) >= 1.7
* Apache Maven >= 3.1.0

Because the fork is still young and we are still working on rebranding all the group ID's are still set to `org.forgerock`. It is advised to clear out your Maven artifact cache (`~/.m2/repository`) before building. Failing to do so will likely cause problems with non-wren artifacts being used which in turn will cause build failures.

wren:AM can be build with the following command:

```
$ mvn -Dignore-artifact-sigs clean install
```

We are working towards having all dependencies in wren:AM signed to make the use of decencies verifiable. However while this effort is still underway it is currently not complete yet. Therefor the signature check must be disabled by supplying the `-Dignore-artifact-sigs` flag to Maven.

## Contributing

Contributing to wren:AM is easy! Just create a GitHub pull request!
 
We do however ask that you run the `precommit` Maven profile which checks your code style among things.

```
$ mvn clean install -P precommit
```

Some legacy code will fail, so if you are modifying an existing module you should run this profile before modifying the code, and then run the profile again after modifications to ensure the number of reported issues has not increased. 

## Authors

See the list of [contributors][contributors] who participated in this project.

## License

This project is licensed under the Common Development and Distribution License (CDDL). The following text applies to both this file, and should also be included in all files in the project:

>   The contents of this file are subject to the terms of the Common Development
>   and Distribution License (the License). You may not use this file except in
>   compliance with the License.
>   You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License
>   for the specific language governing permission and limitations under the
>   License.
>
>   When distributing Covered Software, include this CDDL Header Notice in each
>   file and include the License file at legal/CDDLv1.0.txt. If applicable, add
>   the following below the CDDL Header, with the fields enclosed by brackets []
>   replaced by your own identifying information: "Portions copyright [year]
>   [name of copyright owner]".
>
>   Copyright 2017 Wren Security.

## Acknowledgments

The WrenSecurity community acknowledges the contributions made to the original OpenSSO and OpenAM products on which Wren:AM is based by the following organisations: 

* Sun Microsystems
* Oracle
* ForgeRock

Although the WrenSecurity community acknowledges their contributions the above organisations are in no way affiliated with the WrenSecurity community, Wren:AM or any other WrenSecurity product.
