# Bruk av KI-verktøy

Stillingsbeskrivelser i offentlig sektor nevner i økende grad bruk av KI-verktøy. Dette dokumentet beskriver kort hvordan slike verktøy er brukt i Saksgang, og hvordan resultatet er kvalitetssikret.

## Hvor KI-verktøy er brukt

- **Kodeforslag og oppslag:** forslag til implementasjon og hjelp til å finne riktig API i nye versjoner av rammeverkene, for eksempel endringene i Spring Boot 4 og Spring Data 4.
- **Tester:** utkast til testtilfeller og grensetilfeller, blant annet for mod11-valideringen og tilstandsmaskinen.
- **Dokumentasjon:** utkast til README, ADR-er og beskrivelser av pull requests.

## Hvordan forslagene er kvalitetssikret

- **All kode er gjennomgått før commit.** Forslag er tatt med, endret eller forkastet etter vurdering. Arkitektur, navngiving og avgrensning er bestemt av meg.
- **Eksterne API-er er verifisert mot dokumentasjonen.** Atferden til Enhetsregisteret er sjekket mot den offisielle dokumentasjonen og det ekte API-et, ikke antatt. Det avdekket blant annet at en slettet enhet gir `200 OK` med `slettedato`, og ikke `404`.
- **Versjoner er sjekket mot kildene** (start.spring.io, Maven Central, GitHub) og ikke gjettet.
- **Tester skal kunne feile.** For sentrale tester er det sjekket at testen feiler når koden den beskytter, fjernes. Eksempler er låsingen av feilmeldinger til engelsk og optimistisk låsing.
- **Manuell testing i tillegg til automatiske tester.** En manuell kjøring mot lokal database avdekket en feil som enhetstestene med mocks ikke fanget. Nye notater ble returnert uten ID, fordi `save()` på en allerede lastet entitet gjør `merge` og lagrer en kopi. Feilen ble rettet, og det ble lagt til en regresjonstest mot ekte database.

## Erfaringer

KI-verktøy sparer tid på oppslag og på kode som følger et kjent mønster, men forslagene er bare så gode som verifiseringen rundt dem. Det som har gitt mest verdi, er å kombinere dem med tester mot ekte infrastruktur (Testcontainers og WireMock), og med å lese primærkildene når noe gjelder eksterne tjenester eller nye versjoner av rammeverkene.
