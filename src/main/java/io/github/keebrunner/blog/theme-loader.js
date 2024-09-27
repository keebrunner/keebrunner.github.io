// theme-loader.js
document.addEventListener('DOMContentLoaded', () => {
    // Get the theme from cookies
    let theme = Cookies.get('theme');
    // If theme is not set, default to light
    if (!theme) {
        theme = 'light';
        Cookies.set('theme', theme);
    }

    // Function to update theme classes on the article
    function updateArticleTheme(theme) {
        const article = document.getElementById('myArticle');
        article.classList.remove('dark-article', 'light-article'); // Remove old classes
        if (theme === 'dark') {
            article.classList.add('dark-article');
        } else {
            article.classList.add('light-article');
        }
    }

    // Add the theme class to the body and update article theme initially
    document.body.classList.add(theme);
    updateArticleTheme(theme);

    // Set the initial image, alt text, and aria-label of the theme button
    // based on the loaded theme
    const themeButton = document.getElementById('myButton');
    const button = themeButton.parentElement; // Get the <button> element

    if (theme === 'dark') {
        themeButton.src = 'sun.svg';
        themeButton.alt = 'sun icon';
        button.ariaLabel = 'button for switching to light theme';
    } else {
        themeButton.src = 'moon.svg';
        themeButton.alt = 'moon icon';
        button.ariaLabel = 'button for switching to dark theme';
    }

    // Add an event listener to the theme button to update the theme
    themeButton.addEventListener('click', () => {
        if (theme === 'dark') {
            theme = 'light';
        } else {
            theme = 'dark';
        }
        Cookies.set('theme', theme);
        document.body.classList.remove('dark', 'light'); // Remove old theme class
        document.body.classList.add(theme); // Add new theme class
        updateArticleTheme(theme); // Update article theme

        // Update button image, alt text, and aria-label
        if (theme === 'dark') {
            themeButton.src = 'sun.svg';
            themeButton.alt = 'sun icon';
            button.ariaLabel = 'button for switching to light theme';
        } else {
            themeButton.src = 'moon.svg';
            themeButton.alt = 'moon icon';
            button.ariaLabel = 'button for switching to dark theme';
        }
    });
});