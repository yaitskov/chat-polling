function scheduleFunction(f) {
    setTimeout(f, 2000);
}

function renderMessage(item) {
    var msgTemplate = $("#message-template");
    var message = msgTemplate.clone();
    message.removeAttr("id");
    if (item.author) {
        message.find(".author .name").text(item.author);
    } else {
        message.find(".author").remove();
    }
    message.find(".date").text(new Date(item.created) + "");
    message.find(".content").text(item.content);
    message.removeClass("hide");
    return message;
}

function displayError(display, error) {
    var err = $("<div class='alert alert-error'/>");
    err.text(error.error + ": " + error.message);
    display.append(err);

}

function getCurrentTz() {
    var tz = new Date().getTimezoneOffset() / 60;
    tz = (tz < 0 ? "-" : "+") + (Math.abs(tz) < 10 ? "0" : "") + Math.abs(tz);
    return tz;
}

function normalizeTimeIfRequired(form, pattern, tz) {
    var v = form.find(pattern).val();
    if (v && !v.match(".000[+-][0-9]{2}$")) {
        form.find(pattern).val(v.replace(' ', 'T') + ".000" + tz);
    }
}

function normalizeTime(form) {
    var tz = getCurrentTz();
    normalizeTimeIfRequired(form, "input[name=start]", tz);
    normalizeTimeIfRequired(form, "input[name=end]", tz);
}

function renderHistory(form, reset) {
    if (form.data("processing")) {
        return;
    }
    normalizeTime(form);
    form.data("processing", 1);
    var display = $("#history .messages");
    display.empty();
    var pages = $("#history .pages");
    if (reset) {
        pages.empty();
        form.find("input[name=page]").val(0);
    }
    $.ajax({
        url: "/history",
        data: form.serialize()
    }).done(function (response) {
            form.removeData("processing");
            if (response.error) {
                displayError(display, response);
            } else {
                if (reset) {
                    renderPaging(pages, response.pages);
                }
                for (var i in response.items) {
                    display.append(renderMessage(response.items[i]));
                }
            }
        }).fail(function () {
            form.removeData("processing");
        });
}

function renderPaging(root, pages) {
    root.empty();
    for (i = 0; i < pages; ++i) {
        var page = $("<span></span>");
        page.text(i + 1);
        page.addClass('current');
        page.click(function () {
            root.find(".current").removeClass("current");
            $(this).addClass("current");
            $("#history-form").find("input[name=page]").val($(this).text() - 1);
            renderHistory($("#history-form"), false);
        });
        root.append(page);
    }
}

function renderNewMessages(body, display) {
    var last = display.data("last");
    var number = display.data("number");
    for (var i in body) {
        var item = body[i];
        var message = renderMessage(item);
        display.append(message);

        if (last) {
            if (last == item.created) {
                number += 1;
            } else {
                number = 1;
                last = item.created;
            }
        } else {
            last = item.created;
            number = 1;
        }
    }
    display.data("last", last);
    display.data("number", number);
}

function pullMessages() {
    var display = $("#chat .messages");
    var data = { topic: $("#message-form input[name=topic]").val(),
                 number: display.data("number") };
    if (display.data('last')) {
        data.last = new Date(display.data('last')).toISOString();
    }
    $.ajax({
        url: "/get",
        data: data
    }).done(function (body) {
            if (body.error) {
                displayError(display, body);
            } else {
                renderNewMessages(body, display);
            }
            scheduleFunction(pullMessages);
        }).fail(function () {
            scheduleFunction(pullMessages);
        });
}

$(document).ready(function () {
    $("#set-author-btn").click(function (e) {
        //e.preventDefault();
        var nickname = $("input[type=text][name=nickname]").val();
        $("#set-author-form").modal("hide");
        $("#set-author").hide();
        $("input[name=author]").val(nickname);
    });

    $("#history-form").submit(function () {
        var form = $(this);
        renderHistory(form, true);
        return false;
    });

    $("#message-form").submit(function () {
        var form = $(this);
        if (form.data("processing")) {
            return false;
        }
        form.data("processing", 1);
        $.ajax({
            url:"/send",
            data: form.serialize(),
            success: function (response) {
                if (response.error) {
                    displayError($("#chat .messages"), response);
                } else {
                    $("textarea[name=content]").val('');
                }
            }
        }).done(function (response) {
                form.removeData("processing");
            }).fail(function () {
                form.removeData("processing");
            });
        return false;
    });

    scheduleFunction(pullMessages);

    $('#datetimepicker-start, #datetimepicker-end').datetimepicker();
});