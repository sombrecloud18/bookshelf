import { ref } from 'vue';

// Глобальная очередь подтверждений (один диалог одновременно). Используется как
// замена window.confirm — возвращает Promise<boolean>, который резолвится после
// клика «Подтвердить» / «Отмена» либо закрытия модалки крестиком.
const state = ref({
  open: false,
  title: 'Подтверждение',
  message: '',
  confirmLabel: 'Подтвердить',
  cancelLabel: 'Отмена',
  variant: 'danger', // 'danger' | 'primary' — определяет цвет кнопки подтверждения
  resolve: null,
});

export function useConfirmState() {
  return state;
}

/**
 * Открывает модальное окно подтверждения и возвращает Promise<boolean>.
 *
 *   const ok = await useConfirm()('Удалить эту подборку?');
 *   if (!ok) return;
 */
export function useConfirm() {
  return (message, options = {}) => {
    return new Promise(resolve => {
      // Если предыдущий диалог не успел закрыться — резолвим его как «отказ».
      if (state.value.resolve) state.value.resolve(false);
      state.value = {
        open: true,
        title: options.title || 'Подтверждение',
        message,
        confirmLabel: options.confirmLabel || 'Подтвердить',
        cancelLabel: options.cancelLabel || 'Отмена',
        variant: options.variant || 'danger',
        resolve,
      };
    });
  };
}

export function resolveConfirm(value) {
  const resolve = state.value.resolve;
  state.value.open = false;
  state.value.resolve = null;
  if (resolve) resolve(value);
}
