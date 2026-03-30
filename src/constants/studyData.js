// Список факультетов
export const faculties = [
  { value: 'ФКП', label: 'ФКП' },
  { value: 'ФКСИС', label: 'ФКСИС' },
  { value: 'ФИБ', label: 'ФИБ' },
  { value: 'ФИТУ', label: 'ФИТУ' },
  { value: 'ФРЭ', label: 'ФРЭ' },
  { value: 'ВФ', label: 'ВФ' },
  { value: 'ИЭФ', label: 'ИЭФ' },
];

// Список курсов
export const courses = [
  { value: '1 курс', label: '1 курс' },
  { value: '2 курс', label: '2 курс' },
  { value: '3 курс', label: '3 курс' },
  { value: '4 курс', label: '4 курс' },
];

// Данные по специальностям в зависимости от факультета
export const specialtiesData = {
  ФКП: [
    { value: 'ПОИТ', label: 'ПОИТ' },
    { value: 'ВМСиС', label: 'ВМСиС' },
    { value: 'КС', label: 'КС' },
  ],
  ФКСИС: [
    { value: 'ИИ', label: 'ИИ' },
    { value: 'ПИ', label: 'ПИ' },
    { value: 'АСОИ', label: 'АСОИ' },
  ],
  ФИБ: [
    { value: 'ТК', label: 'ТК' },
    { value: 'ЗИ', label: 'ЗИ' },
    { value: 'МС', label: 'МС' },
  ],
  ФИТУ: [
    { value: 'ИСиТ', label: 'ИСиТ' },
    { value: 'УПИ', label: 'УПИ' },
    { value: 'БИ', label: 'БИ' },
  ],
  ФРЭ: [
    { value: 'РЭиС', label: 'РЭиС' },
    { value: 'МСиС', label: 'МСиС' },
    { value: 'ЭТ', label: 'ЭТ' },
  ],
  ВФ: [
    { value: 'ТТП', label: 'ТТП' },
    { value: 'ЭиУ', label: 'ЭиУ' },
    { value: 'МТ', label: 'МТ' },
  ],
  ИЭФ: [
    { value: 'МЭ', label: 'МЭ' },
    { value: 'ЭС', label: 'ЭС' },
    { value: 'УЭП', label: 'УЭП' },
  ],
};
