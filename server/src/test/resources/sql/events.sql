CREATE TABLE IF NOT EXISTS events (
                                      event_id UUID PRIMARY KEY,
                                      title TEXT NOT NULL,
                                      event_date TIMESTAMPTZ NOT NULL,
                                      location TEXT NOT NULL,
                                      afishe_url TEXT,
                                      is_online BOOLEAN NOT NULL DEFAULT FALSE,
                                      is_offline BOOLEAN NOT NULL DEFAULT FALSE,
                                      language TEXT NOT NULL,
                                      description TEXT NOT NULL,
                                      offline_address TEXT,
                                      registration_url TEXT
);

ALTER TABLE events ADD COLUMN IF NOT EXISTS registration_url TEXT;
ALTER TABLE events ADD COLUMN IF NOT EXISTS is_offline BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE events
SET is_offline = offline_address IS NOT NULL
WHERE is_offline = FALSE;

INSERT INTO events (
    event_id, title, event_date, location, afishe_url, is_online, is_offline,
    language, description, offline_address, registration_url
)
VALUES
    (
        '40000000-0000-4000-8000-000000000001',
        'Зустріч читацького клубу: сучасна українська проза',
        '2026-10-18 14:00:00 Europe/Paris',
        'Культурний центр «Толока», Нант',
        NULL,
        FALSE,
        TRUE,
        'Українська',
        'Обговоримо нові голоси української літератури та оберемо книжку для наступної зустрічі. Приєднуйтеся з власними рекомендаціями.',
        '8 Rue de la Paix, 44000 Nantes',
        NULL
    ),
    (
        '40000000-0000-4000-8000-000000000002',
        'Онлайн-розмова з перекладачкою',
        '2026-10-25 18:30:00 Europe/Paris',
        'Онлайн, посилання надішлемо зареєстрованим учасникам',
        NULL,
        TRUE,
        TRUE,
        'Українська та французька',
        'Поговоримо про те, як українські книжки знаходять нових читачів у Франції, і про виклики літературного перекладу.',
        '8 Rue de la Paix, 44000 Nantes',
        NULL
    ),
    (
        '40000000-0000-4000-8000-000000000003',
        'Казкова субота для дітей',
        '2026-11-07 11:00:00 Europe/Paris',
        'Медіатека Жака Демі, Нант',
        NULL,
        FALSE,
        TRUE,
        'Українська',
        'Читаємо українські казки, малюємо улюблених героїв і знайомимося з книжками для всієї родини. Подія для дітей від 5 років.',
        '24 Quai de la Fosse, 44000 Nantes',
        NULL
    ),
    (
        '40000000-0000-4000-8000-000000000004',
        'Літературний вечір: Україна і Франція',
        '2026-11-21 19:00:00 Europe/Paris',
        'Maison de l’Europe, Нант',
        NULL,
        FALSE,
        TRUE,
        'Українська та французька',
        'Вечір читань українською та французькою мовами про дім, пам’ять і нові початки. Тексти читатимуть учасники клубу.',
        '33 Rue de Strasbourg, 44000 Nantes',
        NULL
    ),
    (
        '40000000-0000-4000-8000-000000000005',
        'Відкрита онлайн-полиця: що читаємо цієї осені',
        '2026-12-05 17:00:00 Europe/Paris',
        'Онлайн-зустріч у Google Meet',
        NULL,
        TRUE,
        FALSE,
        'Українська',
        'Учасники бібліотеки діляться книжками, які їх захопили цієї осені. Можна долучитися, щоб розповісти про свою знахідку або просто послухати.',
        NULL,
        NULL
    )
    ON CONFLICT (event_id) DO NOTHING;

UPDATE events
SET afishe_url = '/img/events.png'
WHERE event_id BETWEEN '40000000-0000-4000-8000-000000000001' AND '40000000-0000-4000-8000-000000000005'
  AND afishe_url IS NULL;

UPDATE events
SET is_online = TRUE,
    is_offline = TRUE,
    location = 'Онлайн та наживо у культурному центрі «Толока», Нант',
    offline_address = '8 Rue de la Paix, 44000 Nantes'
WHERE event_id = '40000000-0000-4000-8000-000000000002';
