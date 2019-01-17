

    window.onload = function () {
        const tests = document.getElementsByTagName('template');
        var cnt = 1;
        Object.values(tests).forEach(function(test) {
            const c = test.content;
            const expected = c.querySelector('#expected');
            const actual = c.querySelector('#actual');
            const scpt = c.querySelector('script');
            const title = cnt++ + ' - ' + test.title;

            renderTest(expected, actual, scpt, title, test.className);
        });
    };

    function renderTest(expected, actual, script, title, className) {
        const iframe = document.createElement('iframe');
        iframe.src = './template.html';
        iframe.style.minHeight= '200px';
        const idiv = document.createElement('div');
        idiv.className = 'tst-iframe-container';
        idiv.appendChild(iframe);
        document.getElementById('tst-container').appendChild(idiv);

        iframe.contentWindow.template = {expected, actual, script, title, className};
        iframe.contentWindow.resizeIframeToHeight= function (size) {
            iframe.style.minHeight= size;
        };
    }

