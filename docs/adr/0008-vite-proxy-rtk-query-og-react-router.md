# 0008 Vite-proxy, RTK Query og React Router

- Status: Akseptert
- Dato: 2026-10-30

## Kontekst

`frontend` må gjøre tre ting som krever et valg:

1. Nå `case-api`, som kjører på en annen port under utvikling. For nettleseren er det en annen opprinnelse (origin).
2. Hente og endre data, med lasting, feil og oppdatering etter endringer.
3. Navigere mellom søknadsskjema, saksliste og saksdetalj.

Prosjektet skal vise stacken som er vanlig i offentlig sektor, og Redux er en del av den.

## Beslutning

### Vite-proxy i stedet for CORS

Vites dev-server sender `/api` videre til `case-api` på port 8080. Nettleseren ser bare én opprinnelse.

Alternativet var CORS i `case-api`. Da måtte backend tillate opprinnelsen til frontend, svare på preflight-forespørsler (`If-Match` og JSON utløser dem) og eksponere `ETag` med `Access-Control-Expose-Headers`, jf. ADR 0006. Planen er at frontend og API skal ha samme opprinnelse også når de deployes i fase 4, så konfigurasjonen ville bare vært i bruk under utvikling.

### RTK Query for API-kall

Alle kall går gjennom ett `api`-objekt i `src/app/api.ts`. Features legger til sine endepunkter med `injectEndpoints()`.

Alternativene var:

- **`createAsyncThunk` med egne slices:** Gir samme resultat, men vi må selv skrive lasting, feil, cache og oppdatering for hvert endepunkt.
- **TanStack Query:** Et godt alternativ for serverdata, men det er ikke en del av Redux, som prosjektet skal vise.

### React Router for visningene

Visningene får egne URL-er, som `/soknad`, `/saker` og `/saker/:id`. URL-ene er på norsk fordi brukerne ser dem. API-et er fortsatt på engelsk, jf. ADR 0003.

Alternativet var å velge visning med tilstand i Redux. Det sparer en avhengighet, men da kan ikke en sak lenkes til, tilbakeknappen virker ikke, og filteret i sakslisten kan ikke deles.

## Konsekvenser

- `case-api` trenger ingen CORS-konfigurasjon. Klienter fra andre opprinnelser kan dermed ikke bruke API-et fra nettleseren.
- Proxyen finnes bare i Vite. Fase 4 må sørge for at `/api` rutes til `case-api` og at frontend har samme opprinnelse.
- Dype lenker krever at webserveren svarer med `index.html` på alle stier. Vite gjør det under utvikling, men det må settes opp i fase 4.
- Redux Toolkit er mer enn en så liten app strengt tatt trenger. Vi tar den kostnaden fordi Redux skal vises, og RTK Query holder mengden egen kode nede.
- Headere som `ETag` er ikke en del av resultatet fra RTK Query som standard, og må hentes ut eksplisitt.
- React Router er en avhengighet utover det som opprinnelig var planlagt.
