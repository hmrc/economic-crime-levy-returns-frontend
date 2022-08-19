(function (document, window) {
    // To enable this script to work in IE11 as forEach is not supported on a NodeList
    if (typeof NodeList.prototype.forEach !== 'function') {
        NodeList.prototype.forEach = Array.prototype.forEach;
    }

    document.querySelectorAll('a[href="#print-dialogue"]')
        .forEach(function (link) {
            link.addEventListener('click', function (event) {
                event.preventDefault();
                window.print();
            })
        })
})(document, window);