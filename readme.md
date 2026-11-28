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

    **Модель post в шаблонах (PostListItemResponse, `postsList` / страница поста)**
        • post.postId — ID поста
        • post.title — заголовок поста
        • post.body — основной текст поста
        • post.createdAt — дата публикации
        • post.edited — флаг редактирования
        • post.authorId — ID автора
        • post.authorFirstName, post.authorLastName — имя/фамилия автора
        • post.authorProfilePictureUrl — URL аватарки автора (может быть null/пустым)
        • post.clubId, post.clubName — ID и название клуба
        • post.clubPictureUrl — URL аватарки клуба (может быть null/пустым)
        • post.clubAbout — описание клуба (может быть null/пустым), удобно для сайдбара на странице поста
        • post.imageUrl — URL изображения поста (может быть null/пустым)
        • post.poll — опрос (PollResponse, может быть null)
        • post.comments — дерево комментариев верхнего уровня (List<CommentResponse>); ответы лежат в `comment.replies`
        • post.commentsCount — общее число комментариев поста, включая ответы любой глубины (рекурсивный подсчёт по дереву). Использовать его, а не `post.comments.size()`: `size()` считает только корневые комментарии

    **Модель comment в шаблонах (CommentResponse)**
        • comment.commentId — ID комментария
        • comment.authorId — ID автора
        • comment.authorFirstName, comment.authorLastName — имя/фамилия автора
        • comment.authorProfilePictureUrl — URL аватарки автора (может быть null/пустым)
        • comment.authorRoleInClub — роль автора в клубе (`MEMBER`/`MODERATOR`/`ADMIN`, может быть null)
        • comment.comment — текст комментария
        • comment.createdAt — дата создания
        • comment.parentId — ID родительского комментария (null для корневого)
        • comment.replies — список ответов (List<CommentResponse>)

    **Модели опросов (PollResponse / PollOptionResponse)**
        • poll.pollId — ID опроса
        • poll.title — вопрос
        • poll.isMultiple — множественный выбор
        • poll.closesAt — дата закрытия (может быть null)
        • poll.closed — закрыт ли опрос
        • poll.alreadyVoted — голосовал ли текущий пользователь
        • poll.options — варианты (List<PollOptionResponse>)
        • option.voteId — ID варианта
        • option.vote — текст варианта
        • option.total — количество голосов
        • option.selectedByCurrentUser — выбран ли вариант текущим пользователем

    **Модель каталога клуба (ClubListItemResponse, элемент `clubsPage.content`)**
        • item.clubId — ID клуба
        • item.club — название
        • item.about — описание (может быть null)
        • item.pictureUrl — аватар/обложка (может быть null)
        • item.membersCount — количество участников

    **Модель страницы клуба (ClubPageResponse)**
        • clubPage.clubId, clubPage.club, clubPage.about, clubPage.pictureUrl, clubPage.createdAt
        • clubPage.membersCount — количество участников
        • clubPage.userRoleInClub — роль текущего пользователя (`NONE`/`MEMBER`/`MODERATOR`/`ADMIN`)
        • clubPage.team — команда клуба (List<ClubTeamMemberResponse>: `userId`, `firstName`, `lastName`, `userRole`)

    **Модель профиля (ProfileResponse)**
        • profile.userId, profile.firstName, profile.lastName, profile.email
        • profile.about, profile.profilePictureUrl, profile.createdAt
        • profile.clubsCount — число клубов пользователя
        • profile.commentsCount — число комментариев пользователя (включая ответы, по данным БД)

    **Модель application (Applications)**
        • application.applicationId, application.clubName, application.appDescription, application.reason, application.appStatus ('PENDING', 'APPROVED', 'REJECTED')

## Имена переменных в Thymeleaf

    *Для фронтендера: обращение через ${...}*

    **currentUser** — объект текущего авторизованного пользователя (передается на все страницы для отображения в шапке и левом меню).
    **myClubs** — список клубов, в которых состоит пользователь (для левого сайдбара «Мои клубы»).
    **loginError** — ошибка входа (показывается в layout).
    **blockReason** — причина блокировки пользователя при входе (показывается в layout).
    **popularClubs** — топ клубов по количеству участников (для правого блока на главной). # пока не делаем
    **postsList** — список постов для вывода в ленте, на странице клуба или похожих блоках.
    **post** — один пост для страницы поста (`PostListItemResponse`).
    **clubsPage** — страница каталога клубов (`PageResponse<ClubListItemResponse>`: `content`, `page`, `size`, `totalElements`, `totalPages`).
    **clubPage** — данные страницы клуба (`ClubPageResponse`).
    **membersPage** — страница участников клуба (`PageResponse<ClubMemberListItemResponse>`).
    **clubForm** — форма редактирования клуба (`ClubEditRequest`: `club`, `about`, `pictureUrl`).
    **profile** — профиль пользователя (`ProfileResponse`).
    **profileForm** — форма редактирования профиля (`ProfileEditRequest`).
    **applications** — заявки на создание клубов (админ-панель).
    **applicationForm**, **applicationSuccess**, **applicationError** — форма/флеш-сообщения заявки на создание клуба.
    **successMessage / errorMessage** — стандартные flash-сообщения после действий.
    **returnUrl** — опциональный безопасный локальный путь для возврата после действия (комментарий, голосование, вступление/выход).

### Карта url маршрутов

    GET /login                              login.html                  Страница входа (Spring Security ожидает поля username и password).
    GET /register                           register.html               Страница регистрации.
    POST /register                          redirect:/login?registered  Обработка регистрации. Поля формы: firstName, lastName, email, password, passwordConfirm.

    GET / или GET /feed                     feed.html                   Главная страница с лентой. Поддерживает параметр ?tab=subscriptions для вкладки «Подписки».

    GET /clubs                              catalog.html                Каталог клубов. Параметры: `q`, `page`, `size`.
    GET /clubs/{id}                         club.html                   Страница клуба: описание, команда, роль пользователя, кнопки вступления/выхода.
    POST /clubs/{id}/join                   redirect:<returnUrl|Referer|/clubs/{id}>  Вступить в клуб (только авторизованный). Опционально: `returnUrl` (безопасный локальный путь). Успех/ошибка через flash `successMessage/errorMessage`.
    POST /clubs/{id}/leave                  redirect:<returnUrl|Referer|/clubs/{id}>  Покинуть клуб. Опционально: `returnUrl`. `MODERATOR/ADMIN` не могут выйти до передачи прав (flash `errorMessage`).
    GET /clubs/{id}/members                 club-members.html           Участники клуба. Только `ADMIN` клуба или `isAppAdmin`. Параметры: `page`, `size`.
    POST /clubs/{id}/members/{userId}/moderator/assign  redirect:/clubs/{id}/members  Назначить модератора (MEMBER -> MODERATOR).
    POST /clubs/{id}/members/{userId}/moderator/remove  redirect:/clubs/{id}/members  Снять модератора (MODERATOR -> MEMBER).
    GET /clubs/{id}/edit                    club-edit.html              Форма редактирования клуба. Только `ADMIN` клуба или `isAppAdmin`.
    POST /clubs/{id}/edit                   redirect:/clubs/{id}        Сохранить клуб. Поля: `club`, `about`, `pictureUrl`. Ошибка валидации — возврат `club-edit.html` с `errorMessage`.

    POST /clubs/{id}/posts/create           redirect:/clubs/{id}        Создание поста (только `MODERATOR/ADMIN` клуба). Поля: `title`, `body`, `imageUrl` (опционально).
    POST /clubs/{id}/posts/{postId}/delete  redirect:/clubs/{id}        Мягкое удаление поста (только `MODERATOR/ADMIN` клуба).
    GET /posts/{id}                         post.html                   Страница поста: пост, опрос, комментарии, сайдбар клуба.

    POST /posts/{id}/comments               redirect:<returnUrl|/clubs/{clubId}>  Добавление комментария. Поля: `comment`, `parentId` (опционально, для ответа), `returnUrl` (опционально). При ошибке возврат на `returnUrl`/безопасный Referer/клуб/ленту.
    POST /comments/{commentId}/delete       redirect:<returnUrl|...>    Мягкое удаление комментария. Права: автор, MODERATOR/ADMIN клуба или isAppAdmin. Поле: `returnUrl` (опционально).

    POST /polls/{pollId}/vote               redirect:<returnUrl|Referer|/feed>  Голосование в опросе. Поля: `voteId`, опционально `returnUrl` (безопасный локальный путь).

    GET /profile                            profile.html                Профиль текущего пользователя.
    GET /profile/edit                       profile-edit.html           Форма редактирования профиля.
    POST /profile/edit                      redirect:/profile           Редактирование профиля. Поля: firstName, lastName, about, profilePictureUrl.
    POST /applications/create               redirect:/profile           Отправка заявки на клуб. Поля: clubName, appDescription, reason.

    GET /admin                              admin.html                  Панель глобального админа (список заявок applications со статусом PENDING).
    POST /admin/applications/{id}/approve   redirect:/admin             Одобрение заявки и автоматическое создание клуба.
