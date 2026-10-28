# 0006 Optimistisk låsing med ETag

- Status: Akseptert
- Dato: 2026-10-21

## Kontekst

Flere saksbehandlere kan ha den samme søknaden åpen. Uten beskyttelse kan dette skje:

1. A og B ser begge søknaden med status `IN_REVIEW`.
2. B innvilger.
3. A avslår, og overskriver Bs vedtak uten at noen merker det.

Tilstandsmaskinen stopper ikke dette, fordi `IN_REVIEW → REJECTED` er en gyldig overgang ut fra det A så. Problemet oppstår både når forespørsler kommer samtidig, og når en saksbehandler har hatt saken åpen en stund mens en kollega har endret den.

## Beslutning

Optimistisk låsing på to nivåer:

- **`@Version` på `GrantApplication`** (kolonnen `version`). Hibernate legger versjonen i `WHERE`-leddet ved oppdatering. Hvis to oppdateringer kommer samtidig, får den siste `OptimisticLockingFailureException`.
- **ETag og `If-Match` i API-et.** `GET`, opprettelse og statusendring svarer med `ETag: "<versjon>"`. `POST /status` krever `If-Match` med versjonen klienten har sett.

| Situasjon | Svar |
|---|---|
| `If-Match` stemmer | 200 og ny `ETag` |
| `If-Match` er utdatert | 412 Precondition Failed |
| `If-Match` mangler | 428 Precondition Required |
| To forespørsler kommer samtidig | Den ene får 412 |

- **Notater endrer ikke versjonen** (`@OptimisticLock(excluded = true)`). Notater blir bare lagt til og kan ikke komme i konflikt med en statusendring.
- **412 og ikke 409.** 409 er allerede brukt for ugyldige statusoverganger. 412 sier presist at klienten må hente søknaden på nytt.
- **Optimistisk og ikke pessimistisk låsing.** Konflikter er sjeldne, og vi vil ikke holde låser i databasen mens en saksbehandler tenker.

## Konsekvenser

- En saksbehandler kan ikke overskrive en endring de ikke har sett.
- Klienter må lagre `ETag` fra siste `GET` og sende den i `If-Match`. Ved 412 må de hente søknaden på nytt og vise brukeren hva som er endret.
- Hvis frontend og API får ulike origins, må `ETag` eksponeres med `Access-Control-Expose-Headers`.
- Klienter som ikke sender `If-Match`, blir avvist med 428. Det er strengere enn å la headeren være valgfri, men gir beskyttelse som standard.
- Testen for samtidige endringer må bruke ekte transaksjoner som committes (`@SpringBootTest` med `TransactionTemplate`), ikke `@DataJpaTest`, som ruller alt tilbake i én transaksjon.
