# 0001 Monorepo og Maven

- Status: Akseptert
- Dato: 2026-10-18

## Kontekst

Saksgang skal etter hvert bestå av flere deler: et REST-API i Java (`case-api`), en frontend i React, en varslingstjeneste i Kotlin (`notification`) og infrastruktur for Jenkins, OpenShift og observerbarhet. Delene endres ofte samtidig, for eksempel når et nytt felt i API-et også må vises i frontend. Prosjektet har én utvikler.

Java-tjenestene trenger et byggverktøy. De to vanlige valgene er Maven og Gradle.

## Beslutning

- **Ett repo (monorepo)** med én mappe per del: `case-api/`, `frontend/`, `notification/`, `infra/` og `docs/`.
- **Maven** som byggverktøy for Java- og Kotlin-tjenestene. Hver tjeneste er et selvstendig Maven-prosjekt med egen `pom.xml`, med Spring Boot som parent. Det er ingen felles parent-pom.
- **Maven Wrapper** (`mvnw`) sjekkes inn, slik at det holder å ha en JDK for å bygge.
- CI kjører per del, styrt av `paths`-filter. Workflowen for `case-api` kjører bare når noe i `case-api/` endres.

## Konsekvenser

- En endring som går på tvers av API og frontend kan gjøres i én pull request og gjennomgås samlet.
- ADR-er, README og docker-compose ligger ett sted for hele systemet.
- Uten en felles parent-pom må avhengighetsversjoner som ikke styres av Spring Boot (for eksempel WireMock), holdes like i hver tjeneste. Det er akseptabelt med to Maven-prosjekter. Blir det flere, bør vi vurdere en felles parent eller en BOM.
- Maven er deklarativt og standardisert. Livssyklusen er den samme i alle prosjekter (`test`, `verify`, `deploy`), og det passer godt med Jenkins og Nexus i fase 4. Ulempen er mindre fleksibilitet enn Gradle når bygget trenger egen logikk.
- `paths`-filtrene i CI betyr at en pull request som bare endrer dokumentasjon, ikke får noen byggsjekk. Hvis sjekkene senere blir påkrevd i branch protection, må dette løses, for eksempel med en jobb som alltid kjører.
