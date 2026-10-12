# Działanie aplikacji

GPS S.O.S ma jeden ekran główny i widok dyktowania. Zakres pierwszej wersji to odczyt pozycji oraz jej przekazanie. Nie ma kont, serwera, mapy w aplikacji, śledzenia w tle ani automatycznej wysyłki alarmów.

## Przepływ danych

| Plik | Odpowiedzialność |
| --- | --- |
| [MainActivity.kt](../app/src/main/java/pl/gpssos/app/MainActivity.kt) | Uprawnienia, cykl życia, ustawienia Androida, schowek i systemowe udostępnianie. |
| [DeviceLocationSource.kt](../app/src/main/java/pl/gpssos/app/DeviceLocationSource.kt) | Odczyty z `LocationManager` i zamiana pomiaru na `GeoFix`. |
| [GpsViewModel.kt](../app/src/main/java/pl/gpssos/app/GpsViewModel.kt) | Stan ekranu, wybór pozycji i odczyt zatrzymany do dyktowania. |
| [LocationText.kt](../app/src/main/java/pl/gpssos/app/LocationText.kt) | Format współrzędnych, półkule, cyfry słownie, wiek pomiaru i wiadomość. |
| [GpsScreen.kt](../app/src/main/java/pl/gpssos/app/GpsScreen.kt) | Widoki Compose, komunikaty o stanie lokalizacji i przyciski. |

Aktywność uruchamia odbiór w `onStart` i zatrzymuje go w `onStop`. Zmiana ustawień lokalizacji powoduje ponowne sprawdzenie dostępności. Przy utracie uprawnień lub przejściu z dokładnej lokalizacji na przybliżoną model usuwa dotychczasowy odczyt z ekranu, również ten zatrzymany do dyktowania.

## Wybór i świeżość pozycji

Źródłem danych jest systemowy `LocationManager`, bez zależności od Google Play services. Przy dokładnym uprawnieniu aplikacja korzysta z dostępnych dostawców GPS, sieciowego i pasywnego. Przy uprawnieniu przybliżonym pomija dostawcę GPS. Najpierw może pokazać ostatni pomiar przechowywany przez Androida, a następnie przyjmować aktualizacje.

Nowszy pomiar według `elapsedRealtimeNanos` zastępuje poprzedni. Przy identycznym czasie wybierany jest pomiar z mniejszym promieniem szacowanej dokładności. Starszy odczyt nie zastępuje nowszego tylko dlatego, że podaje większą dokładność.

Wiek pozycji jest obliczany z zegara monotonicznego i aktualizowany co sekundę. Od 60 sekund pomiar otrzymuje oznaczenie nieaktualnej pozycji. Wiadomość zawiera osobno czas kalendarzowy pomiaru ze strefą czasową. Po około 15 sekundach oczekiwania bez pozycji interfejs pokazuje dodatkową wskazówkę.

Współrzędne mają pięć miejsc po przecinku. Wiadomość zawiera parę liczb ze znakami oraz osobne wartości z półkulami N/S i E/W. Widok dyktowania rozpisuje każdą cyfrę, zachowując zera. Format zapisu nie zwiększa dokładności pomiaru.

## Dyktowanie i udostępnianie

Przycisk **Podaj przez telefon** kopiuje bieżący `GeoFix` do stanu `readingFix`. Nowe pomiary mogą aktualizować główną pozycję, ale nie zmieniają dyktowanych cyfr. Wiek zatrzymanego odczytu nadal rośnie i może uruchomić ostrzeżenie o nieaktualności. Ekran pozostaje włączony, dopóki widok dyktowania jest aktywny.

Udostępnianie używa `Intent.ACTION_SEND` z typem `text/plain` i systemowego okna wyboru. Aplikacja nie wybiera odbiorcy i nie potwierdza dostarczenia wiadomości. Kopiowanie zachowuje tę samą treść, w tym dokładność, czas oraz oznaczenia pozycji przybliżonej, testowej i nieaktualnej.

## Dane i uprawnienia

[Manifest](../app/src/main/AndroidManifest.xml) deklaruje `ACCESS_COARSE_LOCATION` i `ACCESS_FINE_LOCATION`. Nie deklaruje dostępu do lokalizacji w tle, internetu, kontaktów ani wysyłania SMS.

Odczyty pozostają w pamięci procesu. Aplikacja nie zapisuje ich do pliku, bazy danych ani preferencji. Zapamiętuje jedynie, czy wcześniej prosiła o uprawnienie lokalizacji. Android może niezależnie przechowywać ostatnią znaną pozycję i przekazać ją przy ponownym uruchomieniu. Manifest wyłącza kopie zapasowe aplikacji przez `allowBackup="false"`.

Schowek, systemowe udostępnianie i aplikacja odbiorcza mają własne zasady obsługi danych. Link w wiadomości zawiera współrzędne w adresie Google Maps. GPS S.O.S tworzy ten adres jako tekst; otwarcie go odbywa się poza aplikacją.

[Powrót do README](../README.md)
