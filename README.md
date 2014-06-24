#Domain Test
===========

##Overview
Domain Test is a testing tool designed to help developers test their applications for compatibility with new top-level domains (TLDs). Developed by Google and launched in a partnership between Google Registry, Donuts Inc, Uniregistry, and Ausregistry, Domain Test is an open source project available under the Apache 2 license and can be used across 126 new TLDs. It is freely available for use and modification.

In 2011, the Internet Committee for Assigned Names and Numbers (ICANN) approved a new gTLD program, where applicants could apply to own and operate new gTLDs. A total of 1,930 applications were filed, and beginning in 2013, ICANN began delegating new gTLDs to the root zone. These gTLDs have a series of characteristics, such as string length and the use of non-Latin scripts, that can cause bugs in software. Domain Test helps developers to identify and fix these problems.

This repository contains the documentation and code for Domain Test. For clarity, the documentation uses the term “new TLDs” to refer to the universe of new generic top-level domains (gTLDs), new country-code top-level domains (ccTLDs), and internationalized domain names (IDNs). 

The Domain Test service runs on AppEngine and is available for any developer to use. The syntax examples in this documentation use the `domaintest.みんな` domain name. However, depending on what type of new TLD you want to test, you can substitute any of the strings in the **Domain Test TLDs** section of this documentation.

##HTTP Testing API
You can use the HTTP Testing API to construct an HTTP GET or POST request that results in a predictable server response. By observing the server’s response, you can determine whether the application making the HTTP call properly handled the domain name. 

Requests should be formatted in the following syntax.
`http://domaintest.みんな/<command>?<parameter1>=<value1>&<paremeter2>=<value2>&...`
