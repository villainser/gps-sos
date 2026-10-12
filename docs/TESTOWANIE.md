# Testowanie GPS S.O.S

## Sprawdzona wersja

Weryfikacja pierwszej wersji z 12 października 2026 r. obejmowała:

| Sprawdzenie | Wynik |
| --- | --- |
| `assembleDebug` | APK zbudowany; podpis debug zweryfikowany. |
| `testDebugUnitTest` | 20 testów, 0 błędów i 0 pominięć. |
| `lintDebug` | 0 błędów, 10 ostrzeżeń. |
| Wygląd ekranu głównego, dyktowania i czcionki 200% | Wyrenderowane obrazy sprawdzone wizualnie. |
| Rzeczywisty odbiornik GPS i wysłanie wiadomości na telefonie | Jeszcze niesprawdzone. |

Ostrzeżenia Lint dotyczą dostępności nowszych zależności, reguł ekstrakcji danych, kwalifikatora zasobów `v26` i możliwości użycia rozszerzenia KTX. Brak błędów Lint nie oznacza ukończonych prób na urządzeniu.

## Testy automatyczne

Uruchom z katalogu głównego repozytorium, po skonfigurowaniu środowiska opisanego w [README](../README.md):

```sh
./gradlew testDebugUnitTest lintDebug
```

[LocationTextTest](../app/src/test/java/pl/gpssos/app/LocationTextTest.kt) zawiera 14 testów formatowania i wyboru pomiaru. Obejmuje znaki, półkule, zera, zaokrąglanie, wiek odczytu, zmianę czasu kalendarzowego oraz oznaczenia pozycji przybliżonej i testowej.

[GpsScreenTest](../app/src/test/java/pl/gpssos/app/GpsScreenTest.kt) zawiera 6 testów Compose w Robolectric, na modelu API 35 i ekranie 360 × 800 dp. Sprawdza przycisk uprawnień, blokadę udostępniania bez pozycji, przekazany odczyt, niezmienność dyktowanych cyfr, dostępność głównych przycisków przy czcionce 200% oraz treść intencji udostępniania. Nie otwiera rzeczywistego okna uprawnień ani aplikacji SMS.

Raporty powstają w `app/build/reports/tests/testDebugUnitTest/` i `app/build/reports/lint-results-debug.html`.

Opcjonalne obrazy interfejsu można wygenerować poleceniem:

```sh
./gradlew testDebugUnitTest --rerun-tasks -PscreenshotDir="$PWD/app/build/screenshots"
```

To polecenie używa składni powłoki POSIX. Na Windows przekaż do `gradlew.bat` własną ścieżkę bezwzględną jako wartość `screenshotDir`. Obrazy zawierają syntetyczny odczyt testowy.

## Próby na telefonie przed użyciem terenowym

Poniższa lista jest planem prób, nie wykazem zaliczonych testów. Do sprawdzania wysyłki użyj własnego kontaktu testowego.

- [ ] Pierwsze uruchomienie: zezwolenie na dokładną lokalizację, odczyt na otwartej przestrzeni, poprawny czas i dokładność.
- [ ] Odmowa uprawnień, ponowna odmowa i powrót z ustawień: czy aplikacja prowadzi do działającego sposobu nadania dostępu.
- [ ] Lokalizacja przybliżona: oznaczenie na ekranie i w przekazanym tekście.
- [ ] Wyłączenie i ponowne włączenie lokalizacji: komunikat, odzyskanie odczytu i widoczna świeżość pomiaru.
- [ ] Brak internetu: odczyt GPS przy dostępnym sygnale; oddzielnie sprawdzić zachowanie wybranej aplikacji do wiadomości.
- [ ] Dyktowanie: niezmienne cyfry podczas ruchu, obrót telefonu, ostrzeżenie po 60 sekundach, powrót do aktualnej pozycji.
- [ ] Przejście do tła i powrót: wznowienie odczytu oraz zachowanie po odebraniu lub ograniczeniu uprawnień.
- [ ] Udostępnianie i kopiowanie: zgodność współrzędnych, dokładności, czasu i linku; osobno anulowanie udostępniania i rzeczywisty odbiór wiadomości przez kontakt testowy.
- [ ] TalkBack, duża czcionka, mały ekran, orientacja pozioma i ciemny motyw: czy wszystkie informacje i działania pozostają dostępne.
- [ ] Przynajmniej telefon z Androidem 8 oraz telefon z nowszym Androidem: instalacja i cały podstawowy przebieg.

[Powrót do README](../README.md)
