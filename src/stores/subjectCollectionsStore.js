// stores/subjectCollectionsStore.js
import { ref } from 'vue';

export function useSubjectCollectionsStore() {
  const subjectCollections = ref([
    {
      id: 'sub-col-1',
      subject: 'ОПИ',
      specialty: '2-40 01 01',
      specialtyName: 'Программное обеспечение информационных технологий',
      title: 'Основы алгоритмизации',
      description: 'Книги для понимания основ алгоритмов и структур данных',
      bookIds: ['book-satan', 'book-ave-maria'],
      author: 'Иванов И.И.',
      authorId: 'teacher-1',
      authorRole: 'teacher',
      status: 'approved',
      createdAt: '2026-01-15',
      moderatorComment: null,
    },
    {
      id: 'sub-col-2',
      subject: 'БД',
      specialty: '2-40 01 01',
      specialtyName: 'Программное обеспечение информационных технологий',
      title: 'Базы данных для начинающих',
      description: 'Лучшие книги по проектированию БД',
      bookIds: ['book-hunger-games', 'book-kwebe'],
      author: 'Петров студент',
      authorId: 'student-1',
      authorRole: 'student',
      status: 'approved',
      createdAt: '2026-04-12',
      moderatorComment: null,
    },
  ]);

  const pendingCollections = ref([
    {
      id: 'sub-col-3',
      subject: 'Web',
      specialty: '2-40 01 02',
      specialtyName: 'Информационные системы и технологии',
      title: 'Современный фронтенд',
      description: 'Книги по React, Vue и современному JavaScript',
      bookIds: ['book-institute', 'book-crows'],
      author: 'Сидорова А.С.',
      authorId: 'student-2',
      authorRole: 'student',
      status: 'pending',
      createdAt: '2026-04-13',
      moderatorComment: null,
    },
  ]);

  function addSubjectCollection(collection) {
    const newCollection = {
      id: `sub-col-${Date.now()}`,
      ...collection,
      status: 'pending',
      createdAt: new Date().toISOString().split('T')[0],
      moderatorComment: null,
    };
    console.log('Добавлена новая подборка:', newCollection);
    pendingCollections.value.unshift(newCollection);
    return newCollection;
  }

  function approveCollection(id, moderatorComment = null) {
    const index = pendingCollections.value.findIndex(c => c.id === id);
    if (index !== -1) {
      const approved = {
        ...pendingCollections.value[index],
        status: 'approved',
        moderatorComment: moderatorComment,
      };
      subjectCollections.value.unshift(approved);
      pendingCollections.value.splice(index, 1);
      console.log('Подборка одобрена:', approved);
    }
  }

  function rejectCollection(id, reason) {
    const index = pendingCollections.value.findIndex(c => c.id === id);
    if (index !== -1) {
      console.log('Подборка отклонена:', pendingCollections.value[index].title, 'Причина:', reason);
      pendingCollections.value.splice(index, 1);
    }
  }

  function getCollectionsBySubject(subject, specialty = null) {
    let filtered = subjectCollections.value.filter(c => c.subject === subject && c.status === 'approved');
    if (specialty) {
      filtered = filtered.filter(c => c.specialty === specialty);
    }
    return filtered;
  }

  return {
    subjectCollections,
    pendingCollections,
    addSubjectCollection,
    approveCollection,
    rejectCollection,
    getCollectionsBySubject,
  };
}
