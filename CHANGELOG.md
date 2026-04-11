## [1.2.3](https://github.com/pnam-it309/AeroStride/compare/v1.2.2...v1.2.3) (2026-04-11)


### Bug Fixes

* **infra:** add fallback default ports to prevent invalid proto errors in CI ([ba68ebc](https://github.com/pnam-it309/AeroStride/commit/ba68ebcb6ffdeab1d8feff1d5f0f1cc6cace5e44))

## [1.2.2](https://github.com/pnam-it309/AeroStride/compare/v1.2.1...v1.2.2) (2026-04-11)


### Bug Fixes

* **ci:** explicitly pass --env-file docker/.env to docker compose commands ([57e9beb](https://github.com/pnam-it309/AeroStride/commit/57e9bebbbb8497067b45debb15381f1ec486bdc2))

## [1.2.1](https://github.com/pnam-it309/AeroStride/compare/v1.2.0...v1.2.1) (2026-04-11)


### Bug Fixes

* **nginx:** update api prefix location to /api/v1/ and ensure trailing slash for proxy_pass ([964304f](https://github.com/pnam-it309/AeroStride/commit/964304f5540c0968b0369d6b9741717d29915965))

# [1.2.0](https://github.com/pnam-it309/AeroStride/compare/v1.1.0...v1.2.0) (2026-04-11)


### Features

* **infra:** cleanup and simplify docker-compose production config ([0d15eaf](https://github.com/pnam-it309/AeroStride/commit/0d15eafe08d920dcf0af7f951288b8c23f9602ce))

# [1.1.0](https://github.com/pnam-it309/AeroStride/compare/v1.0.2...v1.1.0) (2026-04-11)


### Features

* **infra:** move database to external AWS RDS and remove local mysql from docker-compose ([18b567a](https://github.com/pnam-it309/AeroStride/commit/18b567a48f377931cfbacf286924319526995d9b))

## [1.0.2](https://github.com/pnam-it309/AeroStride/compare/v1.0.1...v1.0.2) (2026-04-11)


### Bug Fixes

* **ci:** use 'docker compose' instead of 'docker-compose' in CD workflow ([5a30f2f](https://github.com/pnam-it309/AeroStride/commit/5a30f2f1df39a385f0fdc2f68792cfd86054c733))

## [1.0.1](https://github.com/pnam-it309/AeroStride/compare/v1.0.0...v1.0.1) (2026-04-11)


### Bug Fixes

* **be:** update tomcat version to 10.1.53 to fix CVE-2026-29145 ([5ca3c35](https://github.com/pnam-it309/AeroStride/commit/5ca3c35413a9663bebe098b36c8b2adf4b03f5c4))

# 1.0.0 (2026-04-11)


### Bug Fixes

* add missing imports in LoginForm.vue after JS conversion ([9a496bf](https://github.com/pnam-it309/AeroStride/commit/9a496bfb687ffeb2ef5b67a7682a2527e0295c48))
* add missing imports in LoginForm.vue after JS conversion ([e20f5e1](https://github.com/pnam-it309/AeroStride/commit/e20f5e10851995f5467277bc3068e16b3561949a))
* add redis for tests ([3c10920](https://github.com/pnam-it309/AeroStride/commit/3c10920f529baeb514c92c12241c602284a42cd4))
* add redis for tests ([8218df5](https://github.com/pnam-it309/AeroStride/commit/8218df541b010a908c5cebb88ac88478b721b96d))
* **cd:** ensure ghcr login always run to prevent unauthorized push ([51ac8d1](https://github.com/pnam-it309/AeroStride/commit/51ac8d11bd9cf542a828f38d01f288c14b90ca81))
* **cd:** force build and deploy latest image on every push to main ([41932fc](https://github.com/pnam-it309/AeroStride/commit/41932fc971c82c043b3a4458864405a1dbc066f7))
* **cd:** handle missing .env by using DOCKER_ENV secret ([ce3c35a](https://github.com/pnam-it309/AeroStride/commit/ce3c35a9da70b8b7717c89ca82e55cb7befa17db))
* **cd:** switch to docker compose V2 to fix ContainerConfig error ([798a0f8](https://github.com/pnam-it309/AeroStride/commit/798a0f83affad1b5ce696771e5467d33d8eda35a))
* **cd:** update docker-compose and upgrade spring-security to 6.5.0 ([a276358](https://github.com/pnam-it309/AeroStride/commit/a27635897eb162ae32be05e4e98f2f895ce06ec2))
* **cd:** update docker-compose and upgrade spring-security to 6.5.0 ([7259fce](https://github.com/pnam-it309/AeroStride/commit/7259fce1a061d174b68e40dc64af157bac22af54))
* **cd:** use docker-compose instead of docker compose for compatibility ([ad3e226](https://github.com/pnam-it309/AeroStride/commit/ad3e22607006b56b2bab0ad36fab293ae5729ca7))
* **cd:** use type=raw for latest tag ([5bcc91a](https://github.com/pnam-it309/AeroStride/commit/5bcc91a90c3d0d7d1c9c01e2df492b0512c05e36))
* **cd:** use type=raw for latest tag ([ff944d0](https://github.com/pnam-it309/AeroStride/commit/ff944d006bb6894c723a73cb9544c387fc77f31f))
* **ci:** complete switch to docker compose V2 and fix Trivy image refs ([95743ac](https://github.com/pnam-it309/AeroStride/commit/95743ac7e38681aaa9047f3543a9feed0b47f1f5))
* **ci:** use direct docker build in CI and update Trivy image refs ([b68c4c7](https://github.com/pnam-it309/AeroStride/commit/b68c4c7d79f115f9c9dcc837d24493e5b9051c27))
* **docker:** add chmod +x for gradlew ([c3f2fe0](https://github.com/pnam-it309/AeroStride/commit/c3f2fe064c1d6e7a82457d20093a8d0fd4d27815))
* **docker:** add chmod +x for gradlew ([e4329a6](https://github.com/pnam-it309/AeroStride/commit/e4329a60ab64aa94887ace1117f2437a87d3974c))
* **docker:** ensure single app.jar is produced and correctly copied in Dockerfile ([9506573](https://github.com/pnam-it309/AeroStride/commit/9506573361e0dc236846487fc2d795037c399292))
* **docker:** nuclear option for jar naming and copying ([41ed2fe](https://github.com/pnam-it309/AeroStride/commit/41ed2fec35f87e9fff7714a2f53e527868e39d62))
* **docker:** optimize docker-compose for production and port mapping ([2aa08d4](https://github.com/pnam-it309/AeroStride/commit/2aa08d41f6af3b5eb528c1f9ee0245cb61fdf3a0))
* **fe:** increase node heap memory limit for build ([a983bbf](https://github.com/pnam-it309/AeroStride/commit/a983bbfcd3fcbb3cbb8a66f7d260509673d42f44))
* lowercase docker image names and add missing mysql env vars ([f31e63f](https://github.com/pnam-it309/AeroStride/commit/f31e63f1e448b89b891cc9627343a4987a042a3d))
* lowercase docker image names and add missing mysql env vars ([73b3d74](https://github.com/pnam-it309/AeroStride/commit/73b3d74522ea2a51f23c3692b49d96b82bb8bcc8))
* **nginx:** add production domain names and improve proxy headers ([fa6c127](https://github.com/pnam-it309/AeroStride/commit/fa6c127bbb725724fc0c22e7eb13c24376a32e4c))
* refine ci test environment and database connection ([3e2da9a](https://github.com/pnam-it309/AeroStride/commit/3e2da9ad79eab2db645ffadb767a699ef1d2a5a7))
* refine ci test environment and database connection ([3d9d898](https://github.com/pnam-it309/AeroStride/commit/3d9d89894f809e6ba0fd024134562ab74d6bf666))
* **security:** resolve 405 login error by fixing CORS OPTIONS order and FE redirect logic ([0177e91](https://github.com/pnam-it309/AeroStride/commit/0177e9123bb762b532363bbec21a1f97f51cdd28))
* **security:** upgrade spring boot to 3.4.6 to resolve vulnerabilities ([0eb5c8d](https://github.com/pnam-it309/AeroStride/commit/0eb5c8d89706c3c4b0f66f5dc5990160afaa7dff))
* **security:** upgrade spring boot to 3.4.6 to resolve vulnerabilities ([8766f0c](https://github.com/pnam-it309/AeroStride/commit/8766f0cdaf616f81016e42be2786d81e4a176e8e))
* switch to hash routing mode for github pages compatibility ([31dede0](https://github.com/pnam-it309/AeroStride/commit/31dede044eb49c588c039256f659d7c915935a49))
* switch to hash routing mode for github pages compatibility ([636ea31](https://github.com/pnam-it309/AeroStride/commit/636ea31d76755585acc910fa726ed2d93d9e56f6))
* **ui:** eliminate vertical scroll and white space on login page ([f429c40](https://github.com/pnam-it309/AeroStride/commit/f429c40aff336c651cfa6821b3c20f4676db3afe))
* update CORS and Nginx proxy headers to troubleshoot 405 error ([fb0cfe0](https://github.com/pnam-it309/AeroStride/commit/fb0cfe0fac006cfe1320fc1efd7ad4d2dc21819d))
* upgrade spring-security to 6.5.1 and fix gradlew permissions for CI volume mount ([ae2e399](https://github.com/pnam-it309/AeroStride/commit/ae2e3999604f9f49c785b75a3e9ab597bfd70910))
* upgrade spring-security to 6.5.1 and fix gradlew permissions for CI volume mount ([a4c6455](https://github.com/pnam-it309/AeroStride/commit/a4c6455e2cdcfcda41b02e9c419476a626d344bd))


### Features

* add auto deploy to github pages and fix 404 paths ([f0f26f4](https://github.com/pnam-it309/AeroStride/commit/f0f26f4c55f11838eaffce6541080895a350bfd9))
* add auto deploy to github pages and fix 404 paths ([ee951c0](https://github.com/pnam-it309/AeroStride/commit/ee951c0eb303ef2b4daeeceab228fc584f7a413c))
* hoan thien quan ly hoa don_Huyen ([3bbe9d1](https://github.com/pnam-it309/AeroStride/commit/3bbe9d1628a178b5ad57688ece3ab076602de199))
* hoan thien quan ly hoa don_Huyen ([acfb28e](https://github.com/pnam-it309/AeroStride/commit/acfb28e7e3de0fba3235cfe1f1a8862d10c4b30e))
* hoan thien quan ly hoa don_Huyen ([a48e82e](https://github.com/pnam-it309/AeroStride/commit/a48e82e671955d3781eb24ad4cde6bdae7c98ea5))
* hoan thien quan ly hoa don_Huyen ([3342773](https://github.com/pnam-it309/AeroStride/commit/334277377b2cb5ea186c09f152fd184828ca5c8d))
* implement professional POS system and standardize order management UI ([df3baf0](https://github.com/pnam-it309/AeroStride/commit/df3baf09d92cd7fa5afb2683107e17f8f388e0af))
* standardize admin module API communication and restore management screens. \- Added hoaDonService.js. \- Updated controllers for vouchers and orders to use ApiResponse. \- Standardized all services to use central api.js. \- Updated frontend components for consistency. ([182d4cd](https://github.com/pnam-it309/AeroStride/commit/182d4cd8e1b8ffd7e8b7b39dad7bc6b308f7c3fa))
* standardize admin module API communication and restore management screens. \- Added hoaDonService.js. \- Updated controllers for vouchers and orders to use ApiResponse. \- Standardized all services to use central api.js. \- Updated frontend components for consistency. ([dedddcb](https://github.com/pnam-it309/AeroStride/commit/dedddcb15aa533879399a7190bbc04510915e8f5))

## [1.2.14](https://github.com/pnam-it309/AeroStride/compare/v1.2.13...v1.2.14) (2026-04-10)


### Bug Fixes

* **cd:** use docker-compose instead of docker compose for compatibility ([ad3e226](https://github.com/pnam-it309/AeroStride/commit/ad3e22607006b56b2bab0ad36fab293ae5729ca7))

## [1.2.13](https://github.com/pnam-it309/AeroStride/compare/v1.2.12...v1.2.13) (2026-04-10)


### Bug Fixes

* **ui:** eliminate vertical scroll and white space on login page ([f429c40](https://github.com/pnam-it309/AeroStride/commit/f429c40aff336c651cfa6821b3c20f4676db3afe))

## [1.2.12](https://github.com/pnam-it309/AeroStride/compare/v1.2.11...v1.2.12) (2026-04-10)


### Bug Fixes

* **cd:** ensure ghcr login always run to prevent unauthorized push ([51ac8d1](https://github.com/pnam-it309/AeroStride/commit/51ac8d11bd9cf542a828f38d01f288c14b90ca81))

## [1.2.11](https://github.com/pnam-it309/AeroStride/compare/v1.2.10...v1.2.11) (2026-04-10)


### Bug Fixes

* **docker:** optimize docker-compose for production and port mapping ([2aa08d4](https://github.com/pnam-it309/AeroStride/commit/2aa08d41f6af3b5eb528c1f9ee0245cb61fdf3a0))

## [1.2.10](https://github.com/pnam-it309/AeroStride/compare/v1.2.9...v1.2.10) (2026-04-10)


### Bug Fixes

* **docker:** nuclear option for jar naming and copying ([41ed2fe](https://github.com/pnam-it309/AeroStride/commit/41ed2fec35f87e9fff7714a2f53e527868e39d62))

## [1.2.9](https://github.com/pnam-it309/AeroStride/compare/v1.2.8...v1.2.9) (2026-04-10)


### Bug Fixes

* **cd:** force build and deploy latest image on every push to main ([41932fc](https://github.com/pnam-it309/AeroStride/commit/41932fc971c82c043b3a4458864405a1dbc066f7))

## [1.2.8](https://github.com/pnam-it309/AeroStride/compare/v1.2.7...v1.2.8) (2026-04-10)


### Bug Fixes

* **nginx:** add production domain names and improve proxy headers ([fa6c127](https://github.com/pnam-it309/AeroStride/commit/fa6c127bbb725724fc0c22e7eb13c24376a32e4c))

## [1.2.7](https://github.com/pnam-it309/AeroStride/compare/v1.2.6...v1.2.7) (2026-04-10)


### Bug Fixes

* **docker:** ensure single app.jar is produced and correctly copied in Dockerfile ([9506573](https://github.com/pnam-it309/AeroStride/commit/9506573361e0dc236846487fc2d795037c399292))

## [1.2.6](https://github.com/pnam-it309/AeroStride/compare/v1.2.5...v1.2.6) (2026-04-10)


### Bug Fixes

* **security:** resolve 405 login error by fixing CORS OPTIONS order and FE redirect logic ([0177e91](https://github.com/pnam-it309/AeroStride/commit/0177e9123bb762b532363bbec21a1f97f51cdd28))

## [1.2.5](https://github.com/pnam-it309/AeroStride/compare/v1.2.4...v1.2.5) (2026-04-09)


### Bug Fixes

* update CORS and Nginx proxy headers to troubleshoot 405 error ([fb0cfe0](https://github.com/pnam-it309/AeroStride/commit/fb0cfe0fac006cfe1320fc1efd7ad4d2dc21819d))

## [1.2.4](https://github.com/pnam-it309/AeroStride/compare/v1.2.3...v1.2.4) (2026-04-09)


### Bug Fixes

* **ci:** use direct docker build in CI and update Trivy image refs ([b68c4c7](https://github.com/pnam-it309/AeroStride/commit/b68c4c7d79f115f9c9dcc837d24493e5b9051c27))

## [1.2.3](https://github.com/pnam-it309/AeroStride/compare/v1.2.2...v1.2.3) (2026-04-09)


### Bug Fixes

* **ci:** complete switch to docker compose V2 and fix Trivy image refs ([95743ac](https://github.com/pnam-it309/AeroStride/commit/95743ac7e38681aaa9047f3543a9feed0b47f1f5))

## [1.2.2](https://github.com/pnam-it309/AeroStride/compare/v1.2.1...v1.2.2) (2026-04-09)


### Bug Fixes

* **cd:** switch to docker compose V2 to fix ContainerConfig error ([798a0f8](https://github.com/pnam-it309/AeroStride/commit/798a0f83affad1b5ce696771e5467d33d8eda35a))

## [1.2.1](https://github.com/pnam-it309/AeroStride/compare/v1.2.0...v1.2.1) (2026-04-09)


### Bug Fixes

* **cd:** handle missing .env by using DOCKER_ENV secret ([ce3c35a](https://github.com/pnam-it309/AeroStride/commit/ce3c35a9da70b8b7717c89ca82e55cb7befa17db))

# [1.2.0](https://github.com/pnam-it309/AeroStride/compare/v1.1.0...v1.2.0) (2026-04-09)


### Features

* implement professional POS system and standardize order management UI ([df3baf0](https://github.com/pnam-it309/AeroStride/commit/df3baf09d92cd7fa5afb2683107e17f8f388e0af))
