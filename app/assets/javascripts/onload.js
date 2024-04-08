(function (document, window) {
    window.onpageshow = function(event) {
        if (event.persisted) {
            window.location.reload();
        }
    };
})(document, window);