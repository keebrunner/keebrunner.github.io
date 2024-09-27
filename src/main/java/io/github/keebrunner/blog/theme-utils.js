// theme-utils.js
function updateTheme(theme) {
    document.body.classList.remove('dark', 'light');
    document.body.classList.add(theme);

    const article = document.getElementById('myArticle');
    article.classList.remove('dark-article', 'light-article');
    article.classList.add(theme === 'dark' ? 'dark-article' : 'light-article');

    const button = document.getElementById('myButton');
    const buttonParent = button.parentElement;

    if (theme === 'dark') {
        button.src = 'sun.svg';
        button.alt = 'sun icon';
        buttonParent.ariaLabel = 'button for switching to light theme';
    } else {
        button.src = 'moon.svg';
        button.alt = 'moon icon';
        buttonParent.ariaLabel = 'button for switching to dark theme';
    }
}

function getThemeFromCookies() {
    let theme = Cookies.get('theme');
    return theme || 'light';
}

document.addEventListener('DOMContentLoaded', () => {
    const theme = getThemeFromCookies();
    updateTheme(theme);

    const button = document.getElementById('myButton');
    button.addEventListener('click', () => {
        const newTheme = theme === 'dark' ? 'light' : 'dark';
        Cookies.set('theme', newTheme);
        updateTheme(newTheme);
    });
});