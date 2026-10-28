# 0005 Problem Details og feilkoder

- Status: Akseptert
- Dato: 2026-10-20

## Kontekst

API-et kan feile av mange grunner: ugyldig input, ukjent søknad, en statusovergang som ikke er lov, en organisasjon som ikke finnes, at Enhetsregisteret er nede, eller at noen andre har endret søknaden i mellomtiden. Klientene trenger et forutsigbart feilformat og statuskoder de kan reagere på.

## Beslutning

- **Alle feil returneres som RFC 9457 Problem Details** (`application/problem+json`) med `title`, `status` og `detail`, og ved behov egne felt som `errors`, `currentStatus` og `allowedStatuses`.
- **All mapping skjer i `ApiExceptionHandler`** (`@RestControllerAdvice`, arver fra `ResponseEntityExceptionHandler`). Spring MVC sine egne feil, som ugyldig JSON og ukjente parametre, håndteres av basisklassen.
- **Domenet vet ingenting om HTTP.** Exceptionene i `grantapplication` og `organization` har ingen `@ResponseStatus`.
- **Statuskoder:**

  | Kode | Brukes når |
  |---|---|
  | 400 | Forespørselen er feil formet: valideringsfeil, ugyldig JSON, ukjent statusverdi eller sorteringsfelt |
  | 404 | Søknaden eller ruten finnes ikke |
  | 409 | Overgangen er ikke lov i tilstandsmaskinen |
  | 412 | Klienten har basert endringen på en utdatert versjon (se [0006](0006-optimistisk-laasing-med-etag.md)) |
  | 422 | Forespørselen er gyldig, men organisasjonen er ukjent eller slettet i Enhetsregisteret |
  | 428 | `If-Match` mangler ved statusendring |
  | 503 | Enhetsregisteret svarer ikke, eller svarer med feil |
  | 500 | Alt annet |

- **Ingen interne detaljer går ut.** Ved 500 og 503 får klienten en generell melding, mens årsaken logges.

## Konsekvenser

- Klienter kan håndtere feil generisk ut fra `status` og `title`, og vise `detail` eller `errors` til brukeren.
- Forskjellen på 400 og 422 er tydelig: 400 betyr at klienten sendte noe feil, mens 422 betyr at forespørselen er riktig, men ikke kan behandles.
- Nye domenefeil krever en handler i `ApiExceptionHandler`. Hvis det glemmes, blir resultatet 500 og ikke en lekkasje av detaljer. Den generelle handleren er sikkerhetsnettet.
- Feilhåndteringen testes uavhengig av featurene, med en egen testcontroller.
