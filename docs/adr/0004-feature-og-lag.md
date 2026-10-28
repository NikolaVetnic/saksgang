# 0004 Inndeling etter feature med lag

- Status: Akseptert
- Dato: 2026-10-19

## Kontekst

Koden i `case-api` må organiseres i pakker. Det finnes tre vanlige alternativer:

1. **Etter lag:** `controllers/`, `services/`, `repositories/`. Én endring sprer seg over hele kodebasen.
2. **Etter feature, med lag inni:** `grantapplication/` inneholder sin egen controller, service og repository.
3. **Vertical slices:** én pakke per use case (`create/`, `changestatus/`, …) med egen request, handler og response, og ingen felles service.

`case-api` har seks endepunkter. Fem av dem jobber på det samme domeneobjektet og deler regler, blant annet oppslaget av organisasjonen og tilstandsmaskinen.

## Beslutning

Inndeling etter feature, med enkel lagdeling inni hver feature:

```
caseapi/
├── grantapplication/   controller, service, repository, domene og DTO-er
├── organization/       organisasjonsnummer og klienten mot Enhetsregisteret
├── statistics/         controller, service og repository med SQL
└── web/                felles feilhåndtering og OpenAPI
```

- Ingen generiske abstraksjoner som `BaseService` eller `BaseController`.
- Klasser som bare brukes i sin egen feature, er pakke-private (for eksempel controllere og `BrregEntity`).
- En service lages bare når det er logikk å legge der. Den skal ikke være et tomt mellomlag.

## Konsekvenser

- Alt som hører til én feature ligger samlet, og pakkestrukturen viser hva systemet gjør.
- Det er den vanligste strukturen i Spring-prosjekter, så nye utviklere kjenner den igjen.
- `GrantApplicationService` samler alle use cases for søknader. Hvis den vokser seg stor, bør vi dele den opp eller gå over til vertical slices for den featuren.
- Pakke-private controllere kan ikke refereres fra tester i andre pakker. Feilhåndteringen testes derfor med en egen testcontroller i stedet for de ekte controllerne.
