# 0007 Ingen autentisering i fase 2

- Status: Akseptert
- Dato: 2026-10-30

## Kontekst

Saksgang har to brukergrupper: innbyggere som søker om tilskudd, og saksbehandlere som behandler søknadene. I et ekte system ville de logget inn på hver sin måte og hatt ulik tilgang.

Innlogging gir mye ekstra arbeid: en identitetsleverandør, tokenhåndtering i frontend, tilgangsstyring i backend og testdata for hver rolle. Hovedvekten i prosjektet ligger på saksflyten, testautomasjon og arkitekturvalg, og en innlogging som bare er delvis gjennomført, ville gitt et falskt inntrykk av sikkerhet.

## Beslutning

Fase 2 har ingen autentisering eller autorisasjon, verken i `frontend` eller i `case-api`.

- Alle endepunkter og visninger er åpne for alle som når dem.
- Visningene for innbyggere og saksbehandlere er bare skilt med ulike URL-er.
- Systemet vet ikke hvem som gjør hva. Statusendringer og notater lagres uten bruker.
- Systemet skal bare brukes med fiktive testdata.

## Konsekvenser

Dette er en bevisst avgrensning som gjør systemet uegnet for produksjon slik det er nå:

- Hvem som helst kan lese alle søknader, også navn og e-postadresser.
- Hvem som helst kan innvilge eller avslå en søknad.
- Det finnes ingen revisjonsspor som viser hvem som har tatt en beslutning. Statushistorikken viser bare hva som skjedde og når.
- Søkeren kan oppgi hvilken som helst organisasjon. Ingenting sjekker at søkeren har rett til å representere den.
- Fase 4 må ta stilling til dette før noe gjøres tilgjengelig på nett. En åpen Route i OpenShift eksponerer hele API-et.

Fordelen er at frontend og testene blir enklere, og at fasen kan bruke tiden på det den skal vise.

## Slik kan det løses senere

- **Innbyggere:** innlogging med ID-porten.
- **Saksbehandlere:** innlogging med virksomhetens egen identitetsleverandør, for eksempel Entra ID.
- **`case-api`:** Spring Security som OAuth2 resource server, som validerer JWT og styrer tilgang etter rolle. Innbyggere skal bare se sine egne søknader.
- **`frontend`:** Authorization Code-flyt med PKCE, eller en backend-for-frontend som holder på tokenene.
- **Representasjon:** retten til å søke på vegne av en organisasjon kan sjekkes mot for eksempel Altinn.
- **Revisjonsspor:** en Flyway-migrering som legger til hvem som utførte handlingen på `status_change` og `case_note`.

Begge innloggingene bruker OpenID Connect, så resten av systemet trenger ikke å vite hvilken identitetsleverandør en bruker kommer fra.
