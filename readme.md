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
    **popularClubs** — топ клубов по количеству участников (для правого блока на главной). # пока не делаем
    **postsList** — список постов для вывода в ленте или на странице клуба.
    **clubsList** — список всех клубов для страницы каталога.
    **club** — объект конкретного клуба (на странице клуба).
    **userRoleInClub** — роль текущего юзера в открытом клубе ('NONE', 'MEMBER', 'MODERATOR', 'ADMIN'), чтобы фронтендер знала, какие кнопки показывать («Вступить» или «Создать пост»).

### Карта url маршрутов

    GET /login	                            login.html	                Страница входа (Spring Security ожидает поля username и password).
    GET /register	                        register.html	            Страница регистрации.
    POST /register	                        redirect:/login	            Обработка регистрации. Поля формы: firstName, lastName, email, password.
    GET / или GET /feed	                    feed.html	                Главная страница с лентой. Поддерживает параметр ?tab=subscriptions для вкладки «Подписки».
    GET /clubs	                            catalog.html	            Каталог всех доступных клубов (поиск через ?q=...).
    GET /clubs/{id}	                        club.html	                Страница конкретного клуба с его постами, описанием и списком команды.
    
    POST /clubs/{id}/join	                redirect:/clubs/{id}	    Вступить в клуб.
    POST /clubs/{id}/leave	                redirect:/clubs/{id}	    Покинуть клуб.
    POST /clubs/{id}/posts/create	        redirect:/clubs/{id}	    Создание поста (для админа/модера). Поля: title, body, imageUrl (опционально).
    POST /posts/{id}/comments	            redirect:/clubs/{clubId}	Добавление комментария. Поля: comment, parentId (скрытое поле для ответа).
    POST /polls/{pollId}/vote	            redirect:/feed	            Голосование в опросе. Поле: voteId.
    
    GET /profile	                        profile.html	            Профиль текущего пользователя и форма подачи заявки на новый клуб.
    POST /applications/create	            redirect:/profile	        Отправка заявки на клуб. Поля: clubName, appDescription, reason.
    
    GET /admin	                            admin.html	                Панель глобального админа (список заявок applications со статусом PENDING).
    POST /admin/applications/{id}/approve	redirect:/admin	            Одобрение заявки и автоматическое создание клуба.

