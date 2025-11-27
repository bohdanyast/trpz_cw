function makeDefaultColorForDropDown() {
    $('.dropdown-value span').css('color', '#777');
}

function go_search() {
    if ($.trim($("#search-input").val()) == "") {
        return false;
    }
}

function setCustomUI(q) {
    if ($('#div-gpt-ad-1630326657192-0').length) {
        $('#div-gpt-ad-1630326657192-0').css('opacity', '0.5');
        var addOpacity = setInterval(function () {
            $('#div-gpt-ad-1630326657192-0').css('opacity', '1');
            clearInterval(addOpacity);
        }, 100);
    }

    var checkAdBlock = setInterval(function () {
        if ($('.gsc-adBlock').length) {
            $(window).blur(function () {
                if ($('iframe').is(':focus')) {
                    makeDefaultColorForDropDown();
                    $('.tool-bar-dropdown').slideUp(0).data('opened', false);
                }
            });
            var label = $('<div class="label-box"><span style="color: white">' + $('#translate').data('advertisements') + '</span></div>');
            if (!$('.gsc-adBlock .label-box').first().length) {
                $('.gsc-adBlock').first().prepend(label);
            }
            clearInterval(checkAdBlock);
        }
    }, 10);

    var checkExistPages = setInterval(function () {
        if ($('.gsc-cursor').length) {
            var spanLeft = $('<span>').attr({
                id: 'paginator-left'
            }).text($('#translate').data('back')).on('click', function (event) {
                $('.gsc-cursor').first().children('div.gsc-cursor-current-page').prev('div.gsc-cursor-page').click();
            });
            if ($('.gsc-cursor').first().children('div.gsc-cursor-current-page').prev('div.gsc-cursor-page').length) {
                $(spanLeft).addClass('paginator-a');
            } else {
                $(spanLeft).addClass('paginator-n');
            }

            var spanRight = $('<span>').attr({
                id: 'paginator-right'
            }).text($('#translate').data('forward')).on('click', function (event) {
                $('.gsc-cursor').first().children('div.gsc-cursor-current-page').next('div.gsc-cursor-page').click();
            });
            if ($('.gsc-cursor').first().children('div.gsc-cursor-current-page').next('div.gsc-cursor-page').length) {
                $(spanRight).addClass('paginator-a');
            } else {
                $(spanRight).addClass('paginator-n');
            }

            $('.gsc-cursor').first().prepend(spanLeft).append(spanRight);

            $('.gsc-cursor div').each(function (index) {
                if (!$(this).hasClass('gsc-cursor-current-page')) {
                    $(this).on('click', function (event) {
                        var label = q + ' (Page: ' + $(this).text() + ')';
                        gtag('event', 'click', {
                            'event_category': 'Serach page',
                            'event_label': label
                        });
                    });
                    $(this).on('keypress', function (event) {
                        if (event.keyCode == 13) {
                            var label = q + ' (Page: ' + $(this).text() + ')';
                            gtag('event', 'keypress', {
                                'event_category': 'Serach page',
                                'event_label': label
                            });
                        }
                    });
                }
            });
            clearInterval(checkExistPages);
        }
    }, 10);

    var checkExistBranding = setInterval(function () {
        if ($('.gcsc-more-maybe-branding-root a').length) {
            $('.gcsc-more-maybe-branding-root a').on('click', function (event) {
                gtag('event', 'click', {
                    'event_category': 'Search on Google',
                    'event_label': q
                });
            });
            clearInterval(checkExistBranding);
        }
    }, 10);
}

var firstLoad = true;
myWebResultsRenderedCallback = function (name, q, promos, results) {
    $('#search-input').val(q);
    setCustomUI(q);
    for (var i = 0; i < promos.concat(results).length; i++) {
        var div = promos.concat(results)[i];
        $(div).find('a.gs-title').first().click(function (event) {
            var title = $(this).text();
            var href = $(this).attr('href');
            var label = title + '(' + href + ')';
            $.ajax({
                type: 'GET',
                url: '//counter.ukr.net/lid/215/cnt.php',
                crossDomain: true,
                dataType: 'JSONP'
            });
            gtag('event', 'click', {
                'event_category': 'Serach result',
                'event_label': label
            });
        });
    }
    // refresh adsbygoogle
    if(!firstLoad){
        googletag.pubads().refresh([slot1]);
    }
    firstLoad = false;

    $.ajax({
        type: 'GET',
        url: '//counter.ukr.net/lid/39/cnt.php',
        crossDomain: true,
        dataType: 'JSONP'
    });

};

window.__gcse || (window.__gcse = {});
window.__gcse.searchCallbacks = {
    web: {
        rendered: 'myWebResultsRenderedCallback',

    },
};

$(document).ready(function () {
    $(function () {

        $('#search-input')
            .autocomplete({
                source: function (request, response) {
                    $.ajax({
                        url: '//suggestqueries.google.com/complete/search?client=firefox&q=' + request.term,
                        dataType: 'jsonp',
                        success: function (data) {
                            response($.map(data[1], function (item) {
                                return {
                                    label: item,
                                    value: item
                                }
                            }));
                        }
                    });
                },
                autoFill: true,
                minLength: 3,
                select: function (event, ui) {
                    $(this).closest('input').val(ui.item.value);
                    $(this).closest('form').trigger('submit');
                },
                focus: function (event, ui) {
                    Array.from(document.getElementById('ui-id-1').childNodes).forEach(function (item) {
                        $(item).removeClass('ui-menu-item-hovered');
                        if (item.innerText === ui.item.value) {
                            $(item).addClass('ui-menu-item-hovered');
                        }
                    });
                }
            })
            .focus(function () {
                this.select()
            });
    });

    $('.tool-bar-dropdown').slideUp(0, function () {
        $(this).data('opened', false)
    });

    $('.dropdown-value').click(function (event) {
        makeDefaultColorForDropDown();
        event.preventDefault();
        event.stopPropagation();
        $(event.target).css('color', 'black');
        var m = this;
        var id = $(this).parent().attr('id');
        if (!$(m).data('opened')) {
            $(m).next('.tool-bar-dropdown').slideDown(100, function () {
                $(m).data('opened', true);
            });
        } else {
            $(m).next('.tool-bar-dropdown').slideUp(0, function () {
                $(m).data('opened', false);
            });
        }
        $('.dropdown-value').next('.tool-bar-dropdown').each(function () {
            if ($(this).parent().attr('id') != id) {
                $(this).slideUp(0, function () {
                    $(this).prev().data('opened', false);
                });
            }
        });
    });

    $('#src input').keypress(function (event) {
        if (event.which == 13) {
            event.preventDefault();
            srcSubmit();
        }
    });

    $('#src input').click(function (event) {
        event.preventDefault();
        event.stopPropagation();
    });

    $('#src button').click(function (event) {
        event.preventDefault();
        event.stopPropagation();
        srcSubmit();
    });

    function srcSubmit() {
        $('#search-input').val($('#search-input').val().replace(/ site:.*$/i, '') + ' site:' + $('#src input').val());
        document.searchForm.submit();
    }

    $(document).click(function () {
        makeDefaultColorForDropDown();
        $('.tool-bar-dropdown').slideUp(0).data('opened', false);
    });

    function fixedColumn() {
        var topPosition = $('#fff-column').get(0).getBoundingClientRect().top - 10;
        if (topPosition <= 0 && $(window).height() >= 770) {
            $('#div-gpt-ad-1630326657192-0').css({
                'top': '20px',
                'position': 'fixed'
            });
        } else {
            $('#div-gpt-ad-1630326657192-0').css({
                'top': 'auto',
                'position': 'static'
            });
        }
    }
    fixedColumn();
    $(window).on('scroll', function () {
        fixedColumn();
    });
});