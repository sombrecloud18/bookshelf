-- =====================================================================
-- V5: Extended seed data — test users (students + teacher), subjects,
--      additional books with covers, reviews, collections, subject
--      collections, events with participants, comments, likes.
-- All test users share password 'admin123' (same bcrypt hash as the
-- admin account). Cover URLs use covers.openlibrary.org.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Test users
-- ---------------------------------------------------------------------
-- Password hash below corresponds to "admin123".
INSERT INTO users (id, login, email, password_hash, first_name, last_name, patronymic,
                   faculty, specialty, course, phone_number, role, user_type)
VALUES
  ('11111111-1111-1111-1111-111111111101', 'ivanov',  'ivanov@bsuir.by',
   '$2a$10$f8O0f5OywnGAWk6s4DmUj.iq1Zqx9uWlFTi9vECfmcL0QcvRTcPwa',
   'Иван',    'Иванов',   'Сергеевич',
   'ФКП',   'ПОИТ',  '3 курс', '+375(29)111-11-11', 'USER', 'STUDENT'),
  ('11111111-1111-1111-1111-111111111102', 'petrova', 'petrova@bsuir.by',
   '$2a$10$f8O0f5OywnGAWk6s4DmUj.iq1Zqx9uWlFTi9vECfmcL0QcvRTcPwa',
   'Анна',    'Петрова',  'Викторовна',
   'ФКСИС', 'ИИ',    '2 курс', '+375(29)222-22-22', 'USER', 'STUDENT'),
  ('11111111-1111-1111-1111-111111111103', 'sidorov', 'sidorov@bsuir.by',
   '$2a$10$f8O0f5OywnGAWk6s4DmUj.iq1Zqx9uWlFTi9vECfmcL0QcvRTcPwa',
   'Алексей', 'Сидоров',  'Михайлович',
   'ФИБ',   'ЗИ',    '4 курс', '+375(29)333-33-33', 'USER', 'STUDENT'),
  ('11111111-1111-1111-1111-111111111104', 'kozlova', 'kozlova@bsuir.by',
   '$2a$10$f8O0f5OywnGAWk6s4DmUj.iq1Zqx9uWlFTi9vECfmcL0QcvRTcPwa',
   'Мария',   'Козлова',  'Александровна',
   'ФИТУ',  'ИСиТ',  '1 курс', '+375(29)444-44-44', 'USER', 'STUDENT'),
  ('11111111-1111-1111-1111-111111111105', 'morozov', 'morozov@bsuir.by',
   '$2a$10$f8O0f5OywnGAWk6s4DmUj.iq1Zqx9uWlFTi9vECfmcL0QcvRTcPwa',
   'Дмитрий', 'Морозов',  'Олегович',
   'ФКП',   'ВМСиС', '3 курс', '+375(29)555-55-55', 'USER', 'STUDENT')
ON CONFLICT (login) DO NOTHING;

-- Teacher
INSERT INTO users (id, login, email, password_hash, first_name, last_name, patronymic,
                   faculty, role, user_type, department, position)
VALUES
  ('11111111-1111-1111-1111-111111111201', 'smirnov', 'smirnov@bsuir.by',
   '$2a$10$f8O0f5OywnGAWk6s4DmUj.iq1Zqx9uWlFTi9vECfmcL0QcvRTcPwa',
   'Олег', 'Смирнов', 'Николаевич',
   'ФКП', 'USER', 'TEACHER', 'Кафедра программного обеспечения информационных технологий', 'Доцент')
ON CONFLICT (login) DO NOTHING;

-- ---------------------------------------------------------------------
-- Subjects (master list per specialty, matches studyData.js)
-- ---------------------------------------------------------------------
INSERT INTO subjects (specialty, name) VALUES
  ('ПОИТ',  'Программирование'),
  ('ПОИТ',  'Алгоритмы и структуры данных'),
  ('ПОИТ',  'Базы данных'),
  ('ПОИТ',  'Веб-технологии'),
  ('ПОИТ',  'Операционные системы'),
  ('ВМСиС', 'Архитектура ЭВМ'),
  ('ВМСиС', 'Компьютерные сети'),
  ('ВМСиС', 'Цифровая схемотехника'),
  ('ВМСиС', 'Микропроцессорные системы'),
  ('ИИ',    'Машинное обучение'),
  ('ИИ',    'Нейронные сети'),
  ('ИИ',    'Анализ данных'),
  ('ИИ',    'Компьютерное зрение'),
  ('ПИ',    'Программная инженерия'),
  ('ПИ',    'Управление проектами'),
  ('ПИ',    'Тестирование ПО'),
  ('ЗИ',    'Криптография'),
  ('ЗИ',    'Сетевая безопасность'),
  ('ЗИ',    'Аудит информационной безопасности'),
  ('ИСиТ',  'Информационные системы'),
  ('ИСиТ',  'Корпоративные ИТ'),
  ('ИСиТ',  'СУБД')
ON CONFLICT (specialty, name) DO NOTHING;

-- ---------------------------------------------------------------------
-- Update existing V3 books with cover URLs
-- ---------------------------------------------------------------------
UPDATE books SET cover_url = 'https://covers.openlibrary.org/b/isbn/9785170389424-L.jpg',
                 image_url = 'https://covers.openlibrary.org/b/isbn/9785170389424-L.jpg'
 WHERE isbn = '978-5-17-038942-4';
UPDATE books SET cover_url = 'https://covers.openlibrary.org/b/isbn/9785389073057-L.jpg',
                 image_url = 'https://covers.openlibrary.org/b/isbn/9785389073057-L.jpg'
 WHERE isbn = '978-5-389-07305-7';
UPDATE books SET cover_url = 'https://covers.openlibrary.org/b/isbn/9785040919516-L.jpg',
                 image_url = 'https://covers.openlibrary.org/b/isbn/9785040919516-L.jpg'
 WHERE isbn = '978-5-04-091951-6';
UPDATE books SET cover_url = 'https://covers.openlibrary.org/b/isbn/9785170963114-L.jpg',
                 image_url = 'https://covers.openlibrary.org/b/isbn/9785170963114-L.jpg'
 WHERE isbn = '978-5-17-096311-4';
UPDATE books SET cover_url = 'https://covers.openlibrary.org/b/isbn/9785389162980-L.jpg',
                 image_url = 'https://covers.openlibrary.org/b/isbn/9785389162980-L.jpg'
 WHERE isbn = '978-5-389-16298-0';
UPDATE books SET cover_url = 'https://covers.openlibrary.org/b/isbn/9785171197150-L.jpg',
                 image_url = 'https://covers.openlibrary.org/b/isbn/9785171197150-L.jpg'
 WHERE isbn = '978-5-17-119715-0';
UPDATE books SET cover_url = 'https://covers.openlibrary.org/b/isbn/9785040896248-L.jpg',
                 image_url = 'https://covers.openlibrary.org/b/isbn/9785040896248-L.jpg'
 WHERE isbn = '978-5-04-089624-8';
UPDATE books SET cover_url = 'https://covers.openlibrary.org/b/isbn/9785171363547-L.jpg',
                 image_url = 'https://covers.openlibrary.org/b/isbn/9785171363547-L.jpg'
 WHERE isbn = '978-5-17-136354-7';
UPDATE books SET cover_url = 'https://covers.openlibrary.org/b/isbn/9785041614729-L.jpg',
                 image_url = 'https://covers.openlibrary.org/b/isbn/9785041614729-L.jpg'
 WHERE isbn = '978-5-04-161472-9';
UPDATE books SET cover_url = 'https://covers.openlibrary.org/b/isbn/9785171160550-L.jpg',
                 image_url = 'https://covers.openlibrary.org/b/isbn/9785171160550-L.jpg'
 WHERE isbn = '978-5-17-116055-0';

-- ---------------------------------------------------------------------
-- Additional books — technical / academic, used by subject collections
-- ---------------------------------------------------------------------
INSERT INTO books (id, title, author, genre, year, description, full_description,
                   image_url, cover_url, pages, publisher, publish_year, isbn, status) VALUES
  ('22222222-2222-2222-2222-222222222201',
   'Совершенный код', 'Стив Макконнелл', 'Программирование', 2004,
   'Практическое руководство по разработке программного обеспечения.',
   'Энциклопедия лучших практик разработки ПО: декомпозиция задач, понятный код, отладка, рефакторинг и инженерные стандарты качества.',
   'https://covers.openlibrary.org/b/isbn/9780735619678-L.jpg',
   'https://covers.openlibrary.org/b/isbn/9780735619678-L.jpg',
   896, 'Русская редакция', 2017, '978-5-7502-0064-1', 'ACTIVE'),

  ('22222222-2222-2222-2222-222222222202',
   'Чистый код', 'Роберт Мартин', 'Программирование', 2008,
   'Создание, анализ и рефакторинг кода: дисциплина, делающая программистов профессионалами.',
   'Книга-классика о принципах чистого кода: имена, функции, комментарии, форматирование, обработка ошибок и тестирование.',
   'https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg',
   'https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg',
   464, 'Питер', 2018, '978-5-4461-0960-1', 'ACTIVE'),

  ('22222222-2222-2222-2222-222222222203',
   'Алгоритмы. Построение и анализ', 'Кормен, Лейзерсон, Ривест, Штайн', 'Программирование', 2009,
   'Фундаментальный учебник по алгоритмам и структурам данных.',
   'Универсальный справочник по алгоритмам — от базовых сортировок и графов до продвинутых тем (NP-полнота, потоки, рандомизация).',
   'https://covers.openlibrary.org/b/isbn/9780262033848-L.jpg',
   'https://covers.openlibrary.org/b/isbn/9780262033848-L.jpg',
   1296, 'Вильямс', 2013, '978-5-8459-1794-2', 'ACTIVE'),

  ('22222222-2222-2222-2222-222222222204',
   'Грокаем алгоритмы', 'Адитья Бхаргава', 'Программирование', 2016,
   'Иллюстрированное пособие по алгоритмам для программистов.',
   'Доступное введение в алгоритмы: жадные алгоритмы, динамическое программирование, K-NN, поиск и сортировка — с большим количеством иллюстраций.',
   'https://covers.openlibrary.org/b/isbn/9781617292231-L.jpg',
   'https://covers.openlibrary.org/b/isbn/9781617292231-L.jpg',
   288, 'Питер', 2017, '978-5-496-02541-6', 'ACTIVE'),

  ('22222222-2222-2222-2222-222222222205',
   'PostgreSQL. Основы языка SQL', 'Евгений Моргунов', 'Базы данных', 2018,
   'Учебное пособие по SQL и PostgreSQL.',
   'Учебник по PostgreSQL: установка, базовый и продвинутый SQL, транзакции, индексы и оптимизация.',
   'https://covers.openlibrary.org/b/isbn/9785970606940-L.jpg',
   'https://covers.openlibrary.org/b/isbn/9785970606940-L.jpg',
   336, 'БХВ-Петербург', 2018, '978-5-9706-0694-0', 'ACTIVE'),

  ('22222222-2222-2222-2222-222222222206',
   'Изучаем SQL', 'Алан Бьюли', 'Базы данных', 2009,
   'Введение в SQL для разработчиков.',
   'Книга охватывает базовые и продвинутые темы SQL — выборки, объединения, подзапросы, окна, транзакции — на примере MySQL и SQLite.',
   'https://covers.openlibrary.org/b/isbn/9780596520830-L.jpg',
   'https://covers.openlibrary.org/b/isbn/9780596520830-L.jpg',
   336, 'Символ-Плюс', 2018, '978-5-93286-204-0', 'ACTIVE'),

  ('22222222-2222-2222-2222-222222222207',
   'Глубокое обучение', 'Ян Гудфеллоу, Иошуа Бенджио, Аарон Курвилль', 'ИИ', 2016,
   'Фундаментальный учебник по глубокому обучению.',
   'Учебник по глубокому обучению: от линейной алгебры и теории вероятностей до сверточных и рекуррентных сетей.',
   'https://covers.openlibrary.org/b/isbn/9780262035613-L.jpg',
   'https://covers.openlibrary.org/b/isbn/9780262035613-L.jpg',
   800, 'ДМК Пресс', 2018, '978-5-97060-618-6', 'ACTIVE'),

  ('22222222-2222-2222-2222-222222222208',
   'Hands-On Machine Learning', 'Аурелиен Жерон', 'ИИ', 2019,
   'Практическое машинное обучение с Scikit-Learn, Keras и TensorFlow.',
   'Практическое руководство по машинному обучению: классические алгоритмы, нейросети и глубокое обучение на Python.',
   'https://covers.openlibrary.org/b/isbn/9781492032649-L.jpg',
   'https://covers.openlibrary.org/b/isbn/9781492032649-L.jpg',
   856, 'O''Reilly', 2019, '978-1-4920-3264-9', 'ACTIVE'),

  ('22222222-2222-2222-2222-222222222209',
   'Прикладная криптография', 'Брюс Шнайер', 'Безопасность', 1996,
   'Протоколы, алгоритмы и исходные коды на C.',
   'Классика прикладной криптографии: шифры, протоколы, ключи, цифровые подписи и безопасные коммуникации.',
   'https://covers.openlibrary.org/b/isbn/9780471117094-L.jpg',
   'https://covers.openlibrary.org/b/isbn/9780471117094-L.jpg',
   816, 'Триумф', 2003, '978-5-89392-055-0', 'ACTIVE'),

  ('22222222-2222-2222-2222-222222222210',
   'Атака на сеть', 'Джеймс Форшоу', 'Безопасность', 2018,
   'Практическое руководство по сетевой безопасности.',
   'Книга для аудиторов и пентестеров: разбор типовых уязвимостей сетевых протоколов и способов их эксплуатации.',
   'https://covers.openlibrary.org/b/isbn/9781593277505-L.jpg',
   'https://covers.openlibrary.org/b/isbn/9781593277505-L.jpg',
   336, 'ДМК Пресс', 2019, '978-5-97060-689-6', 'ACTIVE'),

  ('22222222-2222-2222-2222-222222222211',
   'Современные операционные системы', 'Эндрю Таненбаум', 'Операционные системы', 2014,
   'Учебник по операционным системам.',
   'Подробный обзор современных ОС: процессы, потоки, память, файловые системы, безопасность и виртуализация.',
   'https://covers.openlibrary.org/b/isbn/9780133591620-L.jpg',
   'https://covers.openlibrary.org/b/isbn/9780133591620-L.jpg',
   1120, 'Питер', 2015, '978-5-496-00301-8', 'ACTIVE'),

  ('22222222-2222-2222-2222-222222222212',
   'Компьютерные сети', 'Эндрю Таненбаум, Дэвид Уэзеролл', 'Операционные системы', 2010,
   'Подробное описание сетевых технологий и протоколов.',
   'Классический учебник по сетям: от физического уровня до прикладных протоколов, разбор реальных технологий.',
   'https://covers.openlibrary.org/b/isbn/9780132126953-L.jpg',
   'https://covers.openlibrary.org/b/isbn/9780132126953-L.jpg',
   960, 'Питер', 2017, '978-5-496-02967-4', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- User reviews (mix of statuses; references books by ISBN)
-- ---------------------------------------------------------------------
INSERT INTO reviews (id, book_id, user_id, rating, text, status, created_at)
SELECT '33333333-3333-3333-3333-333333333301',
       (SELECT id FROM books WHERE isbn = '978-5-17-038942-4'),
       '11111111-1111-1111-1111-111111111101',
       5,
       'Перечитываю «Мастера и Маргариту» уже в третий раз и каждый раз нахожу что-то новое. Образ Воланда, философские разговоры о добре и зле, любовная линия Мастера и Маргариты — всё это удивительно органично сплетено в один роман. Особое впечатление производит линия с Понтием Пилатом и Иешуа Га-Ноцри. Булгаков сумел создать произведение, которое одинаково сильно работает и как сатира на советское общество, и как глубокая философская притча. Однозначно рекомендую к прочтению каждому, кто ценит большую литературу.',
       'APPROVED', NOW() - INTERVAL '20 days'
WHERE NOT EXISTS (SELECT 1 FROM reviews WHERE id = '33333333-3333-3333-3333-333333333301');

INSERT INTO reviews (id, book_id, user_id, rating, text, status, created_at)
SELECT '33333333-3333-3333-3333-333333333302',
       (SELECT id FROM books WHERE isbn = '978-5-17-038942-4'),
       '11111111-1111-1111-1111-111111111102',
       4,
       'Сложная, многослойная книга. С первого раза точно не разберёшься во всех аллюзиях и подтекстах, но сюжет затягивает с первой главы. Пилат и Иешуа — один из самых пронзительных эпизодов русской литературы вообще. Снизила оценку до четырёх, потому что сатирическая часть про московских литераторов местами кажется чрезмерно длинной.',
       'APPROVED', NOW() - INTERVAL '15 days'
WHERE NOT EXISTS (SELECT 1 FROM reviews WHERE id = '33333333-3333-3333-3333-333333333302');

INSERT INTO reviews (id, book_id, user_id, rating, text, status, created_at)
SELECT '33333333-3333-3333-3333-333333333303',
       (SELECT id FROM books WHERE isbn = '978-5-17-136354-7'),
       '11111111-1111-1111-1111-111111111103',
       5,
       'Главная антиутопия XX века. Оруэлл удивительно точно показал механизмы тоталитарного контроля: новояз, переписывание истории, двоемыслие, культ личности. Читать страшно — потому что многое из описанного встречается и сегодня. Финал безнадёжен, но именно эта безнадёжность делает книгу честной. Считаю обязательной к прочтению для всех, кто хочет понимать, как устроены современные общественные манипуляции.',
       'APPROVED', NOW() - INTERVAL '10 days'
WHERE NOT EXISTS (SELECT 1 FROM reviews WHERE id = '33333333-3333-3333-3333-333333333303');

INSERT INTO reviews (id, book_id, user_id, rating, text, status, created_at)
SELECT '33333333-3333-3333-3333-333333333304',
       (SELECT id FROM books WHERE isbn = '978-5-17-116055-0'),
       '11111111-1111-1111-1111-111111111104',
       5,
       'Ремарк — мастер показывать, как маленький человек выживает в большой истории. «Три товарища» — это книга про дружбу, любовь и про умение оставаться собой, когда вокруг разруха. Диалоги Роберта и Пэт можно растаскивать на цитаты. Финал разрывает сердце, но книгу обязательно надо прочитать каждому хотя бы раз в жизни — желательно лет в двадцать, чтобы потом перечитывать.',
       'APPROVED', NOW() - INTERVAL '7 days'
WHERE NOT EXISTS (SELECT 1 FROM reviews WHERE id = '33333333-3333-3333-3333-333333333304');

INSERT INTO reviews (id, book_id, user_id, rating, text, status, created_at)
SELECT '33333333-3333-3333-3333-333333333305',
       (SELECT id FROM books WHERE isbn = '978-5-04-161472-9'),
       '11111111-1111-1111-1111-111111111104',
       5,
       'Книга, которую можно читать в любом возрасте — каждый раз она открывается с новой стороны. В двадцать лет это был для меня сборник красивых афоризмов про дружбу и ответственность; через пять лет — целая философия о том, как взрослые теряют способность видеть главное. Маленький, но очень глубокий текст, который должен быть на полке у каждого. Иллюстрации Сент-Экзюпери — отдельная радость.',
       'APPROVED', NOW() - INTERVAL '5 days'
WHERE NOT EXISTS (SELECT 1 FROM reviews WHERE id = '33333333-3333-3333-3333-333333333305');

-- Pending review (waiting for moderation)
INSERT INTO reviews (id, book_id, user_id, rating, text, status, created_at)
SELECT '33333333-3333-3333-3333-333333333306',
       (SELECT id FROM books WHERE isbn = '978-5-389-07305-7'),
       '11111111-1111-1111-1111-111111111105',
       4,
       'Толстой и его «Война и мир» — это огромное полотно русской жизни начала XIX века. Читал в школе по диагонали, но сейчас, в более взрослом возрасте, оценил совершенно иначе. Особенно интересны философские отступления автора и линия Андрея Болконского. Сцены сражений описаны максимально реалистично. Минус один балл за местами затянутые сцены салонов и невыносимо длинные французские диалоги без перевода — приходится постоянно лазить в сноски.',
       'PENDING', NOW() - INTERVAL '1 day'
WHERE NOT EXISTS (SELECT 1 FROM reviews WHERE id = '33333333-3333-3333-3333-333333333306');

-- Rejected review (too short / inappropriate)
INSERT INTO reviews (id, book_id, user_id, rating, text, status, created_at)
SELECT '33333333-3333-3333-3333-333333333307',
       (SELECT id FROM books WHERE isbn = '978-5-04-091951-6'),
       '11111111-1111-1111-1111-111111111101',
       3,
       'Достоевский остаётся Достоевским — психологически тяжёлый роман, который в школе понять почти невозможно. Раскольников — не злодей, а человек, который попал в сети собственной идеи. Особенно сильны диалоги с Порфирием и Соней. Минус один балл за общую гнетущую атмосферу — после нескольких глав начинает физически тяжело. Минус ещё один — за повторы одних и тех же мыслей героев в разных вариациях. Но в целом — must read для понимания русской литературы.',
       'REJECTED', NOW() - INTERVAL '3 days'
WHERE NOT EXISTS (SELECT 1 FROM reviews WHERE id = '33333333-3333-3333-3333-333333333307');

-- ---------------------------------------------------------------------
-- Comments on reviews
-- ---------------------------------------------------------------------
INSERT INTO comments (id, user_id, review_id, text, created_at)
VALUES
  ('44444444-4444-4444-4444-444444444401',
   '11111111-1111-1111-1111-111111111102',
   '33333333-3333-3333-3333-333333333301',
   'Полностью согласна! Линия с Пилатом — гениальная находка Булгакова.',
   NOW() - INTERVAL '18 days'),
  ('44444444-4444-4444-4444-444444444402',
   '11111111-1111-1111-1111-111111111103',
   '33333333-3333-3333-3333-333333333301',
   'Перечитываю каждые два года, и каждый раз книга играет по-новому.',
   NOW() - INTERVAL '17 days'),
  ('44444444-4444-4444-4444-444444444403',
   '11111111-1111-1111-1111-111111111101',
   '33333333-3333-3333-3333-333333333303',
   'Оруэлл — пророк своего времени, к сожалению.',
   NOW() - INTERVAL '8 days'),
  ('44444444-4444-4444-4444-444444444404',
   '11111111-1111-1111-1111-111111111104',
   '33333333-3333-3333-3333-333333333304',
   'Согласна про возраст — я тоже всем советую читать ремарка лет в двадцать.',
   NOW() - INTERVAL '6 days')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- Likes on reviews and comments (mixed authorship to make stats live)
-- ---------------------------------------------------------------------
INSERT INTO likes (user_id, target_type, target_id) VALUES
  ('11111111-1111-1111-1111-111111111102', 'REVIEW', '33333333-3333-3333-3333-333333333301'),
  ('11111111-1111-1111-1111-111111111103', 'REVIEW', '33333333-3333-3333-3333-333333333301'),
  ('11111111-1111-1111-1111-111111111104', 'REVIEW', '33333333-3333-3333-3333-333333333301'),
  ('11111111-1111-1111-1111-111111111105', 'REVIEW', '33333333-3333-3333-3333-333333333301'),
  ('11111111-1111-1111-1111-111111111101', 'REVIEW', '33333333-3333-3333-3333-333333333303'),
  ('11111111-1111-1111-1111-111111111102', 'REVIEW', '33333333-3333-3333-3333-333333333303'),
  ('11111111-1111-1111-1111-111111111104', 'REVIEW', '33333333-3333-3333-3333-333333333303'),
  ('11111111-1111-1111-1111-111111111101', 'REVIEW', '33333333-3333-3333-3333-333333333304'),
  ('11111111-1111-1111-1111-111111111103', 'REVIEW', '33333333-3333-3333-3333-333333333304'),
  ('11111111-1111-1111-1111-111111111101', 'REVIEW', '33333333-3333-3333-3333-333333333305'),
  ('11111111-1111-1111-1111-111111111103', 'REVIEW', '33333333-3333-3333-3333-333333333305'),
  ('11111111-1111-1111-1111-111111111105', 'REVIEW', '33333333-3333-3333-3333-333333333305'),
  ('11111111-1111-1111-1111-111111111101', 'COMMENT', '44444444-4444-4444-4444-444444444401'),
  ('11111111-1111-1111-1111-111111111103', 'COMMENT', '44444444-4444-4444-4444-444444444401'),
  ('11111111-1111-1111-1111-111111111101', 'COMMENT', '44444444-4444-4444-4444-444444444403'),
  ('11111111-1111-1111-1111-111111111104', 'COMMENT', '44444444-4444-4444-4444-444444444404')
ON CONFLICT (user_id, target_type, target_id) DO NOTHING;

-- ---------------------------------------------------------------------
-- User collections (different statuses)
-- ---------------------------------------------------------------------
-- Approved
INSERT INTO collections (id, user_id, title, genre, description, status, created_at)
VALUES
  ('55555555-5555-5555-5555-555555555501',
   '11111111-1111-1111-1111-111111111101',
   'Русская классика на все времена',
   'Классика',
   'Подборка великих русских романов, без которых невозможно представить мировую литературу.',
   'APPROVED',
   NOW() - INTERVAL '12 days'),
  ('55555555-5555-5555-5555-555555555502',
   '11111111-1111-1111-1111-111111111102',
   'Книги, которые заставляют задуматься',
   'Философия',
   'Произведения, после которых хочется отложить телефон и подумать о жизни.',
   'APPROVED',
   NOW() - INTERVAL '9 days'),
  -- Pending (awaiting moderation)
  ('55555555-5555-5555-5555-555555555503',
   '11111111-1111-1111-1111-111111111103',
   'Антиутопии XX века',
   'Антиутопия',
   'Лучшие предостережения литературы XX века о тоталитаризме и потере свободы.',
   'PENDING',
   NOW() - INTERVAL '2 days'),
  -- Draft (only the author sees it)
  ('55555555-5555-5555-5555-555555555504',
   '11111111-1111-1111-1111-111111111104',
   'Мой летний список чтения',
   'Разное',
   'Книги, которые я планирую прочитать этим летом.',
   'DRAFT',
   NOW() - INTERVAL '1 days'),
  -- Rejected
  ('55555555-5555-5555-5555-555555555505',
   '11111111-1111-1111-1111-111111111105',
   'Случайная подборка',
   NULL,
   'Просто несколько книг, которые я люблю.',
   'REJECTED',
   NOW() - INTERVAL '4 days')
ON CONFLICT (id) DO NOTHING;

UPDATE collections
   SET moderator_comment = 'Отсутствует общая идея — пожалуйста, опишите, что объединяет эти книги, и переотправьте.'
 WHERE id = '55555555-5555-5555-5555-555555555505';

-- Books in collections
INSERT INTO collection_books (collection_id, book_id, position)
SELECT '55555555-5555-5555-5555-555555555501', id, position FROM (
  SELECT (SELECT id FROM books WHERE isbn = '978-5-17-038942-4') AS id, 0 AS position
  UNION ALL SELECT (SELECT id FROM books WHERE isbn = '978-5-389-07305-7'), 1
  UNION ALL SELECT (SELECT id FROM books WHERE isbn = '978-5-04-091951-6'), 2
  UNION ALL SELECT (SELECT id FROM books WHERE isbn = '978-5-17-096311-4'), 3
  UNION ALL SELECT (SELECT id FROM books WHERE isbn = '978-5-389-16298-0'), 4
) t
ON CONFLICT DO NOTHING;

INSERT INTO collection_books (collection_id, book_id, position)
SELECT '55555555-5555-5555-5555-555555555502', id, position FROM (
  SELECT (SELECT id FROM books WHERE isbn = '978-5-17-038942-4') AS id, 0 AS position
  UNION ALL SELECT (SELECT id FROM books WHERE isbn = '978-5-04-161472-9'), 1
  UNION ALL SELECT (SELECT id FROM books WHERE isbn = '978-5-04-089624-8'), 2
) t
ON CONFLICT DO NOTHING;

INSERT INTO collection_books (collection_id, book_id, position)
SELECT '55555555-5555-5555-5555-555555555503', id, position FROM (
  SELECT (SELECT id FROM books WHERE isbn = '978-5-17-136354-7') AS id, 0 AS position
) t
ON CONFLICT DO NOTHING;

INSERT INTO collection_books (collection_id, book_id, position)
SELECT '55555555-5555-5555-5555-555555555504', id, position FROM (
  SELECT (SELECT id FROM books WHERE isbn = '978-5-17-116055-0') AS id, 0 AS position
  UNION ALL SELECT (SELECT id FROM books WHERE isbn = '978-5-04-161472-9'), 1
) t
ON CONFLICT DO NOTHING;

INSERT INTO collection_books (collection_id, book_id, position)
SELECT '55555555-5555-5555-5555-555555555505', id, position FROM (
  SELECT (SELECT id FROM books WHERE isbn = '978-5-389-16298-0') AS id, 0 AS position
) t
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- Subject collections (used by the «Subjects» tab)
-- ---------------------------------------------------------------------
INSERT INTO subject_collections (id, user_id, subject, specialty, specialty_name,
                                 title, description, author_role, status, created_at)
VALUES
  ('66666666-6666-6666-6666-666666666601',
   '11111111-1111-1111-1111-111111111201',
   'Программирование', 'ПОИТ', 'Программное обеспечение информационных технологий',
   'Базовая литература для будущих разработчиков',
   'Подборка для студентов 1–2 курсов: книги, формирующие культуру кода и инженерное мышление.',
   'TEACHER', 'APPROVED', NOW() - INTERVAL '14 days'),
  ('66666666-6666-6666-6666-666666666602',
   '11111111-1111-1111-1111-111111111201',
   'Базы данных', 'ПОИТ', 'Программное обеспечение информационных технологий',
   'Введение в SQL и PostgreSQL',
   'Минимально необходимая подборка для уверенного понимания реляционных СУБД и языка SQL.',
   'TEACHER', 'APPROVED', NOW() - INTERVAL '13 days'),
  ('66666666-6666-6666-6666-666666666603',
   '11111111-1111-1111-1111-111111111101',
   'Алгоритмы и структуры данных', 'ПОИТ', 'Программное обеспечение информационных технологий',
   'Алгоритмы для подготовки к собеседованиям',
   'Книги, по которым студенты ПОИТ обычно готовятся к секциям по алгоритмам на собеседованиях.',
   'STUDENT', 'PENDING', NOW() - INTERVAL '2 days'),
  ('66666666-6666-6666-6666-666666666604',
   '11111111-1111-1111-1111-111111111102',
   'Машинное обучение', 'ИИ', 'Искусственный интеллект',
   'Стартовая подборка по ML',
   'Литература, с которой стоит начинать знакомство с машинным обучением — теория плюс практика.',
   'STUDENT', 'APPROVED', NOW() - INTERVAL '8 days'),
  ('66666666-6666-6666-6666-666666666605',
   '11111111-1111-1111-1111-111111111103',
   'Криптография', 'ЗИ', 'Защита информации',
   'Криптография — must read',
   'Подборка по криптографии: классика плюс прикладные книги, чтобы понять реальную безопасность.',
   'STUDENT', 'APPROVED', NOW() - INTERVAL '6 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO subject_collection_books (subject_collection_id, book_id, position)
SELECT '66666666-6666-6666-6666-666666666601', id, position FROM (
  SELECT (SELECT id FROM books WHERE isbn = '978-5-7502-0064-1') AS id, 0 AS position
  UNION ALL SELECT (SELECT id FROM books WHERE isbn = '978-5-4461-0960-1'), 1
) t
ON CONFLICT DO NOTHING;

INSERT INTO subject_collection_books (subject_collection_id, book_id, position)
SELECT '66666666-6666-6666-6666-666666666602', id, position FROM (
  SELECT (SELECT id FROM books WHERE isbn = '978-5-9706-0694-0') AS id, 0 AS position
  UNION ALL SELECT (SELECT id FROM books WHERE isbn = '978-5-93286-204-0'), 1
) t
ON CONFLICT DO NOTHING;

INSERT INTO subject_collection_books (subject_collection_id, book_id, position)
SELECT '66666666-6666-6666-6666-666666666603', id, position FROM (
  SELECT (SELECT id FROM books WHERE isbn = '978-5-8459-1794-2') AS id, 0 AS position
  UNION ALL SELECT (SELECT id FROM books WHERE isbn = '978-5-496-02541-6'), 1
) t
ON CONFLICT DO NOTHING;

INSERT INTO subject_collection_books (subject_collection_id, book_id, position)
SELECT '66666666-6666-6666-6666-666666666604', id, position FROM (
  SELECT (SELECT id FROM books WHERE isbn = '978-5-97060-618-6') AS id, 0 AS position
  UNION ALL SELECT (SELECT id FROM books WHERE isbn = '978-1-4920-3264-9'), 1
) t
ON CONFLICT DO NOTHING;

INSERT INTO subject_collection_books (subject_collection_id, book_id, position)
SELECT '66666666-6666-6666-6666-666666666605', id, position FROM (
  SELECT (SELECT id FROM books WHERE isbn = '978-5-89392-055-0') AS id, 0 AS position
  UNION ALL SELECT (SELECT id FROM books WHERE isbn = '978-5-97060-689-6'), 1
) t
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- Events (book club) with participants
-- ---------------------------------------------------------------------
INSERT INTO events (id, title, description, date, time, location, max_participants, organizer, created_by)
VALUES
  ('77777777-7777-7777-7777-777777777701',
   'Обсуждение «1984»',
   'Открытое обсуждение романа Оруэлла «1984» — приглашаем всех, кто прочитал книгу. Поговорим о новоязе, двоемыслии и о том, почему антиутопия всё актуальнее.',
   CURRENT_DATE + INTERVAL '7 days', '18:00',
   'БГУИР, корпус 1, ауд. 320', 25, 'Книжный клуб БГУИР',
   (SELECT id FROM users WHERE login = 'admin')),
  ('77777777-7777-7777-7777-777777777702',
   'Лекция: «Чистый код на практике»',
   'Открытая лекция доцента Смирнова О.Н. для студентов всех специальностей о принципах чистого кода и их применении в реальных проектах.',
   CURRENT_DATE + INTERVAL '14 days', '17:30',
   'БГУИР, корпус 4, ауд. 401', 60, 'Кафедра ПОИТ',
   (SELECT id FROM users WHERE login = 'admin')),
  ('77777777-7777-7777-7777-777777777703',
   'Литературный вечер: «Маленький принц»',
   'Уютный вечер с обсуждением книги Сент-Экзюпери. Чай, печенье и хорошая компания.',
   CURRENT_DATE + INTERVAL '21 days', '19:00',
   'Студенческий клуб', 15, 'Книжный клуб БГУИР',
   (SELECT id FROM users WHERE login = 'admin')),
  ('77777777-7777-7777-7777-777777777704',
   'Подготовка к олимпиаде по программированию',
   'Серия встреч по разбору алгоритмических задач: от базовых до олимпиадных. Приходите со своими задачами.',
   CURRENT_DATE + INTERVAL '3 days', '16:00',
   'БГУИР, корпус 1, ауд. 215', 30, 'Студенческий совет ФКП',
   (SELECT id FROM users WHERE login = 'admin'))
ON CONFLICT (id) DO NOTHING;

INSERT INTO event_participants (event_id, user_id) VALUES
  ('77777777-7777-7777-7777-777777777701', '11111111-1111-1111-1111-111111111101'),
  ('77777777-7777-7777-7777-777777777701', '11111111-1111-1111-1111-111111111102'),
  ('77777777-7777-7777-7777-777777777701', '11111111-1111-1111-1111-111111111103'),
  ('77777777-7777-7777-7777-777777777702', '11111111-1111-1111-1111-111111111101'),
  ('77777777-7777-7777-7777-777777777702', '11111111-1111-1111-1111-111111111105'),
  ('77777777-7777-7777-7777-777777777704', '11111111-1111-1111-1111-111111111101'),
  ('77777777-7777-7777-7777-777777777704', '11111111-1111-1111-1111-111111111105')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- Orders (some users have queued books)
-- ---------------------------------------------------------------------
INSERT INTO orders (user_id, book_id, status)
SELECT '11111111-1111-1111-1111-111111111101', id, 'ACTIVE' FROM books WHERE isbn = '978-5-17-038942-4'
ON CONFLICT DO NOTHING;
INSERT INTO orders (user_id, book_id, status)
SELECT '11111111-1111-1111-1111-111111111101', id, 'ACTIVE' FROM books WHERE isbn = '978-5-04-161472-9'
ON CONFLICT DO NOTHING;
INSERT INTO orders (user_id, book_id, status)
SELECT '11111111-1111-1111-1111-111111111102', id, 'ACTIVE' FROM books WHERE isbn = '978-5-17-136354-7'
ON CONFLICT DO NOTHING;
INSERT INTO orders (user_id, book_id, status)
SELECT '11111111-1111-1111-1111-111111111104', id, 'ACTIVE' FROM books WHERE isbn = '978-5-04-161472-9'
ON CONFLICT DO NOTHING;
