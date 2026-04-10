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
