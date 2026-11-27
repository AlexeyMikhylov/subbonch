// Сворачивание / разворачивание списка "Мои клубы"
let isClubsExpanded = false;

function toggleClubs(event) {
  if (event) event.preventDefault();
  const clubsList = document.getElementById('clubsList');
  const showMoreBtn = document.getElementById('showMoreBtn');
  if (!clubsList || !showMoreBtn) return;

  isClubsExpanded = !isClubsExpanded;

  clubsList.querySelectorAll('li').forEach((li, index) => {
    if (index >= 5) {
      li.style.display = isClubsExpanded ? 'flex' : 'none';
    }
  });

  showMoreBtn.textContent = isClubsExpanded ? 'Свернуть ↑' : 'Показать ещё →';
}

//  Модальные окна
function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (!modal) return;
  modal.style.display = 'flex';
  modal.classList.add('active');
  document.body.style.overflow = 'hidden';
}

function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (!modal) return;
  modal.classList.remove('active');
  modal.style.display = 'none';
  document.body.style.overflow = '';
}

const openCreateClubModal = () => openModal('createClubModal');
const closeCreateClubModal = () => closeModal('createClubModal');
const openCreatePostModal = () => openModal('createPostModal');
const closeCreatePostModal = () => closeModal('createPostModal');

// Выпадающие меню (профиль, клуб, посты)
function toggleDropdown() {
  document.getElementById('profileDropdown')?.classList.toggle('active');
}

function toggleClubMenu() {
  document.getElementById('clubDropdown')?.classList.toggle('active');
}

function togglePostMenu(event, btn) {
  event.stopPropagation();
  const dropdown = btn.closest('.post-menu-wrapper')?.querySelector('.post-dropdown');
  if (!dropdown) return;

  document.querySelectorAll('.post-dropdown.active').forEach((el) => {
    if (el !== dropdown) el.classList.remove('active');
  });

  dropdown.classList.toggle('active');
}

// Показ / скрытие пароля на страницах входа и регистрации
function togglePasswordField(inputId, iconId) {
  const input = document.getElementById(inputId);
  const icon = document.getElementById(iconId);
  if (!input || !icon) return;

  const isHidden = input.type === 'password';
  input.type = isHidden ? 'text' : 'password';
  icon.innerHTML = isHidden
    ? '<path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/>'
    : '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>';
}

// Показ / скрытие формы ответа на комментарий
function toggleReplyForm(formId) {
  const form = document.getElementById(formId);
  if (!form) return;
  form.style.display = form.style.display === 'none' ? 'flex' : 'none';
}

// Интерактивный выбор и отмена варианта в опросе (демо для статики)
function selectPollOption(event, clickedBtn) {
  event.preventDefault();
  const form = clickedBtn.closest('.post-poll');
  if (!form) return;

  const buttons = Array.from(form.querySelectorAll('.poll-option-btn'));
  const prevSelected = form.querySelector('.poll-option-btn.selected');
  const isUnvoting = prevSelected === clickedBtn;

  if (prevSelected) {
    prevSelected.classList.remove('selected');
    const prevVotesEl = prevSelected.querySelector('.poll-votes');
    prevVotesEl.dataset.count = Math.max(0, parseInt(prevVotesEl.dataset.count, 10) - 1);
  }

  if (!isUnvoting) {
    clickedBtn.classList.add('selected');
    const newVotesEl = clickedBtn.querySelector('.poll-votes');
    newVotesEl.dataset.count = parseInt(newVotesEl.dataset.count, 10) + 1;
  }

  const total = buttons.reduce(
    (sum, btn) => sum + parseInt(btn.querySelector('.poll-votes').dataset.count, 10),
    0
  );

  buttons.forEach((btn) => {
    const votesEl = btn.querySelector('.poll-votes');
    const count = parseInt(votesEl.dataset.count, 10);
    const percent = total > 0 ? Math.round((count / total) * 100) : 0;
    btn.querySelector('.poll-bar').style.width = `${percent}%`;
    votesEl.textContent = formatVotes(count);
  });

  const totalEl = form.querySelector('#poll-total-count');
  if (totalEl) totalEl.textContent = total;
}

function formatVotes(n) {
  const mod10 = n % 10;
  const mod100 = n % 100;
  if (mod10 === 1 && mod100 !== 11) return `${n} голос`;
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) return `${n} голоса`;
  return `${n} голосов`;
}

// Живое превью аватара и буквы-заглушки
function previewAvatarUrl(url) {
  const img = document.getElementById('avatarPreviewImg');
  const fallback = document.getElementById('avatarPreviewFallback');
  if (!img || !fallback) return;

  const trimmed = url.trim();
  if (!trimmed) {
    img.style.display = 'none';
    fallback.style.display = 'flex';
    return;
  }

  img.onload = () => {
    img.style.display = 'block';
    fallback.style.display = 'none';
  };
  img.onerror = () => {
    img.style.display = 'none';
    fallback.style.display = 'flex';
  };
  img.src = trimmed;
}

function previewAvatarLetter(text) {
  const fallback = document.getElementById('avatarPreviewFallback');
  if (!fallback) return;

  const firstChar = text.trim().charAt(0).toUpperCase();
  fallback.textContent = firstChar || '?';
}

// Переход на страницу поста при клике по карточке
function openPost(event, url) {
  if (event.target.closest('a, button, form, input, label')) return;
  if (window.getSelection().toString().length > 0) return;

  window.location.href = url;
}

// Глобальные обработчики событий
document.addEventListener('click', (event) => {
  const target = event.target;

  const profileDropdown = document.getElementById('profileDropdown');
  if (profileDropdown && !target.closest('.profile-menu-btn, #profileDropdown')) {
    profileDropdown.classList.remove('active');
  }

  const clubDropdown = document.getElementById('clubDropdown');
  if (clubDropdown && !target.closest('.club-menu-btn, #clubDropdown')) {
    clubDropdown.classList.remove('active');
  }

  if (!target.closest('.post-menu-wrapper')) {
    document.querySelectorAll('.post-dropdown.active').forEach((el) => {
      el.classList.remove('active');
    });
  }

  if (target.id === 'createClubModal') closeCreateClubModal();
  if (target.id === 'createPostModal') closeCreatePostModal();
});

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') {
    closeCreateClubModal();
    closeCreatePostModal();
  }
});

document.addEventListener('DOMContentLoaded', () => {
  // Скрытие клубов сверх первых 5 в боковом меню
  const clubsList = document.getElementById('clubsList');
  const showMoreBtn = document.getElementById('showMoreBtn');
  if (clubsList && showMoreBtn) {
    const allClubItems = clubsList.querySelectorAll('li');
    if (allClubItems.length > 5) {
      allClubItems.forEach((li, index) => {
        if (index >= 5) li.style.display = 'none';
      });
      showMoreBtn.style.display = 'block';
    }
  }

  // Сворачивание длинных постов в ленте
  document.querySelectorAll('.post-body.clamped').forEach((body) => {
    if (body.scrollHeight > body.clientHeight + 2) {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'btn-read-more';
      btn.textContent = 'Читать далее';
      btn.onclick = (e) => {
        e.stopPropagation();
        const isClamped = body.classList.toggle('clamped');
        btn.textContent = isClamped ? 'Читать далее' : 'Свернуть';
      };
      body.insertAdjacentElement('afterend', btn);
    }
  });

  // Автоматическое превью аватара на странице редактирования
  const urlInput = document.getElementById('profilePictureUrl') || document.getElementById('pictureUrl');
  if (urlInput && urlInput.value.trim()) {
    previewAvatarUrl(urlInput.value);
  }
});

// Копирование ссылки на пост по кнопке "Поделиться"
function copyPostLink(path, btn) {
  const fullUrl = window.location.origin + path;
  if (navigator.clipboard) {
    navigator.clipboard.writeText(fullUrl);
  }
  const label = btn.querySelector('span');
  if (!label) return;
  const originalText = label.textContent;
  label.textContent = 'Ссылка скопирована!';
  setTimeout(() => {
    label.textContent = originalText;
  }, 2000);
}
