import { ref, computed } from 'vue';
import { api } from '../api.js';

// One shared snapshot — every consumer reads from the same reactive ref so
// re-fetching after an admin edit propagates everywhere automatically.
const faculties = ref([]);
const loading = ref(false);
const loaded = ref(false);

async function load(force = false) {
  if (loading.value) return;
  if (loaded.value && !force) return;
  loading.value = true;
  try {
    const data = await api.get('/faculties');
    faculties.value = Array.isArray(data) ? data : [];
    loaded.value = true;
  } catch (e) {
    console.error('Не удалось загрузить факультеты:', e);
  } finally {
    loading.value = false;
  }
}

/**
 * Reactive access to the faculty/specialty tree. The first caller triggers a
 * single network round-trip; subsequent callers share the cached state.
 */
export function useFaculties() {
  if (!loaded.value && !loading.value) load();

  // [{ value, label }] of faculties for <Select>.
  const facultyOptions = computed(() =>
    faculties.value.map(f => ({ value: f.name, label: f.name, id: f.id })),
  );

  // Map faculty name → [{ value, label }] of specialties (for the existing select component).
  const specialtiesByFaculty = computed(() => {
    const map = {};
    for (const f of faculties.value) {
      map[f.name] = (f.specialties || []).map(s => ({ value: s.name, label: s.name, id: s.id }));
    }
    return map;
  });

  // Flat list of every (facultyName, specialty) pair — used for admin pickers.
  const allSpecialties = computed(() => {
    const out = [];
    for (const f of faculties.value) {
      for (const s of f.specialties || []) {
        out.push({
          id: s.id,
          name: s.name,
          facultyId: f.id,
          facultyName: f.name,
          label: `${f.name} / ${s.name}`,
        });
      }
    }
    return out;
  });

  return {
    faculties,
    loading,
    loaded,
    refresh: () => load(true),
    facultyOptions,
    specialtiesByFaculty,
    allSpecialties,
  };
}
