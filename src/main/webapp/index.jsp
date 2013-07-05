<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <meta charset="utf-8" />
    <title>Chat</title>
    <meta name="description" content="chat">
    <meta name="author" content="Daneel Yaitskov">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <link rel="stylesheet" href="/static/bootstrap.min.css">
    <link rel="stylesheet" href="/static/bootstrap-datetimepicker.min.css">
    <link rel="stylesheet" href="/static/style.css">
    <link rel="shortcut icon" href="/favicon.ico">
    <script type="text/javascript" src="/static/jquery.js"></script>
    <script type="text/javascript" src="/static/bootstrap.min.js"></script>
    <script type="text/javascript" src="/static/bootstrap-datetimepicker.min.js"></script>
    <script type="text/javascript" src="/static/app.js"></script>
    <script type="text/javascript">
    </script>
</head>
<body>
<div class="container">
    <div class="well">
        <div id="message-template" class="hide message">
            <div class="author">From <span class="name"></span></div>
            <div class="date"></div>
            <pre class="content"></pre>
        </div>
        <div id="set-author-form" class="modal hide fade" tabindex="-1">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h3>Set nickname</h3>
            </div>
            <div class="modal-body">
                <div class="control-group">
                    <label class="control-label" for="input_content">
                        Nickname
                    </label>

                    <div class="controls">
                        <input type="text" maxlength="100" name="nickname"/>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <a href="#" id="set-author-btn" class="btn btn-danger">Set</a>
                <a href="#" data-dismiss="modal" class="btn btn-primary">Cancel</a>
            </div>
        </div>
        <ul class="nav nav-tabs">
            <li class="active"><a href="#chat" data-toggle="tab">Chat</a></li>
            <li><a href="#history" data-toggle="tab">History</a></li>
        </ul>

        <div class="tab-content">
            <div class="tab-pane active" id="chat">
                <div class="messages">
                </div>
                <form id="message-form">
                    <input type="hidden" name="topic" value="1"/>
                    <input type="hidden" name="author" value=""/>

                    <div class="control-group">
                        <label class="control-label" for="input_content">
                            Content
                        </label>

                        <div class="controls">
                            <textarea name="content" id="input_content" maxlength="1000"
                                      placeholder="Enter the message"></textarea>
                        </div>
                    </div>
                    <div class="control-group">
                        <div class="controls">
                            <button type="submit" class="btn">Send</button>
                            <button type="button" class="btn" id="set-author" href="#set-author-form"
                                    data-toggle="modal">Set nickname
                            </button>
                        </div>
                    </div>
                </form>
            </div>
            <div class="tab-pane" id="history">
                <div class="pages"></div>
                <div class="messages">
                </div>
                <form id="history-form">
                    <input type="hidden" name="topic" value="1"/>
                    <input type="hidden" name="page" value="0"/>

                    <div class="control-group">
                        <label class="control-label" for="input_content">
                            Start
                        </label>

                        <div class="controls">
                            <div id="datetimepicker-start" class="input-append date">
                                <input data-format="yyyy-MM-dd hh:mm:ss" type="text" name="start"/>
                                <span class="add-on">
                                    <i data-time-icon="icon-time" data-date-icon="icon-calendar"/>
                                </span>
                            </div>
                        </div>
                    </div>
                    <div class="control-group">
                        <label class="control-label" for="input_content">
                            End
                        </label>

                        <div class="controls">
                            <div id="datetimepicker-end" class="input-append date">
                                <input data-format="yyyy-MM-dd hh:mm:ss" type="text" name="end"/>
                                <span class="add-on">
                                    <i data-time-icon="icon-time" data-date-icon="icon-calendar"/>
                                </span>
                            </div>
                        </div>
                    </div>
                    <div class="control-group">
                        <div class="controls">
                            <button type="submit" class="btn">Search</button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

</body>
</html>
