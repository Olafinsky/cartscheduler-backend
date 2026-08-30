# CartScheduler Backend

Backend aplikacji CartScheduler oparty na Spring Boot, Java 21 i Gradle.

## Uruchomienie w Dockerze

Wymagany jest Docker Desktop z włączonym Docker Compose.

```powershell
docker compose up --build
```

Aplikacja będzie dostępna pod adresem `http://localhost:8080`. Compose uruchamia także MySQL 8.4, czeka na jego gotowość, a następnie aplikacja wykonuje migracje Flyway.

W środowisku lokalnym MySQL jest dostępny wyłącznie z hosta pod `127.0.0.1:3306` (port można zmienić przez `MYSQL_HOST_PORT`). Do połączenia użyj wartości `MYSQL_DATABASE`, `MYSQL_USER` i `MYSQL_PASSWORD` z pliku `.env`.

Zatrzymanie kontenerów:

```powershell
docker compose down
```

Pełne wyczyszczenie danych MySQL i uruchomienie migracji od początku:

```powershell
docker compose down -v
```

Domyślne dane są przeznaczone wyłącznie do lokalnego uruchomienia. Aby ustawić własne, skopiuj `.env.example` do `.env` i zmień hasła oraz `JWT_SECRET` przed wdrożeniem.

## Automatyczna przebudowa w DEV

Do pracy nad backendem w Dockerze użyj trybu obserwowania plików:

```powershell
docker compose up --build --watch
```

Po zapisaniu zmiany w `src`, konfiguracji Gradle, wrapperze Gradle lub `Dockerfile`, Compose przebuduje obraz i odtworzy wyłącznie kontener `app`. Kontener i wolumen bazy danych nie są usuwane.

Jeśli chcesz mieć logi aplikacji i obserwowanie plików w osobnych terminalach, uruchom:

```powershell
docker compose up -d --build
docker compose watch app
```

## Migracje bazy danych

Schemat bazy jest zarządzany przez Flyway. Pierwsza migracja znajduje się w [V1__initial_schema.sql](src/main/resources/db/migration/V1__initial_schema.sql). Każdą następną zmianę schematu dodawaj jako nowy, wersjonowany plik, na przykład `V2__add_participant_phone.sql`.

Jeżeli baza została wcześniej utworzona przez dawny skrypt `cartscheduler.sql`, przed pierwszym uruchomieniem Flyway w środowisku lokalnym wykonaj `docker compose down -v`. W przeciwnym razie Flyway wykryje niepusty schemat bez swojej historii migracji.

## Uruchomienie lokalne

```powershell
.\gradlew.bat bootRun
```

Domyślne wartości konfiguracji można nadpisać zmiennymi `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` oraz `JWT_EXPIRATION`.

## Testy

| Rodzaj | Katalog | Konwencja nazwy | Polecenie |
| --- | --- | --- | --- |
| Jednostkowe | `src/test/java` | `*Test` | `.\gradlew.bat test` |
| Integracyjne | `src/integrationTest/java` | `*IT` | `.\gradlew.bat integrationTest` |
| Funkcjonalne API | `src/functionalTest/java` | `*FunctionalTest` | `.\gradlew.bat functionalTest` |

Testy integracyjne i funkcjonalne używają Testcontainers z izolowanym MySQL, więc wymagają uruchomionego Docker Desktop. Nie korzystają z bazy uruchomionej przez `docker compose`. Polecenie `.\gradlew.bat check` uruchamia wszystkie trzy grupy testów.

Przed uruchomieniem testów korzystających z Testcontainers sprawdź, czy demon Dockera działa:

```powershell
docker info
```

Zakres zestawu testów:

- jednostkowe: JWT, filtr uwierzytelniania, serwisy, kontrolery oraz generator wpisów harmonogramu;
- integracyjne: migracja Flyway, relacje JPA i własne zapytania repozytoriów na MySQL;
- funkcjonalne: pełny przepływ HTTP — token zaproszenia, JWT, autoryzacja, harmonogramy, dni, uczestnicy, pobieranie propozycji oraz aktualnie niezaimplementowane usuwanie propozycji (`501`).
