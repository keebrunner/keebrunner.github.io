const backToTopButton = document.getElementById('backToTop');

// Скролл вниз - показываем кнопку
window.addEventListener('scroll', () => {
    if (window.scrollY > 80) {
        backToTopButton.classList.add('show');
    } else {
        backToTopButton.classList.remove('show');
    }
});

// Нажатие на кнопку - скролл наверх
backToTopButton.addEventListener('click', () => {
    window.scrollTo({
        top: 0,
        behavior: 'smooth'
    });
});