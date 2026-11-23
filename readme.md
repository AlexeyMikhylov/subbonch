# Фронтенд
    html страницы -> src/main/resources/templates/
    css -> src/main/resources/static/css/
    js -> src/main/resources/static/js/
    images -> src/main/resources/static/images/


# Словарь моделей

    *(в базе данных используется snake_case (например, first_name), а в Java-классах и HTML-шаблонах Thymeleaf строго используется camelCase (firstName).*
    *Маппинг настраивается через аннотацию @Column(name = "..."))*

    *В скобках указывается название таблицы в БД*

    **Модель user (Users)**
        • user.userId — ID пользователя (Integer)
        • user.firstName — имя
        • user.lastName — фамилия
        • user.email — почта (логин)
        • user.profilePictureUrl — ссылка на аватар
        • user.about — описание профиля
        • user.isAppAdmin — глобальный админ сайта (Boolean)
        • user.createdAt — дата регистрации

    **Модель club (Clubs)**
        • club.clubId — ID клуба
        • club.club — название клуба (в HTML выводится как ${club.club} или ${post.club.club})
        • club.about — описание клуба
        • club.pictureUrl — аватар/обложка клуба
        • club.createdAt — дата создания
        • club.members — список участников (List<ClubMember>)

    **Модель member (Club_members)**
        • member.id — ID записи
        • member.user — ссылка на объект User
        • member.club — ссылка на объект Club
        • member.userRole — роль в клубе ('MEMBER', 'MODERATOR', 'ADMIN')

    **Модель post (Post)**
        • post.postId — ID поста
        • post.title — заголовок поста
        • post.body — основной текст поста
        • post.createdAt — дата публикации
        • post.isEdited — флаг редактирования
        • post.club — объект Club (авторский клуб)
        • post.user — объект User (автор поста)
        • post.attachments — список вложений (List<AttachmentPost>)
        • post.comments — список комментариев (List<Comment>)
        • post.poll — прикрепленный опрос (объект Poll, может быть null)

    **Модель comment (Сomment)**
        • comment.commentId — ID комментария
        • comment.comment — текст комментария
        • comment.createdAt — дата создания
        • comment.isDeleted — флаг удаления
        • comment.user — объект User (автор)
        • comment.parent — родительский Comment (для ответов, для корневых — null)

    **Модели опросов (Poll и Vote)**
        • poll.pollId, poll.title, poll.isMultiple, poll.votes (список вариантов List<Vote>)
        • vote.voteId, vote.vote (текст варианта ответа), vote.total (количество голосов)

    **Модель application (Applications)**
        • application.applicationId, application.clubName, application.appDescription, application.reason, application.appStatus ('PENDING', 'APPROVED', 'REJECTED')

## Имена переменных в Thymeleaf

    *Для фронтендера: обращение через ${...}*

    **currentUser** — объект текущего авторизованного пользователя (передается на все страницы для отображения в шапке и левом меню).
    **myClubs** — список клубов, в которых состоит пользователь (для левого сайдбара «Мои клубы»).
    **loginError** — ошибка входа (показывается в layout).
    **blockReason** — причина блокировки пользователя при входе (показывается в layout).
    **popularClubs** — топ клубов по количеству участников (для правого блока на главной). # пока не делаем
    **postsList** — список постов для вывода в ленте или на странице клуба.
    **clubsPage** — страница каталога клубов (`PageResponse<ClubListItemResponse>`: `content`, `page`, `size`, `totalElements`, `totalPages`).
    **clubPage** — данные страницы клуба (`ClubPageResponse`: клуб, `membersCount`, `userRoleInClub`, `team`).
    **membersPage** — страница участников клуба (`PageResponse<ClubMemberListItemResponse>`).
    **clubForm** — форма редактирования клуба (`ClubEditRequest`: `club`, `about`, `pictureUrl`).
    **userRoleInClub** — роль текущего юзера в клубе ('NONE', 'MEMBER', 'MODERATOR', 'ADMIN'), в шаблоне берётся как `clubPage.userRoleInClub`.

### Карта url маршрутов

    GET /login	                            login.html	                Страница входа (Spring Security ожидает поля username и password).
    GET /register	                        register.html	            Страница регистрации.
    POST /register	                        redirect:/login?registered	Обработка регистрации. Поля формы: firstName, lastName, email, password, passwordConfirm.
    GET / или GET /feed	                    feed.html	                Главная страница с лентой. Поддерживает параметр ?tab=subscriptions для вкладки «Подписки».
    GET /clubs	                            catalog.html	            Каталог клубов. Параметры: `q`, `page`, `size`.
    GET /clubs/{id}	                        club.html	                Страница клуба: описание, команда, роль пользователя, кнопки вступления/выхода.
    POST /clubs/{id}/join	                redirect:/clubs/{id}	    Вступить в клуб (только авторизованный). Успех/ошибка через flash `successMessage/errorMessage`.
    POST /clubs/{id}/leave	                redirect:/clubs/{id}	    Покинуть клуб. `MODERATOR/ADMIN` не могут выйти до передачи прав (flash `errorMessage`).
    GET /clubs/{id}/members	                club-members.html	        Участники клуба. Только `ADMIN` клуба или `isAppAdmin`. Параметры: `page`, `size`.
    POST /clubs/{id}/members/{userId}/moderator/assign	redirect:/clubs/{id}/members	Назначить модератора (MEMBER -> MODERATOR).
    POST /clubs/{id}/members/{userId}/moderator/remove	redirect:/clubs/{id}/members	Снять модератора (MODERATOR -> MEMBER).
    GET /clubs/{id}/edit	                club-edit.html	            Форма редактирования клуба. Только `ADMIN` клуба или `isAppAdmin`.
    POST /clubs/{id}/edit	                redirect:/clubs/{id}	    Сохранить клуб. Поля: `club`, `about`, `pictureUrl`. Ошибка валидации — возврат `club-edit.html` с `errorMessage`.
    POST /clubs/{id}/posts/create	        redirect:/clubs/{id}	    Создание поста (для админа/модера). Поля: title, body, imageUrl (опционально).
    POST /posts/{id}/comments	            redirect:/clubs/{clubId}	Добавление комментария. Поля: comment, parentId (скрытое поле для ответа).
    POST /polls/{pollId}/vote	            redirect:/feed	            Голосование в опросе. Поле: voteId.
    
    GET /profile	                        profile.html	            Профиль текущего пользователя.
    GET /profile/edit	                    profile-edit.html	        Форма редактирования профиля.
    POST /profile/edit	                    redirect:/profile	        Редактирование профиля. Поля: firstName, lastName, about, profilePictureUrl.
    POST /applications/create	            redirect:/profile	        Отправка заявки на клуб. Поля: clubName, appDescription, reason.
    
    GET /admin	                            admin.html	                Панель глобального админа (список заявок applications со статусом PENDING).
    POST /admin/applications/{id}/approve	redirect:/admin	            Одобрение заявки и автоматическое создание клуба.
