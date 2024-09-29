// theme-loader.js & theme-switcher.js (объединено)

const themeButton = document.getElementById('myButton');
const articleElement = document.getElementById('myArticle');

// Функция для обновления темы
function updateTheme(theme) {
    document.body.classList.remove('dark', 'light');
    document.body.classList.add(theme);

    articleElement.classList.remove('dark-article', 'light-article');
    articleElement.classList.add(theme === 'dark' ? 'dark-article' : 'light-article');

    themeButton.src = theme === 'dark' ? 'img/sun.svg' : 'img/moon.svg';
    themeButton.alt = theme === 'dark' ? 'img/sun icon' : 'img/moon icon';
    themeButton.parentElement.ariaLabel = theme === 'dark' ? 'button for switching to light theme' : 'button for switching to dark theme';
    themeButton.ariaPressed = theme === 'dark'; // Добавляем aria-pressed
}

// Функция для переключения темы
function toggleTheme() {
    const currentTheme = document.body.classList.contains('dark') ? 'dark' : 'light';
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    updateTheme(newTheme);
    Cookies.set('theme', newTheme);
}

// Загрузка темы из cookies при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    let theme = Cookies.get('theme') || 'light';
    updateTheme(theme);
});

// Добавляем обработчик события для кнопки переключения темы
themeButton.addEventListener('click', toggleTheme);