# Changelog

## [0.3.0](https://github.com/Solierrr/api-notify/compare/v0.2.0...v0.3.0) (2026-10-31)


### Features

* accept web devices for push notifications ([#13](https://github.com/Solierrr/api-notify/issues/13)) ([f3214dc](https://github.com/Solierrr/api-notify/commit/f3214dcadaad8c6f2b122c71ccdc43632fd526aa))
* add device registration endpoints ([4892183](https://github.com/Solierrr/api-notify/commit/4892183cb1fbe72be09269f4471f080a18b9a6cd))
* add fcm configuration ([904946e](https://github.com/Solierrr/api-notify/commit/904946eb85570f1a42e52caa5bbd43b79097709f))
* add internal notification dispatch ([0aeff6f](https://github.com/Solierrr/api-notify/commit/0aeff6f6f5d44baeb68c15a8059f6ff9d9087113))
* add notification history endpoints ([03c1500](https://github.com/Solierrr/api-notify/commit/03c1500ece8fd07a679804c1c1816add1ee17e1c))
* add notification service core ([f61329e](https://github.com/Solierrr/api-notify/commit/f61329e851294289c8fae8a57eedf2e98db84ed7))
* add user jwt and service key security ([aa24d1e](https://github.com/Solierrr/api-notify/commit/aa24d1ebaeedb7debb70fec773c3c93eca9926d4))
* limit internal notification payload size ([86057b1](https://github.com/Solierrr/api-notify/commit/86057b111740db6e8bb11867bb773751f1435041))
* send push notifications through fcm ([7611996](https://github.com/Solierrr/api-notify/commit/76119965ac755d3db54552f367419092b7782d51))
* send push notifications through fcm ([a11eae8](https://github.com/Solierrr/api-notify/commit/a11eae8b2cf83e154770c98d3839789088a1219f))
* standardize the service contract ([#6](https://github.com/Solierrr/api-notify/issues/6)) ([de611dc](https://github.com/Solierrr/api-notify/commit/de611dc7c09115d513fc8a54c8d5c6a81fb91102))


### Bug Fixes

* fcm http timeouts and stricter service account parsing ([eb8fb0d](https://github.com/Solierrr/api-notify/commit/eb8fb0d5a23d20acc4fd69fe925d41b1bdc6bff2))
* handle null fcm error codes and partial batch failures ([5185863](https://github.com/Solierrr/api-notify/commit/5185863caab5b9243c7f1bef2ae7f69d01ba2a30))
* log per-token fcm failure codes and guard sender id mismatch ([de978ec](https://github.com/Solierrr/api-notify/commit/de978ec0a6578e2d6f9958815a034388642c3661))
* name the api-notify service notify in the telemetry ([#15](https://github.com/Solierrr/api-notify/issues/15)) ([0a78112](https://github.com/Solierrr/api-notify/commit/0a781123b1f51219097eb7977f8c8f62ecad132a))
* ping only the service database in the mongo health check ([#9](https://github.com/Solierrr/api-notify/issues/9)) ([bc5d416](https://github.com/Solierrr/api-notify/commit/bc5d416b675ffc6badf6ac094593e05c8b384478))
* remove invalid-token devices only when the token still matches ([c2ec57e](https://github.com/Solierrr/api-notify/commit/c2ec57e558ac3924f3a4b71478fee80aac73d99c))
* report class-level validation errors under field payload ([28c5d86](https://github.com/Solierrr/api-notify/commit/28c5d8616ce5ba910e16efa3bfcf9353a3a2220b))
* require service keys of at least 32 characters ([ce1db2c](https://github.com/Solierrr/api-notify/commit/ce1db2c29633f0c114db10a8b1a6256c7d0860c0))
* return problem detail on parameter type mismatch ([c76dca4](https://github.com/Solierrr/api-notify/commit/c76dca4398426e20c30c6ead9d19df489bfae1a8))
* validate notification data keys and values ([31aa17c](https://github.com/Solierrr/api-notify/commit/31aa17c71c6ea73e8bbcbc80190ab91aa675eeee))

## [0.2.0](https://github.com/Solierrr/generic-template/compare/v0.1.1...v0.2.0) (2026-10-02)


### Features

* add practical bruno api examples ([5382018](https://github.com/Solierrr/generic-template/commit/5382018aa456fa7a07293aaa7b6228478ba187cb))
* add runtime and local api templates ([7bfb0d2](https://github.com/Solierrr/generic-template/commit/7bfb0d2fc19840a41b902e8910e4cf224337350b))

## [0.1.1](https://github.com/Solierrr/generic-template/compare/v0.1.0...v0.1.1) (2026-09-30)


### Bug Fixes

* grant pull-requests write permission to release workflow ([2978b14](https://github.com/Solierrr/generic-template/commit/2978b14aeecc63d0283afffeb9e62bc478b02afe))
* trigger releases by command ([86004f2](https://github.com/Solierrr/generic-template/commit/86004f2b2d859bc09993cf98ec3892d497d23e5a))
* use checkbox trigger for qa sync workflow ([f4448db](https://github.com/Solierrr/generic-template/commit/f4448dbef0bd3018db1b188c677f2530e25a38cd))
