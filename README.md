#Domain Test
===========

##Overview
Domain Test is a testing tool designed to help developers test their applications for compatibility with new top-level domains (TLDs). Developed by Google and launched in a partnership between Google Registry, Donuts Inc, Uniregistry, and Ausregistry, Domain Test is an open source project available under the Apache 2 license and can be used across 126 new TLDs. It is freely available for use and modification.

In 2011, the Internet Committee for Assigned Names and Numbers (ICANN) approved a new gTLD program, where applicants could apply to own and operate new gTLDs. A total of 1,930 applications were filed, and beginning in 2013, ICANN began delegating new gTLDs to the root zone. These gTLDs have a series of characteristics, such as string length and the use of non-Latin scripts, that can cause bugs in software. Domain Test helps developers to identify and fix these problems.

This repository contains the documentation and code for Domain Test. For clarity, the documentation uses the term “new TLDs” to refer to the universe of new generic top-level domains (gTLDs), new country-code top-level domains (ccTLDs), and internationalized domain names (IDNs). 

The Domain Test service runs on AppEngine and is available for any developer to use. The syntax examples in this documentation use the `domaintest.みんな` domain name. However, depending on what type of new TLD you want to test, you can substitute any of the strings in the **Domain Test TLDs** section of this documentation.

##HTTP Testing API
You can use the HTTP Testing API to construct an HTTP GET or POST request that results in a predictable server response. By observing the server’s response, you can determine whether the application making the HTTP call properly handled the domain name. 

Requests should use the following syntax.

`http://domaintest.みんな/<command>?<parameter1>=<value1>&<paremeter2>=<value2>&...`

###ECHO
The `echo` command instructs the Domain Test service to echo a response based on the parameters you specify. You can construct an ECHO command with one or more of the parameters below.

  - `status=<integer>` determines the status code (default 200)
  - `payload=<urlencoded text>` sets the body text or redirect url (default “”, max 10k)
  - `mime=<type>` determines the MIME type (default text/plain)
  - `sleep=<seconds>` causes a sleep before the response (default 0 sec, max 10 sec)
  - `header=<name=value>` adds a header to the response
  - `setcookie=<name=value>` sets a session-scoped cookie
  - `delcookie=<name>` deletes a cookie

For example, the request below will return the string `echoed-narwhal`.

`http://domaintest.みんな/echo?payload=echoed-narwhal`

The request below will return a 302 redirect to `http://www.example.com/`.

`http://domaintest.みんな/echo?status=302&payload=http://www.example.com/`

The request below will return a 302 redirect to http://www.example.com/ after sleeping for 10 seconds.

`http://domaintest.みんな/echo?status=302&sleep=10&payload=http://www.example.com/`

###STASH
The `stash` command instructs the Domain Test service to stash a response to the parameters specified in the request for later retrieval. It uses the same parameters as the `echo` command.

For example, the request below will stash the string 'stashed-narwhal'.

`http://domaintest.みんな/stash?payload=stashed-narwhal`

The Domain Test service responds to stash requests with a URL in the form below, which can be used later to retrieve the stashed response.

`http://domaintest.みんな/temp/<token>`

A single token URL is available for use for 5 minutes after it's been generated, and it can be used once.

Alternatively, you can use the URL below if you want to pre-generate a token *before* stashing:

`http://domaintest.みんな/token`

If you’ve pre-generated a token prior to stashing a request, you can assign a stash command to your pre-generated token using the `<token>` parameter:

`http://domaintest.みんな/stash?token=<pre-generated_token>`

A single pre-generated token can be used an unlimited number of times within one hour of generation.

##Email Testing API
The Email Testing API allows you to trigger an automatic email response from the Domain Test service, which enables you to determine whether an application’s email stack properly handles new TLDs. You can trigger an autoresponse by sending an email with a subject that begins with `Test` to `<local-part>@domaintest.みんな`, where `<local-part>` is any string:

```
To: narwhal@domaintest.みんな
Subject: Test ALL the autoresponders! 
```

The autoresponder will reply with an email from `tester@domaintest.みんな` with the subject, `Automated testing service response`. (Although you can send the outbound email to any of the domains listed in the Domain Test TLDs section below, the autoresponse will always be sent from `tester@domaintest.みんな`.) The autoresponder respects a Reply-To header, if present.

The email testing API is compliant with IDNA2008, but it does not support full email address internationalization as defined in RFCs 6530, 6531, and 6532. 

##Domain Test TLDs
The Domain Test tool is available on the following TLDs, thanks to a partnership between Google Registry, Donuts Inc, Uniregistry, and Ausregistry.

