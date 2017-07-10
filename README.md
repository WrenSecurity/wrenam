# wren:AM

[![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/WrenSecurity/Lobby)

wren:am is a CDDL-licensed, community fork of ForgeRock's OpenAM product. Though our project originated with code that ForgeRock previously released, we are not affiliated with ForgeRock in any way. 

ForgeRock no longer releases any of the most recent versions of their software under an open-source license. ForgeRock’s “Community Edition” versions are much older versions then their commercial offerings. Join our community for the latest and greatest. 

See [wrensecurity.org](http://wrensecurity.org/) for more information.

## Preparing your build environment

In order to build wren:AM from source you need the following software installed:

* Java (OpenJDK) >= 1.7
* Apache Maven >= 3.1.0

## Building wren:AM 

wren:am Is fully build-able however currently not all required artifacts (dependencies) are deployed to public artifact repositories. Therefor there are 2 repositories which you need to clone to install all required artifacts in to your local `~/.m2/repository` location. Our current progress on getting all artifacts in public repositories with verifiable sources can be tracked in this [Google spreadsheet](https://docs.google.com/spreadsheets/d/1HUEprS3Mdm7vxtkPhGu8UYVh0U5V9j1p-dj0bXYhGOs/edit?usp=sharing).

```
$ git clone https://github.com/WrenSecurity/wrensec-deps.git
$ pushd wrensec-deps.git
$ ./install_wrenam_13-5_deps.sh
$ popd
```

```
$ git clone https://github.com/WrenSecurity/forgerock-xui-deps.git
$ pushd forgerock-xui-deps
$ git checkout sustaining/13.5       # This step is important!
$ ./install.sh
$ popd
```

After installing all required artifacts clone the `wrenam` repository an checkout the `sustaining/13.5` branch:

```
$ git clone https://github.com/WrenSecurity/wrenam.git
$ cd wrenam
$ git checkout -b sustaining/13.5 origin/sustaining/13.5
$ mvn clean install
```

## Contributing

Contributing to wren:AM is easy! Just create a GitHub pull request!
 
We do however ask that you run the `precommit` Maven profile which checks your codestyle among things.

```
$ mvn clean install -P precommit
```

Some legacy code will fail, so if you are modifying an existing module you should run this profile before modifying the code, and then run the profile again after modifications to ensure the number of reported issues has not increased. 

## Authors

See the list of [contributors][contributors] who participated in this project.

## License

This project is licensed under the Common Development and Distribution License (CDDL). The following text applies to both this file, and should also be included in all files in the project:

> The contents of this file are subject to the terms of the Common Development and  Distribution License (the License). 
> You may not use this file except in compliance with the License.  
>   
> You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the specific language governing 
> permission and limitations under the License.  
>  
> When distributing Covered Software, include this CDDL Header Notice in each file and include the License file at 
> legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header, with the fields enclosed by brackets [] 
> replaced by your own identifying information: "Portions copyright [year] [name of copyright owner]".  
>   
> Copyright 2016 ForgeRock AS.   

## Acknowledgments

The WrenSecurity community acknowledges the contributions made to the original OpenSSO and OpenAM products on which Wren:AM is based by the following organisations: 

* Sun Microsystems
* Oracle
* ForgeRock

Although the WrenSecurity community acknowledges their contributions the above organisations are in no way affiliated with the WrenSecurity community, Wren:AM or any other WrenSecurity product.