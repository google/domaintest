#Domain Test

##Overview
Domain Test is a tool designed to help developers test their applications for compatibility with new top-level domains (TLDs). Developed by Google and launched in a partnership between Google Registry, Donuts Inc, Uniregistry, and Ausregistry, Domain Test is an open source project available under the Apache 2 license and can be used across 126 new TLDs. It is freely available for use and modification.

In 2011, the Internet Committee for Assigned Names and Numbers (ICANN) approved a new gTLD program, where applicants could apply to own and operate new gTLDs. A total of 1,930 applications were filed, and beginning in 2013, ICANN began delegating new gTLDs to the root zone. These gTLDs have a series of characteristics, such as string length and the use of non-Latin scripts, that can cause bugs in software. Domain Test helps developers identify and fix these problems.

This repository contains the documentation and code for Domain Test. For clarity, the documentation uses the term “new TLDs” to refer to the universe of new generic top-level domains (gTLDs), new country-code top-level domains (ccTLDs), and internationalized domain names (IDNs). 

The Domain Test service runs on AppEngine and is available for any developer to use. The syntax examples in this documentation use the `domaintest.みんな` domain name. However, depending on what type of new TLD you want to test, you can substitute any of the strings in the **Domain Test TLDs** section of this documentation.

##HTTP Testing API
You can use the HTTP Testing API to construct an HTTP GET or POST request that results in a predictable server response. By observing the server’s response, you can determine whether the application making the HTTP call properly handled the domain name. 

GET requests should use the following syntax:

`http://domaintest.みんな/<command>?<parameter1>=<value1>&<paremeter2>=<value2>&...`

POST requests can mix parameters between the query string, like GET, and the POST body. Both `multipart/form-data` and `application/x-www-form-urlencoded` are supported, and the `postpayload` parameter does not interpret the POST body at all.

The HTTP Testing API supports Cross-Origin Resource Sharing on all requests, including support for preflight. This means that you can test AJAX requests to new TLDs from JavaScript running on any page.

###ECHO
The `echo` command instructs the Domain Test service to echo a response based on the parameters you specify. You can construct an ECHO command with one or more of the parameters below.

  - `status=<integer>` determines the status code (default 200).
  - `payload=<urlencoded text>` sets the body text or redirect url (default "").
  - `postpayload` is an alternative to `payload` that interprets the entire POST body as the payload.
  - `mime=<type>` determines the MIME type (default text/plain).
  - `sleep=<seconds>` causes a sleep before the response (default 0 sec, max 10 sec).
  - `header=<name=value>` adds a header to the response.
  - `setcookie=<name=value>` sets a session-scoped cookie.
  - `delcookie=<name>` deletes a cookie.

For example, the request below will return the string `echoed-narwhal`.

`http://domaintest.みんな/echo?payload=echoed-narwhal`

The request below will return a 302 redirect to `http://www.example.com/`.

`http://domaintest.みんな/echo?status=302&payload=http://www.example.com/`

The request below will return a 302 redirect to `http://www.example.com/` after sleeping for 10 seconds.

`http://domaintest.みんな/echo?status=302&sleep=10&payload=http://www.example.com/`

###STASH
The `stash` command instructs the Domain Test service to stash a response to the parameters specified in the request for later retrieval. It uses the same parameters as the `echo` command. A stashed payload is truncated after 10K.

For example, the request below will stash the string 'stashed-narwhal'.

`http://domaintest.みんな/stash?payload=stashed-narwhal`

The Domain Test service responds to stash requests with a temp URL in the form below, which can be used later to retrieve the stashed response.

`http://domaintest.みんな/temp/<token>`

A single temp URL is available for use for 5 minutes after it's been generated, and it can be used once. Note that stashed data is stored in memory and should be considered highly ephemeral. Under some circumstances it may be lost even before the stated expiration time, in which case you should re-stash and try again.

###TOKEN

Alternatively, you can use the URL below if you want to pre-generate a token *before* stashing:

`http://domaintest.みんな/token`

If you’ve pre-generated a token prior to stashing a request, you can assign a stash command to your pre-generated token using the `token` parameter:

`http://domaintest.みんな/stash?token=<pre-generated_token>`

A single pre-generated token can be used an unlimited number of times within one hour of generation.

##Email Testing API
The Email Testing API allows you to trigger an automatic email response from the Domain Test service, which enables you to determine whether an application’s email stack properly handles new TLDs. You can trigger an autoresponse by sending an email with a subject that begins with the word `Test` to `<local-part>@domaintest.みんな`, where `<local-part>` is any string:

```
To: narwhal@domaintest.みんな
Subject: Test ALL the autoresponders! 
```

The autoresponder will reply with an email from `tester@domaintest.みんな` with the subject `Automated testing service response`. (Although you can send the outbound email to any address on `domaintest.みんな` the autoresponse will always be sent from `tester@domaintest.みんな`.) The autoresponder respects a Reply-To header, if present.

The email testing API is compliant with IDNA2008, but it does not support full email address internationalization as defined in RFCs 6530, 6531, and 6532. 

If the second word of the email subject is a token retrieved from the `/token` endpoint, the headers (but not body) of the incoming email will be stashed at 

`http://domaintest.みんな/temp/<token>`

for 15 minutes and will be retrievable once. You can use this to determine whether an email reached the Domain Test service, even if you do not receive an autoresponse.

##Security Considerations
By design, the Domain Test service is highly insecure. You should consider any data sent to the service to be public, and should not stash or email anything other than test data. It is trivial to execute arbitrary Javascript within the domaintest.みんな origin, both directly via `/echo` and stored via `/stash`, which is why it is crucial that there not be anything private within the same domain that is worth stealing. For this reason, there is no content other than the Domain Test service on the domains listed below.

You should think very carefully before running the service on your own domain, since it opens an XSS vector against any other content on the domain. In addition, due to the possibility of stored XSS attacks that can live beyond the lifetime of a stash (for example, by manipulating the HTML5 Application Cache), running the service on a domain name means that domain name will *always* be vulnerable from a security perspective, and should prevent you from reusing that domain for any non-testing purposes even in the future.

##Discussion
The discussion forum for this project is hosted on Google Groups: [domain-test@googlegroups.com](https://groups.google.com/forum/#!forum/domain-test).

##Domain Test TLDs
The Domain Test tool is available on the following TLDs, thanks to a partnership between Google Registry, Donuts Inc, Uniregistry, and Ausregistry.

<table>
<tbody>
<tr>
<td>اختبارنطاق.شبكة</td>
<td>domaintest.contractors</td>
<td>domaintest.graphics</td>
<td>domaintest.repair</td>
</tr>
<tr>
<td>domaintest.みんな</td>
<td>domaintest.cool</td>
<td>domaintest.gripe</td>
<td>domaintest.report</td>
</tr>
<tr>
<td>domaintest.academy</td>
<td>domaintest.cruises</td>
<td>domaintest.guitars</td>
<td>domaintest.schule</td>
</tr>
<tr>
<td>domaintest.agency</td>
<td>domaintest.dating</td>
<td>domaintest.guru</td>
<td>domaintest.services</td>
</tr>
<tr>
<td>domaintest.associates</td>
<td>domaintest.dental</td>
<td>domaintest.holdings</td>
<td>domaintest.shoes</td>
</tr>
<tr>
<td>domaintest.bargains</td>
<td>domaintest.diamonds</td>
<td>domaintest.holiday</td>
<td>domaintest.singles</td>
</tr>
<tr>
<td>domaintest.bike</td>
<td>domaintest.directory</td>
<td>domaintest.house</td>
<td>domaintest.solar</td>
</tr>
<tr>
<td>domaintest.blackfriday</td>
<td>domaintest.discount</td>
<td>domaintest.institute</td>
<td>domaintest.solutions</td>
</tr>
<tr>
<td>domaintest.boutique</td>
<td>domaintest.domains</td>
<td>domaintest.international</td>
<td>domaintest.support</td>
</tr>
<tr>
<td>domaintest.builders</td>
<td>domaintest.education</td>
<td>domaintest.investments</td>
<td>domaintest.surgery</td>
</tr>
<tr>
<td>domaintest.cab</td>
<td>domaintest.email</td>
<td>domaintest.kitchen</td>
<td>domaintest.systems</td>
</tr>
<tr>
<td>domaintest.camera</td>
<td>domaintest.engineering</td>
<td>domaintest.land</td>
<td>domaintest.tax</td>
</tr>
<tr>
<td>domaintest.camp</td>
<td>domaintest.enterprises</td>
<td>domaintest.lease</td>
<td>domaintest.technology</td>
</tr>
<tr>
<td>domaintest.capital</td>
<td>domaintest.equipment</td>
<td>domaintest.lighting</td>
<td>domaintest.tienda</td>
</tr>
<tr>
<td>domaintest.cards</td>
<td>domaintest.estate</td>
<td>domaintest.limited</td>
<td>domaintest.tips</td>
</tr>
<tr>
<td>domaintest.care</td>
<td>domaintest.events</td>
<td>domaintest.limo</td>
<td>domaintest.today</td>
</tr>
<tr>
<td>domaintest.careers</td>
<td>domaintest.exchange</td>
<td>domaintest.link</td>
<td>domaintest.town</td>
</tr>
<tr>
<td>domaintest.cash</td>
<td>domaintest.expert</td>
<td>domaintest.maison</td>
<td>domaintest.toys</td>
</tr>
<tr>
<td>domaintest.catering</td>
<td>domaintest.exposed</td>
<td>domaintest.management</td>
<td>domaintest.training</td>
</tr>
<tr>
<td>domaintest.center</td>
<td>domaintest.fail</td>
<td>domaintest.marketing</td>
<td>domaintest.university</td>
</tr>
<tr>
<td>domaintest.cheap</td>
<td>domaintest.farm</td>
<td>domaintest.media</td>
<td>domaintest.vacations</td>
</tr>
<tr>
<td>domaintest.christmas</td>
<td>domaintest.financial</td>
<td>domaintest.partners</td>
<td>domaintest.ventures</td>
</tr>
<tr>
<td>domaintest.cleaning</td>
<td>domaintest.fish</td>
<td>domaintest.photography</td>
<td>domaintest.viajes</td>
</tr>
<tr>
<td>domaintest.clinic</td>
<td>domaintest.fitness</td>
<td>domaintest.photos</td>
<td>domaintest.villas</td>
</tr>
<tr>
<td>domaintest.clothing</td>
<td>domaintest.flights</td>
<td>domaintest.pics</td>
<td>domaintest.vision</td>
</tr>
<tr>
<td>domaintest.codes</td>
<td>domaintest.florist</td>
<td>domaintest.pictures</td>
<td>domaintest.voyage</td>
</tr>
<tr>
<td>domaintest.coffee</td>
<td>domaintest.foo</td>
<td>domaintest.plumbing</td>
<td>domaintest.watch</td>
</tr>
<tr>
<td>domaintest.community</td>
<td>domaintest.foundation</td>
<td>domaintest.productions</td>
<td>domaintest.works</td>
</tr>
<tr>
<td>domaintest.company</td>
<td>domaintest.fund</td>
<td>domaintest.properties</td>
<td>domaintest.wtf</td>
</tr>
<tr>
<td>domaintest.computer</td>
<td>domaintest.furniture</td>
<td>domaintest.recipes</td>
<td>domaintest.zone</td>
</tr>
<tr>
<td>domaintest.condos</td>
<td>domaintest.gallery</td>
<td>domaintest.reisen</td>
<td></td>
</tr>
<tr>
<td>domaintest.construction</td>
<td>domaintest.glass</td>
<td>domaintest.rentals</td>
<td></td>
</tr>
</tbody>
</table>

##Legal
Domain Test is an open source project available under the Apache 2 license. Use of the Domain Test service is subject to Google’s Terms of Service and Privacy Policy. [Learn more](https://www.google.com/intl/en/policies/). 
