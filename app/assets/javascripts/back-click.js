document.addEventListener('DOMContentLoaded', function (event) {
    var backLink = document.querySelector('.govuk-back-link');
    if (backLink !== null) {
        backLink.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            window.history.back();
        });
    }
});