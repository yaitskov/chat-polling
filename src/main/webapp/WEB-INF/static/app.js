function scheduleFunction(f) {
    setTimeout(f, 10);
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
    message.data('created', item.created);
    message.data("id", item.id);
    message.find(".date").text(new Date(item.created) + "");
    message.find(".content").text(item.content);
    message.removeClass("hide");
    return message;
}

function findIdsOfRecentMessages() {
    var result = [];
    var previousCreation = 0;
    var lastMessages = $("#chat .messages > div.message").slice(-20);
    for (var i = 0; i < lastMessages.length; ++i) {
        item = $(lastMessages.get(i));
        if (item.data("id")) {
            if (result.length !== 0) {
                var id = item.data("id");
                result.push(id);
            } else if (previousCreation === 0) {
                previousCreation = 0 + item.data("created");
            } else if (previousCreation < item.data("created")) {
                var id = item.data("id");
                result.push(id);
            }
        }
    }
    return { ids: result, createdAfter: previousCreation };
}

function displayError(display, error) {
    var err = $("<div class='alert alert-error'/>");
    err.text(error.error + ": " + error.message);
    var last = display.find(">div:last-child");
    if (last.is(".alert-error")) {
        console.error(err.text());
    } else {
        display.append(err);
    }
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
    for (var i in body) {
        var item = body[i];
        var message = renderMessage(item);
        var created = 0 + item.created;
        var messages = display.find(".message");
        if (messages.length === 0) {
            display.append(message);
        } else if (created > $(messages.get(messages.length - 1)).data('created')) {
            display.append(message);
        } else {
            for (var j = 0; j < messages.length; ++j) {
                var existing = $(messages.get(j));
                if (created < existing.data("created")) {
                    existing.before(message);
                    break;
                }
            }
        }
    }
}

/**
 * I tried to use HTTP header to pass message date
 * but ajax starts to bug: done and fail callbacks are called multiple time.
 * The bottom line messages are duplicated profoundly.
 */
function pullMessages() {
    var display = $("#chat .messages");
    var idsAndBorder = findIdsOfRecentMessages();
    var data = { topic: $("#message-form input[name=topic]").val(),
                 'KnownIds': idsAndBorder.ids.join(" "),
                 'firstDate': idsAndBorder.createdAfter };
    console.log("pullMessage 1 start: cache " + data.KnownIds);
    $.ajax({
        url: "/api/get",
        dataType: 'json',
        success: function (body, status, jqXHR) {
            if (body.error) {
                displayError(display, body);
            } else if (body instanceof Array && body.length) {
                renderNewMessages(body, display);
            }
            scheduleFunction(pullMessages);
        },
        error: function () {
            scheduleFunction(pullMessages);
        },
        data: data
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

    $("#chat textarea").keypress(function (e) {
        if (e.keyCode == 13 || e.keyCode == 10) {
            $("#message-form").submit();
        }
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