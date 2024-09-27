// theme-switcher.js
const button = document.getElementById('myButton');
const article = document.getElementById('myArticle');
button.addEventListener('click', () => {
    document.body.classList.toggle('dark');
// Обновление aria-label
    button.ariaLabel = document.body.classList.contains('dark') ?
        'button for switching to light theme' :
        'button for switching to dark theme';

// Обновление alt
    button.alt = document.body.classList.contains('dark') ?
        'sun icon' :
        'moon icon';

// Обновление изображения
    button.src = document.body.classList.contains('dark') ?
        'sun.svg' :
        'moon.svg';

// Переключение классов для статьи
    if (document.body.classList.contains('dark')) {
        article.classList.remove('light-article');
        article.classList.add('dark-article');
        Cookies.set('theme', 'dark'); // Обновляем куки для dark темы
    } else {
        article.classList.remove('dark-article');
        article.classList.add('light-article');
        Cookies.set('theme', 'light'); // Обновляем куки для light темы
    }
});