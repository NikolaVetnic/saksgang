# 0002 Flyway og eksplisitt SQL

- Status: Akseptert
- Dato: 2026-10-18

## Kontekst

`case-api` lagrer søknader, statushistorikk og saksnotater i PostgreSQL. Skjemaet må kunne endres trygt over tid og være likt i alle miljøer: lokalt, i tester og i drift.

Det meste av dataaksessen er enkel CRUD. Statistikken (antall søknader og sum av beløp per status) er derimot en aggregering, der det ikke er behov for å laste entiteter.

## Beslutning

- **Flyway eier skjemaet.** Alle endringer er versjonerte SQL-filer i `src/main/resources/db/migration` (`V1__create_grant_application.sql` osv.). En migrering som er tatt i bruk, endres aldri. Endringer skjer med en ny migrering.
- **Hibernate validerer, men endrer ikke.** `spring.jpa.hibernate.ddl-auto=validate` gjør at appen ikke starter hvis entitetene ikke stemmer med skjemaet.
- **Feilnavngitte migreringer stopper oppstarten** (`spring.flyway.validate-migration-naming=true`). Ellers hopper Flyway stille over filer som ikke følger navnemønsteret.
- **JPA (Spring Data) brukes til CRUD.** Søknaden er en aggregatrot, og historikk og notater lagres via den.
- **Eksplisitt SQL med `JdbcClient` brukes til rapporter.** Statistikken er én `group by`-spørring som returnerer records direkte.
- **Databasen håndhever også reglene** med `check`-constraints på organisasjonsnummer, beløp og statusverdier, i tillegg til valideringen i applikasjonen.

## Konsekvenser

- Skjemaendringer kan gjennomgås som vanlig SQL i pull requests, og historikken ligger i git.
- Testene kjører de samme migreringene mot PostgreSQL i Testcontainers som appen gjør. Feil i SQL eller mapping oppdages før deploy.
- Vi bruker to verktøy for dataaksess. Utviklere må vite når de skal velge JPA og når SQL. Tommelfingerregelen er JPA for å lese og endre aggregater, og SQL for aggregering og rapporter.
- SQL-spørringen er knyttet til PostgreSQL-syntaks. Det er akseptabelt, siden PostgreSQL er fastsatt for prosjektet.
- Uten `ddl-auto=update` må hver skjemaendring skrives som en migrering. Det er et bevisst valg, fordi automatiske skjemaendringer er risikable i drift.
