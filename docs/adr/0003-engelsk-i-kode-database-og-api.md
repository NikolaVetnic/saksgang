# 0003 Engelsk i kode, database og API

- Status: Akseptert
- Dato: 2026-10-18

## Kontekst

Det opprinnelige oppsettet la opp til engelske klassenavn, men tillot norske domenebegreper som `organisasjonsnummer`. Tabellene skulle ha norske navn (`soknad`, `soker_navn`), og API-et skulle ligge under `/api/soknader` med norske statusverdier (`MOTTATT`).

Underveis ble det tydelig at en blanding gir oversettelse i hvert lag. `GrantApplication` mappes til `soknad`, `RECEIVED` til `MOTTATT`, og `organizationNumber` til `organisasjonsnummer`. Hver slik mapping er kode som må skrives, testes og holdes i synk.

## Beslutning

Ett språk per lag:

| Lag | Språk |
|---|---|
| Java-kode, database, REST-API (stier, JSON-felt, statusverdier), feilmeldinger i API-et | Engelsk |
| README, ADR-er, commit-meldinger, pull requests og issues | Norsk |
| Brukergrensesnittet (fase 2) | Norsk |

Konkret betyr det:

- Klasser og tabeller heter `GrantApplication` / `grant_application`, `StatusChange` / `status_change` og `CaseNote` / `case_note`. Kolonnenavnene følger Spring Boots standard navnestrategi, slik at `@Table` og `@Column` ikke trengs.
- Statusverdiene er `RECEIVED`, `IN_REVIEW`, `APPROVED` og `REJECTED`, både i kode, database og API.
- API-et ligger under `/api/grant-applications`.
- Modulen heter `case-api`, og pakken heter `io.github.nikolavetnic.saksgang.caseapi`. `case` er et reservert ord i Java og kan ikke brukes som pakkenavn. Produktnavnet Saksgang beholdes.
- Feilmeldingene låses til engelsk (`spring.web.locale=en`, `spring.web.locale-resolver=fixed`). Ellers ville klientens `Accept-Language` gitt meldinger på andre språk fra Hibernate Validator.

## Konsekvenser

- Det er ingen mapping mellom norske og engelske navn, og samme ord brukes fra database til JSON.
- API-et blir lettere å ta i bruk for utviklere som ikke kan norsk, og det følger vanlig praksis for åpne API-er.
- Norske fagbegreper må oversettes én gang, ved navngiving. Vi har valgt `grant application` for tilskuddssøknad, `case officer` for saksbehandler og `case note` for saksnotat. Disse oversettelsene må brukes konsekvent.
- Frontend (fase 2) må oversette statusverdier og feilmeldinger til norsk i brukergrensesnittet. Det er et naturlig sted for slik oversettelse.
