# Release notes v.2.6


## Bugfix: Incorrect property name in the response to the AIS consent creation request
From now on, when POST request `create ais consent` is received then response has correct name of message field `psuMessage` instead of `message`.
